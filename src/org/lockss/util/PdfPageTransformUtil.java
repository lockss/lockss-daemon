/*
 * $Id: PdfPageTransformUtil.java,v 1.4 2006-08-23 19:14:07 thib_gc Exp $
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
import org.lockss.filter.pdf.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdfparser.PDFStreamParser;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.PDPage;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.util.*;

/**
 * <p>Utility classes and methods to deal with PDF page transforms.</p>
 * @author Thib Guicherd-Callin
 * @see PdfPageTransform
 */
public class PdfPageTransformUtil {

  /**
   * <p>This PDF page transform implements a PDF token sequence
   * mutator that enables subclasses to replace string matches by
   * other strings.</p>
   * @author Thib Guicherd-Callin
   */
  public static abstract class PdfAbstractStringReplacePageTransform
      extends PdfAbstractTokenSequenceMutator {

    /**
     * <p>Builds a new transform.</p>
     * @param stopAfterFirstMatchOnPage Whether the mutator should
     *                                  stop after a first match on a
     *                                  page.
     */
    public PdfAbstractStringReplacePageTransform(boolean stopAfterFirstMatchOnPage) {
      super(2, SHOW_TEXT, stopAfterFirstMatchOnPage);
    }

    /**
     * <p>Computes the string that should replace the matched
     * string.</p>
     * @param candidateForReplacement The matched string which is to
     *                                be replaced.
     * @return The string which should replace the matched string.
     */
    public abstract String getReplacementString(String candidateForReplacement);

    /**
     * <p>Determines if a string found on the PDF page is a candidate
     * for replacement by this transform.</p>
     * @param str A candidate string from the PDF page.
     * @return True if the string from the PDF string is a candidate
     *         for replacement, false otherwise.
     */
    public abstract boolean isCandidateForReplacement(String str);

    /* Inherit documentation */
    public void processTokenSequence(Object[] pdfTokens) throws IOException {
      String str = PdfPageTransformUtil.getPdfString(pdfTokens[0]);
      PdfPageTransformUtil.replacePdfString(pdfTokens[0], getReplacementString(str));
    }

    /* Inherit documentation */
    public boolean tokenSequenceMatches(Object[] pdfTokens) {
      boolean ret = PdfPageTransformUtil.isPdfString(pdfTokens[0])
      && isCandidateForReplacement(PdfPageTransformUtil.getPdfString(pdfTokens[0]));
      logger.debug3("Evaluating candidate match: " + Boolean.toString(ret));
      return ret;
    }

  }

  public static abstract class PdfAbstractTokenSequenceMatcher
      implements PdfPageTransform, PdfTokenSequenceMatcher {

    protected String lastTokenInSequence;

    protected int sequenceLength;

    public PdfAbstractTokenSequenceMatcher(int sequenceLength,
                                           String lastTokenInSequence) {
      this.sequenceLength = sequenceLength;
      this.lastTokenInSequence = lastTokenInSequence;
    }

    public boolean isLastTokenInSequence(Object candidateToken) {
      return isPdfOperator(candidateToken, lastTokenInSequence);
    }

    public int tokenSequenceSize() {
      return sequenceLength;
    }

    public void transform(PdfDocument pdfDocument,
                          PDPage pdfPage)
        throws IOException {
      runPdfTokenSequenceMatcher(this, pdfPage);
    }

  }

  public static abstract class PdfAbstractTokenSequenceMutator
      implements PdfPageTransform, PdfTokenSequenceMutator {

    protected String lastTokenInSequence;

    protected int sequenceLength;

    protected boolean stopAfterFirstMatch;

    public PdfAbstractTokenSequenceMutator(int sequenceLength,
                                           String lastTokenInSequence) {
      this(sequenceLength, lastTokenInSequence, false);
    }

    public PdfAbstractTokenSequenceMutator(int sequenceLength,
                                           String lastTokenInSequence,
                                           boolean stopAfterFirstMatch) {
      this.sequenceLength = sequenceLength;
      this.lastTokenInSequence = lastTokenInSequence;
      this.stopAfterFirstMatch = stopAfterFirstMatch;
    }

    /* Inherit documentation */
    public boolean isLastTokenInSequence(Object candidateToken) {
      return isPdfOperator(candidateToken, lastTokenInSequence);
    }

    /* Inherit documentation */
    public int tokenSequenceSize() {
      return sequenceLength;
    }

    /* Inherit documentation */
    public void transform(PdfDocument pdfDocument,
                          PDPage pdfPage)
        throws IOException {
      runPdfTokenSequenceMutator(this, pdfDocument, pdfPage, stopAfterFirstMatch);
    }

  }

