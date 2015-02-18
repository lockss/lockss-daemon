/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

/*
 * Some portion of this code is Copyright.
 * 
 * SitemapUrl.java - Represents a URL found in a Sitemap 
 *  
 * Copyright 2009 Frank McCown
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.lockss.extractor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/** 
 * Represents a &lt;sitemap&gt; or &lt;url&gt; node found in a Sitemap,
 * according to the protocol in http://www.sitemaps.org/protocol.
 * Modified from SourceForge open-source by Frank McCown.
 */
public class SitemapUrl {
	
  private static final Logger log = Logger.getLogger(SitemapUrl.class);

  /** Allowed change frequencies */
  public enum ChangeFrequency {ALWAYS, HOURLY, DAILY,
                               WEEKLY, MONTHLY, YEARLY, NEVER};
                               
  /** &lg;loc&gt; URL found in Sitemap (required) */         
  private String url = null;
  /** when the 's URL was last modified (optional) */
  private long lastModified = -1;
  /** How often the URL changes (optional) */
  private ChangeFrequency changeFreq = null;
  /** Value between [0.0 - 1.0] (optional) */
  private double priority = 0.0;
  
  /**
   * Constructs a SitemapUrl with url.
   * 
   * @param url the URL at &lt;loc&gt; tag
   * @throw IllegalArgumentException if url is null
   */
  SitemapUrl(String url) {
    if (url == null) {
      throw new IllegalArgumentException("url can not be null");
    }
    this.url = url;
  }
 
  /**
   * Constructs a SitemapUrl with url and last modified date of this URL.
   * 
   * @param url the URL at &lt;loc&gt; tag
   * @param lastModified last modified date
   */
   SitemapUrl(String url, String lastModified) {
    this(url);
    setLastModified(lastModified);
  }
  
  /**
   * Constructs a SitemapUrl with url, last modified date of this URL and
   * the change frequency.
   * 
   * @param url the URL at &lt;loc&gt; tag
   * @param lastModified -*-*lastModified last modified date
   * @param changeFreq a string change frequency
   */
  SitemapUrl(String url, String lastModified, String changeFreq) {
    this(url, lastModified);
    setChangeFrequency(changeFreq);
  }

  /**
   * Constructs a SitemapUrl with url, last modified date of this URL and
   * the change frequency.
   * 
   * @param url the URL at &lt;loc&gt; tag
   * @param lastModified lastModified last modified date
   * @param changeFreq an enum change frequency
   */
  SitemapUrl(String url, String lastModified, ChangeFrequency changeFreq) {
    this(url, lastModified);
    setChangeFrequency(changeFreq);
  }

  /**
   * Constructs a SitemapUrl with url, last modified date of this URL and
   * the priority.
   * 
   * @param url the URL at &lt;loc&gt; tag
   * @param lastModified lastModified last modified date
   * @param priority double value between [0.0 - 1.0]
   */
  SitemapUrl(String url, String lastModified, double priority) {
    this(url, lastModified);
    setPriority(priority);
  }

  /**
   * Constructs a SitemapUrl with url, last modified date of this URL,
   * the change frequency and the priority.
   * 
   * @param url the URL at &lt;loc&gt; tag
   * @param lastModified lastModified last modified date
   * @param changeFreq a string change frequency
   * @param priority double value between [0.0 - 1.0]
   */
  SitemapUrl(String url, String lastModified,
             String changeFreq, String priority) {
    this(url, lastModified);
    setChangeFrequency(changeFreq);
    setPriority(priority);		
  }  
  
  /**
   * Constructs a SitemapUrl with url, last modified date of this URL,
   * the change frequency and the priority.
   * 
   * @param url the URL at &lt;loc&gt; tag
   * @param lastModified lastModified last modified date
   * @param changeFreq an enum change frequency
   * @param priority value between [0.0 - 1.0]
   */
   SitemapUrl(String url, String lastModified, 
              ChangeFrequency changeFreq, double priority) {
    this(url, lastModified);
    setChangeFrequency(changeFreq);
    setPriority(priority);
  }
	
