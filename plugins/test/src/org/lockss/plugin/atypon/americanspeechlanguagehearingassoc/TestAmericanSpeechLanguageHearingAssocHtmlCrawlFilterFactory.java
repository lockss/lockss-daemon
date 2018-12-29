/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the \"Software\"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/


package org.lockss.plugin.atypon.americanspeechlanguagehearingassoc;

import java.io.*;
import java.nio.ByteBuffer;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.*;
import org.apache.commons.io.FileUtils;


public class TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory extends LockssTestCase {

  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;

  private static final String PLUGIN_ID =
          "org.lockss.plugin.atypon.americanspeechlanguagehearingassoc.AmericanSpeechLanguageHearingAssocAtyponPlugin";

  private static final String manifestContent =
      "<html>\n" +
              "<head>\n" +
              "    <title>American Journal of Audiology 2018 CLOCKSS Manifest Page</title>\n" +
              "    <meta charset=\"UTF-8\" />\n" +
              "</head>\n" +
              "<body>\n" +
              "<h1>American Journal of Audiology 2018 CLOCKSS Manifest Page</h1>\n" +
              "<ul>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/4\">December 2018 (Vol. 27 Issue 4)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/3S\">November 2018 (Vol. 27 Issue 3S)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/3\">September 2018 (Vol. 27 Issue 3)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/2\">June 2018 (Vol. 27 Issue 2)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/1\">March 2018 (Vol. 27 Issue 1)</a></li>\n" +
              "    \n" +
              "</ul>\n" +
              "<p>\n" +
              "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>\n" +
              "    The CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.\n" +
              "</p>\n" +
              "</body>\n" +
              "</html>";

  private static final String manifestHashFiltered = " December 2018 (Vol. 27 Issue 4) November 2018 (Vol. 27 Issue 3S) " +
          "September 2018 (Vol. 27 Issue 3) June 2018 (Vol. 27 Issue 2) March 2018 (Vol. 27 Issue 1) ";

