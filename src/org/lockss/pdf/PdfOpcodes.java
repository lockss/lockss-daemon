/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
   * @since 1.56
   */
  public static final String BEGIN_IMAGE_DATA = "ID";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String BEGIN_IMAGE_OBJECT = "BI";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String BEGIN_TEXT_OBJECT = "BT";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String END_TEXT_OBJECT = "ET";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String INVOKE_XOBJECT = "Do";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String NEXT_LINE_SHOW_TEXT = "'";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56.3
   */
  public static final String RESTORE_GRAPHICS_STATE = "Q";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56.3
   */
  public static final String SAVE_GRAPHICS_STATE = "q";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String SET_SPACING_NEXT_LINE_SHOW_TEXT = "\"";

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
   * @since 1.56
   */
  public static final String SHOW_TEXT_GLYPH_POSITIONING = "TJ";

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #BEGIN_IMAGE_DATA}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #BEGIN_IMAGE_DATA}
   * @since 1.70
   */
  public static boolean isBeginImageData(PdfToken operator) {
    return isOpcode(operator, BEGIN_IMAGE_DATA);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #BEGIN_IMAGE_OBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #BEGIN_IMAGE_OBJECT}
   * @since 1.70
   */
  public static boolean isBeginImageObject(PdfToken operator) {
    return isOpcode(operator, BEGIN_IMAGE_OBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #BEGIN_TEXT_OBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #BEGIN_TEXT_OBJECT}
   * @since 1.70
   */
  public static boolean isBeginTextObject(PdfToken operator) {
    return isOpcode(operator, BEGIN_TEXT_OBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #END_TEXT_OBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #END_TEXT_OBJECT}
   * @since 1.70
   */
  public static boolean isEndTextObject(PdfToken operator) {
    return isOpcode(operator, END_TEXT_OBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #INVOKE_XOBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #INVOKE_XOBJECT}
   * @since 1.70
   */
  public static boolean isInvokeXObject(PdfToken operator) {
    return isOpcode(operator, INVOKE_XOBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #NEXT_LINE_SHOW_TEXT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #NEXT_LINE_SHOW_TEXT}
   * @since 1.70
   */
  public static boolean isNextLineShowText(PdfToken operator) {
    return isOpcode(operator, NEXT_LINE_SHOW_TEXT);
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
   * is the opcode {@link #RESTORE_GRAPHICS_STATE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #RESTORE_GRAPHICS_STATE}
   * @since 1.70
   */
  public static boolean isRestoreGraphicsState(PdfToken operator) {
    return isOpcode(operator, RESTORE_GRAPHICS_STATE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SAVE_GRAPHICS_STATE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SAVE_GRAPHICS_STATE}
   * @since 1.70
   */
  public static boolean isSaveGraphicsState(PdfToken operator) {
    return isOpcode(operator, SAVE_GRAPHICS_STATE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SET_SPACING_NEXT_LINE_SHOW_TEXT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SET_SPACING_NEXT_LINE_SHOW_TEXT}
   * @since 1.70
   */
  public static boolean isSetSpacingNextLineShowText(PdfToken operator) {
    return isOpcode(operator, SET_SPACING_NEXT_LINE_SHOW_TEXT);
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
   * is the opcode {@link #SHOW_TEXT_GLYPH_POSITIONING}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link #SHOW_TEXT_GLYPH_POSITIONING}
   * @since 1.70
   */
  public static boolean isShowTextGlyphPositioning(PdfToken operator) {
    return isOpcode(operator, SHOW_TEXT_GLYPH_POSITIONING);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link #SHOW_TEXT_GLYPH_POSITIONING}, and its equivalent
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
   *         {@link #SHOW_TEXT_GLYPH_POSITIONING} and its operand contains the
   *         given substring
   * @since 1.70
   */
  public static boolean isShowTextGlyphPositioningContains(PdfToken operator,
                                                           PdfToken operand,
                                                           String substr) {
    if (isShowTextGlyphPositioning(operator)) {
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
   * is the opcode {@link #SHOW_TEXT_GLYPH_POSITIONING}, and its equivalent
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
   *         {@link #SHOW_TEXT_GLYPH_POSITIONING} and its operand ends with the
   *         given suffix
   * @since 1.70
   */
  public static boolean isShowTextGlyphPositioningEndsWith(PdfToken operator,
                                                           PdfToken operand,
                                                           String suffix) {
    if (isShowTextGlyphPositioning(operator)) {
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
   * is the opcode {@link #SHOW_TEXT_GLYPH_POSITIONING}, and its equivalent
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
   *         {@link #SHOW_TEXT_GLYPH_POSITIONING} and its operand is equal to
   *         the given string
   * @since 1.70
   */
  public static boolean isShowTextGlyphPositioningEquals(PdfToken operator,
                                                         PdfToken operand,
                                                         String str) {
    if (isShowTextGlyphPositioning(operator)) {
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
   * is the opcode {@link #SHOW_TEXT_GLYPH_POSITIONING}, and its equivalent
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
   *         {@link #SHOW_TEXT_GLYPH_POSITIONING} and its operand is equal to
   *         the given string (case-independently)
   * @since 1.70
   */
  public static boolean isShowTextGlyphPositioningEqualsIgnoreCase(PdfToken operator,
                                                                   PdfToken operand,
                                                                   String str) {
    if (isShowTextGlyphPositioning(operator)) {
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
   * is the opcode {@link #SHOW_TEXT_GLYPH_POSITIONING}, and its equivalent
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
   *         {@link #SHOW_TEXT_GLYPH_POSITIONING} and its operand matches the
   *         given pattern (using {@link Matcher#find()})
   * @since 1.70
   * @see Matcher#find()
   */
  public static boolean isShowTextGlyphPositioningFind(PdfToken operator,
                                                       PdfToken operand,
                                                       Pattern pattern) {
    if (isShowTextGlyphPositioning(operator)) {
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
   * is the opcode {@link #SHOW_TEXT_GLYPH_POSITIONING}, and its equivalent
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
   *         {@link #SHOW_TEXT_GLYPH_POSITIONING} and its operand is starts with
   *         the given prefix
   * @since 1.70
   */
  public static boolean isShowTextGlyphPositioningStartsWith(PdfToken operator,
                                                             PdfToken operand,
                                                             String prefix) {
    if (isShowTextGlyphPositioning(operator)) {
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

}
