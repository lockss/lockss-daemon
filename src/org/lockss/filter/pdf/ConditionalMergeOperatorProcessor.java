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
 */
@Deprecated
public abstract class ConditionalMergeOperatorProcessor extends ConditionalOperatorProcessor {

  @Deprecated
  public ConditionalMergeOperatorProcessor() {}
  
  /**
   * <p>Computes a replacement for tokens identified by this
   * operator processor.</p>
   * @param tokens The tokens recognized by
   *               {@link ConditionalOperatorProcessor#identify}.
   * @return A list of replacement tokens to be merged.
   * @see ConditionalOperatorProcessor#identify
   */
  public abstract List getReplacement(List tokens);

  /* Inherit documentation */
  public void processIdentified(PageStreamTransform pdfPageStreamTransform,
                                List tokens) {
    pdfPageStreamTransform.mergeOutputList(getReplacement(tokens));
  }

  /* Inherit documentation */
  public void processNotIdentified(PageStreamTransform pdfPageStreamTransform,
                                   List tokens) {
    pdfPageStreamTransform.mergeOutputList();
  }

}