  // Since Java String can not exceed 64K, need to read the html content from file to test it
  private static String tocContent = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/tocContent.html";
      tocContent  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String tocContentCrawlFiltered = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/tocContentCrawlFiltered.html";
      tocContentCrawlFiltered  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiFullContent = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiFullContent.html";
      doiFullContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiFullContentCrawlFiltered = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiFullContentCrawlFiltered.html";
      doiFullContentCrawlFiltered  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiAbsContent = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiAbsContent.html";
      doiAbsContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiAbsContentCrawlFiltered = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiAbsContentCrawlFiltered.html";
      doiAbsContentCrawlFiltered  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiPdfContent = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiPdfContent.html";
      doiPdfContent = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String doiPdfContentCrawlFiltered = "";
  {
    try {
      String currentDirectory = System.getProperty("user.dir");
      String pathname = currentDirectory +
              "/plugins/test/src/org/lockss/plugin/atypon/americanspeechlanguagehearingassoc/doiPdfContentCrawlFiltered.html";
      doiPdfContentCrawlFiltered  = FileUtils.readFileToString(new File(pathname), Constants.DEFAULT_ENCODING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static final String tocContentHashFiltered = " Clinical Focus Clinical Focus 08 March 2018 Audiological Assessment of Word Recognition Skills in Persons With Aphasia Min Zhang , Sheila R. Pratt , Patrick J. Doyle , Malcolm R. McNeil , John D. Durrant , Jillyn Roxberg , and Amanda Ortmann https://doi.org/10.1044/2017_AJA-17-0041 Preview Abstract Purpose The purpose of this study was to evaluate the ability of persons with aphasia, with and without hearing loss, to complete a commonly used open-set word recognition test that requires a verbal response. Furthermore, phonotactic probabilities and ... Abstract Full text PDF Clinical Focus 08 March 2018 Management of Recurrent Vestibular Neuritis in a Patient Treated for Rheumatoid Arthritis Richard A. Roberts https://doi.org/10.1044/2017_AJA-17-0090 Preview Abstract Purpose This clinical report is presented to describe how results of vestibular function testing were considered along with other medical history to develop a management plan that was ultimately successful. Method The patient underwent audio-vestibular ... Abstract Full text PDF Clinical Focus 08 March 2018 Clinicians' Guide to Obtaining a Valid Auditory Brainstem Response to Determine Hearing Status: Signal, Noise, and Cross-Checks Linda W. Norrix , and David Velenovsky https://doi.org/10.1044/2017_AJA-17-0074 Preview Abstract Purpose The auditory brainstem response (ABR) is a powerful tool for making clinical decisions about the presence, degree, and type of hearing loss in individuals in whom behavioral hearing thresholds cannot be obtained or are not reliable. Although the ... Abstract Full text PDF Clinical Focus 08 March 2018 Validation of the Chinese Sound Test: Auditory Performance of Hearing Aid Users Yu-Chen Hung , Ya-Jung Lee , and Li-Chiun Tsai https://doi.org/10.1044/2017_AJA-17-0057 Preview Abstract Purpose The Chinese Sound Test (Hung, Lin, Tsai, &amp; Lee, 2016) has been recently developed as a modified version of the Ling Six-Sound Test (Ling, 2012). By incorporating Chinese speech sounds, this test should be able to estimate whether the listener can ... Abstract Full text PDF Research Articles Research Article 08 March 2018 Improving Hearing Aid Self-Efficacy and Utility Through Revising a Hearing Aid User Guide: A Pilot Study Alexandra McMullan , Rebecca J. Kelly-Campbell , and Kim Wise https://doi.org/10.1044/2017_AJA-17-0035 Preview Abstract Purpose This pilot study aimed to investigate whether revising a hearing aid user guide (HAUG) is associated with improved hearing aid self-efficacy and utility performance. Method In Part 1, an HAUG was evaluated using the Suitability Assessment of Material ... Abstract Full text PDF Research Article 08 March 2018 Newborn Screening of Genetic Mutations in Common Deafness Genes With Bloodspot-Based Gene Chip Array Xuehu He , Xiuzhong Li , Yaqi Guo , Yue Zhao , Hui Dong , Jie Dong , Li Zhong , Zhiyun Shi , Yuying Zhang , Mario Soliman , Chunhua Song , and Zhijun Zhao https://doi.org/10.1044/2017_AJA-17-0042 Preview Abstract Purpose This study screens for deafness gene mutations in newborns in the Northwest China population. Method The 9 sites of 4 common deafness genes ( GJB2, GJB3, SLC26A 4, and mt 12S rRNA ) were detected by bloodspot-based gene chip array in 2,500 newborns. ... Abstract Full text PDF Research Article 08 March 2018 Investigating the Knowledge, Skills, and Tasks Required for Hearing Aid Management: Perspectives of Clinicians and Hearing Aid Owners Rebecca J. Bennett , Carly J. Meyer , Robert H. Eikelboom , and Marcus D. Atlas https://doi.org/10.1044/2017_AJA-17-0059 Preview Abstract Purpose The purpose of this study is to identify hearing aid owners' and clinicians' opinions of the knowledge, skills, and tasks required for hearing aid management and the importance of each of these to overall success with hearing aids. Method Concept ... Abstract Full text PDF Research Article 08 March 2018 Binaural Speech Understanding With Bilateral Cochlear Implants in Reverberation Kostas Kokkinakis https://doi.org/10.1044/2017_AJA-17-0065 Preview Abstract Purpose The purpose of this study was to investigate whether bilateral cochlear implant (CI) listeners who are fitted with clinical processors are able to benefit from binaural advantages under reverberant conditions. Another aim of this contribution was ... Abstract Full text PDF Research Article 08 March 2018 The Effects of Varying Directional Bandwidth in Hearing Aid Users' Preference and Speech-in-Noise Performance Adriana Goyette , Jeff Crukley , and Jason Galster https://doi.org/10.1044/2017_AJA-17-0063 Preview Abstract Purpose Directional microphone systems are typically used to improve hearing aid users' understanding of speech in noise. However, directional microphones also increase internal hearing aid noise. The purpose of this study was to investigate how varying ... Abstract Full text PDF Research Article 08 March 2018 Reevaluating Order Effects in the Binaural Bithermal Caloric Test Elizabeth Burnette , Erin G. Piker , and Dennis Frank-Ito https://doi.org/10.1044/2017_AJA-17-0028 Preview Abstract Purpose The purpose of this study was to determine whether a significant order effect exists in the binaural bithermal caloric test. Method Fifteen volunteers (mean age = 24.3 years, range = 18–38 years) with no history of vestibular disorder, hearing loss, ... Abstract Full text PDF Research Article 08 March 2018 A Pilot Study to Investigate the Relationship Between Interaural Differences in Temporal Bone Anatomy and Normal Variations in Caloric Asymmetry David Carpenter , David Kaylie , Erin Piker , and Dennis Frank-Ito https://doi.org/10.1044/2017_AJA-16-0048 Preview Abstract Purpose This study assesses interaural differences in temporal bone anatomy in subjects with normal caloric findings. Method Eligible patients included those referred to the Duke University Medical Center otology clinic complaining of dizziness, with a head ... Abstract Full text PDF Research Article 08 March 2018 Effects of Bilateral Hearing Aid Use on Balance in Experienced Adult Hearing Aid Users D. Mike McDaniel , Susan D. Motts , and Richard A. Neeley https://doi.org/10.1044/2017_AJA-16-0071 Preview Abstract Purpose The purpose of this study was to evaluate the balance of experienced adult hearing aid users with and without their hearing aids via computerized posturography. Method Computerized posturography was accomplished by employing the Sensory Organization ... Abstract Full text PDF Research Article 08 March 2018 Self-Stigma and Age-Related Hearing Loss: A Qualitative Study of Stigma Formation and Dimensions Dana David , Gil Zoizner , and Perla Werner https://doi.org/10.1044/2017_AJA-17-0050 Preview Abstract Purpose This study explored experiences of self-stigma among older persons with age-related hearing loss (ARHL) using Corrigan's conceptualization of self-stigma process formation and the attribution model as its theoretical framework. Method In-depth ... Abstract Full text PDF Research Article 08 March 2018 Predictive Factors for Vestibular Loss in Children With Hearing Loss Kristen L. Janky , Megan L. A. Thomas , Robin R. High , Kendra K. Schmid , and Oluwaseye Ayoola Ogun https://doi.org/10.1044/2017_AJA-17-0058 Preview Abstract Purpose The aim of this study was to determine if there are factors that can predict whether a child with hearing loss will also have vestibular loss. Method A retrospective chart review was completed on 186 children with hearing loss seen at Boys Town ... Abstract Full text PDF Research Article 08 March 2018 A Cross-Sectional Study on the Hearing Threshold Levels Among People in Qinling, Qinghai, and Nanjing, China Junguo Wang , Xiaoyun Qian , Jie Chen , Ye Yang , and Xia Gao https://doi.org/10.1044/2017_AJA-16-0053 Preview Abstract Purpose This study aimed to investigate the hearing threshold among different age groups, genders, and geographic areas in China to provide some insight into the appropriate clinical interventions for hearing loss. Method Using a systematic random sampling ... Abstract Full text PDF Review Article Review Article 08 March 2018 Structured Review of Dichotic Tests of Binaural Integration: Clinical Performance in Children Kairn Stetler Kelley , and Benjamin Littenberg https://doi.org/10.1044/2017_AJA-17-0032 Preview Abstract Purpose The aim of the study was to evaluate the evidence of clinical utility for dichotic speech tests of binaural integration used to assess auditory processing in English-speaking children 6–14 years old. Method Dichotic speech test recordings and ... Abstract Full text PDF Letters to the Editor Letters to the Editor 08 March 2018 Comment on Hall et al. (2017), “How to Choose Between Measures of Tinnitus Loudness for Clinical Research? A Report on the Reliability and Validity of an Investigator-Administered Test and a Patient-Reported Measure Using Baseline Data Collected in a Phase IIa Drug Trial” Siamak Sabour https://doi.org/10.1044/2017_AJA-17-0086 Preview Abstract Purpose The purpose of this letter, in response to Hall, Mehta, and Fackrell (2017), is to provide important knowledge about methodology and statistical issues in assessing the reliability and validity of an audiologist-administered tinnitus loudness ... Abstract Full text PDF Letters to the Editor 08 March 2018 Author Response to Sabour (2018), “Comment on Hall et al. (2017), ‘How to Choose Between Measures of Tinnitus Loudness for Clinical Research? A Report on the Reliability and Validity of an Investigator-Administered Test and a Patient-Reported Measure Using Baseline Data Collected in a Phase IIa Drug Trial’” Deborah A. Hall , Rajnikant L. Mehta , and Kathryn Fackrell https://doi.org/10.1044/2017_AJA-17-0102 Preview Abstract Purpose The authors respond to a letter to the editor (Sabour, 2018) concerning the interpretation of validity in the context of evaluating treatment-related change in tinnitus loudness over time. Method The authors refer to several landmark methodological ... Abstract Full text PDF Masthead Masthead 08 March 2018 Masthead https://doi.org/10.1044/2018_Mar2018AJAMasthead Full text PDF ";
  private static final String doiFullContentHashFiltered = " Abstract Purpose The purpose of this study is to identify hearing aid owners' and clinicians' opinions of the knowledge, skills, and tasks required for hearing aid management and the importance of each of these to overall success with hearing aids. Method Concept mapping techniques were used to identify key themes, wherein participants generated, sorted, and rated the importance of statements in response to the question “What must hearing aid owners do in order to use, handle, manage, maintain, and care for their hearing aids?” Twenty-four hearing aid owners (56 to 91 years of age; 54.2% men, 45.8% women) and 22 clinicians (32 to 69 years of age; 9.1% men, 90.9% women) participated. Result Participants identified 111 unique items describing hearing aid management within 6 concepts: (a) “Daily Hearing Aid Use,” (b) “Hearing Aid Maintenance and Repairs,” (c) “Learning to Come to Terms with Hearing Aids,” (d) “Communication Strategies,” (e) “Working With Your Clinician,” and (f) “Advanced Hearing Aid Knowledge.” Clinicians' opinions of the importance of each statement varied only slightly from the opinions of the hearing aid owner group. Hearing aid owners indicated that all 6 concepts were of similar importance, whereas clinicians indicated that the concept “Advanced Hearing Aid Knowledge” was significantly less important than the other 5 concepts. Conclusion The results highlight the magnitude of information and skill required to optimally manage hearing aids. Clinical recommendations are made to improve hearing aid handling education and skill acquisition. ";
  private static final String doiAbsContentHashFiltered = " Abstract Purpose The purpose of this study is to identify hearing aid owners' and clinicians' opinions of the knowledge, skills, and tasks required for hearing aid management and the importance of each of these to overall success with hearing aids. Method Concept mapping techniques were used to identify key themes, wherein participants generated, sorted, and rated the importance of statements in response to the question “What must hearing aid owners do in order to use, handle, manage, maintain, and care for their hearing aids?” Twenty-four hearing aid owners (56 to 91 years of age; 54.2% men, 45.8% women) and 22 clinicians (32 to 69 years of age; 9.1% men, 90.9% women) participated. Result Participants identified 111 unique items describing hearing aid management within 6 concepts: (a) “Daily Hearing Aid Use,” (b) “Hearing Aid Maintenance and Repairs,” (c) “Learning to Come to Terms with Hearing Aids,” (d) “Communication Strategies,” (e) “Working With Your Clinician,” and (f) “Advanced Hearing Aid Knowledge.” Clinicians' opinions of the importance of each statement varied only slightly from the opinions of the hearing aid owner group. Hearing aid owners indicated that all 6 concepts were of similar importance, whereas clinicians indicated that the concept “Advanced Hearing Aid Knowledge” was significantly less important than the other 5 concepts. Conclusion The results highlight the magnitude of information and skill required to optimally manage hearing aids. Clinical recommendations are made to improve hearing aid handling education and skill acquisition. ";
  private static final String doiPdfContentHashFiltered = " Abstract Background Fetal alcohol spectrum disorders (FASD) are a highly prevalent spectrum of patterns of congenital defects resulting from prenatal exposure to alcohol. Approximately 90% of the cases involve speech impairment. Yet, to date, no detailed symptom profiles nor dedicated treatment plans are available for this population. Purpose This study set out to chart the speech and speech motor characteristics in boys with FASD to profile the concomitant speech impairment and identify possible underlying mechanisms. Method Ten boys with FASD (4.5–10.3 years old) and 26 typically developing children (4.1–8.7 years old; 14 boys, 12 girls) participated in the study. Speech production and perception, and oral motor data were collected by standardized tests. Results The boys with FASD showed reduced scores on all tasks as well as a deviant pattern of correlations between production and perception tasks and intelligibility compared with the typically developing children. Speech motor profiles showed specific problems with nonword repetition and tongue control. Conclusions Findings indicate that the speech impairment in boys with FASD results from a combination of deficits in multiple subsystems and should be approached as a disorder rather than a developmental delay. The results suggest that reduced speech motor planning/programming, auditory discrimination, and oral motor abilities should be considered in long-term, individually tailored treatment. ";


  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
  }

  private Configuration thisAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "abc");
    conf.put("volume_name", "99");
    return conf;
  }

