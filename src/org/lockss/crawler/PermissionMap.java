/*
 * $Id: PermissionMap.java,v 1.3 2004-09-27 23:33:57 dcfok Exp $
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
package org.lockss.crawler;
import java.util.HashMap;
import org.lockss.util.*;
import java.net.MalformedURLException;

/**
 * This Map facilitate the action of putting a permission URL and its status to a hash map
 *
 * @author Chun D. Fok
 * @version 0.0
 */
public class PermissionMap{
  public static final int PERMISSION_MISSING = PermissionRecord.PERMISSION_MISSING;
  public static final int PERMISSION_UNCHECKED = PermissionRecord.PERMISSION_UNCHECKED;
  public static final int PERMISSION_OK = PermissionRecord.PERMISSION_OK;
  public static final int PERMISSION_NOT_OK = PermissionRecord.PERMISSION_NOT_OK;
  public static final int FETCH_PERMISSION_FAILED = PermissionRecord.FETCH_PERMISSION_FAILED;

  private HashMap hMap;

  public PermissionMap(){
    hMap = new HashMap();
  }
  
  /**
   * Put a object to a hashmap using the lowercased host name of permissionUrl as the key.
   * The object contains the host's permission url and permission status. 
   *
   * @param permissionUrl the host's permission url
   * @param status the host's permission status  
   */
  public void putStatus(String permissionUrl, int status) throws MalformedURLException {
    hMap.put(UrlUtil.getHost(permissionUrl).toLowerCase(), new PermissionRecord(permissionUrl,status));
  }
  
  /**
   * Get a PermissionRecord from host name's url as the key
   *
   * @param url the host's url
   * @return PermissionRecord of the host of url
   */
  public PermissionRecord get(String url) throws MalformedURLException{
    return (PermissionRecord) hMap.get(UrlUtil.getHost(url).toLowerCase());
  }

  /**
   * Get the host's permission url from a url
   *
   * @param url a url 
   * @return the host's permission url of the given url
   */
  public String getPermissionUrl(String url) throws MalformedURLException{
    PermissionRecord pr = get(url);
    if (pr == null) {
      return null;
    }
    return pr.getPermissionUrl();
  }

  /**
   * Get the host's permission status from a url
   *
   * @param url a url
   * @return the host's permission status of the given url
   */
  public int getStatus(String url) throws MalformedURLException{
    PermissionRecord pr = get(url);
    if (pr == null) {
      return PERMISSION_MISSING;
    }
    return pr.getPermissionStatus();
  }
}
