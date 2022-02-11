package org.lockss.plugin.oecd;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OecdJournalsArticleMetadataExtractor extends BaseArticleMetadataExtractor {
  protected static Logger log = Logger.getLogger(OecdJournalsArticleMetadataExtractor.class);

  private static final String PDF_ARTICLE_PATTERN = ".*/.+\\.pdf\\?itemId=%2Fcontent%2Fpaper%2F.+&mimeType=pdf$";
  private static final String PDF_ISSUE_PATTERN = ".*/.+\\.pdf\\?itemId=%2Fcontent%2Fpublication%2F.+&mimeType=pdf$";

  private static final String PDF_ROLE = ArticleFiles.ROLE_FULL_TEXT_PDF;

  public OecdJournalsArticleMetadataExtractor(String roleArticleMetadata) {
    super(roleArticleMetadata);
  }

  protected String getPdfUrlFromHtml(CachedUrl cu) throws IOException {
    String PDF_PATTERN;
    String srcUrl = cu.getUrl();
    if (srcUrl.contains("volume") && srcUrl.contains("issue")) {
      PDF_PATTERN = PDF_ISSUE_PATTERN;
    } else {
      PDF_PATTERN = PDF_ARTICLE_PATTERN;
    }
    List<String> pdf_urls = new ArrayList<>();;
    HtmlParserLinkExtractor OecdLinkExtractor =  new HtmlParserLinkExtractor();
    OecdLinkExtractor.extractUrls(
        cu.getArchivalUnit(),
        cu.getUnfilteredInputStream(),
        cu.getEncoding(),
        srcUrl,
        url -> {
          if (url.matches(PDF_PATTERN)) {
            pdf_urls.add(url);
          }
        }
    );
    if (!pdf_urls.isEmpty()) {
      return pdf_urls.get(0);
    }
    return null;
  }

  protected boolean hasPdfRole(ArticleFiles af) {
    String pdf_url = af.getRoleUrl(PDF_ROLE);
    // if pdf_url exists, nothing to do
    if (pdf_url != null) {
      return true;
    }
    return false;
  }

  protected void checkPdfRole(ArticleFiles af,
                                  CachedUrl cu) throws IOException {

    String pdf_url = af.getRoleUrl(PDF_ROLE);
    // if pdf_url exists, nothing to do
    if (pdf_url != null) {
      return;
    }
    log.debug3("no PDF in ArticleFile, finding in HTML ");
    // try to get the pdf url from the html
    pdf_url = getPdfUrlFromHtml(cu);
    if (pdf_url == null) {
      return;
    }
    // check if the pdf_url exists in the crawl, if not, return.
    CachedUrl testCu = cu.getArchivalUnit().makeCachedUrl(pdf_url);
    if ((testCu == null) || (!testCu.hasContent())) {
      return;
    }
    af.setRoleCu(PDF_ROLE, testCu);
    return;
  }

  class OecdEmitter implements FileMetadataExtractor.Emitter {
    private Emitter parent;
    private ArticleFiles af;


    OecdEmitter(ArticleFiles af, Emitter parent) {
      this.af = af;
      this.parent = parent;
    }

    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
      try {
        checkPdfRole(af, cu);
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (!hasPdfRole(af)) {
        // no pdf, don't emit
        return;
      }
      if (isAddTdbDefaults()) {
        addTdbDefaults(af, cu, am);
      }
      if (isCheckAccessUrl()) {
        checkAccessUrl(af, cu, am);
      }
      parent.emitMetadata(af, am);
    }

  }

  public void extract(MetadataTarget target, ArticleFiles af,
                      ArticleMetadataExtractor.Emitter emitter)
      throws IOException, PluginException {

    OecdEmitter myEmitter = new OecdEmitter(af, emitter);
    CachedUrl cu = getCuToExtract(af);
    if (log.isDebug3()) log.debug3("extract(" + af + "), cu: " + cu);
    if (cu != null) {
      try {
        FileMetadataExtractor me = cu.getFileMetadataExtractor(target);
        if (me != null) {
          me.extract(target, cu, myEmitter);
          return;
        }
      } catch (IOException ex) {
        log.warning("Error in FileMetadataExtractor", ex);
      } finally {
        AuUtil.safeRelease(cu);
      }
    } else {
      // get full-text CU if cuRole CU not present
      cu = af.getFullTextCu();
      if (log.isDebug3()) {
        log.debug3("Missing CU for role " + cuRole
            + ". Using fullTextCU " + af.getFullTextUrl());
      }
    }
    // Here if cuRole wasn't present or extractor threw IOException
    if (cu != null) {
      try {
        if (log.isDebug3()) {
          log.debug3("Storing tdb info for  " + cu.getUrl());
        }
        ArticleMetadata am = new ArticleMetadata();
        myEmitter.emitMetadata(cu, am);
      } finally {
        AuUtil.safeRelease(cu);
      }
    }
  }

}