  /**
   * <p>An implementation of
   * {@link PdfAbstractStringReplacePageTransform} which can carry
   * two strings of state (an old string and a new string), and is
   * thus well-suited for situations when the replacement string is
   * a constant string and the string to be matched is based on a
   * constant string.</p>
   * @author Thib Guicherd-Callin
   * @see #makeTransformEquals(String, String, boolean)
   * @see #makeTransformIgnoreCase(String, String, boolean)
   * @see #makeTransformMatches(String, String, boolean)
   * @see #makeTransformStartsWith(String, String, boolean)
   */
  public static abstract class PdfStringReplacePageTransform
      extends PdfAbstractStringReplacePageTransform {

    /**
     * <p>The replacement string (new string).</p>
     */
    protected String newString;

    /**
     * <p>The old string on which the matched string is based.</p>
     */
    protected String oldString;

    /**
     * <p>Builds a new transform.</p>
     * @param oldString                 The old string on which the
     *                                  matched string is based.
     * @param newString                 The replacement string (new
     *                                  string).
     * @param stopAfterFirstMatchOnPage Whether the mutator should
     *                                  stop after a first match on a
     *                                  page.
     */
    protected PdfStringReplacePageTransform(String oldString,
                                            String newString,
                                            boolean stopAfterFirstMatchOnPage) {
      super(stopAfterFirstMatchOnPage);
      this.oldString = oldString;
      this.newString = newString;
    }

    /* Inherit documentation */
    public String getReplacementString(String candidateForReplacement) {
      return newString;
    }

    /* Inherit documentation */
    public abstract boolean isCandidateForReplacement(String str);

    /**
     * <p>Builds a transform which looks for an old string (using
     * {@link String#equals}) and replaces it by a new string.</p>
     * @param oldString                 The string to be matched.
     * @param newString                 The replacement string.
     * @param stopAfterFirstMatchOnPage Whether the mutator should
     *                                  stop after a first match on a
     *                                  page.
     * @return A PDF page transform.
     */
    public static PdfStringReplacePageTransform makeTransformEquals(String oldString,
                                                                    String newString,
                                                                    boolean stopAfterFirstMatchOnPage) {
      return new PdfStringReplacePageTransform(oldString, newString, stopAfterFirstMatchOnPage) {
        public boolean isCandidateForReplacement(String str) {
          return str.equals(oldString);
        }
      };
    }

    /**
     * <p>Builds a transform which looks for an old string (using
     * {@link String#equalsIgnoreCase}) and replaces it by a new
     * string.</p>
     * @param oldString                 The string to be matched.
     * @param newString                 The replacement string.
     * @param stopAfterFirstMatchOnPage Whether the mutator should
     *                                  stop after a first match on a
     *                                  page.
     * @return A PDF page transform.
     */
    public static PdfStringReplacePageTransform makeTransformIgnoreCase(String oldString,
                                                                        String newString,
                                                                        boolean stopAfterFirstMatchOnPage) {
      return new PdfStringReplacePageTransform(oldString, newString, stopAfterFirstMatchOnPage) {
        public boolean isCandidateForReplacement(String str) {
          return str.equalsIgnoreCase(oldString);
        }
      };
    }

    /**
     * <p>Builds a transform which looks for a string which matches
     * the given regular expressions (using Java's
     * {@link String#matches}) and replaces it by a new string.</p>
     * @param regex                     The regular expression against
     *                                  which to match.
     * @param newString                 The replacement string.
     * @param stopAfterFirstMatchOnPage Whether the mutator should
     *                                  stop after a first match on a
     *                                  page.
     * @return A PDF page transform.
     */
    public static PdfStringReplacePageTransform makeTransformMatches(String regex,
                                                                     String newString,
                                                                     boolean stopAfterFirstMatchOnPage) {
      return new PdfStringReplacePageTransform(regex, newString, stopAfterFirstMatchOnPage) {
        public boolean isCandidateForReplacement(String str) {
          return str.matches(oldString);
        }
      };
    }

    /**
     * <p>Builds a transform which looks for a string which starts
     * with a pattern (using {@link String#startsWith(String)})
     * and replaces it by a new string.</p>
     * @param beginning                 The string by which the matched
     *                                  string should start.
     * @param newString                 The replacement string.
     * @param stopAfterFirstMatchOnPage Whether the mutator should
     *                                  stop after a first match on a
     *                                  page.
     * @return A PDF page transform.
     */
    public static PdfStringReplacePageTransform makeTransformStartsWith(String beginning,
                                                                        String newString,
                                                                        boolean stopAfterFirstMatchOnPage) {
      return new PdfStringReplacePageTransform(beginning, newString, stopAfterFirstMatchOnPage) {
        public boolean isCandidateForReplacement(String str) {
          return str.startsWith(oldString);
        }
      };
    }

  }

