/*
 * $Id: FuncZipExporter.java,v 1.3 2010-02-22 08:07:45 tlipkis Exp $
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

public class FuncZipExporter extends BaseFuncExporter {

  // Combining in one testcase is a little faster because the sim content
  // tree doesn't have to be recreated and recrawled for each different
  // export test.
  public void testSeveralVariants() throws Exception {
    testExport(-1);
    testExport(2000);
  }

  public void testExport(long maxSize) throws Exception {
    exportDir = getTempDir();
    exportFiles = null;
    ZipExporter exp = new ZipExporter(daemon, sau);
    exp.setDir(exportDir);
    exp.setPrefix("zippre");
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

