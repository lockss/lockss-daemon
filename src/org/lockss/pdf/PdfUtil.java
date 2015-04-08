/*
 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.lang3.StringEscapeUtils;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.util.*;

/**
 * <p>
 * PDF-related utilities.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public class PdfUtil {

  /**
   * <p>
   * The configuration prefix for this class ({@value}).
   * </p>
   * 
   * @since 1.56
   */
  public static final String CONFIG_PREFIX = Configuration.PREFIX + "pdf.";

  /**
   * Default: 5MB
   */
  public static final int DEFAULT_PDF_MEMORY_LIMIT = 5 * 1024 * 1024;

  /**
   * <p>
   * The constant string for the PDF name {@value}.
   * </p>
   * 
   * @since 1.56
   */
  public static final String NAME_ID = "ID";

  /**
   * <p>
   * The constant string for the PDF name {@value}.
   * </p>
   * 
   * @since 1.56
   */
  public static final String NAME_LINK = "Link";

  /**
   * <p>
   * The constant string for the PDF name {@value}.
   * </p>
   * 
   * @since 1.56
   */
  public static final String NAME_SUBTYPE = "Subtype";

  /**
   * Number of megabytes above which a filtered PDF file is transferred from
   * memory to a temporary file. May also be used by other PDF operations that
   * require in-memory processing.
   */
  public static final String PARAM_PDF_MEMORY_LIMIT =
      CONFIG_PREFIX + "pdfMemoryLimit";

  /**
   * <p>
   * A suggested prefix for title database attributes conveying hints about PDF
   * filter factories.
   * </p>
   * 
   * @see DefinableArchivalUnit#SUFFIX_FILTER_FACTORY
   */
  public static final String PREFIX_PDF_FILTER_FACTORY_HINT = "hint_";

  /**
   * <p>
   * A logger for use by this class.
   * </p>
   * 
   * @since 1.56
   */
  private static final Logger log = Logger.getLogger(PdfUtil.class);

  /**
   * <p>
   * Convenience call to {@link #asInputStream(PdfDocument, int)} using a
   * default memory limit defined by the parameter
   * {@link #PARAM_PDF_MEMORY_LIMIT}.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @return The saved PDF document, as an input stream.
   * @throws PdfException
   *           If processing fails at the PDF level.
   * @throws IOException
   *           If processing fails at the I/O level.
   * @see #asInputStream(PdfDocument, int)
   */
  public static InputStream asInputStream(PdfDocument pdfDocument)
      throws PdfException, IOException {
    return asInputStream(pdfDocument, getPdfMemoryLimit());
  }

  /**
   * <p>
   * Saves the given PDF documeSnt and returns the result as an input stream,
   * staying if possible in memory up to the given number of megabytes.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @param memoryLimitMb
   *          The number of megabytes of memory up to which processing can be
   *          done entirely in memory.
   * @return The saved PDF document, as an input stream.
   * @throws PdfException
   *           If processing fails at the PDF level.
   * @throws IOException
   *           If processing fails at the I/O level.
   * @see DeferredTempFileOutputStream
   */
  public static InputStream asInputStream(PdfDocument pdfDocument,
                                          int memoryLimitMb)
      throws PdfException, IOException {
    DeferredTempFileOutputStream os = new DeferredTempFileOutputStream(memoryLimitMb);
    try {
      pdfDocument.save(os);
      os.close();
      return os.getDeleteOnCloseInputStream();
    }
    catch (PdfException | IOException | RuntimeException e) {
      os.deleteTempFile();
      throw e;
    }
  }

  /**
   * <p>
   * Retrieves from the title database the value of a special attribute the
   * given AU may have, that is used by convention to direct a PDF filter
   * factory to use a particular PDF transformation for that AU. The special
   * attribute is the concatenation of {@link #PREFIX_PDF_FILTER_FACTORY_HINT},
   * {@link Constants#MIME_TYPE_PDF} and
   * {@link DefinableArchivalUnit#SUFFIX_ARTICLE_MIME_TYPE}.
   * </p>
   * 
   * @param au
   *          An archival unit.
   * @return The value of the PDF hint attribute, or <code>null</code> if unset.
   */
  public static String getPdfHint(ArchivalUnit au) {
    String key = PREFIX_PDF_FILTER_FACTORY_HINT + Constants.MIME_TYPE_PDF
        + DefinableArchivalUnit.SUFFIX_HASH_FILTER_FACTORY;
    return AuUtil.getTitleAttribute(au, key);
  }

  /**
   * <p>
   * Convenience method to retrieve the value of the daemon parameter
   * {@link #PARAM_PDF_MEMORY_LIMIT}/{@link #DEFAULT_PDF_MEMORY_LIMIT}.
   * </p>
   * 
   * @return The value of {@link #PARAM_PDF_MEMORY_LIMIT}/
   *         {@link #DEFAULT_PDF_MEMORY_LIMIT} in the current configuration.
   * @since 1.56
   */
  public static int getPdfMemoryLimit() {
    return CurrentConfig.getCurrentConfig().getInt(PARAM_PDF_MEMORY_LIMIT,
                                                   DEFAULT_PDF_MEMORY_LIMIT);
  }
  
  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#BEGIN_IMAGE_DATA}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#BEGIN_IMAGE_DATA}
   * @since 1.68
   */
  public static boolean isBeginImageData(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.BEGIN_IMAGE_DATA);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#BEGIN_IMAGE_OBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#BEGIN_IMAGE_OBJECT}
   * @since 1.68
   */
  public static boolean isBeginImageObject(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.BEGIN_IMAGE_OBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#BEGIN_TEXT_OBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#BEGIN_TEXT_OBJECT}
   * @since 1.68
   */
  public static boolean isBeginTextObject(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.BEGIN_TEXT_OBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#END_TEXT_OBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#END_TEXT_OBJECT}
   * @since 1.68
   */
  public static boolean isEndTextObject(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.END_TEXT_OBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#INVOKE_XOBJECT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#INVOKE_XOBJECT}
   * @since 1.68
   */
  public static boolean isInvokeXObject(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.INVOKE_XOBJECT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#NEXT_LINE_SHOW_TEXT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#NEXT_LINE_SHOW_TEXT}
   * @since 1.68
   */
  public static boolean isNextLineShowText(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.NEXT_LINE_SHOW_TEXT);
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
   * @since 1.68
   * @see PdfOpcodes
   */
  public static boolean isOpcode(PdfToken operator, String opcode) {
    return opcode.equals(operator.getOperator());
  }
  
  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#RESTORE_GRAPHICS_STATE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#RESTORE_GRAPHICS_STATE}
   * @since 1.68
   */
  public static boolean isRestoreGraphicsState(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.RESTORE_GRAPHICS_STATE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SAVE_GRAPHICS_STATE}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SAVE_GRAPHICS_STATE}
   * @since 1.68
   */
  public static boolean isSaveGraphicsState(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.SAVE_GRAPHICS_STATE);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SET_SPACING_NEXT_LINE_SHOW_TEXT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SET_SPACING_NEXT_LINE_SHOW_TEXT}
   * @since 1.68
   */
  public static boolean isSetSpacingNextLineShowText(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.SET_SPACING_NEXT_LINE_SHOW_TEXT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SET_TEXT_FONT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SET_TEXT_FONT}
   * @since 1.68
   */
  public static boolean isSetTextFont(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.SET_TEXT_FONT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SET_TEXT_MATRIX}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SET_TEXT_MATRIX}
   * @since 1.68
   */
  public static boolean isSetTextMatrix(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.SET_TEXT_MATRIX);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT}
   * @since 1.68
   */
  public static boolean isShowText(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.SHOW_TEXT);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its operand contains the
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
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand contains the given
   *         substring.
   * @since 1.68
   */
  public static boolean isShowTextContains(PdfToken operator,
                                           PdfToken operand,
                                           String substr) {
    return isShowText(operator) && operand.getString().contains(substr);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its operand ends with the
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
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand ends with the given
   *         suffix.
   * @since 1.68
   */
  public static boolean isShowTextEndsWith(PdfToken operator,
                                           PdfToken operand,
                                           String suffix) {
    return isShowText(operator) && operand.getString().endsWith(suffix);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its operand is equal to the
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
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand is equal to the given
   *         string.
   * @since 1.68
   */
  public static boolean isShowTextEquals(PdfToken operator,
                                         PdfToken operand,
                                         String str) {
    return isShowText(operator) && operand.getString().equals(str);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its operand is equal to the
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
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand is equal to the given
   *         string (case-independently).
   * @since 1.68
   */
  public static boolean isShowTextEqualsIgnoreCase(PdfToken operator,
                                                   PdfToken operand,
                                                   String str) {
    return isShowText(operator) && operand.getString().equalsIgnoreCase(str);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its operand matches the
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
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand matches the given
   *         pattern (using {@link Matcher#find()})
   * @since 1.68
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
   * is the opcode {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING}.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT_GLYPH_POSITIONING}
   * @since 1.68
   */
  public static boolean isShowTextGlyphPositioning(PdfToken operator) {
    return isOpcode(operator, PdfOpcodes.SHOW_TEXT_GLYPH_POSITIONING);
  }

  /**
   * <p>
   * Determines if a PDF token for which {@link PdfToken#isOperator()} is true
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its equivalent string
   * operand contains the given substring.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param substr
   *          A substring.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand contains the given
   *         substring
   * @since 1.68
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
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its equivalent string
   * operand ends with the given suffix.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param suffix
   *          A suffix.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand ends with the given
   *         suffix
   * @since 1.68
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
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its equivalent string
   * operand is equal to the given string.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param str
   *          A string.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand is equal to the given
   *         string
   * @since 1.68
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
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its equivalent string
   * operand is equal to the given string (case-independently).
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param str
   *          A string.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand is equal to the given
   *         string (case-independently)
   * @since 1.68
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
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its equivalent string
   * operand matches the given pattern (using {@link Matcher#find()}, which does
   * not implicitly anchor).
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param pattern
   *          A pattern.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand matches the given
   *         pattern (using {@link Matcher#find()})
   * @since 1.68
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
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its equivalent string
   * operand starts with the given prefix.
   * </p>
   * 
   * @param operator
   *          A PDF token that is an operator.
   * @param operand
   *          A PDF token that is treated as an array.
   * @param prefix
   *          A prefix.
   * @return <code>true</code> if the given operator is the opcode
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand is starts with the
   *         given prefix
   * @since 1.68
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
   * is the opcode {@link PdfOpcodes#SHOW_TEXT}, and its operand starts with the
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
   *         {@link PdfOpcodes#SHOW_TEXT} and its operand starts with the given
   *         string.
   * @since 1.68
   */
  public static boolean isShowTextStartsWith(PdfToken operator,
                                             PdfToken operand,
                                             String prefix) {
    return isShowText(operator) && operand.getString().startsWith(prefix);
  }

  /**
   * <p>
   * Normalizes all token stream of each page of the given PDF document.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #normalizeTokenStream(PdfTokenStream)
   */
  public static void normalizeAllTokenStreams(PdfDocument pdfDocument)
      throws PdfException {
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      normalizeAllTokenStreams(pdfPage);
    }
  }

  /**
   * <p>
   * Normalizes all the token streams of the given PDF page.
   * </p>
   * 
   * @param pdfPage
   *          A PDF page.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #normalizeTokenStream(PdfTokenStream)
   */
  public static void normalizeAllTokenStreams(PdfPage pdfPage)
      throws PdfException {
    for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
      normalizeTokenStream(pdfTokenStream);
    }
  }

  /**
   * <p>
   * Normalizes the page token stream of the given PDF page.
   * </p>
   * 
   * @param pdfPage
   *          A PDF page.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #normalizeTokenStream(PdfTokenStream)
   */
  public static void normalizePageTokenStream(PdfPage pdfPage)
      throws PdfException {
    normalizeTokenStream(pdfPage.getPageTokenStream());
  }

  /**
   * <p>
   * Normalizes the page stream of each page of the given PDF document.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #normalizeTokenStream(PdfTokenStream)
   */
  public static void normalizePageTokenStreams(PdfDocument pdfDocument)
      throws PdfException {
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      normalizePageTokenStream(pdfPage);
    }
  }

  /**
   * <p>
   * Reads all the tokens from the given stream, then writes the result back to
   * it.
   * </p>
   * <p>
   * The purpose of this seemingly idempotent operation is to force the
   * underlying stream implementation to unravel any parts of the stream it may
   * have been able to delay interpreting until the stream is accessed, such as
   * decoding a filtered stream.
   * </p>
   * 
   * @param pdfTokenStream
   *          A token stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  public static void normalizeTokenStream(PdfTokenStream pdfTokenStream)
      throws PdfException {
    pdfTokenStream.setTokens(pdfTokenStream.getTokens());
  }

  /**
   * <p>
   * Sets the ID array of the given PDF document to one consisting of the
   * arbitrary ID string <code>"12345678901234567890123456789012"</code> twice.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see #setTrailerId(PdfDocument, String, String)
   */
  public static void normalizeTrailerId(PdfDocument pdfDocument)
      throws PdfException {
    setTrailerId(pdfDocument,
                 "12345678901234567890123456789012",
                 "12345678901234567890123456789012");
  }

  /**
   * <p>
   * Convenience method to convert the given PDF token to a human-readable
   * String.
   * </p>
   * 
   * @param pdfToken
   *          A PDF token.
   * @return A string representing the token.
   */
  public static String prettyPrint(PdfToken pdfToken) {
    StringBuilder sb = new StringBuilder();
    prettyPrint(sb, pdfToken);
    return sb.toString();
  }

  /**
   * <p>
   * Convenience method to output a human-readable version of the given token to
   * the given string builder.
   * </p>
   * 
   * @param sb
   *          A string builder.
   * @param pdfToken
   *          A PDF token.
   * @since 1.57
   */
  public static void prettyPrint(StringBuilder sb, PdfToken pdfToken) {
    sb.append("[");
    if (pdfToken.isArray()) {
      sb.append("array:");
      for (PdfToken arrayToken : pdfToken.getArray()) {
        prettyPrint(arrayToken);
      }
    } else if (pdfToken.isBoolean()) {
      sb.append("boolean:");
      sb.append(Boolean.toString(pdfToken.getBoolean()));
    } else if (pdfToken.isDictionary()) {
      boolean first = true;
      sb.append("dictionary:");
      for (Map.Entry<String, PdfToken> entry : pdfToken.getDictionary()
          .entrySet()) {
        if (first) {
          first = false;
        } else {
          sb.append(";");
        }
        sb.append(StringEscapeUtils.escapeJava(entry.getKey()));
        sb.append("=");
        prettyPrint(entry.getValue());
      }
    } else if (pdfToken.isFloat()) {
      sb.append("float:");
      sb.append(Float.toString(pdfToken.getFloat()));
    } else if (pdfToken.isInteger()) {
      sb.append("integer:");
      sb.append(Long.toString(pdfToken.getInteger()));
    } else if (pdfToken.isName()) {
      sb.append("name:");
      sb.append(StringEscapeUtils.escapeJava(pdfToken.getName()));
    } else if (pdfToken.isNull()) {
      sb.append("null");
    } else if (pdfToken.isObject()) {
      sb.append("object:");
      prettyPrint(sb, pdfToken.getObject());
    } else if (pdfToken.isOperator()) {
      sb.append("operator:");
      sb.append(StringEscapeUtils.escapeJava(pdfToken.getOperator()));
    } else if (pdfToken.isString()) {
      sb.append("string:\"");
      sb.append(StringEscapeUtils.escapeJava(pdfToken.getString()));
      sb.append("\"");
    }
    sb.append("]");
  }

  /**
   * <p>
   * If the given PDF document is not <code>null</code>, closes it ignoring any
   * exception thrown by {@link PdfDocument#close()}.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @since 1.56
   * @see PdfDocument#close()
   */
  public static void safeClose(PdfDocument pdfDocument) {
    try {
      if (pdfDocument != null) {
        pdfDocument.close();
      }
    } catch (PdfException pdfe) {
      log.debug2("Error closing a PDF document", pdfe);
    }
  }

  /**
   * <p>
   * Sets the trailer ID array of the given PDF document to one consisting of
   * the two given strings.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @param id0
   *          The first string in the trailer ID array.
   * @param id1
   *          The second string in the trailer ID array.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  public static void setTrailerId(PdfDocument pdfDocument,
                                  String id0,
                                  String id1)
        throws PdfException {
    PdfTokenFactory pdfTokenFactory = pdfDocument.getTokenFactory();
    Map<String, PdfToken> trailerMapping = pdfDocument.getTrailer();
    trailerMapping.remove(NAME_ID);
    List<PdfToken> idArray = new ArrayList<PdfToken>(2);
    idArray.add(pdfTokenFactory.makeString(id0));
    idArray.add(pdfTokenFactory.makeString(id1));
    trailerMapping.put(NAME_ID, pdfTokenFactory.makeArray(idArray));
    pdfDocument.setTrailer(trailerMapping);
  }

  /**
   * <p>
   * This class cannot be instantiated.
   * </p>
   * 
   * @since 1.56
   */
  private PdfUtil() {
    // Prevent instantiation
  }

}
