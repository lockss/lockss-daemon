/*
Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

public class OecdArticleMetadataExtractor extends BaseArticleMetadataExtractor {
  protected static Logger log = Logger.getLogger(OecdArticleMetadataExtractor.class);

  private static final String PDF_ARTICLE_PATTERN = ".*/.+\\.pdf\\?itemId=%2Fcontent%2Fpaper%2F.+&mimeType=pdf$";
  private static final String PDF_ISSUE_PATTERN = ".*/.+\\.pdf\\?itemId=%2Fcontent%2Fpublication%2F.+&mimeType=pdf$";

  private static boolean IS_BOOK = false;

  private static final String PDF_ROLE = ArticleFiles.ROLE_FULL_TEXT_PDF;

  public OecdArticleMetadataExtractor(String roleArticleMetadata,
                                              boolean isBook) {
    super(roleArticleMetadata);
    IS_BOOK = isBook;
  }

  protected String getPdfUrlFromHtml(CachedUrl cu) throws IOException {
    String PDF_PATTERN;
    String srcUrl = cu.getUrl();
    if ((srcUrl.contains("volume") && srcUrl.contains("issue")) || IS_BOOK ) {
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