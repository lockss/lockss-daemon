/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
