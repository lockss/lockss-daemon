package org.lockss.kbart;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FillInUnknownNames {
  static List<String[]> inList = new ArrayList<String[]>();
  static List<String[]> modList = new ArrayList<String[]>();
  static int modPissn = 0;
  static int modEissn=1;
  static int modPublisher = 2;
  static int inPissn = 1;
  static int inEissn = 2;
  static int inPublisher = 4;
  
  static void readList(String fname, List<String[]> list)
    throws IOException {
    BufferedReader rdr  = new java.io.BufferedReader(new FileReader(fname));
    String line;
    while ((line = rdr.readLine()) != null) {
      String[] fields = line.split("\t");
      list.add(fields);
    }
  }
  
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
  
  static public void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Need two file names");
      System.exit(1);
    }
    readList(args[0], inList);
    readList(args[1], modList);
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
      if (inFields.length <= inPublisher) {
        continue;
      }
      String modPub = modMap.get(inFields[inPissn]);
      if (modPub == null) {
        modPub = modMap.get(inFields[inEissn]);
      }
      if (modPub != null) {
        inFields[inPublisher] = modPub;
      }
    }
    writeList(inList);
  }
}
