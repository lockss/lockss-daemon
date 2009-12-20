/*
 * $Id: MetadataUtil.java,v 1.1 2009-12-20 00:05:00 dshr Exp $
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

package org.lockss.util;

import org.apache.commons.lang.StringEscapeUtils;

import java.net.URLDecoder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MetadataUtil {


  static Logger log = Logger.getLogger("TestBePressMetadataExtractor");

  /**
   * Check that ISSN is valid. Method checks that ISSN number is correctly balanced (4 digits on either side of a hyphen)
   * and that the ckeck digit (rightmost digit) is valid.
   * @param issn the issn string
   * @return true if issn is valid, false otherwise
   */
  public static boolean isISSN(String issn) {

    Pattern p = Pattern.compile("\\d{4}-\\d{3}[\\d{1}|x{1}|X{1}]");
    Matcher m = p.matcher(issn);

    if(!m.matches()){
      log.debug("ISSN is not valid: "+issn);
      return false;
    }

    String issnArr[] = issn.split("-");
    String issnStr = issnArr[0] + "" + issnArr[1];

    char issnChars[] = issnStr.toCharArray();
    int checkSum = 0;

    // calculate what the check digit should be
    for (int i = 0; i < issnChars.length - 1; i++) {
      checkSum += Integer.parseInt(String.valueOf(issnChars[i])) * (issnChars.length - i);
    }

    char checkDigitChar = issnChars[issnChars.length - 1];
    int checkDigit;

    // a check digit of X means it's digit 10 in roman numerals
    if (String.valueOf(checkDigitChar).equalsIgnoreCase("x")) {
      checkDigit = 10;
    } else {
      checkDigit = Integer.parseInt(String.valueOf(checkDigitChar));
    }

    int remainder = checkSum % 11;
    int correctCheckDigit;

    if (checkDigit != 0 && remainder == 0) {
      log.debug("Check digit is not right. Expected: 0, Found: "+checkDigit);
      return false;
    } else if (checkDigit == 0 && remainder != 0) {
      correctCheckDigit = (11 - remainder);
      log.debug("Check digit is not right. Expected "+correctCheckDigit+", Found: "+checkDigit);
      return false;
    } else if (checkDigit != 0 && 11 - remainder != checkDigit) {

      String found;
      String shouldBe;

      correctCheckDigit = (11 - remainder);
      found = checkDigit == 10 ? "X" : checkDigit + "";
      shouldBe = correctCheckDigit == 10 ? "X" : correctCheckDigit + "";

      log.debug("Check digit is not right. Expected: "+shouldBe+" Found: "+found);
      return false;
    }

    return true;
  }

  /**
   * Check that DOI number is a valid DOI string. 
   * @param doi the DOI string
   * @return true of DOI is a valid string, false otherwise
   */
  public static boolean isDOI(String doi) {    

    Pattern p = Pattern.compile("10\\.\\d{4}/.*");    
    Matcher m = p.matcher(doi);

    if(!m.matches()){
      log.debug("DOI is not valid: "+doi);
      return false;
    }

    int firstIndexOfSlash = doi.indexOf("/");
    String doiSuffix = doi.substring(firstIndexOfSlash+1);

    // there should NOT be a '/' character in the suffix
    p = Pattern.compile(".*/.*");    
    Matcher doiSuffixMatcher = p.matcher(doiSuffix);

    if(doiSuffixMatcher.matches()){
      log.debug("Suffix should not include '/' characters: "+doi);
      return false;
    }
    return true;
  }  

}
