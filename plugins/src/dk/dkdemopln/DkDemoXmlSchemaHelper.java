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

package dk.dkdemopln;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;


/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  the Dk Mods XML format
 *  The pdf filenames use the same name as the .xml file
 *  There is only one record for each file
 *  @author etenbrink
 */
public class DkDemoXmlSchemaHelper implements SourceXmlSchemaHelper {
  
  static Logger log = Logger.getLogger(DkDemoXmlSchemaHelper.class);
  
  
  /* 
   *  specific XPATH key definitions that we care about
   *  mods xsi:schemaLocation="http://oai.dads.dtic.dk/schema/mods-3-5-ds-dtic-dk-extended.xsd"
   */
  
  private static String DK_DEMO_mods = "/mods";
  
  /* these are all relative to the /mods node */
  private static String DK_DEMO_record_id = "identifier[@type=\"ds.dtic.dk:id:pub:dads:recordid\"]";
  private static String DK_DEMO_doi = "identifier[@type=\"ds.dtic.dk:id:pub:dads:doi\"]";
  private static String DK_DEMO_add = "titleInfo[not(@type)]/title[@lang=\"eng\"]";
  
/*
   *  The following variables are needed to construct the XPathXmlMetadataParser
   */
  
  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> DK_DEMO_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    DK_DEMO_articleMap.put(DK_DEMO_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    DK_DEMO_articleMap.put(DK_DEMO_record_id, XmlDomMetadataExtractor.TEXT_VALUE);
    DK_DEMO_articleMap.put(DK_DEMO_add, XmlDomMetadataExtractor.TEXT_VALUE);
  }
  
  /* 2. Each item (article) has its own XML file */
  static private final String DK_DEMO_articleNode = DK_DEMO_mods; 
  
  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name, etc. (per VR)
    cookMap.put(DK_DEMO_record_id, MetadataField.DC_FIELD_IDENTIFIER);
    cookMap.put(DK_DEMO_doi, MetadataField.FIELD_DOI);
    cookMap.put(DK_DEMO_add, MetadataField.FIELD_ARTICLE_TITLE);
  }
  
  
  /**
   * Does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return null;
  }
  
  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return DK_DEMO_articleMap;
  }
  
  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return DK_DEMO_articleNode;
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
