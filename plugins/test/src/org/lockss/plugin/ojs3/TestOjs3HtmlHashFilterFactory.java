package org.lockss.plugin.ojs3;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestOjs3HtmlHashFilterFactory  extends LockssTestCase {
  private Ojs3HtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new Ojs3HtmlHashFilterFactory();
  }
  private static final String vancouver1 =
    "<div class=\"csl-entry\"><div class=\"csl-right-inline\">Dal PontGE. Lawyer Professionalism in the 21st Century. VULJ [Internet]. 2018Dec.31 [cited 2022May19];8(1):7\u001318. Available from: https://vulj.vu.edu.au/index.php/vulj/article/view/1132</div></div>";

  private static final String vancouver1Filt =
    "<div class=\"csl-entry\"><div class=\"csl-right-inline\">Dal PontGE. Lawyer Professionalism in the 21st Century. VULJ [Internet]. 2018Dec.31 ;8(1):7 18. Available from: https://vulj.vu.edu.au/index.php/vulj/article/view/1132</div></div>";

  private static final String vancouver2 =
      "<div class=\"csl-entry\"><div class=\"csl-left-margin\">1. </div><div class=\"csl-right-inline\">Hebbar PA, Pashilkar AA, Biswas P. Using eye tracking system for aircraft design \u0013 a flight simulator study. Aviation [Internet]. 2022Mar.22 [cited 2022 May 20];26(1):11\u001321. Available from: https://journals.vilniustech.lt/index.php/Aviation/article/view/16398</div></div>";

  private static final String vancouver2Filt =
      "<div class=\"csl-entry\"><div class=\"csl-left-margin\">1. </div><div class=\"csl-right-inline\">Hebbar PA, Pashilkar AA, Biswas P. Using eye tracking system for aircraft design a flight simulator study. Aviation [Internet]. 2022Mar.22 ;26(1):11 21. Available from: https://journals.vilniustech.lt/index.php/Aviation/article/view/16398</div></div>";

  private static final String vancouver3 =
      "<div class=\"csl-bib-body\">\n" +
          "  <div class=\"csl-entry\"><div class=\"csl-left-margin\">1.</div><div class=\"csl-right-inline\">Gross A. The Continuity of Scientific Discovery and Its Communication: The Example of Michael Faraday. Disc Collab [Internet]. 2009 Feb. 27 [cited 2022 Jun. 9];4:3. Available from: https://journals.uic.edu/ojs/index.php/jbdc/article/view/2444</div></div>\n" +
          "</div>";

  private static final String vancouver3Filt =
      "<div class=\"csl-entry\"><div class=\"csl-left-margin\">1.</div><div class=\"csl-right-inline\">Gross A. The Continuity of Scientific Discovery and Its Communication: The Example of Michael Faraday. Disc Collab [Internet]. 2009 Feb. 27 ;4:3. Available from: https://journals.uic.edu/ojs/index.php/jbdc/article/view/2444</div></div>";

  private static final String tarubian =
      "<div class=\"csl-entry\">Zeller, Bruno, and Richard Lightfoot. \u001CGood Faith: An ICSID Convention Requirement?\u001D. <i>Victoria University Law and Justice Journal</i> 8, no. 1 (December 31, 2018): 19\u001332. Accessed May 19, 2022. https://vulj.vu.edu.au/index.php/vulj/article/view/1139.</div>";

  private static final String tarubianFilt =
      "<div class=\"csl-entry\">Zeller, Bruno, and Richard Lightfoot. Good Faith: An ICSID Convention Requirement? . <i>Victoria University Law and Justice Journal</i> 8, no. 1 (December 31, 2018): 19 32. . https://vulj.vu.edu.au/index.php/vulj/article/view/1139.</div>";

  private static final String harvard =
      "<div class=\"csl-entry\">Mendoza, R. \u001CRuby\u001D . (2022) \u001CBook Review\u0014The Borders of AIDS: Race, Quarantine &amp; Resistance by Karma R. Chavez\u001D, <i>Literacy in Composition Studies</i>, 9(2), pp. 67-72. Available at: https://licsjournal.org/index.php/LiCS/article/view/2193 (Accessed: 21May2022).</div>";

  private static final String harvardFilt =
      "<div class=\"csl-entry\">Mendoza, R. Ruby . (2022) Book Review The Borders of AIDS: Race, Quarantine &amp; Resistance by Karma R. Chavez , <i>Literacy in Composition Studies</i>, 9(2), pp. 67-72. Available at: https://licsjournal.org/index.php/LiCS/article/view/2193 .</div>";

  private static final String abdnt =
      "<div class=\"csl-entry\">DRERUP, C.; EVESLAGE, M.; SUNDERKTTER, C.; EHRCHEN, J. Diagnostic Value of Laboratory Parameters for Distinguishing Between Herpes Zoster and Bacterial Superficial Skin and Soft Tissue Infections. <b>Acta Dermato-Venereologica</b>, <i>[S. l.]</i>, v. 100, n. 1, p. 1\u00135, 2020. DOI: 10.2340/00015555-3357. Disponvel em: https://medicaljournalssweden.se/actadv/article/view/1631. Acesso em: 19 may. 2022.</div>";

  private static final String abdntFilt =
      "<div class=\"csl-entry\">DRERUP, C.; EVESLAGE, M.; SUNDERKTTER, C.; EHRCHEN, J. Diagnostic Value of Laboratory Parameters for Distinguishing Between Herpes Zoster and Bacterial Superficial Skin and Soft Tissue Infections. <b>Acta Dermato-Venereologica</b>, <i>[S. l.]</i>, v. 100, n. 1, p. 1 5, 2020. DOI: 10.2340/00015555-3357. Disponvel em: https://medicaljournalssweden.se/actadv/article/view/1631. .</div>";

  private static final String jatsArticle =
    "<div class=\"jatsParser__center-article-block\">\n" +
      "<div class=\"jatsParser__article-fulltext\" id=\"jatsParserFullText\">\n" +
        "<h2 class=\"article-section-title jatsParser__abstract\">Abstract</h2>\n" +
        "The genus <em>Mahanarva</em> Distant, 1909 (Hemiptera: Cercopoidea: Cercopidae) currently includes two subgenera: <em>Mahanarva</em> Distant, 1909 with 38 species and six subspecies, and <em>Ipiranga</em> Fennah, 1968 with nine species. The <em>Manaharva</em> species are all from the Americas, and a few species are important pests in pasture grasses and sugarcane. There are no reports of any <em>Manaharva</em> species from North America, including Mexico and areas to the north. Here, a new species is described from Mexico and a key to the species of <em>Mahanarva</em><strong> </strong>from Central America and Mexico is proposed." +
      "</div>" +
    "</div>";

  private static final String jatsArticleFilt =
    "<div class=\"jatsParser__article-fulltext\" id=\"jatsParserFullText\"> " +
      "<h2 class=\"article-section-title jatsParser__abstract\">Abstract</h2> " +
      "The genus <em>Mahanarva</em> Distant, 1909 (Hemiptera: Cercopoidea: Cercopidae) currently includes two subgenera: <em>Mahanarva</em> Distant, 1909 with 38 species and six subspecies, and <em>Ipiranga</em> Fennah, 1968 with nine species. The <em>Manaharva</em> species are all from the Americas, and a few species are important pests in pasture grasses and sugarcane. There are no reports of any <em>Manaharva</em> species from North America, including Mexico and areas to the north. Here, a new species is described from Mexico and a key to the species of <em>Mahanarva</em><strong> </strong>from Central America and Mexico is proposed." +
    "</div>";

  private static final String articleWithStats =
    "<div class=\"main_entry\">" +
      "<h1>Artcle Title</h1>" +
      "<div style=\"padding-left: 4%;\">\n" +
        "|Resumen <div class=\"fa fa-eye\"></div> = <b>5729</b> veces\n" +
        "\n" +
        " |\n" +
        "PDF <div class=\"fa fa-eye\"></div> = <b>2414</b> veces|\n" +
        " |\n" +
        "XML <div class=\"fa fa-eye\"></div> = <b>306</b> veces|\n" +
        " |\n" +
        "HTML <div class=\"fa fa-eye\"></div> = <b>42</b> veces|\n" +
        "\n" +
      "</div>\n" +
      "<div class=\"item downloads_chart\">\n" +
        "<h3 class=\"label\">\n" +
        "Descargas\n" +
        "</h3>\n" +
        "<div class=\"value\">\n" +
          "\t\t<canvas class=\"usageStatsGraph\" data-object-type=\"Submission\" data-object-id=\"342002\"></canvas>\n" +
          "\t\t<div class=\"usageStatsUnavailable\" data-object-type=\"Submission\" data-object-id=\"342002\">\n" +
            "\t\t\tLos datos de descargas todavía no están disponibles.\n" +
          "</div>\n" +
        "</div>\n" +
      "</div>"+
      "<div class=\"article\">Body of article. Blah blah</div>" +
    "</div>\n";

  private static final String articleWithoutStats =
    "<div class=\"main_entry\">" +
      "<h1>Artcle Title</h1>" +
      " " +
      "<div class=\"article\">Body of article. Blah blah</div>" +
    "</div>";


  private static final String bodyBlockQuotes =
    "<body bgcolor=\"#ffffff\" LINK=\"#bb7777\" VLINK=\"#7777bb\" ALINK=\"#ffee99\" text=\"#000000\">\n" +
      "<blockquote>" +
        "<img src=\"https://journals.uic.edu/ojs/index.php/fm/article/download/10812/version/8410/10591/77128/logo.gif\" border=\"1\" alt=\"First Monday\" align=\"bottom\">" +
        "<br>" +
      "</blockquote>\n" +
      "<hr>\n" +
      "<blockquote>\n" +
        "\n" +
        "<center><a href=\"#author\"><img src=\"https://journals.uic.edu/ojs/index.php/fm/article/download/10812/version/8410/10591/77124/moll.png\" alt=\"Intimacy collapse: Temporality, pleasure, and embodiment in gay hook-up app use by Kristian Moller\" border=\"1\"></a></center>\n" +
        "\n" +
        "<br><hr><br>\n" +
        "\n" +"<p>Hook-up apps facilitate sexual, bodily encounters. At the same time, many participants report that they to a larger degree use the apps for &lsquo;sexting&rsquo;, that is, sending &ldquo;sexual images and sometimes sexual texts via cell phone and other electronic devices&rdquo; <a name=\"11a\"></a>[<a href=\"#11\">11</a>].</p>\n" +
        "\n" +
        "<p>Like the sexual technologies before it, hook-up apps reconfigure the flows between the somatic and the virtual, that is material bodies and their faculties on the one hand, and virtual bodies that emerge through media texts and representations. Hook-up apps in their materiality support both orientations towards what can be called more <em>organic intimacy</em> and that of more attuned to what we may call <em>representational intimacy</em>. Beyond being material and social facts, these intimacies are also normative. The <em>organic intimacy</em> script places bodily encounters as the meaningful way to have sexual encounters, placing textual encounters that do not lead to bodily meetings outside the intimate sphere. On the other hand <em>representational intimacy</em> implicitly values features of mediated culture: asynchronicity, radical performativity, and the ways this erotic mode is available to different publics. Campbell&rsquo;s (2004) study of sexual chat on online gay bodybuilding forums deeply explores what kinds of pleasure can be had when the somatic body of one&rsquo;s counterparts are made invisible. He found that within these spaces, textual bodies become objects of desire, and the ability to perform these bodies decides your desirability. Consequently, the material or &ldquo;real&rdquo; bodies of the others are not of immediate concern in this particular sexual practice. In terms of hook-up several works have highlighted that many interactions and exchanges are as much to be understood as pornographic producers of erotic charge as social and communicative (Mowlabocus, 2010; Tziallas, 2015).</p>\n" +
        "\n" +
        "<p>In Grindr and Scruff the choices of what you are &ldquo;looking for&rdquo; are very similar (see <a href=\"#fig1\">Figure 1</a>): You can mark yourself as looking for &ldquo;relationship&rdquo; and/or &ldquo;dates&rdquo;, which points toward future bodily meeting. By contrast, you can be looking for &ldquo;chat&rdquo; which takes place wholly online. The categories &ldquo;networking&rdquo; and &ldquo;friends&rdquo; are somewhat unclear as to which intimacy norm they adhere to. The &ldquo;right now&rdquo; and &ldquo;Random Play/NSA&rdquo; categories mark the user as looking for here-and-now sex, but it is entirely up to interpretation if it includes either organic or representational sexual intimacies, or both. As such, in the current mediatization of cruising, there is a material opening which supports the &ldquo;seeping&rdquo; of both intimacies into each other.</p>\n" +
        "\n" +
      "</blockquote>\n" +
    "</body>";

  private static final String bodyBlockQuotesFiltered =
    "<body bgcolor=\"#ffffff\" LINK=\"#bb7777\" VLINK=\"#7777bb\" ALINK=\"#ffee99\" text=\"#000000\"> <blockquote><img src=\"https://journals.uic.edu/ojs/index.php/fm/article/download/10812/version/8410/10591/77128/logo.gif\" border=\"1\" alt=\"First Monday\" align=\"bottom\"><br></blockquote> <hr> <blockquote> <center><a href=\"#author\"><img src=\"https://journals.uic.edu/ojs/index.php/fm/article/download/10812/version/8410/10591/77124/moll.png\" alt=\"Intimacy collapse: Temporality, pleasure, and embodiment in gay hook-up app use by Kristian Moller\" border=\"1\"></a></center> <br><hr><br> <p>Hook-up apps facilitate sexual, bodily encounters. At the same time, many participants report that they to a larger degree use the apps for &lsquo;sexting&rsquo;, that is, sending &ldquo;sexual images and sometimes sexual texts via cell phone and other electronic devices&rdquo; <a name=\"11a\"></a>[<a href=\"#11\">11</a>].</p> <p>Like the sexual technologies before it, hook-up apps reconfigure the flows between the somatic and the virtual, that is material bodies and their faculties on the one hand, and virtual bodies that emerge through media texts and representations. Hook-up apps in their materiality support both orientations towards what can be called more <em>organic intimacy</em> and that of more attuned to what we may call <em>representational intimacy</em>. Beyond being material and social facts, these intimacies are also normative. The <em>organic intimacy</em> script places bodily encounters as the meaningful way to have sexual encounters, placing textual encounters that do not lead to bodily meetings outside the intimate sphere. On the other hand <em>representational intimacy</em> implicitly values features of mediated culture: asynchronicity, radical performativity, and the ways this erotic mode is available to different publics. Campbell&rsquo;s (2004) study of sexual chat on online gay bodybuilding forums deeply explores what kinds of pleasure can be had when the somatic body of one&rsquo;s counterparts are made invisible. He found that within these spaces, textual bodies become objects of desire, and the ability to perform these bodies decides your desirability. Consequently, the material or &ldquo;real&rdquo; bodies of the others are not of immediate concern in this particular sexual practice. In terms of hook-up several works have highlighted that many interactions and exchanges are as much to be understood as pornographic producers of erotic charge as social and communicative (Mowlabocus, 2010; Tziallas, 2015).</p> <p>In Grindr and Scruff the choices of what you are &ldquo;looking for&rdquo; are very similar (see <a href=\"#fig1\">Figure 1</a>): You can mark yourself as looking for &ldquo;relationship&rdquo; and/or &ldquo;dates&rdquo;, which points toward future bodily meeting. By contrast, you can be looking for &ldquo;chat&rdquo; which takes place wholly online. The categories &ldquo;networking&rdquo; and &ldquo;friends&rdquo; are somewhat unclear as to which intimacy norm they adhere to. The &ldquo;right now&rdquo; and &ldquo;Random Play/NSA&rdquo; categories mark the user as looking for here-and-now sex, but it is entirely up to interpretation if it includes either organic or representational sexual intimacies, or both. As such, in the current mediatization of cruising, there is a material opening which supports the &ldquo;seeping&rdquo; of both intimacies into each other.</p> </blockquote> </body>";

  private static final String bodyReferences =
    "<body>" +
      " [1] W R Ketterhagen, M T am Ende, B C Hancock, Process modeling in the pharmaceutical industry using the discrete element method, J. Pharm. Sci. 98, 442 (2009)." +
      "<br>" +
      "<a href=\"https://doi.org/10.1002/jps.21466\" target=\"_blank\">https://doi.org/10.1002/jps.21466</a>" +
      "<br>" +
      "<br>" +
      " [2] P W Cleary, Industrial particle flow modellingusing discrete element method, Eng. Comput. 26, 698 (2009)." +
      "<br>" +
      "<a href=\"https://doi.org/10.1108/02644400910975487\" target=\"_blank\">https://doi.org/10.1108/02644400910975487</a>" +
      "<br>" +
      "<br>" +
    "</body>";

  private static final String bodyMalformedReferences =
      "<body>" +
        " 1] W R Ketterhagen, M T am Ende, B C Hancock, Process modeling in the pharmaceutical industry using the discrete element method, J. Pharm. Sci. 98, 442 (2009)." +
        "<br>" +
        "<a href=\"https://doi.org/10.1002/jps.21466\" target=\"_blank\">https://doi.org/10.1002/jps.21466</a>" +
        "<br>" +
        "<br>" +
        " [2] P W Cleary, Industrial particle flow modellingusing discrete element method, Eng. Comput. 26, 698 (2009)." +
        "<br>" +
        "<a href=\"https://doi.org/10.1108/02644400910975487\" target=\"_blank\">https://doi.org/10.1108/02644400910975487</a>" +
        "<br>" +
        "<br>" +
      "</body>";

  private static final String bodyAndPs =
    "<body class=\"c13\">" +
      "<div>" +
        "<p class=\"c2\">" +
          "<span class=\"c0\">Alisha Rao Sister Cities </span>" +
          "<span class=\"c0 c12\">AmeriQuests </span>" +
          "<span class=\"c1 c0\">17.1 (2022)</span>" +
        "</p>" +
        "<p class=\"c6 c9\">" +
          "<span class=\"c1 c10\">" +
          "</span>" +
        "</p>" +
      "</div>" +
      "<p class=\"c6\">" +
        "<span class=\"c1 c11\">Sister Cities </span>" +
      "</p>" +
      "<p class=\"c6\">" +
        "<span class=\"c1 c0\">Alisha Rao</span>" +
      "</p>" +
      "<p class=\"c6 c9\">" +
        "<span class=\"c1 c0\"></span>" +
      "</p>" +
      "<p class=\"c6\">" +
        "<span class=\"c0\">Standing in Westminster College, where 75 years ago Winston Churchill gave his Iron Curtain Speech, Senator Bernie Sanders of the United States shared his conception of world affairs. The audience that Churchill addressed was facing a very different set of concerns, but Sanders made some remarks that resonated with the past: </span>" +
        "<span class=\"c3\">" +
          "<a class=\"c5\" href=\"https://www.google.com/url?q=https://www.vox.com/world/2017/9/21/16345600/bernie-sanders-full-text-transcript-foreign-policy-speech-westminster&amp;sa=D&amp;source=editors&amp;ust=1649224029777410&amp;usg=AOvVaw2C9BKXHvUR67mWiGOJ0-uc\">In my view the United States must seek partnerships not just between governments, but between peoples& a sensible and effective foreign policy recognizes that our safety and welfare is bound up with the safety and welfare of others around the world</a>" +
        "</span>" +
        "<span class=\"c1 c0\">. </span>" +
      "</p>" +
      "<p class=\"c4\">" +
        "<span class=\"c1 c0\">Since entering politics, Sanders approach to international affairs reflects his resistance to the romanticized image of American policy established and brought to life by the countrys elite, on both the domestic and national stages. He has insisted that unity within and beyond borders can be attained despite cultural differences, and divergent national interests.</span>" +
      "</p>" +
      "<p class=\"c4\">" +
        "<span class=\"c0\">From the early days of his political career, as mayor of Vermont, Sanders has </span>" +
        "<span class=\"c3\">" +
          "<a class=\"c5\" href=\"https://www.google.com/url?q=https://www.nytimes.com/interactive/2020/us/politics/bernie-sanders-foreign-policy.html&amp;sa=D&amp;source=editors&amp;ust=1649224029778208&amp;usg=AOvVaw0AathlX417-DXolaTsWgdy\">challenged the rigid approach of Washington bureaucrats</a>" +
        "</span>" +
        "<span class=\"c0\">. In a Cold War era of heightened tensions, he gave a city with 45,000 individuals its </span>" +
        "<span class=\"c3\">" +
          "<a class=\"c5\" href=\"https://www.google.com/url?q=https://www.nytimes.com/2019/05/17/us/bernie-sanders-burlington-mayor.html&amp;sa=D&amp;source=editors&amp;ust=1649224029778654&amp;usg=AOvVaw20eQr4QXclWnFaaMmIaW36\">own foreign policy</a>" +
        "</span>" +
        "<span class=\"c0 c1\">&nbsp;and agenda, in the hope that his city could become a beacon for the rest of the nation. His message, to build bridges rather than walls, created national news, particularly when he established sister cities with Puerto Cabeza, Venezuela and Yaroslavl, Russia. They became conduits through which communication could pass, and they created a sense of shared humanity beyond the covers of war and conflict. This kinship helped citizens of very different states to recognize shared ideals of empathy and connection. This effort had the effect of empowering ordinary citizens. </span>" +
      "</p>" +
    "</body>";

  private static final String bodyWdivArticle =
      "<body bgcolor=\"#f8f8f8\">\n" +
      "      \n" +
      "      \n" +
      "      <div id=\"article0-front\" class=\"fm\">\n" +
      "         \n" +
      "         \n" +
      "         <table width=\"100%\" class=\"fm\">\n" +
      "            \n" +
      "            \n" +
      "            <tbody>" +
                    "<tr>\n" +
      "               <td width=\"50%\"></td>\n" +
      "               <td></td>\n" +
       "            </tr>\n" +
      "            \n" +
      "            <tr>\n" +
      "               \n" +
      "               <td valign=\"top\"><span class=\"gen\">Journal Information</span><br>\n" +
      "                  <span class=\"gen\">Journal ID (</span>publisher<span class=\"gen\">): </span>J Biomed Discov<br>\n" +
      "                  <span class=\"gen\">ISSN: </span>1911-2092<br>\n" +
      "                  <span class=\"gen\">Publisher: </span>University of Illinois at Chicago Library<br>\n" +
      "                  \n" +
      "               </td>\n" +
      "               \n" +
      "               <td valign=\"top\"><span class=\"gen\">Article Information</span><br>                  \n" +
      "                 <span class=\"gen\">Copyright: </span>2009\n" +
      "                  <br>                  \n" +
      "                  <span class=\"gen\">Received </span><span class=\"gen\">Day: </span> <span class=\"gen\">Month: </span>12 <span class=\"gen\">Year: </span>2008<br>                  \n" +
      "                  <span class=\"gen\">Accepted </span><span class=\"gen\">Day: </span>15 <span class=\"gen\">Month: </span>02 <span class=\"gen\">Year: </span>2009<br>\n" +
      "                  <span class=\"gen\">Electronic </span><span class=\"gen\"> publication date: </span><span class=\"gen\">Day: </span>25 <span class=\"gen\">Month: </span>02 <span class=\"gen\">Year: </span>2009<br>\n" +
      "                  <span class=\"gen\">Volume: </span>4 <span class=\"gen\">Issue: </span><br>\n" +
      "                  <span class=\"gen\">First Page: </span>3 <span class=\"gen\">Last Page: </span><br>\n" +
      "                  <span class=\"gen\">PubMed Id: </span>19350498<br>\n" +
      "                  \n" +
      "               </td>\n" +
      "               \n" +
      "            </tr>\n" +
      "            \n" +
      "            <tr>\n" +
      "               <td colspan=\"2\" valign=\"top\">\n" +
      "                  <hr class=\"part-rule\">\n" +
      "               </td>\n" +
      "            </tr>\n" +
      "            \n" +
      "         </tbody></table>\n" +
      "         \n" +
      "         \n" +
      "         \n" +
      "      </div>\n" +
      "      \n" +
      "      \n" +
      "      <div id=\"article-level-0-body\" class=\"body\">\n" +
      "         \n" +
      "         \n" +
      "         <hr class=\"part-rule\">\n" +
      "         \n" +
      "         \n" +
      "         <div>\n" +
      "            <span class=\"tl-main-part\">Preface</span>\n" +
      "            \n" +
      "            <p>There is, says Richard Feynman, a rhythm and a pattern between the phenomena of nature which is not apparent to the eye,\n" +
      "               but only to the eye of analysis. In reflecting on scientific discovery, Feynman uses this metaphor to describe a process\n" +
      "               that is, in his view, essentially metaphorical, a constructive movement from seeing to seeing as, from sight to insight. This\n" +
      "               is also Faraday's view of scientific discovery. In making his first significant electro-magnetic discovery, he learns to see\n" +
      "               apparent magnetic attraction and repulsion as, in reality, circular motion, the first hint of the existence of an electro-magnetic\n" +
      "               field. In literary experiments carried out in connection with this discovery, he makes another discovery, a discovery about\n" +
      "               scientific communication. He learns to recreate for his peers the feeling of discovery he has experienced. For Faraday, this\n" +
      "               strategy will apply across the board, whether his audience is professional or popular. While he must accommodate what he knows\n" +
      "               to the limited attention span and state of knowledge of the general audiences of his popular lectures, he is determined never\n" +
      "               to simplify to the point of omitting from his exposition what it is to make a scientific discovery. As we move from his Diary,\n" +
      "               tohis scientific papers, to his lectures to young people, we see a continuum in the way scientific discovery is conveyed.\n" +
      "               In all of these cases, Faraday leads us on a journey from seeing to seeing as, from sight to insight. \n" +
      "            </p>\n" +
      "            \n" +
      "         </div>\n" +
      "         \n" +
      "         \n" +
      "         <hr class=\"section-rule\">\n" +
      "         \n" +
      "         <div>\n" +
      "            <span class=\"tl-main-part\">Faraday's Discovery </span>\n" +
      "            \n" +
      "            <p>In 1821, at the age of thirty-one, a virtually unknown Michael Faraday published in the Quarterly Journal of Science his first\n" +
      "               significant discovery: the reciprocal circular motion of a magnet and an electric current, the first foray in the life-long\n" +
      "               enterprise of developing a field theory of magnetism and electricity. In a letter to Charles-Gaspard de la Rive, a Swiss professor\n" +
      "               of chemistry, dated 12 September, Faraday makes it clear that discovery for him is the coincidence of seeing and seeing as:\n" +
      "               \n" +
      "            </p>\n" +
      "            \n" +
      "            \n" +
      "            <blockquote>\n" +
      "               <p>I find that all the usual attractions and repulsions of the Magnetic needle by the conjunctive wire are deceptions[,] the\n" +
      "                  motions being not attractions &amp;c or repulsions nor the result of any attractive or repulsive force but the results of a force\n" +
      "                  in the wire which instead of bringing the pole of the needle nearer to or farther from the wire endeavours to make it move\n" +
      "                  round it in a never ending circle and motion whilst the battery remains in action[.] I have succeeded not only in shewing\n" +
      "                  the existence of this motion theoretically but experimentally and have been able to make the wire revolve round a magnetic\n" +
      "                  pole or a magnetic pole round the wire at pleasure[.] The law of revolution and to which the other motions of the needle and\n" +
      "                  wire are reducible is simple and beautiful [1, p. 222].\n" +
      "               </p>\n" +
      "               \n" +
      "            </blockquote>\n" +
      "            \n" +
      "         </div>\n" +
      "         \n" +
      "         \n" +
      "         \n" +
      "      </div>\n" +
      "</body>";

  private static String bodyWdivArticleFiltered =
    "<body bgcolor=\"#f8f8f8\"> <div id=\"article0-front\" class=\"fm\"> <table width=\"100%\" class=\"fm\"> <tbody><tr> <td width=\"50%\"></td> <td></td> </tr> <tr> <td valign=\"top\"><span class=\"gen\">Journal Information</span><br> <span class=\"gen\">Journal ID (</span>publisher<span class=\"gen\">): </span>J Biomed Discov<br> <span class=\"gen\">ISSN: </span>1911-2092<br> <span class=\"gen\">Publisher: </span>University of Illinois at Chicago Library<br> </td> <td valign=\"top\"><span class=\"gen\">Article Information</span><br> <span class=\"gen\">Copyright: </span>2009 <br> <span class=\"gen\">Received </span><span class=\"gen\">Day: </span> <span class=\"gen\">Month: </span>12 <span class=\"gen\">Year: </span>2008<br> <span class=\"gen\">Accepted </span><span class=\"gen\">Day: </span>15 <span class=\"gen\">Month: </span>02 <span class=\"gen\">Year: </span>2009<br> <span class=\"gen\">Electronic </span><span class=\"gen\"> publication date: </span><span class=\"gen\">Day: </span>25 <span class=\"gen\">Month: </span>02 <span class=\"gen\">Year: </span>2009<br> <span class=\"gen\">Volume: </span>4 <span class=\"gen\">Issue: </span><br> <span class=\"gen\">First Page: </span>3 <span class=\"gen\">Last Page: </span><br> <span class=\"gen\">PubMed Id: </span>19350498<br> </td> </tr> <tr> <td colspan=\"2\" valign=\"top\"> <hr class=\"part-rule\"> </td> </tr> </tbody></table> </div> <div id=\"article-level-0-body\" class=\"body\"> <hr class=\"part-rule\"> <div> <span class=\"tl-main-part\">Preface</span> <p>There is, says Richard Feynman, a rhythm and a pattern between the phenomena of nature which is not apparent to the eye, but only to the eye of analysis. In reflecting on scientific discovery, Feynman uses this metaphor to describe a process that is, in his view, essentially metaphorical, a constructive movement from seeing to seeing as, from sight to insight. This is also Faraday's view of scientific discovery. In making his first significant electro-magnetic discovery, he learns to see apparent magnetic attraction and repulsion as, in reality, circular motion, the first hint of the existence of an electro-magnetic field. In literary experiments carried out in connection with this discovery, he makes another discovery, a discovery about scientific communication. He learns to recreate for his peers the feeling of discovery he has experienced. For Faraday, this strategy will apply across the board, whether his audience is professional or popular. While he must accommodate what he knows to the limited attention span and state of knowledge of the general audiences of his popular lectures, he is determined never to simplify to the point of omitting from his exposition what it is to make a scientific discovery. As we move from his Diary, tohis scientific papers, to his lectures to young people, we see a continuum in the way scientific discovery is conveyed. In all of these cases, Faraday leads us on a journey from seeing to seeing as, from sight to insight. </p> </div> <hr class=\"section-rule\"> <div> <span class=\"tl-main-part\">Faraday's Discovery </span> <p>In 1821, at the age of thirty-one, a virtually unknown Michael Faraday published in the Quarterly Journal of Science his first significant discovery: the reciprocal circular motion of a magnet and an electric current, the first foray in the life-long enterprise of developing a field theory of magnetism and electricity. In a letter to Charles-Gaspard de la Rive, a Swiss professor of chemistry, dated 12 September, Faraday makes it clear that discovery for him is the coincidence of seeing and seeing as: </p> <blockquote> <p>I find that all the usual attractions and repulsions of the Magnetic needle by the conjunctive wire are deceptions[,] the motions being not attractions &amp;c or repulsions nor the result of any attractive or repulsive force but the results of a force in the wire which instead of bringing the pole of the needle nearer to or farther from the wire endeavours to make it move round it in a never ending circle and motion whilst the battery remains in action[.] I have succeeded not only in shewing the existence of this motion theoretically but experimentally and have been able to make the wire revolve round a magnetic pole or a magnetic pole round the wire at pleasure[.] The law of revolution and to which the other motions of the needle and wire are reducible is simple and beautiful [1, p. 222]. </p> </blockquote> </div> </div> </body>";

  private static final String pdf2html =
    "<body>" +
      "<div id=\"page-container\">" +
        "<div id=\"pf1\" class=\"pf w0 h0\" data-page-no=\"1\">" +
          "<div class=\"pc pc1 w0 h0 opened\">" +
            "<img />" + // embedded image data
          "</div>" +
        "</div>" +
        "<div id=\"pf2\" class=\"pf w0 h0\" data-page-no=\"2\">" +
          "<div class=\"pc pc2 w0 h0 opened\">" +
            "<img />" + // embedded image data
          "</div>" +
        "</div>"+
      "</div>" +
    "</body>";

  private static final String pdf2htmlFiltered =
    "<div id=\"pf1\" class=\"pf w0 h0\" data-page-no=\"1\">" +
      "<div class=\"pc pc1 w0 h0 opened\">" +
        "<img />" + // embedded image data
      "</div>" +
    "</div>" +
    "<div id=\"pf2\" class=\"pf w0 h0\" data-page-no=\"2\">" +
      "<div class=\"pc pc2 w0 h0 opened\">" +
        "<img />" + // embedded image data
      "</div>" +
    "</div>";

  private static final String spanWithTitleAttrKeep =
    "<span class=\"Z3988\" title=\"ctx_ver=Z39.88-2004&rft_id=https%3A%2F%2Frevistacta.agrosavia.co%2Findex.php%2Frevista%2Farticle%2Fview%2F1050&rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Ajournal&rft.language=es_ES&rft.genre=article&rft.title=Ciencia+y+Tecnolog%C3%ADa+Agropecuaria&rft.jtitle=Ciencia+y+Tecnolog%C3%ADa+Agropecuaria&rft.atitle=Lineamientos+para+una+metodolog%C3%ADa+de+identificaci%C3%B3n+de+estilos+de+aprendizaje+aplicables+al+sector+agropecuario+colombiano&rft.artnum=1050&rft.stitle=CTA&rft.volume=21&rft.issue=3&rft.aulast=Rodr%C3%ADguez-Espinosa&rft.aufirst=Holmes&rft.date=2020-08-18&rft.au=Carlos+Eduardo+Ospina-Parra&rft.au=Carlos+Juli%C3%A1n+Ram%C3%ADrez-G%C3%B3mez&rft.au=Isabel+Cristina+Toro-Gonz%C3%A1lez&rft.au=Alexandra+Gallego-Lopera&rft.au=Mar%C3%ADa+Alejandra+Piedrahita-P%C3%A9rez&rft.au=Alexandra+Vel%C3%A1squez-Chica&rft.au=Swammy+Guti%C3%A9rrez-Molina&rft.au=Natalia+Fl%C3%B3rez-Tuta&rft.au=Oscar+Dar%C3%ADo+Hincapi%C3%A9-Echeverri&rft.au=Laura+Cristina+Romero-Rubio&rft_id=info%3Adoi%2F10.21930%2Frcta.vol21_num3_art%3A1050&rft.pages=1-19&rft.issn=0120-8322&rft.eissn=2500-5308\">"+
    "</span>";

  private static final String getSpanWithTitleAttrKeepTransformed =
      "<span class=\"Z3988\" title=\"ctx_ver=Z39.88-2004&rft_id=https%3A%2F%2Frevistacta.agrosavia.co%2Findex.php%2Frevista%2Farticle%2Fview%2F1050&rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Ajournal&rft.language=es_ES&rft.genre=article&rft.title=Ciencia+y+Tecnolog%C3%ADa+Agropecuaria&rft.jtitle=Ciencia+y+Tecnolog%C3%ADa+Agropecuaria&rft.atitle=Lineamientos+para+una+metodolog%C3%ADa+de+identificaci%C3%B3n+de+estilos+de+aprendizaje+aplicables+al+sector+agropecuario+colombiano&rft.artnum=1050&rft.stitle=CTA&rft.volume=21&rft.issue=3&rft.aulast=Rodr%C3%ADguez-Espinosa&rft.aufirst=Holmes&rft.date=2020-08-18&rft.au=Carlos+Eduardo+Ospina-Parra&rft.au=Carlos+Juli%C3%A1n+Ram%C3%ADrez-G%C3%B3mez&rft.au=Isabel+Cristina+Toro-Gonz%C3%A1lez&rft.au=Alexandra+Gallego-Lopera&rft.au=Mar%C3%ADa+Alejandra+Piedrahita-P%C3%A9rez&rft.au=Alexandra+Vel%C3%A1squez-Chica&rft.au=Swammy+Guti%C3%A9rrez-Molina&rft.au=Natalia+Fl%C3%B3rez-Tuta&rft.au=Oscar+Dar%C3%ADo+Hincapi%C3%A9-Echeverri&rft.au=Laura+Cristina+Romero-Rubio&rft_id=info%3Adoi%2F10.21930%2Frcta.vol21_num3_art%3A1050&rft.pages=1-19&rft.issn=0120-8322&rft.eissn=2500-5308\">ctx_ver=Z39.88-2004&rft_id=https%3A%2F%2Frevistacta.agrosavia.co%2Findex.php%2Frevista%2Farticle%2Fview%2F1050&rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Ajournal&rft.language=es_ES&rft.genre=article&rft.title=Ciencia+y+Tecnolog%C3%ADa+Agropecuaria&rft.jtitle=Ciencia+y+Tecnolog%C3%ADa+Agropecuaria&rft.atitle=Lineamientos+para+una+metodolog%C3%ADa+de+identificaci%C3%B3n+de+estilos+de+aprendizaje+aplicables+al+sector+agropecuario+colombiano&rft.artnum=1050&rft.stitle=CTA&rft.volume=21&rft.issue=3&rft.aulast=Rodr%C3%ADguez-Espinosa&rft.aufirst=Holmes&rft.date=2020-08-18&rft.au=Carlos+Eduardo+Ospina-Parra&rft.au=Carlos+Juli%C3%A1n+Ram%C3%ADrez-G%C3%B3mez&rft.au=Isabel+Cristina+Toro-Gonz%C3%A1lez&rft.au=Alexandra+Gallego-Lopera&rft.au=Mar%C3%ADa+Alejandra+Piedrahita-P%C3%A9rez&rft.au=Alexandra+Vel%C3%A1squez-Chica&rft.au=Swammy+Guti%C3%A9rrez-Molina&rft.au=Natalia+Fl%C3%B3rez-Tuta&rft.au=Oscar+Dar%C3%ADo+Hincapi%C3%A9-Echeverri&rft.au=Laura+Cristina+Romero-Rubio&rft_id=info%3Adoi%2F10.21930%2Frcta.vol21_num3_art%3A1050&rft.pages=1-19&rft.issn=0120-8322&rft.eissn=2500-5308</span>";

  public void testAbdntFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(abdnt), abdntFilt);
  }
  public void testVancouverFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(vancouver1), vancouver1Filt);
  }
  public void testVancouverBFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(vancouver2), vancouver2Filt);
  }
  public void testVancouver3iltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(vancouver3), vancouver3Filt);
  }
  public void testHarvardFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(harvard), harvardFilt);
  }
  public void testTarubianFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(tarubian), tarubianFilt);
  }
  public void testJatsArtFiltering() throws Exception {
    assertEquals(getStringfromFilteredInputStream(jatsArticle), jatsArticleFilt);
  }
  public void testRemoveViewCount() throws Exception {
    assertEquals(getStringfromFilteredInputStream(articleWithStats), articleWithoutStats);
  }

  public void testKeepSparseHtml() throws Exception {
    assertEquals(getStringfromFilteredInputStream(bodyBlockQuotes), bodyBlockQuotesFiltered);
  }
  public void testKeepSparseHtml2() throws Exception {
    assertEquals(getStringfromFilteredInputStream(bodyReferences), bodyReferences);
    assertEquals(getStringfromFilteredInputStream(bodyMalformedReferences), bodyMalformedReferences);
  }
  public void testKeepSparseHtml3() throws Exception {
    assertEquals(getStringfromFilteredInputStream(bodyAndPs), bodyAndPs);
  }

  public void testKeepSparseHtml4() throws Exception {
    assertEquals(getStringfromFilteredInputStream(bodyWdivArticle), bodyWdivArticleFiltered);
  }
  public void testKeepPdfImageData() throws Exception {
    assertEquals(getStringfromFilteredInputStream(pdf2html), pdf2htmlFiltered);
  }
  public void testKeepAndConvertSpanTitle() throws Exception {
    assertEquals(getStringfromFilteredInputStream(spanWithTitleAttrKeep), getSpanWithTitleAttrKeepTransformed);
  }

  public String getStringfromFilteredInputStream(String in) throws IOException {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(in),
        Constants.DEFAULT_ENCODING);
    return StringUtil.fromInputStream(actIn);
  }
}
