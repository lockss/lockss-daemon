/*
 * $Id: Crawler.java,v 1.42 2006-10-09 20:32:37 adriz Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
nto use, copy, modify, merge, publish, distribute, sublicense, and/or sell
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

package org.lockss.daemon;

import java.io.*;
import java.io.BufferedInputStream;
import java.util.*;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.collections.map.LinkedMap;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This interface is implemented by the generic LOCKSS daemon.
 * The plug-ins use it to call the crawler to actually fetch
 * content.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */
public interface Crawler {

  public static final int NEW_CONTENT = 0;
  public static final int REPAIR = 1;
  public static final int BACKGROUND = 2;
  public static final int OAI = 3;

  public static final String STATUS_QUEUED = "Pending";
  public static final String STATUS_ACTIVE = "Active";
  public static final String STATUS_SUCCESSFUL = "Successful";
  public static final String STATUS_ERROR = "Error";
  public static final String STATUS_ABORTED = "Aborted";
  public static final String STATUS_WINDOW_CLOSED = "Crawl window closed";
  public static final String STATUS_FETCH_ERROR = "Fetch error";
  public static final String STATUS_NO_PUB_PERMISSION = "No permission from publisher";
  public static final String STATUS_PLUGIN_ERROR = "Plugin error";
  public static final String STATUS_REPO_ERR = "Repository error";
  //public static final String STATUS_UNKNOWN = "Unknown";

  /**
   * Initiate a crawl starting with all the urls in urls
   * @return true if the crawl was successful
   */
  public boolean doCrawl();


  /**
   * Return the AU that this crawler is crawling within
   * @return the AU that this crawler is crawling within
   */
  public ArchivalUnit getAu();

  /**
   * Returns the type of crawl
   * @return crawl type
   */
  public int getType();

  /**
   * Return true iff the crawl tries to collect the entire AU content.
   */
  public boolean isWholeAU();

  /**
   * aborts the running crawl
   */
  public void abortCrawl();


  /**
   * Set a watchdog that should be poked periodically by the crawl
   * @param wdog the watchdog
   */
  public void setWatchdog(LockssWatchdog wdog);

  /**
   * Returns an int representing the status of this crawler
   */
  public Crawler.Status getStatus();


  public static class Status {
    private static int ctr = 0;

    private String key;
    protected long startTime = -1;
    protected long endTime = -1;
    protected String crawlError = null;
    protected Collection startUrls = null;
    protected ArchivalUnit au = null;
    protected String type;
    private long contentBytesFetched = 0;

    protected Map urlsWithErrors = new LinkedMap();
    protected Set urlsFetched = new ListOrderedSet();
    protected Set urlsExcluded = new ListOrderedSet();
    protected Set urlsNotModified = new ListOrderedSet();
    protected Set urlsParsed = new ListOrderedSet();

    protected Set sources = new ListOrderedSet();
    
    /* add -  class: public static class RecordMimeType include basic array: urlsArray and Int
     *  updateUrlsArrayOfMimeType will update based on Boolean recordUrl to update int-RecordMimeType.numOfUrls  or add also the url to 
     *  RecordMimeType.urlsArray
     *  ucontentTypeUrls  will map a mimeTypeKey to an object of RecordMimeType
    */
    
    protected Map mimeTypeUrls = new HashMap();/*MimeType - keep record of urls with the given type of mime-type */
    protected Crawler.Status.RecordMimeTypeUrls recordUrls = null;
    
    public Crawler.Status.RecordMimeTypeUrls getRecordMimeTypeUrls() {
         return recordUrls;
    }

    public Status(ArchivalUnit au, Collection startUrls, String type) {
      this.au = au;
      this.startUrls = startUrls;
      this.type = type;
      key = Integer.toString(nextIdx());
    }

    private static synchronized int nextIdx() {
      return ++ctr;
    }

    public String getKey() {
      return key;
    }

