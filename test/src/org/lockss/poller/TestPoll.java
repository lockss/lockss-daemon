package org.lockss.poller;

import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.protocol.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import java.util.*;
import org.lockss.util.*;
import java.security.MessageDigest;
import java.io.*;
import java.net.InetAddress;
import junit.framework.TestCase;
import java.net.UnknownHostException;

/** JUnitTest case for class: org.lockss.poller.Poll */
public class TestPoll extends TestCase {
  private static String[] rooturls = {"http://www.test.org",
  "http://www.test1.org", "http://www.test2.org"};

  private static String urlstr = "http://www.test.org";
  private static String regexp = "*.doc";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String[] testentries = {"test1.doc", "test2.doc", "test3.doc"};
  protected static ArchivalUnit testau;
 static {
   testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
   org.lockss.plugin.Plugin.registerArchivalUnit(testau);
 }

  protected InetAddress testaddr;
  protected Identity testID;
  protected Message testmsg;
  protected Poll ctestpoll;
	protected Poll ntestpoll;


  public TestPoll(String _name) {
    super(_name);
  }

  /** setUp method for test case */
  protected void setUp() {
    try {
      testaddr = InetAddress.getByName("127.0.0.1");
      testID = Identity.getIdentity(testaddr,0);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = Message.makeRequestMsg(urlstr,
                                       regexp,
                                       testentries,
                                       testaddr,
                                       (byte)5,
                                       testbytes,
                                       testbytes,
                                       Message.CONTENT_POLL_REQ,
                                       5 * 60 *60 *1000,
                                       testID);
    }
    catch (IOException ex) {
      fail("can't create test message");
    }

 }

	public void testContentPoll() {
		Message m = null;
		Poll p = null;

		try {
			m = Message.makeRequestMsg(urlstr,
																			 regexp,
																			 testentries,
																			 testaddr,
																			 (byte)5,
																			 testbytes,
																			 testbytes,
																			 Message.CONTENT_POLL_REQ,
																			 5 * 60 *60 *1000,
																			 testID);
		}
		catch (IOException ex) {
			fail("can't create test message");
		}
		// test for creation
    try {
      p = Poll.makePoll(m);
    }
    catch (IOException ex) {
			fail("content poll failed");
    }

		assertTrue(p != null);

		// lets see if we can find it
    Poll foundp = null;
    try {
      foundp = Poll.findPoll(m);
    }
    catch (IOException ex) {
			fail("content poll mismatch in findPoll");
    }
		assertEquals(p,foundp);

		// TODO test run
		// p.startPoll();
	}

	public void testNamePoll() {
		Poll p = null;
		Message m = null;

		try {
			m = Message.makeRequestMsg(rooturls[1],
																			 regexp,
																			 testentries,
																			 testaddr,
																			 (byte)5,
																			 testbytes,
																			 testbytes,
																			 Message.NAME_POLL_REQ,
																			 5 * 60 *60 *1000,
																			 testID);
		}
		catch (IOException ex) {
			fail("can't create test message");
		}
		// test for creation
		try {
			p = Poll.makePoll(m);
		}
		catch (IOException ex) {
			fail("name poll creation failed" + ex.toString());
		}

		assertTrue(p != null);

		// lets see if we can find it
		Poll foundp = null;
		try {
			foundp = Poll.findPoll(m);
		}
		catch (IOException ex) {
			fail("name poll mismatch in findPoll");
		}
		assertEquals(p,foundp);

		// TODO test run
		// p.startPoll();
	}

	public void testVerifyPoll() {
		Poll p = null;
		Message m = null;

		try {
			m = Message.makeRequestMsg(urlstr,
																			 regexp,
																			 testentries,
																			 testaddr,
																			 (byte)5,
																			 testbytes,
																			 testbytes,
																			 Message.VERIFY_POLL_REQ,
																			 5 * 60 *60 *1000,
																			 testID);
		}
		catch (IOException ex) {
			fail("can't create test message");
		}
		// test for creation
		try {
			p = Poll.makePoll(m);
		}
		catch (IOException ex) {
			fail("verify poll creation failed");
		}

		assertTrue(p != null);

		// lets see if we can find it
		Poll foundp = null;
		try {
			foundp = Poll.findPoll(m);
		}
		catch (IOException ex) {
			fail("verify poll mismatch in findPoll");
		}
		assertEquals(p,foundp);

		// TODO test run
		// p.startPoll();
	}

	public void testcheckForConflicts() {
		Poll p = null;
		Message m = null;

		try {
			m = Message.makeRequestMsg(rooturls[0],
																			 regexp,
																			 testentries,
																			 testaddr,
																			 (byte)5,
																			 testbytes,
																			 testbytes,
																			 Message.CONTENT_POLL_REQ,
																			 5 * 60 *60 *1000,
																			 testID);
		}
		catch (IOException ex) {
			fail("can't create test message");
		}
		// test for creation
		try {
			p = Poll.makePoll(m);
		}
		catch (IOException ex) {
		}
		// this should fail
		assertTrue(p == null);
		try {
			m = Message.makeRequestMsg( rooturls[0],
																			 regexp,
																			 testentries,
																			 testaddr,
																			 (byte)5,
																			 testbytes,
																			 testbytes,
																			 Message.NAME_POLL_REQ,
																			 5 * 60 *60 *1000,
																			 testID);
		}
		catch (IOException ex) {
		}
		// this should also fail
		try {
			p = Poll.makePoll(m);
		}
		catch (IOException ex) {
		}
		assertTrue(p == null);

	}

  /** Executes the test case */
  public static void main(String[] argv) {
    String[] testCaseList = {TestPoll.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}