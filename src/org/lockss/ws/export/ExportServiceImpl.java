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

import java.io.File;
import java.net.MalformedURLException;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.exporter.Exporter;
import org.lockss.exporter.Exporter.FilenameTranslation;
import org.lockss.exporter.Exporter.Type;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.util.FileUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.DataHandlerWrapper;
import org.lockss.ws.entities.ExportServiceParams;
import org.lockss.ws.entities.ExportServiceParams.FilenameTranslationEnum;
import org.lockss.ws.entities.ExportServiceParams.TypeEnum;
import org.lockss.ws.entities.ExportServiceWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/*
 * @author Ahmed AlSum
 */
public class ExportServiceImpl implements ExportService {
  private LockssDaemon daemon;
  private PluginManager pluginMgr;
  private Configuration config;

  static Logger log = Logger.getLogger(ExportServiceImpl.class);

  static final String PREFIX = Configuration.PREFIX + "export.";
  static final String DEFAULT_EXPORT_DIR = "export";
  static final String PARAM_EXPORT_PATH = PREFIX + "directory";

  public ExportServiceImpl() {
    super();
    daemon = LockssDaemon.getLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    config = ConfigManager.getCurrentConfig();
  }

  @Override
  public ExportServiceWsResult createExportFiles(ExportServiceParams exportParam)
      throws LockssWebServicesFault {
    ExportServiceWsResult results = null;
    ArchivalUnit au = getAu(exportParam.getAuid());
    try {
      Exporter exp = exportFiles(exportParam, au);
      results = generateResults(exp, exportParam);
    } catch (Exception e) {
      e.printStackTrace();
      throw new LockssWebServicesFault(e.getCause());
    }
    return results;
  }

  private ExportServiceWsResult generateResults(Exporter exp,
      ExportServiceParams exportParam) throws MalformedURLException {
    int n = exp.getExportFiles().size();
    log.info("n = " + n);
    DataHandlerWrapper[] dataHandlerWrapperArray = new DataHandlerWrapper[n];
    for (int i = 0; i < n; i++) {
      FileDataSource fileDS = new FileDataSource(exp.getExportFiles().get(i));
      DataHandler dataHandler = new DataHandler(fileDS);
      long size = fileDS.getFile().length();
      DataHandlerWrapper dataHandlerWrapper = new DataHandlerWrapper();
      dataHandlerWrapper.setDataHandler(dataHandler);
      dataHandlerWrapper.setSize(size);
      dataHandlerWrapper.setName(fileDS.getName());
      dataHandlerWrapperArray[i] = dataHandlerWrapper;
    }
    ExportServiceWsResult result = new ExportServiceWsResult();
    result.setDataHandlerWrappers(dataHandlerWrapperArray);
    result.setAuId(exportParam.getAuid());
    return result;
  }

  private Exporter exportFiles(ExportServiceParams exportParam, ArchivalUnit au)
      throws LockssWebServicesFault {
    long size = (long) (exportParam.getMaxSize() * 1024 * 1024);

    Exporter exp = generateExporter(exportParam.getFileType(), au);
    exp.setCompress(exportParam.isCompress());
    exp.setExcludeDirNodes(exportParam.isExcludeDirNodes());
    exp.setFilenameTranslation(generateFilenameTranslation(exportParam
	.getXlateFilenames()));
    exp.setDir(getExportDir());
    exp.setPrefix(exportParam.getFilePrefix());
    if (size > 0) {
      exp.setMaxSize(size);
    }
    if (exportParam.getMaxVersions() > 0) {
      exp.setMaxVersions(exportParam.getMaxVersions());
    }
    exp.export();
    return exp;
  }

  private FilenameTranslation generateFilenameTranslation(
      FilenameTranslationEnum xlateFilenames) {
    switch (xlateFilenames) {
    case XLATE_MAC:
      return FilenameTranslation.XLATE_MAC;
    case XLATE_WINDOWS:
      return FilenameTranslation.XLATE_WINDOWS;
    case XLATE_NONE:
      return FilenameTranslation.XLATE_NONE;
    }
    return null;
  }

  private Exporter generateExporter(TypeEnum fileType, ArchivalUnit au) {
    switch (fileType) {
    case WARC_RESPONSE:
      return Type.WARC_RESPONSE.makeExporter(daemon, au);
    case ARC_RESPONSE:
      return Type.ARC_RESPONSE.makeExporter(daemon, au);
    case WARC_RESOURCE:
      return Type.WARC_RESOURCE.makeExporter(daemon, au);
    case ARC_RESOURCE:
      return Type.ARC_RESOURCE.makeExporter(daemon, au);
    case ZIP:
      return Type.ZIP.makeExporter(daemon, au);
    }
    return null;
  }

  private File getExportDir() throws LockssWebServicesFault {
    File exportdir = null;
    String path = config.get(PARAM_EXPORT_PATH);

    if (StringUtil.isNullString(path)) {
      String tmpdir = config.get(ConfigManager.PARAM_TMPDIR);
      exportdir = new File(tmpdir, DEFAULT_EXPORT_DIR);
    } else {
      exportdir = new File(path);
    }
    if (!exportdir.exists()) {
      if (!FileUtil.ensureDirExists(exportdir)) {
	throw new LockssWebServicesFault("Could not create export directory "
	    + exportdir);
      }
    }
    return exportdir;
  }

  private ArchivalUnit getAu(String auid) throws LockssWebServicesFault {
    if (StringUtil.isNullString(auid)) {
      throw new LockssWebServicesFault("auid parameter is null");
    }
    ArchivalUnit au = pluginMgr.getAuFromId(auid);
    if (au == null) {
      throw new LockssWebServicesFault("auid " + auid + " can't be found");
    }
    return au;
  }
}
