/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter;

import java.io.*;
import java.util.zip.*;

import org.lockss.test.LockssTestCase;

/*

$ echo "This is file1.txt" > file1.txt
$ echo "This is file2.txt" > file2.txt
$ touch empty3.txt
$ mkdir dir1
$ echo "This is dir1/file4.txt" > dir1/file4.txt
$ echo "This is dir1/file5.txt" > dir1/file5.txt
$ touch dir1/empty6.txt
$ zip -r /tmp/testzipfilterinputstream1.zip .
  adding: file2.txt (stored 0%)
  adding: file1.txt (stored 0%)
  adding: empty3.txt (stored 0%)
  adding: dir1/ (stored 0%)
  adding: dir1/empty6.txt (stored 0%)
  adding: dir1/file4.txt (stored 0%)
  adding: dir1/file5.txt (stored 0%)

*/

public class TestZipFilterInputStream extends LockssTestCase {

  public void testRunThrough() throws Exception {
    ZipInputStream zis = new ZipInputStream(getResourceAsStream("testzipfilterinputstream1.zip"));
    ZipFilterInputStream zfis = new ZipFilterInputStream(zis) {
      @Override
      public boolean keepZipEntry(ZipEntry zipEntry, String normalizedZipEntryName) {
        return true;
      }
    };
    byte[] buf = new byte[11];
    // file2.txt
    oneAssert(zfis, buf, 11, "file2.txt" + "Th");
    oneAssert(zfis, buf, 11, "is is file2");
    oneAssert(zfis, buf, 5, ".txt\n");
    // file1.txt
    oneAssert(zfis, buf, 11, "file1.txt" + "Th");
    oneAssert(zfis, buf, 11, "is is file1");
    oneAssert(zfis, buf, 5, ".txt\n");
    // empty3.txt
    oneAssert(zfis, buf, 10, "empty3.txt" + "");
    // dir1/
    oneAssert(zfis, buf, 5, "dir1/");
    // dir1/empty6.txt
    oneAssert(zfis, buf, 11, "dir1/empty6");
    oneAssert(zfis, buf, 4, ".txt");
    // dir1/file4.txt
    oneAssert(zfis, buf, 11, "dir1/file4.");
    oneAssert(zfis, buf, 11, "txt" + "This is ");
    oneAssert(zfis, buf, 11, "dir1/file4.");
    oneAssert(zfis, buf, 4, "txt\n");
    // dir1/file5.txt
    oneAssert(zfis, buf, 11, "dir1/file5.");
    oneAssert(zfis, buf, 11, "txt" + "This is ");
    oneAssert(zfis, buf, 11, "dir1/file5.");
    oneAssert(zfis, buf, 4, "txt\n");
    // EOF
    oneAssert(zfis, buf, -1, null);
    oneAssert(zfis, buf, -1, null);
    zfis.close();
    try {
      zfis.close();
    }
    catch (IOException expected) {
      assertEquals("stream closed", expected.getMessage());
    }
  }

  public void testSkipZeroLength() throws Exception {
    ZipInputStream zis = new ZipInputStream(getResourceAsStream("testzipfilterinputstream1.zip"));
    ZipFilterInputStream zfis = new ZipFilterInputStream(zis) {
      @Override
      public boolean keepZipEntry(ZipEntry zipEntry, String normalizedZipEntryName) {
        return zipEntry.getSize() != 0L;
      }
    };
    byte[] buf = new byte[11];
    // file2.txt
    oneAssert(zfis, buf, 11, "file2.txt" + "Th");
    oneAssert(zfis, buf, 11, "is is file2");
    oneAssert(zfis, buf, 5, ".txt\n");
    // file1.txt
    oneAssert(zfis, buf, 11, "file1.txt" + "Th");
    oneAssert(zfis, buf, 11, "is is file1");
    oneAssert(zfis, buf, 5, ".txt\n");
    // empty3.txt
    // ...nothing
    // dir1/
    // ...nothing (directory entries are inherently empty)
    // dir1/empty6.txt
    // ...nothing
    // dir1/file4.txt
    oneAssert(zfis, buf, 11, "dir1/file4.");
    oneAssert(zfis, buf, 11, "txt" + "This is ");
    oneAssert(zfis, buf, 11, "dir1/file4.");
    oneAssert(zfis, buf, 4, "txt\n");
    // dir1/file5.txt
    oneAssert(zfis, buf, 11, "dir1/file5.");
    oneAssert(zfis, buf, 11, "txt" + "This is ");
    oneAssert(zfis, buf, 11, "dir1/file5.");
    oneAssert(zfis, buf, 4, "txt\n");
    // EOF
    oneAssert(zfis, buf, -1, null);
    oneAssert(zfis, buf, -1, null);
    zfis.close();
    try {
      zfis.close();
    }
    catch (IOException expected) {
      assertEquals("stream closed", expected.getMessage());
    }
  }

