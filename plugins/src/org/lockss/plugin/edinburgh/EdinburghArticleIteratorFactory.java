/*
 * $Id: EdinburghArticleIteratorFactory.java,v 1.1 2010-05-24 19:13:27 edwardsb1 Exp $
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
package org.lockss.plugin.edinburgh;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * @author edwardsb
 *
 */
public class EdinburghArticleIteratorFactory implements ArticleIteratorFactory {
  static Logger log = Logger.getLogger("EdinburghArticleIteratorFactory");

  /**
   * The URLs for all of the .pdfs that I've found have been of the forms:
   * 
   * "http://www.euppublishing.com/doi/pdfplus/10.3366/E0305149809000455", 
   *   where doi/pdfplus/10.3366/ is constant (yes, even the 10.3366), and 
   *   only the last part of the URL changes.  (The most common)
   * "http://www.euppublishing.com/doi/pdfplus/10.3366/E135075240900017X",
   *   which shows that not all digits in the E are numeric.  (Blame the base-11
   *   checksum number)
   * "http://www.euppublishing.com/doi/pdfplus/10.3366/afr.2010.0002", for Africa (recent issue;
   *    earlier issues had the E# form.)
   * "http://www.euppublishing.com/doi/pdfplus/10.3366/abib.2008.4", for Africa Bibliography 
   *    (which shows that the name is not always three letters long, and the article number is
   *    not always four digits long.) 
   */
  
  protected String subTreeRoot = "doi/pdfplus/10.3366/";
  protected Pattern pat = Pattern.compile("(E[0-9X]+)|([a-z]+.[0-9]+.[0-9]+)",
                                  Pattern.CASE_INSENSITIVE);

  /* (non-Javadoc)
   * @see org.lockss.plugin.ArticleIteratorFactory#createArticleIterator(java.lang.String, org.lockss.plugin.ArchivalUnit)
   */
  public Iterator createArticleIterator(String mimeType, ArchivalUnit au)
      throws PluginException {
    log.debug("createArticleIterator(" + mimeType + "," + au.toString() +
        ") " + subTreeRoot);
    return new SubTreeArticleIterator(mimeType, au, subTreeRoot, pat);
  }

}
