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

public class TestElsevierXmlLinkExtractorFactory extends LockssTestCase {
  private static Logger logger =
    Logger.getLogger("TestElsevierXmlLinkExtractorFactory");
  protected LinkExtractorFactory lef = null;
  protected LinkExtractor le = null;
  ArchivalUnit au = null;
  String encoding = null;
  String srcUrl = "http://www.example.com/";
  LinkExtractor.Callback cb = null;
  InputStream in = null;
  Hashtable calledBack = new Hashtable();

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

  public void setUp() {
    lef = new ElsevierXmlLinkExtractorFactory();
    au = new SimulatedArchivalUnit();
    cb = new MyCallback();
  }

  public void testReturnsCorrectClass() throws Exception {
    assertNotNull("factory is null", lef);
    assertTrue("factory isn't a LinkExtractorFactory",
	       lef instanceof LinkExtractorFactory);
    Object o = lef.createLinkExtractor("text/xml");
    assertNotNull("factory returns null", o);
    assertTrue("Return is not LinkExtractor", o instanceof LinkExtractor);
  }

  public void testFindCorrectEntries () throws Exception {
    le = lef.createLinkExtractor("text/xml");
    in = new StringInputStream(withLinks);
    for (int i = 0; i < links.length; i++) {
      logger.debug3("Looking for " + srcUrl + links[i]);
      calledBack.put(srcUrl + links[i], links[i]);
    }

    le.extractUrls(au, in, encoding, srcUrl, cb);
    assertTrue("Some links missed", calledBack.isEmpty());
  }

  public void testFindNoEntries () throws Exception {
    le = lef.createLinkExtractor("text/xml");
    in = new StringInputStream(withoutLinks);
    le.extractUrls(au, in, encoding, srcUrl, cb);
  }

  private class MyCallback implements LinkExtractor.Callback {
    MyCallback() {
    }

    public void foundLink(String url) {
      logger.debug3("Found link " + url);
      assertTrue(url + " doesn't start with " + srcUrl,
		 url.startsWith(srcUrl));
      assertTrue(url + " unexpected", calledBack.containsKey(url));
      calledBack.remove(url);
    }
  }

 }
