/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.truezip;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

// A more complete (functional) test of zip file processing is in
// TestArchiveMembers

public class TestTFileCache extends LockssTestCase {

  File tmpDir;
  TFileCache tfc;
  MockArchivalUnit mau;
  MockCachedUrl mcu[];
  TFileCache.Entry ent[];

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    tmpDir = new File(tempDirPath);
    tfc = new TFileCache(tempDirPath);

    mau = new MockArchivalUnit(new MockPlugin());
    mau.setArchiveFileTypes(ArchiveFileTypes.DEFAULT);

    setupMockAus(20);

  }

  List<String> dirList() {
    return ListUtil.fromArray(tmpDir.list());
  }


  String url(int n) {
    return "http://example.com/path/file" + n + ".zip";
  }

  String content(int n) {
    if (n > 10) {
      return "small content";
    }
    return StringUtils.repeat(Integer.toString(n), n*n*n)
      + " shall be the number of the counting";
  }

  void setupMockAus(int n) {
    mcu = new MockCachedUrl[n];
    ent = new TFileCache.Entry[n];
    for (int ix = 0; ix < n; ix++) {
      mcu[ix] = mau.addUrl(url(ix), content(ix));
    }
  }

  MockCachedUrl copy(MockCachedUrl mcu) {
    MockCachedUrl res = new MockCachedUrl(mcu.getUrl(), mcu.getArchivalUnit());
    res.setContent(mcu.getContent());
    return res;
  }

  void assertFile(int n) {
    assertTrue("File " + n + " should exist", ent[n].ctf.exists());
  }

  void assertNoFile(int n) {
    assertFalse("File " + n + " should not exist", ent[n].ctf.exists());
  }

  void assertValid(int n) {
    assertTrue("Entry " + n + " should be valid", ent[n].isValid());
    assertFile(n);
  }

  void assertInvalid(int n) {
    assertFalse("Entry " + n + " should not be valid", ent[n].isValid());
    assertNoFile(n);
  }


  public void testOne() throws Exception {
    tfc.setMaxSize(800, 5);
    assertEquals(Collections.EMPTY_LIST, dirList());
    ent[0] = tfc.getCachedTFileEntry(mcu[0]);
    assertValid(0);
    assertEquals(36, ent[0].size);
    String names[] = tmpDir.list();
    assertEquals(1, names.length);
    String name0 = names[0];
    assertEquals(name0, ent[0].ctf.getName());
    assertEquals(0, tfc.getCacheHits());
    assertEquals(1, tfc.getCacheMisses());
    assertEquals(36, tfc.getCurrentSize());

    CachedUrl c0 = copy(mcu[0]);
    assertNotSame(c0, mcu[0]);
    TFileCache.Entry e0 = tfc.getCachedTFileEntry(c0);
    assertSame(ent[0], e0);
    assertEquals(2, e0.refCnt);
    assertEquals(1, tfc.getCacheMisses());
    assertEquals(1, tfc.getCacheHits());
    assertEquals(1, tmpDir.list().length);

    ent[1] = tfc.getCachedTFileEntry(mcu[1]);
    assertEquals(73, tfc.getCurrentSize());
    ent[2] = tfc.getCachedTFileEntry(mcu[8]);
    assertEquals(621, tfc.getCurrentSize());
    ent[3] = tfc.getCachedTFileEntry(mcu[5]);
    assertEquals(782, tfc.getCurrentSize());

    c0 = copy(mcu[0]);
    assertNotSame(c0, mcu);
    e0 = tfc.getCachedTFileEntry(c0);
    assertSame(ent[0], e0);
    assertEquals(3, e0.refCnt);
    assertEquals(4, tfc.getCacheMisses());
    assertEquals(2, tfc.getCacheHits());
    assertEquals(4, tmpDir.list().length);

    assertValid(0);
    assertValid(1);
    assertValid(2);
    assertValid(3);
    ent[4] = tfc.getCachedTFileEntry(mcu[7]);
    assertValid(0);
    assertInvalid(1);
    assertInvalid(2);
    assertValid(3);
    assertEquals(576, tfc.getCurrentSize());

    e0 = tfc.getCachedTFileEntry(c0);
    assertSame(ent[0], e0);

    ent[5] = tfc.getCachedTFileEntry(mcu[6]);

    assertEquals(3, tmpDir.list().length);
    CachedUrl c1 = copy(mcu[1]);
    assertNotSame(c1, mcu[1]);
    TFileCache.Entry e1 = tfc.getCachedTFileEntry(c1);
    assertNotSame(ent[1], e1);
    assertEquals(1, e1.refCnt);
    assertEquals(7, tfc.getCacheMisses());
    assertEquals(3, tfc.getCacheHits());
    assertEquals(4, tmpDir.list().length);

    assertInvalid(3);
    assertEquals(704, tfc.getCurrentSize());

    // add some tiny files, which should cause flushes due to max number of
    // files in cache
    ent[6] = tfc.getCachedTFileEntry(mcu[11]);
    assertEquals(4, tmpDir.list().length);
    ent[7] = tfc.getCachedTFileEntry(mcu[12]);
    assertEquals(4, tmpDir.list().length);
    ent[8] = tfc.getCachedTFileEntry(mcu[13]);
    assertEquals(4, tmpDir.list().length);
    assertEquals(10, tfc.getCacheMisses());
    assertEquals(3, tfc.getCacheHits());
  }

  public void testIrregularSplitZipNumbering() throws IOException {
    CachedUrl zipCu = mau.addUrl("http://a.b/foo.zip", "main zip");

    List<String> parts = new ArrayList<>();

    for (int i = 1; i <= 9; i++) {
      storePart(mau, "0" + i);
    }
    for (int i = 10; i <= 99; i++) {
      storePart(mau, "" + i);
    }
    for (int i = 100; i <= 110; i++) {
      storePart(mau, "" + i);
    }

    File tmpdir = getTempDir();

    tfc.copyParts(zipCu, 2, "splitzip.", tmpdir);

    Set<String> zipparts = Arrays.stream(tmpdir.listFiles())
      .map(f -> f.getName())
      .collect(Collectors.toSet());

    assertEquals(110, zipparts.size());
    for (int i = 1; i <= 110; i++) {
      assertContains(zipparts, String.format("splitzip.z%02d", i));
    }
  }

  void storePart(ArchivalUnit au, String suffix) throws IOException {
    String name = "http://a.b/foo.z" + suffix;
    CachedUrl addedCu = mau.addUrl(name, "part " + suffix);
  }
}
