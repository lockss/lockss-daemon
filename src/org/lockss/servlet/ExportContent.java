/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.text.*;
import org.mortbay.html.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.exporter.*;
import org.lockss.exporter.Exporter.Type;
import org.lockss.exporter.Exporter.FilenameTranslation;

/** 
 */
public class ExportContent extends LockssServlet {
  static Logger log = Logger.getLogger("ExportContent");

  static final String PREFIX = Configuration.PREFIX + "export.";

  /** Enable AU export from UI.  Daemon restart required when set to true,
   * not when set false */
  public static final String PARAM_ENABLE_EXPORT = PREFIX + "enabled";
  public static final boolean DEFAULT_ENABLE_EXPORT = false;

  /** Output directory for export files.  Defaults to
   * <code><i>daemon_tmpdir</i>/export</code> .  Changes require daemon
   * restart */
  static final String PARAM_EXPORT_PATH = PREFIX + "directory";

  // paramdoc only
  static final String DEFAULT_EXPORT_PATH = "<tmpdir>/export";

  static final String DEFAULT_EXPORT_DIR = "export";

  /** Default export file type */
  static final String PARAM_EXPORT_TYPE = PREFIX + "defaultType";
  static final Type DEFAULT_EXPORT_TYPE = Type.WARC_RESOURCE;

  /** Default export file max size */
  static final String PARAM_MAX_SIZE = PREFIX + "defaultMaxSize";
  static final String DEFAULT_MAX_SIZE = "";

  /** Default max number of versions of each content file to export */
  static final String PARAM_MAX_VERSIONS = PREFIX + "defaultMaxVersions";
  static final String DEFAULT_MAX_VERSIONS = "1";

  /** Default compression of export files */
  static final String PARAM_COMPRESS = PREFIX + "defaultCompress";
  static final boolean DEFAULT_COMPRESS = true;

  /** Default excludeDirNodes. */
  static final String PARAM_EXCLUDE_DIR_NODES =
    PREFIX + "defaultExcludeDirNodes";
  static final boolean DEFAULT_EXCLUDE_DIR_NODES = false;


  static final String KEY_ACTION = "action";
  static final String KEY_MSG = "msg";
  static final String KEY_AUID = "auid";
  static final String KEY_COMPRESS = "compress";
  static final String KEY_EXCLUDE_DIR_NODES = "excludeDirNodes";
  static final String KEY_XLATE = "xlate";
  static final String KEY_FILE_TYPE = "filetype";
  static final String KEY_FILE_PREFIX = "filePrefix";
  static final String KEY_MAX_SIZE = "maxSize";
  static final String KEY_MAX_VERSIONS = "maxVersions";

  static final String ACTION_EXPORT = "Create Export File(s)";
  static final String ACTION_DELETE = "Delete Export Files";

  static final String COL2 = "colspan=2";
  static final String COL2CENTER = COL2 + " align=center";


  private static final String FILE_TYPE_FOOT =
    "Select the type of archive file(s) to create.  Each record in an ARC or WARC file may contain either a content file, or a complete HTTP response: headers and content.  For WARC, these are written into <i>resource</i> or <i>response</i> records, respectively.  ZIP records contain content only; the HTTP headers are stored as the comment in each record.";

  private static final String EXCLUDE_DIR_FOOT =
    "If checked, files that have the same name as a directory will not be included in the archive file(s).  Useful for AUs that were collected from a directory hierarchy, where the added directory indices would cause conflicts when unpacking the archive.";

  private static final String FILE_PREFIX_FOOT =
    "One or more files will be written, with generated names using this string as a prefix.";

  private static final String MAX_SIZE_FOOT =
    "The approximate maximum size for each ARC/WARC/ZIP archive file.  If the total output is larger, multiple files will be written.";

  private static final String MAX_VER_FOOT =
    "The maximum number of versions included, for content files that have older versions.  (ARC and WARC only).";

  private static final String XLATE_FOOT =
    "If the exported file will be unpacked on Windows or MacOS, this option must be used to ensure that filenames don't contain illegal characters.  All characters that are legal in URLs but not in filenames will be replaced with underscore.";

  private LockssDaemon daemon;
  private PluginManager pluginMgr;
  private ConfigManager cfgMgr;

  File exportDir;

  String auid;
  Type eType;
  boolean isCompress;
  boolean isExcludeDirNodes;
  FilenameTranslation xlateFilenames;
  String filePrefix;
  String maxSize;
  String maxVersions;

  protected void resetLocals() {
    resetVars();
    super.resetLocals();
  }

  void resetVars() {
    errMsg = null;
    statusMsg = null;
    auid = null;
    filePrefix = null;
    maxVersions = null;

  }

