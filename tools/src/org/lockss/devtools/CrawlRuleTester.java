package org.lockss.devtools;
import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;

public class CrawlRuleTester extends Thread {
  /* Message Types */
  public static final int ERROR_MESSAGE = 0;
  public static final int WARNING_MESSAGE = 1;
  public static final int PLAIN_MESSAGE = 2;
  public static final int URL_SUMMARY_MESSAGE = 3;
  public static final int TEST_SUMMARY_MESSAGE = 4;

  public static long DEFAULT_DELAY =
      BaseArchivalUnit.DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS;

  private String m_baseUrl;
  private CrawlSpec m_crawlSpec;
  private int m_crawlDepth;
  private long m_crawlDelay;
  private String m_outputFile = null;
  private  BufferedWriter m_outWriter = null;
  private Deadline fetchDeadline = Deadline.in(0);
  private boolean useLocalWriter = true;
  private MessageHandler m_msgHandler;

  // our storage for extracted urls
  private TreeSet m_extracted = new TreeSet();
  private TreeSet m_incls = new TreeSet();
  private TreeSet m_excls = new TreeSet();

  public CrawlRuleTester(int crawlDepth, long crawlDelay, String baseUrl,
                         CrawlSpec crawlSpec) {
    super("crawlrule tester");
    m_crawlDepth = crawlDepth;
    m_crawlDelay = Math.max(crawlDelay, DEFAULT_DELAY);
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

  private void checkRules() {
    outputMessage("\nChecking " + m_baseUrl, TEST_SUMMARY_MESSAGE);
    outputMessage("crawl depth: " + m_crawlDepth +
                  "     crawl delay: " + m_crawlDelay + " millisecs.\n",
                  PLAIN_MESSAGE);

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

        // crawl the page
        buildUrlSets(urlstr);
        fetched.add(urlstr);
        // output incl/excl results
        outputUrlResults(urlstr, m_incls, m_excls);
        // add the m_incls to the crawlList for next crawl depth loop
        crawlList.addAll(m_incls);
      }
    }
    long elapsed_time = TimeBase.nowMs() - start_time;
    outputSummary(m_baseUrl, fetched, elapsed_time);
  }


  private void buildUrlSets(String url) {

    try {
      URL srcUrl = new URL(url);
      URLConnection conn = srcUrl.openConnection();
      String type = conn.getContentType();
      type = conn.getHeaderField("content-type");
      if (type == null || !type.toLowerCase().startsWith("text/html"))
        return;
      InputStreamReader reader = new InputStreamReader(conn.getInputStream());
      MyMockCachedUrl mcu = new MyMockCachedUrl(srcUrl.toString(), reader);
      GoslingHtmlParser parser = new GoslingHtmlParser();
      parser.parseForUrls(mcu, new MyFoundUrlCallback());
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

  private void outputUrlResults(String url, Set m_inclset, Set m_exclset) {
    outputMessage("\nExtracted Urls from: " + url,URL_SUMMARY_MESSAGE);

    Iterator it = m_inclset.iterator();
    if (it.hasNext()) {
      outputMessage("\nIncluded Urls:", URL_SUMMARY_MESSAGE);
    }
    while (it.hasNext()) {
      outputMessage(it.next().toString(), PLAIN_MESSAGE);
    }

    it = m_exclset.iterator();
    if (it.hasNext())
      outputMessage("\nExcluded Urls:", URL_SUMMARY_MESSAGE);
    while (it.hasNext()) {
      outputMessage(it.next().toString(), PLAIN_MESSAGE);
    }
    if(m_outWriter != null) {
      try {
        m_outWriter.flush();
      }
      catch (IOException ex) {
      }
    }
  }

  private void outputSummary(String baseUrl, Set fetched, long elapsedTime) {
    int fetchCount = fetched.size();
    outputMessage("\n\nSummary for starting Url: " + baseUrl +
                  " and depth: " + m_crawlDepth, TEST_SUMMARY_MESSAGE);
    outputMessage("\nUrls fetched: " + fetchCount +
                  "    Urls extracted: " + m_extracted.size(), PLAIN_MESSAGE);
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
  }

  private class MyFoundUrlCallback
      implements ContentParser.FoundUrlCallback {

    MyFoundUrlCallback() {
    }

    public void foundUrl(String url) {

      if (!m_extracted.contains(url)) {
        m_extracted.add(url);
	try {
	  String normUrl = UrlUtil.normalizeUrl(url);
	  if (CrawlerImpl.isSupportedUrlProtocol(normUrl) &&
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
     * getUnfilteredContentSize
     *
     * @return byte[]
     */
    public byte[] getUnfilteredContentSize() {
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

    public String toString() {
      StringBuffer sb = new StringBuffer(url.length() + 17);
      sb.append("[MyMockCachedUrl: ");
      sb.append(url);
      sb.append("]");
      return sb.toString();
    }
  }
}
