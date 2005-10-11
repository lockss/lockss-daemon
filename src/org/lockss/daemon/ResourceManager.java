/*
 * $Id: ResourceManager.java,v 1.7 2005-10-11 05:44:15 tlipkis Exp $
 *

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.BaseLockssManager;
import org.lockss.app.LockssApp;
import org.lockss.app.LockssAppException;
import org.lockss.util.Logger;
import org.lockss.util.PlatformInfo;

/**
 * <p>Arbitrates ownership of resources such as TCP listen ports and
 * UDP ports.</p>
 * @author Tom Lipkis
 */
public class ResourceManager extends BaseLockssManager  {

  /**
   * <p>A logger for use by this class.</p>
   */
  protected static Logger log = Logger.getLogger("ResourceManager");

  /**
   * <p>A map keyed by resource identifiers; the values are ownership
   * tokens.</p>
   */
  private Map inUse;

  public void initService(LockssApp app) throws LockssAppException {
    super.initService(app);
    inUse = new HashMap();
  }

  /**
   * <p>Determines if a resource is available.</p>
   * @param resource A resource identifier.
   * @param token    An ownership token.
   * @return True if and only if the resource is not in use or
   *         the ownership token matches the resource's.
   */
  private boolean isAvailable(String resource, Object token) {
    // assumes synchronized
    Object curTok = inUse.get(resource);
    return curTok == null || curTok.equals(token);
  }

  /**
   * <p>Reserves a resource if it is available.</p>
   * @param resource A resource identifier.
   * @param token    An ownership token.
   * @return True if and only if the resource was available (and as a
   *         side effect was taken using the given token), or if it
   *         was in use and the ownership token matches the
   *         resource's.
   */
  private boolean reserve(String resource, Object token) {
    // assumes synchronized
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

  /**
   * <p>Releases a resource.</p>
   * @param resource A resource identifier.
   * @param token    An ownership token.
   * @return True if and only if the resource was not in use, or the
   *         resource was in use and the ownership token matches the
   *         resource's (and as a side effect the resource was
   *         released).
   */
  private boolean release(String resource, Object token) {
    // assumes synchronized
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

  /**
   * <p>Determines if a TCP port is available or already assigned to
   * the token.</p>
   * @param port  A TCP port number.
   * @param token An ownership token.
   * @return True if and only if the TCP port is available or if it is
   *         in use by the given token.
   */
  public synchronized boolean isTcpPortAvailable(int port, Object token) {
    return isAvailable(TCP_PREFIX + port, token);
  }

  /**
   * <p>Assigns a TCP port to the token if it is available.</p>
   * @param port  A TCP port number.
   * @param token An ownership token.
   * @return True if and only if the port is now assigned to the
   *         token.
   */
  public synchronized boolean reserveTcpPort(int port, Object token) {
    return reserve(TCP_PREFIX + port, token);
  }

  /**
   * <p>Releases a TCP port if it is assigned to the token.</p>
   * @param port  A TCP port number.
   * @param token An ownership token.
   * @return True if and only if the port is now available.
   **/
  public synchronized boolean releaseTcpPort(int port, Object token) {
    return release(TCP_PREFIX + port, token);
  }

  /**
   * <p>Determines if a UDP port is available or already assigned to
   * the token.</p>
   * @param port  A UDP port number.
   * @param token An ownership token.
   * @return True if and only if the TCP port is available or if it is
   *         in use by the given token.
   */
  public synchronized boolean isUdpPortAvailable(int port, Object token) {
    return isAvailable(UDP_PREFIX + port, token);
  }

  /**
   * <p>Assigns a UDP port to the token if it is available.</p>
   * @param port  A UDP port number.
   * @param token An ownership token.
   * @return True if and only if the port is now assigned to the
   *         token.
   */
  public synchronized boolean reserveUdpPort(int port, Object token) {
    return reserve(UDP_PREFIX + port, token);
  }

  /**
   * <p>Releases a UDP port if it is assigned to the token.</p>
   * @param port  A UDP port number.
   * @param token An ownership token.
   * @return True if and only if the port is now available.
   **/
  public synchronized boolean releaseUdpPort(int port, Object token) {
    return release(UDP_PREFIX + port, token);
  }

  /** Return list of unfiltered tcp ports not already assigned to another
   * server */
  public List getUsableTcpPorts(Object serverToken) {
    List unfilteredPorts = PlatformInfo.getInstance().getUnfilteredTcpPorts();
    if (unfilteredPorts == null || unfilteredPorts.isEmpty()) {
      return null;
    }
    List res = new ArrayList();
    for (Iterator iter = unfilteredPorts.iterator(); iter.hasNext(); ) {
      String str = (String)iter.next();
      try {
        int port = Integer.parseInt(str);
        if (isTcpPortAvailable(port, serverToken)){
          res.add(str);
        }
      } catch (NumberFormatException e) {
        // allow port number ranges, not checked for availability
        res.add(str);
      }
    }
    return res;
  }

  public List getUsableUdpPorts(Object serverToken) {
    List unfilteredPorts = PlatformInfo.getInstance().getUnfilteredUdpPorts();
    if (unfilteredPorts == null || unfilteredPorts.isEmpty()) {
      return null;
    }
    List res = new ArrayList();
    for (Iterator iter = unfilteredPorts.iterator(); iter.hasNext(); ) {
      String str = (String)iter.next();
      try {
        int port = Integer.parseInt(str);
        if (isUdpPortAvailable(port, serverToken)){
          res.add(str);
        }
      } catch (NumberFormatException e) {
        // allow port number ranges, not checked for availability
        res.add(str);
      }
    }
    return res;
  }

  /**
   * <p>An internal prefix to build TCP port identifiers.</p>
   */
  private static final String TCP_PREFIX = "tcp:";

  /**
   * <p>An internal prefix to build UDP port identifiers.</p>
   */
  private static final String UDP_PREFIX = "udp:";

}
