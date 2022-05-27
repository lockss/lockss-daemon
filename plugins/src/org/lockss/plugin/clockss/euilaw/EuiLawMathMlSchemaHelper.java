package org.lockss.plugin.clockss.euilaw;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

public class EuiLawMathMlSchemaHelper implements SourceXmlSchemaHelper {

  /*
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE article>
<article xmlns:mml="http://www.w3.org/1998/Math/MathML" xmlns:xlink="http://www.w3.org/1999/xlink" article-type="research-article">
    <head>
      <timestamp>2021010100000003</timestamp>
      <depositor>
          <depositor_name>Crossref</depositor_name>
          <email_address>pfeeney@crossref.org</email_address>
      </depositor>
  </head>
  <front>
    <journal-meta>
      <journal-id journal-id-type="nlm-ta">INTERNATIONAL JOURNAL OF LAW: “LAW AND WORLD“</journal-id>
      <journal-title>INTERNATIONAL JOURNAL OF LAW: “LAW AND WORLD“</journal-title>
      <issn pub-type="ppub">2346-7916</issn>
      <issn pub-type="epub">2587-5043</issn>
      <publisher>
        <publisher-name>INTERNATIONAL JOURNAL OF LAW: “LAW AND WORLD“</publisher-name>
      </publisher>
    </journal-meta>
    <article-meta>
      <article-id pub-id-type="doi">doi.org/10.36475/8.1.2</article-id>
      <article-categories>
        <subj-group>
          <subject>Domestic violence, Judicial approach, Live-in realationship, Social changes.</subject>
        </subj-group>
      </article-categories>
      <title-group>
        <article-title>Judicial Approach Amidst Growing Live-in Relationship</article-title>
        <subtitle>0</subtitle>
      </title-group>
      <contrib-group>
        <contrib contrib-type="author">
          <name name-style="western">
            <surname>Sharma</surname>
            <given-names> Arpit</given-names>
          </name>
          <aff>Assistant Professor, Institute of Law, Nirma University, India, Ahmedabad</aff>
        </contrib>
        <contrib contrib-type="author">
          <name name-style="western">
            <surname>Sinta</surname>
            <given-names>Umpo</given-names>
          </name>
          <aff>Assistant Professor, Jarbom Gamlin Government Law College, India, Itanagar</aff>
        </contrib>
      </contrib-group>
      <pub-date pub-type="ppub">
        <month>03</month>
        <year>2022</year>
      </pub-date>
      <pub-date pub-type="epub">
        <day>31</day>
        <month>03</month>
        <year>2022</year>
      </pub-date>
      <volume>8</volume>
      <issue>1</issue>
      <permissions>
        <copyright-year>2022</copyright-year>
        <license license-type="open-access" xlink:href="http://creativecommons.org/licenses/by/2.5/"><p>This is an open-access article distributed under the terms of the Creative Commons Attribution License, which permits unrestricted use, distribution, and reproduction in any medium, provided the original work is properly cited.</p></license>
      </permissions>
      <related-article related-article-type="companion" vol="2" page="e235" id="RA1" ext-link-type="pmc">
			<article-title>Judicial Approach Amidst Growing Live-in Relationship</article-title>
      </related-article>
	  <abstract abstract-type="toc">
		<p>
			Purpose: The purpose of this article is to comprehend the Judiciary's response and apprehend the pace of change in the social dynamics of the society about live-in relations. This article also comprehends the inter-linkage of the concept of live-in relationships vis-à-vis other laws, which require more clarity. Design/Methodology: In this study, the research brings down the concept of a live-in relationship by analyzing various judgments down the link from pre-independence to post-independence through the Protection of Women from Domestic Violence Act, 2005 (PWDWA). This study analyzed the data of the Supreme Court and High Courts down the line from 2010 to 2021 and analyzed the high courts that have the maximum number of cases. The approach is to identify how Judiciary interprets the live-in relationship in different facts under the light of various laws. Practical Implication: This study helps to identify the way forward in the absence of the legislative framework on live-in relationships through the interpretation by the Hon’ble Supreme Court and other High Courts. Originalities/Value: This study is predominantly based upon the question placed before the larger bench by Madras High Court as to whether the woman is entitled to get a pension in a live-in relationship. The resultant of this work is based upon the opinion of the existing judgment that brings support to reach out to the conclusion. Outcome/Finding: The dimension of the social changes and changing the mindset of youth is an accepted fact to give legal recognition to live-in relationships. The Court has paved the way for a live-in relationship in property, maintenance, and domestic violence. This is high time for the legislature to code the separate statute for the live-in relationship so that the women from this relationship have not exploited due to the non-presence of the statute.
		</p>
		</abstract>
    </article-meta>
  </front>
  <body></body>
</article>

  */

