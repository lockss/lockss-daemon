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

import org.lockss.pdf.PdfTokenStreamWorker.Direction;
import org.lockss.test.LockssTestCase;

public class TestPdfTokenStreamWorker extends LockssTestCase {

  public abstract class TwoWayStateMachineWorker extends PdfTokenStreamWorker {

    protected int state;
    
    public TwoWayStateMachineWorker(Direction direction) {
      super(direction);
    }

    public void doOperand(int relativeIndex) {
      int absoluteIndex = getIndex() - getOperands().size() + relativeIndex;
      PdfToken expected = lot.get(absoluteIndex);
      /*
       * The following two assertions negate the need for all the
       * subsequent assertions, but it is not a requirement that the
       * references be identical. If one day this assumption is
       * broken, the test will fail and we can decide if it matters.
       */
      assertSame(expected, getTokens().get(absoluteIndex));
      assertSame(expected, getOperands().get(relativeIndex));
      PdfToken actual = getOperands().get(relativeIndex);
      assertFalse(actual.isOperator());
      assertEquals(expected.isArray(), actual.isArray());
      assertEquals(expected.isBoolean(), actual.isBoolean());
      assertEquals(expected.isDictionary(), actual.isDictionary());
      assertEquals(expected.isFloat(), actual.isFloat());
      assertEquals(expected.isInteger(), actual.isInteger());
      assertEquals(expected.isName(), actual.isName());
      assertEquals(expected.isNull(), actual.isNull());
      assertEquals(expected.isOperator(), actual.isOperator());
      assertEquals(expected.isString(), actual.isString());
      if (expected.isBoolean()) {
        assertEquals(expected.getBoolean(), actual.getBoolean());
        return;
      }
      if (expected.isFloat()) {
        assertEquals(expected.getFloat(), actual.getFloat());
        return;
      }
      if (expected.isInteger()) {
        assertEquals(expected.getInteger(), actual.getInteger());
        return;
      }
      fail("Unexpected token type");
    }

    public void doOperands(int expectedOperands) {
      assertEquals(expectedOperands, getOperands().size());
      for (int relativeIndex = 0 ; relativeIndex < expectedOperands ; ++relativeIndex) {
        doOperand(relativeIndex);
      }
    }
    
    public void doOperator(int expectedIndex) {
      assertEquals(expectedIndex, getIndex());
      PdfToken expectedOperator = lot.get(getIndex());
      /*
       * The following two assertions negate the need for all the
       * subsequent assertions, but it is not a requirement that the
       * references be identical. If one day this assumption is
       * broken, the test will fail and we can decide if it matters.
       */
      assertSame(expectedOperator, getTokens().get(getIndex()));
      assertSame(expectedOperator, getOperator());
      assertTrue(getOperator().isOperator());
      assertEquals(expectedOperator.getOperator(), getOperator().getOperator());
      assertEquals(expectedOperator.getOperator(), getOpcode());
    }
    
    public abstract int getNumberOfOperands();
    
    public abstract int getNumberOfStates();
    
    public abstract int getOperatorIndex();
    
    @Override
    public void operatorCallback() throws PdfException {
      doOperator(getOperatorIndex());
      doOperands(getNumberOfOperands());
      updateState();
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      state = (getDirection() == Direction.FORWARD) ? 0 : getNumberOfStates() - 1;
    }
    
    protected void updateState() {
      state = state + ((getDirection() == Direction.FORWARD) ? 1 : -1);
    }
    
    @Override
    public void process(PdfTokenStream pdfTokenStream) throws PdfException {
      super.process(pdfTokenStream);
      assertEquals((getDirection() == Direction.FORWARD) ? getNumberOfStates() : -1, state);
    }

  }
  
  private List<PdfToken> lot;
  
  private PdfTokenFactory tf;
  
  public void setUp() {
    this.tf = new MockPdfTokenFactory();
    this.lot = new ArrayList<PdfToken>();
  }
  
  public void testStartsWithNoneEndsWithNone() throws PdfException {
    /* 0 */ lot.add(tf.makeOperator("opA"));
    /* 1 */ lot.add(tf.makeFloat(1.0f));
    /* 2 */ lot.add(tf.makeFloat(2.0f));
    /* 3 */ lot.add(tf.makeOperator("float2"));
    /* 4 */ lot.add(tf.makeOperator("opB"));
    
    class MyPdfTokenStreamWorker extends TwoWayStateMachineWorker {
    
      public MyPdfTokenStreamWorker(Direction direction) {
        super(direction);
      }
      
      @Override
      public int getNumberOfOperands() {
        switch (state) {
          case 0: return 0;
          case 1: return 2;
          case 2: return 0;
          default: return -1234;
        }
      }
      
      @Override
      public int getNumberOfStates() {
        return 3;
      }
      
      @Override
      public int getOperatorIndex() {
        switch (state) {
          case 0: return 0;
          case 1: return 3;
          case 2: return 4;
          default: return -1234;
        }
      }
      
    }

    TwoWayStateMachineWorker forward = new MyPdfTokenStreamWorker(Direction.FORWARD);
    forward.process(lot, tf);
    TwoWayStateMachineWorker backward = new MyPdfTokenStreamWorker(Direction.BACKWARD);
    backward.process(lot, tf);
  }
  
