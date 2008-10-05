package org.lockss.plugin.archiveit;

import org.lockss.test.LockssTestCase;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.util.CIProperties;

public class TestArchiveItExploderHelper extends LockssTestCase {

  private static final String[] urls = {
    "http://archiveit.lockss.org/index.html",
    "http://archiveit.lockss.org:8081/index.html",
    "http://foobar.archive.org/foo/bar/index.html",
    "http://user:passwd@archiveit.lockss.org:8081/index.html",
  };
  private static final String[] hosts = {
    "http://archiveit.lockss.org",
    "http://archiveit.lockss.org:8081",
    "http://foobar.archive.org",
    "http://archiveit.lockss.org:8081",
  };
  private static final String[] files = {
    "/index.html",
    "/index.html",
    "/foo/bar/index.html",
    "/index.html",
  };
    
  public void testProcessCorrectEntry() throws Exception {
    for (int i = 0; i < urls.length; i++) {
      ArchiveEntry ae = new ArchiveEntry(urls[i], 7654, 0, null, null);
      ArchiveItExploderHelper eeh = new ArchiveItExploderHelper();
      
      eeh.process(ae);
      assertEquals(hosts[i], ae.getBaseUrl());
      assertEquals(files[i], ae.getRestOfUrl());
      assertTrue(7654 == ae.getSize());
      // XXX - check addText
      // XXX - check auProps
    }
  }

  private static final String[] badUrls = {
    "htt://archiveit.lockss.org/index.html",
    "http://archiveit.lockss.org:barf/index.html",
    "ftp://host/foo/bar/index.html",
  };

  public void testProcessIncorrectEntry() throws Exception {
    for (int i = 0; i < badUrls.length; i++) {
      ArchiveEntry ae = new ArchiveEntry(badUrls[i], 7654, 0, null, null);
      ArchiveItExploderHelper eeh = new ArchiveItExploderHelper();
      
      eeh.process(ae);
      assertNotNull(ae);
      assertTrue(null == ae.getBaseUrl());
      assertTrue(null == ae.getRestOfUrl());
    }
  }
}

