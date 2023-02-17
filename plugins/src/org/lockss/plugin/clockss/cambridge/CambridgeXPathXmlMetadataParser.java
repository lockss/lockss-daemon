/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.cambridge;

import java.io.*;

import org.apache.commons.lang3.tuple.Pair;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.lockss.util.Logger;
import org.xml.sax.*;



public class CambridgeXPathXmlMetadataParser extends XPathXmlMetadataParser {
  private static final Logger log = Logger.getLogger(CambridgeXPathXmlMetadataParser.class);

  // Provide the constructor called by the createXpathXmlMetadataParser override
  // in CambridgeXmlMetadataExtractorFactory
  // all behavior is default except for reading in the sgml file
  public CambridgeXPathXmlMetadataParser() {
    super();
  }

  @Override
  protected InputSource makeInputSource(CachedUrl cu) throws IOException {
      String url = cu.getUrl();
      // If this is an sgml file, we need to make it conform to xml rules
      if ((url != null) && url.endsWith(".sgm")) {
        log.debug3("filtering sgml in to conforming xml");
        Pair<Reader, String> sgmlReaderPair = makeInputSourceReader(cu);
        // clean up non-terminated tags
        Reader xmlReader = new CambridgeSgmlAdapter(sgmlReaderPair.getLeft());
        InputSource is = new InputSource(xmlReader);
        is.setEncoding(sgmlReaderPair.getRight());
        return is;
      }
      // This already was an xml file
      return super.makeInputSource(cu);
     }
  
}
