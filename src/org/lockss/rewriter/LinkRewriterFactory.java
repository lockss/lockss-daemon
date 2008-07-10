/*
 * $Id: LinkRewriterFactory.java,v 1.3 2008-07-10 03:50:32 dshr Exp $
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

package org.lockss.rewriter;

import java.io.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.daemon.PluginException;

/** Factory to create an InputStream that rewrites URLs */
public interface LinkRewriterFactory {
  /**
   * Create an InputStream that rewrites links for the MimeType in question.
   * @param mimeType the MIME type in which to rewrite URLs
   * @param au the ArchivalUnit the stream comes from
   * @param in the InputStream to be rewritten
   * @param encoding the character encoding to use when reading, or null
   * @param url the url to which in is attached
   */
  public InputStream createLinkRewriter(String mimeType,
					ArchivalUnit au,
					InputStream in,
					String encoding,
					String url)
      throws PluginException;
  /**
   * Create an InputStream that rewrites links for the MimeType in question.
   * @param mimeType the MIME type in which to rewrite URLs
   * @param au the ArchivalUnit the stream comes from
   * @param in the InputStream to be rewritten
   * @param encoding the character encoding to use when reading, or null
   * @param url the url to which in is attached
   */
  public Reader createLinkRewriterReader(String mimeType,
					 ArchivalUnit au,
					 Reader in,
					 String encoding,
					 String url)
      throws PluginException;
}
