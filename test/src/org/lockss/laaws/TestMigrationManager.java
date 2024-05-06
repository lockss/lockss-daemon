/*

Copyright (c) 2000-2024 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.laaws;

import org.lockss.servlet.MigrateSettings;
import org.lockss.test.LockssTestCase;
import org.lockss.util.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TestMigrationManager extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestMigrationManager");

  private MigrationManager migrationMgr;

  public void setUp() throws Exception {
    super.setUp();
    migrationMgr = getMockLockssDaemon().getMigrationManager();
  }

  public void testMapFromCsvStream() throws Exception {
    final String CSV = "Name,Value\n" +
        "org.lockss.contentui.port,24680\n" +
        "org.lockss.contentui.start,true\n" +
        "org.lockss.proxy.port,24670\n" +
        "org.lockss.proxy.start,true\n" +
        "org.lockss.localV3Identity,TCP:[127.0.0.1]:9729\n" +
        "org.lockss.metadataDbManager.datasource.className,org.apache.derby.jdbc.ClientDataSource\n" +
        "org.lockss.metadataDbManager.datasource.dbcp.enabled,true\n" +
        "org.lockss.metadataDbManager.datasource.portNumber,1527\n";

    InputStream csvStream = new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8));

    Properties csvMap = migrationMgr.propsFromCsv(csvStream);

    assertEquals("24680", csvMap.get("org.lockss.contentui.port"));
    assertEquals("true", csvMap.get("org.lockss.contentui.start"));
    assertEquals("24670", csvMap.get("org.lockss.proxy.port"));
    assertEquals("true", csvMap.get("org.lockss.proxy.start"));
    assertEquals("TCP:[127.0.0.1]:9729", csvMap.get("org.lockss.localV3Identity"));
    assertEquals("org.apache.derby.jdbc.ClientDataSource",
        csvMap.get("org.lockss.metadataDbManager.datasource.className"));
    assertEquals("true", csvMap.get("org.lockss.metadataDbManager.datasource.dbcp.enabled"));
    assertEquals("1527", csvMap.get("org.lockss.metadataDbManager.datasource.portNumber"));
  }

}
