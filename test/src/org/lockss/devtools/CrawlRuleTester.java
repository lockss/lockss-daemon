package org.lockss.devtools;

import org.lockss.daemon.*;
import java.net.*;
import java.util.*;
import java.io.*;
import gnu.regexp.*;
import org.lockss.util.*;

/**
 * CrawlRuleTester: test application for CrawlRules.
 * @author Claire Griffin
 * @version 1.0
 */

public class CrawlRuleTester {

  private CrawlRuleTester() {
  }

  static private void displayHelpAndExit() {
    System.out.println("\nUsage: java CrawlRuleTester " +
                       "-p <property file>" +
                       "[-o <output file>] [-d <crawl depth>]");
    System.exit(0);
  }

  static private void processArgs(String[] args) {
    String prop_file = null;
    String output_file = null;
    int crawl_depth = -1;

    if (args.length < 2) {
      displayHelpAndExit();
    }
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-p")) { // prop file to process
        prop_file = args[++i];
      }
      else if (args[i].equals("-o")) { // output file
        output_file = args[++i];
      }
      else if (args[i].equals("-d")) { // crawl depth
        crawl_depth = Integer.parseInt(args[++i]);
      }
    }
    if (prop_file != null) {
      RuleTester tester = new RuleTester(prop_file, output_file, crawl_depth);
      tester.runTest();
    }
  }

  public static void main(String[] args) {
    CrawlRuleTester crawlRuleTester1 = new CrawlRuleTester();
    processArgs(args);
  }
}

class RuleTester {
  private static final String IMGTAG = "img";
  private static final String ATAG = "a";
  private static final String FRAMETAG = "frame";
  private static final String LINKTAG = "link";
  private static final String SCRIPTTAG = "script";
  private static final String SCRIPTTAGEND = "/script";
  private static final String BODYTAG = "body";
  private static final String TABLETAG = "table";
  private static final String TDTAG = "tc";

  private static final String ASRC = "href";
  private static final String SRC = "src";
  private static final String BACKGROUNDSRC = "background";
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
  private String m_propFile;

  RuleTester(String propFile, String outFile, int crawlDepth) {
    m_propFile = propFile;
    m_outputFile = outFile;
    m_crawlDepth = crawlDepth;
  }

  void runTest() {
    loadProps();

    checkRules();
  }

  private void loadProps() {
    Properties props = new Properties();
    try {
      FileInputStream fis = new FileInputStream(m_propFile);
      props.load(fis);

      // initialize the output file
      if (m_outputFile == null) {
        m_outputFile = props.getProperty(OUT_FILE_PROP, m_outputFile);
      }
      props.remove(OUT_FILE_PROP);

      // initialize the crawl depth
      if (m_crawlDepth == -1) {
        m_crawlDepth = Integer.parseInt(props.getProperty(CRAWL_DEPTH_PROP, "0"));
      }
      props.remove(CRAWL_DEPTH_PROP);

      // initialize the crawl delay
      m_crawlDelay = Math.max(Long.parseLong(props.getProperty(CRAWL_DELAY_PROP,
          String.valueOf(DEFAULT_CRAWL_DELAY))), DEFAULT_CRAWL_DELAY);
      props.remove(CRAWL_DELAY_PROP);

     // initialize the base url
      m_baseUrl = props.getProperty(BASE_URL_PROP);
      props.remove(BASE_URL_PROP);

      // now load the crawl rules
      m_crawlSpec = new CrawlSpec(m_baseUrl, makeRules(props), null, 1);
    }
    catch (IOException ex) {
      exitOnError("Error processing prop file: ", ex);
    }
  }

  private void checkRules() {
    TreeSet crawled = new TreeSet();
    TreeSet crawlList = new TreeSet();
    TreeSet incls = new TreeSet();
    TreeSet excls = new TreeSet();

    // inialize with the baseUrl
    crawlList.add(m_baseUrl);
    long start_time = TimeBase.nowMs();
    for (int i = 0; i < m_crawlDepth; i++) {
      String[] urls = (String[]) crawlList.toArray(new String[0]);
      crawlList.clear();
      for (int u_count = 0; u_count < urls.length; u_count++) {
        pauseBeforeFetch();
        String urlstr = urls[u_count];
        crawled.add(urlstr);
        try {
          URL url = new URL(urlstr);
          // crawl the page
          buildUrlSets(url, incls, excls, crawled);
          // output incl/excl results
          outputUrlResults(urlstr, incls, excls);
          // add the incls to the crawlList for next crawl depth loop
          Iterator it = incls.iterator();
          while(it.hasNext()) {
            crawlList.add(it.next());
          }
          incls.clear();
          excls.clear();
        }
        catch (MalformedURLException ex) {
          outputErrResults(urlstr, ex.getMessage());
        }
      }
    }
    long elapsed_time = TimeBase.nowMs() - start_time;
    outputSummary(m_baseUrl, crawled, elapsed_time);
  }


