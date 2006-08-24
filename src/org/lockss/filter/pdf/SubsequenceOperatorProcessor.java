/*
 * $Id: SubsequenceOperatorProcessor.java,v 1.1 2006-08-24 01:19:34 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.List;

import org.pdfbox.util.PDFOperator;
import org.pdfbox.util.operator.OperatorProcessor;

/**
 * <p>A PDF operator processor that first passes its operands and
 * operator to the output list unconditionally, then conditionally
 * replaces the end subsequence of the output list with another.</p>
 * <p>{@link SubsequenceOperatorProcessor} instances, like
 * {@link OperatorProcessor} instances, are only instantiated once
 * per instantiation of a {@link PdfPageStreamTransform}, and should
 * have a no-argument constructor.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class SubsequenceOperatorProcessor extends SimpleOperatorProcessor {

  /**
   * <p>Determines if the given subsequence of tokens (taken from the
   * end of the output list) should be replaced in the output list by
   * the list constructed by {@link #modify}.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li>pdfTokens != null</li>
   *  <li>pdfTokens.length <= subsequenceLength()</li>
   * </ul>
   * <p>It is not guaranteed that
   * <code>pdfTokens.length == subsequenceLength()</code>, so calls
   * to this method such that
   * <code>pdfTokens.length != subsequenceLength()</code> should
   * return false.</p>
   * @param pdfTokens A subsequence of PDF tokens to recognize.
   * @return False if <code>pdfTokens.length != subsequenceLength()</code>
   *         or if the token subsequence does not match the pattern
   *         recognized by this operator processor, true otherwise.
   */
  public abstract boolean identify(Object[] pdfTokens);

  /**
   * <p>Constructs a replacement for the given PDF tokens (which are
   * the last <code>subsequenceLength()</code> in the output list)
   * into the argument list.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>replacement != null</code></li>
   *  <li><code>pdfTokens != null</code></li>
   *  <li><code>pdfTokens.length == subsequenceLength()</code></li>
   *  <li><code>identify(pdfTokens)</code></li>
   * </ul>
   * @param pdfTokens   A subsequence of PDF tokens of length
   *                    <code>subsequenceLength()</code>, previously
   *                    recognized by {@link #identify}, to be
   *                    replaced.
   * @param replacement A list into which to output replacement for
   *                    the PDF tokens (which will then be used to
   *                    replace the last
   *                    <code>subsequenceLength()</code> tokens in the
   *                    output list).
   */
  public abstract void modify(Object[] pdfTokens, List replacement);

  /* Inherit documentation */
  public void process(PDFOperator operator,
                      List arguments,
                      PdfPageStreamTransform pdfPageStreamTransform)
      throws IOException {
    super.process(operator, arguments, pdfPageStreamTransform);
    int subsequenceLength = subsequenceLength();
    List outputList = pdfPageStreamTransform.getOutputList();
    int outputListSize = outputList.size();
    Object[] pdfTokens = (outputListSize >= subsequenceLength ? outputList.subList(outputListSize - subsequenceLength, outputListSize) : outputList).toArray();
    if (identify(pdfTokens)) {
      pdfPageStreamTransform.signalChange();
      List replacement = new ArrayList();
      modify(pdfTokens, replacement);
      outputList.subList(outputListSize - subsequenceLength, outputListSize).clear();
      outputList.addAll(replacement);
    }
  }

  /**
   * <p>Indicates how many PDF tokens from the end of the output list
   * correspond to the subsequence recognized by this operator
   * processor.</p>
   * <p>If {@link #identify} determines that a subsequence should be
   * replaced, then the number of tokens that will be removed from the
   * end of the output list to be replaced will also be that returned
   * by {@link #subsequenceLength}.
   * @return A number of tokens to be considered as the subsequence
   *         from the end of the output list.
   */
  public abstract int subsequenceLength();

}
