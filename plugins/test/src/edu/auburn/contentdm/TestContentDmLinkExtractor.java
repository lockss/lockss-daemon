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

package edu.auburn.contentdm;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.extractor.*;

public class TestContentDmLinkExtractor extends LinkExtractorTestCase {

  public String getMimeType() {
    return "text/xml";
  }

  public String getUrl() {
    return "http://www.foo.com/test.xml";
  }

  public LinkExtractorFactory getFactory() {
    return new DublinCoreLinkExtractor.Factory();
  }

  /** This is an error for Dublin Core; supress this test */
  @Override
  public void testEmptyFileReturnsNoLinks() throws Exception {
  }

  /**
   * Test that a single links is found.
   */
  public void testOneUrl() throws Exception {
    try {    
      Set urls = SetUtil.set("http://www.foo.com/blah.jpg");
      assertEquals(urls, extractUrls(constructValidRDF(urls)));
    } catch (Throwable ex) {
      fail("", ex);
    }
  }
  
  public String constructValidRDF(Collection<String> urlList) {
    return constructRDF(urlList, true);
  }
  
  private String constructRDF(Collection<String> urlList, boolean withIdentifiers) {
    StringBuilder sb = new StringBuilder("<?xml version=\"1.0\"?>\n");
    sb.append("<!DOCTYPE rdf:RDF SYSTEM \"http://purl.org/dc/schemas/dcmes-xml-20000714.dtd\">\n");
    sb.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
              "         xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");
    
    for (String url : urlList) {
      sb.append(constructResource(url, withIdentifiers));
    }
    
    sb.append("</rdf:RDF>");
    return sb.toString();
  }
  
  private String constructResource(String url) {
    return constructResource(url, true);
  }
  
  private String constructResource(String url, boolean includeIdentifier) {
    StringBuilder sb =
      new StringBuilder("  <rdf:Description about=\"" + url + "\">\n");
    sb.append("    <dc:title>Title</dc:title>\n");
    sb.append("    <dc:creator>Creator</dc:creator>\n");
    sb.append("    <dc:description>Description</dc:description>\n");
    sb.append("    <dc:publisher>Publisher</dc:publisher>\n");
    sb.append("    <dc:contributor>Contributor</dc:contributor>\n");
    sb.append("    <dc:date>2006-05-18</dc:date>\n");
    sb.append("    <dc:type>Image</dc:type>\n");
    sb.append("    <dc:format>jpeg</dc:format>\n");
    if (includeIdentifier) {
      sb.append("    <dc:identifier>" + url + "</dc:identifier>\n");
    }
    sb.append("    <dc:source>Source</dc:source>\n");
    sb.append("    <dc:language>eng</dc:language>\n");
    sb.append("    <dc:relation>Relation</dc:relation>\n");
    sb.append("    <dc:coverage>Coverage</dc:coverage>\n");
    sb.append("    <dc:rights>In the public domain.</dc:rights>\n");
    sb.append("  </rdf:Description>");
    return sb.toString();
  }

}
