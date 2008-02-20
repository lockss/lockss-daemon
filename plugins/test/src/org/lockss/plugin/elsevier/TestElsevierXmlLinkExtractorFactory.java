/*
 * $Id: TestElsevierXmlLinkExtractorFactory.java,v 1.3 2008-02-20 19:11:55 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.elsevier;

import java.io.*;
import java.util.*;

import org.lockss.util.*;
import org.lockss.test.LockssTestCase;
import org.lockss.test.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.elsevier.*;

public class TestElsevierXmlLinkExtractorFactory
  extends LinkExtractorTestCase {

  private static Logger logger =
    Logger.getLogger("TestElsevierXmlLinkExtractorFactory");

  String srcUrl = "http://www.example.com/";

  private static final String withLinks =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<!DOCTYPE dataset SYSTEM \"http://support.sciencedirect.com/xml/sdosftp10.dtd\">\n" +
    "<dataset identifier=\"OXM10160\" customer=\"OHL\"" +
    " status=\"Announcement\"" +
    " version=\"Network Dataset Announcement/Confirmation v1.0\">" +
    " <date year=\"2007\" month=\"May\" day=\"1\"/>\n" +
    "<file name=\"01407007.tar\" size=\"21780480\"" +
    " md5=\"6c7266e0e246bf3e8cf1cd8b659a7a73\"/>\n" +
    "<file name=\"03064530.tar\" size=\"12748800\"" +
    " md5=\"df9519d3075e164d22f5dd4988a693c3\"/>\n" +
    "<file name=\"dataset.toc\" size=\"2216587\"" +
    " md5=\"cd21741eb91fa0fdfef2fa36485e21a0\"/>\n" +
    "</dataset>\n";

  private static final String withoutLinks =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<!DOCTYPE dataset SYSTEM \"http://support.sciencedirect.com/xml/sdosftp10.dtd\">\n" +
    "<dataset identifier=\"OXM10160\" customer=\"OHL\"" +
    " status=\"Announcement\"" +
    " version=\"Network Dataset Announcement/Confirmation v1.0\">" +
    " <date year=\"2007\" month=\"May\" day=\"1\"/>\n" +
    "</dataset>\n";

  private static final String[] links = {
    "01407007.tar", "03064530.tar", "dataset.toc",
  };

  public String getMimeType() {
    return "text/xml";
  }

  public LinkExtractorFactory getFactory() {
    return new ElsevierXmlLinkExtractorFactory();
  }

  public void testFindCorrectEntries () throws Exception {
    Set expected = new HashSet();
    for (String link : links) {
      expected.add(srcUrl + link);
    }
    assertEquals(expected, extractUrls(withLinks));
  }

  public void testFindNoEntries () throws Exception {
    assertEmpty(extractUrls(withoutLinks));
  }

 }