  void getConfig() {
    Configuration config = cfgMgr.getCurrentConfig();
    ServletManager mgr = getServletManager();
    if ((mgr instanceof AdminServletManager)) {
      exportDir = ((AdminServletManager)mgr).getExportDir();
    }
    if (exportDir != null && !exportDir.exists()) {
      if (!FileUtil.ensureDirExists(exportDir)) {
	errMsg = "Could not create export directory " + exportDir;
	return;
      }
    }

    // Defaults, will be overwritten by form values if present
    eType = (Type)config.getEnum(Type.class,
				 PARAM_EXPORT_TYPE, DEFAULT_EXPORT_TYPE);
    isCompress = config.getBoolean(PARAM_COMPRESS, DEFAULT_COMPRESS);
    isExcludeDirNodes = config.getBoolean(PARAM_EXCLUDE_DIR_NODES,
					  DEFAULT_EXCLUDE_DIR_NODES);
    xlateFilenames = FilenameTranslation.XLATE_NONE;
    maxSize = config.get(PARAM_MAX_SIZE, DEFAULT_MAX_SIZE);
    maxVersions = config.get(PARAM_MAX_VERSIONS, DEFAULT_MAX_VERSIONS);
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    daemon = getLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    cfgMgr = daemon.getConfigManager();
  }

  public void lockssHandleRequest() throws IOException {

    resetVars();
    getConfig();
    if (errMsg == null) {
      String action = getParameter(KEY_ACTION);

      if (!StringUtil.isNullString(action)) {
	auid = getParameter(KEY_AUID);
	String typename = getParameter(KEY_FILE_TYPE);
	try {
	  eType = Type.valueOf(typename);
	} catch (RuntimeException e) {
	  log.error("illtype", e);
	  errMsg = "Illegal export file type: " + typename;
	  displayPage();
	  return;
	}	
	isCompress = (getParameter(KEY_COMPRESS) != null);
	isExcludeDirNodes = (getParameter(KEY_EXCLUDE_DIR_NODES) != null);
	String xlate = getParameter(KEY_XLATE);
	try {
	  xlateFilenames = FilenameTranslation.valueOf(xlate);
	} catch (RuntimeException e) {
	  log.error("illxlate", e);
	  errMsg = "Illegal translation type: " + xlate;
	  displayPage();
	  return;
	}	
	filePrefix = getParameter(KEY_FILE_PREFIX);
	maxSize = getParameter(KEY_MAX_SIZE);
	maxVersions = getParameter(KEY_MAX_VERSIONS);

      }
      if (ACTION_EXPORT.equals(action)) {
	doExport();
      } else if (ACTION_DELETE.equals(action)) {
	doDelete();
      }
    }
    displayPage();
  }

  private void doExport() {
    ArchivalUnit au = getAu();
    if (au == null) {return;}
    if (errMsg != null) {
      return;
    }
//     try {
    long size;
    int versions;
    if (StringUtil.isNullString(maxSize)) {
      size = -1;
    } else {
      try {
	Double fsize = PlatformUtil.parseDouble(maxSize);
	size = (long)(fsize * 1024 * 1024);
	if (size == 0) {
	  size = -1;
	}
      } catch (NumberFormatException e) {
	errMsg = "Max size must be an integer";
	return;
      }
    }
    if (StringUtil.isNullString(maxVersions)) {
      versions = -1;
    } else {
      try {
	versions = Integer.parseInt(maxVersions);
      } catch (NumberFormatException e) {
	errMsg = "Max versions must be an integer";
	return;
      }
    }

    try {
      Exporter exp = eType.makeExporter(daemon, au);
      exp.setCompress(isCompress);
      exp.setExcludeDirNodes(isExcludeDirNodes);
      exp.setFilenameTranslation(xlateFilenames);
      exp.setDir(exportDir);
      exp.setPrefix(filePrefix);
      exp.setMaxSize(size);
      exp.setMaxVersions(versions);
      exp.export();
      List errs = exp.getErrors();
      log.debug("errs: " + errs);
      if (!errs.isEmpty()) {
	errMsg = StringUtil.separatedString(errs, "<br>");
      } else {
	statusMsg = "File(s) written";
      }
      if (log.isDebug2()) {
	log.debug2("Export files: " + exp.getExportFiles());
      }
    } catch (Exception e) {
      errMsg = e.getMessage();
      return;
    }
  }

  private void doDelete() {
    if (FileUtil.emptyDir(exportDir)) {
      statusMsg = "Export files deleted";
    }
  }

  ArchivalUnit getAu() {
    if (StringUtil.isNullString(auid)) {
      errMsg = "Select an AU";
      return null;
    }
    ArchivalUnit au = pluginMgr.getAuFromId(auid);
    if (au == null) {
      errMsg = "No such AU.  Select an AU";
      return null;
    }
    return au;
  }

