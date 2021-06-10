/*  $Id: TestBaseAtyponHtmlHashFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 
 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestTafHtmlHashFilterFactory extends LockssTestCase {
  private TafHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new TafHtmlHashFilterFactory();
  }

  public String getFileAsString(String fName) throws Exception {
    InputStream actIn = fact.createFilteredInputStream(
        mau,
        getResourceAsStream(fName),
        Constants.ENCODING_UTF_8);
    String fStr = StringUtil.fromInputStream(actIn);
    return fStr;
  }

  public String filterString(String string) throws Exception {
    InputStream actIn = fact.createFilteredInputStream(
        mau,
        new StringInputStream(string),
        Constants.DEFAULT_ENCODING);
    String fStr = StringUtil.fromInputStream(actIn);
    return fStr;
  }

  private static final String withPrevNext =
    "    <div class=\"overview borderedmodule-last\">\n" +
    "<div class=\"hd\">\n" +
    "      <h2>\n" +
    "      <a href=\"vol_47\">Volume 47</a>,\n\n" +
    "<span style=\"float: right;margin-right: 5px\">\n" +
    "      \n" +
    "      \n" +
    "<a href=\"/47/2\" title=\"Previous issue\">&lt; Prev</a>\n\n\n" +

    "|\n\n\n" +
    "<a href=\"47/4\" title=\"Next issue\">Next &gt;</a>\n" +
    "</span>\n" +
    "      </h2>\n" +
    "  </div>\n";

  private static final String withoutPrevNext =
    "";//"" Volume 47 ";
  
  private static final String manifest =
    "<!DOCTYPE html>\n"+
    " <html>\n"+
    " <head>\n"+
    "     <title>2012 CLOCKSS Manifest Page</title>\n"+
    "     <meta charset=\"UTF-8\" />\n"+
    " </head>\n"+
    " <body>\n"+
    " <h1>2012 CLOCKSS Manifest Page</h1>\n"+
    " <ul>\n"+
    "     \n"+
    "     <li><a href=\"http://www.online.com/toc/20/17/4\">01 Oct 2012 (Vol. 17 Issue 4 Page 291-368)</a></li>\n"+
    "     \n"+
    "     <li><a href=\"http://www.online.com/toc/20/17/2-3\">01 Jul 2012 (Vol. 17 Issue 2-3 Page 85-290)</a></li>\n"+
    "     \n"+
    "     <li><a href=\"http://www.online.com/toc/20/17/1\">01 Jan 2012 (Vol. 17 Issue 1 Page 1-84)</a></li>\n"+
    "     \n"+
    " </ul>\n"+
    " <p>\n"+
    "     <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>\n"+
    "     CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.\n"+
    " </p>\n"+
    " </body>\n"+
    " </html>\n";
 
  private static final String manifestFiltered =
    "01Oct2012(Vol.17Issue4Page291-368)01Jul2012(Vol.17Issue2-3Page85-290)01Jan2012(Vol.17Issue1Page1-84)";

  private static final String withTocLink =
    "     <div class=\"options\">\n"+
    "   <ul>\n"+
    "       <li class=\"publisherImprint\">\n"+
    "           Route\n"+
    "       </li>\n"+
    "       <li>\n"+
    "           <a href=\"/toc/rmle20/15/4\">\n"+
    "              Sample copy\n"+
    "           </a>\n"+
    "       </li>\n"+
    "       </div> ";
 
  private static final String withoutTocLink =
    "";

   private static final String withPeopleAlsoRead =
    "    <div class=\"overview borderedmodule-last\">Hello World" +
    "      <div class=\"foo\"> Hello Article" +
    //Originally had this line - but this leads to Hello Article getting included twice
    // because both regex"overview" and "tocArticleEntry are in the include filter
    // which needs investigation but that isn't the point of this test...
    //    "      <div class=\"tocArticleEntry\"> Hello Article </div>" +
    "    <div class=\"widget combinedRecommendationsWidget none  widget-none  widget-compact-vertical\" id=\"abcde\"  >" + //147_1
    "      <div class=\"wrapped \" ><h1 class=\"widget-header header-none  header-compact-vertical\">People also read</h1>" + //148_2 wrapped
    "        <div class=\"widget-body body body-none  body-compact-vertical\">" + //149_3 widget-body
    "          <div class=\"relatedArt\">" +       //150_4 related_art
    "            <div class=\"sidebar\">" + //151_5 sidebar
    "              <div class=\"relatedItem\"> " +  //152_6 relatedItem
    "                <div class=\"article-card col-md-1-4\">" + //153_7 article-card
    "                  <div class=\"header\">" + //154_8  header
    "                    <div class=\"art_title  hlFld-Title\">" + //155_9 art_title
    "                      <div class=\"article_type\">Article" + //156_10 article_type tests "_"
    "                      </div><a class=\"ref nowrap\" href=\"/doi/full/10.1080/2049761X.2015.1107307?src=recsys\">Cape Town Convention closing opinions in aircraft finance transactions: custom, standards and practice</a><span class=\"access-icon oa\"></span>" + //156_10 article-type
    "                    </div>" + //155_9 art_title
    "                  </div>" + //154_8  header
    "                  <div class=\"footer\"><a class=\"entryAuthor search-link\" href=\"/author/Durham%2C+Phillip+L\"><span class=\"hlFld-ContribAuthor\">Phillip L Durham</span></a> et al." + //160_11 footer
    "                    <div class=\"card-section\">Cape Town Convention Journal" + //161_12 card-section
    "                    </div>" + //161_12 card-section
    "                    <div class=\"card-section\">" + //163_13 card-section
    "                      <div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online: </strong>4 Nov 2015</span>" + //164_14 tocEPubDate
    "                      </div>" + //164_14 tocEPubDate
    "                    </div>" + //163_13 card-section
    "                  </div><span class=\"access-icon oa\"></span>" + //160_11 footer
    "                </div>" + //153_7 article-card
    "              </div>" + //152_6 relatedItem
    "            </div>" + //151_5 sidebar
    "          </div>" + //150_4 related_art
    "        </div>" + //149_3 widget-body
    "      </div>" + //148_2 wrapped
    "    </div>" + //147_1
    "    </div>" ;
  
  
  private static final String withoutPeopleAlsoRead =  
    "";//"" Hello World Hello Article ";

  private static final String withArticleMetrics = 
    "    <div class=\"overview borderedmodule-last\">Hello Kitty" +
    "    <div class=\"widget literatumArticleMetricsWidget none  widget-none\" id=\"123\"  >" +
    "      <div class=\"section citations\">" +
    "        <div class=\"title\">" +
    "          Citations" +
    "          <span> CrossRef </span> " +
    "          <span class=\"value\">0</span>" +
    "          <span> Scopus </span>" +
    "          <span class=\"value\">6</span>" +
    "        </div>" +
    "      </div>" +
    "    </div>" +
    "    </div>";

  private static final String withoutArticleMetrics =  
    "";//"" Hello Kitty ";
  
  private static final String withTocArticleEntry =
    "<div class=\"tocArticleEntry\"> Hello Article </div>" +
    "<div class=\"tocArticleEntry include-metrics-panel\"> Hello World </div>";
  
  private static final String withoutTocArticleEntry =
    "";//"" Hello Article Hello World ";

  /*
   *  Compare Html and HtmlHashFiltered
   */
  public void testWithPrevNext() throws Exception {
    String filteredwithPrevNext = filterString(withPrevNext);
    assertEquals(withoutPrevNext, filteredwithPrevNext);
  }
  
  public void testManifest() throws Exception {
    String filteredmanifest = filterString(manifest);
    assertEquals(manifestFiltered, filteredmanifest);
  }
  
  public void testWithTocLink() throws Exception {
    String filteredwithTocLink = filterString(withTocLink);
    assertEquals(withoutTocLink, filteredwithTocLink);
  }
  
  public void testPeopleAlsoRead() throws Exception {
    String filteredwithPeopleAlsoRead = filterString(withPeopleAlsoRead);
    //System.out.println("[" + StringUtil.fromInputStream(actIn) + "]");
    assertEquals(withoutPeopleAlsoRead, filteredwithPeopleAlsoRead);
  }
  
  public void testArticleMetrics() throws Exception {
    String filteredwithArticleMetrics = filterString(withArticleMetrics);
    assertEquals(withoutArticleMetrics, filteredwithArticleMetrics);
  }
 
  public void testTocArticleEntry() throws Exception {
    String filteredwithTocArticleEntry = filterString(withTocArticleEntry);
    assertEquals(withoutTocArticleEntry, filteredwithTocArticleEntry);
  }

  public void testIortArticleOldNew() throws Exception {
    // this tests whether an old html document crawl in 2016 will hash compare to the "same" article from 2021
    // the html documents are Open Access
    String art1 = getFileAsString("ARTICLE17453670810016597.2021.html");
    String art2 = getFileAsString("ARTICLE17453670810016597.2016.html");
    String expected = "Patientsatisfactionpainandqualityoflife4monthsafterdisplacedfemoralneckfractures:Acomparisonof663fracturestreatedwithinternalfixationand906withbipolarhemiarthroplastyreportedtotheNorwegianHipFractureRegisterBackgroundPrimaryarthroplastyandinternalfixationarethetwomainoptionsfortreatmentofdisplacedfemoralneckfractures.Despitethefactthattherehavebeenseveralrandomizedstudiestheoptimaltreatmentintheelderlyisstillcontroversial.InthepresentstudybasedondatafromtheNorwegianHipFractureRegisterwecomparedsatisfactionpainandqualityoflife4monthsaftersurgeryinpatientsover70yearsofagewithadisplacedfemoralneckfractureoperatedwithinternalfixationorwithabipolarhemiarthroplasty.PatientsandmethodsDataon1569fracturesinpatientsover70yearsofageoperatedwithinternalfixation(n=663)orhemiarthroplasty(n=906)wereregisteredinthehipfractureregister.Theregisteralsoincludeddataonpatientsatisfactionpainandqualityoflife(EQ-5D)assessed4monthsaftersurgeryusingVASscalesandEQ-5Dhealthquestionnaires.ResultsPatientsoperatedwithhemiarthroplastyhadlesspain(VAS27vs.41)weremoresatisfiedwiththeresultoftheoperation(VAS33vs.48)andhadbetterEQ-5Dindexscore4monthspostoperatively(0.51vs.0.42)thanpatientswhowereoperatedwithinternalfixation.InterpretationOurfindingssuggestthatelderlypatientswithdisplacedfemoralneckfractureshouldbetreatedwitharthroplasty.EveryyearinNorwayapproximately9000patientsarehospitalizedandoperatedonduetohipfractures(DirectorateforHealthandSocialAffairs).Femoralneckfracturesconstitute5360%ofthehipfracturesandtwo-thirdsofthesefracturesaredisplaced(RogmarkThorngrenGjertsen).Whilemostauthorsadvocateosteosynthesisforyoungerpatientsandforthosewithundisplacedfracturesthereisstillcontroversyastohowtotreatdisplacedfemoralneckfracturesinelderlypatients(ChuaBhandariIorio).Thereseemshowevertobeagrowingopinionthattreatmentshouldbebasedonthepatientsagefunctionaldemandsandindividualriskprofile(TidermarkBlomfeldtRogmarkandJohnell).Primaryarthroplastyandinternalfixation(IF)withnailsorscrewsarethetwomainoptionsfortreatmentofdisplacedfemoralneckfractures.Inrecentrandomizedcontrolledtrialstotalhiparthroplasties(THAs)havebeenshowntoprovidesuperiorfunctionaloutcometoIFasassessedbyHarrishipscore(Johansson)andEQ-5D(TidermarkBlomfeldtKeating).Anotherstudyfoundthathemiarthroplasty(HA)providedasuperioroutcomethanIFastreatmentfordisplacedfracturesintheelderly(Rogmark).InelderlypatientswithseverecognitiveimpairmentrandomizedcontrolledstudiesshowedpoorresultsforHAwhencomparedtoIFastreatmentfordisplacedfemoralneckfractures(RavikumarandMarshBlomfeldt).ACochranereviewcomparingIFandarthroplastyfoundnodefinitedifferencesinpainandresidualdisability(ParkerandGurusamy).Ahipfractureisassociatedwithincreasedmortality;halfofthepatientsmaydiewithin5years(OhmanJensenandTondevold).Itisthereforeimportanttoachieveagoodoutcomeassoonaspossible.Thuswebelievethatevaluationofdifferenttreatmentmodalitiesduringthefirstpostoperativemonthsisimportant.WecomparedIFandbipolarHAastreatmentfordislocatedfemoralneckfracturesinpatientsover70yearsofageusingpatientsatisfactionpainandqualityoflife4monthsaftersurgeryasoutcome.TheNorwegianHipFractureRegister(NHFR)startedregistrationofhipfracturesinJanuary2005(Gjertsen)andtheaimofthisnationalprospectivestudyistoimprovethequalityofcare.NationalrecommendationsontreatingdislocatedfemoralneckfractureswithprosthesesexistinNorway(DirectorateforHealthandSocialAffairs);howeverthedecisiononwhethertousescrews/pinsorHAisbasedonthepreferenceofindividualhospitals.FromJanuary2005throughDecember200613104proximalfemurfractureswereregisteredintheNHFR.Ofthese5224patientswereregisteredashavingaprimaryoperationduetoadislocatedfemoralneckfracture.Ourprimaryinclusioncriteriawerepatientsover70yearsoldwhowereoperatedduetoadislocatedfemoralneckfracture(GardenIIIandIV)with2screws/pinsorabipolarHA.4245patientsfulfilledthesecriteria(Figure1).Patientswhodiedduringthefirst4postoperativemonthswereexcluded.Wealsoexcludedpatientswhoemigratedduringthisperiodandpatientswithanunknownaddress(Figure1).Theremaining3317patientsreceivedaquestionnairefromtheregistry4monthsaftersurgery.Noremindersweresenttopatientswhodidnotanswerthequestionnaire.1583patientswhodidnotreturnthequestionnaireand165patientswhosequestionnairewasnotfilledinasatisfactorywaywereexcludedfromfurtheranalysis.Thesetwogroupsofpatientswereolder(meanage82SD6.2)hadhigherASAscores(AmericanSocietyofAnaesthesiologists)andweremoreoftencognitivelyimpaired(32%)thanthepatientswhoreturnedthequestionnaire.Thedifferenceswerestatisticallysignificantforallthreevariables(plt;0.001).Finally1569fracturesoperatedwithIF(n=663)andHA(n=906)remainedforfurtheranalyses.Figure1.Flowchartofthestudy.Figure1.Flowchartofthestudy.Patientandoperativedatawereobtainedfromaformfilledinbythesurgeonimmediatelyaftertheoperation.Todeterminethepresenceofcognitiveimpairmentthesurgeonsifindoubtusedtheclock-drawingtest(Shulman).Bothprimaryoperationsandreoperationswereregisteredatall55hospitalsperforminghipfracturesurgeryinNorway(Gjertsen).Anyreoperationswerelinkedtotheprimaryoperationsusingthepatientsnationalsocialsecuritynumber.ThedefinitionofareoperationwasanyoperationperformedduetocomplicationsaftertheprimaryoperationincludingremovalofosteosynthesismaterialclosedreductionofdislocatedhemiprosthesesrevisiontoanHAoraTHAandsofttissuerevisions.The4-monthsquestionnaireincludedtheNorwegiantranslationoftheEuroQol(EQ-5D)(Brooks).AnEQ-5Dindexscoreof1indicatedthebestpossiblehealthstateandascoreof0indicatedahealthstatesimilartodeath.Somehealthstatesweregivenanegativescorewhichindicatedahealthstateworsethandeath.ThepatientswerealsoaskedtoassesstheirpreoperativeEQ-5D.Furthermorethepatientswereaskedtofillinavisualanalogscale(VAS)concerningaveragepainfromtheoperatedhipduringthepreviousmonth.Avalueof0indicatednopainandavalueof100representedunbearablepain.ThepatientsalsofilledinaVAStodescribehowsatisfiedtheywerewiththeresultoftheoperation.Thevalue0representedverysatisfiedwhilethevalue100representedverydissatisfied.FinallyweusedtheCharnleyclassificationforfunctionalassessment(Charnley).Intheanalysisallpatientsincludedinthestudyremainedinthesamegroup(IForHA)accordingtotheintention-to-treatprinciplewhetherornotareoperationwasperformed.65ofthepatientsintheIFgrouphadalreadybeenreoperatedwithanHAatthetimeofthe4-monthevaluation.Sincethereoperatedpatientscouldnotbeexpectedtodemonstrategoodclinicaloutcome(painsatisfactionandqualityoflife)inaveryshorttimeafterreoperationwealsoperformedadditionalanalyseswithoutthereoperatedpatientsinbothtreatmentgroups.Separateanalysesforpatientswithcognitiveimpairmentandforpatientsindifferentagegroups(7079years8089yearsand9099years)werealsodone.WealsoperformedsubanalysesonpatientsinCharnleyclassAi.e.patientswithinvolvementoftheipsilateralhiponlyandnoinvolvementofotherjointsorsystemicproblemslimitingactivity.RecordswithinformationondatesofdeathandemigrationwereobtainedfromtheNorwegianRegisterofVitalStatistics.TheNorwegianDataInspectorateapprovedtherecordingofdataandallpatientssignedaninformedconsentform.ThePearsonchi-squaretestwasusedforcomparisonofcategoricalvariablesinindependentgroups.Theindependentsamplest-test(Studentst-test)wasusedforparametricscalevariablesinindependentgroups.Alltestsweretwo-sided.Thep-valuesinTable3wereadjustedforpotentialconfounders(agesexcognitiveimpairmentASA-classandpreoperativedelayofsurgery)withgenerallinearmodels(GLMs).Inthefiguresmeanvalueswithstandarderrorofthemeanarepresented.Allresultswereconsideredstatisticallysignificantatthe5%level.TheanalyseswereperformedusingSPSSsoftwareversion13.0.PatientsoperatedwithanHAwereolderweremoreoftenfemaleandhadahigherpreoperativedelaycomparedtopatientsoperatedwithIF.TherewerenostatisticallysignificantdifferencesinthepreoperativeASAscorecognitiveimpairmentandEQ-5Dindexscore(Table1).Table1.BaselinecharacteristicsofpatientsIntheHAgroupuncementedprosthesesaccountedfor22%ofthetotal.Onlycontemporaryuncementedimplantswereused.NoAustinMooreorThompsonprostheseswerereported(Table2).After4months110patientshadbeenreoperated92intheIFgroupand18intheHAgroup.Table2.TypesofimplantsPatientsintheIFgrouphadmorepainthanpatientsintheHAgroup4monthsaftersurgery(plt;0.001).MorepatientsintheHAgroupweresatisfiedwiththeresultoftheoperationthanthoseintheIFgroup(plt;0.001)(Table3A).EvenafterreoperatedpatientshadbeenexcludedpatientsintheIFgrouphadmorepainandwerelesssatisfied4monthsaftersurgerythanpatientsintheHAgroup(plt;0.001)(Table3B).Table3.Painandsatisfactionwiththeresultoftheoperationderived4monthspostoperativelyfromvisualanalogscales(VAS)MostofthepatientswithunbearablepainwerefoundintheIFgroupandmostpatientswithminimalpainwerefoundintheHAgroup(Figure2).MostofthesatisfiedpatientswerefoundintheHAgroupwhilemostofthedissatisfiedpatientswerefoundintheIFgroup(Figure3).Figure2.Thedegreeofpainderivedfromavisualanalogscale(VAS)4monthspostoperatively.Thefigureshowsthedistributionofpainforthe2differenttreatmentgroups.0indicatesnopainand100indicatesunbearablepain.Figure2.Thedegreeofpainderivedfromavisualanalogscale(VAS)4monthspostoperatively.Thefigureshowsthedistributionofpainforthe2differenttreatmentgroups.0indicatesnopainand100indicatesunbearablepain.Figure3.Thedegreeofsatisfactionwiththeresultoftheoperationderivedfromavisualanalogscale(VAS)4monthspostoperatively.Thefigureshowsthedistributionofpatientsatisfactionforthe2differenttreatmentgroups.0indicatesverysatisfiedand100indicatesverydissatisfied.Figure3.Thedegreeofsatisfactionwiththeresultoftheoperationderivedfromavisualanalogscale(VAS)4monthspostoperatively.Thefigureshowsthedistributionofpatientsatisfactionforthe2differenttreatmentgroups.0indicatesverysatisfiedand100indicatesverydissatisfied.Only625IFpatientsand862HApatientshadfilledinboththepreoperativeEQ-5Dandthe4-monthEQ-5Dquestionnairecorrectly.ThepreoperativeEQ-5DindexscoreswereequalintheIFandtheHAgroups:0.68and0.69respectively(Table1).4monthspostoperativelyaninferiorEQ-5DindexscorewasfoundfortheIFgroup(0.42)comparedtotheHAgroup(0.51)(plt;0.001).ThedeclineinEQ-5Dindexscorewas0.26fortheIFgroupand0.19fortheHAgroup(plt;0.001)(Figure4).WhenseparateanalyseswereperformedexcludingallreoperatedpatientsinbothtreatmentgroupstheEQ-5Dindexscorewas0.43fortheIFgroup(n=488)and0.51fortheHAgroup(n=843)(plt;0.001).Figure4.Health-relatedqualityoflife(EQ-5Dindexscore)forpatientsat0and4months.0indicatestheworstpossiblehealthstateand1.0indicatesfullhealth.Thep-valuesaregivenfordifferencesbetweenthetreatmentgroups(generallinearmodel).Figure4.Health-relatedqualityoflife(EQ-5Dindexscore)forpatientsat0and4months.0indicatestheworstpossiblehealthstateand1.0indicatesfullhealth.Thep-valuesaregivenfordifferencesbetweenthetreatmentgroups(generallinearmodel).Preoperativelynodifferencesbetweenthetwogroupsinanyofthe5dimensionsoftheEQ-5Dcouldbedetected(Table4).4monthsaftersurgerytheHAgroupwasmoremobilethantheIFgroup(plt;0.001).Moreovertheyhadlessproblemswithself-care(p=0.001)andinperformingtheirusualactivities(plt;0.001)thantheIFgroup.FinallytheHAgrouphadlesspainordiscomfortthanthepatientsoperatedwithIF(plt;0.001)(Table4).Nodifferenceinanxiety/depressionwasfoundbetweenthetwogroups.Table4.Qualityoflife(EQ-5D)forpatientsoperatedwithinternalfixationorbipolarhemiarthroplastySeparateanalysesonpatientssufferingfromdementiapatientsindifferentagegroupsandpatientswhohadbeenwalkingwithoutproblemspriortothefractureshowedpracticallythesamedifferencesregardingpainsatisfactionandEQ-5Dindexscore.AlsoseparateanalysesonpatientsinCharnleyclassAshowedsimilardifferencesregardingtheseoutcomes.FinallytherewerenostatisticallysignificantdifferencesinpainsatisfactionandEQ-5Dindexscorebetweenuncementedandcementedhemiprostheses.Wefoundthatpatientsoperatedwithabipolarhemiarthroplastyduetoadislocatedfemoralneckfracturehadlesspainweremoresatisfiedwiththeresultoftheoperationandhadabetterqualityoflife4monthspostoperativelythanpatientsoperatedwithinternalfixation.WefoundamarkedreductioninEQ-5Dindexscorepostoperativelyinbothtreatmentgroups.ThepatientstreatedwithabipolarHAdidhoweverhaveabetterEQ-5Dindexscoreat4monthsthantheIFgroup.Tidermark()foundareductioninEQ-5Dindexscoresat412and24monthsinpatientswithdisplacedfemoralneckfracturestreatedwithIFevenwhenthefracturehadhealeduneventfully.InelderlypatientswithseverecognitiveimpairmentBlomfeldt()foundalowerqualityoflifeforuncementedHAaccordingtotheEQ-5Dat2-yearfollow-upcomparedtoIF.WefoundthatHAwasalsosuperiortoIFforthepatientswithcognitiveimpairment.Onereasonforthisdifferenceinresultsbetweenstudiescouldbethatdifferentimplantswereused.WhileBlomfeldtusedtheunipolarAustinMooreuncoateduncementedhemiprosthesiswhichisdocumentedtobeinferior(AustralianOrthopaedicAssociation)mostoftheprosthesesusedinourstudywerecementedandtheuncementedprosthesesusedwereallmodernhydroxyapatite-coatedimplants.TheresultsofcementedHAshavebeenreportedtobebetterthantheresultsofuncementeduncoatedHAsconcerningpainwalkingabilityuseofwalkaidsandADL(Khan).Keating()foundthattherewerenostatisticallysignificantdifferencesbetweenIFandbipolarHAwhentheEQ-5Dwasused412and24monthspostoperatively.Ourstudyhadmorepatientshoweverandthereforehigherpower.WefoundagoodcorrelationbetweentheEQ-5Dindexscoresandtheotheroutcomevariablesat4months;i.e.patientsreportedsimilarpainandsatisfactionscores.ThisisinaccordancewithanearlierstudythatshowedagoodagreementbetweentheEQ-5DindexscoresandotheroutcomevariablessuchaspainmobilityindependenceinADLandindependentlivingstatus(Tidermark).PatientstreatedwithanIFhadmorepain4monthsaftersurgerythanpatientstreatedwithaprimaryHA(VASscores:41and27respectively).ThisisinaccordancewithonestudyfromSweden(Rogmark).OtherstudieshavehoweverreportednostatisticallysignificantdifferenceinpainbetweenIFandHA(ParkerandPryorKeating).InthestudybyParkerandPryoruncementeduncoatedAustinMoorehemiprostheseswereused.Resultsfromobservationalregister-basedstudies(cohortstudies)arelessconclusivethanthosefromrandomizedclinicaltrials.Ifpotentialconfoundersarecontrolledforhoweverobservationalstudiesmaygiveresultsthataresimilartothoseofcontrolledrandomizedtrials(BensonandHartz).Onlyknownandmeasuredconfounderscanofcoursebeadjustedforinobservationalstudieswhereasrandomizedstudiestakeaccountofallconfoundersbothknownandunknown.Ontheotherhandobservationalstudieshaveseveraladvantagesovercontrolledrandomizedstudiesincludinglowercostgreatertimelinessandawiderrangeofpatients.Ourstudyrepresentstheresultsfromthewholecountryandoftheaveragesurgeonandnotonlytheresultsfromonespecializedclinicasinmanyrandomizedstudies.Consideringthehighageandconsiderablecomorbidityofthepatientsthe60%responsetothepatientquestionnairewasasexpectedbutahighercompliancewouldhavestrengthenedourresults.ThepatientswhodidnotreturnthequestionnaireweregenerallyoldermorecognitivelyimpairedandhadahigherASAclassthanthepatientswhoresponded.SincewehadnoEQ-5Dscoresforthepatientswhofailedtorespondwecanofcoursenotbesureofanydifferencesinqualityoflifeinthetwogroups.HoweverpreoperativeagecognitiveimpairmentandASAclassweresimilarforthenon-respondersinthe2treatmentgroups.Consequentlythecomparisonofthetreatmentgroupswasreliable.Therelativelyhighnumberofpatientslosttofollow-upmayalsoreflectthefactthatmanyofthesefrailpatientsaretransferredtonursinghomeswhendischargedfromhospital;thustheycannotbecontactedattheirpermanentaddress.Insummary4monthsaftersurgeryabipolarhemiarthroplastyshowedgoodresultsbetterthanthoseafterscreworpinfixationindislocatedfemoralneckfracturesinpatientsover70yearsofage.Alongerfollow-upwillbenecessarytodeterminewhetherthesuperioroutcomesofhemiarthroplastypersistinthelongterm.TheauthorsthankalltheNorwegianorthopedicsurgeonswhohaveloyallyreportedtotheregister.TheNorwegianHipFractureRegisterisfundedbytheRegionalHealthBoardofHelse-VestRHF.Nocompetinginterestsdeclared.ThisstudyrepresentscloseteamworkbytheorthopedicsurgeonsJEGTVLBELIHOFandJMFandstatisticianSAL.Allauthorsparticipatedintheinterpretationoftheresultsandinpreparationofthemanuscript.JEGSALandJMFperformedthestatisticalanalyses.JEGwasmainlyresponsibleforwritingthemanuscript.";
    //log.info(StringUtils.difference(art1,art2));
    assertEquals(art1, art2);

  }

  public void testTaclTocOldNew() throws Exception {
    // this tests whether an old html document crawl in 2016 will hash compare to the "same" TOC from 2021
    // the html documents are Open Access
    String art1 = getFileAsString("TOCtacl20384.2019.html");
    String art2 = getFileAsString("TOCtacl20384.2016.html");
    String expected = "EditorialStephenMcLoughlinSerendipaceratopsarthurcclarkeiRichVickers-Rich2003isanAustralianEarlyCretaceousceratopsianThomasH.RichBenjaminP.KearRobertSinclairBrendaChinneryKennethCarpenterMaryL.McHughPatriciaVickers-RichAnewChanghsingian(LatePermian)brachiopodfaunafromtheZhongzhaisection(SouthChina)Part2:LingulidaOrthidaOrthotetidaandSpiriferidaYangZhangG.R.ShiWei-HongHeKe-XinZhangHui-TingWuAnewrecordofanaristonectineelasmosaurid(SauropterygiaPlesiosauria)fromtheUpperCretaceousofNewZealand:implicationsfortheMauisaurushaastiHector1874hypodigmJosP.OgormanRodrigoA.OteroNortonHillerNewmaterialreferabletoWakaleo(Marsupialia:Thylacoleonidae)fromtheRiversleighWorldHeritageAreanorthwesternQueensland:revisingspeciesboundariesanddistributionsinOligo/MiocenemarsupiallionsAnnaK.GillespieMichaelArcherSuzanneJ.HandKarenH.BlackATremadocianasterozoanfromTasmaniaandalateLlandoveryedrioasteroidfromVictoriaPeterA.JellProbableoribatidmite(Acari:Oribatida)tunnelsandfaecalpelletsinsilicifiedconiferwoodfromtheUpperCretaceous(CenomanianTuronian)portionoftheWintonFormationcentral-westernQueenslandAustraliaTamaraL.FletcherStevenW.SalisburyFirstrecordsofWuchiapingian(LatePermian)conodontsintheXainzaareaLhasaBlockTibetandtheirpalaeobiogeographicimplicationsDong-XunYuanYi-ChunZhangYu-JieZhangTong-XingZhuShu-ZhongShenAnewgenusandspeciesofSteninaefromthelateEoceneofFrance(ColeopteraStaphylinidae)ChenyangCaiDaveJ.ClarkeDiyingHuangAndrNelThefirstfossilavianeggfromBrazilJlioCesar.deA.MarsolaGeraldGrellet-TinnerFelipeC.MontefeltroJulianaM.SayoAnnieSchmaltzHsiouMaxC.LangerAnearlyMioceneant(subfam.Amblyoponinae)fromFouldenMaar:thefirstfossilHymenopterafromNewZealandUweKaulfussAnthonyC.HarrisJohnG.ConranDaphneE.LeeLowerOrdoviciantrilobitesfromtheSeptembersformationNorth-EastGreenlandLucyM.E.McCobbW.DouglasBoyceIanKnightSvendStougeEditorialBoard";
    assertEquals(art1, art2);
  }
}
