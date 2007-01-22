/*
 * $Id: PermissionRecord.java,v 1.7 2007-01-22 22:10:09 troberts Exp $
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

/**
 * This sturcture keep the perission page's URL of a host
 * and the permission status for crawling that host
 *
 * @author Chun D. Fok
 * @version 0.0
 */
public class PermissionRecord {
  public static final int PERMISSION_UNCHECKED = 0;
  public static final int PERMISSION_OK = 1;
  public static final int PERMISSION_NOT_OK = 2;
  public static final int PERMISSION_FETCH_FAILED = 3;
  public static final int PERMISSION_MISSING = 4;
  public static final int PERMISSION_REPOSITORY_ERROR = 5;
  public static final int PERMISSION_NOT_IN_CRAWL_SPEC = 6;
  public static final int PERMISSION_CRAWL_WINDOW_CLOSED = 7;

  private String url;
  private String host;
  private int status = PERMISSION_UNCHECKED;

  public PermissionRecord(String url, String host){
    if (url == null) {
      throw new IllegalArgumentException("Called with null url");
    }
    if (host == null) {
      throw new IllegalArgumentException("Called with null host");
    }
    this.url = url;
    this.host = host;
  }

  public String getUrl() {
    return url;
  }

  public String getHost() {
    return host;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status=status;
  }
}
