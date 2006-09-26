/*
 * $Id: ProcessString.java,v 1.3 2006-09-26 07:32:24 thib_gc Exp $
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
import java.util.List;

import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.util.PDFOperator;

public abstract class ProcessString extends PdfOperatorProcessor {

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

  public abstract void processString(PageStreamTransform pageStreamTransform,
                                     PDFOperator operator,
                                     List operands,
                                     COSString cosString)
      throws IOException;

  private static Logger logger = Logger.getLogger("ProcessString");

}
