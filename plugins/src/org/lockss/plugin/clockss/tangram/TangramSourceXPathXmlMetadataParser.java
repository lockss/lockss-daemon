/*
 * $Id: TangramSourceXPathXmlMetadataParser.java,v 1.2 2014-10-08 19:35:02 aishizaki Exp $
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

package org.lockss.plugin.clockss.tangram;

import java.io.*;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.xml.sax.*;

public class TangramSourceXPathXmlMetadataParser extends XPathXmlMetadataParser {
  private static Logger log = Logger.getLogger(TangramSourceXPathXmlMetadataParser.class);

  public TangramSourceXPathXmlMetadataParser(Map<String, XPathValue> globalMap,
                                             String articleNode,
                                             Map<String, XPathValue> articleMap)
      throws XPathExpressionException {
    super(globalMap, articleNode, articleMap);
  }
  
  public TangramSourceXPathXmlMetadataParser(Map<String, XPathValue> globalMap, 
                                String articleNode, 
                                Map<String, XPathValue> articleMap,
                                boolean doXmlFiltering)
      throws XPathExpressionException {
    super(globalMap, articleNode, articleMap);
    setDoXmlFiltering(doXmlFiltering);
  }
  
  /*
   * This is a temporary fix to set the encoding on the input stream to be UTF-8
   * Otherwise, the way the daemon currently is, even though the file is UTF-8,
   * the default (ISO-8859-1) overrides.  When that is fixed, the generic 
   * XPathXmlMetadataParser can be used, and this class can be removed
   * along with supporting code in TangramSourceXmlMetadataExtractorFactory

  @Override
  protected InputSource makeInputSource(CachedUrl cu) throws UnsupportedEncodingException {
    InputSource is = null;
    try {
      is = super.makeInputSource(cu);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // setting the Encoding to default to UTF-8 until the lockss daemon provides
    // a way to get the correct encoding from the XML file
    log.debug3("InputStream.fromSuper:" +is.getEncoding() );  
log.info("InputStream.fromSuper:" +is.getEncoding() );    

    if (is != null) {
      is.setEncoding(Constants.ENCODING_UTF_8);
log.info("Default charset: "+java.nio.charset.Charset.defaultCharset());

      log.debug3("InputStream.setEncoding:" +Constants.ENCODING_UTF_8 );
    }
    return is;
  }
     */
  @Override
  protected InputSource makeInputSource(CachedUrl cu) throws UnsupportedEncodingException {
    InputSource is = null;
    // setting the encoding to default to UTF-8 until the lockss daemon provides
    // a way to get the correct encoding from the XML file
    try {

      is = super.makeInputSource(cu, Constants.ENCODING_UTF_8);
      log.debug3("Default charset: "+java.nio.charset.Charset.defaultCharset());
      log.debug3("InputStream.fromSuper:" +is.getEncoding() );    

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return is;
  }
  
}
