/*
 * $Id: ResourceManager.java,v 1.1 2004-10-18 03:35:12 tlipkis Exp $
 *

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

package org.lockss.daemon;
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.util.*;

/** ResourceManager arbitrates ownership of resources such as tcp listen
 * ports. 

*/

public class ResourceManager extends BaseLockssManager  {
  protected static Logger log = Logger.getLogger("ResourceManager");

  private Map inUse;

  public void startService() {
    super.startService();
    inUse = new HashMap();
  }

  public void stopService() {
    super.stopService();
  }

  // assumes synchronized
  private boolean isAvailable(String resource, Object token) {
    Object curTok = inUse.get(resource);
    return curTok == null || curTok.equals(token);
  }

  // assumes synchronized
  private boolean reserve(String resource, Object token) {
    Object curTok = inUse.get(resource);
    if (curTok == null) {
      inUse.put(resource, token);
      return true;
    } else if (curTok.equals(token)) {
      return true;
    } else {
      return false;
    }
  }

  // assumes synchronized
  private boolean release(String resource, Object token) {
    Object curTok = inUse.get(resource);
    if (curTok == null) {
      return true;
    } else if (curTok.equals(token)) {
      inUse.remove(resource);
      return true;
    } else {
      return false;
    }
  }

  /** Return true if the tcp port is available, or already assigned to the
   * token */
  public synchronized boolean isTcpPortAvailable(int port, String token) {
    return isAvailable("tcp:" + port, token);
  }

  /** Assign the tcp port to the token iff not already assigned, and return
   * true if the port is now assigned to the token */
  public synchronized boolean reserveTcpPort(int port, Object token) {
    return reserve("tcp:" + port, token);
  }

  /** Release the port if it is assigned to the token, return true if it is
   * now available */
  public synchronized boolean releaseTcpPort(int port, Object token) {
    return release("tcp:" + port, token);
  }
}
