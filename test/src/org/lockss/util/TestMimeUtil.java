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

package org.lockss.util;

import org.lockss.test.*;

public class TestMimeUtil extends LockssTestCase {

  public void testGetExtensionFromMimeType() {
    assertEquals(".html", MimeUtil.getExtensionFromMimeType("text/html"));
    assertEquals(".html", MimeUtil.getExtensionFromMimeType("TEXT/HTML"));
    assertEquals(".html", MimeUtil.getExtensionFromMimeType("TEXT/html"));
    assertEquals(".html", MimeUtil.getExtensionFromMimeType("Text/Html"));
    assertEquals(".gif", MimeUtil.getExtensionFromMimeType("image/gif"));
    assertEquals(".jpeg", MimeUtil.getExtensionFromMimeType("image/jpeg"));
    assertEquals(".tiff", MimeUtil.getExtensionFromMimeType("image/tiff"));

    assertEquals(".xml", MimeUtil.getExtensionFromMimeType("APPLICATION/XML"));
    assertEquals(".xml", MimeUtil.getExtensionFromMimeType("application/xml"));
    assertEquals(".pdf", MimeUtil.getExtensionFromMimeType("application/pdf"));
    assertEquals(".zip", MimeUtil.getExtensionFromMimeType("application/zip"));
    assertEquals(".tar",
		 MimeUtil.getExtensionFromMimeType("application/x-tar"));
    assertEquals(".gtar",
		 MimeUtil.getExtensionFromMimeType("application/x-gtar"));
    assertEquals(".ps",
		 MimeUtil.getExtensionFromMimeType("application/postscript"));
  }

  public void testGetExtensionFromContentType() {
    assertEquals(".html", MimeUtil.getExtensionFromContentType("text/html"));
    assertEquals(".html",
		 MimeUtil.getExtensionFromContentType("text/html;charset=utf-8"));
    assertEquals(".html",
		 MimeUtil.getExtensionFromContentType("TEXT/HTML;CHARSET=FOO"));
  }

  public void testGetMimeTypeFromExtension() {
    assertEquals("text/html", MimeUtil.getMimeTypeFromExtension(".html"));
    assertEquals("text/html", MimeUtil.getMimeTypeFromExtension("HTML"));
    assertEquals("text/html", MimeUtil.getMimeTypeFromExtension(".HTM"));
    assertEquals("text/html", MimeUtil.getMimeTypeFromExtension("htm"));
    assertEquals("image/gif", MimeUtil.getMimeTypeFromExtension(".gif"));
    assertEquals("image/jpeg", MimeUtil.getMimeTypeFromExtension(".jpeg"));
    assertEquals("image/jpeg", MimeUtil.getMimeTypeFromExtension(".JPG"));
    assertEquals("image/tiff", MimeUtil.getMimeTypeFromExtension(".tiff"));
    assertEquals("image/tiff", MimeUtil.getMimeTypeFromExtension(".tif"));

    assertEquals("application/zip", MimeUtil.getMimeTypeFromExtension(".zip"));
    assertEquals("application/x-tar",
		 MimeUtil.getMimeTypeFromExtension(".tar"));
    assertEquals("application/x-gtar",
		 MimeUtil.getMimeTypeFromExtension(".gtar"));
    assertEquals("application/x-gtar",
		 MimeUtil.getMimeTypeFromExtension(".tgz"));
    assertEquals("application/postscript",
		 MimeUtil.getMimeTypeFromExtension(".ps"));
    assertEquals("application/postscript",
		 MimeUtil.getMimeTypeFromExtension(".eps"));
  }
}