  /**
   * <p>An interface for classes that are interested in examining
   * subsequences of PDF tokens from PDF streams, such as the content
   * stream of the pages of a PDF document.</p>
   * <p>This kind of matching is appropriate when one is looking to
   * identify specific patterns within short subsequences of PDF
   * tokens, such as "all subsequences of length 19 where the first
   * token is A, the fifth token is of type B, the eleventh token is
   * of type C and looks like D, and the nineteenth token is E". This
   * kind of pattern would be captured by writing an implementation of
   * this interface for which {@link #tokenSequenceSize} returns 19,
   * {@link #isLastTokenInSequence} determines if the argument is E,
   * and {@link #tokenSequenceMatches} determines if pdfTokens[0] is
   * A, pdfTokens[4] is of type B, and pdfTokens[10] is of type C and
   * looks like D. See
   * {@link PdfPageTransformUtil#runPdfTokenSequenceMatcher} for a
   * description of how this interface is used to evaluate candidate
   * matches.</p>
   * @author Thib Guicherd-Callin
   * @see PdfPageTransformUtil#runPdfTokenSequenceMatcher
   */
  public interface PdfTokenSequenceMatcher {

    /**
     * <p>Determines if the argument corresponds to the last token in
     * a candidate subsequence.</p>
     * <p>This is used by
     * {@link PdfPageTransformUtil#runPdfTokenSequenceMatcher} as it
     * iterates through a stream of tokens to stop and propose the
     * subsequence that ends with the current token as a candidate
     * match to {@link #tokenSequenceMatches}.</p>
     * @param candidateToken A candidate PDF token.
     * @return True if this token would be the last one in a
     *         subsequence of tokens appropriate for evaluation by
     *         {@link #tokenSequenceMatches}, false otherwise.
     * @see PdfPageTransformUtil#runPdfTokenSequenceMatcher
     */
    boolean isLastTokenInSequence(Object candidateToken);

    /**
     * <p>Determines if the argument subsequence of PDF tokens matches
     * the pattern that this instance is looking for.</p>
     * <p>Preconditions:</p>
     * <ul>
     *  <li><code>pdfTokens.length == tokenSequenceSize()</code></li>
     *  <li><code>isLastTokenInSequence(pdfTokens[tokenSequenceSize() - 1])</code></li>
     * </ul>
     * @param pdfTokens A candidate PDF token subsequence.
     * @return True if the argument subsequence is a match, false
     *         otherwise.
     * @see PdfPageTransformUtil#runPdfTokenSequenceMatcher
     * @see PdfTokenSequenceMatcher#isLastTokenInSequence
     * @see PdfTokenSequenceMatcher#tokenSequenceSize
     */
    boolean tokenSequenceMatches(Object[] pdfTokens);

