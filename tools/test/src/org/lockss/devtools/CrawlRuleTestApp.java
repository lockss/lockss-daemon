/*
 * $Id: CrawlRuleTestApp.java,v 1.7 2006-02-04 05:34:57 tlipkis Exp $
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

import java.io.*;
import java.util.*;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;


/**
 * CrawlRuleTester: test application for CrawlRules.
 * @author Claire Griffin
 * @version 1.0
 */

public class CrawlRuleTestApp {
  private static Logger log = Logger.getLogger("CrawlRuleTestApp");

  private static String OUT_FILE_PROP = "OutputFile";
  private static String CRAWL_DEPTH_PROP = "CrawlDepth";
  private static String BASE_URL_PROP = "BaseUrl";
  private static String CRAWL_DELAY_PROP = "CrawlDelay";
  private static final long DEFAULT_CRAWL_DELAY = 10 * Constants.SECOND;

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
    }
    catch (ArrayIndexOutOfBoundsException e) {
      displayHelpAndExit();
    }

    if (prop_file == null) {
      displayHelpAndExit();
    }
    runTest(prop_file, output_file, crawl_depth);
  }

  private static void runTest(String prop_file, String output_file,
                              int crawl_depth) {
    Configuration config;
    try {
      Properties props = new Properties();
      try {
        InputStream is = new BufferedInputStream(new FileInputStream(prop_file));
        props.load(is);
      }
      catch (IOException e) {
        System.err.println("Error reading prop file: " + e.toString());
        System.exit(1);
      }
      config = ConfigManager.fromProperties(props);
      if (output_file == null) {
        output_file = props.getProperty(OUT_FILE_PROP);
      }

      if (crawl_depth < 0) {
        crawl_depth = config.getInt(CRAWL_DEPTH_PROP, 0);
      }
      long delay = config.getLong(CRAWL_DELAY_PROP, DEFAULT_CRAWL_DELAY);
      String base_url = config.get(BASE_URL_PROP);
      CrawlRule rule = makeRules(config, base_url);
      CrawlSpec spec =
	new SpiderCrawlSpec(base_url, rule,
			    config.getInt("BASE_CRAWL_DEPTH", 1));

      CrawlRuleTester tester = new CrawlRuleTester(output_file, crawl_depth,
          delay, base_url, spec);
      tester.run();
    }
    catch (Exception ex) {
      System.err.println("Error occured while processing properties:" + ex);
      System.exit(2);
    }
  }

  private static CrawlRule makeRules(Configuration config, String root) {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;

    Configuration allRules = config.getConfigTree("rule");
    TreeSet sorted = new TreeSet(allRules.keySet());
    for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      String val = allRules.get(key);
      int pos = val.indexOf(":");
      String op = val.substring(0, pos);
      String regexp = val.substring(pos + 1);
      try {
        if (op.startsWith("incl")) {
          rules.add(new CrawlRules.RE(regexp, incl));
        }
        else if (op.startsWith("excl")) {
          rules.add(new CrawlRules.RE(regexp, excl));
        }
        else if (op.startsWith("nomatch_incl")) {
          rules.add(new CrawlRules.RE(regexp, CrawlRules.RE.NO_MATCH_INCLUDE));
        }
        else if (op.startsWith("nomatch_excl")) {
          rules.add(new CrawlRules.RE(regexp, CrawlRules.RE.NO_MATCH_EXCLUDE));
        }
	System.err.println("rule " + key + ": " + op + ": " + regexp);

      }
      catch (LockssRegexpException ex) {
        System.err.println("Error creating crawl rule: " + ex);
      }
    }
    return new CrawlRules.FirstMatch(rules);
  }

  public static void main(String[] args) {
    processArgs(args);
  }
}
