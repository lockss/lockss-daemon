/*
 * $Id: ACMSourceXmlMetadataExtractorFactory.java 43117 2015-07-10 03:16:05Z alexandraohlson $
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

package org.lockss.plugin.associationforcomputingmachinery;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class ACMSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ACMSourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper ACMHelper = null;
  private static SourceXmlSchemaHelper ACMBooksHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new ACMSourceXmlMetadataExtractor();
  }

  
  /*
   * We have two helpers - return the one needed by this file
   */
  public class ACMSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    /* 
     * this version allows us to see what type of schema we're in 
     * and therefore return the right sort of helper
     */
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
      //look at the top node of the Document to identify the schema
      Element top_element = xmlDoc.getDocumentElement();
      String element_name = top_element.getNodeName();
      if ("whole_books".equals(element_name)) {
        if (ACMBooksHelper == null) {
          ACMBooksHelper = new ACMBooksXmlSchemaHelper();
        }
        return ACMBooksHelper;
      }
      // default to the proceedings/journals schema
      return setUpSchema(cu);
    }
    
    /* The default is just the original journals/proceedings schema */    
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
    // Once you have it, just keep returning the same one. It won't change.
      if (ACMHelper == null) {
        ACMHelper = new ACMXmlSchemaHelper();
      }
      return ACMHelper;
    }
    
    
    /*
     * starting in late Nov 2014 ACM started to change the location of the identified
     * fulltext filenames. 
     * Originally (and occasionally) the listed filename is at the same level as
     * the XML file in which it is named.
     * Now, usually, the listed filename is in a subdirectory of the specific 
     * article_rec article_id name.  
     * That is, if the article_id is 2366543, then that is the name of the subedirectory.
     * In the interest of being flexible - we'll look in both places! 
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      
      // get the key for a piece of metadata used in building the filename
      String fn_key = helper.getFilenameXPathKey();  
      // the schema doesn't define a filename so don't do a default preEmitCheck
      if (fn_key == null) {
        return null; // no preEmitCheck 
      }
      String filenameValue = oneAM.getRaw(helper.getFilenameXPathKey());
      log.debug3("PreEmit filename is " + filenameValue);
      // we expected a value, but didn't get one...we need to return something
      // for preEmitCheck to fail
      if (filenameValue == null) {
        filenameValue = "NOFILEINMETADATA"; // we expected a value, but got none
      }
      if (filenameValue.endsWith(".html")) {
        filenameValue = filenameValue.replace("\\", "/");
        log.debug3("html filename is now " + filenameValue);
      }
      
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String article_id_directory = "fulltext"; //the default - for books
      if (helper != ACMBooksHelper) {
        article_id_directory = oneAM.getRaw(ACMXmlSchemaHelper.ACM_article_id);
      }
      List<String> returnList = new ArrayList<String>();
      // default version is just the filename associated with the key, in this directory
      returnList.add(cuBase + filenameValue);
      // next option is same filename in subdirectory of the article_id or the fulltext dir
      returnList.add(cuBase + article_id_directory + "/" + filenameValue);
      return returnList;
    }

    
    /* 
     * Do a better job of identifying the type of content based on information in 
     * the schema or CU
     * Identify the PROC- items as proceedings now so they will differentiate from books
     *    
     */
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      log.debug3("in ACM spcific postCookProcess");
      String url = cu.getUrl();
      
      /* if the PROPRIETARY_ID isn't set it was probably a proceeding, get
       * the code from the URL 
       * UPD-PROC-AOSD13-2451436
       * NEW-TRAN-TIIS-V4I1-2602757
       * For proceedings (PROC) there is not volume/issue portion
       * For MAG, TRAN, NEWSL, JOUR, there is
       */
      // 1. new or update
      // 2. (type) group
      // 3. (jcode) group 
      // 4. optional (volume/issue) group
      // then id# - not captured
      Pattern DELIVERY_PATTERN = Pattern.compile("/(UPD|NEW)-([^-/]+)-([^-/]+)(-V\\d+I\\d+)?-[^/]+\\.xml", Pattern.CASE_INSENSITIVE);
      Matcher mat = DELIVERY_PATTERN.matcher(url);
      String itemType = null; 
      if (mat.find()) {
        String jCode = mat.group(3);
        itemType = mat.group(2);
        thisAM.putIfBetter(MetadataField.FIELD_PROPRIETARY_IDENTIFIER, jCode);
      }
 
      String articleType;
      String publicationType;
      // are we in the realm of books?
      if (schemaHelper == ACMBooksHelper) {
        publicationType = MetadataField.PUBLICATION_TYPE_BOOK;
        articleType = MetadataField.ARTICLE_TYPE_BOOKCHAPTER;
        // When the book_rec was the node there is not <title> node
        // and so make it a full book and give it the title of the whole book
        if (thisAM.get(MetadataField.FIELD_ARTICLE_TITLE) == null) {
          articleType = MetadataField.ARTICLE_TYPE_BOOKVOLUME;
          thisAM.put(MetadataField.FIELD_ARTICLE_TITLE,  thisAM.get(MetadataField.FIELD_PUBLICATION_TITLE));
        }
      } else {
        // Now that we support proceedings, make them such.  It will take a while to percolate through to the database
        if ("PROC".equals(itemType)) {
          publicationType = MetadataField.PUBLICATION_TYPE_PROCEEDINGS;
          articleType = MetadataField.ARTICLE_TYPE_PROCEEDINGSARTICLE;
        } else {
          articleType = MetadataField.ARTICLE_TYPE_JOURNALARTICLE; 
          publicationType = MetadataField.PUBLICATION_TYPE_JOURNAL;
        }
      }
      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, publicationType);
      thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, articleType);
    }
    

    @Override
    public boolean getDoXmlFiltering() {
      return true;
    }
  }
}
