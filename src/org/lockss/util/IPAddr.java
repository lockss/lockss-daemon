/*
 * $Id$
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

package org.lockss.util;

import java.util.*;
import java.net.*;


/**
 * This class is used to wrap InetAddress instances, in order to intercept
 * and prevent unnecessary calls to the resolver.  Most of the methods
 * simply forward to the wrapped InetAddress
 */
public class IPAddr implements java.io.Serializable {
  private static boolean exerciseDNS = false;
  private InetAddress ina;

  /** Create a new IPAddr from an existing InetAddress.  Should be used
   * only to wrap InetAddresses extracted from datagrams, sockets, etc.  To
   * create an IPAddr from a hostname or address, use {@link
   * #getByName(String)}, {@link #getAllByName(String)} or {@link
   * #getLocalHost()}
   * @param ina an existing InetAddresses
   * @throws NullPointerException if ina is null
   */
  public IPAddr(InetAddress ina) {
    if (ina == null) {
      throw new NullPointerException();
    }
    this.ina = ina;
  }

  /** Set the wrapper class to maximize the use of DNS, by doing reverse
   * lookups in toString().
   * @param val if true, will minimize the use of DNS */
  public static void setExerciseDNS(boolean val) {
    exerciseDNS = val;
  }

  /** Extract the wrapped InetAddress to pass to a socket, datagram,
   * etc. */
  public InetAddress getInetAddr() {
    return ina;
  }

  public boolean isMulticastAddress() {
    return ina.isMulticastAddress();
  }

  public boolean isLoopbackAddress() {
    byte[] bytes = ina.getAddress();
    return bytes[0] == 127 && bytes[1] == 0 && bytes[2] == 0 && bytes[3] != 0;
  }

  public String getHostName() {
    return ina.getHostName();
  }

  public byte[] getAddress() {
    return ina.getAddress();
  }

  public String getHostAddress() {
    return ina.getHostAddress();
  }

  public int hashCode() {
    return ina.hashCode();
  }

  public boolean equals(Object obj) {
    return (obj != null) && (obj instanceof IPAddr) &&
      ina.equals(((IPAddr)obj).getInetAddr());
  }

  public String toString() {
    if (exerciseDNS) {
      return ina.toString();
    } else {
      return ina.getHostAddress();
    }
  }

  public static IPAddr getByAddress(int[] ipBytes)
      throws UnknownHostException {
    return getByAddress( new byte[] {
        (byte)ipBytes[0], (byte)ipBytes[1], (byte)ipBytes[2], (byte)ipBytes[3]
    });
  }

  public static IPAddr getByAddress(byte[] ipBytes)
      throws UnknownHostException {
    return new IPAddr(InetAddress.getByAddress(ipBytes));
  }

  public static IPAddr getByName(String host) throws UnknownHostException {
    return new IPAddr(InetAddress.getByName(host));
  }

  public static IPAddr[] getAllByName(String host)
      throws UnknownHostException {
    InetAddress[] all = InetAddress.getAllByName(host);
    int len = all.length;
    IPAddr[] res = new IPAddr[len];
    for (int ix = 0; ix < len; ix++) {
      res[ix] = new IPAddr(all[ix]);
    }
    return res;
  }

  public static IPAddr getLocalHost() throws UnknownHostException {
    return new IPAddr(InetAddress.getLocalHost());
  }

  public static boolean isLoopbackAddress(String addr) {
    try {
      return getByName(addr).isLoopbackAddress();
    } catch (Exception e) {
      return false;
    }
  }
}
