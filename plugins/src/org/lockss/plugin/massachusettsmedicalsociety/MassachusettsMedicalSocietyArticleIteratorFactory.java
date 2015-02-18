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

package org.lockss.plugin.massachusettsmedicalsociety;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

/*
 * HTML Full Text: http://www.nejm.org/doi/full/10.1056/NEJMoa042957
 * PDF Full Text: http://www.nejm.org/doi/pdf/10.1056/NEJMoa042957
 * Citation (containing metadata): www.nejm.org/action/downloadCitation?format=(ris|endnote|bibTex|medlars|procite|referenceManager)&doi=10.1056%2FNEJMoa042957&include=cit&direct=checked
 * Supplemental Materials page: http://www.nejm.org/action/showSupplements?doi=10.1056%2FNEJMc1304053
 */

public class MassachusettsMedicalSocietyArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
	
  //define roles for different citation types the key is the ArticleFiles key and the value is the format = value in the url
  protected static final String ROLE_CITATION_RIS = ArticleFiles.ROLE_CITATION + "Ris";
  protected static final String ROLE_CITATION_ENDNOTE = ArticleFiles.ROLE_CITATION + "Endnote";
  protected static final String ROLE_CITATION_BIBTEX = ArticleFiles.ROLE_CITATION + "Bibtex";
  protected static final String ROLE_CITATION_MEDLARS = ArticleFiles.ROLE_CITATION + "Medlars";
  protected static final String ROLE_CITATION_PROCITE = ArticleFiles.ROLE_CITATION + "Procite";
  protected static final String ROLE_CITATION_REFMANAGER = ArticleFiles.ROLE_CITATION + "Refmanager";
  protected static final String ROLE_SUPPLEMENTARY_MATERIALS = ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS;
  
  protected static Logger log = Logger.getLogger("MassachusettsMedicalSocietyArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%sdoi\", base_url"; // params from tdb file corresponding to AU
  protected static final String PATTERN_TEMPLATE = "\"^%sdoi/(full|pdf)/\", base_url";

  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new MassachusettsMedicalSocietyArticleIterator(au,
                                         new SubTreeArticleIterator.Spec()
                                             .setTarget(target)
                                             .setRootTemplate(ROOT_TEMPLATE)
                                             .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class MassachusettsMedicalSocietyArticleIterator extends SubTreeArticleIterator {

    protected Pattern HTML_PATTERN = Pattern.compile("/doi/full/(.*)$", Pattern.CASE_INSENSITIVE);
    protected Pattern PDF_PATTERN = Pattern.compile("/doi/pdf/(.*)$", Pattern.CASE_INSENSITIVE);

    public MassachusettsMedicalSocietyArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.debug3("createArticleFiles: cu="+url);
      Matcher mat;
      mat = HTML_PATTERN.matcher(url);
      if (mat.find()) {
    	if(isHtml(cu)) {
    		return processFullTextHtml(cu, mat);
    	}
    	//Check the mime-type. Some full article links point to PDFs instead of HTML pages in older volumes
    	else if(isPdf(cu)) {
    		return processFullTextPdf(cu, mat, true);
        }
      }
     
      mat = PDF_PATTERN.matcher(url);
      if (mat.find() && isPdf(cu)) {
        return processFullTextPdf(cu, mat);
      }
      //log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu,
                                               Matcher htmlMat) {
      ArticleFiles af = new ArticleFiles();
      log.debug3("setFullTextCu: "+htmlCu);
      af.setFullTextCu(htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      if (spec.getTarget() != MetadataTarget.Article()) {
        guessFullTextPdf(af, htmlMat);
        guessCitations(af, htmlMat);
        guessSupplements(af, htmlMat);
      }
      return af;
    }
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat) {
    	return processFullTextPdf(pdfCu, pdfMat, false);
    }
    protected ArticleFiles processFullTextPdf(CachedUrl pdfCu, Matcher pdfMat, boolean isFull) {
      if(!isFull){
	      CachedUrl htmlCu = au.makeCachedUrl(pdfMat.replaceFirst("/doi/full/$1"));
	      if (htmlCu != null && htmlCu.hasContent() && isHtml(htmlCu)) {
	        return null;
	      }
      }
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(pdfCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      if (spec.getTarget() != MetadataTarget.Article()) {
        guessCitations(af, pdfMat);
        guessSupplements(af, pdfMat);
      }
      return af;
    }
    
    protected boolean isPdf(CachedUrl cu){
    	if(cu.getContentType().trim().startsWith(Constants.MIME_TYPE_PDF)) {
    		cu.release();
    		return true;
    	}
    	cu.release();
    	return false;
    }
    
    protected boolean isHtml(CachedUrl cu){
    	if(cu.getContentType().trim().startsWith(Constants.MIME_TYPE_HTML)) {
    		cu.release();
    		return true;
    	}
    	cu.release();
    	return false;
    }
    
    protected void guessFullTextPdf(ArticleFiles af, Matcher mat) {
      CachedUrl pdfCu = au.makeCachedUrl(mat.replaceFirst("/doi/pdf/$1"));
      if (pdfCu != null && pdfCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
      }
    }

    protected void guessCitations(ArticleFiles af, Matcher mat) {
      String citDoi = mat.group(1);
      CachedUrl primCitCu = null;
      citDoi = citDoi.replace("/", "%2F");
      String[][] citTypes = {{ROLE_CITATION_RIS, "ris"},
    		  				 {ROLE_CITATION_PROCITE, "procite"},
    		  				 {ROLE_CITATION_ENDNOTE, "endnote"},
    		  				 {ROLE_CITATION_BIBTEX, "bibTex"},
    		  				 {ROLE_CITATION_MEDLARS, "medlars"},
    		  				 {ROLE_CITATION_REFMANAGER, "referenceManager"}};
      
      for(String[] citType : citTypes){
	      CachedUrl citCu = au.makeCachedUrl(mat.replaceFirst("/action/downloadCitation?format=" + citType[1] + "&doi=" + citDoi + "&include=cit&direct=checked"));
	      if (citCu != null && citCu.hasContent()) {
	    	if(primCitCu == null){
	    		primCitCu = citCu;
	    	}
	        af.setRoleCu(citType[0], citCu);
	      }
      }
      af.setRoleCu(ArticleFiles.ROLE_CITATION, primCitCu);
    }
    
    // Assigning the Supplementary_Materials role to the url (eg):
    // http://www.nejm.org/action/showSupplements?doi=10.1056%2FNEJMc1304053
    // which contain links to the appendix, disclosures and/or protocol page(s)
    // Supplementary_Materials landing page doi case matches that of main article
    // urls (whereas underlying specific supplementary data urls are always lower case
    protected void guessSupplements(ArticleFiles af, Matcher mat) {
      String origdoi = mat.group(1);
      String supDoi = origdoi.replace("/", "%2F");
      log.debug3("guessSupplements: "+ "/action/showSupplements?doi="+supDoi);   
      CachedUrl supCu = au.makeCachedUrl(
          mat.replaceFirst("/action/showSupplements?doi="+supDoi));

      if (supCu != null && supCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, supCu);
      }  
    }
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
	  return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_CITATION);
  }

}
