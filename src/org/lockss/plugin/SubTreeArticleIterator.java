/*
 * $Id: SubTreeArticleIterator.java,v 1.1 2009-05-28 22:52:57 dshr Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.daemon.PluginException;

/*
 * XXX This implementation builds an ArrayList of the CachedUrls.
 * XXX Not a good idea - the iterator returned should invoke
 * XXX internal iterators to avoid holding that much memory.
 */
public class SubTreeArticleIterator implements Iterator {
  static Logger log = Logger.getLogger("SubTreeArticleIterator");
  String mimeType;
  ArchivalUnit au;
  Iterator it = null;
  ArrayList al = new ArrayList();
  String subTreeRoot = null;
  public SubTreeArticleIterator(String mimeType, ArchivalUnit au,
				String subTreeRoot) {
    this.mimeType = ( mimeType == null ? "text/html" : mimeType );
    this.au = au;
    this.subTreeRoot = subTreeRoot;
    log.debug("Mime " + this.mimeType + " subTree " + this.subTreeRoot +
	      " au " + this.au.toString());
  }
  public void makeIterator() {
    Collection stems = au.getUrlStems();
    for (Iterator it = stems.iterator(); it.hasNext(); ) {
      String stem = (String) it.next();
      if (!stem.endsWith("/")) stem += "/";
      log.debug("Subtree: " + stem + " + " + subTreeRoot);
      CachedUrlSet cus = null;
      if ("".equals(subTreeRoot)) {
	  cus = ((BaseArchivalUnit)au).getAuCachedUrlSet();
      } else {
        CachedUrlSetSpec cuss = new RangeCachedUrlSetSpec(stem + subTreeRoot);
	cus = au.makeCachedUrlSet(cuss);
      }
      Iterator chi = cus.contentHashIterator();
      while (chi.hasNext()) {
	Object n = chi.next();
	if (n instanceof CachedUrl) {
	    CachedUrl cu = (CachedUrl) n;
	    String contentType = cu.getContentType();
	    String mimeType2 =
		HeaderUtil.getMimeTypeFromContentType(contentType);
	    log.debug("CU: " + cu.getUrl() + " mime " + mimeType2);
	    if (mimeType.equalsIgnoreCase(mimeType2)) {
		log.debug("Add " + cu.getUrl());
		al.add(cu);
	    }
	} else if (n instanceof CachedUrlSet) {
	    CachedUrlSet cus2 = (CachedUrlSet) n;
	    log.debug("CUS: " + cus.getUrl());
	} else {
	    log.debug("XXX: " + n.getClass());
	}
      }
    }
    al.trimToSize();
    it = al.iterator();
  }
  public boolean hasNext() {
    if (it == null) {
      makeIterator();
    }
    return it.hasNext();
  }
  public Object next() {
    if (it == null) {
      makeIterator();
    }
    return it.next();
  }
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
