/*
 * $Id: ClockssSubscriptionProbe.java,v 1.1 2006-09-22 06:21:51 tlipkis Exp $
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

package org.lockss.clockss;

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.crawler.PermissionMap;

/**
 * Logic for 2-IP-address CLOCKSS subscription probe.
 */
public class ClockssSubscriptionProbe {
  private static Logger log = Logger.getLogger("ClockssSubscriptionProbe");

  public static final int PROBE_NONE = 0;
  public static final int PROBE_INST = 1;
  public static final int PROBE_CLOCKSS = 2;

  private ArchivalUnit au;
  private int probeState = PROBE_NONE;
  private boolean executed = false;

  public ClockssSubscriptionProbe(ArchivalUnit au) {
    this.au = au;
  }

  // State machine to control the local address we fetch from, and whether
  // to retry from the other address.  There are two UrlCacher methods that
  // might open a connection to be opened to the server: cache() and
  // getUncachedInputStream().  The wrappers for those methods call
  // setupAddr before forwarding the call.

  boolean setupAddr(UrlCacher uc) {
    if (executed) {
      // already succeeded or failed, don't change state, don't retry
      return false;
    }
    AuState aus = AuUtil.getAuState(au);
    ClockssParams mgr = AuUtil.getDaemon(au).getClockssParams();
    switch (aus.getClockssSubscriptionStatus()) {
    case AuState.CLOCKSS_SUB_UNKNOWN:
    case AuState.CLOCKSS_SUB_YES:
    case AuState.CLOCKSS_SUB_INACCESSIBLE:
      switch (probeState) {
      case PROBE_NONE:
	uc.setLocalAddress(mgr.getInstitutionSubscriptionAddr());
	probeState = PROBE_INST;
	return true;
      case PROBE_INST:
	uc.setLocalAddress(mgr.getClockssSubscriptionAddr());
	probeState = PROBE_CLOCKSS;
	return true;
      case PROBE_CLOCKSS:
	return false;
      default:
	log.error("Unexpected probeState: " + probeState);
      }
      return false;
//     case AuState.CLOCKSS_SUB_YES:
//       uc.setLocalAddress(mgr.getInstitutionSubscriptionAddr());
//       return true;
    case AuState.CLOCKSS_SUB_NO:
      switch (probeState) {
      case PROBE_NONE:
	uc.setLocalAddress(mgr.getClockssSubscriptionAddr());
	probeState = PROBE_CLOCKSS;
	return true;
      case PROBE_CLOCKSS:
      default:
	return false;
      }
    default:
      log.error("Unknown subscription state: " +
		aus.getClockssSubscriptionStatus());
    }
    return false;
  }

  // XXX this isn't quite right.  If we're fetching the permission page,
  // subscription status shouldn't be updated until the permissions are
  // found, probe page checked, etc.  This probably has to be done at a
  // higher level, e.g, in the crawler.
  void updateSubscriptionStatus(boolean worked) {
    executed = true;
    AuState aus = AuUtil.getAuState(au);
    if (worked) {
      switch (aus.getClockssSubscriptionStatus()) {
      case AuState.CLOCKSS_SUB_UNKNOWN:
      case AuState.CLOCKSS_SUB_INACCESSIBLE:
	switch (probeState) {
	case PROBE_INST:
	  aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
	  break;
	case PROBE_CLOCKSS:
	  aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
	  break;
	default:
	  log.error("Unexpected probeState: " + probeState);
	}
	break;
      case AuState.CLOCKSS_SUB_YES:
	switch (probeState) {
	case PROBE_INST:
	  break;
	case PROBE_CLOCKSS:
	  aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
	  break;
	default:
	  log.error("Unexpected probeState: " + probeState);
	}
	break;
      case AuState.CLOCKSS_SUB_NO:
	// If we determined we don't have a subscription, should we change
	// our mind here?
	break;
      default:
	log.error("Unknown subscription state: " +
		  aus.getClockssSubscriptionStatus());
      }
    } else {
      aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    }
  }

  public void reset() {
    executed = false;
    probeState = PROBE_NONE;
  }

  public String toString() {
    return "[ClockssProbe: " + au + "]";
  }
}
