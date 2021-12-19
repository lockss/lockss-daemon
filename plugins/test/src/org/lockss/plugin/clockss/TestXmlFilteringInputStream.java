/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.clockss;

import java.io.*;

import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Test class for org.lockss.filter.FilterUtil
 */
public class TestXmlFilteringInputStream extends LockssTestCase {

  private static final String BASIC_STRING = "123";
  private static final String BASIC_XML = "<?xml version=\"1.0\"?><foo><bar>a\u00e9b a &amp; b</bar></foo>";
  private static final String EMPTY_STRING = "            ";

  private static final String BAD_XML = "<foo><![CDATA[" + "\u001c" + "]]></foo>"; //15th char is 0x1c
  private static final String UNPROTECTED_XML = "<foo>a\u00e9b a & b</foo>"; //12th char is unprotected &
  private static final String EDGE_STRING1 = "&";//should replace - followed by end of buffer
  private static final String EDGE_STRING2 = "&blah;& "; //should replace last one, followed by space
  private static final String EDGE_STRING3 = "& &amp; &\u0009";  // should replace first and last &

  private static final String FIXED_BAD_XML = "<foo><![CDATA[" + "?" + "]]></foo>"; //15th char is 0x1c
  private static final String FIXED_UNPROTECTED_XML = "<foo>a\u00e9b a ? b</foo>"; //12th char is unprotected &
  private static final String FIXED_EDGE_STRING1 = "?";//should replace - followed by end of buffer
  private static final String FIXED_EDGE_STRING2 = "&blah;? "; //should replace last one, followed by space
  private static final String FIXED_EDGE_STRING3 = "? &amp; ?\u0009";  // should replace first and last &

  // compare reading of string input  - these strings shouldn't change
  public void testRead() {
    testReadArrayReplacement(BASIC_STRING, BASIC_STRING); // no changes expected
    testSingleReadReplacement(BASIC_STRING, BASIC_STRING); // no changes expected
    testReadArrayReplacement(BASIC_XML, BASIC_XML); // no changes expected
    testSingleReadReplacement(BASIC_XML, BASIC_XML); // no changes expected
    testReadArrayReplacement(EMPTY_STRING, EMPTY_STRING); // no changes expected
    testSingleReadReplacement(EMPTY_STRING, EMPTY_STRING); // no changes expected
  }

  public void testReplacement() {
    testReadArrayReplacement(UNPROTECTED_XML, FIXED_UNPROTECTED_XML);
    testSingleReadReplacement(UNPROTECTED_XML, FIXED_UNPROTECTED_XML);
    testReadArrayReplacement(BAD_XML, FIXED_BAD_XML);
    testSingleReadReplacement(BAD_XML, FIXED_BAD_XML);
    testReadArrayReplacement(EDGE_STRING1, FIXED_EDGE_STRING1);
    testSingleReadReplacement(EDGE_STRING1, FIXED_EDGE_STRING1);
    testReadArrayReplacement(EDGE_STRING2, FIXED_EDGE_STRING2);
    testSingleReadReplacement(EDGE_STRING2, FIXED_EDGE_STRING2);
    testReadArrayReplacement(EDGE_STRING3, FIXED_EDGE_STRING3);
    testSingleReadReplacement(EDGE_STRING3, FIXED_EDGE_STRING3);
  }

  public void testDifficultChars() {

    //Couldn't set these in unicode notation as a string - eclipse was unhappy
    // should replace 2,3,6,7,9 and leave the others
    String LOTS_OF_ODD_CHARS = new String(new byte[] { 0x58, 0x01, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x1c, 0x58});
    String FIXED_LOTS_OF_ODD_CHARS = new String(new byte[] { 0x58, 0x3F, 0x3F, 0x09, 0x0A, 0x3F, 0x3F, 0x0D, 0x3F, 0x58});
    // replace none of them
    String LOTS_OF_GOOD_CHARS = new String(new byte[] { 0x58, 0x3c, 0x20, 0x0A, 0x0D, 0x58 });

    testReadArrayReplacement(LOTS_OF_ODD_CHARS, FIXED_LOTS_OF_ODD_CHARS);
    testSingleReadReplacement(LOTS_OF_ODD_CHARS, FIXED_LOTS_OF_ODD_CHARS);
    testReadArrayReplacement(LOTS_OF_GOOD_CHARS,LOTS_OF_GOOD_CHARS);
    testSingleReadReplacement(LOTS_OF_GOOD_CHARS,LOTS_OF_GOOD_CHARS);
  }


  /*
   * Take in an original string that may or may not get changed with filtering
   * use read() to force filtering
   * compare the result against the expected string
   */

  private void testSingleReadReplacement(String origString, String expectedString) {
    XmlFilteringInputStream filtIn = (XmlFilteringInputStream) createInputStreamFromString(origString, true);

    int strLen = origString.length();
    int nextbyte;
    try {
      byte exchars[] = expectedString.getBytes(Constants.ENCODING_ISO_8859_1);
      for (int i = 0; i < strLen; i++) {
        nextbyte = filtIn.read();
        assertEquals(exchars[i], (byte)nextbyte);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error(e.getMessage(), e);
    } finally {
      IOUtil.safeClose(filtIn);
    }
  }


  // Take in an original string that may or may not get changed with filtering
  // use read(char, offset, int) to force filtering
  // compare the result against the expected string
  // Try this two ways - read in as one big bytearray and as well
  // read in as a bunch of reads to a single byte bytearray to test edge cases
  private void testReadArrayReplacement(String origString, String expectedString) {

    /* make 2 copies - one for each reading test */
    XmlFilteringInputStream filtIn = (XmlFilteringInputStream) createInputStreamFromString(origString, true);
    XmlFilteringInputStream filtIn2 = (XmlFilteringInputStream) createInputStreamFromString(origString, true);
    int strLen = origString.length();

    try {
      byte exchars[] = expectedString.getBytes(Constants.ENCODING_ISO_8859_1);

      // Read string in to array of full length and check result against expected
      byte fchars[] = new byte[strLen];
      filtIn.read(fchars, 0, strLen);
      assertEquals(exchars,fchars);
      
      //Read string in to array ONE char at a time (to force lookahead) and check against expected
      byte onechar[] = new byte[1];
      byte allchars[] = new byte[strLen];
      for (int i = 0; i< strLen; i++) {
        filtIn2.read(onechar, 0, 1);
        allchars[i] = onechar[0]; // add to complete array
      }
      // now compare the result array to the expected array
      assertEquals(exchars, allchars);
    } catch  (IOException e) {
      // TODO Auto-generated catch block
      log.error(e.getMessage(), e);
    } finally {
      IOUtil.safeClose(filtIn);
      IOUtil.safeClose(filtIn2);
    }
  }


  // Set up stream to get bytes encoded for ISO_8859
  private InputStream createInputStreamFromString(String s, boolean filter) {
    try {
      if (filter) {
        return new XmlFilteringInputStream(new ByteArrayInputStream(s.getBytes(Constants.ENCODING_ISO_8859_1)));
      } else {
        return new ByteArrayInputStream(s.getBytes(Constants.ENCODING_ISO_8859_1));
      }
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      log.error(e.getMessage(), e);
      return null;
    }
  }

}

