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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class ElfParserImplTest {

  ObjectMapper objectMapper;

  @BeforeEach
  public void beforeEach() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    this.objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, true);
    this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
    this.objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  @TestFactory
  public Stream<DynamicTest> parse() {
    File inputRoot = new File("src/test/resources/com/github/jcustenborder/parsers/elf/");
    return Arrays.stream(inputRoot.listFiles(f -> f.getName().endsWith(".json")))
        .map(inputFile -> dynamicTest(inputFile.getName(), () -> {
          ElfParserTestCase testCase = objectMapper.readValue(inputFile, ElfParserTestCase.class);
          try (ElfParser parser = ElfParserBuilder.of().build(new StringReader(testCase.input))) {

            List<LogEntry> actual = new ArrayList<>();
            LogEntry entry;
            while (null != (entry = parser.next())) {
              actual.add(entry);
            }
            assertEquals(testCase.expected.size(), actual.size());
            for (int i = 0; i < testCase.expected.size(); i++) {
              final LogEntry actualEntry = actual.get(i);
              final LogEntry expectedEntry = clean(testCase.expected, i);

              assertEquals(expectedEntry, actualEntry);
            }
          }
        }));
  }

  private LogEntry clean(List<LogEntry> expected, int i) {
    LogEntry dirtyEntry = expected.get(i);

    Map<String, Object> data = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : dirtyEntry.fieldData().entrySet()) {
      Class<?> expectedClass = dirtyEntry.fieldTypes().get(entry.getKey());
      Object value = objectMapper.convertValue(entry.getValue(), expectedClass);
      data.put(entry.getKey(), value);
    }

    return ImmutableLogEntry.builder()
        .fieldData(data)
        .fieldTypes(dirtyEntry.fieldTypes())
        .build();
  }


  @Disabled
  @TestFactory
  public Stream<DynamicTest> convert() {
    File inputRoot = new File("src/test/resources/com/github/jcustenborder/parsers/elf/");
    return Arrays.stream(inputRoot.listFiles(f -> f.getName().endsWith(".log")))
        .map(inputFile -> dynamicTest(inputFile.getName(), () -> {
          try (ElfParser parser = ElfParserBuilder.of().build(inputFile)) {
            ElfParserTestCase testCase = new ElfParserTestCase();
            testCase.expected = new ArrayList<>();
            LogEntry entry;
            while (null != (entry = parser.next())) {
              testCase.expected.add(entry);
            }
            testCase.input = new String(Files.readAllBytes(inputFile.toPath()), "UTF-8");

            File outputFile = new File(inputRoot, inputFile.getName().replaceAll("log$", "json"));
            objectMapper.writeValue(outputFile, testCase);
          }
        }));
  }
}
