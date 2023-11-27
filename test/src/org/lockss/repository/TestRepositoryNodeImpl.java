/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import java.nio.channels.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import org.apache.commons.lang3.StringUtils;

import org.lockss.test.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;

/**
 * This is the test class for org.lockss.repository.RepositoryNodeImpl
 */
public class TestRepositoryNodeImpl extends LockssTestCase {
  static final String TREE_SIZE_PROPERTY =
    RepositoryNodeImpl.TREE_SIZE_PROPERTY;
  static final String CHILD_COUNT_PROPERTY =
    RepositoryNodeImpl.CHILD_COUNT_PROPERTY;

  private MockLockssDaemon theDaemon;
  private MyLockssRepositoryImpl repo;
  private String tempDirPath;
  MockArchivalUnit mau;

  private MockIdentityManager idmgr;
  
  Properties props;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mau = new MockArchivalUnit();

    theDaemon = getMockLockssDaemon();
    
    // Create the identity manager...
    idmgr = new MockIdentityManager();
    theDaemon.setIdentityManager(idmgr);
    idmgr.initService(theDaemon);
    
    repo = (MyLockssRepositoryImpl)MyLockssRepositoryImpl.createNewLockssRepository(mau);
    theDaemon.setAuManager(LockssDaemon.LOCKSS_REPOSITORY, mau, repo);
    repo.initService(theDaemon);
    repo.startService();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    repo.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  // RepositoryNodeImpl relies on nonexistent dir.listFiles() returning
  // null, not empty list.
  public void testFileAssumptions() throws Exception {
    // empty dir returns empty list
    File dir1 = getTempDir();
    assertNotNull(null, dir1.listFiles());
    assertEquals(new File[0], dir1.listFiles());
    // nonexistent dir returns null
    File dir2 = new File(dir1, "bacds");
    assertNull(null, dir2.listFiles());
    // dir list of non-dir returns null
    File file1 = FileUtil.createTempFile("xxx", ".tmp", dir1);
    assertTrue(file1.exists());
    assertNull(null, file1.listFiles());
  }

  // Ensure that our maximum filename component calculation agrees with
  // reality.

  File root;
  int maxDirname;
  int preflen;
  int sufflen;
  String pref;
  String suff;

  public void testUnicodeAssumptions() throws Exception {
    assertEquals("1234", trimTo("1234", 4));
    assertEquals("1234", trimTo("12345", 4));
    assertEquals("1234", trimTo("123454444444", 4));

    root = getTempDir();
    assertTrue(canMkdir(root, "xyz" + "\u00e9"));
    maxDirname = findMaxDirname(root);
    log.info("Max dirname on " + root + ": " + maxDirname);
    if (maxDirname < 30) {
      log.critical("Skipping test because filesystem is inadequate for LOCKSS");
      return;
    }
    preflen = (maxDirname - 10) / 2;
    sufflen = maxDirname - preflen;
    pref = mkstr(preflen);
    suff = mkstr(sufflen);

    String t1 = pref + suff;
    String t2 = pref + "e" + suff;
    assertTrue(byteLength(t1) == t1.length());
    assertTrue(canMkdir(root, t1));
    assertFalse(canMkdir(root, t2));

    String uniStrs[] = {

      "\u00e9",			// Latin–1 Supplement
      "\u0113",			// Latin Extended-A
      "\u01a2",			// Latin Extended-B
      "\u025a",			// IPA Extensions
      "\u0393",			// Greek
      "\u0409",			// Cyrillic
      "\u05d2",			// Hebrew
      "\u062c",			// Arabic
      "\u0ab2",			// Gujarati
      "\u0E28",			// Thai
      "\u0EC0",			// Lao
      "\u0F44",			// Tibetan
      "\u305D",			// Hiragana
      "\u30AE",			// Katakana
      "\u2EA8",			// CJK Radicals Supplement
      "\u3028",			// CJK Symbols and Punctuation
      "\u4E05",			// CJK Unified Ideographs
      "\uAC07",			// Hangul Syllables
    };

    // One unicode char
    for (String s : uniStrs) {
      testUni(s);
    }

    // Two unicode chars
    for (String s1 : uniStrs) {
      for (String s2 : uniStrs) {
	testUni(s1 + s2);
      }
    }
  }

  void testUni(String unistr) {
    String str = pref + unistr + suff;

    int failat = -1;
    int probelen = str.length();
    while (probelen >= 1) {
      String probe = trimTo(str, probelen);

      int blen = byteLength(probe);
      boolean should = blen <= maxDirname;
      boolean does = canMkdir(root, probe);
      log.debug2("probelen: " + probe.length() +
		 ", byte: " + byteLength(probe) + ": " + does);
      if (should) {
	if (does) {
	  return;
	}
	fail("foo");
      } 
      probelen--;
    }
  }


  static String trimTo(String s, int len) {
    return s.substring(0, len);
  }

  int byteLength(String s) {
    return RepositoryNodeImpl.byteLength(s);
  }

  int findMaxDirname(File root) {
    for (int len = 1; len < 1000; len++) {
      if (!canMkdir(root, len)) {
	return len - 1;
      }
    }
    return -1;
  }

  void findMaxDirPath(File root) {
    int maxName = findMaxDirname(root) - 10;
    String one = mkstr("onedir", maxName) + "/";
    for (int rpt = 1; rpt < 1000; rpt++) {
      String path = StringUtils.repeat(one, rpt);
      File dir = new File(root, path);
      String dirstr = dir.getPath();
      boolean res = dir.mkdirs();
      if (!res) {
	log.info("mkdirs failed at " + dirstr.length() + " chars");
	break;
      }
      log.info("mkdirs ok: " + dirstr.length());
      File f = new File(dir, "foobbb");
      try {
	OutputStream os = new FileOutputStream(f);
	os.close();
	log.info("file ok at " + f.getPath().length() + " chars");
      } catch (FileNotFoundException fnfe) {
	log.info("FNF: " + f.getPath().length(), fnfe);
      } catch (IOException ioe) {
	log.error("IOE: " + f.getPath().length() + ", " + ioe.getMessage());
      }
    }
  }

  void findMaxDirPathNio(File root) {
    int maxName = findMaxDirname(root) - 10;
    String one = mkstr("onedir", maxName) + "/";
    for (int rpt = 1; rpt < 1000; rpt++) {
      String path = StringUtils.repeat(one, rpt);
      File dir = new File(root, path);
      String dirstr = dir.getPath();
      boolean res = dir.mkdirs();
      if (!res) {
	log.info("mkdirs failed at " + dirstr.length() + " chars");
	break;
      }
      log.info("mkdirs ok: " + dirstr.length());
      File f = new File(dir, "foobbb");
      try {
	Path npath = Paths.get(f.getPath());
	Files.createFile(npath);
	FileChannel ochan = FileChannel.open(npath, StandardOpenOption.WRITE);
	OutputStream os = Channels.newOutputStream(ochan);
	os.write((byte)44);
	os.close();

	FileChannel ichan = FileChannel.open(npath, StandardOpenOption.READ);
	InputStream is = Channels.newInputStream(ichan);
	int bb = is.read();
	is.close();
	assertEquals(44, bb);
	log.info("file ok at " + npath.toString().length() + " chars");
      } catch (FileNotFoundException fnfe) {
	log.error("FNF: " + f.getPath().length(), fnfe);
      } catch (IOException ioe) {
	log.error("IOE: " + f.getPath().length() + ", " + ioe.getMessage());
      }
    }
  }

  boolean canMkdir(File root, int len) {
    return canMkdir(root, mkstr(len));
  }

  boolean canMkdir(String root, String name) {
    return canMkdir(new File(root), name);
  }

  boolean canMkdir(File root, String name) {
    File f = new File(root, name);
    boolean res = f.mkdirs();
    if (!res) {
      if (f.exists()) {
	throw new RuntimeException("mkdirs = false but dir exists: " + f);
      }
      log.debug2("canMkdir("+f+"): false");
      return false;
    }
    if (f.exists()) {
      if (!f.delete()) {
	throw new RuntimeException("Couldn't delete newly created dir: " + f);
      }
      if (f.exists()) {
	throw new RuntimeException("Deleted newly created dir still exists: " + f);
      }
      log.debug2("canMkdir("+f+"): true");
      return true;
    }
    throw new RuntimeException("mkdirs() == true but exists() == false: " + f);
  }

  public void testMkstr() {
    for (int ix = 0; ix < 1000; ix++) {
      assertEquals(ix, mkstr(ix).length());
    }
  }

  String mkstr(int len) {
    return mkstr("abcdefghijklmnopqrstuvwxyz0123456789", len);
  }

  String mkstr(String al, int len) {
    StringBuilder sb = new StringBuilder(len);
    for (int ix = 1; ix <= len / al.length(); ix++) {
      sb.append(al);
    }
    sb.append(al.substring(0, len % al.length()));
    if (sb.length() != len) {
      throw new RuntimeException("mkstr(" + len + ") made string w/ len: "
				 + sb.length());
    }
    return sb.toString();
  }

  public void testGetNodeUrl() {
    RepositoryNode node = new RepositoryNodeImpl("testUrl", "testDir", null);
    assertEquals("testUrl", node.getNodeUrl());
    node = new RepositoryNodeImpl("testUrl/test.txt", "testUrl/test.txt", null);
    assertEquals("testUrl/test.txt", node.getNodeUrl());
  }

