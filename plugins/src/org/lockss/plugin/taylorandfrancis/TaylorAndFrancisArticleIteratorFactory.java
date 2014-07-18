/*
 * $Id: TaylorAndFrancisArticleIteratorFactory.java,v 1.9.2.1 2014-07-18 15:56:30 wkwilson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.atypon.BaseAtyponArticleIteratorFactory;
import org.lockss.util.Logger;


public class TaylorAndFrancisArticleIteratorFactory
extends BaseAtyponArticleIteratorFactory {

  protected static Logger log = Logger.getLogger("TaylorAndFrancisArticleIteratorFactory");
  private static final Pattern URL_UACP_HTML_PATTERN = Pattern.compile("/doi/full/([.0-9]+)/([^?&]+)_HTML/");

  // Override creation of builder to allow override of underlying createArticleFiles
  // to solve a T&F bug that was generating bogus URLS that looked like article files...
  @Override
  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
    return new SubTreeArticleIteratorBuilder(au) {
      @Override
      protected void maybeMakeSubTreeArticleIterator() {
        if (au != null && spec != null && iterator == null) { /// FIXME 1.63
          this.iterator = new BuildableSubTreeArticleIterator(au, spec) {
            @Override
            protected ArticleFiles createArticleFiles(CachedUrl cu) {
              // modify the returned Builder's createArticleFiles to ignore specific URLs
              if (cu.getUrl().contains("/null?")) { // 
                return null; // ignore these URLs
              }
              /* 
               * This one is to avoid iterating over all the contents of the 
               * subdirectories created for full text HTML for the TRIGGERED
               * website created for UACP and WJPT journals ONLY
               * This website didn't exactly match the usual T&F patterns
               * and doing it here avoids modifying the full Atypon PATTERN
               * to exclude directories that match "doi/full/<prefix>/<suffix>_HTML/"
               */
              Matcher UrlMat = URL_UACP_HTML_PATTERN.matcher(cu.getUrl());
              if (UrlMat.find()) {
                return null; // ignore these URLs
              }
              return super.createArticleFiles(cu); // normal processing
            }
          };
        }
      }
    };
  }

}

