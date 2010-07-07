/*
 * $Id: HighWirePressArticleIteratorFactory.java,v 1.1 2010-07-07 23:22:10 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HighWirePressArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("HighWirePressH20ArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%scgi/content/\", base_url";
  
  protected static final String PATTERN_TEMPLATE = "\"^%scgi/content/(full/([^/]+;)?%s/[^/]+/[^/]+|reprint/([^/]+;)?%s/[^/]+/[^/]+\\.pdf)$\", base_url, volume_name, volume_name";

  protected static final String PATTERN_TEMPLATE_OLD = "\"^%scgi/content/(full/([^/]+;)?%d/[^/]+/[^/]+|reprint/([^/]+;)?%d/[^/]+/[^/]+\\.pdf)$\", base_url, volume, volume";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new HighWirePressArticleIterator(au, new SubTreeArticleIterator.Spec()
                                                .setTarget(target)
                                                .setRootTemplate(ROOT_TEMPLATE)
                                                .setPatternTemplate("org.lockss.plugin.highwire.HighWirePlugin".equals(au.getPluginId())
                                                                    ? PATTERN_TEMPLATE_OLD
                                                                    : PATTERN_TEMPLATE));
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new HighWirePressArticleMetadataExtractor();
  }
  
  protected static class HighWirePressArticleIterator extends SubTreeArticleIterator {
    
    protected static Pattern HTML_PATTERN = Pattern.compile("/cgi/content/full/([^/]+;)?([^/]+/[^/]+/[^/]+)$", Pattern.CASE_INSENSITIVE);
    
    protected static Pattern PDF_PATTERN = Pattern.compile("/cgi/content/reprint/([^/]+;)?([^/]+/[^/]+/[^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    
    public HighWirePressArticleIterator(ArchivalUnit au,
                                        SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      
      mat = HTML_PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextHtml(cu, mat);
      }
        
      mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextPdf(cu, mat);
      }
      
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu, Matcher htmlMat) {
      if (htmlMat.group(1) != null) {
        CachedUrl altCu = au.makeCachedUrl(htmlMat.replaceFirst("/cgi/content/full/$2"));
        if (altCu != null && altCu.hasContent()) {
          return null;
        }
      }
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      guessFullTextPdf(af, htmlMat);
      guessOtherParts(af, htmlMat);
      return af;
    }
    
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/cgi/content/full/$2"));
      if (htmlCu != null && htmlCu.hasContent()) {
        return null;
      }
      if (pdfMat.group(1) != null) {
        CachedUrl altCu = au.makeCachedUrl(pdfMat.replaceFirst("/cgi/content/full/$1$2"));
        if (altCu != null && altCu.hasContent()) {
          return null;
        }
      }
      
      ArticleFiles af = new ArticleFiles();
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);

      String[] successiveAttempts = new String[] {
          "/cgi/reprint/$2",
          "/cgi/reprint/$1$2",
          "/cgi/reprintframed/$2",
          "/cgi/reprintframed/$1$2",
          "/cgi/framedreprint/$2",
          "/cgi/framedreprint/$1$2",
      };
      for (String repl : successiveAttempts) {
        if (repl.contains("$1") && pdfMat.group(1) == null) {
          continue;
        }
        CachedUrl pdfLandCu = au.makeCachedUrl(pdfMat.replaceFirst(repl));
        if (pdfLandCu != null && pdfLandCu.hasContent()) {
          af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, pdfLandCu);
          af.setFullTextCu(pdfLandCu);
          break;
        }
      }
      if (af.getFullTextCu() == null) {
        af.setFullTextCu(pdfCu);
      }
      guessOtherParts(af, pdfMat);
      return af;
    }
    
    protected void guessOtherParts(ArticleFiles af, Matcher mat) {
      guessAbstract(af, mat);
      guessSupplementaryMaterials(af, mat);
    }
    
    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/cgi/content/reprint/$1.pdf"));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
        
        String[] successiveAttempts = new String[] {
            "/cgi/reprint/$2",
            "/cgi/reprint/$1$2",
            "/cgi/reprintframed/$2",
            "/cgi/reprintframed/$1$2",
            "/cgi/framedreprint/$2",
            "/cgi/framedreprint/$1$2",
        };
        for (String repl : successiveAttempts) {
          if (repl.contains("$1") && mat.group(1) == null) {
            continue;
          }
          CachedUrl pdfLandCu = au.makeCachedUrl(mat.replaceFirst(repl));
          if (pdfLandCu != null && pdfLandCu.hasContent()) {
            af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, pdfLandCu);
            break;
          }
        }
      }
    }

    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/cgi/content/abstract/$2"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
      }
      else if (mat.group(1) != null) {
        CachedUrl altAbsCu = au.makeCachedUrl(mat.replaceFirst("/cgi/content/abstract/$1$2"));
        if (altAbsCu != null && altAbsCu.hasContent()) {
          af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, altAbsCu);
        }
      }
    }
    
    protected void guessSupplementaryMaterials(ArticleFiles af, Matcher mat) {
      CachedUrl suppinfoCu = au.makeCachedUrl(mat.replaceFirst("/cgi/content/full/$2/DC1"));
      if (suppinfoCu != null && suppinfoCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, suppinfoCu);
      }
      else if (mat.group(1) != null) {
        CachedUrl altSuppinfoCu = au.makeCachedUrl(mat.replaceFirst("/cgi/content/full/$1$2/DC1"));
        if (altSuppinfoCu != null && altSuppinfoCu.hasContent()) {
          af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, altSuppinfoCu);
        }
      }
    }
   
  }

  protected static class HighWirePressArticleMetadataExtractor implements ArticleMetadataExtractor {

    public ArticleMetadata extract(ArticleFiles af) throws IOException, PluginException {
      String url = af.getFullTextUrl();
      ArticleMetadata am = new ArticleMetadata();
      am.put(ArticleMetadata.KEY_ACCESS_URL, url);
      return am;
    }

  }

}
