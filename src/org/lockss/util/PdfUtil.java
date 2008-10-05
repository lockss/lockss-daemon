/*
 * $Id: PdfUtil.java,v 1.28 2007-08-27 06:50:55 tlipkis Exp $
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

package org.lockss.util;

import java.io.*;
import java.util.*;

import org.apache.commons.collections.iterators.*;
import org.apache.commons.io.output.*;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.lockss.config.*;
import org.lockss.filter.pdf.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.pdfbox.cos.*;
import org.pdfbox.util.PDFOperator;

/**
 * <p>Utilities for PDF processing and filtering.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfUtil {

  /** Filtered PDF files smaller than this will be kept in memory, larger
   * than this will be written to a temp file */
  static final String PARAM_TEMP_STREAM_THRESHOLD =
    Configuration.PREFIX + "pdfutil.tempStreamThreshold";
  static final int DEFAULT_TEMP_STREAM_THRESHOLD = 1024 * 1024;

  /**
   * <p>An interface for looping policies.</p>
   * <p>This interface is intended for the following types of loops:</p>
<pre>
boolean success = resultPolicy.initialValue();
while (...) {
  boolean oneStep = doSomething(...);
  success = resultPolicy.updateResult(success, oneStep);
  if (!resultPolicy.shouldKeepGoing(success)) {
    break;
  }
}
return success;
</pre>
   * <p>For instance, the above loop could have short-circuiting "or"
   * semantics: it returns true as soon as any of the steps returns
   * true, and if none return true it returns false. This would be
   * achieved with {@link ResultPolicy#initialValue} returning false,
   * {@link ResultPolicy#updateResult}<code>(success, oneStep)</code>
   * returning <code>success || oneStep</code>, and
   * {@link ResultPolicy#shouldKeepGoing}<code>(success)</code>
   * returning <code>!success</code>.</p>
   * <p>To give it non short-circuiting semantics, just make
   * {@link ResultPolicy#shouldKeepGoing} return true constantly.</p>
   * <p>Likewise, the loop can have "and" semantics with or without
   * short-circuiting, for appropriate values of the three
   * methods.</p>
   * <p>Examples of how to use these result policies can be found
   * for instance in {@link AggregateDocumentTransform#transform},
   * {@link AggregatePageTransform#transform} or
   * {@link TransformSelectedPages#transform}.</p>
   * @author Thib Guicherd-Callin
   * @see PdfUtil#AND
   * @see PdfUtil#AND_ALL
   * @see PdfUtil#OR
   * @see PdfUtil#OR_ALL
   */
  public interface ResultPolicy {

    /**
     * <p>Provides the initial value for the success flag.</p>
     * @return The value of the success flag before the loop.
     */
    boolean initialValue();

    /**
     * <p>Determines whether the loop should continue based on the
     * current value of the success flag (passed as argument).</p>
     * @param currentResult The current value of the success flag.
     * @return Whether the loop should continue based on the current
     *         value of the success flag.
     */
    boolean shouldKeepGoing(boolean currentResult);

    /**
     * <p>Computes the new value of the success flag, given the
     * current value of the success flag and a new result from an
     * iteration of the loop.</p>
     * @param currentResult The current value of the success flag.
     * @param update        A new result from an iteration of the loop.
     * @return The new value of the success flag.
     */
    boolean updateResult(boolean currentResult, boolean update);

  }

  /**
   * <p>A version of {@link ResultPolicy} that implements
   * short-circuiting "and" semantics.</p>
   * @see PdfUtil#AND_ALL
   */
  public static final ResultPolicy AND = new ResultPolicy() {

    /* Inherit documentation */
    public boolean initialValue() {
      return true;
    }

    /* Inherit documentation */
    public boolean shouldKeepGoing(boolean currentResult) {
      return currentResult;
    }

    public String toString() {
      return "AND";
    }

    /* Inherit documentation */
    public boolean updateResult(boolean currentResult, boolean update) {
      return currentResult && update;
    }

  };

  /**
   * <p>A version of {@link ResultPolicy} that implements
   * non short-circuiting "and" semantics.</p>
   * @see PdfUtil#AND
   */
  public static final ResultPolicy AND_ALL = new ResultPolicy() {

    /* Inherit documentation */
    public boolean initialValue() {
      return true;
    }

    /* Inherit documentation */
    public boolean shouldKeepGoing(boolean currentResult) {
      return true;
    }

    public String toString() {
      return "AND_ALL";
    }

    /* Inherit documentation */
    public boolean updateResult(boolean currentResult, boolean update) {
      return currentResult && update;
    }

  };

  /**
   * <p>A version of {@link ResultPolicy} that implements
   * short-circuiting "or" semantics.</p>
   * @see PdfUtil#OR_ALL
   */
  public static final ResultPolicy OR = new ResultPolicy() {

    /* Inherit documentation */
    public boolean initialValue() {
      return false;
    }

    /* Inherit documentation */
    public boolean shouldKeepGoing(boolean currentResult) {
      return !currentResult;
    }

    public String toString() {
      return "OR";
    }

    /* Inherit documentation */
    public boolean updateResult(boolean currentResult, boolean update) {
      return currentResult || update;
    }

  };

  /**
   * <p>A version of {@link ResultPolicy} that implements
   * non short-circuiting "or" semantics.</p>
   * @see PdfUtil#OR
   */
  public static final ResultPolicy OR_ALL = new ResultPolicy() {

    /* Inherit documentation */
    public boolean initialValue() {
      return false;
    }

    /* Inherit documentation */
    public boolean shouldKeepGoing(boolean currentResult) {
      return true;
    }

    public String toString() {
      return "OR_ALL";
    }

    /* Inherit documentation */
    public boolean updateResult(boolean currentResult, boolean update) {
      return currentResult || update;
    }

  };

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String APPEND_CURVED_SEGMENT = "c";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String APPEND_CURVED_SEGMENT_FINAL = "y";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String APPEND_CURVED_SEGMENT_INITIAL = "v";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String APPEND_RECTANGLE = "re";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String APPEND_STRAIGHT_LINE_SEGMENT = "l";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String BEGIN_COMPATIBILITY_SECTION = "BX";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String BEGIN_INLINE_IMAGE_DATA = "ID";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String BEGIN_INLINE_IMAGE_OBJECT = "BI";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String BEGIN_MARKED_CONTENT = "BMC";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String BEGIN_MARKED_CONTENT_PROP = "BDC";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String BEGIN_NEW_SUBPATH = "m";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String BEGIN_TEXT_OBJECT = "BT";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String CLOSE_FILL_STROKE_EVENODD = "b*";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String CLOSE_FILL_STROKE_NONZERO = "b";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String CLOSE_STROKE = "s";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String CLOSE_SUBPATH = "h";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String CONCATENATE_MATRIX = "cm";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String DEFINE_MARKED_CONTENT_POINT = "MP";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String DEFINE_MARKED_CONTENT_POINT_PROP = "DP";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String END_COMPATIBILITY_SECTION = "EX";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String END_INLINE_IMAGE_OBJECT = "EI";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String END_MARKED_CONTENT = "EMC";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String END_PATH = "n";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String END_TEXT_OBJECT = "ET";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String FILL_EVENODD = "f*";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String FILL_NONZERO = "f";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String FILL_NONZERO_OBSOLETE = "F";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String FILL_STROKE_EVENODD = "B*";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String FILL_STROKE_NONZERO = "B";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String INVOKE_NAMED_XOBJECT = "Do";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String MOVE_TEXT_POSITION = "Td";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String MOVE_TEXT_POSITION_SET_LEADING = "TD";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String MOVE_TO_NEXT_LINE = "T*";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String MOVE_TO_NEXT_LINE_SHOW_TEXT = "\'";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String PAINT_SHADING_PATTERN = "sh";

  /**
   * <p>A suggested prefix for non-definitional parameters conveying
   * hints about PDF filter factories.</p>
   * @see DefinableArchivalUnit#SUFFIX_FILTER_FACTORY
   */
  public static final String PREFIX_PDF_FILTER_FACTORY_HINT = "hint_";

  /**
   * <p>The PDF MIME type, <code>{@value}</code>.</p>
   * @see <a href="http://www.rfc-editor.org/rfc/rfc3778.txt">RFC3778</a>
   */
  public static final String PDF_MIME_TYPE = "application/pdf";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String RESTORE_GRAPHICS_STATE = "Q";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SAVE_GRAPHICS_STATE = "q";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_CHARACTER_SPACING = "Tc";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_CLIPPING_PATH_EVENODD = "W*";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_CLIPPING_PATH_NONZERO = "W";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_CMYK_COLOR_NONSTROKING = "k";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_CMYK_COLOR_STROKING = "K";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_COLOR_NONSTROKING = "sc";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_COLOR_NONSTROKING_SPECIAL = "scn";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_COLOR_RENDERING_INTENT = "ri";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_COLOR_SPACE_NONSTROKING = "cs";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_COLOR_SPACE_STROKING = "CS";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_COLOR_STROKING = "SC";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_COLOR_STROKING_SPECIAL = "SCN";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_FLATNESS_TOLERANCE = "i";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_FROM_GRAPHICS_STATE = "gs";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_GLYPH_WIDTH = "d0";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_GLYPH_WIDTH_BOUNDING_BOX = "d1";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_GRAY_LEVEL_NONSTROKING = "g";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_GRAY_LEVEL_STROKING = "G";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_HORIZONTAL_TEXT_SCALING = "Tz";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_LINE_CAP_STYLE = "J";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_LINE_DASH_PATTERN = "d";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_LINE_JOIN_STYLE = "j";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_LINE_WIDTH = "w";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_MITER_LIMIT = "M";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_RGB_COLOR_NONSTROKING = "rg";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_RGB_COLOR_STROKING = "RG";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT = "\"";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_TEXT_FONT_AND_SIZE = "Tf";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_TEXT_LEADING = "TL";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_TEXT_MATRIX = "Tm";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_TEXT_RENDERING_MODE = "Tr";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_TEXT_RISE = "Ts";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SET_WORD_SPACING = "Tw";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SHOW_TEXT = "Tj";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String SHOW_TEXT_GLYPH_POSITIONING = "TJ";

  /**
   * <p>The PDF <code>{@value}</code> operator string.</p>
   */
  public static final String STROKE = "S";

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
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("PdfUtil");

  /**
   * <p>Applies the given transform to the given PDF document, and
   * saves the result to the given output stream.</p>
   * @param documentTransform A PDF transform.
   * @param pdfDocument       A PDF document.
   * @param outputStream      An output stream into which to write the
   *                          transformed PDF document.
   */
  public static boolean applyAndSave(DocumentTransform documentTransform,
                                     PdfDocument pdfDocument,
                                     OutputStream outputStream) {
    try {
      boolean ret = documentTransform.transform(pdfDocument);
      logger.debug2("Document transform result: " + ret);
      pdfDocument.save(outputStream);
      return ret;
    }
    catch (OutOfMemoryError oome) {
      logger.error("Out of memory in the PDF framework", oome);
      throw oome; // rethrow
    }
    catch (IOException ioe) {
      logger.error("Document transform failed", ioe);
      return false;
    }
  }

  public static InputStream applyFromInputStream(OutputDocumentTransform documentTransform,
                                                 InputStream inputStream) {
    PdfDocument pdfDocument = null;
    DeferredTempFileOutputStream outputStream = null;
    Configuration config = CurrentConfig.getCurrentConfig();
    int tempStreamThreshold = config.getInt(PARAM_TEMP_STREAM_THRESHOLD,
					    DEFAULT_TEMP_STREAM_THRESHOLD);
    try {
      // Parse the PDF file
      pdfDocument = new PdfDocument(inputStream);

      // Create a thresholding output stream
      outputStream = new DeferredTempFileOutputStream(tempStreamThreshold);
      // Apply the output document transform into the output stream
      if (documentTransform.transform(pdfDocument, outputStream)) {
	outputStream.close();
	logger.debug2("Transform from input stream succeeded");
      }
      else {
	deleteTempFile(outputStream);
	logger.debug2("Transform from input stream did not succeed; using PDF document as is");
	outputStream = new DeferredTempFileOutputStream(tempStreamThreshold);
	pdfDocument.save(outputStream);
	outputStream.close();
      }

      // Return the transformed PDF file as an input stream
      if (outputStream.isInMemory()) {
	return new ByteArrayInputStream(outputStream.getData());
      }
      else {
	// If temp file was created, arrange for it to be deleted when
	// the input stream is closed.
	File tempFile = outputStream.getFile();
	InputStream fileStream =
	  new BufferedInputStream(new FileInputStream(tempFile));
	CloseCallbackInputStream.Callback cb =
	  new CloseCallbackInputStream.Callback() {
	    public void streamClosed(Object file) {
	      ((File)file).delete();
	    }};
	return new CloseCallbackInputStream(fileStream, cb, tempFile);
      }
    }
    catch (OutOfMemoryError oome) {
      logger.error("Out of memory in the PDF framework", oome);
      throw oome; // rethrow
    }
    catch (IOException ioe) {
      logger.error("Transform from input stream failed", ioe);
      if (outputStream != null) {
	deleteTempFile(outputStream);
      }
      return null;
    }
    finally {
      PdfDocument.close(pdfDocument);
    }
  }

  private static void deleteTempFile(DeferredTempFileOutputStream dtfos) {
    try {
      dtfos.deleteTempFile();
    } catch (Exception e) {
      logger.error("Couldn't delete failed PDF temp file", e);
    }
  }

  public static OutputDocumentTransform getOutputDocumentTransform(ArchivalUnit au) {
    String key = PREFIX_PDF_FILTER_FACTORY_HINT + PDF_MIME_TYPE + DefinableArchivalUnit.SUFFIX_FILTER_FACTORY;
    String className = AuUtil.getTitleAttribute(au, key);
    if (className == null) {
      logger.debug2("No PDF filter factory hint");
      return null;
    }
    try {
      OutputDocumentTransform ret =
	(OutputDocumentTransform)au.getPlugin().newAuxClass(className, OutputDocumentTransform.class);
      logger.debug2("Successfully loaded and instantiated " + ret.getClass().getName());
      return ret;
    } catch (org.lockss.daemon.PluginException.InvalidDefinition e) {
      logger.error("Can't load PDF transform", e);
      return null;
    } catch (RuntimeException e) {
      logger.error("Can't load PDF transform", e);
      return null;
    }
  }

  public static Iterator getPdf16Operators() {
    return UnmodifiableIterator.decorate(new ObjectArrayIterator(PDF_1_6_OPERATORS));
  }

  /**
   * <p>Extracts the float data associated with the PDF token at the
   * given index that is known to be a PDF float.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfFloat(tokens, index)</code></li>
   * </ul>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @return The float associated with the selected PDF float.
   * @see #isPdfFloat(List, int)
   * @see #getPdfFloat(Object)
   */
  public static float getPdfFloat(List tokens,
                                  int index) {
    return getPdfFloat(tokens.get(index));
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
   * @see COSFloat#floatValue
   * @see #isPdfFloat(Object)
   */
  public static float getPdfFloat(Object pdfFloat) {
    return ((COSFloat)pdfFloat).floatValue();
  }

  /**
   * <p>Extracts the integer data associated with the PDF token at the
   * given index that is known to be a PDF integer.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfInteger(tokens, index)</code></li>
   * </ul>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @return The integer associated with the selected PDF integer.
   * @see #isPdfInteger(List, int)
   * @see #getPdfInteger(Object)
   */
  public static int getPdfInteger(List tokens,
                                  int index) {
    return getPdfInteger(tokens.get(index));
  }

  /**
   * <p>Extracts the integer data associated with a PDF token that is
   * a PDF integer.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfInteger(pdfInteger)</code></li>
   * </ul>
   * @param pdfInteger A PDF integer.
   * @return The integer associated with this PDF integer.
   * @see COSInteger#intValue
   * @see #isPdfInteger(Object)
   */
  public static int getPdfInteger(Object pdfInteger) {
    return ((COSInteger)pdfInteger).intValue();
  }

  /**
   * <p>Extracts the number data (expressed as a float) associated
   * with the PDF token at the given index that is known to be a PDF number.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfNumber(tokens, index)</code></li>
   * </ul>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @return The number (as a float) associated with the selected PDF number.
   * @see #getPdfNumber(Object)
   */
  public static float getPdfNumber(List tokens,
                                   int index) {
    return getPdfNumber(tokens.get(index));
  }

  /**
   * <p>Extracts the integer data associated with a PDF token that is
   * a PDF integer.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfNumber(pdfNumber)</code></li>
   * </ul>
   * @param pdfNumber A PDF number.
   * @return The number (as a float) associated with this PDF number.
   * @see COSInteger#intValue
   * @see #isPdfInteger(Object)
   */
  public static float getPdfNumber(Object pdfNumber) {
    if (isPdfFloat(pdfNumber)) {
      return getPdfFloat(pdfNumber);
    }
    else /* isPdfInteger(pdfNumber) */ {
      return (float)getPdfInteger(pdfNumber);
    }
  }

  public static Iterator getPdfOperators() {
    return getPdf16Operators();
  }

  /**
   * <p>Extracts the string data associated with the PDF token at the
   * given index that is known to be a PDF string.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfString(tokens, index)</code></li>
   * </ul>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @return The {@link String} associated with the selected PDF string.
   * @see #isPdfString(List, int)
   * @see #getPdfString(Object)
   */
  public static String getPdfString(List tokens,
                                    int index) {
    return getPdfString(tokens.get(index));
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
   * @see COSString#getString
   * @see #isPdfString(Object)
   */
  public static String getPdfString(Object pdfString) {
    return ((COSString)pdfString).getString();
  }

  /**
   * <p>Determines if the token at the given index is
   * {@link #BEGIN_TEXT_OBJECT}.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the candidate token.
   * @return True if the selected token is the expected operator, false
   *         otherwise.
   * @see #isBeginTextObject(Object)
   */
  public static boolean isBeginTextObject(List tokens,
                                          int index) {
    return 0 <= index
    && index < tokens.size()
    && isBeginTextObject(tokens.get(index));
  }

  /**
   * <p>Determines if the given token is
   * {@link #BEGIN_TEXT_OBJECT}.</p>
   * @param candidateToken A candidate PDF token.
   * @return True if the argument is the expected operator, false
   *         otherwise.
   * @see #BEGIN_TEXT_OBJECT
   * @see #matchPdfOperator(Object, String)
   */
  public static boolean isBeginTextObject(Object candidateToken) {
    return matchPdfOperator(candidateToken,
                            BEGIN_TEXT_OBJECT);
  }

  /**
   * <p>Determines if the token at the given index is
   * {@link #END_TEXT_OBJECT}.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the candidate token.
   * @return True if the selected token is the expected operator, false
   *         otherwise.
   * @see #isEndTextObject(Object)
   */
  public static boolean isEndTextObject(List tokens,
                                        int index) {
    return 0 <= index
    && index < tokens.size()
    && isEndTextObject(tokens.get(index));
  }

  /**
   * <p>Determines if the given token is
   * {@link #END_TEXT_OBJECT}.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #END_TEXT_OBJECT
   * @see #matchPdfOperator(Object, String)
   */
  public static boolean isEndTextObject(Object candidateToken) {
    return matchPdfOperator(candidateToken,
                            END_TEXT_OBJECT);
  }

  /**
   * <p>Determines if the token at the given index is
   * {@link #MOVE_TO_NEXT_LINE_SHOW_TEXT}.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the candidate token.
   * @return True if the selected token is the expected operator, false
   *         otherwise.
   * @see #isMoveToNextLineShowText(Object)
   */
  public static boolean isMoveToNextLineShowText(List tokens,
                                                 int index) {
    return 0 <= index
    && index < tokens.size()
    && isMoveToNextLineShowText(tokens.get(index));
  }

  /**
   * <p>Determines if the given token is
   * {@link #MOVE_TO_NEXT_LINE_SHOW_TEXT}.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #MOVE_TO_NEXT_LINE_SHOW_TEXT
   * @see #matchPdfOperator(Object, String)
   */
  public static boolean isMoveToNextLineShowText(Object candidateToken) {
    return matchPdfOperator(candidateToken,
                            MOVE_TO_NEXT_LINE_SHOW_TEXT);
  }

  /**
   * <p>Determines if a candidate PDF token at the given index is a PDF float token.</p>
   * @param tokens A list of tokens.
   * @param index The index of the selected token.
   * @return True if the selected token is a PDF float, false otherwise.
   * @see #isPdfFloat(Object)
   */
  public static boolean isPdfFloat(List tokens,
                                   int index) {
    return 0 <= index
    && index < tokens.size()
    && isPdfFloat(tokens.get(index));
  }

  /**
   * <p>Determines if a candidate PDF token is a PDF float token.</p>
   * @param candidateToken A candidate PDF token.
   * @return True if the argument is a PDF float, false otherwise.
   * @see COSFloat
   */
  public static boolean isPdfFloat(Object candidateToken) {
    return candidateToken instanceof COSFloat;
  }

  /**
   * <p>Determines if a candidate PDF token at the given index is a PDF integer token.</p>
   * @param tokens A list of tokens.
   * @param index The index of the selected token.
   * @return True if the selected token is a PDF integer, false otherwise.
   * @see #isPdfInteger(Object)
   */
  public static boolean isPdfInteger(List tokens,
                                     int index) {
    return 0 <= index
    && index < tokens.size()
    && isPdfInteger(tokens.get(index));
  }

  /**
   * <p>Determines if a candidate PDF token is a PDF integer.</p>
   * @param candidateToken A candidate PDF toekn.
   * @return True if the argument is a PDF integer, false otherwise.
   * @see COSInteger
   */
  public static boolean isPdfInteger(Object candidateToken) {
    return candidateToken instanceof COSInteger;
  }

  /**
   * <p>Determines if a candidate PDF token at the given index is a PDF number token.</p>
   * @param tokens A list of tokens.
   * @param index The index of the selected token.
   * @return True if the selected token is a PDF number, false otherwise.
   * @see #isPdfNumber(Object)
   */
  public static boolean isPdfNumber(List tokens,
                                    int index) {
    return 0 <= index
    && index < tokens.size()
    && isPdfNumber(tokens.get(index));
  }

  /**
   * <p>Determines if a candidate PDF token is a PDF number.</p>
   * @param candidateToken A candidate PDF toekn.
   * @return True if the argument is a PDF integer or a PDF float,
   *         false otherwise.
   * @see #isPdfFloat(Object)
   * @see #isPdfInteger(Object)
   */
  public static boolean isPdfNumber(Object candidateToken) {
    return isPdfFloat(candidateToken)
    || isPdfInteger(candidateToken);
  }

  /**
   * <p>Determines if a candidate PDF token at the given index is a PDF string token.</p>
   * @param tokens A list of tokens.
   * @param index The index of the selected token.
   * @return True if the selected token is a PDF string, false otherwise.
   * @see #isPdfString(Object)
   */
  public static boolean isPdfString(List tokens,
                                    int index) {
    return 0 <= index
    && index < tokens.size()
    && isPdfString(tokens.get(index));
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
   * <p>Determines if the token at the given index is
   * {@link #SET_RGB_COLOR_NONSTROKING}.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the candidate token.
   * @return True if the selected token is the expected operator, false
   *         otherwise.
   * @see #isSetRgbColorNonStroking(Object)
   */
  public static boolean isSetRgbColorNonStroking(List tokens,
                                                 int index) {
    return 0 <= index
    && index < tokens.size()
    && isSetRgbColorNonStroking(tokens.get(index));
  }

  /**
   * <p>Determines if the given token is
   * {@link #SET_RGB_COLOR_NONSTROKING}.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #SET_RGB_COLOR_NONSTROKING
   * @see #matchPdfOperator(Object, String)
   */
  public static boolean isSetRgbColorNonStroking(Object candidateToken) {
    return matchPdfOperator(candidateToken,
                            SET_RGB_COLOR_NONSTROKING);
  }

  /**
   * <p>Determines if the token at the given index is
   * {@link #SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT}.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the candidate token.
   * @return True if the selected token is the expected operator, false
   *         otherwise.
   * @see #isSetSpacingMoveToNextLineShowText(Object)
   */
  public static boolean isSetSpacingMoveToNextLineShowText(List tokens,
                                                           int index) {
    return 0 <= index
    && index < tokens.size()
    && isSetSpacingMoveToNextLineShowText(tokens.get(index));
  }

  /**
   * <p>Determines if the given token is
   * {@link #SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT}.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT
   * @see #matchPdfOperator(Object, String)
   */
  public static boolean isSetSpacingMoveToNextLineShowText(Object candidateToken) {
    return matchPdfOperator(candidateToken,
                            SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT);
  }

  /**
   *
   * @param tokens
   * @param index
   * @return TODO
   */
  public static boolean isSetTextMatrix(List tokens,
                                        int index) {
    return 0 <= index
    && index < tokens.size()
    && isSetTextMatrix(tokens.get(index));
  }

  /**
   *
   * @param candidateToken
   * @return TODO
   */
  public static boolean isSetTextMatrix(Object candidateToken) {
    return matchPdfOperator(candidateToken,
                            SET_TEXT_MATRIX);
  }

  /**
   * <p>Determines if the token at the given index is
   * {@link #SHOW_TEXT}.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the candidate token.
   * @return True if the selected token is the expected operator, false
   *         otherwise.
   * @see #isShowText(Object)
   */
  public static boolean isShowText(List tokens,
                                   int index) {
    return 0 <= index
    && index < tokens.size()
    && isShowText(tokens.get(index));
  }

  /**
   * <p>Determines if the given token is
   * {@link #SHOW_TEXT}.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #SHOW_TEXT
   * @see #matchPdfOperator(Object, String)
   */
  public static boolean isShowText(Object candidateToken) {
    return matchPdfOperator(candidateToken,
                            SHOW_TEXT);
  }

  /**
   * <p>Determines if the token at the given index is
   * {@link #SHOW_TEXT_GLYPH_POSITIONING}.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the candidate token.
   * @return True if the selected token is the expected operator, false
   *         otherwise.
   * @see #isShowTextGlyphPositioning(Object)
   */
  public static boolean isShowTextGlyphPositioning(List tokens,
                                                   int index) {
    return 0 <= index
    && index < tokens.size()
    && isShowTextGlyphPositioning(tokens.get(index));
  }

  /**
   * <p>Determines if the given token is
   * {@link #SHOW_TEXT_GLYPH_POSITIONING}.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #SHOW_TEXT_GLYPH_POSITIONING
   * @see #matchPdfOperator(Object, String)
   */
  public static boolean isShowTextGlyphPositioning(Object candidateToken) {
    return matchPdfOperator(candidateToken,
                            SHOW_TEXT_GLYPH_POSITIONING);
  }

  /**
   * <p>Determines if the token at the given index is a PDF float
   * with the given value.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @param num    A value to match the token against.
   * @return True if the selected token is a PDF float and its value
   *         is equal to the given value, false otherwise.
   * @see #matchPdfFloat(Object, float)
   */
  public static boolean matchPdfFloat(List tokens,
                                      int index,
                                      float num) {
    return 0 <= index
    && index < tokens.size()
    && matchPdfFloat(tokens.get(index),
                     num);
  }

  /**
   * <p>Determines if the given token is a PDF float
   * with the given value.</p>
   * @param candidateToken A candidate PDF token.
   * @param num            A value to match the token against.
   * @return True if the argument is a PDF float and its value
   *         is equal to the given value, false otherwise.
   * @see #isPdfFloat(Object)
   * @see #getPdfFloat(Object)
   */
  public static boolean matchPdfFloat(Object candidateToken,
                                      float num) {
    return isPdfFloat(candidateToken)
    && getPdfFloat(candidateToken) == num;
  }

  /**
   * <p>Determines if the token at the given index is a PDF integer
   * with the given value.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @param num    A value to match the token against.
   * @return True if the selected token is a PDF integer and its value
   *         is equal to the given value, false otherwise.
   * @see #matchPdfInteger(Object, int)
   */
  public static boolean matchPdfInteger(List tokens,
                                        int index,
                                        int num) {
    return 0 <= index
    && index < tokens.size()
    && matchPdfInteger(tokens.get(index),
                       num);
  }

  /**
   * <p>Determines if the given token is a PDF integer
   * with the given value.</p>
   * @param candidateToken A candidate PDF token.
   * @param num            A value to match the token against.
   * @return True if the argument is a PDF integer and its value
   *         is equal to the given value, false otherwise.
   * @see #isPdfInteger(Object)
   * @see #getPdfInteger(Object)
   */
  public static boolean matchPdfInteger(Object candidateToken,
                                        int num) {
    return isPdfInteger(candidateToken)
    && getPdfInteger(candidateToken) == num;
  }

  /**
   * <p>Determines if the token at the given index is a PDF number
   * with the given value (expressed as a float).</p>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @param num    A value to match the token against.
   * @return True if the selected token is a PDF number and its value
   *         is equal to the given value, false otherwise.
   * @see #matchPdfNumber(Object, float)
   */
  public static boolean matchPdfNumber(List tokens,
                                       int index,
                                       float num) {
    return 0 <= index
    && index < tokens.size()
    && matchPdfNumber(tokens.get(index),
                      num);
  }

  /**
   * <p>Determines if the given token is a PDF number
   * with the given value (expressed as a float).</p>
   * @param candidateToken A candidate PDF token.
   * @param num            A value to match the token against.
   * @return True if the argument is a PDF number and its value
   *         is equal to the given value, false otherwise.
   * @see #isPdfNumber(Object)
   * @see #getPdfNumber(Object)
   */
  public static boolean matchPdfNumber(Object candidateToken,
                                       float num) {
    return isPdfNumber(candidateToken)
    && getPdfNumber(candidateToken) == num;
  }

  /**
   * <p>Determines if the token at the given index is a PDF operator,
   * and if so, if it is the expected operator..</p>
   * @param tokens           A list of tokens.
   * @param index            The index of the selected token.
   * @param expectedOperator A PDF operator string to match the token
   *                         against.
   * @return True if the selected token is a PDF operator of the expected
   *         type, false otherwise.
   * @see #matchPdfFloat(Object, float)
   */
  public static boolean matchPdfOperator(List tokens,
                                         int index,
                                         String expectedOperator) {
    return 0 <= index
    && index < tokens.size()
    && matchPdfOperator(tokens.get(index),
                        expectedOperator);
  }

  /**
   * <p>Determines if a PDF token is a PDF operator, if is so,
   * if it is the expected operator.</p>
   * @param candidateToken   A candidate PDF token.
   * @param expectedOperator A PDF operator string to match the token against.
   * @return True if the argument is a PDF operator of the expected
   *         type, false otherwise.
   */
  public static boolean matchPdfOperator(Object candidateToken,
                                         String expectedOperator) {
    return candidateToken instanceof PDFOperator
    && ((PDFOperator)candidateToken).getOperation().equals(expectedOperator);
  }

  /**
   * <p>Determines if the token at the given index is a PDF string
   * and if it equals the given value.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @param str    A value to match the token against with {@link String#equals}.
   * @return True if the selected token is a PDF string and its value
   *         is equal to the given value, false otherwise.
   * @see #matchPdfString(Object, String)
   */
  public static boolean matchPdfString(List tokens,
                                       int index,
                                       String str) {
    return 0 <= index
    && index < tokens.size()
    && matchPdfString(tokens.get(index),
                      str);
  }

  /**
   * <p>Determines if the given token is a PDF string
   * and if it equals the given value.</p>
   * @param candidateToken A candidate PDF token.
   * @param str            A value to match the token against with {@link String#equals}.
   * @return True if the argument is a PDF string and its value
   *         is equal to the given value, false otherwise.
   * @see #isPdfString(Object)
   * @see #getPdfString(Object)
   */
  public static boolean matchPdfString(Object candidateToken,
                                       String str) {
    return isPdfString(candidateToken)
    && getPdfString(candidateToken).equals(str);
  }

  /**
   * <p>Determines if the token at the given index is a PDF string
   * and if it ends with the given value.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @param str    A value to match the token against with {@link String#endsWith(String)}.
   * @return True if the selected token is a PDF string and its value
   *         ends with the given value, false otherwise.
   * @see #matchPdfStringEndsWith(Object, String)
   */
  public static boolean matchPdfStringEndsWith(List tokens,
                                               int index,
                                               String str) {
    return 0 <= index
    && index < tokens.size()
    && matchPdfStringEndsWith(tokens.get(index),
                              str);
  }

  /**
   * <p>Determines if the given token is a PDF string
   * and if it ends with the given value.</p>
   * @param candidateToken A candidate PDF token.
   * @param str            A value to match the token against with {@link String#endsWith(String)}.
   * @return True if the argument is a PDF string and its value
   *         ends with the given value, false otherwise.
   * @see #isPdfString(Object)
   * @see #getPdfString(Object)
   */
  public static boolean matchPdfStringEndsWith(Object candidateToken,
                                               String str) {
    return isPdfString(candidateToken)
    && getPdfString(candidateToken).endsWith(str);
  }

  /**
   * <p>Determines if the token at the given index is a PDF string
   * and if it matches the given regular expression.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @param str    A regular expression to match the token against with {@link String#matches(String)}.
   * @return True if the selected token is a PDF string and its value
   *         matches the given regular expression, false otherwise.
   * @see #matchPdfStringStartsWith(Object, String)
   */
  public static boolean matchPdfStringMatches(List tokens,
                                              int index,
                                              String regex) {
    return 0 <= index
    && index < tokens.size()
    && matchPdfStringMatches(tokens.get(index),
                             regex);
  }

  /**
   * <p>Determines if the given token is a PDF string
   * and if it matches the given regular expression.</p>
   * @param candidateToken A candidate PDF token.
   * @param str            A regular expression to match the token against with {@link String#matches(String)}.
   * @return True if the argument is a PDF string and its value
   *         matches the given regular expression, false otherwise.
   * @see #isPdfString(Object)
   * @see #getPdfString(Object)
   */
  public static boolean matchPdfStringMatches(Object candidateToken,
                                              String regex) {
    return isPdfString(candidateToken)
    && getPdfString(candidateToken).matches(regex);
  }

  /**
   * <p>Determines if the token at the given index is a PDF string
   * and if it starts with the given value.</p>
   * @param tokens A list of tokens.
   * @param index  The index of the selected token.
   * @param str    A value to match the token against with {@link String#startsWith(String)}.
   * @return True if the selected token is a PDF string and its value
   *         starts with the given value, false otherwise.
   * @see #matchPdfStringStartsWith(Object, String)
   */
  public static boolean matchPdfStringStartsWith(List tokens,
                                                 int index,
                                                 String str) {
    return 0 <= index
    && index < tokens.size()
    && matchPdfStringStartsWith(tokens.get(index),
                                str);
  }

  /**
   * <p>Determines if the given token is a PDF string
   * and if it starts with the given value.</p>
   * @param candidateToken A candidate PDF token.
   * @param str            A value to match the token against with {@link String#startsWith(String)}.
   * @return True if the argument is a PDF string and its value
   *         starts with the given value, false otherwise.
   * @see #isPdfString(Object)
   * @see #getPdfString(Object)
   */
  public static boolean matchPdfStringStartsWith(Object candidateToken,
                                                 String str) {
    return isPdfString(candidateToken)
    && getPdfString(candidateToken).startsWith(str);
  }

  /**
   *
   * @param tokens
   * @param index
   * @param red
   * @param green
   * @param blue
   * @return TODO
   * @see #isSetRgbColorNonStroking(List, int)
   * @see #matchPdfNumber(List, int, float)
   */
  public static boolean matchSetRgbColorNonStroking(List tokens,
                                                    int index,
                                                    float red,
                                                    float green,
                                                    float blue) {
    return isSetRgbColorNonStroking(tokens, index)
    && matchPdfNumber(tokens, index - 3, red)
    && matchPdfNumber(tokens, index - 2, green)
    && matchPdfNumber(tokens, index - 1, blue);
  }

  /**
   *
   * @param tokens
   * @param index
   * @param red
   * @param green
   * @param blue
   * @return TODO
   * @see #matchSetRgbColorNonStroking(List, int, float, float, float)
   */
  public static boolean matchSetRgbColorNonStroking(List tokens,
                                                    int index,
                                                    int red,
                                                    int green,
                                                    int blue) {
    return matchSetRgbColorNonStroking(tokens,
                                       index,
                                       (float)red,
                                       (float)green,
                                       (float)blue);
  }

  /**
   *
   * @param tokens
   * @param index
   * @return TODO
   */
  public static boolean matchSetTextMatrix(List tokens,
                                           int index) {
    return isSetTextMatrix(tokens, index)
    && isPdfNumber(tokens, index - 6)
    && isPdfNumber(tokens, index - 5)
    && isPdfNumber(tokens, index - 4)
    && isPdfNumber(tokens, index - 3)
    && isPdfNumber(tokens, index - 2)
    && isPdfNumber(tokens, index - 1);
  }

  /**
   *
   * @param tokens
   * @param index
   * @return TODO
   * @see #isShowText(List, int)
   * @see #isPdfString(List, int)
   */
  public static boolean matchShowText(List tokens,
                                      int index) {
    return isShowText(tokens, index)
    && isPdfString(tokens, index - 1);
  }

  /**
   *
   * @param tokens
   * @param index
   * @param str
   * @return TODO
   * @see #isShowText(List, int)
   * @see #matchPdfString(List, int, String)
   */
  public static boolean matchShowText(List tokens,
                                      int index,
                                      String str) {
    return isShowText(tokens, index)
    && matchPdfString(tokens, index - 1, str);
  }

  /**
   *
   * @param tokens
   * @param index
   * @param str
   * @return TODO
   * @see #isShowText(List, int)
   * @see #matchPdfStringEndsWith(List, int, String)
   */
  public static boolean matchShowTextEndsWith(List tokens,
                                              int index,
                                              String str) {
    return isShowText(tokens, index)
    && matchPdfStringEndsWith(tokens, index - 1, str);
  }

  /**
   *
   * @param tokens
   * @param index
   * @param regex
   * @return TODO
   * @see #isShowText(List, int)
   * @see #matchPdfStringMatches(List, int, String)
   */
  public static boolean matchShowTextMatches(List tokens,
                                             int index,
                                             String regex) {
    return isShowText(tokens, index)
    && matchPdfStringMatches(tokens, index - 1, regex);
  }

  /**
  *
  * @param tokens
  * @param index
  * @param str
  * @return TODO
  * @see #isShowText(List, int)
  * @see #matchPdfStringStartsWith(List, int, String)
  */
 public static boolean matchShowTextStartsWith(List tokens,
                                               int index,
                                               String str) {
   return isShowText(tokens, index)
   && matchPdfStringStartsWith(tokens, index - 1, str);
 }

  /**
   * <p>Determines if the tokens at the given indices form a text
   * object, i.e. if they are {@link #BEGIN_TEXT_OBJECT} and
   * {@link #END_TEXT_OBJECT} respectively.</p>
   * @param tokens A list of PDF tokens.
   * @param begin  The index of the selected {@link #BEGIN_TEXT_OBJECT}
   *               candidate.
   * @param end    The index of the selected {@link #END_TEXT_OBJECT}
   *               candidate.
   * @return True if the selected tokens are the expected operators,
   *         false otherwise.
   * @see #isBeginTextObject(List, int)
   * @see #isEndTextObject(List, int)
   */
  public static boolean matchTextObject(List tokens,
                                        int begin,
                                        int end) {
    return isBeginTextObject(tokens, begin)
    && isEndTextObject(tokens, end);
  }

}
