/*
 * $Id$
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
import org.xml.sax.helpers.*;

/** SAX-based XML metadata extractor.  Requires XML to be well-formed; see
 * {@link SimpleXmlMetadataExtractor} for a more permissive alternative.
*/
public class SaxMetadataExtractor extends SimpleFileMetadataExtractor {
  static Logger log = Logger.getLogger("SaxMetadataExtractor");
  protected Collection<String> tags;
  protected ArticleMetadata am;

  /**
   * Create an extractor that will extract the value(s) of the xml tags in
   * <code>tags</code>.  Tags are matched independent of case.
   * @param tags the collection of XML tags whose value to extract
   */
  public SaxMetadataExtractor(Collection<String> tags) {
    this.tags = tags;
  }

  /**
   * Create an extractor that will extract the value(s) of the xml tags in
   * <code>tagMap.keySet()</code>  Tags are matched independent of case.
   * @param tagMap a map whose keys are the XML tags whose value to
   * extract.  (The values in the map are not used.)
   */
  public SaxMetadataExtractor(Map<String,? extends Object> tagMap) {
    this(tagMap.keySet());
  }

  /*
   * Do a sax-based XML parse and get all the metadata
   */
  public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
    if (cu == null) {
      throw new IllegalArgumentException("extract() called with null CachedUrl");
    }
    am = new ArticleMetadata();
    InputSource bReader = new InputSource(cu.openForReading());
    try {
      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setErrorHandler(new LoggingErrorHandler());
      xmlReader.setContentHandler(getContentHandler());
      xmlReader.parse(bReader);
    } catch (SAXException e) {
      // XXX Should this terminate the extraction?
      // SimpleXmlMetadataExtractor simply skips malformed constructs
      //       throw new IOException(e);
    } finally {
      IOUtil.safeClose(bReader.getCharacterStream());
    }
    return am;
  }

  /** Override to use a custom ContentHandler */
  protected ContentHandler getContentHandler() {
    return new SaxEventHandler();
  }
    
  /** Return true if this is a tag whose value we should record */
  protected boolean isInterestingTagName(String tagName) {
    for (String tag : tags) {
      if (tagName.equalsIgnoreCase(tag)) {
	return true;
      }
    }
    return false;
  }

  /** Default SAX ContentHandler records values of matching tags */
  protected class SaxEventHandler extends DefaultHandler {
    protected StringBuilder charBuf = null;

    public void characters(char[] ch, int start, int length)
	throws SAXException {
      // Collect chars iff we're in a tag of interest
      if (charBuf != null) {
	charBuf.append(ch, start, length);
      }
    }

    public void startElement(String uri, String localName, String qName,
			     Attributes atts) throws SAXException {
      if (isInterestingTagName(localName)) {
	charBuf = new StringBuilder();
      } else {
 	charBuf = null;
      }
    }

    public void endElement(String uri, String localName, String qName)
	throws SAXException {
      if (charBuf != null && charBuf.length() != 0) {
	am.putRaw(localName, charBuf.toString());
	charBuf = null;
      }
    }
  }

  /** SAX error handler that logs output to LOCKSS logger. */
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
