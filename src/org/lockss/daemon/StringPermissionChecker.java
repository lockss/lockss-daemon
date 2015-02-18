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
package org.lockss.daemon;

import java.io.*;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.filter.*;

public class StringPermissionChecker extends BasePermissionChecker {
  static Logger m_logger = Logger.getLogger("PermissionCheck");
  public static final int IGNORE_CASE = 0;

  String m_matchString;
  BitSet m_flags = new BitSet(8);
  FilterRule m_filter;
  String m_encoding;
  AuState.AccessType accessType;
  boolean setAccessType = false;

  public StringPermissionChecker(String matchString, FilterRule filterRule) {
      m_matchString = matchString;
      m_filter = filterRule;
      m_encoding = Constants.DEFAULT_ENCODING;
      setFlag(IGNORE_CASE, true);
  }

  public StringPermissionChecker(String matchString) {
    this(matchString, null);
  }

  public void setFlag(int bitNumber, boolean setOn) {
    if(setOn) {
      m_flags.set(bitNumber);
    }
    else {
      m_flags.clear(bitNumber);
    }
  }

  public void setEncoding(String encoding) {
    m_encoding = encoding;
  }

  public void doSetAccessType(AuState.AccessType type) {
    this.accessType = type;
    setAccessType = true;
  }

  public boolean checkPermission(Crawler.CrawlerFacade crawlFacade,
				 Reader reader, String permissionUrl) {
    boolean res =  checkPermission0(crawlFacade, reader, permissionUrl);
    if (res && setAccessType) {
      setAuAccessType(crawlFacade, accessType);
    }
    return res;
  }

  private boolean checkPermission0(Crawler.CrawlerFacade crawlFacade,
				  Reader reader, String permissionUrl) {
    if (m_filter != null) {
      try {
	reader = m_filter.createFilteredReader(reader);
      } catch (PluginException e) {
	m_logger.warning("Plugin error checking permission at " +
			 permissionUrl, e);
	return false;
      }
      m_logger.debug3("Creating filtered reader to check permissions");
    }

    try {
      return StringUtil.containsString(reader, m_matchString,
				       m_flags.get(IGNORE_CASE));
    } catch (IOException ex) {
      m_logger.warning("Error checking permission at " + permissionUrl, ex);
    }

    return false;
  }

  static public class StringFilterRule implements FilterRule {
    public Reader createFilteredReader(Reader reader) {
      Reader filteredReader = StringFilter.makeNestedFilter(reader,
          new String[][] { {"<br>", " "} , {"&nbsp;", " "} } , true);
      return new WhiteSpaceFilter(filteredReader);
    }
  }
}
