/**
 * $Id:
 */
package org.lockss.plugin.atypon;

import java.util.List;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponSubstancePredicateFactory.BaseAtyponSubstancePredicate;


public class TestBaseAtyponSubstancePredicate extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;
  private BaseAtyponSubstancePredicateFactory sfact;
  private BaseAtyponSubstancePredicate subP;

  private final String doi_prefix = "11.1111";
  private final String url_abs = "http://www.baseatypon.com/doi/abs/" + doi_prefix + "/123abc";
  private final String url_abs2 = "http://www.baseatypon.com/doi/abs/" + doi_prefix + "/456abc";
  private final String url_pdf = "http://www.baseatypon.com/doi/pdf/" + doi_prefix + "/123abc";
  private final String url_full = "http://www.baseatypon.com/doi/full/" + doi_prefix + "/123abc";
  private final String url_pdfplus = "http://www.baseatypon.com/doi/pdfplus/" + doi_prefix + "/123abc";

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.baseatypon.com/";

  protected static Logger logger = Logger.getLogger("SimulatedContentGenerator");

  private CIProperties htmlHeader;    
  private CIProperties pdfHeader;    
  private final String goodHtmlContent = "<html><body><h1>It works!</h1></body></html>";
  private final String goodPdfContent = "Foo"; // doesn't really matter


  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau = new MockArchivalUnit();
    mau.setConfiguration(auConfig());

    // This is the same as the BaseAtypon plugin's substance pattern
    List subPat = ListUtil.list("doi/(pdf|pdfplus|full)/[.0-9]+/[^\\?^\\&]+$");
    // set the substance check pattern on the mock AU
    mau.setSubstanceUrlPatterns(compileRegexps(subPat));   
    sfact = new BaseAtyponSubstancePredicateFactory();
    subP = sfact.makeSubstancePredicate(mau);

    // set up headers to use in the tests
    htmlHeader = new CIProperties();    
    htmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    pdfHeader = new CIProperties();    
    pdfHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);


  }   

  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "xxxx");
    conf.put("volume_name","123");
    return conf;
  }

  public void tearDown() throws Exception {
    getMockLockssDaemon().stopDaemon();
    super.tearDown();
  }

  List<Pattern> compileRegexps(List<String> regexps)
      throws MalformedPatternException {
    return RegexpUtil.compileRegexps(regexps);
  }
  
  
  // a full html version of the article 
  public void testSubstantiveFullHtml() throws Exception {

    MockCachedUrl cu = mau.addUrl(url_full, true, true, htmlHeader);
    mau.addUrl(url_full, true, true, htmlHeader);
    //MockCachedUrl cu = new MockCachedUrl(url_full, au);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);

    // for atypon substance must be full, pdf or pdfplus AND the mime-type
    // must match the type of file 
    assertTrue(subP.isSubstanceUrl(url_full));
  }


  // With redirection, a full url could actually be the pdf version 
  // while this is still substantive, we want to know about it and emit
  // a warning
  public void testRedirectedFullHtml() throws Exception {
    // make it a pdf even though it should be full
    MockCachedUrl cu = mau.addUrl(url_full, true, true, pdfHeader);
    mau.addUrl(url_full, true, true, pdfHeader);
    //MockCachedUrl cu = new MockCachedUrl(url_full, au);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);

    // for atypon substance must be full, pdf or pdfplus AND the mime-type
    // must match the type of file 
    assertFalse(subP.isSubstanceUrl(url_full));
  }

  // a full pdf or pdfplus version of the article
  public void testSubstantivePdfFiles() throws Exception {

    MockCachedUrl cu = mau.addUrl(url_pdf, true, true, pdfHeader);
    mau.addUrl(url_pdf, true, true, pdfHeader);
    cu.setContent(goodPdfContent);
    cu.setContentSize(goodPdfContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);

    MockCachedUrl cu2 = mau.addUrl(url_pdfplus, true, true, pdfHeader);
    mau.addUrl(url_pdfplus, true, true, pdfHeader);
    cu2.setContent(goodPdfContent);
    cu2.setContentSize(goodPdfContent.length());
    cu2.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);

    // for atypon substance must be full, pdf or pdfplus AND the mime-type
    // must match the type of file 
    assertTrue(subP.isSubstanceUrl(url_pdf));
    assertTrue(subP.isSubstanceUrl(url_pdfplus));
  }


  // with redirection, a pdf url could actually point to a full html version
  // while this would still be substantive, we want know about it and 
  // emit a warning
  public void testRedirectedPdfFiles() throws Exception {

    MockCachedUrl cu = mau.addUrl(url_pdf, true, true, htmlHeader);
    mau.addUrl(url_pdf, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    
    MockCachedUrl cu2 = mau.addUrl(url_pdfplus, true, true, htmlHeader);
    mau.addUrl(url_pdfplus, true, true, htmlHeader);
    cu2.setContent(goodHtmlContent);
    cu2.setContentSize(goodHtmlContent.length());
    cu2.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);

    // for atypon substance must be full, pdf or pdfplus AND the mime-type
    // must match the type of file 
    assertFalse(subP.isSubstanceUrl(url_pdf));
    assertFalse(subP.isSubstanceUrl(url_pdfplus));
  }


  // Abstracts are a little different. They are technically not substance
  // but we are using the substancepredicate to check whether they have
  // been redirected. So they will never return "isSubstanceUrl" but
  // a warning might have been emitted
  public void testAbstractFiles() throws Exception {

    MockCachedUrl cu = mau.addUrl(url_abs, true, true, htmlHeader);
    mau.addUrl(url_abs, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);

    // a mismatched url and mime-type
    MockCachedUrl cu2 = mau.addUrl(url_abs2, true, true, pdfHeader);
    mau.addUrl(url_abs2, true, true, pdfHeader);
    cu2.setContent(goodHtmlContent);
    cu2.setContentSize(goodHtmlContent.length());
    cu2.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);

    // for atypon substance must be full, pdf or pdfplus AND the mime-type
    // even though this url matches its mime type
    assertFalse(subP.isSubstanceUrl(url_abs));
    assertFalse(subP.isSubstanceUrl(url_abs2));
  }



}

