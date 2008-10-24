/*
 * $Id: MockPollSpec.java,v 1.10 2008-10-24 07:11:44 tlipkis Exp $
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

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.app.*;
import org.lockss.test.*;

/**
 * MockPollSpec can be created from an AUID, will create mock AU and Plugin
 * as necessary
 */

public class MockPollSpec extends PollSpec {
  private static Logger log = Logger.getLogger("MockPollSpec");

  private String overridePluginVersion = null;
  private boolean nullCUS = false;

  /**
   * Constructor for a "mock" poll spec, for debugging
   * @param auId the archival unit id
   * @param url the url
   * @param lwrBound the lower bound of the url
   * @param uprBound the upper bound of the url
   * @param pollType one of the types defined by Poll
   */
  public MockPollSpec(String auId, String url,
		      String lwrBound, String uprBound,
		      int pollType) {
    this(auId, url, lwrBound, uprBound, null, pollType);
  }

  public MockPollSpec(ArchivalUnit au, String url,
		      String lwrBound, String uprBound,
		      int pollType) {
    super(makeCus(au, url, lwrBound, uprBound, null),
	  lwrBound, uprBound, pollType);
  }

  public MockPollSpec(CachedUrlSet cus, String lwrBound,
                      String uprBound, int pollType) {
    super(cus, lwrBound, uprBound, pollType);
  }

  public MockPollSpec(String auId, String url,
		      String lwrBound, String uprBound, String pluginVersion,
		      int pollType) {
    super(makeCus(auId, url, lwrBound, uprBound, pluginVersion),
	  lwrBound, uprBound, pollType);
  }

  public MockPollSpec(PollSpec ps, int pollType) {
    super(ps, pollType);
  }

  public MockPollSpec(CachedUrlSet cus, int pollType) {
    super(cus, pollType);
  }

  public String getPluginVersion() {
    return (overridePluginVersion != null)
      ? overridePluginVersion : super.getPluginVersion();
  }

  public void setPluginVersion(String version) {
    overridePluginVersion = version;
  }

  public void setNullCUS(boolean val) {
    nullCUS = val;
  }

  public CachedUrlSet getCachedUrlSet() {
    if (nullCUS) {
      return null;
    }
    return super.getCachedUrlSet();
  }

  private static CachedUrlSet makeCus(String auId, String url,
				      String lwrBound, String uprBound,
				      String pluginVersion) {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setAuId(auId);
    return makeCus(au, url, lwrBound, uprBound, pluginVersion);
  }

  private static CachedUrlSet makeCus(ArchivalUnit au, String url,
				      String lwrBound, String uprBound,
				      String pluginVersion) {
    Plugin plug = au.getPlugin();
    if (plug == null) {
      log.warning("Making plugin");
      if (au instanceof MockArchivalUnit) {
	MockArchivalUnit mau = (MockArchivalUnit)au;
	MockPlugin mplug = new MockPlugin();
	mau.setPlugin(mplug);
	plug = mplug;
      } else {
	throw
	  new RuntimeException("Can't add plugin to a non MockArchivalUnit");
      }
    }
    if (pluginVersion != null) {
      if (plug instanceof MockPlugin) {
	((MockPlugin)plug).setVersion(pluginVersion);
      } else {
	throw
	  new RuntimeException("Can't add plugin version to a non MockPlugin");
      }
    }
    CachedUrlSet cus =
      au.makeCachedUrlSet(new RangeCachedUrlSetSpec(url, lwrBound, uprBound));
    return cus;
  }
}
