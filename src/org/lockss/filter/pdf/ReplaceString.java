/*
 * $Id: ReplaceString.java,v 1.4 2006-09-27 08:00:32 thib_gc Exp $
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

/**
 * <p>A PDF operator processor that deals with the "show text" PDF
 * operator (<code>Tj</code>), replacing the operand string by
 * another conditionally.</p>
 * <p>{@link ReplaceString} instances, like
 * {@link PdfOperatorProcessor} instances, <em>must</em> have a
 * no-argument constructor, and are instantiated once per key
 * associated with their class name during a given
 * {@link PageStreamTransform} instantiation.</p>
 * @author Thib Guicherd-Callin
 * @see PdfUtil#SHOW_TEXT
 */
public abstract class ReplaceString extends PdfOperatorProcessor {

  /**
   * <p>Determines if an operand string is to be replaced.</p>
   * @param candidate A candidate string (operand of the "show text"
   *                  operator).
   * @return True if the candidate string matches the pattern this
   *         operator processor is looking for and should be replaced
   *         by {@link #getReplacement}, false otherwise.
   * @see #getReplacement
   */
  public abstract boolean identify(String candidate);

  /**
   * <p>Computes a replacement for a matched operand string.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>candidateMatches(match)</code></li>
   * </ul>
   * @param match The operand string that was matched.
   * @return A replacement for the matched string.
   */
  public abstract String getReplacement(String match);

  /* Inherit documentation */
  public void process(PageStreamTransform pageStreamTransform,
                      PDFOperator operator,
                      List operands)
      throws IOException {
    String candidate = PdfUtil.getPdfString(operands, 0);
    List outputList = pageStreamTransform.getOutputList();
    if (identify(candidate)) {
      String replacement = getReplacement(candidate);
      logger.debug3("Replacing \"" + candidate + "\" by \"" + replacement + "\"");
      pageStreamTransform.signalChange();
      outputList.add(new COSString(replacement));
      outputList.add(operator);
    }
    else {
      logger.debug3("Keeping \"" + candidate + "\"");
      outputList.addAll(operands);
      outputList.add(operator);
    }
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("ReplaceString");

}
