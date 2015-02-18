/**
 * $Id$
 */

/**

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.medknow;

import java.io.*;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.extractor.Sitemap.SitemapType;
import org.xml.sax.SAXException;

/**
 * Creates instances of a LinkExtractor that parses Medknow
 * sitemap (xml) files to extract "links" to be collected.
 * Implements extractUrls() method.
 */
public class MedknowSitemapLinkExtractor implements LinkExtractor {
    
  protected static Logger log =
      Logger.getLogger(MedknowSitemapLinkExtractor.class);
  
  private ArchivalUnit au;
  
  /**
   * Check if the year in AU matches with the year 
   * from xml <loc> node value (journal issue string).
   */
  public void processJournalIssueLink(Sitemap sitemap,
      LinkExtractor.Callback cb) {
    
    String baseUrl = au.getProperties().getString(ConfigParamDescr.BASE_URL.getKey()); 
    int year = au.getProperties().getInt(ConfigParamDescr.YEAR.getKey()); 
    String issn = au.getProperties().getString(ConfigParamDescr.JOURNAL_ISSN.getKey());
    String volume_name = au.getProperties().getString(ConfigParamDescr.VOLUME_NAME.getKey());
      
    /** Pattern PATTERN_ISSUE_TOC = Pattern.compile(".+sitemap_([0-9]{4})_([0-9])_([0-9]).+$", Pattern.CASE_INSENSITIVE); */
    Pattern PATTERN_ISSUE_TOC = Pattern.compile(".+sitemap_" + (year) + "_" + (volume_name) + "_([0-9]).+$", Pattern.CASE_INSENSITIVE);

    Collection<SitemapUrl> sitemapUrlMap = sitemap.getUrls();
    for (SitemapUrl sitemapUrlObj : sitemapUrlMap) {
      
      String issueStr =  sitemapUrlObj.getUrl().toString();
      
      Matcher issueTocMat = PATTERN_ISSUE_TOC.matcher(issueStr);
      if (!issueTocMat.find()) {
        continue;
      }
      
      /** Before transformmed: // http://www.afrjpaedsurg.org/sitemap_2012_9_1.xml */
      cb.foundLink(issueStr);
      log.debug(issueStr);
        
      /** Reconstruct journal issue url from sitemap <loc> value. */
      String issueUrl = issueTocMat.replaceFirst(baseUrl + "showBackIssue\\.asp\\?issn=" + issn + ";year=" + year + ";volume=" + volume_name + ";issue=$1");
      log.debug(issueUrl);
      
      /** After transformed:  // http://www.afrjpaedsurg.org/showBackIssue.asp?issn=0189-6725;year=2012;volume=9;issue=1 */
      cb.foundLink(issueUrl);
      
    }
    
  } /** end processJournalIssueLink */
  

  /**
   * Medknow sitemap uses <sitemapindex> structure.
   */
  public Sitemap processSitemap(InputStream xmlSitemapStream, String encoding) 
      throws SitemapException, SAXException, IOException {
    
    log.debug3("in processSitemapIndex()");
  
    /** Process xml <sitemapindex> input stream. */
    SitemapParser parser = new SitemapParser();
    
    /** return Sitemap object */
    return (parser.processXmlSitemap(xmlSitemapStream, encoding));
    
  } /** end processSitemap */
  
  /**
   * Implements extractUrls method from interface LinkExtractor
   * input XML should be <base_url>/sitemap.xml.
   */
  public void extractUrls(ArchivalUnit au, InputStream xmlSitemapStream,
      String encoding, String srcUrl, LinkExtractor.Callback cb) {
    
    log.debug3("in extractUrl()");
    
    this.au = au;
      
    if ( xmlSitemapStream == null) {
      throw new IllegalArgumentException("Called with null xml stream");
    }
    
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    
    try {
      /** sitemapIndex contains the sitemap nodes matching the AU's param year. */
      Sitemap sitemapObj = processSitemap(xmlSitemapStream, encoding);
     
      SitemapType sitemapType = sitemapObj.getType();
      if (sitemapType != SitemapType.INDEX) {
        log.error("Expected sitemapindex type, but got " + sitemapType);
        return;
      }
      
      /** Set the journal issue links, then send them 
        to link extractor through call back function. */
      processJournalIssueLink(sitemapObj, cb);
      
    } catch (Exception e) {
      log.error("Error processing Sitemap", e);
    }
    
  } /** end extractUrls */
    
} /** end MedknowSitemapLinkExtractor */