  public void testStartsWithNoneEndsWithOperands() throws PdfException {
    /* 0 */ lot.add(tf.makeOperator("opA"));
    /* 1 */ lot.add(tf.makeFloat(1.0f));
    /* 2 */ lot.add(tf.makeFloat(2.0f));
    /* 3 */ lot.add(tf.makeOperator("float2"));
    /* 4 */ lot.add(tf.makeInteger(1L));
    /* 5 */ lot.add(tf.makeInteger(2L));
    /* 6 */ lot.add(tf.makeInteger(3L));
    /* 7 */ lot.add(tf.makeOperator("integer3"));
    
    class MyPdfTokenStreamWorker extends TwoWayStateMachineWorker {
            
      public MyPdfTokenStreamWorker(Direction direction) {
        super(direction);
      }
      
      @Override
      public int getNumberOfOperands() {
        switch (state) {
          case 0: return 0;
          case 1: return 2;
          case 2: return 3;
          default: return -1234;
        }
      }
      
      @Override
      public int getNumberOfStates() {
        return 3;
      }
      
      @Override
      public int getOperatorIndex() {
        switch (state) {
          case 0: return 0;
          case 1: return 3;
          case 2: return 7;
          default: return -1234;
        }
      }
      
    }

    TwoWayStateMachineWorker forward = new MyPdfTokenStreamWorker(Direction.FORWARD);
    forward.process(lot, tf);
    TwoWayStateMachineWorker backward = new MyPdfTokenStreamWorker(Direction.BACKWARD);
    backward.process(lot, tf);
  }

  public void testStartsWithOperandsEndsWithNone() throws PdfException {
    /* 00 */ lot.add(tf.makeBoolean(true));
    /* 01 */ lot.add(tf.makeOperator("boolean1"));
    /* 02 */ lot.add(tf.makeFloat(1.0f));
    /* 03 */ lot.add(tf.makeFloat(2.0f));
    /* 04 */ lot.add(tf.makeOperator("float2"));
    /* 05 */ lot.add(tf.makeOperator("opA"));
    
    class MyPdfTokenStreamWorker extends TwoWayStateMachineWorker {
            
      public MyPdfTokenStreamWorker(Direction direction) {
        super(direction);
      }
      
      @Override
      public int getNumberOfOperands() {
        switch (state) {
          case 0: return 1;
          case 1: return 2;
          case 2: return 0;
          default: return -1234;
        }
      }
      
      @Override
      public int getNumberOfStates() {
        return 3;
      }
      
      @Override
      public int getOperatorIndex() {
        switch (state) {
          case 0: return 1;
          case 1: return 4;
          case 2: return 5;
          default: return -1234;
        }
      }
      
    }

    TwoWayStateMachineWorker forward = new MyPdfTokenStreamWorker(Direction.FORWARD);
    forward.process(lot, tf);
    TwoWayStateMachineWorker backward = new MyPdfTokenStreamWorker(Direction.BACKWARD);
    backward.process(lot, tf);
  }
  
  public void testStartsWithOperandsEndsWithOperands() throws PdfException {
    /* 0 */ lot.add(tf.makeBoolean(true));
    /* 1 */ lot.add(tf.makeOperator("boolean1"));
    /* 2 */ lot.add(tf.makeFloat(1.0f));
    /* 3 */ lot.add(tf.makeFloat(2.0f));
    /* 4 */ lot.add(tf.makeOperator("float2"));
    /* 5 */ lot.add(tf.makeInteger(1L));
    /* 6 */ lot.add(tf.makeInteger(2L));
    /* 7 */ lot.add(tf.makeInteger(3L));
    /* 8 */ lot.add(tf.makeOperator("integer3"));
    
    class MyPdfTokenStreamWorker extends TwoWayStateMachineWorker {
            
      public MyPdfTokenStreamWorker(Direction direction) {
        super(direction);
      }
      
      @Override
      public int getNumberOfOperands() {
        switch (state) {
          case 0: return 1;
          case 1: return 2;
          case 2: return 3;
          default: return -1234;
        }
      }
      
      @Override
      public int getNumberOfStates() {
        return 3;
      }
      
      @Override
      public int getOperatorIndex() {
        switch (state) {
          case 0: return 1;
          case 1: return 4;
          case 2: return 8;
          default: return -1234;
        }
      }
      
    }

    TwoWayStateMachineWorker forward = new MyPdfTokenStreamWorker(Direction.FORWARD);
    forward.process(lot, tf);
    TwoWayStateMachineWorker backward = new MyPdfTokenStreamWorker(Direction.BACKWARD);
    backward.process(lot, tf);
  }
  
}
