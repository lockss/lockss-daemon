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

import org.lockss.test.LockssTestCase;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * <p>
 * Unit tests related to {@link MiscSetupManager}.
 * </p>
 *
 * @since 1.75.8
 * @see MiscSetupManager
 */
public class TestMiscSetupManager extends LockssTestCase {

  /**
   * <p>
   * Tests {@link MiscSetupManager#initializeJsonPath()}.
   * </p>
   * 
   * @throws Exception
   *           if an exception occurs
   * @since 1.75.8
   * @see MiscSetupManager#initializeJsonPath()
   */
  public void testInitializeJsonPath() throws Exception {
    com.jayway.jsonpath.Configuration jsonPathConfig = null;
    try {
      jsonPathConfig = com.jayway.jsonpath.Configuration.defaultConfiguration();
      // Option 1: the default is json-smart
      assertEquals("com.jayway.jsonpath.spi.json.JsonSmartJsonProvider",
                   jsonPathConfig.jsonProvider().getClass().getName());
      assertEquals("com.jayway.jsonpath.spi.mapper.JsonSmartMappingProvider",
                   jsonPathConfig.mappingProvider().getClass().getName());
      assertEquals(0, jsonPathConfig.getOptions().size());
    }
    catch (NoClassDefFoundError ncdfe) {
      // Option 2: NoClassDefFoundError (json-smart is not on the classpath)
      assertTrue("Expected NoClassDefFoundError on net.minidev.json.writer.JsonReaderI but got: " + ncdfe.getMessage(),
                 ncdfe.getMessage().endsWith("net/minidev/json/writer/JsonReaderI"));
    }
    MiscSetupManager.initializeJsonPath();
    jsonPathConfig = Configuration.defaultConfiguration();
    assertTrue(jsonPathConfig.jsonProvider() instanceof JacksonJsonProvider);
    assertTrue(jsonPathConfig.mappingProvider() instanceof JacksonMappingProvider);
    assertEquals(0, jsonPathConfig.getOptions().size());
  }
  
}
