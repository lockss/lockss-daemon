/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ingenta;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class IngentaArticleIteratorFactory implements ArticleIteratorFactory,
    ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(IngentaArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = 
      "\"%scontent/%s/%s\", api_url, publisher_id, journal_id";
  
  protected static final String PATTERN_TEMPLATE = 
      "\"^%scontent/%s/%s/[0-9]{4}/0*%s/.{8}/art[0-9]{5}\\?crawler=true$\", " +
      "api_url, publisher_id, journal_id, volume_name";
  
  protected static final Pattern PLAIN_PATTERN = Pattern.compile(
      "^(.*)content/([^/]+/[^/]+/[0-9]{4}/[^/]+/[^/]+/[^/]+)[?]crawler=true$",
      Pattern.CASE_INSENSITIVE);
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
      MetadataTarget target) throws PluginException {
    return new IngentaArticleIterator(au, new SubTreeArticleIterator.Spec()
        .setTarget(target).setRootTemplate(ROOT_TEMPLATE)
        .setPatternTemplate(PATTERN_TEMPLATE), target);
  }
  
  protected static class IngentaArticleIterator extends SubTreeArticleIterator {
    
    protected String baseUrl;
    protected MetadataTarget target;
    
    public IngentaArticleIterator(ArchivalUnit au,
        SubTreeArticleIterator.Spec spec, MetadataTarget target) {
      super(au, spec);
      this.baseUrl = au.getConfiguration().get(
          ConfigParamDescr.BASE_URL.getKey());
      this.target = target;
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.debug3("Entry point: " + url);
      
      Matcher mat = PLAIN_PATTERN.matcher(url);
      if (mat.find()) {
        return processPlainFullTextCu(cu, mat);
      }
      
      log.warning("Mismatch between article iterator factory and article iterator: "
          + url);
      return null;
    }
    
    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu, Matcher htmlMat) {
      CachedUrl plainCu = au.makeCachedUrl(htmlMat
          .replaceFirst("$1content/$2?crawler=true"));
      if (plainCu != null && plainCu.hasContent()) {
        log.debug3("Defer to plain URL");
        AuUtil.safeRelease(plainCu);
        return null; // Defer to plain URL
      }
      
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(htmlCu);
      guessFullTextPdf(af, htmlMat);
      if (spec.getTarget() != MetadataTarget.Article()) {
        guessAbstract(af, htmlMat);
        guessReferences(af, htmlMat);
      }
      return af;
    }
    
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      CachedUrl plainCu = au.makeCachedUrl(pdfMat
          .replaceFirst("$1content/$2?crawler=true"));
      if (plainCu != null && plainCu.hasContent()) {
        log.debug3("Defer to plain URL");
        AuUtil.safeRelease(plainCu);
        return null; // Defer to plain URL
      }
      
      CachedUrl plainMetaCu = au.makeCachedUrl(pdfMat
          .replaceFirst("$1content/$2"));
      if (plainMetaCu != null && plainMetaCu.hasContent()) {
        log.debug3("Defer to plain URL");
        AuUtil.safeRelease(plainMetaCu);
        return null; // Defer to plain URL
      }
      
      CachedUrl htmlCu = au.makeCachedUrl(pdfMat
          .replaceFirst("$1content/$2?crawler=true&mimetype=text/html"));
      if (htmlCu != null && htmlCu.hasContent()) {
        AuUtil.safeRelease(htmlCu);
        return null; // Defer to HTML URL
      }
      
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      if (spec.getTarget() != MetadataTarget.Article()) {
        guessAbstract(af, pdfMat);
        guessReferences(af, pdfMat);
      }
      return af;
    }

    protected ArticleFiles processPlainFullTextCu(CachedUrl plainCu,
        Matcher plainMat) {
      
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(plainCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, plainCu);
      if (target != MetadataTarget.Article()) {
        guessFullTextHtml(af, plainMat);
      }
      else {
        log.warning("Unexpected content type of " + plainCu.getUrl() + ": "
            + plainCu.getContentType());
	AuUtil.safeRelease(plainCu);
      }
      if (spec.getTarget() != MetadataTarget.Article()) {
        guessAbstract(af, plainMat);
        guessReferences(af, plainMat);
      }
      return af;
    }

    protected void guessFullTextHtml(ArticleFiles af, Matcher mat) {

      String modStr = mat
          .replaceFirst("http://www.ingentaconnect.com/content/$2");
      CachedUrl htmlCu = au.makeCachedUrl(modStr);
      if (htmlCu != null && htmlCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
        AuUtil.safeRelease(htmlCu);
      }
    }

    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat
          .replaceFirst("$1content/$2?crawler=true&mimetype=application/pdf"));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
        AuUtil.safeRelease(pdfCu);
      }
    }

    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst(String.format(
          "%scontent/$2", baseUrl)));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        AuUtil.safeRelease(absCu);
      }
    }

    protected void guessReferences(ArticleFiles af, Matcher mat) {
      CachedUrl refCu = au.makeCachedUrl(mat.replaceFirst(String.format(
          "%scontent/$2/references", baseUrl)));
      if (refCu != null && refCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, refCu);
        AuUtil.safeRelease(refCu);
      }
    }

  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(
      MetadataTarget target) throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_FULL_TEXT_HTML);
  }

}
