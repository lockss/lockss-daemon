package org.lockss.hasher;

import java.security.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.ByteArray;

public class TestHashBlock extends LockssTestCase {
  
  private HashBlock makeHashBlock() {
    MockCachedUrl cu = new MockCachedUrl("http://foo.com/x.html");
    return new HashBlock(cu);
  }
  
  private void addVersion(HashBlock block, int versionNum, String input)
      throws Exception {
    MessageDigest[] digests = new MessageDigest[1];
    digests[0] = MessageDigest.getInstance("MD5");
    digests[0].update(input.getBytes());
    
    block.addVersion(0, 100, 0, 100, 200, digests, versionNum, null);    
  }
  
  private void addVersion(HashBlock block, int versionNum) throws Exception {
    addVersion(block, versionNum, "foobarbazquux");
  }

  public void testByteCount() throws Exception {
    HashBlock block = makeHashBlock();
    addVersion(block, 1);
    addVersion(block, 2);
    addVersion(block, 3);

    assertEquals(600, block.getTotalHashedBytes());
    assertEquals(300, block.getTotalFilteredBytes());
  }
  
  public void testArraySortOrder() throws Exception {
    HashBlock block = makeHashBlock();
    addVersion(block, 100);
    addVersion(block, 50);
    addVersion(block, 1);
    addVersion(block, 75);
    addVersion(block, 10);

    HashBlock.Version[] versions = block.getVersions();
    
    // Repository versions start at the oldest, are incremented as new
    // versions are added...
    assertEquals(1, versions[4].getRepositoryVersion());   // Oldest...
    assertEquals(10, versions[3].getRepositoryVersion());
    assertEquals(50, versions[2].getRepositoryVersion());
    assertEquals(75, versions[1].getRepositoryVersion());
    assertEquals(100, versions[0].getRepositoryVersion()); // most recent (current)
  }
  
  public void testIteratorSortOrder() throws Exception {
    HashBlock block = makeHashBlock();
    addVersion(block, 5);
    addVersion(block, 3);
    addVersion(block, 1);
    addVersion(block, 4);
    addVersion(block, 2);
    addVersion(block, 0);

    Iterator iter = block.versionIterator();

    int idx = 5;
    while (iter.hasNext()) {
      HashBlock.Version version = (HashBlock.Version)iter.next();
      assertEquals(idx--, version.repositoryVersion);
    }
  }
  
  public void testSortedVersions() throws Exception {
    String[] expected = {
      "N7UdGUp1E+RbVvZSTy1R8g==",
      "N7UdGUp1E+RbVvZSTy1R8g==",
      "U26/Ks0PtsL3PpixMQn7bw==",
      "c/7/pLf2u2jkTPmEyF9uiA==",
      "c/7/pLf2u2jkTPmEyF9uiA==",
      "rL0Y20zC+Fzt72VPzMSk2A=="};

    HashBlock block = makeHashBlock();
    addVersion(block, 100, "foo");
    addVersion(block, 50, "bar");
    addVersion(block, 1, "bar");
    addVersion(block, 75, "baz");
    addVersion(block, 10, "baz");
    addVersion(block, 6, "yab");
    
    Comparator<HashBlock.Version> comparator =
      new Comparator<HashBlock.Version>() {
        public int compare(HashBlock.Version o1, HashBlock.Version o2) {
	  byte[] hash1 = o1.getHashes()[0];
	  byte[] hash2 = o2.getHashes()[0];
	  return ByteArray.lexicographicalCompare(hash1, hash2);
	}
    };

    assertEquals(4, block.countUniqueVersions(comparator));

    HashBlock.Version[] versions = block.sortedVersions(comparator);
    assertEquals(expected.length, versions.length);

    for (int idx = 0; idx < expected.length; idx++) {
      assertEquals(expected[idx],
		   ByteArray.toBase64(versions[idx].getHashes()[0]));
    }
  }
}
