/*
 * $Id:
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.kbart;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class replaces unknown publisher names in a tab-separated input
 * file with actual publisher names in a tab-separated mod file, using
 * issn/eissn as a matching key.
 * 
 * @author Philip Gust
 *
 */
public class FillInUnknownNames {
  static List<String[]> inList = new ArrayList<String[]>();
  static List<String[]> modList = new ArrayList<String[]>();
  static int modPissn = 0;              // mod file column for print issn
  static int modEissn=1;                // mod file column for eissn
  static int modPublisher = 2;          // mod file column for publisher

  static int inPissn = 1;               // input column for print issn
  static int inEissn = 2;               // input column for eissn
  static int inPublisher = 4;           // input column for publisher
  
  /**
   * Read a tab-separated rows of input into a list string arrays.
   * @param fname the file name
   * @param list the list to fill
   * @throws IOException if an error occurred reading the file
   */
  static void readList(String fname, List<String[]> list)
    throws IOException {
    BufferedReader rdr  = new java.io.BufferedReader(new FileReader(fname));
    String line;
    while ((line = rdr.readLine()) != null) {
      String[] fields = line.split("\t");
      list.add(fields);
    }
  }
  
  /**
   * Write a list of string arrays to a stdout as tab-separated lines.
   * @param list the list to write
   */
  static void writeList(List<String[]> list) {
    PrintStream ps = System.out;
    for (String[] fields : list) {
      for (int i = 0; i < fields.length; i++) {
        if (i > 0) {
          ps.print("\t");
        }
        ps.print(fields[i]);
      }
      ps.println();
    }
  }
  
  /**
   * This method reads two files specified by command line arguments.
   * The first file contains tab-separated lines of fields including
   * issn, eissn, and publisher. The second file contans tab-separated
   * lines of fields containing pissn, eissn, and corresponding correct
   * publisher names. The output is a version of the input file with
   * gensym publisher names replaced by correct publisher names, using
   * the issn and eissn as keys.
   * 
   * @param args the names of the input and modification files
   * @throws IOException if an IO error occurs.
   */
  // PJG: TODO: Should accept input as comma-separated lists with
  // quotes around fields that contain embedded commas. KBART files
  // are comma-separated rather than tab-separated.
  static public void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Need two file names");
      System.exit(1);
    }
    
    // read input and mod lists
    readList(args[0], inList);
    readList(args[1], modList);
    
    // build a map if issn/eissn  to publisher title
    Map<String,String> modMap = new HashMap<String,String>();
    for (String[] modFields : modList) {
      if (modFields[modPissn].length() > 0) {
        modMap.put(modFields[modPissn], modFields[modPublisher]);
      }
      if (modFields[modEissn].length() > 0) {
        modMap.put(modFields[modEissn], modFields[modPublisher]);
      }
    }
    
    for (String[] inFields : inList) {
      // validate row
      if (inFields.length <= inPublisher) {
        continue;
      }
      // get publisher name from the mod map based on print issn
      String modPub = modMap.get(inFields[inPissn]);
      if (modPub == null) {
        // get publisher name from the mod map based on eissn 
        modPub = modMap.get(inFields[inEissn]);
      }
      if (modPub != null) {
        // replace the publisher name in the input map
        inFields[inPublisher] = modPub;
      }
    }
    writeList(inList);
  }
}
