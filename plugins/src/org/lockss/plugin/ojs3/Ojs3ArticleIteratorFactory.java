/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs3;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.lexer.InputStreamSource;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.lexer.Stream;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.util.Logger;

/*
 * HTML Abstract: https://scholarworks.iu.edu/journals/index.php/jedhe/article/view/19369
 * PDF Landing: https://scholarworks.iu.edu/journals/index.php/jedhe/article/view/19369/28943
 * PDF Full Text: https://scholarworks.iu.edu/journals/index.php/jedhe/article/download/19369/28943
 * <meta name="citation_pdf_url" content="https://scholarworks.iu.edu/journals/index.php/jedhe/article/download/19369/28943">
 *
 * Special case:
 * Manifest page: https://scholarworks.iu.edu/journals/index.php/tmr/gateway/clockss?year=2021
 * Issue page: https://scholarworks.iu.edu/journals/index.php/tmr/issue/view/2078
 * Article urls, no PDF, only text:
 * https://scholarworks.iu.edu/journals/index.php/tmr/article/view/31853/31853
 * https://scholarworks.iu.edu/journals/index.php/tmr/article/view/31978/35871
 * https://scholarworks.iu.edu/journals/index.php/tmr/article/view/31979/35872
 * https://scholarworks.iu.edu/journals/index.php/tmr/article/view/32050/23
 * https://scholarworks.iu.edu/journals/index.php/tmr/article/view/32052/30
 *
 * Speical case:
 * Manifest page: https://scholarworks.iu.edu/journals/index.php/psource/gateway/clockss?year=2011
 * Issue page: https://scholarworks.iu.edu/journals/index.php/psource/issue/view/1253
 * No articles on the issue page
 */

