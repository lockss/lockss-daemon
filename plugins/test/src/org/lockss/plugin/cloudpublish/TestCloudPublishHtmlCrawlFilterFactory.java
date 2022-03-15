package org.lockss.plugin.cloudpublish;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;
import sun.security.action.OpenFileInputStreamAction;

import java.io.FileInputStream;
import java.io.InputStream;

public class TestCloudPublishHtmlCrawlFilterFactory extends LockssTestCase {

  static String ENC = Constants.DEFAULT_ENCODING;

  private CloudPublishHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new CloudPublishHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String article =
    "<div class=\"cms-content-body journal-tabs-wrapper\">\n" +
    "  <div class=\"journal-tabs js-tabs\">\n" +
    "    <div class=\"ui top attached tabular menu js-tablist\">\n" +
    "      <div class=\"js-tablist__item\">\n" +
    "        <a href=\"#journal-references\" id=\"label_journal-references\" class=\"item js-tablist__link active investigation\" data-tab=\"journal-references\" data-item-type=\"journal_article\" data-item-id=\"30568\"> References</a>\n" +
    "      </div>\n" +
    "      <div class=\"js-tablist__item\">\n" +
    "        <a href=\"#journal-full-text\" id=\"label_journal-full-text\" class=\"item js-tablist__link investigation\" data-tab=\"journal-full-text\" data-item-type=\"journal_article\" data-item-id=\"30568\"> Full Text</a>\n" +
    "      </div>\n" +
    "      <div class=\"js-tablist__item\">\n" +
    "        <a href=\"#journal-pdf\" id=\"label_journal-pdf\" class=\"item js-tablist__link\" data-tab=\"journal-pdf\">PDF</a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div id=\"journal-full-text\" class=\"ui bottom attached tab segment js-tab-content\" data-tab=\"journal-full-text\">\n" +
    "      <div class=\"journal-tab__copy\">\n" +
    "        <div class=\"ui\">\n" +
    "          <h2 class=\"ui header work-title\">Intimate Linguistic Contact and Spanish: Western Nahua (Nawa) Varieties of San Pedro Jícora and San Agustín Buenaventura, Durango, Mexico</h2>\n" +
    "          <div class=\"article-abstract\">\n" +
    "            <h4>Abstract</h4>\n" +
    "            <p>\n" +
    "              <abstract xml:lang=\"en\"></abstract>\n" +
    "            </p>\n" +
    "            <p>This paper illustrates the extensive influence of Spanish on the Mexicanero language of Durango, Mexico, with comparisons with Spanish influence on other varieties of Nawa.</p>\n" +
    "            <p></p>\n" +
    "            <p>\n" +
    "              <trans-abstract xml:lang=\"es\"></trans-abstract>\n" +
    "            </p>\n" +
    "            <p>Este artículo ilustra la influencia extensa del español en el idioma mexicanero de Durango, México, en que se hacen también comparaciones de la influencia española en otras variedades del nahua.</p>\n" +
    "            <p></p>\n" +
    "          </div>\n" +
    "          <p></p>\n" +
    "          <p>Hundreds of speakers in San Pedro Jícora and San Agustín Buenaventura in the <i>municipio</i> of Mezquital in the southern part of the Mexican province of Durango speak forms of a variety of Western Nahua / Nawa popularly known as ‘Mexicanero’. <sup>\n" +
    "              <a href=\"#fn1\" id=\"text1\">1</a>\n" +
    "            </sup> All also speak Spanish; furthermore, some in San Agustín Buenaventura speak Southeastern Tepehuán, which belongs to a different branch of Uto-Aztecan from Nawa. Durango Nawa has been extremely strongly influenced by Spanish over the centuries, to the extent that the typically Mesoamerican NP-NP (Noun-Phrase-Noun-Phrase) possessive construction which is customarily found in other Nawa varieties has largely been replaced by one which incorporates the use of Spanish <i>de</i> with a Nawa article. </p>\n" +
    "          <p>Records of the language in the form of texts collected by German Americanist Konrad Theodor Preuss in the first decade of the twentieth century (see Ziehm 1968–1976), mostly from speakers from San Pedro Jícora, show that much of the influence was already in place by 1906, but further influence has taken place, including the incorporation of massive amounts of Spanish lexicon.</p>\n" +
    "          <p>Drawing on a variety of sources, and illustrating several lexical and structural features, I will attempt to place Mexicanero in the context of modern Nawa varieties which have been heavily hispanized. Some of these, such as Pipil of El Salvador (an Eastern Nawa variety), are also highly moribund.</p>\n" +
    "          <p>Below, section 1 presents the language in its historical context, while section 2 outlines Mexicanero data sources. Section 3 discusses some of the literature on Nawa-Spanish contact and section 4 gives illustrations from Mexicanero phonology and lexicon. Section 5 discusses some Mexicanero features which are attested in the Mesoamerican <i>Sprachbund</i>, with special reference to syntactic features regarding NP-NP possession, and section 6 examines the use of some Spanish conjunctions in Mexicanero. section 7 looks at Mexicanero on the Thomason and Kaufman borrowing scale ( <a href=\"#R52\">1988</a>) and Section 8 at the effects of Spanish when compared with that found in Pipil. Conclusions are presented in the final section. </p>\n" +
    "          <label>1.</label>\n" +
    "          <h3>The language and its historical context</h3>\n" +
    "          <p>Mexicanero is a Uto-Aztecan language. Uto-Aztecan languages comprise one of the most widespread language families in the Americas (Caballero <a href=\"#R9\">2011</a>). There are several branches. According to the classification in Canger ( <a href=\"#R14\">1988</a>), four of them comprise Northern (Numic, Takic, Tübatulabal, Hopi: all are confined to US, and Tübatulabal and Takic are severely endangered). Six branches constitute a Southern group: Taracahitan, Tepiman, Cahitan, Opatan, Corachol, Aztecan. These are mostly spoken in Mexico with Aztecan Pipil also used in El Salvador and some speakers of Tepiman (especially Tohono O’odham, formerly ‘Papago’) and Cahitan languages (specifcally Yo’eme or Yaqui) constitute language communities in the Southwestern US). Tepehuán, in which some speakers of Mexicanero in San Pedro Jícora are bilingual, is a Tepiman language. Opatan is the only extinct branch in Southern Uto-Aztecan. </p>\n" +
    "          <p>Aztecan comprises Pochutec of the Mexican state of Oaxaca, which is extinct and poorly attested, and Nahua or Nawa (which is generally if inaccurately known as ‘Nahuatl’, which is the name of some varieties rather than all of them; in Spanish it is often also popularly called <i>mexicano</i>). According to Canger ( <a href=\"#R14\">1988</a>), Nawa or Nahua comprises three branches: Western, Central, Eastern, each of which has numerous varieties. Pipil (autonym <i>nawat</i>) of El Salvador and formerly Nicaragua is a variety of Eastern Nawa, as are those of the area of La Huasteca (including parts of the province of Veracruz and San Juan Potosí); Classical Nahuatl (autonym <i>na:waλ</i>) is a Central variety, as are those spoken in the states of Puebla, Guerrero, Tlaxcala and Hidalgo. Classifications which combine Western and Central Nawa as ‘Western Nawa / Nahua’ against Eastern Nawa (e.g. Romero forthcoming) refer to Canger’s Western Nawa branch as ‘Western Peripheral’. </p>\n" +
    "          <p>Mexicanero is a Western Nawa language, as is the Nawa of Michoacán and what remains of Nawa in the states of Jalisco, Colima and Nayarit. Sischo ( <a href=\"#R45\">1979</a>; Sischo and Hollenbach <a href=\"#R46\">2015</a>) describes Michoacán Nawa. This area is referred to in Nawa studies as ‘la perifería occidental’. In terms of speakers, Western Nawa is certainly the smallest of the three branches and the one with the smallest amount of coverage and documentation. In 1692 Juan Guerra ( <a href=\"#R30\">1900</a>) and in <a href=\"#R23\">1765</a> Gerónimo Cortes y Zedeño described earlier forms of Western Nawa then used in Jalisco, in and around the city of Guadalajara, and both authors commented upon the impact which Spanish had on Nawa’s vocabulary (cf. Canger <a href=\"#R18\">2001</a>: 10). Canger’s definitive classification of Nawa lects (1988) and its findings are followed here. </p>\n" +
    "          <p>Mexicanero has as autonyms ‘Nawat’ and ‘Meshikan’. Its popular name is ‘Mexicanero’. This is also its Spanish name and is used in linguistic literature. It should not be confused with ‘Mexicano’. It is spoken close to Durango’s border with Nayarit, where there are also some speakers. The varieties exhibit a number of differences (e.g. <i>at</i> ‘water’ in San Pedro Jícora, <i>ati</i> in San Agustín Buenaventura) but the impact of Spanish on them is very similar and they are usually treated here jointly. Ethnologue codes are ‘azd’ for the variety used in San Pedro Jícora (hereafter SPJ), and ‘azn’ for the form used in San Agustín Buenaventura (SAB) and Nayarit. In 2016, Mexicanero varieties had around 1,300 speakers, all bilingual in Spanish. Those in SAB also often know Southeastern Tepehuán. Mexicanero is largely unwritten, though a literacy project has been developed (an account is given in Castro-Medina <a href=\"#R20\">2008</a>). </p>\n" +
    "          <p>Mexicanero shares the earlier part of its history with other Nawa varieties; in terms of contact this means that it has been influenced by Huastec (Wasteko / Teenek; a divergent Mayan language), by Mixe-Zoquean languages and by Totonac (see Kaufman <a href=\"#R36\">2001</a>) and that it has moved northwest into its current territory from its earlier home in the Valley of Mexico. This is manifested in some borrowings from these languages which are found throughout Nawa. These include terms found in Mexican Spanish, such as <i>petate</i> ‘mat’, <i>zacate</i> ‘grass’, both of which come from Mixe-Zoquean via their earlier absorption into Nawa. There is some influence on Mexicanero from Cora (a Corachol Uto-Aztecan language of the neighbouring province of Nayarit, indeed, the name ‘Nayarit’ is from the Cora autonym ‘nááyeri’). A cultural term containing non-Nawa /r/ in Mexicanero is noteworthy: &gt;xurawet&gt; /ʃurawet/ ‘festival’ &gt; Cora <i>şúɁráve</i>-, Huichol <i>şuráve</i>- ‘star’ (Kaufman <a href=\"#R36\">2001</a>: 6; see also Dakin <a href=\"#R24\">2017</a>). </p>\n" +
    "          <label>2.</label>\n" +
    "          <h3>Data sources for Mexicanero</h3>\n" +
    "          <p>There are two major bodies of work reflecting the Mexicanero language at the beginning and the end of the twentieth century respectively. Though different in nature, these clearly reflect the same language, with material from SPJ and SAB in both, and the amount of diachronic change between the two ends of the century appears to be rather small.</p>\n" +
    "          <p></p>\n" +
    "          <div class=\"cues-list\" id=\"fn1\">\n" +
    "            <a href=\"#text1\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>\n" +
    "            <label>1</label>\n" +
    "            <p>I wish to thank Miriam Bouzouita, Una Canger, Renata Enghels, John Green, Ewan Higgs, Hugo Salgado, Kim Schulte, Lameen Souag, Joel K. Swadesh and Clara Vanderschueren for manifold forms of help.</p>\n" +
    "          </div>\n" +
    "          <div class=\"cues-list\" id=\"fn2\">\n" +
    "            <a href=\"#text2\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>\n" +
    "            <label>2</label>\n" +
    "            <p>Note that Nawa, like many Mesoamerican languages, is a ‘grue’ language which expresses ‘blue’ and ‘green’ by the same word.</p>\n" +
    "          </div>\n" +
    "          <div class=\"cues-list\" id=\"fn3\">\n" +
    "            <a href=\"#text3\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>\n" +
    "            <label>3</label>\n" +
    "            <p>See Kaufman ( <xref ref-type=\"bibr\" rid=\"R35\">1973</xref>); Campbell, Kaufman and Smith-Stark ( <xref ref-type=\"bibr\" rid=\"R12\">1986</xref>); Smith-Stark ( <xref ref-type=\"bibr\" rid=\"R47\">1994</xref>); and especially Brown ( <xref ref-type=\"bibr\" rid=\"R8\">2011</xref>). </p>\n" +
    "          </div>\n" +
    "          <div class=\"cues-list\" id=\"fn4\">\n" +
    "            <a href=\"#text4\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>\n" +
    "            <label>4</label>\n" +
    "            <p>Surface phonemic transcriptions are followed by morphophonemic analyses and the transcriptional system used by Canger ( <xref ref-type=\"bibr\" rid=\"R18\">2001</xref>) is modified by replacing &gt;č ¢&gt; with &gt;ch ts&gt;. </p>\n" +
    "          </div>\n" +
    "          <div class=\"cues-list\" id=\"fn5\">\n" +
    "            <a href=\"#text5\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>\n" +
    "            <label>5</label>\n" +
    "            <p>To illustrate, note the use of the future to parallel the Spanish subjunctive in (16).</p>\n" +
    "          </div>\n" +
    "          <p></p>\n" +
    "          <p></p>\n" +
    "          <h3 class=\"ui label ribbon\">Works cited</h3>\n" +
    "          <p id=\"R1\">\n" +
    "            <a href=\"#ref-text-R1\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>Alcocer, Paulina, 2005. ‘Elsa Ziehm y la edición de los textos nahuas de San Pedro Jícora registrados por Konrad Th. Preuss’, <span>Dimensión Antropológica</span> 34: 147–66. <nobr>\n" +
    "              <a href=\"https://scholar.google.com/scholar?hl=en&amp;q=Alcocer%2C+Paulina%2C+2005.+%E2%80%98Elsa+Ziehm+y+la+edici%C3%B3n+de+los+textos+nahuas+de+San+Pedro+J%C3%ADcora+registrados+por+Konrad+Th.+Preuss%E2%80%99%2C+Dimensi%C3%B3n+Antropol%C3%B3gica+34%3A+147%E2%80%9366.\" target=\"_blank\">\n" +
    "                <span class=\"fa fa-external-link\"></span>&nbsp;Google Scholar </a>\n" +
    "            </nobr>\n" +
    "          </p>\n" +
    "          <p id=\"R46\">\n" +
    "            <a href=\"#ref-text-R46\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>Sischo, William (Guillermo), and Elena Erickson de Hollenbach (Barbara Elaine Hollenbach), 2015. <span>Gramática breve del náhuatl de Michoacán</span>. <a href=\"barbaraelenahollenbachcom/PDFs/nclGr1015.pdf\" target=\"_blank\">barbaraelenahollenbachcom/PDFs/nclGr1015.pdf</a>. Accessed 16 November 2019. <nobr>\n" +
    "              <a href=\"https://scholar.google.com/scholar?hl=en&amp;q=Sischo%2C+William+%28Guillermo%29%2C+and+Elena+Erickson+de+Hollenbach+%28Barbara+Elaine+Hollenbach%29%2C+2015.+Gram%C3%A1tica+breve+del+n%C3%A1huatl+de+Michoac%C3%A1n.+barbaraelenahollenbachcom%2FPDFs%2FnclGr1015.pdf.+Accessed+16+November+2019.\" target=\"_blank\">\n" +
    "                <span class=\"fa fa-external-link\"></span>&nbsp;Google Scholar </a>\n" +
    "            </nobr>\n" +
    "          </p>" +
    "          <p id=\"R2513\">\n" +
    "            <a href=\"#ref-text-R2\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>Boas, Franz, 1930. ‘Spanish elements in Modern Nahuatl’, in <span>Todd Memorial Volume Philological Studies</span> 1, ed. J. D. FitzGerald and Pauline Taylor (New York: Columbia University Press), pp. 85–89. <nobr>\n" +
    "              <a href=\"https://scholar.google.com/scholar?hl=en&amp;q=Boas%2C+Franz%2C+1930.+%E2%80%98Spanish+elements+in+Modern+Nahuatl%E2%80%99%2C+in+Todd+Memorial+Volume+Philological+Studies+1%2C+ed.+J.+D.+FitzGerald+and+Pauline+Taylor+%28New+York%3A+Columbia+University+Press%29%2C+pp.+85%E2%80%9389.\" target=\"_blank\">\n" +
    "                <span class=\"fa fa-external-link\"></span>&nbsp;Google Scholar </a>\n" +
    "            </nobr>\n" +
    "          </p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div id=\"journal-references\" class=\"ui bottom attached tab segment js-tab-content active\" data-tab=\"journal-references\">\n" +
    "      <div class=\"journal-tab__copy\">\n" +
    "        <div class=\"ui\">\n" +
    "          <h3 class=\"ui label ribbon\">Works cited</h3>\n" +
    "          <p id=\"R1\">\n" +
    "            <a href=\"#ref-text-R1\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>Alcocer, Paulina, 2005. ‘Elsa Ziehm y la edición de los textos nahuas de San Pedro Jícora registrados por Konrad Th. Preuss’, <span>Dimensión Antropológica</span> 34: 147–66. <nobr>\n" +
    "              <a href=\"https://scholar.google.com/scholar?hl=en&amp;q=Alcocer%2C+Paulina%2C+2005.+%E2%80%98Elsa+Ziehm+y+la+edici%C3%B3n+de+los+textos+nahuas+de+San+Pedro+J%C3%ADcora+registrados+por+Konrad+Th.+Preuss%E2%80%99%2C+Dimensi%C3%B3n+Antropol%C3%B3gica+34%3A+147%E2%80%9366.\" target=\"_blank\">\n" +
    "                <span class=\"fa fa-external-link\"></span>&nbsp;Google Scholar </a>\n" +
    "            </nobr>\n" +
    "          </p>\n" +
    "          <p id=\"R46\">\n" +
    "            <a href=\"#ref-text-R46\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>Sischo, William (Guillermo), and Elena Erickson de Hollenbach (Barbara Elaine Hollenbach), 2015. <span>Gramática breve del náhuatl de Michoacán</span>. <a href=\"barbaraelenahollenbachcom/PDFs/nclGr1015.pdf\" target=\"_blank\">barbaraelenahollenbachcom/PDFs/nclGr1015.pdf</a>. Accessed 16 November 2019. <nobr>\n" +
    "              <a href=\"https://scholar.google.com/scholar?hl=en&amp;q=Sischo%2C+William+%28Guillermo%29%2C+and+Elena+Erickson+de+Hollenbach+%28Barbara+Elaine+Hollenbach%29%2C+2015.+Gram%C3%A1tica+breve+del+n%C3%A1huatl+de+Michoac%C3%A1n.+barbaraelenahollenbachcom%2FPDFs%2FnclGr1015.pdf.+Accessed+16+November+2019.\" target=\"_blank\">\n" +
    "                <span class=\"fa fa-external-link\"></span>&nbsp;Google Scholar </a>\n" +
    "            </nobr>\n" +
    "          </p>" +
    "          <p id=\"R2513\">\n" +
    "            <a href=\"#ref-text-R2\">\n" +
    "              <i class=\"arrow-up\"></i>\n" +
    "            </a>Boas, Franz, 1930. ‘Spanish elements in Modern Nahuatl’, in <span>Todd Memorial Volume Philological Studies</span> 1, ed. J. D. FitzGerald and Pauline Taylor (New York: Columbia University Press), pp. 85–89. <nobr>\n" +
    "              <a href=\"https://scholar.google.com/scholar?hl=en&amp;q=Boas%2C+Franz%2C+1930.+%E2%80%98Spanish+elements+in+Modern+Nahuatl%E2%80%99%2C+in+Todd+Memorial+Volume+Philological+Studies+1%2C+ed.+J.+D.+FitzGerald+and+Pauline+Taylor+%28New+York%3A+Columbia+University+Press%29%2C+pp.+85%E2%80%9389.\" target=\"_blank\">\n" +
    "                <span class=\"fa fa-external-link\"></span>&nbsp;Google Scholar </a>\n" +
    "            </nobr>\n" +
    "          </p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div id=\"journal-pdf\" class=\"ui bottom attached tab segment js-tab-content\" data-tab=\"journal-pdf\">\n" +
    "      <div class=\"journal-tab__copy\">\n" +
    "        <div class=\"ui\">\n" +
    "          <p class=\"read-now\">\n" +
    "            <a class=\"ui primary button\" title=\"Download\" href=\"/read/?item_type=journal_article&amp;item_id=30568&amp;mode=download\">\n" +
    "              <i class=\"download icon\"></i> Download (PDF) </a>\n" +
    "          </p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>";

