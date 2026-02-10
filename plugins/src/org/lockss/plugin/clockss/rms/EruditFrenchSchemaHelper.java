/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.rms;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
<?xml version="1.0" encoding="UTF-8"?>
<article xmlns:xlink="http://www.w3.org/1999/xlink" idproprio="RMS_232_0100" lang="fr" typeart="article" traitement="artr">
   <admin>
      <infoarticle>
         <idpublic norme="doi">????</idpublic>
         <section_sommaire>Articles thématiques : nouveautés en médecine 2009 (deuxième partie)</section_sommaire>
         <tri>RMS_232_0100</tri>
         <pagination>
            <ppage>100</ppage>
            <dpage>104</dpage>
         </pagination>
         <nbpage>5</nbpage>
         <nbpara>7</nbpara>
         <nbmot>3088</nbmot>
         <nbfig>0</nbfig>
         <nbtabl>3</nbtabl>
         <nbimage>3</nbimage>
         <nbaudio>0</nbaudio>
         <nbvideo>0</nbvideo>
         <nbrefbiblio>15</nbrefbiblio>
         <nbnote>2</nbnote>
      </infoarticle>
      <revue id="RMS">
         <titrerev>Revue Médicale Suisse</titrerev>
         <titrerevabr>RMS</titrerevabr>
         <idissn>1660-9379</idissn>
         <idissnnum>en cours</idissnnum>
      </revue>
      <numero id="RMS_232">
         <volume>N˚ 232</volume>
         <nonumero>2</nonumero>
         <pub>
            <periode>20 janvier</periode>
            <annee>2010</annee>
         </pub>
         <pubnum>
            <date>20100414</date>
         </pubnum>
         <theme>Nouveautés en médecine 2009 (deuxième partie)</theme>
         <idisbn />
      </numero>
      <editeur id="MH">
         <nomorg>Médecine &amp; Hygiène</nomorg>
      </editeur>
      <prodnum id="CairnP">
         <nomorg>Cairn/Softwin</nomorg>
      </prodnum>
      <diffnum id="CairnD">
         <nomorg>Cairn</nomorg>
      </diffnum>
      <dtd nom="Erudit Article" version="vCairn 1.0" />
      <droitsauteur>
         ©
         <nomorg>Médecine &amp; Hygiène</nomorg>
         , 2010
      </droitsauteur>
   </admin>
   <grlien xlink:type="extended">
      <pointeur xlink:type="locator" xlink:label="no1" xlink:href="#xpointer(id('no1')/no)" typemime="none" />
      <pointeur xlink:type="locator" xlink:label="re2no1" xlink:href="#re2no1" typemime="none" />
      <lien xlink:type="arc" xlink:show="replace" xlink:actuate="onRequest" xlink:from="re2no1" xlink:to="no1" />
      <lien xlink:type="arc" xlink:show="replace" xlink:actuate="onRequest" xlink:from="no1" xlink:to="re2no1" />
      <pointeur xlink:type="locator" xlink:label="no2" xlink:href="#xpointer(id('no2')/no)" typemime="none" />
      <pointeur xlink:type="locator" xlink:label="re1no2" xlink:href="#re1no2" typemime="none" />
      <lien xlink:type="arc" xlink:show="replace" xlink:actuate="onRequest" xlink:from="re1no2" xlink:to="no2" />
      <lien xlink:type="arc" xlink:show="replace" xlink:actuate="onRequest" xlink:from="no2" xlink:to="re1no2" />
   </grlien>
   <liminaire>
      <grtitre>
         <titre>Diabète</titre>
      </grtitre>
      <grauteur>
         <auteur id="au1">
            <nompers>
               <prenom>Marc</prenom>
               <nomfamille>Egli</nomfamille>
            </nompers>
            <affiliation>
               <alinea>Service d’endocrinologie, diabétologie et métabolisme</alinea>
               <alinea>Département de médecine interne</alinea>
               <alinea>CHUV, 1011 Lausanne</alinea>
            </affiliation>
            <courriel>
               <liensimple id="l1" xlink:href="mailto:marc.egli@chuv.ch">marc.egli@chuv.ch</liensimple>
            </courriel>
         </auteur>
         <auteur id="au2">
            <nompers>
               <prenom>Juan</prenom>
               <nomfamille>Ruiz</nomfamille>
            </nompers>
            <affiliation>
               <alinea>Service d’endocrinologie, diabétologie et métabolisme</alinea>
               <alinea>Département de médecine interne</alinea>
               <alinea>CHUV, 1011 Lausanne</alinea>
            </affiliation>
            <courriel>
               <liensimple id="l2" xlink:href="mailto:juan.ruiz@chuv.ch">juan.ruiz@chuv.ch</liensimple>
            </courriel>
         </auteur>
      </grauteur>
      <resume lang="fr">
         <titre>Résumé</titre>
         <alinea>Parmi les apports de l’année 2009 en diabétologie clinique, une nouvelle stratégie de dépistage basée sur le taux d’hémoglobine glyquée avec un seuil diagnostique ≥ 6,5% a été proposée par les principales organisations internationales. L’efficacité préventive du contrôle multirisque chez le diabétique de type 2 reflétée par le faible taux d’événements cardiaques dans l’étude DIAD 2 amène à revoir les recommandations pour le dépistage coronarien. Dans le diabète gestationnel, la corrélation linéaire entre risque de complications et degré d’hyperglycémie dans l’étude HAPO consolide les repères pour le traitement de cette affection fréquente, qui identifie des femmes à risque futur élevé de diabète. A la lumière de l’ensemble des données disponibles, il n’y a pas matière à retenir un risque accru de cancer associé à l’insuline glargine.</alinea>
      </resume>
      <resume lang="en">
         <titre>Abstract</titre>
         <alinea>In 2009 a novel screening strategy for diabetes based on the level of glycated hemoglobin has been proposed by the main international organizations, with a diagnostic threshold of 6,5%. The preventive efficacy of multiple risk factor control in type 2 diabetes reflected by the low rate of cardiac events in the DIAD 2 study calls for a revision of the current recommendations for coronary disease screening. In gestational diabetes, the linear correlation between degree of hyperglycemia and risk of associated complications in the HAPO study strenghtens the therapeutic targets for this frequent condition, which identifies women at high future risk of diabetes. No conclusive evidence for an increased risk of cancer associated with insulin glargin remains when taking into account all the data currently available on this topic.</alinea>
      </resume>
   </liminaire>
