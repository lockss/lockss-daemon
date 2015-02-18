/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.hasher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;
import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.config.*;
import org.lockss.hasher.SimpleHasher;
import org.lockss.metadata.TestMetadataManager.MySimulatedPlugin0;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.protocol.LcapMessage;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.LockssServlet;
import org.lockss.servlet.ServletManager;
import org.lockss.test.*;
import org.lockss.util.ByteArray;
import org.lockss.util.StringUtil;
import org.lockss.ws.cxf.AuthorizationInterceptor;
import org.lockss.ws.entities.HasherWsAsynchronousResult;
import org.lockss.ws.entities.HasherWsParams;
import org.lockss.ws.entities.HasherWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * Functional test class for org.lockss.ws.hasher.HasherService.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class FuncHasherService extends LockssTestCase {
  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";
  private static final String TARGET_NAMESPACE = "http://hasher.ws.lockss.org/";
  private static final String SERVICE_NAME = "HasherServiceImplService";

  private MockLockssDaemon theDaemon;
  private String tempDirPath;
  private PluginManager pluginMgr;
  private AccountManager accountManager;
  private HasherService proxy;
  private ArchivalUnit au;

  public void setUp() throws Exception {
    super.setUp();

    tempDirPath = setUpDiskSpace();

    int port = TcpTestUtil.findUnboundTcpPort();
    ConfigurationUtil.addFromArgs(AdminServletManager.PARAM_PORT, "" + port,
	ServletManager.PARAM_PLATFORM_USERNAME, USER_NAME,
	ServletManager.PARAM_PLATFORM_PASSWORD, PASSWORD_SHA1);

    theDaemon = getMockLockssDaemon();

    accountManager = theDaemon.getAccountManager();
    accountManager.startService();

    MockIdentityManager idMgr = new MockIdentityManager();
    theDaemon.setIdentityManager(idMgr);
    idMgr.initService(theDaemon);

    pluginMgr = theDaemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getRemoteApi().startService();
    theDaemon.getServletManager().startService();
    pluginMgr.startService();

    theDaemon.setAusStarted(true);

    // The client authentication.
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
      }
    });

    String addressLocation = "http://localhost:" + port
	+ "/ws/HasherService?wsdl";

    Service service = Service.create(new URL(addressLocation), new QName(
	TARGET_NAMESPACE, SERVICE_NAME));

    proxy = service.getPort(HasherService.class);
    au = createAndStartAu();
  }

  private ArchivalUnit createAndStartAu() throws Exception {
    theDaemon.getAlertManager();
    theDaemon.getCrawlManager();

    SimulatedArchivalUnit sau = PluginTestUtil
	.createAndStartSimAu(MySimulatedPlugin0.class,
	    simAuConfig(tempDirPath + "/0"));
    PluginTestUtil.crawlSimAu(sau);

    return sau;
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
                                SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  /**
   * Tests the synchronous hashing of an Archival Unit.
   */
  public void testHash() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);

    // User "userAdminRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    HasherWsParams params = new HasherWsParams();
    params.setAuId(au.getAuId());
    HasherWsResult result = proxy.hash(params);
    assertNotNull(result.getStartTime());
    assertNull(result.getRecordFileName());
    assertNull(result.getRecordFileDataHandler());
    assertNotNull(result.getBlockFileName());
    assertNotNull(result.getBlockFileDataHandler());
    assertNull(result.getHashResult());
    assertNull(result.getErrorMessage());
    assertEquals(SimpleHasher.HasherStatus.Done.toString(), result.getStatus());
    assertEquals(3751, result.getBytesHashed().longValue());
    assertEquals(21, result.getFilesHashed().intValue());
    assertNotNull(result.getElapsedTime());

    File file = writeFile(result.getBlockFileName(),
	result.getBlockFileDataHandler(), "/tmp");
    String fileText = StringUtil.fromFile(file);
    assertTrue(fileText.contains("# Hash algorithm: "
	+ LcapMessage.DEFAULT_HASH_ALGORITHM));
    assertTrue(fileText.contains("# Encoding: "
	+ SimpleHasher.DEFAULT_RESULT_ENCODING));
    assertTrue(fileText.contains("21BDE2E4107D2BE49DE1DAD4B32335E8312858B2   http://www.example.com/index.html"));

    // Clean up the result file.
    file.delete();

    // User "debugRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_DEBUG);

    params = new HasherWsParams();
    params.setAuId(au.getAuId());
    params.setUrl("http://www.example.com/branch1/branch1/003file.pdf");
    result = proxy.hash(params);
    assertNotNull(result.getStartTime());
    assertNull(result.getRecordFileName());
    assertNull(result.getRecordFileDataHandler());
    assertNotNull(result.getBlockFileName());
    assertNotNull(result.getBlockFileDataHandler());
    assertNull(result.getHashResult());
    assertNull(result.getErrorMessage());
    assertEquals(SimpleHasher.HasherStatus.Done.toString(), result.getStatus());
    assertEquals(0, result.getBytesHashed().longValue());
    assertEquals(1, result.getFilesHashed().intValue());
    assertNotNull(result.getElapsedTime());

    file = writeFile(result.getBlockFileName(),
	result.getBlockFileDataHandler(), "/tmp");
    fileText = StringUtil.fromFile(file);
    assertTrue(fileText.contains("# Hash algorithm: "
	+ LcapMessage.DEFAULT_HASH_ALGORITHM));
    assertTrue(fileText.contains("# Encoding: "
	+ SimpleHasher.DEFAULT_RESULT_ENCODING));
    assertTrue(fileText.contains("DA39A3EE5E6B4B0D3255BFEF95601890AFD80709   http://www.example.com/branch1/branch1/003file.pdf"));

    // Clean up the result file.
    file.delete();

    // User "contentAccessRole" should succeed.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);

    params = new HasherWsParams();
    params.setAuId(au.getAuId());
    params.setResultEncoding(SimpleHasher.ResultEncoding.Base64.toString());
    result = proxy.hash(params);
    assertNotNull(result.getStartTime());
    assertNull(result.getRecordFileName());
    assertNull(result.getRecordFileDataHandler());
    assertNotNull(result.getBlockFileName());
    assertNotNull(result.getBlockFileDataHandler());
    assertNull(result.getHashResult());
    assertNull(result.getErrorMessage());
    assertEquals(SimpleHasher.HasherStatus.Done.toString(), result.getStatus());
    assertEquals(3751, result.getBytesHashed().longValue());
    assertEquals(21, result.getFilesHashed().intValue());
    assertNotNull(result.getElapsedTime());

    file = writeFile(result.getBlockFileName(),
	result.getBlockFileDataHandler(), "/tmp");
    fileText = StringUtil.fromFile(file);
    assertTrue(fileText.contains("# Hash algorithm: "
	+ LcapMessage.DEFAULT_HASH_ALGORITHM));
    assertTrue(fileText.contains("# Encoding: "
	+ SimpleHasher.ResultEncoding.Base64));
    assertTrue(fileText.contains("2jmj7l5rSw0yVb/vlWAYkK/YBwk=   http://www.example.com/branch1/branch1/003file.pdf"));

    // Clean up the result file.
    file.delete();

    // Once more with an AU that can't be configured
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);

    params = new HasherWsParams();
    String badAuId = "deliberately" + au.getAuId() + "bad";
    params.setAuId(badAuId);
    result = proxy.hash(params);
    assertNotNull(result.getStartTime());
    assertNull(result.getRecordFileName());
    assertNull(result.getRecordFileDataHandler());
    assertNull(result.getBlockFileName());
    assertNull(result.getBlockFileDataHandler());
    assertNull(result.getHashResult());
    assertEquals("No AU exists with the specified identifier " + badAuId,
	result.getErrorMessage());
    assertEquals(SimpleHasher.HasherStatus.Error.toString(),
	result.getStatus());
    assertEquals(0, result.getBytesHashed().longValue());
    assertEquals(0, result.getFilesHashed().intValue());
    assertNotNull(result.getElapsedTime());

    // User "contentAdminRole" should fail.
    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    try {
      params = new HasherWsParams();
      params.setAuId(au.getAuId());
      result = proxy.hash(params);
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
      params = new HasherWsParams();
      params.setAuId(au.getAuId());
      result = proxy.hash(params);
      fail("Test should have failed for role " + LockssServlet.ROLE_AU_ADMIN);
    } catch (LockssWebServicesFault lwsf) {
      // Expected authorization failure.
      assertEquals(AuthorizationInterceptor.NO_REQUIRED_ROLE,
	  lwsf.getMessage());
    }
  }

  private File writeFile(String fileName, DataHandler dataHandler,
      String dirName) throws IOException {
    File file = new File(dirName, fileName);

    // Write the received file.
    InputStream dhis = null;
    FileOutputStream fos = null;
    byte[] buffer = new byte[1024 * 1024];
    int bytesRead = 0;

    try {
      dhis = dataHandler.getInputStream();
      fos = new FileOutputStream(file);

      while ((bytesRead = dhis.read(buffer)) != -1) {
	fos.write(buffer, 0, bytesRead);
      }
    } finally {
      if (dhis != null) {
	try {
	  dhis.close();
	} catch (IOException ioe) {
	  // Ignore.
	}
      }

      if (fos != null) {
	fos.flush();
	fos.close();
      }
    }

    return file;
  }

  /**
   * Tests the asynchronous hashing workflow.
   */
  public void testAsynchronousHashWorkflow() throws Exception {
    UserAccount userAccount = accountManager.getUser(USER_NAME);
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);

    // No stored asynchronous hash results to begin with.
    List<HasherWsAsynchronousResult> results =
	proxy.getAllAsynchronousHashResults();

    assertNull(results);
    int storedResultsCount = 0;

    // Schedule an asynchronous hash.
    HasherWsParams params = new HasherWsParams();
    params.setAuId(au.getAuId());
    params.setUrl("http://www.example.com/branch1/branch1/003file.pdf");
    params.setHashType(SimpleHasher.HashType.V3File.toString());
    params.setRecordFilteredStream(true);
    HasherWsAsynchronousResult result = proxy.hashAsynchronously(params);
    assertNotNull(result.getRequestTime());
    String requestId = result.getRequestId();
    assertNotNull(requestId);
    assertNull(result.getStartTime());
    assertNull(result.getRecordFileName());
    assertNull(result.getRecordFileDataHandler());
    assertNull(result.getBlockFileName());
    assertNull(result.getBlockFileDataHandler());
    assertNull(result.getHashResult());
    assertNull(result.getErrorMessage());
    String status = result.getStatus();
    assertNull(status);
    assertNull(result.getBytesHashed());
    assertNull(result.getFilesHashed());
    assertNull(result.getElapsedTime());

    // Wait for the asynchronous hash to complete.
    result = waitForCompletion(requestId);
    assertNotNull(result.getStartTime());
    assertNull(result.getRecordFileName());
    assertNull(result.getRecordFileDataHandler());
    assertNotNull(result.getBlockFileName());
    assertNotNull(result.getBlockFileDataHandler());
    assertNull(result.getHashResult());
    assertNull(result.getErrorMessage());
    assertEquals(SimpleHasher.HasherStatus.Done.toString(), result.getStatus());
    assertEquals(0, result.getBytesHashed().longValue());
    assertEquals(1, result.getFilesHashed().intValue());
    assertNotNull(result.getElapsedTime());

    File file = writeFile(result.getBlockFileName(),
	result.getBlockFileDataHandler(), "/tmp");
    String fileText = StringUtil.fromFile(file);
    assertTrue(fileText.contains("# Hash algorithm: "
	+ LcapMessage.DEFAULT_HASH_ALGORITHM));
    assertTrue(fileText.contains("# Encoding: "
	+ SimpleHasher.DEFAULT_RESULT_ENCODING));
    assertTrue(fileText.contains("DA39A3EE5E6B4B0D3255BFEF95601890AFD80709   http://www.example.com/branch1/branch1/003file.pdf"));

    // Clean up the result file.
    file.delete();

    results = proxy.getAllAsynchronousHashResults();
    assertEquals(++storedResultsCount, results.size());

    // Schedule another asynchronous hash.
    params = new HasherWsParams();
    params.setAuId(au.getAuId());
    params.setHashType(SimpleHasher.HashType.V1File.toString());
    params.setAlgorithm("SHA-256");
    result = proxy.hashAsynchronously(params);
    assertNotNull(result.getRequestTime());
    requestId = result.getRequestId();
    assertNotNull(requestId);
    assertNull(result.getStartTime());
    assertNull(result.getRecordFileName());
    assertNull(result.getRecordFileDataHandler());
    assertNull(result.getBlockFileName());
    assertNull(result.getBlockFileDataHandler());
    assertNull(result.getHashResult());
    assertNull(result.getErrorMessage());
    status = result.getStatus();
    assertNull(status);
    assertNull(result.getBytesHashed());
    assertNull(result.getFilesHashed());
    assertNull(result.getElapsedTime());

    // Wait for the asynchronous hash to complete.
    result = waitForCompletion(requestId);
    assertNotNull(result.getStartTime());
    assertNull(result.getRecordFileName());
    assertNull(result.getRecordFileDataHandler());
    assertNull(result.getBlockFileName());
    assertNull(result.getBlockFileDataHandler());
    assertEquals("4D5BA4B4AD376937A9EB987C1636E7C5175D3CE266FDEBE433BF319771B7F0C9",
	ByteArray.toHexString(result.getHashResult()));
    assertNull(result.getErrorMessage());
    assertEquals(SimpleHasher.HasherStatus.Done.toString(), result.getStatus());
    assertEquals(4796, result.getBytesHashed().longValue());
    assertEquals(0, result.getFilesHashed().intValue());
    assertNotNull(result.getElapsedTime());

    results = proxy.getAllAsynchronousHashResults();
    assertEquals(++storedResultsCount, results.size());

    for (HasherWsAsynchronousResult storedResult : results) {
      HasherWsAsynchronousResult removalResult =
	  proxy.removeAsynchronousHashRequest(storedResult.getRequestId());
      assertNotNull(removalResult.getRequestId());
      assertNull(removalResult.getStartTime());
      assertNull(removalResult.getRecordFileName());
      assertNull(removalResult.getRecordFileDataHandler());
      assertNull(removalResult.getBlockFileName());
      assertNull(removalResult.getBlockFileDataHandler());
      assertNull(removalResult.getHashResult());
      assertNull(removalResult.getErrorMessage());
      assertEquals(SimpleHasher.HasherStatus.Done.toString(),
	  removalResult.getStatus());
      assertNull(removalResult.getBytesHashed());
      assertNull(removalResult.getFilesHashed());
      assertNull(removalResult.getElapsedTime());

      List<HasherWsAsynchronousResult> storedResults =
	  proxy.getAllAsynchronousHashResults();

      if (--storedResultsCount > 0) {
	assertEquals(storedResultsCount, storedResults.size());
      } else {
	assertNull(storedResults);
      }
    }
  }

  private HasherWsAsynchronousResult waitForCompletion(String requestId)
      throws Exception {
    while (true) {
      HasherWsAsynchronousResult result =
	  proxy.getAsynchronousHashResult(requestId);
      String status = result.getStatus();

      if (SimpleHasher.HasherStatus.Done.toString().equals(status)
	  || SimpleHasher.HasherStatus.Error.toString().equals(status)
	  || SimpleHasher.HasherStatus.RequestError.toString().equals(status)) {
	return result;
      }
    }
  }
}
