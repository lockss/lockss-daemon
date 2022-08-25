/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.util.urlconn;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.lang3.tuple.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;

/**
 * Maps a URL pattern to a CacheException, by first looking the URL up
 * in the PatternMap, then either returning the action specified by
 * the corresponding ResultAction or remapping that value through the
 * plugin's CacheResultMap (HttpResultMap).  Currently used only to
 * check redirect URLs - will need to be slightly generalized if/when
 * another use arises. */
public class AuHttpResultMap implements AuCacheResultMap {
  static Logger log = Logger.getLogger("AuHttpResultMap");

  /** A default instance specifying just the normal HttpResultMap
   * actions */
  public static final AuHttpResultMap DEFAULT =
    new AuHttpResultMap(new HttpResultMap(), PatternMap.EMPTY);

  PatternMap<ResultAction> urlMap;
  CacheResultMap resultMap;

  public AuHttpResultMap(CacheResultMap resultMap,
                         PatternMap<ResultAction> urlMap) {
    this.resultMap = resultMap;
    this.urlMap = urlMap;
  }

  /** Match a URL against the patterns in urlMap returning the value
   * returned by the corresponding ResultAction */
  public CacheException mapUrl(ArchivalUnit au,
                                    LockssUrlConnection connection,
                                    String origUrl,
                                    String toUrl,
                                    String message) {
    ResultAction ra = urlMap.getMatch(toUrl);
    if (ra == null) {
      return null;
    }
    try {
      CacheEvent evt = new CacheEvent.RedirectEvent(toUrl, message);
      return resultMap.triggerAction(au, origUrl, evt, ra, message);
    } catch (Exception e) {
      return new CacheException.UnknownExceptionException(e);
    }
  }

  /** Return a copy of the list of pairs in the PatternMap (for status
   * display) */
  public List<Pair<Pattern,ResultAction>> getUrlMapPairs() {
    return urlMap.getPairs();
  }

  public String toString() {
    return "[AuHRM: " + urlMap + "]";
  }
}
