/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.uiapi.commands;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

import org.lockss.uiapi.servlet.*;
import org.lockss.uiapi.util.*;


/**
 * Base class for servlet command handlers
 */
public abstract class ActivityBase implements ApiParameters,
                                              ClusterControlParameters {

  private static Logger log = Logger.getLogger("Activity");

  /*
   * Servlet services
   */
  protected ServletInterface  _servletInterface;

  /*
   * Request parameters
   */
  protected String    _requestCmd;
  protected Document  _requestDoc;
  /*
   * Response related objects
   */
  protected Document  _responseDoc;
  protected String    _responseDetail;
  protected String    _responseError;
  protected HashMap   _responseStatusOptions;
  /*
   * Interface the commend request arrived on, XmlUtils object
   */
  protected String    _serverName;
  protected XmlUtils  _xmlUtils;

  /*
   * Invoked from subclass
   */
  protected ActivityBase() {
    _xmlUtils = ParseUtils.getApiXmlUtils();
  }

  /**
   * Initialize a new response
   * @param servletInterface Servlet services
   * @param server The name of the server (interface) the command came in on
   * @param command Command name
   * @param document The "command details" document
   */
  public void initialize(ServletInterface servletInterface,
                         String   server,
                         String   command,
                         Document document) throws XmlException {
    Element responseRoot;
    Element detail;

    /*
     * Save servlet services, request parameters
     */
    _servletInterface = servletInterface;

    _serverName = server;
    _requestCmd = command;
    _requestDoc = document;

    /*
     * Create a standard response Document
     */
    _responseDoc = XmlUtils.createDocument();
    responseRoot = _xmlUtils.createRoot(_responseDoc, AP_RESPONSEROOT);

    _xmlUtils.setAttribute(responseRoot, AP_A_TYPE, AP_TYPE_STANDARD);

    /*
     * Add the "detail block"
     */
    detail = _xmlUtils.createElement(responseRoot, AP_E_DETAIL);

    _xmlUtils.setAttribute(detail, AP_A_SYSTEM, _serverName);
    _xmlUtils.setAttribute(detail, AP_A_COMMAND, _requestCmd);
    _xmlUtils.setAttribute(detail, AP_A_DATE, DateFormatter.now());
  }

  /**
   * Return the servlet services
   * @return ServletInterface object
   */
  protected ServletInterface getServletInterface() {
    return _servletInterface;
  }

  /**
   * Get the XmlUtils object for this response
   */
  protected XmlUtils getXmlUtils() {
    return _xmlUtils;
  }

	/**
   * Get the command name
   */
  protected String getCommand() {
    return _requestCmd;
  }

  /**
   * Get the request document (as a DOM)
   * @return Request document
   */
  protected Document getRequestDocument() {
    return _requestDoc;
  }

  /**
   * Get the root element of the request Document
   * @return Root element
   */
  protected Element getRequestRoot() {
    return (_requestDoc == null) ? null : _requestDoc.getDocumentElement();
  }

  /**
   * Get the version of the XML syntax employed by this request document
   * @return Syntax version (empty String if none is present)
   */
  protected String getRequestVersion() {
    Element root = _requestDoc.getDocumentElement();

    return (root == null) ? ""
                          : _xmlUtils.getAttribute(root, COM_XML_VERSIONNAME);
  }

  /**
   * Get the response document (DOM rendition)
   * @return Response document
   * <p>
   * In addition to the expected subclass use, this method is referenced
   * by the <code>org.lockss.uiapi.render</code> package
   */
  public Document getResponseDocument() {
    return _responseDoc;
  }

  /**
   * Get the root element of the response document
   * @return Response root element
   */
  protected Element getResponseRoot() {
    return (_responseDoc == null) ? null : _responseDoc.getDocumentElement();
  }

  /**
   * Get response specific error text
   * @return Error text
   */
  protected String getResponseStatusMessage() {
    return _responseError;
  }

  /**
   * Set response specific error text
   * @param text Error text
   */
  protected void setResponseStatusMessage(String text) {
    _responseError = text;
  }

  /**
   * Add a response specific status option
   * @param name name of the status option to set
   * @param value Response information
   */
  protected void addResponseStatusOption(String name, String value) {
    if (_responseStatusOptions == null) {
      _responseStatusOptions = new HashMap();
    }
    _responseStatusOptions.put(name, value);
  }

  /**
   * Get response specific detail information
   * @return Detail text
   */
  protected String getResponseDetailMessage() {
    return _responseDetail;
  }

  /**
   * Set response specific detail information
   * @param text Response information
   */
  protected void setResponseDetailMessage(String text) {
    _responseDetail = text;
  }

  /**
   * Get the version of the XML syntax used to create the reponse document
   * @return Syntax version (empty String if none is present)
   */
  protected String getResponseVersion() {
    Element root = _responseDoc.getDocumentElement();

    return (root == null) ? ""
                          : _xmlUtils.getAttribute(root, COM_XML_VERSIONNAME);
  }

  /**
   * Get text associated with a named element in the client request document
   * @param name Element name
   * @return Parameter value (null if none)
   */
	protected String getText(String name) {
    return ParseUtils.getText(_xmlUtils, getRequestRoot(), name);
  }

  /**
   * Get a parameter value from the client request (a synonym for getText())
   * @param name Parameter name
   * @return Parameter value (null if none)
   */
	protected String getParameter(String name) {
    return getText(name);
  }

  /**
   * Get the server the current command is addressed to
   * @return Server name
   */
  protected String getServerName() {
    return _serverName;
  }

  /*
   * Error handling
   */

  /**
   * Add a final status block to the response document
   *
   * @param success true = success, false = error
   */
  public void setStatus(boolean success) {
    setStatus(success, getResponseStatusMessage());
  }

  /**
   * Add a final status block to the response document, update the "details"
   * block with any informational text established during command execution
   *
   * @param success true = success, false = error
   * @param text error message
   */
  public void setStatus(boolean success, String text) {

    Element element;
    Element textElement;

    /*
     * Command status and error text
     */
    element = _xmlUtils.createElement(getResponseRoot(), AP_E_STATUS);
    _xmlUtils.setAttribute(element, AP_A_SUCCESS,
                                   success ? AP_TRUE: AP_FALSE);

    textElement = _xmlUtils.createElement(element, AP_E_MESSAGE);

    if (text != null) {
      XmlUtils.addText(textElement, text.trim());
    }

    /*
     * Options
     */
    if (_responseStatusOptions != null) {
      Iterator iterator = _responseStatusOptions.entrySet().iterator();

      while (iterator.hasNext()) {
        Map.Entry entry         = (Map.Entry) iterator.next();
        Element   entryElement  = _xmlUtils.createElement(element, AP_E_OPTION);

        _xmlUtils.setAttribute(entryElement, AP_E_NAME, (String) entry.getKey());
        _xmlUtils.setAttribute(entryElement, AP_E_VALUE, (String) entry.getValue());
      }
    }

    /*
     * Informational text
     */
    element     = _xmlUtils.getElement(getResponseRoot(), AP_E_DETAIL);
    textElement = _xmlUtils.createElement(element, AP_E_MESSAGE);

    if (_responseDetail != null) {
      XmlUtils.addText(textElement, _responseDetail.trim());
    }

    try {
      log.debug2("OPTIONS: " + XmlUtils.serialize(_responseDoc));
    } catch (Exception ignore) { }
  }

  /**
   * Create an error response document
   * @param server Server on which the error occured
   * @param command Command active when the error occured
   * @param message Text describing the error
   * @return document The error response
   */
  public static Document errorDocument(String server, String command,
                                       String message) {
   return handleError(null, server, command, message, false);
  }

  /**
   * Create and send an error response document
   * @param writer Target for error block
   * @param server Server on which the error occured
   * @param command Command active when the error occured
   * @param message Text describing the error
   */
  public static void sendError(Writer writer,
                               String server, String command,
                               String message) {
    handleError(writer, server, command, message, true);
  }

  /**
   * Create (and possibly send) a command error response
   * @param writer Target for error block
   * @param server Server on which the error occured
   * @param command Command active when the error occured
   * @param message Text describing the error
   * @param writeToClient Send the error block back to the client?
   */
  private static Document handleError(Writer writer,
                                      String server, String command,
                                      String message,
                                      boolean writeToClient) {
    try {
      ActivityBase error = new Error();

      error.initialize(null, server, command, null);
      error.setStatus(false, message);

      if (writeToClient) {
        error.sendResponse(writer);
      }
      return error.getResponseDocument();

    } catch (Throwable exception) {
      log.error("Failed to handle error response for command \""
              + command
              + "\"");
      throw new ResponseException(exception.toString());
    }
  }

  /**
   * Send a text rendition of the response document to the client
   * @param writer Client writer
   */
  public void sendResponse(Writer writer) throws IOException, XmlException {

    XmlUtils.serialize(_responseDoc, writer);
    writer.flush();
  }

  /**
   * ResponseException
   * <p>
   * Thrown by individual command handlers to indicate internal failure
   */
  public static class ResponseException extends RuntimeException {

    public ResponseException() {
      super();
    }

    public ResponseException(String text) {
      super(text);
    }
  }
}
