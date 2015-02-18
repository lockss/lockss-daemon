/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.remote.*;

/** The set of titles configured on the cache */
public class TitleSetInactiveAus extends BaseTitleSet {

  /** Create a TitleSet that consists of all configured titles
   * @param daemon used to get list of all known titles
   */
  public TitleSetInactiveAus(LockssDaemon daemon) {
    super(daemon, "All inactive AUs");
  }

  /** Return the titles in the set.
   * @return a collection of TitleConfig */
  public Collection<TitleConfig> getTitles() {
    Collection aus = daemon.getRemoteApi().getInactiveAus();
    ArrayList<TitleConfig> res = new ArrayList<TitleConfig>(aus.size());
    for (Iterator iter = aus.iterator(); iter.hasNext();) {
      InactiveAuProxy aup = (InactiveAuProxy)iter.next();
      res.add(titleConfigFromAu(aup));
    }
    res.trimToSize();
    return res;
  }

  /** Return the number of titles in the set that can be reactivated. */
  public int countTitles(int action) {
    switch (action) {
    case TitleSet.SET_REACTABLE:
      return daemon.getRemoteApi().getInactiveAus().size();
    case TitleSet.SET_ADDABLE:
    case TitleSet.SET_DELABLE:
      return 0;
    }
    return 0;
  }

  /** Return a TitleConfig for the AU.  Returns matching entry from the
   * title db if found, else creates one */
  TitleConfig titleConfigFromAu(InactiveAuProxy au) {
    PluginProxy plugin = au.getPlugin();
    String auname = au.getName();
    Configuration auConfig = au.getConfiguration();
    TitleConfig tc = AuUtil.findTitleConfig(auConfig, plugin.getPlugin());
    if (tc != null) {
      return tc;
    }

    tc = new TitleConfig(auname, plugin.getPluginId());
    ArrayList<ConfigParamAssignment> params = new ArrayList();
    for (Iterator iter = auConfig.keyIterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (!ConfigParamDescr.isReservedParam(key)) {
	String val = auConfig.get(key);
	ConfigParamDescr descr = plugin.findAuConfigDescr(key);
	if (descr != null) {
	  ConfigParamAssignment cpa = new ConfigParamAssignment(descr, val);
	  params.add(cpa);
	} else {
	  log.warning("Unknown parameter key: " + key + " in au: " + auname);
	}
      }
    }
    params.trimToSize();
    tc.setParams(params);
    return tc;
  }

  protected Collection<TitleConfig>
    filterTitles(Collection<TitleConfig> allTitles) {

    return allTitles;
  }

  protected int getActionables() {
    return SET_REACTABLE;
  }

  protected int getMajorOrder() {
    return 3;
  }

  public boolean equals(Object o) {
    return (o instanceof TitleSetInactiveAus);
  }

  public int hashCode() {
    return 0x272157;
  }

  public String toString() {
    return "[TS.InactiveAus]";
  }
}
