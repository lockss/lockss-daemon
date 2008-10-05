/*
 * $Id: MaxDaemonVersion.java,v 1.1 2005-07-22 23:45:51 tlipkis Exp $
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

import java.io.*;

/**
 * Simple main program that prints the maximum of the daemon versions
 * listed on the command line.
 */
public class MaxDaemonVersion {
  public static String max(String[] args) {
    String maxVer = "";
    long maxVal = -1;

    for (int ix = 0; ix < args.length; ix++) {
      String verstr = args[ix];
      try {
	DaemonVersion ver = new DaemonVersion(verstr);
	long val = ver.toLong();
	if (val > maxVal) {
	  maxVal = val;
	  maxVer = verstr;
	}
      } catch (Exception e) {
	System.err.println("Unparseable daemon version: " + verstr +
			   ": " + e.getMessage());
      }
    }
    return maxVer;
  }

  public static void main(String[] args) {
    System.out.println(max(args));
  }
}
