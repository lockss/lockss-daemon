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

package org.lockss.pdf;

import java.util.List;
import java.util.regex.*;

/**
 * <p>
 * A convenience class for the constant strings for useful PDF opcodes, and
 * various utility methods on operators and operands.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public class PdfOpcodes {

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.76
   */
  public static final String BEGIN_INLINE_IMAGE = "BI";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.76
   */
  public static final String BEGIN_INLINE_IMAGE_DATA = "ID";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.76
   */
  public static final String BEGIN_TEXT = "BT";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.76
   */
  public static final String DRAW_OBJECT = "Do";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.76
   */
  public static final String END_TEXT = "ET";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56.3
   */
  public static final String RESTORE = "Q";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.76
   */
  public static final String SAVE = "q";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.76
   */
  public static final String SET_GRAPHICS_STATE_PARAMS = "gs";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.62
   */
  public static final String SET_TEXT_FONT = "Tf";

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.62
   */
  public static final String SET_TEXT_MATRIX = "Tm";

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String SHOW_TEXT = "Tj";

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.76
   */
  public static final String SHOW_TEXT_ADJUSTED = "TJ";

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String SHOW_TEXT_LINE = "'";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String SHOW_TEXT_LINE_AND_SPACE = "\"";

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #BEGIN_INLINE_IMAGE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #BEGIN_INLINE_IMAGE}.}
   * @since 1.76
   */
  public static boolean isBeginInlineImage(PdfToken operator) {
    return isOpcode(operator, BEGIN_INLINE_IMAGE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #BEGIN_INLINE_IMAGE_DATA}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #BEGIN_INLINE_IMAGE_DATA}
   * @since 1.76
   */
  public static boolean isBeginInlineImageData(PdfToken operator) {
    return isOpcode(operator, BEGIN_INLINE_IMAGE_DATA);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #BEGIN_TEXT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #BEGIN_TEXT}
   * @since 1.76
   */
  public static boolean isBeginText(PdfToken operator) {
    return isOpcode(operator, BEGIN_TEXT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #DRAW_OBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #DRAW_OBJECT}
   * @since 1.76
   */
  public static boolean isDrawObject(PdfToken operator) {
    return isOpcode(operator, DRAW_OBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #END_TEXT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #END_TEXT}
   * @since 1.76
   */
  public static boolean isEndText(PdfToken operator) {
    return isOpcode(operator, END_TEXT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the given opcode.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param opcode
   *          An opcode string.
   * @return <code>true</code> if the given operator is the given opcode
   * @since 1.70
   */
  public static boolean isOpcode(PdfToken operator, String opcode) {
    return opcode.equals(operator.getOperator());
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #RESTORE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #RESTORE}
   * @since 1.76
   */
  public static boolean isRestore(PdfToken operator) {
    return isOpcode(operator, RESTORE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SAVE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SAVE}
   * @since 1.76
   */
  public static boolean isSave(PdfToken operator) {
    return isOpcode(operator, SAVE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SET_GRAPHICS_STATE_PARAMS}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SET_GRAPHICS_STATE_PARAMS}
   * @since 1.76
   */
  public static boolean isSetGraphicsStateParams(PdfToken operator) {
    return isOpcode(operator, SET_GRAPHICS_STATE_PARAMS);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SET_TEXT_FONT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SET_TEXT_FONT}
   * @since 1.70
   */
  public static boolean isSetTextFont(PdfToken operator) {
    return isOpcode(operator, SET_TEXT_FONT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SET_TEXT_MATRIX}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SET_TEXT_MATRIX}
   * @since 1.70
   */
  public static boolean isSetTextMatrix(PdfToken operator) {
    return isOpcode(operator, SET_TEXT_MATRIX);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT}
   * @since 1.70
   */
  public static boolean isShowText(PdfToken operator) {
    return isOpcode(operator, SHOW_TEXT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_ADJUSTED}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_ADJUSTED}
   * @since 1.76
   */
  public static boolean isShowTextAdjusted(PdfToken operator) {
    return isOpcode(operator, SHOW_TEXT_ADJUSTED);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_ADJUSTED}, and its equivalent
   * string operand contains the given substring.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param substr
   *          A substring.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_ADJUSTED} and its operand contains the
   *         given substring
   * @since 1.76
   */
  public static boolean isShowTextAdjustedContains(PdfToken operator,
                                                   PdfToken operand,
                                                   String substr) {
    if (isShowTextAdjusted(operator)) {
      StringBuilder sb = new StringBuilder();
      for (PdfToken tok : operand.getArray()) {
        if (tok.isString()) {
          sb.append(tok.getString());
          if (sb.toString().contains(substr)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_ADJUSTED}, and its equivalent
   * string operand ends with the given suffix.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param suffix
   *          A suffix.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_ADJUSTED} and its operand ends with the
   *         given suffix
   * @since 1.76
   */
  public static boolean isShowTextAdjustedEndsWith(PdfToken operator,
                                                   PdfToken operand,
                                                   String suffix) {
    if (isShowTextAdjusted(operator)) {
      StringBuilder sb = new StringBuilder();
      List<PdfToken> array = operand.getArray();
      for (int i = array.size() - 1 ; i >= 0 ; --i) {
        PdfToken tok = array.get(i);
        if (tok.isString()) {
          sb.insert(0, tok.getString());
          if (sb.length() >= suffix.length()) {
            return sb.toString().endsWith(suffix);
          }
        }
      }
    }
    return false;
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_ADJUSTED}, and its equivalent
   * string operand is equal to the given string.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param str
   *          A string.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_ADJUSTED} and its operand is equal to
   *         the given string
   * @since 1.76
   */
  public static boolean isShowTextAdjustedEquals(PdfToken operator,
                                                 PdfToken operand,
                                                 String str) {
    if (isShowTextAdjusted(operator)) {
      StringBuilder sb = new StringBuilder();
      for (PdfToken tok : operand.getArray()) {
        if (tok.isString()) {
          sb.append(tok.getString());
        }
      }
      return sb.toString().equals(str);
    }
    return false;
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_ADJUSTED}, and its equivalent
   * string operand is equal to the given string (case-independently).
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param str
   *          A string.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_ADJUSTED} and its operand is equal to
   *         the given string (case-independently)
   * @since 1.70
   */
  public static boolean isShowTextAdjustedEqualsIgnoreCase(PdfToken operator,
                                                           PdfToken operand,
                                                           String str) {
    if (isShowTextAdjusted(operator)) {
      StringBuilder sb = new StringBuilder();
      for (PdfToken tok : operand.getArray()) {
        if (tok.isString()) {
          sb.append(tok.getString());
        }
      }
      return sb.toString().equalsIgnoreCase(str);
    }
    return false;
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_ADJUSTED}, and its equivalent
   * string operand matches the given pattern (using {@link Matcher#find()},
   * which does not implicitly anchor).
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param pattern
   *          A pattern.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_ADJUSTED} and its operand matches the
   *         given pattern (using {@link Matcher#find()})
   * @since 1.76
   * @see Matcher#find()
   */
  public static boolean isShowTextAdjustedFind(PdfToken operator,
                                               PdfToken operand,
                                               Pattern pattern) {
    if (isShowTextAdjusted(operator)) {
      StringBuilder sb = new StringBuilder();
      for (PdfToken tok : operand.getArray()) {
        if (tok.isString()) {
          sb.append(tok.getString());
        }
      }
      return pattern.matcher(sb.toString()).find();
    }
    return false;
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_ADJUSTED}, and its equivalent
   * string operand starts with the given prefix.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param prefix
   *          A prefix.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_ADJUSTED} and its operand is starts with
   *         the given prefix
   * @since 1.70
   */
  public static boolean isShowTextAdjustedStartsWith(PdfToken operator,
                                                     PdfToken operand,
                                                     String prefix) {
    if (isShowTextAdjusted(operator)) {
      StringBuilder sb = new StringBuilder();
      for (PdfToken tok : operand.getArray()) {
        if (tok.isString()) {
          sb.append(tok.getString());
          if (sb.length() >= prefix.length()) {
            return sb.toString().startsWith(prefix);
          }
        }
      }
    }
    return false;
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT}, and its operand contains the
   * given substring.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as a string.
   * @param substr
   *          A substring.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT} and its operand contains the given
   *         substring.
   * @since 1.70
   */
  public static boolean isShowTextContains(PdfToken operator,
                                           PdfToken operand,
                                           String substr) {
    return isShowText(operator) && operand.getString().contains(substr);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT}, and its operand ends with the
   * given suffix.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as a string.
   * @param suffix
   *          A suffix.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT} and its operand ends with the given
   *         suffix.
   * @since 1.70
   */
  public static boolean isShowTextEndsWith(PdfToken operator,
                                           PdfToken operand,
                                           String suffix) {
    return isShowText(operator) && operand.getString().endsWith(suffix);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT}, and its operand is equal to the
   * given string.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as a string.
   * @param str
   *          A string.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT} and its operand is equal to the given
   *         string.
   * @since 1.70
   */
  public static boolean isShowTextEquals(PdfToken operator,
                                         PdfToken operand,
                                         String str) {
    return isShowText(operator) && operand.getString().equals(str);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT}, and its operand is equal to the
   * given string (case-independently).
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as a string.
   * @param str
   *          A string.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT} and its operand is equal to the given
   *         string (case-independently).
   * @since 1.70
   */
  public static boolean isShowTextEqualsIgnoreCase(PdfToken operator,
                                                   PdfToken operand,
                                                   String str) {
    return isShowText(operator) && operand.getString().equalsIgnoreCase(str);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT}, and its operand matches the
   * given pattern (using {@link Matcher#find()}, which does not implicitly
   * anchor).
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as a string.
   * @param str
   *          A string.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT} and its operand matches the given
   *         pattern (using {@link Matcher#find()})
   * @since 1.70
   * @see Matcher#find()
   */
  public static boolean isShowTextFind(PdfToken operator,
                                       PdfToken operand,
                                       Pattern pattern) {
    return isShowText(operator) && pattern.matcher(operand.getString()).find();
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_LINE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_LINE}
   * @since 1.76
   */
  public static boolean isShowTextLine(PdfToken operator) {
    return isOpcode(operator, SHOW_TEXT_LINE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_LINE_AND_SPACE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_LINE_AND_SPACE}
   * @since 1.76
   */
  public static boolean isShowTextLineAndSpace(PdfToken operator) {
    return isOpcode(operator, SHOW_TEXT_LINE_AND_SPACE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT}, and its operand starts with the
   * given prefix.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as a string.
   * @param prefix
   *          A prefix.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT} and its operand starts with the given
   *         string.
   * @since 1.70
   */
  public static boolean isShowTextStartsWith(PdfToken operator,
                                             PdfToken operand,
                                             String prefix) {
    return isShowText(operator) && operand.getString().startsWith(prefix);
  }

  /**
   * <p>
   * This class cannot be instantiated.
   * </p>
   * 
   * @since 1.56
   */
  private PdfOpcodes() {
    // Prevent instantiation
  }

  /* ***************************************************************************
   * DEPRECATED IN 1.76
   ************************************************************************** */
  
  /**
   * <p>
   * Deprecated: renamed to {@link #BEGIN_INLINE_IMAGE}.
   * </p>
   * 
   * @since 1.56
   * @deprecated renamed to {@link #BEGIN_INLINE_IMAGE}.
   */
  @Deprecated
  public static final String BEGIN_IMAGE_OBJECT = BEGIN_INLINE_IMAGE;
  
  /**
   * <p>
   * Deprecated: renamed to {@link #BEGIN_INLINE_IMAGE_DATA}.
   * </p>
   * 
   * @since 1.56
   * @deprecated renamed to {@link #BEGIN_INLINE_IMAGE_DATA}.
   */
  @Deprecated
  public static final String BEGIN_IMAGE_DATA = BEGIN_INLINE_IMAGE_DATA;
  
  /**
   * <p>
   * Deprecated: renamed to {@link #BEGIN_TEXT}.
   * </p>
   * 
   * @since 1.56
   * @deprecated renamed to {@link #BEGIN_TEXT}.
   */
  @Deprecated
  public static final String BEGIN_TEXT_OBJECT = BEGIN_TEXT;
  
  /**
   * <p>
   * Deprectaed: renamed to {@link #END_TEXT}.
   * </p>
   * 
   * @since 1.56
   * @deprecated renamed to {@link #END_TEXT}.
   */
  @Deprecated
  public static final String END_TEXT_OBJECT = END_TEXT;
  
  /**
   * <p>
   * Deprecated: renamed to {@link #DRAW_OBJECT}.
   * </p>
   * 
   * @since 1.56
   * @deprecated renamed to {@link #DRAW_OBJECT}.
   */
  @Deprecated
  public static final String INVOKE_XOBJECT = DRAW_OBJECT;
  
  /**
   * <p>
   * Deprecated: renamed to {@link #SHOW_TEXT_LINE}.
   * </p>
   * 
   * @since 1.56
   * @deprecated renamed to {@link #SHOW_TEXT_LINE}.
   */
  @Deprecated
  public static final String NEXT_LINE_SHOW_TEXT = SHOW_TEXT_LINE;
  
  /**
   * <p>
   * Deprecated: renamed to {@link #RESTORE}.
   * </p>
   * 
   * @since 1.56.3
   * @deprecated renamed to {@link #RESTORE}.
   */
  @Deprecated
  public static final String RESTORE_GRAPHICS_STATE = RESTORE;
  
  /**
   * <p>
   * Deprecated: renamed to {@link #SAVE}.
   * </p>
   * 
   * @since 1.56.3
   * @deprecated renamed to {@link #SAVE}.
   */
  @Deprecated
  public static final String SAVE_GRAPHICS_STATE = SAVE;
  
  /**
   * <p>
   * Deprecated: renamed to {@link #SHOW_TEXT_LINE_AND_SPACE}.
   * </p>
   * 
   * @since 1.56
   * @deprecated renamed to {@link #SHOW_TEXT_LINE_AND_SPACE}.
   */
  @Deprecated
  public static final String SET_SPACING_NEXT_LINE_SHOW_TEXT = SHOW_TEXT_LINE_AND_SPACE;

  /**
   * <p>
   * Deprecated: renamed to {@link #SHOW_TEXT_ADJUSTED}.
   * </p>
   * 
   * @since 1.56
   * @deprecated renamed to {@link #SHOW_TEXT_ADJUSTED}.
   */
  @Deprecated
  public static final String SHOW_TEXT_GLYPH_POSITIONING = SHOW_TEXT_ADJUSTED;

  /**
   * <p>
   * Deprecated: renamed to {@link #isBeginInlineImageData(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isBeginInlineImageData(PdfToken)}.
   */
  @Deprecated
  public static boolean isBeginImageData(PdfToken operator) {
    return isBeginInlineImageData(operator);
  }

  /**
   * <p>
   * Deprecated renamed to {@link #isBeginInlineImage(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isBeginInlineImage(PdfToken)}.
   */
  @Deprecated
  public static boolean isBeginImageObject(PdfToken operator) {
    return isBeginInlineImage(operator);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isBeginText(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isBeginText(PdfToken)}.
   */
  @Deprecated
  public static boolean isBeginTextObject(PdfToken operator) {
    return isBeginText(operator);
  }

  /**
   * <p>
   * Deprecated renamed to {@link #isEndText(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isEndText(PdfToken)}.
   */
  @Deprecated
  public static boolean isEndTextObject(PdfToken operator) {
    return isEndText(operator);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isDrawObject(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isDrawObject(PdfToken)}.
   */
  @Deprecated
  public static boolean isInvokeXObject(PdfToken operator) {
    return isDrawObject(operator);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isShowTextLine(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isShowTextLine(PdfToken)}.
   */
  @Deprecated
  public static boolean isNextLineShowText(PdfToken operator) {
    return isShowTextLine(operator);
  }

  /**
   * <p>
   * Deprecated renamed to {@link #isRestore(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isRestore(PdfToken)}.
   */
  @Deprecated
  public static boolean isRestoreGraphicsState(PdfToken operator) {
    return isRestore(operator);
  }

  /**
   * <p>
   * Deprecated: @deprecated renamed to {@link #isSave(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isSave(PdfToken)}.
   */
  @Deprecated
  public static boolean isSaveGraphicsState(PdfToken operator) {
    return isSave(operator);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isShowTextLineAndSpace(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isShowTextLineAndSpace(PdfToken)}.
   */
  @Deprecated
  public static boolean isSetSpacingNextLineShowText(PdfToken operator) {
    return isShowTextLineAndSpace(operator);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isShowTextAdjusted(PdfToken)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isShowTextAdjusted(PdfToken)}.
   */
  @Deprecated
  public static boolean isShowTextGlyphPositioning(PdfToken operator) {
    return isShowTextAdjusted(operator);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isShowTextAdjustedContains(PdfToken, PdfToken, String)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isShowTextAdjustedContains(PdfToken, PdfToken, String)}.
   */
  @Deprecated
  public static boolean isShowTextGlyphPositioningContains(PdfToken operator,
                                                           PdfToken operand,
                                                           String substr) {
    return isShowTextAdjustedContains(operator, operand, substr);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isShowTextAdjustedEndsWith(PdfToken, PdfToken, String)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isShowTextAdjustedEndsWith(PdfToken, PdfToken, String)}.
   */
  @Deprecated
  public static boolean isShowTextGlyphPositioningEndsWith(PdfToken operator,
                                                           PdfToken operand,
                                                           String suffix) {
    return isShowTextAdjustedEndsWith(operator, operand, suffix);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isShowTextAdjustedEquals(PdfToken, PdfToken, String)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isShowTextAdjustedEquals(PdfToken, PdfToken, String)}.
   */
  public static boolean isShowTextGlyphPositioningEquals(PdfToken operator,
                                                         PdfToken operand,
                                                         String str) {
    return isShowTextAdjustedEquals(operator, operand, str);
  }

  /**
   * <p>
   * Deprecated: renamed to {@link #isShowTextAdjustedEqualsIgnoreCase(PdfToken, PdfToken, String)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated: renamed to {@link #isShowTextAdjustedEqualsIgnoreCase(PdfToken, PdfToken, String)}.
   */
  @Deprecated
  public static boolean isShowTextGlyphPositioningEqualsIgnoreCase(PdfToken operator,
                                                                   PdfToken operand,
                                                                   String str) {
    return isShowTextAdjustedEqualsIgnoreCase(operator, operand, str);
  }

  /**
   * <p>
   * Deprecated renamed to {@link #isShowTextAdjustedFind(PdfToken, PdfToken, Pattern)}.
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to {@link #isShowTextAdjustedFind(PdfToken, PdfToken, Pattern)}.
   */
  @Deprecated
  public static boolean isShowTextGlyphPositioningFind(PdfToken operator,
                                                       PdfToken operand,
                                                       Pattern pattern) {
    return isShowTextAdjustedFind(operator, operand, pattern);
  }

  /**
   * <p>
   * Deprecated: renamed to #isShowTextAdjustedStartsWith(PdfToken, PdfToken, String).
   * </p>
   * 
   * @since 1.70
   * @deprecated renamed to #isShowTextAdjustedStartsWith(PdfToken, PdfToken, String).
   */
  @Deprecated
  public static boolean isShowTextGlyphPositioningStartsWith(PdfToken operator,
                                                             PdfToken operand,
                                                             String prefix) {
    return isShowTextAdjustedStartsWith(operator, operand, prefix);
  }

}
