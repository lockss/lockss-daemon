/*
 * $Id: CreativeCommonsPermissionChecker.java,v 1.1 2004-10-18 02:57:49 smorabito Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.io.*;
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.rdf.arp.*;
import com.hp.hpl.jena.graph.*;
import org.lockss.util.*;

/**
 * An implementation of PermissionChecker that looks for an RDF
 * statement granting permission to distribute the licensed work.
 * Used by RegistryArchivalUnit to check for plugin redistribution
 * permission.
 *
 * Currently does not support checking any other license restrictions
 * or permissions.
 */
public class CreativeCommonsPermissionChecker
  implements PermissionChecker {

  private static String RDF_START = "<rdf:RDF ";
  private static String RDF_END = "</rdf:RDF>";

  // Maximum size for the RDF buffer
  private static int RDF_BUF_LEN = 65535;

  // Creative Commons RDF resource URIs
  private static final String WORK =
    "http://www.w3.org/1999/02/22-rdf-syntax-ns";
  private static final String LICENSE =
    "http://web.resource.org/cc/license";

  // Permissions URIs (currently only "PERMITS" is checked)
  private static final String PERMITS =
    "http://web.resource.org/cc/permits";
  private static final String REQUIRES =
    "http://web.resources.org/cc/requires";
  private static final String PROHIBITS =
    "http://web.resources.org/cc/prohibits";

  // Permissions URIs (currently only "DISTRIBUTION" is checked)
  private static final String DERIVATIVE_WORKS =
    "http://web.resource.org/cc/DerivativeWorks";
  private static final String REPRODUCTION =
    "http://web.resource.org/cc/Reproduction";
  private static final String DISTRIBUTION =
    "http://web.resource.org/cc/Distribution";

  private static final Logger log =
    Logger.getLogger("CreativeCommonsPermissionChecker");

  /**
   * Check for "Distribution" permission granted by a Creative Commons
   * License.
   */
  public boolean checkPermission(Reader reader) {
    String rdfString;

    try {
      rdfString = extractRDF(reader);
    } catch (IOException ex) {
      log.warning("Extracting RDF caused an IOException", ex);
      return false;
    }

    // extractRDF will return null if no RDF is found
    if (rdfString == null) {
      log.warning("No Creative Commons RDF found to parse.");
      return false;
    }

    Model model = ModelFactory.createDefaultModel();
    JenaReader jreader = new JenaReader();

    // Default error handler prints to stderr.  This error handler
    // will log any RDF parsing errors to the LOCKSS cache log
    // instead.
    jreader.setErrorHandler(new LoggingErrorHandler());

    StringReader rdfIn = new StringReader(rdfString);
    jreader.read(model, rdfIn, RDF.getURI());
    rdfIn.close();

    // Get the "Work" resource in order to determine the license type
    Resource work = model.getResource(WORK);
    Statement licenseStmt =
      work.getProperty(new PropertyImpl(LICENSE));

    if (licenseStmt == null) {
      log.warning("No 'work' resource.  Invalid CC RDF.");
      return false;
    }

    RDFNode licenseType = licenseStmt.getObject();

    if (licenseType == null) {
      log.warning("No 'license' resource.  Invalid CC RDF.");
      return false;
    }

    // Get the "License" resource based on the license from the "Work"
    // section.
    Resource license = model.getResource(licenseType.toString());

    // Iterate through all "Permits" statements.
    StmtIterator iter = license.listProperties(new PropertyImpl(PERMITS));
    while (iter.hasNext()) {
      Statement stmt = (Statement)iter.next();
      Node rsrc = stmt.getObject().asNode();
      if (rsrc.getURI().equals(DISTRIBUTION)) {
	return true;
      }
    }

    // No permission granted if this point is reached.
    return false;
  }

  /**
   * Extract RDF from a reader.  This method will read from the input
   * reader until it has found the closing RDF tag, or until the
   * reader is exhausted.  It will not close the reader.
   *
   * Because it builds a String representation of the RDF, it will
   * only read to a predetermined maximum length to avoid problems
   * with unbounded input. Since it targets Creative Commons License
   * RDF blocks, the size of the buffer is fairly small, 65535 chars.
   * Anything larger than that is almost certainly not a properly
   * formed CC licence.
   */
  private String extractRDF(Reader in) throws IOException {
    boolean found_start = false;
    boolean found_end = false;

    char[] buf = new char[RDF_BUF_LEN];

    int in_pos = 0;
    int buf_pos = 0;

    int start_len = RDF_START.length();
    int end_len = RDF_END.length();

    int c;
    while ((c = in.read()) != -1) {
      if (buf_pos > RDF_BUF_LEN) {
	// Too long to fit in buffer.
	log.warning("RDF block too long to fit in buffer.");
	return null;
      }

      buf[buf_pos++] = (char)c;

      if (!found_start) {
	if (c != RDF_START.charAt(in_pos++)) {
	  in_pos = 0;
	  buf_pos = 0;
	  continue;
	}

	if (in_pos == start_len) {
	  found_start = true;
	  in_pos = 0;
	  continue;
	}
      } else {
	// Found the starting token, read until
	// the ending token is found.
	if (c != RDF_END.charAt(in_pos++)) {
	  in_pos = 0;
	}
	if (in_pos == end_len) {
	  found_end = true;
	  break; // done with this stream.
	}
      }
    }

    if (found_end) {
      return new String(buf, 0, buf_pos);
    } else {
      return null;
    }
  }

  /**
   * Simple RDF error handler that logs output instead of dumping to
   * stderr.
   */
  private static class LoggingErrorHandler implements RDFErrorHandler {
    public void error(Exception e) {
      log.warning("RDF Parser: " + e);
    }

    public void fatalError(Exception e) {
      log.error("RDF Parser: " + e);
    }

    public void warning(Exception e) {
      log.warning("RDF Parser: " + e);
    }
  }
}
