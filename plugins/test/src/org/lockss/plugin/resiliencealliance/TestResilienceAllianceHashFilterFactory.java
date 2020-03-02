package org.lockss.plugin.resiliencealliance;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import java.io.InputStream;

public class TestResilienceAllianceHashFilterFactory extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestResilienceAllianceHashFilterFactory.class);

    static String ENC = Constants.DEFAULT_ENCODING;

    private ResilienceAllianceHashFilterFactory fact;
    private MockArchivalUnit mau;

    public void setUp() throws Exception {
        super.setUp();
        fact = new ResilienceAllianceHashFilterFactory();
        mau = new MockArchivalUnit();
    }
    // this example is from an abstract
    private static final String HtmlHash = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" class=\"ms\">\n" +
            "<head>\n" +
            "    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=windows-1252\" />\n" +
            "    <title>Avian Conservation and Ecology: Influence of microhabitat on Honduran Emerald (<em>Amazilia luciae</em>) abundance in tropical dry forest remnants</title>\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/styles/journal.css\" />\n" +
            "    <script type=\"text/javascript\" src=\"/common/jquery/jquery-1.6.4.js\"></script>\n" +
            "    <meta name=\"description\" content=\"Rodr&iacute;guez, F., D. Escoto, T. Mej&iacute;a-Ord&oacute;&ntilde;ez, L. Ferrufino-Acosta, S. Y. Cruz, J. E. Duchamp, and J. L. Larkin. 2019. Influence of microhabitat on Honduran Emerald (Amazilia luciae) abundance in tropical dry forest remnants. Avian Conservation and Ecology 14(1):3. https://doi.org/10.5751/ACE-01321-140103\" />\n" +
            "    <meta name=\"keywords\" content=\"Agalta Valley; habitat use; hierarchical models; hummingbird\" />\n" +
            "</head>\n" +
            "<body id=\"published_ms\">\n" +
            "<div id=\"published_content\">\n" +
            "    <!-- HEADER INFORMATION (DO NOT EDIT) -->\n" +
            "    <div id=\"ms_menu\">\n" +
            "        <div id=\"ms_header_image\"><img src=\"/images/custom/ace_title_left.jpg\" alt=\"Avian Conservation and Ecology\" style=\"float: left\" /></div>\n" +
            "        <div class=\"menu\">\n" +
            "            <ul>\n" +
            "                <li><a href=\"/index.php\">Home</a></li>\n" +
            "                <li>|</li>\n" +
            "                <li><a href=\"/issues/\">Past issues</a></li>\n" +
            "                <li>|</li>\n" +
            "                <li><a href=\"/about/\">About</a></li>\n" +
            "            </ul>\n" +
            "        </div>\n" +
            "        <!-- menu -->\n" +
            "        <div style=\"clear: both\"> </div>\n" +
            "        <div id=\"ms_nav_bar_wrap\">\n" +
            "            <div id=\"ms_nav_bar\">\n" +
            "                &nbsp;<a href=\"/\">Avian Conservation and Ecology Home</a> &gt; <a href=\"/vol14/iss1/\">Vol. 14, No. 1</a> &gt; Art. 3\n" +
            "            </div>\n" +
            "            <!-- ms_nav_bar -->\n" +
            "        </div>\n" +
            "        <!-- ms_nav_bar_wrap -->\n" +
            "    </div>\n" +
            "    <!-- ms_menu -->\n" +
            "    <div id=\"ms_content\">\n" +
            "        <div id=\"proof_copyright\">\n" +
            "            Copyright &#169; 2019 by the author(s). Published here under license by The Resilience Alliance. This article  is under a <a rel=\"license\" href=\"http://creativecommons.org/licenses/by-nc/4.0/\" target=\"_blank\">Creative Commons Attribution-NonCommercial 4.0 International License</a>.  You may share and adapt the work for noncommercial purposes provided the original author and source are credited, you indicate whether any changes were made, and you include a link to the license.<br />\n" +
            "            Go to the <a href=\"ACE-ECO-2018-1321.pdf\">pdf</a> version of this article\n" +
            "        </div>\n" +
            "        <div id=\"proof_citation\">The following is the established format for referencing this article:<br />\n" +
            "            Rodríguez, F., D. Escoto, T. Mejía-Ordóñez, L. Ferrufino-Acosta, S. Y. Cruz, J. E. Duchamp, and J. L. Larkin. 2019. Influence of microhabitat on Honduran Emerald (<em>Amazilia luciae</em>) abundance in tropical dry forest remnants. <i>Avian Conservation and Ecology</i> 14(1):3. <br />https://doi.org/10.5751/ACE-01321-140103\n" +
            "        </div>\n" +
            "        <span id=\"proof_section\">\n" +
            "            Research Paper</span>\n" +
            "        <h1>Influence of microhabitat on Honduran Emerald</h1>\n" +
            "        <div id=\"authors\"><a href=\"#author_address\">Fabiola  Rodríguez</a><sup> 1</sup>, <a href=\"mailto:escotodorian185@gmail.com\">Dorian  Escoto</a><sup> 1</sup>, <a href=\"mailto:thelma.mejia@unah.edu.hn\">Thelma M. Mejía-Ordóñez</a><sup> 2</sup>, <a href=\"mailto:Lilian.ferrufino@unah.edu.hn\">Lilian  Ferrufino-Acosta</a><sup> 2</sup>, <a href=\"mailto:sy_champloo@hotmail.com\">Saby Y. Cruz</a><sup> 2</sup>, <a href=\"mailto:jduchamp@iup.edu\">Joseph E. Duchamp</a><sup> 1</sup> and <a href=\"mailto:larkin@iup.edu\">Jeffery L. Larkin</a><sup> 1,3</sup></div>\n" +
            "        <div id=\"affiliations\"><sup>1</sup>Department of Biology, Indiana University of Pennsylvania, Indiana, Pennsylvania, USA, <sup>2</sup>Escuela de Biolog&iacute;a, Ciudad Universitaria, Universidad Nacional Aut&oacute;noma de Honduras, Tegucigalpa, Francisco Moraz&aacute;n, Honduras, <sup>3</sup>American Bird Conservancy, The Plains, Virginia, USA</div>\n" +
            "        <!-- END HEADER INFORMATION -->\n" +
            "        <!-- TABLE OF CONTENTS -->\n" +
            "        <ul id=\"article_toc\">\n" +
            "            <li><a href=\"#abstract\">Abstract</a></li>\n" +
            "            <li><a href=\"#introduction5\">Introduction</a></li>\n" +
            "            <li><a href=\"#literatureci23\">Literature Cited</a></li>\n" +
            "        </ul>\n" +
            "        <!-- END TABLE OF CONTENTS -->\n" +
            "        <!-- ABSTR (DO NOT EDIT) -->\n" +
            "        <div id=\"abstract_block\">\n" +
            "            <h2 id=\"abstract\">ABSTRACT</h2>\n" +
            "            ABSTRACT content is here...\n" +
            "            <h2>R&Eacute;SUM&Eacute;</h2>\n" +
            "            <div>Resume content is here...</div>\n" +
            "        </div>\n" +
            "        <div id=\"ms_keywords\">Key words: Agalta Valley; habitat use; hierarchical models; hummingbird</div>\n" +
            "        <!-- END ABSTR -->\n" +
            "        <h2 id=\"introduction5\">INTRODUCTION</h2>\n" +
            "        INTRODUCTION, METHOD, RESULTS are all here\n" +
            "\n" +
            "        <h2 id=\"discussion14\">DISCUSSION</h2>\n" +
            "        <p>Our study revealed that shrub-sapling density and cacti structural diversity explained Honduran Emerald local abundance in tropical dry forest remnants of the Agalta Valley. In other parts of the species range, shrubs and cacti species have been highlighted as components of the tropical dry forest used by this species (Thorn et al. 2000, House 2004). In fact, it is well documented that shrub and cacti are ecologically important to many species of tropical hummingbirds for feeding, or perching close to feeding areas (Skutch 1958, Wolf 1964, Wolf and Stiles 1970, Feinsinger 1976, Hainsworth 1977, Snow and Snow 1986, Fraga 1989). In addition, small trees or saplings are considered to have suitable floral resources that are suitable for hummingbirds (Snow and Snow 1986). Honduran Emeralds&#8217; use of shrubs and cacti as a floral resource has been reported elsewhere (Anderson et al. 2010). The use of the shrub-sapling strata by Honduran Emeralds as a source of floral resources was observed previously in the Agalta Valley (Mora et al. 2016). Moreover, field observations during our study revealed that the species used floral resources of four shrub and two cacti species: <em>Aphelandra scabra</em> (Acanthaceae), <em>Combretum fruticosum</em> (Combretaceae), <em>Pedilanthus tithymaloides</em> (Euphorbiaceae), <em>Cnidoscolus aconitifolius</em> (Euphorbiaceae), and <em>Opuntia hondurensis</em> (Cactaceae) and <em>Pilosocereus leucocephalus</em> (Cactaceae), respectively. Honduran Emeralds also used shrub-sapling and cacti strata as nesting substrate (Rodr&iacute;guez et al. 2016).</p>\n" +
            "       <!-- RESPONSES (DO NOT EDIT) -->\n" +
            "        <div id=\"responses_block\">\n" +
            "            <h2 id=\"responses\">RESPONSES TO THIS ARTICLE</h2>\n" +
            "            Responses to this article are invited\n" +
            "        </div>\n" +
            "        <!-- END RESPONSES -->\n" +
            "        <p />\n" +
            "        <!-- ACKNOWLEDGMENT_BLOCK (DO NOT EDIT) -->\n" +
            "        <div id=\"acknowledgments\">\n" +
            "            <p><strong>ACKNOWLEDGMENTS</strong></p>\n" +
            "            <p>Acknowledgements content is here...</p>\n" +
            "        </div>\n" +
            "        <!-- END ACKNOWLEDGMENT_BLOCK -->\n" +
            "        <h2 id=\"literatureci23\">LITERATURE CITED</h2>\n" +
            "        <p>LITERATURE CITED content is here...</p>\n" +
            "        <div id=\"author_address\">\n" +
            "            <!-- AUTHOR CONTACT (DO NOT EDIT) --><strong>Address of Correspondent:</strong><br />\n" +
            "            Fabiola  Rodríguez<br />\n" +
            "            New Orleans, Louisiana, USA<br />\n" +
            "            <a href=\"mailto:fabiola.rodriguezv@gmail.com\">fabiola.rodriguezv@gmail.com</a>\n" +
            "            <!-- END AUTHOR CONTACT -->\n" +
            "            <div id=\"footer_image\"></div>\n" +
            "        </div>\n" +
            "        <!-- ARTICLE CONTENT FOOTER (DO NOT EDIT) -->\n" +
            "        <div id=\"ms_uparrow\"><a href=\"#proof_copyright\"><img src=\"/images/uparrow.gif\" alt=\"Jump to top\" style=\"border: 0px; width: 15px; height: 15px\" /></a></div>\n" +
            "    </div>\n" +
            "    <!-- ms_content -->\n" +
            "</div>\n" +
            "<!-- published_content -->\n" +
            "<!-- ARTICLE CONTENT ATTACHMENTS -->\n" +
            "<div id=\"attachments\"><a href=\"javascript:awin('table1.html','pAttachment',850,720)\"  title=\"Structure and composition microhabitat features of the Agalta Valley&#8217;s tropical dry forest remnants used in the analysis of Honduran Emerald local abundance.\">Table1</a></div>\n" +
            "<!-- attachments -->\n" +
            "<!-- END ARTICLE CONTENT ATTACHMENTS -->\n" +
            "</body>\n" +
            "</html>";

    private static final String HtmlHashFiltered = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" class=\"ms\">\n" +
            "\n" +
            "<body id=\"published_ms\">\n" +
            "<div id=\"published_content\">\n" +
            "    \n" +
            "    \n" +
            "    \n" +
            "    <div id=\"ms_content\">\n" +
            "        \n" +
            "        \n" +
            "        <span id=\"proof_section\">\n" +
            "            Research Paper</span>\n" +
            "        <h1>Influence of microhabitat on Honduran Emerald</h1>\n" +
            "        \n" +
            "        \n" +
            "        \n" +
            "        \n" +
            "        \n" +
            "        \n" +
            "        \n" +
            "        <div id=\"abstract_block\">\n" +
            "            <h2 id=\"abstract\">ABSTRACT</h2>\n" +
            "            ABSTRACT content is here...\n" +
            "            <h2>R&Eacute;SUM&Eacute;</h2>\n" +
            "            <div>Resume content is here...</div>\n" +
            "        </div>\n" +
            "        \n" +
            "        \n" +
            "        <h2 id=\"introduction5\">INTRODUCTION</h2>\n" +
            "        INTRODUCTION, METHOD, RESULTS are all here\n" +
            "\n" +
            "        <h2 id=\"discussion14\">DISCUSSION</h2>\n" +
            "        <p>Our study revealed that shrub-sapling density and cacti structural diversity explained Honduran Emerald local abundance in tropical dry forest remnants of the Agalta Valley. In other parts of the species range, shrubs and cacti species have been highlighted as components of the tropical dry forest used by this species (Thorn et al. 2000, House 2004). In fact, it is well documented that shrub and cacti are ecologically important to many species of tropical hummingbirds for feeding, or perching close to feeding areas (Skutch 1958, Wolf 1964, Wolf and Stiles 1970, Feinsinger 1976, Hainsworth 1977, Snow and Snow 1986, Fraga 1989). In addition, small trees or saplings are considered to have suitable floral resources that are suitable for hummingbirds (Snow and Snow 1986). Honduran Emeralds&#8217; use of shrubs and cacti as a floral resource has been reported elsewhere (Anderson et al. 2010). The use of the shrub-sapling strata by Honduran Emeralds as a source of floral resources was observed previously in the Agalta Valley (Mora et al. 2016). Moreover, field observations during our study revealed that the species used floral resources of four shrub and two cacti species: <em>Aphelandra scabra</em> (Acanthaceae), <em>Combretum fruticosum</em> (Combretaceae), <em>Pedilanthus tithymaloides</em> (Euphorbiaceae), <em>Cnidoscolus aconitifolius</em> (Euphorbiaceae), and <em>Opuntia hondurensis</em> (Cactaceae) and <em>Pilosocereus leucocephalus</em> (Cactaceae), respectively. Honduran Emeralds also used shrub-sapling and cacti strata as nesting substrate (Rodr&iacute;guez et al. 2016).</p>\n" +
            "       \n" +
            "        \n" +
            "        \n" +
            "        <p />\n" +
            "        \n" +
            "        \n" +
            "        \n" +
            "        <h2 id=\"literatureci23\">LITERATURE CITED</h2>\n" +
            "        <p>LITERATURE CITED content is here...</p>\n" +
            "        \n" +
            "        \n" +
            "        \n" +
            "    </div>\n" +
            "    \n" +
            "</div>\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "</body>\n" +
            "</html>";
    
    public void testFilterA() throws Exception {
        InputStream actIn;

        actIn = fact.createFilteredInputStream(mau,
                new StringInputStream(HtmlHash), ENC);
        String filteredStr = StringUtil.fromInputStream(actIn);
        
        assertEquals(HtmlHashFiltered, filteredStr);
    }
}

