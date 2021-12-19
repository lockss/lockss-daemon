/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.clockss;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import org.lockss.config.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;


/*
 *  Set up useful utility methods to help with extractors based on 
 *  SourceXmlMetadataExtractor
 *  The class 
 *    fooSourceXmlMetadataExtractor which extends sourceXmlMetadataExtractor
 *  should define a test class
 *    testFooSourceXmlMDExtractor which extends testSourceXmlMetadataExtractor
 *  it will then have access to the utility functions for setting up XML
 *  source and examining the results.
 *  Additionally, the test class can define a test version of the extractor
 *  that does not require validating against actual content files in order
 *  to simplify testing.
 */
public class SourceXmlMetadataExtractorTest
extends LockssTestCase {

  private static Logger log = Logger.getLogger(SourceXmlMetadataExtractorTest.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;
  private static String DEFAULT_PLUGIN_NAME = "org.lockss.plugin.clockss.ClockssSourcePlugin";
  private static String DEFAULT_BASE_URL = "http://www.source.org/";
  private static String DEFAULT_YEAR = "2013";
  private static String DEFAULT_XML_URL = "";
  private static String DEFAULT_XML_MIME = "text/xml";

  private static String plugin_name;
  private static String base_url;
  private static String year;

  // doesn't matter. just not empty
  private static final String DEF_PDF_CONTENT = "    ";
  private static CIProperties pdfHeader;


  public SourceXmlMetadataExtractorTest() {
    super();
    plugin_name = DEFAULT_PLUGIN_NAME;
    base_url = DEFAULT_BASE_URL;
    year = DEFAULT_YEAR;

    pdfHeader = new CIProperties();   
    pdfHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
  }

  /*
   * alternate constructor allows for custom setting of plugin params
   * In most cases, default shuld work
   */
  public SourceXmlMetadataExtractorTest(
      String inPName,
      String inBase, String inYear) {
    super();
    plugin_name = inPName;
    base_url = inBase;
    year = inYear;
  }

  /*
   * For use in building up urls, get back the params being used
   *
   */
  public String getBaseUrl() {
    return base_url;
  }
  public String getYear() {
    return year;
  }



  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();

    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau.setConfiguration(auConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", base_url);
    conf.put("year", year);
    return conf;
  }

  /*
   * USEFUL TEST METHODS
   */
  //TODO
  public String getContentFromFile(String inFileName) {
    return null;
  }
  /*
   * Take a string of content and use a pattern to match chunks to replace
   * using the ordered values in the list of String replacements.
   * Useful to say, take an XML template, and fill in a set of DOIs - one per record.
   * If you wish to replace the DOIs and then the ISSNs with set values, just
   * call the method twice using different replacement patterns
   * Use with care - it will only replace as many instances as there are replacements
   */
  //TODO
  public String getContentFromTemplate(String template, Pattern replacePattern, 
      List<String>Replacements) {
    return template;
  }

  /*
   * Take a string of content and use a pattern to match chunks to replace
   * all occurrences of the pattern with the replacement string.
   */
  public String getContentFromTemplate(String template, Pattern replacePattern, 
      String replacement) {
    Matcher mat = replacePattern.matcher(template);
    if (mat.find()) {
      return mat.replaceAll(replacement);
    }
    return template;
  }

  /*
   * Ways to extract content
   *  - minimum: test specifies String content an FileMetadataListExtractor
   *  - optional - provide list of String pdf_urls to be added to the mock daemon
   *  - optional - provide a specific xml_url and xml_url_mime to be used for the content
   *  In each case the method returns an mdList containing the extracted metadata 
   */
  public List<ArticleMetadata> extractFromContent(String inContent, 
      FileMetadataListExtractor mfle) {
    return extractFromContent(DEFAULT_XML_URL, DEFAULT_XML_MIME, inContent, mfle, null);
  }
  public List<ArticleMetadata> extractFromContent(String inContent, 
      FileMetadataListExtractor mfle, 
      List<String> pdf_urls) {
    return extractFromContent(DEFAULT_XML_URL, DEFAULT_XML_MIME, inContent, mfle, pdf_urls);
  }
  public List<ArticleMetadata> extractFromContent(String inUrl, 
      String inMime, 
      String inContent, 
      FileMetadataListExtractor mfle) {
    return extractFromContent(inUrl, inMime, inContent, mfle, null);
  }
  public List<ArticleMetadata> extractFromContent(String xml_url,
      String xml_url_mime,
      String inContent, 
      FileMetadataListExtractor mfle, 
      List<String> pdf_urls) {

    CIProperties xmlHead = new CIProperties();
    xmlHead.put(CachedUrl.PROPERTY_CONTENT_TYPE, xml_url_mime);
    MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHead);
    cu.setContent(inContent);
    cu.setContentSize(inContent.length());
    //cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, xml_url_mime);
    if(pdf_urls != null) {
      Iterator<String> pIt =  pdf_urls.iterator();
      while (pIt.hasNext()) {
        String pdf_url = (String) pIt.next();
        MockCachedUrl pdf_cu = mau.addUrl(pdf_url, true, true, pdfHeader);
        pdf_cu.setContent(DEF_PDF_CONTENT); // just not empty
        pdf_cu.setContentSize(DEF_PDF_CONTENT.length());
      }
    }
    try {
      return mfle.extract(MetadataTarget.Any(), cu);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error(e.getMessage(), e);
    } catch (PluginException e) {
      // TODO Auto-generated catch block
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /*
   *  Convenience methods to help with testing returned results
   *  For one metadata record (either the sole record in a list or an
   *  individual record), check the record content against the 
   *  specified field and value OR
   *  against a map of fields and values.
   *  For extraction that returns multiple records, the testor should control
   *  iterating over the list to control individual record validation since
   *  the order of the returned list is arbitrary.
   */
  public void checkOneMD(List<ArticleMetadata> mdList, MetadataField checkField, 
      String checkValue) {
    assertEquals(1, mdList.size());
    checkOneMD(mdList.get(0), checkField, checkValue);
  }
  public void checkOneMD(ArticleMetadata md, MetadataField checkField,
      String checkValue) {
    if (checkField.getCardinality() == MetadataField.Cardinality.Single) {
      assertEquals(checkValue, md.get(checkField));
    } else {
      // dealing with a multi
      assertEquals(checkValue, md.getList(checkField).toString());
    }
  }
  public void checkOneMD(List<ArticleMetadata> mdList, Map<MetadataField,String> checkMap) {
    assertEquals(1, mdList.size());
    checkOneMD(mdList.get(0), checkMap);
  }
  public void checkOneMD(ArticleMetadata md, Map<MetadataField,String> checkMap) {

    log.debug3("checkOneMD: " + md.ppString(2));
    if (checkMap != null) {
      //Iterate over the map
      for (MetadataField keyField : checkMap.keySet()) {
        // must do special for multi value
        if (keyField.getCardinality() == MetadataField.Cardinality.Single) {
          assertEquals(checkMap.get(keyField), md.get(keyField));
        } else {
          // dealing with a multi
          assertEquals(checkMap.get(keyField), md.getList(keyField).toString());
        }
      }
    }
  }


  /*
   * Printout functions, useful for debugging;
   * list or individual record
   * raw map, cooked map or both
   */
  public void debug3_MDList(List<ArticleMetadata> mdList) {
    Iterator<ArticleMetadata> mdIt = mdList.iterator();
    ArticleMetadata mdRecord = null;
    while (mdIt.hasNext()) {
      mdRecord = (ArticleMetadata) mdIt.next();
      debug3_MDRecord(mdRecord);
    }
  }
  public void debug3_MDRecord(ArticleMetadata md) {
    assertNotNull(md);
    log.debug3(md.ppString(2));
  }

}
