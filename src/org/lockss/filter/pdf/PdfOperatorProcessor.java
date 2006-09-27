/*
 * $Id: PdfOperatorProcessor.java,v 1.7 2006-09-27 08:00:33 thib_gc Exp $
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

import org.pdfbox.util.PDFOperator;
import org.pdfbox.util.operator.OperatorProcessor;

/**
 * <p>A PDF operator processor that is specialized to work in the
 * context of a PDF page stream transform.</p>
 * <p>{@link PdfOperatorProcessor} instances, like
 * {@link OperatorProcessor} instances, <em>must</em> have a
 * no-argument constructor, and are instantiated once per key
 * associated with their class name during a given
 * {@link PageStreamTransform} instantiation.</p>
 * @author Thib Guicherd-Callin
 * @see PageStreamTransform
 */
public abstract class PdfOperatorProcessor extends OperatorProcessor {

  /**
   * <p>Inherited from {@link OperatorProcessor}; simply calls
   * {@link #process(PageStreamTransform, PDFOperator, List)}
   * with the context being the current PDF page stream transform.</p>
   * @param operator  A PDF operator being processed.
   * @param arguments The operands that the operator applies to.
   * @see #process(PageStreamTransform, PDFOperator, List)
   * @see OperatorProcessor#getContext
   */
  public void process(PDFOperator operator,
                      List arguments)
      throws IOException {
    process((PageStreamTransform)getContext(),
            operator,
            Collections.unmodifiableList(arguments));
  }

  /**
   * <p>Processes the operation (operator and operands) in the context
   * of the given PDF page stream transform.</p>
   * @param pageStreamTransform The PDF page stream transform being
   *                            applied.
   * @param operator            A PDF operator being processed.
   * @param operands            The operands that the operator
   *                            applies to.
   * @throws IOException if any processing error occurs.
   * @see #process(PDFOperator, List)
   */
  public abstract void process(PageStreamTransform pageStreamTransform,
                               PDFOperator operator,
                               List operands)
      throws IOException;

}
