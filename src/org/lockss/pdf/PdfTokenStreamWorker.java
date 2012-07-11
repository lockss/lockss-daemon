/*
 * $Id: PdfTokenStreamWorker.java,v 1.2 2012-07-11 23:53:38 thib_gc Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * <p>
 * A utility class that traverses a PDF token stream, either forward
 * or backward, and invokes a callback for each operator (and its
 * operands).
 * </p>
 * <p>
 * At the end of the transform, the token stream is left unchanged.
 * Only a call to the token stream's
 * {@link PdfTokenStream#setTokens(List)} method. As such, it is
 * typical for subclasses of this class to implement {@link Transform}
 * &lt;{@link PdfTokenStream}&gt; (or similar) to perform a
 * transformation based on the results of this worker.
 * </p>
 * <p>
 * Below is an example of a worker that determines if a string in the
 * token stream contains <code>"foo"</code>.
 * </p>
 * 
 * <pre>
class MyWorker extends PdfTokenStreamWorker() {
  
  boolean result;
  
  public void setUp() throws PdfException {
    result = false;
  }
  
  public void operatorCallback() throws PdfException {
    if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
        && tokens.get(index - 1).getString().contains("foo")) {
      result = true;
      stop();
    }
  }

}
MyWorker worker = new MyWorker();
worker.process(myPdfTokenStream);
if (worker.result) {
  // At least one string contains "foo"
}
</pre>
 * <p>
 * Supposing one wanted to write a transform that replaces the first
 * occurrence of <code>"foo"</code> by <code>"bar"</code> in the first
 * string that contains it in the token stream, the following would
 * not work.
 * </p>
<pre>
class BadWorker extends PdfTokenStreamWorker {
  
  boolean result;
  
  public void setUp() throws PdfException {
    result = false;
  }
  
  public void operatorCallback() throws PdfException {
    if (PdfOpcodes.SHOW_TEXT.equals(opcode)) {
      String operand = tokens.get(index - 1).getString();
      if (operand.contains("foo")) {
        tokens.set(index - 1, adapter.makeString(operand.replaceFirst("foo", "bar")));
        result = true;
        stop();
      }
    }
  }

}
BadWorker worker = new BadWorker();
worker.process(myPdfTokenStream);
// Nothing has changed, even if worker.result is true
</pre>
 * <p>
 * An idiom for the desired behavior is shown below.
 * </p>
<pre>
class MyWorkerTransform extends PdfTokenStreamWorker implements Transform<PdfTokenStream> {
  
  boolean result;
  
  public void setUp() throws PdfException {
    result = false;
  }
  
  public void operatorCallback() throws PdfException {
    if (PdfOpcodes.SHOW_TEXT.equals(opcode)) {
      String operand = tokens.get(index - 1).getString();
      if (operand.contains("foo")) {
        tokens.set(index - 1, adapter.makeString(operand.replaceFirst("foo", "bar")));
        result = true;
        stop();
      }
    }
  }
  
  public void transform(ArchivalUnit au, PdfTokenStream pdfTokenStream) throws PdfException {
    process(pdfTokenStream);
    if (result) {
      pdfTokenStream.setTokens(tokens);
    }
  }

}
MyWorkerTransform workerTransform = new MyWorkerTransform();
workerTransform.transform(myPdfTokenStream);
</pre>
 * <p>
 * Another idiom is shown below.
 * </p>
<pre>
class MyWorkerTransform2 extends PdfTokenStreamWorker implements Transform<PdfTokenStream> {
  
  boolean result;
  
  public void setUp() throws PdfException {
    result = false;
  }
  
  public void operatorCallback() throws PdfException {
    if (PdfOpcodes.SHOW_TEXT.equals(opcode)) {
      String operand = tokens.get(index - 1).getString();
      if (operand.contains("foo")) {
        tokens.set(index - 1, adapter.makeString(operand.replaceFirst("foo", "bar")));
        result = true;
        stop();
      }
    }
  }
  
  public void transform(ArchivalUnit au, PdfTokenStream pdfTokenStream) throws PdfException {
    pdfTokenStream.setTokens(tokens);
  }

}
MyWorkerTransform2 workerTransform = new MyWorkerTransform2();
workerTransform.process(myPdfTokenStream);
if (workerTransform.result) {
  workerTransform.transform(myPdfTokenStream);
}
</pre>
 * <p>
 * This utility class works on a single PDF token stream at a time.
 * It can be re-used but its behavior is undefined if used
 * concurrently by multiple threads.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public abstract class PdfTokenStreamWorker {

  /**
   * <p>
   * The direction in which this worker traverses a PDF token stream.
   * </p>
   * @author Thib Guicherd-Callin
   * @since 1.56
   */
  public enum Direction {
    BACKWARD,
    FORWARD
  }
  
  /**
   * <p>
   * A PDF adapter for the token stream being processed.
   * </p>
   * @since 1.56
   */
  protected PdfTokenFactory adapter;
  
  /**
   * <p>
   * The index within {@link #tokens} of the current operator.
   * </p>
   * @since 1.56
   */
  protected int index;
  
  /**
   * <p>
   * Convenience variable holding the result of calling
   * {@link PdfToken#getOperator()} on the current operator.
   * </p>
   * @since 1.56
   */
  protected String opcode;

  /**
   * <p>
   * Zero or more operands for the current operator.
   * </p>
   * @since 1.56
   */
  protected List<PdfToken> operands;
  
  /**
   * <p>
   * The current operator.
   * </p>
   * @since 1.56
   */
  protected PdfToken operator;
  
  /**
   * <p>
   * The token sequence being operated upon by this worker.
   * </p>
   * @since 1.56
   */
  protected List<PdfToken> tokens;

  /**
   * <p>
   * Whether to keep traversing this token stream.
   * </p>
   * @since 1.56
   * @see #stop()
   */
  private boolean continueFlag;

  /**
   * <p>
   * The direction in which the token stream is being traversed.
   * </p>
   * @since 1.56
   * @see Direction
   */
  private Direction direction;

  /**
   * <p>
   * Creates a worker that traverses a token stream forward.
   * </p>
   * @since 1.56
   */
  public PdfTokenStreamWorker() {
    this(Direction.FORWARD);
  }

  /**
   * <p>
   * Creates a worker that traverses a token stream in the given
   * direction.
   * </p>
   * @param direction A direction.
   * @since 1.56
   * @see Direction
   */
  public PdfTokenStreamWorker(Direction direction) {
    this.direction = direction;
  }
  
  /**
   * <p>
   * Callback when an operator is encountered. The following protected
   * variables are accessible to subclasses:
   * </p>
   * <ul>
   * <li>{@link #adapter}</li>
   * <li>{@link #index}</li>
   * <li>{@link #opcode}</li>
   * <li>{@link #operands}</li>
   * <li>{@link #operator}</li>
   * <li>{@link #tokens}</li>
   * </ul>
   * @throws PdfException If PDF processing fails.
   */
  public abstract void operatorCallback() throws PdfException;
  
  /**
   * <p>
   * Processes the given PDF token stream after calling
   * {@link #setUp()}.
   * </p>
   * @param pdfTokenStream A PDF token stream.
   * @throws PdfException If PDF processing fails.
   */
  public void process(PdfTokenStream pdfTokenStream) throws PdfException {
    adapter = pdfTokenStream.getTokenFactory();
    index = -1;
    opcode = null;
    operands = new ArrayList<PdfToken>();
    operator = null;
    tokens = pdfTokenStream.getTokens();
    continueFlag = true;
    setUp();
    if (direction.equals(Direction.FORWARD)) {
      processForward(pdfTokenStream);
    }
    else {
      processBackward(pdfTokenStream);
    }
  }
  
  /**
   * <p>
   * Allows subclasses to initialize whatever variables and data
   * structures are required for processing the current token stream.
   * </p>
   * @throws PdfException If PDF processing fails.
   */
  public abstract void setUp() throws PdfException;
  
  /**
   * <p>
   * Requests that processing of the token stream end prematurely.
   * No calls to {@link #operatorCallback()} will be issued unless
   * {@link #process(PdfTokenStream)} is invoked again.
   * </p>
   * @since 1.56
   */
  public void stop() {
    continueFlag = false;
  }

  /**
   * <p>
   * Processes a token stream backward.
   * </p>
   * @param pdfTokenStream The PDF token stream being processed.
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  protected void processBackward(PdfTokenStream pdfTokenStream) throws PdfException {
    int end = tokens.size() - 1;
    while (end >= 0 && continueFlag) {
      operands.clear();
      operator = tokens.get(end);
      opcode = operator.getOperator();
      index = end;
      int beginMinusOne = end - 1;
      while (beginMinusOne >= 0 && !tokens.get(beginMinusOne).isOperator()) {
        --beginMinusOne;
      }
      operands.addAll(tokens.subList(beginMinusOne + 1, end));
      operatorCallback();
      end = beginMinusOne;
    }
  }
  
  /**
   * <p>
   * Processes a token stream forward.
   * </p>
   * @param pdfTokenStream The PDF token stream being processed.
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  protected void processForward(PdfTokenStream pdfTokenStream) throws PdfException {
    int begin = 0;
    while (begin < tokens.size() && continueFlag) {
      operands.clear();
      int end = begin;
      while (end < tokens.size() && !tokens.get(end).isOperator()) {
        ++end;
      }
      operands.addAll(tokens.subList(begin, end));
      operator = tokens.get(end);
      opcode = operator.getOperator();
      index = end;
      operatorCallback();
      begin = end;
    }
  }

}
