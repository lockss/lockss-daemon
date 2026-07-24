/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.wolterskluwer;

import java.io.*;

import java.util.Map;

import javax.xml.parsers.*;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.tuple.Pair;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.lockss.util.Logger;
import org.xml.sax.*;
import org.lockss.util.Constants;



public class WoltersKluwerXPathXmlMetadataParser extends XPathXmlMetadataParser {
  private static final Logger log = Logger.getLogger(WoltersKluwerXPathXmlMetadataParser.class);

  public WoltersKluwerXPathXmlMetadataParser(Map<String, XPathValue> globalMap,
      String articleNode, Map<String, XPathValue> articleMap)
      throws XPathExpressionException {
    super(globalMap, articleNode, articleMap);
  }
  
  /*
   *  uses the sgmlentities.dtd to help parse WK's metadata/sgml file(non-Javadoc)
   */
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
  protected InputSource makeInputSource(CachedUrl cu) throws IOException {
 
      Pair<Reader, String> sgmlReaderPair = makeInputSourceReader(cu);
      String sgmlReader_cset = sgmlReaderPair.getRight();
      if (sgmlReader_cset != Constants.ENCODING_UTF_8) {
        log.debug3("WARNING: WoltersKluwer sgml input NOT UTF");
      }
      Reader xmlReader = new WoltersKluwerSgmlAdapter(sgmlReaderPair.getLeft());
      InputSource is = new InputSource(xmlReader);
      is.setEncoding(sgmlReader_cset);
      return is;
     }
  
}
