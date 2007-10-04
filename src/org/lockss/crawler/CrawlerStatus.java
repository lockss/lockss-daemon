/*
 * $Id: CrawlerStatus.java,v 1.5 2007-10-04 04:06:17 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.util.*;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.collections.map.LinkedMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

/** Status of an individual crawl, including start, stop times, bytes
 * fetched, and URL counters/sets (fetched, excluded, errors, etc.)  A
 * config param ({@link #PARAM_RECORD_URLS}) controls whether the sets/maps
 * of URLs are recorded and displayed in the UI, or merely counted.
*/
public class CrawlerStatus {
  static Logger log = Logger.getLogger("CrawlerStatus");

  public static final String ALL_URLS = "all";

  /** Determines which sets/maps of URLs are recorded and which are only
   * counted.  (Recording URLs in crawl status takes lots of memory.)  If
   * the substrings <code>fetched</code>, <code>excluded</code>,
   * <code>parsed</code>, <code>notModified</code>, <code>pending</code>,
   * <code>error</code> appear in the value of the parameter, the
   * corresponding sets or URLs will be recorded.  <code>all</code> causes
   * all sets to be recorded. */
  public static final String PARAM_RECORD_URLS =
    Configuration.PREFIX + "crawlStatus.recordUrls";
  public static final String DEFAULT_RECORD_URLS = ALL_URLS;

  /** Determines which sets/maps of URLs are recorded and which are only
   * counted.  (Recording URLs in crawl status takes lots of memory.)  If
   * the substrings <code>fetched</code>, <code>excluded</code>,
   * <code>parsed</code>, <code>notModified</code>, <code>pending</code>,
   * <code>error</code> appear in the value of the parameter, the
   * corresponding sets or URLs will be recorded.  <code>all</code> causes
   * all sets to be recorded. */
  public static final String PARAM_KEEP_URLS =
    Configuration.PREFIX + "crawlStatus.keepUrls";
  public static final String DEFAULT_KEEP_URLS = "errors, sources";

  /** Max number of off-site excluded URLs to keep; any more are just
   * counted.  -1 is the same as infinite. */
  public static final String PARAM_KEEP_OFF_HOST_EXCLUDES =
    Configuration.PREFIX + "crawlStatus.keepOffHostExcludes";
  public static final int DEFAULT_KEEP_OFF_HOST_EXCLUDES = 50;

  static Map<Integer,String> DEFAULT_MESSAGES = new HashMap();
  static {
    DEFAULT_MESSAGES.put(Crawler.STATUS_UNKNOWN, "Unknown");
    DEFAULT_MESSAGES.put(Crawler.STATUS_QUEUED, "Pending");
    DEFAULT_MESSAGES.put(Crawler.STATUS_ACTIVE, "Active");
    DEFAULT_MESSAGES.put(Crawler.STATUS_SUCCESSFUL, "Successful");
    DEFAULT_MESSAGES.put(Crawler.STATUS_ERROR, "Error");
    DEFAULT_MESSAGES.put(Crawler.STATUS_ABORTED, "Aborted");
    DEFAULT_MESSAGES.put(Crawler.STATUS_WINDOW_CLOSED,
			 "Interrupted by crawl window");
    DEFAULT_MESSAGES.put(Crawler.STATUS_FETCH_ERROR, "Fetch error");
    DEFAULT_MESSAGES.put(Crawler.STATUS_NO_PUB_PERMISSION,
			 "No permission from publisher");
    DEFAULT_MESSAGES.put(Crawler.STATUS_PLUGIN_ERROR, "Plugin error");
    DEFAULT_MESSAGES.put(Crawler.STATUS_REPO_ERR, "Repository error");
    DEFAULT_MESSAGES.put(Crawler.STATUS_RUNNING_AT_CRASH,
			 "Interrupted by daemon exit");
  }

  private static int ctr = 0;		// Instance counter (for getKey())

  private String key;
  protected long startTime = -1;
  protected long endTime = -1;
  protected String statusMessage = null;
  protected int status = Crawler.STATUS_QUEUED;
  protected Collection startUrls = null;
  protected ArchivalUnit au = null;
  protected String type;
  private long contentBytesFetched = 0;
  private String paramRecordUrls;
  private int paramKeepOffHostExcludes = DEFAULT_KEEP_OFF_HOST_EXCLUDES;
  private String forceRecord;