  private static final String filteredArticle =
    "<div class=\"cms-content-body journal-tabs-wrapper\">\n" +
    "  <div class=\"journal-tabs js-tabs\">\n" +
    "    <div class=\"ui top attached tabular menu js-tablist\">\n" +
    "      <div class=\"js-tablist__item\">\n" +
    "        <a href=\"#journal-references\" id=\"label_journal-references\" class=\"item js-tablist__link active investigation\" data-tab=\"journal-references\" data-item-type=\"journal_article\" data-item-id=\"30568\"> References</a>\n" +
    "      </div>\n" +
    "      <div class=\"js-tablist__item\">\n" +
    "        <a href=\"#journal-full-text\" id=\"label_journal-full-text\" class=\"item js-tablist__link investigation\" data-tab=\"journal-full-text\" data-item-type=\"journal_article\" data-item-id=\"30568\"> Full Text</a>\n" +
    "      </div>\n" +
    "      <div class=\"js-tablist__item\">\n" +
    "        <a href=\"#journal-pdf\" id=\"label_journal-pdf\" class=\"item js-tablist__link\" data-tab=\"journal-pdf\">PDF</a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    \n" +
    "    \n" +
    "    <div id=\"journal-pdf\" class=\"ui bottom attached tab segment js-tab-content\" data-tab=\"journal-pdf\">\n" +
    "      <div class=\"journal-tab__copy\">\n" +
    "        <div class=\"ui\">\n" +
    "          <p class=\"read-now\">\n" +
    "            <a class=\"ui primary button\" title=\"Download\" href=\"/read/?item_type=journal_article&amp;item_id=30568&amp;mode=download\">\n" +
    "              <i class=\"download icon\"></i> Download (PDF) </a>\n" +
    "          </p>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>";

  public void testFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(article),
        Constants.DEFAULT_ENCODING);
    //log.info( StringUtil.fromInputStream(inStream));
    //log.info(filteredArticle);
    assertEquals(filteredArticle, StringUtil.fromInputStream(inStream));
  }

}
