package org.lockss.test;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.io.InputStream;
import java.io.IOException;
import java.security.Permission;

public class MockHttpURLConnection extends HttpURLConnection{
    private int responseCode = -1;

    public void setResponseCode(int responseCode){
	this.responseCode = responseCode;
    }


    //Methods defined in HttpURLConnection
    protected MockHttpURLConnection(){
	super(null);
    }

    public static void setFollowRedirects(boolean set) {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public static boolean getFollowRedirects() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public void setInstanceFollowRedirects(boolean followRedirects) {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean getInstanceFollowRedirects() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public void setRequestMethod(String method) throws ProtocolException {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public String getRequestMethod() {
      throw new UnsupportedOperationException("Not Implemented");
    }
    
    public int getResponseCode() throws IOException {
	return this.responseCode;
    }

    public String getResponseMessage() throws IOException {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public long getHeaderFieldDate(String name, long Default) {
      throw new UnsupportedOperationException("Not Implemented");
    }


    public void connect(){
      throw new UnsupportedOperationException("Not Implemented");
    }

    public void disconnect(){
      throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean usingProxy(){
      throw new UnsupportedOperationException("Not Implemented");
    }

    public Permission getPermission() throws IOException {
      throw new UnsupportedOperationException("Not Implemented");
    }

    public InputStream getErrorStream() {
      throw new UnsupportedOperationException("Not Implemented");
    }
}