  protected static final String journal_meta_path = "article/front/journal-meta/";
  private static final String journal_title = journal_meta_path + "journal-title";
  private static final String journal_id = journal_meta_path + "journal-id";
  private static final String issn = journal_meta_path + "issn[@type = \"ppub\"]";
  private static final String eissn = journal_meta_path + "issn[@type = \"epub\"]";
  private static final String publisher = journal_meta_path + "publisher/publisher-name";

  protected static final String article_meta_path = "article/front/article-meta/";
  private static final String doi = article_meta_path + "article-id[@pub-id-type = \"doi\"]";
  private static final String article_title = article_meta_path + "title-group/article-title";
  private static final String article_subtitle = article_meta_path + "title-group/subtitle"; // may be 0, instead of null...
  private static final String contrib_author = article_meta_path + "/contrib-group/contrib[@contrib-type = \"author\"]/name";
  private static final String author_affiliation = article_meta_path + "/contrib-group/contrib[@contrib-type = \"author\"]/aff";
  private static final String art_pubdate = article_meta_path + "pub-date[@pub-type = \"ppub\"]";
  private static final String issue = article_meta_path + "issue";
  private static final String volume = article_meta_path + "volume";
  private static final String art_abstract = article_meta_path + "abstract";
  private static final String keywords = article_meta_path + "article-categories/subj-group/subject";



  /*
    <name name-style="western">
      <surname>Sharma</surname>
      <given-names> Arpit</given-names>
    </name>
   */
  static private final XmlDomMetadataExtractor.NodeValue AUTHOR_NAME_BUILDER = new XmlDomMetadataExtractor.NodeValue() {
    @Override
    public String getValue(Node node) {

      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String given = null;
      String surname = null;
      String midname = null;
      // look at each child of the node for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("given-names".equals(nodeName)) {
          given = checkNode.getTextContent();
        } else if ("surname".equals(nodeName) ) {
          surname = checkNode.getTextContent();
        } else if ("middle".equals(nodeName) ) {
          midname = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (surname != null) {
        valbuilder.append(surname);
        if (given != null) {
          valbuilder.append(", " + given);
          if (midname != null) {
            valbuilder.append(" " + midname);
          }
        }
      } else {
        return null;
      }
      return valbuilder.toString();
    }
  };


  /*
      <pub-date pub-type="ppub">
        <month>03</month>
        <year>2022</year>
      </pub-date>
      <pub-date pub-type="epub">
        <day>31</day>
        <month>03</month>
        <year>2022</year>
      </pub-date>
   */
  static private final XmlDomMetadataExtractor.NodeValue DATE_BUILDER = new XmlDomMetadataExtractor.NodeValue() {
    @Override
    public String getValue(Node node) {

      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String year = null;
      String month = null;
      String day = null;
      // look at each child of the node for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("year".equals(nodeName)) {
          year = checkNode.getTextContent();
        } else if ("month".equals(nodeName) ) {
          month = checkNode.getTextContent();
        } else if ("day".equals(nodeName) ) {
          day = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (year != null) {
        valbuilder.append(year);
        if (month != null) {
          valbuilder.append("-" + month);
          if (day != null) {
            valbuilder.append("-" + day);
          }
        }
      } else {
        return null;
      }
      return valbuilder.toString();
    }
  };


  static private final Map<String, XmlDomMetadataExtractor.XPathValue>
      articleMap = new HashMap<String, XmlDomMetadataExtractor.XPathValue>();
  static {
    // article specific stuff
    articleMap.put(art_pubdate, DATE_BUILDER);
    articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(journal_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(contrib_author, AUTHOR_NAME_BUILDER);
    articleMap.put(issue, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(volume, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(keywords, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  static private final Map<String, XmlDomMetadataExtractor.XPathValue>
      globalMap = null;

  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(article_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(contrib_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(art_pubdate, MetadataField.FIELD_DATE);
    cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(issue, MetadataField.FIELD_ISSUE);
    cookMap.put(volume, MetadataField.FIELD_VOLUME);
    cookMap.put(keywords, MetadataField.FIELD_KEYWORDS);
  }


  @Override
  public Map<String, XmlDomMetadataExtractor.XPathValue> getGlobalMetaMap() {
    return null; //globalMap;
  }

  /**
   * return Chinese University of Hong Kong article paths representing metadata of interest
   */
  @Override
  public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
    return articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return null;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  @Override
  public String getFilenameXPathKey() {
    return null;
  }
}