/*
 * $Id: BoyerMoore.java,v 1.2 2005-10-11 05:48:30 tlipkis Exp $
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

// Original code is

// Copyright by Michael Lecuyer 1998.
// This program may be used in any way you choose.
// Please acknowledge my copyright where you use this in your program.
// No need for a public copyright notice.

// Modified by Theodore Hong.
// The original version of this code can be found at:
//      http://www.theorem.com/java/BM.java

package org.lockss.util;

import java.util.*;

public class BoyerMoore {


  private static final int MAXCHAR = 256;
  // Maximum chars in character set.

  private char pat[];	// Byte representation of pattern
  private int patLen;
  private int partial;
  // Bytes of a partial match found at the end of a text buffer

  private int skip[];	// Internal BM table
  private int d[];		// Internal BM table

  private boolean ignoreCase = false;

  /**
   * Boyer-Moore text search
   * <P> Scans text left to right using what it knows of the pattern
   * quickly determine if a match has been made in the text. In addition
   * it knows how much of the text to skip if a match fails.
   * This cuts down considerably on the number of comparisons between
   * the pattern and text found in pure brute-force compares
   * This has some advantages over the Knuth-Morris-Pratt text search.
   * <P>The particular version used here is
   * from "Handbook of Algorithms and Data
   * Structures", G.H. Gonnet & R. Baeza-Yates.
   *
   * Example of use:
   * <PRE>
   * String pattern = "and ";
   * <BR>
   * BoyerMoore bm = new BoyerMoore();
   * bm.compile(pattern);
   *
   * int bcount;
   * int search;
   * while ((bcount = f.read(b)) >= 0)
   * {
   *    System.out.println("New Block:");
   *    search = 0;
   *    while ((search = bm.search(b, search, bcount-search)) >= 0)
   *    {
   *       if (search >= 0)
   *       {
   *          System.out.println("full pattern found at " + search);
   * <BR>
   *          search += pattern.length();
   *          continue;
   *       }
   *    }
   *    if ((search = bm.partialMatch()) >= 0)
   *    {
   *       System.out.println("Partial pattern found at " + search);
   *    }
   * }
   * </PRE>
   */
  public BoyerMoore() {
    skip = new int[MAXCHAR];
    d = null;
  }


  public BoyerMoore(boolean ignoreCase) {
    this();
    this.ignoreCase = ignoreCase;
  }

  /**
   * Shortcut constructor
   */
  public BoyerMoore(char[] pattern) {
    this();
    compile(pattern);
  }


  /**
   * Shortcut constructor
   */
  public BoyerMoore(String pattern) {
    this();
    compile(pattern.toCharArray());
  }


  /**
   * Compiles the text pattern for searching.
   *
   * @param pattern What we're looking for.
   */
  public void compile(String pattern) {
    compile(pattern.toCharArray());
  }


  /**
   * Compiles the text pattern for searching.
   *
   * @param pattern What we're looking for.
   */
  public void compile(char[] pattern) {
    pat = pattern;
    patLen = pat.length;

    int j, k, m, t, t1, q, q1;
    int f[] = new int[patLen];
    d = new int[patLen];

    m = patLen;
    for (k = 0; k < MAXCHAR; k++) {
      skip[k] = m;
    }

    for (k = 1; k <= m; k++) {
      d[k-1] = (m << 1) - k;
      skip[(pat[k-1] & 0xff)] = m - k;    // cast to unsigned byte
    }

    t = m + 1;
    for (j = m; j > 0; j--) {
      f[j-1] = t;
      while (t <= m && pat[j-1] != pat[t-1]) {
	d[t-1] = (d[t-1] < m - j) ? d[t-1] : m - j;
	t = f[t-1];
      }
      t--;
    }
    q = t;
    t = m + 1 - q;
    q1 = 1;
    t1 = 0;

    for (j = 1; j <= t; j++) {
      f[j-1] = t1;
      while (t1 >= 1 && pat[j-1] != pat[t1-1]) {
	t1 = f[t1-1];
      }
      t1++;
    }

    while (q < m) {
      for (k = q1; k <= q; k++) {
	d[k-1] = (d[k-1] < m + q - k) ? d[k-1] : m + q - k;
      }
      q1 = q + 1;
      q = q + t - f[t-1];
      t = f[t-1];
    }
  }


  /**
   * Search for all occurrences of the compiled pattern in the given text.
   *
   * @param text  Buffer containing the text
   * @param start Start position for search
   * @param end   Ending position for search
   *
   * @return Vector containing all matching positions in buffer.
   */
  public Vector searchAll(char text[], int start, int end) {
    Vector results = new Vector();
    int pos = start;

    while ((pos = search(text, pos, end)) >= 0) {
      if (pos >= 0) {
	results.addElement(new Integer(pos));
	pos += patLen;
      }
    }

    return results;
  }


  /**
   * Search for the compiled pattern in the given text.
   * A side effect of the search is the notion of a partial
   * match at the end of the searched buffer.
   * This partial match is helpful in searching text files when
   * the entire file doesn't fit into memory.
   *
   * @param text  Buffer containing the text
   *
   * @return position in buffer where the pattern was found.
   * @see #partialMatch()
   */
  public int search(char text[]) {
    return search(text, 0, text.length);
  }

  /**
   * Search for the compiled pattern in the given text.
   * A side effect of the search is the notion of a partial
   * match at the end of the searched buffer.
   * This partial match is helpful in searching text files when
   * the entire file doesn't fit into memory.
   *
   * @param text  Buffer containing the text
   * @param start Start position for search
   * @param end   Ending position for search
   *
   * @return position in buffer where the pattern was found.
   * @see #partialMatch()
   */
  public int search(char text[], int start, int end) {
    partial = 0;	// assume no partial match

    if (d == null) {
      return -1;	// no pattern compiled, nothing matches.
    }

    int m = patLen;
    if (m == 0) {
      return 0;
    }

    int k, j = 0;
    int max = 0;	// used in calculation of partial match. Max distand we jumped.

    for (k = start+m-1; k < end+m-1;) {
      // set up possible partial match
      int save_k = k;
      if (k >= end) {
	partial = m - (k-end+1);
      }

      // scan string vs. pattern
      // ignore positions beyond end of buffer
      for (j = m-1; j >= 0; j--, k--) {
	if (k < end &&
	    (ignoreCase ? Character.toLowerCase(text[k]) : text[k])
	    != pat[j]) {
	  break;   // confirmed non-match
	}
      }

      // did we make it all the way through the string?
      if (j == -1) {
	return (partial == 0 ? k+1 : -1);   // full or partial match?
      }

      // skip to next possible start
      int z =
	skip[ignoreCase ? Character.toLowerCase(text[k]) : text[k]];

      max = (z > d[j]) ? z : d[j];
      if (save_k < end) {
	k += max;
      } else {
	k = save_k+1;  // calculation doesn't work past end of buffer,
	// just do it by hand
      }
      partial = 0;
    }

    /*
      if (k >= end && k < end+m-1) {    // if we're near end of buffer --
      k = end-1;     // i.e. k - (k-end+1)
      for (j = partial-1; j >= 0 && text[k] == pat[j]; j--)
      k--;

      if (j >= 0)
      partial = 0;    // no partial match

      return -1;	// not a real match
      }
    */

    return -1;	// No match
  }

  private char possiblyLower(char kar) {
    if (ignoreCase) {
      return Character.toLowerCase(kar);
    }
    return kar;
  }

  /**
   * Returns the number of partially matching characters at the end
   * of the text buffer where a partial match was found.
   * <P>
   * In many case where a full text search of a large amount of data
   * precludes access to the entire file or stream the search algorithm
   * will note where the final partial match occurs.
   * After an entire buffer has been searched for full matches calling
   * this method will reveal if a potential match appeared at the end.
   * This information can be used to patch together the partial match
   * with the next buffer of data to determine if a real match occurred.
   *
   * @return the number of bytes that formed a partial match, 0 if no
   * partial match
   */
  public int partialMatch() {
    return partial;
  }
}
