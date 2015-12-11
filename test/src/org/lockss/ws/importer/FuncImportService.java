/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.plugin.PluginManager;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.LockssServlet;
import org.lockss.servlet.ServletManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.TcpTestUtil;
import org.lockss.ws.cxf.AuthorizationInterceptor;
import org.lockss.ws.entities.ImportWsParams;
import org.lockss.ws.entities.ImportWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * Functional test class for org.lockss.ws.importer.ImportService.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class FuncImportService extends LockssTestCase {
  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";
  private static final String TARGET_NAMESPACE =
      "http://importer.ws.lockss.org/";
  private static final String SERVICE_NAME = "ImportServiceImplService";

  private MockLockssDaemon theDaemon;
  private String tempDirPath;
  private PluginManager pluginMgr;
  private AccountManager accountManager;
  private ImportService proxy;

  public void setUp() throws Exception {
    super.setUp();

    tempDirPath = setUpDiskSpace();

    int port = TcpTestUtil.findUnboundTcpPort();
    ConfigurationUtil.addFromArgs(AdminServletManager.PARAM_PORT, "" + port,
	ServletManager.PARAM_PLATFORM_USERNAME, USER_NAME,
	ServletManager.PARAM_PLATFORM_PASSWORD, PASSWORD_SHA1);

    theDaemon = getMockLockssDaemon();
    theDaemon.suppressStartAuManagers(false);
    accountManager = theDaemon.getAccountManager();
    pluginMgr = theDaemon.getPluginManager();
    theDaemon.getServletManager().startService();
    pluginMgr.startService();

    // The client authentication.
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
      }
    });

    String addressLocation = "http://localhost:" + port
	+ "/ws/ImportService?wsdl";

    Service service = Service.create(new URL(addressLocation), new QName(
	TARGET_NAMESPACE, SERVICE_NAME));

    proxy = service.getPort(ImportService.class);
  }

  /**
   * Tests the importing of a pulled file.
   */
  public void testImportPulledFile() throws Exception {
    File importFile = writeTextToFile("1234567890", "fileToImport", "/tmp");
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    // Import to an unnamed AU.
    ImportWsParams params = new ImportWsParams();
    params.setSourceUrl(importFile.toURI().toURL().toString());
    params.setTargetId("targetId");
    String targetUrl = "file://dummy.host" + importFile.getAbsolutePath();
    params.setTargetUrl(targetUrl);

    ImportWsResult result = proxy.importPulledFile(params);
    assertEquals(Boolean.TRUE, result.getIsSuccess());
    assertEquals("Imported", result.getMessage());
    assertEquals(10, new File(tempDirPath + "/cache/a/dummy.host/file"
	+ importFile.getAbsolutePath() + "/#content/current").length());

    // User "contentAccessRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);

    // Import to another unnamed AU.
    params.setTargetId(params.getTargetId() + "1");

    result = proxy.importPulledFile(params);
    assertEquals(Boolean.TRUE, result.getIsSuccess());
    assertEquals("Imported", result.getMessage());
    assertEquals(10, new File(tempDirPath + "/cache/b/dummy.host/file"
	+ importFile.getAbsolutePath() + "/#content/current").length());
    String auId = ImportServiceImpl.makeAuId(params.getTargetId());
    assertTrue(pluginMgr.getAuFromId(auId).getName().startsWith("Import AU"));

    // Import to an AU with a provided name.
    params.setTargetId(params.getTargetId() + "2");
    String[] properties = new String[1];
    properties[0] = "reserved.displayName=Test of custom AU name";
    params.setProperties(properties);

    result = proxy.importPulledFile(params);
    assertEquals(Boolean.TRUE, result.getIsSuccess());
    assertEquals("Imported", result.getMessage());
    assertEquals(10, new File(tempDirPath + "/cache/c/dummy.host/file"
	+ importFile.getAbsolutePath() + "/#content/current").length());
    auId = ImportServiceImpl.makeAuId(params.getTargetId());
    assertEquals("Test of custom AU name",
	pluginMgr.getAuFromId(auId).getName());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);

    try {
      result = proxy.importPulledFile(params);
      fail("Test should have failed for role "
	  + LockssServlet.ROLE_CONTENT_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "auAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);
    try {
      result = proxy.importPulledFile(params);
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // User "debugRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    try {
      result = proxy.importPulledFile(params);
      fail("Test should have failed for role " + LockssServlet.ROLE_DEBUG);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }

    // Clean up the import file.
    importFile.delete();
  }

  /**
   * Tests the importing of a pushed file.
   */
  public void testImportPushedFile() throws Exception {
    File importFile = writeTextToFile("1234567890", "fileToImport", "/tmp");
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "contentAccessRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);

    // Import to an unnamed AU.
    ImportWsParams params = new ImportWsParams();
    params.setDataHandler(new DataHandler(new FileDataSource(importFile)));
    params.setTargetId("targetId");
    String targetUrl = "file://dummy.host" + importFile.getAbsolutePath();
    params.setTargetUrl(targetUrl);

    ImportWsResult result = proxy.importPushedFile(params);
    assertEquals(Boolean.TRUE, result.getIsSuccess());
    assertEquals("Imported", result.getMessage());
    assertEquals(10, new File(tempDirPath + "/cache/a/dummy.host/file"
	+ importFile.getAbsolutePath() + "/#content/current").length());

    // Import with a valid MD5 checksum.
    params.setTargetId(params.getTargetId() + "9");
    String[] properties = new String[1];
    properties[0] = ImportServiceImpl.CHECKSUM_KEY
	+ "=MD5:E807F1FCF82D132F9BB018CA6738A19F";
    params.setProperties(properties);

    result = proxy.importPushedFile(params);
    assertEquals(Boolean.TRUE, result.getIsSuccess());
    assertEquals("Imported", result.getMessage());
    assertEquals(10, new File(tempDirPath + "/cache/b/dummy.host/file"
	+ importFile.getAbsolutePath() + "/#content/current").length());

    // Import with an invalid MD5 checksum.
    properties[0] = ImportServiceImpl.CHECKSUM_KEY
	+ "=MD5:E907F1FCF82D132F9BB018CA6738A19F";
    params.setProperties(properties);

    result = proxy.importPushedFile(params);
    assertEquals(Boolean.FALSE, result.getIsSuccess());
    assertTrue(result.getMessage().startsWith("Checksum error"));

    // Import with an invalid checksum algorithm.
    properties[0] = ImportServiceImpl.CHECKSUM_KEY
	+ "=invalid:E907F1FCF82D132F9BB018CA6738A19F";
    params.setProperties(properties);

    result = proxy.importPushedFile(params);
    assertEquals(Boolean.FALSE, result.getIsSuccess());
    assertTrue(result.getMessage()
	.startsWith("Unsupported checksum algorithm"));

    // Import with a valid SHA-1 checksum.
    params.setTargetId(params.getTargetId() + "8");
    properties[0] = ImportServiceImpl.CHECKSUM_KEY
	+ "=SHA-1:01B307ACBA4F54F55AAFC33BB06BBBF6CA803E9A";
    params.setProperties(properties);

    result = proxy.importPushedFile(params);
    assertEquals(Boolean.TRUE, result.getIsSuccess());
    assertEquals("Imported", result.getMessage());
    assertEquals(10, new File(tempDirPath + "/cache/c/dummy.host/file"
	+ importFile.getAbsolutePath() + "/#content/current").length());

    // Import with a valid SHA-256 checksum.
    params.setTargetId(params.getTargetId() + "7");
    properties = new String[2];
    properties[0] = ImportServiceImpl.CHECKSUM_KEY
	+ "=SHA-256:C775E7B757EDE630CD0AA1113BD102661AB38829CA52A6422AB782862F268646";
    properties[1] = "reserved.displayName=Test of SHA-256";
    params.setProperties(properties);

    result = proxy.importPushedFile(params);
    assertEquals(Boolean.TRUE, result.getIsSuccess());
    assertEquals("Imported", result.getMessage());
    assertEquals(10, new File(tempDirPath + "/cache/d/dummy.host/file"
	+ importFile.getAbsolutePath() + "/#content/current").length());
    String auId = ImportServiceImpl.makeAuId(params.getTargetId());
    assertEquals("Test of SHA-256", pluginMgr.getAuFromId(auId).getName());

    // Clean up the import file.
    importFile.delete();
  }

  /**
   * Tests the retrieval of the names of the supported checksum algorithms.
   */
  public void testSupportedChecksumAlgorithms() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    // The 3 algorithms that Oracle claims that any JVM must support.
    boolean hasMd5 = false;
    boolean hasSha1 = false;
    boolean hasSha256 = false;

    // Loop through all the algorithms supported by this JVM.
    for (String name : proxy.getSupportedChecksumAlgorithms()) {
      if ("MD5".equals(name)) {
	hasMd5 = true;
      } else if ("SHA-1".equals(name)) {
	hasSha1 = true;
      } else if ("SHA-256".equals(name)) {
	hasSha256 = true;
      }
    }

    // Check that no expected algorithm is missing.
    if (!hasMd5) {
      fail("Missing 'MD5' among the expected supported algorithms");
    }

    if (!hasSha1) {
      fail("Missing 'SHA-1' among the expected supported algorithms");
    }

    if (!hasSha256) {
      fail("Missing 'SHA-256' among the expected supported algorithms");
    }
  }

  private File writeTextToFile(String text, String fileName, String dirName)
      throws IOException {
    File file = new File(dirName, fileName);
    byte[] buffer = text.getBytes();

    // Write the received file.
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(file);
      fos.write(buffer, 0, buffer.length);
    } finally {
      if (fos != null) {
	fos.flush();
	fos.close();
      }
    }

    return file;
  }
}
