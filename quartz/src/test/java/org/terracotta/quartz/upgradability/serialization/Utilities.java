/*
 * Copyright 2013 Terracotta, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.quartz.upgradability.serialization;

/**
 *
 * @author cdennis
 */
public final class Utilities {
  
  private Utilities() {
    
  }
  
  public static String[] expand(String pattern, String ... arguments) {
    String[] expansion = new String[arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      expansion[i] = pattern.replace("{?}", arguments[i]);
    }
    return expansion;
  }
}
