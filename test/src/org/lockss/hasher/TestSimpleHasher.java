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
package org.lockss.hasher;

import java.util.*;
import java.io.*;
import java.security.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.AuCachedUrlSetSpec;
import org.lockss.filter.*;
import org.lockss.hasher.SimpleHasher.HasherStatus;
import org.lockss.hasher.SimpleHasher.HashType;
import org.lockss.hasher.SimpleHasher.ResultEncoding;
import org.lockss.metadata.TestMetadataManager.MySimulatedPlugin0;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.protocol.LcapMessage;
import org.lockss.repository.*;

public class TestSimpleHasher extends LockssTestCase {
  static final String HASH_ALG = "SHA-1";

  static final String BASE_URL = "http://www.test.com/blah/";

  static String[] urls = {
    BASE_URL,
    BASE_URL + "x.html",
    BASE_URL + "foo/",
    BASE_URL + "foo/1",
    BASE_URL + "foo/2",
    BASE_URL + "foo/2/a.txt",
    BASE_URL + "foo/2/b.txt",
    BASE_URL + "foo/2/c.txt",
    BASE_URL + "foo/2/d.txt",
    BASE_URL + "foo/3",
    BASE_URL + "foo/3/a.html",
    BASE_URL + "foo/3/b.html",
  };

  byte[] challenge = null;
  byte[] verifier = null;

