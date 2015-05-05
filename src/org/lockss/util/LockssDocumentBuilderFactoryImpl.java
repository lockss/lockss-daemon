/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;
import java.lang.reflect.*;
import javax.xml.parsers.*;
import javax.xml.validation.Schema;
import org.xml.sax.*;
import org.lockss.util.*;


/** Wrapper for an existing DocumentBuilderFactory, delegates all calls to
 * it except that the ErrorHandler of created DocumentBuilders is set to
 * one that logs to the LOCKSS logger.  XXX Java version-dependent: if the
 * DocumentBuilderFactory API changes, this must be updated to forward all
 * methods.  Needs to know what underlying factory to use, which makes it
 * also dependent on xercesImpl - XXX Xerces
 */
public class LockssDocumentBuilderFactoryImpl extends DocumentBuilderFactory {
  static Logger log = Logger.getLogger("DocumentBuilderFactory");
  public static String ERROR_LOGGER_NAME = "SAX";

  DocumentBuilderFactory fact;

  public LockssDocumentBuilderFactoryImpl() {
//     fact = new org.apache.crimson.jaxp.DocumentBuilderFactoryImpl();
    fact = new org.apache.xerces.jaxp.DocumentBuilderFactoryImpl();
    log.debug3("Created fact: " + fact);
  }

  /** Forward to real factory, set error handler */
  @Override
  public DocumentBuilder newDocumentBuilder()
      throws ParserConfigurationException {
    DocumentBuilder db = fact.newDocumentBuilder();
    log.debug3("Created builder: " + db);
    db.setErrorHandler(new MyErrorHandler());
    return db;
  }

  @Override
  public void setAttribute(String name, Object value) {
    fact.setAttribute(name, value);
  }

  @Override
  public Object getAttribute(String name) {
    return fact.getAttribute(name);
  }

  @Override
  public void setNamespaceAware(boolean awareness) {
    fact.setNamespaceAware(awareness);
  }

  @Override
  public void setSchema(Schema schema) {
    fact.setSchema(schema);
  }

  @Override
  public void setValidating(boolean validating) {
    fact.setValidating(validating);
  }

  @Override
  public void setIgnoringElementContentWhitespace(boolean whitespace) {
    fact.setIgnoringElementContentWhitespace(whitespace);
  }

  @Override
  public void setExpandEntityReferences(boolean expandEntityRef) {
    fact.setExpandEntityReferences(expandEntityRef);
  }

  @Override
  public void setIgnoringComments(boolean ignoreComments) {
    fact.setIgnoringComments(ignoreComments);
  }

  @Override
  public void setCoalescing(boolean coalescing) {
    fact.setCoalescing(coalescing);
  }

  @Override
  public boolean isNamespaceAware() {
    return fact.isNamespaceAware();
  }

  @Override
  public boolean isValidating() {
    return fact.isValidating();
  }

  @Override
  public boolean isIgnoringElementContentWhitespace() {
    return fact.isIgnoringElementContentWhitespace();
  }

  @Override
  public boolean isExpandEntityReferences() {
    return fact.isExpandEntityReferences();
  }

  @Override
  public boolean isIgnoringComments() {
    return fact.isIgnoringComments();
  }

  @Override
  public boolean isCoalescing() {
    return fact.isCoalescing();
  }

  @Override
  public boolean getFeature(String name) throws ParserConfigurationException {
    return fact.getFeature(name);
  }

  @Override
  public void setFeature(String name, boolean value)
      throws ParserConfigurationException {
    fact.setFeature(name, value);
  }

  // This error handler uses a Logger to log error messages
  static class MyErrorHandler implements ErrorHandler {
    private Logger log = Logger.getLogger(ERROR_LOGGER_NAME);

    //  This method is called in the event of a recoverable error
    public void error(SAXParseException e) {
      log(Logger.LEVEL_WARNING, e);
    }

    //  This method is called in the event of a non-recoverable error
    public void fatalError(SAXParseException e) {
      log(Logger.LEVEL_ERROR, e);
    }

    //  This method is called in the event of a warning
    public void warning(SAXParseException e) {
      log(Logger.LEVEL_WARNING, e);
    }

    // Log the error
    private void log(int level, SAXParseException e) {
      int line = e.getLineNumber();
      int col = e.getColumnNumber();
      String publicId = e.getPublicId();
      String systemId = e.getSystemId();
      StringBuffer sb = new StringBuffer();
      sb.append(e.getMessage());
      if (line > 0 || col > 0) {
	sb.append(": line=");
	sb.append(line);
	sb.append(", col=");
	sb.append(col);
      }
      if (publicId != null || systemId != null) {
	sb.append(": publicId=");
	sb.append(publicId);
	sb.append(", systemId=");
	sb.append(systemId);
      }
      // Log the message
      log.log(level, sb.toString());
    }
  }

}
