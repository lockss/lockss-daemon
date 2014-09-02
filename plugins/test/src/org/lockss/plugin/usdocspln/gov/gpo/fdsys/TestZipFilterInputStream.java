/*
 * $Id: TestZipFilterInputStream.java,v 1.1 2014-09-02 17:59:43 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;
import java.util.zip.*;

import org.lockss.test.LockssTestCase;

public class TestZipFilterInputStream extends LockssTestCase {

  /*

$ echo "This is ./file1.txt" > file1.txt
$ echo "This is ./file2.txt" > file2.txt
$ mkdir dir1
$ echo "This is ./dir1/file3.txt" > dir1/file3.txt
$ echo "This is ./dir1/file4.txt" > dir1/file4.txt
$ zip -r /tmp/sample.zip .
  adding: dir1/ (stored 0%)
  adding: dir1/file3.txt (stored 0%)
  adding: dir1/file4.txt (stored 0%)
  adding: file2.txt (stored 0%)
  adding: file1.txt (stored 0%)

  */
  
  public void testRunThrough() throws Exception {
    ZipInputStream zis = new ZipInputStream(getResourceAsStream("sample.zip"));
    ZipFilterInputStream zfis = new ZipFilterInputStream(zis) {
      @Override
      public boolean keepZipEntry(ZipEntry zipEntry) {
        return true;
      }
    };
    byte[] buf = new byte[8];
    oneAssert(zfis, buf, 8, "This is ");
    oneAssert(zfis, buf, 8, "./dir1/f");
    oneAssert(zfis, buf, 8, "ile3.txt");
    oneAssert(zfis, buf, 1, "\n");
    oneAssert(zfis, buf, 8, "This is ");
    oneAssert(zfis, buf, 8, "./dir1/f");
    oneAssert(zfis, buf, 8, "ile4.txt");
    oneAssert(zfis, buf, 1, "\n");
    oneAssert(zfis, buf, 8, "This is ");
    oneAssert(zfis, buf, 8, "./file2.");
    oneAssert(zfis, buf, 4, "txt\n");
    oneAssert(zfis, buf, 8, "This is ");
    oneAssert(zfis, buf, 8, "./file1.");
    oneAssert(zfis, buf, 4, "txt\n");
    oneAssert(zfis, buf, -1, null);
    oneAssert(zfis, buf, -1, null);
    zfis.close();
    try {
      zfis.close();
    }
    catch (IOException ioe) {
      assertEquals("Stream closed", ioe.getMessage());
    }
  }
  
  public void testWithSkippedEntries() throws Exception {
    ZipInputStream zis = new ZipInputStream(getResourceAsStream("sample.zip"));
    ZipFilterInputStream zfis = new ZipFilterInputStream(zis) {
      @Override
      public boolean keepZipEntry(ZipEntry zipEntry) {
        return !zipEntry.getName().contains("dir1");
      }
    };
    byte[] buf = new byte[8];
    oneAssert(zfis, buf, 8, "This is ");
    oneAssert(zfis, buf, 8, "./file2.");
    oneAssert(zfis, buf, 4, "txt\n");
    oneAssert(zfis, buf, 8, "This is ");
    oneAssert(zfis, buf, 8, "./file1.");
    oneAssert(zfis, buf, 4, "txt\n");
    oneAssert(zfis, buf, -1, null);
    oneAssert(zfis, buf, -1, null);
    zfis.close();
    try {
      zfis.close();
    }
    catch (IOException ioe) {
      assertEquals("Stream closed", ioe.getMessage());
    }
  }

  protected void oneAssert(InputStream is,
                           byte[] buf,
                           int expectedRead,
                           String expectedString)
      throws IOException {
    assertEquals(expectedRead, is.read(buf));
    if (expectedRead != -1) {
      for (int i = 0 ; i < expectedRead ; ++i) {
        assertEquals(expectedString.charAt(i), (char)buf[i]);
      }
    }
  }
  
}
