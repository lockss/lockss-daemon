/*
 * $Id: SimulatedHtmlFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.simulated;

import java.io.*;
import java.util.List;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.filter.*;
import org.lockss.plugin.*;

public class SimulatedHtmlFilterFactory implements FilterFactory {
  static final Logger log = Logger.getLogger("SimulatedHtmlFilterFactory");

  static final String CITATION_STRING = "Citation String";

  public InputStream createFilteredInputStream(ArchivalUnit au,
					       InputStream in,
					       String encoding) {

    List tagList = ListUtil.list(
        new HtmlTagFilter.TagPair("<!--", "-->", true),
        new HtmlTagFilter.TagPair("<script", "</script>", true),
        new HtmlTagFilter.TagPair("<", ">")
        );

    Reader rdr = FilterUtil.getReader(in, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(rdr, tagList);
    Reader citeFilter = new StringFilter(tagFilter, CITATION_STRING);

    InputStream res = new ReaderInputStream(new WhiteSpaceFilter(citeFilter));

    Configuration auConfig = au.getConfiguration();
    String filtThrow = auConfig.get(SimulatedPlugin.PD_FILTER_THROW.getKey());
    String msg = null;
    if (!StringUtil.isNullString(filtThrow)) {
      List<String> l = StringUtil.breakAt(filtThrow, ":");
      if (l.size() == 2) {
	msg = l.get(1);
      }
      filtThrow = l.get(0);
      try {
	Throwable th =
	  (Throwable)PrivilegedAccessor.invokeConstructor(filtThrow, msg);
	ThrowingInputStream tis = new ThrowingInputStream(res, null, null);
	if (th instanceof Error) {
	  tis.setErrorOnRead((Error)th);
	} else if (th instanceof IOException) {
	  tis.setThrowOnRead((IOException)th);
	}
	res = tis;
      } catch (Exception e) {
	log.error("Couldn't instantiate throwable: " + filtThrow, e);
      }
    }
    return res;
  }
}
