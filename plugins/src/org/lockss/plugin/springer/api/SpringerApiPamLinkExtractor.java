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
public class SpringerApiPamLinkExtractor implements LinkExtractor {

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
  
  // Will become a definitional param
  private static final String CDN_URL = "http://download.springer.com/";

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
    InputSource inputSource = new InputSource(new PamRewritingReader(new InputStreamReader(in, encoding)));
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
      // XPathUtil should be beefed up so that parsing bad numbers actually throws.
      start = XPathUtil.evaluateNumber(START, doc).intValue();
      pageLength = XPathUtil.evaluateNumber(PAGE_LENGTH, doc).intValue();
      total = XPathUtil.evaluateNumber(TOTAL, doc).intValue();
      done = (start + pageLength > total);
      articles = XPathUtil.evaluateNodeSet(ARTICLE, doc);
    }
    catch (XPathExpressionException xpee) {
      throw new IOException("Error while parsing results for " + srcUrl, xpee);
    }

    if (articles.getLength() == 0) {
      throw new IOException("Internal error parsing results for " + srcUrl);
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
                                    encodeDoi(doi));
      cb.foundLink(absUrl);
    }
  }
  
  public void processHtml(ArchivalUnit au, Callback cb, String doi, String url) {
    if (!StringUtils.isEmpty(url)) { 
      String htmlUrl = String.format("%sarticle/%s/fulltext.html",
                                     au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
                                     encodeDoi(doi));
      cb.foundLink(htmlUrl);
    }
  }
  
  public void processPdf(ArchivalUnit au, Callback cb, String doi, String url) {
    if (!StringUtils.isEmpty(url)) { 
      String pdfUrl = String.format("%scontent/pdf/%s.pdf",
                                    CDN_URL,
                                    encodeDoi(doi));
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
  
  /**
   * <p>
   * Encode a DOI for use in URLs, using the encoding of
   * <code>application/x-www-form-urlencoded</code> and {@link URLEncoder},
   * except that a space (<code>' '</code>) is encoded as <code>"%20"</code>
   * rather than <code>'+'</code>.
   * </p>
   * 
   * @param doi
   *          A DOI.
   * @return An encoded DOI (URL-encoded with <code>"%20"</code> for a space).
   * @since 1.67.5
   */
  public static String encodeDoi(String doi) {
    try {
      return URLEncoder.encode(doi, Constants.ENCODING_UTF_8).replace("+", "%20");
    }
    catch (UnsupportedEncodingException uee) {
      throw new ShouldNotHappenException("Could not URL-encode '" + doi + "' as UTF-8");
    }
  }
  
  public static final String logUrl(String srcUrl) {
    return srcUrl.replaceAll("&api_key=[^&]*", "");
  }
  
}
