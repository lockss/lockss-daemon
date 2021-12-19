/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.daemon;

import org.lockss.util.*;

import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.*;
import com.jayway.jsonpath.spi.mapper.*;

import java.util.*;

import org.lockss.app.*;

/**
 * A place to put miscellaneous one-time setup, such as configuring
 * other software packages
 * 
 * @since 1.75.8
 */
public class MiscSetupManager extends BaseLockssDaemonManager {

  private static final Logger log = Logger.getLogger(MiscSetupManager.class);

  /** Init performed before config is loaded */
  public void initService() {
  }

  /** Init performed after config is loaded */
  public void startService() {
    // Initialize Json-Path
    initializeJsonPath();
  }
  
  /**
   * <p>
   * Flag to note if Json-Path was initialized already.
   * </p>
   * 
   * @since 1.75.8
   * @see #initializeJsonPath()
   */
  private static boolean jsonPathInitialized = false;

  /**
   * <p>
   * Initializes Json-Path so it uses Jackson (which we have) rather than
   * json-smart (so we don't need to depend on it and its dependencies).
   * </p>
   * 
   * @since 1.75.8
   * @see <a href="https://github.com/json-path/JsonPath#jsonprovider-spi">https://github.com/json-path/JsonPath#jsonprovider-spi</a>
   */
  public static synchronized void initializeJsonPath() {
    if (!jsonPathInitialized) {
      com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {
        private final JsonProvider jsonProvider = new JacksonJsonProvider();
        private final MappingProvider mappingProvider = new JacksonMappingProvider();
        @Override
        public JsonProvider jsonProvider() {
          return jsonProvider;
        }
        @Override
        public MappingProvider mappingProvider() {
          return mappingProvider;
        }
        @Override
        public Set<Option> options() {
          return EnumSet.noneOf(Option.class);
        }
      });
      jsonPathInitialized = true;
    }
  }

}