  public void testFileLocation() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/branch1/leaf1",
                   "test stream", null);
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File testFile = new File(tempDirPath);
    assertTrue(testFile.exists());
    testFile = new File(tempDirPath + "/#content/current");
    assertTrue(testFile.exists());
    testFile = new File(tempDirPath + "/#content/current.props");
    assertTrue(testFile.exists());
    testFile = new File(tempDirPath + "/#node_props");
    assertFalse(testFile.exists());
    testFile = new File(tempDirPath + "/#agreement");
    assertFalse(testFile.exists());
  }
  
  public void testUpdateAgreementCreatesFile() throws Exception {
    RepositoryNode leaf =
      createLeaf("http://www.example.com/testDir/branch1/leaf1",
                 "test stream", null);
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File testFile = new File(tempDirPath, "#agreement");
    assertFalse(testFile.exists());
    
    // Agreeing IDs.
    PeerIdentity[] agreeingPeers =
      { new MockPeerIdentity("TCP:[192.168.0.1]:9723"),
        new MockPeerIdentity("TCP:[192.168.0.2]:9723")
      };
    
    leaf.signalAgreement(ListUtil.fromArray(agreeingPeers));
    assertTrue(testFile.exists());
  }

  public void testUpdateAndLoadAgreement() throws Exception {
    RepositoryNode leaf =
      createLeaf("http://www.example.com/testDir/branch1/leaf1",
                 "test stream", null);
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    PeerIdentity testid_1 = new MockPeerIdentity("TCP:[192.168.0.1]:9723");
    PeerIdentity testid_2 = new MockPeerIdentity("TCP:[192.168.0.2]:9723");
    PeerIdentity testid_3 = new MockPeerIdentity("TCP:[192.168.0.3]:9723");
    PeerIdentity testid_4 = new MockPeerIdentity("TCP:[192.168.0.4]:9723");
    
    idmgr.addPeerIdentity(testid_1.getIdString(), testid_1);
    idmgr.addPeerIdentity(testid_2.getIdString(), testid_2);
    idmgr.addPeerIdentity(testid_3.getIdString(), testid_3);
    idmgr.addPeerIdentity(testid_4.getIdString(), testid_4);
    
    leaf.signalAgreement(ListUtil.list(testid_1, testid_3));

    assertEquals(2, ((RepositoryNodeImpl)leaf).loadAgreementHistory().size());

    assertTrue(leaf.hasAgreement(testid_1));
    assertFalse(leaf.hasAgreement(testid_2));
    assertTrue(leaf.hasAgreement(testid_3));
    assertFalse(leaf.hasAgreement(testid_4));

    leaf.signalAgreement(ListUtil.list(testid_1, testid_2, testid_3, testid_4));
    
    assertEquals(4, ((RepositoryNodeImpl)leaf).loadAgreementHistory().size());

    assertTrue(leaf.hasAgreement(testid_1));
    assertTrue(leaf.hasAgreement(testid_2));
    assertTrue(leaf.hasAgreement(testid_3));
    assertTrue(leaf.hasAgreement(testid_4));
  }
  
  public void testVersionFileLocation() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/branch1/leaf1",
        "test stream", null);
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File testFile = new File(tempDirPath + "/#content/1");
    assertFalse(testFile.exists());
    testFile = new File(tempDirPath + "/#content/1.props");
    assertFalse(testFile.exists());

    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream 2");
    StreamUtil.copy(is, os);
    is.close();
    os.close();
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertFalse(leaf.isIdenticalVersion());
    testFile = new File(tempDirPath + "/#content/1");
    assertTrue(testFile.exists());
    testFile = new File(tempDirPath + "/#content/1.props");
    assertTrue(testFile.exists());
  }

  public void testInactiveFileLocation() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/branch1/leaf1",
                   "test stream", null);
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File curFile = new File(tempDirPath + "/#content/current");
    File curPropsFile = new File(tempDirPath + "/#content/current.props");
    File inactFile = new File(tempDirPath + "/#content/inactive");
    File inactPropsFile = new File(tempDirPath + "/#content/inactive.props");
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());

    leaf.deactivateContent();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());

    //reactivate
    leaf.restoreLastVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());

    leaf.deactivateContent();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());

    // make new version
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream 2");
    StreamUtil.copy(is, os);
    is.close();
    os.close();
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
    assertFalse(leaf.isIdenticalVersion());
  }

  public void testDeleteFileLocation() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/branch1/leaf1",
                   "test stream", null);
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/branch1/leaf1");
    File curFile = new File(tempDirPath + "/#content/current");
    File curPropsFile = new File(tempDirPath + "/#content/current.props");
    File inactFile = new File(tempDirPath + "/#content/inactive");
    File inactPropsFile = new File(tempDirPath + "/#content/inactive.props");
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());

    leaf.markAsDeleted();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());

    //reactivate
    leaf.restoreLastVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());

    leaf.markAsDeleted();
    assertFalse(curFile.exists());
    assertFalse(curPropsFile.exists());
    assertTrue(inactFile.exists());
    assertTrue(inactPropsFile.exists());

    // make new version
    leaf.makeNewVersion();
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream 2");
    StreamUtil.copy(is, os);
    is.close();
    os.close();
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(curFile.exists());
    assertTrue(curPropsFile.exists());
    assertFalse(inactFile.exists());
    assertFalse(inactPropsFile.exists());
    assertFalse(leaf.isIdenticalVersion());
  }

  public void testListEntriesNonexistentDir() throws Exception {
    RepositoryNode node = new RepositoryNodeImpl("foo-no-url", "foo-no-dir",
						 null);
    try {
      node.listChildren(null, false);
      fail("listChildren() is nonexistent dir should throw");
    } catch (LockssRepository.RepositoryStateException e) {
    }
  }

  public void testListEntries() throws Exception {
    createLeaf("http://www.example.com/testDir/branch1/leaf1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/leaf2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);

    // root branch
    RepositoryNode dirEntry =
        repo.getNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listChildren(null, false);
    ArrayList childL = new ArrayList(3);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/leaf4"
      };
    assertSameElements(expectedA, childL);

    // sub-branch
    dirEntry = repo.getNode("http://www.example.com/testDir/branch1");
    childL.clear();
    childIt = dirEntry.listChildren(null, false);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1/leaf1",
      "http://www.example.com/testDir/branch1/leaf2",
      };
    assertSameElements(expectedA, childL);

    // sub-branch with content
    dirEntry = repo.getNode("http://www.example.com/testDir/branch2");
    childL.clear();
    childIt = dirEntry.listChildren(null, false);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] {
      "http://www.example.com/testDir/branch2/leaf3",
      };
    assertSameElements(expectedA, childL);

    // leaf node
    dirEntry = repo.getNode("http://www.example.com/testDir/branch1/leaf1");
    childL.clear();
    childIt = dirEntry.listChildren(null, false);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] { };
    assertSameElements(expectedA, childL);
  }

  String normalizeName(RepositoryNodeImpl node, String name) {
    return node.normalize(new File(name)).getPath();
  }

  public void testNormalizeUrlEncodingCase() throws Exception {
	if (!PlatformUtil.getInstance().isCaseSensitiveFileSystem()) {
	    log.debug("Skipping testNormalizeUrlEncodingCase: file system is not case sensitive.");
	    return;
	}
    RepositoryNodeImpl node = new RepositoryNodeImpl("foo", "bar", null);
    // nothing to normalize
    File file = new File("foo/bar/baz");
    assertSame(file, node.normalize(file));
    file = new File("foo/bar/ba%ABz");
    assertSame(file, node.normalize(file));
    // unnormalized in parent dir name is left alone
    file = new File("ba%abz/bar");
    assertSame(file, node.normalize(file));
    file = new File("foo/ba%abz/bar");
    assertSame(file, node.normalize(file));
    // should be normalized
    assertEquals("ba%ABz", normalizeName(node, "ba%aBz"));
    assertEquals("/ba%ABz", normalizeName(node, "/ba%aBz"));
    assertEquals("foo/bar/ba%ABz", normalizeName(node, "foo/bar/ba%aBz"));
    assertEquals("foo/bar/ba%ABz", normalizeName(node, "foo/bar/ba%Abz"));
    assertEquals("foo/bar/ba%ABz", normalizeName(node, "foo/bar/ba%abz"));
    assertEquals("foo/bar/ba%abz/ba%ABz", normalizeName(node, "foo/bar/ba%abz/ba%abz"));
  }

  public void testNormalizeTrailingQuestion() throws Exception {
    RepositoryNodeImpl node = new RepositoryNodeImpl("foo", "bar", null);
    // nothing to normalize
    File file = new File("foo/bar/baz");
    assertSame(file, node.normalize(file));
    file = new File("foo/bar/ba?z");
    assertSame(file, node.normalize(file));
    // unnormalized in parent dir name is left alone
    file = new File("ba?/bar");
    assertSame(file, node.normalize(file));
    // should be normalized
    assertEquals("baz", normalizeName(node, "baz?"));
    assertEquals(new File("/ba").getPath(), normalizeName(node, "/ba?"));
    assertEquals(new File("foo/bar/bar").getPath(), normalizeName(node, "foo/bar/bar?"));
    assertEquals(new File("foo/ba?r/bar").getPath(), normalizeName(node, "foo/ba?r/bar?"));
    assertEquals(new File("foo/bar?/bar").getPath(), normalizeName(node, "foo/bar?/bar?"));

    // disable trailing ? normalization
    ConfigurationUtil.addFromArgs(UrlUtil.PARAM_NORMALIZE_EMPTY_QUERY,
				  "false");
    assertEquals("baz?", normalizeName(node, "baz?"));
  }

  List getChildNames(String nodeName) throws MalformedURLException {
    RepositoryNode dirEntry = repo.getNode(nodeName);
    ArrayList res = new ArrayList();
    for (Iterator childIt = dirEntry.listChildren(null, false);
	 childIt.hasNext(); ) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      log.debug2("node: " + node);
      res.add(node.getNodeUrl());
    }
    return res;
  }


  public void testFixUnnormalized_Rename() throws Exception {
    if (!PlatformUtil.getInstance().isCaseSensitiveFileSystem()) {
      log.debug("Skipping testFixUnnormalized_Rename: file system is not case sensitive.");
      return;
    }

    repo.setDontNormalize(true);
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "No");
    createLeaf("http://www.example.com/testDir/branch%3c1/leaf%2C1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch%3c1/leaf%2c2",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/leaf3",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2", "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf4", "test stream", null);

    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch%3c1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/leaf4"
      };
    assertSameElements(expectedA,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Log");
    assertSameElements(expectedA,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Fix");
    String[] expectedB = new String[] {
      "http://www.example.com/testDir/branch%3C1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/leaf4"
      };
    assertSameElements(expectedB,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "No");
    assertSameElements(expectedB,
		     getChildNames(("http://www.example.com/testDir")));

    String[] expectedC = new String[] {
      "http://www.example.com/testDir/branch%3C1/leaf%2C1",
      "http://www.example.com/testDir/branch%3C1/leaf%2c2",
      };
    assertSameElements(expectedC,
		     getChildNames(("http://www.example.com/testDir/branch%3C1")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Log");
    assertSameElements(expectedB,
		     getChildNames(("http://www.example.com/testDir")));

    assertSameElements(expectedC,
		     getChildNames(("http://www.example.com/testDir/branch%3C1")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Fix");
    assertSameElements(expectedB,
		     getChildNames(("http://www.example.com/testDir")));

    String[] expectedD = new String[] {
      "http://www.example.com/testDir/branch%3C1/leaf%2C1",
      "http://www.example.com/testDir/branch%3C1/leaf%2C2",
      };
    assertSameElements(expectedD,
		     getChildNames(("http://www.example.com/testDir/branch%3C1")));
  }

  public void testFixUnnormalizedMultiple_Delete() throws Exception {
	if (!PlatformUtil.getInstance().isCaseSensitiveFileSystem()) {
	    log.debug("Skipping testFixUnnormalizedMultiple_Delete: file system is not case sensitive.");
	    return;
	}
    repo.setDontNormalize(true);
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "No");
    createLeaf("http://www.example.com/testDir/leaf%2C1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf%2c1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf3",
               "test stream", null);

    String[] expectedA = new String[] {
      "http://www.example.com/testDir/leaf%2C1",
      "http://www.example.com/testDir/leaf%2c1",
      "http://www.example.com/testDir/leaf3",
      };
    assertSameElements(expectedA,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Log");
    assertSameElements(expectedA,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Fix");
    String[] expectedB = new String[] {
      "http://www.example.com/testDir/leaf%2C1",
      "http://www.example.com/testDir/leaf3",
      };
    assertSameElements(expectedB,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "No");
    assertSameElements(expectedB,
		     getChildNames(("http://www.example.com/testDir")));
  }

  public void testFixUnnormalizedMultiple_DeleteMultiple() throws Exception {
	if (!PlatformUtil.getInstance().isCaseSensitiveFileSystem()) {
	    log.debug("Skipping testFixUnnormalizedMultiple_DeleteMultiple: file system is not case sensitive.");
	    return;
	}
    repo.setDontNormalize(true);
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "No");
    createLeaf("http://www.example.com/testDir/leaf%CA%3E",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf%cA%3E",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf%ca%3E",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf%ca%3e",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf3",
               "test stream", null);

    String[] expectedA = new String[] {
      "http://www.example.com/testDir/leaf%CA%3E",
      "http://www.example.com/testDir/leaf%cA%3E",
      "http://www.example.com/testDir/leaf%ca%3E",
      "http://www.example.com/testDir/leaf%ca%3e",
      "http://www.example.com/testDir/leaf3",
      };
    assertSameElements(expectedA,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Log");
    assertSameElements(expectedA,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Fix");
    String[] expectedB = new String[] {
      "http://www.example.com/testDir/leaf%CA%3E",
      "http://www.example.com/testDir/leaf3",
      };
    assertSameElements(expectedB,
		     getChildNames(("http://www.example.com/testDir")));

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "No");
    assertSameElements(expectedB,
		     getChildNames(("http://www.example.com/testDir")));
  }

  public void testFixUnnormalized_DontFixParent() throws Exception {
	if (!PlatformUtil.getInstance().isCaseSensitiveFileSystem()) {
	    log.debug("Skipping testFixUnnormalized_DontFixParent: file system is not case sensitive.");
	    return;
	}
    repo.setDontNormalize(true);
    createLeaf("http://www.example.com/testDir/branch%3c1/leaf%2C1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/branch%3c1/leaf%2c2",
               "test stream", null);

    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Fix");
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch%3c1/leaf%2C1",
      "http://www.example.com/testDir/branch%3c1/leaf%2C2",
      };
    assertSameElements(expectedA,
		     getChildNames(("http://www.example.com/testDir/branch%3c1")));
  }

  public void testUnnormalizedIterate() throws Exception {
    if (!PlatformUtil.getInstance().isCaseSensitiveFileSystem()) {
      log.debug("Skipping testUnnormalizedIterate: file system is not case sensitive.");
      return;
    }
    repo.setDontNormalize(true);
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "No");
    createLeaf("http://www.example.com/testDir/leaf1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf%2c1",
               "test stream", null);
    createLeaf("http://www.example.com/testDir/leaf3",
               "test stream", null);
    repo.setDontNormalize(false);

    // Unnormalized name in result of listChildren not included in result
    // because name gets normalized when node created, thus not found
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/leaf1",
      "http://www.example.com/testDir/leaf3",
      };
    assertSameElements(expectedA,
		       getChildNames(("http://www.example.com/testDir")));

    // Same with checkUnnormalized = Log
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Log");
    assertSameElements(expectedA,
		       getChildNames(("http://www.example.com/testDir")));

    // If checkUnnormalized = Fix, unnnormalized file in repo will be fixed
    // and inluded in result
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_CHECK_UNNORMALIZED,
				  "Fix");
    String[] expectedB = new String[] {
      "http://www.example.com/testDir/leaf1",
      "http://www.example.com/testDir/leaf%2C1",
      "http://www.example.com/testDir/leaf3",
      };
    assertSameElements(expectedB,
		       getChildNames(("http://www.example.com/testDir")));
  }

  public void testEntrySort() throws Exception {
    createLeaf("http://www.example.com/testDir/branch2/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/leaf4", null, null);
    createLeaf("http://www.example.com/testDir/branch1/leaf1", null, null);
    createLeaf("http://www.example.com/testDir/leaf3", null, null);

    RepositoryNode dirEntry =
        repo.getNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listChildren(null, false);
    ArrayList childL = new ArrayList(4);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/leaf3",
      "http://www.example.com/testDir/leaf4"
      };
    assertSameElements(expectedA, childL);
  }

  public void testIllegalOperations() throws Exception {
    RepositoryNode leaf =
      repo.createNewNode("http://www.example.com/testDir/test.cache");
    assertFalse(leaf.hasContent());
    try {
      leaf.getCurrentVersion();
      fail("Cannot get current version if no content.");
    } catch (UnsupportedOperationException uoe) { }
    try {
      leaf.getContentSize();
      fail("Cannot get content size if no content.");
    } catch (UnsupportedOperationException uoe) { }
    try {
      leaf.getNodeContents();
      fail("Cannot get RepositoryNodeContents if no content.");
    } catch (UnsupportedOperationException uoe) { }
    try {
      leaf.sealNewVersion();
      fail("Cannot seal version if not open.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    try {
      leaf.sealNewVersion();
      fail("Cannot seal version if getNewOutputStream() uncalled.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    try {
      leaf.deactivateContent();
      fail("Cannot deactivate if currently open for writing.");
    } catch (UnsupportedOperationException uoe) { }
    writeToLeaf(leaf, "test stream");
    try {
      leaf.sealNewVersion();
      fail("Cannot seal version if setNewProperties() uncalled.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    writeToLeaf(leaf, "test stream");
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertEquals(1, leaf.getCurrentVersion());
    assertTrue(leaf.hasContent());
    assertFalse(leaf.isIdenticalVersion());
  }

  public void testVersionTimeout() throws Exception {
    TimeBase.setSimulated();
    RepositoryNode leaf =
      repo.createNewNode("http://www.example.com/testDir/test.cache");
    RepositoryNode leaf2 =
      repo.getNode("http://www.example.com/testDir/test.cache");
    leaf.makeNewVersion();
    try {
      leaf2.makeNewVersion();
      fail("Can't make new version while version open.");
    } catch (UnsupportedOperationException e) { }
    TimeBase.step(RepositoryNodeImpl.DEFAULT_VERSION_TIMEOUT/2);
    try {
      leaf2.makeNewVersion();
      fail("Can't make new version while version not timed out.");
    } catch (UnsupportedOperationException e) { }
    TimeBase.step(RepositoryNodeImpl.DEFAULT_VERSION_TIMEOUT/2);
    leaf2.makeNewVersion();
  }

  public void testMakeNewCache() throws Exception {
    RepositoryNode leaf =
      repo.createNewNode("http://www.example.com/testDir/test.cache");
    assertFalse(leaf.hasContent());
    try {
      leaf.getCurrentVersion();
      fail("Cannot get current version if no content.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    writeToLeaf(leaf, "test stream");
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(leaf.hasContent());
    assertEquals(1, leaf.getCurrentVersion());
  }

  static String LONG_1 = "http://ijs.macroeconomicsresearch.org/articles/renderlist.action/fmt=ahah&items=http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY585,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY592,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY591,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY593,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY594,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY601,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY602,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY603,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY604,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY611,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY614,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY619,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY618,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY620,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY621,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY626,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY629,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY630,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY635,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY633,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY637,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY639,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY643,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY649,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY650,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY652,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY655,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY656,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY659,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY666,http:s/sgm.metawrite.magenta.com/content/journal/ijsem/42.867-5309/ijsem.0.XXXYYY673";

  static String LONG_2 = "http://mic.microbiologyresearch.org/articles/renderlist.action?fmt=ahah&items=http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.064675-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.070672-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.070763-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.071332-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.073783-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.074443-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.071688-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.074252-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.072405-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.068726-0,http://sgm.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.071159-0";

  public void testLongPath(String url) throws Exception {
//     findMaxDirPath(getTempDir());
//     findMaxDirPathNio(getTempDir());
    
    String longUrl = trimUrlForOs(url);

    RepositoryNode leaf =
      repo.createNewNode(longUrl);
    assertFalse(leaf.hasContent());
    try {
      leaf.getCurrentVersion();
      fail("Cannot get current version if no content.");
    } catch (UnsupportedOperationException uoe) { }
    leaf.makeNewVersion();
    writeToLeaf(leaf, "test stream");
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(leaf.hasContent());
    assertEquals(1, leaf.getCurrentVersion());
    assertEquals(longUrl, leaf.getNodeUrl());
    RepositoryNode.RepositoryNodeContents rnc = leaf.getNodeContents();
    assertInputStreamMatchesString("test stream", rnc.getInputStream());

    RepositoryNode top = repo.getNode(AuUrl.PROTOCOL_COLON);
    List<String> tree = getSubTreeUrls(top);
    assertEquals(ListUtil.list(longUrl), tree);
  }

  public void testLongPath1() throws Exception {
    testLongPath(LONG_1);
  }

  public void testLongPath2() throws Exception {
    testLongPath(LONG_2);
  }

  List<String> getSubTreeUrls(RepositoryNode node) throws Exception {
    List<String> res = new ArrayList<String>();
    addSubTreeUrls(res, node);
    return res;
  }

  void addSubTreeUrls(List<String> res, RepositoryNode node) throws Exception {
    if (node.hasContent()) {
      res.add(node.getNodeUrl());
    }
    Iterator<RepositoryNode> iter = node.listChildren(null, false);
    while (iter.hasNext()) {
      addSubTreeUrls(res, iter.next());
    }
  }

  String trimUrlForOs(String url) throws MalformedURLException {
    int pad = 10 + tempDirPath.length() + "/cache/xxx".length() +
      RepositoryNodeImpl.CONTENT_DIR.length() +
      Math.max(RepositoryNodeImpl.CURRENT_FILENAME.length(),
	       RepositoryNodeImpl.CURRENT_PROPS_FILENAME.length());

    // Account for slash escaping in query
    URL u = new URL(url);
    String query = u.getQuery();
    if (query!=null) {
      pad +=
	(LockssRepositoryImpl.escapeQuery(query).length() - query.length());
    }


    PlatformUtil pi = PlatformUtil.getInstance();
    int max = pi.maxPathname() - pad;
    if (url.length() <= max) {
      return url;
    }
    url = trimTo(url, max);
    if (url.endsWith("/")) {
      url += "a";
    }
    log.info("Trimmed long URL to (" + url.length() + "): " + url);
    return url;
  }


  public void testMakeNodeLocation() throws Exception {
    RepositoryNodeImpl leaf = (RepositoryNodeImpl)
        repo.createNewNode("http://www.example.com/testDir");
    String nodeLoc = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							      mau);
    nodeLoc = LockssRepositoryImpl.mapUrlToFileLocation(nodeLoc,
        "http://www.example.com/testDir");
    File testFile = new File(nodeLoc);
    assertFalse(testFile.exists());
    leaf.createNodeLocation();
    assertTrue(testFile.exists());
    assertTrue(testFile.isDirectory());
  }

  public void testMakeNewVersion() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream 1", props);
    assertEquals(1, leaf.getCurrentVersion());

    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();
    assertEquals(2, leaf.getCurrentVersion());
    assertFalse(leaf.isIdenticalVersion());

    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 2", resultStr);
    props = leaf.getNodeContents().getProperties();
    assertEquals("value 2", props.getProperty("test 1"));
  }

  static final int DEL_NODE_DIR = 1;
  static final int DEL_CONTENT_DIR = 2;
  static final int DEL_CONTENT_FILE = 3;
  static final int DEL_PROPS_FILE = 4;


  public void testDisappearingFile(int whichFile, boolean tryRead)
      throws Exception {
    String url = "http://www.example.com/foo.html";
    RepositoryNodeImpl leaf = (RepositoryNodeImpl)repo.createNewNode(url);
    String nodeLoc = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							      mau);
    nodeLoc = LockssRepositoryImpl.mapUrlToFileLocation(nodeLoc, url);
    File testFile;
    switch (whichFile) {
    case DEL_NODE_DIR:
      testFile = new File(nodeLoc);
      break;
    case DEL_CONTENT_DIR:
      testFile = new File(nodeLoc, "#content");
      break;
    case DEL_CONTENT_FILE:
      testFile = new File(nodeLoc, "#content/current");
      break;
    case DEL_PROPS_FILE:
      testFile = new File(nodeLoc, "#content/current.props");
      break;
    default:
      throw new UnsupportedOperationException();
    }
    assertFalse(testFile.exists());

    Properties props1 = PropUtil.fromArgs("key1", "value 1");

    createContentVersion(leaf, "test content 11111", props1);
    assertEquals(1, leaf.getCurrentVersion());

    assertTrue(testFile.exists());
    switch (whichFile) {
    case DEL_NODE_DIR:
    case DEL_CONTENT_DIR:
      assertTrue(FileUtil.delTree(testFile));
      break;
    case DEL_CONTENT_FILE:
    case DEL_PROPS_FILE:
      assertTrue(testFile.delete());
      break;
    }
    assertFalse(testFile.exists());

    Properties props2 = PropUtil.fromArgs("key2", "value 2");
    RepositoryNode leaf2 = repo.createNewNode(url);
    assertSame(leaf, leaf2);
    assertTrue(leaf.hasContent());
    if (tryRead) {
      try {
	getLeafContent(leaf);
      } catch (LockssRepository.RepositoryStateException e) {
	// expected
      }
    }
    leaf2.makeNewVersion();

    writeToLeaf(leaf, "test content 22222");
    leaf.setNewProperties(props2);
    leaf.sealNewVersion();
    assertFalse(leaf.isIdenticalVersion());

    assertTrue(testFile.exists());
    int expver = 2;
    // if we tried to read while node or content dir was missing, version
    // number will have been reset.
    if (tryRead) {
      switch (whichFile) {
      case DEL_NODE_DIR:
      case DEL_CONTENT_DIR:
	expver = 1;
      }
    }
    assertEquals(expver, leaf.getCurrentVersion());

    assertEquals("test content 22222", getLeafContent(leaf));
    assertEquals("value 2", leaf.getNodeContents().getProperties().get("key2"));
  }

  public void testDisappearingNodeDir() throws Exception {
    testDisappearingFile(DEL_NODE_DIR, false);
  }

  public void testDisappearingContentDir() throws Exception {
    testDisappearingFile(DEL_CONTENT_DIR, false);
  }

  public void testDisappearingContentFile() throws Exception {
    testDisappearingFile(DEL_CONTENT_FILE, false);
  }

  public void testDisappearingPropsFile() throws Exception {
    testDisappearingFile(DEL_PROPS_FILE, false);
  }

  public void testDisappearingNodeDirWithRead() throws Exception {
    testDisappearingFile(DEL_NODE_DIR, true);
  }

  public void testDisappearingContentDirWithRead() throws Exception {
    testDisappearingFile(DEL_CONTENT_DIR, true);
  }

  public void testDisappearingContentFileWithRead() throws Exception {
    testDisappearingFile(DEL_CONTENT_FILE, true);
  }

  public void testDisappearingPropsFileWithRead() throws Exception {
    testDisappearingFile(DEL_PROPS_FILE, true);
  }

  public void testMakeNewVersionWithoutClosingStream() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream 1", new Properties());

    leaf.makeNewVersion();
    leaf.setNewProperties(new Properties());
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream("test stream 2");
    StreamUtil.copy(is, os);
    is.close();
    // don't close outputstream
    leaf.sealNewVersion();
    assertEquals(2, leaf.getCurrentVersion());
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 2", resultStr);
    assertFalse(leaf.isIdenticalVersion());
  }

  public void testMakeNewIdenticalVersionDefault() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf(
        "http://www.example.com/testDir/test.cache", "test stream", props));
    assertEquals(1, leaf.getCurrentVersion());
    // set the file extension
    leaf.dateValue = 123321;

    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream");
    leaf.sealNewVersion();
    assertEquals(1, leaf.getCurrentVersion());
    assertTrue(leaf.isIdenticalVersion());

    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
    props = leaf.getNodeContents().getProperties();
    assertEquals("value 2", props.getProperty("test 1"));

    // make sure proper files exist
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/test.cache");

    File testFileDir = new File(tempDirPath + "/#content");
    File[] files = testFileDir.listFiles();
    assertEquals(2, files.length);
    File testFile = new File(testFileDir, "current");
    assertTrue(testFile.exists());
    testFile = new File(testFileDir, "current.props");
    assertTrue(testFile.exists());
//    testFile = new File(testFileDir, "1.props-123321");
//    assertFalse(testFile.exists());

    // ensure non-identical version clears isIdenticalVersion()
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream not the same");
    leaf.sealNewVersion();
    assertEquals(2, leaf.getCurrentVersion());
    assertFalse(leaf.isIdenticalVersion());
  }

  public void testMakeNewIdenticalVersionOldWay() throws Exception {
    props.setProperty(RepositoryNodeImpl.PARAM_KEEP_ALL_PROPS_FOR_DUP_FILE,
                      "true");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf(
        "http://www.example.com/testDir/test.cache", "test stream", props));
    assertEquals(1, leaf.getCurrentVersion());
    // set the file extension
    leaf.dateValue = 123321;

    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream");
    leaf.sealNewVersion();
    assertEquals(1, leaf.getCurrentVersion());
    assertTrue(leaf.isIdenticalVersion());

    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
    props = leaf.getNodeContents().getProperties();
    assertEquals("value 2", props.getProperty("test 1"));

    // make sure proper files exist
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/test.cache");

    File testFileDir = new File(tempDirPath + "/#content");
    File[] files = testFileDir.listFiles();
    assertEquals(3, files.length);
    File testFile = new File(testFileDir, "current");
    assertTrue(testFile.exists());
    testFile = new File(testFileDir, "current.props");
    assertTrue(testFile.exists());
    testFile = new File(testFileDir, "1.props-123321");
    assertTrue(testFile.exists());
  }

  public void testMakeNewIdenticalVersionNewWay() throws Exception {
    props.setProperty(RepositoryNodeImpl.PARAM_KEEP_ALL_PROPS_FOR_DUP_FILE,
                      "false");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf(
        "http://www.example.com/testDir/test.cache", "test stream", props));
    assertEquals(1, leaf.getCurrentVersion());
    // set the file extension
    leaf.dateValue = 123321;

    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream");
    leaf.sealNewVersion();
    assertEquals(1, leaf.getCurrentVersion());
    assertTrue(leaf.isIdenticalVersion());

    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
    props = leaf.getNodeContents().getProperties();
    assertEquals("value 2", props.getProperty("test 1"));

    // make sure proper files exist
    tempDirPath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    tempDirPath = LockssRepositoryImpl.mapUrlToFileLocation(tempDirPath,
        "http://www.example.com/testDir/test.cache");

    File testFileDir = new File(tempDirPath + "/#content");
    File[] files = testFileDir.listFiles();
    assertEquals(2, files.length);
    File testFile = new File(testFileDir, "current");
    assertTrue(testFile.exists());
    testFile = new File(testFileDir, "current.props");
    assertTrue(testFile.exists());
