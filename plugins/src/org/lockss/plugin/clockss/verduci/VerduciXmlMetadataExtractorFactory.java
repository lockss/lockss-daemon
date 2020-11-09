/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.verduci;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.TdbAu;
import org.lockss.util.*;
import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.clockss.PubMedSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * If the xml is at <issuedirectory>/XML/EurRevMedPharmacolSciv21i22.xml
 * then the pdf is at <issuedirectory>/PDF/5153-5159-MiR-155 facilitates lymphoma proliferation.pdf
 * The XML subdirectory and/or PDF subdirectory may not be there.
 * 
 */

public class VerduciXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(VerduciXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper PubMedHelper = null;
  /*
   * If <dir> is the issue_directory
   * XML could be in <dir>/XML/ or <dir>/XML FILES or <dir>/XML FILE or in 1 case, just <dir>
   */
  protected static final Pattern XML_DIR_PATTERN = 
	      Pattern.compile("(.*)/XML(( |%20)FILE(S)?)?/$", Pattern.CASE_INSENSITIVE);  

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new VerduciPubMedXmlMetadataExtractor();
  }

  public class VerduciPubMedXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    /*
     * This class will get created for each XML file that we find.
     * We want to store a CachedUrlList of the PDF files that exist
     * within the same issue subdirectory as this XML file so we can 
     * identify the appropriate PDF file for each article based on the first part
     * of the PDF filename - which is the start page.  
     * The file naming is very inconsistent and can't be extrapolated in its entirety.
     * 
     */
    CachedUrlSet issueUrls = null;

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      /*
       * This will get created once for each XML file
       * which is appropriate as the XML and corresponding PDF files
       * live below the same issue directory 
       */
    	if (issueUrls == null) {
    		ArchivalUnit au = cu.getArchivalUnit();
    		// get an iterable list of the cu's in this subdirectory
    		String issuePath = FilenameUtils.getPath(cu.getUrl());
    		if (issuePath.contains("/XML")) {
    			Matcher xmat = XML_DIR_PATTERN.matcher(issuePath);
    			if (xmat.find()) {
    				issuePath = xmat.group(1);
    			}
    		}
			// Store a CachedUrlSet of all the urls below the issue directory
			issueUrls = au.makeCachedUrlSet(new RangeCachedUrlSetSpec(issuePath));
    	}
    	// Once you have it, just keep returning the same one. It won't change.
    	if (PubMedHelper == null) {
    		PubMedHelper = new PubMedSchemaHelper();
    	}
    	return PubMedHelper;
    }


    /* 
     * Verduci's layout is inconsistent
     * The PDF and XML both live under the same issue level subdirectory.
     * The XML might live under an XML subdirectory below that.
     * The PDF files might live under a PDF subdirectory
     * either/or live just at the level of the issue directory.
     * The PDF filename is inconsistent
     * The filename is supposedly startpage-endpage.pdf
     * and if there is no endpage, then startpage-startpage.pdf
     * but sometimes it's startpage-otherpage-<titleportion> or
     * startpage-<titleportion>
     * so we are using an iterator which lists all the files in the issue
     * subdirectory to narrow down and find the CU
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      List<String> returnList = new ArrayList<String>();
      String spage = oneAM.getRaw(PubMedSchemaHelper.art_sp);
      String epage = oneAM.getRaw(PubMedSchemaHelper.art_lp);

      // In oct/2020, their upload a new folder with different structure. PDF file is named using
      // <ELocationID EIdType="pii">1287</ELocationID>
      // /verduci-released/2020/WCRJ/2019%20VOLUME%206/III/e1287.pdf

      String elocation_id = oneAM.getRaw(PubMedSchemaHelper.elocation_id);

      if (epage == null) {
        epage = spage;
      }
      if (issueUrls != null) {
        // We have a CachedUrlSet of urls below this issue directory
        for (CachedUrl trycu : issueUrls.getCuIterable()) {
          String tryUrl = trycu.getUrl();
          if (FilenameUtils.getBaseName(tryUrl).startsWith(spage + "-")) {
            log.debug3("found likely pdfName is" + tryUrl);
            returnList.add(tryUrl);
            break;
          }
        }
        // if for some reason we don't have a CachedUrlSet or if we couldn't fine any  
        // files in it - try for some default behavior
        if (returnList.size() == 0) {
          String url_string = cu.getUrl();
          String xmlPath = FilenameUtils.getPath(url_string);
          if (xmlPath.endsWith("XML/")) {
            xmlPath = xmlPath.substring(0,xmlPath.length() - 4); 
          }

          String pdfName = "";

          if (spage != null && epage != null) {
              // 1st option - no PDF directory
              pdfName = xmlPath + spage + "-" + epage + ".pdf";
              log.debug3("pdfName is " + pdfName);
              returnList.add(pdfName);
              // 2nd option - PDF subdirectory
              pdfName = xmlPath + "PDF/" + spage + "-" + epage + ".pdf";
              log.debug3("pdfName is " + pdfName);
          } else if (elocation_id != null) {

              pdfName = xmlPath + "e" + elocation_id + ".pdf";
              log.debug3("WRC pdfName is " + pdfName);
          }
          returnList.add(pdfName);
        }
      }

      String publisherName = "Verduci Editore";

        TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
        if (tdbau != null) {
            publisherName =  tdbau.getPublisherName();
        }

        oneAM.put(MetadataField.FIELD_PUBLISHER, publisherName);

      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in Verduci postCookProcess");

    }
    
  }
}
