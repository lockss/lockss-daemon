/*
 * $Id: CrawlRuleTester.java,v 1.9 2004-04-19 19:03:03 tlipkis Exp $
 */

/*

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

package org.lockss.devtools;

import java.net.*;
import java.util.*;
import java.io.*;
import gnu.regexp.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.test.*;

/**
 * CrawlRuleTester: test application for CrawlRules.
 * @author Claire Griffin
 * @version 1.0
 */

public class CrawlRuleTester {

  private CrawlRuleTester() {
  }

  static private void displayHelpAndExit() {
    System.err.println("\nUsage: java CrawlRuleTester " +
                       "-p <property file>" +
                       "[-o <output file>] [-d <crawl depth>]");
    System.exit(1);
  }

  static private void processArgs(String[] args) {
    String prop_file = null;
    String output_file = null;
    int crawl_depth = -1;

    if (args.length < 2) {
      displayHelpAndExit();
    }
    try {
      for (int ix = 0; ix < args.length; ix++) {
	String arg = args[ix];
	if (arg.equals("-p")) { // prop file to process
	  prop_file = args[++ix];
	}
	else if (arg.equals("-o")) { // output file
	  output_file = args[++ix];
	}
	else if (arg.equals("-d")) { // crawl depth
	  crawl_depth = Integer.parseInt(args[++ix]);
	}
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      displayHelpAndExit();
    }
      
    if (prop_file == null) {
      displayHelpAndExit();
    }
    Properties props = new Properties();
    try {
      InputStream is = new BufferedInputStream(new FileInputStream(prop_file));
      props.load(is);
    } catch (IOException e) {
      System.err.println("Error reading prop file: " + e.toString());
      System.exit(1);
    }
    RuleTester tester = new RuleTester(props, output_file, crawl_depth);
    tester.runTest();
  }

  public static void main(String[] args) {
    processArgs(args);
  }

  static class RuleTester {
    private static String OUT_FILE_PROP = "OutputFile";
    private static String CRAWL_DEPTH_PROP = "CrawlDepth";
    private static String BASE_URL_PROP = "BaseUrl";
    private static String CRAWL_DELAY_PROP="CrawlDelay";
    private static final long DEFAULT_CRAWL_DELAY = 10 * Constants.SECOND;

    private String m_baseUrl;
    private CrawlSpec m_crawlSpec;
    private int m_crawlDepth;
    private long m_crawlDelay;
    private String m_outputFile;
    private Configuration m_config;
    private static BufferedWriter m_outWriter;

    private TreeSet m_extracted = new TreeSet();
    private TreeSet m_incls = new TreeSet();
    private TreeSet m_excls = new TreeSet();

    RuleTester(Properties props, String outFile, int crawlDepth) {
      m_config = ConfigManager.fromProperties(props);
      m_outputFile =
	(outFile != null) ? outFile : props.getProperty(OUT_FILE_PROP);
      m_crawlDepth = crawlDepth;
    }

    void runTest() {
      openOutputFile();
      loadProps();
      checkRules();
      closeOutputFile();
    }

    private void openOutputFile() {
      if (m_outputFile != null) {
	try {
	  m_outWriter = new BufferedWriter(new FileWriter(m_outputFile,false));
	  return;
	}
	catch (Exception ex) {
	  System.err.println("Error opening output file, writing to stdout: "
			     + ex);
	}
	m_outWriter = new BufferedWriter(new OutputStreamWriter(System.out));
      }
    }

    private void closeOutputFile() {
      try {
	if(m_outWriter != null) {
	  m_outWriter.close();
	}
      }
      catch (IOException ex) {
	System.err.println("Error closing output file.");
      }
    }

    private void loadProps() {
      try {
	// initialize the crawl depth
	if (m_crawlDepth == -1) {
	  m_crawlDepth = m_config.getInt(CRAWL_DEPTH_PROP, 0);
	}

	// initialize the crawl delay
	m_crawlDelay =
	  Math.max(m_config.getLong(CRAWL_DELAY_PROP, DEFAULT_CRAWL_DELAY),
		   DEFAULT_CRAWL_DELAY);

	// initialize the base url
	m_baseUrl = m_config.get(BASE_URL_PROP);

	// initialize the base crawl depth
	int baseCrawlDepth = m_config.getInt("BASE_CRAWL_DEPTH", 1);

	// now load the crawl rules
	m_crawlSpec = new CrawlSpec(m_baseUrl, makeRules(m_config),
				    baseCrawlDepth);
      }
      catch (Exception ex) {
	exitOnError("Error processing prop file: ", ex);
      }
    }

    private void checkRules() {
      TreeSet crawlList = new TreeSet();
      TreeSet fetched = new TreeSet();

      // inialize with the baseUrl
      crawlList.add(m_baseUrl);
      long start_time = TimeBase.nowMs();
      for (int depth = 0; depth < m_crawlDepth; depth++) {
	String[] urls = (String[]) crawlList.toArray(new String[0]);
	crawlList.clear();
	for (int ix = 0; ix < urls.length; ix++) {
	  pauseBeforeFetch();
	  String urlstr = urls[ix];

	  m_incls.clear();
	  m_excls.clear();
	  try {
	    // crawl the page
	    buildUrlSets(urlstr);
	    fetched.add(urlstr);
	    // output incl/excl results
	    outputUrlResults(urlstr, m_incls, m_excls);
	    // add the m_incls to the crawlList for next crawl depth loop
	    crawlList.addAll(m_incls);
	  }
	  catch (MalformedURLException ex) {
	    outputErrResults(urlstr, ex.getMessage());
	  }
	}
      }
      long elapsed_time = TimeBase.nowMs() - start_time;
      outputSummary(m_baseUrl, fetched, elapsed_time);
    }


    private CrawlRule makeRules(Configuration config) {
      List rules = new LinkedList();
      final int incl = CrawlRules.RE.MATCH_INCLUDE;
      final int excl = CrawlRules.RE.MATCH_EXCLUDE;
      String root = m_baseUrl.toString();

      for (Iterator iter = config.keyIterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	String regexp = config.get(key);
	try {
	  if (key.startsWith("incl"))
	    rules.add(new CrawlRules.RE(regexp, incl));
	  else if(key.startsWith("excl"))
	    rules.add(new CrawlRules.RE(regexp, excl));
	}
	catch (REException ex) {
	  exitOnError("Error creating crawl rule: ", ex);
	}
      }
      outputMessage("\nChecking " + root + " using rules:\n" + rules);
      return new CrawlRules.FirstMatch(rules);
    }

    private void exitOnError(String msg, Exception ex) {
      outputMessage(msg + ex.toString());
      closeOutputFile();
      System.exit(2);
    }

    private void buildUrlSets(String url) throws MalformedURLException {
      URL srcUrl = new URL(url);
      try {
	URLConnection conn = srcUrl.openConnection();
	String type = conn.getContentType();
	type = conn.getHeaderField("content-type");
	String encoding = conn.getContentEncoding();
	if (type == null || !type.toLowerCase().startsWith("text/html"))
	  return;
	MockCachedUrl mcu = new MockCachedUrl(srcUrl.toString());
	InputStreamReader reader = new InputStreamReader(conn.getInputStream());
	GoslingHtmlParser parser = new GoslingHtmlParser();
	parser.parseForUrls(mcu, new MyFoundUrlCallback());
      }
      catch (Exception ex) {
	outputErrResults(url, ex.getMessage());
      }
    }

    private class MyFoundUrlCallback
      implements ContentParser.FoundUrlCallback {

      MyFoundUrlCallback() {
      }

      public void foundUrl(String url) {
	if (!m_extracted.contains(url)) {
	  m_extracted.add(url);
	  if (CrawlerImpl.isSupportedUrlProtocol(url) &&
	      m_crawlSpec.isIncluded(url)) {
	    m_incls.add(url);
	  }
	  else {
	    m_excls.add(url);
	  }
	}
      }
    }
    private Deadline fetchDeadline = Deadline.in(0);

    private void pauseBeforeFetch() {
      if (!fetchDeadline.expired()) {
	try {
	  fetchDeadline.sleep();
	} catch (InterruptedException ie) {
	  // no action
	}
      }
      fetchDeadline.expireIn(m_crawlDelay);
    }

    private static void outputMessage(String msg) {
      try {
	m_outWriter.write(msg);
	m_outWriter.newLine();
      }
      catch (Exception ex) {
	System.err.println(msg);
      }
    }

    private void outputErrResults(String url, String errMsg) {
      outputMessage("Error: " + errMsg + " occured while processing " + url);
    }

    private void outputUrlResults(String url, Set m_inclset, Set m_exclset) {
      outputMessage("\nProcessed: " + url);

      Iterator it = m_inclset.iterator();
      if (it.hasNext()) {
	outputMessage("\nUrl's included:");
      }
      while (it.hasNext()) {
	outputMessage(it.next().toString());
      }

      it = m_exclset.iterator();
      if (it.hasNext())
	outputMessage("\nUrl's excluded:");
      while (it.hasNext()) {
	outputMessage(it.next().toString());
      }
      try {
	m_outWriter.flush();
      }
      catch (IOException ex) {
      }
    }

    private void outputSummary(String baseUrl, Set fetched, long elapsedTime) {
      int fetchCount = fetched.size();
      outputMessage("\n\nSummary for base Url:" + baseUrl +
		    " at depth " + m_crawlDepth);
      outputMessage("\nUrls checked: " + m_extracted.size() +
		    "    Urls fetched: " + fetchCount);
      long secs = elapsedTime / Constants.SECOND;
      long fetchRate = fetchCount * 60 * Constants.SECOND / elapsedTime;
      outputMessage("Elapsed Time: " + secs + " secs." +
		    "    Fetch Rate: " + fetchRate + " p/m" );
    }

  }
}
