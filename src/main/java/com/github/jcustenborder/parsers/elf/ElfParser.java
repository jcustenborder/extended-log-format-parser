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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public interface ElfParser extends Closeable {
  /**
   * The data types associated with the fields.
   * @return
   */
  Map<String, Class<?>> fieldTypes();

  /**
   * Method used to return the next LogEntry.
   * @return LogEntry if one is available. Null if at the end of the file.
   * @throws IOException
   */
  LogEntry next() throws IOException;
}
