/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.springer.link;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException.UnknownExceptionException;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * <p>
 * A link extractor for Springer's PAM-based API responses (XML).
 * </p>
 * <p>
 * A simple SAX parser does not mind that the response has imperfect
 * namespace or schema/DTD declarations, but then the resulting tree cannot
 * be processed using XPath, so a simple pre-processing step is applied by
 * {@link PamRewritingReader} to make responses work with XPath. 
 * </p>
 * 
 * @since 1.67.5
 */
public class SpringerLinkPamLinkExtractor implements LinkExtractor {

  /**
   * <p>
   * An XPath expression to select the total number of records from the result
   * section of the response. (See static initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final XPathExpression TOTAL;

  /**
   * <p>
   * An XPath expression to select the page length from the result section of
   * the response. (See static initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final XPathExpression PAGE_LENGTH;
  
  /**
   * <p>
   * An XPath expression to select the starting index from the result section of
   * the response. (See static initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final XPathExpression START;
  
  /**
   * <p>
   * An XPath expression to select the articles from the records section of the
   * response. (See static initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final XPathExpression ARTICLE;
  
  /**
   * <p>
   * An XPath expression to select the DOI from an article. (See static
   * initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final XPathExpression DOI;
  
  /**
   * <p>
   * An XPath expression to select the nominal abstract URL from an article.
   * (See static initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final XPathExpression ABSTRACT;

  /**
   * <p>
   * An XPath expression to select the nominal full text HTML URL from an
   * article. (See static initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final XPathExpression HTML;

  /**
   * <p>
   * An XPath expression to select the nominal full text PDF URL from an
   * article. (See static initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final XPathExpression PDF;

  /*
   * STATIC INITIALIZER
   * 
   * May 2019 - namesapce_map is no longer used; instead create the DocumentBuilderFactory to
   * ignore namespaces
   * The XPATH definitions will work without the namespace portion unless doing a direct
   * string comparison of the node value.
   *
   * ARTICLE = xpath.compile("/response/records/pam:message/xhtml:head/pam:article");
   * DOI = xpath.compile("prism:doi");
   * ABSTRACT = xpath.compile("prism:url[not(@format)]");
   * HTML = xpath.compile("prism:url[@format='html']");
   * PDF = xpath.compile("prism:url[@format='pdf']");
   * 
   */
  static {
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      START = xpath.compile("/response/result/start");
      PAGE_LENGTH = xpath.compile("/response/result/pageLength");
      TOTAL = xpath.compile("/response/result/total");
      ARTICLE = xpath.compile("/response/records/message/head/article");
      DOI = xpath.compile("doi");
      ABSTRACT = xpath.compile("url[not(@format)]");
      HTML = xpath.compile("url[@format='html']");
      PDF = xpath.compile("url[@format='pdf']");
    }
    catch (XPathExpressionException xpee) {
      throw new ExceptionInInitializerError(xpee);
    }
  }

  /**
   * <p>
   * A flag indicating whether work on this query is done. 
   * </p>
   * 
   * @since 1.67.5
   */
  protected boolean done;

  /**
   * <p>
   * The starting index found in the parsed response.
   * </p>
   * 
   * @since 1.67.5
   */
  protected int start;
  
  /**
   * <p>
   * The page length found in the parsed response.
   * </p>
   * 
   * @since 1.67.5
   */
  protected int pageLength;
  
  /**
   * <p>
   * The total found in the parsed response.
   * </p>
   * 
   * @since 1.67.5
   */
  protected int total;
  
  /**
   * <p>
   * Builds a link extractor for Springer's PAM-based API responses (XML).
   * </p>
   * 
   * @since 1.67.5
   */
  public SpringerLinkPamLinkExtractor() {
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
    String loggerUrl = loggerUrl(srcUrl);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // do not set namespaces to true - default is false
    // so that we don't need to track variations to the namespaces sent in the XML over time
    //factory.setNamespaceAware(true);
    // Now that namespaces are ignored, no need to rewrite any of the lines
    //InputSource inputSource = new InputSource(new PamRewritingReader(new InputStreamReader(in, encoding)));
    InputSource inputSource = new InputSource(new InputStreamReader(in, encoding));
    Document doc = null;
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      doc = builder.parse(inputSource);
    }
    catch (ParserConfigurationException pce) {
      throw new UnknownExceptionException("Error configuring parser for " + loggerUrl, pce);
    }
    catch (SAXException se) {
      throw new UnknownExceptionException("Error while parsing " + loggerUrl, se);
    }

    NodeList articles = null;
    try {
      // XPathUtil should be beefed up so that parsing bad numbers actually throws.
      start = XPathUtil.evaluateNumber(START, doc).intValue();
      pageLength = XPathUtil.evaluateNumber(PAGE_LENGTH, doc).intValue();
      total = XPathUtil.evaluateNumber(TOTAL, doc).intValue();
      done = (start + pageLength > total);
      articles = XPathUtil.evaluateNodeSet(ARTICLE, doc);
    }
    catch (XPathExpressionException xpee) {
      throw new UnknownExceptionException("Error while parsing results for " + loggerUrl, xpee);
    }

    if (articles.getLength() == 0) {
      throw new UnknownExceptionException("Internal error parsing results for " + loggerUrl);
    }
    
    Node article = null;
    String doi = null;
    for (int i = 0 ; i < articles.getLength() ; ++i) {
      article = articles.item(i);
      try {
        doi = XPathUtil.evaluateString(DOI, article);
        processDoi(cb, doi);
      }
      catch (XPathExpressionException xpee) {
        throw new UnknownExceptionException(
            String.format("Error while parsing stanza for %s in %s",
                          doi == null ? "first DOI" : "DOI immediately after " + doi,
                          loggerUrl),
            xpee);
      }
    }
  }

  /**
   * <p>
   * If a nominal abstract URL is found, emits the desired abstract URLs based
   * on the given DOI.
   * </p>
   * 
   * @param au
   *          The current AU.
   * @param cb
   *          The callback to emit into.
   * @param doi
   *          The current article's DOI.
   * @param url
   *          A nominal abstract URL (or null or an empty string if not
   *          applicable).
   * @since 1.67.5
   */
  public void processDoi(Callback cb, String doi) {
    if (doi != null && !StringUtils.isEmpty(doi)) {
      cb.foundLink(doi);
    }
  }
  

  /**
   * <p>
   * Determines if this link extractor is done processing records for the
   * current query.
   * </p>
   * 
   * @return Whether this link extractor is done processing records for the
   *         current query.
   * @since 1.67.5
   */
  public boolean isDone() {
    return done;
  }

  /**
   * <p>
   * Retrieves the total from the result section of the latest response.
   * </p>
   * 
   * @return The total from the result section of the latest response.
   * @since 1.67.5
   */
  public int getTotal() {
    return total;
  }

  /**
   * <p>
   * Retrieves the starting index from the result section of the latest response.
   * </p>
   * 
   * @return The starting index from the result section of the latest response.
   * @since 1.67.5
   */
  public int getStart() {
    return start;
  }

  /**
   * <p>
   * Retrieves the page length from the result section of the latest response.
   * </p>
   * 
   * @return The page length from the result section of the latest response.
   * @since 1.67.5
   */
  public int getPageLength() {
    return pageLength;
  }
  
  public static final String loggerUrl(String srcUrl) {
    return srcUrl.replaceAll("&api_key=[^&]*", "");
  }
  
}
