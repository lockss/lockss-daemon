/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.metapress;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;


public class MetaPressArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(MetaPressArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%scontent\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%scontent/[A-Za-z0-9]{16}/fulltext\\.pdf$\", base_url";

  protected static final Pattern RIS_PATTERN = Pattern.compile(
      "^VL[ ]+[-][ ]+([0-9-]+)", Pattern.CASE_INSENSITIVE);

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    
    return new MetaPressArticleIterator(au,
                                        new SubTreeArticleIterator.Spec()
                                            .setTarget(target)
                                            .setRootTemplate(ROOT_TEMPLATE)
                                            .setPatternTemplate(PATTERN_TEMPLATE),
                                        target);
  }

  protected class MetaPressArticleIterator extends SubTreeArticleIterator {

    protected Pattern PATTERN = Pattern.compile("/content/([a-z0-9]{16})/fulltext\\.pdf$",
        Pattern.CASE_INSENSITIVE);
    
    protected MetadataTarget target;
    
    protected String au_vol = null;

    public MetaPressArticleIterator(ArchivalUnit au,
                                    SubTreeArticleIterator.Spec spec,
                                    MetadataTarget target) {
      super(au, spec);
      this.target = target;
      this.au_vol = getAuVol(au);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;

      mat = PATTERN.matcher(url);
      if (mat.find()) {
        return processFullTextPdf(cu, mat);
      }

      log.warning("Mismatch between article iterator factory and article iterator: "+ url);
      return null;
    }
    //http://inderscience.metapress.com/content/kv824m8x38336011/fulltext.pdf map={
    // FullTextPdfLanding=[BCU: http://inderscience.metapress.com/content/p20687286306321u],
    // FullTextPdfFile=[BCU: http://inderscience.metapress.com/content/p20687286306321u/fulltext.pdf],
    // IssueMetadata=[BCU: http://inderscience.metapress.com/content/p20687286306321u],
    // Citation=[BCU: http://inderscience.metapress.com/export.mpx?code=P20687286306321U&mode=ris],
    // Abstract=[BCU: http://inderscience.metapress.com/content/p20687286306321u]}])

    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      if (!target.isArticle()) {
        guessAbstract(af, pdfMat);
        guessFullTextHtml(af, pdfMat);
        guessReferences(af, pdfMat);
      }
      guessCitations(af, pdfMat);
      if (au_vol != null) {
        CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_CITATION_RIS);
        if (cu != null) {
          String vol = getCuVol(cu);
          if (vol != null && !vol.isEmpty() && !au_vol.equals(vol)) {
            af = null;
            // probably an overcrawled cu, so warn
            log.warning("Au (" + au_vol + ") and Cu (" + vol + ") do not match " +
                "probable overcrawled url: " + pdfCu.getUrl());
          }
        }
      }
      log.debug3("af: " + af);
      return af;
    }
          
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/content/$1"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, absCu);
        af.setRoleCu(ArticleFiles.ROLE_ISSUE_METADATA, absCu);
      }
    }
    

    protected void guessFullTextHtml(ArticleFiles af, Matcher mat) {
      CachedUrl htmlCu = au.makeCachedUrl(mat.replaceFirst("/content/$1/fulltext.html"));
      if (htmlCu != null && htmlCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      }
    }

    protected void guessCitations(ArticleFiles af, Matcher mat) {
      String citStr = mat.replaceFirst("/export.mpx?code=$1&mode=ris");
      log.debug3("citStr (1): " + citStr);
      CachedUrl citCu = au.makeCachedUrl(citStr);
      if (citCu == null || !citCu.hasContent()) {
        citStr = mat.replaceFirst(String.format("/export.mpx?code=%s&mode=ris", 
            mat.group(1).toUpperCase()));
        log.debug3("citStr (2): " + citStr);
        citCu = au.makeCachedUrl(citStr);
      }
      if (citCu != null && citCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_CITATION_RIS, citCu);
        log.debug3("citCu :" + citCu);
      }
     }
        
    protected void guessReferences(ArticleFiles af, Matcher mat) {
      CachedUrl refCu = au.makeCachedUrl(mat.replaceFirst("/content/$1/?referencesMode=Show"));
      if (refCu != null && refCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_REFERENCES, refCu);
      }
    }
  }
  
  protected String getAuVol(ArchivalUnit au) {
    String vol = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());
    return vol;
  }
  
  protected String getCuVol(CachedUrl cu) {
    BufferedReader bReader = null;
    try {
      bReader = new BufferedReader(new InputStreamReader(
          cu.getUnfilteredInputStream(), cu.getEncoding())
          );
      Matcher matcher;
      
      // go through the cached URL content line by line
      // if a match is found, look for valid url & content
      // if found then set the role for ROLE_FULL_TEXT_PDF
      for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
        matcher = RIS_PATTERN.matcher(line);
        if (matcher.find()) {
          String vol = matcher.group(1); 
          return vol;
        }
      }
    } catch (Exception e) {
      // probably not serious, so warn
      log.warning(e + " : Looking for volume name");
    }
    finally {
      IOUtil.safeClose(bReader);
    }
    return null;
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_CITATION_RIS);
  }

}