  /**
   * Returns the url of the SitemapUrl.
   * 
   * @return url the URL at at &lt;loc&gt; tag
   */
  public String getUrl() {
    return url;
  }

  /**
   * Returns the last modified date of the url.
   * 
   * @return lastModified the last modified date of the url
   */
  public long getLastModified() {
    return lastModified;
  }

  /**
   * Returns the change frequency.
   * 
   * @return changeFreq the change frequency
   */
  public ChangeFrequency getChangeFrequency() {
    return changeFreq;
  }

  /**
   * Returns the priority value between [0.0 - 1.0].
   * 
   * @return priority value between [0.0 - 1.0]
   */
  public double getPriority() {
    return priority;
  }
  
  /**
   * Set the last modified date with a long value.
   * 
   * @param lastModified the last modified date.
   */
  private void setLastModified(String lastModified) {
    // Date method getTime() gives the date in long value
    Date date = convertToDate(lastModified);
    if (date != null) {
      this.lastModified = date.getTime();
    } 
  }
  
  // 
  /**
   * The supported W3C date format (http://www.w3.org/TR/NOTE-datetime).
   */
  private static DateFormat dateFormats[] = {
    new SimpleDateFormat("yyyy-MM-dd"),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm+hh:00"),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm-hh:00"),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+hh:00"),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-hh:00"),
    /** Accept RSS dates */
    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
  };
  
  /**
   * Converts the given date (given in an acceptable DateFormat), 
   *
   * @param date the string date in W3C date format
   * @return null a Date object or null if the date is not in the correct format.
   */
  private static Date convertToDate(String date) {
    if (date != null) {			
      for (DateFormat df : dateFormats) {
        try {					
          return df.parse(date);
        }
        catch (ParseException e) {
          log.error("Error parsing date" + e);
          // do nothing - continue processing with other date formats
        }
      }			
    }		
    return null;
  }

  /**
   * Set the priority to a value between [0.0 - 1.0] with a double value.
   * 
   * @param priority a value between  [0.0 - 1.0]
   */
  private void setPriority(double priority) {
    if (priority < 0.0 || priority > 1.0) {
      this.priority = 0.0;
    }
    else {
      this.priority = priority;
    }
  }

  /**
   * Set the priority to a value between [0.0 - 1.0] with a string value.
   * 
   * @param priority a value between  [0.0 - 1.0]
   */
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
	
  /**
   * Set the change frequency with an enum value.
   * 
   * @param changeFreq a ChangeFrequency value.
   */
  private void setChangeFrequency(ChangeFrequency changeFreq) {
    this.changeFreq = changeFreq;
  }
	
  /**
   * Set the change frequency with a string value.
   * 
   * @param changeFreq a ChangeFrequency value.
   */
  private void setChangeFrequency(String changeFreq) {
    if (changeFreq != null) {
      this.changeFreq = ChangeFrequency.valueOf(changeFreq.toUpperCase());
    }
  }
  
  /**
   * Implements hashCode() using all the member values as used in equals().
   * 
   * @return a hash
   */
  @Override
  public int hashCode() {
    HashCodeBuilder hcb = new HashCodeBuilder(); 
    hcb.append(url);
    hcb.append(lastModified);
    hcb.append(changeFreq);
    hcb.append(priority);
    return hcb.toHashCode();
  }
  
  /**
   * Compare two SitemapUrl objects for equality.
   * 
   * @param obj the other SitemapUrl object
   * @return true if this and obj's members are equals
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SitemapUrl) {
      SitemapUrl sUrl = (SitemapUrl)obj;
      if (StringUtil.equalStrings(this.url, sUrl.getUrl())
        && this.lastModified == sUrl.getLastModified()
        && this.changeFreq == sUrl.getChangeFrequency()
        && this.priority == sUrl.getPriority()) {
        return true;
      }
    }
    return false; 
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[SitemapUrl: ");
    sb.append(url);
    sb.append(", lastMod=");
    sb.append(lastModified);
    sb.append(", changeFreq=");
    sb.append(changeFreq);
    sb.append(", priority=");
    sb.append(priority);
    sb.append("]");
    return sb.toString();
  }
	
}