/*
 * $Id: TestVoteBlock.java,v 1.2 2007-05-09 10:34:06 smorabito Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol;

import java.util.Iterator;

import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.poller.*;

public class TestVoteBlock extends LockssTestCase {
  
  private byte[] testBytes = ByteArray.makeRandomBytes(20);
  
  public void testConstructor() throws Exception {
    VoteBlock vb;
    vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
  }

  public void testSize() throws Exception {
    VoteBlock vb;

    // One version.
    vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    vb.addVersion(0, 0, 0, 0, testBytes, testBytes, false);
    assertEquals(1, vb.size());
    
    // Eight versions.
    vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    for (int ix = 0; ix < 8; ix++) {
      vb.addVersion(0, 0, 0, 0, testBytes, testBytes, false);
    }
    assertEquals(8, vb.size());
    
    // 33 versions.
    vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    for (int ix = 0; ix < 33; ix++) {
      vb.addVersion(0, 0, 0, 0, testBytes, testBytes, false);
    }
    assertEquals(33, vb.size());
  }
  
  public void testGetVoteType() throws Exception {
    VoteBlock vb;
    vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    assertEquals(VoteBlock.CONTENT_VOTE, vb.getVoteType());
    assertEquals("Content", vb.getVoteTypeString());
    vb = new VoteBlock("foo");
    assertEquals(VoteBlock.CONTENT_VOTE, vb.getVoteType());
    assertEquals("Content", vb.getVoteTypeString());
    vb = new VoteBlock("foo", VoteBlock.HEADER_VOTE);
    assertEquals(VoteBlock.HEADER_VOTE, vb.getVoteType());
    assertEquals("Header", vb.getVoteTypeString());
    vb = new VoteBlock("foo", VoteBlock.METADATA_VOTE);
    assertEquals(VoteBlock.METADATA_VOTE, vb.getVoteType());
    assertEquals("Metadata", vb.getVoteTypeString());
    vb = new VoteBlock("foo", 99);
    assertEquals(99, vb.getVoteType());
    assertEquals("Unknown", vb.getVoteTypeString());
  }
  
  public void testSetAndGetVersion() throws Exception {
    VoteBlock vb;
    vb = new VoteBlock("foo");
    
    vb.addVersion(0, 1, 0, 1, testBytes, testBytes, false);
    vb.addVersion(0, 2, 0, 2, testBytes, testBytes, false);
    vb.addVersion(0, 3, 0, 3, testBytes, testBytes, false);
    vb.addVersion(0, 4, 0, 4, testBytes, testBytes, false);
    
    assertEquals(4, vb.size());
    
    VoteBlock.Version v1a = vb.currentVersion();
    VoteBlock.Version v1b = vb.getVersion(0);
    VoteBlock.Version v2 = vb.getVersion(1);
    VoteBlock.Version v3 = vb.getVersion(2);
    VoteBlock.Version v4 = vb.getVersion(3);

    assertEquals(v1a, v1b);
    assertEquals(1, v1a.getUnfilteredLength());
    assertEquals(2, v2.getUnfilteredLength());
    assertEquals(3, v3.getUnfilteredLength());
    assertEquals(4, v4.getUnfilteredLength());
  }
  
  public void testCurrentVersion() throws Exception {
    VoteBlock vb1, vb2;
    vb1 = new VoteBlock("foo");
    vb1.addVersion(0, 1024, 0, 1024,
                   ByteArray.makeRandomBytes(20),
                   ByteArray.makeRandomBytes(20), false);
    vb1.addVersion(0, 2048, 0, 2048,
                   ByteArray.makeRandomBytes(20),
                   ByteArray.makeRandomBytes(20), false);
    assertSame(vb1.currentVersion(), vb1.getVersion(0));
  }
  
  public void testGetNonExistentVersionReturnsNull() throws Exception {
    VoteBlock vb1 = new VoteBlock("foo");
    assertNull(vb1.getVersion(-1));
    assertNull(vb1.getVersion(99));
  }
  
  public void testIterator() throws Exception {
    VoteBlock vb = new VoteBlock("foo");
    for (int ix = 0; ix < 10; ix++) {
      vb.addVersion(0, 1024, 0, 1024,
                    ByteArray.makeRandomBytes(20),
                    ByteArray.makeRandomBytes(20), false);
    }
    
    Iterator iter = vb.versionIterator();

    for (int ix = 0; ix < 10; ix++) {
      assertTrue(iter.hasNext());
      VoteBlock.Version ver = (VoteBlock.Version)iter.next();
      assertNotNull(ver);
      assertEquals(1024, ver.getFilteredLength());
      assertEquals(1024, ver.getUnfilteredLength());
    }
    
    assertFalse(iter.hasNext());
    try {
      iter.next();
      fail("Should have thrown NoSuchElementException.");
    } catch (java.util.NoSuchElementException ignore) {}
  }
  
  public void testGetVersions() throws Exception {
    VoteBlock vb = new VoteBlock("foo");
    for (int ix = 0; ix < 10; ix++) {
      vb.addVersion(0, ix, 0, ix,
                    ByteArray.makeRandomBytes(20),
                    ByteArray.makeRandomBytes(20), false);
    }
    
    VoteBlock.Version[] versions = vb.getVersions();
    assertEquals(10, versions.length);
    for (int ix = 0; ix < versions.length; ix++) {
      VoteBlock.Version ver = versions[ix];
      assertNotNull(ver);
      assertEquals(ix, ver.getFilteredLength());
      assertEquals(ix, ver.getUnfilteredLength());
    }
  }
  
  public void testEncodeDecode() throws Exception {
    VoteBlock vb1, vb2;
    vb1 = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    vb1.addVersion(0, 1024, 0, 1024,
                   ByteArray.makeRandomBytes(20),
                   ByteArray.makeRandomBytes(20), false);
    vb1.addVersion(0, 2048, 0, 2048,
                   ByteArray.makeRandomBytes(20),
                   ByteArray.makeRandomBytes(20), false);
                   
    // Encode.
    byte[] enc = vb1.getEncoded();
    
    assertNotNull(enc);
    
    // Decode.
    vb2 = new VoteBlock(enc);
    assertEquals(vb1.url, vb2.url);
    assertEquals(vb1.size(), vb2.size());
    
    assertEquals(vb1.currentVersion(), vb2.currentVersion());
    assertEquals(vb1.getVersion(0), vb2.getVersion(0));
    assertEquals(vb1.getVersion(1), vb2.getVersion(1));
  }
  
}
