/*
 * $Id: TestEmeraldMetadataExtractor.java,v 1.2 2011-06-14 09:26:51 tlipkis Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.emerald;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.extractor.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www.emeraldinsight.com/journals.htm?issn=0961-5539&volume=14&issue=5&articleid=1455115&show=html&view=printarticle
 */
public class TestEmeraldMetadataExtractor
  extends FileMetadataExtractorTestCase {
  static Logger log = Logger.getLogger("TestEmeraldMetadataExtractor");

  static String GOOD_FILE = "sample1.html"; // anonymized from URL above

  static String badContent =
    "<HTML><HEAD><TITLE>Article Title</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    "1144-875X</div>\n";

  // Expected raw metdata
  static Map<String,String> expRaw = new HashMap<String,String>();
  static {
    expRaw.put("citation_abstract_html_url", "http://www.example.com/journals.htm?issn=1144-875X&volume=14&issue=5&articleid=1455115");
    expRaw.put("citation_authors", "A. Chicken; B.P. Chick; H.P. LoveChicken");
    expRaw.put("citation_date", "01/07/2004");
    expRaw.put("citation_doi", "10.1234/314159");
    expRaw.put("citation_firstpage", "579");
    expRaw.put("citation_fulltext_html_url", "http://www.example.com/journals.htm?issn=1144-875X&volume=14&issue=5&articleid=1455115&show=html");
    expRaw.put("citation_issn", "1144-875X");
    expRaw.put("citation_issue", "5");
    expRaw.put("citation_journal_title", "International Journal of Chicken Little");
    expRaw.put("citation_keywords", " Flow; Gases; Turbulence");
    expRaw.put("citation_lastpage", "605");
    expRaw.put("citation_pdf_url", "http://www.example.com/journals.htm?issn=1144-875X&volume=14&issue=5&articleid=1455115&show=pdf");
    expRaw.put("citation_publisher", "Chicken Group Publishing Limited");
    expRaw.put("citation_title", "The chicken-chicken chicken of chicken-chicken effects");
    expRaw.put("citation_volume", "14");
    expRaw.put("keywords", "Chicken,International Journal of ChickenChicken,The chicken-chicken chicken of chicken-chicken");
    expRaw.put("robots", "index,follow");
    expRaw.put("site development", "Graham Cracker, Barry Goldwater, Chris Columbus");
  };

  // Expected cooked metdata
  static Map<MetadataField,Object> expCooked =
    new HashMap<MetadataField,Object>();
  static {
    expCooked.put(MetadataField.FIELD_DOI, "10.1234/314159");
    expCooked.put(MetadataField.FIELD_DATE, "01/07/2004");
    expCooked.put(MetadataField.FIELD_ISSN, "1144-875X");
    expCooked.put(MetadataField.FIELD_VOLUME, "14");
    expCooked.put(MetadataField.FIELD_ISSUE, "5");
    expCooked.put(MetadataField.FIELD_START_PAGE, "579");
    expCooked.put(MetadataField.FIELD_AUTHOR,
		  ListUtil.list("A. Chicken",
				"B.P. Chick",
				"H.P. LoveChicken"));
    expCooked.put(MetadataField.FIELD_ARTICLE_TITLE,
		  "The chicken-chicken chicken of chicken-chicken effects");
    expCooked.put(MetadataField.FIELD_JOURNAL_TITLE,
		  "International Journal of Chicken Little");
  };

  protected FileMetadataExtractorFactory getFactory() {
    return new EmeraldHtmlMetadataExtractorFactory();
  }

  protected String getMimeType() {
    return MIME_TYPE_HTML;
  }

  public void testGoodFile() throws Exception {
    ArticleMetadata md = extractFromResource(GOOD_FILE);
    assertRawEquals(expRaw, md);
    assertCookedEquals(expCooked, md);
  }

  public void testBadContent() throws Exception {
    assertRawEquals("foo", "bar", extractFrom(badContent));
  }
}
