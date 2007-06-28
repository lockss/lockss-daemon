/*
 * $Id: TestDublinCoreLinkExtractor.java,v 1.1 2007-06-28 07:14:24 smorabito Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import junit.framework.Assert;

import org.lockss.test.*;
import org.lockss.util.*;

public class TestDublinCoreLinkExtractor extends LockssTestCase {

  /**
   * <p>An implementation of {@link FoundUrlCallback} that always
   * fails by calling {@link Assert#fail(String)}.</p>
   */
  protected static class AlwaysFail implements LinkExtractor.Callback {
    public void foundLink(String url) {
      fail("Callback should not have been called");
    }
  }
  
  /**
   * <p>Implementation of LinkExtractor.Callback that appends URLs to a 
   * list.</p>
   */
  protected static class ListBuilderCallback implements LinkExtractor.Callback {
    private List list = null;

    public ListBuilderCallback(List addTo) {
      this.list = addTo;
    }

    public void foundLink(String url) {
      list.add(url);
    }
  }
  
  /**
   * Test that a single links is found.
   */
  public void testOneUrl() throws Exception {
    List urls = new ArrayList();
    urls.add("http://www.foo.com/blah.jpg");
    String rdf = constructValidRDF(urls);
    
    // Construct DCLE
    List parsedUrls = new ArrayList();
    DublinCoreLinkExtractor dcle = new DublinCoreLinkExtractor();
    dcle.extractUrls(null, new ByteArrayInputStream(rdf.getBytes()),
                     null, "http://www.foo.com/test.xml",
                     new ListBuilderCallback(parsedUrls));
    
    assertIsomorphic(urls, parsedUrls);
  }
  
  /**
   * Test that multiple links are found.
   */
  public void testMultipleUrls() throws Exception {
    List urls = new ArrayList();
    urls.add("http://www.foo.com/blah.jpg");
    urls.add("http://www.foo.com/blatch.jpg");
    urls.add("http://www.foo.com/burble.jpg");
    String rdf = constructValidRDF(urls);
    
    // Construct DCLE
    List parsedUrls = new ArrayList();
    DublinCoreLinkExtractor dcle = new DublinCoreLinkExtractor();
    dcle.extractUrls(null, new ByteArrayInputStream(rdf.getBytes()),
                     null, "http://www.foo.com/test.xml",
                     new ListBuilderCallback(parsedUrls));
    Collections.sort(urls);
    Collections.sort(parsedUrls);
    assertIsomorphic(urls, parsedUrls);
  }
  
  /**
   * Ensure that a Dublin Core RDF file with no links will not
   * cause any links to be found.
   * 
   * @throws Exception
   */
  public void testNoUrls() throws Exception {
    List urls = Collections.EMPTY_LIST;
    String rdf = constructValidRDF(urls);

    // Construct DCLE
    DublinCoreLinkExtractor dcle = new DublinCoreLinkExtractor();

    // This will fail if the callback finds any URLs
    dcle.extractUrls(null, new ByteArrayInputStream(rdf.getBytes()),
                     null, "http://www.foo.com/test.xml",
                     new AlwaysFail());
  }
  
  /**
   * The parser looks for <dc:identifier></dc:identifier> tags in the
   * source RDF.  If these aren't there, it shouldn't find any links.
   */
  public void testNoIdentifiersMeansNoLinks() throws Exception {
    List urls = new ArrayList();
    urls.add("http://www.foo.com/blah.jpg");
    urls.add("http://www.foo.com/blatch.jpg");
    urls.add("http://www.foo.com/burble.jpg");
    // Don't include identifiers (shouldn't have any links)
    String rdf = constructRDFWithoutIdentifiers(urls);
    
    // Construct DCLE
    List parsedUrls = new ArrayList();
    DublinCoreLinkExtractor dcle = new DublinCoreLinkExtractor();
    dcle.extractUrls(null, new ByteArrayInputStream(rdf.getBytes()),
                     null, "http://www.foo.com/test.xml",
                     new ListBuilderCallback(parsedUrls));

    // Should be empty
    assertEquals(0, parsedUrls.size());
  }
  
  public void testNullCallbackThrows() throws Exception {
    List urls = new ArrayList();
    urls.add("http://www.foo.com/blah.jpg");
    urls.add("http://www.foo.com/blatch.jpg");
    urls.add("http://www.foo.com/burble.jpg");
    String rdf = constructValidRDF(urls);
    DublinCoreLinkExtractor dcle = new DublinCoreLinkExtractor();
    try {
      dcle.extractUrls(null, new ByteArrayInputStream(rdf.getBytes()),
                       null, "http://www.foo.com/test.xml",
                       null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      ; // expected
    }
  }
  

  public void testNullInputStreamThrows() throws Exception {
    List urls = new ArrayList();
    urls.add("http://www.foo.com/blah.jpg");
    urls.add("http://www.foo.com/blatch.jpg");
    urls.add("http://www.foo.com/burble.jpg");
    String rdf = constructValidRDF(urls);

    List parsedUrls = new ArrayList();
    DublinCoreLinkExtractor dcle = new DublinCoreLinkExtractor();
    try {
      dcle.extractUrls(null, null,
                       null, "http://www.foo.com/test.xml",
                       new ListBuilderCallback(parsedUrls));
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      ; // expected
    }
  }

  public String constructRDFWithoutIdentifiers(List<String> urlList) {
    return constructRDF(urlList, false);    
  }

  public String constructValidRDF(List<String> urlList) {
    return constructRDF(urlList, true);
  }
  
  private String constructRDF(List<String> urlList, boolean withIdentifiers) {
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
