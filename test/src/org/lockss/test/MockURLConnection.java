package org.lockss.test;

import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.FileNameMap;
import java.net.URL;
import java.net.ContentHandlerFactory;
import java.net.UnknownServiceException;
import java.security.Permission;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;


public class MockURLConnection extends URLConnection{

  protected MockURLConnection(URL url){
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
    throw new UnsupportedOperationException("Not Implemented");
  }

  public String getHeaderField(int n) {
    throw new UnsupportedOperationException("Not Implemented");
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
