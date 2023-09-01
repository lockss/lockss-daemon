/*
 * $Id$
 */

/*

 Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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

package edu.fcla.plugin.arkivoc;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class ArkivocXmlSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(ArkivocXmlSchemaHelper.class);
  
  /*
  <?xml version="1.0" encoding="UTF-8"?>
  <records>
       <record>
          <id>19-11069LU</id>
          <issn>1551-7004</issn>
          <eissn>1551-7012</eissn>
          <journal>ARKIVOC</journal>
          <volume>2019</volume>
          <issue>1</issue>
          <publication-date>2020-02-03</publication-date>
          <start-page>340</start-page>
          <end-page>352</end-page>
          <doi>https:/doi.org/10.24820/ark.5550190.p011.069</doi>
          <title>Methods of synthesis of Pimavanserin: the first drug approved for the treatment of Parkinson's disease psychosis (PDP)</title>
          <authors>
             <author>Nader Robin Al Bujuq</author>
          </authors>
          <html-url>https:/www.arkat-usa.org/arkivoc-journal/browse-arkivoc/ark.5550190.p011.069</html-url>
          <ris-url>https:/www.arkat-usa.org/get-ris/11069/arkivoc.19-11069LU.ris</ris-url>
          <mainmanuscript-url>https:/www.arkat-usa.org/get-file/68736/19-11069LU+published+mainmanuscript.pdf</mainmanuscript-url>
          <supplementary-url />
       </record>
    </records>
    */

  private static final String article = "/records/record";

  protected static final String RECORD_ID =  article + "/id";

  private static final String issn =  article + "/issn";
  private static final String eissn = article + "/eissn";
  private static final String volume = article + "/volume";
  private static final String issue = article + "/issue";
  private static final String title = article + "/title";
  private static final String author = article + "/author";
  private static final String pubdate = article + "/publication-date";

  private static final String doi = article + "/doi";

  protected static final String accessurl = article + "/mainmanuscript-url";
  private static final String startpage = article + "/start-page";
  private static final String endpage = article + "/end-page";
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  private static final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  static {
      articleMap.put(RECORD_ID, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(issn, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(eissn, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(volume, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(issue, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(title, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(author, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(pubdate, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(doi, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(accessurl, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(startpage, XmlDomMetadataExtractor.TEXT_VALUE);
      articleMap.put(endpage, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  private static final String articleNode = article; 

  /* 3. in MARCXML there is no global information because one file/article */
  private static final Map<String,XPathValue> globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {

      cookMap.put(issn, MetadataField.FIELD_ISSN);
      cookMap.put(eissn, MetadataField.FIELD_EISSN);
      cookMap.put(volume, MetadataField.FIELD_VOLUME);
      cookMap.put(issue, MetadataField.FIELD_ISSUE);
      cookMap.put(title, MetadataField.FIELD_ARTICLE_TITLE);
      cookMap.put(author, MetadataField.FIELD_AUTHOR);
      cookMap.put(pubdate, MetadataField.FIELD_DATE);
      cookMap.put(doi, MetadataField.FIELD_DOI);
      cookMap.put(startpage, MetadataField.FIELD_START_PAGE);
      cookMap.put(endpage, MetadataField.FIELD_END_PAGE);
      cookMap.put(accessurl, MetadataField.FIELD_ACCESS_URL);
      // Read this one from tdb file
      //cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
      //cookMap.put(publisher, MetadataField.FIELD_PROVIDER);
  }


  /**
   * MARCXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return articleNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /**
   * No duplicate data 
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * No consolidation required
   */
  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * The filenames are the same as the XML filenames with .pdf suffix
   */
  @Override
  public String getFilenameXPathKey() {
    return null;
  }

}
