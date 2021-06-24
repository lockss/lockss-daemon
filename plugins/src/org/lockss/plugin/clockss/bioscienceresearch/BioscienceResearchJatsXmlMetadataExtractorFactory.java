/*
 * $Id:$
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

package org.lockss.plugin.clockss.bioscienceresearch;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class BioscienceResearchJatsXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(BioscienceResearchJatsXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper BioscienceResearchJatsPublishingHelper = null;
  static final String JATS_author = "front/article-meta/contrib-group/contrib/name | front/article-meta/contrib-group/contrib/name-alternatives/name[@name-style = \"western\"]";
  static final String JATS_doi = "front/article-meta/article-id[@pub-id-type = \"doi\"]";
  static final String JATS_contrib_article_related_pdf = "front/article-meta/contrib-group/related-article[@related-article-type = \"pdf\"]";

  public class BioscienceResearchJatsPublishingSchemaHelper extends JatsPublishingSchemaHelper {

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
      // get the default map
      Map<String, XmlDomMetadataExtractor.XPathValue> JATS_articleMap = super.getArticleMetaMap();
      // override the JATS_contrib key value
      JATS_articleMap.put(JATS_author, JATS_AUTHOR_VALUE_GIVEN_SUR);
      // add backup JATS related article
      JATS_articleMap.put(JATS_contrib_article_related_pdf, XmlDomMetadataExtractor.TEXT_VALUE);
      return JATS_articleMap;
    }

    // this is almost the same as JATS_AUTHOR_VALUE
    XmlDomMetadataExtractor.NodeValue JATS_AUTHOR_VALUE_GIVEN_SUR = new XmlDomMetadataExtractor.NodeValue() {
      @Override
      public String getValue(Node node) {
        log.debug3("getValue of JATS author using Biooveride");
        NodeList elementChildren = node.getChildNodes();
        // only accept no children if this is a "string-name" node
        if (elementChildren == null &&
            !("string-name".equals(node.getNodeName()))) return null;

        String tsurname = null;
        String tgiven = null;
        String tprefix = null;

        if (elementChildren != null) {
          // perhaps pick up iso attr if it's available
          // look at each child
          for (int j = 0; j < elementChildren.getLength(); j++) {
            Node checkNode = elementChildren.item(j);
            String nodeName = checkNode.getNodeName();
            if ("surname".equals(nodeName)) {
              tsurname = checkNode.getTextContent();
            } else if ("given-names".equals(nodeName)) {
              tgiven = checkNode.getTextContent();
            } else if ("prefix".equals(nodeName)) {
              tprefix = checkNode.getTextContent();
            }
          }
        } else {
          // we only fall here if the node is a string-name
          // no children - just get the plain text value
          tsurname = node.getTextContent();
        }

        // where to put the prefix?
        StringBuilder valbuilder = new StringBuilder();
        //isBlank checks for null, empty & whitespace only
        if (!StringUtils.isBlank(tgiven)) {
          // trim and replace is necessary because for some reason the given names have new lines and tabs before, after
          // and between the names. e.g.
          // "  Syeda    " +
          // "						Muntaka"
          valbuilder.append(tgiven.trim().replaceAll("\\s+", " "));
          if (!StringUtils.isBlank(tsurname)) {
            // might as well do the same for the surname, just in case.
            valbuilder.append(" " + tsurname.trim().replaceAll("\\s+", " "));
          }
        } else {
          log.debug3("no author found");
          return null;
        }
        log.debug3("author found: " + valbuilder.toString());
        return valbuilder.toString();
      }
    };
  }


  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new BioscienceResearchJatsPublishingSourceXmlMetadataExtractor();
  }

  public class BioscienceResearchJatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {
    // using a modifed JatsPublisheringSchemaHelper as defined above
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      log.debug3("in setUpSchema: cu " + cu.getContentType());
      // Once you have it, just keep returning the same one. It won't change.
      if (BioscienceResearchJatsPublishingHelper == null) {
        BioscienceResearchJatsPublishingHelper = new BioscienceResearchJatsPublishingSchemaHelper();
      }
      return BioscienceResearchJatsPublishingHelper;
    }

    /* pdf file has the journal prefixed to it. e.g. 548.xml Scholar548.pdf
    We also must append the url prefix to it.
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                            ArticleMetadata oneAM) {
      List<String> returnList = new ArrayList<>();
      String url_string = cu.getUrl();
      if (url_string.contains(".xml")) {
        returnList.add(url_string.replace(".xml", ".pdf"));
        // rarely found, but doesnt hurt to check.
        returnList.add(url_string.replace(".xml", "%20.pdf"));
      }
      if (oneAM != null) {
        String root_url = url_string.substring(
          0,
          url_string.lastIndexOf("/") + 1
        );
        String pdfName = oneAM.getRaw(BioscienceResearchJatsPublishingSchemaHelper.JATS_article_related_pdf);
        // backup xml path to the pdf file
        String pdfName2 = oneAM.getRaw(JATS_contrib_article_related_pdf);
        String doi = oneAM.getRaw(JATS_doi);
        if (pdfName != null) {
          returnList.add(root_url + pdfName);
        }
        if (pdfName2 != null) {
          returnList.add(root_url + pdfName2);
        }
        if (doi != null) {
          // 10.52586/4931
          returnList.add(root_url +
            doi.substring(
              doi.lastIndexOf("/") + 1
            ) + ".pdf"
          );
        }
      }
      log.debug3(" returnList equals: " + returnList);
      return returnList;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in BioscienceResearch postCookProcess");
      //If we didn't get a valid author names, take a deeper look
      if (thisAM.get(MetadataField.FIELD_PUBLISHER) == null) {
        log.debug3("FIELD_PUBLISHER WAS NULL");
        String JATS_publisher = thisAM.getRaw(BioscienceResearchJatsPublishingSchemaHelper.JATS_pubname);
        if (JATS_publisher != null) {
          thisAM.put(MetadataField.FIELD_PUBLISHER, JATS_publisher);
          log.debug3("ADDED JATS PUBLISHER: '"+JATS_publisher+"' to Metadata");
        }
      } else {
        log.debug3("NOT ADDING JATS FIELD_PUBLISHER");
        log.debug3(" map: "+thisAM.rawKeySet());
        log.debug3("cookMap: "+thisAM.keySet());
      }
    }
  }
}
