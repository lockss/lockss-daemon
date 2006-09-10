/*
 * $Id: PdfUtil.java,v 1.4 2006-09-10 07:50:51 thib_gc Exp $
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

package org.lockss.util;

import java.io.*;
import java.util.Iterator;

import org.apache.commons.collections.iterators.*;
import org.lockss.filter.pdf.*;
import org.pdfbox.cos.*;
import org.pdfbox.util.PDFOperator;

/**
 * <p>Utilities for PDF processing and filtering.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfUtil {

  public interface ResultPolicy {
    boolean resetResult();
    boolean updateResult(boolean currentResult, boolean update);
    boolean shouldKeepGoing(boolean currentResult);
  }

  public static final ResultPolicy EXHAUSTIVE_AND = new ResultPolicy() {
    public boolean resetResult() {
      return true;
    }
    public boolean shouldKeepGoing(boolean currentResult) {
      return true;
    }
    public boolean updateResult(boolean currentResult, boolean update) {
      return currentResult && update;
    }
  };

  public static final ResultPolicy AND = new ResultPolicy() {
    public boolean resetResult() {
      return true;
    }
    public boolean shouldKeepGoing(boolean currentResult) {
      return currentResult;
    }
    public boolean updateResult(boolean currentResult, boolean update) {
      return currentResult && update;
    }
  };

  public static final ResultPolicy EXHAUSTIVE_OR = new ResultPolicy() {
    public boolean resetResult() {
      return false;
    }
    public boolean shouldKeepGoing(boolean currentResult) {
      return true;
    }
    public boolean updateResult(boolean currentResult, boolean update) {
      return currentResult || update;
    }
  };

  public static final ResultPolicy OR = new ResultPolicy() {
    public boolean resetResult() {
      return false;
    }
    public boolean shouldKeepGoing(boolean currentResult) {
      return !currentResult;
    }
    public boolean updateResult(boolean currentResult, boolean update) {
      return currentResult || update;
    }
  };

  /**
   * <p>A PDF page transform that does nothing, for testing.</p>
   * @author Thib Guicherd-Callin
   */
  public static class IdentityPageTransform implements PageTransform {

    protected boolean returnValue;

    public IdentityPageTransform() {
      this(true);
    }

    public IdentityPageTransform(boolean returnValue) {
      this.returnValue = returnValue;
    }

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      logger.debug3("Indentity page transform: " + returnValue);
      return returnValue;
    }

  }

  /**
   * <p>A PDF transform that does nothing, for testing.</p>
   * @author Thib Guicherd-Callin
   */
  public static class IdentityDocumentTransform implements DocumentTransform {

    protected boolean returnValue;

    public IdentityDocumentTransform() {
      this(true);
    }

    public IdentityDocumentTransform(boolean returnValue) {
      this.returnValue = returnValue;
    }

    /* Inherit documentation */
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      logger.debug3("Indentity document transform: " + returnValue);
      return returnValue;
    }

  }

  /**
   * <p>The PDF <code>c</code> operator string.</p>
   */
  public static final String APPEND_CURVED_SEGMENT = "c";

  /**
   * <p>The PDF <code>y</code> operator string.</p>
   */
  public static final String APPEND_CURVED_SEGMENT_FINAL = "y";

  /**
   * <p>The PDF <code>v</code> operator string.</p>
   */
  public static final String APPEND_CURVED_SEGMENT_INITIAL = "v";

  /**
   * <p>The PDF <code>re</code> operator string.</p>
   */
  public static final String APPEND_RECTANGLE = "re";

  /**
   * <p>The PDF <code>l</code> operator string.</p>
   */
  public static final String APPEND_STRAIGHT_LINE_SEGMENT = "l";

  /**
   * <p>The PDF <code>BX</code> operator string.</p>
   */
  public static final String BEGIN_COMPATIBILITY_SECTION = "BX";

  /**
   * <p>The PDF <code>ID</code> operator string.</p>
   */
  public static final String BEGIN_INLINE_IMAGE_DATA = "ID";

  /**
   * <p>The PDF <code>BI</code> operator string.</p>
   */
  public static final String BEGIN_INLINE_IMAGE_OBJECT = "BI";

  /**
   * <p>The PDF <code>BMC</code> operator string.</p>
   */
  public static final String BEGIN_MARKED_CONTENT = "BMC";

  /**
   * <p>The PDF <code>BDC</code> operator string.</p>
   */
  public static final String BEGIN_MARKED_CONTENT_PROP = "BDC";

  /**
   * <p>The PDF <code>m</code> operator string.</p>
   */
  public static final String BEGIN_NEW_SUBPATH = "m";

  /**
   * <p>The PDF <code>BT</code> operator string.</p>
   */
  public static final String BEGIN_TEXT_OBJECT = "BT";

  /**
   * <p>The PDF <code>b*</code> operator string.</p>
   */
  public static final String CLOSE_FILL_STROKE_EVENODD = "b*";

  /**
   * <p>The PDF <code>b</code> operator string.</p>
   */
  public static final String CLOSE_FILL_STROKE_NONZERO = "b";

  /**
   * <p>The PDF <code>s</code> operator string.</p>
   */
  public static final String CLOSE_STROKE = "s";

  /**
   * <p>The PDF <code>h</code> operator string.</p>
   */
  public static final String CLOSE_SUBPATH = "h";

  /**
   * <p>The PDF <code>cm</code> operator string.</p>
   */
  public static final String CONCATENATE_MATRIX = "cm";

  /**
   * <p>The PDF <code>MP</code> operator string.</p>
   */
  public static final String DEFINE_MARKED_CONTENT_POINT = "MP";

  /**
   * <p>The PDF <code>DP</code> operator string.</p>
   */
  public static final String DEFINE_MARKED_CONTENT_POINT_PROP = "DP";

  /**
   * <p>The PDF <code>EX</code> operator string.</p>
   */
  public static final String END_COMPATIBILITY_SECTION = "EX";

  /**
   * <p>The PDF <code>EI</code> operator string.</p>
   */
  public static final String END_INLINE_IMAGE_OBJECT = "EI";

  /**
   * <p>The PDF <code>EMC</code> operator string.</p>
   */
  public static final String END_MARKED_CONTENT = "EMC";

  /**
   * <p>The PDF <code>n</code> operator string.</p>
   */
  public static final String END_PATH = "n";

  /**
   * <p>The PDF <code>ET</code> operator string.</p>
   */
  public static final String END_TEXT_OBJECT = "ET";

  /**
   * <p>The PDF <code>f*</code> operator string.</p>
   */
  public static final String FILL_EVENODD = "f*";

  /**
   * <p>The PDF <code>f</code> operator string.</p>
   */
  public static final String FILL_NONZERO = "f";

  /**
   * <p>The PDF <code>F</code> operator string.</p>
   */
  public static final String FILL_NONZERO_OBSOLETE = "F";

  /**
   * <p>The PDF <code>B*</code> operator string.</p>
   */
  public static final String FILL_STROKE_EVENODD = "B*";

  /**
   * <p>The PDF <code>B</code> operator string.</p>
   */
  public static final String FILL_STROKE_NONZERO = "B";

  /**
   * <p>The PDF <code>Do</code> operator string.</p>
   */
  public static final String INVOKE_NAMED_XOBJECT = "Do";

  /**
   * <p>The PDF <code>Td</code> operator string.</p>
   */
  public static final String MOVE_TEXT_POSITION = "Td";

  /**
   * <p>The PDF <code>TD</code> operator string.</p>
   */
  public static final String MOVE_TEXT_POSITION_SET_LEADING = "TD";

  /**
   * <p>The PDF <code>T*</code> operator string.</p>
   */
  public static final String MOVE_TO_NEXT_LINE = "T*";

  /**
   * <p>The PDF <code>'</code> operator string.</p>
   */
  public static final String MOVE_TO_NEXT_LINE_SHOW_TEXT = "\'";

  /**
   * <p>The PDF <code>sh</code> operator string.</p>
   */
  public static final String PAINT_SHADING_PATTERN = "sh";

  /**
   * <p>The PDF <code>Q</code> operator string.</p>
   */
  public static final String RESTORE_GRAPHICS_STATE = "Q";

  /**
   * <p>The PDF <code>q</code> operator string.</p>
   */
  public static final String SAVE_GRAPHICS_STATE = "q";

  /**
   * <p>The PDF <code>Tc</code> operator string.</p>
   */
  public static final String SET_CHARACTER_SPACING = "Tc";

  /**
   * <p>The PDF <code>W*</code> operator string.</p>
   */
  public static final String SET_CLIPPING_PATH_EVENODD = "W*";

  /**
   * <p>The PDF <code>W</code> operator string.</p>
   */
  public static final String SET_CLIPPING_PATH_NONZERO = "W";

  /**
   * <p>The PDF <code>k</code> operator string.</p>
   */
  public static final String SET_CMYK_COLOR_NONSTROKING = "k";

  /**
   * <p>The PDF <code>K</code> operator string.</p>
   */
  public static final String SET_CMYK_COLOR_STROKING = "K";

  /**
   * <p>The PDF <code>sc</code> operator string.</p>
   */
  public static final String SET_COLOR_NONSTROKING = "sc";

  /**
   * <p>The PDF <code>scn</code> operator string.</p>
   */
  public static final String SET_COLOR_NONSTROKING_SPECIAL = "scn";

  /**
   * <p>The PDF <code>ri</code> operator string.</p>
   */
  public static final String SET_COLOR_RENDERING_INTENT = "ri";

  /**
   * <p>The PDF <code>cs</code> operator string.</p>
   */
  public static final String SET_COLOR_SPACE_NONSTROKING = "cs";

  /**
   * <p>The PDF <code>CS</code> operator string.</p>
   */
  public static final String SET_COLOR_SPACE_STROKING = "CS";

  /**
   * <p>The PDF <code>SC</code> operator string.</p>
   */
  public static final String SET_COLOR_STROKING = "SC";

  /**
   * <p>The PDF <code>SCN</code> operator string.</p>
   */
  public static final String SET_COLOR_STROKING_SPECIAL = "SCN";

  /**
   * <p>The PDF <code>i</code> operator string.</p>
   */
  public static final String SET_FLATNESS_TOLERANCE = "i";

  /**
   * <p>The PDF <code>gs</code> operator string.</p>
   */
  public static final String SET_FROM_GRAPHICS_STATE = "gs";

  /**
   * <p>The PDF <code>d0</code> operator string.</p>
   */
  public static final String SET_GLYPH_WIDTH = "d0";

  /**
   * <p>The PDF <code>d1</code> operator string.</p>
   */
  public static final String SET_GLYPH_WIDTH_BOUNDING_BOX = "d1";

  /**
   * <p>The PDF <code>g</code> operator string.</p>
   */
  public static final String SET_GRAY_LEVEL_NONSTROKING = "g";

  /**
   * <p>The PDF <code>G</code> operator string.</p>
   */
  public static final String SET_GRAY_LEVEL_STROKING = "G";

  /**
   * <p>The PDF <code>Tz</code> operator string.</p>
   */
  public static final String SET_HORIZONTAL_TEXT_SCALING = "Tz";

  /**
   * <p>The PDF <code>J</code> operator string.</p>
   */
  public static final String SET_LINE_CAP_STYLE = "J";

  /**
   * <p>The PDF <code>d</code> operator string.</p>
   */
  public static final String SET_LINE_DASH_PATTERN = "d";

  /**
   * <p>The PDF <code>j</code> operator string.</p>
   */
  public static final String SET_LINE_JOIN_STYLE = "j";

  /**
   * <p>The PDF <code>w</code> operator string.</p>
   */
  public static final String SET_LINE_WIDTH = "w";

  /**
   * <p>The PDF <code>M</code> operator string.</p>
   */
  public static final String SET_MITER_LIMIT = "M";

  /**
   * <p>The PDF <code>rg</code> operator string.</p>
   */
  public static final String SET_RGB_COLOR_NONSTROKING = "rg";

  /**
   * <p>The PDF <code>RG</code> operator string.</p>
   */
  public static final String SET_RGB_COLOR_STROKING = "RG";

  /**
   * <p>The PDF <code>"</code> operator string.</p>
   */
  public static final String SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT = "\"";

  /**
   * <p>The PDF <code>Tf</code> operator string.</p>
   */
  public static final String SET_TEXT_FONT_AND_SIZE = "Tf";

  /**
   * <p>The PDF <code>TL</code> operator string.</p>
   */
  public static final String SET_TEXT_LEADING = "TL";

  /**
   * <p>The PDF <code>Tm</code> operator string.</p>
   */
  public static final String SET_TEXT_MATRIX = "Tm";

  /**
   * <p>The PDF <code>Tr</code> operator string.</p>
   */
  public static final String SET_TEXT_RENDERING_MODE = "Tr";

  /**
   * <p>The PDF <code>Ts</code> operator string.</p>
   */
  public static final String SET_TEXT_RISE = "Ts";

  /**
   * <p>The PDF <code>Tw</code> operator string.</p>
   */
  public static final String SET_WORD_SPACING = "Tw";

  /**
   * <p>The PDF <code>Tj</code> operator string.</p>
   */
  public static final String SHOW_TEXT = "Tj";

  /**
   * <p>The PDF <code>TJ</code> operator string.</p>
   */
  public static final String SHOW_TEXT_GLYPH_POSITIONING = "TJ";

  /**
   * <p>The PDF <code>S</code> operator string.</p>
   */
  public static final String STROKE = "S";

  /**
   * <p>A logger for use by this class.</p>
   */
  protected static Logger logger = Logger.getLogger("PdfUtil");

  /**
   * <p>All 73 operators defined by PDF 1.6, in the order they are
   * listed in the specification (Appendix A).</p>
   * @see <a href="http://partners.adobe.com/public/developer/en/pdf/PDFReference16.pdf">PDF Reference, Fifth Edition, Version 1.6</a>
   */
  protected static final String[] PDF_1_6_OPERATORS = {
    CLOSE_FILL_STROKE_NONZERO,
    FILL_STROKE_NONZERO,
    CLOSE_FILL_STROKE_EVENODD,
    FILL_STROKE_EVENODD,
    BEGIN_MARKED_CONTENT_PROP,
    BEGIN_INLINE_IMAGE_OBJECT,
    BEGIN_MARKED_CONTENT,
    BEGIN_TEXT_OBJECT,
    BEGIN_COMPATIBILITY_SECTION,
    APPEND_CURVED_SEGMENT,
    CONCATENATE_MATRIX,
    SET_COLOR_SPACE_STROKING,
    SET_COLOR_SPACE_NONSTROKING,
    SET_LINE_DASH_PATTERN,
    SET_GLYPH_WIDTH,
    SET_GLYPH_WIDTH_BOUNDING_BOX,
    INVOKE_NAMED_XOBJECT,
    DEFINE_MARKED_CONTENT_POINT_PROP,
    END_INLINE_IMAGE_OBJECT,
    END_MARKED_CONTENT,
    END_TEXT_OBJECT,
    END_COMPATIBILITY_SECTION,
    FILL_NONZERO,
    FILL_NONZERO_OBSOLETE,
    FILL_EVENODD,
    SET_GRAY_LEVEL_STROKING,
    SET_GRAY_LEVEL_NONSTROKING,
    SET_FROM_GRAPHICS_STATE,
    CLOSE_SUBPATH,
    SET_FLATNESS_TOLERANCE,
    BEGIN_INLINE_IMAGE_DATA,
    SET_LINE_JOIN_STYLE,
    SET_LINE_CAP_STYLE,
    SET_CMYK_COLOR_STROKING,
    SET_CMYK_COLOR_NONSTROKING,
    APPEND_STRAIGHT_LINE_SEGMENT,
    BEGIN_NEW_SUBPATH,
    SET_MITER_LIMIT,
    DEFINE_MARKED_CONTENT_POINT,
    END_PATH,
    SAVE_GRAPHICS_STATE,
    RESTORE_GRAPHICS_STATE,
    APPEND_RECTANGLE,
    SET_RGB_COLOR_STROKING,
    SET_RGB_COLOR_NONSTROKING,
    SET_COLOR_RENDERING_INTENT,
    CLOSE_STROKE,
    STROKE,
    SET_COLOR_STROKING,
    SET_COLOR_NONSTROKING,
    SET_COLOR_STROKING_SPECIAL,
    SET_COLOR_NONSTROKING_SPECIAL,
    PAINT_SHADING_PATTERN,
    MOVE_TO_NEXT_LINE,
    SET_CHARACTER_SPACING,
    MOVE_TEXT_POSITION,
    MOVE_TEXT_POSITION_SET_LEADING,
    SET_TEXT_FONT_AND_SIZE,
    SHOW_TEXT,
    SHOW_TEXT_GLYPH_POSITIONING,
    SET_TEXT_LEADING,
    SET_TEXT_MATRIX,
    SET_TEXT_RENDERING_MODE,
    SET_TEXT_RISE,
    SET_WORD_SPACING,
    SET_HORIZONTAL_TEXT_SCALING,
    APPEND_CURVED_SEGMENT_INITIAL,
    SET_LINE_WIDTH,
    SET_CLIPPING_PATH_NONZERO,
    SET_CLIPPING_PATH_EVENODD,
    APPEND_CURVED_SEGMENT_FINAL,
    MOVE_TO_NEXT_LINE_SHOW_TEXT,
    SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT,
  };

  /**
   * <p>Parses a PDF document from an input stream, applies a
   * transform to it, and writes the result to an output stream.</p>
   * <p>This method closes the PDF document at the end of processing</p>
   * @param pdfTransform    A PDF transform.
   * @param pdfInputStream  An input stream containing a PDF document.
   * @param pdfOutputStream An output stream into which to write the
   *                        transformed PDF document.
   * @throws IOException if any processing error occurs.
   */
  public static void applyPdfTransform(DocumentTransform pdfTransform,
                                       InputStream pdfInputStream,
                                       OutputStream pdfOutputStream)
      throws IOException {
    boolean mustReleaseResources = false;
    PdfDocument pdfDocument = null;
    try {
      // Parse
      pdfDocument = new PdfDocument(pdfInputStream);
      mustReleaseResources = true;

      // Transform
      pdfTransform.transform(pdfDocument);

      // Save
      pdfDocument.save(pdfOutputStream);
    }
    finally {
      if (mustReleaseResources) {
        pdfDocument.close();
      }
    }
  }

  public static Iterator getPdf16Operators() {
    return UnmodifiableIterator.decorate(new ObjectArrayIterator(PDF_1_6_OPERATORS));
  }

  /**
   * <p>Extracts the float data associated with a PDF token that is
   * a PDF float.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfFloat(pdfFloat)</code></li>
   * </ul>
   * @param pdfFloat A PDF float.
   * @return The float associated with this PDF float.
   * @see #isPdfFloat
   */
  public static float getPdfFloat(Object pdfFloat) {
    return ((COSFloat)pdfFloat).floatValue();
  }

  public static int getPdfInteger(Object candidateToken) {
    return ((COSInteger)candidateToken).intValue();
  }

  public static Iterator getPdfOperators() {
    return getPdf16Operators();
  }

  /**
   * <p>Extracts the string data associated with a PDF token that is
   * a PDF string.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfString(pdfString)</code></li>
   * </ul>
   * @param pdfString A PDF string.
   * @return The {@link String} associated with this PDF string.
   * @see #isPdfString
   */
  public static String getPdfString(Object pdfString) {
    return ((COSString)pdfString).getString();
  }

  /**
   * <p>Determines if a candidate PDF token is the "begin text object"
   * PDF operator.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #BEGIN_TEXT_OBJECT
   * @see #isPdfOperator
   */
  public static boolean isBeginTextObject(Object candidateToken) {
    return isPdfOperator(candidateToken, BEGIN_TEXT_OBJECT);
  }

  /**
   * <p>Determines if a candidate PDF token is the "end text object"
   * PDF operator.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #END_TEXT_OBJECT
   * @see #isPdfOperator
   */
  public static boolean isEndTextObject(Object candidateToken) {
    return isPdfOperator(candidateToken, END_TEXT_OBJECT);
  }

  /**
   * <p>Determines if a candidate PDF token is a PDF float token.</p>
   * @param candidateToken A candidate PDF toekn.
   * @return True if the argument is a PDF float, false otherwise.
   * @see COSFloat
   */
  public static boolean isPdfFloat(Object candidateToken) {
    return candidateToken instanceof COSFloat;
  }

  public static boolean isPdfInteger(Object candidateToken) {
    return candidateToken instanceof COSInteger;
  }

  /**
   * <p>Determines if a PDF token is a PDF operator, if is so,
   * if it is the expected operator.</p>
   * @param candidateToken   A candidate PDF token.
   * @param expectedOperator A PDF operator name (as a string).
   * @return True if the argument is a PDF operator of the expected
   *         type, false otherwise.
   */
  public static boolean isPdfOperator(Object candidateToken,
                                      String expectedOperator) {
    if (candidateToken != null && candidateToken instanceof PDFOperator) {
      return ((PDFOperator)candidateToken).getOperation().equals(expectedOperator);
    }
    return false;
  }

  /**
   * <p>Determines if a candidate PDF token is a PDF string token.</p>
   * @param candidateToken A candidate PDF toekn.
   * @return True if the argument is a PDF string, false otherwise.
   * @see COSString
   */
  public static boolean isPdfString(Object candidateToken) {
    return candidateToken instanceof COSString;
  }

  /**
   * <p>Determines if a candidate PDF token is the "show text"
   * PDF operator.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #SHOW_TEXT
   * @see #isPdfOperator
   */
  public static boolean isShowText(Object candidateToken) {
    return isPdfOperator(candidateToken, SHOW_TEXT);
  }

}
