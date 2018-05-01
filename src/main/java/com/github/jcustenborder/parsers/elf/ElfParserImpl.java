/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.parsers.elf;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ElfParserImpl implements ElfParser {
  static final String NULL_INDICATOR = "-";
  private static final Logger log = LoggerFactory.getLogger(ElfParserImpl.class);
  private final LineNumberReader lineReader;
  private final List<ParserEntry> fieldParsers;
  private final Map<String, Class<?>> fieldTypes;
  private final CSVParser parser;

  ElfParserImpl(LineNumberReader lineReader, List<ParserEntry> fieldParsers) {
    this.lineReader = lineReader;
    this.fieldParsers = fieldParsers;
    this.fieldTypes = this.fieldParsers
        .stream()
        .collect(Collectors.toMap(
            p -> p.fieldName,
            p -> p.parser.fieldType())
        );

    this.parser = new CSVParserBuilder()
        .withQuoteChar('"')
        .withSeparator(' ')
        .withFieldAsNull(CSVReaderNullFieldIndicator.NEITHER)
        .build();
  }

  public LogEntry next() throws IOException {

    String line;
    while (null != (line = this.lineReader.readLine())) {
      if (line.startsWith("#")) {
        log.trace("next() - Skipping line. Starts with #.");
        continue;
      }
      log.trace("next() - Processing line '{}'", line);
      String[] unparsedData = this.parser.parseLine(line);
      Map<String, Object> data = new LinkedHashMap<>(this.fieldParsers.size());
      for (int i = 0; i < this.fieldParsers.size(); i++) {
        ParserEntry entry = this.fieldParsers.get(i);
        log.trace("next() - Converting field('{}') to {}", entry.fieldName, entry.parser.fieldType());
        String input = unparsedData[i];

        final Object value;

        if (NULL_INDICATOR.equals(input)) {
          value = null;
        } else {
          value = entry.parser.parse(input);
        }

        data.put(entry.fieldName, value);
      }

      return com.github.jcustenborder.parsers.elf.ImmutableLogEntry.builder()
          .fieldData(data)
          .fieldTypes(this.fieldTypes)
          .build();

    }

    return null;
  }


  @Override
  public void close() throws Exception {
    this.lineReader.close();
  }
}
