/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.jetty;

import java.io.*;
import java.util.*;
import java.security.*;

import org.mortbay.http.*;
import org.mortbay.util.*;

import org.lockss.util.*;
import org.lockss.test.LockssTestCase;

public class TestMDCredential extends LockssTestCase {

  String calcDigest(String type, String message) throws Exception {
    MessageDigest md = MessageDigest.getInstance(type);
    md.update(message.getBytes(Constants.DEFAULT_ENCODING));
    return TypeUtil.toString(md.digest(), 16);
  }

  public void testIll() throws Exception {
    try {
      MDCredential.makeCredential(null);
      fail("getCredential(null) should throw");
    } catch (NullPointerException e) {
    }
  }

  public void testUnknown() throws Exception {
    try {
      MDCredential.makeCredential("FOOHASH:SDLFKJSDF");
    fail("getCredential(unknwon algorithm) should throw");
    } catch (NoSuchAlgorithmException e) {
    }
    try {
      MDCredential.makeCredential("CRYPT:SDLFKJSDF");
    fail("getCredential(unknwon algorithm) should throw");
    } catch (NoSuchAlgorithmException e) {
    }
  }

  public void testIt(String type) throws Exception {
    String pwd = "passw0rd";
    String dig = calcDigest(type, pwd);
    MDCredential cred =
      (MDCredential)MDCredential.makeCredential(type + ":" + dig);
    assertEquals(type, cred.getType());
    assertEquals(dig, TypeUtil.toString(cred.getDigest(), 16));

    assertTrue(cred.check(pwd));
    assertFalse(cred.check("wordpass"));

    Credential equiv = MDCredential.makeCredential(type + ":" + dig);
    assertTrue(cred.check(equiv));
  }

  public void testMD5() throws Exception {
    testIt("MD5");
  }

  public void testSHA1() throws Exception {
    testIt("SHA1");
    testIt("SHA-1");
  }

//   public void testSHA256() throws Exception {
//     testIt("SHA-256");
//   }

//   public void testSHA384() throws Exception {
//     testIt("SHA-384");
//   }

//   public void testSHA512() throws Exception {
//     testIt("SHA-512");
//   }

}
