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

/** The set of titles currently configured on the cache */
public class TitleSetActiveAus extends BaseTitleSet {

  /** Create a TitleSet that consists of the titles of all configured AUs
   * @param daemon used to get list of all known titles
   */
  public TitleSetActiveAus(LockssDaemon daemon) {
    super(daemon, "All active AUs");
  }

  /** Return the titles currently configured on the cache.
   * @return a collection of {@link TitleConfig} */
  public Collection<TitleConfig> getTitles() {
    PluginManager pmgr = daemon.getPluginManager();
    List<ArchivalUnit> aus = pmgr.getAllAus();
    ArrayList<TitleConfig> res = new ArrayList<TitleConfig>(aus.size());
    for (ArchivalUnit au : aus) {
      if (!pmgr.isInternalAu(au)) {
	res.add(titleConfigFromAu(au));
      }
    }
    res.trimToSize();
    return res;
  }

  /** Return the number of titles in the set that can be
   * delated/deactivated. */
  public int countTitles(int action) {
    switch (action) {
    case TitleSet.SET_DELABLE:
      PluginManager pmgr = daemon.getPluginManager();
      int res = 0;
      for (ArchivalUnit au : pmgr.getAllAus()) {
	if (!pmgr.isInternalAu(au)) {
	  res++;
	}
      }
      return res;
    case TitleSet.SET_REACTABLE:
    case TitleSet.SET_ADDABLE:
      return 0;
    }
    return 0;
  }

  /** Return a TitleConfig for the AU.  Returns matching entry from the
   * title db if found, else creates one.
   * @param au the AU
   * @return an existing or synthesized TitleConfig describing the AU */
  TitleConfig titleConfigFromAu(ArchivalUnit au) {
    TitleConfig tc = au.getTitleConfig();
    if (tc == null) {
      Plugin plugin = au.getPlugin();
      String auname = au.getName();
      tc = new TitleConfig(auname, plugin);
      Configuration auConfig = au.getConfiguration();
      ArrayList<ConfigParamAssignment> params = new ArrayList<ConfigParamAssignment>();
      for (Iterator iter = auConfig.keyIterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
 	if (!ConfigParamDescr.isReservedParam(key)) {
	  String val = auConfig.get(key);
	  ConfigParamDescr descr = plugin.findAuConfigDescr(key);
	  if (descr != null) {
	    ConfigParamAssignment cpa = new ConfigParamAssignment(descr, val);
	    params.add(cpa);
	  } else {
	    log.warning("Unknown (extra) parameter key: " + key +
			" in au: " + auname);
	  }
	}
      }
      params.trimToSize();
      tc.setParams(params);
    }
    return tc;
  }

  /** This method needs to be defined to satisfy the abstract base class,
   * but should never be called.
   * @throw UnsupportedOperationException
  */
  protected Collection<TitleConfig>
    filterTitles(Collection<TitleConfig> allTitles) {

    throw
      new UnsupportedOperationException("This method should never be called");
  }

  protected int getActionables() {
    return SET_DELABLE;
  }

  /** Sort this second */
  protected int getMajorOrder() {
    return 2;
  }

  public boolean equals(Object o) {
    return (o instanceof TitleSetActiveAus);
  }

  public int hashCode() {
    return 0x272057;
  }

  public String toString() {
    return "[TS.ActiveAus]";
  }
}
