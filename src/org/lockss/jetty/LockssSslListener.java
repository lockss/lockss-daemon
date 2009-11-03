/*
 * $Id: LockssSslListener.java,v 1.2.4.2 2009-11-03 23:52:01 edwardsb1 Exp $
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
// Some portions of this code are:
// ========================================================================
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.

package org.lockss.jetty;

import java.net.*;
import java.security.*;
import javax.net.ssl.*;

import org.mortbay.http.*;
import org.mortbay.util.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.util.*;


/**
 * Extension of org.mortbay.http.SslListener that works with an externally
 * supplied, initialized, KeyManagerFactory.
 */
public class LockssSslListener extends SslListener {
  private static Logger log = Logger.getLogger("LockssSslListener");

  private KeyManagerFactory _keyManagerFactory;

  public LockssSslListener() {
    super();
  }

  public LockssSslListener(InetAddrPort p_address) {
    super(p_address);
  }

  public void setKeyManagerFactory(KeyManagerFactory fact) {
    _keyManagerFactory = fact;
  }

  public KeyManagerFactory getKeyManagerFactory() {
    return _keyManagerFactory;
  }

  protected SSLServerSocketFactory createFactory() throws Exception {
    if (_keyManagerFactory == null) {
      return super.createFactory();
    }
    RandomManager rmgr = LockssDaemon.getLockssDaemon().getRandomManager();
    SecureRandom rng = rmgr.getSecureRandom();

    SSLContext context = SSLContext.getInstance(getProtocol());
    context.init(_keyManagerFactory.getKeyManagers(),
		 null, rng);
    return context.getServerSocketFactory();
  }
}
