/*
 * $Id: FuncZipExporter.java,v 1.4 2010-02-24 03:29:16 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.exporter.Exporter.FilenameTranslation;

public class FuncZipExporter extends BaseFuncExporter {

  public void testXlate() throws Exception {
    String t1 = "http://foo?bar<a>b\\c*d|e";
    ZipExporter exp = new ZipExporter(daemon, sau);
    assertSame(t1, exp.xlateFilename(t1));
    exp.setFilenameTranslation(FilenameTranslation.XLATE_MAC);
    assertEquals("http_//foo?bar<a>b\\c*d|e", exp.xlateFilename(t1));
    exp.setFilenameTranslation(FilenameTranslation.XLATE_WINDOWS);
    assertEquals("http_//foo_bar_a_b_c_d_e", exp.xlateFilename(t1));
  }

  // Combining in one testcase is a little faster because the sim content
  // tree doesn't have to be recreated and recrawled for each different
  // export test.
  public void testSeveralVariants() throws Exception {
    testExport(true, -1, FilenameTranslation.XLATE_NONE);
    testExport(false, 2000, FilenameTranslation.XLATE_NONE);
    testExport(true, -1, FilenameTranslation.XLATE_WINDOWS);
  }

  public void testExport(boolean isCompress, long maxSize,
			 FilenameTranslation xlate)
      throws Exception {
    exportDir = getTempDir();
    exportFiles = null;
    ZipExporter exp = new ZipExporter(daemon, sau);
    exp.setDir(exportDir);
    exp.setPrefix("zippre");
    exp.setCompress(isCompress);
    exp.setFilenameTranslation(xlate);
    if (maxSize > 0) {
      exp.setMaxSize(maxSize);
    }
    exp.export();

    int numZipRecords = 0;
    File expFile;
    while ((expFile = nextExportFile()) != null) {
      ZipFile zip = new ZipFile(expFile);

      Enumeration iter = zip.entries();
      while (iter.hasMoreElements()) {
	ZipEntry rec = null;
	CachedUrl cu = null;
	try {
	  rec = (ZipEntry)iter.nextElement();
	  String url = rec.getName();
	  switch (xlate) {
	  case XLATE_NONE:
	    assertMatchesRE("^http://", url);
	    break;
	  case XLATE_WINDOWS:
	  case XLATE_MAC:
	    // The filenames were translated.  Current windows translation
	    // isn't reversible in general, but we know that the only
	    // windows-illegal character in simulated content is the colon,
	    // so we can reverse it.  If that changes, this test will
	    // break.

	    assertMatchesRE("^http_//", url);
	    url = StringUtil.replaceString(url, "_", ":");
	    break;

	  }
	  String comment = rec.getComment();
	  cu = sau.makeCachedUrl(url);
// 	  log.debug("Comp " + hdr.getMimetype() + ": " + url);
	  assertTrue("No content in AU: " + url, cu.hasContent());
	  InputStream ins = cu.getUnfilteredInputStream();
	  assertTrue(StreamUtil.compare(zip.getInputStream(rec), ins));
	  assertEquals(cu.getContentSize(), rec.getSize());
//  	  assertMatchesRE(cu.getContentType(), comment);
	  numZipRecords++;

	} finally {
 	  AuUtil.safeRelease(cu);
	}
      }

    }
    assertEquals(numAuFiles, numZipRecords);
    if (maxSize <= 0) {
      assertEquals(1, exportFiles.length);
    } else {
      assertTrue("Expected more than one export file",
		 exportFiles.length > 1);
    }
  }
}

