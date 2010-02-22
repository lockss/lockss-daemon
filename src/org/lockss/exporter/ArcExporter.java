/*
 * $Id: ArcExporter.java,v 1.2 2010-02-22 07:01:07 tlipkis Exp $
 */

/*

Copyright (c) 2010 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.concurrent.atomic.AtomicInteger;

import org.archive.io.*;
import org.archive.io.arc.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Export to an ARC file
 */
public class ArcExporter extends Exporter {

  private static Logger log = Logger.getLogger("ArcExporter");

  protected CIProperties arcProps = null;
  String arcFilePrefix = "SimulatedCrawl";
  AtomicInteger serialNo = new AtomicInteger(0);
  ARCWriter aw;
  boolean isResponse;

  public ArcExporter(LockssDaemon daemon, ArchivalUnit au,
		     boolean isResponse) {
    super(daemon, au);
    this.isResponse = isResponse;
  }

  protected void start() {
    aw = makeARCWriter();
  }

  protected void finish() throws IOException {
    aw.close();
  }

  private ARCWriter makeARCWriter() {
    return new ARCWriter(serialNo,
			 ListUtil.list(dir),
			 prefix,
			 compress,
			 maxSize >= 0 ? maxSize : Long.MAX_VALUE);
  }

  protected void writeCu(CachedUrl cu) throws IOException {
    String url = cu.getUrl();
    InputStream contentIn = cu.getUnfilteredInputStream();
    long contentSize = cu.getContentSize();
    CIProperties props = cu.getProperties();
    long fetchTime =
      Long.parseLong(props.getProperty(CachedUrl.PROPERTY_FETCH_TIME));
    try {
      if (isResponse) {
	String hdrString = getHttpResponseString(cu);
	long size = contentSize + hdrString.length();
	InputStream headerIn =
	  new ReaderInputStream(new StringReader(hdrString));
	InputStream concat = new SequenceInputStream(headerIn, contentIn);
	aw.write(url, cu.getContentType(),
		 getHostIp(), fetchTime, size, concat);
      } else {
	aw.write(url, cu.getContentType(),
		 getHostIp(), fetchTime, cu.getContentSize(), contentIn);
      }
    } catch (IOException e) {
      log.error("writeCu("+url+"): ", e);
      throw e;
    } finally {
      AuUtil.safeRelease(cu);
    }
  }
}
