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
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public abstract class ConditionalOperatorProcessor extends PdfOperatorProcessor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalOperatorProcessor() {}
  
  /**
   * <p>Computes a sequence of tokens to be examined by the
   * {@link #identify} method.</p>
   * @param pdfPageStreamTransform The current page stream transform
   * @return A list of tokens to be examined by {@link #identify}.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
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
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public abstract boolean identify(List tokens);

  /* Inherit documentation */
  @Deprecated
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
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void processIdentified(PageStreamTransform pdfPageStreamTransform,
                                List tokens) {
    // do nothing
  }

  /**
   * <p>Processes tokens not identified by the {@link #identify} method.</p>
   * @param pdfPageStreamTransform The current page stream transform.
   * @param tokens                 The tokens not recognized by {@link #identify}.
   * @see #identify
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void processNotIdentified(PageStreamTransform pdfPageStreamTransform,
                                   List tokens) {
    // do nothing
  }

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(ConditionalOperatorProcessor.class);

}
