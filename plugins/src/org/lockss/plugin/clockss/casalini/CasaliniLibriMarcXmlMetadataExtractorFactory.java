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

package org.lockss.plugin.clockss.casalini;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;


public class CasaliniLibriMarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(CasaliniLibriMarcXmlMetadataExtractorFactory.class);
  
  private static SourceXmlSchemaHelper CasaliniHelper = null;
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new CasaliniMarcXmlMetadataExtractor();
  }

  public class CasaliniMarcXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (CasaliniHelper != null) {
        return CasaliniHelper;
      }
      CasaliniHelper = new CasaliniMarcXmlSchemaHelper();
      return CasaliniHelper;
    }
    
    
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      
      List <String>pubList = oneAM.getRawList(CasaliniMarcXmlSchemaHelper.MARC_publisher);
      String pubDir = cleanPublisherName(StringUtil.separatedString(pubList, "; "), true);
      String dirNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_dir);
      String fileNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_file);
      // when it's not set then it is the whole book (same dir and filenum)
      // except for two filenumbers which are missing entirely.
      if (dirNum == null) {
        dirNum = fileNum;
      }
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      ArrayList<String> returnList = new ArrayList<String>();
      returnList.add(cuBase + "Monographs/" + pubDir + "/" + dirNum + "/" + fileNum + ".pdf");
      return returnList;
    }    
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
    
      thisAM.put(MetadataField.FIELD_PROVIDER,"Casalini Libri");
      // Now build up a full title if that is necessary
      StringBuilder title_br = new StringBuilder(thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_title));
      String subT = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_subtitle);  
      if (subT != null) {
        title_br.append(": ");
        title_br.append(subT);
      }
      
      // Now clean up any missing ISBN values by pulling in from alternate location
      if (thisAM.get(MetadataField.FIELD_ISBN) == null) {
        String alt_isbn = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_isbn);
        if (alt_isbn != null) {
          thisAM.put(MetadataField.FIELD_ISBN,  alt_isbn);
        }
      }
      
      //Now clean up the cooked publisher - leave the raw publisher as given
      List <String>pubList = thisAM.getRawList(CasaliniMarcXmlSchemaHelper.MARC_publisher);
      String cleanPubString = cleanPublisherName(StringUtil.separatedString(pubList, "; "), false);
      thisAM.replace(MetadataField.FIELD_PUBLISHER, cleanPubString);
      
      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);
      // they're giving us both full book pdfs and chapter pdfs
      String dirName = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_dir);
      String fName = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_file);
      if (dirName == null || dirName == fName) {
        thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
        // attribute the title information correctly
        thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE,  title_br.toString());
      } else {
        thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKCHAPTER);
        // in which case, we inaccurately put the title in to the publication.title field, when it's a chapter (article) title
        thisAM.put(MetadataField.FIELD_ARTICLE_TITLE,  title_br.toString());
      }
      
      // TEMPORARY
      //just for debugging - temporary
      // this causes the preEmit to assume no need for file
      // the publisher can be multiple - so concatenate the values
      String pubDir = cleanPublisherName(StringUtil.separatedString(pubList, "; "), true);
      String dirNum = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_dir);
      String fileNum = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_file);
      // when it's not set then it is the whole book (same dir and filenum)
      // except for two filenumbers which are missing entirely.
      if (dirNum == null) {
        dirNum = fileNum;
      }
      thisAM.put(MetadataField.FIELD_ACCESS_URL, pubDir + "/" + dirNum + "/" + fileNum + ".pdf");
      // TEMPORARY end debugging only
      
      log.debug3("in Casalini Libri postCookProcess");
    }
    
    /*
     *  The publishing data is all over the map
     *  this is a legacy drop - just make it work
     *  by stripping off trailing punctuation and then mapping 
     *  the string to a manually-made map of publishers.
     *  easier to look for unique bits that are in many variants,
     *  rather than mapping the specific strings
     */

    private String cleanPublisherName(String pubval, boolean shortform) {
        String fullvalue = (StringUtils.strip(pubval)).toLowerCase();
        
        if (fullvalue.contains("ateneo")) {
          return (shortform == true) ? "ATENEO" : "Edizioni dell'Ateneo";  
        } else if (fullvalue.contains("casalini libri")) {
          return (shortform == true) ? "CASA" : "Casalini Libri";  
        } else if (fullvalue.contains("istituti editoriali") || 
            fullvalue.contains("macerata") ||
            fullvalue.contains("antenore")) {
          return (shortform == true) ? "IEPI" : "Istituti Editoriali e Poligrafici Internazionali";  
        } else if (fullvalue.contains("giardini")) {
          return (shortform == true) ? "GIARDI" : "Giardini";  
        } else if (fullvalue.contains("cadmo") || 
            fullvalue.contains("amalthea") ||
            fullvalue.contains("wolfsonian") ||
            fullvalue.contains("centro per la filosofia")) {
          return (shortform == true) ? "CADMO" : "Cadmo";  
        } else if (fullvalue.contains("gruppo editor")) {
          return (shortform == true) ? "GEI" : "Gruppo editoriale internazionale";  
        } else {
          return "CLUEB";  
        }
      }

  }
}
