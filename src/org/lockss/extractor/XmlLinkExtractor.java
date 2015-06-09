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

package org.lockss.extractor;

import java.io.*;
import java.net.MalformedURLException;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.XmlLinkExtractor.XmlLinkExtractorHandler.DoneProcessing;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.xml.sax.*;

/**
 * <p>
 * XML documents can point to a stylesheets via processing instructions
 * (<code>&lt;?xml-stylesheet href="..."?&gt;</code>). Some browsers fetch these
 * external resources automatically and sometimes render the result of applying
 * the stylesheet to the document. This link extractor parses the prolog of an
 * XML document and emits the appropriate links.
 * </p>
 * 
 * @since 1.68
 * @see http://www.w3.org/TR/xml-stylesheet/
 */
public class XmlLinkExtractor implements LinkExtractor {

  /**
   * <p>
   * A logger for use by this class.
   * </p>
   * 
   * @since 1.68
   */
  private static final Logger log = Logger.getLogger(XmlLinkExtractor.class);
  
  /**
   * <p>
   * A handler that focuses on processing instructions with target
   * <code>xml-stylesheet</code> and parses their <code>href</code>
   * pseudo-attribute. This handler stops processing at the first actual element
   * by throwing {@link DoneProcessing} (SAX offers no signaling mechanism).
   * </p>
   * 
   * @since 1.68
   * @see DoneProcessing
   */
  protected static class XmlLinkExtractorHandler extends org.xml.sax.helpers.DefaultHandler {

    /**
     * <p>
     * A marker exception intended to signal the expected, early end of
     * processing.
     * </p>
     * 
     * @since 1.68
     */
    protected static class DoneProcessing extends SAXException {
      // empty class
    }
    
    /**
     * <p>
     * A regular expression that matches the <code>href</code> pseudo-attribute
     * and captures its value. No decoding of the value string is performed.
     * </p>
     * 
     * @since 1.68
     * @see http://www.w3.org/TR/xml-stylesheet/#NT-PseudoAtt
     */
    protected static final Pattern HREF = Pattern.compile("\\bhref[ \\t\\n\\r]*=[ \\t\\n\\r]*(\"([^\"]*)\"|'([^']*)')");

    /**
     * <p>
     * The callback for the link extractor currently executing.
     * </p>
     * 
     * @since 1.68
     */
    protected Callback cb;
    
    /**
     * <p>
     * The archival unit for the link extractor currently executing.
     * </p>
     * 
     * @since 1.68
     */
    protected ArchivalUnit au;
    
    /**
     * <p>
     * The source URL for the link extractor currently executing.
     * </p>
     * 
     * @since 1.68
     */
    protected String srcUrl;

    /**
     * <p>
     * Makes a new handler for a given link extractor execution.
     * </p>
     * 
     * @param cb
     *          The callback for the link extractor currently executing.
     * @param au
     *          The archival unit for the link extractor currently executing.
     * @param srcUrl
     *          The source URL for the link extractor currently executing.
     * @since 1.68
     */
    public XmlLinkExtractorHandler(Callback cb, ArchivalUnit au, String srcUrl) {
      this.cb = cb;
      this.au = au;
      this.srcUrl = srcUrl;
    }
    
    @Override
    public void processingInstruction(String target, String data) throws SAXException {
      if ("xml-stylesheet".equals(target)) {
        Matcher mat = HREF.matcher(data);
        if (mat.find()) {
          String val = mat.group(2);
          if (val == null) {
            val = mat.group(3);
          }
          if (UrlUtil.isAbsoluteUrl(val)) {
            cb.foundLink(val);
          }
          else {
            try {
              cb.foundLink(UrlUtil.resolveUri(srcUrl, val));
            }
            catch (MalformedURLException mue) {
              log.debug2("Malformed URL", mue);
            }
          }
        }
      }
    }
    
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
        throws IOException, SAXException {
      return new InputSource(new StringReader("")); // suppress DOCTYPE DTD parsing
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes attributes)
        throws SAXException {
      throw new DoneProcessing(); // Intentionally end processing early
    }
    
    @Override
    public void warning(SAXParseException spe) throws SAXException {
      log.debug2("Internal warning", spe);
      super.warning(spe);
    }
    
    @Override
    public void error(SAXParseException spe) throws SAXException {
      log.debug2("Internal error", spe);
      super.error(spe);
    }
    
    @Override
    public void fatalError(SAXParseException spe) throws SAXException {
      log.debug2("Internal fatal error", spe);
      super.fatalError(spe);
    }

  }
  
  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException, PluginException {
    try {
      SAXParserFactory saxParserFactory = makeSaxParserFactory();
      SAXParser saxParser = makeSaxParser(saxParserFactory);
      saxParser.parse(in, makeDefaultHandler(au, srcUrl, cb));
    }
    catch (ParserConfigurationException pce) {
      throw new IOException(pce);
    }
    catch (DoneProcessing expected) {
      // do nothing (intentional signal)
    }
    catch (SAXException se) {
      throw new IOException(se);
    }
  }

  /**
   * <p>
   * Makes a new {@link DefaultHandler} instance with the given input arguments
   * (to be passed to {@link SAXParser#parse(InputStream, DefaultHandler)}).
   * </p>
   * 
   * @param au
   *          An archival unit
   * @param srcUrl
   *          The document's source URL
   * @param cb
   *          A link extractor callback
   * @return A {@link DefaultHandler} instance
   * @since 1.68
   */
  protected org.xml.sax.helpers.DefaultHandler makeDefaultHandler(ArchivalUnit au,
                                                                  String srcUrl,
                                                                  Callback cb) {
    return new XmlLinkExtractorHandler(cb, au, srcUrl);
  }

  /**
   * <p>
   * Makes a new SAX parser from the given SAX parser factory (by default simply
   * {@link SAXParserFactory#newSAXParser()}).
   * </p>
   * 
   * @param saxParserFactory
   *          A SAX parser factory
   * @return A SAX parser
   * @throws ParserConfigurationException
   *           if a parser configuration error occurs
   * @throws SAXException
   *           if a SAX error occurs
   * @since 1.68
   */
  protected SAXParser makeSaxParser(SAXParserFactory saxParserFactory)
      throws ParserConfigurationException, SAXException {
    return saxParserFactory.newSAXParser();
  }

  /**
   * <p>
   * Makes a SAX parser factory (by default simply
   * {@link SAXParserFactory#newInstance()}).
   * </p>
   * 
   * @return A SAX parer factory
   * @since 1.68
   */
  protected SAXParserFactory makeSaxParserFactory() {
    return SAXParserFactory.newInstance();
  }

}
