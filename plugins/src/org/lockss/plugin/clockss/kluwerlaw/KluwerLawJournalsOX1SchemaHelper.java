/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.clockss.kluwerlaw;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.CrossRefSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

/*
 * <!DOCTYPE oases PUBLIC "-//KLI//DTD OASES-XML//EN" "oases-xml.dtd">
 * <oases version="1">
 *   <oasis pdept="kli" pips="204497" jcode="AILA" version="published" setter="Typesetter">
 *     <dbinfo pid="KLI" mod-day="" mod-month="" mod-year="" annual="" />
 *     <jrnlinfo>
 *       <pnm>Kluwer Law International</pnm>
 *       <loc>Alphen aan den Rijn, The Netherlands</loc>
 *       <jtl>Air and Space Law</jtl>
 *       <jbst>
 *       </jbst>
 *       <jcode>AILA</jcode>
 *       <issn>0927-3379</issn>
 *     </jrnlinfo>
 *     <issueinfo>
 *       <binding vid="24" lvid="" iid=" 1" liid="" supplement="No" />
 *       <coverdate>19990201</coverdate>
 *     </issueinfo>
 *     <artinfo>
 *       <artty type="research-article" />
 *       <pages ppct="14" ppf="3" ppl="16" />
 *       <crn>Kluwer Law International</crn>
 *       <atl>Air Carrier Liability and State Responsibility for the Carriage of Inadmissible Persons and Refugees</atl>
 *       <au>
 *         <fnms>R.I.R.</fnms>
 *         <snm>Abeyratne</snm>
 *         <aff>
 *         </aff>
 *       </au>
 *       <abs>
 *       </abs>
 *       <nokwds>
 *       </nokwds>
 *       <url>https://kluwerlawonline.com/journalarticle/Air+and+Space+Law/24.1/204497</url>
 *     </artinfo>
 *   </oasis>
 * </oases>
 */

public class KluwerLawJournalsOX1SchemaHelper implements SourceXmlSchemaHelper {
    private static final Logger log = Logger.getLogger(KluwerLawJournalsOX1SchemaHelper.class);


    static private final XmlDomMetadataExtractor.NodeValue AUTHOR_NAME = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of CrossRef person name");
            NodeList elementChildren = node.getChildNodes();
            if (elementChildren == null) return null;

            String tgiven = null;
            String tsurname = null;
            // look at each child of the TitleElement for information
            for (int j = 0; j < elementChildren.getLength(); j++) {
                Node checkNode = elementChildren.item(j);
                String nodeName = checkNode.getNodeName();
                if ("snm".equals(nodeName)) {
                    tgiven = checkNode.getTextContent();
                } else if ("fnms".equals(nodeName) ) {
                    tsurname = checkNode.getTextContent();
                }
            }

            StringBuilder valbuilder = new StringBuilder();
            if (tsurname != null) {
                valbuilder.append(tsurname);
                if (tgiven != null) {
                    valbuilder.append(", " + tgiven);
                }
            } else {
                log.debug3("no name found");
                return null;
            }
            log.debug3("name found: " + valbuilder.toString());
            return valbuilder.toString();
        }
    };

    // from the immediately preceding sibling -
    private static String publisher = "/oases/oasis/jrnlinfo/pnm";
    private static String pub_title = "/oases/oasis/jrnlinfo/jtl";
    private static String pub_issn = "/oases/oasis/jrnlinfo/issn";

    private static String pub_date = "/oases/oasis/issueinfo/coverdate";
    private static String pub_volume = "/oases/oasis/issueinfo/binding/@vid";
    private static String pub_issue = "/oases/oasis/issueinfo/binding/@iid";

    private static String art_title = "/oases/oasis/artinfo/atl";
    //no subtitle?
    private static String art_contrib = "/oases/oasis/artinfo/au";

    public static String art_sp = "/oases/oasis/artinfo/pages/@ppf";
    public static String art_lp = "/oases/oasis/artinfo/pages/@ppl";

    public static String elocation_id = "/oases/oasis/artinfo/url";

    /*
     *  The following 3 variables are needed to use the XPathXmlMetadataParser
     */

    /* 1.  MAP associating xpath & value type definition or evaluator */
    static private final Map<String, XmlDomMetadataExtractor.XPathValue>
            articleMap = new HashMap<String, XmlDomMetadataExtractor.XPathValue>();

    static {
        articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_issn, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_date, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_contrib, AUTHOR_NAME);
        articleMap.put(elocation_id, XmlDomMetadataExtractor.TEXT_VALUE);
    }

    /* 2.  Top level per-article node */
    private static String articleNode = "/oases/oasis";


    /* 3. Global metadata is the publisher - work around if it gets troublesome */
    static private final Map<String, XmlDomMetadataExtractor.XPathValue> globalMap = null;
    /*
     * The emitter will need a map to know how to cook raw values
     */
    protected static final MultiValueMap cookMap = new MultiValueMap();

    static {
        // normal journal article schema
        cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
        cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(pub_issn, MetadataField.FIELD_ISSN);
        cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
        cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
        cookMap.put(pub_date, MetadataField.FIELD_DATE);
        cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
        cookMap.put(art_lp, MetadataField.FIELD_END_PAGE);
        cookMap.put(art_contrib, MetadataField.FIELD_AUTHOR);
    }

    /**
     * publisher comes from a global node
     */
    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getGlobalMetaMap() {
        //no globalMap, so returning null
        return globalMap;
    }

    /**
     * return  article paths representing metadata of interest
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
     *
     */

    @Override
    public String getDeDuplicationXPathKey() {
        return null;
    }

    /**
     * Return the path for product form so when multiple records for the same
     * item are combined, the product forms are combined together
     */

    @Override
    public String getConsolidationXPathKey() {
        return null;
    }

    /**
     * using filenamePrefix (see above)
     */

    @Override
    public String getFilenameXPathKey() {
        return null; // the XML and PDF use the same base filename
    }

}
