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
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public abstract class ReplaceString extends PdfOperatorProcessor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ReplaceString() {}
  
  /**
   * <p>Determines if an operand string is to be replaced.</p>
   * @param candidate A candidate string (operand of the "show text"
   *                  operator).
   * @return True if the candidate string matches the pattern this
   *         operator processor is looking for and should be replaced
   *         by {@link #getReplacement}, false otherwise.
   * @see #getReplacement
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public abstract boolean identify(String candidate);

  /**
   * <p>Computes a replacement for a matched operand string.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>candidateMatches(match)</code></li>
   * </ul>
   * @param match The operand string that was matched.
   * @return A replacement for the matched string.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public abstract String getReplacement(String match);

  /* Inherit documentation */
  @Deprecated
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
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(ReplaceString.class);

}
