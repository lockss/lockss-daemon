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
