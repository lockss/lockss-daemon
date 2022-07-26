package org.lockss.plugin.clockss.scienceopen;

import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class TestScienceOpenBookXMLMetadataExtractor extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestScienceOpenBookXMLMetadataExtractor.class);

  private static String BASE_URL = "https://clockss-test.lockss.org/sourcefiles/scienceopenbooks-released/";
  private static String DIRECTORY = "2021_01";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();

  private MockLockssDaemon theDaemon;
  private ArchivalUnit bau;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    ConfigurationUtil.addFromUrl(getResource("test_scienceopenbooks.xml"));
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();

    TdbAu tdbau1 = tdb.getTdbAusLikeName( "").get(0);
    assertNotNull("Didn't find named TdbAu",tdbau1);
    bau = PluginTestUtil.createAndStartAu(tdbau1);
    assertNotNull(bau);
    TypedEntryMap auConfig =  bau.getProperties();
    assertEquals(BASE_URL, auConfig.getString(BASE_URL_KEY));
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  private List<ArticleMetadata> setupContentForAU(ArchivalUnit au,
                                                  String fname
  ) throws IOException, PluginException {
    String content = getResourceContent(fname);
    return setupContentForAU(au, fname, content);
  }

  private List<ArticleMetadata> setupContentForAU(ArchivalUnit au,
                                                  String fname,
                                                  String content
  ) throws IOException, PluginException {
    String url =  BASE_URL + DIRECTORY + "/" + fname;
    InputStream input = IOUtils.toInputStream(content, "utf-8");
    CIProperties xmlHead = new CIProperties();
    xmlHead.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    FileMetadataExtractor me = new ScienceOpenBookXmlMetadataExtractorFactory.ScienceOpenBookXmlMetadataExtractor();
    UrlData ud = new UrlData(input, xmlHead, url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
    CachedUrl cu = uc.getCachedUrl();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    return mle.extract(MetadataTarget.Any(), cu);
  }

  public void testExtractXmlSchema() throws Exception {
    List<ArticleMetadata> mdlist = setupContentForAU(bau, "sample_book.xml");
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    ArrayList authors = new ArrayList<>();
    authors.add("Cloete, Nico");
    authors.add("Mouton, Johann");
    authors.add("Sheppard, Charles");

    assertEquals("10.47622/9781928331001", md.get(MetadataField.FIELD_DOI));
    assertEquals("2016-1-16", md.get(MetadataField.FIELD_DATE));
    assertEquals("book_volume", md.get(MetadataField.FIELD_ARTICLE_TYPE));
    assertEquals("296", md.get(MetadataField.FIELD_END_PAGE));
    assertEquals("978-1-928331-00-1", md.get(MetadataField.FIELD_ISBN));
    assertEquals("Doctoral Education in South Africa", md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals("African Minds", md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals("ScienceOpen", md.get(MetadataField.FIELD_PROVIDER));
    assertEquals(authors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals("\n            Worldwide, in Africa and in South Africa, the importance of the doctorate has increased disproportionately in relation to its share of the overall graduate output over the past decade. This heightened attention has not only been concerned with the traditional role of the PhD, namely the provision of future academics; rather, it has focused on the increasingly important role that higher education - and, particularly, high-level skills - is perceived to play in national development and the knowledge economy.\n            " +
            "This book is unique in the area of research into doctoral studies because it draws on a large number of studies conducted by the Centre of Higher Education Trust (CHET) and the Centre for Research on Evaluation, Science and Technology (CREST), as well as on studies from the rest of Africa and the world. In addition to the historical studies, new quantitative and qualitative research was undertaken to produce the evidence base for the analyses presented in the book.The findings presented in Doctoral Education in South Africa pose anew at least six tough policy questions that the country has struggled with since 1994, and continues to struggle with, if it wishes to gear up the system to meet the target of 5 000 new doctorates a year by 2030. Discourses framed around the single imperatives of growth, efficiency, transformation or quality will not, however, generate the kind of policy discourses required to resolve these tough policy questions effectively. What is needed is a change in approach that accommodates multiple imperatives and allows for these to be addressed simultaneously.\n" + "        ",
        md.get(MetadataField.FIELD_ABSTRACT)
    );
  }

  public void testDoNotExtractXmlSchema() throws Exception {
    List<ArticleMetadata> mdlist = setupContentForAU(bau,"sample_book_chapter.xml");
    assertEmpty(mdlist);
  }

  public void testNormalizePub() throws Exception {
    String book_meta =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE article PUBLIC \"-//NLM//DTD BITS Book Interchange DTD v2.0 20151225//EN\" \"BITS-book2.dtd\">" +
      "<book xmlns:ns4=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.w3.org/2001/XInclude\">" +
        "<book_meta>" +
          "<publisher>" +
            "<publisher-name>Science   Open</publisher-name>" +
          "</publisher>" +
        "</book_meta>" +
      "</book>" ;

    List<ArticleMetadata> mdlist = setupContentForAU(bau,"sample_pub_snippet.xml", book_meta);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    log.info(md.toString());
    assertEquals("ScienceOpen", md.get(MetadataField.FIELD_PUBLISHER));
  }

}

