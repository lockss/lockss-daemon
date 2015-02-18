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

package org.lockss.daemon;

import java.util.*;
import java.io.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.rewriter.*;
import org.lockss.plugin.*;
import org.lockss.plugin.wrapper.*;

/** Record of MIME type-specific factories (<i>eg</i>, FilterFactory,
 * LinkExtractorFactory).  Primary interface is immutable.
 */
public interface MimeTypeInfo {
  /** An empty MimeTypeInfo */
  public static MimeTypeInfo NULL_INFO = new MimeTypeInfo.Impl();

  public static final String DEFAULT_METADATA_TYPE = "*";

  /** Returns the hash FilterFactory, or null */
  public FilterFactory getHashFilterFactory();
  /** Returns the crawl FilterFactory, or null */
  public FilterFactory getCrawlFilterFactory();
  /** Returns the LinkExtractorFactory, or null */
  public LinkExtractorFactory getLinkExtractorFactory();
  /** Returns the UrlRewriterFactory, or null */
  public LinkRewriterFactory getLinkRewriterFactory();
  /** Returns the default FileMetadataExtractorFactory, or null */
  public FileMetadataExtractorFactory getFileMetadataExtractorFactory();
  /** Returns the FileMetadataExtractorFactory for the metadata type, or null */
  public FileMetadataExtractorFactory
    getFileMetadataExtractorFactory(String mdType);
  /** Returns the FileMetadataExtractorFactoryMap, or null */
  public Map<String,FileMetadataExtractorFactory>
    getFileMetadataExtractorFactoryMap();

  /** Sub interface adds setters */
  public interface Mutable extends MimeTypeInfo {
    public Impl setHashFilterFactory(FilterFactory fact);
    public Impl setCrawlFilterFactory(FilterFactory fact);
    public Impl setLinkExtractorFactory(LinkExtractorFactory fact);
    public Impl setLinkRewriterFactory(LinkRewriterFactory fact);
    public Impl setFileMetadataExtractorFactoryMap(Map<String,FileMetadataExtractorFactory> map);
  }

  class Impl implements Mutable {
    static Logger log = Logger.getLogger("MimeTypeInfo");

    private FilterFactory hashFilterFactory;
    private FilterFactory crawlFilterFactory;
    private LinkExtractorFactory extractorFactory;
    private LinkRewriterFactory linkFactory;
    private Map<String,FileMetadataExtractorFactory>
      metadataExtractorFactoryMap;

    public Impl() {
    }

    public Impl(MimeTypeInfo toClone) {
      if (toClone != null) {
	hashFilterFactory = toClone.getHashFilterFactory();
	crawlFilterFactory = toClone.getCrawlFilterFactory();
	extractorFactory = toClone.getLinkExtractorFactory();
	linkFactory = toClone.getLinkRewriterFactory();
	metadataExtractorFactoryMap =
	  toClone.getFileMetadataExtractorFactoryMap();
      }
    }

    public FilterFactory getHashFilterFactory() {
      return hashFilterFactory;
    }

    public Impl setHashFilterFactory(FilterFactory fact) {
      hashFilterFactory = fact;
      return this;
    }

    public FilterFactory getCrawlFilterFactory() {
      return crawlFilterFactory;
    }

    public Impl setCrawlFilterFactory(FilterFactory fact) {
      crawlFilterFactory = fact;
      return this;
    }

    public LinkExtractorFactory getLinkExtractorFactory() {
      return extractorFactory;
    }

    public Impl setLinkExtractorFactory(LinkExtractorFactory fact) {
      extractorFactory = fact;
      return this;
    }

    public LinkRewriterFactory getLinkRewriterFactory() {
      return linkFactory;
    }

    public Impl setLinkRewriterFactory(LinkRewriterFactory fact) {
      linkFactory = fact;
      return this;
    }

    public FileMetadataExtractorFactory getFileMetadataExtractorFactory() {
      return getFileMetadataExtractorFactory(DEFAULT_METADATA_TYPE);
    }

    public FileMetadataExtractorFactory
      getFileMetadataExtractorFactory(String mdType) {
      if (metadataExtractorFactoryMap != null) {
        return
	  (FileMetadataExtractorFactory)metadataExtractorFactoryMap.get(mdType);
      }
      return null;
    }

    public Map getFileMetadataExtractorFactoryMap() {
      return metadataExtractorFactoryMap;
    }

    public Impl setFileMetadataExtractorFactoryMap(Map map) {
      metadataExtractorFactoryMap = map;
      return this;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[mti: hf: ");
      sb.append(getHashFilterFactory());
      sb.append(", cf: ");
      sb.append(getCrawlFilterFactory());
      sb.append(", lef: ");
      sb.append(getLinkExtractorFactory());
      sb.append(", mef: ");
      sb.append(metadataExtractorFactoryMap);
      sb.append("]");
      return sb.toString();
    }

  }

}
