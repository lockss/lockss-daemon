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
    }

    public static boolean getFollowRedirects() {
	return false;
    }

    public void setInstanceFollowRedirects(boolean followRedirects) {
    }

    public boolean getInstanceFollowRedirects() {
	return false;
    }

    public void setRequestMethod(String method) throws ProtocolException {
    }

    public String getRequestMethod() {
	return null;
    }
    
    public int getResponseCode() throws IOException {
	return this.responseCode;
    }

    public String getResponseMessage() throws IOException {
	return null;
    }

    public long getHeaderFieldDate(String name, long Default) {
	return 0;
    }


    public void connect(){
    }

    public void disconnect(){
    }

    public boolean usingProxy(){
	return false;
    }

    public Permission getPermission() throws IOException {
	return null;
    }

    public InputStream getErrorStream() {
	return null;
    }
}
