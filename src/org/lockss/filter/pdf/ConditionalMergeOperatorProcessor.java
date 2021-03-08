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
 * <p>A conditional operator processor that conditionally merges the
 * output list of the page stream transform being applied, based on
 * the contents of the current output list.</p>
 * <p>This operator processor is to be associated with operators,
 * typically no-operand operators such as "end text object", that end
 * the accumulation of tokens into a sublist. It will usually be
 * paired with an unconditional split (see
 * {@link SplitOperatorProcessor}) or a conditional split.</p>
 * <p>In summary, the behavior of this class is as follows:</p>
 * <ul>
 *  <li>{@link #processIdentified} merges the result of
 *  {@link #getReplacement}.</li>
 *  <li>{@link #processNotIdentified} simply merges.</li>
 * </ul>
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
 * <p>{@link ConditionalMergeOperatorProcessor} instances, like
 * {@link ConditionalOperatorProcessor} instances, <em>must</em> have a
 * no-argument constructor, and are instantiated once per key
 * associated with their class name during a given
 * {@link PageStreamTransform} instantiation.</p>
 * @author Thib Guicherd-Callin
 * @see PageStreamTransform#splitOutputList
 * @see PageStreamTransform#mergeOutputList()
 * @see PageStreamTransform#mergeOutputList(List)
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public abstract class ConditionalMergeOperatorProcessor extends ConditionalOperatorProcessor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalMergeOperatorProcessor() {}
  
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

  /* Inherit documentation */
  @Deprecated
  public void processIdentified(PageStreamTransform pdfPageStreamTransform,
                                List tokens) {
    pdfPageStreamTransform.mergeOutputList(getReplacement(tokens));
  }

  /* Inherit documentation */
  @Deprecated
  public void processNotIdentified(PageStreamTransform pdfPageStreamTransform,
                                   List tokens) {
    pdfPageStreamTransform.mergeOutputList();
  }

}
