/*
* $Id: MockIdentityManager.java,v 1.6 2004-02-06 03:12:07 troberts Exp $
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

import java.util.*;
import org.lockss.app.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * Mock override of IdentityManager.
 */
public class MockIdentityManager extends IdentityManager {
  public HashMap idMap = new HashMap();

  public Map agreeMap = new HashMap();

  public MockIdentityManager() { }
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    localIdentityStr = "127.1.2.3";
    makeLocalIdentity();
  }
  public void startService() { }
  public void stopService() {
    idMap = new HashMap();
  }

  public void changeReputation(IPAddr id, int changeKind) {
    idMap.put(id, new Integer(changeKind));
  }

  public int lastChange(IPAddr id) {
    Integer change = (Integer)idMap.get(id);
    if (change==null) {
      return -1;
    }
    return change.intValue();
  }

  public void signalAgreed(LcapIdentity id, ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void signalDisagreed(LcapIdentity id, ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public Map getAgreed(ArchivalUnit au) {
    return (Map)agreeMap.get(au);
  }
  
  public void setAgeedForAu(ArchivalUnit au, Map map) {
    agreeMap.put(au, map);
  }

  

}
