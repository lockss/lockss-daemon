/*
 * $Id$
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
import org.apache.commons.collections.CollectionUtils;

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

  public void testIsDirOf() throws Exception {
    String base = (String)CollectionUtil.getAnElement(sau.getUrlStems());
    String dir, dirno;
    if (!base.endsWith("/")) {
      base += "/";
    }
    dir = base + "dirname/";
    dirno = base + "dirname";
      
    CachedUrl cudir = sau.makeCachedUrl(dir);
    CachedUrl cudirno = sau.makeCachedUrl(dirno);
    CachedUrl cufile1 = sau.makeCachedUrl(dir + "file");
    CachedUrl cufile2 = sau.makeCachedUrl(dir + "dir2/file.bar");
    CachedUrl cufile3 = sau.makeCachedUrl(base + "nondirname/file.bar");

    ZipExporter exp = new ZipExporter(daemon, sau);
    assertTrue(exp.isDirOf(cudir, cufile1));
    assertTrue(exp.isDirOf(cudir, cufile2));
    assertFalse(exp.isDirOf(cudir, cufile3));
    assertFalse(exp.isDirOf(cufile1, cudir));

    assertTrue(exp.isDirOf(cudirno, cufile1));
    assertTrue(exp.isDirOf(cudirno, cufile2));
    assertFalse(exp.isDirOf(cudirno, cufile3));
    assertFalse(exp.isDirOf(cufile1, cudirno));

    assertFalse(exp.isDirOf(cudir, cudir));
    assertFalse(exp.isDirOf(cufile1, cufile1));

  }

  // Combining in one testcase is a little faster because the sim content
  // tree doesn't have to be recreated and recrawled for each different
  // export test.
  public void testSeveralVariants() throws Exception {
    assertEquals(auUrls,
		 testExport(true, -1, FilenameTranslation.XLATE_NONE, false));
    testExport(false, 2000, FilenameTranslation.XLATE_NONE, false);
    testExport(true, -1, FilenameTranslation.XLATE_WINDOWS, false);

    assertEquals(CollectionUtils.subtract(auUrls, auDirs),
		 testExport(true, -1, FilenameTranslation.XLATE_WINDOWS, true));
  }

  public List<String> testExport(boolean isCompress, long maxSize,
				 FilenameTranslation xlate,
				 boolean excludeDirContent)
      throws Exception {
    exportDir = getTempDir();
    exportFiles = null;
    ZipExporter exp = new ZipExporter(daemon, sau);
    exp.setDir(exportDir);
    exp.setPrefix("zippre");
    exp.setCompress(isCompress);
    exp.setFilenameTranslation(xlate);
    exp.setExcludeDirNodes(excludeDirContent);
    if (maxSize > 0) {
      exp.setMaxSize(maxSize);
    }
    exp.export();

    int numZipRecords = 0;
    File expFile;
    List<String> urls = new ArrayList<String>();
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
	  urls.add(cu.getUrl());
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
    if (excludeDirContent) {
      assertEquals(auUrls.size() - auDirs.size(), numZipRecords);
    } else {
      assertEquals(auUrls.size(), numZipRecords);
    }
    if (maxSize <= 0) {
      assertEquals(1, exportFiles.length);
    } else {
      assertTrue("Expected more than one export file",
		 exportFiles.length > 1);
    }

    List<File> filesWritten = exp.getExportFiles();
    if (maxSize < 0) {
      assertEquals(1, filesWritten.size());
    } else {
      assertEquals(exportFiles.length, filesWritten.size());
    }
    int ix = 1;
    for (File f : filesWritten) {
      assertMatchesRE(String.format("%s%s%s-[0-9]+-%0,5d\\.zip",
				    exportDir, File.separator,
				    exp.getPrefix(), ix),
		      f.toString());
      ix++;
    }
    assertSameElements(exportFiles, filesWritten);
    return urls;
  }
}

