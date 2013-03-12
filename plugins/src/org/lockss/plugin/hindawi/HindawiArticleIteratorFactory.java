/*
 * $Id: HindawiArticleIteratorFactory.java,v 1.1 2013-03-12 22:32:20 aishizaki Exp $

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.hindawi;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;

public class HindawiArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log =
    Logger.getLogger("HindawiArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE =
    "\"%s\", base_url";
  protected static final String DOWNLOAD_ROOT_TEMPLATE =
    "\"%s\", download_url";
  // the pattern of typical url to send to the article iterator
  //http://downloads.hindawi.com/journals/ahci/2008/145363.pdf
  protected static final String PPATTERN_TEMPLATE =
    "\"%sjournals/%s/%s/[\\d]+\\.pdf\", download_url, journal_id, volume_name";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new
      HindawiArticleIterator(au,
              new SubTreeArticleIterator.Spec()
              .setTarget(target)
              .setRootTemplates(ListUtil.list(DOWNLOAD_ROOT_TEMPLATE, ROOT_TEMPLATE))
              .setPatternTemplate(PPATTERN_TEMPLATE));
  }

  protected static class HindawiArticleIterator
    extends SubTreeArticleIterator {
    
    protected String BASE_ROOT = String.format("%s", 
                au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()));
    protected static Pattern PDF_PATTERN =
      Pattern.compile("(http://downloads.hindawi.com/)(journals/[\\w]+/[\\d]{4}/[\\d]+).pdf", Pattern.CASE_INSENSITIVE);
    protected static Pattern HTML_PATTERN =
      Pattern.compile("(http://www.hindawi.com/)(journals/[\\w]+/[\\d]{4}/[\\d]+)$", Pattern.CASE_INSENSITIVE);
    protected static Pattern ABSTRACT_PATTERN =
      Pattern.compile("(http://www.hindawi.com/)(journals/)([\\w]+/[\\d]{4}/[\\d]+)/abs$", Pattern.CASE_INSENSITIVE);
    
    public HindawiArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    /*
     * (non-Javadoc)
     * @see org.lockss.plugin.SubTreeArticleIterator#createArticleFiles(org.lockss.plugin.CachedUrl)
     *   set the pattern to find article PDF files, then surmise the HTML and Abstracts
     *   files and set the appropriate Roles
     */
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;

      mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextPdf(cu, mat);
      }

      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles guessHtml(ArticleFiles af, Matcher mat) {
      String init = mat.replaceFirst(String.format("%s$2", BASE_ROOT));
      CachedUrl htmlCu = au.makeCachedUrl(init);
      log.debug3("guessHtml("+htmlCu+")");
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      AuUtil.safeRelease(htmlCu);

      return af;
    }
    protected ArticleFiles guessAbs(ArticleFiles af, Matcher mat) {
      String init = mat.replaceFirst(String.format("%s$2/abs", BASE_ROOT));
      CachedUrl cu = au.makeCachedUrl(init);
      
      log.debug3("guessAbs("+cu+")");
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
      AuUtil.safeRelease(cu);

      return af;
    }
    protected ArticleFiles processFullTextPdf(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
      af.setFullTextCu(cu);
      // only set roles when getting metadata, not creating article list
      if(spec.getTarget() != null && !(spec.getTarget().isArticle())){
        guessAbs(af, mat);
        guessHtml(af, mat);
        af.setFullTextCu(cu);
      }
      return af;   
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