</article>

Xpath translation

revue == journal

/article/admin/
    infoarticle:
        idpublic:

    pagination:
        ppage: first page
        dpage: last page


    revue - info about the journal
        -- idissn: journal issn
        -- titrerev: journal title

    numero: volume, issue section
        -- volume: volume, it has extra No #
        -- nonumero: issue number, it may have No #
    pub:
        annee: year
    pubnum:
        date
    dtd: maybe used as schema detection

    liminaire: (similar to front or intro)
    grtitre: (title group)
        titre: article title

    grauteur: (author group)
        auteur:
            nompers: (person name)
                prenom: first name
                nomfamille: family name

    resume: (abstract)

    editeur: (publisher group)
    nomorg: (publisher name)

    corps: (body)

 */

public class EruditFrenchSchemaHelper implements SourceXmlSchemaHelper {
    private static final Logger log = Logger.getLogger(EruditPublishingSchemaHelper.class);

    private static final String AUTHOR_SEPARATOR = ",";

    static private final NodeValue Erudit_AUTHOR_VALUE = new NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of Erudit author");
            NodeList elementChildren = node.getChildNodes();
            if (elementChildren == null)
            {
                return null;
            }

            String tsurname = null;
            String tgiven = null;

            if (elementChildren != null) {
                log.debug3("elementChildren != null");
                for (int j = 0; j < elementChildren.getLength(); j++) {
                    Node checkNode = elementChildren.item(j);
                    String nodeName = checkNode.getNodeName();
                    if ("nomfamille".equals(nodeName)) {
                        tsurname = checkNode.getTextContent();
                    } else if ("prenom".equals(nodeName) ) {
                        tgiven = checkNode.getTextContent();
                    }
                }
            } else {
                tsurname = node.getTextContent();
            }

