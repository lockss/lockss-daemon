/*
 * $Id$
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
import java.util.*;

import org.lockss.util.CIProperties;
import org.lockss.crawler.Exploder;

/**
 * A data structure representing an entry in a generic archive
 * file. Created by the Exploder sub-classes to communicate
 * with the publisher-specific ExploderHelper classes.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class ArchiveEntry {
  // Input fields
  private String name;
  private long bytes;
  private long date;
  private InputStream is;
  private Exploder exploder;
  private String archiveName;
  private String explodedAUBaseUrlStem;
  // Output fields
  private String baseUrl;
  private String restOfUrl;
  private CIProperties header;
  private CIProperties auProps;
  private Hashtable addText;

  public ArchiveEntry(String name, long bytes, long date, InputStream is) {
    setup(name, bytes, date, is, (Exploder) null, null);
  }

  public ArchiveEntry(String name, long bytes, long date, InputStream is,
		      Exploder exploder) {
    setup(name, bytes, date, is, exploder, null);
  }

  public ArchiveEntry(String name, long bytes, long date, InputStream is,
		       Exploder exploder, String archiveName) {
    setup(name, bytes, date, is, exploder, archiveName);
  }

  private void setup(String name, long bytes, long date, InputStream is,
		     Exploder exploder, String archiveName) {
    if (name.startsWith("./")) {
      this.name = name.substring(2);
    } else {
      this.name = name;
    }
    this.bytes = bytes;
    this.date = date;
    this.is = is;
    this.exploder = exploder;
    this.archiveName = archiveName;
    baseUrl = null;
    restOfUrl = null;
    header = null;
    auProps = null;
    addText = null;
  }

  // Input field accessors

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

  public Exploder getExploder() {
    return exploder;
  }

  public String  getArchiveName() {
    return archiveName;
  }

  // Output field accessors

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

  public CIProperties getAuProps() {
    return auProps;
  }

  public void setAuProps(CIProperties cip) {
    auProps = cip;
  }

  public Hashtable getAddText() {
    return addText;
  }

  /**
   * After explosion, the collected set of (text,where) pairs
   * will be processed to ensure that <code>text</code> appears
   * once in the filein the AU specified by <code>url</code>.
   * The file will be created if needed.  The file is expected
   * to contain HTML - it will have head and tail HTML
   * auto-generated with <ul></ul> where the links should go.
   * The goal is to auto-generate manifest pages linking
   * to the exploded content.
   * @param restOfUrl where to add it.
   * @param text to be added
   */
  public void addTextTo(String restOfUrl, String text) {
    if (addText == null) {
      addText = new Hashtable();
    }
    addText.put(restOfUrl, text);
  }
}
