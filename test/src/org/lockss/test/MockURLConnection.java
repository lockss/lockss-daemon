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
	return null;
    }

    public static void setFileNameMap(FileNameMap map) {
    }

    public void connect() throws IOException{
    }

    public URL getURL() {
	return null;
    }

    public int getContentLength() {
	return 0;
    }

    public String getContentType() {
	return null;
    }

    public String getContentEncoding() {
	return null;
    }

    public long getExpiration() {
	return 0;
    }

    public long getDate() {
	return 0;
    }

    public long getLastModified() {
	return 0;
    }

    public String getHeaderField(String name) {
	return null;
    }

    public int getHeaderFieldInt(String name, int Default) {
	return 0;
    }

    public long getHeaderFieldDate(String name, long Default) {
	return 0;
    }

    public String getHeaderFieldKey(int n) {
	return null;
    }

    public String getHeaderField(int n) {
	return null;
    }

    public Object getContent() throws IOException {
	return null;
    }

    public Object getContent(Class[] classes) throws IOException {
	return null;
    }

    public Permission getPermission() throws IOException {
	return null;
    }

    public InputStream getInputStream() throws IOException {
	throw new UnknownServiceException("protocol doesn't support input");
    }

    public OutputStream getOutputStream() throws IOException {
	throw new UnknownServiceException("protocol doesn't support output");
    }

    public String toString() {
	return null;
    }

    public void setDoInput(boolean doinput) {
    }

    public boolean getDoInput() {
	return false;
    }

    public void setDoOutput(boolean dooutput) {
    }

    public boolean getDoOutput() {
	return false;
    }

    public void setAllowUserInteraction(boolean allowuserinteraction) {
    }

    public boolean getAllowUserInteraction() {
	return false;
    }

    public static void setDefaultAllowUserInteraction(boolean defaultallowuserinteraction) {
    }

    public static boolean getDefaultAllowUserInteraction() {
	return false;
    }

    public void setUseCaches(boolean usecaches) {
    }

    public boolean getUseCaches() {
	return false;
    }
	
    public void setIfModifiedSince(long ifmodifiedsince) {
    }

    public long getIfModifiedSince() {
	return 0;
    }

    public boolean getDefaultUseCaches() {
	return false;
    }

    public void setDefaultUseCaches(boolean defaultusecaches) {
    }

    public void setRequestProperty(String key, String value) {
    }

    public String getRequestProperty(String key) {
	return null;
    }

  /* deprecated
    public static void setDefaultRequestProperty(String key, String value) {
    }

    public static String getDefaultRequestProperty(String key) {
	return null;
    }
  */

    public static synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {
    }

    static public String guessContentTypeFromStream(InputStream is) throws IOException
    {
	return null;
    }

}
