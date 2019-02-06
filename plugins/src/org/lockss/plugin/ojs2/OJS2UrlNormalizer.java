/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class OJS2UrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
	private static final Pattern DOUBLE_ARG_PAT = Pattern.compile("gateway/c?lockss(\\?year=[0-9]+)\\1$");
	private static final String GATEWAY_PATH = "/gateway/";
	private static final Logger log = Logger.getLogger(OJS2UrlNormalizer.class);
	
	
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
	  
	 /* OJS bug that causes http -> https redirect to add second copy of year arg
	 * http://www.psychoanalyse-journal.ch/index.php/psychoanalyse/gateway/lockss?year=0
	 * becomes
	 * http://www.psychoanalyse-journal.ch/index.php/psychoanalyse/gateway/lockss?year=0?year=0
	 */
	  if (url.contains(GATEWAY_PATH)) {
		  log.debug3("found a doubled year argument");
		  Matcher dubMat = DOUBLE_ARG_PAT.matcher(url);
		  if (dubMat.find()) {
			  return StringUtils.substringBeforeLast(url,dubMat.group(1));
		  }
	  }
	  
    // EU-style cookies disclosure banner e.g. http://ojs.statsbiblioteket.dk/
    return StringUtils.substringBeforeLast(url, "?acceptCookies=1");
  }

}
