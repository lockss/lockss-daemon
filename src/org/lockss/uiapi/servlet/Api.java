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

package org.lockss.uiapi.servlet;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.servlet.*;

import org.lockss.uiapi.commands.*;
import org.lockss.uiapi.util.*;


public class Api extends ServletBase
                         implements ApiParameters, ClusterControlParameters
{
  /*
   * Globals
   */
  private static Logger   log           = Logger.getLogger("Api");
  private LockssDaemon    lockssDaemon;
  
  /**
   * Info
   */
  public String getServletInfo() {
    return "LOCKSS User Interface - Data exchange API";
  }

  /**
   * Cleanup
   */
  public void destroy() {
    super.destroy();
  }
  
  /**
   * Initialize
   */
  public void init(ServletConfig servletConfig) throws ServletException {
    super.init(servletConfig);
    lockssDaemon = (LockssDaemon) _context.getAttribute("LockssApp");
  }
 
  /**
   * Service a GET request (no action taken)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response) 
              throws ServletException, IOException {
    /*
     * Remove 
     */
    doPost(request, response);
    return;
  }

  /**
   * Service the client request
   * <p>Execute requested command, return any response generated</p>
   * <p>Only <code>POST</codes>s are honored
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response) 
              throws ServletException, IOException {

    ApiActivityBase responder;
    CommandTable entry;
    ServletInterface servletInterface;

    XmlUtils xmlUtils;
    
    String command;
    String xml;
    Document document;
    
    OutputStreamWriter writer;
    String  interfaceName;
    boolean status;
    

    /*
     * Set up servlet "services", XML utilities
     */
    servletInterface  = new ServletInterfaceImpl(request, response);
    interfaceName     = servletInterface.getServerName();
    xmlUtils          = ParseUtils.getApiXmlUtils();
    
    /*
     * Get parameters, set response characteristics (XML, UTF-8, buffer size)
     */
    command = request.getParameter(AP_PARAM_COMMAND);
    xml     = request.getParameter(AP_PARAM_XML);
    writer  = servletInterface.getWriter();
    
    response.setContentType("text/xml; charset=UTF-8");

    /*
     * Select the appropriate command processor
     */
    try {
      if ((entry = CommandTable.getEntry(command)) == null) { 
        sendError(writer, interfaceName, command, "Unknown command");
        
        log.warning("Unknown command: \"" 
                  + command 
                  + "\"");
        return; 
      }
      
      responder = ApiActivityBase.getApiActivity(
                                  CommandTable.selectApiResponder(entry)
                                                );
      if (responder == null) {
        sendError(writer, interfaceName, command, 
                  "No page activity method found");
        
        log.warning("No ActivityBase method for command: \"" 
                  + command 
                  + "\"");
        return; 
      }
      /*
       * Parse the "command details" XML text, select and execute the requested
       * command activity (either command setup or the command itself) and 
       * return the response document
       */
      document = XmlUtils.parseXmlString(xml);
      
      responder.initialize(servletInterface, 
                           interfaceName,
                           command, document);
      responder.setLockssDaemon(lockssDaemon);

      if (ParseUtils.isSetupRequest(xmlUtils, document, command)) {
        status = responder.doRemoteSetupAndVerification();
      } else {
        status = responder.doCommand();
      }

      responder.setStatus(status);
      responder.sendResponse(writer);

    } catch (Exception exception) {
      /*
       * Error
       */
      try {
        response.reset();
        sendError(writer, interfaceName, command, exception);
        
        log.error("\"" 
                + command 
                + "\" command failed, sending error status to client",
                  exception);
      
      } catch (Throwable ignore) { }
    }
    finally {
      writer.close();
    }
  }

  /*
   * Send an error document back to the client
   */
  private void sendError(Writer writer, String server,
                         String command, Exception exception) {

    sendError(writer, server, command, exception.toString());
  }
  
  private void sendError(Writer writer, String server,
                         String command, String message) {
    try {
      ActivityBase.sendError(writer, server, command, message); 
    } catch (Throwable ignore) { }
  }
}
