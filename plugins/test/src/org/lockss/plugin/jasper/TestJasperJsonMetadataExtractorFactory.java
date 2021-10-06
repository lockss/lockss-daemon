package org.lockss.plugin.jasper;

import org.lockss.config.ConfigManager;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.CIProperties;
import org.lockss.util.TypedEntryMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

import org.apache.commons.io.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class TestJasperJsonMetadataExtractorFactory extends LockssTestCase {
  private JasperJsonMetadataExtractorFactory fact;
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.jasper.ClockssJasperPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static String BASE_URL = "https://warcs.archive-it.org/";

  static String metadataUrl = BASE_URL + "2051-5960/00003741594643f4996e2555a01e03c7/data/metadata/metadata.json";

  public void setUp() throws Exception {
    super.setUp();
    fact = new JasperJsonMetadataExtractorFactory();
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    ConfigurationUtil.addFromUrl(getResource("test_clockssjasper.xml"));
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

  String jsonRecord =
    "{\n" +
        "  \"last_updated\": \"2020-11-24T21:55:20Z\",\n" +
        "  \"created_date\": \"2018-11-04T12:37:46Z\",\n" +
        "  \"id\": \"00003741594643f4996e2555a01e03c7\",\n" +
        "  \"bibjson\": {\n" +
        "    \"title\": \"Variation in TMEM106B in chronic traumatic encephalopathy\",\n" +
        "    \"year\": \"2018\",\n" +
        "    \"month\": \"11\",\n" +
        "    \"start_page\": \"1\",\n" +
        "    \"end_page\": \"9\",\n" +
        "    \"abstract\": \"Abstract The genetic basis of chronic traumatic encephalopathy (CTE) is poorly understood. Variation in transmembrane protein 106B (TMEM106B) has been associated with enhanced neuroinflammation during aging and with TDP-43-related neurodegenerative disease, and rs3173615, a missense coding SNP in TMEM106B, has been implicated as a functional variant in these processes. Neuroinflammation and TDP-43 pathology are prominent features in CTE. The purpose of this study was to determine whether genetic variation in TMEM106B is associated with CTE risk, pathological features, and ante-mortem dementia. Eighty-six deceased male athletes with a history of participation in American football, informant-reported Caucasian, and a positive postmortem diagnosis of CTE without comorbid neurodegenerative disease were genotyped for rs3173615. The minor allele frequency (MAF = 0.42) in participants with CTE did not differ from previously reported neurologically normal controls (MAF = 0.43). However, in a case-only analysis among CTE cases, the minor allele was associated with reduced phosphorylated tau (ptau) pathology in the dorsolateral frontal cortex (DLFC) (AT8 density, odds ratio [OR] of increasing one quartile = 0.42, 95% confidence interval [CI] 0.22–0.79, p = 0.008), reduced neuroinflammation in the DLFC (CD68 density, OR of increasing one quartile = 0.53, 95% CI 0.29–0.98, p = 0.043), and increased synaptic protein density (β = 0.306, 95% CI 0.065–0.546, p = 0.014). Among CTE cases, TMEM106B minor allele was also associated with reduced ante-mortem dementia (OR = 0.40, 95% CI 0.16–0.99, p = 0.048), but was not associated with TDP-43 pathology. All case-only models were adjusted for age at death and duration of football play. Taken together, variation in TMEM106B may have a protective effect on CTE-related outcomes.\",\n" +
        "    \"journal\": {\n" +
        "      \"volume\": \"6\",\n" +
        "      \"number\": \"1\",\n" +
        "      \"publisher\": \"BMC\",\n" +
        "      \"title\": \"Acta Neuropathologica Communications\",\n" +
        "      \"country\": \"GB\",\n" +
        "      \"license\": [\n" +
        "        {\n" +
        "          \"title\": \"CC BY\",\n" +
        "          \"type\": \"CC BY\",\n" +
        "          \"url\": \"https://actaneurocomms.biomedcentral.com/submission-guidelines/copyright\",\n" +
        "          \"open_access\": true\n" +
        "        }\n" +
        "      ],\n" +
        "      \"language\": [\n" +
        "        \"EN\"\n" +
        "      ],\n" +
        "      \"issns\": [\n" +
        "        \"2051-5960\"\n" +
        "      ]\n" +
        "    },\n" +
        "    \"identifier\": [\n" +
        "      {\n" +
        "        \"type\": \"doi\",\n" +
        "        \"id\": \"10.1186/s40478-018-0619-9\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"type\": \"eissn\",\n" +
        "        \"id\": \"2051-5960\"\n" +
        "      }\n" +
        "    ],\n" +
        "    \"keywords\": [\n" +
        "      \"Chronic traumatic encephalopathy\",\n" +
        "      \"TMEM106B\",\n" +
        "      \"Neuroinflammation\",\n" +
        "      \"Football\",\n" +
        "      \"Traumatic brain injury\",\n" +
        "      \"Tau\"\n" +
        "    ],\n" +
        "    \"link\": [\n" +
        "      {\n" +
        "        \"type\": \"fulltext\",\n" +
        "        \"url\": \"http://link.springer.com/article/10.1186/s40478-018-0619-9\",\n" +
        "        \"content_type\": \"HTML\"\n" +
        "      }\n" +
        "    ],\n" +
        "    \"subject\": [\n" +
        "      {\n" +
        "        \"scheme\": \"LCC\",\n" +
        "        \"term\": \"Neurology. Diseases of the nervous system\",\n" +
        "        \"code\": \"RC346-429\"\n" +
        "      }\n" +
        "    ],\n" +
        "    \"author\": [\n" +
        "      {\n" +
        "        \"name\": \"Jonathan D. Cherry\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Jesse Mez\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"John F. Crary\",\n" +
        "        \"affiliation\": \"Department of Pathology, Fishberg Department of Neuroscience, Friedman Brain Institute, Ronald M. Loeb Center for Alzheimer’s Disease, Icahn School of Medicine at Mount Sinai School\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Yorghos Tripodis\",\n" +
        "        \"affiliation\": \"Department of Biostatistics, Boston University School of Public Health\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Victor E. Alvarez\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Ian Mahar\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Bertrand R. Huber\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Michael L. Alosco\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Raymond Nicks\",\n" +
        "        \"affiliation\": \"Department of Veterans Affairs Medical Center\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Bobak Abdolmohammadi\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Patrick T. Kiernan\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Laney Evers\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Sarah Svirsky\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Katharine Babcock\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Hannah M. Gardner\",\n" +
        "        \"affiliation\": \"VA Boston Healthcare System\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Gaoyuan Meng\",\n" +
        "        \"affiliation\": \"VA Boston Healthcare System\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Christopher J. Nowinski\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Brett M. Martin\",\n" +
        "        \"affiliation\": \"Department of Biostatistics, Boston University School of Public Health\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Brigid Dwyer\",\n" +
        "        \"affiliation\": \"Department of Neurology, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Neil W. Kowall\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Robert C. Cantu\",\n" +
        "        \"affiliation\": \"Department of Anatomy and Neurobiology, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Lee E. Goldstein\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Douglas I. Katz\",\n" +
        "        \"affiliation\": \"Department of Neurology, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Robert A. Stern\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Lindsay A. Farrer\",\n" +
        "        \"affiliation\": \"Department of Neurology, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Ann C. McKee\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Thor D. Stein\",\n" +
        "        \"affiliation\": \"Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine\"\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
        "}";

  static String goodAuthors = "[Jonathan D. Cherry, Jesse Me, John F. Crar, Yorghos Tripodi, Victor E. Alvare, Ian Maha, Bertrand R. Hube, Michael L. Alosc, Raymond Nick, Bobak Abdolmohammad, Patrick T. Kierna, Laney Ever, Sarah Svirsk, Katharine Babcoc, Hannah M. Gardne, Gaoyuan Men, Christopher J. Nowinsk, Brett M. Marti, Brigid Dwye, Neil W. Kowal, Robert C. Cant, Lee E. Goldstei, Douglas I. Kat, Robert A. Ster, Lindsay A. Farre, Ann C. McKe, Thor D. Stein]";
  static String goodTitle = "Variation in TMEM106B in chronic traumatic encephalopathy";
  static String goodDate = "2018-11-04T12:37:46Z";
  static String goodJournal = "Acta Neuropathologica Communications";
  static String goodPublisher = "BMC";

  public void testExtractAlternateRisContent() throws Exception {

    List<ArticleMetadata> mdlist = setupContentForAU(bau, metadataUrl, jsonRecord, false);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodAuthors, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }

  /* private support methods */
  private List<ArticleMetadata> setupContentForAU(ArchivalUnit au, String url,
                                                  String content,
                                                  boolean isHtmlExtractor) throws IOException, PluginException {
    FileMetadataExtractor me;

    InputStream input = null;
    CIProperties props = null;
    input = IOUtils.toInputStream(content, "utf-8");
    props = getContentProperties();
    me = new JasperJsonMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/html");
    UrlData ud = new UrlData(input, props, url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
    CachedUrl cu = uc.getCachedUrl();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    return mle.extract(MetadataTarget.Any(), cu);
  }
  private CIProperties getContentProperties() {
    CIProperties cProps = new CIProperties();
    // the CU checks the X-Lockss-content-type, not the content-type to determine encoding
    cProps.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=UTF-8");
    cProps.put("Content-type",  "text/html; charset=UTF-8");
    return cProps;
  }

  public void testJsonMap() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(jsonRecord);
    Map<String, String> map = new HashMap<>();
    JasperJsonMetadataExtractorFactory.JasperJsonMetadataExtractor.processKeys("", root, map, new ArrayList<>());

    map.entrySet()
        .forEach(System.out::println);
  }

/*
  public void testJsonExtraction() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = null;
    rootNode = objectMapper.readTree(jsonRecord);

    String updated = rootNode.get("last_updated").asText(null);
    String created = rootNode.get("created_date").asText(null);
    String id = rootNode.get("id").asText(null);
    log.info(updated);
    log.info(created);
    log.info(id);

    JsonNode bibNode = rootNode.get("bibjson");
    if (!bibNode.isMissingNode()) {
      String title = bibNode.get("title").asText(null);
      int year = bibNode.get("year").asInt(-1);
      int month = bibNode.get("month").asInt(-1);
      int start_page = bibNode.get("start_page").asInt(-1);
      int end_page = bibNode.get("end_page").asInt(-1);
      String abs = bibNode.get("abstract").asText(null);
      log.info(" " +title );
      log.info(" " +year );
      log.info(" " +month );
      log.info(" " +start_page );
      log.info(" " +end_page );
      log.info(" " +abs );
      JsonNode journalNode = bibNode.get("journal");
      if (!journalNode.isMissingNode()) {
        int volume = journalNode.get("volume").asInt(-1);
        int number = journalNode.get("number").asInt(-1);
        String publisherID = journalNode.get("publisher").asText(null);
        String journalTitle = journalNode.get("title").asText(null);
        String country = journalNode.get("country").asText(null);
        String langCode = journalNode.get("language").get(0).asText(null);
        String issnNode = journalNode.get("issns").get(0).asText(null);
        log.info(" " +volume );
        log.info(" " +number );
        log.info(" " +publisherID );
        log.info(" " +journalTitle );
        log.info(" " +country );
        log.info(" " +langCode );
        log.info(" " +issnNode );
      }
      JsonNode identifierNode = bibNode.get("identifier");
      if (!identifierNode.isMissingNode() && identifierNode.isArray()) {
        Iterator<JsonNode> identifierIter = identifierNode.elements();
        for (int i = 1; identifierIter.hasNext(); ++i) {
          JsonNode ident = identifierIter.next();
          String type = ident.get("type").asText(null);
          if (type.equals("doi")) {
            String doi = ident.get("id").asText(null);
            log.info(" " +doi );
          } else if (type.equals("eissn")) {
            String eissn = ident.get("id").asText(null);
            log.info(" " +eissn );
          }
        }
      }
      JsonNode keywordsNode = bibNode.get("keywords");
      if (!keywordsNode.isMissingNode() && keywordsNode.isArray()) {
        Iterator<JsonNode> keywordIter = keywordsNode.elements();
        StringBuilder keywordBuilder = new StringBuilder();
        JsonNode kw;
        for (int i = 1; keywordIter.hasNext(); ++i) {
          kw = keywordIter.next();
          keywordBuilder.append(kw.asText(""));
          log.info("   -" +kw );
        }
        log.info("   -" +keywordBuilder.toString() );
      }
      JsonNode authorsNode = bibNode.get("author");
      if (!authorsNode.isMissingNode() && authorsNode.isArray()) {
        Iterator<JsonNode> authorIter = authorsNode.elements();
        StringBuilder authors = new StringBuilder();
        JsonNode author;
        for (int i = 1; authorIter.hasNext(); ++i) {
          author = authorIter.next();
          String name = author.get("name").asText(null);
          authors.append(name);
        }
      }
    }
  }

 */

}