  private CrawlRule makeRules(Properties props) {
    List rules = new LinkedList();
    final int incl = CrawlRules.RE.MATCH_INCLUDE;
    final int excl = CrawlRules.RE.MATCH_EXCLUDE;
    String root = m_baseUrl.toString();

    Enumeration enum = props.propertyNames();
    while (enum.hasMoreElements()) {
      String value = (String) enum.nextElement();
      String regexp = props.getProperty(value);
      try {
        if (value.startsWith("incl"))
          rules.add(new CrawlRules.RE(regexp, incl));
        else if(value.startsWith("excl"))
          rules.add(new CrawlRules.RE(regexp, excl));
      }
      catch (REException ex) {
        exitOnError("Error creating crawl rule: ", ex);
      }
    }
    System.out.println("\nChecking " + root + " using rules:\n" + rules);
    return new CrawlRules.FirstMatch(rules);
  }

  private void exitOnError(String msg, Exception ex) {
    System.out.println(msg + ex.toString());
    System.exit(2);
  }
  private int fetchCount;
  private void buildUrlSets(URL srcUrl,
                            Set fetchSet,
                            Set ignoreSet,

                            Set crawledSet) {
    try {
      URLConnection conn = srcUrl.openConnection();
      fetchCount++;
      String type = conn.getContentType();
      type = conn.getHeaderField("content-type");
      String encoding = conn.getContentEncoding();
      if (type == null || !type.toLowerCase().startsWith("text/html"))
        return;
      int i = type.indexOf("charset=");
      if(i > 0) {
        encoding = type.substring(i + "charset=".length());
        System.out.println("Encoding:" + encoding);
      }
      InputStreamReader reader = new InputStreamReader(conn.getInputStream());
      String nextUrl = null;
      System.out.println("System encoding " + System.getProperty("file.encoding")
      + " reader encoding: " + reader.getEncoding() +"\n\n");
      while ( (nextUrl = extractNextLink(reader, srcUrl)) != null) {
        if (!crawledSet.contains(nextUrl)) {
          crawledSet.add(nextUrl);
          if (isSupportedUrlProtocol(srcUrl, nextUrl) &&
              m_crawlSpec.isIncluded(nextUrl)) {
            fetchSet.add(nextUrl);
          }
          else {
            ignoreSet.add(nextUrl);
          }
        }
      }
    }
    catch (Exception ex) {
      System.out.println("Error reading " + srcUrl + "Ex:" + ex.getMessage());
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

  private void outputErrResults(String url, String errMsg) {
    System.out.println("Error: " + errMsg + " occured while processing " + url);
  }

  private void outputUrlResults(String url, Set inclSet, Set exclSet) {
    System.out.println("\nProcessed: " + url);

    Iterator it = inclSet.iterator();
    if (it.hasNext()) {
      System.out.println("\nUrl's included:");
    }
    while (it.hasNext()) {
      System.out.println(it.next());
    }

    it = exclSet.iterator();
    if (it.hasNext())
      System.out.println("\nUrl's excluded:");
    while (it.hasNext()) {
      System.out.println(it.next());
    }
  }

  private void outputSummary(String baseUrl, Set crawledSet, long elapsedTime) {
    System.out.println("\n\nSummary for base Url:" + baseUrl +
                       " at depth " + m_crawlDepth);
    System.out.println("\nUrls checked: " + crawledSet.size() +
                       "    Urls fetched: " + fetchCount);
    long secs = elapsedTime / Constants.SECOND;

    System.out.println("Elapsed Time: " + secs + " secs." +
                       "    Fetch Rate: " + (fetchCount* 60)/secs + " p/m" );
  }

  /**
   * Read through the reader stream, extract and return the next url found
   *
   * @param reader Reader object to extract the link from
   * @param srcUrl URL object representing the page we are looking at
   * (for resolving relative links)
   * @return String representing the next url in reader
   * @throws IOException
   * @throws MalformedURLException
   */
  protected static String extractNextLink(Reader reader, URL srcUrl) throws
      IOException, MalformedURLException {
    boolean inscript = false; //FIXME or I will break when we look at scripts
    String nextLink = null;
    int c = 0;
    StringBuffer lineBuf = new StringBuffer();

    while (nextLink == null && c >= 0) {
      //skip to the next tag
      do {
        c = reader.read();
      }
      while (c >= 0 && c != '<');

      if (c == '<') {
        int pos = 0;
        c = reader.read();
        while (c >= 0 && c != '>') {
          if (pos == 2 && c == '-' && lineBuf.charAt(0) == '!'
              && lineBuf.charAt(1) == '-') {
            // we're in a HTML comment
            pos = 0;
            int lc1 = 0;
            int lc2 = 0;
            while ( (c = reader.read()) >= 0
                   && (c != '>' || lc1 != '-' || lc2 != '-')) {
              lc1 = lc2;
              lc2 = c;
            }
            break;
          }
          lineBuf.append( (char) c);
          pos++;
          c = reader.read();
        }

        if (inscript) {
          //FIXME when you deal with the script problems
          //	  if(lookingAt(lineBuf, 0, pos, scripttagend)) {
          inscript = false;
          //}
        }
        else if (lineBuf.length() >= 5) { //see if the lineBuf has a link tag
          nextLink = parseLink(lineBuf, srcUrl);
        }
        lineBuf = new StringBuffer();
      }
    }
    return nextLink;
  }

  protected static boolean isSupportedUrlProtocol(URL srcUrl, String url) {
    try {
      URL ur = new URL(srcUrl, url);
      if (StringUtil.getIndexIgnoringCase(url, "https") != 0) {
        return true;
      }
    }
    catch (Exception ex) {
    }
    return false;
  }

  private static boolean beginsWithTag(String s1, String tag) {
    if( StringUtil.getIndexIgnoringCase(s1,tag) == 0 ) {
      int len = tag.length();
      if(s1.length() > len && Character.isWhitespace(s1.charAt(len))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Method to take a link tag, and parse out the URL it points to, returning
   * a string representation of the url (lifted and rewritten from the Gosling
   * crawler)
   *
   * @param link StringBuffer containing the text of a link tag (everything
   * between < and > (ie, "a href=http://www.test.org")
   * @param srcUrl URL object representing the page on which this
   * url was taken from (for resolving relative tags)
   * @return string representation of the url from the link tag
   * @throws MalformedURLException
   */
  protected static String parseLink(StringBuffer link, URL srcUrl) throws
      MalformedURLException {
    String returnStr = null;

    switch (link.charAt(0)) {
      case 'a': //<a href=http://www.yahoo.com>
      case 'A':
        if (beginsWithTag(link.toString(),ATAG)) {
          returnStr = getAttributeValue(ASRC, link.toString());
        }
        break;
      case 'f': //<frame src=frame1.html>
      case 'F':
        if (beginsWithTag(link.toString(),FRAMETAG)) {
          returnStr = getAttributeValue(SRC, link.toString());
        }
        break;
      case 'i': //<img src=image.gif>
      case 'I':
        if (beginsWithTag(link.toString(),IMGTAG)) {
          returnStr = getAttributeValue(SRC, link.toString());
        }
        break;
      case 'l': //<link href=blah.css>
      case 'L':
        if (beginsWithTag(link.toString(),LINKTAG)) {
          returnStr = getAttributeValue(ASRC, link.toString());
        }
        break;
      case 'b': //<body backgroung=background.gif>
      case 'B':
        if (beginsWithTag(link.toString(),BODYTAG)) {
          returnStr = getAttributeValue(BACKGROUNDSRC, link.toString());
        }
        break;
      case 's': //<script src=blah.js>
      case 'S':
        if (beginsWithTag(link.toString(),SCRIPTTAG)) {
          returnStr = getAttributeValue(SRC, link.toString());
        }
        break;
      case 't': //<tc background=back.gif> or <table background=back.gif>
      case 'T':
        if (beginsWithTag(link.toString(),TABLETAG) ||
          beginsWithTag(link.toString(),TDTAG)) {
          returnStr = getAttributeValue(BACKGROUNDSRC, link.toString());
        }
        break;
      default:
        return null;
    }

    if (returnStr != null) {
      returnStr = StringUtil.trimAfterChars(returnStr, " #\"");
      if (!isSupportedUrlProtocol(srcUrl, returnStr)) {
        System.out.println("skipping unsupported url " + returnStr);
      }
      else {
        URL retUrl = new URL(srcUrl, returnStr);
        returnStr = retUrl.toString();
      }
      return returnStr;
    }
    return null;
  }

  private static String getAttributeValue(String attribute, String src) {
    StringTokenizer st = new StringTokenizer(src, "\n\t\r =\"", true);
    String lastToken = null;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (!token.equals("=")) {
        if (!token.equals(" ") && !token.equals("\"")) {
          lastToken = token;
        }
      }
      else {
        if (attribute.equalsIgnoreCase(lastToken))
          while (st.hasMoreTokens()) {
            token = st.nextToken();
            // we need to allow for arguments in the url which use '='
            if (!token.equals(" ") && !token.equals("\"")) {
              StringBuffer sb = new StringBuffer(token);
              while (st.hasMoreTokens() &&
                     !token.equals(" ") && !token.equals("\"")) {
                token = st.nextToken();
                sb.append(token);
              }
              return sb.toString();
            }
          }
      }
    }
    return null;
  }

}