  protected UrlCount sources;
  protected UrlCount fetched;
  protected UrlCount excluded;
  protected int excludedExcludes = 0;
  protected int includedExcludes = 0;
  protected UrlCount notModified;
  protected UrlCount parsed;
  protected UrlCount pending;
  protected UrlCount errors;

  //  Maps mimetype to UrlCounter
  protected Map mimeCounts = new HashMap(); 
    
  public CrawlerStatus(ArchivalUnit au, Collection startUrls, String type) {
    this.au = au;
    this.startUrls = startUrls;
    this.type = type;
    key = nextIdx();
    initCounters();
  }

  public CrawlerStatus(ArchivalUnit au, Collection startUrls, String type,
		       String forceRecord) {
    this(au, startUrls, type);
    this.forceRecord = forceRecord;
  }

  /** Create UrlCounters with or without lists/maps.  Create ListCounters
   * for those lists that are already maintained as sets by the crawler (so
   * no dups); SetCounters for others. */
  void initCounters() {
    Configuration config = ConfigManager.getCurrentConfig();

    paramKeepOffHostExcludes = config.getInt(PARAM_KEEP_OFF_HOST_EXCLUDES,
					     DEFAULT_KEEP_OFF_HOST_EXCLUDES);
    if (paramKeepOffHostExcludes == -1) {
      paramKeepOffHostExcludes = Integer.MAX_VALUE;
    }

    String recordUrls = config.get(PARAM_RECORD_URLS, DEFAULT_RECORD_URLS);
    if (forceRecord != null && !recordUrls.equalsIgnoreCase(ALL_URLS)) {
      recordUrls += forceRecord;
    }
    if (paramRecordUrls == null || !paramRecordUrls.equals(recordUrls)) {
      fetched = newListCounter("fetched", recordUrls);
      excluded = newSetCounter("excluded", recordUrls);
      notModified = newListCounter("notModified", recordUrls);
      parsed = newListCounter("parsed", recordUrls);
      sources = newListCounter("source", recordUrls);
      pending = newSetCounter("pending", recordUrls);
      errors = newMapCounter("error", recordUrls);
      paramRecordUrls = recordUrls;
    }
  }

  /** Called after crawl ends and lists/counts are no longer needed by any
   * daemon processing.  Currently deletes or trims in-core structures.
   * Eventually should write lists to disk, delete in-core struct. */
  public void sealCounters() {
    String keepUrls = CurrentConfig.getParam(PARAM_KEEP_URLS,
					     DEFAULT_KEEP_URLS);
    log.debug2("sealCounters(" + keepUrls + ")");
    fetched = fetched.seal(isType("fetched", keepUrls));
    excluded = excluded.seal(isType("excluded", keepUrls));
    notModified = notModified.seal(isType("notModified", keepUrls));
    parsed = parsed.seal(isType("parsed", keepUrls));
    sources = sources.seal(isType("sources", keepUrls));
    pending = pending.seal(isType("pending", keepUrls));
    errors = errors.seal(isType("errors", keepUrls));
    boolean keepMime = isType("mime", keepUrls);
    for (Iterator iter = mimeCounts.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry ent = (Map.Entry)iter.next();
      ent.setValue(((UrlCount)ent.getValue()).seal(keepMime));
    }
  }

  private static synchronized String nextIdx() {
    return Integer.toString(++ctr);
  }

  /** Return true if we should record URLs of type <code>type</code>,
   * according to the param value in <code>recordTypes</code> */
  static boolean isType(String type, String urlTypes) {
    return (ALL_URLS.equalsIgnoreCase(urlTypes) ||
	    StringUtil.indexOfIgnoreCase(urlTypes, type) >= 0);
  }

  /** Make a list counter, with a list if specified by config */
  static UrlCount newListCounter(String type, String recordTypes) {
    if (isType(type, recordTypes)) {
      return new UrlCountWithList();
    } else {
      return new UrlCount();
    }
  }

  /** Make a set counter, with a set if specified by config */
  static UrlCount newSetCounter(String type, String recordTypes) {
    if (isType(type, recordTypes)) {
      return new UrlCountWithSet();
    } else {
      return new UrlCount();
    }
  }

  /** Make a map counter, with a map if specified by config */
  static UrlCount newMapCounter(String type, String recordTypes) {
    if (isType(type, recordTypes)) {
      return new UrlCountWithMap();
    } else {
      return new UrlCount();
    }
  }

  /** Return key for this CrawlerStatus */
  public String getKey() {
    return key;
  }

  // Scaler stats: start, end, error, bytes, etc.

