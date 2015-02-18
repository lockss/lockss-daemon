/*
 * $Id$
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

package org.lockss.extractor;

import java.io.*;
import java.util.*;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
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

  /** Resource name of local copy of expected DTD */
  static final String DTD_RESOURCE = "dcmes-xml-20000714.dtd";

  /** Map known DTD URLs to their local resource name */
  private static final Map<String,String> LOCAL_DTD_NAMES = 
    MapUtil.map("http://dublincore.org/schemas/dcmes-xml-20000714.dtd",
		DTD_RESOURCE,
		"http://purl.org/dc/schemas/dcmes-xml-20000714.dtd",
		DTD_RESOURCE);

  private static Map<String,String> LOCAL_DTDS = new HashMap<String,String>();


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

    // Create a parser with a custom EntityResolver, which returns a local
    // copy of the expected DTD.  (SiRPAC implements EntityResolver)

    RDFParser parser = 
      new org.w3c.rdf.implementation.syntax.sirpac.SiRPAC() {
	@Override
	public InputSource resolveEntity(String publicID, String systemID)
	    throws SAXException {
	  String dtdstr = getDtd(systemID);
	  if (dtdstr != null) {
	    log.debug3("Substituting local DTD for " + systemID);
	    return new InputSource(new StringReader(dtdstr));
	  }
	  log.debug2("Using original DTD: " + publicID + ", " + systemID);
	  return super.resolveEntity(publicID, systemID);
	}
      };
    
    try {
      log.debug("Parsing source URL " + srcUrl);

      // Pass in the original source URL
      m.setSourceURI(srcUrl);

      // Now parse -- use reader to ensure correct end-of-lines for local files
      Reader rdr = StringUtil.getLineReader(in, encoding);
      parser.parse(new InputSource(rdr), new ModelConsumer(m));

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
          log.debug2("Found link: " + subject.getURI());
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
  
  String getDtd(String url) {
    synchronized (LOCAL_DTDS) {
      String str = LOCAL_DTDS.get(url);
      if (str == null) {
	String resname = LOCAL_DTD_NAMES.get(url);
	if (resname != null) {
	  InputStream in = null;
	  try {
	    in = DublinCoreLinkExtractor.class.getResourceAsStream(resname);
	    str = StringUtil.fromInputStream(in);
	  } catch (IOException e) {
	    log.warning("Couldn't load Dublin Core rdf DTD, using empty DTD",
			e);
	    str = "";
	  } finally {
	    IOUtil.safeClose(in);
	  }
	  LOCAL_DTDS.put(url, str);
	}
      }
      return str;
    }
  }

  public static class Factory implements LinkExtractorFactory {
    public LinkExtractor createLinkExtractor(String mimeType) {
      return new DublinCoreLinkExtractor();
    }
  }

}
