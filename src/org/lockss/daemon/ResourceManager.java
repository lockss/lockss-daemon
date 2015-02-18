/*
 * $Id$
 *

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.*;

/**
 * <p>Arbitrates ownership of resources such as TCP listen ports and
 * UDP ports.</p>
 * @author Tom Lipkis
 */
public class ResourceManager extends BaseLockssManager  {

  /**
   * <p>A logger for use by this class.</p>
   */
  protected static Logger logger = Logger.getLogger("ResourceManager");

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
    boolean ret = false;
    if (curTok == null) {
      inUse.put(resource, token);
      logger.info("Resource " + resource + " now held by token " + token);
      ret = true;
    } else if (curTok.equals(token)) {
      logger.info("Resource " + resource + " still held by token " + token);
      ret = true;
    }
    return ret;
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
    boolean ret = false;
    if (curTok == null) {
      logger.info("Resource " + resource + " still not held by " + token);
      ret = true;
    } else if (curTok.equals(token)) {
      inUse.remove(resource);
      logger.info("Resource " + resource + " no longer held by " + token);
      ret = true;
    }
    return ret;
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

  protected interface PortAvailable {
    boolean isAvailable(int port, Object token);
  }

  protected List parseUsablePortList(List portList,
                                     Object serverToken,
                                     PortAvailable portAvailable) {
    if (portList == null || portList.isEmpty()) {
      return null;
    }

    List res = new ArrayList();
    for (Iterator iter = portList.iterator() ; iter.hasNext() ; ) {
      String str = (String)iter.next();
      try {
        // Try a single port
        int port = Integer.parseInt(str);
        if (portAvailable.isAvailable(port, serverToken)) {
          res.add(str);
        }
      } catch (NumberFormatException exc1) {
        try {
          // Try a port range
          Vector parts = StringUtil.breakAt(str, '-', 2);
          if (parts.size() != 2) {
            throw new NumberFormatException("Not a valid range: " + str);
          }
          int lo = Integer.parseInt((String)parts.get(0));
          int hi = Integer.parseInt((String)parts.get(1));
          ArrayList scratch = new ArrayList();
          for (int ix = lo ; ix <= hi ; ++ix) {
            if (portAvailable.isAvailable(ix, serverToken)) {
              scratch.add(Integer.toString(ix));
            }
          }
          if (scratch.size() == hi - lo + 1) {
            // Whole range available
            res.add(str);
          }
          else {
            // Some not available in range
            res.addAll(scratch);
          }
        }
        catch (NumberFormatException exc2) {
          // Neither a single port nor a port range
          res.add("(" + str + ")");
        }
      }
    }
    return res;
  }

  /** Return list of unfiltered tcp ports not already assigned to another
   * server */
  public List getUsableTcpPorts(Object serverToken) {
    List unfilteredPorts = PlatformUtil.getInstance().getUnfilteredTcpPorts();
    PortAvailable tcpAvailable = new PortAvailable() {
      public boolean isAvailable(int port, Object token) {
        return isTcpPortAvailable(port, token);
      }
    };
    return parseUsablePortList(unfilteredPorts, serverToken, tcpAvailable);
  }

  public List getUsableUdpPorts(Object serverToken) {
    List unfilteredPorts = PlatformUtil.getInstance().getUnfilteredUdpPorts();
    PortAvailable udpAvailable = new PortAvailable() {
      public boolean isAvailable(int port, Object token) {
        return isUdpPortAvailable(port, token);
      }
    };
    return parseUsablePortList(unfilteredPorts, serverToken, udpAvailable);
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