  private static void doFilterTest(ArchivalUnit au, FilterFactory fact, String nameToHash, String expectedStr) {

    ByteBuffer byteBuffFiltered = java.nio.charset.StandardCharsets.UTF_8.encode(nameToHash);
    ByteBuffer byteBuffExpected = java.nio.charset.StandardCharsets.UTF_8.encode(expectedStr);

    String unicodeFilteredStr = byteBuffExpected.toString();
    String unicodeExpectedStr = byteBuffExpected.toString();

    assertEquals(unicodeFilteredStr, unicodeExpectedStr);
  }

  public void startMockDaemon() {
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();
    startMockDaemon();
    mau = createAu();
  }

  public void testContentLengthComparision() {

    int tocContentLen = tocContent.length();
    int tocContentCrawlFilteredLen = tocContentCrawlFiltered.length();
    int tocContentHashFilteredLen = tocContentHashFiltered.length();

    int doiFullContentLen = doiFullContent.length();
    int doiFullContentCrawlFilteredLen = doiFullContentCrawlFiltered.length();
    int doiFullContentHashFilteredLen = doiFullContentHashFiltered.length();

    int doiAbsContentLen = doiAbsContent.length();
    int doiAbsContentCrawlFilteredLen = doiAbsContentCrawlFiltered.length();
    int doiAbsContentHashFilteredLen = doiAbsContentHashFiltered.length();

    int doiPdfContentLen = doiPdfContent.length();
    int doiPdfContentCrawlFilteredLen = doiPdfContentCrawlFiltered.length();
    int doiPdfContentHashFilteredLen = doiPdfContentHashFiltered.length();


    assertTrue(tocContentLen > 0);
    assertTrue(tocContentCrawlFilteredLen > 0);
    assertTrue(tocContentHashFilteredLen > 0);
    assertTrue(tocContentLen > tocContentCrawlFilteredLen);
    assertTrue(tocContentLen > tocContentHashFilteredLen);

    assertTrue(doiFullContentLen > 0);
    assertTrue(doiFullContentCrawlFilteredLen > 0);
    assertTrue(doiFullContentHashFilteredLen > 0);
    assertTrue(doiFullContentLen > doiFullContentCrawlFilteredLen);
    assertTrue(doiFullContentLen > doiFullContentHashFilteredLen);

    assertTrue(doiAbsContentLen > 0);
    assertTrue(doiAbsContentCrawlFilteredLen > 0);
    assertTrue(doiAbsContentHashFilteredLen > 0);
    assertTrue(doiAbsContentLen > doiAbsContentCrawlFilteredLen);
    assertTrue(doiAbsContentLen > doiAbsContentHashFilteredLen);

    assertTrue(doiPdfContentLen > 0);
    assertTrue(doiPdfContentCrawlFilteredLen > 0);
    assertTrue(doiPdfContentHashFilteredLen > 0);
    assertTrue(doiPdfContentLen > doiPdfContentCrawlFilteredLen);
    assertTrue(doiPdfContentLen > doiPdfContentHashFilteredLen);
  }

  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, manifestContent, manifestContent);
      doFilterTest(mau, variantFact, tocContent, tocContentCrawlFiltered);
      doFilterTest(mau, variantFact, doiFullContent, doiFullContentCrawlFiltered);
      doFilterTest(mau, variantFact, doiAbsContent, doiAbsContentCrawlFiltered);
      doFilterTest(mau, variantFact, doiPdfContent, doiPdfContentCrawlFiltered);
    }
  }

  // Variant to test with Hash Filter
  public static class TestHash extends TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AmericanSpeechLanguageHearingAssocHtmlHashFilterFactory();
      doFilterTest(mau, variantFact, manifestContent, manifestHashFiltered);
      doFilterTest(mau, variantFact, tocContent, tocContentHashFiltered);
      doFilterTest(mau, variantFact, doiFullContent, doiFullContentHashFiltered);
      doFilterTest(mau, variantFact, doiAbsContent, doiAbsContentHashFiltered);
      doFilterTest(mau, variantFact, doiPdfContent, doiPdfContentHashFiltered);
    }
  }

  public static Test suite() {
    return variantSuites(new Class[] {
            TestCrawl.class,
            TestHash.class
    });
  }

}
