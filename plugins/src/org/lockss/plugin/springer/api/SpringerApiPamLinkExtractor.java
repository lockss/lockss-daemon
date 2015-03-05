/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer.api;

import static org.lockss.plugin.springer.api.SpringerApiCrawlSeed.logUrl;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.*;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class SpringerApiPamLinkExtractor implements LinkExtractor {

  protected static final XPathExpression TOTAL;
  protected static final XPathExpression PAGE_LENGTH;
  protected static final XPathExpression START;
  protected static final XPathExpression ARTICLE;
  protected static final XPathExpression DOI;
  protected static final XPathExpression ABSTRACT;
  protected static final XPathExpression HTML;
  protected static final XPathExpression PDF;
  
  static {
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      xpath.setNamespaceContext(new OneToOneNamespaceContext()
                                .put("dc", "http://purl.org/dc/elements/1.1/")
                                .put("pam", "http://prismstandard.org/namespaces/pam/2.0/")
                                .put("prism", "http://prismstandard.org/namespaces/basic/2.0/")
                                .put("xhtml", "http://www.w3.org/1999/xhtml"));
      START = xpath.compile("/response/result/start");
      PAGE_LENGTH = xpath.compile("/response/result/pageLength");
      TOTAL = xpath.compile("/response/result/total");
      ARTICLE = xpath.compile("/response/records/pam:message/xhtml:head/pam:article");
      DOI = xpath.compile("prism:doi");
      ABSTRACT = xpath.compile("prism:url[not(@format)]");
      HTML = xpath.compile("prism:url[@format='html']");
      PDF = xpath.compile("prism:url[@format='pdf']");
    }
    catch (XPathExpressionException xpee) {
      throw new ExceptionInInitializerError(xpee);
    }
  }
  
  protected boolean done;
  
  protected int start;
  
  protected int pageLength;
  
  protected int total;
  
  public SpringerApiPamLinkExtractor() {
    this.done = false;
    this.start = -1;
    this.pageLength = -1;
    this.total = -1;
  }
  
  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException {
    srcUrl = logUrl(srcUrl);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    InputSource inputSource = new InputSource(in);
    inputSource.setEncoding(encoding);
    Document doc = null;
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      doc = builder.parse(inputSource);
    }
    catch (ParserConfigurationException pce) {
      throw new IOException("Error configuring parser for " + srcUrl, pce);
    }
    catch (SAXException se) {
      throw new IOException("Error while parsing " + srcUrl, se);
    }

    NodeList articles = null;
    try {
      start = XPathUtil.evaluateNumber(START, doc).intValue();
      pageLength = XPathUtil.evaluateNumber(PAGE_LENGTH, doc).intValue();
      total = XPathUtil.evaluateNumber(TOTAL, doc).intValue();
      done = (start + pageLength > total);
      articles = XPathUtil.evaluateNodeSet(ARTICLE, doc);
    }
    catch (XPathExpressionException xpee) {
      throw new IOException("Error while parsing results for " + srcUrl, xpee);
    }

    Node article = null;
    String doi = null;
    for (int i = 0 ; i < articles.getLength() ; ++i) {
      article = articles.item(i);
      try {
        doi = XPathUtil.evaluateString(DOI, article);
        if (StringUtils.isEmpty(doi)) {
          continue; // FIXME log?
        }
      }
      catch (XPathExpressionException xpee) {
        throw new IOException(String.format("Error while parsing stanza for %s in %s",
                                            doi == null ? "first DOI" : "DOI immediately after " + doi,
                                            srcUrl),
                              xpee);
      }
      try {
        processAbstract(au, cb, doi, XPathUtil.evaluateString(ABSTRACT, article));
        processHtml(au, cb, doi, XPathUtil.evaluateString(HTML, article));
        processPdf(au, cb, doi, XPathUtil.evaluateString(PDF, article));
      }
      catch (XPathExpressionException xpee) {
        throw new IOException(String.format("Error while parsing stanza for DOI %s in %s",
                                            doi,
                                            srcUrl),
                              xpee);
      }
    }
  }
  
  public void processAbstract(ArchivalUnit au, Callback cb, String doi, String url) {
    if (!StringUtils.isEmpty(url)) {
      String absUrl = String.format("%sarticle/%s",
                                    au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
                                    doi);
      cb.foundLink(absUrl);
    }
  }
  
  public void processHtml(ArchivalUnit au, Callback cb, String doi, String url) {
    if (!StringUtils.isEmpty(url)) { 
      String htmlUrl = String.format("%sarticle/%s/fulltext.html",
                                     au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
                                     doi);
      cb.foundLink(htmlUrl);
    }
  }
  
  public void processPdf(ArchivalUnit au, Callback cb, String doi, String url) {
    if (!StringUtils.isEmpty(url)) { 
      String pdfUrl = String.format("%sarticle/%s/fulltext.pdf",
                                    SpringerApiCrawlSeed.CDN_URL,
                                    doi);
      cb.foundLink(pdfUrl);
    }
  }

  public boolean isDone() {
    return done;
  }

  public int getTotal() {
    return total;
  }

  public int getStart() {
    return start;
  }

  public int getPageLength() {
    return pageLength;
  }
  
}
