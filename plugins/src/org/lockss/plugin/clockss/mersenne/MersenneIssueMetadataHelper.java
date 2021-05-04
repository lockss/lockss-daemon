/*


 * $Id$
 */

/*

 Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.mersenne;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.*;

/**
 * While the individual articles appear to be of the JATS schema,
 * they've been organized in to a journal-issue set
 *  
 *  @author alexohlson
 */
public class MersenneIssueMetadataHelper
extends JatsPublishingSchemaHelper {
  private static final Logger log = Logger.getLogger(MersenneIssueMetadataHelper.class);


  /*
   * The only difference between JATS and this usage is that 
   * multiple articles are collected in to /journal-issue/body
   * above the articles are also "journal-meta" and "issue-meta"
   * but that information is available in each article as well (per JATS)
   */

  /*
   * Mersenne combines its jats-like article node in to a journal-issue/body
   * There is useful global information in the 
   * journal-issue/journal-meta
   * and
   * journal-issue/issue-meta
   * For now we're just using the issue-meta to get the issue
   * but it confirms the volume and publication year as well
   * 
   */
  
  static protected final String Issue_issue = "/journal-issue/issue-meta/issue";
  static protected final String Issue_volume = "/journal-issue/issue-meta/volume";
  static protected final String Issue_year = "/journal-issue/issue-meta/pub-date/year";
  static protected final String Special_JATS_date =  "front/article-meta/pub-date[@publication-format=\"electronic\"]/@iso-8601-date";
  
  static private final Map<String,XPathValue> Mersenne_globalMap = 
	      new HashMap<String,XPathValue>();
	  static {
		  Mersenne_globalMap.put(Issue_issue,XmlDomMetadataExtractor.TEXT_VALUE);
	    // only in earlier versions
		  Mersenne_globalMap.put(Issue_volume, XmlDomMetadataExtractor.TEXT_VALUE);
		  Mersenne_globalMap.put(Issue_year, XmlDomMetadataExtractor.TEXT_VALUE);
	  }	    

  /* In our case, we have multiple articles in one file */
  static private final String Issue_articleNode = "/journal-issue/body/article";
  /* override the generic for a more specific uri value */
  /* self_uri @content-type="application/pdf" @xlink:href="VALUE" */
  public static String Mersenne_pdf_uri = "front/article-meta/self-uri[@content-type = \"application/pdf\"]/@href";
 
  // add our extra mersenne specific pdf_uri to the map requested
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    Map<String, XPathValue> theMap = super.getArticleMetaMap();
    theMap.put(Mersenne_pdf_uri, XmlDomMetadataExtractor.TEXT_VALUE);
    theMap.put(Special_JATS_date, XmlDomMetadataExtractor.TEXT_VALUE);
    return theMap;
  }
  
  @Override
  public MultiValueMap getCookMap() {
	MultiValueMap theCookMap = super.getCookMap();
    return theCookMap;
  }


  /**
   * return JATS article map to identify xpaths of interest
   */
  @Override
  public String getArticleNode() {
    return Issue_articleNode;
  }
  
  
  /*
   * Mersenne has some issue information in the journal-issue metadata
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return Mersenne_globalMap;
  }


  /*
   * "kee_pdf_list.dat" is the list of PDF files that publisher would like to kept.
   * All the rest of the PDF files in the delivery should be ignored
   */
  public List<String> getKeptPDFFileList() throws IOException {

    int count = 0;
    String fname = "kept_pdf_list.dat";
    InputStream is = null;
    List<String> pdfList = new ArrayList<>();

    is = getClass().getResourceAsStream(fname);

    if (is == null) {
      throw new ExceptionInInitializerError("ClockssMersenneSourcePlugin external data file not found");
    }

    BufferedReader bufr = new BufferedReader(new InputStreamReader(is));
    
    String next_url = null;
    while ((next_url = bufr.readLine()) != null) {
      next_url = next_url.trim();
      pdfList.add(next_url.trim());
    }
    bufr.close();

    return pdfList;
  }
}