    /**
     * Return the time at which this crawl began
     * @return time at which this crawl began or -1 if it hadn't yet
     */
    public long getStartTime() {
      return startTime;
    }

    public void signalCrawlStarted() {
      startTime = TimeBase.nowMs();
    }

    /**
     * Return the time at which this crawl ended
     * @return time at which this crawl ended or -1 if it hadn't yet
     */
    public long getEndTime() {
      return endTime;
    }

    public void signalCrawlEnded() {
      endTime = TimeBase.nowMs();
    }

    /**
     * Return the number of urls that have been fetched by this crawler
     * @return number of urls that have been fetched by this crawler
     */
    public synchronized long getNumFetched() {
      return urlsFetched.size();
    }

    /**
     * Return the number of urls that have been excluded because they didn't 
     * match the crawl rules
     * @return number of urls that have been excluded because they didn't 
     * match the crawl rules
     */
    public synchronized long getNumExcluded() {
      return urlsExcluded.size();
    }

    /**
     * Return the number of urls whose GETs returned 304 not modified
     * @return number of urls whose contents were not modified
     */
    public synchronized long getNumNotModified() {
      return urlsNotModified.size();
    }

    public synchronized void signalUrlFetched(String url) {
      urlsFetched.add(url);
    }

    public synchronized void signalUrlExcluded(String url) {
      urlsExcluded.add(url);
    }

    public synchronized void signalUrlNotModified(String url) {
      urlsNotModified.add(url);
    }

    public synchronized void addContentBytesFetched(long size) {
      contentBytesFetched += size;
    }

    public long getContentBytesFetched() {
      return contentBytesFetched;
    }

    /**
     * @return hash of the urls that couldn't be fetched due to errors and the
     * error they got
     */
    public synchronized Map getUrlsWithErrors() {
      if (isCrawlActive()) {
	return new LinkedMap(urlsWithErrors);
      } else {
	return urlsWithErrors;
      }
    }

    public synchronized Collection getSources() {
      if (isCrawlActive()) {
	return new ArrayList(sources);
      } else {
	return sources;
      }
    }

    public synchronized long getNumUrlsWithErrors() {
      return urlsWithErrors.size();
    }

    public synchronized Collection getUrlsFetched() {
      if (isCrawlActive()) {
	return new ArrayList(urlsFetched);
      } else {
	return urlsFetched;
      }
    }

    public synchronized Collection getUrlsNotModified() {
      if (isCrawlActive()) {
	return new ArrayList(urlsNotModified);
      } else {
	return urlsNotModified;
      }
    }

    public synchronized Collection getUrlsExcluded() {
      if (isCrawlActive()) {
 	return new ArrayList(urlsExcluded);
      } else {
 	return urlsExcluded;
      }
    }

    public synchronized Collection getUrlsParsed() {
      if (isCrawlActive()) {
	return new ArrayList(urlsParsed);
      } else {
	return urlsParsed;
      }
    }
    
    /* return the list of the different types of content-mime types found from the map: mimeTypeUrls.keys */
    public synchronized Collection getMimeTypesVals() {
      if (isCrawlActive()) {  // consider switching between the 2 cases, if isactive ret the orig-set so if is updated during the crawl ->it's reflected ->seen by ui
        return new ArrayList( mimeTypeUrls.keySet() );
      } else {
        return  mimeTypeUrls.keySet();
      }
    }
    /* return the size of the list of the different types of mime types found from the map: (ArrayList)mimeTypeUrls.numberOfKeys */
    public synchronized long getNumOfMimeTypes() {
      return mimeTypeUrls.keySet().size();
    }
   /**
    *  check and update: mimeTypeUrls, 
    * if there is already an entry (list of string urls) for the given key: mimeType
    * if y return the list-ptr else create an empty list and return its pointer  
    */

