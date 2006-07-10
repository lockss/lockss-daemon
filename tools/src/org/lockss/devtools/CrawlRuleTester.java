package org.lockss.devtools;
import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

public class CrawlRuleTester extends Thread {
  /* Message Types */
  public static final int ERROR_MESSAGE = 0;
  public static final int WARNING_MESSAGE = 1;
  public static final int PLAIN_MESSAGE = 2;
  public static final int URL_SUMMARY_MESSAGE = 3;
  public static final int TEST_SUMMARY_MESSAGE = 4;

  public static long MIN_DELAY = BaseArchivalUnit.MIN_FETCH_DELAY;

  private String m_baseUrl;
  private CrawlSpec m_crawlSpec;
  private int m_crawlDepth;
  private long m_crawlDelay;
  private int m_curDepth;
  private String m_outputFile = null;
  private  BufferedWriter m_outWriter = null;
  private Deadline fetchDeadline = Deadline.in(0);
  private boolean useLocalWriter = true;
  private MessageHandler m_msgHandler;
  private LockssUrlConnectionPool connectionPool =
    new LockssUrlConnectionPool();

  // our storage for extracted urls
  private TreeSet m_extracted = new TreeSet();
  private TreeSet m_incls = new TreeSet();
  private TreeSet m_excls = new TreeSet();
  private TreeSet m_reported = new TreeSet();

  public CrawlRuleTester(int crawlDepth, long crawlDelay, String baseUrl,
                         CrawlSpec crawlSpec) {
    super("crawlrule tester");
    m_crawlDepth = crawlDepth;
    m_crawlDelay = Math.max(crawlDelay, MIN_DELAY);
    m_baseUrl = baseUrl;
    m_crawlSpec = crawlSpec;

  }
  /**
   * RuleTest
   *
   * @param outFile String
   * @param crawlDepth int
   * @param crawlDelay long
   * @param baseUrl String
   * @param crawlSpec CrawlSpec
   */
  public CrawlRuleTester(String outFile, int crawlDepth, long crawlDelay,
                  String baseUrl, CrawlSpec crawlSpec) {

    this(crawlDepth, crawlDelay,baseUrl,crawlSpec);
    m_outputFile = outFile;
  }

  /**
   * RuleTest
   *
   * @param outWriter BufferedWriter
   * @param crawlDepth int
   * @param crawlDelay long
   * @param baseUrl String
   * @param crawlSpec CrawlSpec
   */
  public CrawlRuleTester(BufferedWriter outWriter, int crawlDepth,
                         long crawlDelay,
                         String baseUrl, CrawlSpec crawlSpec) {
    this(crawlDepth, crawlDelay, baseUrl, crawlSpec);
    m_outWriter = outWriter;
  }

  /**
   * RuleTest
   *
   * @param msgHandler MessageHandler to take all output
   * @param crawlDepth the crawl depth to use
   * @param crawlDelay the type to wait between fetches
   * @param baseUrl the url to start from
   * @param crawlSpec a CrawlSpec to use for url checking.
   */
  public CrawlRuleTester(MessageHandler msgHandler, int crawlDepth,
                         long crawlDelay,
                         String baseUrl, CrawlSpec crawlSpec) {
    this(crawlDepth, crawlDelay, baseUrl, crawlSpec);
    m_msgHandler = msgHandler;
  }

  public void run() {
    if(m_outWriter == null && m_msgHandler == null) {
      useLocalWriter = true;
    }
    else {
      useLocalWriter = false;
    }
    if(useLocalWriter) {
      openOutputFile();
    }
    checkRules();
    if(useLocalWriter) {
      closeOutputFile();
    }
  }


  private void openOutputFile() {
    if (m_outputFile != null) {
      try {
        m_outWriter = new BufferedWriter(new FileWriter(m_outputFile, false));
        return;
      }
      catch (Exception ex) {
        System.err.println("Error opening output file, writing to stdout: "
                           + ex);
      }
    }
    m_outWriter = new BufferedWriter(new OutputStreamWriter(System.out));
  }

  private void closeOutputFile() {
    try {
      if (m_outWriter != null) {
        m_outWriter.close();
      }
    }
    catch (IOException ex) {
      System.err.println("Error closing output file.");
    }
  }

  int[] depth_incl;
  int[] depth_fetched;
  int[] depth_parsed;