  MockLockssDaemon daemon;
  String tempDirPath;
  MockArchivalUnit mau = null;
  MockAuState maus;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    tempDirPath = setUpDiskSpace();
    mau = new MockArchivalUnit(new MockPlugin(daemon), "maud");
    MockNodeManager nodeMgr = new MockNodeManager();
    daemon.setNodeManager(nodeMgr, mau);
    maus = new MockAuState(mau);
    nodeMgr.setAuState(maus);
    PluginTestUtil.registerArchivalUnit(mau);
  }

  MockArchivalUnit setupContentTree() {
    LockssRepositoryImpl repo =
      (LockssRepositoryImpl)LockssRepositoryImpl.createNewLockssRepository(
        mau);
    daemon.setLockssRepository(repo, mau);
    repo.initService(daemon);
    repo.startService();

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    List<CachedUrl> files = new ArrayList<CachedUrl>();
    for (int ix = 0; ix < urls.length; ix++) {
      String url = urls[ix];
      CachedUrl cu = mau.addUrl(url, false, true);
      files.add(cu);
      addContent(mau, url, org.apache.commons.lang3.StringUtils.repeat(url, ix));
    }
    cus.setHashItSource(files);
    return mau;
  }

  void addContent(MockArchivalUnit mau, String url, String content) {
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(url);
    cu.setContent(content);
  }
  
  public MessageDigest getMessageDigest(String alg) throws Exception {
    return MessageDigest.getInstance(alg);
  }

  String exp =
    "# comment 17\n" +
    "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709   http://www.test.com/blah/\n" +
    "CA44A2EE70C871B73DAAB1ABD23CE8EDF6CDBA33   http://www.test.com/blah/x.html\n" +
    "1018215CC5D8B604B3238F1E08141145D536BDFC   http://www.test.com/blah/foo/\n" +
    "DF88B88A0DCC1CBE4157790E24E903E653105F4F   http://www.test.com/blah/foo/1\n" +
    "621A46465EAF1ED35DCB88CD8F6ED39471A210F7   http://www.test.com/blah/foo/2\n" +
    "572F349D47A3C67294B0BEFB88014177275720E3   http://www.test.com/blah/foo/2/a.txt\n" +
    "5960969EEDB86FC43C7AE8A5A9AA0049B4AF216C   http://www.test.com/blah/foo/2/b.txt\n" +
    "0124C19FB7F0ECFEB572B00EE3C06F453F74CFA4   http://www.test.com/blah/foo/2/c.txt\n" +
    "57BDD3F19D10A89A1682C5FEF8318345CA44D2A0   http://www.test.com/blah/foo/2/d.txt\n" +
    "A014C62B813FD27AB76840AC0372310C5E5E5A13   http://www.test.com/blah/foo/3\n" +
    "C74D7B027A31CE9F885ACAD106D4320438B2B69B   http://www.test.com/blah/foo/3/a.html\n" +
    "40935FE5E3C5282F6D06B5ACEDBD909B12DE3FC0   http://www.test.com/blah/foo/3/b.html\n";

  String exp64 =
    "# comment 17\n" +
    "2jmj7l5rSw0yVb/vlWAYkK/YBwk=   http://www.test.com/blah/\n" +
    "ykSi7nDIcbc9qrGr0jzo7fbNujM=   http://www.test.com/blah/x.html\n" +
    "EBghXMXYtgSzI48eCBQRRdU2vfw=   http://www.test.com/blah/foo/\n" +
    "34i4ig3MHL5BV3kOJOkD5lMQX08=   http://www.test.com/blah/foo/1\n" +
    "YhpGRl6vHtNdy4jNj27TlHGiEPc=   http://www.test.com/blah/foo/2\n" +
    "Vy80nUejxnKUsL77iAFBdydXIOM=   http://www.test.com/blah/foo/2/a.txt\n" +
    "WWCWnu24b8Q8euilqaoASbSvIWw=   http://www.test.com/blah/foo/2/b.txt\n" +
    "ASTBn7fw7P61crAO48BvRT90z6Q=   http://www.test.com/blah/foo/2/c.txt\n" +
    "V73T8Z0QqJoWgsX++DGDRcpE0qA=   http://www.test.com/blah/foo/2/d.txt\n" +
    "oBTGK4E/0nq3aECsA3IxDF5eWhM=   http://www.test.com/blah/foo/3\n" +
    "x017Anoxzp+IWsrRBtQyBDiytps=   http://www.test.com/blah/foo/3/a.html\n" +
    "QJNf5ePFKC9tBrWs7b2QmxLeP8A=   http://www.test.com/blah/foo/3/b.html\n" +
    "#end\n";

  public void testV3() throws Exception {
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new SimpleFilterFactory());
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17", null);
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(exp, StringUtil.fromFile(blockFile));
  }

  public void testV364() throws Exception {
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new SimpleFilterFactory());
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    hasher.setBase64Result(true);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17", "#end");
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(exp64, StringUtil.fromFile(blockFile));
  }

  String expFilt =
    "# comment 17\n" +
    "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709   http://www.test.com/blah/\n" +
    "CA44A2EE70C871B73DAAB1ABD23CE8EDF6CDBA33   http://www.test.com/blah/x.html\n" +
    "325A92DEB64D764F4C80B30B6EBCA1DEA68773BB   http://www.test.com/blah/foo/\n" +
    "8F61702A9CDB0CF54C4954C14A455EB147E3D340   http://www.test.com/blah/foo/1\n" +
    "69EB01F0B9C1A6DC58C05ADC62C077985C5A27D7   http://www.test.com/blah/foo/2\n" +
    "136B317433070E1355B77BBF8C0D5BC64E100E43   http://www.test.com/blah/foo/2/a.txt\n" +
    "A89EA485278644EB3E11D3C1F3974DC95E1A2F0F   http://www.test.com/blah/foo/2/b.txt\n" +
    "F69DA3BFB2B49D8B391D76A1793BFC7C15F238C2   http://www.test.com/blah/foo/2/c.txt\n" +
    "F92DBF9FE11A9AB2BC2DD670AA82EC1E75B4B682   http://www.test.com/blah/foo/2/d.txt\n" +
    "442ACC24CEADC9EA9D038B71ABDDFFC50F3C6018   http://www.test.com/blah/foo/3\n" +
    "B39CA4E43E4C2D9624A83D6D07891A18F804AEE0   http://www.test.com/blah/foo/3/a.html\n" +
    "62E6BEFE0C9A37D1124F0D4A5CC64614B0396C1C   http://www.test.com/blah/foo/3/b.html\n";

  String expFilt64 =
    "# comment 17\n" +
    "2jmj7l5rSw0yVb/vlWAYkK/YBwk=   http://www.test.com/blah/\n" +
    "ykSi7nDIcbc9qrGr0jzo7fbNujM=   http://www.test.com/blah/x.html\n" +
    "MlqS3rZNdk9MgLMLbryh3qaHc7s=   http://www.test.com/blah/foo/\n" +
    "j2FwKpzbDPVMSVTBSkVesUfj00A=   http://www.test.com/blah/foo/1\n" +
    "aesB8LnBptxYwFrcYsB3mFxaJ9c=   http://www.test.com/blah/foo/2\n" +
    "E2sxdDMHDhNVt3u/jA1bxk4QDkM=   http://www.test.com/blah/foo/2/a.txt\n" +
    "qJ6khSeGROs+EdPB85dNyV4aLw8=   http://www.test.com/blah/foo/2/b.txt\n" +
    "9p2jv7K0nYs5HXaheTv8fBXyOMI=   http://www.test.com/blah/foo/2/c.txt\n" +
    "+S2/n+EamrK8LdZwqoLsHnW0toI=   http://www.test.com/blah/foo/2/d.txt\n" +
    "RCrMJM6tyeqdA4txq93/xQ88YBg=   http://www.test.com/blah/foo/3\n" +
    "s5yk5D5MLZYkqD1tB4kaGPgEruA=   http://www.test.com/blah/foo/3/a.html\n" +
    "Yua+/gyaN9ESTw1KXMZGFLA5bBw=   http://www.test.com/blah/foo/3/b.html\n";

  public void testV3Filtered() throws Exception {
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new SimpleFilterFactory());
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    hasher.setFiltered(true);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17", null);
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(expFilt, StringUtil.fromFile(blockFile));
  }

  public void testV3Filtered64() throws Exception {
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new SimpleFilterFactory());
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    hasher.setFiltered(true);
    hasher.setBase64Result(true);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17", null);
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(expFilt64, StringUtil.fromFile(blockFile));
  }

  public class SimpleFilterFactory implements FilterFactory {
    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream in,
						 String encoding) {
      log.info("createFilteredInputStream");
      Reader rdr = FilterUtil.getReader(in, encoding);
      StringFilter filt = new StringFilter(rdr, "foo", "bar");
      filt.setIgnoreCase(true);
      return new ReaderInputStream(filt);
    }
  }

  public void testProcessHashTypeParam() throws Exception {
    HasherParams params = new HasherParams("thisMachine", false);
    SimpleHasher hasher = new SimpleHasher(null);
    HasherResult result = new HasherResult();
    String errorMessage = hasher.processHashTypeParam(params, result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(result.getRunnerError());
    assertNull(errorMessage);
    assertNull(params.getHashType());
    assertEquals(SimpleHasher.DEFAULT_HASH_TYPE, result.getHashType());

    params.setHashType("V1Content");
    result = new HasherResult();
    errorMessage = hasher.processHashTypeParam(params, result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(result.getRunnerError());
    assertNull(errorMessage);
    assertEquals("V1Content", params.getHashType());
    assertEquals(HashType.V1Content, result.getHashType());

    params.setHashType("WrongType");
    result = new HasherResult();
    errorMessage = hasher.processHashTypeParam(params, result);
    assertEquals(HasherStatus.Error, result.getRunnerStatus());
    assertTrue(result.getRunnerError()
	.startsWith("Unknown hash type: WrongType - No enum const"));
    assertEquals(result.getRunnerError(), errorMessage);
    assertEquals("WrongType", params.getHashType());
    assertNull(result.getHashType());

    params.setHashType("5");
    result = new HasherResult();
    errorMessage = hasher.processHashTypeParam(params, result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(result.getRunnerError());
    assertNull(errorMessage);
    assertEquals("V3File", params.getHashType());
    assertEquals(HashType.V3File, result.getHashType());

    params.setHashType("6");
    result = new HasherResult();
    errorMessage = hasher.processHashTypeParam(params, result);
    assertEquals(HasherStatus.Error, result.getRunnerStatus());
    assertEquals("Unknown hash type: 6", result.getRunnerError());
    assertEquals(result.getRunnerError(), errorMessage);
    assertEquals("6", params.getHashType());
    assertNull(result.getHashType());
  }

  public void testProcessResultEncodingParam() throws Exception {
    HasherParams params = new HasherParams("thisMachine", false);
    SimpleHasher hasher = new SimpleHasher(null);
    HasherResult result = new HasherResult();
    String errorMessage = hasher.processResultEncodingParam(params, result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(result.getRunnerError());
    assertNull(errorMessage);
    assertNull(params.getResultEncoding());
    assertEquals(SimpleHasher.DEFAULT_RESULT_ENCODING,
	result.getResultEncoding());

    params.setResultEncoding("Base64");
    result = new HasherResult();
    errorMessage = hasher.processResultEncodingParam(params, result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(result.getRunnerError());
    assertNull(errorMessage);
    assertEquals("Base64", params.getResultEncoding());
    assertEquals(ResultEncoding.Base64, result.getResultEncoding());

    params.setResultEncoding("WrongEncoding");
    result = new HasherResult();
    errorMessage = hasher.processResultEncodingParam(params, result);
    assertEquals(HasherStatus.Error, result.getRunnerStatus());
    assertTrue(result.getRunnerError()
	.startsWith("Unknown result encoding: WrongEncoding - No enum const"));
    assertEquals(result.getRunnerError(), errorMessage);
    assertEquals("WrongEncoding", params.getResultEncoding());
    assertNull(result.getResultEncoding());
  }

  public void testOOMESetsResult() throws Exception {
    HasherParams params = new HasherParams("foo", false);
    SimpleHasher hasher = new SimpleHasher(null);
    HasherResult result = new HasherResult();
    params.setAuId(mau.getAuId());
    mau.addUrl("http://foo.bar/", "cont");
    mau.populateAuCachedUrlSet();
    maus.setSuppressRecomputeNumCurrentSuspectVersions(true);
    hasher.hash(params, result);
    assertEquals(HasherStatus.Done, result.getRunnerStatus());

    MockFilterFactory mfilt = new MockFilterFactory();
    ThrowingInputStream tis =
      new ThrowingInputStream(new StringInputStream("foo"),
			      null /* new IOException("foobar")*/,
			      null);
    tis.setErrorOnRead(new OutOfMemoryError("Test OOME"));
    mfilt.setFilteredInputStream(tis);
    mau.setHashFilterFactory(mfilt);
    mau.populateAuCachedUrlSet();
    result = new HasherResult();
    hasher = new SimpleHasher(null);
    try {
      hasher.hash(params, result);
      fail("Error didn't abort hash");
    } catch (OutOfMemoryError e) {
      assertEquals(HasherStatus.Error, result.getRunnerStatus());
      assertEquals("Error hashing: Test OOME", result.getRunnerError());
    } // any other error causes failure

    tis.setErrorOnRead(null);
    tis.setThrowOnRead(new IOException("Test IOException"));
    result = new HasherResult();
    hasher = new SimpleHasher(null);
    hasher.hash(params, result);
    assertEquals(HasherStatus.Done, result.getRunnerStatus());
    assertNull(result.getRunnerError());
  }

  public void testProcessParams() throws Exception {
    HasherParams params = new HasherParams("thisMachine", false);
    assertEquals("thisMachine", params.getMachineName());
    assertFalse(params.isAsynchronous());
    params = new HasherParams("thisMachine", true);
    assertTrue(params.isAsynchronous());

    SimpleHasher hasher = new SimpleHasher(null);
    HasherResult result = new HasherResult();
    result.setHashType(SimpleHasher.DEFAULT_HASH_TYPE);

    String errorMessage = hasher.processParams(params, result);
    assertEquals(HasherStatus.Error, result.getRunnerStatus());
    assertEquals("No AU identifer has been specified", result.getRunnerError());
    assertEquals("Select an AU", errorMessage);

    params.setAuId("NoSuchAu");
    result = new HasherResult();
    result.setHashType(SimpleHasher.DEFAULT_HASH_TYPE);

    errorMessage = hasher.processParams(params, result);
    assertEquals(HasherStatus.Error, result.getRunnerStatus());
    assertEquals("No AU exists with the specified identifier NoSuchAu",
	result.getRunnerError());
    assertEquals("No such AU.  Select an AU", errorMessage);

    params.setAuId(createAndStartAu().getAuId());
    result = new HasherResult();
    result.setHashType(SimpleHasher.DEFAULT_HASH_TYPE);

    errorMessage = hasher.processParams(params, result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(result.getRunnerError());
    assertNull(errorMessage);
    // Clean up the result file.
    result.getBlockFile().delete();

    assertEquals(LcapMessage.getDefaultHashAlgorithm(), params.getAlgorithm());
  }

  private ArchivalUnit createAndStartAu() throws Exception {
    daemon.getAlertManager();
    PluginManager pluginManager = daemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    daemon.getCrawlManager();

    SimulatedArchivalUnit sau = PluginTestUtil
	.createAndStartSimAu(MySimulatedPlugin0.class,
	    simAuConfig(tempDirPath + "/0"));
    PluginTestUtil.crawlSimAu(sau);
    daemon.setAusStarted(true);

    return sau;
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
                                SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  public void testIsV3() throws Exception {
    assertFalse(SimpleHasher.isV3(HashType.V1Content));
    assertFalse(SimpleHasher.isV3(HashType.V1Name));
    assertFalse(SimpleHasher.isV3(HashType.V1File));
    assertTrue(SimpleHasher.isV3(HashType.V3Tree));
    assertTrue(SimpleHasher.isV3(HashType.V3File));
  }

  public void testDecodeBase64Value() throws Exception {
    SimpleHasher hasher = new SimpleHasher(null);
    assertNull(hasher.decodeBase64Value(null));
    byte[] expected1 = {};
    assertEquals(expected1, hasher.decodeBase64Value(""));

    try {
      hasher.decodeBase64Value(" ");
      fail("decodeBase64Value(\" \") should throw IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // Expected.
    }

    byte[] expected2 = {-38, 57, -93, -18, 94, 107, 75, 13, 50, 85, -65, -17};
    assertEquals(expected2, hasher.decodeBase64Value("2jmj7l5rSw0yVb/v"));
  }

  public void testCheckCus() throws Exception {
    HasherResult result = new HasherResult();
    assertNull(result.getCus());
    SimpleHasher hasher = new SimpleHasher(null);

    ArchivalUnit au = createAndStartAu();
    String errorMessage = hasher.processCus(au.getAuId(), AuCachedUrlSetSpec.URL,
	null, null, HashType.V3Tree, result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(errorMessage);
    assertNull(result.getRunnerError());
    CachedUrlSet cus = result.getCus();
    assertNotNull(cus);
    assertTrue(isUrlInCus("http://www.example.com/branch1/branch1/003file.pdf",
	cus));
    assertFalse(isUrlInCus("http://totally.fake.url", cus));

    errorMessage = hasher.processCus(au.getAuId(), AuCachedUrlSetSpec.URL, null,
	null, HashType.V3Tree, result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(errorMessage);
    assertNull(result.getRunnerError());
    cus = result.getCus();
    assertNotNull(cus);
    assertTrue(isUrlInCus("http://www.example.com/index.html", cus));
    assertFalse(isUrlInCus("http://www.example.com/branch2/index.html", cus));
  }

  private boolean isUrlInCus(String url, CachedUrlSet cus) {
    if (url == null || cus == null) {
      return false;
    }

    for (CachedUrl cu : cus.getCuIterable()) {
      if (url.equals(cu.getUrl())) {
	return true;
      }
    }

    return false;
  }

  public void testMakeDigest() throws Exception {
    HasherResult result = new HasherResult();
    MessageDigest digest = new SimpleHasher(null).makeDigestAndRecordStream(
	LcapMessage.getDefaultHashAlgorithm(), false, result);

    assertTrue(digest instanceof MessageDigest);
    assertFalse(digest instanceof RecordingMessageDigest);
    assertEquals(LcapMessage.getDefaultHashAlgorithm(), digest.getAlgorithm());
    assertEquals(20, digest.getDigestLength());
    assertNull(result.getRecordFile());
    assertNull(result.getRecordStream());

    result = new HasherResult();
    digest = new SimpleHasher(null).makeDigestAndRecordStream(
	LcapMessage.getDefaultHashAlgorithm(), true, result);

    assertTrue(digest instanceof MessageDigest);
    assertTrue(digest instanceof RecordingMessageDigest);
    assertEquals("SHA-1", digest.getAlgorithm());
    assertEquals(20, digest.getDigestLength());
    assertTrue(result.getRecordFile().getName().startsWith("HashCUS"));
    assertTrue(result.getRecordFile().getName().endsWith(".tmp"));
    assertNotNull(result.getRecordStream());
    // Clean up the result file.
    result.getRecordFile().delete();
    IOUtil.safeClose(result.getRecordStream());

    result = new HasherResult();
    digest =
	new SimpleHasher(null).makeDigestAndRecordStream("SHA", false, result);

    assertTrue(digest instanceof MessageDigest);
    assertFalse(digest instanceof RecordingMessageDigest);
    assertEquals("SHA", digest.getAlgorithm());
    assertEquals(20, digest.getDigestLength());
    assertNull(result.getRecordFile());
    assertNull(result.getRecordStream());

    result = new HasherResult();
    digest =
	new SimpleHasher(null).makeDigestAndRecordStream("SHA1", false, result);

    assertTrue(digest instanceof MessageDigest);
    assertFalse(digest instanceof RecordingMessageDigest);
    assertEquals("SHA1", digest.getAlgorithm());
    assertEquals(20, digest.getDigestLength());
    assertNull(result.getRecordFile());
    assertNull(result.getRecordStream());

    result = new HasherResult();
    digest =
	new SimpleHasher(null).makeDigestAndRecordStream("MD5", false, result);

    assertTrue(digest instanceof MessageDigest);
    assertFalse(digest instanceof RecordingMessageDigest);
    assertEquals("MD5", digest.getAlgorithm());
    assertEquals(16, digest.getDigestLength());
    assertNull(result.getRecordFile());
    assertNull(result.getRecordStream());

    result = new HasherResult();
    digest = new SimpleHasher(null).makeDigestAndRecordStream("SHA-256", false,
	result);

    assertTrue(digest instanceof MessageDigest);
    assertFalse(digest instanceof RecordingMessageDigest);
    assertEquals("SHA-256", digest.getAlgorithm());
    assertEquals(32, digest.getDigestLength());
    assertNull(result.getRecordFile());
    assertNull(result.getRecordStream());

    try {
      digest =
	  new SimpleHasher(null).makeDigestAndRecordStream(null, false, result);
      fail("Null algorithm should throw NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      digest = new SimpleHasher(null).makeDigestAndRecordStream("SHA256", false,
	  result);
      fail("Invalid algorithm should throw NoSuchAlgorithmException");
    } catch (NoSuchAlgorithmException nsae) {
      // Expected.
    }

    try {
      digest = new SimpleHasher(null).makeDigestAndRecordStream("FGL", false,
	  result);
      fail("Invalid algorithm should throw NoSuchAlgorithmException");
    } catch (NoSuchAlgorithmException nsae) {
      // Expected.
    }
  }

  public void testDoV1() throws Exception {
    ArchivalUnit au = createAndStartAu();

    runTestDoV1(au, LcapMessage.getDefaultHashAlgorithm(), null, null, null,
	"27831E7E869AF658024E1049DC1D1CF6C491F094");

    runTestDoV1(au, LcapMessage.getDefaultHashAlgorithm(), null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"578C716600A207EFC63FBFAE2DF331397A42F85D");

    runTestDoV1(au, "SHA-1", null, null, null,
	"27831E7E869AF658024E1049DC1D1CF6C491F094");

    runTestDoV1(au, "SHA-1", null, "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "578C716600A207EFC63FBFAE2DF331397A42F85D");

    runTestDoV1(au, "MD5", null, null, null,
	"F16B6C9E4F022BF78350CCB09CE7BF35");

    runTestDoV1(au, "MD5", null, "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "44C7B7CD4E10F20A3A265FDFF66FEF09");

    runTestDoV1(au, "SHA-256", null, null, null,
	"4D5BA4B4AD376937A9EB987C1636E7C5175D3CE266FDEBE433BF319771B7F0C9");

    runTestDoV1(au, "SHA-256", null, "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"192B024B18D764E0A2B83624DC6A5D5307167C5363030B96C6D30F3E210E785D");

    runTestDoV1(au, LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/003file.pdf", null, null,
	"B3C403B58B84B18CB1D4FD34F691BD894AABB7CA");

    runTestDoV1(au, LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/003file.pdf", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "01526544C35351819EEE11B736488B2829B7F835");

    runTestDoV1(au, "SHA-1", "http://www.example.com/003file.pdf", null, null,
	"B3C403B58B84B18CB1D4FD34F691BD894AABB7CA");

    runTestDoV1(au, "SHA-1", "http://www.example.com/003file.pdf",
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"01526544C35351819EEE11B736488B2829B7F835");

    runTestDoV1(au, "MD5", "http://www.example.com/003file.pdf", null, null,
	"2D4284FB95A5368966C2EF640FE6083E");

    runTestDoV1(au, "MD5", "http://www.example.com/003file.pdf",
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"A42495DF6A71E0EEE72AEA5D671CF4A9");

    runTestDoV1(au, "SHA-256", "http://www.example.com/003file.pdf", null, null,
	"18A1E3B2C4532F3B3CA2A76A12BA2587D91E73FC3F5049DAF0293C137C5D4EFB");

    runTestDoV1(au, "SHA-256", "http://www.example.com/003file.pdf",
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"1BDA8A60D6DD16970DEF4A4AB9B46324D64A79D4E6956B8C00ABD6ABCA2308D3");
  }

  private void runTestDoV1(ArchivalUnit au, String digestType, String url,
      String challenge, String verifier, String expectedHash) throws Exception {
    HasherParams params = new HasherParams("thisMachine", false);
    HasherResult result = new HasherResult();
    MessageDigest digest = new SimpleHasher(null).makeDigestAndRecordStream(
	digestType, false, result);
    SimpleHasher hasher = new SimpleHasher(digest);
    hasher.processHashTypeParam(params, result);
    params.setAuId(au.getAuId());
    params.setUrl(url);
    params.setChallenge(challenge);
    params.setVerifier(verifier);
    hasher.processParams(params, result);

    hasher.doV1(result.getCus().getContentHasher(digest), result);
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(result.getRunnerError());
    assertTrue(result.isShowResult());
    assertEquals(expectedHash, ByteArray.toHexString(result.getHashResult()));
    if (StringUtil.isNullString(url)) {
      assertEquals(4796, hasher.getBytesHashed());
    } else {
      assertEquals(36, hasher.getBytesHashed());
    }
    assertEquals(0, hasher.getFilesHashed());
    File blockFile = result.getBlockFile();
    assertNotNull(blockFile);
    assertTrue(blockFile.exists());
    assertEquals(0, blockFile.length());
    // Clean up the result file.
    blockFile.delete();
  }

  public void testDoV3() throws Exception {
    ArchivalUnit au = createAndStartAu();

    runTestDoV3(au, null, "Hex", LcapMessage.getDefaultHashAlgorithm(), null,
	null, null, null, null,
	"21BDE2E4107D2BE49DE1DAD4B32335E8312858B2   http://www.example.com/index.html");

    runTestDoV3(au, null, "Hex", LcapMessage.getDefaultHashAlgorithm(), null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"1069ED623BFA4876DEFC3BA45CCEBF53E3A7676A   http://www.example.com/index.html");

    runTestDoV3(au, null, "Base64", LcapMessage.getDefaultHashAlgorithm(), null,
	null, null, null, null,
	"Ib3i5BB9K+Sd4drUsyM16DEoWLI=   http://www.example.com/index.html");

    runTestDoV3(au, null, "Base64", LcapMessage.getDefaultHashAlgorithm(), null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"EGntYjv6SHbe/DukXM6/U+OnZ2o=   http://www.example.com/index.html");

    runTestDoV3(au, null, "Hex", "SHA-1", null, null, null, null, null,
	"21BDE2E4107D2BE49DE1DAD4B32335E8312858B2   http://www.example.com/index.html");

    runTestDoV3(au, null, "Hex", "SHA-1", null, "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"1069ED623BFA4876DEFC3BA45CCEBF53E3A7676A   http://www.example.com/index.html");

    runTestDoV3(au, null, "Base64", "SHA-1", null, null, null, null, null,
	"Ib3i5BB9K+Sd4drUsyM16DEoWLI=   http://www.example.com/index.html");

    runTestDoV3(au, null, "Base64", "SHA-1", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"EGntYjv6SHbe/DukXM6/U+OnZ2o=   http://www.example.com/index.html");

    runTestDoV3(au, null, "Hex", "MD5", null, null, null, null, null,
	"CA3298E2378E427CD7100E374C9F3A13   http://www.example.com/index.html");

    runTestDoV3(au, null, "Hex", "MD5", null, "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"973F97A3E8DF0832ECF3AB4F302C559B   http://www.example.com/index.html");

    runTestDoV3(au, null, "Base64", "MD5", null, null, null, null, null,
	"yjKY4jeOQnzXEA43TJ86Ew==   http://www.example.com/index.html");

    runTestDoV3(au, null, "Base64", "MD5", null, "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"lz+Xo+jfCDLs86tPMCxVmw==   http://www.example.com/index.html");

    runTestDoV3(au, null, "Hex", "SHA-256", null, null, null, null, null,
	"459F9DF30517AC989A814ED51CEBE98EEF209E32B850C155390908AE23A13E1F   http://www.example.com/index.html");

    runTestDoV3(au, null, "Hex", "SHA-256", null, "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"7BF270607FBA5E79A47FF7D2B35FF7AC83755C6F9DAEC338CBF27EC5695C8B80   http://www.example.com/index.html");

    runTestDoV3(au, null, "Base64", "SHA-256", null, null, null, null, null,
	"RZ+d8wUXrJiagU7VHOvpju8gnjK4UMFVOQkIriOhPh8=   http://www.example.com/index.html");

    runTestDoV3(au, null, "Base64", "SHA-256", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"e/JwYH+6Xnmkf/fSs1/3rIN1XG+drsM4y/J+xWlci4A=   http://www.example.com/index.html");

    runTestDoV3(au, null, "Hex", LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/003file.pdf", null, null, null, null,
	"DA39A3EE5E6B4B0D3255BFEF95601890AFD80709   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Hex", LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/003file.pdf", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"C57147E1B8AC535B0E1FF5B5BA7EB83CA02D1577   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Base64", LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/003file.pdf", null, null, null, null,
	"2jmj7l5rSw0yVb/vlWAYkK/YBwk=   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Base64", LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/003file.pdf", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"xXFH4bisU1sOH/W1un64PKAtFXc=   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Hex", "SHA-1", "http://www.example.com/003file.pdf",
	null, null, null, null,
	"DA39A3EE5E6B4B0D3255BFEF95601890AFD80709   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Hex", "SHA-1", null, "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"C57147E1B8AC535B0E1FF5B5BA7EB83CA02D1577   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Base64", "SHA-1",
	"http://www.example.com/003file.pdf", null, null, null, null,
	"2jmj7l5rSw0yVb/vlWAYkK/YBwk=   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Base64", "SHA-1",
	"http://www.example.com/003file.pdf", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"xXFH4bisU1sOH/W1un64PKAtFXc=   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Hex", "MD5", "http://www.example.com/003file.pdf",
	null, null, null, null,
	"D41D8CD98F00B204E9800998ECF8427E   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Hex", "MD5", "http://www.example.com/003file.pdf",
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"5B7B2BAB276BE566DFD27106DFD49E39   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Base64", "MD5", "http://www.example.com/003file.pdf",
	null, null, null, null,
	"1B2M2Y8AsgTpgAmY7PhCfg==   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Base64", "MD5", "http://www.example.com/003file.pdf",
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"W3srqydr5Wbf0nEG39SeOQ==   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Hex", "SHA-256",
	"http://www.example.com/003file.pdf", null, null, null, null,
	"E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Hex", "SHA-256",
	"http://www.example.com/003file.pdf", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"9C2FCB809A0F9B4253A3142C8F532E8633E6F7FE77454420296D79A9CA34E265   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Base64", "SHA-256",
	"http://www.example.com/003file.pdf", null, null, null, null,
	"47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=   http://www.example.com/003file.pdf");

    runTestDoV3(au, null, "Base64", "SHA-256",
	"http://www.example.com/003file.pdf", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"nC/LgJoPm0JToxQsj1MuhjPm9/53RUQgKW15qco04mU=   http://www.example.com/003file.pdf");
  }

  public void testDoV3File() throws Exception {
    ArchivalUnit au = createAndStartAu();

    runTestDoV3(au, "V3File", "Hex", LcapMessage.getDefaultHashAlgorithm(),
	null, null, null, null, null,
	"21BDE2E4107D2BE49DE1DAD4B32335E8312858B2   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Hex", LcapMessage.getDefaultHashAlgorithm(),
	null, "RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"1069ED623BFA4876DEFC3BA45CCEBF53E3A7676A   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Base64", LcapMessage.getDefaultHashAlgorithm(),
	null, null, null, null, null,
	"Ib3i5BB9K+Sd4drUsyM16DEoWLI=   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Base64", LcapMessage.getDefaultHashAlgorithm(),
	null, "RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"EGntYjv6SHbe/DukXM6/U+OnZ2o=   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Hex", "SHA-1", null, null, null, null, null,
	"21BDE2E4107D2BE49DE1DAD4B32335E8312858B2   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Hex", "SHA-1", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"1069ED623BFA4876DEFC3BA45CCEBF53E3A7676A   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Base64", "SHA-1", null, null, null, null, null,
	"Ib3i5BB9K+Sd4drUsyM16DEoWLI=   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Base64", "SHA-1", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"EGntYjv6SHbe/DukXM6/U+OnZ2o=   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Hex", "MD5", null, null, null, null, null,
	"CA3298E2378E427CD7100E374C9F3A13   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Hex", "MD5", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"973F97A3E8DF0832ECF3AB4F302C559B   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Base64", "MD5", null, null, null, null, null,
	"yjKY4jeOQnzXEA43TJ86Ew==   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Base64", "MD5", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"lz+Xo+jfCDLs86tPMCxVmw==   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Hex", "SHA-256", null, null, null, null, null,
	"459F9DF30517AC989A814ED51CEBE98EEF209E32B850C155390908AE23A13E1F   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Hex", "SHA-256", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"7BF270607FBA5E79A47FF7D2B35FF7AC83755C6F9DAEC338CBF27EC5695C8B80   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Base64", "SHA-256", null, null, null, null, null,
	"RZ+d8wUXrJiagU7VHOvpju8gnjK4UMFVOQkIriOhPh8=   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Base64", "SHA-256", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K",
	"e/JwYH+6Xnmkf/fSs1/3rIN1XG+drsM4y/J+xWlci4A=   http://www.example.com/index.html");

    runTestDoV3(au, "V3File", "Hex", LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/branch1/", null, null, null, null,
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Hex", LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/branch1/", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Base64", LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/branch1/", null, null, null, null,
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Base64", LcapMessage.getDefaultHashAlgorithm(),
	"http://www.example.com/branch1/", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K", "http://www.example.com");

    runTestDoV3(au, "V3File", "Hex", "SHA-1",
	"http://www.example.com/branch1/", null, null, null, null,
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Hex", "SHA-1", null,
	"RmVybmFuZG8gRy4gTG95Z29ycmkK", "TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Base64", "SHA-1",
	"http://www.example.com/branch1/", null, null, null, null,
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Base64", "SHA-1",
	"http://www.example.com/branch1/", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K", "http://www.example.com");

    runTestDoV3(au, "V3File", "Hex",
	"MD5", "http://www.example.com/branch1/", null, null, null, null,
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Hex", "MD5",
	"http://www.example.com/branch1/", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Base64", "MD5",
	"http://www.example.com/branch1/", null, null, null, null,
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Base64", "MD5",
	"http://www.example.com/branch1/", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K", "http://www.example.com");

    runTestDoV3(au, "V3File", "Hex", "SHA-256",
	"http://www.example.com/branch1/", null, null, null, null,
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Hex", "SHA-256",
	"http://www.example.com/branch1/", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K",
	"# Poller nonce: 4665726E616E646F20472E204C6F79676F7272690A",
	"# Voter nonce: 4D7964756E6720542E205472616E0A",
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Base64", "SHA-256",
	"http://www.example.com/branch1/", null, null, null, null,
	"http://www.example.com");

    runTestDoV3(au, "V3File", "Base64", "SHA-256",
	"http://www.example.com/branch1/", "RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"TXlkdW5nIFQuIFRyYW4K", "# Poller nonce: RmVybmFuZG8gRy4gTG95Z29ycmkK",
	"# Voter nonce: TXlkdW5nIFQuIFRyYW4K", "http://www.example.com");
  }

  private void runTestDoV3(ArchivalUnit au, String hashType,
      String resultEncoding, String digestType, String url, String challenge,
      String verifier, String expectedPollerNonceLine,
      String expectedVoterNonceLine, String expectedHashLine) throws Exception {
    HasherParams params = new HasherParams("thisMachine", false);
    HasherResult result = new HasherResult();
    MessageDigest digest = new SimpleHasher(null).makeDigestAndRecordStream(
	digestType, false, result);
    SimpleHasher hasher = new SimpleHasher(digest);
    params.setHashType(hashType);
    hasher.processHashTypeParam(params, result);
    params.setResultEncoding(resultEncoding);
    hasher.processResultEncodingParam(params, result);
    params.setAuId(au.getAuId());
    params.setUrl(url);
    params.setChallenge(challenge);
    params.setVerifier(verifier);
    hasher.processParams(params, result);

    hasher.doV3("thisMachine", false, result);
    if (hashType == null) {
      assertEquals(SimpleHasher.DEFAULT_HASH_TYPE, result.getHashType());
    } else {
      assertEquals(hashType, result.getHashType().toString());
    }
    assertEquals(HasherStatus.NotStarted, result.getRunnerStatus());
    assertNull(result.getRunnerError());
    assertTrue(result.isShowResult());
    assertNull(result.getHashResult());
    if (StringUtil.isNullString(url)) {
      assertEquals(3751, hasher.getBytesHashed());
      assertEquals(21, hasher.getFilesHashed());
    } else {
      assertEquals(0, hasher.getBytesHashed());
      if (hashType == null || "V3Tree".equals(hashType)) {
	assertEquals(1, hasher.getFilesHashed());
      } else {
	assertEquals(0, hasher.getFilesHashed());
      }
    }
    File blockFile = result.getBlockFile();
    assertNotNull(blockFile);
    assertTrue(blockFile.exists());
    String fileText = StringUtil.fromFile(blockFile);
    assertTrue(fileText.contains("# Hash algorithm: " + digestType));
    assertTrue(fileText.contains("# Encoding: " + resultEncoding));
    if (!StringUtil.isNullString(challenge)) {
      assertTrue(fileText.contains(expectedPollerNonceLine));
    }
    if (!StringUtil.isNullString(verifier)) {
      assertTrue(fileText.contains(expectedVoterNonceLine));
    }
    if (hashType == null || "V3Tree".equals(hashType) || url == null) {
      assertTrue(fileText.contains(expectedHashLine));
    } else {
      assertFalse(fileText.contains(expectedHashLine));
    }
    // Clean up the result file.
    blockFile.delete();
    }

  public void testFormatDateTime() throws Exception {
    runTestFormatDateTime(0L, "00:00:00 01/01/70");
    runTestFormatDateTime(1000000000L, "13:46:40 01/12/70");
    runTestFormatDateTime(100000000000L, "09:46:40 03/03/73");
    runTestFormatDateTime(1000000000000L, "01:46:40 09/09/01");
  }

  private void runTestFormatDateTime(long timestamp, String expected) {
    TimeBase.setSimulated(timestamp
	- TimeZone.getDefault().getOffset(timestamp));
    assertEquals(expected, SimpleHasher.formatDateTime(TimeBase.nowMs()));
  }

  public void testByteString() throws Exception {
    byte[] a = {};
    assertEquals("", SimpleHasher.byteString(a, ResultEncoding.Base64));
    assertEquals("", SimpleHasher.byteString(a, ResultEncoding.Hex));
    byte[] b = {1, 2, 3};
    assertEquals("AQID", SimpleHasher.byteString(b, ResultEncoding.Base64));
    assertEquals("010203", SimpleHasher.byteString(b, ResultEncoding.Hex));
    byte[] c = {99, 88, 77, 66, 55, 44, 33, 22, 11};
    assertEquals("Y1hNQjcsIRYL",
	SimpleHasher.byteString(c, ResultEncoding.Base64));
    assertEquals("63584D42372C21160B",
	SimpleHasher.byteString(c, ResultEncoding.Hex));
  }
}
