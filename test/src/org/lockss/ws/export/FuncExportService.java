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
package org.lockss.ws.export;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.account.AccountManager;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.servlet.AdminServletManager;
import org.lockss.servlet.ServletManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockAuState;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.NoCrawlEndActionsFollowLinkCrawler;
import org.lockss.test.TcpTestUtil;
import org.lockss.ws.entities.ExportServiceParams;
import org.lockss.ws.entities.ExportServiceParams.TypeEnum;
import org.lockss.ws.entities.ExportServiceWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/*
 * @author Ahmed AlSum
 */
public class FuncExportService extends LockssTestCase {
  private static final String USER_NAME = "lockss-u";
  private static final String PASSWORD = "lockss-p";
  private static final String PASSWORD_SHA1 =
      "SHA1:ac4fc8fa9930a24c8d002d541c37ca993e1bc40f";
  private static final String TARGET_NAMESPACE = "http://export.ws.lockss.org/";
  private static final String SERVICE_NAME = "ExportServiceImplService";

  private String tempDirPath;
  private PluginManager pluginMgr;
  private AccountManager accountManager;
  private ExportService proxy;
  private SimulatedArchivalUnit sau;

  protected void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();

    int port = TcpTestUtil.findUnboundTcpPort();
    ConfigurationUtil.addFromArgs(AdminServletManager.PARAM_PORT, "" + port,
	ServletManager.PARAM_PLATFORM_USERNAME, USER_NAME,
	ServletManager.PARAM_PLATFORM_PASSWORD, PASSWORD_SHA1);

    MockLockssDaemon theDaemon = getMockLockssDaemon();

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

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    sau.generateContentTree();

    Crawler crawler = new NoCrawlEndActionsFollowLinkCrawler(sau,
	new MockAuState());
    crawler.doCrawl();

    theDaemon.setAusStarted(true);

    // The client authentication.
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	return new PasswordAuthentication(USER_NAME, PASSWORD.toCharArray());
      }
    });

    String addressLocation = "http://localhost:" + port
	+ "/ws/ExportService?wsdl";

    Service service = Service.create(new URL(addressLocation), new QName(
	TARGET_NAMESPACE, SERVICE_NAME));

    proxy = service.getPort(ExportService.class);
  }

  public void testCreateCompressedFile() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setCompress(true);
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertTrue(result.getDataHandlerWrappers()[0].getName().endsWith("gz"));
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateNotCompressedFile() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setCompress(false);
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertFalse(result.getDataHandlerWrappers()[0].getName().endsWith("gz"));
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateWARCNotCompressedFile() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setCompress(false);
      exportParam.setFileType(TypeEnum.WARC_RESPONSE);
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      log.info("result = " + result);
      log.info("result.getDataHandlerWrappers() = " + result.getDataHandlerWrappers());
      assertTrue(result.getDataHandlerWrappers()[0].getName()
	  .endsWith(".warc"));
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateWARCCompressedFile() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setCompress(true);
      exportParam.setFileType(TypeEnum.WARC_RESPONSE);
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertTrue(result.getDataHandlerWrappers()[0].getName().endsWith(
	  ".warc.gz"));
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateARCNotCompressedFile() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setCompress(false);
      exportParam.setFileType(TypeEnum.ARC_RESPONSE);
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertTrue(result.getDataHandlerWrappers()[0].getName().endsWith(".arc"));
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateARCCompressedFile() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setCompress(true);
      exportParam.setFileType(TypeEnum.ARC_RESPONSE);
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertTrue(result.getDataHandlerWrappers()[0].getName().endsWith(
	  ".arc.gz"));
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateZipFile() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setCompress(true);
      exportParam.setFileType(TypeEnum.ZIP);
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertTrue(result.getDataHandlerWrappers()[0].getName().endsWith(".zip"));
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateFileWithNoMaxSize() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertEquals(result.getDataHandlerWrappers().length, 1);
      assertTrue(result.getDataHandlerWrappers()[0].getSize()
	  > 2 * 1024 * 1024);
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateFileWithMaxSize1MB() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setMaxSize(1); // Max size is 1MB
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertEquals(result.getDataHandlerWrappers().length, 3);
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  public void testCreateFileWithMaxSize2MB() {
    ExportServiceParams exportParam = initializeExportParams();
    try {
      exportParam.setMaxSize(2); // Max size is 2MB
      ExportServiceWsResult result = proxy.createExportFiles(exportParam);
      assertEquals(result.getDataHandlerWrappers().length, 2);
    } catch (LockssWebServicesFault e) {
      fail(e.getMessage());
    }
  }

  private ExportServiceParams initializeExportParams() {
    String auId = sau.getAuId();
    ExportServiceParams exportParam = new ExportServiceParams();
    exportParam.setAuid(auId);
    return exportParam;
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "4");
    conf.put("branch", "3");
    conf.put("numFiles", "50");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_HTML);
    conf.put(SimulatedPlugin.AU_PARAM_ODD_BRANCH_CONTENT, "true");
    return conf;
  }
}
