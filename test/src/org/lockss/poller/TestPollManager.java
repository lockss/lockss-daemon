package org.lockss.poller;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.mortbay.util.*;
import gnu.regexp.*;
import junit.framework.TestCase;

/** JUnitTest case for class: org.lockss.poller.PollManager */
public class TestPollManager extends TestCase {

  private static String[] rooturls = {"http://www.test.org",
    "http://www.test1.org",
    "http://www.test2.org"};

  private static String urlstr = "http://www.test3.org";
  private static String regexp = "*.doc";
  private static long testduration = 5 * 60 *60 *1000; /* 5 min */

  private static String[] testentries = {"test1.doc", "test2.doc", "test3.doc"};
  protected static ArchivalUnit testau;
  static {
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    org.lockss.plugin.Plugin.registerArchivalUnit(testau);
  }

  protected InetAddress testaddr;
  protected LcapIdentity testID;
  protected LcapMessage[] testmsg;

  public TestPollManager(String _name) {
    super(_name);
  }

  /** setUp method for test case */
  protected void setUp() {
    try {
      testaddr = InetAddress.getByName("127.0.0.1");
      testID = LcapIdentity.getIdentity(testaddr);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = new LcapMessage[3];

      for(int i= 0; i<3; i++) {
        testmsg[i] =  LcapMessage.makeRequestMsg(
          rooturls[i],
          regexp,
          testentries,
          testaddr,
          (byte)5,
          PollManager.generateRandomBytes(),
          PollManager.generateRandomBytes(),
          LcapMessage.NAME_POLL_REQ + (i * 2),
          testduration,
          testID);
      }
    }
    catch (IOException ex) {
      fail("can't create test message" + ex.toString());
    }
  }

  /** tearDown method for test case */
  protected void tearDown() {
    for(int i=0; i<3; i++) {
      PollManager.removePoll(PollManager.makeKey(testmsg[i].getChallenge()));
    }
  }

  /** test for method makePoll(..) */
  public void testMakePoll() {
    // make a name poll
    try {
      Poll p1 = PollManager.makePoll(testmsg[0]);
      // make sure we got the right type of poll here
      assertTrue(p1 instanceof NamePoll);
    }
    catch (IOException ex) {
      fail("unable to make a name poll");
    }

    // make a content poll
    try {
      Poll p2 = PollManager.makePoll(testmsg[1]);
      // make sure we got the right type of poll here
      assertTrue(p2 instanceof ContentPoll);

    }
    catch (IOException ex) {
      fail("unable to make a content poll");
    }


    // make a verify poll
    try {
      Poll p3 = PollManager.makePoll(testmsg[2]);
      // make sure we got the right type of poll here
      assertTrue(p3 instanceof VerifyPoll);
    }
    catch (IOException ex) {
      fail("unable to make a verify poll");
    }

  }

  /** test for method makePollRequest(..) */
  public void testMakePollRequest() {
    try {
      PollManager.makePollRequest(urlstr,regexp,LcapMessage.VERIFY_POLL_REQ,
                                  (byte)5,testaddr,testduration, 10000);
    }
    catch (NullPointerException npe) {
      // ignore this for now
    }
    catch (IOException ex) {
      fail("unable to make a poll request message");
    }
  }

  /** test for method findPoll(..) */
  public void testFindPoll() {
    // lets see if we can find our name poll
    try {
      Poll p1 = PollManager.makePoll(testmsg[0]);
      Poll p2 = PollManager.findPoll(testmsg[0]);
      assertEquals(p1, p2);
    }
    catch (IOException ex) {
      fail("name poll couldn't be found");
    }
  }

  /** test for method handleMessage(..) */
  public void testHandleMessage() {
    try {
      PollManager.handleMessage(testmsg[1]);
    }
    catch (IOException ex) {
      fail("could not create or find content message");
    }
  }

  /** test for method removePoll(..) */
  public void testRemovePoll() {
    try {
      Poll p1 = PollManager.makePoll(testmsg[0]);
      assertNotNull(p1);
      Poll p2 = PollManager.removePoll(p1.m_key);
      assertEquals(p1, p2);
    }
    catch (IOException ex) {
      fail("name poll couldn't be found");
    }

  }

