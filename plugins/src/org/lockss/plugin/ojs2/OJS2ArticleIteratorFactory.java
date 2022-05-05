/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.ojs2;

import java.io.*;

import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.*;

import org.htmlparser.*;
import org.htmlparser.lexer.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.util.*;


/*
 * OJS2ArticleIterator processes the table of contents collected for
 * an AU (Archival Unit).  It chooses a full text content, then
 * outputs these full text URLs.
 * 
 * OJS2 issues page or the table of contents typically has the following
 * URL's format:
 * 
 * Issues page or TOC URL:
 * <base_url/>index.php/<journal_id>/issue/view/<[^/]+>
 * http://www.ojs2articleiteratortest.com/index.php/lq/issue/view/478
 * 
 * Articles URL:
 * <base_url>/index.php/<journal_id>/article/view/<[^/]+>/<[^/]+>
 * http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110/8514
 */

public class OJS2ArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  public static class Role {
    public static final String FULL_TEXT_EPUB = "FullTextEpub";
    public static final String SOURCE_XML = "SourceXml";
  }
  
  protected static Logger log = Logger.getLogger(OJS2ArticleIteratorFactory.class);
  protected static Pattern SHOW_TOC_PATTERN =
      Pattern.compile("/issue/view/([^/]+)/showToc$", Pattern.CASE_INSENSITIVE);
  protected static Pattern PLAIN_TOC_PATTERN =
      Pattern.compile("/issue/view/([^/]+)$", Pattern.CASE_INSENSITIVE);
  protected static Pattern JLA_PDF_LABEL_PATTERN =
      Pattern.compile("^[0-9]+[.] \\[pdf\\]$");
  
  // params from tdb file corresponding to AU
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%s(index[.]php/)?(%s/)?article/(viewFile|download)/[^/]+/[^/?]+$\", base_url, journal_id";
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/article/(?:viewFile|download)/([^/]+)/([^/]+)$",
      Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String PDF_REPLACEMENT1 = "/article/viewFile/$1/$2";
  protected static final String PDF_REPLACEMENT2 = "/article/download/$1/$2";
  // NOTE: 
  // http://e-revista.unioeste.br/index.php/ccsaemperspectiva/article/view/5545/5248
  // was not a fulltext view, but just a PDF instruction page
  // so if that form of URL is needed, then that could mean a problem
  // XXX protected static final String PDF_REPLACEMENT3 = "/article/view/$1/$2";
  protected static final String ABSTRACT_REPLACEMENT = "/article/view/$1";

  // CITATION files
  //        https://www.afrjournal.org/index.php/afr/article/view/356
  // ris    https://www.afrjournal.org/index.php/afr/rt/captureCite/356/0/ProCiteCitationPlugin
  // bibtex https://www.afrjournal.org/index.php/afr/rt/captureCite/356/0/BibtexCitationPlugin
  protected static final String RIS_REPLACEMENT = "/rt/captureCite/$1/0/ProCiteCitationPlugin";
  protected static final String BIBTEX_REPLACEMENT = "/rt/captureCite/$1/0/BibtexCitationPlugin";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String journal_id = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());

    // Added possible cases to roots list to cover optional 'index.php'
    // and journal_id
    // e.g., Ubiquity Press has neither index.php nor journal_id
    // http://www.ancient-asia-journal.com/article/view/aa.10202/33
    // Biologic Institute has both index.php and journal_id (main)
    // http://bio-complexity.org/ojs/index.php/main/article/view/BIO-C.2011.3
    List<String> roots = ListUtil.list(
        String.format("%sindex.php/issue/view", base_url),
        String.format("%sissue/view", base_url),
        String.format("%sindex.php/%s/issue/view", base_url, journal_id),
        String.format("%s%s/issue/view", base_url, journal_id)
        );
    if (!journal_id.toLowerCase().equals(journal_id)) {
      roots.add(String.format("%sindex.php/%s/issue/view", base_url, journal_id.toLowerCase()));
      roots.add(String.format("%s%s/issue/view", base_url, journal_id.toLowerCase()));
    }
    if (!journal_id.toUpperCase().equals(journal_id)) {
      roots.add(String.format("%sindex.php/%s/issue/view", base_url, journal_id.toUpperCase()));
      roots.add(String.format("%s%s/issue/view", base_url, journal_id.toUpperCase()));
    }
    
    Iterator<ArticleFiles> ai = new OJS2ArticleIterator(au,
        new SubTreeArticleIterator.Spec().setTarget(target).setRoots(roots),
        target);
    if (ai.hasNext()) {
      return ai;
    }
    
    // otherwise try SubTreeArticleIteratorBuilder 
    log.debug3("Using SubTreeArticleIteratorBuilder");
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up pdf to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, Arrays.asList(PDF_REPLACEMENT1, PDF_REPLACEMENT2),
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up abstract to be an aspect
    builder.addAspect(
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);
    builder.addAspect(
        BIBTEX_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_BIBTEX);
    
    return builder.getSubTreeArticleIterator();
  } // createArticleIterator
  
  protected static class OJS2ArticleIterator extends SubTreeArticleIterator {
    
    protected MetadataTarget target;
    protected Set<String> alreadyEmitted;
    protected ArticleFiles nextAF = null;
    protected boolean checkedOnce = false;
    
    // Constructor
    public OJS2ArticleIterator(ArchivalUnit au,
                               SubTreeArticleIterator.Spec spec,
                               MetadataTarget target) {
      super(au, spec);
      this.target = target;
      this.alreadyEmitted = new HashSet<String>();
    }
    
    @Override
    public boolean hasNext() {
      if (checkedOnce) {
        return super.hasNext();
      }
      checkedOnce = true;
      ArticleFiles tmp = null;
      try {
        tmp = super.next();
      }
      catch (NoSuchElementException nse) {
        // do nothing
      }
      if(tmp == null){
        return false;
      }
      nextAF = tmp;
      return true;
    }
    
    @Override
    public ArticleFiles next() {
      if (nextAF != null) {
        ArticleFiles tmp = nextAF;
        nextAF = null;
        return tmp;
      }
      return super.next();
    }

    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;

      mat = SHOW_TOC_PATTERN.matcher(url);
      if (mat.find()) {
        processShowToc(cu, mat);
        return null;
      }

      mat = PLAIN_TOC_PATTERN.matcher(url);
      if (mat.find()) {
        processPlainToc(cu, mat);
        return null;
      }

      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
      
    }
    
    protected void processShowToc(CachedUrl tocCu, Matcher tocMat) {
      processToc(tocCu.getUnfilteredInputStream(), tocCu.getEncoding(), tocCu.getUrl());
    }
    
    protected void processPlainToc(CachedUrl tocCu, Matcher tocMat) {
      CachedUrl showCu = au.makeCachedUrl(tocMat.replaceFirst("/issue/view/$1/showToc"));
      if (showCu != null && showCu.hasContent()) {
        AuUtil.safeRelease(showCu);
        return;
      }
      processToc(tocCu.getUnfilteredInputStream(), tocCu.getEncoding(), tocCu.getUrl());
    }
    
    protected void processToc(InputStream in, String encoding, String url) {
      
      try {
        Lexer.STRICT_REMARKS = false; // Accept common variants of HTML comments
        InputStreamSource source = new InputStreamSource(in, encoding);
        Parser parser = new Parser(new Lexer(new Page(source)), new LoggerAdapter(url));
        NodeList nodeList = parser.extractAllNodesThatMatch(HtmlNodeFilters.tagWithAttribute("table", "class", "tocArticle"));
        SimpleNodeIterator iter = nodeList.elements();
        
        log.debug3("Processing articles");
        while (iter.hasMoreNodes()) {
          log.debug3("Processing one article");
          processArticle(iter.nextNode());
        }
      }
      catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      catch (ParserException e) {
        e.printStackTrace();
      }
      
    }

    protected void processArticle(Node node) {
      Map<String, CachedUrl> map = new HashMap<String, CachedUrl>();
      NodeList links = new NodeList();
      node.collectInto(links, HtmlNodeFilters.tagWithAttribute("a", "href"));
      SimpleNodeIterator iter = links.elements();
      boolean jla = false;
      
      while (iter.hasMoreNodes()) {
        LinkTag link = (LinkTag)iter.nextNode();
        CachedUrl linkCu = null;
        
        String url = link.extractLink();
        try {
          // Need to normalizeUrl() for HttpHttps
          if (au != null) {
            try {
              url = new BaseUrlHttpHttpsUrlNormalizer().normalizeUrl(url, au);
            } catch (PluginException pe) {
              log.debug3("Could not create instance of BaseUrlHttpHttpsUrlNormalizer", pe);
            }
          }
          linkCu = au.makeCachedUrl(UrlUtil.normalizeUrl(url, au));
        }
        catch (PluginBehaviorException pbe) {
          log.debug3("Plugin behavior exception", pbe);
          continue; // Ignore
        }
        catch (MalformedURLException mue) {
          log.debug3("Malformed URL exception", mue);
          continue; // Ignore
        }
        
        if (linkCu != null) {
          if (linkCu.hasContent()) {
            String linkUrl = linkCu.getUrl();
            if (linkUrl.endsWith("/0")) {
              AuUtil.safeRelease(linkCu);
              continue; // Ignore (dupe)
            }
            String label = link.toPlainTextString().trim().toLowerCase();
            if (label == null || label.length() == 0) {
              label = link.getText().trim().toLowerCase();
            }
            if (JLA_PDF_LABEL_PATTERN.matcher(label).find()) {
              label = "pdf";
              jla = true;
            }
            if (!map.containsKey(label)) {
              log.debug3(label + " -> " + linkUrl);
              map.put(label, linkCu);
            }
          }
          else {
            AuUtil.safeRelease(linkCu);
          }
        }
      }
      
      ArticleFiles af = new ArticleFiles();
      guessAbstract(af, map);
      guessFullTextHtml(af, map); // cover both label HTML and Full Text
      guessFullTextPdf(af, map);
      doGuess(af, map, "epub", Role.FULL_TEXT_EPUB, "Full-text EPUB");
      doGuess(af, map, "xml", Role.SOURCE_XML, "Source XML");
      setRoleArticleMetadata(af, jla);
      chooseFullTextCu(af);

      // Emit
      CachedUrl cu = af.getFullTextCu();
      if (cu != null) {
        log.debug3("getfulltextcu url: " + cu.getUrl());
        
        if (!alreadyEmitted.contains(cu.getUrl())) {
          alreadyEmitted.add(cu.getUrl());
          emitArticleFiles(af);
        }
      }
      
      // Clean up
      for (CachedUrl releaseCu : map.values()) {
        AuUtil.safeRelease(releaseCu);
      }
      
    }
    
    // Set role_article_metadata. Check if frame src url exists
    private void setRoleArticleMetadata(ArticleFiles af, boolean jla) {
      CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ABSTRACT);
      if ((cu == null) || !(cu.hasContent())) {
        cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML);
      }
      if (cu != null && cu.hasContent()) {
        String url = cu.getUrl();
        String frameSrcUrl = StringUtil.replaceFirst(url, "/view/", "/viewArticle/");
        log.debug3("setRoleArticleMetadata() : url: " + url);
        log.debug3("setRoleArticleMetadata() frameSrcUrl: " + frameSrcUrl);
        CachedUrl frameSrcCu = au.makeCachedUrl(frameSrcUrl);
        if ((frameSrcCu != null) && frameSrcCu.hasContent()) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, frameSrcCu);
          frameSrcCu.release();
        } else if (jla) {
          String jlaUrl = url.substring(0, url.lastIndexOf("/"));
          CachedUrl jlaCu = au.makeCachedUrl(jlaUrl);
          if ((jlaCu != null) && jlaCu.hasContent()) {
            af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, jlaCu);
            jlaCu.release();
          } else {
            af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
          }
        } else {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
        }
      } else {
        log.warning("setRoleArticleMetadata() cu is null");
      }
    }

    protected void guessAbstract(ArticleFiles af, Map<String, CachedUrl> map) {
      // Try labels
      CachedUrl cu = doGuessFromLabels(map,
                                       new String[] {
                                           "abstract",
                                           "r\u00e9sum\u00e9",
                                       });
      
      if (cu == null) {
        // Find a single undecorated link
        cu = doGuessSingleUrl(map, "/article/view/[^/]+$");
        if (cu == null) {
          // Alternatively, try a single full link
          cu = doGuessSingleUrl(map, "/article/view/[^/]+/[^/]+$");
        }         
      }  
    
      if (cu != null) {
        log.debug2("Abstract url: " + cu.getUrl());
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
        return;
      }
    
    }

    // This method handles full text urls with both labels 'HTML' or 'Full Text'
    protected void guessFullTextHtml(ArticleFiles af, Map<String, CachedUrl> map) {
      // Try labels
      CachedUrl cu = doGuessFromLabels(map,
                                       new String[] {
                                           "html",
                                           "full text",
                                       });
      
      if (cu != null && cu.hasContent()) {
        log.debug2("Full text html: " + cu.getUrl());
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, cu);
        return;
      }
      
    } // guessFullTextHtml
        
    protected void guessFullTextPdf(ArticleFiles af, Map<String, CachedUrl> map) {
      // Try labels
      CachedUrl cu = doGuessFromLabels(map,
                                       new String[] {
                                           "high-resolution pdf",
                                           "high resolution pdf",
                                           "high-res pdf",
                                           "high res pdf",
                                           "pdf",
                                       });
      if (cu != null) {
        log.debug2("Full-text PDF: " + cu.getUrl());
        guessPdfLanding(af, cu);
        return;
      }
      
      // Alternatively, find a single link decorated with correct suffix
      cu = doGuessSingleUrl(map, "/article/view/[^/]+/pdf(_[^/]+)?$");
      if (cu != null) {
        log.debug2("Full-text PDF candidate: " + cu.getUrl());
        guessPdfLanding(af, cu);
        return;
      }
      
      // Pick the largest "PDF (2.3MB)" (or whatever) label if applicable
      Pattern pat = Pattern.compile("^pdf *\\(([0-9]+(\\.[0-9]+)?) *[a-z]+\\)$", Pattern.CASE_INSENSITIVE);
      float largestFloat = 0.0f;
      
      for (String str : map.keySet()) {
        Matcher mat = pat.matcher(str);
        if (mat.find()) {
          try {
            float candidateFloat = Float.parseFloat(mat.group(1));
            if (largestFloat < candidateFloat) {
              largestFloat = candidateFloat;
              cu = map.get(str);
            }
          }
          catch (NumberFormatException nfe) {
            // Just move on
            log.debug3("Bad float conversion in: " + str);
          }
        } // if
      } // for
      
      if (cu != null) {
        log.debug2("Full-text PDF candidate: " + cu.getUrl());
        guessPdfLanding(af, cu);
        return;
      }
      
      // Still nothing? Try a single link that has a PDF variant
      cu = doGuessSingleUrl(map, "/article/view/([^/]+)/([^/]+)$");
      if (cu != null) {
        guessPdfLanding(af, cu);
        return;
      }
      
    } // guessFullTextPdf

    protected void guessPdfLanding(ArticleFiles af, CachedUrl cu) {
      
      try {
	if (cu.getContentType().startsWith("application/pdf")) {
	  af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
	  return;
	}
      } finally {
	AuUtil.safeRelease(cu);
      }
      
      Pattern pat = Pattern.compile("/article/view/([^/]+)/(pdf_)?([^/]+)$", Pattern.CASE_INSENSITIVE);
      Matcher mat = pat.matcher(cu.getUrl());
      
      if (mat.find()) {
        CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/article/view/$1/$3"));
        
        try {
          if (pdfCu != null && pdfCu.hasContent()) {
            if (pdfCu.getContentType().startsWith(Constants.MIME_TYPE_PDF)) {
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, cu);
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
            }
            else if (pdfCu.getContentType().startsWith("text")) {
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, pdfCu);
            }
          }
        } finally {
          AuUtil.safeRelease(pdfCu);
        }
        return;
      } // if

      // Ideally this doesn't happen
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
      
    } // guessPdfLanding
    
    protected void doGuess(ArticleFiles af,
                           Map<String, CachedUrl> map,
                           String label,
                           String role,
                           String debugWords) {
      
      // Find the right label
      CachedUrl labelCu = map.get(label);
      
      if (labelCu != null) {
        log.debug2(debugWords + ": " + labelCu.getUrl());
        af.setRoleCu(role, labelCu);
        return;
      }
      
      // Alternatively, find a single link decorated with correct suffix
      Pattern pat = Pattern.compile("/article/view/[^/]+/" + label + "(_[^/]+)?$", Pattern.CASE_INSENSITIVE);
      CachedUrl candidate = null;
      
      for (CachedUrl cu : map.values()) {
        if (pat.matcher(cu.getUrl()).find()) {
          if (candidate == null) {
            candidate = cu; // Remember candidate
          }
          else if (!candidate.getUrl().equalsIgnoreCase(cu.getUrl())) {
            log.debug3("Competing " + debugWords + "candidate: " + candidate.getUrl());
            candidate = null; // More than one candidate -- forget it
            break; // Bail
          }
        }
      } // for
      
      if (candidate != null) {
        log.debug2(debugWords + " candidate: " + candidate.getUrl());
        af.setRoleCu(role, candidate);
      }
      
    } // doGuess
      
    protected CachedUrl doGuessFromLabels(Map<String, CachedUrl> map,
                                         String[] labels) {
      for (String str : labels) {
        if (map.containsKey(str)) {
          CachedUrl cu = map.get(str);
          log.debug3("Found candidate: " + cu.getUrl());
          return cu;
        }
      }
      return null;
      
    } // doGuessFromLabels
    
    protected CachedUrl doGuessSingleUrl(Map<String, CachedUrl> map,
                                         String patternString) {
      CachedUrl candidate = null;
      Pattern pat = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
      
      for (CachedUrl cu : map.values()) {
        
        if (pat.matcher(cu.getUrl()).find()) {
          if (candidate == null) {
            log.debug3("Found candidate: " + cu.getUrl());
            candidate = cu; // Remember candidate
          }
          else if (!candidate.getUrl().equalsIgnoreCase(cu.getUrl())) {
            log.debug3("Competing candidate: " + cu.getUrl());
            return null; // Bail
          }
        }
      } // for
      
      if (candidate != null) {
        log.debug3("Final candidate: " + candidate.getUrl());
        return candidate;
      }
      return null; // No candidate found
      
    } // doGuessSingleUrl
    
    protected void chooseFullTextCu(ArticleFiles af) {
      
      final String[] ORDER = new String[] {
          ArticleFiles.ROLE_FULL_TEXT_HTML,
          ArticleFiles.ROLE_FULL_TEXT_PDF,
          ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
          ArticleFiles.ROLE_ABSTRACT,
      };
      
      for (String role : ORDER) {
        CachedUrl cu = af.getRoleCu(role);
        if (cu != null) {
          af.setFullTextCu(cu);
          return;
        }
      }
      
      log.debug2("No full-text CU");
      
    } // chooseFullTextCu
    
    protected static class LoggerAdapter implements ParserFeedback {
      
      protected String url;
      public LoggerAdapter(String url) {
        this.url = url;
      }
      @Override
      public void error(String msg, ParserException pe) {
        log.error(String.format("While processing %s: %s", url, msg), pe);
      }
      @Override
      public void info(String msg) {
        log.info(String.format("While processing %s: %s", url, msg));
      }
      @Override
      public void warning(String msg) {
        log.warning(String.format("While processing %s: %s", url, msg));
      }
      
    } // LoggerAdapter
  
  } // OJS2ArticleIterator 

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    
  } // createArticleMetadataExtractor
  
} // OJS2ArticleIteratorFactory