  public void testSkipFiles() throws Exception {
    ZipInputStream zis = new ZipInputStream(getResourceAsStream("testzipfilterinputstream1.zip"));
    ZipFilterInputStream zfis = ZipFilterInputStream.skipFiles(zis,
                                                               "file2.txt",
                                                               "dir1/file4.txt",
                                                               "does/not/exist");
    byte[] buf = new byte[11];
    // file2.txt
    // ...nothing
    // file1.txt
    oneAssert(zfis, buf, 11, "file1.txt" + "Th");
    oneAssert(zfis, buf, 11, "is is file1");
    oneAssert(zfis, buf, 5, ".txt\n");
    // empty3.txt
    oneAssert(zfis, buf, 10, "empty3.txt" + "");
    // dir1/
    oneAssert(zfis, buf, 5, "dir1/");
    // dir1/empty6.txt
    oneAssert(zfis, buf, 11, "dir1/empty6");
    oneAssert(zfis, buf, 4, ".txt");
    // dir1/file4.txt
    // ...nothing
    // dir1/file5.txt
    oneAssert(zfis, buf, 11, "dir1/file5.");
    oneAssert(zfis, buf, 11, "txt" + "This is ");
    oneAssert(zfis, buf, 11, "dir1/file5.");
    oneAssert(zfis, buf, 4, "txt\n");
    // EOF
    oneAssert(zfis, buf, -1, null);
    oneAssert(zfis, buf, -1, null);
    zfis.close();
    try {
      zfis.close();
    }
    catch (IOException expected) {
      assertEquals("stream closed", expected.getMessage());
    }
  }
  
  public void testRemovesDotSlash() throws Exception {
    ZipInputStream zis = new ZipInputStream(getResourceAsStream("testzipfilterinputstream1.zip")) {
      @Override
      public ZipEntry getNextEntry() throws IOException {
        ZipEntry ze = super.getNextEntry();
        if (ze != null) {
          ze = new ZipEntry(ze) {
            @Override
            public String getName() {
              String name = super.getName();
              return name.startsWith("./") ? name : "./" + name;
            }
          };
        }
        return ze;
      }
    };
    ZipFilterInputStream zfis = new ZipFilterInputStream(zis) {
      @Override
      public boolean keepZipEntry(ZipEntry zipEntry, String normalizedZipEntryName) {
        assertTrue(zipEntry.getName().startsWith("./"));
        assertEquals(normalizedZipEntryName, zipEntry.getName().substring(2));
        return true;
      }
    };
    byte[] buf = new byte[11];
    // file2.txt
    oneAssert(zfis, buf, 11, "file2.txt" + "Th");
    oneAssert(zfis, buf, 11, "is is file2");
    oneAssert(zfis, buf, 5, ".txt\n");
    // file1.txt
    oneAssert(zfis, buf, 11, "file1.txt" + "Th");
    oneAssert(zfis, buf, 11, "is is file1");
    oneAssert(zfis, buf, 5, ".txt\n");
    // empty3.txt
    oneAssert(zfis, buf, 10, "empty3.txt" + "");
    // dir1/
    oneAssert(zfis, buf, 5, "dir1/");
    // dir1/empty6.txt
    oneAssert(zfis, buf, 11, "dir1/empty6");
    oneAssert(zfis, buf, 4, ".txt");
    // dir1/file4.txt
    oneAssert(zfis, buf, 11, "dir1/file4.");
    oneAssert(zfis, buf, 11, "txt" + "This is ");
    oneAssert(zfis, buf, 11, "dir1/file4.");
    oneAssert(zfis, buf, 4, "txt\n");
    // dir1/file5.txt
    oneAssert(zfis, buf, 11, "dir1/file5.");
    oneAssert(zfis, buf, 11, "txt" + "This is ");
    oneAssert(zfis, buf, 11, "dir1/file5.");
    oneAssert(zfis, buf, 4, "txt\n");
    // EOF
    oneAssert(zfis, buf, -1, null);
    oneAssert(zfis, buf, -1, null);
    zfis.close();
    try {
      zfis.close();
    }
    catch (IOException expected) {
      assertEquals("stream closed", expected.getMessage());
    }
  }

  protected void oneAssert(InputStream is,
                           byte[] buf,
                           int expectedRead,
                           String expectedString)
      throws IOException {
    assertEquals(expectedRead, is.read(buf));
    if (expectedRead != -1) {
      assertEquals(expectedString, new String(buf, 0, expectedRead));
    }
  }

}
