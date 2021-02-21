/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.pdf;

import java.util.*;
import java.util.regex.*;

import org.apache.commons.lang3.ObjectUtils;

/**
 * <p>
 * A utility class that traverses a PDF token stream, either forward
 * or backward, and invokes a callback for each operator (and its
 * operands).
 * </p>
 * <p>
 * At the end of the transform, the token stream is left unchanged.
 * Only a call to {@link PdfTokenStream#setTokens(List)} alters the
 * stream's sequence of tokens. As such, it is typical for subclasses
 * of this class to implement {@link Transform}&lt;{@link PdfTokenStream}&gt;
 * (or similar) to perform a transformation based on the results of
 * this worker.
 * </p>
 * <p>
 * Below is an example of a worker that determines if a string in the
 * token stream contains <code>"foo"</code>.
 * </p>
<pre>
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
 * An idiom for the desired behavior is shown below. In this idiom,
 * the transform invokes the worker, and if the worker signals it has
 * altered its internal list of tokens, sets the stream's tokens. The
 * caller views this as a single operation in the form of a
 * transform.
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
 * Another idiom is shown below. In this idiom, the caller invokes the
 * worker, then if the worker signals it has altered its internal list
 * of tokens, invokes the transform, which sets the stream's tokens.
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
 * The former is easier to view as a single operation, and keeps all
 * knowledge of the worker-transform interaction internal. The latter
 * is more flexible in applying transformations conditionally, for
 * instance if the worker-transform is re-used.
 * </p>
 * <p>
 * This utility class can be re-used but is not thread-safe.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public abstract class PdfTokenStreamWorker {

  /**
   * <p>
   * The direction in which this worker traverses a PDF token stream.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @since 1.56
   * @deprecated As of 1.76, {@link PdfTokenStreamWorker} is becoming
   *             {@link PdfOperandsAndOperator}-based and is always "forward".
   */
  @Deprecated
  public enum Direction {

    /**
     * @since 1.56
     * @deprecated As of 1.76, {@link PdfTokenStreamWorker} is becoming
     *             {@link PdfOperandsAndOperator}-based and is always "forward".
     */
    @Deprecated
    BACKWARD,
    
    /**
     * @since 1.56
     * @deprecated As of 1.76, {@link PdfTokenStreamWorker} is becoming
     *             {@link PdfOperandsAndOperator}-based and is always "forward".
     */
    @Deprecated
    FORWARD
    
  }
  
  /**
   * <p>
   * Whether to keep traversing this token stream.
   * </p>
   * 
   * @since 1.56
   * @see #stop()
   */
  private boolean continueFlag;
  
  /**
   * <p>
   * This worker's direction.
   * </p>
   * 
   * @since 1.56
   * @deprecated As of 1.76, {@link PdfTokenStreamWorker} is becoming
   *             {@link PdfOperandsAndOperator}-based and is always "forward".
   * @see Direction
   */
  @Deprecated
  private final Direction direction;

  /**
   * <p>
   * The current PDF token factory.
   * </p>
   * 
   * @since 1.56.3
   */
  private PdfTokenFactory factory;
  
  /**
   * <p>
   * The index of the current operator.
   * </p>
   * 
   * @since 1.56
   */
  private int index;
  
  /**
   * <p>
   * The current opcode.
   * </p>
   * 
   * @since 1.56
   */
  private String opcode;

  /**
   * <p>
   * The current operands.
   * </p>
   * 
   * @since 1.56
   */
  private List<? extends PdfToken> operands;

  /**
   * <p>
   * The current operands, in old FORWARD/BACKWARD mode.
   * </p>
   * 
   * @since 1.76
   * @deprecated Shift away from this mode after 1.76.
   */
  @Deprecated
  private List<PdfToken> operands_old;

  /**
   * <p>
   * The current operator.
   * </p>
   * 
   * @since 1.56
   */
  private PdfToken operator;

  /**
   * <p>
   * The current token sequence.
   * </p>
   * 
   * @since 1.56
   * @deprecated As of 1.76, {@link PdfTokenStreamWorker} is becoming
   *             {@link PdfOperandsAndOperator}-based and this is no longer used.
   */
  @Deprecated
  private List<PdfToken> tokens;

  /**
   * <p>
   * Creates a worker that traverses a token stream forward.
   * </p>
   * 
   * @since 1.56
   */
  public PdfTokenStreamWorker() {
    this(Direction.FORWARD);
  }

  /**
   * <p>
   * Creates a worker that traverses a token stream in the given direction.
   * </p>
   * 
   * @param direction
   *          A direction.
   * @since 1.56
   * @deprecated As of 1.76, {@link PdfTokenStreamWorker} is becoming
   *             {@link PdfOperandsAndOperator}-based and is always "forward".
   * @see Direction
   */
  @Deprecated
  public PdfTokenStreamWorker(Direction direction) {
    this.direction = direction;
  }
  
  /**
   * <p>
   * Callback when an operator is encountered. State is accessible to subclasses
   * via the following methods:
   * </p>
   * <ul>
   * <li>{@link #getDirection()}</li>
   * <li>{@link #getFactory()}</li>
   * <li>{@link #getIndex()}</li>
   * <li>{@link #getOpcode()}</li>
   * <li>{@link #getOperands()}</li>
   * <li>{@link #getOperator()}</li>
   * <li>{@link #getTokens()}</li>
   * </ul>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  public abstract void operatorCallback() throws PdfException;
  
  /**
   * <p>
   * Processes the given PDF token stream's tokens with
   * {@link #process(List, PdfTokenFactory)}.
   * </p>
   * 
   * @param pdfTokenStream
   *          A PDF token stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @deprecated As of 1.76, {@link PdfTokenStreamWorker} is becoming
   *             {@link PdfOperandsAndOperator}-based and is always "forward".
   *             Use instead. //FIXME
   * @see #process(List, PdfTokenFactory)
   * 
   */
  public void process(PdfTokenStream pdfTokenStream) throws PdfException {
    process(pdfTokenStream.getTokens(), PdfUtil.getTokenFactory(pdfTokenStream));
  }
  
  /**
   * <p>
   * Processes the given list of PDF tokens after calling {@link #setUp()},
   * using the given PDF token factory.
   * </p>
   * 
   * @param tokens
   *          A list of PDF tokens.
   * @param factory
   *          A PDF token factory.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.62
   * @deprecated FIXME
   */
  @Deprecated // FIXME
  public void process(List<PdfToken> tokens,
                      PdfTokenFactory factory)
      throws PdfException {
    this.tokens = tokens;
    this.factory = factory;
    index = -1;
    operator = null;
    operands_old = new ArrayList<PdfToken>();
    continueFlag = true;
    setUp();
    switch (getDirection()) {
      case FORWARD: {
        processForward();
      } break;
      case BACKWARD: {
        processBackward();
      } break;
      default: {
        throw new IllegalStateException("Illegal direction: " + getDirection());
      }
    }
  }
  
  /**
   * 
   * @param oaoIterator
   * @param factory
   * @throws PdfException
   * @see 1.76
   */
  public void processOperandsAndOperatorIterator(Iterator<? extends PdfOperandsAndOperator<? extends PdfToken>> oaoIterator,
                                                 PdfTokenFactory factory)
      throws PdfException {
    tokens = null;
    this.factory = factory;
    index = -1;
    continueFlag = true;
    setUp();
    while (oaoIterator.hasNext() && continueFlag) {
      PdfOperandsAndOperator<? extends PdfToken> oao = oaoIterator.next();
      operands = oao.getOperands();
      operator = oao.getOperator();
      opcode = operator.getOperator();
      index += operands.size() + 1;
      operatorCallback();
    }
  }

  /**
   * 
   * @param oaos
   * @param factory
   * @throws PdfException
   * @since 1.76
   * @see #processOperandsAndOperatorIterator(Iterator, PdfTokenFactory)
   */
  public void processOperandsAndOperatorList(List<? extends PdfOperandsAndOperator<? extends PdfToken>> oaos,
                                             PdfTokenFactory factory)
      throws PdfException {
    processOperandsAndOperatorIterator(oaos.iterator(), factory);
  }

  /**
   * 
   * @throws PdfException
   * @since 1.76
   */
  public void processPdfTokenStream(PdfTokenStream pdfTokenStream) throws PdfException {
    processOperandsAndOperatorIterator(pdfTokenStream.getOperandsAndOperatorIterator(),
                                       PdfUtil.getTokenFactory(pdfTokenStream));
  }
  
  /**
   * 
   * @param tokens
   * @param factory
   * @throws PdfException
   * @since 1.76
   */
  public void processTokenIterator(Iterator<? extends PdfToken> tokenIter,
                                   PdfTokenFactory factory)
      throws PdfException {
    processOperandsAndOperatorIterator(PdfUtil.toOperandsAndOperatorIterator(tokenIter), factory);
  }
  
  /**
   * <p>
   * Processes the given list of PDF tokens after calling {@link #setUp()},
   * using the given PDF token factory.
   * </p>
   * 
   * @param tokens
   *          A list of PDF tokens.
   * @param factory
   *          A PDF token factory.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #processTokenIterator(Iterator, PdfTokenFactory)
   */
  public void processTokenList(List<? extends PdfToken> tokens,
                               PdfTokenFactory factory)
      throws PdfException {
    processTokenIterator(tokens.iterator(), factory);
  }
  
  /**
   * <p>
   * Initializes whatever variables and data structures are required for
   * processing the current token stream. Subclasses that do not call
   * <code>super.setUp()</code> risk being incorrectly initialized.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   */
  public void setUp() throws PdfException {
    // This method intentionally left blank
  }
  
  /**
   * <p>
   * Gets the current direction.
   * </p>
   * 
   * @return the current direction.
   * @since 1.56
   * @see Direction
   */
  protected Direction getDirection() {
    if (tokens == null) {
      throw new UnsupportedOperationException("Operation not available in iterator mode");
    }
    return direction;
  }

  /**
   * <p>
   * Gets the current PDF token factory. <b>Renamed {@link #getTokenFactory()}
   * in 1.60.</b>
   * </p>
   * 
   * @return the current PDF token factory.
   * @since 1.56
   * @deprecated As of 1.60, use {@link #getTokenFactory()} instead.
   */
  @Deprecated
  protected PdfTokenFactory getFactory() {
    return getTokenFactory();
  }

  /**
   * <p>
   * Gets the index of the operator for which {@link #operatorCallback()} is
   * being called.
   * </p>
   * 
   * @return The index of the current operator.
   * @since 1.56.3
   */
  protected int getIndex() {
    return index;
  }

  /**
   * <p>
   * Gets the opcode for which {@link #operatorCallback()} is being called.
   * Equivalent to <code>getOperator().getOperator()</code>.
   * </p>
   * 
   * @return The current opcode.
   * @since 1.56.3
   */
  protected String getOpcode() {
    return opcode;
  }

  /**
   * <p>
   * Retrieves the operand of a single-operand operator (at index
   * <code>getIndex() - 1</code>).
   * </p>
   * 
   * @return The current operator's single operand (or null if the operator has
   * another number of operands).
   * @since 1.67
   */
  protected PdfToken getSingleOperand() {
    return (operands.size() == 1) ? operands.get(0) : null;
  }

  /**
   * <p>
   * Gets the operands that go with the operator for which
   * {@link #operatorCallback()} is being called.
   * </p>
   * 
   * @return The current operator's operands (possibly an empty list).
   * @since 1.56.3
   */
  protected List<? extends PdfToken> getOperands() {
    return (tokens == null) ? operands : operands_old; // FIXME
  }

  /**
   * <p>
   * Gets the operator token for which {@link #operatorCallback()} is being
   * called.
   * </p>
   * 
   * @return The current operator.
   * @since 1.56.3
   */
  protected PdfToken getOperator() {
    return operator;
  }

  /**
   * <p>
   * Gets the current PDF token factory.
   * </p>
   * 
   * @return the current PDF token factory.
   * @since 1.60
   */
  protected PdfTokenFactory getTokenFactory() {
    return factory;
  }

  /**
   * <p>
   * Gets the internal list of all tokens currently being processed by
   * {@link #process(PdfTokenStream)}. Altering it is okay if it does not
   * interfere with the internal loop skipping from operator to operator,
   * otherwise the behavior of the worker becomes undefined.
   * </p>
   * 
   * @return The current list of all tokens.
   * @since 1.56.3
   * @deprecated FIXME
   */
  @Deprecated // FIXME
  protected List<? extends PdfToken> getTokens() {
    if (tokens == null) {
      throw new UnsupportedOperationException("Operation not available in iterator mode");
    }
    return tokens;
  }

  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isBeginInlineImage(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#BEGIN_INLINE_IMAGE}
   * @since 1.76
   */
  protected boolean isBeginInlineImage() {
    return PdfOpcodes.isBeginInlineImage(getOperator());
  }

  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isBeginInlineImageData(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#BEGIN_INLINE_IMAGE_DATA}
   * @since 1.76
   */
  protected boolean isBeginInlineImageData() {
    return PdfOpcodes.isBeginInlineImageData(getOperator());
  }

  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isBeginText(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#BEGIN_TEXT}
   * @since 1.60
   */
  protected boolean isBeginText() {
    return PdfOpcodes.isBeginText(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isDrawObject(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#DRAW_OBJECT}
   * @since 1.76
   */
  protected boolean isDrawObject() {
    return PdfOpcodes.isDrawObject(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isEndText(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#END_TEXT}
   * @since 1.76
   */
  protected boolean isEndText() {
    return PdfOpcodes.isEndText(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isRestore(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#RESTORE}
   * @since 1.76
   */
  protected boolean isRestore() {
    return PdfOpcodes.isRestore(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isSave(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SAVE}
   * @since 1.76
   */
  protected boolean isSave() {
    return PdfOpcodes.isSave(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isSetGraphicsStateParams(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SET_GRAPHICS_STATE_PARAMS}
   * @since 1.76
   */
  protected boolean isSetGraphicsStateParams() {
    return PdfOpcodes.isSetGraphicsStateParams(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isSetTextFont(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SET_TEXT_FONT}
   * @since 1.62
   */
  protected boolean isSetTextFont() {
    return PdfOpcodes.isSetTextFont(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isSetTextMatrix(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SET_TEXT_MATRIX}
   * @since 1.62
   */
  protected boolean isSetTextMatrix() {
    return PdfOpcodes.isSetTextMatrix(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowText(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT}
   * @since 1.60
   */
  protected boolean isShowText() {
    return PdfOpcodes.isShowText(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextAdjusted(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_ADJUSTED}
   * @since 1.76
   */
  protected boolean isShowTextAdjusted() {
    return PdfOpcodes.isShowTextAdjusted(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextAdjustedContains(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given substring.
   * </p>
   * 
   * @param substr
   *          A given substring.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_ADJUSTED} and its equivalent
   *         string operand contains <code>substr</code>.
   * @since 1.76
   */
  protected boolean isShowTextAdjustedContains(String substr) {
    return PdfOpcodes.isShowTextAdjustedContains(getOperator(), getSingleOperand(), substr);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextAdjustedEndsWith(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given suffix.
   * </p>
   * 
   * @param suffix
   *          A given suffix.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_ADJUSTED} and its equivalent
   *         string operand ends with <code>suffix</code>.
   * @since 1.76
   */
  protected boolean isShowTextAdjustedEndsWith(String suffix) {
    return PdfOpcodes.isShowTextAdjustedEndsWith(getOperator(), getSingleOperand(), suffix);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextAdjustedEquals(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given suffix.
   * </p>
   * 
   * @param str
   *          A given string.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_ADJUSTED} and its equivalent
   *         string operand is equal to <code>str</code>.
   * @since 1.76
   */
  protected boolean isShowTextAdjustedEquals(String str) {
    return PdfOpcodes.isShowTextAdjustedEquals(getOperator(), getSingleOperand(), str);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextAdjustedEqualsIgnoreCase(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given suffix.
   * </p>
   * 
   * @param str
   *          A given string.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_ADJUSTED} and its equivalent
   *         string operand is equal to <code>str</code> (case-independently).
   * @since 1.76
   */
  protected boolean isShowTextAdjustedEqualsIgnoreCase(String str) {
    return PdfOpcodes.isShowTextAdjustedEqualsIgnoreCase(getOperator(), getSingleOperand(), str);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextAdjustedFind(PdfToken, PdfToken, Pattern)
   * using the current operator, its single operand and the given pattern.
   * </p>
   * 
   * @param pattern
   *          A given pattern.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its equivalent
   *         string operand matches <code>pattern</code> (using
   *         {@link Matcher#find()}, which does not implicitly anchor).
   * @since 1.76
   * @see Matcher#find()
   */
  protected boolean isShowTextAdjustedFind(Pattern pattern) {
    return PdfOpcodes.isShowTextAdjustedFind(getOperator(), getSingleOperand(), pattern);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextAdjustedStartsWith(PdfToken, PdfToken, String)
   * using the current operator, its single operand and the given prefix.
   * </p>
   * 
   * @param prefix
   *          A given prefix.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_ADJUSTED} and its equivalent
   *         string operand starts with <code>prefix</code>.
   * @since 1.76
   */
  protected boolean isShowTextAdjustedStartsWith(String prefix) {
    return PdfOpcodes.isShowTextAdjustedStartsWith(getOperator(), getSingleOperand(), prefix);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextContains(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given substring.
   * </p>
   * 
   * @param substr
   *          A given substring.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its string operand contains
   *         <code>substr</code>.
   * @since 1.67
   */
  protected boolean isShowTextContains(String substr) {
    return PdfOpcodes.isShowTextContains(getOperator(), getSingleOperand(), substr);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextEndsWith(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given suffix.
   * </p>
   * 
   * @param suffix
   *          A given suffix.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its string operand ends with
   *         <code>suffix</code>.
   * @since 1.60
   */
  protected boolean isShowTextEndsWith(String suffix) {
    return PdfOpcodes.isShowTextEndsWith(getOperator(), getSingleOperand(), suffix);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextEquals(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given string.
   * </p>
   * 
   * @param str
   *          A given string.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its string operand is equal to
   *         <code>str</code>.
   * @since 1.60
   */
  protected boolean isShowTextEquals(String str) {
    return PdfOpcodes.isShowTextEquals(getOperator(), getSingleOperand(), str);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextEqualsIgnoreCase(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given string.
   * </p>
   * 
   * @param str
   *          A given string.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its string operand is equal to
   *         <code>str</code> (case-independently).
   * @since 1.60
   */
  protected boolean isShowTextEqualsIgnoreCase(String str) {
    return PdfOpcodes.isShowTextEqualsIgnoreCase(getOperator(), getSingleOperand(), str);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextFind(PdfToken, PdfToken, Pattern)}
   * using the current operator, its single operand and the given pattern.
   * </p>
   * 
   * @param pattern
   *          A given pattern.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its string operand matches
   *         <code>pattern</code> (using {@link Matcher#find()}, which does not
   *         implicitly anchor).
   * @since 1.62
   * @see Matcher#find()
   */
  protected boolean isShowTextFind(Pattern pattern) {
    return PdfOpcodes.isShowTextFind(getOperator(), getSingleOperand(), pattern);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextLine(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_LINE}
   * @since 1.76
   */
  protected boolean isShowTextLine() {
    return PdfOpcodes.isShowTextLine(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextLineAndSpace(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_LINE_AND_SPACE}
   * @since 1.76
   */
  protected boolean isShowTextLineAndSpace() {
    return PdfOpcodes.isShowTextLineAndSpace(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfOpcodes#isShowTextStartsWith(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given prefix.
   * </p>
   * 
   * @param prefix
   *          A given prefix.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its string operand starts with
   *         <code>prefix</code>.
   * @since 1.60
   */
  protected boolean isShowTextStartsWith(String prefix) {
    return PdfOpcodes.isShowTextStartsWith(getOperator(), getSingleOperand(), prefix);
  }
  
  /**
   * <p>
   * Processes a token stream backward.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  protected void processBackward() throws PdfException {
    int end = tokens.size() - 1;
    while (end >= 0 && continueFlag) {
      operands_old.clear();
      operator = tokens.get(end);
      opcode = operator.getOperator();
      index = end;
      int beginMinusOne = end - 1;
      while (beginMinusOne >= 0 && !tokens.get(beginMinusOne).isOperator()) {
        --beginMinusOne;
      }
      operands_old.addAll(tokens.subList(beginMinusOne + 1, end));
      operatorCallback();
      end = beginMinusOne;
    }
  }
  
  /**
   * <p>
   * Processes a token stream forward.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  protected void processForward() throws PdfException {
    int begin = 0;
    while (begin < tokens.size() && continueFlag) {
      operands_old.clear();
      int end = begin;
      while (end < tokens.size() && !tokens.get(end).isOperator()) {
        ++end;
      }
      operands_old.addAll(tokens.subList(begin, end));
      operator = tokens.get(end);
      opcode = operator.getOperator();
      index = end;
      operatorCallback();
      begin = end + 1;
    }
  }
  
  /**
   * <p>
   * Requests that processing of the token stream end prematurely. No calls to
   * {@link #operatorCallback()} will be issued unless
   * {@link #process(PdfTokenStream)} is invoked again.
   * </p>
   * 
   * @since 1.56
   */
  protected void stop() {
    continueFlag = false;
  }

  /* ***************************************************************************
   * DEPRECATED IN 1.76
   ************************************************************************** */
  
  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isBeginInlineImageData()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isBeginInlineImageData()}.
   */
  @Deprecated
  protected boolean isBeginImageData() {
    return isBeginInlineImageData();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isBeginInlineImage()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isBeginInlineImage()}.
   */
  @Deprecated
  protected boolean isBeginImageObject() {
    return isBeginInlineImage();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isBeginText()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isBeginText()}.
   */
  protected boolean isBeginTextObject() {
    return isBeginText();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isEndText()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isEndText()}.
   */
  @Deprecated
  protected boolean isEndTextObject() {
    return isEndText();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isDrawObject()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isDrawObject()}.
   */
  @Deprecated
  protected boolean isInvokeXObject() {
    return isDrawObject();
  }
  
  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isShowTextLine()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isShowTextLine()}.
   */
  @Deprecated
  protected boolean isNextLineShowText() {
    return isShowTextLine();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isRestore()}.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#RESTORE_GRAPHICS_STATE}
   * @since 1.60
   * @deprecated Renamed to {@link #isRestore()}.
   */
  @Deprecated
  protected boolean isRestoreGraphicsState() {
    return isRestore();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isSave()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isSave()}.
   */
  @Deprecated
  protected boolean isSaveGraphicsState() {
    return isSave();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isShowTextLineAndSpace()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isShowTextLineAndSpace()}.
   */
  @Deprecated
  protected boolean isSetSpacingNextLineShowText() {
    return isShowTextLineAndSpace();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isShowTextAdjusted()}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isShowTextAdjusted()}.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioning() {
    return isShowTextAdjusted();
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isShowTextAdjustedContains(String)}.
   * </p>
   * 
   * @since 1.67
   * @deprecated Renamed to {@link #isShowTextAdjustedContains(String)}.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioningContains(String substr) {
    return isShowTextAdjustedContains(substr);
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isShowTextAdjustedEndsWith(String)}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isShowTextAdjustedEndsWith(String)}.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioningEndsWith(String suffix) {
    return isShowTextAdjustedEndsWith(suffix);
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isShowTextAdjustedEquals(String)}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isShowTextAdjustedEquals(String)}.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioningEquals(String str) {
    return isShowTextAdjustedEquals(str);
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isShowTextAdjustedEqualsIgnoreCase(String)}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isShowTextAdjustedEqualsIgnoreCase(String)}.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioningEqualsIgnoreCase(String str) {
    return isShowTextAdjustedEqualsIgnoreCase(str);
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to{@link #isShowTextAdjustedFind(Pattern)}.
   * </p>
   * 
   * @since 1.62
   * @deprecated Renamed to{@link #isShowTextAdjustedFind(Pattern)}.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioningFind(Pattern pattern) {
    return isShowTextAdjustedFind(pattern);
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #isShowTextAdjustedStartsWith(String)}.
   * </p>
   * 
   * @since 1.60
   * @deprecated Renamed to {@link #isShowTextAdjustedStartsWith(String)}.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioningStartsWith(String prefix) {
    return isShowTextAdjustedStartsWith(prefix);
  }

}