    public synchronized void updateUrlsArrayOfMimeType(String keyMimeType,
                                                                String url , boolean keepUrl) {
      if (keyMimeType == null) return;      
      recordUrls = (RecordMimeTypeUrls)mimeTypeUrls.get(keyMimeType);                                    // if get returns null --> key is not there (there are no mappings to null)
      if ( recordUrls == null ) {          
        RecordMimeTypeUrls recordUrls = new RecordMimeTypeUrls();
        recordUrls.addCtUrls(url, keepUrl);
        mimeTypeUrls.put(keyMimeType, recordUrls);
      } else  {
        recordUrls.addCtUrls(url, keepUrl);
      }
    }
 
    /**
     * get urls from  mimeTypeUrls for the given key: mimeType
     */
     public synchronized Collection getUrlsArrayOfMimeType(String keyMimeType) {
       if ( mimeTypeUrls.containsKey(keyMimeType) ) {    
         RecordMimeTypeUrls recordUrls = (RecordMimeTypeUrls)mimeTypeUrls.get(keyMimeType);
         return recordUrls.urlsArray; 
       } else {
         return null;// return null, none urls of this content/mime type
       }
     }

     /**
      * Return the number of urls that have been found by this crawler
      *  with the given mime-type 
      * @return number of urls with the mime-type that have been by this crawler
      */
     public synchronized long getNumUrlsOfMimeType(String keyMimeType) {
       if ( mimeTypeUrls.containsKey(keyMimeType) ) {    
         RecordMimeTypeUrls recordUrls = (RecordMimeTypeUrls)mimeTypeUrls.get(keyMimeType);
         return recordUrls.numUrls; 
       } else {
         return 0;//  no urls of this content type
       }

     }

     /**
     * Return the number of urls that have been parsed by this crawler
     * @return number of urls that have been parsed by this crawler
     */
    public synchronized long getNumParsed() {
      return urlsParsed.size();
    }

     public synchronized void signalUrlParsed(String url) {
       urlsParsed.add(url);
     }

    public synchronized void addSource(String source) {
      sources.add(source);
    }


    public Collection getStartUrls() {
      return startUrls;
    }

    public boolean isCrawlWaiting() {
      return (startTime == -1);
    }

    public boolean isCrawlActive() {
      return (startTime != -1) && (endTime == -1);
    }

    public String getCrawlStatus() {
      if (startTime == -1) {
	return Crawler.STATUS_QUEUED;
      } else if (endTime == -1) {
	return Crawler.STATUS_ACTIVE;
      } else if (crawlError != null) {
	return crawlError;
      }
      return Crawler.STATUS_SUCCESSFUL;
    }

    public void setCrawlError(String crawlError) {
      this.crawlError = crawlError;
    }

    public synchronized void signalErrorForUrl(String url, String error) {
      urlsWithErrors.put(url, error);
    }

    public synchronized String getErrorForUrl(String url) {
      return (String)urlsWithErrors.get(url);
    }

    public String getCrawlError() {
      return crawlError;
    }

    public String getType() {
      return type;
    }

    public ArchivalUnit getAu() {
      return au;
    }

    public static class RecordMimeTypeUrls {
      protected int numUrls; // = 0;
      protected ArrayList urlsArray; // =  new ArrayList();     
    
      public RecordMimeTypeUrls() {
        this.numUrls = 0;
        this.urlsArray =  new ArrayList();
      }

      protected void addCtUrls(String url, boolean keepUrl) {
        numUrls++;
        if (keepUrl){
          urlsArray.add( url );
        }  
      }
    }
  
    public String toString() {
      return "[Crawler.Status " + key + "]";
    }

  }

  /**
   * Encapsulation for the methods that the PermissionMap needs from a
   * crawler
   *
   * @author troberts
   *
   */
  public static interface PermissionHelper {
    /**
     * Generate a URL cacher  for the given URL
     * @param url
     * @return UrlCacher for the given URL
     */
    public UrlCacher makeUrlCacher(String url);

    public BufferedInputStream resetInputStream(BufferedInputStream is, String url)
           throws IOException;

    public void refetchPermissionPage(String url) throws IOException;

    public Crawler.Status getCrawlStatus();

  }

}