//    testFile = new File(testFileDir, "1.props-123321");
//    assertFalse(testFile.exists());
  }

  public void testIdenticalVersionFixesVersionError() throws Exception {
    Properties props = new Properties();
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf(
        "http://www.example.com/testDir/test.cache", "test stream", props));
    assertEquals(1, leaf.getCurrentVersion());

    props = new Properties();
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    // set to error state
    leaf.currentVersion = 0;
    writeToLeaf(leaf, "test stream");
    assertEquals(0, leaf.currentVersion);
    leaf.sealNewVersion();
    // fixes error state, even though identical
    assertEquals(1, leaf.getCurrentVersion());
    assertTrue(leaf.isIdenticalVersion());
  }

  public void testMakeNewVersionFixesVersionError() throws Exception {
    Properties props = new Properties();
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf(
        "http://www.example.com/testDir/test.cache", "test stream", props));
    assertEquals(1, leaf.getCurrentVersion());

    props = new Properties();
    leaf.makeNewVersion();
    // set to error state
    leaf.currentVersion = -1;
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream2");
    leaf.sealNewVersion();
    // fixes error state
    assertEquals(1, leaf.getCurrentVersion());
    assertFalse(leaf.isIdenticalVersion());
  }

  public void testUnsealedRnc() throws Exception {
    String url = "http://www.example.com/foo.html";
    String content = "test test test";
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    RepositoryNode leaf = repo.createNewNode(url);
    try {
      leaf.getUnsealedRnc();
      fail("Should throw");
    } catch (IllegalStateException e) {
    }
    leaf.makeNewVersion();
    writeToLeaf(leaf, content);
    RepositoryNode.RepositoryNodeContents rnc = leaf.getUnsealedRnc();
    assertInputStreamMatchesString(content, rnc.getInputStream());
    assertInputStreamMatchesString(content, rnc.getInputStream());

    try {
      rnc.getProperties();
      fail("Should throw");
    } catch (UnsupportedOperationException e) {
    }
    try {
      rnc.addProperty("foo", "bar");
      fail("Should throw");
    } catch (UnsupportedOperationException e) {
    }
    rnc.release();

    leaf.setNewProperties(props);
    leaf.sealNewVersion();
    assertFalse(leaf.isIdenticalVersion());
    try {
      rnc.getInputStream();
      fail("Should throw");
    } catch (IllegalStateException e) {
    }
    RepositoryNode.RepositoryNodeContents rncSealed = leaf.getNodeContents();
    assertInputStreamMatchesString(content, rncSealed.getInputStream());
  }


  public void testGetInputStream() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", null);
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
  }

  public void testGetProperties() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", props);

    RepositoryNode.RepositoryNodeContents contents = leaf.getNodeContents();
    props = contents.getProperties();
    // close stream to allow the file to be renamed later
    // XXX 'getProperties()' creates an input stream, and 'release()' just
    // sets it to null.  The rename still fails in Windows unless the stream
    // is closed first.
    contents.getInputStream().close();
    contents.release();

    assertEquals("value 1", props.getProperty("test 1"));

    leaf.makeNewVersion();
    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();

    props = leaf.getNodeContents().getProperties();
    assertEquals("value 2", props.getProperty("test 1"));
  }

  // More addProperty tests below in testGetNodeVersions()
  public void testAddProperty() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 2");
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", props);

    RepositoryNode.RepositoryNodeContents rnc0 = leaf.getNodeContents();
    // get two more rncs on the same node
    RepositoryNode.RepositoryNodeContents rnc1 = leaf.getNodeContents();
    RepositoryNode.RepositoryNodeContents rnc2 = leaf.getNodeContents();
    // and have one of them load the props
    rnc2.getProperties();
    assertNotSame(rnc0, rnc1);

    // original props
    Properties rnc0p1 = rnc0.getProperties();
    assertEquals("value 2", rnc0p1.getProperty("test 1"));

    // add prop to node
    rnc0.addProperty(CachedUrl.PROPERTY_CHECKSUM, "checksum");

    // old Properties object isn't updated.
    assertFalse(rnc0p1.containsKey(CachedUrl.PROPERTY_CHECKSUM));

    // rnc properties should have new prop
    Properties rnc0p2 = rnc0.getProperties();
    // not essential, but this is the way it works
    assertNotSame(rnc0p1, rnc0p2);
    assertEquals("checksum", rnc0p2.getProperty(CachedUrl.PROPERTY_CHECKSUM));
    assertEquals("value 2", rnc0p2.getProperty("test 1"));

    // previously obtained rnc whose properties hadn't already been loaded
    // has new prop
    Properties rnc1p1 = rnc1.getProperties();
    assertEquals("checksum", rnc1p1.getProperty(CachedUrl.PROPERTY_CHECKSUM));
    assertEquals("value 2", rnc1p1.getProperty("test 1"));

    // previously obtained rnc whose properties *had* already been loaded
    // doesn't have new prop
    Properties rnc2p1 = rnc2.getProperties();
    assertFalse(rnc2p1.containsKey(CachedUrl.PROPERTY_CHECKSUM));
    assertEquals("value 2", rnc2p1.getProperty("test 1"));

    RepositoryNode.RepositoryNodeContents rnc = leaf.getNodeContents();
    props = rnc.getProperties();
    assertNotNull(props);
    assertTrue(props.containsKey(CachedUrl.PROPERTY_CHECKSUM));
    assertEquals("checksum", props.getProperty(CachedUrl.PROPERTY_CHECKSUM));

  }

  RepositoryNode createNodeWithCorruptProps(String url) throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    RepositoryNode leaf = createLeaf(url, "test stream", props);

    RepositoryNodeImpl leafImpl = (RepositoryNodeImpl)leaf;
    File propsFile = new File(leafImpl.getContentDir(),
			      RepositoryNodeImpl.CURRENT_PROPS_FILENAME);
    // Write a Malformed unicode escape that will cause Properties.load()
    // to throw
    OutputStream os =
      new BufferedOutputStream(new FileOutputStream(propsFile, true));
    os.write("\\uxxxxfoo=bar".getBytes());
    os.close();
    return leaf;
  }

  public void testCorruptProperties1() throws Exception {
    RepositoryNode leaf =
      createNodeWithCorruptProps("http://www.example.com/testDir/test.cache");

    assertFalse(leaf.hasContent());
    assertTrue(leaf.isDeleted());
    leaf.makeNewVersion();
    writeToLeaf(leaf, "test stream");
    leaf.setNewProperties(new Properties());
    leaf.sealNewVersion();
    assertTrue(leaf.hasContent());
    assertFalse(leaf.isDeleted());
  }

  public void testCorruptProperties2() throws Exception {
    String stem = "http://www.example.com/testDir";
    RepositoryNode leaf = createNodeWithCorruptProps(stem + "/test.cache");
    RepositoryNode leaf2 = createLeaf(stem + "/foo", "test stream", props);

    RepositoryNode dirEntry =
        repo.getNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listChildren(null, false);
    assertEquals(ListUtil.list(leaf2), ListUtil.fromIterator(childIt));
  }

  static String PROP_VAL_STEM = "valstem_";
  static String PROP_KEY = "key";

  static String cntnt(int ix) {
    return "content " + ix + "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(0, ix);
  }
  static int lngth(int ix) {
    return cntnt(ix).length();
  }

  void checkVersion(RepositoryNodeVersion nodeVer,
		    int exp, String addedPropVal)
      throws IOException {
    RepositoryNode.RepositoryNodeContents rnc = nodeVer.getNodeContents();
    String verCont = getRNCContent(rnc);
    Properties verProps = rnc.getProperties();
    log.debug("ver: " + nodeVer.getVersion() + ", content: " + verCont);
    log.debug2("ver: " + nodeVer.getVersion() + ", " +
	       StringUtil.shortName(props.getClass()) + ": " + verProps);
    assertEquals(exp, nodeVer.getVersion());

    assertEquals(cntnt(exp), verCont);
    assertEquals(lngth(exp), nodeVer.getContentSize());
    assertEquals(PROP_VAL_STEM+exp, verProps.getProperty(PROP_KEY));

    assertEquals(addedPropVal,
		 verProps.getProperty(CachedUrl.PROPERTY_CHECKSUM));


    // ensure can reread content from same rnc
    assertEquals(verCont, getRNCContent(rnc));
  }

  void addPropToVersion(RepositoryNodeVersion nodeVer, String propVal) {
    RepositoryNode.RepositoryNodeContents rnc = nodeVer.getNodeContents();
    rnc.addProperty(CachedUrl.PROPERTY_CHECKSUM, propVal);
  }
  
  public void testGetNodeVersion() throws Exception {
    int max = 5;
    String url = "http://www.example.com/versionedcontent.txt";
    Properties props = new Properties();

    RepositoryNode leaf = repo.createNewNode(url);
    // create several versions
    for (int ix = 1; ix <= max; ix++) {
      props.setProperty(PROP_KEY, PROP_VAL_STEM+ix);
      createContentVersion(leaf, cntnt(ix), props);
    }
    // getNodeVersion(current) should return the main node
    assertEquals(leaf, leaf.getNodeVersion(leaf.getCurrentVersion()));

    // loop through other versions checking version, content, props
    for (int ix = 1; ix < max; ix++) {
      RepositoryNodeVersion nodeVer = leaf.getNodeVersion(ix);
      checkVersion(nodeVer, ix, null);
    }
  }

  public void testGetNodeVersions() throws Exception {
    int max = 5;
    String url = "http://www.example.com/versionedcontent.txt";
    Properties props = new Properties();

    // No existing node
    RepositoryNode leaf0 = repo.createNewNode(url);
    assertFalse(leaf0.hasContent());
    RepositoryNodeVersion[] vers0 = leaf0.getNodeVersions();
    assertEquals(1, vers0.length);

    RepositoryNode leaf = repo.createNewNode(url);
    // create several versions
    for (int ix = 1; ix <= max; ix++) {
      props.setProperty(PROP_KEY, PROP_VAL_STEM+ix);
      createContentVersion(leaf, cntnt(ix), props);
    }
    // check expected current version number
    assertEquals(max, leaf.getCurrentVersion());
    assertEquals(max, leaf.getVersion());

    // check version, content, props of current version
    assertEquals(cntnt(max), getLeafContent(leaf));
    assertEquals(lngth(max), leaf.getContentSize());
    props = leaf.getNodeContents().getProperties();
    assertEquals(PROP_VAL_STEM+max, props.getProperty(PROP_KEY));

    // ask for all older versions
    RepositoryNodeVersion[] vers = leaf.getNodeVersions();
    assertEquals(max, vers.length);
    RepositoryNode.RepositoryNodeContents[] rncs =
      new RepositoryNode.RepositoryNodeContents[max];
    // loop through them checking version, content, props
    for (int ix = 0; ix < max; ix++) {
      int exp = max - ix;
      RepositoryNodeVersion nodeVer = vers[ix];

      rncs[ix] = nodeVer.getNodeContents();
      
      checkVersion(nodeVer, exp, null);
    }

    // now ask for and check a subset of the older versions
    assertTrue("max must be at least 4 for this test", max >= 4);
    int numver = max - 2;
    RepositoryNodeVersion[] subvers = leaf.getNodeVersions(numver);
    assertEquals(numver, subvers.length);
    for (int ix = 0; ix < numver; ix++) {
      int exp = max - ix;
      RepositoryNodeVersion nodeVer = subvers[ix];
      log.debug("ver: " + nodeVer.getVersion() + ", content: " +
		getLeafContent(nodeVer));
      assertEquals(exp, nodeVer.getVersion());

      assertEquals(cntnt(exp), getLeafContent(nodeVer));
      assertEquals(lngth(exp), nodeVer.getContentSize());
      props = nodeVer.getNodeContents().getProperties();
      assertEquals(PROP_VAL_STEM+exp, props.getProperty(PROP_KEY));
    }

    // Add a property to selected versions, ensure only the correct
    // versions change

    addPropToVersion(vers[0], "5new1");
    checkVersion(vers[0], 5, "5new1");
    for (int ix = 1; ix <= 4; ix++) {
      checkVersion(vers[ix], max-ix, null);
    }

    addPropToVersion(vers[2], "3new1");
    checkVersion(vers[2], 3, "3new1");
    checkVersion(vers[0], 5, "5new1");
    for (int ix : new int[]{1,3,4}) {
      checkVersion(vers[ix], max-ix, null);
    }

  }

  public void testIllegalVersionOperations() throws Exception {
    RepositoryNode.RepositoryNodeContents rnc;
    RepositoryNodeVersion nv;

    RepositoryNode leaf =
      repo.createNewNode("http://www.example.com/testDir/test.cache");
    try {
      nv = leaf.getNodeVersion(7);
      fail("No content, shouldn't be able to get versioned node: " + nv);
    } catch (UnsupportedOperationException e) { }
    // create first version
    Properties props = new Properties();
    props.setProperty("key", "val1");
    createContentVersion(leaf, cntnt(1), props);

    // We're allowed to get a RepositoryNodeVersion when the version
    // doesn't exist ...
    nv = leaf.getNodeVersion(7);
    // but all operations on it should throw
    try {
      nv.getContentSize();
      fail("No version; shouldn't get content size");
    } catch (UnsupportedOperationException e) { }
    try {
      rnc = nv.getNodeContents();
      fail("No version; shouldn't get RepositoryNodeContents");
    } catch (UnsupportedOperationException e) { }
  }

  public void testDirContent() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", null);
    assertTrue(leaf.hasContent());

    RepositoryNode dir =
        repo.getNode("http://www.example.com/testDir");
    dir.makeNewVersion();
    writeToLeaf(dir, "test stream");
    dir.setNewProperties(new Properties());
    dir.sealNewVersion();
    assertTrue(dir.hasContent());

    dir = createLeaf("http://www.example.com/testDir/test.cache/new.test",
                     "test stream", null);
    assertTrue(dir.hasContent());
  }

  public void testNodeSize() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/testDir/test.cache",
        "test stream", null);
    assertTrue(leaf.hasContent());
    assertEquals(11, (int)leaf.getContentSize());
  }

  public void testTreeSize() throws Exception {
    createLeaf("http://www.example.com/testDir", "test", null);
    createLeaf("http://www.example.com/testDir/test1", "test1", null);
    createLeaf("http://www.example.com/testDir/test2", "test2", null);
    createLeaf("http://www.example.com/testDir/test3/branch1",
               "test33", null);
    createLeaf("http://www.example.com/testDir/test3/branch2",
               "test33", null);

    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir");
    assertEquals(-1, leaf.getTreeContentSize(null, false));
    assertEquals(26, leaf.getTreeContentSize(null, true));
    assertEquals(26, leaf.getTreeContentSize(null, false));
    leaf = repo.getNode("http://www.example.com/testDir/test1");
    assertEquals(5, leaf.getTreeContentSize(null, true));
    leaf = repo.getNode("http://www.example.com/testDir/test3");
    assertEquals(12, leaf.getTreeContentSize(null, true));
    CachedUrlSetSpec cuss =
      new RangeCachedUrlSetSpec("http://www.example.com/testDir/test3",
				"/branch1", "/branch1");
    assertEquals(6, leaf.getTreeContentSize(cuss, true));
  }

  public void testDetermineParentNode() throws Exception {
    repo.createNewNode("http://www.example.com");
    repo.createNewNode("http://www.example.com/test");
    assertNotNull(repo.getNode("http://www.example.com/test"));
    RepositoryNodeImpl node = (RepositoryNodeImpl)repo.createNewNode(
      "http://www.example.com/test/branch");
    assertEquals("http://www.example.com/test/branch", node.getNodeUrl());
    node = node.determineParentNode();
    assertEquals("http://www.example.com/test", node.getNodeUrl());
    node = node.determineParentNode();
    assertEquals("http://www.example.com", node.getNodeUrl());
    node = node.determineParentNode();
    assertEquals(AuUrl.PROTOCOL, node.getNodeUrl());
    node = node.determineParentNode();
    assertEquals(AuUrl.PROTOCOL, node.getNodeUrl());

    // query args containing slash formerly caused determineParentNode() to
    // return wrong result
    RepositoryNodeImpl node2 = (RepositoryNodeImpl)repo.createNewNode(
      "http://www.example.com/test/branch/file?http://foo.bar/path/to/file");
    node = node2.determineParentNode();
    assertEquals("http://www.example.com/test/branch", node.getNodeUrl());
  }

  public void testCacheInvalidation() throws Exception {
    RepositoryNodeImpl root =
        (RepositoryNodeImpl)createLeaf("http://www.example.com",
                                       "test", null);
    RepositoryNodeImpl branch =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/branch",
                                       "test", null);
    RepositoryNodeImpl branch2 =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/branch/branch2",
                                       "test", null);
    // This one has directory level with no node prop file, to check that
    // cache invalidation traverses them correctly
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/branch/branch2/a/b/c/leaf",
                                       "test", null);
    assertNull(branch.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    assertNull(leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    // force invalidation to happen
    branch.nodeProps.setProperty(TREE_SIZE_PROPERTY, "789");
    branch.invalidateCachedValues(true);
    // should now be explicitly marked invalid
    assertEquals(RepositoryNodeImpl.INVALID,
		 branch.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    assertEquals(RepositoryNodeImpl.INVALID,
		 branch.nodeProps.getProperty(CHILD_COUNT_PROPERTY));
    // fake prop set at root to check invalidation stops properly
    root.nodeProps.setProperty(TREE_SIZE_PROPERTY, "789");
    root.nodeProps.setProperty(CHILD_COUNT_PROPERTY, "3");
    // don't set branch so the invalidate stops there
    branch2.nodeProps.setProperty(TREE_SIZE_PROPERTY, "456");
    branch2.nodeProps.setProperty(CHILD_COUNT_PROPERTY, "1");
    leaf.nodeProps.setProperty(TREE_SIZE_PROPERTY, "123");
    leaf.nodeProps.setProperty(CHILD_COUNT_PROPERTY, "0");

    leaf.invalidateCachedValues(true);
    // shoulddn't be set here anymore
    assertFalse(isPropValid(leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertFalse(isPropValid(leaf.nodeProps.getProperty(CHILD_COUNT_PROPERTY)));
    // or here (requires recursing up through dirs that have no node props
    // file)
    assertFalse(isPropValid(branch2.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertFalse(isPropValid(branch2.nodeProps.getProperty(CHILD_COUNT_PROPERTY)));
    // still invalid, recursion should have stopped here
    assertFalse(isPropValid(branch.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertFalse(isPropValid(branch.nodeProps.getProperty(CHILD_COUNT_PROPERTY)));
    // so not cleared these
    assertTrue(isPropValid(root.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertTrue(isPropValid(root.nodeProps.getProperty(CHILD_COUNT_PROPERTY)));
    assertEquals("789", root.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    assertEquals("3", root.nodeProps.getProperty(CHILD_COUNT_PROPERTY));
  }

  // Add a first child after #node_props have been created
  public void testCacheInvalidationIncremental() throws Exception {
    RepositoryNodeImpl root =
        (RepositoryNodeImpl)createLeaf("http://www.example.com",
                                       "test", null);
    RepositoryNodeImpl branch =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/branch",
                                       "test", null);
    assertFalse(isPropValid(branch.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertTrue(branch.isLeaf());
    assertEquals(0, branch.getChildCount());
    assertEquals(4, branch.getTreeContentSize(null, true));
    assertTrue(isPropValid(branch.nodeProps.getProperty(TREE_SIZE_PROPERTY)));


    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/branch/leaf?http://foo/bar/baz",
                                       "test", null);
    assertEquals(8, branch.getTreeContentSize(null, true));
    assertFalse(branch.isLeaf());
  }

  boolean isPropValid(String val) {
    return RepositoryNodeImpl.isPropValid(val);
  }

  boolean isPropInvalid(String val) {
    return RepositoryNodeImpl.isPropInvalid(val);
  }

  public void testTreeSizeCaching() throws Exception {
    createLeaf("http://www.example.com/testDir", "test", null);

    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)repo.getNode("http://www.example.com/testDir");
    assertNull(leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    assertEquals(4, leaf.getTreeContentSize(null, true));
    assertEquals("4", leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY));
    leaf.markAsDeleted();
    assertTrue(isPropInvalid(leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY)));
    assertEquals(0, leaf.getTreeContentSize(null, true));
    assertEquals("0", leaf.nodeProps.getProperty(TREE_SIZE_PROPERTY));
  }

  public void testChildCount() throws Exception {
    createLeaf("http://www.example.com/testDir", "test", null);

    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)repo.getNode("http://www.example.com/testDir");
    assertNull(leaf.nodeProps.getProperty(CHILD_COUNT_PROPERTY));
    assertEquals(0, leaf.getChildCount());
    assertEquals("0", leaf.nodeProps.getProperty(CHILD_COUNT_PROPERTY));

    createLeaf("http://www.example.com/testDir/test1", "test1", null);
    createLeaf("http://www.example.com/testDir/test2", "test2", null);
    assertEquals(2, leaf.getChildCount());
    assertEquals("2", leaf.nodeProps.getProperty(CHILD_COUNT_PROPERTY));
  }

  public void testDeactivate() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    assertTrue(leaf.hasContent());
    assertFalse(leaf.isContentInactive());
    assertEquals(1, leaf.getCurrentVersion());
    assertNull(leaf.nodeProps.getProperty(RepositoryNodeImpl.INACTIVE_CONTENT_PROPERTY));

    leaf.deactivateContent();
    assertFalse(leaf.hasContent());
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());
    assertEquals("true", leaf.nodeProps.getProperty(RepositoryNodeImpl.INACTIVE_CONTENT_PROPERTY));
  }

  public void testDelete() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    assertTrue(leaf.hasContent());
    assertFalse(leaf.isDeleted());
    assertEquals(1, leaf.getCurrentVersion());
    assertNull(leaf.nodeProps.getProperty(RepositoryNodeImpl.DELETION_PROPERTY));

    leaf.markAsDeleted();
    assertFalse(leaf.hasContent());
    assertTrue(leaf.isDeleted());
    assertEquals(RepositoryNodeImpl.DELETED_VERSION, leaf.getCurrentVersion());
    assertEquals("true", leaf.nodeProps.getProperty(RepositoryNodeImpl.DELETION_PROPERTY));
  }

  public void testUnDelete() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    leaf.markAsDeleted();
    assertTrue(leaf.isDeleted());
    assertEquals(RepositoryNodeImpl.DELETED_VERSION, leaf.getCurrentVersion());

    leaf.markAsNotDeleted();
    assertFalse(leaf.isContentInactive());
    assertFalse(leaf.isDeleted());
    assertEquals(1, leaf.getCurrentVersion());
    // make to null, not 'false'
    assertNull(leaf.nodeProps.getProperty(RepositoryNodeImpl.DELETION_PROPERTY));
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
  }

  public void testRestoreLastVersion() throws Exception {
    Properties props = new Properties();
    props.setProperty("test 1", "value 1");
    RepositoryNode leaf =
        createLeaf("http://www.example.com/test1", "test stream 1", props);
    assertEquals(1, leaf.getCurrentVersion());

    props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();
    assertEquals(2, leaf.getCurrentVersion());

    leaf.restoreLastVersion();
    assertEquals(1, leaf.getCurrentVersion());

    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 1", resultStr);
    props = leaf.getNodeContents().getProperties();
    assertEquals("value 1", props.getProperty("test 1"));
  }

  public void testReactivateViaRestore() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    leaf.deactivateContent();
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());

    leaf.restoreLastVersion();
    assertFalse(leaf.isContentInactive());
    assertEquals(1, leaf.getCurrentVersion());
    // back to null, not 'false'
    assertNull(leaf.nodeProps.getProperty(RepositoryNodeImpl.INACTIVE_CONTENT_PROPERTY));
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream", resultStr);
  }

  public void testReactivateViaNewVersion() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com/test1",
                                       "test stream", null);
    leaf.deactivateContent();
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());

    Properties props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.sealNewVersion();
    assertFalse(leaf.isContentInactive());
    assertEquals(2, leaf.getCurrentVersion());
    String resultStr = getLeafContent(leaf);
    assertEquals("test stream 2", resultStr);

    File lastProps = new File(leaf.contentDir, "1.props");
    assertTrue(lastProps.exists());
    InputStream is =
        new BufferedInputStream(new FileInputStream(lastProps));
    props.load(is);
    is.close();
    // make sure the 'was inactive' property hasn't been lost
    assertEquals("true",
                 props.getProperty(RepositoryNodeImpl.NODE_WAS_INACTIVE_PROPERTY));
  }

  public void testAbandonReactivateViaNewVersion() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/test1", "test stream", null);
    leaf.deactivateContent();
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());

    Properties props = new Properties();
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.abandonNewVersion();
    assertTrue(leaf.isContentInactive());
    assertEquals(RepositoryNodeImpl.INACTIVE_VERSION, leaf.getCurrentVersion());
  }

  public void testAbandonNewVersion() throws Exception {
    RepositoryNode leaf =
        createLeaf("http://www.example.com/test1", "test stream", null);
    assertTrue(leaf.hasContent());
    props.setProperty("test 1", "value 2");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 2");
    leaf.abandonNewVersion();

    props.setProperty("test 1", "value 3");
    leaf.makeNewVersion();
    leaf.setNewProperties(props);
    writeToLeaf(leaf, "test stream 3");
    leaf.sealNewVersion();
  }

  public void testIsLeaf() throws Exception {
    createLeaf("http://www.example.com/testDir/test1", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/test4", "test stream", null);

    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/test1");
    assertTrue(leaf.isLeaf());
    leaf = repo.getNode("http://www.example.com/testDir/branch1");
    assertFalse(leaf.isLeaf());
  }

  public void testListInactiveNodes() throws Exception {
    createLeaf("http://www.example.com/testDir/test1", "test stream", null);
    createLeaf("http://www.example.com/testDir/test2", "test stream", null);
    createLeaf("http://www.example.com/testDir/test3", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch1/test4", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch2/test5", "test stream", null);

    RepositoryNode dirEntry = repo.getNode("http://www.example.com/testDir");
    Iterator childIt = dirEntry.listChildren(null, false);
    ArrayList childL = new ArrayList(3);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    String[] expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/branch2",
      "http://www.example.com/testDir/test1",
      "http://www.example.com/testDir/test2",
      "http://www.example.com/testDir/test3"
      };
    assertSameElements(expectedA, childL);

    RepositoryNode leaf = repo.getNode("http://www.example.com/testDir/test2");
    leaf.deactivateContent();
    // this next shouldn't be excluded since it isn't a leaf node
    leaf = repo.getNode("http://www.example.com/testDir/branch1");
    leaf.deactivateContent();
    // this next should be excluded because it's deleted
    leaf = repo.getNode("http://www.example.com/testDir/branch2");
    leaf.markAsDeleted();

    childIt = dirEntry.listChildren(null, false);
    childL = new ArrayList(2);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/test1",
      "http://www.example.com/testDir/test3"
      };
    assertSameElements("Excluding inactive nodes failed.", expectedA, childL);

    childIt = dirEntry.listChildren(null, true);
    childL = new ArrayList(3);
    while (childIt.hasNext()) {
      RepositoryNode node = (RepositoryNode)childIt.next();
      childL.add(node.getNodeUrl());
    }
    expectedA = new String[] {
      "http://www.example.com/testDir/branch1",
      "http://www.example.com/testDir/test1",
      "http://www.example.com/testDir/test2",
      "http://www.example.com/testDir/test3"
      };
    assertSameElements("Including inactive nodes failed.", expectedA, childL);
  }

  public void testDeleteInnerNode() throws Exception {
    createLeaf("http://www.example.com/testDir/test1", "test stream", null);
    createLeaf("http://www.example.com/testDir/test2", "test stream", null);

    RepositoryNode dirEntry = repo.getNode("http://www.example.com/testDir");
    assertFalse(dirEntry.isDeleted());
    dirEntry.markAsDeleted();
    assertTrue(dirEntry.isDeleted());
    dirEntry.markAsNotDeleted();
    assertFalse(dirEntry.isDeleted());
  }

  public void testGetFileStrings() throws Exception {
    RepositoryNodeImpl node = (RepositoryNodeImpl)repo.createNewNode(
        "http://www.example.com/test.url");
    node.initNodeRoot();
    String contentStr = FileUtil.sysDepPath(node.nodeLocation + "/#content");
    contentStr = contentStr.replaceAll("\\//", "\\\\/"); // sysDepPath mangles our backslashes
    assertEquals(contentStr, node.getContentDir().toString());
    contentStr = contentStr + File.separator;
    String expectedStr = contentStr + "123";
    assertEquals(expectedStr,
                 node.getVersionedCacheFile(123).getAbsolutePath());
    expectedStr = contentStr + "123.props";
    assertEquals(expectedStr,
                 node.getVersionedPropsFile(123).getAbsolutePath());
    expectedStr = contentStr + "inactive";
    assertEquals(expectedStr, node.getInactiveCacheFile().getAbsolutePath());
    expectedStr = contentStr + "inactive.props";
    assertEquals(expectedStr, node.getInactivePropsFile().getAbsolutePath());
  }

  public void testCheckNodeConsistency() throws Exception {
    // check returns proper values for errors
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)repo.createNewNode("http://www.example.com/testDir"));
    leaf.makeNewVersion();
    // should abort and return true since version open
    leaf.failRootConsist = true;
    assertTrue(leaf.checkNodeConsistency());

    // finish write
    leaf.setNewProperties(new Properties());
    writeToLeaf(leaf, "test stream");
    leaf.sealNewVersion();

    // should return false if node root fails
    assertFalse(leaf.checkNodeConsistency());
    leaf.failRootConsist = false;
    assertTrue(leaf.checkNodeConsistency());

    // check returns false if content fails
    leaf.failContentConsist = true;
    assertFalse(leaf.checkNodeConsistency());
    leaf.failContentConsist = false;
    assertTrue(leaf.checkNodeConsistency());

    // check returns false if current info load fails
    leaf.failEnsureCurrentLoaded = true;
    assertFalse(leaf.checkNodeConsistency());
    leaf.failEnsureCurrentLoaded = false;
    assertTrue(leaf.checkNodeConsistency());
  }

  public void testCheckNodeRootConsistency() throws Exception {
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)repo.createNewNode("http://www.example.com/testDir"));
    leaf.createNodeLocation();
    assertTrue(leaf.nodeRootFile.exists());
    // returns true when normal
    assertTrue(leaf.checkNodeRootConsistency());

    leaf.nodeRootFile.delete();
    assertFalse(leaf.nodeRootFile.exists());
    // creates dir, returns true when missing
    assertTrue(leaf.checkNodeRootConsistency());
    assertTrue(leaf.nodeRootFile.exists());
    assertTrue(leaf.nodeRootFile.isDirectory());

    // fail node props load
    leaf.getChildCount();
    assertTrue(leaf.nodePropsFile.exists());
    File renameFile = new File(leaf.nodePropsFile.getAbsolutePath()+
                               RepositoryNodeImpl.FAULTY_FILE_EXTENSION);
    assertFalse(renameFile.exists());
    leaf.failPropsLoad = true;
    assertTrue(leaf.checkNodeRootConsistency());
    assertFalse(leaf.nodePropsFile.exists());
    assertTrue(renameFile.exists());
  }

  public void testCheckContentConsistency() throws Exception {
    MyMockRepositoryNode leaf = new MyMockRepositoryNode(
        (RepositoryNodeImpl)createLeaf("http://www.example.com/testDir",
        "test stream", null));
    leaf.ensureCurrentInfoLoaded();

    // should return false if content dir fails
    MyMockRepositoryNode.failEnsureDirExists = true;
    assertFalse(leaf.checkContentConsistency());
    MyMockRepositoryNode.failEnsureDirExists = false;
    assertTrue(leaf.checkContentConsistency());

    // should return false if content file absent
    File renameFile =
        new File(leaf.currentCacheFile.getAbsolutePath()+"RENAME");
    assertTrue(PlatformUtil.updateAtomically(leaf.currentCacheFile, renameFile));
    assertFalse(leaf.checkContentConsistency());
    PlatformUtil.updateAtomically(renameFile, leaf.currentCacheFile);
    assertTrue(leaf.checkContentConsistency());

    // should return false if content props absent
    PlatformUtil.updateAtomically(leaf.currentPropsFile, renameFile);
    assertFalse(leaf.checkContentConsistency());
    PlatformUtil.updateAtomically(renameFile, leaf.currentPropsFile);
    assertTrue(leaf.checkContentConsistency());

    // should return false if inactive and files missing
    leaf.currentVersion = RepositoryNodeImpl.INACTIVE_VERSION;
    assertFalse(leaf.checkContentConsistency());
    PlatformUtil.updateAtomically(leaf.currentPropsFile, leaf.getInactivePropsFile());
    assertFalse(leaf.checkContentConsistency());
    PlatformUtil.updateAtomically(leaf.currentCacheFile, leaf.getInactiveCacheFile());
    assertTrue(leaf.checkContentConsistency());
    PlatformUtil.updateAtomically(leaf.getInactivePropsFile(), leaf.currentPropsFile);
    assertFalse(leaf.checkContentConsistency());
    // finish restoring
    PlatformUtil.updateAtomically(leaf.getInactiveCacheFile(), leaf.currentCacheFile);
    leaf.currentVersion = 1;
    assertTrue(leaf.checkContentConsistency());

    // remove residual files
    // - create files
    FileOutputStream fos = new FileOutputStream(leaf.tempCacheFile);
    StringInputStream sis = new StringInputStream("test stream");
    StreamUtil.copy(sis, fos);
    fos.close();
    sis.close();

    fos = new FileOutputStream(leaf.tempPropsFile);
    sis = new StringInputStream("test stream");
    StreamUtil.copy(sis, fos);
    fos.close();
    sis.close();

    // should be removed
    assertTrue(leaf.tempCacheFile.exists());
    assertTrue(leaf.tempPropsFile.exists());
    assertTrue(leaf.checkContentConsistency());
    assertFalse(leaf.tempCacheFile.exists());
    assertFalse(leaf.tempPropsFile.exists());
}


  public void testEnsureDirExists() throws Exception {
    RepositoryNodeImpl leaf =
        (RepositoryNodeImpl)createLeaf("http://www.example.com", null, null);
    File testDir = new File(tempDirPath, "testDir");
    // should return true if dir exists
    testDir.mkdir();
    assertTrue(testDir.exists());
    assertTrue(testDir.isDirectory());
    assertTrue(leaf.ensureDirExists(testDir));

    // should create dir, return true if not exists
    testDir.delete();
    assertFalse(testDir.exists());
    assertTrue(leaf.ensureDirExists(testDir));
    assertTrue(testDir.exists());
    assertTrue(testDir.isDirectory());

    // should rename file, create dir, return true if file exists
    // -create file
    testDir.delete();
    FileOutputStream fos = new FileOutputStream(testDir);
    StringInputStream sis = new StringInputStream("test stream");
    StreamUtil.copy(sis, fos);
    fos.close();
    sis.close();
    assertTrue(testDir.exists());
    assertTrue(testDir.isFile());

    // rename via 'ensureDirExists()'
    File renameFile = new File(tempDirPath, "testDir"+
        RepositoryNodeImpl.FAULTY_FILE_EXTENSION);
    assertFalse(renameFile.exists());
    assertTrue(leaf.ensureDirExists(testDir));
    assertTrue(testDir.isDirectory());
    assertEquals("test stream", StringUtil.fromFile(renameFile));
  }

  public void testCheckFileExists() throws Exception {
    // return false if doesn't exist
    File testFile = new File(tempDirPath, "testFile");
    assertFalse(testFile.exists());
    assertFalse(RepositoryNodeImpl.checkFileExists(testFile, "test file"));

    // rename if dir (to make room for file creation), then return false
    testFile.mkdir();
    File renameDir = new File(tempDirPath, "testFile"+
        RepositoryNodeImpl.FAULTY_FILE_EXTENSION);
    assertTrue(testFile.exists());
    assertTrue(testFile.isDirectory());
    assertFalse(renameDir.exists());
    assertFalse(RepositoryNodeImpl.checkFileExists(testFile, "test file"));
    assertFalse(testFile.exists());
    assertTrue(renameDir.exists());
    assertTrue(renameDir.isDirectory());

    // return true if exists
    FileOutputStream fos = new FileOutputStream(testFile);
    StringInputStream sis = new StringInputStream("test stream");
    StreamUtil.copy(sis, fos);
    fos.close();
    sis.close();
    assertTrue(testFile.exists());
    assertTrue(testFile.isFile());
    assertTrue(RepositoryNodeImpl.checkFileExists(testFile, "test file"));
    assertEquals("test stream", StringUtil.fromFile(testFile));
  }

  public void testCheckChildCountCacheAccuracy() throws Exception {
    createLeaf("http://www.example.com/testDir/branch2", "test stream", null);
    createLeaf("http://www.example.com/testDir/branch3", "test stream", null);

    RepositoryNodeImpl dirEntry =
        (RepositoryNodeImpl)repo.getNode("http://www.example.com/testDir");
    assertEquals(2, dirEntry.getChildCount());
    assertEquals("2",
        dirEntry.nodeProps.getProperty(RepositoryNodeImpl.CHILD_COUNT_PROPERTY));

    // check that no change to valid count cache
    dirEntry.checkChildCountCacheAccuracy();
    log.debug2("child count: " +
	       dirEntry.nodeProps.getProperty(RepositoryNodeImpl.CHILD_COUNT_PROPERTY));
    assertEquals("2",
        dirEntry.nodeProps.getProperty(RepositoryNodeImpl.CHILD_COUNT_PROPERTY));

    // check that invalid cache removed
    dirEntry.nodeProps.setProperty(RepositoryNodeImpl.CHILD_COUNT_PROPERTY, "3");
    dirEntry.checkChildCountCacheAccuracy();
    assertEquals(RepositoryNodeImpl.INVALID,
                 dirEntry.nodeProps.getProperty(RepositoryNodeImpl.CHILD_COUNT_PROPERTY));
  }

  public void testEncodeUrl() {
    assertEquals(null, RepositoryNodeImpl.encodeUrl(null));
    assertEquals("", RepositoryNodeImpl.encodeUrl(""));
    assertEquals("www.example.com",
		 RepositoryNodeImpl.encodeUrl("www.example.com"));
    assertEquals("www.example.com/val",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val"));
    assertEquals("www.example.com%5Cval",
		 RepositoryNodeImpl.encodeUrl("www.example.com\\val"));
    assertEquals("www.example.com/val%5Cval",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val\\val"));
    assertEquals("www.example.com/val/val",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/val"));
    assertEquals("www.example.com/val%5C%5Cval",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val\\\\val"));
    assertEquals("www.example.com/val/val",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/val"));
    assertEquals("www.example.com/val/val/",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/val/"));
    assertEquals("www.example.com/val/val%5C",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/val\\"));
    assertEquals("www.example.com%5Cval%5Cval%5C",
		 RepositoryNodeImpl.encodeUrl("www.example.com\\val\\val\\"));
  }

  public void testEncodeUrlUnicode() {
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_MAX_COMPONENT_LENGTH,
				  "30");

    assertEquals("www.example.com/val/12345678901234567890123456789\\/0123",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/123456789012345678901234567890123"));
    assertEquals("www.example.com/val/123\u00E7567890123456789012345678\\/90123",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/123\u00E756789012345678901234567890123"));
  }

  public void testEncodeUrlCompatibility() {
    ConfigurationUtil.addFromArgs(RepositoryManager.PARAM_ENABLE_LONG_COMPONENTS_COMPATIBILITY,
				  "true");
    assertEquals(null, RepositoryNodeImpl.encodeUrl(null));
    assertEquals("", RepositoryNodeImpl.encodeUrl(""));
    assertEquals("www.example.com",
		 RepositoryNodeImpl.encodeUrl("www.example.com"));
    assertEquals("www.example.com/val",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val"));
    assertEquals("www.example.com%5cval",
		 RepositoryNodeImpl.encodeUrl("www.example.com\\val"));
    assertEquals("www.example.com/val%5cval",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val\\val"));
    assertEquals("www.example.com/val/val",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/val"));
    assertEquals("www.example.com/val%5c%5cval",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val\\\\val"));
    assertEquals("www.example.com/val/val",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/val"));
    assertEquals("www.example.com/val/val/",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/val/"));
    assertEquals("www.example.com/val/val%5c",
		 RepositoryNodeImpl.encodeUrl("www.example.com/val/val\\"));
    assertEquals("www.example.com%5cval%5cval%5c",
		 RepositoryNodeImpl.encodeUrl("www.example.com\\val\\val\\"));
  }

  public void testShortDecodeUrl() {
    assertEquals(null, RepositoryNodeImpl.decodeUrl(null));
    assertEquals("", RepositoryNodeImpl.decodeUrl(""));
    assertEquals("www.example.com",
		 RepositoryNodeImpl.decodeUrl("www.example.com"));
    assertEquals("www.example.com/val",
		 RepositoryNodeImpl.decodeUrl("www.example.com/val"));
    assertEquals("www.example.com%5Cval",
		 RepositoryNodeImpl.decodeUrl("www.example.com%5Cval"));
    assertEquals("www.example.com/val%5Cval",
		 RepositoryNodeImpl.decodeUrl("www.example.com/val%5Cval"));
    assertEquals("www.example.com/val/val",
		 RepositoryNodeImpl.decodeUrl("www.example.com/val/val"));
    assertEquals("www.example.com/val%5C%5Cval",
		 RepositoryNodeImpl.decodeUrl("www.example.com/val%5C%5Cval"));
    assertEquals("www.example.com/val/val",
		 RepositoryNodeImpl.decodeUrl("www.example.com/val/val"));
    assertEquals("www.example.com/val/val/",
		 RepositoryNodeImpl.decodeUrl("www.example.com/val/val/"));
    assertEquals("www.example.com/val/val%5C",
		 RepositoryNodeImpl.decodeUrl("www.example.com/val/val%5C"));
    assertEquals("www.example.com%5cval%5Cval%5c",
		 RepositoryNodeImpl.decodeUrl("www.example.com%5cval%5Cval%5c"));
  }
  
  public void testLongDecodeUrl() {
    StringBuilder longUrl = new StringBuilder();
    longUrl.append("www.example.com/");
    for(int i=0; i<218; i++)  {
      longUrl.append(i + ",");
    }
    longUrl.append(".");
    String result = RepositoryNodeImpl.encodeUrl(longUrl.toString());
    log.debug2(longUrl.toString());
    log.debug2(result);
    String result2 = RepositoryNodeImpl.decodeUrl(result);
    log.debug2(result2);
    assertTrue(longUrl.toString().equals(result2));
  }

  private RepositoryNode createLeaf(String url, String content,
      Properties props) throws Exception {
    return createLeaf(repo, url, content, props);
  }

  public static RepositoryNode createLeaf(LockssRepository repo, String url,
      String content, Properties props) throws Exception {
    RepositoryNode leaf = repo.createNewNode(url);
    createContentVersion(leaf, content, props);
    return leaf;
  }

  public static RepositoryNode createLeaf(LockssRepository repo, String url,
      InputStream contentStream, Properties props) throws Exception {
    RepositoryNode leaf = repo.createNewNode(url);
    createContentVersion(leaf, contentStream, props);
    return leaf;
  }

  public static void createContentVersion(RepositoryNode leaf,
					  String content, Properties props)
      throws Exception {
    leaf.makeNewVersion();
    writeToLeaf(leaf, content);
    if (props==null) {
      props = new Properties();
    }
    leaf.setNewProperties(props);
    leaf.sealNewVersion();
  }

  public static void createContentVersion(RepositoryNode leaf,
					  InputStream contentStream,
					  Properties props)
      throws Exception {
    leaf.makeNewVersion();
    writeToLeaf(leaf, contentStream);
    if (props==null) {
      props = new Properties();
    }
    leaf.setNewProperties(props);
    leaf.sealNewVersion();
  }

  public static void writeToLeaf(RepositoryNode leaf, String content)
      throws Exception {
    if (content==null) {
      content = "";
    }
    OutputStream os = leaf.getNewOutputStream();
    InputStream is = new StringInputStream(content);
    StreamUtil.copy(is, os);
    os.close();
    is.close();
  }

  public static void writeToLeaf(RepositoryNode leaf, InputStream contentStream)
      throws Exception {
    OutputStream os = leaf.getNewOutputStream();
    StreamUtil.copy(contentStream, os);
    os.close();
    contentStream.close();
  }

  public static String getLeafContent(RepositoryNodeVersion leaf)
      throws IOException {
    return getRNCContent(leaf.getNodeContents());
  }

  public static String getRNCContent(RepositoryNode.RepositoryNodeContents rnc)
      throws IOException {
    InputStream is = rnc.getInputStream();
    OutputStream baos = new ByteArrayOutputStream(20);
    StreamUtil.copy(is, baos);
    is.close();
    String resultStr = baos.toString();
    baos.close();
    return resultStr;
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestRepositoryNodeImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  // this class overrides 'getDatedVersionedPropsFile()' so I can
  // manipulate the file names for testing.  Also allows 'loadNodeProps()
  // to fail on demand
  static class MyMockRepositoryNode extends RepositoryNodeImpl {
    long dateValue;
    boolean failPropsLoad = false;
    boolean failRootConsist = false;
    boolean failContentConsist = false;
    boolean failEnsureCurrentLoaded = false;
    static boolean failEnsureDirExists = false;

    MyMockRepositoryNode(RepositoryNodeImpl nodeImpl) {
      super(nodeImpl.url, nodeImpl.nodeLocation, nodeImpl.repository);
    }

    File getDatedVersionedPropsFile(int version, long date) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(version);
      buffer.append(PROPS_EXTENSION);
      buffer.append("-");
      buffer.append(dateValue);
      return new File(getContentDir(), buffer.toString());
    }

    void loadNodeProps(boolean okIfNotThere) {
      if (failPropsLoad) {
        throw new LockssRepository.RepositoryStateException("Couldn't load properties file.");
      } else {
        super.loadNodeProps(okIfNotThere);
      }
    }

    boolean checkNodeRootConsistency() {
      if (failRootConsist) {
        return false;
      } else {
        return super.checkNodeRootConsistency();
      }
    }

    boolean checkContentConsistency() {
      if (failContentConsist) {
        return false;
      } else {
        return super.checkContentConsistency();
      }
    }

    void ensureCurrentInfoLoaded() {
      if (failEnsureCurrentLoaded) {
        throw new LockssRepository.RepositoryStateException("Couldn't load current info.");
      } else {
        super.ensureCurrentInfoLoaded();
      }
    }

    boolean ensureDirExists(File dirFile) {
      if (failEnsureDirExists) {
        return false;
      } else {
        return super.ensureDirExists(dirFile);
      }
    }
  }

  static class MyLockssRepositoryImpl extends LockssRepositoryImpl {
    boolean dontNormalize = false;
    void setDontNormalize(boolean val) {
      dontNormalize = val;
    }

    MyLockssRepositoryImpl(String rootPath) {
      super(rootPath);
    }

    public String canonicalizePath(String url)
	throws MalformedURLException {
      if (dontNormalize) return url;
      return super.canonicalizePath(url);
    }

    public static LockssRepository createNewLockssRepository(ArchivalUnit au) {
      String root = getRepositoryRoot(au);
      if (root == null) {
	throw new LockssRepository.RepositoryStateException("null root");
      }
      String auDir = LockssRepositoryImpl.mapAuToFileLocation(root, au);
      log.debug("repo: " + auDir + ", au: " + au.getName());
//       staticCacheLocation = extendCacheLocation(root);
      LockssRepositoryImpl repo = new MyLockssRepositoryImpl(auDir);
      Plugin plugin = au.getPlugin();
      if (plugin != null) {
	LockssDaemon daemon = plugin.getDaemon();
	if (daemon != null) {
	  RepositoryManager mgr = daemon.getRepositoryManager();
	  if (mgr != null) {
	    mgr.setRepositoryForPath(auDir, repo);
	  }
	}
      }
      return repo;
    }


  }
}
