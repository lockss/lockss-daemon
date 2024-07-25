/*

Copyright (c) 2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.laaws;

import java.io.*;
import org.lockss.config.*;
import org.lockss.servlet.MigrateSettings;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestConfigFileMover extends LockssTestCase {
  static Logger log = Logger.getLogger("TestConfigFileMover");

  String v1ContentServers =
    "org.lockss.config.fileVersion.content_servers_config=1\n" +
    "org.lockss.contentui.port=8088\n" +
    "org.lockss.contentui.start=true\n" +
    "org.lockss.icp.enabled=false\n" +
    "org.lockss.proxy.audit.port=8082\n" +
    "org.lockss.proxy.audit.start=true\n" +
    "org.lockss.proxy.port=8089\n" +
    "org.lockss.proxy.sslPort=8887\n" +
    "org.lockss.proxy.start=true\n";

  String v2ContentServers =
    "org.lockss.config.fileVersion.content_servers_config=1\n" +
    "org.lockss.contentui.port=24680\n" +
    "org.lockss.contentui.start=true\n" +
    "org.lockss.icp.enabled=false\n" +
    "org.lockss.proxy.audit.port=24672\n" +
    "org.lockss.proxy.audit.start=true\n" +
    "org.lockss.proxy.port=24670\n" +
    "org.lockss.proxy.sslPort=24671\n" +
    "org.lockss.proxy.start=true\n";

  public void testTransformContentServers() throws IOException {
    Configuration c1 = ConfigurationUtil.fromString(v1ContentServers);
    Configuration c2 = ConfigFileMover.transformContentServers("foo", c1);
    log.debug("c2: " + c2);
    Configuration exp = ConfigurationUtil.fromString(v2ContentServers);
    assertEquals(exp, c2);
  }
}
