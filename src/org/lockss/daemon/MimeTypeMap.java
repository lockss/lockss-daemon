/*
 * $Id: MimeTypeMap.java,v 1.1 2007-02-06 00:50:26 tlipkis Exp $
 */

/*
 Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.*;
// import org.lockss.app.*;
// import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/** Record of MIME type-specific factories (<i>eg</i>, FilterFactory,
 * LinkExtractorFactory), and static global defaults
 */
public class MimeTypeMap {
  static Logger log = Logger.getLogger("MimeTypeMap");

  private static Map canonMap = new HashMap();
  public static MimeTypeMap DEFAULT = new MimeTypeMap();

  private static MimeTypeInfo HTML = new MimeTypeInfo();
  private static MimeTypeInfo CSS = new MimeTypeInfo();
  static {
    HTML.setLinkExtractorFactory(new GoslingHtmlLinkExtractor.Factory());
    DEFAULT.putMimeTypeInfo("text/html", HTML);
    CSS.setLinkExtractorFactory(new CssLinkExtractor.Factory());
    DEFAULT.putMimeTypeInfo("text/css", CSS);
  }

  private Map map = new HashMap();
  private MimeTypeMap parent;
//   private boolean modifiable = true;

  public MimeTypeMap() {
  }

  public MimeTypeMap(MimeTypeMap parent) {
    this.parent = parent;
  }

  public MimeTypeMap getParent() {
    return parent;
  }      

  public void putMimeTypeInfo(String contentType, MimeTypeInfo mti) {
    String mime = HeaderUtil.getMimeTypeFromContentType(contentType);
    map.put(mime, mti);
  }

  public MimeTypeInfo getMimeTypeInfo(String contentType) {
    String mime = HeaderUtil.getMimeTypeFromContentType(contentType);
    MimeTypeInfo res = (MimeTypeInfo)map.get(mime);
    if (res == null && parent != null) {
      return parent.getMimeTypeInfo(mime);
    }
    return res;
  }

  /** Return a modifiable (<i>Ie</i>, local to this map) MimeTypeInfo for
   * the given MIME type */
  public MimeTypeInfo modifyMimeTypeInfo(String contentType) {
    String mime = HeaderUtil.getMimeTypeFromContentType(contentType);
    MimeTypeInfo res = (MimeTypeInfo)map.get(mime);
    if (res == null) {
      if (parent != null) {
	res = new MimeTypeInfo(parent.getMimeTypeInfo(mime));
      } else {
	res = new MimeTypeInfo();
      }
      map.put(mime, res);
    }
    return res;
  }
}
