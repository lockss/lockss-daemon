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

package org.lockss.plugin.archiveit;

import java.net.*;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.base.BaseExploderHelper;

/**
 * This ExploderHelper encapsulates knowledge about the way
 * Archive-It delivers the files it collects in its crawls.
 * They are delivered as ARC files.
 *
 * If the input ArchiveEntry contains a name starting "http://"
 * and a set of header fields,  the baseUrl field is set to the host part
 * and the restOfUrl is set to the rest. The headerFields are left alone.
 * Otherwise, they are left null.  This conforms to the assumption in
 * Exploder that there is one AU per base URL.
 */
public class ArchiveItExploderHelper extends BaseExploderHelper {
  static Logger logger = Logger.getLogger("ArchiveItExploderHelper");

  public ArchiveItExploderHelper() {
  }

  public void process(ArchiveEntry ae) {
    String name = ae.getName();
    URL url;
    try {
      url = new URL(name);
      if (!"http".equals(url.getProtocol())) {
	logger.debug2("ignoring: " + url.toString());
	return;
      }
    } catch (MalformedURLException ex) {
      logger.debug2("Bad URL: " + (name == null ? "null" : name));
      return;
    }
    String baseUrl = null;
    String restOfUrl = null;
    // Put the content in an AU per host to match the assumption in Exploder
    baseUrl = "http://" + url.getHost();
    int port = url.getPort();
    if (port > 0 && port != 80) {
      baseUrl += ":" + port;
    }
    restOfUrl = url.getFile();
    logger.debug(ae.getName() + " mapped to " +
		 baseUrl + " plus " + restOfUrl);
    ae.setBaseUrl(baseUrl);
    ae.setRestOfUrl(restOfUrl);
    // XXX may be necessary to synthesize some header fields
    CIProperties props = new CIProperties();
    props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    ae.setAuProps(props);
  }

}
