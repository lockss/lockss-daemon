/*
 * $Id: PollSpec.java,v 1.1 2003-02-25 21:52:54 tal Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;

public class PollSpec {
  private String auId;
  private String pluginId;
  private String url;
  private String uprBound = null;
  private String lwrBound = null;
  private CachedUrlSet cus;
  private int opcode;			// want this here?

  /** Construct a PollSpec from a CachedUrlSet */
  public PollSpec(CachedUrlSet cus) {
    ArchivalUnit au = cus.getArchivalUnit();
    auId = au.getAUId();
    pluginId = au.getPluginId();
    CachedUrlSetSpec cuss = cus.getSpec();
    url = cuss.getUrl();
    if (cuss instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec rcuss = (RangeCachedUrlSetSpec)cuss;
      lwrBound = rcuss.getLowerBound();
      uprBound = rcuss.getUpperBound();
    }
  }

  /** Construct a PollSpec from a CachedUrlSet and poll opcode */
  public PollSpec(CachedUrlSet cus, int pollOpcode) {
    this(cus);
    opcode = pollOpcode;
  }

  /** Construct a PollSpec from an incoming message */
  public PollSpec(LcapMessage msg) {
  }

  // logically this should be here, but it needs the PluginManager, which
  // would make it harder to test.  So at least for now the logic is in
  // PluginManager instead.
//    public CachedUrlSet getCachedUrlSet() {
//    }

  public String getPluginId() {
    return pluginId;
  }
  public String getAUId() {
    return auId;
  }
  public String getUrl() {
    return url;
  }
  public String getLwrBound() {
    return lwrBound;
  }
  public String getUprBound() {
    return uprBound;
  }

}


