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

import java.util.List;

import org.lockss.util.Logger;

/**
 * <p>
 * A {@link PdfTokenStreamWorker} helper class structured as a state machine
 * with 10 states, providing typical slots for a state variable, a result flag,
 * and a beginning and ending index for a range.
 * </p>
 * <p>
 * Each state is represented by a callback named {@link #state0()}, ...,
 * {@link #state9()}. Unless overridden, a state callback simply throws (via
 * {@link #throwUndefinedStateException()}). Illegal state values also cause
 * {@link #operatorCallback()} to throw (via
 * {@link #throwInvalidStateException()}). To increase the number of states,
 * override {@link #stateDispatch()} similarly to this and provide whatever
 * additional state callbacks are needed:
 * </p>
 * <pre>
    public void stateDispatch() throws PdfException {
      switch (state) {
        state 10: myState10(); break;
        state 11: myState11(); break;
        state 12: myState12(); break;
        default: super.stateDispatch();
      }
    }
 * </pre>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class PdfTokenStreamStateMachine extends PdfTokenStreamWorker {
  
  /**
   * <p>
   * A logger used when the concrete subclass does not provide one.
   * </p>
   * 
   * @since 1.67
   */
  private static final Logger DEFAULT_LOGGER = Logger.getLogger(PdfTokenStreamStateMachine.class);

  /**
   * <p>
   * A logger for this instance (set to {@link #DEFAULT_LOGGER} if the subclass
   * does not provide one).
   * </p>
   * 
   * @since 1.67.6
   */
  private Logger log;
  
  /**
   * <p>
   * A result flag (initially false).
   * </p>
   * 
   * @since 1.67
   */
  protected boolean result;
  
  /**
   * <p>
   * A state variable (initially set to zero).
   * </p>
   * 
   * @since 1.67
   */
  protected int state;
  
  /**
   * <p>
   * A beginning index (initially set to -1).
   * </p>
   * 
   * @since 1.67
   */
  protected int begin;
  
  /**
   * <p>
   * An ending index (initially set to -1).
   * </p>
   * 
   * @since 1.67
   */
  protected int end;
  
  /**
   * <p>
   * Creates a new state machine in the default direction with the default
   * logger.
   * </p>
   * 
   * @since 1.67
   */
  public PdfTokenStreamStateMachine() {
    this(DEFAULT_LOGGER);
  }

  /**
   * <p>
   * Creates a new state machine in the given direction with the default logger.
   * </p>
   * 
   * @param direction
   *          A direction.
   * @since 1.67
   */
  public PdfTokenStreamStateMachine(Direction direction) {
    this(direction, DEFAULT_LOGGER);
  }

  /**
   * <p>
   * Creates a new state machine in the default direction with the given logger.
   * </p>
   * 
   * @param log
   *          A logger.
   * @since 1.67
   */
  public PdfTokenStreamStateMachine(Logger log) {
    super();
    this.log = log;
  }
  
  /**
   * <p>
   * Creates a new state machine in the given direction with the given logger.
   * </p>
   * 
   * @param direction
   *          A direction.
   * @param log
   *          A logger.
   * @since 1.67
   */
  public PdfTokenStreamStateMachine(Direction direction,
                                    Logger log) {
    super(direction);
    this.log = log;
  }

  @Override
  public void setUp() throws PdfException {
    super.setUp();
    this.result = false;
    this.state = 0;
    this.begin = -1;
    this.end = -1;
  }  

  /**
   * <p>
   * Retrieves the result flag.
   * </p>
   * 
   * @return The result flag.
   * @since 1.67
   */
  public boolean getResult() {
    return result;
  }

  /**
   * <p>
   * Sets the result flag.
   * </p>
   * 
   * @param result
   *          Value of the result flag.
   * @since 1.67
   */
  public void setResult(boolean result) {
    this.result = result;
  }

  /**
   * <p>
   * Retrieves the state variable.
   * </p>
   * 
   * @return The state variable.
   * @since 1.67
   */
  public int getState() {
    return state;
  }

  /**
   * <p>
   * Sets the state variable.
   * </p>
   * 
   * @param state
   *          Value of the state variable.
   * @since 1.67
   */
  public void setState(int state) {
    this.state = state;
  }

  /**
   * <p>
   * Retrieves the beginning index.
   * </p>
   * 
   * @return The beginning index.
   * @since 1.67
   */
  public int getBegin() {
    return begin;
  }

  /**
   * <p>
   * Sets the beginning index.
   * </p>
   * 
   * @param begin
   *          Value of the beginning index.
   * @since 1.67
   */
  public void setBegin(int begin) {
    this.begin = begin;
  }

  /**
   * <p>
   * Retrieves the ending index.
   * </p>
   * 
   * @return The ending index.
   * @since 1.67
   */
  public int getEnd() {
    return end;
  }

  /**
   * <p>
   * Sets the ending index.
   * </p>
   * 
   * @param end
   *          Value of the ending index.
   * @since 1.67
   */
  public void setEnd(int end) {
    this.end = end;
  }

  @Override
  public void operatorCallback() throws PdfException {
    if (log != null && log.isDebug3()) {
      log.debug3("initial: " + state);
      log.debug3("index: " + getIndex());
      log.debug3("operator: " + getOpcode());
    }
    stateDispatch();
    if (log != null && log.isDebug3()) {
      log.debug3("final: " + state);
      log.debug3("result: " + result);
    }
  }
  
  @Override
  public void process(List<PdfToken> tokens, PdfTokenFactory factory) throws PdfException {
    if (log == DEFAULT_LOGGER) {
      log.debug3("actual class: " + getClass().getName());
    }
    super.process(tokens, factory);
  }
  
  /**
   * <p>
   * Portion of {@link #operatorCallback()} that simply switches on the state
   * variable and dispatches to the corresponding callback.
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void stateDispatch() throws PdfException {
    switch (state) {
      case 0: state0(); break;
      case 1: state1(); break;
      case 2: state2(); break;
      case 3: state3(); break;
      case 4: state4(); break;
      case 5: state5(); break;
      case 6: state6(); break;
      case 7: state7(); break;
      case 8: state8(); break;
      case 9: state9(); break;
      default: throwInvalidStateException();
    }
  }

  /**
   * <p>
   * Callback for state value 0 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state0() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 1 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state1() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 2 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state2() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 3 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state3() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 4 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state4() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 5 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state5() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 6 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state6() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 7 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state7() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 8 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state8() throws PdfException {
    throwUndefinedStateException();
  }
  
  /**
   * <p>
   * Callback for state value 9 (throws if called but not overridden).
   * </p>
   * 
   * @throws PdfException
   *           if any processing error occurs.
   * @since 1.67
   */
  public void state9() throws PdfException {
    throwUndefinedStateException();
  }

  /**
   * <p>
   * Throws a {@link PdfException} for a state value that has not been defined
   * (callback not overridden).
   * </p>
   * 
   * @throws PdfException
   *           unconditionally
   * @since 1.67
   */
  public void throwUndefinedStateException() throws PdfException {
    throw new PdfException(String.format("Undefined state at index %d: %d", getIndex(), state));
  }
  
  /**
   * <p>
   * Throws a {@link PdfException} for a state value that is invalid.
   * </p>
   * 
   * @throws PdfException
   *           unconditionally
   * @since 1.67
   */
  public void throwInvalidStateException() throws PdfException {
    throw new PdfException(String.format("Invalid state at index %d: %d", getIndex(), state));
  }
  
}
