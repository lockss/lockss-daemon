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

/**
 * This sturcture keep the perission page's URL of a host 
 * and the permission status for crawling that host 
 *
 * @author Chun D. Fok
 * @version 0.0
 */
public class PermissionRecord{
  public static final int PERMISSION_UNCHECKED = 0;
  public static final int PERMISSION_OK = 1;
  public static final int PERMISSION_NOT_OK = 2;
  public static final int FETCH_PERMISSION_FAILED = 3;
  
  private String permissionUrl="";
  private int permissionStatus=PERMISSION_UNCHECKED;
  
  public PermissionRecord(String permissionUrl,int permissionStatus){
    if (permissionUrl == null) {
      throw new IllegalArgumentException("Called with null permissionUrl");
    }
     
    setPermissionUrl(permissionUrl);
    setPermissionStatus(permissionStatus);
  }
  
  public String getPermissionUrl(){
    return permissionUrl;
  }
  
  public void setPermissionUrl(String permissionUrl){
    this.permissionUrl=permissionUrl;
  }

  public int getPermissionStatus(){
    return permissionStatus;
  }
  
  public void setPermissionStatus(int permissionStatus){
    this.permissionStatus=permissionStatus;
  }

}
