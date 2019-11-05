/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.parsers.elf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class ElfParserImpl implements ElfParser {
  static final String NULL_INDICATOR = "-";
  private static final Logger log = LoggerFactory.getLogger(ElfParserImpl.class);
  private final LineNumberReader lineReader;
  private final List<ParserEntry> fieldParsers;
  private final Map<String, Class<?>> fieldTypes;

  ElfParserImpl(LineNumberReader lineReader, List<ParserEntry> fieldParsers) {
    this.lineReader = lineReader;
    this.fieldParsers = fieldParsers;

    List<String> duplicateFields = this.fieldParsers.stream()
        .collect(Collectors.groupingBy(ParserEntry::fieldName))
        .entrySet().stream()
        .filter(e -> e.getValue().size() > 1)
        .map(Map.Entry::getKey)
        .sorted()
        .collect(Collectors.toList());

    if (!duplicateFields.isEmpty()) {
      String fieldNames = String.join(", ", duplicateFields);
      throw new IllegalStateException(
          String.format("Field(s) are defined more than once: %s", fieldNames)
      );
    }

    Map<String, Class<?>> fieldTypes = this.fieldParsers
        .stream()
        .collect(
            Collectors.toMap(
                ParserEntry::fieldName,
                p -> p.parser().fieldType(),
                (e1, e2) -> e1,
                LinkedHashMap::new
            )
        );
    this.fieldTypes = Collections.unmodifiableMap(fieldTypes);
  }

  @Override
  public Map<String, Class<?>> fieldTypes() {
    return this.fieldTypes;
  }

  static final Pattern SPLITTER = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

  public LogEntry next() throws IOException {
    String line;
    while (null != (line = this.lineReader.readLine())) {
      final int lineNumber = this.lineReader.getLineNumber();
      if (line.startsWith("#")) {
        log.trace("next() - Skipping line {}. Starts with #.", lineNumber);
        continue;
      }
      log.trace("next() - Processing line {}: '{}'", lineNumber, line);
      Matcher lineMatcher = SPLITTER.matcher(line);
      int fieldIndex = 0;

      Map<String, Object> data = new LinkedHashMap<>(this.fieldParsers.size());
      while (lineMatcher.find()) {
        final ParserEntry entry;
        try {
          entry = this.fieldParsers.get(fieldIndex);
        } catch (IndexOutOfBoundsException ex) {
          throw new IllegalStateException(
              String.format(
                  "Line %s has more field(s) than specified in the header. fieldIndex = %s",
                  lineNumber,
                  fieldIndex
              ),
              ex
          );
        }

        String input = lineMatcher.group(0);
        if (input.startsWith("\"") && input.endsWith("\"")) {
          input = input.replaceAll("^\"|\"$", "");
        }
        log.trace(
            "next() - Processing line {} field({}) fieldIndex {}: '{}'",
            lineNumber,
            entry.fieldName(),
            fieldIndex,
            input
        );
        final Object fieldValue;

        if (NULL_INDICATOR.equals(input)) {
          fieldValue = null;
        } else {
          try {
            fieldValue = entry.parser().parse(input);
          } catch (Exception ex) {
            throw new IOException(
                String.format(
                    "Could not parse line %s fieldIndex %s input = '%s'",
                    lineNumber,
                    fieldIndex,
                    input
                ),
                ex
            );
          }
        }
        data.put(entry.fieldName(), fieldValue);
        fieldIndex++;
      }

      return com.github.jcustenborder.parsers.elf.ImmutableLogEntry.builder()
          .fieldData(data)
          .fieldTypes(this.fieldTypes)
          .build();
    }

    return null;
  }


  @Override
  public void close() throws IOException {
    this.lineReader.close();
  }
}