  public String getType() {
    return type;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  /** Signal that crawl has started.  Config'ed counter type is checked
  again here because it's easier for tests */
  public void signalCrawlStarted() {
    startTime = TimeBase.nowMs();
    setCrawlStatus(Crawler.STATUS_ACTIVE);
    initCounters();
  }

  /**
   * @return time at which this crawl began or -1 if it hasn't started yet
   */
  public long getStartTime() {
    return startTime;
  }

  /** Signal that crawl has ended. */
  public void signalCrawlEnded() {
    endTime = TimeBase.nowMs();
    if (!isCrawlError()) {
      setCrawlStatus(Crawler.STATUS_SUCCESSFUL);
    }
  }

  /**
   * @return time at which this crawl ended or -1 if it hasn't ended yet
   */
  public long getEndTime() {
    return endTime;
  }

  public void setCrawlStatus(int status) {
    setCrawlStatus(status, null);
  }

  public void setCrawlStatus(int status, String message) {
    this.status = status;
    this.statusMessage = message;
  }

  /** Return true if crawl hasn't started yet */
  public boolean isCrawlWaiting() {
    return (startTime == -1);
  }

  /** Return true if crawl is active */
  public boolean isCrawlActive() {
    return (startTime != -1) && (endTime == -1);
  }

  /** Return true if any error has been recorded */
  public boolean isCrawlError() {
    switch (status) {
    case Crawler.STATUS_UNKNOWN:
    case Crawler.STATUS_SUCCESSFUL:
    case Crawler.STATUS_ACTIVE:
    case Crawler.STATUS_QUEUED:
      return false;
    default:
      return true;
    }
  }

  public int getCrawlStatus() {
    return status;
  }

  /** Return current status of crawl.  If crawl is still running, this will
   * be "Active" even if an error has occurred */
  public String getCrawlStatusMsg() {
    if (startTime != -1 && endTime == -1) {
      return getDefaultMessage(Crawler.STATUS_ACTIVE);
//     } else if (startTime == -1 && status != null) {
//       return getDefaultMessage(Crawler.STATUS_QUEUED);
    } else {
      return getCrawlErrorMsg();
    }
  }

  /** Return crawl error message, even if crawl is still running */
  public String getCrawlErrorMsg() {
    if (statusMessage != null) {
      return statusMessage;
    } else {
      return getDefaultMessage(status);
    }
  }

  public static String getDefaultMessage(int code) {
    String msg = DEFAULT_MESSAGES.get(code);
    if (msg == null) {
      return "Unknown code " + code;
    }
    return msg;
  }

  /** Increment counter of total bytes fetched */
  public synchronized void addContentBytesFetched(long size) {
    contentBytesFetched += size;
  }

  public long getContentBytesFetched() {
    return contentBytesFetched;
  }

  // URL set stats

  public Collection getStartUrls() {
    return startUrls;
  }

  // Sources

  /** Add to the list of sources for the crawl (e.g., Publisher) */
  public synchronized void addSource(String source) {
    sources.addToList(source);
  }

  public UrlCount getSourcesCtr() {
    return sources;
  }

  public synchronized List getSources() {
    return sources.getList();
  }

  // Fetched

  public synchronized void signalUrlFetched(String url) {
    fetched.addToList(url);
  }

  public UrlCount getFetchedCtr() {
    return fetched;
  }

  /**
   * Return the number of urls that have been fetched by this crawler
   * @return number of urls that have been fetched by this crawler
   */
  public synchronized int getNumFetched() {
    return fetched.getCount();
  }

  public synchronized List getUrlsFetched() {
    return fetched.getList();
  }

  // Excluded

  public synchronized void signalUrlExcluded(String url) {
    if (excluded.hasList() && isOffHost(url)) {
      if (includedExcludes >= paramKeepOffHostExcludes) {
	excludedExcludes++;
      } else {
	int cnt = excluded.getCount();
	excluded.addToList(url);
	if (cnt != excluded.getCount()) {
	  includedExcludes++;
	}
      }
    } else {
      excluded.addToList(url);
    }
  }

  public UrlCount getExcludedCtr() {
    return excluded;
  }

  public int getExcludedExcludes() {
    return excludedExcludes;
  }

  private boolean isOffHost(String url) {
    Collection<String> stems = au.getUrlStems();
    if (stems == null) {
      return false;
    }
    for (String stem : stems) {
      if (StringUtil.startsWithIgnoreCase(url, stem)) {
	return false;
      }
    }
    return true;
  }

  public int getNumExcludedExcludes() {
    return excludedExcludes;
  }

  /**
   * Return the number of urls that have been excluded because they didn't 
   * match the crawl rules
   * @return number of urls that have been excluded because they didn't 
   * match the crawl rules
   */
  public synchronized int getNumExcluded() {
    return excluded.getCount();
  }

  public synchronized List getUrlsExcluded() {
    return excluded.getList();
  }

  // Not Modified

  public synchronized void signalUrlNotModified(String url) {
    notModified.addToList(url);
  }

  public UrlCount getNotModifiedCtr() {
    return notModified;
  }

  /**
   * Return the number of urls whose GETs returned 304 not modified
   * @return number of urls whose contents were not modified
   */
  public synchronized int getNumNotModified() {
    return notModified.getCount();
  }

  public synchronized List getUrlsNotModified() {
    return notModified.getList();
  }

  // Pending

  /**
   *  add url to the list of pending urls
   */
  public synchronized void addPendingUrl(String url) {
    pending.addToList(url);
  }
    
  /**
   * remove one url element from the list of the pending urls
   */
  public synchronized void removePendingUrl(String url) {
    pending.removeFromList(url);
  }
    
  public UrlCount getPendingCtr() {
    return pending;
  }

  /**
   * @return number of urls that are pending  
   * to be handled by the crawler 
   */
  public synchronized int getNumPending() {
    return pending.getCount();
  }

  /**
   * @return list of the urls that are pending
   */
  public synchronized List getUrlsPending() {
    return pending.getList();
  }

  // Parsed

  public synchronized void signalUrlParsed(String url) {
    parsed.addToList(url);
  }

  public UrlCount getParsedCtr() {
    return parsed;
  }

  /**
   * Return the number of urls that have been parsed by this crawler
   * @return number of urls that have been parsed by this crawler
   */
  public synchronized int getNumParsed() {
    return parsed.getCount();
  }

  public synchronized List getUrlsParsed() {
    return parsed.getList();
  }
    
  // Errors

  public synchronized void signalErrorForUrl(String url, String error) {
    errors.addToMap(url, error);
  }

  public UrlCount getErrorCtr() {
    return errors;
  }

  public synchronized int getNumUrlsWithErrors() {
    return errors.getCount();
  }

  public synchronized String getErrorForUrl(String url) {
    return (String)(errors.getMap().get(url));
  }

  /**
   * @return map of the urls that couldn't be fetched due to errors and the
   * error they got
   */
  public synchronized Map getUrlsWithErrors() {
    return errors.getMap();
  }

  // MIME types

  public synchronized void signalMimeTypeOfUrl(String mimeType,
					       String url) {
    if (mimeType == null) return;      
    UrlCount ctr = (UrlCount)mimeCounts.get(mimeType);  
    if (ctr == null) {          
      ctr = newListCounter("mime", paramRecordUrls);
      mimeCounts.put(mimeType, ctr);
    }
    ctr.addToList(url);     
  }
 
  public UrlCount getMimeTypeCtr(String mimeType) {
    return (UrlCount)mimeCounts.get(mimeType);  
  }

  /**
   * @return list of the mime types
   */
  public synchronized Collection getMimeTypes() {
    if (isCrawlActive()) {   
      return new ArrayList(mimeCounts.keySet());
    } else {
      return mimeCounts.keySet();
    }
  }

  /**
   * @return the number of different types of mime types
   */
  public synchronized int getNumOfMimeTypes() {
    return mimeCounts.size();               
  }
    
  /**
   * @return list of urls with this mime-type found during a crawl
   */
  public synchronized List getUrlsOfMimeType(String mimeType) {
    UrlCount ctr = (UrlCount)mimeCounts.get(mimeType);  
    if (ctr != null) {          
      return ctr.getList(); 
    }
    return Collections.EMPTY_LIST;
  }

  /**
   * @return number of urls with this mime-type found during a crawl
   */
  public synchronized int getNumUrlsOfMimeType(String mimeType) {
    UrlCount ctr = (UrlCount)mimeCounts.get(mimeType);  
    if (ctr != null) {          
      return ctr.getCount(); 
    }
    return 0;  
  }

  /** Maintain a count and optional collection (list/set/map).  If
  collection is present, seal() will make it immutable and possibly
  condense it.  Set or map count may behave differently if collection is
  not present, as redundant operations won't be detected. */

  public static class UrlCount {
    protected int cnt = 0;	     // set negative minus 1 to mark sealed

    protected UrlCount() {
    }

    protected UrlCount(int initCnt) {
      cnt = initCnt;
    }

    public int getCount() {
      if (hasCollection() && getCollection() != null) {
	return getCollSize();
      }
      if (cnt >= 0) return cnt;
      return -1 - cnt;
    }

    void addToList(String s) {
      chkUpdate();
      cnt++;
    }

    void removeFromList(String s) {
      chkUpdate();
      if (cnt > 0) cnt--;
    }

    void addToMap(String key, String val) {
      chkUpdate();
      cnt++;
    }

    public boolean hasList() {
      return false;
    }

    public boolean hasMap() {
      return false;
    }

    public boolean hasCollection() {
      return hasList() || hasMap();
    }

    public List getList() {
      return Collections.EMPTY_LIST;
    }

    public Map getMap() {
      return Collections.EMPTY_MAP;
    }

    Object getCollection() {
      return null;
    }

    int getCollSize() {
      return 0;
    }

    public UrlCount seal(boolean keepColl) {
      if (!isSealed()) {
	cnt = -1 - cnt;
      }
      return this;
    }

    public boolean isSealed() {
      return cnt < 0;
    }

    protected void chkUpdate() {
      if (isSealed()) {
	throw
	  new IllegalStateException("Can't modify counter after it's sealed");
      }
    }
  }

  public static class UrlCountWithList extends UrlCount {
    ArrayList lst;

    UrlCountWithList() {
      super();
    }

    UrlCountWithList(Collection init) {
      super();
      if (init != null) {
	lst = new ArrayList(init.size());
	lst.addAll(init);
      }
    }

    void addToList(String s) {
      super.addToList(s);
      if (lst == null) {
	lst = new ArrayList();
      }
      lst.add(s);
    }

    void removeFromList(String s) {
      super.removeFromList(s);
      lst.remove(s);
    }

    public boolean hasList() {
      return true;
    }

    public List getList() {
      if (lst == null) {
	return Collections.EMPTY_LIST;
      }
      if (isSealed()) {
	return lst;
      }
      return new ArrayList(lst);
    }

    Object getCollection() {
      return lst;
    }

    int getCollSize() {
      return lst.size();
    }

    public UrlCount seal(boolean keepColl) {
      super.seal(keepColl);
      if (keepColl) {
	if (lst != null) {
	  lst.trimToSize();
	}
	return this;
      } else {
	return new UrlCount((lst != null) ? lst.size() : 0);
      }
    }
  }

  public static class UrlCountWithSet extends UrlCount {
    Set set;

    void addToList(String s) {
      super.addToList(s);
      if (set == null) {
	set = new ListOrderedSet();
      }
      set.add(s);
      if (set.size() != getCount()) {
	log.warning("Added " + s + ", UrlSet size: " + getCount() +
		    ", s.b. " + set.size() + ", " + set);
      }
    }

    void removeFromList(String s) {
      super.removeFromList(s);
      if (set == null) {
	return;
      }
      set.remove(s);
      if (set.size() != getCount()) {
	log.warning("Removed " + s + ", UrlSet size: " + getCount() +
		    ", s.b. " + set.size() + ", " + set);
      }
    }

    public boolean hasList() {
      return true;
    }

    public List getList() {
      if (set == null) {
	return Collections.EMPTY_LIST;
      }
      return new ArrayList(set);
    }

    Object getCollection() {
      return set;
    }

    int getCollSize() {
      return set.size();
    }

    public UrlCount seal(boolean keepColl) {
      super.seal(keepColl);
      if (keepColl) {
	return new UrlCountWithList(set);
      } else {
	return new UrlCount((set != null) ? set.size() : 0);
      }
    }
  }

  public static class UrlCountWithMap extends UrlCount {
    LinkedMap map;

    void addToMap(String key, String val) {
      super.addToMap(key, val);
      if (map == null) {
	map = new LinkedMap();
      }
      map.put(key, val);
    }

    public boolean hasMap() {
      return true;
    }

    public Map getMap() {
      if (map == null) {
	return Collections.EMPTY_MAP;
      }
      if (isSealed()) {
	return map;
      }
      return new LinkedMap(map);
    }

    Object getCollection() {
      return map;
    }

    int getCollSize() {
      return map.size();
    }

    public UrlCount seal(boolean keepColl) {
      super.seal(keepColl);
      if (keepColl) {
	return this;
      } else {
	return new UrlCount((map != null) ? map.size() : 0);
      }
    }
  }

  public String toString() {
    return "[CrawlerStatus " + key + "]";
  }
}
