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

package org.lockss.test;

import java.io.*;
import org.lockss.util.*;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.plugin.CachedUrl;

public class RunHashTest {

  public static void main(String argv[]) {
    String dir = argv[0];
    long bytesRead = 0;
    System.err.println("Beginning read: "+TimeBase.nowDate());
    long startTime = TimeBase.nowMs();
    try {
      bytesRead = readDir(dir);
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.err.println("Finished read: "+TimeBase.nowDate());
    long endTime = TimeBase.nowMs();
    System.err.println("Read "+bytesRead+" bytes");
    System.err.println(bytesRead/(endTime-startTime)+" b/ms");
  }

  private static long readDir(String dir) throws IOException {
    File directory = new File(dir);
    return readDir(directory);
  }

  private static long readDir(File directory) throws IOException {
    byte bytes[] = new byte[256];
    File files[] = directory.listFiles();
    long bytesRead = 0;
    for (int ix=0; ix < files.length; ix++) {
      if (files[ix].isFile()) {
	InputStream is = getInputStream(files[ix]);
	bytesRead += readStream(is, bytes);
      } else if (files[ix].isDirectory()) {
	bytesRead += readDir(files[ix]);
      }
    }
    return bytesRead;
  }

  private static InputStream getInputStream(File file)
      throws FileNotFoundException {
    Reader reader = new BufferedReader(new FileReader(file));
    HtmlTagFilter.TagPair tagPair = new HtmlTagFilter.TagPair("<", ">");
    Reader filteredReader = new HtmlTagFilter(reader, tagPair);
    return new ReaderInputStream(filteredReader);
//     return new ReaderInputStream(reader);
  }

  private static long readStream(InputStream is, byte bytes[])
      throws IOException {
    long bytesRead = 0;
    long curRead = 0;
    while ((curRead = is.read(bytes)) != -1) {
      bytesRead += curRead;
    }
    return bytesRead;
  }

  private void readContent(CachedUrl cu, byte bytes[]) {
    try {
      InputStream is = cu.openForHashing();
      while (is.read(bytes) != -1) {}
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
