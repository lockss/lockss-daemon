/*
 * $Id: TestXmlFilteringInputStream.java,v 1.1 2014-01-17 18:05:32 alexandraohlson Exp $
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

package org.lockss.plugin.clockss;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Test class for org.lockss.filter.FilterUtil
 */
public class TestXmlFilteringInputStream extends LockssTestCase {

  private static final String BASIC_STRING = "123";
  private static final String BASIC_XML = "<?xml version=\"1.0\"?><foo><bar>a\u00e9b a &amp; b</bar></foo>";

  private static final String BAD_XML = "<foo><![CDATA[" + "\u001c" + "]]></foo>"; //15th char is 0x1c
  private static final String UNPROTECTED_XML = "<foo>a\u00e9b a & b</foo>"; //12th char is unprotected &
  private static final String EDGE_STRING1 = "&";//should replace - followed by end of buffer
  private static final String EDGE_STRING2 = "&blah;& "; //should replace last one, followed by space
  private static final String EDGE_STRING3 = "& &amp; &\u0009";  // should replace first and last &
 

  // compare reading of string input  - these strings shouldn't change
  public void testRead() {
    testCharacterReplacement(BASIC_STRING, null); // no characters should replace
    testCharacterReplacement(BASIC_XML, null); // no characters should replace
  }

  public void testReplacement() {
    testCharacterReplacement(UNPROTECTED_XML, Arrays.asList(12));
    testCharacterReplacement(BAD_XML, Arrays.asList(15));
    testCharacterReplacement(EDGE_STRING1, Arrays.asList(1));
    testCharacterReplacement(EDGE_STRING2, Arrays.asList(7));
    testCharacterReplacement(EDGE_STRING3, Arrays.asList(1,9));

  }
  
  public void testDifficultChars() {

    //Couldn't set these in unicode notation as a string - eclipse was unhappy
    // should replace 2,3,6,7,9 and leave the others
    String LOTS_OF_ODD_CHARS = new String(new byte[] { 0x58, 0x01, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x1c, 0x58});
    // replace none of them
    String LOTS_OF_GOOD_CHARS = new String(new byte[] { 0x58, 0x3c, 0x20, 0x0A, 0x0D, 0x58 });

    testCharacterReplacement(LOTS_OF_ODD_CHARS, Arrays.asList(2,3,6,7,9));
    testCharacterReplacement(LOTS_OF_GOOD_CHARS,null);
  }

  // filter a string and if the correct characters have been turned in to '?'
  // the list of integers indicate which position in the string should have been
  // altered. For other slots, compare against and unfiltered version of the string.
  // POSITION in string starts counting at ONE (eg. 1st char or 2nd char) not from zero
  private void testCharacterReplacement(String testString, List<Integer> changedSlots) {

    XmlFilteringInputStream filtIn = (XmlFilteringInputStream) createInputStreamFromString(testString, true);
    InputStream unFiltIn = createInputStreamFromString(testString,false);

    int strLen = testString.length();
    byte fchars[] = new byte[strLen+1];//offset so count starts at 1
    fchars[0] = 0;
    byte ufchars[] = new byte[strLen+1];
    ufchars[0] = 0;
    try {
      filtIn.read(fchars, 1, strLen);
      unFiltIn.read(ufchars, 1, strLen);

      for (int i=1; i < strLen+1; i ++) {
        log.debug3("Filtered char at " + i + " is " + (byte)fchars[i]);
        log.debug3("Original char at " + i + " is " + (byte)ufchars[i]);
        if ((changedSlots != null) && changedSlots.contains(i)) {
          assertEquals(fchars[i], '?');
        } else {
          assertEquals(ufchars[i],fchars[i]);
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      IOUtil.safeClose(filtIn);
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
      e.printStackTrace();
      return null;
    }
  }

}
