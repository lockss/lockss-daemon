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
package org.lockss.test;

import java.util.*;
import java.security.Principal;
import org.mortbay.http.*;
import javax.servlet.http.*;
import org.mortbay.util.*;

public class MockHttpRequest extends HttpRequest {
  boolean wasHandled = false;
  Map fields = new HashMap();
  String remoteAddr;
  URI uri;


//   public UserPrincipal	basicAuthenticated(UserRealm realm) {
//   throw new UnsupportedOperationException("not implemented");
//   }

 public void destroy() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.util.List getAcceptableTransferCodings() {
    throw new UnsupportedOperationException("not implemented");
  }

  public Cookie[] getCookies() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getEncodedPath() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getHost() {
    throw new UnsupportedOperationException("not implemented");
  }

  public HttpResponse getHttpResponse() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getMethod() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getParameter(java.lang.String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.util.Set getParameterNames() {
    throw new UnsupportedOperationException("not implemented");
  }

  public MultiMap getParameters() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.util.Map getParameterStringArrayMap() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.util.List getParameterValues(java.lang.String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getPath() {
    throw new UnsupportedOperationException("not implemented");
  }

  public int getPort() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getQuery() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getRemoteAddr() {
    return remoteAddr;
  }

  public void setRemoteAddr(String remoteAddr) {
    this.remoteAddr = remoteAddr;
  }

  public java.lang.String getRemoteHost() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getRequestLine() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.StringBuffer getRequestURL() {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * @deprecated
   */
  public HttpResponse getResponse() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.StringBuffer getRootURL() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getScheme() {
    throw new UnsupportedOperationException("not implemented");
  }

  public long getTimeStamp() {
    throw new UnsupportedOperationException("not implemented");
  }

  public URI getURI() {
    return uri;
  }

  public void setURI(URI uri) {
    this.uri = uri;
  }

  public Principal getUserPrincipal() {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean isHandled() {
    return this.wasHandled;
  }

  public boolean isUserInRole(java.lang.String role) {
    throw new UnsupportedOperationException("not implemented");
  }

//   public void readHeader(ChunkableInputStream in) {
//     throw new UnsupportedOperationException("not implemented");
//   }

  public void setHandled(boolean handled) {
    this.wasHandled = handled;
  }

  public void setUserPrincipal(Principal principal) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void writeHeader(java.io.Writer writer) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void writeRequestLine(java.io.Writer writer) {
    throw new UnsupportedOperationException("not implemented");
  }



  //HttpMessage.Implementation methods

  public boolean acceptTrailer() {
    throw new UnsupportedOperationException("not implemented");
  }

  public void addDateField(java.lang.String name, java.util.Date date) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void addDateField(java.lang.String name, long date) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void addField(java.lang.String name, java.lang.String value) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void addIntField(java.lang.String name, int value) {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean containsField(java.lang.String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.Object getAttribute(java.lang.String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.util.Enumeration getAttributeNames() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getCharacterEncoding() {
    throw new UnsupportedOperationException("not implemented");
  }

  public int getContentLength(){
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getContentType() {
    throw new UnsupportedOperationException("not implemented");
  }

  public long getDateField(java.lang.String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  public int getDotVersion() {
    throw new UnsupportedOperationException("not implemented");
  }

  HttpMessage getFacade() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getField(java.lang.String name) {
    return (String)fields.get(name);
  }

  public String setField(String name, String value) {
    return (String)fields.put(name, value);
  }

  public java.util.Enumeration getFieldNames() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.util.Enumeration getFieldValues(java.lang.String name) {
    String val = getField(name);
    if (val == null) {
      return null;
    }
    Vector lst = org.lockss.util.StringUtil.breakAt(val, ',');
    return lst.elements();
  }

  public java.util.Enumeration getFieldValues(java.lang.String name, java.lang.String separators) {
    throw new UnsupportedOperationException("not implemented");
  }

  public HttpFields getHeader() {
    throw new UnsupportedOperationException("not implemented");
  }

  public HttpConnection getHttpConnection() {
    throw new UnsupportedOperationException("not implemented");
  }

  HttpMessage getHttpMessage() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.io.InputStream getInputStream() {
    throw new UnsupportedOperationException("not implemented");
  }

  public int getIntField(java.lang.String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getMimeType() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.io.OutputStream getOutputStream() {
    throw new UnsupportedOperationException("not implemented");
  }


  public int getState() {
    throw new UnsupportedOperationException("not implemented");
  }

  public HttpFields getTrailer() {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String getVersion() {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean isCommitted() {
    throw new UnsupportedOperationException("not implemented");
  }


  public void removeAttribute(java.lang.String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String removeField(java.lang.String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setAcceptTrailer(boolean acceptTrailer) {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.Object setAttribute(java.lang.String name, java.lang.Object attribute) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setCharacterEncoding(java.lang.String encoding) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setContentLength(int len) {
    throw new UnsupportedOperationException("not implemented");
  }


  public void setContentType(java.lang.String contentType) {
    throw new UnsupportedOperationException("not implemented");
  }


  public void setDateField(java.lang.String name, java.util.Date date) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setDateField(java.lang.String name, long date) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setFacade(HttpMessage facade) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setField(java.lang.String name, java.util.List value) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setIntField(java.lang.String name, int value) {
    throw new UnsupportedOperationException("not implemented");
  }

  public int setState(int state) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setVersion(java.lang.String version) {
    throw new UnsupportedOperationException("not implemented");
  }

  public java.lang.String toString() {
    throw new UnsupportedOperationException("not implemented");
  }

}
