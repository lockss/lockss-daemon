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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ElsevierDTD5XmlSourceMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger(ElsevierDTD5XmlSourceMetadataExtractorFactory.class);


  //Use to identify the volume and optional issue and supplement information
  // from the path stored in the dataset.xml file
  //         03781119/v554i2/S0378111914011998/main.xml
  // will have a "v" portion $1, may have "i" portion $2 and/or "s" portion $3
  // there could be a hyphen in the volume or issue portion
  static final Pattern ISSUE_INFO_PATTERN = Pattern.compile("^[^/]+/v([^is/]+)(?:i([^s/]+))?(?:s([^/]+))?/[^/]+/main\\.xml$", Pattern.CASE_INSENSITIVE);

  // Used in modifyAMList to identify the name for the current SET of tar files 
  static final Pattern TOP_METADATA_PATTERN = Pattern.compile("(.*/)[^/]+A\\.tar!/([^/]+)/dataset\\.xml$", Pattern.CASE_INSENSITIVE);
  // used to exclude underlying archives so we don't open them
  static final Pattern NESTED_ARCHIVE_PATTERN = Pattern.compile(".*/[^/]+[A-Z]\\.tar!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", Pattern.CASE_INSENSITIVE);
  static final Pattern DTD_PATTERN = Pattern.compile("^([A-Z]{2})\\s5\\.\\d(\\.\\d)?\\s([A-Z-]+)$", Pattern.CASE_INSENSITIVE);
  private static final String JOURNAL_ARTICLE = "JA";



  // Use this map to determine which node to use for underlying article schema
  static private final Map<String, String> JASchemaMap =
      new HashMap<String,String>();
  static {
    JASchemaMap.put("ARTICLE", "/article/head");
    JASchemaMap.put("SIMPLE-ARTICLE", "/simple-article/simple-head"); 
    JASchemaMap.put("EXAM", "/exam/simple-head");
    JASchemaMap.put("BOOK-REVIEW", "/book-review/book-review-head");
  }

  private static SourceXmlSchemaHelper ElsevierDTD5PublishingHelper = null;

  // one delivery, eg CLKS0000000000003.tar is broken in to 
  //         CLKS0000000000003A.tar, CLKS0000000000003B.tar, ....
  // we extract metadate for the entire group from only one file
  //    CLKS<#>A.tar/dataset.xml (top level metadata for entire delivery)
  // but before we cook the resulting AMList, we do a second iteration
  // over all the tar files looking for low-level "main.xml" files
  // from in the same number group and create a map of
  // underlying articles to their specific tar files.
  // We use this map to check for the existence of the correct "main.pdf" 
  // and to extract the final necessary metadata - article title and authors


  private static final Pattern MAIN_XML_PATTERN = Pattern.compile("/(main)\\.xml$", Pattern.CASE_INSENSITIVE);
  private static final String XML_REPLACEMENT = "/$1.xml";

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new ElsevierDTD5XmlSourceMetadataExtractor();
  }

  public class ElsevierDTD5XmlSourceMetadataExtractor extends SourceXmlMetadataExtractor {



    /* 
     * This must live in the extractor, not the extractor factory
     * There must be one for each SET of related tar files, not per AU
     */
    private final Map<String,String> TarContentsMap;
    public ElsevierDTD5XmlSourceMetadataExtractor() {
      TarContentsMap = new HashMap<String, String>();
    }


    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (ElsevierDTD5PublishingHelper == null) {
        ElsevierDTD5PublishingHelper = new ElsevierDTD5XmlSchemaHelper();
      }
      return ElsevierDTD5PublishingHelper;
    }
    
    /*
     * Choose a schema depending on information in the loaded xml Document tree
     * For Elsevier, this could be a 
     *     journal article schema
     *     book schema
     *     ...
     * There is no obvious attribute, but there is the top node
     * <dataset schema-version="2015.1" xmlns="foo" xsi:schemaLocation="blah"
     * 
     * xmlns="http://www.elsevier.com/xml/schema/transport/ew-xcr/book-2015.1/book-projects"
     * xsi:schemLocation="http://www.elsevier.com/xml/schema/transport/ew-xcr/book-2015.1/book-projects
     * has <dataset>/<dataset-content>/<book-project> node
     * 
     * xmlns="http://www.elsevier.com/xml/schema/transport/ew-xcr/journal-2015.1/issues" 
     * xsi:schemaLocation="http://www.elsevier.com/xml/schema/transport/ew-xcr/journal-2015.1/issues
     * has <dataset>/<dataset-content>/<journal-issue> node  
     *   as well as <dataset>/<dataset-content>/<journal-item> nodes                                                       
     *
     * xmlns="http://www.elsevier.com/xml/schema/transport/ew-xcr/journal-2015.1/items" 
     * xsi:schemaLocation="http://www.elsevier.com/xml/schema/transport/ew-xcr/journal-2015.1/items                                                           
     * has <dataset>/<dataset-content>/<journal-item> 
     *   with no <journal-issue> node
     */
    
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
      if (ElsevierDTD5PublishingHelper == null) {
        ElsevierDTD5PublishingHelper = new ElsevierDTD5XmlSchemaHelper();
      }
      return ElsevierDTD5PublishingHelper;
    }


    /*
     * (non-Javadoc)
     * Before we emit, we want to check if the related PDF file exists AND
     * set the access_url AND 
     * we pull the title and author from the article level main.xml file
     * Use the TarContentsMap we created to associate relative article xml filename 
     * with its actual location in a specific cu.
     */
    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      ArchivalUnit B_au = cu.getArchivalUnit();

      // The schema tells us which raw metadata value points to the correct article xml file
      String key_for_filename = schemaHelper.getFilenameXPathKey();

      // Use the map created earlier to locate the file from it's relative path
      String full_article_md_file = TarContentsMap.get(thisAM.getRaw(key_for_filename));
      log.debug3("full_article_md_file is : " + thisAM.getRaw(key_for_filename));
      if (full_article_md_file == null) {
        return false;
      }

      /*
       * 1. Check for existence of PDF file; otherwise return false & don't emit
       */
      // pdf file has the same name as the xml file, but with ".pdf" suffix

      CachedUrl fileCu = null;
      CachedUrl mdCu = null;;
      try {
        String full_article_pdf = full_article_md_file.substring(0,full_article_md_file.length() - 3) + "pdf"; 
        fileCu = B_au.makeCachedUrl(full_article_pdf);
        log.debug3("Check for existence of " + full_article_pdf);
        if(fileCu != null && (fileCu.hasContent())) {
          thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
          /*
           * 2. Now get the volume, issue and supplement status from the URL
           * key_for_filename looks like this:
           *      03781119/v554i2/S0378111914011998/main.xml
           *   we want the second section which could have a v, i, and s component
           *   v# or v#-# 
           *   i# or i#-#
           *   s#orLtr
           *   will have at least v
           *   exs: /v113-115sC/ or /v58i2-3/ or /v117i6/ or /v39sC/ or /v100i8sS/   
           */
          
          Matcher vMat = ISSUE_INFO_PATTERN.matcher(thisAM.getRaw(ElsevierDTD5XmlSchemaHelper.dataset_article_metadata));
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
          /* 
           * 3. Now get remaining metadata from the article xml file 
           */
          mdCu = B_au.makeCachedUrl(full_article_md_file);
          /*
           * This is defensive programming. It's not clear how this could happen.
           * Since we got the full_article_md_file from the map, we know it's in
           * the AU. So an error here is a sign of a big problem.
           */
          if(mdCu == null || !(mdCu.hasContent())) {
            log.siteWarning("The stored article XML file is no longer accessible");
            return true; 
          }
          extractRemainingMetadata(thisAM, mdCu);
          return true;
        }
      } finally {
        AuUtil.safeRelease(fileCu);
        AuUtil.safeRelease(mdCu);
      }
      log.debug3("No pdf file exists associated with this record - don't emit");
      return false; 
    }


    /*
     *  Pull the article title and author information from the low-level
     *  "main.xml" file 
     *    thisAM - where to put the informmation once it's been extracted
     *    mdCu - the CU from which to pull the information
     *  
     *  If there is some issue extracting from the file, just return
     */
    private void extractRemainingMetadata(ArticleMetadata thisAM,
        CachedUrl mdCu) {

      // Which top node is appropriate for this specific dtd
      String top_node = null;
      String dtdString = thisAM.getRaw(ElsevierDTD5XmlSchemaHelper.dataset_dtd_metadata);
      Matcher mat = DTD_PATTERN.matcher(dtdString);
      if (mat.matches() && JOURNAL_ARTICLE.equals(mat.group(1))) {
        top_node = JASchemaMap.get(mat.group(3));
      }
      //String top_node = SchemaMap.get(dtdString);
      if (top_node == null) {
        log.siteWarning("Unknown type of Elsevier DTD provided for article" + dtdString);
        return; // we can't extract article level metadata (author & title)
      }
      try {
        List<ArticleMetadata> amList = 
            new XPathXmlMetadataParser(null, 
                top_node,
                ElsevierDTD5XmlSchemaHelper.articleLevelMDMap,
                false).extractMetadataFromCu(MetadataTarget.Any(), mdCu);
        /*
         * There should only be ONE top_node per main.xml; don't verify
         * but just access first one.
         */
        if (amList.size() > 0) {
          log.debug3("found article level metadata...");
          ArticleMetadata oneAM = amList.get(0);
          String rawVal = oneAM.getRaw(ElsevierDTD5XmlSchemaHelper.common_title);
          if (rawVal != null) {
            thisAM.putRaw(ElsevierDTD5XmlSchemaHelper.common_title, rawVal);
          } else {
            // a simple-article might use document heading, like "Book Review" as title
            rawVal = oneAM.getRaw(ElsevierDTD5XmlSchemaHelper.common_dochead);
            if (rawVal != null) {
              // store it in the title anyway, it only exists if title doesn't
              thisAM.putRaw(ElsevierDTD5XmlSchemaHelper.common_title, rawVal);
            }
          }
          rawVal = oneAM.getRaw(ElsevierDTD5XmlSchemaHelper.common_author_group);
          if ( rawVal != null) {
            thisAM.putRaw(ElsevierDTD5XmlSchemaHelper.common_author_group, rawVal);
          }
          // in case the top file didn't have a date for the issue, use this article's copyright
          rawVal = oneAM.getRaw(ElsevierDTD5XmlSchemaHelper.common_copyright);
          if ( rawVal != null) {
            thisAM.putRaw(ElsevierDTD5XmlSchemaHelper.common_copyright, rawVal);
          }
        } else {
          log.debug3("no md extracted from " + mdCu.getUrl());
        }
      } catch (XPathExpressionException e) {
        log.debug3("Xpath expression exception:",e); // this is a note to the PLUGIN writer!
      } catch (IOException e) {
        // We going to keep going and just not extract from this file
        log.siteWarning("IO exception loading article level XML file", e);
      } catch (SAXException e) {
        // We going to keep going and just not extract from this file
        log.siteWarning("SAX exception loading article level XML file", e);
      }      
    }

    /* 
     * This will get called ONCE for each dataset.xml 
     * and therefore once per delivery set (CLKS#A.tar, CLKS#B.tar...)
     *    with the same unique file number
     *  Use this opportunity to generate a map identifying which specific tar a particular
     *   article lives in
     *  We generate the ARTICLE_METADATA_PATTERN here because we need the current cu, to limit
     *   the results to just the set of tar files with the same unique file number.  
     */
    protected Collection<ArticleMetadata> modifyAMList(SourceXmlSchemaHelper helper,
        CachedUrl datasetCu, List<ArticleMetadata> allAMs) {

      Matcher mat = TOP_METADATA_PATTERN.matcher(datasetCu.getUrl());
      Pattern ARTICLE_METADATA_PATTERN = null;
      if (mat.matches()) {
        // must create this here because it is specific to this tar set
        String pattern_string = "^" + mat.group(1) + mat.group(2) + "[A-Z]\\.tar!/" + mat.group(2) + "/.*/main\\.xml$";
        log.debug3("Iterate and find the pattern: " + pattern_string);
        ARTICLE_METADATA_PATTERN = Pattern.compile(pattern_string, Pattern.CASE_INSENSITIVE);

        // Limit the scope of the iteration to only those TAR archives that share this tar number
        List<String> rootList = createRootList(datasetCu, mat);
        // Now create the map of files to the tarfile they're in
        ArchivalUnit au = datasetCu.getArchivalUnit();
        SubTreeArticleIteratorBuilder articlebuilder = new SubTreeArticleIteratorBuilder(au);
        SubTreeArticleIterator.Spec artSpec = articlebuilder.newSpec();
        // Limit it just to this group of tar files
        artSpec.setRoots(rootList); 
        artSpec.setPattern(ARTICLE_METADATA_PATTERN); // look for url-ending "main.xml" files
        artSpec.setExcludeSubTreePattern(NESTED_ARCHIVE_PATTERN); //but do not descend in to any underlying archives
        artSpec.setVisitArchiveMembers(true);
        articlebuilder.setSpec(artSpec);
        articlebuilder.addAspect(MAIN_XML_PATTERN,
            XML_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

        for (SubTreeArticleIterator art_iterator = articlebuilder.getSubTreeArticleIterator();
            art_iterator.hasNext(); ) {
          // because we haven't set any roles, the AF will be what the iterator matched
          String article_xml_url = art_iterator.next().getFullTextCu().getUrl();
          log.debug3("tar map iterator found: " + article_xml_url);
          int tarspot = StringUtil.indexOfIgnoreCase(article_xml_url, ".tar!/");
          int dividespot = StringUtil.indexOfIgnoreCase(article_xml_url, "/", tarspot+6);
          TarContentsMap.put(article_xml_url.substring(dividespot + 1), article_xml_url);
          log.debug3("TarContentsMap add key: " + article_xml_url.substring(dividespot + 1));
        }
      } else {
        log.warning("ElsevierDTD5: Unable to create article-level map for " + datasetCu.getUrl() + " - metadata will not include article titles or useful access.urls");
      }
      return  allAMs;
    }
  }

  
  /*
   * There are a few cases where a date isn't provided in the top file. 
   * In this case fall back to the common copyright date if its there in the raw
   * data
   */
  protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
      CachedUrl cu, ArticleMetadata thisAM) {

    log.debug3("in postEmitProcess");
    //If we didn't get a valid date, check the common_copyright                                                                                                                                 
    if (thisAM.get(MetadataField.FIELD_DATE) == null) {
      log.debug3("using the main.xml copyright date");
      thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(ElsevierDTD5XmlSchemaHelper.common_copyright));
    }
  }


  /* 
   * Create a list of strings, one for each unique tar within this tarset.
   * The passed in CU is for the A file of the list, the one that dataset.xml came off of.
   */

  private List<String> createRootList(CachedUrl cu, Matcher mat) {
    String test_tar_name;
    String tar_begin = mat.group(1) + mat.group(2);
    List<String> rootList = new ArrayList<String>();
    // we know this one exists!
    rootList.add(tar_begin + "A.tar");
    for(char alphabet = 'B'; alphabet <= 'Z';alphabet++) {
      test_tar_name = tar_begin + String.valueOf(alphabet) + ".tar";
      CachedUrl testCu = cu.getArchivalUnit().makeCachedUrl(test_tar_name);
      if ((testCu == null) || (!testCu.hasContent())){
        break;
      } else {
        rootList.add(test_tar_name);
      }
    }
    log.debug3("ROOT templates for inner iterator is: " + rootList.toString());
    return rootList;
  }

}
