/*
 * $Id: WoltersKluwerXPathXmlMetadataParser.java,v 1.1.2.2 2014-07-18 15:56:33 wkwilson Exp $
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

package org.lockss.plugin.wolterskluwer;

import java.io.*;
import java.util.Map;

import javax.xml.parsers.*;
import javax.xml.xpath.XPathExpressionException;

import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.xml.sax.*;

public class WoltersKluwerXPathXmlMetadataParser extends XPathXmlMetadataParser {

  public WoltersKluwerXPathXmlMetadataParser(Map<String, XPathValue> globalMap,
                                             String articleNode,
                                             Map<String, XPathValue> articleMap)
      throws XPathExpressionException {
    super(globalMap, articleNode, articleMap);
  }

  @Override
  protected DocumentBuilder makeDocumentBuilder(DocumentBuilderFactory dbf)
      throws ParserConfigurationException {
    DocumentBuilder db = super.makeDocumentBuilder(dbf);
    db.setEntityResolver(new EntityResolver() {
      @Override
      public InputSource resolveEntity(String publicId, String systemId)
          throws SAXException, IOException {
        if (systemId.contains("ovidbase.dtd")) {
          return new InputSource(getClass().getResourceAsStream("sgmlentities.dtd"));
        }
        return null;
      }
    });
    return db;
  }
  
  @Override
  protected InputSource makeInputSource(CachedUrl cu) throws UnsupportedEncodingException {
    InputStream cuInputStream = getInputStreamFromCU(cu);
    Reader sgmlReader = new InputStreamReader(cuInputStream, cu.getEncoding());
    Reader xmlReader = new WoltersKluwerSgmlAdapter(sgmlReader);
    return new InputSource(xmlReader);
  }
  
}
