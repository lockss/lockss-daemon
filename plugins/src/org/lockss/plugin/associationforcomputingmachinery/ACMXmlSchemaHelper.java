/*
 * $Id$
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.lang.StringUtils;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  ACM source files
 *  
 */
public class ACMXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(ACMXmlSchemaHelper.class);
  static StringBuilder urlName = new StringBuilder();

  private static final String NAME_SEPARATOR = ", ";
  /* 
   *  ACM specific node evaluators to extract the information we want
   */
  /**
   *  Journal or Proceedings Title
   *  Note that this will only be EITHER
   *    <journal_name>ACM Journal of Computer Documentation (JCD)</journal_name>
   *  OR
   *  <proceeding_rec> which has child nodes that hold the information
   *    <acronym>NordiCHI '10</acronym>
   *    <proc_desc>Proceedings of the 6th Nordic Conference</proc_desc>
   *    <proc_title>Human-Computer Interaction</proc_title>
   *    <proc_subtitle>Extending Boundaries</proc_subtitle>
   *  from dl.acm.org: 
   *  acronym: proc_desc on proc_title: proc_subtitle
   *  
   */  
  static private final NodeValue ACM_JOURNAL_TITLE_VALUE = new NodeValue() {   
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of ACM journal/proceedings title");
      String nodeName = node.getNodeName();
      String title = null;

      /* the incoming node will either be the journal_name or 
       * catenated proceedings title
       * 
       */
      if ("journal_name".equalsIgnoreCase(nodeName)) {
        title = node.getTextContent();
        return title;
      } else if ("proceeding_rec".equalsIgnoreCase(nodeName)) {
        String ptitle = null;
        String pdesc = null;
        String pacro = null;
        String psubt = null;
        NodeList childNodes = node.getChildNodes();
        for (int m = 0; m < childNodes.getLength(); m++) {
          Node infoNode = childNodes.item(m);
          nodeName = infoNode.getNodeName();
          if ("acronym".equalsIgnoreCase(nodeName)) {
            pacro = infoNode.getTextContent();
          } else if ("proc_desc".equalsIgnoreCase(nodeName)) {
            pdesc = infoNode.getTextContent();
          } else if ("proc_title".equalsIgnoreCase(nodeName)) {
            ptitle = infoNode.getTextContent();
          } else if ("proc_subtitle".equalsIgnoreCase(nodeName)) {
            psubt = infoNode.getTextContent();
          } else
            continue;
        }
        // now construct the title from available pieces
        title = makeProceedingsTitle(pacro, pdesc, ptitle, psubt);
      }
      return title;
    }
  };
  
  /* makeProceedingsTitle(
   *  acro - ACM's acronym for the conference
   *  desc - proceedings description
   *  title - proceedings title
   *  subtitle - proceedings subtitle)
   * proc_acronym: proc_desc on proc_title: proc_subtitle
   *  if all elements aren't there, put together something credible
   */
  public static String makeProceedingsTitle(String acro, String desc, String title, String subt) {
    StringBuilder ptitle = new StringBuilder();
    Boolean hasAcro = false;
    Boolean hasDesc = false;

    if (!(hasAcro = StringUtils.isEmpty(acro))) {
      ptitle.append(acro);      
    }
    if (!(hasDesc = StringUtils.isEmpty(desc))) {
      if (!hasAcro) {
        ptitle.append(": ");
      }
      ptitle.append(desc);
    }
    if (!StringUtils.isEmpty(title)) {
      if (!hasDesc) {
        ptitle.append(" on ");
      } else if (!hasAcro) {
        ptitle.append(": ");
      } 
      ptitle.append(title);
    }
    if (!StringUtils.isEmpty(subt)) {
      if (ptitle.length() > 0) {
        ptitle.append(": ");
      }
      ptitle.append(subt);
    }

    return ptitle.toString();
  }
  
  /*
   * AUTHOR information example
   * NODE = <au>
   * /periodical/section/article_rec/authors/au
   * <person_id=PERSONID
   * <seq_no>1
   * <first_name>=John
   * <middle_name>=A.
   * <last_name>=AuthorName
   * <suffix>
   * <affiliation=Writer
   * <role>=Author
   */
  static private final NodeValue ACM_AUTHOR_VALUE = new NodeValue() {   
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of ACM contributor");
      String seq = null;
      String fname = null;
      String mname = null;
      String lname = null;
      boolean isAuthor = false;
      NodeList childNodes = node.getChildNodes(); 
      StringBuilder names = new StringBuilder();
      if (urlName.length() > 0)
        urlName = urlName.delete(0, urlName.length()-1);
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        String nodeName = infoNode.getNodeName();
        if ("first_name".equalsIgnoreCase(nodeName)) {
          fname = infoNode.getTextContent();
        } else if ("middle_name".equalsIgnoreCase(nodeName)){
          mname = infoNode.getTextContent();
        } else if("last_name".equalsIgnoreCase(nodeName)){
          lname = infoNode.getTextContent();
        } else if ("role".equalsIgnoreCase(nodeName)) {
          String type = infoNode.getTextContent();
          if ("Author".equalsIgnoreCase(type)) {
            isAuthor = true;
          } else
            continue;
        } else 
          continue;
      }
      if (isAuthor) {
        if (!lname.isEmpty()) {
          names.append(lname);
          if (!fname.isEmpty()) {
            names.append(NAME_SEPARATOR);
          }
        }
        if (!fname.isEmpty()) {
          names.append(fname);
          if (!mname.isEmpty()) {
            names.append(' ');
          }
        }
        if(!mname.isEmpty()){
          names.append(mname);
        }
      } 
      if (names.length() == 0) {
        log.debug3("no valid contributor found");
        return null;
      }
      return names.toString();
    }
  };

  /*
   * get the last name of the first (primary) author
   *  to be able to create the url name
   * NODE = <au>
   * /periodical/section/article_rec/authors/au
   * <person_id=PERSONID
   * <seq_no>1
   * <first_name>=John
   * <middle_name>=A.
   * <last_name>=AuthorName
   */

  /* 
   *  ACM specific XPATH key definitions that we care about
   */

  /* Under an item node, the interesting bits live at these relative locations */
  // periodical only:

  private static final String ACM_issn = "//issn";
  private static final String ACM_eissn = "//eissn";
  private static final String ACM_journal_id = "//journal_code";
  // using ACM_journal_name for both proceeding and periodical name
  //private static final String ACM_journal_name = "//journal_name | //proc_desc";
  private static final String ACM_journal_name = "//journal_name | //proceeding_rec";
  /* vol, issue */
  private static final String ACM_issue = ".//issue";
  private static final String ACM_vol = ".//volume";
  // proceedings only:
  private static final String ACM_isbn = "//isbn";

  // common to both proceedings and periodicals
  /* components under Publisher */
  private static final String ACM_publisher_name = "./publisher-name";

  /* article title, id, doi, pubdate*/
  private static final String ACM_article_title =  "./title";
  private static final String ACM_article_id = "./article_id";
  private static final String ACM_doi = "./doi_number";
  private static final String ACM_art_pubdate = "./article_publication_date";

  /* filename (relative) */
  private static final String ACM_article_url = "./fulltext/file/fname";
  /* filenames not relative */
  private static final String ACM_fmatter = "//front_matter/fm_file";
  private static final String ACM_bmatter = "//back_matter/bm_file";
  /* xpath  author */
  private static final String ACM_author =  "./authors/au";

  private static final String ACM_startpage =  "./article_rec/page_from";
  private static final String ACM_endpage = "./article_rec/page_to";

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  ACM_articleMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    ACM_articleMap.put(ACM_doi, XmlDomMetadataExtractor.TEXT_VALUE);    
    ACM_articleMap.put(ACM_article_url, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_articleMap.put(ACM_article_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_articleMap.put(ACM_article_id, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_articleMap.put(ACM_art_pubdate, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_articleMap.put(ACM_author, ACM_AUTHOR_VALUE);
    ACM_articleMap.put(ACM_startpage, XmlDomMetadataExtractor.TEXT_VALUE);
    ACM_articleMap.put(ACM_endpage, XmlDomMetadataExtractor.TEXT_VALUE);
  }


  /* 2. Each item (book) has its own subNode */
  /* some ACM pubs do not have a "section" level between 
   * /(proceeding|periodical)/ and /article_rec
   * Also, the '|' in the ACM_topNode is an Xpath AND, not a C OR... 
   * but it works since each xml file is either proceeding OR periodical
   */
  static private final String ACM_topNode = "/periodical//article_rec | /proceeding//article_rec";

  /* 3. in ACM, there some global information we gather */ 
  static private final Map<String,XPathValue>     
  ACM_globalMap = new HashMap<String,XPathValue>();
  static {
    // periodical only
    ACM_globalMap.put(ACM_issn, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_globalMap.put(ACM_eissn, XmlDomMetadataExtractor.TEXT_VALUE);
    ACM_globalMap.put(ACM_journal_id, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_globalMap.put(ACM_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_globalMap.put(ACM_vol, XmlDomMetadataExtractor.TEXT_VALUE); 

    // proceedings only
    ACM_globalMap.put(ACM_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    //ACM_globalMap.put(ACM_proceeding_id, XmlDomMetadataExtractor.TEXT_VALUE);

    // periodical + proceedings
    // proceeding_id = either journal_name or proceeding description
    //ACM_globalMap.put(ACM_journal_name, XmlDomMetadataExtractor.TEXT_VALUE);
    ACM_globalMap.put(ACM_journal_name, ACM_JOURNAL_TITLE_VALUE);
    ACM_globalMap.put(ACM_publisher_name, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_globalMap.put(ACM_fmatter, XmlDomMetadataExtractor.TEXT_VALUE);
    ACM_globalMap.put(ACM_bmatter, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // periodical specific schema
    cookMap.put(ACM_issn, MetadataField.FIELD_ISSN);
    cookMap.put(ACM_eissn, MetadataField.FIELD_EISSN);
    cookMap.put(ACM_vol, MetadataField.FIELD_VOLUME);
    cookMap.put(ACM_issue, MetadataField.FIELD_ISSUE);

    // proceedings specific schema
    cookMap.put(ACM_isbn, MetadataField.FIELD_ISBN);

    // both
    cookMap.put(ACM_doi, MetadataField.FIELD_DOI);    

    // ACM_journal_name is used for both/either periodical or proceeding name
    //cookMap.put(ACM_journal_name, MetadataField.FIELD_PUBLICATION_TITLE);
    // using deprecated FIELD_JOURNAL_TITLE until updated everywhere
    cookMap.put(ACM_journal_name, MetadataField.FIELD_JOURNAL_TITLE);
    cookMap.put(ACM_article_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(ACM_article_id, MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
    cookMap.put(ACM_author, MetadataField.FIELD_AUTHOR);
    // taking out publisher name - will be added from TDB
    cookMap.put(ACM_publisher_name, MetadataField.FIELD_PUBLISHER);
    cookMap.put(ACM_art_pubdate, MetadataField.FIELD_DATE);
    // these "urls" are relative filenames - must fill in later
    cookMap.put(ACM_article_url, MetadataField.FIELD_ACCESS_URL);
  }

  /**
   * ACM does have some global info (journal || issue)
   * putting front_matter and backmatter in globalMap
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return ACM_globalMap;
  }

  /**
   * return ACM article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return ACM_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return ACM_topNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /* (non-Javadoc)
   * @see org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractorHelper#getDeDuplicationXPathKey()
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractorHelper#getConsolidationXPathKey()
   */
  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractorHelper#getFilenameXPathKey()
   */
  @Override
  public String getFilenameXPathKey() {

    return ACM_article_url;
  }

}