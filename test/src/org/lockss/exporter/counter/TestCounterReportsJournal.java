/*
 * $Id$
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
 * Test class for org.lockss.exporter.counter.CounterReportsJournal.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
package org.lockss.exporter.counter;

import org.lockss.exporter.counter.CounterReportsJournal;
import org.lockss.test.LockssTestCase;

public class TestCounterReportsJournal extends LockssTestCase {
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Tests the successful creation of a journal.
   * 
   * @throws Exception
   */
  public void testCreation() throws Exception {
    CounterReportsJournal journal =
	new CounterReportsJournal("Journal1", "Publisher1", null, "02468",
	    null, "1234-5678", "9876-5432");

    assertEquals("Journal1", journal.getName());
    assertEquals("Publisher1", journal.getPublisherName());
    assertEquals("02468", journal.getDoi());
    assertEquals("1234-5678", journal.getPrintIssn());
    assertEquals("9876-5432", journal.getOnlineIssn());
  }

  /**
   * Tests the failure to create a journal with no title.
   * 
   * @throws Exception
   */
  public void runTestNoTitleFailure() throws Exception {
    CounterReportsJournal journal = null;
    try {
      journal =
	  new CounterReportsJournal(null, "Publisher1", null, null, null,
	      "1234-5678", "9876-5432");
      fail("Invalid null journal title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(journal);

    try {
      journal =
	  new CounterReportsJournal("", "Publisher1", null, null, null,
	      "1234-5678", "9876-5432");
      fail("Invalid empty journal title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(journal);

    try {
      journal =
	  new CounterReportsJournal(" ", "Publisher1", null, null, null,
	      "1234-5678", "9876-5432");
      fail("Invalid empty journal title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(journal);
  }
}
