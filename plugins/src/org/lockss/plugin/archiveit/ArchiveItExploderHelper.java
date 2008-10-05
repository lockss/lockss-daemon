/*
 * $Id: ArchiveItExploderHelper.java,v 1.2 2008-05-06 21:35:36 dshr Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.archiveit;

import java.util.*;
import java.net.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.crawler.Exploder;

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
public class ArchiveItExploderHelper implements ExploderHelper {
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
