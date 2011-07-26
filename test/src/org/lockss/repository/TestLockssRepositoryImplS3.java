/*
 * $Id: TestLockssRepositoryImplS3.java,v 1.1.2.7 2011-07-26 19:48:00 dshr Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.UserAuthenticator;
import org.apache.commons.vfs.auth.*;
import org.apache.commons.vfs.impl.*;
import com.intridea.io.vfs.provider.s3.S3FileProvider;

/**
 * This is the test class for org.lockss.daemon.LockssRepositoryImpl
 * using S3 and "compatible" services. The following properties must
 * be set to use service "foo":
 * - org.lockss.repository.service=foo
 * - org.lockss.repository.foo.host
 * - org.lockss.repository.foo.accessKey
 * - org.lockss.repository.foo.secretKey
 * Since the tests depend on external services being available and
 * configured, if the properties are not set the tests succeed with
 * a warning.
 */

// XXX Status - 7/16/11 - all tests passing with Walrus & S3

public class TestLockssRepositoryImplS3 extends LockssTestCase {
  private static Logger logger = Logger.getLogger("TestLockssRepositoryImplS3");
  private MockLockssDaemon daemon;
  private RepositoryManager repoMgr;
  private LockssRepositoryImpl repo;
  private MockArchivalUnit mau;
  private String tempDirPath;
  private String tempDirURI;

