/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.*;

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
   */
  public enum Direction {
    BACKWARD,
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
   * @see Direction
   */
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
   * The current operator.
   * </p>
   * 
   * @since 1.56
   */
  private List<PdfToken> operands;

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
   */
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
   * @see Direction
   */
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
   * @see #process(List, PdfTokenFactory)
   */
  public void process(PdfTokenStream pdfTokenStream) throws PdfException {
    process(pdfTokenStream.getTokens(), pdfTokenStream.getTokenFactory());
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
   */
  public void process(List<PdfToken> tokens,
                      PdfTokenFactory factory)
      throws PdfException {
    this.tokens = tokens;
    this.factory = factory;
    index = -1;
    operator = null;
    operands = new ArrayList<PdfToken>();
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
    return direction;
  }

  /**
   * <p>
   * Gets the current PDF token factory.
   * </p>
   * 
   * @return the current PDF token factory.
   * @since 1.56
   * @deprecated As of 1.60, use {@link #getTokenFactory()} instead.
   */
  @Deprecated
  protected PdfTokenFactory getFactory() {
    return factory;
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
    List<PdfToken> operands = getOperands();
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
  protected List<PdfToken> getOperands() {
    return operands;
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
   */
  protected List<PdfToken> getTokens() {
    return tokens;
  }

  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isBeginImageData(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#BEGIN_IMAGE_DATA}
   * @since 1.60
   */
  protected boolean isBeginImageData() {
    return PdfUtil.isBeginImageData(getOperator());
  }

  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isBeginImageObject(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#BEGIN_IMAGE_OBJECT}
   * @since 1.60
   */
  protected boolean isBeginImageObject() {
    return PdfUtil.isBeginImageObject(getOperator());
  }

  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isBeginTextObject(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#BEGIN_TEXT_OBJECT}
   * @since 1.60
   */
  protected boolean isBeginTextObject() {
    return PdfUtil.isBeginTextObject(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isEndTextObject(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#END_TEXT_OBJECT}
   * @since 1.60
   */
  protected boolean isEndTextObject() {
    return PdfUtil.isEndTextObject(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isInvokeXObject(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#INVOKE_XOBJECT}
   * @since 1.60
   */
  protected boolean isInvokeXObject() {
    return PdfUtil.isInvokeXObject(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isNextLineShowText(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#NEXT_LINE_SHOW_TEXT}
   * @since 1.60
   */
  protected boolean isNextLineShowText() {
    return PdfUtil.isNextLineShowText(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isRestoreGraphicsState(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#RESTORE_GRAPHICS_STATE}
   * @since 1.60
   */
  protected boolean isRestoreGraphicsState() {
    return PdfUtil.isRestoreGraphicsState(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isSaveGraphicsState(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SAVE_GRAPHICS_STATE}
   * @since 1.60
   */
  protected boolean isSaveGraphicsState() {
    return PdfUtil.isSaveGraphicsState(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isSetSpacingNextLineShowText(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SET_SPACING_NEXT_LINE_SHOW_TEXT}
   * @since 1.60
   */
  protected boolean isSetSpacingNextLineShowText() {
    return PdfUtil.isSetSpacingNextLineShowText(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isSetTextFont(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SET_TEXT_FONT}
   * @since 1.62
   */
  protected boolean isSetTextFont() {
    return PdfUtil.isSetTextFont(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isSetTextMatrix(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SET_TEXT_MATRIX}
   * @since 1.62
   */
  protected boolean isSetTextMatrix() {
    return PdfUtil.isSetTextMatrix(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowText(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT}
   * @since 1.60
   */
  protected boolean isShowText() {
    return PdfUtil.isShowText(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextContains(PdfToken, PdfToken, String)}
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
    return PdfUtil.isShowTextContains(getOperator(), getSingleOperand(), substr);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextEndsWith(PdfToken, PdfToken, String)}
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
    return PdfUtil.isShowTextEndsWith(getOperator(), getSingleOperand(), suffix);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextEquals(PdfToken, PdfToken, String)}
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
    return PdfUtil.isShowTextEquals(getOperator(), getSingleOperand(), str);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextEqualsIgnoreCase(PdfToken, PdfToken, String)}
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
    return PdfUtil.isShowTextEqualsIgnoreCase(getOperator(), getSingleOperand(), str);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextFind(PdfToken, PdfToken, Pattern)}
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
    return PdfUtil.isShowTextFind(getOperator(), getSingleOperand(), pattern);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextGlyphPositioning(PdfToken)}
   * using the current operator.
   * </p>
   * 
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING}
   * @since 1.60
   */
  protected boolean isShowTextGlyphPositioning() {
    return PdfUtil.isShowTextGlyphPositioning(getOperator());
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextGlyphPositioningContains(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given substring.
   * </p>
   * 
   * @param substr
   *          A given substring.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its equivalent
   *         string operand contains <code>substr</code>.
   * @since 1.67
   */
  protected boolean isShowTextGlyphPositioningContains(String substr) {
    return PdfUtil.isShowTextGlyphPositioningContains(getOperator(), getSingleOperand(), substr);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextGlyphPositioningEndsWith(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given suffix.
   * </p>
   * 
   * @param suffix
   *          A given suffix.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its equivalent
   *         string operand ends with <code>suffix</code>.
   * @since 1.60
   */
  protected boolean isShowTextGlyphPositioningEndsWith(String suffix) {
    return PdfUtil.isShowTextGlyphPositioningEndsWith(getOperator(), getSingleOperand(), suffix);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextGlyphPositioningEquals(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given suffix.
   * </p>
   * 
   * @param str
   *          A given string.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its equivalent
   *         string operand is equal to <code>str</code>.
   * @since 1.60
   */
  protected boolean isShowTextGlyphPositioningEquals(String str) {
    return PdfUtil.isShowTextGlyphPositioningEquals(getOperator(), getSingleOperand(), str);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextGlyphPositioningEqualsIgnoreCase(PdfToken, PdfToken, String)}
   * using the current operator, its single operand and the given suffix.
   * </p>
   * 
   * @param str
   *          A given string.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its equivalent
   *         string operand is equal to <code>str</code> (case-independently).
   * @since 1.60
   */
  protected boolean isShowTextGlyphPositioningEqualsIgnoreCase(String str) {
    return PdfUtil.isShowTextGlyphPositioningEqualsIgnoreCase(getOperator(), getSingleOperand(), str);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextGlyphPositioningFind(PdfToken, PdfToken, Pattern)
   * using the current operator, its single operand and the givenpattern.
   * </p>
   * 
   * @param pattern
   *          A given pattern.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its equivalent
   *         string operand matches <code>pattern</code> (using
   *         {@link Matcher#find()}, which does not implicitly anchor).
   * @since 1.62
   * @see Matcher#find()
   */
  protected boolean isShowTextGlyphPositioningFind(Pattern pattern) {
    return PdfUtil.isShowTextGlyphPositioningFind(getOperator(), getSingleOperand(), pattern);
  }
  
  /**
   * <p>
   * Determines if the current operator is
   * {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its string operand
   * matches the given pattern according to {@link Matcher#matches()}.
   * </p>
   * <p>
   * This method uses {@link Matcher#matches()}, which implicitly anchors at the
   * beginning of the input string.
   * </p>
   * 
   * @param pattern
   *          A regular expression expressed as a {@link Pattern}.
   * @return <code>true</code> if the current operator is
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its string
   *         operand matches <code>regex</code>.
   * @since 1.60
   * @see Matcher#matches()
   * @deprecated Adapt your code to using
   *             {@link #isShowTextGlyphPositioningFind(Pattern)} instead.
   *             This method will be removed in a future release.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioningMatches(Pattern pattern) {
    if (!isShowTextGlyphPositioning()) {
      return false;
    }
    StringBuilder sb = new StringBuilder();
    for (PdfToken tok : getSingleOperand().getArray()) {
      if (tok.isString()) {
        sb.append(tok.getString());
      }
    }
    return pattern.matcher(sb.toString()).matches();
  }
  
  /**
   * <p>
   * Determines if the current operator is
   * {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its string operand
   * matches the given regular expression.
   * </p>
   * <p>
   * This method uses {@link String#matches(String)} which compiles the regular
   * expression each time, so it may be inefficient compared to
   * {@link #isShowTextGlyphPositioningMatches(Pattern)}, and also implicitly
   * anchors at the beginning of the input string.
   * </p>
   * 
   * @param regex
   *          A regular expression expressed as a string.
   * @return <code>true</code> if the current operator is
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its string
   *         operand matches <code>regex</code>.
   * @since 1.60
   * @see #isShowTextGlyphPositioningMatches(Pattern)
   * @see String#matches(String)
   * @deprecated Adapt your code to using
   *             {@link #isShowTextGlyphPositioningFind(Pattern)} instead.
   *             This method will be removed in a future release.
   */
  @Deprecated
  protected boolean isShowTextGlyphPositioningMatches(String regex) {
    if (!isShowTextGlyphPositioning()) {
      return false;
    }
    StringBuilder sb = new StringBuilder();
    for (PdfToken tok : getSingleOperand().getArray()) {
      if (tok.isString()) {
        sb.append(tok.getString());
      }
    }
    return sb.toString().matches(regex);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextGlyphPositioningStartsWith(PdfToken, PdfToken, String)
   * using the current operator, its single operand and the given prefix.
   * </p>
   * 
   * @param prefix
   *          A given prefix.
   * @return <code>true</code> if the current operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING} and its equivalent
   *         string operand starts with <code>prefix</code>.
   * @since 1.60
   */
  protected boolean isShowTextGlyphPositioningStartsWith(String prefix) {
    return PdfUtil.isShowTextGlyphPositioningStartsWith(getOperator(), getSingleOperand(), prefix);
  }
  
  /**
   * <p>
   * Determines if the current operator is {@link PdfOpcodes#SHOW_TEXT} and its
   * string operand matches the given pattern according to
   * {@link Matcher#matches()}.
   * </p>
   * <p>
   * This method uses {@link Matcher#matches()}, which implicitly anchors at the
   * beginning of the input string.
   * </p>
   * 
   * @param pattern
   *          A regular expression expressed as a {@link Pattern}.
   * @return <code>true</code> if the current operator is
   *         {@link PdfOpcodes#SHOW_TEXT} and its string operand matches
   *         <code>pattern</code>.
   * @since 1.60
   * @see Matcher#matches()
   * @deprecated Adapt your code to using {@link #isShowTextFind(Pattern)}
   *             instead. This method will be removed in a future release.
   */
  @Deprecated
  protected boolean isShowTextMatches(Pattern pattern) {
    return isShowText() && pattern.matcher(getSingleOperand().getString()).matches();
  }
  
  /**
   * <p>
   * Determines if the current operator is {@link PdfOpcodes#SHOW_TEXT} and its
   * string operand matches the given regular expression.
   * </p>
   * <p>
   * This method uses {@link String#matches(String)} which compiles the regular
   * expression each time, so it may be inefficient compared to
   * {@link #isShowTextMatches(Pattern)}, and also implicitly anchors at the
   * beginning of the input string.
   * </p>
   * 
   * @param regex
   *          A regular expression expressed as a string.
   * @return <code>true</code> if the current operator is
   *         {@link PdfOpcodes#SHOW_TEXT} and its string operand matches
   *         <code>regex</code>.
   * @since 1.60
   * @see #isShowTextMatches(Pattern)
   * @see String#matches(String)
   * @deprecated Adapt your code to using {@link #isShowTextFind(Pattern)}
   *             instead. This method will be removed in a future release.
   */
  @Deprecated
  protected boolean isShowTextMatches(String regex) {
    return isShowText() && getSingleOperand().getString().matches(regex);
  }
  
  /**
   * <p>
   * Convenience call to
   * {@link PdfUtil#isShowTextStartsWith(PdfToken, PdfToken, String)}
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
    return PdfUtil.isShowTextStartsWith(getOperator(), getSingleOperand(), prefix);
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
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  protected void processForward() throws PdfException {
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
  
}
