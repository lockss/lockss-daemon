/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.pdf;

import java.util.*;

import org.lockss.test.LockssTestCase;

public class TestPdfTokenStreamStateMachine extends LockssTestCase {

  public void testStateMachine() throws Exception {
    /*
     * LOCAL CLASS
     */
    class MyPdfTokenStreamStateMachine extends PdfTokenStreamStateMachine {

      protected int stateCounter;
      
      protected void doStateTest() {
        assertEquals(stateCounter, state);
        assertEquals("op" + stateCounter, getOpcode());
        ++stateCounter;
        ++state;
        assertFalse(result);
      }
      
      @Override
      public void setUp() throws PdfException {
        super.setUp();
        stateCounter = 0;
        assertEquals(0, getState());
        assertFalse(getResult());
        assertEquals(-1, getBegin());
        assertEquals(-1, getEnd());
      }
      
      @Override
      public void state0() throws PdfException {
        doStateTest();
      }

      @Override
      public void state1() throws PdfException {
        doStateTest();
      }

      @Override
      public void state2() throws PdfException {
        doStateTest();
      }

      @Override
      public void state3() throws PdfException {
        doStateTest();
      }

      @Override
      public void state4() throws PdfException {
        doStateTest();
      }

      @Override
      public void state5() throws PdfException {
        doStateTest();
      }

      @Override
      public void state6() throws PdfException {
        doStateTest();
      }

      @Override
      public void state7() throws PdfException {
        doStateTest();
      }

      @Override
      public void state8() throws PdfException {
        doStateTest();
      }

      @Override
      public void state9() throws PdfException {
        doStateTest();
        setResult(true);
        setBegin(123);
        setEnd(456);
        stop();
      }

    }
    
    MockPdfTokenFactory tf = new MockPdfTokenFactory();
    MyPdfTokenStreamStateMachine sm = new MyPdfTokenStreamStateMachine();
    sm.process(Arrays.asList(tf.makeOperator("op0"),
                             tf.makeOperator("op1"),
                             tf.makeOperator("op2"),
                             tf.makeOperator("op3"),
                             tf.makeOperator("op4"),
                             tf.makeOperator("op5"),
                             tf.makeOperator("op6"),
                             tf.makeOperator("op7"),
                             tf.makeOperator("op8"),
                             tf.makeOperator("op9")),
               tf);
    assertEquals(10, sm.stateCounter);
    assertTrue(sm.getResult());
    assertEquals(123, sm.getBegin());
    assertEquals(456, sm.getEnd());
  }
  
}