  // Services
  private static final String[] serviceNames = { "s3", "ias3", "walrus"};
  private static String serviceName = "bogus";


  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    repoMgr = daemon.getRepositoryManager();
  }

  public void tearDown() throws Exception {
    if (repo != null) {
      repo.stopService();
    }
    super.tearDown();
  }

  String getCacheLocation() {
    return LockssRepositoryImpl.getCacheLocation();
  }

  boolean propertiesAreSet() {
    boolean ret = false;
    //  XXX using environment variables for now
    Map<String, String> env = System.getenv();
    String host = null;
    String accessKey = null;
    String secretKey = null;
    // XXX the system properties below don't work - they need to
    // XXX be set in src/jets3t.properties. Need to fix this.
    if ("s3".equals(serviceName)) {
      host = env.get("S3_HOST");
      accessKey = env.get("EC2_ACCESS_KEY");
      secretKey = env.get("EC2_SECRET_KEY");
      if (host != null && accessKey != null && secretKey != null) {
        System.setProperty("s3service.https-only", "true");
      }
    } else if ("ias3".equals(serviceName)) {
      host = env.get("IAS3_HOST");
      accessKey = env.get("IAS3_ACCESS_KEY");
      secretKey = env.get("IAS3_SECRET_KEY");
      if (host != null && accessKey != null && secretKey != null) {
        System.setProperty("s3service.https-only", "false");
        System.setProperty("s3service.s3-endpoint-http-port", "82");
      }
    } else if ("walrus".equals(serviceName)) {
      host = env.get("WALRUS_HOST");
      accessKey = env.get("WALRUS_ACCESS_KEY");
      secretKey = env.get("WALRUS_SECRET_KEY");
      if (host != null && accessKey != null && secretKey != null) {
        System.setProperty("s3service.https-only", "true");
        System.setProperty("s3service.s3-endpoint", host);
        System.setProperty("s3service.s3-endpoint-http-port", "8773");
        System.setProperty("s3service.s3-endpoint-https-port", "8443");
        System.setProperty("s3service.disable-dns-buckets", "true");
        System.setProperty("s3service.s3-endpoint-virtual-path", "/services/Walrus");
      }
    }
    Configuration config = ConfigManager.getCurrentConfig();
    String prefix = RepositoryManager.PREFIX + serviceName;
    logger.debug("propertiesAreSet: " + prefix);
    logger.debug("propertiesAreSet: " + host + " + " + accessKey + " + " + secretKey);
    ret = (host != null && accessKey != null && secretKey != null);
    if (ret) {
      Properties props = new Properties();
      props.put(prefix + ".host", host);
      props.put(prefix + ".accessKey", accessKey);
      props.put(prefix + ".secretKey", secretKey);
      props.put(RepositoryManager.PREFIX + "service", serviceName);
      tempDirURI =  "s3://test.lockss.org" + File.separator + "unit" +
        File.separator + ((new Date()).getTime()/1000);
      props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
  		      tempDirURI);
      if (true) // XXX
        props.setProperty("org.lockss.defaultCommonsLogLevel", "debug");
      ConfigurationUtil.setCurrentConfigFromProps(props);
      mau = new MockArchivalUnit();
      repo =
	(LockssRepositoryImpl)LockssRepositoryImpl.createNewLockssRepository(
        mau);
      repo.setNodeCacheSize(17);
      repo.initService(daemon);
    }
    return ret;
  }

  public void testGetLocalRepository() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetLocalRepository();
    }
  }
  public void doTestGetLocalRepository() throws Exception {
    logger.debug("doTestGetLocalRepository: " + serviceName);
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }

    mau = new MockArchivalUnit();
    repo =
      (LockssRepositoryImpl)LockssRepositoryImpl.createNewLockssRepository(
      mau);
    repo.setNodeCacheSize(17);
    repo.initService(daemon);
    // set small node cache; one test needs to fill it up
    LockssRepositoryImpl.LocalRepository localRepo =
      LockssRepositoryImpl.getLocalRepository(mau);
    assertNotNull("Failed to create LocalRepository for: " + mau, localRepo);
    logger.debug3("tempDirURI: " + tempDirURI + " localRepo: " + localRepo.getRepositoryPath());
    assertEquals(tempDirURI, localRepo.getRepositoryPath());

    String tempDir2 = getTempDir().getAbsolutePath() + File.separator;
    MockArchivalUnit mau2 = new MockArchivalUnit();
    mau2.setConfiguration(ConfigurationUtil.fromArgs
			  (PluginManager.AU_PARAM_REPOSITORY,
			   repoSpec(tempDir2)));
    LockssRepositoryImpl.LocalRepository localRepo2 =
      LockssRepositoryImpl.getLocalRepository(mau2);
    assertNotNull("Failed to create LocalRepository for: " + mau2, localRepo2);
    assertNotSame(localRepo2, localRepo);
    assertEquals(RepositoryManager.LOCAL_REPO_PROTOCOL + tempDir2, localRepo2.getRepositoryPath());

  }

  String repoSpec(String path) {
    return RepositoryManager.LOCAL_REPO_PROTOCOL + path;
  }

  public void testGetLocalRepositoryPath() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetLocalRepositoryPath();
    }
  }
  public void doTestGetLocalRepositoryPath() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    assertEquals(RepositoryManager.LOCAL_REPO_PROTOCOL + "/foo",
		 LockssRepositoryImpl.getLocalRepositoryPath(RepositoryManager.LOCAL_REPO_PROTOCOL + "/foo"));
    assertEquals(RepositoryManager.LOCAL_REPO_PROTOCOL + "/cache/foo",
		 LockssRepositoryImpl.getLocalRepositoryPath(RepositoryManager.LOCAL_REPO_PROTOCOL + "/cache/foo"));
    assertNull(LockssRepositoryImpl.getLocalRepositoryPath("other:foo"));
    assertNull(LockssRepositoryImpl.getLocalRepositoryPath("foo"));
  }

  public void testLocalRepository_GetAuMap() {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestLocalRepository_GetAuMap();
    }
  }
  public void doTestLocalRepository_GetAuMap() {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    Properties newProps = new Properties();
    mau.setAuId("barfoo");
    newProps.setProperty(LockssRepositoryImpl.AU_ID_PROP, mau.getAuId());
    String location = getCacheLocation() + "ab";
    LockssRepositoryImpl.saveAuIdProperties(location, newProps);

    LockssRepositoryImpl.LocalRepository localRepo =
      LockssRepositoryImpl.getLocalRepository(mau);
    localRepo.auMap = null;
    Map aumap = localRepo.getAuMap();
    String id = mau.getAuId();
    logger.debug3("location: " + location + " addSlash " + addSlash(location) + " auId: " + id + " map " + aumap);
    assertNotNull(aumap);
    assertEquals(addSlash(location), aumap.get(id));
  }

  String addSlash(String s) {
    return (s.endsWith(File.separator)) ? s : s + File.separator;
  }

  public void testGetRepositoryRoot() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetRepositoryRoot();
    }
  }
  public void doTestGetRepositoryRoot() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    assertEquals(tempDirURI, LockssRepositoryImpl.getRepositoryRoot(mau));

    Configuration auconf =
      ConfigurationUtil.fromArgs(PluginManager.AU_PARAM_REPOSITORY,
				 RepositoryManager.LOCAL_REPO_PROTOCOL + "/foo/bar");
    mau.setConfiguration(auconf);
    assertEquals(RepositoryManager.LOCAL_REPO_PROTOCOL + "/foo/bar", LockssRepositoryImpl.getRepositoryRoot(mau));
  }

  // The whole point of isDirInRepository() is to resolve symbolic links,
  // but testing that would require using Runtime.exec() to create such a
  // link.  So we test only that isDirInRepository() is canonicalizing the
  // path.
  public void testIsDirInRepository() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestIsDirInRepository();
    }
  }
  public void doTestIsDirInRepository() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    assertTrue(LockssRepositoryImpl.isDirInRepository("/foo/bar", "/foo"));
    assertTrue(LockssRepositoryImpl.isDirInRepository("/foo/bar", "/foo/"));
    assertTrue(LockssRepositoryImpl.isDirInRepository("/foo/../bar/a",
						      "/bar"));
    assertFalse(LockssRepositoryImpl.isDirInRepository("/foo/bar", "/bar"));
  }

  public void testFileLocation() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestFileLocation();
    }
  }
  public void doTestFileLocation() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    String cacheURI =
      LockssRepositoryImpl.mapAuToFileLocation(tempDirURI, mau);
    FileObject testFileObject = VFS.getManager().resolveFile(cacheURI);

    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);
    assertTrue(testFileObject.exists());
    cacheURI += "www.example.com/http/";
    testFileObject = VFS.getManager().resolveFile(cacheURI);
    assertTrue(testFileObject.exists());
    cacheURI += "testDir/branch1/leaf1/";
    testFileObject = VFS.getManager().resolveFile(cacheURI);
    assertTrue(testFileObject.exists());
  }

  public void testGetRepositoryNode() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetRepositoryNode();
    }
  }
  public void doTestGetRepositoryNode() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);

    RepositoryNode node = repo.getNode("http://www.example.com/testDir");
    assertFalse(node.hasContent());
    assertEquals("http://www.example.com/testDir", node.getNodeUrl());
    node = repo.getNode("http://www.example.com/testDir/branch1");
    assertFalse(node.hasContent());
    assertEquals("http://www.example.com/testDir/branch1", node.getNodeUrl());
    node = repo.getNode("http://www.example.com/testDir/branch2/leaf3");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com/testDir/branch2/leaf3",
                 node.getNodeUrl());
    node = repo.getNode("http://www.example.com/testDir/leaf4");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com/testDir/leaf4", node.getNodeUrl());
  }

  public void testGetNodeWithQuery() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetNodeWithQuery();
    }
  }
  public void doTestGetNodeWithQuery() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    // check if '?' allowed on system
    if (FileUtil.canWriteToFileWithChar(tempDirPath, '?')) {
      createLeaf("http://www.example.com/testDir?leaf=2",
          "test stream", null);

      RepositoryNode node = repo.getNode("http://www.example.com/testDir");
      assertNull(node);
      node = repo.getNode("http://www.example.com/testDir?leaf=2");
      assertTrue(node.hasContent());
      assertEquals("http://www.example.com/testDir?leaf=2", node.getNodeUrl());
    }
  }

  public void testGetNodeWithPort() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetNodeWithPort();
    }
  }
  public void doTestGetNodeWithPort() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com:22/testDir", "test stream", null);
    assertNull(repo.getNode("http://www.example.com/testDir"));
    RepositoryNode node =
      repo.getNode("http://www.example.com:22/testDir");
    assertTrue(node.hasContent());
    assertEquals("http://www.example.com:22/testDir", node.getNodeUrl());
  }

  public void testDotUrlHandling() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestDotUrlHandling();
    }
  }
  public void doTestDotUrlHandling() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    //testing correction of nodes with bad '..'-including urls,
    //filtering the first '..' but resolving the second
    RepositoryNode node = repo.createNewNode(
        "http://www.example.com/branch/test/../test2");
    assertEquals("http://www.example.com/branch/test2", node.getNodeUrl());

    //remove single '.' references
    node = repo.createNewNode(
        "http://www.example.com/branch/./test/");
    assertEquals("http://www.example.com/branch/test", node.getNodeUrl());

    try {
      node = repo.createNewNode("http://www.example.com/..");
      fail("Should have thrown MalformedURLException.");
    } catch (MalformedURLException mue) { }
    try {
      node = repo.createNewNode("http://www.example.com/test/../../test2");
      fail("Should have thrown MalformedURLException.");
    } catch (MalformedURLException mue) { }
  }

  public void testCanonicalizePath() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestCanonicalizePath();
    }
  }
  public void doTestCanonicalizePath() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    assertEquals("http://www.example.com/test",
		 repo.canonicalizePath("http://www.example.com/test/"));
    assertEquals("http://foo.com/test",
		 repo.canonicalizePath("http://foo.com/bar/../test/"));
    assertEquals("http://foo.com:20/test",
		 repo.canonicalizePath("http://foo.com:20/test/"));
    assertEquals("http://foo.com:20/test",
		 repo.canonicalizePath("http://foo.com:20/bar/../test/"));
  }

  public void testGetAuNode() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetAuNode();
    }
  }
  public void doTestGetAuNode() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com/testDir1/leaf1", "test stream", null);
    createLeaf("http://www.example.com/testDir2/leaf2", "test stream", null);
    createLeaf("http://image.example.com/testDir3/leaf3", "test stream", null);
    createLeaf("ftp://www.example.com/file", "test stream", null);

    RepositoryNode auNode = repo.getNode(AuUrl.PROTOCOL_COLON+"//www.example.com");
    assertFalse(auNode.hasContent());
    assertEquals(AuUrl.PROTOCOL, auNode.getNodeUrl());
    Iterator childIt = auNode.listChildren(null, false);
    ArrayList childL = new ArrayList(3);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    String[] expectedA = new String[] {
      "ftp://www.example.com",
      "http://image.example.com",
      "http://www.example.com",
      };
    assertIsomorphic(expectedA, childL);
  }

  public void testDeleteNode() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestDeleteNode();
    }
  }
  public void doTestDeleteNode() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com/test1", "test stream", null);

    RepositoryNode node = repo.getNode("http://www.example.com/test1");
    assertTrue(node.hasContent());
    assertFalse(node.isDeleted());
    repo.deleteNode("http://www.example.com/test1");
    assertFalse(node.hasContent());
    assertTrue(node.isDeleted());
  }

  public void testDeactivateNode() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestDeactivateNode();
    }
  }
  public void doTestDeactivateNode() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com/test1", "test stream", null);

    RepositoryNode node = repo.getNode("http://www.example.com/test1");
    assertTrue(node.hasContent());
    assertFalse(node.isContentInactive());
    repo.deactivateNode("http://www.example.com/test1");
    assertFalse(node.hasContent());
    assertTrue(node.isContentInactive());
  }

  public void testCaching() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestCaching();
    }
  }
  public void doTestCaching() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com/testDir/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/leaf2", null, null);
    LockssRepositoryImpl repoImpl = (LockssRepositoryImpl)repo;

    // initial values are strange because creating each child node
    // causes invalidateCachedValues() to be called nodes up to the root
    int hits = repoImpl.getCacheHits();
    int misses = repoImpl.getCacheMisses();
    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/leaf1");
    assertEquals(hits + 1, repoImpl.getCacheHits());
    RepositoryNode leaf2 = repo.getNode("http://www.example.com/testDir/leaf1");
    assertSame(leaf, leaf2);
    assertEquals(hits + 2, repoImpl.getCacheHits());
    assertEquals(misses, repoImpl.getCacheMisses());
  }

  public void testWeakReferenceCaching() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestWeakReferenceCaching();
    }
  }
  public void doTestWeakReferenceCaching() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com/testDir/leaf1", null, null);

    LockssRepositoryImpl repoImpl = (LockssRepositoryImpl)repo;
    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/leaf1");
    RepositoryNode leaf2 = null;
    int loopSize = 1;
    int refHits = 0;
    // create leafs in a loop until fetching an leaf1 creates a cache miss
    while (true) {
      loopSize *= 2;
      for (int ii=0; ii<loopSize; ii++) {
        createLeaf("http://www.example.com/testDir/testleaf"+ii, null, null);
      }
      int misses = repoImpl.getCacheMisses();
      refHits = repoImpl.getRefHits();
      leaf2 = repo.getNode("http://www.example.com/testDir/leaf1");
      if (repoImpl.getCacheMisses() == misses+1) {
        break;
      }
    }
    assertSame(leaf, leaf2);
    assertEquals(refHits+1, repoImpl.getRefHits());
  }

  public void testConsistencyCheck() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestConsistencyCheck();
    }
  }
  public void doTestConsistencyCheck() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com/testDir/leaf1", "test stream", null);

    RepositoryNodeImpl leaf = (RepositoryNodeImpl)
        repo.getNode("http://www.example.com/testDir/leaf1");
    assertTrue(leaf.hasContent());
    assertTrue(leaf.contentDir.exists());

    // delete content directory
    leaf.currentCacheFile.delete();
    assertTrue(leaf.contentDir.exists());
    // version still indicates content
    assertEquals(1, leaf.getCurrentVersion());

    try {
      leaf.getNodeContents().getInputStream();
      fail("Should have thrown state exception.");
    } catch (LockssRepository.RepositoryStateException rse) { }

    assertTrue(leaf.contentDir.exists());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());
    assertFalse(leaf.hasContent());
  }

  public void testRecursiveConsistencyCheck() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestRecursiveConsistencyCheck();
    }
  }
  public void doTestRecursiveConsistencyCheck() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    createLeaf("http://www.example.com", "test stream", null);
    createLeaf("http://www.example.com/testDir", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf", "test stream", null);

    RepositoryNodeImpl dir = (RepositoryNodeImpl)
      repo.getNode("http://www.example.com");

    // set leaves inconsistent
    RepositoryNodeImpl leaf = (RepositoryNodeImpl)
      repo.getNode("http://www.example.com/testDir");
    MyMockRepositoryNode mockLeaf = new MyMockRepositoryNode(leaf);
    mockLeaf.isConsistent = false;
    repo.nodeCache.put("http://www.example.com/testDir", mockLeaf);
    // set leaf inconsistent
    leaf = (RepositoryNodeImpl)
        repo.getNode("http://www.example.com/testDir/leaf");
    MyMockRepositoryNode mockLeaf2 = new MyMockRepositoryNode(leaf);
    mockLeaf.isConsistent = false;
    repo.nodeCache.put("http://www.example.com/testDir/leaf", mockLeaf);

    // everything starts active
    assertFalse(dir.isContentInactive());
    assertFalse(mockLeaf.isContentInactive());
    assertFalse(mockLeaf2.isContentInactive());

    // run check
    repo.nodeConsistencyCheck();

    // leaf, but not its child, should be deactivated
    assertFalse(dir.isContentInactive());
    assertTrue(mockLeaf.isContentInactive());
    assertFalse(mockLeaf2.isContentInactive());
  }

  // test static naming calls

  public void testGetNextDirName() {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetNextDirName();
    }
  }
  public void doTestGetNextDirName() {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    assertEquals("a", LockssRepositoryImpl.getNextDirName(""));
    assertEquals("b", LockssRepositoryImpl.getNextDirName("a"));
    assertEquals("c", LockssRepositoryImpl.getNextDirName("b"));
    assertEquals("z", LockssRepositoryImpl.getNextDirName("y"));
    assertEquals("aa", LockssRepositoryImpl.getNextDirName("z"));
    assertEquals("ab", LockssRepositoryImpl.getNextDirName("aa"));
    assertEquals("ba", LockssRepositoryImpl.getNextDirName("az"));
    assertEquals("aaa", LockssRepositoryImpl.getNextDirName("zz"));
  }

  public void testGetAuDirFromMap() {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetAuDirFromMap();
    }
  }
  public void doTestGetAuDirFromMap() {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    LockssRepositoryImpl.LocalRepository localRepo =
      LockssRepositoryImpl.getLocalRepository("/foo");
    Map aumap = localRepo.getAuMap();
    aumap.put(mau.getAuId(), "/foo/bar/testDir");
    assertEquals("/foo/bar/testDir",
		 LockssRepositoryImpl.getAuDir(mau, "/foo", false));
  }

  public void testGetAuDirFromMapNoCacheWrongRepo() {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetAuDirFromMapNoCacheWrongRepo();
    }
  }
  public void doTestGetAuDirFromMapNoCacheWrongRepo() {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    LockssRepositoryImpl.LocalRepository localRepo =
      LockssRepositoryImpl.getLocalRepository("/foo");
    Map aumap = localRepo.getAuMap();
    aumap.put(mau.getAuId(), "/foo/bar/testDir");
    assertNull(LockssRepositoryImpl.getAuDir(mau, "/other/repo", false));
    assertEquals("/foo/bar/testDir",
		 LockssRepositoryImpl.getAuDir(mau, "/foo", false));
    assertNull(LockssRepositoryImpl.getAuDir(mau, "/other/repo", false));
  }

  public void testGetAuDirNoCreate() {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetAuDirNoCreate();
    }
  }
  public void doTestGetAuDirNoCreate() {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    mau.setAuId("foobar23");
    assertNull(LockssRepositoryImpl.getAuDir(mau, "/tmp", false));
  }

  public void testSaveAndLoadNames() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestSaveAndLoadNames();
    }
  }
  public void doTestSaveAndLoadNames() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    String location =
      LockssRepositoryImpl.mapAuToFileLocation(tempDirURI, mau);

    FileObject idFile = VFS.getManager().resolveFile(location + LockssRepositoryImpl.AU_ID_FILE);
    assertTrue(idFile.exists());

    Properties props = LockssRepositoryImpl.getAuIdProperties(location);
    assertNotNull(props);
    assertEquals(mau.getAuId(),
                 props.getProperty(LockssRepositoryImpl.AU_ID_PROP));
  }

  public void testMapAuToFileLocation() {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestMapAuToFileLocation();
    }
  }
  public void doTestMapAuToFileLocation() {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    LockssRepositoryImpl.localRepositories.clear();
    LockssRepositoryImpl.lastPluginDir = "ba";
    String expectedStr = getCacheLocation() + "bb/";
    logger.debug3("Expected: " + expectedStr + " munged " +
                  FileUtil.sysDepPath(expectedStr));
    assertEquals(FileUtil.sysDepPath(expectedStr),
		 LockssRepositoryImpl.mapAuToFileLocation(tempDirURI,
							  new MockArchivalUnit()));
  }

  public void testDoesAuDirExist() {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestDoesAuDirExist();
    }
  }
  public void doTestDoesAuDirExist() {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    LockssRepositoryImpl.localRepositories.clear();
    MockArchivalUnit mau = new MockArchivalUnit();
    String auid = "sdflkjsd";
    mau.setAuId(auid);
    assertFalse(LockssRepositoryImpl.doesAuDirExist(auid, tempDirURI));
    // ensure asking doesn't create it
    assertFalse(LockssRepositoryImpl.doesAuDirExist(auid, tempDirURI));
    LockssRepositoryImpl.lastPluginDir = "ga";
    String expectedStr = getCacheLocation() + "gb/";
    assertEquals(FileUtil.sysDepPath(expectedStr),
		 LockssRepositoryImpl.mapAuToFileLocation(tempDirURI, mau));
    assertTrue(LockssRepositoryImpl.doesAuDirExist(auid, tempDirURI));
  }

  public void testGetAuDirInitWithOne() {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetAuDirInitWithOne();
    }
  }
  public void doTestGetAuDirInitWithOne() {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    LockssRepositoryImpl.localRepositories.clear();
    String root = getCacheLocation();
    assertEquals(FileUtil.sysDepPath(root + "b/"),
                 LockssRepositoryImpl.mapAuToFileLocation(tempDirURI,
							  new MockArchivalUnit()));
  }

  public void testGetAuDirSkipping() throws Exception {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestGetAuDirSkipping();
    }
  }
  public void doTestGetAuDirSkipping() throws Exception {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    LockssRepositoryImpl.localRepositories.clear();
    String root = getCacheLocation();
    // a already made by setup
    FileObject testFileObject = VFS.getManager().resolveFile(root + "a");
    assertTrue(testFileObject.exists());
    VFS.getManager().resolveFile(root + "b").createFolder();
    VFS.getManager().resolveFile(root + "c").createFolder();
    VFS.getManager().resolveFile(root + "e").createFolder();

    assertEquals(expDir("d"), probe());
    assertEquals(expDir("f"), probe());
    VFS.getManager().resolveFile(root + "g").createFolder();
    VFS.getManager().resolveFile(root + "h").createFolder();
    VFS.getManager().resolveFile(root + "i").createFolder();
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MAX_UNUSED_DIR_SEARCH,
				 "2");
    try {
      probe();
      fail("Shouldn't find next unused dir with maxUnusedDirSearch = 2");
    } catch (RuntimeException e) {
    }
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MAX_UNUSED_DIR_SEARCH,
				 "5");
    assertEquals(expDir("j"), probe());
  }

  String expDir(String sub) {
    return FileUtil.sysDepPath(getCacheLocation() + sub + "/");
  }

  String probe() {
    return LockssRepositoryImpl.mapAuToFileLocation(tempDirURI,
						    new MockArchivalUnit());
  }

  public void testMapUrlToFileLocation() throws MalformedURLException {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestMapUrlToFileLocation();
    }
  }
  public void doTestMapUrlToFileLocation() throws MalformedURLException {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    String testStr = "http://www.example.com/branch1/branch2/index.html";
    String expectedStr = "root/www.example.com/http/branch1/branch2/index.html";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapUrlToFileLocation("root", testStr));

    testStr = "hTTp://www.exaMPLE.com/branch1/branch2/index.html";
    expectedStr = "root/www.example.com/http/branch1/branch2/index.html";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapUrlToFileLocation("root", testStr));

    try {
      testStr = ":/brokenurl.com/branch1/index/";
      LockssRepositoryImpl.mapUrlToFileLocation("root", testStr);
      fail("Should have thrown MalformedURLException");
    } catch (MalformedURLException mue) {}
  }

  public void testCharacterEscaping() throws MalformedURLException {
    for (int i = 0; i < serviceNames.length; i++) {
      serviceName = serviceNames[i];
      doTestCharacterEscaping();
    }
  }
  public void doTestCharacterEscaping() throws MalformedURLException {
    if (!propertiesAreSet()) {
      logger.warning("test disabled for " + serviceName + ": storage service host & key properties not set.");
      return;
    }
    String testStr = "http://www.example.com/"+UrlUtil.encodeUrl("#")+"nodestate.xml";
    String expectedStr = "root/www.example.com/http/##nodestate.xml";
//    assertEquals(FileUtil.sysDepPath(expectedStr),
  //               LockssRepositoryImpl.mapUrlToFileLocation("root", testStr));
    assertEquals("root/www.example.com/http/#nodestate.xml",
                 LockssRepositoryImpl.unescape(expectedStr));

    testStr = "http://www.example.com/index.html?leaf=bad"+File.separator+
        "query"+File.separator;
    expectedStr = "root/www.example.com/http/index.html?leaf=bad#squery#s";
    assertEquals(FileUtil.sysDepPath(expectedStr),
                 LockssRepositoryImpl.mapUrlToFileLocation("root", testStr));
    assertEquals("root/www.example.com/http/index.html?leaf=bad"+
                 File.separator+"query"+File.separator,
                 LockssRepositoryImpl.unescape(expectedStr));
  }

  private RepositoryNode createLeaf(String url, String content,
                                    Properties props) throws Exception {
    return TestRepositoryNodeImpl.createLeaf(repo, url, content, props);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestLockssRepositoryImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  /**
   * This class overrides 'checkNodeConsistency()' so I can manipulate it.
   */
  static class MyMockRepositoryNode extends RepositoryNodeImpl {
    boolean isConsistent = true;
    MyMockRepositoryNode(RepositoryNodeImpl nodeImpl) {
      super(nodeImpl.url, nodeImpl.nodeLocation, nodeImpl.repository);
    }

    boolean checkNodeConsistency() {
      return isConsistent;
    }
  }
}
