package org.lockss.hasher;

import java.security.*;
import java.util.*;

import org.lockss.test.*;

public class TestHashBlock extends LockssTestCase {
  
  private HashBlock makeHashBlock() {
    MockCachedUrl cu = new MockCachedUrl("http://foo.com/x.html");
    return new HashBlock(cu);
  }
  
  private void addVersion(HashBlock block, int versionNum) throws Exception {
    MessageDigest[] digests = new MessageDigest[1];
    digests[0] = MessageDigest.getInstance("MD5");
    digests[0].update("foobarbazquux".getBytes());
    
    block.addVersion(0, 100, 0, 100, digests, versionNum, null);    
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
}
