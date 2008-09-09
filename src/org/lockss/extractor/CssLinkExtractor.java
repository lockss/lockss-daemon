/*
 * $Id: CssLinkExtractor.java,v 1.5 2008-09-09 07:52:07 tlipkis Exp $
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
import java.net.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;
import org.w3c.css.sac.*;
import org.w3c.flute.parser.Parser;

/**
 * <p>Extracts links (URIs) from CSS documents.</p>
 * <p>The current implementation is based on the W3C's Flute.</p>
 * @author Thib Guicherd-Callin
 * @see <a href="http://www.w3.org/Style/CSS/SAC/">SAC and Flute at the W3C</a>
 */
public class CssLinkExtractor implements LinkExtractor {

  /**
   * <p>A subclass of {@link CSSException} that wraps a Java-style
   * {@link MalformedURLException}, so that it can be caught outside
   * the CSS backend.</p>
   * @author Thib Guicherd-Callin
   */
  private static class MalformedUrlException extends CSSException {
    
    /**
     * <p>The Java-style {@link MalformedURLException} inside this
     * exception.</p>
     */
    protected MalformedURLException javaMalformedUrlException;
    
    /**
     * <p>Wraps a Java-style {@link MalformedURLException} into a new
     * exception.</p>
     * @param javaMalformedUrlException A Java-style
     *                                  {@link MalformedURLException}.
     */
    public MalformedUrlException(MalformedURLException javaMalformedUrlException) {
      this.javaMalformedUrlException = javaMalformedUrlException;
    }
    
    /**
     * <p>Retrieves this exception's underlying Java-style
     * {@link MalformedURLException}.</p>
     * @return The Java-style {@link MalformedURLException} inside
     *         this exception
     */
    public MalformedURLException getJavaMalformedUrlException() {
      return javaMalformedUrlException;
    }
    
  }
  
  /**
   * <p>An implementation of {@link DocumentHandler} that extracts all
   * links (URIs) from a CSS document and passes them to a
   * {@link FoundUrlCallback} instance.</p>
   * @author Thib Guicherd-Callin
   */
  private static class LockssDocumentHandler implements DocumentHandler {

    protected URL baseUrl;
    
    protected LinkExtractor.Callback callback;
    
    public LockssDocumentHandler(URL baseUrl,
                                 LinkExtractor.Callback callback) {
      this.baseUrl = baseUrl;
      this.callback = callback;
    }

    /* Inherit documentation */
    public void importStyle(String uri,
                            SACMediaList media,
                            String defaultNamespaceURI)
        throws CSSException {
      emit(uri);
    }

    /* Inherit documentation */
    public void property(String name,
                         LexicalUnit value,
                         boolean important)
        throws CSSException {
      if (value.getLexicalUnitType() == LexicalUnit.SAC_URI) {
        emit(value.getStringValue());
      }
    }

    protected void emit(String url) throws CSSException {
      if ("".equals(url)) {
        throw new MalformedUrlException(new MalformedURLException("Empty URL"));
      }
      
      try {
        String resolved = UrlUtil.resolveUri(baseUrl, url);
        if (logger.isDebug2()) {
          logger.debug2("Found " + url + " which resolves to " + resolved);
        }
        callback.foundLink(resolved);
      }
      catch (MalformedURLException javaMalformedUrlException) {
        throw new MalformedUrlException(javaMalformedUrlException);
      }
    }
    
    /*
     * All the following methods just ignore their event
     */
    
    public void comment(String text) throws CSSException {}
    public void endDocument(InputSource source) throws CSSException {}
    public void endFontFace() throws CSSException {}
    public void endMedia(SACMediaList media) throws CSSException {}
    public void endPage(String name, String pseudo_page) throws CSSException {}
    public void endSelector(SelectorList selectors) throws CSSException {}
    public void ignorableAtRule(String atRule) throws CSSException {}
    public void namespaceDeclaration(String prefix, String uri) throws CSSException {}
    public void startDocument(InputSource source) throws CSSException {}
    public void startFontFace() throws CSSException {}
    public void startMedia(SACMediaList media) throws CSSException {}
    public void startPage(String name, String pseudo_page) throws CSSException {}
    public void startSelector(SelectorList selectors) throws CSSException {}

  }
  
  protected Parser makeParser() {
    return new Parser();
  }

  /* Inherit documentation */
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
			  String encoding,
                          String srcUrl,
			  LinkExtractor.Callback cb)
      throws MalformedURLException, IOException {
    logger.debug2("Parsing " + srcUrl + ", enc " + encoding);
    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    URL baseUrl = new URL(srcUrl);
    DocumentHandler documentHandler = new LockssDocumentHandler(baseUrl, cb);
    Parser parser = makeParser();
    parser.setDocumentHandler(documentHandler);
    
    try {
      InputSource inputSource = new InputSource();
      inputSource.setEncoding(encoding);
      inputSource.setByteStream(in);
      parser.parseStyleSheet(inputSource);
    } catch (MalformedUrlException lockssMalformedUrlException) {
      MalformedURLException javaMalformedUrlException =
        lockssMalformedUrlException.getJavaMalformedUrlException();
      logger.siteError("Malformed URL while parsing " + srcUrl,
                   javaMalformedUrlException);
      throw javaMalformedUrlException;
    } catch (org.w3c.css.sac.CSSException e) {
      logger.error("Can't parse CSS: " + srcUrl, e);
      throw new CacheException.ExtractionError(e.toString(), e);
    }
  }
  
  private static final Logger logger = Logger.getLogger("CssLinkExtractor");
  
  public static class Factory implements LinkExtractorFactory {
    public LinkExtractor createLinkExtractor(String mimeType) {
      return new CssLinkExtractor();
    }
  }

}
