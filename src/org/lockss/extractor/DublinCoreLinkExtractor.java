/*
 * $Id: DublinCoreLinkExtractor.java,v 1.1 2007-06-28 07:14:23 smorabito Exp $
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
import java.util.Enumeration;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;
import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.RDFParser;
import org.w3c.rdf.util.*;
import org.xml.sax.*;

import edu.stanford.db.rdf.model.i.StatementImpl;

/**
 * <p>An extractor for links found in Dublin Core RDF.</p>
 * 
 * <p>This class is currently only used for parsing CONTENTdm metadata
 * files, which can be exported in Dublin Core format.  LOCKSS plugins
 * may be written that use the DublinCoreLinkExtractor to harvest
 * CONTENTdm content by crawling the Dublin Core backup file, and
 * extracting and following links to the content.</p>
 *
 */
public class DublinCoreLinkExtractor implements LinkExtractor {
  
  private static final Logger log = Logger.getLogger("DublinCoreLinkExtractor");

  public void extractUrls(ArchivalUnit au, InputStream in, String encoding,
                          String srcUrl, Callback cb)
          throws IOException, PluginException {
    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }

    RDFFactory f = new RDFFactoryImpl();
    Model m = f.createModel();
    RDFParser parser = 
      new org.w3c.rdf.implementation.syntax.sirpac.SiRPAC();
    
    try {
      log.info("*** Creating DublinCoreLinkExtractor with source URL " 
               + srcUrl);

      // Pass in the original source URL
      m.setSourceURI(srcUrl);

      // Now parse
      parser.parse(new InputSource(in), new ModelConsumer(m));

      // So now we've got this model, we'll need to look at all of its
      // elements for HTTP links.
      for (Enumeration<StatementImpl> en = m.elements(); en.hasMoreElements(); ) {
        Statement statement = en.nextElement();
        Resource subject = statement.subject();
        Resource predicate = statement.predicate();
        // Each RDF statement contains a pile of Dublin Core metadata.
        // We're only looking at the statements whose predicates are named
        // 'identifier', of which there should be only one per statement.
        if ("identifier".equals(predicate.getLocalName())) {
          log.info("*** Found link: " + subject.getURI());
          // the RDF spec says that this may be an absolute or relative URL.
          cb.foundLink(subject.getURI());
        }
      }
    } catch (SAXException ex) {
      // This almost certainly means malformed XML.
      throw new PluginException("Error parsing Dublin Core RDF", ex);
    } catch (ModelException ex) {
      // Generic model exception
      throw new PluginException("Generic exception in Dublin Core RDF parsing",
                                ex);
    }
  }
  
  public static class Factory implements LinkExtractorFactory {
    public LinkExtractor createLinkExtractor(String mimeType) {
      return new DublinCoreLinkExtractor();
    }
  }

}