  private void displayPage() throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "");
    page.add(makeForm());
    page.add("<br>");
    endPage(page);
  }

  static String CENTERED_CELL = "align=\"center\" colspan=3";

  private Element makeForm() {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");

    Table tbl = new Table(0, "align=center cellspacing=2 cellpadding=0");
    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add("Select AU<br>");
    Composite ausel = ServletUtil.layoutSelectAu(this, KEY_AUID, auid);
    tbl.add(ausel);
    setTabOrder(ausel);

    tbl.newRow();
    tbl.newCell();
    Select typeSel = new Select(KEY_FILE_TYPE, false);
    for (Type t : Type.values()) {
      typeSel.add(t.getLabel(), t == eType, t.name());
    }
    addElementToTable(tbl,
		      "Export file type" + addFootnote(FILE_TYPE_FOOT),
		      typeSel);

    addElementToTable(tbl, "Compress", checkBox(null, "true", KEY_COMPRESS,
						isCompress));

    addElementToTable(tbl,
		      "Exclude Directories" + addFootnote(EXCLUDE_DIR_FOOT),
		      checkBox(null, "true", KEY_EXCLUDE_DIR_NODES,
			       isExcludeDirNodes));

    Composite xlate = new Composite();
    for (FilenameTranslation fx : FilenameTranslation.values()) {
      xlate.add(radioButton(fx.getLabel() + "&nbsp;", fx.name(), KEY_XLATE,
			    xlateFilenames == fx));
    }
    addElementToTable(tbl,
		      "Translate filenames" + addFootnote(XLATE_FOOT),
		      xlate);

    addInputToTable(tbl,
		    "Export file prefix" + addFootnote(FILE_PREFIX_FOOT),
		    KEY_FILE_PREFIX, filePrefix, 20);
    addInputToTable(tbl,
		    "Max export file size (MB)" + addFootnote(MAX_SIZE_FOOT),
		    KEY_MAX_SIZE, maxSize, 6);
//     tbl.newRow();
//     tbl.newCell();
//     tbl.add("&nbsp;");
    addInputToTable(tbl,
		    "Max content file versions" + addFootnote(MAX_VER_FOOT),
		    KEY_MAX_VERSIONS, maxVersions, 6);

    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    Input export = new Input(Input.Submit, KEY_ACTION, ACTION_EXPORT);
    tbl.add(export);

    frm.add(tbl);
    Table filetab = getFileList();
    if (filetab != null) {
      frm.add(filetab);
    }

    comp.add(frm);
    return comp;
  }

  private Table getFileList() {
    Table tbl = new Table(0, "align=center cellspacing=2 cellpadding=0");
    try {
      if (exportDir.isDirectory() && exportDir.exists()) {
	File files[] = exportDir.listFiles();
	if (files != null && files.length > 0) {
	  Arrays.sort(files);
	  tbl.newRow();
	  tbl.newCell();
	  tbl.add("&nbsp;&nbsp;");
// 	  tbl.newRow();
// 	  tbl.newCell(CENTERED_CELL);
// 	  tbl.add(srvLink(AdminServletManager.LINK_EXPORTS,
// 			  "Export files"));
	  tbl.newRow();
	  tbl.newCell(CENTERED_CELL);
	  tbl.add("<b>Export Files</b>");

	  ServletDescr descr = AdminServletManager.LINK_EXPORTS;
	  String base = srvURL(descr);
	  DateFormat df = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");

	  for (File file : files) {
	    String fname = file.getName();
	    tbl.newRow();
	    tbl.newCell();
	    Link lnk = new Link(base + "/" + fname, fname);
	    tbl.add(lnk);
	    tbl.add("&nbsp;&nbsp;");
	    tbl.newCell("align=right");
	    tbl.add(StringUtil.sizeToString(file.length()));
	    tbl.add("&nbsp;&nbsp;");
	    tbl.newCell();
	    tbl.add(df.format(file.lastModified()));

	  }
	  tbl.newRow();
	  tbl.newCell(CENTERED_CELL);
	  Input delete = new Input(Input.Submit, KEY_ACTION, ACTION_DELETE);
	  tbl.add(delete);
	}
      }
    } catch (RuntimeException e) {
    }
    return tbl;
  }


  void addInputToTable(Table tbl, String label, String key, String init, int size) {
    Input in = new Input(Input.Text, key, init);
    in.setSize(size);
    addElementToTable(tbl, label, in);
  }

  void addElementToTable(Table tbl, String label, Element elem) {
    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add(label);
    tbl.add(":");
    tbl.newCell();
    tbl.add("&nbsp;");
    tbl.newCell();
    tbl.add(elem);
  }

//   Iterator iter = pluginMgr.getAllAus().iterator(); iter.hasNext(); ) {
//     ArchivalUnit au0 = (ArchivalUnit)iter.next();
//     String id = au0.getAuId();
//     sel.add(au0.getName(), id.equals(auid), id);
//   }



}
