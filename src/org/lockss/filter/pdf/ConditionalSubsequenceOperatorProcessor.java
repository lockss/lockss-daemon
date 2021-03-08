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
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public abstract class ConditionalSubsequenceOperatorProcessor extends ConditionalOperatorProcessor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalSubsequenceOperatorProcessor() {}
  
  /**
   * <p>Computes a replacement for tokens identified by this
   * operator processor.</p>
   * @param tokens The tokens recognized by
   *               {@link ConditionalOperatorProcessor#identify}.
   * @return A list of replacement tokens to be merged.
   * @see ConditionalOperatorProcessor#identify
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public abstract List getReplacement(List tokens);

  /**
   * <p>Indicates the fixed length of the tail subsequence of the
   * output list which is to be considered by
   * {@link ConditionalOperatorProcessor#identify}.</p>
   * @return The number of tokens to examine from the end of the
   *         output list.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public abstract int getSubsequenceLength();

  /* Inherit documentation */
  @Deprecated
  public List getSequence(PageStreamTransform pdfPageStreamTransform) {
    int subsequenceLength = getSubsequenceLength();
    List outputList = pdfPageStreamTransform.getOutputList();
    int outputListSize = outputList.size();
    return Collections.unmodifiableList(new ArrayList(outputListSize > subsequenceLength
                                                      ? outputList.subList(outputListSize - subsequenceLength, outputListSize)
                                                      : outputList));
  }

  /* Inherit documentation */
  @Deprecated
  public void processIdentified(PageStreamTransform pdfPageStreamTransform,
                                List tokens) {
    int subsequenceLength = getSubsequenceLength();
    List outputList = pdfPageStreamTransform.getOutputList();
    int outputListSize = outputList.size();
    outputList.subList(outputListSize - subsequenceLength, outputListSize).clear();
    outputList.addAll(getReplacement(tokens));
  }

}
