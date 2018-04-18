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

import com.github.jcustenborder.parsers.elf.parsers.FieldParser;
import com.github.jcustenborder.parsers.elf.parsers.FieldParsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElfParserBuilder {
  static final Map<String, FieldParser> DEFAULT_PARSERS;
  static final Pattern HEADER_PATTERN = Pattern.compile("#([\\S]+):\\s+(.+)");
  static final String HEADER_FIELDS = "Fields";
  private static final Logger log = LoggerFactory.getLogger(ElfParserBuilder.class);

  static {
    Map<String, FieldParser> fieldParsers = new HashMap<>();
    fieldParsers.put("date", FieldParsers.DATE);
    fieldParsers.put("time", FieldParsers.TIME);
    fieldParsers.put("time-taken", FieldParsers.LONG);
    fieldParsers.put("sc-status", FieldParsers.LONG);
    fieldParsers.put("sc-bytes", FieldParsers.LONG);
    fieldParsers.put("cs-bytes", FieldParsers.LONG);
    fieldParsers.put("cs-uri-port", FieldParsers.INT);

    DEFAULT_PARSERS = Collections.unmodifiableMap(fieldParsers);
  }

  final Map<String, FieldParser> fieldParsers = new LinkedHashMap<>();
  Matcher headerMatcher = HEADER_PATTERN.matcher("");

  private ElfParserBuilder() {

  }

  public static ElfParserBuilder of() {
    return new ElfParserBuilder();
  }

  public ElfParser build(Reader reader) throws IOException {
    LineNumberReader lineNumberReader = new LineNumberReader(reader);

    String line;
    List<String> fieldNames = new ArrayList<>(100);

    while (null != (line = lineNumberReader.readLine())
        && headerMatcher.reset(line).find()
        && (lineNumberReader.getLineNumber() < 20)) {
      final String headerName = headerMatcher.group(1);
      final String headerValue = headerMatcher.group(2);
      log.trace("build() - line = '{}'", line);
      log.trace("build() - headerName = '{}'", headerName);
      log.trace("build() - headerValue = '{}'", headerValue);

      if (HEADER_FIELDS.equalsIgnoreCase(headerName)) {
        String[] fields = headerValue.split("\\s+");
        Collections.addAll(fieldNames, fields);
      }
    }

    if (fieldNames.isEmpty()) {
      throw new IllegalStateException(
          String.format("No Fields found after reading {} line(s)", lineNumberReader.getLineNumber())
      );
    }
    log.trace("build() - Found {} field(s). {}", fieldNames.size(), fieldNames);

    List<ParserEntry> parsers = new ArrayList<>();
    for (String fieldName : fieldNames) {
      log.trace("build() - Determining parser for field({}).", fieldName);
      FieldParser parser = this.fieldParsers.computeIfAbsent(fieldName, s -> {
        log.trace("build() - No definition for {}. Checking defaults", fieldName);
        FieldParser p = DEFAULT_PARSERS.get(fieldName);
        if (null != p) {
          return p;
        }

        return FieldParsers.STRING;
      });

      parsers.add(ParserEntry.of(fieldName, parser));
    }
    return new ElfParserImpl(lineNumberReader, parsers);
  }

  public ElfParser build(InputStream inputStream) throws IOException {
    return build(new InputStreamReader(inputStream));
  }

  public ElfParser build(File file) throws IOException {
    return build(new FileInputStream(file));
  }
}
