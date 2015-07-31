/*
 * $Id: SpringerApiPamLinkExtractor.java 40441 2015-03-12 02:54:52Z thib_gc $
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

package org.lockss.plugin.springer.link;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.*;
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
   * A simple line rewriting reader that adds namespace declarations to a bare
   * top-level <code>&lt;response&gt;</code> node.
   * </p>
   * 
   * @since 1.67.5
   */
  public static class PamRewritingReader extends LineRewritingReader {

    /**
     * <p>
     * A flag indicating if the single rewriting operation has taken place.
     * </p>
     * 
     * @since 1.67.5
     */
    protected boolean rewritten;
    
    /**
     * <p>
     * Wraps an incoming reader.
     * </p>
     * 
     * @param reader
     *          A reader.
     * @since 1.67.5
     */
    public PamRewritingReader(Reader reader) {
      super(reader);
      this.rewritten = false;
    }
    
    @Override
    public String rewriteLine(String line) {
      if (!rewritten && line.startsWith("<response>")) {
        rewritten = true;
        StringBuilder sb = new StringBuilder();
        sb.append("<response");
        for (Map.Entry<String, String> ent : NAMESPACE_MAP.entrySet()) {
          sb.append(" xmlns:");
          sb.append(ent.getKey());
          sb.append("=\"");
          sb.append(ent.getValue());
          sb.append("\"");
        }
        sb.append(">");
        line = sb.toString() + line.substring(10);
      }
      return line;
    }
    
  }

  /**
   * <p>
   * The map of namespaces used in responses. (See static initializer.)
   * </p>
   * 
   * @since 1.67.5
   */
  protected static final Map<String, String> NAMESPACE_MAP;

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
   */
  static {
    NAMESPACE_MAP = new HashMap<String, String>();
    NAMESPACE_MAP.put("dc", "http://purl.org/dc/elements/1.1/");
    NAMESPACE_MAP.put("pam", "http://prismstandard.org/namespaces/pam/2.0/");
    NAMESPACE_MAP.put("prism", "http://prismstandard.org/namespaces/basic/2.0/");
    NAMESPACE_MAP.put("xhtml", "http://www.w3.org/1999/xhtml");
    try {
      XPath xpath = XPathFactory.newInstance().newXPath();
      xpath.setNamespaceContext(new OneToOneNamespaceContext(NAMESPACE_MAP));
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
    factory.setNamespaceAware(true);
    InputSource inputSource = new InputSource(new PamRewritingReader(new InputStreamReader(in, encoding)));
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