    /**
     * <p>Declares the length of subsequences that this instance is
     * interested in seeing.</p>
     * <p>This method should return constant values over time.</p>
     * @return The length of subsequences processed by this instance.
     * @see PdfPageTransformUtil#runPdfTokenSequenceMatcher
     */
    int tokenSequenceSize();

  }

  /**
   * <p>An interface for PDF token sequence matchers that are also
   * interested in applying an in-place transformation to the
   * subsequences of tokens they match.</p>
   * See {@link PdfPageTransformUtil#runPdfTokenSequenceMutator} for a
   * description of how this interface is used to evaluate candidate
   * matches and apply modifications.</p>
   * @author Thib Guicherd-Callin
   * @see PdfPageTransformUtil#runPdfTokenSequenceMutator
   */
  public interface PdfTokenSequenceMutator extends PdfTokenSequenceMatcher {

    /**
     * <p>Applies a modification to a subsequence of PDF tokens
     * matched by this instance.</p>
     * <p>Preconditions:</p>
     * <ul>
     *  <li><code>pdfTokens.length == tokenSequenceSize()</code></li>
     *  <li><code>isLastTokenInSequence(pdfTokens[tokenSequenceSize() - 1])</code></li>
     *  <li><code>tokenSequenceMatches(pdfTokens, logger)</code></li>
     * </ul>
     * @param pdfTokens A matching PDF token subsequence.
     * @throws IOException if any processing error occurs.
     * @see PdfPageTransformUtil#runPdfTokenSequenceMatcher
     * @see PdfTokenSequenceMatcher#isLastTokenInSequence
     * @see PdfTokenSequenceMatcher#tokenSequenceMatches
     * @see PdfTokenSequenceMatcher#tokenSequenceSize
     */
    void processTokenSequence(Object[] pdfTokens) throws IOException;

  }

  /**
   * <p>The "begin text object" PDF operator (<code>BT</code>).</p>
   */
  public static final String BEGIN_TEXT_OBJECT = "BT";

  /**
   * <p>The "end text object" PDF operator (<code>ET</code>).</p>
   */
  public static final String END_TEXT_OBJECT = "ET";

  /**
   * <p>The "show text" PDF operator (<code>Tj</code>).</p>
   */
  public static final String SHOW_TEXT = "Tj";

  protected static Logger logger = Logger.getLoggerWithInitialLevel("PdfPageTransformUtil", Logger.LEVEL_DEBUG3);

