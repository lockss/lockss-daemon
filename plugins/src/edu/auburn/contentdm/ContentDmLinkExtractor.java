/*
 * $Id: ContentDmLinkExtractor.java,v 1.1 2009-04-09 21:40:00 thib_gc Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package edu.auburn.contentdm;

import java.io.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.filter.StringFilter;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.ReaderInputStream;

public class ContentDmLinkExtractor extends DublinCoreLinkExtractor {

  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          LinkExtractor.Callback cb)
      throws IOException,
             PluginException {
    final String urlBad = "<!DOCTYPE rdf:RDF SYSTEM \"http://purl.org/dc/schemas/dcmes-xml-20000714.dtd\">";
    final String urlGood = "<!DOCTYPE rdf:RDF SYSTEM \"http://dublincore.org/schemas/dcmes-xml-20000714.dtd\">";

    // Replace:
    //     <!DOCTYPE rdf:RDF SYSTEM "http://purl.org/dc/schemas/dcmes-xml-20000714.dtd">
    // by:
    //     <!DOCTYPE rdf:RDF SYSTEM "http://dublincore.org/schemas/dcmes-xml-20000714.dtd">
    InputStream rewritten = new ReaderInputStream(new StringFilter(new InputStreamReader(in,
                                                                                         encoding),
                                                                   urlBad,
                                                                   urlGood));

    super.extractUrls(au, rewritten, encoding, srcUrl, cb);
  }
  
}