public class Ojs3ArticleIteratorFactory implements ArticleIteratorFactory,
               					     ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(Ojs3ArticleIteratorFactory.class);

  protected static final String PATTERN_TEMPLATE = "\".*/article/view/[^/]+$\"";
  protected static Pattern ABSTRACT_PATTERN = Pattern.compile("article/view/[^/]+$", Pattern.CASE_INSENSITIVE);
  protected static String PUB_ID = "";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    PUB_ID = getPudIdFromAu(au);
    return new CitationArticleIterator(au,
        new SubTreeArticleIterator.Spec()
            .setTarget(target)
            .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected String getPudIdFromAu(ArchivalUnit au) {
    String pubIdParam = "&publicationId=";
    String risCitationParam = "citationstylelanguage/download/ris?submissionId=";
    for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
      String cuUrl = cu.getUrl();
      if (cuUrl.contains(risCitationParam)) {
        if (cuUrl.contains(pubIdParam)) {
          // return the param and the value, we just need it for this url construction
          return cuUrl.substring(cuUrl.lastIndexOf(pubIdParam));
        }
        return "";
      }
    }
    return "";
  }

  protected static class CitationArticleIterator extends SubTreeArticleIterator {
   
    public CitationArticleIterator(ArchivalUnit au,
                                 SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {
          return processAbstract(cu, mat);
      } else {
        log.warning("Mismatch between article iterator factory and article iterator: " + url);
      }
      return null;
    }
    
    /* 
     * In order to find full text PDF you need to find the citation_pdf_url meta tag in the
     * abstract html pull out the pdf url and find the matching cached url
     * The PDF landing page is a variant of the pdf url
     */
    protected ArticleFiles processAbstract(CachedUrl absCu, Matcher absMat) {
      NodeList pdfnl = null;
      NodeList htmlnl = null;
      NodeList xmlnl = null;
      NodeList epubnl = null;
      NodeList wordnl = null;
      ArticleFiles af = new ArticleFiles();
      if (absCu != null && absCu.hasContent()) {
        // set absCU as default full text CU in case there is
        af.setFullTextCu(absCu);
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);

        if (absCu.getUrl().contains("scholarworks.iu.edu")) {
          return af;
        }

        // now find the PDF from the meta tags on the abstract page
        try {
          pdfnl = getNodesFromAbstract(absCu, MetaTagNameNodeFilter("citation_pdf_url"));
          htmlnl = getNodesFromAbstract(absCu, MetaTagNameNodeFilter("citation_fulltext_html_url"));
          xmlnl = getNodesFromAbstract(absCu, FileLinkNodeFilter("xml"));
          epubnl = getNodesFromAbstract(absCu, FileLinkNodeFilter("epub"));
          // there is rarely a word document of the article
          wordnl =  getNodesFromAbstract(absCu, FileLinkNodeFilter("word"));
        } catch(ParserException e) {
          log.debug("Unable to parse abstract page html", e);
        } catch(UnsupportedEncodingException e) {
          log.debug("Bad encoding in abstract page html", e);
        } finally {
          absCu.release();
        }
      }
      // process the nodelists that were found.
      processNodes(af, pdfnl, ArticleFiles.ROLE_FULL_TEXT_PDF, true, true, ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
      processNodes(af, htmlnl, ArticleFiles.ROLE_FULL_TEXT_HTML, true, true, ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);
      processNodes(af, xmlnl, ArticleFiles.ROLE_FULL_TEXT_XML, false, false, null);
      processNodes(af, epubnl, ArticleFiles.ROLE_FULL_TEXT_EPUB, false, false, null);
      processNodes(af, wordnl, "FullTextWord", false, false, null);
      setCitationFiles(af, absCu.getUrl());
      return af;
    }

    protected final NodeList getNodesFromAbstract(CachedUrl absCu,
                                                  NodeFilter nf
    ) throws ParserException, UnsupportedEncodingException {
      InputStreamSource is = new InputStreamSource(new Stream(absCu.getUnfilteredInputStream()));
      Page pg = new Page(is);
      Lexer lx = new Lexer(pg);
      Parser parser = new Parser(lx);
      Lexer.STRICT_REMARKS = false;
      return parser.extractAllNodesThatMatch(nf);
    }

    protected final NodeFilter MetaTagNameNodeFilter(String metaTagName) {
      NodeFilter nf = node -> {
        if (!(node instanceof MetaTag)) return false;
        MetaTag meta = (MetaTag) node;
        if (!metaTagName.equalsIgnoreCase(meta.getMetaTagName())) return false;
        return true;
      };
      return nf;
    }

    protected final NodeFilter FileLinkNodeFilter(String fileType) {
      NodeFilter nf = node -> {
        if (!(node instanceof LinkTag)) return false;
        LinkTag aTag = (LinkTag) node;
        String aClass = aTag.getAttribute("class");
        if (aClass == null) return false;
        if (aClass.contains("obj_galley_link file") ||
            aClass.matches("galley-link.*file") ) {
          String tagText = aTag.getLinkText();
          if (tagText != null) {
            return tagText.toLowerCase().contains(fileType);
          }
        }
        return false;
      };
      return nf;
    }

    protected void processNodes(ArticleFiles af,
                                NodeList nl,
                                String role,
                                boolean setToFullTextCu,
                                boolean convertToLandingPage,
                                String landingRole) {
      try{
        if(nl != null) {
          // there may be more than one of any aspect.
          // e.g. a spanish and an english pdf file.
          // the first one is usually' the default of that journal
          // so we just use the first one.
          if (nl.size() > 0) {
            String urlStr = null;
            if (nl.elementAt(0) instanceof MetaTag) {
              urlStr = ((MetaTag) nl.elementAt(0)).getMetaContent();
            } else if (nl.elementAt(0) instanceof LinkTag) {
              urlStr = ((LinkTag) nl.elementAt(0)).getLink();
            } else {
              log.debug("node was an unexpected type.");
            }
            if (urlStr != null) {
              log.debug3(role + " is " + urlStr);
              CachedUrl cu = au.makeCachedUrl(urlStr);
              if (cu != null && cu.hasContent()) {
                // replace the fulltext with this cu if exists and has content
                if (setToFullTextCu) {
                  af.setFullTextCu(cu);
                }
                af.setRoleCu(role, cu);
              }
              // Now try for the PDF landing page which is the same as the PDF
              // but with "download" turned to "view"
              if (convertToLandingPage) {
                String landingStr = urlStr.replace("download/", "view/");
                cu = au.makeCachedUrl(landingStr);
                if (cu != null && cu.hasContent()) {
                  // replace absCU with landCu if exists and has content
                  af.setRoleCu(landingRole, cu);
                }
              }
            }
          }
        }
      } catch (IllegalArgumentException e) {
        log.debug("Badly formatted url link for " + role, e);
      }
    }

    protected final void setCitationFiles(ArticleFiles af,
                                          String absUrl) {
      // https://www.aijournals.com/index.php/ajmr/article/view/6664
      // https://www.aijournals.com/index.php/ajmr/citationstylelanguage/download/bibtex?submissionId=6664
      // https://www.aijournals.com/index.php/ajmr/citationstylelanguage/download/ris?submissionId=6664
      String risUrl = absUrl.replace(
          "article/view/",
          "citationstylelanguage/download/ris?submissionId="
      );
      String bibtexUrl = absUrl.replace(
          "article/view/",
          "citationstylelanguage/download/bibtex?submissionId="
      );
      risUrl += PUB_ID;
      bibtexUrl += PUB_ID;
      CachedUrl cu = au.makeCachedUrl(risUrl);
      if (cu != null && cu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_CITATION_RIS, cu);
      }
      cu = au.makeCachedUrl(bibtexUrl);
      if (cu != null && cu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_CITATION_BIBTEX, cu);
      }
    }
  }

  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ABSTRACT);
  }
}
