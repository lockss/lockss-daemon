/*
 * $Id: ConditionalOperatorProcessor.java,v 1.5 2007-02-23 19:41:34 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter.pdf;

import java.io.IOException;
import java.util.*;

import org.lockss.util.Logger;
import org.pdfbox.util.PDFOperator;

/**
 * <p>A PDF operator processor that processes an operation (operator
 * and operands) conditionally.</p>
 * <p>Based on the value returned by {@link #identify}, either
 * {@link #processIdentified} or {@link #processNotIdentified} is
 * invoked. This class does implement {@link #processIdentified} and
 * {@link #processNotIdentified}, but they do nothing, so subclasses
 * should override the methods as necessary.</p>
 * <p>By default, the scope of tokens examined by {@link #identify}
 * is the page stream transform's entire current output list, but
 * subclasses can examine other subparts of the output list by
 * overriding {@link #getSequence}.</p>
 * <p>{@link ConditionalOperatorProcessor} instances, like
 * {@link PdfOperatorProcessor} instances, <em>must</em> have a
 * no-argument constructor, and are instantiated once per key
 * associated with their class name during a given
 * {@link PageStreamTransform} instantiation.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class ConditionalOperatorProcessor extends PdfOperatorProcessor {

  /**
   * <p>Computes a sequence of tokens to be examined by the
   * {@link #identify} method.</p>
   * @param pdfPageStreamTransform The current page stream transform
   * @return A list of tokens to be examined by {@link #identify}.
   */
  public List getSequence(PageStreamTransform pdfPageStreamTransform) {
    return Collections.unmodifiableList(pdfPageStreamTransform.getOutputList());
  }

  /**
   * <p>Determines whether the given tokens are recognized by this
   * operator processor.</p>
   * <p>In most normal situations:</p>
   * <ul>
   *  <li>This method will be called with the result returned by
   *  {@link #getSequence}.</li>
   *  <li>If this method returns true, {@link #processIdentified}
   *  will be called, otherwise {@link #processNotIdentified} will
   *  be called.</li>
   * @param tokens A list of tokens.
   * @return True if this operator processor recognizes the sequence
   *         of tokens, false otherwise.
   * @see #getSequence
   */
  public abstract boolean identify(List tokens);

  /* Inherit documentation */
  public void process(PageStreamTransform pageStreamTransform,
                      PDFOperator operator,
                      List operands)
      throws IOException {
    logger.debug3("Processing " + operator.getOperation());
    List outputList = pageStreamTransform.getOutputList();
    outputList.addAll(operands);
    outputList.add(operator);
    List tokens = getSequence(pageStreamTransform);
    if (identify(tokens)) {
      logger.debug3("The tokens were identified");
      pageStreamTransform.signalChange();
      processIdentified(pageStreamTransform, tokens);
    }
    else {
      logger.debug3("The tokens were not identified");
      processNotIdentified(pageStreamTransform, tokens);
    }
  }

  /**
   * <p>Processes tokens identified by the {@link #identify} method.</p>
   * @param pdfPageStreamTransform The current page stream transform.
   * @param tokens                 The tokens recognized by {@link #identify}.
   * @see #identify
   */
  public void processIdentified(PageStreamTransform pdfPageStreamTransform,
                                List tokens) {
    // do nothing
  }

  /**
   * <p>Processes tokens not identified by the {@link #identify} method.</p>
   * @param pdfPageStreamTransform The current page stream transform.
   * @param tokens                 The tokens not recognized by {@link #identify}.
   * @see #identify
   */
  public void processNotIdentified(PageStreamTransform pdfPageStreamTransform,
                                   List tokens) {
    // do nothing
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("ConditionalOperatorProcessor");

}
