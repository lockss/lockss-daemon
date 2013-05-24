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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

/**
 * This class replaces unknown publisher names in a comma-separated input
 * file with actual publisher names in a comma-separated mod file, using
 * issn/eissn as a matching key.
 * It uses the CsvReader and CsvWriter to handle the csv parsing and output
 * 
 * @author Philip Gust
 *
 */
public class FillInUnknownNames {
  private final static String PUB_TITLE = "publisher_name";
  private final static String PISSN_TITLE = "print_identifier";
  private final static String EISSN_TITLE = "online_identifier";
  private final static String UNKNOWN_PUB = "UNKNOWN_PUBLISHER";
  
  
  /*
   * Read in a csv file that has at values for publisher, print issn and eissn
   * Create a map between the issns and the publisher
   */
  static void createModMap(String fname, Map<String,String> mMap) {
    try {
      CsvReader modifications = new CsvReader(fname);
      String pub;
      String pISSN;
      String eISSN;
      
      modifications.readHeaders();
      while (modifications.readRecord())
      {
              pub = modifications.get(PUB_TITLE);
              pISSN = modifications.get(PISSN_TITLE);
              eISSN = modifications.get(EISSN_TITLE);
              mMap.put(pISSN, pub);
              mMap.put(eISSN, pub);
              
      }
      modifications.close();
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
    }
          
  }

  
  /* gets a return from the map on the key but ensures the return is never an empty string */
  static String getFromModMap(Map<String,String> mMap, String key) {
    String retVal = mMap.get(key);
    if ( !(retVal == null) && !(retVal.isEmpty()) ) return retVal;
    else return null;
  }
  
  /**
   * This method reads two files specified by command line arguments.
   * The first file contains comma-separated lines of fields including 
   * but not limited to
   * issn, eissn, and publisher. The second file contains comma-separated
   * lines of fields containing pissn, eissn, and corresponding correct
   * publisher names. The output file, which is the third command line
   * argument, is a copy of the input file with
   * gensym publisher names replaced by correct publisher names, using
   * the issn and eissn as keys.
   * The output file is overwritten if it already exists
   * 
   * @param args the names of the input and modification files and the output file
   * @throws IOException if an IO error occurs.
   */

  static public void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("Need three file names - <origKbart>.csv <modPubs>.csv <newKbart>.csv [note output file will be overwritten]");
      System.exit(1);
    }
    
    /* If output file already exists, get rid of it */
    File f = new File(args[2]);
    if (f.exists() ) {
      f.delete();    
    }

    // build a map if issn/eissn  to publisher title
    Map<String,String> modMap = new HashMap<String,String>();
    createModMap(args[1], modMap);

    String origPub;
    String origPISSN;
    String origEISSN;
    String modPub;
    String newPub;
    String values[];
    
    try {
      CsvReader origKbart = new CsvReader(args[0], ',', Charset.forName("utf-8"));
      CsvWriter newKbart = new CsvWriter(new FileWriter(args[2],true), ',');
      origKbart.readHeaders();
      int origPubIndex = origKbart.getIndex(PUB_TITLE); 

      /* write the headers in to the new output kbart file */
      newKbart.writeRecord(origKbart.getHeaders());
      while (origKbart.readRecord()) 
      {
        values = origKbart.getValues();
        origPub = origKbart.get(PUB_TITLE);
        newPub = origPub;
        if ( !(origPub == null) && (origPub.contains(UNKNOWN_PUB)) ) {
          /* see if you can substitute something from the modMap */
          origPISSN = origKbart.get(PISSN_TITLE);
          origEISSN = origKbart.get(EISSN_TITLE);
          /* if the original kbart record has a print issn, see if there is a match */
          modPub = null;
          if ( !(origPISSN.isEmpty()) ) modPub = getFromModMap(modMap, origPISSN);
          if (modPub == null) {
            /* no pissn match, so if the original kbart record has an eissn, try for that match */
            if ( !(origEISSN.isEmpty()) ) modPub = getFromModMap(modMap, origEISSN);
          }
          if (modPub != null) {
            newPub = modPub;
          } 
          if ( !(newPub.equals(origPub)) ) {
            /*modify this one item in the values array*/
            values[origPubIndex] = newPub;
          }
        }
        newKbart.writeRecord(values);
      }
      /* done with all origKbart records, so finish, write output and close readers */
      origKbart.close();
      newKbart.close();
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
    }
  }
}
