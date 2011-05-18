/*
 * $Id: SaxMetadataExtractor.java,v 1.1 2011-05-18 04:04:23 tlipkis Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;
import org.lockss.plugin.*;

import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

public class SaxMetadataExtractor extends SimpleFileMetadataExtractor
  implements ContentHandler {

  static Logger log = Logger.getLogger("SaxMetadataExtractor");
  private Collection<String> tags;
  private StringBuilder charBuf = new StringBuilder();
  private ArticleMetadata am;

  /**
   * Create an extractor what will extract the value(s) of the xml tags in
   * <code>tags</code>
   * @param tags the list of XML tags whose value to extract
   */
  public SaxMetadataExtractor(Collection<String> tags) {
    this.tags = tags;
  }

  /**
   * Create an extractor that will extract the value(s) of the xml tags in
   * <code>tagMap.keySet()</code>
   * @param tagMap a map from XML tags to cooked keys.  (Only the set of
   * tags is used by this object.)
   */
  public SaxMetadataExtractor(Map tagMap) {
    this.tags = tagMap.keySet();
  }

  /*
   * Do a sax-based XML parse and get all the metadata
   */
  public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
    if (cu == null) {
      throw new IllegalArgumentException("extract() called with null CachedUrl");
    }
    this.am = new ArticleMetadata();
    InputSource bReader = new InputSource(cu.openForReading());
    try {
      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setErrorHandler(new LoggingErrorHandler());
      xmlReader.setContentHandler(this);
      xmlReader.parse(bReader);
    } catch (SAXException e) {
      // XXX Should this terminate the extraction?
      // SimpleXmlMetadataExtractor simply skips malformed constructs
      //       throw new IOException(e);
    }
    return am;
  }

  public void characters(char[] ch, int start, int length)
      throws SAXException {
    charBuf.append(ch, start, length);
  }

  public void startElement(String uri, String localName, String qName,
			   Attributes atts) throws SAXException {
    charBuf = new StringBuilder();
  }

  public void endElement(String uri, String localName, String qName)
      throws SAXException {

    for (Iterator it = tags.iterator(); it.hasNext();) {
      String tag = (String)it.next();
      if (localName.equalsIgnoreCase(tag) && (charBuf.length() != 0)) {
        am.putRaw(tag, charBuf.toString());
      }
    }
  }

  public void endDocument() throws SAXException {}
  public void endPrefixMapping(String prefix) throws SAXException {}
  public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException {}
  public void processingInstruction(String target, String data)
      throws SAXException {}
  public void setDocumentLocator(Locator locator) {  }
  public void skippedEntity(String name) throws SAXException {}
  public void startDocument() throws SAXException {}
  public void startPrefixMapping(String prefix, String uri)
      throws SAXException {}

  /**
   * Simple SAX error handler that logs output instead of dumping to
   * stderr.
   */
  private static class LoggingErrorHandler implements ErrorHandler {
    public void error(SAXParseException e) {
      log.warning("Recoverable parse error: " + e.getMessage());
    }

    public void fatalError(SAXParseException e) {
      log.error("Parse error: " + e.getMessage());
    }

    public void warning(SAXParseException e) {
      log.warning(e.getMessage());
    }
  }
}
