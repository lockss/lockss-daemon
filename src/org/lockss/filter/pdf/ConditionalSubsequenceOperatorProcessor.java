/*
 * $Id: ConditionalSubsequenceOperatorProcessor.java,v 1.4 2007-02-23 19:41:34 thib_gc Exp $
 */

/*
76 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.pdfbox.util.PDFOperator;

/**
 * <p>A conditional operator processor that examines a fixed-length
 * subsequence from the end of the current output list and
 * conditionally acts on it.</p>
 * <p>This operator processor is useful when only a fixed-length
 * portion of the end of the output list is sufficient to determine
 * a condition, or at least if there is a safe upper bound on the
 * number of tokens drawn from the end of the output list are
 * needed to determine the condition.</p>
 * <p>In summary, the behavior of this class is as follows:</p>
 * <ul>
 *  <li>{@link #getSequence} returns the last
 *  <code>getSubsequenceLength()</code> tokens from the output list
 *  (or the whole output list if fewer than the required number of
 *  tokens are in it).</li>
 *  <li>{@link #processIdentified} replaces the last
 *  {@link #getSubsequenceLength} tokens from the output list by the
 *  result of {@link #getReplacement}.</li>
 * </ul>
 * <p>The {@link #processIdentified} method assumes that the number of
 * tokens to remove from the end of the output list (to be replaced by
 * tokens computed by {@link #getReplacement}) is indeed exactly
 * <code>getSubsequenceLength()</code>. If this operator processor's
 * {@link ConditionalOperatorProcessor#identify} method omits to check
 * so and signals it recognizes the tokens, indices can become out of
 * bounds.</p>
 * <p>Subclasses can have state between a successful call to
 * {@link ConditionalOperatorProcessor#identify} and the subsequent
 * call to {@link #getReplacement} by using instance variables, as
 * long as they override
 * {@link PdfOperatorProcessor#process(PageStreamTransform, PDFOperator, List)}
 * as follows:</p>
<pre>
  public synchronized void process(PageStreamTransform pageStreamTransform,
                                   PDFOperator operator,
                                   List operands)
      throws IOException {
    super.process(pageStreamTransform, operator, operands);
  }
</pre>
 * <p>{@link ConditionalSubsequenceOperatorProcessor} instances, like
 * {@link ConditionalOperatorProcessor} instances, <em>must</em> have a
 * no-argument constructor, and are instantiated once per key
 * associated with their class name during a given
 * {@link PageStreamTransform} instantiation.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class ConditionalSubsequenceOperatorProcessor extends ConditionalOperatorProcessor {

  /**
   * <p>Computes a replacement for tokens identified by this
   * operator processor.</p>
   * @param tokens The tokens recognized by
   *               {@link ConditionalOperatorProcessor#identify}.
   * @return A list of replacement tokens to be merged.
   * @see ConditionalOperatorProcessor#identify
   */
  public abstract List getReplacement(List tokens);

  /**
   * <p>Indicates the fixed length of the tail subsequence of the
   * output list which is to be considered by
   * {@link ConditionalOperatorProcessor#identify}.</p>
   * @return The number of tokens to examine from the end of the
   *         output list.
   */
  public abstract int getSubsequenceLength();

  /* Inherit documentation */
  public List getSequence(PageStreamTransform pdfPageStreamTransform) {
    int subsequenceLength = getSubsequenceLength();
    List outputList = pdfPageStreamTransform.getOutputList();
    int outputListSize = outputList.size();
    return Collections.unmodifiableList(new ArrayList(outputListSize > subsequenceLength
                                                      ? outputList.subList(outputListSize - subsequenceLength, outputListSize)
                                                      : outputList));
  }

  /* Inherit documentation */
  public void processIdentified(PageStreamTransform pdfPageStreamTransform,
                                List tokens) {
    int subsequenceLength = getSubsequenceLength();
    List outputList = pdfPageStreamTransform.getOutputList();
    int outputListSize = outputList.size();
    outputList.subList(outputListSize - subsequenceLength, outputListSize).clear();
    outputList.addAll(getReplacement(tokens));
  }

}
