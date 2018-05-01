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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Map;

@JsonDeserialize(as = com.github.jcustenborder.parsers.elf.ImmutableLogEntry.class)
@JsonSerialize(as = com.github.jcustenborder.parsers.elf.ImmutableLogEntry.class)
@Value.Immutable
public interface LogEntry {
  /**
   * Map containing the field name to java type for the classes.
   * @return
   */
  Map<String, Class<?>> fieldTypes();

  /**
   * Map containing the field data.
   * @return
   */
  @AllowNulls
  Map<String, Object> fieldData();
}
