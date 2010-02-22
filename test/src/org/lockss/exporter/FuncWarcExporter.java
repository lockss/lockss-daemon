/*
 * $Id: FuncWarcExporter.java,v 1.3 2010-02-22 08:07:46 tlipkis Exp $
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

import org.archive.io.*;
import org.archive.io.warc.*;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

public class FuncWarcExporter extends BaseFuncExporter {

  // Combining in one testcase is a little faster because the sim content
  // tree doesn't have to be recreated and recrawled for each different
  // export test.
  public void testSeveralVariants() throws Exception {
    testExport(true, false, -1);
    testExport(false, false, -1);
    testExport(true, false, 5000);
    testExport(false, false, 5000);

    testExport(true, true, -1);
    testExport(false, true, -1);
    testExport(true, true, 5000);
    testExport(false, true, 5000);
  }

  public void testExport(boolean isCompress, boolean isResponse,
			 long maxSize)
      throws Exception {
    exportDir = getTempDir();
    exportFiles = null;
    WarcExporter exp = new WarcExporter(daemon, sau, isResponse);
    exp.setDir(exportDir);
    exp.setPrefix("warcpre");
    exp.setCompress(isCompress);
    if (maxSize > 0) {
      exp.setMaxSize(maxSize);
    }
    exp.export();

    int numWarcRecords = 0;
    File expFile;
    while ((expFile = nextExportFile()) != null) {
      ArchiveReader ardr = WARCReaderFactory.get(expFile);
      assertEquals(isCompress, ardr.isCompressed());

      Iterator iter = ardr.iterator();
      // Skip first record
      iter.next();
      while (iter.hasNext()) {
	ArchiveRecord rec = null;
	try {
	  rec = (ArchiveRecord)iter.next();
	  // 	rec.dump();
	  ArchiveRecordHeader hdr = rec.getHeader();

	  String url = hdr.getUrl();
	  CachedUrl cu = sau.makeCachedUrl(url);
	  log.debug("Comp " + hdr.getMimetype() + ": " + url);
	  assertTrue("No content in AU: " + url, cu.hasContent());
	  InputStream ins = cu.getUnfilteredInputStream();
	  if (isResponse) {
	    assertEquals("application/http; msgtype=response",
			 hdr.getMimetype());

	    String httphdr = readHeader(rec);
	    assertMatchesRE("HTTP/1\\.1 200 OK", httphdr);
	    assertMatchesRE("Last-Modified:", httphdr);
	    assertEquals(cu.getContentSize() + httphdr.length(),
			 hdr.getLength() - hdr.getContentBegin());
	    assertTrue(StreamUtil.compare(rec, ins));
	  } else {
	    assertTrue(StreamUtil.compare(rec, ins));
	    assertEquals(cu.getContentSize(),
			 hdr.getLength() - hdr.getContentBegin());
	    assertEquals(cu.getContentType(), hdr.getMimetype());
	  }
	  numWarcRecords++;

	} finally {
	  IOUtil.safeClose(rec);
	}
      }

    }
    assertEquals(numAuFiles, numWarcRecords);
    if (maxSize <= 0) {
      assertEquals(1, exportFiles.length);
    } else {
      // XXX IF THIS FAILS IT MAY BE DUE TO SYSTEM-DEPENDENT I/O BUFFERING
      // Instead of counting the bytes written to the output stream, the
      // ARC/WARC writers call File.length(), which generally doesn't
      // include any buffered data waiting to be flushed.  May have to
      // change test to write more data to reduce effect of buffering.

      assertTrue("Expected more than one export file",
		 exportFiles.length > 1);
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {FuncWarcExporter.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}

