/*
 * $Id$
 */

/*

Copyright (c) 2010 Board of Trustees of Leland Stanford Jr. University,
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


/**
 * Parse a host:port string.  Finds last colon to allow for IPv6 addresses
 */
public class HostPortParser {
  private String host;
  private int port;

  public HostPortParser(String spec) throws InvalidSpec {
    if (StringUtil.isNullString(spec) || isDirect(spec)) {
      return;
    }
    int pos = spec.lastIndexOf(':');
    if (pos < 0) {
      throw new InvalidSpec("host:port spec doesn't contain colon");
    }
    host = spec.substring(0, pos);
    if (StringUtil.isNullString(host)) {
      throw new InvalidSpec("host:port spec doesn't contain host");
    }
    try {
      port = Integer.parseInt(spec.substring(pos +1));
    } catch (NumberFormatException e) {
      throw new InvalidSpec("host:port spec port not number");
    }
  }

  @Override
  public String toString() {
    return host + ":" + port;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public static boolean isDirect(String spec) {
    return "DIRECT".equalsIgnoreCase(spec) || "NONE".equalsIgnoreCase(spec);
  }

  public class InvalidSpec extends Exception {
    public InvalidSpec(String msg) {
      super(msg);
    }
  }

}
