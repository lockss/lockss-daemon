package org.lockss.devtools;
import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.crawler.*;
import org.lockss.daemon.*;

import org.lockss.util.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.*;
import java.math.*;

public class CrawlRuleTester {
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

  // our storage for extracted urls
  private TreeSet m_extracted = new TreeSet();
  private TreeSet m_incls = new TreeSet();
  private TreeSet m_excls = new TreeSet();

  public CrawlRuleTester(int crawlDepth, long crawlDelay, String baseUrl,
                         CrawlSpec crawlSpec) {
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

  public void runTest() {
    useLocalWriter = m_outWriter == null ? true : false;
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
    outputMessage("\nChecking " + m_baseUrl +
                  " at crawl depth " + m_crawlDepth +
                  " and crawl delay of " + m_crawlDelay + " millisecs" +
                  " using crawlspec:\n" + m_crawlSpec + "\n");

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


  private void buildUrlSets(String url) throws MalformedURLException {
    URL srcUrl = new URL(url);
    try {
      URLConnection conn = srcUrl.openConnection();
      String type = conn.getContentType();
      type = conn.getHeaderField("content-type");
      if (type == null || !type.toLowerCase().startsWith("text/html"))
        return;
      InputStreamReader reader = new InputStreamReader(conn.getInputStream());
      MockCachedUrl mcu = new MockCachedUrl(srcUrl.toString(), reader);
      GoslingHtmlParser parser = new GoslingHtmlParser();
      parser.parseForUrls(mcu, new MyFoundUrlCallback());
    }
    catch (Exception ex) {
      outputErrResults(url, ex.getMessage());
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

  private void outputMessage(String msg) {
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
                  "    Fetch Rate: " + fetchRate + " p/m");
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

  class MockCachedUrl implements CachedUrl {
    private String url;
    private boolean doesExist = false;
    private Reader reader = null;

    public MockCachedUrl(String url, Reader reader) {
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
      throw new UnsupportedOperationException("Not implemented");
    }

    public String toString() {
      StringBuffer sb = new StringBuffer(url.length() + 17);
      sb.append("[MockCachedUrl: ");
      sb.append(url);
      sb.append("]");
      return sb.toString();
    }
  }
}
