/*
 * $Id: ZipExporter.java,v 1.2 2010-02-22 07:01:07 tlipkis Exp $
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
import java.text.*;
import java.util.*;
import java.util.zip.*;

import org.archive.util.*;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.output.CountingOutputStream;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Export to a zip file
 */
public class ZipExporter extends Exporter {

  private static Logger log = Logger.getLogger("ZipExporter");

  ZipOutputStream zip = null;
  CountingOutputStream cos = null;
  int serialNo = 1;
  String timestamp = null;

  // Formatter to pad filename numbers to four digits.
  private static NumberFormat serialNumberFormatter = new DecimalFormat("0000");

  public ZipExporter(LockssDaemon daemon, ArchivalUnit au) {
    super(daemon, au);
  }

  protected void checkArgs() {
    if (getMaxVersions() > 1) {
      throw new IllegalArgumentException("Zip exporter cannot write multiple file versions");
    }
    super.checkArgs();
  }

  protected void start() {
  }

  protected void finish() throws IOException {
    if (zip != null) {
      closeZip();
    }
  }

  protected void writeCu(CachedUrl cu) throws IOException {
    ensureOpenZip();
    InputStream ins = cu.getUnfilteredInputStream();
    CIProperties props = cu.getProperties();
    ZipEntry ent = new ZipEntry(cu.getUrl());
    // Store HTTP response headers into entry comment
    ent.setComment(getHttpResponseString(cu));
    try {
      long cuLastModified =
	dateAsLong(props.getProperty(CachedUrl.PROPERTY_LAST_MODIFIED));
      ent.setTime(cuLastModified);
    } catch (RuntimeException e) {
    }
    zip.putNextEntry(ent);
    StreamUtil.copy(ins, zip);
    zip.closeEntry();
//     zip.flush();
  }
  
  private long dateAsLong(String datestr) {
    try {
      return DateUtil.parseDate(datestr).getTime();
    } catch (org.apache.commons.httpclient.util.DateParseException e) {
      log.warning("Error parsing response header: " + datestr
		  + ": " + e.getMessage());
      return -1;
    }
  }

  private void closeZip() throws IOException {
    zip.close();
    zip = null;
    cos = null;
  }

  private void ensureOpenZip() throws IOException {
    if (zip != null) {
      if (getMaxSize() <= 0 || cos.getByteCount() < getMaxSize()) {
	return;
      }
      closeZip();
    }

    if (timestamp == null) {
      timestamp = ArchiveUtils.get14DigitDate();
    }
    String name =
      prefix + "-" + timestamp + "-" +
      serialNumberFormatter.format(serialNo++) + ".zip";
    File file = new File(getDir(), name);
    cos = new CountingOutputStream(new FileOutputStream(file));
    OutputStream os = new BufferedOutputStream(cos);
    zip = new ZipOutputStream(os);
    setZipComment(zip);
  }

  void setZipComment(ZipOutputStream zip) {
    StringBuilder sb = new StringBuilder();
    sb.append(au.getName());
    sb.append(Constants.CRLF);
    sb.append("Exported from LOCKSS by ");
    sb.append(getHostName());
    sb.append(" at ");
    sb.append(new Date());
    sb.append(Constants.CRLF);

    try {
      zip.setComment(sb.toString());
    } catch (RuntimeException e) {
      log.warning("Couldn't write zip comment", e);
    }
  }

}
