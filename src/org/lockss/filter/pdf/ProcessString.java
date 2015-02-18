/*
 * $Id$
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
 */
@Deprecated
public abstract class ProcessString extends PdfOperatorProcessor {

  @Deprecated
  public ProcessString() {}
  
  /* Inherit documentation */
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
   */
  public abstract void processString(PageStreamTransform pageStreamTransform,
                                     PDFOperator operator,
                                     List operands,
                                     COSString cosString)
      throws IOException;

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("ProcessString");

}
