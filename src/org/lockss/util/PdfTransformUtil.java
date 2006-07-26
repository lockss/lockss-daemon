/*
 * $Id: PdfTransformUtil.java,v 1.1 2006-07-26 22:40:14 thib_gc Exp $
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
import java.util.*;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.lockss.filter.PdfTransform;
import org.pdfbox.cos.*;
import org.pdfbox.pdfparser.PDFStreamParser;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.PDPage;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.util.PDFOperator;

/**
 * <p>Utility classes and methods to deal with PDF transforms.</p>
 * @author Thib Guicherd-Callin
 * @see PdfTransform
 */
public class PdfTransformUtil {

  /**
   * <p>The "show text" PDF operator (<code>Tj</code>).</p>
   */
  public static final String SHOW_TEXT = "Tj";

  /**
   * <p>The "begin text object" PDF operator (<code>BT</code>).</p>
   */
  public static final String BEGIN_TEXT_OBJECT = "BT";

  /**
   * <p>The "end text object" PDF operator (<code>ET</code>).</p>
   */
  public static final String END_TEXT_OBJECT = "ET";

  /**
   * <p>A PDF transform that does nothing.</p>
   * @author Thib Guicherd-Callin
   */
  public static class IdentityPdfTransform implements PdfTransform {

    public void transform(PdfDocument pdfDocument,
                          Logger logger)
        throws IOException {
      (logger == null ? defaultLogger : logger).debug("Identity PDF transform");
    }

  }

  public interface PdfTokenSubstreamMatcher {

    boolean isLastSubstreamToken(Object candidateToken);

    boolean tokenSubstreamMatches(Object[] pdfTokens,
                                  Logger logger);

    int tokenSubstreamSize();

  }

  public interface PdfTokenSubstreamMutator extends PdfTokenSubstreamMatcher {

    void processTokenSubstream(Object[] pdfTokens,
                               Logger logger)
        throws IOException;

  }

  /**
   * <p>A logger for use by {@link PdfTransform#transform} methods
   * when no logger is passed by the caller.</p>
   */
  protected static Logger defaultLogger = Logger.getLogger("PdfMultiTransform");

  public static String getPdfString(Object candidateToken) {
    return ((COSString)candidateToken).getString();
  }

  public static void replacePdfString(Object candidateToken,
                                      String newString)
      throws IOException {
    replacePdfString(candidateToken, newString, null);
  }

  public static void replacePdfString(Object candidateToken,
                                      String newString,
                                      Logger logger)
      throws IOException {
    if (logger != null && logger.isDebug3()) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Replacing \"");
      buffer.append(PdfTransformUtil.getPdfString(candidateToken));
      buffer.append("\" by \"");
      buffer.append(newString);
      buffer.append("\"");
      logger.debug3(buffer.toString());
    }
    COSString cosString = (COSString)candidateToken;
    cosString.reset();
    cosString.append(newString.getBytes());
  }

  public static boolean isPdfOperator(Object candidateToken,
                                      String expectedOperator) {
    if (candidateToken instanceof PDFOperator) {
      return ((PDFOperator)candidateToken).getOperation().equals(expectedOperator);
    }
    return false;
  }

  public static boolean isPdfString(Object candidateToken) {
    return candidateToken instanceof COSString;
  }

  /**
   * <p>Parses a PDF document from an input stream, applies a
   * transform to it, and writes the result to an output stream.</p>
   * <p>This method closes the PDF document at the end of processing</p>
   * @param pdfInputStream  An input stream containing a PDF document.
   * @param pdfOutputStream An output stream into which to write the
   *                        transformed PDF document.
   * @param pdfTransform    A PDF transform.
   * @param logger          A logger into which to write messages.
   * @throws IOException if any processing error occurs.
   */
  public static void parse(InputStream pdfInputStream,
                           OutputStream pdfOutputStream,
                           PdfTransform pdfTransform,
                           Logger logger)
      throws IOException {
    boolean mustReleaseResources = false;
    PdfDocument pdfDocument = null;
    try {
      // Parse
      pdfDocument = new PdfDocument(pdfInputStream);
      mustReleaseResources = true;

      // Transform
      pdfTransform.transform(pdfDocument, logger);

      // Save
      pdfDocument.save(pdfOutputStream);
    }
    finally {
      if (mustReleaseResources) {
        pdfDocument.close();
      }
    }
  }

  public static boolean runPdfTokenSubstreamMatcher(PdfTokenSubstreamMatcher matcher,
                                                    PDPage pdPage,
                                                    Logger logger)
      throws IOException {
    // Parse stream
    PDStream pdStream = pdPage.getContents();
    COSStream cosStream = pdStream.getStream();
    PDFStreamParser pdfStreamParser = new PDFStreamParser(cosStream);
    pdfStreamParser.parse();

    // Prepare iteration
    Iterator iter = pdfStreamParser.getTokens().iterator();
    CircularFifoBuffer buffer = new CircularFifoBuffer(matcher.tokenSubstreamSize());

    // Look for match
    while (iter.hasNext()) {
      Object candidateToken = iter.next();
      buffer.add(candidateToken);
      if (matcher.isLastSubstreamToken(candidateToken)
          && buffer.size() <= matcher.tokenSubstreamSize()) {
        Object[] pdfTokens = IteratorUtils.toArray(buffer.iterator());
        if (matcher.tokenSubstreamMatches(pdfTokens, logger)) {
          return true;
        }
      }
    }
    return false;
  }

  public static void runPdfTokenSubstreamMutator(PdfTokenSubstreamMutator mutator,
                                                 PdfDocument pdfDocument,
                                                 PDPage pdPage,
                                                 boolean stopAfterFirstMatch,
                                                 Logger logger)
      throws IOException {
    logger.debug2("Running PDF token substream mutator");

    // Parse stream
    PDStream pdStream = pdPage.getContents();
    COSStream cosStream = pdStream.getStream();
    PDFStreamParser pdfStreamParser = new PDFStreamParser(cosStream);
    pdfStreamParser.parse();

    // Prepare iteration
    List tokens = pdfStreamParser.getTokens();
    Iterator iter = tokens.iterator();
    CircularFifoBuffer buffer = new CircularFifoBuffer(mutator.tokenSubstreamSize());
    boolean atLeastOneMatch = false;

    // Look for matches
    while (iter.hasNext()) {
      Object candidateToken = iter.next();
      buffer.add(candidateToken);
      if (mutator.isLastSubstreamToken(candidateToken)
          && buffer.size() <= mutator.tokenSubstreamSize()) {
        logger.debug3("Found candidate match");
        Object[] pdfTokens = IteratorUtils.toArray(buffer.iterator());
        if (mutator.tokenSubstreamMatches(pdfTokens, logger)) {
          logger.debug3("Applying transform to match");
          atLeastOneMatch = true;
          mutator.processTokenSubstream(pdfTokens, logger);
          if (stopAfterFirstMatch) {
            logger.debug2("Stop after first match requested");
            break;
          }
        }
      }
    }

    if (atLeastOneMatch) {
      logger.debug2("At least one match; saving stream");
      PDStream updatedStream = new PDStream(pdfDocument.getPDDocument());
      OutputStream out = updatedStream.createOutputStream();
      ContentStreamWriter tokenWriter = new ContentStreamWriter(out);
      tokenWriter.writeTokens(tokens);
      pdPage.setContents(updatedStream);
    }
  }

}
