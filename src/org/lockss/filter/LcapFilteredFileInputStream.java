// ========================================================================
// $Id: LcapFilteredFileInputStream.java,v 1.1 2003-07-23 00:19:14 troberts Exp $
// ========================================================================

package org.lockss.filter;
import java.util.*;
import java.io.*;

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

// A FilterInputStream that removes HTML markup.  Idea is to remove all
// variable content (e.g., advertisement URLs), leaving only journal text
// to feed to hasher.
// XXX Curently too simplistic and easily fooled, but works well enough
// XXX for beta test.
// XXX Need to generalize and make part of journal definition API.

public class LcapFilteredFileInputStream extends FilterInputStream {
  static String[] filteredPostfixes = {
    ".htm", ".HTM",
    ".html", ".HTML",
    ".shtml", ".SHTML",
  };
  static final String[] flags = {
    "<!DOCTYPE HTML",
    "<HEAD>",
    "<HTML>"
  };
  boolean inHTML;
  boolean inWhiteSpace;
  boolean hitEOF;
  static final byte asciiSpace = 32;

  // Determine whether the file needs to be filtered.
  // If file has extension that implies not html, return regular input stream.
  // If file has extension that implies html, or contains one of the
  // markup flag strings, return a filtered input stream, else a
  // regular input stream
  public static InputStream getInputStream(File f) throws IOException {
    FileInputStream fis = new FileInputStream(f);
    String name = f.getName();
    if (name.endsWith(".tmp")) {
      String newName = name.substring(0, name.length() - 4);
      name = newName;
    }
    boolean match = false;
    // If the extension is one of the ones we do filter,  do filter
    for (int i = 0; i < filteredPostfixes.length; i++) {
      if (name.endsWith(filteredPostfixes[i])) {
	match = true;
	break;
      }
    }
    // Otherwise look for HTML near the front of the file
    if (!match) {
      InputStreamReader isr = new InputStreamReader(fis);
      char[] buf= new char[80];
      int m = 0;
      if ((m = isr.read(buf, 0, buf.length)) > 0) {
	int offset = 0;
	while (isWhiteSpace(buf[offset]))
	  offset++;
	for (int i = 0; i < flags.length; i++) {
	  int j = 0;
	  while (j < flags[i].length() && j < (m - offset)) {
	    if (buf[offset + j] != flags[i].charAt(j))
	      break;
	    j++;
	    if (j >= flags[i].length())
	      match = true;
	  }
	}
      }
      fis.close();
      if (!match) {
	return (new FileInputStream(f));
      }
    }
    return (new LcapFilteredFileInputStream(new FileInputStream(f)));
  }

  // Create filtered stream, initialize state.
  public LcapFilteredFileInputStream(InputStream fis) {
    super(fis);
    inHTML = false;
    inWhiteSpace = false;
    hitEOF = false;
  }

  // Read one byte
  public int read() throws IOException {
    byte[] buf = new byte[1];
    int ret = read(buf, 0, 1);
    if (ret >= 0)
      return ((int)buf[0]);
    else
      return (ret);
  }

  // Read bytes into array
  public int read(byte[] b) throws IOException {
    return (read(b, 0, b.length));
  }

  // Read bytes into array.  Remove everything between < and >, and
  // collapse all whitespace to a single space
  public int read(byte[] b, int off, int len) throws IOException {
    if (hitEOF)
      return -1;
    int ptr = off;
    while (ptr < len) {
      // we still haven't found enough bytes
      int next = super.read();
      if (next == -1) {
	// we just hit EOF
	hitEOF = true;
	break;
      }
      // handle HTML tags - omit <.*>
      // XXX Aso need to ignore stuff before & including beginFlag and
      // XXX after & including endFlag
      if (inHTML) {
	if (isEndDelimiter(next)) {
	  inHTML = false;
	  if (!inWhiteSpace) {
	    inWhiteSpace = true;
	    b[ptr++] = asciiSpace;
	  }
	}
      } else {
	if (isStartDelimiter(next)) {
	  inHTML = true;
	} else {
	  // handle whitespace - collapse multiple whitespace to single space
	  if (inWhiteSpace) {
	    if (isWhiteSpace(next))
	      continue;
	    inWhiteSpace = false;
	  } else {
	    if (isWhiteSpace(next)) {
	      inWhiteSpace = true;
	      next = asciiSpace;
	    }
	  }
	  // not whitespace - output the character
	  b[ptr++] = (byte) next;
	}
      }
    }
    return (ptr - off);
  }

  private static boolean isEndDelimiter(int b) {
    return (b == 62); // '>' ascii
  }

  private static boolean isStartDelimiter(int b) {
    return (b == 60); // '<' ascii
  }

  private static boolean isWhiteSpace(int b) {
    return ((b >= 0 && b <= asciiSpace) || b == 127);
  }

  public static void main(String[] args) {
    OutputStream ops = System.out;
    for (int i = 0; i < args.length; i++) try {
      InputStream ins = getInputStream(new File(args[i]));
      if (ins != null) {
	int m;
	byte[] buf = new byte[4096];
	while ((m = ins.read(buf)) >= 0) {
	  ops.write(buf, 0, m);
	}
      }
    } catch (IOException e) {
      System.err.println(e.toString());
    }
  }
}
