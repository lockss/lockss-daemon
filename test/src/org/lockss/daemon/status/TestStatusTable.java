/*
 * $Id: TestStatusTable.java,v 1.3 2003-03-27 23:52:53 tal Exp $
 */

/*
 Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon.status;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;


public class TestStatusTable extends LockssTestCase {

  public void testEmbeddedValue() {
    Integer val = new Integer(3);
    StatusTable.DisplayedValue dval = new StatusTable.DisplayedValue(val);
    StatusTable.Reference rval = new StatusTable.Reference(val, "foo", "bar");
    // should be able to embed DisplayedValue in Reference
    new StatusTable.Reference(dval, "foo", "bar");

    try {
      new StatusTable.DisplayedValue(rval);
      fail("Shouldn't be able to embed Reference in DisplayedValue");
    } catch (IllegalArgumentException e) {
    }
    try {
      new StatusTable.DisplayedValue(dval);
      fail("Shouldn't be able to embed DisplayedValue in DisplayedValue");
    } catch (IllegalArgumentException e) {
    }
    try {
      new StatusTable.Reference(rval, "foo", "bar");
      fail("Shouldn't be able to embed Reference in Reference");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testGetActualValue() {
    Integer val = new Integer(3);
    StatusTable.DisplayedValue dval = new StatusTable.DisplayedValue(val);
    StatusTable.Reference rval = new StatusTable.Reference(val, "foo", "bar");
    StatusTable.Reference rdval = new StatusTable.Reference(dval,
							    "foo", "bar");
    assertEquals(val, StatusTable.getActualValue(val));
    assertEquals(val, StatusTable.getActualValue(dval));
    assertEquals(val, StatusTable.getActualValue(rval));
    assertEquals(val, StatusTable.getActualValue(rdval));
  }

//   public void testReferenceCompareTo() {
//     StatusTable.Reference ref = new StatusTable.Reference("C", "blah", null);
//     assertEquals(0, 
// 		 ref.compareTo(new StatusTable.Reference("C", "blah", null)));
//     assertEquals(-1, 
// 		 ref.compareTo(new StatusTable.Reference("D", "blah", null)));
//     assertEquals(1, 
// 		 ref.compareTo(new StatusTable.Reference("B", "blah", null)));
//   }

  public void testReferenceEquals() {
    StatusTable.Reference ref = new StatusTable.Reference("C", "blah", null);
    
    assertFalse(ref.equals("blah"));

    assertTrue(ref.equals(new StatusTable.Reference("C", "blah", null)));

    assertFalse(ref.equals(new StatusTable.Reference("D", "blah", null)));

    assertFalse(ref.equals(new StatusTable.Reference("C", "blah2", null)));

    assertFalse(ref.equals(new StatusTable.Reference("C", "blah", "key")));

    ref = new StatusTable.Reference("C", "blah", "key1");

    assertTrue(ref.equals(new StatusTable.Reference("C", "blah", "key1")));

    assertFalse(ref.equals(new StatusTable.Reference("C", "blah", "key")));
  }

}
