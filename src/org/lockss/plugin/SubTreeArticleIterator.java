/*
 * $Id: SubTreeArticleIterator.java,v 1.3.2.2 2009-10-11 23:08:12 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.base.*;


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
  Pattern pat = null;
  
  public SubTreeArticleIterator(String mimeType, ArchivalUnit au,
				String subTreeRoot) {
    this(mimeType, au, subTreeRoot, null);
  }
  
  public SubTreeArticleIterator(String mimeType, ArchivalUnit au,
				String subTreeRoot, Pattern pat) {
    this.mimeType = ( mimeType == null ? "text/html" : mimeType );
    this.au = au;
    this.subTreeRoot = subTreeRoot;
    this.pat = pat;
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
	  try {
	    processCachedUrl(cu);
	  } catch (Exception ex) {
	    // No action intended - iterator should ignore this cu.
	    log.warning("Error processing " + cu.getUrl(), ex);
	  }
	  finally {
	    AuUtil.safeRelease(cu);
	  }
	} else if (n instanceof CachedUrlSet) {
	  CachedUrlSet cus2 = (CachedUrlSet) n;
	  if (log.isDebug2()) log.debug2("CUS: " + cus.getUrl());
	} else {
	  log.warning("Unknown node type: " + n.getClass());
	}
      }
    }
    al.trimToSize();
    it = al.iterator();
  }

  protected void processCachedUrl(CachedUrl cu) {
    if (cu.hasContent()) {
      String contentType = cu.getContentType();
      String mimeType2 =
	HeaderUtil.getMimeTypeFromContentType(contentType);
      if (log.isDebug2()) {
	log.debug2("CU: " + cu.getUrl() + " mime " + mimeType2);
      }
      Matcher match = null;
      if (pat != null) {
	match = pat.matcher(cu.getUrl());
      }
      if (mimeType.equalsIgnoreCase(mimeType2)
	  && (match == null || match.find())) {
	if (log.isDebug2()) log.debug2("Add " + cu.getUrl());
	al.add(cu);
      }
    }
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