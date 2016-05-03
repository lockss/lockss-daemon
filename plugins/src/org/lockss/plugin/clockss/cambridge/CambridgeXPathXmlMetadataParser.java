/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
