/*
 * $Id: WarcExporter.java,v 1.4 2010-02-24 03:29:16 tlipkis Exp $
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
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.archive.io.*;
import org.archive.io.warc.*;
import org.archive.util.*;
import org.archive.util.anvl.*;
import static org.archive.io.warc.WARCConstants.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Export to an ARC file
 */
public class WarcExporter extends Exporter {

  private static Logger log = Logger.getLogger("WarcExporter");

  protected CIProperties arcProps = null;
  String arcFilePrefix = "SimulatedCrawl";
  AtomicInteger serialNo = new AtomicInteger(0);
  WARCWriter ww;
  boolean isResponse;

  public WarcExporter(LockssDaemon daemon, ArchivalUnit au,
		      boolean isResponse) {
    super(daemon, au);
    this.isResponse = isResponse;
  }

  protected void start() throws IOException {
    ww = makeWARCWriter();
  }

  protected void finish() throws IOException {
    ww.close();
  }

  private WARCWriter makeWARCWriter() {
    Properties props = new Properties();
    props.put("software", getSoftwareVersion());
    props.put("ip", getHostIp());
    props.put("hostname", getHostName());
    props.put("format","WARC File Format 0.17");
    props.put("conformsTo",
	      "http://crawler.archive.org/warc/0.17/WARC0.17ISO.doc");
    props.put("created", ArchiveUtils.getLog14Date(TimeBase.nowMs()));
    props.put("description", au.getName());
    props.put("robots", "ignore");
    props.put("http-header-user-agent", daemon.getUserAgent());
    List metadata = new ArrayList<String>();
    for (Map.Entry ent : props.entrySet()) {
      String key = (String)ent.getKey();
      metadata.add((String)ent.getKey() + ": "
		   + (String)ent.getValue() + "\r\n");
    }
    return new WARCWriter(serialNo,
			  ListUtil.list(dir),
			  prefix,
			  "" /*settings.getSuffix()*/ ,
			  compress,
			  maxSize >= 0 ? maxSize : Long.MAX_VALUE,
			  metadata
			  );
  }

  protected void writeCu(CachedUrl cu) throws IOException {
    String url = cu.getUrl();
    InputStream contentIn = cu.getUnfilteredInputStream();
    long contentSize = cu.getContentSize();
    CIProperties props = cu.getProperties();
    ANVLRecord headers = new ANVLRecord(5);
    headers.addLabelValue(HEADER_KEY_IP, getHostIp());
    long fetchTime =
      Long.parseLong(props.getProperty(CachedUrl.PROPERTY_FETCH_TIME));
    String timestamp = ArchiveUtils.getLog14Date(fetchTime);
    if (isResponse) {
      try {
	String hdrString = getHttpResponseString(cu);
	long size = contentSize + hdrString.length();
	InputStream headerIn =
	  new ReaderInputStream(new StringReader(hdrString));
	InputStream concat = new SequenceInputStream(headerIn, contentIn);
	try {
	  ww.writeResponseRecord(xlateFilename(url),
				 timestamp,
				 HTTP_RESPONSE_MIMETYPE,
				 WARCWriter.getRecordID(),
				 headers, concat, size);
	} finally {
	  IOUtil.safeClose(contentIn);
	}
      } catch (IOException e) {
	log.error("writeCu("+url+"): ", e);
	throw e;
      }
    } else {
      try {
	String mimeType =
	  HeaderUtil.getMimeTypeFromContentType(cu.getContentType());
	try {
	  ww.writeResourceRecord(xlateFilename(url),
				 timestamp,
				 mimeType,
				 headers, contentIn, contentSize);
      } finally {
	  IOUtil.safeClose(contentIn);
	}
      } catch (IOException e) {
	log.error("writeCu("+url+"): ", e);
	throw e;
      }
    }
  }
  


}
