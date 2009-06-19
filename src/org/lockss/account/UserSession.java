/*
 * $Id: UserSession.java,v 1.2 2009-06-19 08:27:25 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.account;

import java.util.*;

import org.lockss.config.*;
import org.lockss.util.*;

/** Simple record of user login sessions
 */
public class UserSession {
  String name;
  String runningServlet;
  String reqHost;
  long loginTime;
  long idleTime;

  public UserSession(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setRunningServlet(String val) {
    runningServlet = val;
  }

  public String getReqHost() {
    return reqHost;
  }

  public void setReqHost(String val) {
    reqHost = val;
  }

  public String getRunningServlet() {
    return runningServlet;
  }

  public void setLoginTime(long val) {
    loginTime = val;
  }

  public long getLoginTime() {
    return loginTime;
  }

  public void setIdleTime(long val) {
    idleTime = val;
  }

  public long getIdleTime() {
    return idleTime;
  }
}
