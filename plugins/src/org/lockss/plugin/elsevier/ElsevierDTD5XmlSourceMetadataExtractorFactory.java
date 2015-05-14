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

package org.lockss.plugin.elsevier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

public class ElsevierDTD5XmlSourceMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ElsevierDTD5XmlSourceMetadataExtractorFactory.class);


  //Use to identify the volume and optional issue and supplement information
  // from the full path of the main.xml
  //     <base_url>/<year>/<tarnum>.tar!/<tarnum>/03781119/v554i2/S0378111914011998/main.xml
  // will have a "v" portion $1, may have "i" portion $2 and/or "s" portion $3
  // there could be a hyphen in the volume or issue portion
  static final Pattern ISSUE_INFO_PATTERN = 
      Pattern.compile(".*\\.tar!/[^/]+/[^/]+/v([^is/]+)(?:i([^s/]+))?(?:s([^/]+))?/[^/]+/main\\.xml$", Pattern.CASE_INSENSITIVE);

  private static SourceXmlSchemaHelper datasetHelper = null;
  private static SourceXmlSchemaHelper mainHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new ElsevierDTD5XmlSourceMetadataExtractor();
  }

  public static class ElsevierDTD5XmlSourceMetadataExtractor extends SourceXmlMetadataExtractor {



    /* 
     * The iterator finds both "dataset.xml" and all the low-leven "main.xml" files.
     * The dataset.xml file contains information for all the articles included in the tar delivery
     * The main.xml file is specific to one article and is the only place for the author, article.title
     *   as well as a backup place for doi and copyright data
     */
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {

      // we have two schema helpers - return the one needed by this file
      if ((cu.getUrl()).endsWith("main.xml")) {
        if(mainHelper == null) {
          mainHelper = new ElsevierMainDTD5XmlSchemaHelper();
        }
        return mainHelper;
      }
      // Once you have it, just keep returning the same one. It won't change.
      if (datasetHelper == null) {
        datasetHelper = new ElsevierDatasetXmlSchemaHelper();
      }
      return datasetHelper;
    }


    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
      // for now keep it the same
      return setUpSchema(cu);
    }


    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      
      //if (!(full_article_md_file.endsWith("main.xml"))) {
      // if this is the dataset file, just emit - we'll check for pdf at the article level
      if (helper == datasetHelper) {
        return null;
      }
      String md_url = cu.getUrl();
      String pdf_url = md_url.substring(0,md_url.length() - 4) + ".pdf"; 
      log.debug3("pdf file is " + pdf_url);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdf_url);
      return returnList;
    }
    
    /* 
     * We know we have a matching pdf if we get to this routine
     * There is no post-cook for dataset.xml processing
     * For main.xml processing, use the URL path to identify volume & issue 
     */
    @Override 
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      if (schemaHelper == datasetHelper) {
        // if this is an AM from the dataset level file, we don't need any additional processing done.
        return;
      }
      log.debug3("in Elsevier postCookProcess");
      String md_url = cu.getUrl();
      //FIELD_ACCESS_URL is set in the parent class preEmitCheck

       /*
        * Now get the volume, issue and supplement status from the URL
        * md_url looks like this:
        *      03781119/v554i2/S0378111914011998/main.xml
        *   we want the second section which could have a v, i, and s component
        *   v# or v#-# 
        *   i# or i#-#
        *   s#orLtr
        *   will have at least v
        *   exs: /v113-115sC/ or /v58i2-3/ or /v117i6/ or /v39sC/ or /v100i8sS/   
        */
  
        Matcher vMat = ISSUE_INFO_PATTERN.matcher(md_url);
        log.debug3("checking for volume information from path");
        if (vMat.matches()) {
          String vol = vMat.group(1);
          String optIss = vMat.group(2);
          String optSup = vMat.group(3);
          log.debug3("found volume information: V" + vol + "I" + optIss + "S" + optSup);
          thisAM.put(MetadataField.FIELD_VOLUME, vol);
          if( (optIss != null) || (optSup != null)){
            StringBuilder val = new StringBuilder();
            if (optIss != null) {
              val.append(optIss);
            }
            if (optSup != null) {
              val.append(optSup);
            }
            // there is no field equivalent to the suppl used by Elsevier
            thisAM.put(MetadataField.FIELD_ISSUE, val.toString()); 
          }
        }
    }
  }
}
