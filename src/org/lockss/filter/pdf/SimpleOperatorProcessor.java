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

import org.pdfbox.util.PDFOperator;

/**
 * <p>A PDF operator processor that simply passes its operands and
 * operator through to the PDF page stream transform's output list
 * unconditionally.</p>
 * <p>{@link SimpleOperatorProcessor} instances, like
 * {@link PdfOperatorProcessor} instances, <em>must</em> have a
 * no-argument constructor, and are instantiated once per key
 * associated with their class name during a given
 * {@link PageStreamTransform} instantiation.</p>
 * @author Thib Guicherd-Callin
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class SimpleOperatorProcessor extends PdfOperatorProcessor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public SimpleOperatorProcessor() {}
  
  /* Inherit documentation */
  @Deprecated
  public void process(PageStreamTransform pageStreamTransform,
                      PDFOperator operator,
                      List operands)
      throws IOException {
    pageStreamTransform.getOutputList().addAll(operands);
    pageStreamTransform.getOutputList().add(operator);
  }

}