  /**
   * <p>Extracts the String data associated with a PDF token that is
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
  public static boolean isBeginTextObjectOperator(Object candidateToken) {
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
  public static boolean isEndTextObjectOperator(Object candidateToken) {
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
  public static boolean isShowTextOperator(Object candidateToken) {
    return isPdfOperator(candidateToken, SHOW_TEXT);
  }

  /**
   * <p>Replaces the contents of a PDF string by another string.</p>
   * @param pdfString A PDF string.
   * @param newString The new contents for the PDF string.
   * @throws IOException if any processing error occurs.
   */
  public static void replacePdfString(Object pdfString,
                                      String newString)
      throws IOException {
    if (logger.isDebug3()) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Replacing \"");
      buffer.append(getPdfString(pdfString));
      buffer.append("\" by \"");
      buffer.append(newString);
      buffer.append("\"");
      logger.debug3(buffer.toString());
    }
    COSString cosString = (COSString)pdfString;
    cosString.reset();
    cosString.append(newString.getBytes());
  }

  /**
   * <p>Runs a PDF token sequence matcher on the stream of tokens
   * found in the contents of a PDF page.</p>
   * <p>Below is a simplified explanation of the process.</p>
   * <p>The stream is read one token at a time, keeping the most
   * recent subsequence of length
   * {@link PdfTokenSequenceMatcher#tokenSequenceSize} in memory.
   * When the last token read is identified by
   * {@link PdfTokenSequenceMatcher#isLastTokenInSequence},
   * the current subsequence is evaluated by
   * {@link PdfTokenSequenceMatcher#tokenSequenceMatches}. This
   * proceeds until the matcher reports a match (return true),
   * or the stream runs out of tokens (return false).</p>
   * @param matcher A PDF token sequence matcher.
   * @param pdPage  A PDF page (of type {@link PDPage}).
   * @return True if a match is found in the page, false otherwise.
   * @throws IOException if any processing error occurs
   */
  public static boolean runPdfTokenSequenceMatcher(PdfTokenSequenceMatcher matcher,
                                                   PDPage pdPage)
      throws IOException {
    logger.debug2("Running PDF token sequence matcher");

    // Parse stream
    PDStream pdStream = pdPage.getContents();
    COSStream cosStream = pdStream.getStream();
    PDFStreamParser pdfStreamParser = new PDFStreamParser(cosStream);
    pdfStreamParser.parse();

    // Prepare iteration
    Iterator iter = pdfStreamParser.getTokens().iterator();
    CircularFifoBuffer buffer = new CircularFifoBuffer(matcher.tokenSequenceSize());

    // Look for match
    while (iter.hasNext()) {
      Object candidateToken = iter.next();
      buffer.add(candidateToken);
      if (matcher.isLastTokenInSequence(candidateToken)
          && buffer.size() <= matcher.tokenSequenceSize()) {
        logger.debug3("Found candidate match");
        Object[] pdfTokens = IteratorUtils.toArray(buffer.iterator());
        if (matcher.tokenSequenceMatches(pdfTokens)) {
          logger.debug3("Successful match");
          return true;
        }
      }
    }

    logger.debug3("No successful match");
    return false;
  }

  /**
   * <p>Runs a PDF token sequence mutator on the stream of tokens
   * found in the contents of a PDF page.</p>
   * <p>Below is a simplified explanation of the process.</p>
   * <p>The stream is read one token at a time, keeping the most
   * recent subsequence of length
   * {@link PdfTokenSequenceMatcher#tokenSequenceSize} in memory.
   * When the last token read is identified by
   * {@link PdfTokenSequenceMatcher#isLastTokenInSequence},
   * the current subsequence is evaluated by
   * {@link PdfTokenSequenceMatcher#tokenSequenceMatches}. If a match
   * is found, it is modified by calling
   * {@link PdfTokenSequenceMutator#processTokenSequence}. This
   * proceeds until the stream runs out of tokens (return false).</p>
   * <p>Through the boolean argument, the process can be limited to
   * the first match only.</p>
   * @param mutator             A PDF token sequence matcher.
   * @param pdfDocument         A PDF document.
   * @param pdfPage              A PDF page (of type {@link PDPage})
   *                            from the PDF document.
   * @param stopAfterFirstMatch Whether the process should stop after
   *                            the first successful match (true) or
   *                            keep going over the entire page on
   *                            any subsequent matches (false).
   * @throws IOException if any processing error occurs
   */
  public static void runPdfTokenSequenceMutator(PdfTokenSequenceMutator mutator,
                                                PdfDocument pdfDocument,
                                                PDPage pdfPage,
                                                boolean stopAfterFirstMatch)
      throws IOException {
    logger.debug2("Running PDF token sequence mutator");

    // Parse stream
    PDStream pdStream = pdfPage.getContents();
    COSStream cosStream = pdStream.getStream();
    PDFStreamParser pdfStreamParser = new PDFStreamParser(cosStream);
    pdfStreamParser.parse();

    // Prepare iteration
    List tokens = pdfStreamParser.getTokens();
    Iterator iter = tokens.iterator();
    CircularFifoBuffer buffer = new CircularFifoBuffer(mutator.tokenSequenceSize());
    boolean atLeastOneMatch = false;

    // Look for matches
    while (iter.hasNext()) {
      Object candidateToken = iter.next();
      buffer.add(candidateToken);
      if (mutator.isLastTokenInSequence(candidateToken)
          && buffer.size() <= mutator.tokenSequenceSize()) {
        logger.debug3("Found candidate match");
        Object[] pdfTokens = IteratorUtils.toArray(buffer.iterator());
        if (mutator.tokenSequenceMatches(pdfTokens)) {
          logger.debug3("Applying transform to match");
          atLeastOneMatch = true;
          mutator.processTokenSequence(pdfTokens);
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
      pdfPage.setContents(updatedStream);
    }
  }

}
