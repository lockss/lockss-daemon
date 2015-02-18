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

package org.lockss.util;

import java.io.*;
import java.net.*;
import org.lockss.util.urlconn.LockssUrlConnection;

/**
 * IO utilities that don't belong elsewhere.  <p>The
 * <code>safeClose()</code> methods call <code>close()</code> on their
 * argument, suppressing and ignoring any errors that result (including
 * passing in a null pointer).  They are intended for use in error handling
 * cleanup code, where one wants to ensure that relevant streams, etc. get
 * closed, but where each <code>close()</code> call would have to be
 * protected by a try-catch block to avoid errors derailing the
 * higher-level error processing.
 *
 * <pre>
 *  try {
 *   instream = new InputStream(...);
 *   ...
 *  } catch (FooException e) {
 *   log(foo);
 *   IOUtil.safeClose(inStream);
 *   IOUtil.safeClose(outStream);
 *   IOUtil.safeClose(socket);
 *  }</pre>
 */

public class IOUtil {
  /** No instances */
  private IOUtil() {
  }

  /** Call close() on the stream, ignoring any errors */
  public static void safeClose(InputStream s) {
    // No need for null check, NPE will be ignored
    try {
      s.close();
    } catch (Exception e) {}
  }

  /** Call close() on the stream, ignoring any errors */
  public static void safeClose(OutputStream s) {
    try {
      s.close();
    } catch (Exception e) {}
  }

  /** Call close() on the Reader, ignoring any errors */
  public static void safeClose(Reader s) {
    try {
      s.close();
    } catch (Exception e) {}
  }

  /** Call close() on the Writer, ignoring any errors */
  public static void safeClose(Writer s) {
    try {
      s.close();
    } catch (Exception e) {}
  }

  /** Call close() on the Socket, ignoring any errors */
  public static void safeClose(Socket s) {
    try {
      s.close();
    } catch (Exception e) {}
  }

  /** Call close() on the ServerSocket, ignoring any errors */
  public static void safeClose(ServerSocket s) {
    try {
      s.close();
    } catch (Exception e) {}
  }
  
  /** Call close() on the RandomAccessFile, ignoring any errors */
  public static void safeClose(RandomAccessFile f) {
    try {
      f.close();
    } catch (Exception e) {}
  }
  /** Call release() on the LockssUrlConnection, ignoring any errors */
  public static void safeRelease(LockssUrlConnection conn) {
    try {
      conn.release();
    } catch (Exception e) {}
  }
}
