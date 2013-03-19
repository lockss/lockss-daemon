/**
 * $Id: SitemapUrl.java,v 1.1 2013-03-19 18:42:23 ldoan Exp $
 */

/**

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

import org.lockss.util.Logger;

/** 
 * The SitemapUrl class represents a URL found in a Sitemap.
 * This class is modified from SourceForge open-source by Frank McCown.
 */
public class SitemapUrl {
	
  private static Logger log = Logger.getLogger(SitemapUrl.class);

  /** Allowed change frequencies */
  public enum ChangeFrequency {ALWAYS, HOURLY, DAILY,
                               WEEKLY, MONTHLY, YEARLY, NEVER};
                               
  private String url; // url found in Sitemap (required)
  private String lastModified; // When url was last modified (optional)
  private ChangeFrequency changeFreq; // How often the URL changes (optional)
  private double priority; // Value between [0.0 - 1.0] (optional)
  
  /**
   * Constructors
   */
  public SitemapUrl(String url) {
    setUrl(url);
  }
  
  public SitemapUrl(String url, String lastModified) {
    setUrl(url);
    setLastModified(lastModified);
  }

  public SitemapUrl(String url, String lastModified, 
                    String changeFreq, String priority) {
    setUrl(url);		
    setLastModified(lastModified);				
    setChangeFrequency(changeFreq);
    setPriority(priority);		
  }
	
  public SitemapUrl(String url, String lastModified, 
                    ChangeFrequency changeFreq, double priority) {
    setUrl(url);		
    setLastModified(lastModified);				
    setChangeFrequency(changeFreq);
    setPriority(priority);
  }
	
  /**
   * Getters
   */
  public String getUrl() {
    return url;
  }

  public String getLastModified() {
    return lastModified;
  }

  public ChangeFrequency getChangeFrequency() {
    return changeFreq;
  }

  public double getPriority() {
    return priority;
  }

  /**
   * Setters
   */
  public void setUrl(String url) {
    this.url = url;
  }
	
  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }
	
  /** Set the String's priority to a value between [0.0 - 1.0]
    (0.0 is used if the given priority is out of range). */
  private void setPriority(double priority) {
    
    /** Ensure proper value */
    if (priority < 0.0 || priority > 1.0) {
      this.priority = 0.0;
    }
    else {
      this.priority = priority;
    }
  }

  /** Set the String's priority to a value between [0.0 - 1.0]
    (0.0 is used if the given priority is out of range). */
  private void setPriority(String priority) {
    
    if (priority != null && priority.length() > 0) {
      try {
        setPriority(Double.parseDouble(priority));
      }
      catch (NumberFormatException e) {
        log.warning("Can't parse priority");
        setPriority(0.0);
      }
    }
    else {
      setPriority(0.0);
    }
  }
	
  private void setChangeFrequency(ChangeFrequency changeFreq) {
    this.changeFreq = changeFreq;
  }
	
  private void setChangeFrequency(String changeFreq) {
    
    if (changeFreq != null) {
      changeFreq = changeFreq.toUpperCase();
      this.changeFreq = ChangeFrequency.valueOf(changeFreq);
    } else {
      this.changeFreq = null;
    }
    
  } /** end setChangeFrequency */
			
  
  /**
   * Display string
   */
  public String toString() {
        
    StringBuilder sb = new StringBuilder();
    sb.append("[url=\"");
    sb.append(url);
    sb.append("\", lastMod=");
    sb.append(lastModified);
    sb.append(",changeFreq=");
    sb.append(changeFreq);
    sb.append(",priority=");
    sb.append(priority);
    sb.append("]");
    return sb.toString();
  }
	
} /** end SitemapUrl */