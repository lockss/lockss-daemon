/*
 * $Id: Indexer.java,v 1.3 2006-07-11 17:42:24 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.doclet;

import com.sun.javadoc.*;
import java.io.*;
import java.util.*;

public class Indexer {
  static PrintStream out = System.out;
  static boolean closeOut = false;
  static boolean noDots = false;
  static int col2 = 45;

  public static boolean start(RootDoc root){
    if (!processOptions(root)) {
      return false;
    }
    writeContents(root.classes(), null);
    if (closeOut) {
      out.close();
      out = null;
    }
    return true;
  }

  private static void writeContents(ClassDoc[] classes, String tagName) {
    for (int i=0; i < classes.length; i++) {
      ClassDoc c = classes[i];
      doOneClass(c);
    }
  }

  private static void doOneClass(ClassDoc c) {
    printLine(0, c.name() + (c.isInterface() ? " (interface)" : " (class)"),
	      c.position().line());
    doOneSection("Fields", c.fields());
    doOneSection("Constructors", c.constructors());
    doOneSection("Methods", c.methods());
  }

  private static void doOneSection(String heading, MemberDoc doc[]) {
    boolean empty = true;
    for (int ix=0; ix < doc.length; ix++) {
      if (!doc[ix].isSynthetic()) {
	empty = false;
	break;
      }
    }
    if (empty) {
      return;
    }
    Arrays.sort(doc, new Comparator() {
	public int compare(Object o1, Object o2) {
	  ProgramElementDoc d1 = (ProgramElementDoc)o1;
	  ProgramElementDoc d2 = (ProgramElementDoc)o2;
	  return d1.name().compareTo(d2.name());
	}
      });
    out.print("  ");
    out.println(heading);
    for (int ix=0; ix < doc.length; ix++) {
      MemberDoc d = doc[ix];
      if (!d.isSynthetic()) {
	printLine(4, d.name(), d.position().line());
      }
    }
  }

  static final String blanks = "                                                                                ";
  static final String dots = " . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .";

  public static void printLine(int indent, String str1, int line) {
    printLine(indent, str1, Integer.toString(line));
  }

  public static void printLine(int indent, String str1, String str2) {
    int rpt = col2 - (str1.length() + str2.length() + indent);
    out.print(blanks.substring(0, indent));
    out.print(str1);
    out.print((noDots ? blanks : dots).substring(0, Math.max(rpt, 1)));
    out.println(str2);
  }

  private static boolean processOptions(RootDoc root) {
    String[][] options = root.options();
    for (int i = 0; i < options.length; i++) {
      String[] opt = options[i];
      if (false) {
	for (int j = 0; j < opt.length; j++) {
	  System.out.print(opt[j]+", ");
	}
	System.out.println("");
      }
      if (opt[0].equals("-o")) {
	String name = opt[1];
	File file = new File(name);
	try {
	  out =
	    new PrintStream(new
	      BufferedOutputStream(new FileOutputStream(file)));
	  closeOut = true;
	} catch (FileNotFoundException e) {
	  root.printError("Indexer doclet: File not found: " + name);
	  return false;
	}
	continue;
      }
      if (opt[0].equalsIgnoreCase("-dots")) {
	String v = opt[1];
	noDots = v.equals("0");
	continue;
      }
    }
    return true;
  }

  public static int optionLength(String option) {
    if (option.equals("-o")) {
      return 2;
    }
    if (option.equals("-d")) {
      return 2;
    }
    if (option.equalsIgnoreCase("-dots")) {
      return 2;
    }
    return 0;
  }

//    public static boolean validOptions(String options[][],
//  				     DocErrorReporter reporter) {
//      boolean foundTagOption = false;
//      for (int i = 0; i < options.length; i++) {
//        String[] opt = options[i];
//        if (opt[0].equals("-tag")) {
//  	if (foundTagOption) {
//  	  reporter.printError("Only one -tag option allowed.");
//  	  return false;
//  	} else {
//  	  foundTagOption = true;
//  	}
//        }
//      }
//      if (!foundTagOption) {
//        reporter.printError("Usage: javadoc -tag mytag -doclet ListTags ...");
//      }
//      return foundTagOption;
//    }
}
