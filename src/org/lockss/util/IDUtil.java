/*
 * $Id: IDUtil.java,v 1.3 2006-01-12 00:48:39 tlipkis Exp $
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

package org.lockss.util;

import java.util.*;
import org.lockss.protocol.*;

public class IDUtil {

  private static final String V3_TCP_KEY_PREFIX =
    IdentityManager.V3_ID_PROTOCOL_TCP +
    IdentityManager.V3_ID_PROTOCOL_SUFFIX +
    IdentityManager.V3_ID_TCP_ADDR_PREFIX;

  private static final String V3_TCP_KEY_MIDDLE =
    IdentityManager.V3_ID_TCP_ADDR_SUFFIX +
    IdentityManager.V3_ID_TCP_IP_PORT_SEPARATOR;

  public static String ipAddrToKey(String addr, int port) {
    if (port == 0) {
      // V1 key is IPAddr
      return addr;
    }
    return ipAddrToKey(addr, String.valueOf(port));
  }

  public static String ipAddrToKey(String addr, String port) {
    // V3 key is TCP:[ip]:port
    return V3_TCP_KEY_PREFIX + addr + V3_TCP_KEY_MIDDLE + port;
  }

  public static String ipAddrToKey(IPAddr addr, int port) {
    return ipAddrToKey(addr.getHostAddress(), port);
  }
}
