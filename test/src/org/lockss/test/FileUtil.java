/*
 * $Id: FileUtil.java,v 1.1 2002-08-31 06:58:16 tal Exp $
 *

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.util.*;
import java.io.*;
import java.net.*;

/** Utilities for Files
 */
public class FileUtil {
  /** Create and return the name of a temp file that will be deleted
   * when jvm terminated
   */
  public static File tempFile(String prefix)
      throws IOException {
    File f = File.createTempFile(prefix, null, null);
    f.deleteOnExit();
    return f;
  }

  /** Write  a temp file containing string and return its name */
  public static File writeTempFile(String prefix, String contents)
      throws IOException {
    File file = tempFile(prefix);
    FileWriter fw = new FileWriter(file);
    fw.write(contents);
    fw.close();
    return file;
  }

  /** Store the string in a temp file and return a file: url for it */
  public static String urlOfString(String s) throws IOException {
    File file = FileUtil.writeTempFile("test", s);
    URL url = new URL("file", null, file.getAbsolutePath());
    return url.toString();
  }
}
