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

import java.util.List;
import java.net.*;
import java.security.Permission;
import java.io.*;


public class MockURLConnection extends URLConnection {

  List headerFieldKeys = null;
  List headerFields = null;

  public MockURLConnection(URL url){
    super(url);
  }


  public static synchronized FileNameMap getFileNameMap() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public static void setFileNameMap(FileNameMap map) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void connect() throws IOException{
    throw new UnsupportedOperationException("Not Implemented");
  }

  public URL getURL() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public int getContentLength() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getContentType() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getContentEncoding() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public long getExpiration() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public long getDate() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public long getLastModified() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getHeaderField(String name) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public int getHeaderFieldInt(String name, int Default) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public long getHeaderFieldDate(String name, long Default) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getHeaderFieldKey(int n) {
    if (headerFieldKeys == null || headerFieldKeys.size() <= n) {
      return null;
    }
    return (String)headerFieldKeys.get(n);
  }

  public String getHeaderField(int n) {
    if (headerFields == null || headerFields.size() <= n) {
      return null;
    }
    return (String)headerFields.get(n);
  }

  public void setHeaderFieldKeys(List keys) {
    this.headerFieldKeys = keys;
  }

  public void setHeaderFields(List fields) {
    this.headerFields = fields;
  }

  public Object getContent() throws IOException {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public Object getContent(Class[] classes) throws IOException {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public Permission getPermission() throws IOException {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public InputStream getInputStream() throws IOException {
    throw new UnknownServiceException("protocol doesn't support input");
  }

  public OutputStream getOutputStream() throws IOException {
    throw new UnknownServiceException("protocol doesn't support output");
  }

  public String toString() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setDoInput(boolean doinput) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean getDoInput() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setDoOutput(boolean dooutput) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean getDoOutput() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setAllowUserInteraction(boolean allowuserinteraction) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean getAllowUserInteraction() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public static void setDefaultAllowUserInteraction(boolean defaultAUI) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public static boolean getDefaultAllowUserInteraction() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setUseCaches(boolean usecaches) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean getUseCaches() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setIfModifiedSince(long ifmodifiedsince) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public long getIfModifiedSince() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean getDefaultUseCaches() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setDefaultUseCaches(boolean defaultusecaches) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void setRequestProperty(String key, String value) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getRequestProperty(String key) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public static
    synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {
      throw new UnsupportedOperationException("Not Implemented");
    }

  static public String guessContentTypeFromStream(InputStream is)
      throws IOException {
    throw new UnsupportedOperationException("Not Implemented");
  }

}