            StringBuilder valbuilder = new StringBuilder();
            if (!StringUtils.isBlank(tsurname)) {
                valbuilder.append(tsurname);
                if (!StringUtils.isBlank(tgiven)) {
                    valbuilder.append(AUTHOR_SEPARATOR + " " + tgiven);
                }
            } else {
                log.debug3("no author found");
                return null;
            }
            log.debug3("author found: " + valbuilder.toString());
            return valbuilder.toString();
        }
    };


    private static String Erudit_article = "/article";
    private static String Erudit_jtitle = "/article/admin/revue/titrerev";
    private static String Erudit_publisher_name= "/article/admin/editeur/nomorg";

    private static String Erudit_issn = "/article/admin/revue/idissn";
    private static String Erudit_eissn = "/article/admin/revue/idissn";
    private static String Erudit_doi = "/article/admin/infoarticle/idpublic[@norme='doi']";
    private static String Erudit_volume = "/article/admin/numero/volume";
    private static String Erudit_issue = "/article/admin/numero/nonumero";
    private static String Erudit_pub_date = "/article/admin/numero/pubnum/date";
    private static String Erudit_pub_date_alt = "/article/admin/numero/pub/annee";

    private static String Erudit_atitle = "/article/liminaire/grtitre/titre";
    private static String Erudit_author = "/article/liminaire/grauteur/auteur/nompers";

    private static String Erudit_fpage = "/article/admin/infoarticle/pagination/ppage";
    private static String Erudit_lpage = "/article/admin/infoarticle/pagination/dpage";

  
    /* 1.  MAP associating xpath with value type with evaluator */
    static private final Map<String,XPathValue> Erudit_articleMap =
          new HashMap<String,XPathValue>();
    static {
        Erudit_articleMap.put(Erudit_jtitle, XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_issn, XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_eissn,XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_volume,XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_issue,XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_doi,XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_pub_date, XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_pub_date_alt, XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_atitle, XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_author, Erudit_AUTHOR_VALUE);
        Erudit_articleMap.put(Erudit_fpage,XmlDomMetadataExtractor.TEXT_VALUE);
        Erudit_articleMap.put(Erudit_lpage,XmlDomMetadataExtractor.TEXT_VALUE);
    }

    /* 2. Each item (article) has its own XML file */
    static private final String Erudit_articleNode = Erudit_article;

    /* 3. in Erudit there is no global information because one file/article */
    static private final Map<String,XPathValue> Erudit_globalMap = null;

    /*
    * The emitter will need a map to know how to cook ONIX raw values
    */
    private static final MultiValueMap cookMap = new MultiValueMap();
    static {
        cookMap.put(Erudit_jtitle, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(Erudit_atitle, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(Erudit_issn, MetadataField.FIELD_ISSN);
        cookMap.put(Erudit_eissn,MetadataField.FIELD_EISSN);
        cookMap.put(Erudit_volume,MetadataField.FIELD_VOLUME);
        cookMap.put(Erudit_issue,MetadataField.FIELD_ISSUE);
        //cookMap.put(Erudit_doi,MetadataField.FIELD_DOI);
        cookMap.put(Erudit_pub_date, MetadataField.FIELD_DATE);
        cookMap.put(Erudit_pub_date_alt, MetadataField.FIELD_DATE);
        cookMap.put(Erudit_author, MetadataField.FIELD_AUTHOR);
        cookMap.put(Erudit_fpage,MetadataField.FIELD_START_PAGE);
        cookMap.put(Erudit_lpage,MetadataField.FIELD_END_PAGE);
    }


    /**
    * Erudit does not contain needed global information outside of article records
    * return NULL
    */
    @Override
    public Map<String, XPathValue> getGlobalMetaMap() {
    return Erudit_globalMap;
    }

    /**
    * return Erudit article map to identify xpaths of interest
    */
    @Override
    public Map<String, XPathValue> getArticleMetaMap() {
    return Erudit_articleMap;
    }

    /**
    * Return the article node path
    */
    @Override
    public String getArticleNode() {
    return Erudit_articleNode;
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
