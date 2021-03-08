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

import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.util.PDFOperator;

/**
 * <p>A PDF operator processor that is overloaded to extract strings
 * from all four string-bearing PDF operators
 * ({@link PdfUtil#SHOW_TEXT},
 * {@link PdfUtil#SHOW_TEXT_GLYPH_POSITIONING},
 * {@link PdfUtil#MOVE_TO_NEXT_LINE_SHOW_TEXT} and
 * {@link PdfUtil#SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT}).
 * @author Thib Guicherd-Callin
 * @see PdfUtil#MOVE_TO_NEXT_LINE_SHOW_TEXT
 * @see PdfUtil#SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT
 * @see PdfUtil#SHOW_TEXT
 * @see PdfUtil#SHOW_TEXT_GLYPH_POSITIONING
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public abstract class ProcessString extends PdfOperatorProcessor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ProcessString() {}

  /* Inherit documentation */
  @Deprecated
  public void process(PageStreamTransform pageStreamTransform,
                      PDFOperator operator,
                      List operands)
      throws IOException {
    logger.debug3("Processing " + operator.getOperation());
    if (PdfUtil.isShowText(operator) || PdfUtil.isMoveToNextLineShowText(operator)) {
      processString(pageStreamTransform,
                    operator,
                    operands,
                    (COSString)operands.get(0));
    }
    else if (PdfUtil.isSetSpacingMoveToNextLineShowText(operator)) {
      processString(pageStreamTransform,
                    operator,
                    operands,
                    (COSString)operands.get(2));
    }
    else if (PdfUtil.isShowTextGlyphPositioning(operator)) {
      COSArray array = (COSArray)operands.get(0);
      for (int elem = 0 ; elem < array.size() ; ++elem) {
        if (PdfUtil.isPdfString(array.get(elem))) {
          processString(pageStreamTransform,
                        operator,
                        operands,
                        (COSString)array.get(elem));
        }
      }
    }
  }

  /**
   * <p>Processes one PDF string (expressed as a {@link COSString}
   * instance).</p>
   * @param pageStreamTransform The PDF page stream transform being
   *                            applied.
   * @param operator            A PDF operator being processed.
   * @param operands            The operands that the operator
   *                            applies to.
   * @param cosString           A PDF string taken from the operands
   *                            (of possibly several).
   * @throws IOException if any processing error occurs.
   * @see PdfUtil#getPdfString(Object)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public abstract void processString(PageStreamTransform pageStreamTransform,
                                     PDFOperator operator,
                                     List operands,
                                     COSString cosString)
      throws IOException;

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(ProcessString.class);

}
