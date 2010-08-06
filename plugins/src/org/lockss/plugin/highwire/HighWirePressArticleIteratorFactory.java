/*
 * $Id: HighWirePressArticleIteratorFactory.java,v 1.2 2010-08-06 11:04:39 thib_gc Exp $
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
import java.util.*;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class HighWirePressArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("HighWirePressArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE_HTML = "\"%scgi/content/full/%s/\", base_url, volume_name";
  
  protected static final String OLD_ROOT_TEMPLATE_HTML = "\"%scgi/content/full/%d/\", base_url, volume";
  
  protected static final String ROOT_TEMPLATE_PDF = "\"%scgi/reprint/%s/\", base_url, volume_name";
  
  protected static final String OLD_ROOT_TEMPLATE_PDF = "\"%scgi/reprint/%d/\", base_url, volume";
  
  protected static final String PATTERN_TEMPLATE = "\"^%scgi/(content/full/([^/]+;)?%s/[^/]+/[^/]+|reprint/([^/]+;)?%s/[^/]+/[^/]+\\.pdf)$\", base_url, volume_name, volume_name";

  protected static final String OLD_PATTERN_TEMPLATE = "\"^%scgi/(content/full/([^/]+;)?%d/[^/]+/[^/]+|reprint/([^/]+;)?%d/[^/]+/[^/]+\\.pdf)$\", base_url, volume, volume";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    List<String> rootTemplates = new ArrayList<String>(2);
    if ("org.lockss.plugin.highwire.HighWirePlugin".equals(au.getPluginId())) {
      rootTemplates.add(OLD_ROOT_TEMPLATE_HTML);
      rootTemplates.add(OLD_ROOT_TEMPLATE_PDF);
      return new HighWirePressArticleIterator(au, new SubTreeArticleIterator.Spec()
                                                  .setTarget(target)
                                                  .setRootTemplates(rootTemplates)
                                                  .setPatternTemplate(OLD_PATTERN_TEMPLATE));
    }
    rootTemplates.add(ROOT_TEMPLATE_HTML);
    rootTemplates.add(ROOT_TEMPLATE_PDF);
    return new HighWirePressArticleIterator(au, new SubTreeArticleIterator.Spec()
                                                .setTarget(target)
                                                .setRootTemplates(rootTemplates)
                                                .setPatternTemplate(PATTERN_TEMPLATE));
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new HighWirePressArticleMetadataExtractor();
  }
  
  protected static class HighWirePressArticleIterator extends SubTreeArticleIterator {
    
    protected static Pattern HTML_PATTERN = Pattern.compile("/cgi/content/full/([^/]+;)?([^/]+/[^/]+/[^/]+)$", Pattern.CASE_INSENSITIVE);
    
    protected static Pattern PDF_PATTERN = Pattern.compile("/cgi/reprint/([^/]+;)?([^/]+/[^/]+/[^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    
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
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, htmlCu);
      guessFullTextPdf(af, htmlMat);
      guessOtherParts(af, htmlMat);
      return af;
    }
    
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      if (pdfMat.group(1) != null) {
        CachedUrl altPdfCu = au.makeCachedUrl(pdfMat.replaceFirst("/cgi/reprint/$2.pdf"));
        if (altPdfCu != null && altPdfCu.hasContent()) {
          return null;
        }
        CachedUrl altHtmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/cgi/content/full/$1$2"));
        if (altHtmlCu != null && altHtmlCu.hasContent()) {
          return null;
        }
      }
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/cgi/content/full/$2"));
      if (htmlCu != null && htmlCu.hasContent()) {
        return null;
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
      guessReferences(af, mat);
      guessSupplementaryMaterials(af, mat);
    }
    
    protected void guessFullTextPdf(ArticleFiles af, Matcher htmlMat) {
      CachedUrl pdfCu = null;
      if (htmlMat.group(1) != null) {
        pdfCu = au.makeCachedUrl(htmlMat.replaceFirst("/cgi/reprint/$1$2.pdf"));
      }
      if (pdfCu == null || !pdfCu.hasContent()) {
        pdfCu = au.makeCachedUrl(htmlMat.replaceFirst("/cgi/reprint/$2.pdf"));
      }
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
        
        String[] successiveAttempts = new String[] {
            "/cgi/reprint/$1$2",
            "/cgi/reprint/$2",
            "/cgi/reprintframed/$1$2",
            "/cgi/reprintframed/$2",
            "/cgi/framedreprint/$1$2",
            "/cgi/framedreprint/$2",
        };
        for (String repl : successiveAttempts) {
          if (repl.contains("$1") && htmlMat.group(1) == null) {
            continue;
          }
          CachedUrl pdfLandCu = au.makeCachedUrl(htmlMat.replaceFirst(repl));
          if (pdfLandCu != null && pdfLandCu.hasContent()) {
            af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, pdfLandCu);
            if (af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA) == null) {
              af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, pdfLandCu);
            }
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
    
    protected void guessReferences(ArticleFiles af, Matcher mat) {
      CachedUrl refsCu = au.makeCachedUrl(mat.replaceFirst("/cgi/content/refs/$2"));
      if (refsCu != null && refsCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, refsCu);
      }
      else if (mat.group(1) != null) {
        CachedUrl altRefsCu = au.makeCachedUrl(mat.replaceFirst("/cgi/content/refs/$1$2"));
        if (altRefsCu != null && altRefsCu.hasContent()) {
          af.setRoleCu(ArticleFiles.ROLE_REFERENCES, altRefsCu);
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
