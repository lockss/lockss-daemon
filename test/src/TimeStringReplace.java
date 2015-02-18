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

import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;

class TimeStringReplace {
  public static String replaceString1(String line,
				     String oldstr, String newstr) {
    int oldLen = oldstr.length();
    int newLen = newstr.length();
    if (oldLen == 0 || oldstr.equals(newstr)) {
      return line;
    }
    StringBuffer sb = new StringBuffer(line);
    int bufIdx = 0;
    int oldIdx = 0;
    int thisIdx;
    while ((thisIdx = line.indexOf(oldstr, oldIdx)) >= 0) {
      bufIdx += (thisIdx - oldIdx);
      sb.replace(bufIdx, bufIdx+oldLen, newstr);
      bufIdx += newLen;
      oldIdx = thisIdx + oldLen;
    }
    return sb.toString();
  }

  public static String replaceString2(String line,
				     String oldstr, String newstr) {
    int oldLen = oldstr.length();
    if (oldLen == 0 || oldstr.equals(newstr)) {
      return line;
    }
    int lineLen = line.length();
    StringBuffer sb = new StringBuffer(lineLen+10);
    int oldIdx = 0;
    int thisIdx;
    while ((thisIdx = line.indexOf(oldstr, oldIdx)) >= 0) {
      for (int ix=oldIdx; ix < thisIdx; ix++){
	sb.append(line.charAt(ix));
      }
      sb.append(newstr);
      oldIdx = thisIdx + oldLen;
    }
    for (int ix=oldIdx; ix<lineLen; ix++){
      sb.append(line.charAt(ix));
    }
    return sb.toString();
  }

  public static String replaceStringNew(String line,
					String oldstr, String newstr) {
    StringBuffer sb = null;
    int oldLen = oldstr.length();
    if (oldLen == 0) {
      return line;
    }
    if (oldLen == newstr.length()){
      if (oldstr.equals(newstr)){
	return line;
      }
      sb = new StringBuffer(line);
      int prevIdx = 0;
      while ((prevIdx = line.indexOf(oldstr, prevIdx)) >= 0) {
	sb.replace(prevIdx, prevIdx+oldLen, newstr);
	prevIdx+= oldLen;
      }
    }
    else{
      int lineLen = line.length();
      sb = new StringBuffer(lineLen);
      int oldIdx = 0;
      int thisIdx;
      while ((thisIdx = line.indexOf(oldstr, oldIdx)) >= 0) {
	for (int ix=oldIdx; ix < thisIdx; ix++){
	  sb.append(line.charAt(ix));
	}
	sb.append(newstr);
	oldIdx = thisIdx + oldLen;
      }
      for (int ix=oldIdx; ix<lineLen; ix++){
	sb.append(line.charAt(ix));
      }
    }
    return sb.toString();
  }

  public static String replaceStringOld(String line,
					String oldstr, String newstr) {
    if (oldstr.compareTo(newstr) == 0){
      return line;
    }
    StringBuffer sb = null;
    if (oldstr.length() == newstr.length()){
      sb = new StringBuffer(line);
      int lastIdx = line.indexOf(oldstr);
      if (lastIdx >= 0){
	do{
	  sb.replace(lastIdx, lastIdx+newstr.length(), newstr);
	}while ((lastIdx = line.indexOf(oldstr, lastIdx+1)) >= 0);
      }
    }
    else{
      sb = new StringBuffer();
      int oldStrIdx = 0;
      int lastIdx = line.indexOf(oldstr);
      if (lastIdx >= 0){
	do{
	  for (int ix=oldStrIdx; ix < lastIdx; ix++){
	    sb.append(line.charAt(ix));
	  }
	  sb.append(newstr);
	  oldStrIdx = lastIdx + oldstr.length();
	}while ((lastIdx = line.indexOf(oldstr, lastIdx+1)) >= 0);
      }
      for (int ix=oldStrIdx; ix<line.length(); ix++){
	sb.append(line.charAt(ix));
      }
    }
    return sb.toString();
  }

  private static final int RPT = 100000;

  private static long timeRep1(String str, String os, String ns) {
    Date start = new Date();
    for (int ix = 0; ix < RPT; ix++) {
      replaceString1(str, os, ns);
    }
    return TimerUtil.timeSince(start);
  }

  private static long timeRep2(String str, String os, String ns) {
    Date start = new Date();
    for (int ix = 0; ix < RPT; ix++) {
      replaceString2(str, os, ns);
    }
    return TimerUtil.timeSince(start);
  }

  private static String longString = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

  private static String xString = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

  public static void timeOne(int btwnLen, int oldLen, int newLen, int rpt) {
    String os = longString.substring(0, oldLen);
    String ns = longString.substring(1, newLen+1);
    String btwn = xString.substring(0,btwnLen);
    StringBuffer sb = new StringBuffer();
    for (int ix = 0; ix < rpt; ix++) {
      sb.append(os);
      sb.append(btwn);
    }
    String s = sb.toString();
    System.out.print(s.length()+" char string, rep "+rpt+" copies of "+oldLen+" chars with "+newLen+" chars");
//      System.out.println(s+" | "+os+" | "+ns);
    System.out.println(" 1/2: "+ timeRep1(s, os, ns) +
		       " / "+ timeRep2(s, os, ns));
  }

  public static void main(String argv[]) {
    timeOne(3, 1, 1, 5);
    timeOne(3, 1, 2, 5);
    timeOne(3, 2, 1, 5);
    timeOne(3, 10, 10, 5);
    timeOne(3, 10, 11, 5);
    timeOne(3, 11, 10, 5);
    timeOne(3, 40, 40, 5);
    timeOne(3, 40, 41, 5);
    timeOne(3, 40, 43, 5);
    timeOne(3, 41, 40, 5);
  }
}
