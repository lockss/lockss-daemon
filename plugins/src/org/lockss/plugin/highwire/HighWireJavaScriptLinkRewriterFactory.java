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
package org.lockss.plugin.highwire;

import java.io.IOException;
import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.StringFilter;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/**
 * Rewrites links in HighWire javascript code.  Handles specific cases as
 * the come up:
 * 
 * 1.PDF articles is displayed in a frameset. A document of the form: 
 * <i>http://jid.sagepub.com/cgi/framedreprint/13/1/55</i> or 
 * <i>http://archfami.ama-assn.org/cgi/reprintframed/3/9/747</i> have a 
 * window.onload handler that sets a timer, which sets window.location to a 
 * relative URL of the form: </i>/cgi/reprint/13/1/55.pdf</i>. This transform 
 * rewrites the relative URL to a ServeContent relative URL of the form 
 * <i>/ServeContent?url=http://jid.sagepub.com/cgi/reprint/13/1/55.pdf</i>.
 * 
 * 2. For any other document, this transform simply removes the window.onload
 * handler of the form: <i>window.onload = handleOnLoad;</i>.
 * 
 * Note that this really should be generalized, using watch handlers on
 * window.location.href and window.open(). This is a standard method on
 * Object since javascript 1.2. Unfortunately, some modern browsers,
 * including Safari 5.1.2 (JS 1.6) and IE 9 don't support it.  
 * 
 * There are several JS shims available (e.g. https://gist.github.com/384583) 
 * that provide the functionality for many browsers, but still not IE 9. This
 * will require further investigation before using any of them. 
 */
public class HighWireJavaScriptLinkRewriterFactory 
implements LinkRewriterFactory {
  static final Logger logger =
    Logger.getLogger("HighWireJavaScriptLinkRewriterFactory");
  public InputStream createLinkRewriter(
      String mimeType,ArchivalUnit au, InputStream in,
      String encoding, String url,
      ServletUtil.LinkTransform srvLinkXform)
  throws PluginException, IOException {
    logger.debug2("Rewriting " + url + " in AU " + au);
    
    String origStr, replaceStr;
    // highwire seems to use several URL paths for these pages
    int i = url.indexOf("/cgi/framedreprint/");
    if (i < 0) {
      i = url.indexOf("/cgi/reprintframed/");
    }
    if (i >= 0) {
      // Rewrite URL to go through LOCKSS ServeContent servlet 
      // example from: http://jid.sagepub.com/reprint/13/1/55
      // replace: window.location = "/cgi/reprint/" 
      // with: window.location = "/ServeContent?url=[[base_url]]/cgi/reprint/
      // Note: assumes RHS that is server relative; should be generalized
      // to handle page-relative and absolute addresses, as well as a
      // RHS that is a variable.
      origStr = "window.location = ";
      replaceStr =   "window.location = \"/ServeContent?url=" 
                   + url.substring(0,i) + "\" + ";
    } else {
      // disable setting the new URL
      origStr = "window.location = ";
      replaceStr = "/* window.location = */ ";
    }
    // HTML gets default URL rewriting
    StringFilter sf = new StringFilter(FilterUtil.getReader(in, encoding),
               origStr, replaceStr);
    sf.setIgnoreCase(true);
    return new ReaderInputStream(sf, encoding);
  }
}
