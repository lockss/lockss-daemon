/*
* $Id: AuUrl.java,v 1.1 2003-02-04 23:50:21 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller;
import java.io.*;
import java.net.*;

/** This class just contains the one-time initialization necessary so that
 * LOCKSSAU: URLs can be created.  It can only be done once per JVM, so is
 * in a separate class to avoid problems that occur when running multiple
 * junit tests in the same JVM. */
public class AuUrl {

  /** Set up the URLStreamHandlerFactory that understands the LOCKSSAU:
   * protocol */
  public static void init() {
    URL.setURLStreamHandlerFactory(new AuUrlFactory());
  }

  // This allows creation of URLs with the LOCKSSAU: protocol.  They are
  // never opened, so the stream factory doesn't need to do anything.
  private static class AuUrlFactory implements URLStreamHandlerFactory {
    public URLStreamHandler createURLStreamHandler(String protocol) {
      if ("lockssau".equalsIgnoreCase(protocol)) {
	return new URLStreamHandler() {
	    protected URLConnection openConnection(URL u) throws IOException {
	      return null;
	    }};
      } else {
	return null;	 // use default stream handlers for other protocols
      }
    }
  }
}
