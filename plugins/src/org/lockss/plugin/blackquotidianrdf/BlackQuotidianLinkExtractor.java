package org.lockss.plugin.blackquotidianrdf;

import org.apache.commons.lang3.StringUtils;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.XPathUtil;
import org.lockss.util.urlconn.CacheException;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BlackQuotidianLinkExtractor implements LinkExtractor {

    protected static final XPathExpression LiveNoteDataSets;

    private static final Logger log = Logger.getLogger(BlackQuotidianLinkExtractor.class);

    /*
        <?xml version="1.0" encoding="UTF-8"?>
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
          xmlns:scalar="http://scalar.usc.edu/2012/01/scalar-ns#"
          xmlns:art="http://simile.mit.edu/2003/10/ontologies/artstor#"
          xmlns:prov="http://www.w3.org/ns/prov#"
          xmlns:dcterms="http://purl.org/dc/terms/"
          xmlns:ov="http://open.vocab.org/terms/"
          xmlns:sioc="http://rdfs.org/sioc/ns#"
          xmlns:iptc="http://ns.exiftool.ca/IPTC/IPTC/1.0/"
          xmlns:dc="http://purl.org/dc/elements/1.1/">
          <rdf:Description rdf:about="http://blackquotidian.supdigital.org/bq/acknowledgements">
            <rdf:type rdf:resource="http://scalar.usc.edu/2012/01/scalar-ns#Composite"/>
            <scalar:isLive>1</scalar:isLive>
            <prov:wasAttributedTo rdf:resource="http://blackquotidian.supdigital.org/bq/users/1"/>
            <dcterms:created>2019-03-12T23:56:18+00:00</dcterms:created>
            <scalar:urn rdf:resource="urn:scalar:content:3"/>
            <scalar:version rdf:resource="http://blackquotidian.supdigital.org/bq/acknowledgements.15"/>
            <dcterms:hasVersion rdf:resource="http://blackquotidian.supdigital.org/bq/acknowledgements.15"/>
            <scalar:citation>method=instancesof/content;methodNumNodes=1932;</scalar:citation>
           </rdf:Description>
           <rdf:Description rdf:about="http://blackquotidian.supdigital.org/bq/acknowledgements.15">
            <ov:versionnumber>15</ov:versionnumber>
            <dcterms:title>Acknowledgments</dcterms:title>
            <sioc:content><em>Black Quotidian </em> is dedicated to my father and grandmother, <a data-size="medium" data-align="right" data-caption="title-and-description" data-annotations="" resource="media/black-quotidian-chicago-1948" href="http://blackquotidian.supdigital.org/bq/media/Family photo Chicago - 1948 copy.png"> Frank E. Bowman and Leari Jean Anderson</a>, both of whom passed away in the three years I worked on this project.&nbsp;Their names never appeared in the <em> Chicago Defender </em> or <em> Twin Cities Courier</em>, but they are remembered.<br /><br />Thank you to my mom, Diane Delmont, for teaching me the importance of working hard every day and being nice to people.&nbsp;<br /><br />Thank you to Jacque Wernimont for introducing me to Digital Humanities, and for being a brilliant scholar and a wonderful partner.&nbsp;Thank you to Xavier and Simone for teaching me to appreciate the quotidian in new ways.<br /><br />Over fifty students and scholars contributed guest posts to <em> Black Quotidian </em>.&nbsp;Thank you to Keisha N. Blain, Stanley Bowling, Lucy Caplan, Julian Chambliss, Catrien Egbert, Deidre B. Flowers, Michael Glass, Nick Juravich, Yasmin Mitchel, Lillian G. Page, and Mark Speltz for their contributions. Thanks also to Adam Arenson at Manhattan College,&nbsp;Nicole Maurantonio at University of Richmond, and&nbsp;Kimberly Zarecor at Iowa State University for making this project part of their classes, and to their students (listed in the order that the posts appear):&nbsp;Daniel Arico,&nbsp;Caroline Arkesteyn,&nbsp;Alex Bishop,&nbsp;Connor Callahan,&nbsp;Samuel Carter,&nbsp;Trent Cork,&nbsp;Katelyn Culver,&nbsp;Austin Demers,&nbsp;Thomas Esposito,&nbsp;Mark Fowler,&nbsp;Derek Gilman,&nbsp;Ethan Hill,&nbsp;Luke Johnson,&nbsp;Samanvay Kasarala,&nbsp;Robert Kinser,&nbsp;Samuel Kramer,&nbsp;Caitlin Sullivan,&nbsp;Matthew Lee,&nbsp;Blake Miller,&nbsp;Andrew Mullinnix,&nbsp;Kelly Monfredini,&nbsp;Charles Zazzera,&nbsp;Aaron Nostwich,&nbsp;Daniel Obren,&nbsp;Niccolo Peterson,&nbsp;Ariel Sumendap,&nbsp;Bryce Rooney,&nbsp;Paige Ross,&nbsp;Ellie Siwicki,&nbsp;Paige Champman,&nbsp;Lauren Sendlebach,&nbsp;Andrew Spargo,&nbsp;Katherine Garnett,&nbsp;Denver Studebaker,&nbsp;Filomena Matoshi,&nbsp;Nathan Volkert,&nbsp;Jordan Washington,&nbsp;Shelby Worth,&nbsp;Bridget McEvoy,&nbsp;Christian Ruggiero,&nbsp;Courtney Canavan,&nbsp;Victoria Thomas,&nbsp;Vincent Wiedemann,&nbsp;Morgan Quartulli,&nbsp;Madison Wolf,&nbsp;Ashley Hogan,&nbsp;Ivan Montoya,&nbsp;Matthew Peters,&nbsp;Nicholas Matson,&nbsp;Andrew Reda,&nbsp;Amanda D&rsquo;Addona,&nbsp;Nicole D&rsquo;Addona,&nbsp;Madeleine Berry,&nbsp;Chris Sullivan,&nbsp;Damian Hondares,&nbsp;Dominique Brown,&nbsp;Bal Artis,&nbsp;Emeline Blevins,&nbsp;Tenaya Bien,&nbsp;Canyon Teague,&nbsp;Dominique Harrington,&nbsp;Madeleine Jordan-Lord,&nbsp;Bailey Duplessie,&nbsp;Megan Wirtz,&nbsp;Keshara Moore,&nbsp;Maddy Dunbar,&nbsp;Aggy Barnowski,&nbsp;Natalia Chaney, and Karissa Lim.<br /><br />Thank you to the students in my online MA History course at Arizona State University for their contributions (listed in the order that the posts appear):&nbsp;Jodi Silvio,&nbsp;Ivan Monroy,&nbsp;Tracy Stefanov,&nbsp;Brian Lloyd,&nbsp;Emilie Theobald,&nbsp;Alexander Cooper,&nbsp;Monique Vanderlaan,&nbsp;Kerri Ryer,&nbsp;Chase Miller,&nbsp;Todd Daily,&nbsp;Keisha Smith,&nbsp;Avi Buckles,&nbsp;Jeffrey Joynt,&nbsp;Michael Embry,&nbsp;Geoff Schumacher,&nbsp;John Loll,&nbsp;Adam Pinkerton,&nbsp;Tiffanie Butcher,&nbsp;Kristopher Boatman,&nbsp;Caryn Tijsseling,&nbsp;Rubin McMillan,&nbsp;Stephen Huff, and&nbsp;Candace F. Bryson.<br /><br />This project started at Arizona State University, and I am thankful for the support of colleagues in the School of Historical, Philosophical, and Religious Studies and across ASU during my four years there.&nbsp;Thanks also to my new colleagues at Dartmouth College.&nbsp;My research presentation on <em> Black Quotidian </em> and the subsequent Q&amp;A helped me think though important parts of this project&#39;s structure and arguments.&nbsp;I look forward to many more generative conversations in the years to come.<br /><br />Thank you to the three anonymous external reviewers whose comments and suggestions helped me strengthen this project.<br /><br />Thank you to Danny Bakewell Jr., Alonzo Kittrels, José Lusende, Ashley Johnson, Tanisha Leonard, and Sheila Scott for their help in securing permissions to use historical black newspapers in this project.&nbsp;Thanks also to&nbsp;Stanley Bowling,&nbsp;Corye Bradbury, Mary Beth Perrot, Scott Shultz, and Rafael Sidi at ProQuest for their encouragement of <em> Black Quotidian</em>.<br /><br />Thank you to Craig Detrich, Erik Loyer, and&nbsp;Curtis Fletcher for developing and managing Scalar, and for answer my many questions over the years.&nbsp;Thanks to Tara McPherson, Phil Ethington, and John Carlos Rowe for organizing the NEH Digital Humanities+American Studies workshop in summer 2011, where I learned how to use Scalar.&nbsp;It has proven to be one of the most important experiences in my professional life.<br /><br />Finally, thank you to the team at Stanford University Press.&nbsp;I appreciate the work Leah Pennywark and Greta Lindquist have put into the permissions process.&nbsp; Jasmine Mulliken has done an amazing job of managing the production process and I am grateful for her patience and technical expertise. Senior Editor Friederike Sundaram was excited about this project from the earliest stages and helped me stay the course when the finish line seemed too far away.&nbsp; Thanks for making this project a reality.</sioc:content>
            <scalar:defaultView>plain</scalar:defaultView>
            <scalar:editorialState>published</scalar:editorialState>
            <prov:wasAttributedTo rdf:resource="http://blackquotidian.supdigital.org/bq/users/6"/>
            <dcterms:created>2019-10-04T20:56:37+00:00</dcterms:created>
            <scalar:urn rdf:resource="urn:scalar:version:7726"/>
            <dcterms:isVersionOf rdf:resource="http://blackquotidian.supdigital.org/bq/acknowledgements"/>
            <rdf:type rdf:resource="http://scalar.usc.edu/2012/01/scalar-ns#Version"/>
            </rdf:Description>
         </rdf:RDF>

         Xpath: which means "in rdf:RDF, in rdf:Description, if the scalar:isLive text is "1", give me the text of the parent node's dcterms:created"
         /rdf:RDF/rdf:Description/scalar:isLive[text() = "1"]/../dcterms:created/text()
         /rdf:RDF/rdf:Description/@rdf:about
         /rdf:RDF/rdf:Description/scalar:isLive[text() = "1"]/../@rdf:about
     */
    static {

        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            SimpleNamespaceContext nsCtx = new SimpleNamespaceContext();
            xpath.setNamespaceContext(nsCtx);
            nsCtx.bindNamespaceUri("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            nsCtx.bindNamespaceUri("scalar", "http://scalar.usc.edu/2012/01/scalar-ns#");
            LiveNoteDataSets = xpath.compile("/rdf:RDF/rdf:Description/scalar:isLive[text() = \"1\"]/../@rdf:about");
        }
        catch (XPathExpressionException xpee) {
            throw new ExceptionInInitializerError(xpee);
        }
    }

    @Override
    public void extractUrls(ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            String srcUrl,
                            Callback cb)
            throws IOException {

        String loggerUrl = loggerUrl(srcUrl);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        InputSource inputSource = new InputSource(new InputStreamReader(in, encoding));
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            boolean isAware = builder.isNamespaceAware();
            doc = builder.parse(in);
        }
        catch (ParserConfigurationException pce) {
            throw new CacheException.UnknownExceptionException("Error configuring parser for " + loggerUrl, pce);
        }
        catch (SAXException se) {
            throw new CacheException.UnknownExceptionException("Error while parsing " + loggerUrl, se);
        }

        NodeList liveNodes = null;
        try {
            liveNodes = XPathUtil.evaluateNodeSet(LiveNoteDataSets, doc);
            int node_count = liveNodes.getLength(); 
            log.debug3("Total node count : " + node_count);
        }
        catch (XPathExpressionException xpee) {
            throw new CacheException.UnknownExceptionException("Error while parsing results for " + loggerUrl, xpee);
        }

        if (liveNodes.getLength() == 0) {
            throw new CacheException.UnknownExceptionException("Internal error parsing results for " + loggerUrl);
        }

        for (int i = 0 ; i < liveNodes.getLength() ; ++i) {
            log.debug3("Each node value " + liveNodes.item(i).getNodeValue());
            processnode(cb, liveNodes.item(i).getNodeValue());
        }
    }

    public void processnode(Callback cb, String node) {
        if (node != null && !StringUtils.isEmpty(node)) {
            cb.foundLink(node);
        }
    }

    public static final String loggerUrl(String srcUrl) {
        return srcUrl;
    }

}
