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

/**
 * This test class tests the functionality we assume Castor has
 */

//NOTE: all tests are commented out because 0.9.4.1 can't do any of this

package org.lockss.state;

import java.util.*;
import java.net.*;
import java.io.*;
import org.exolab.castor.xml.*;
import org.exolab.castor.mapping.*;

import org.lockss.test.*;

public class TestCastor extends LockssTestCase {
  String tempDirPath;

  static final String FILE_NAME = "testFile";

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
  }

  protected Object marshalAndUnmarshal(Object obj) throws Exception {
    return marshalAndUnmarshal(obj, (Mapping)null);
  }

  protected Object marshalAndUnmarshal(Object obj, String mapping)
      throws Exception {
    return marshalAndUnmarshal(obj, loadMappingFile(mapping));
  }

  protected Object marshalAndUnmarshal(Object obj, Mapping mapping)
      throws Exception {
    String testFile = tempDirPath + FILE_NAME;
    Writer writer = null;
    try {
      writer = new FileWriter(testFile);
      Marshaller marshaller = new Marshaller(writer);
      if (mapping != null) {
	marshaller.setMapping(mapping);
      }
      marshaller.marshal(obj);
    } finally {
      writer.close();
    }

    Reader reader = null;
    try {
      reader = new FileReader(testFile);
      Unmarshaller unmarshaller = new Unmarshaller(obj.getClass());
      if (mapping != null) {
	unmarshaller.setMapping(mapping);
      }
      return unmarshaller.unmarshal(reader);
    } finally {
      reader.close();
    }
  }

  protected void assertMarshalUnmarshalGivesEqualObj(Object obj1)
      throws Exception {
    Object obj2 = marshalAndUnmarshal(obj1);
    assertNotSame(obj1, obj2);
    assertEquals(obj1, obj2);
  }

  public void testBasicTypes() throws Exception {
    //broken in 0.9.4.1
//     assertMarshalUnmarshalGivesEqualObj(new Integer(5));
//     assertMarshalUnmarshalGivesEqualObj(new Boolean(false));
//     assertMarshalUnmarshalGivesEqualObj(new Boolean(true));
//     assertMarshalUnmarshalGivesEqualObj(new Long(5));
//     assertMarshalUnmarshalGivesEqualObj(new Float(5.0));
//     assertMarshalUnmarshalGivesEqualObj(new Double(5.0));
    //broken in 0.9.5.4
//     assertMarshalUnmarshalGivesEqualObj("Test string");
  }

    //broken in 0.9.4.1
//   public void testArrayList() throws Exception {
//     ArrayList list = new ArrayList();
//     list.add("Blah");
//     list.add("Blah2");
//     assertMarshalUnmarshalGivesEqualObj(list);
//   }
    //broken in 0.9.4.1
//   public void testArrayListHetero() throws Exception {
//     ArrayList list = new ArrayList();
//     list.add("Blah");
//     list.add(new Integer(2));
//     assertMarshalUnmarshalGivesEqualObj(list);
//   }

  //broken in 0.9.5.4
  /*
  public void testHashMap() throws Exception {
    HashMap map = new HashMap();
    map.put("Blah", "Not Blah");
    map.put("Blah2", "Is Blah");
    assertMarshalUnmarshalGivesEqualObj(map);
  }
  */



  /**
   * Loads a Mapping from a fileName.
   * @param fileName the mapping file name
   * @return Mapping the loaded Mapping
   */
  Mapping loadMappingFile(String fileName) throws Exception {
    URL mappingLoc = getResource(fileName);

    Mapping mapping = new Mapping();
    mapping.loadMapping(mappingLoc);
    return mapping;
  }
}