  /** test for method checkForConflicts(..) */
  public void testCheckForConflicts() {
    // lets try to run two content polls in the same location

   LcapMessage[] sameroot = new LcapMessage[3];

    try {
      for(int i= 0; i<3; i++) {
        sameroot[i] =  LcapMessage.makeRequestMsg(
          urlstr,
          regexp,
          testentries,
          testaddr,
          (byte)5,
          PollManager.generateRandomBytes(),
          PollManager.generateRandomBytes(),
          LcapMessage.NAME_POLL_REQ + (i * 2),
          testduration,
          testID);
      }
    }
    catch (IOException ex) {
      fail("Unable to make test messages");
    }

    // check content poll conflicts
    try {
      Poll c1 = PollManager.makePoll(sameroot[1]);
      // differnt content poll should be ok
      ArchivalUnit au = PollManager.checkForConflicts(testmsg[1]);
      assertNull("different content poll s/b ok", au);

      // same content poll should be a conflict
      au = PollManager.checkForConflicts(sameroot[1]);
      assertNotNull("same content poll root s/b conflict", au);

      // different name poll should be ok
      au = PollManager.checkForConflicts(testmsg[0]);
      assertNull("name poll with different root s/b ok", au);

      // same name poll should be conflict
      au = PollManager.checkForConflicts(sameroot[0]);
      assertNotNull("same name poll root s/b conflict", au);

      // different verify poll should be ok
      au = PollManager.checkForConflicts(testmsg[2]);
      assertNull("verify poll s/b ok", au);

      au = PollManager.checkForConflicts(sameroot[2]);
      assertNull("verify poll s/b ok", au);

      // remove the poll
      PollManager.removePoll(c1.m_key);
    }
    catch (IOException ex) {
      fail("unable to make content poll");
    }

    // check name poll conflicts
    try {
      Poll np = PollManager.makePoll(sameroot[0]);
      // differnt name poll should be ok
      ArchivalUnit au = PollManager.checkForConflicts(testmsg[0]);
      assertNull("different name poll s/b ok", au);

      // same content poll should be a conflict
      au = PollManager.checkForConflicts(sameroot[0]);
      assertNotNull("same name poll root s/b conflict", au);

      // different name poll should be ok
      au = PollManager.checkForConflicts(testmsg[1]);
      assertNull("content poll with different root s/b ok", au);

      // same name poll should be conflict
      au = PollManager.checkForConflicts(sameroot[1]);
      assertNotNull("content poll root s/b conflict", au);

      // different verify poll should be ok
      au = PollManager.checkForConflicts(testmsg[2]);
      assertNull("verify poll s/b ok", au);

      au = PollManager.checkForConflicts(sameroot[2]);
      assertNull("verify poll s/b ok", au);

      // remove the poll
      PollManager.removePoll(np.m_key);
    }
    catch (IOException ex) {
      fail("unable to make content poll");
    }
  }


  /** test for method rememberVote(..) */
  public void testRememberVote() {
  }

  /** test for method closeThePoll(..) */
  public void testCloseThePoll() {
  }

  /** test for method getHasher(..) */
  public void testGetHasher() {
    MessageDigest md = PollManager.getHasher();
    assertNotNull(md);
  }

  /** test for method makeVerifier(..) */
  public void testMakeVerifier() {
    // test for make verifier - this will also store the verify/secret pair
    byte[] verifier = PollManager.makeVerifier();
    assertNotNull("unable to make and store a verifier", verifier);

    // retrieve our secret
    byte[] secret = PollManager.getSecret(verifier);
    assertNotNull("unable to retrieve secret for verifier", secret);

    // confirm that the verifier is the hash of the secret
    MessageDigest md = PollManager.getHasher();
    md.update(secret, 0, secret.length);
    byte[] verifier_check = md.digest();
    assertTrue("secret does not match verifier",
               Arrays.equals(verifier, verifier_check));

  }

  /** test for method makeKey(..) */
  public void testMakeKey() {

    byte[] key_bytes = PollManager.generateRandomBytes();
    String key_str = PollManager.makeKey(key_bytes);

    // the key str reconverted as bytes
    byte[] str_bytes = B64Code.decode(key_str.toCharArray());

    assertTrue(Arrays.equals(key_bytes, str_bytes));

  }


  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {TestPollManager.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}