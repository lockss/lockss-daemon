/*
 * $Id: HighWireUrlCacher.java,v 1.2 2002-10-21 23:13:41 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is the UrlCacher object for the HighWirePlugin
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class HighWireUrlCacher extends BaseUrlCacher {
  protected static Logger logger = Logger.getLogger(HighWirePlugin.LOG_NAME);
  private URLConnection conn;
  private Properties headers;

  public HighWireUrlCacher(CachedUrlSet owner, String url) {
    super(owner, url);
  }

  public boolean shouldBeCached(){
    logger.info("checking: "+url);
    return super.shouldBeCached();
  }
  
  // Write interface - used by the crawler.
  
  public void storeContent(InputStream input,
			   Properties headers) throws IOException{
    if (input != null){
      File file = new File(mapUrlToFileName());
      File parentDir = file.getParentFile();
      if (!parentDir.exists()){
	parentDir.mkdirs();
      }
      OutputStream os = new FileOutputStream(file);
      int kar = input.read();
      while (kar >= 0){
	os.write(kar);
	kar = input.read();
      }
      os.close();
      input.close();
    }
    File file = new File(mapUrlToFileName() + ".props");
    File parentDir = file.getParentFile();
    if (!parentDir.exists()){
      parentDir.mkdirs();
    }
    OutputStream os = new FileOutputStream(file);
    headers.store(os, "HTTP headers for " + url);
    os.close();
    
    this.headers = headers;
  }

  public InputStream getUncachedInputStream(){
    try{
      if (conn == null){
	URL urlO = new URL(url);
	conn = urlO.openConnection();
      }
      return conn.getInputStream();
    } catch (IOException ioe){
      ioe.printStackTrace();
    }
    return null;
  }


  public Properties getUncachedProperties(){
    Properties props = new Properties();
    try{
      if (conn == null){
	URL urlO = new URL(url);
	conn = urlO.openConnection();
      }
      String contentType = conn.getContentType();
      props.setProperty("content-type", contentType);
      return props;
    }catch (IOException ioe){
      ioe.printStackTrace();
    }
    return null;
  }


  private String mapUrlToFileName(){
    int idx = url.indexOf("://")+3;
    int lastSlashIdx = url.lastIndexOf("/");
    int lastPeriodIdx = url.lastIndexOf(".");

    StringBuffer fileName = new StringBuffer("./");
    fileName.append(url.substring(idx));

    if (lastSlashIdx >= lastPeriodIdx){
      if (url.charAt(url.length()-1) != '/'){
	fileName.append('/');
      }
      fileName.append("index.html");
    }
    return fileName.toString();
  }


}

