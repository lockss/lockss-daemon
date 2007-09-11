/*
 * $Id: ArchiveEntry.java,v 1.1.2.1 2007-09-11 19:14:56 dshr Exp $
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.io.*;
import org.lockss.util.CIProperties;
import org.lockss.daemon.CrawlSpec;

/**
 * A data structure representing an entry in a generic archive
 * file. Created by the Exploder sub-classes to communicate
 * with the publisher-specific ExploderHelper classes.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class ArchiveEntry {
  private String name;
  private long bytes;
  private long date;
  private InputStream is;
  private String baseUrl;
  private String restOfUrl;
  private CIProperties header;
  private CrawlSpec crawlSpec;

  public ArchiveEntry(String name, long bytes, long date, InputStream is,
		      CrawlSpec crawlSpec) {
    this.name = name;
    this.bytes = bytes;
    this.date = date;
    this.is = is;
    this.crawlSpec = crawlSpec;
    baseUrl = null;
    restOfUrl = null;
    header = null;
  }

  public String getName() {
    return name;
  }

  public long getSize() {
    return bytes;
  }

  public long getDate() {
    return date;
  }

  public InputStream getInputStream() {
    return is;
  }

  public CrawlSpec getCrawlSpec() {
    return crawlSpec;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String url) {
    baseUrl = url;
  }

  public String getRestOfUrl() {
    return restOfUrl;
  }

  public void setRestOfUrl(String url) {
    restOfUrl = url;
  }

  public CIProperties getHeaderFields() {
    return header;
  }

  public void setHeaderFields(CIProperties cip) {
    header = cip;
  }
}
