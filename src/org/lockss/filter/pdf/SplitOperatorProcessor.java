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
import java.util.List;

import org.lockss.util.Logger;
import org.pdfbox.util.PDFOperator;

/**
 * <p>A PDF operator processor that unconditionally splits the output
 * list of the PDF page stream transform being applied, then simply
 * passes its operands and operator into the output sublist
 * unconditionally.</p>
 * <p>This operator processor is to be associated with operators,
 * typically no-operand operators such as "begin text object"
 * (<code>BT</code>), that start sublists and accumulate content up
 * until some end condition, typically an end marker such as "end
 * text object" (<code>ET</code>). The matching end condition will
 * then need to merge the output sublist, or split/merge mismatches
 * will occur.</p>
 * <p>For example, this operator processor could be associated with
 * "begin text object", and the operator processor for "end text
 * object" could merge with or without replacement based on a
 * condition derived from the contents of the output sublist. See
 * {@link ConditionalMergeOperatorProcessor} and
 * {@link ConditionalSubsequenceOperatorProcessor}.</p>
 * <p>{@link SplitOperatorProcessor} instances, like
 * {@link PdfOperatorProcessor} instances, <em>must</em> have a
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
public class SplitOperatorProcessor extends PdfOperatorProcessor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public SplitOperatorProcessor() {}
  
  /* Inherit documentation */
  @Deprecated
  public void process(PageStreamTransform pageStreamTransform,
                      PDFOperator operator,
                      List operands)
      throws IOException {
    logger.debug3("Processing " + operator.getOperation());
    pageStreamTransform.splitOutputList();
    pageStreamTransform.getOutputList().addAll(operands);
    pageStreamTransform.getOutputList().add(operator);
  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(SplitOperatorProcessor.class);

}
