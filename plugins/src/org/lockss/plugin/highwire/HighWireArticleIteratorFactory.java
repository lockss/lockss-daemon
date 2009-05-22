/*
 * $Id: HighWireArticleIteratorFactory.java,v 1.2 2009-05-22 19:14:54 dshr Exp $
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

package org.lockss.plugin.highwire;

import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.daemon.PluginException;

public class HighWireArticleIteratorFactory implements ArticleIteratorFactory {
  static String defaultSubTreeRoot = "cgi/reprint";
  static Logger log = Logger.getLogger("HighWireArticleIterator");

  public HighWireArticleIteratorFactory() {
  }
  /**
   * Create an Iterator that iterates through the AU's articles, pointing
   * to the appropriate CachedUrl of type mimeType for each, or to the plugin's
   * choice of CachedUrl if mimeType is null
   * @param mimeType the MIME type desired for the CachedUrls
   * @param au the ArchivalUnit to iterate through
   * @return the ArticleIterator
   */
  public Iterator createArticleIterator(String mimeType, ArchivalUnit au)
    throws PluginException {
      log.debug("createArticleIterator(" + mimeType + "," + au.toString() +
		") " + defaultSubTreeRoot);
      return new ArticleIterator(mimeType, au, defaultSubTreeRoot);
  }
  public void setSubTreeRoot(String root) {
    defaultSubTreeRoot = root;
    log.debug("Set subTreeRoot: " + defaultSubTreeRoot);
  }
    /*
     * The HighWire URL structure means that the HTML for an article
     * is at a URL like http://apr.sagepub.com/cgi/reprint/34/2/135
     */
    private static class ArticleIterator implements Iterator {
	String mimeType;
	ArchivalUnit au;
	Iterator it = null;
	ArrayList al = new ArrayList();
	String subTreeRoot = null;
	ArticleIterator(String mimeType, ArchivalUnit au,
			String subTreeRoot) {
	    this.mimeType = ( mimeType == null ? "text/html" : mimeType );
	    this.au = au;
	    this.subTreeRoot = subTreeRoot;
	    log.debug("Mime " + this.mimeType + " subTree " + this.subTreeRoot +
		      " au " + this.au.toString());
	}
	private void makeIterator() {
	    Collection stems = au.getUrlStems();
	    for (Iterator it = stems.iterator(); it.hasNext(); ) {
		String stem = (String) it.next() + "/";
		log.debug("Subtree: " + stem + " + " + subTreeRoot);
		CachedUrlSetSpec cuss =
		    new RangeCachedUrlSetSpec(stem + subTreeRoot);
		CachedUrlSet cus = au.makeCachedUrlSet(cuss);
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
}