  private void checkRules() {
    outputMessage("\nChecking " + m_baseUrl, TEST_SUMMARY_MESSAGE);
    outputMessage("crawl depth: " + m_crawlDepth +
                  "     crawl delay: " + m_crawlDelay + " ms.",
                  PLAIN_MESSAGE);

    TreeSet crawlList = new TreeSet();
    TreeSet fetched = new TreeSet();
    // inialize with the baseUrl
    crawlList.add(m_baseUrl);
    depth_incl = new int[m_crawlDepth];
    depth_fetched = new int[m_crawlDepth];
    depth_parsed = new int[m_crawlDepth];
    long start_time = TimeBase.nowMs();
    for (int depth = 1; depth <= m_crawlDepth; depth++) {
      if (interrupted()) {
        return;
      }
      m_curDepth = depth;
      if (crawlList.isEmpty() && depth <= m_crawlDepth) {
	outputMessage("\nNothing left to crawl, exiting after depth " +
		      (depth - 1), PLAIN_MESSAGE);
	break;
      }
      String[] urls = (String[]) crawlList.toArray(new String[0]);
      crawlList.clear();
      outputMessage("\nDepth " + depth, PLAIN_MESSAGE);
      for (int ix = 0; ix < urls.length; ix++) {
        if (interrupted()) {
          return;
        }
        pauseBeforeFetch();
        String urlstr = urls[ix];

        m_incls.clear();
        m_excls.clear();

        // crawl the page
        buildUrlSets(urlstr);
        fetched.add(urlstr);
        // output incl/excl results,
        // add the new_incls to the crawlList for next crawl depth loop
        crawlList.addAll(outputUrlResults(urlstr, m_incls, m_excls));
      }
    }
    long elapsed_time = TimeBase.nowMs() - start_time;
    outputSummary(m_baseUrl, fetched, crawlList, elapsed_time);
  }


  private void buildUrlSets(String url) {

    try {
      outputMessage("\nFetching " + url, TEST_SUMMARY_MESSAGE);
      URL srcUrl = new URL(url);
//       URLConnection conn = srcUrl.openConnection();
//       String type = conn.getContentType();
//       type = conn.getHeaderField("content-type");
//       InputStream istr = conn.getInputStream();
      LockssUrlConnection conn = UrlUtil.openConnection(url, connectionPool);
      try {
	conn.execute();
	int resp = conn.getResponseCode();
	if (resp != 200) {
	  outputMessage("Resp: " + resp + ": " + conn.getResponseMessage(),
			TEST_SUMMARY_MESSAGE);
	  return;
	}
	depth_fetched[m_curDepth - 1]++;
	String cookies = conn.getResponseHeaderValue("Set-Cookie");
	if (cookies != null) {
	  outputMessage("Cookies: " + cookies, PLAIN_MESSAGE);
	}
	String type = conn.getResponseContentType();
	if (type == null || !type.toLowerCase().startsWith("text/html")) {
	  outputMessage("Type: " + type + ", not parsing",URL_SUMMARY_MESSAGE);
	  return;
	}
	outputMessage("Type: " + type + ", extracting Urls",
		      URL_SUMMARY_MESSAGE);
	InputStream istr = conn.getResponseInputStream();
	InputStreamReader reader = new InputStreamReader(istr);
	//       MyMockCachedUrl mcu = new MyMockCachedUrl(srcUrl.toString(), reader);
	GoslingHtmlParser parser = new GoslingHtmlParser();
	parser.parseForUrls(reader, srcUrl.toString() ,
			    new MyFoundUrlCallback());
	istr.close();
	depth_parsed[m_curDepth - 1]++;
      } finally {
	conn.release();
      }
    }
    catch (MalformedURLException murle) {
      murle.printStackTrace();
      outputErrResults(url, "Malformed URL:" + murle.getMessage());
    }
    catch (IOException ex) {
      ex.printStackTrace();
      outputErrResults(url, "IOException: " + ex.getMessage());
    }
 }

  private void pauseBeforeFetch() {
    if (!fetchDeadline.expired()) {
      try {
        fetchDeadline.sleep();
      }
      catch (InterruptedException ie) {
        // no action
      }
    }
    fetchDeadline.expireIn(m_crawlDelay);
  }

  private void outputMessage(String msg, int msgType) {
    if(m_msgHandler != null) {
      m_msgHandler.outputMessage(msg + "\n", msgType);
    }
    else {
      try {
        m_outWriter.write(msg);
        m_outWriter.newLine();
      }
      catch (Exception ex) {
        System.err.println(msg);
      }
    }
  }

  private void outputErrResults(String url, String errMsg) {
    outputMessage("Error: " + errMsg + " occured while processing " + url,
                  ERROR_MESSAGE);
  }

