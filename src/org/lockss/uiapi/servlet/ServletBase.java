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
import java.lang.*;
import java.sql.*;
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

public class ServletBase extends HttpServlet
                         implements ApiParameters, 
                                    ClusterControlParameters
{
  /**
   * Servlet context
   */
  protected ServletContext    _context;
	
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
    _context = servletConfig.getServletContext();
  }

  /**
   * Interface to exposed servlet functionality (servlet "services")
   */
  public static class ServletInterfaceImpl implements ServletInterface {
    /*
     * HTTP request and response objects, session state
     */
    private HttpServletRequest    request;
    private HttpServletResponse   response;
    private HttpSession           session;
    
    /**
     * Private constructor
     */
    private ServletInterfaceImpl() { 
    }
    /**
     * Set up services 
     * @param request   Client request info
     * @param response  Servlet response object
     */
    public ServletInterfaceImpl(HttpServletRequest  request,
                                HttpServletResponse response) {
      this.request  = request;
      this.response = response;
      this.session  = request.getSession(true);
    }
 
    /**
     * Send an HTTP redirect
     * @param target Destination URL
     */
    public void redirect(String target) throws IOException {
      response.sendRedirect(target);
    }
 
    /**
     * Set a session context name=value pair
     * @param name Attribute name
     * @param value Attribute value
     */
    public void set(String name, Object value) {
      session.setAttribute(name, value);
    }
    
    /**
     * Delete a session context name=value pair
     * @param name Attribute name
     */
    public void remove(String name) {
      session.removeAttribute(name);
    }

    /**
     * Fetch a value
     * @param name Attribute name
     * @return Requested value
     */
    public Object get(String name) {
      return session.getAttribute(name);
    }

    /**
     * Fetch a value
     * @param name Attribute name
     * @return Requested value (as a String)
     */
    public String getString(String name) {
      return (String) session.getAttribute(name);
    }

    /**
     * Set up an output stream for the servlet response
     */
    public OutputStreamWriter getWriter() throws IOException {
      
      response.setBufferSize(16 * 1024);
      return new OutputStreamWriter(response.getOutputStream(), "UTF-8");
    }

    /**
     * Flush the response ouput buffer
     */
    public void flush() throws IOException {
      response.flushBuffer();
    }

    /**
     * Set response content type
     * @param type Content type text ("text/html", etc)
     */
    public void setContentType(String type) {
      response.setContentType(type);
    }

    /**
     * Return the request URL
     * @return URL Target (plus any query text)
     */
    public String getRequestURL() {
      String url        = request.getRequestURI();
      String arguments  = request.getQueryString();

      if (arguments != null) {
        url += "?";
        url += arguments;
      }
      return url.toString();
    }

    /**
     * Get the server (interface) name this request came in on
     * @return The name of this interface
     */
    public String getServerName() {
      return InetUtils.getInterfaceName(request.getServerName());
    }
  }
}