  private Set outputUrlResults(String url, Set m_inclset, Set m_exclset) {
    Set new_incls =
      new TreeSet(CollectionUtils.subtract(m_inclset, m_reported));
    Set new_excls =
      new TreeSet(CollectionUtils.subtract(m_exclset, m_reported));
    if (!m_inclset.isEmpty()) {
      outputMessage("\nIncluded Urls: (" + new_incls.size() + " new, " +
		    (m_inclset.size() - new_incls.size()) + " old)",
		    URL_SUMMARY_MESSAGE);
      depth_incl[m_curDepth - 1] += new_incls.size();
    }
    for (Iterator it = new_incls.iterator(); it.hasNext(); ) {
      outputMessage(it.next().toString(), PLAIN_MESSAGE);
    }

    if (!m_exclset.isEmpty()) {
      outputMessage("\nExcluded Urls: (" + new_excls.size() + " new, " +
		    (m_exclset.size() - new_excls.size()) + " old)",
		    URL_SUMMARY_MESSAGE);
    }
    for (Iterator it = new_excls.iterator(); it.hasNext(); ) {
      outputMessage(it.next().toString(), PLAIN_MESSAGE);
    }
    m_reported.addAll(new_incls);
    m_reported.addAll(new_excls);

    if(m_outWriter != null) {
      try {
        m_outWriter.flush();
      }
      catch (IOException ex) {
      }
    }
    return new_incls;
  }

  private void outputSummary(String baseUrl, Set fetched, Set toCrawl,
			     long elapsedTime) {
    int fetchCount = fetched.size();
    outputMessage("\n\nSummary for starting Url: " + baseUrl +
                  " and depth: " + m_crawlDepth, TEST_SUMMARY_MESSAGE);
    outputMessage("\nUrls fetched: " + fetchCount +
                  "    Urls extracted: " + m_extracted.size(), PLAIN_MESSAGE);

    outputMessage("\nDepth  Fetched  Parsed  New URLs", PLAIN_MESSAGE);
    for (int depth = 1; depth <= m_crawlDepth; depth++) {
      PrintfFormat pf = new PrintfFormat("%5d  %7d  %6d  %8d");
      Integer[] args = new Integer[]{
	new Integer(depth),
	new Integer(depth_fetched[depth - 1]),
	new Integer(depth_parsed[depth - 1]),
	new Integer(depth_incl[depth - 1]),
      };
      String s = pf.sprintf(args);
      outputMessage(s, PLAIN_MESSAGE);
    }

    outputMessage("\nRemaining unfetched: " + toCrawl.size(), PLAIN_MESSAGE);
    if (false) {
      for (Iterator iter = toCrawl.iterator(); iter.hasNext(); ) {
	String url = (String)iter.next();
	outputMessage(url, PLAIN_MESSAGE);
      }
    }
    long secs = elapsedTime / Constants.SECOND;
    long fetchRate = 0;
    if(secs > 0) {
      fetchRate = fetchCount * 60 * Constants.SECOND / elapsedTime;
    }
    outputMessage("\nElapsed Time: " + secs + " secs." +
                  "    Fetch Rate: " + fetchRate + " p/m", PLAIN_MESSAGE);
  }


  public interface MessageHandler {
    void outputMessage(String message, int messageType);
    void close();
  }

  private class MyFoundUrlCallback
      implements ContentParser.FoundUrlCallback {

    MyFoundUrlCallback() {
    }

    public void foundUrl(String url) {

      m_extracted.add(url);
      try {
	String normUrl = UrlUtil.normalizeUrl(url);
	if (BaseCrawler.isSupportedUrlProtocol(normUrl) &&
	    m_crawlSpec.isIncluded(normUrl)) {
	  m_incls.add(normUrl);
	}
	else {
	  m_excls.add(normUrl);
	}
      } catch (MalformedURLException e) {
	m_excls.add(url);
      }

    }
  }

  class MyMockCachedUrl implements CachedUrl {
    private String url;
    private boolean doesExist = false;
    private Reader reader = null;

    public MyMockCachedUrl(String url, Reader reader) {
      this.url = url;
      this.reader = reader;
    }

    public ArchivalUnit getArchivalUnit() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public String getUrl() {
      return url;
    }

    public CachedUrl getCuVersion(int version) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public CachedUrl[] getCuVersions() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public CachedUrl[] getCuVersions(int maxVersions) {
      throw new UnsupportedOperationException("Not implemented");
    }

    public int getVersion() {
      return 1;
    }

    public Reader openForReading() {
      return reader;
    }

    /**
     * getUnfilteredInputStream
     *
     * @return InputStream
     */
    public InputStream getUnfilteredInputStream() {
      throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * openForHashing
     *
     * @return InputStream
     */
    public InputStream openForHashing() {
      throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * getContentSize
     *
     * @return long
     */
    public long getContentSize() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public boolean hasContent() {
      return doesExist;
    }

    public boolean isLeaf() {
      return true;
    }

    public int getType() {
      return CachedUrlSetNode.TYPE_CACHED_URL;
    }

    public CIProperties getProperties() {
      return null;
    }

    public void release() {
    }

    public String toString() {
      StringBuffer sb = new StringBuffer(url.length() + 17);
      sb.append("[MyMockCachedUrl: ");
      sb.append(url);
      sb.append("]");
      return sb.toString();
    }
  }
}
