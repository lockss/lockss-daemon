/*
 * $Id: AmericanPhysiologicalSocietyPdfTransform.java,v 1.22 2006-11-01 22:24:09 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.util.*;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.highwire.AmericanPhysiologicalSocietyPdfTransform.EraseDateString.ProcessDateString;
import org.lockss.plugin.highwire.AmericanPhysiologicalSocietyPdfTransform.EraseVerticalBanner.ProcessEndTextObject;
import org.lockss.plugin.highwire.AmericanPhysiologicalSocietyPdfTransform.EraseVerticalBanner2.ProcessEndTextObject2;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.SanitizeMetadata;
import org.lockss.util.*;
import org.pdfbox.cos.*;

/**
 * <p>A PDF transform for PDF files of the American Physiological
 * Society.</p>
 * <h3>Overview of dynamic elements in APS PDF files</h3>
 * <p>APS PDF files have the following dynamic elements:</p>
 * <ul>
 *  <li>The modification date is when the dynamic document was
 *  generated.</li>
 *  <li>The metadata contains the modification date.</li>
 *  <li>The metadata contains an instance ID.</li>
 *  <li>The trailer contains an instance ID.</li>
 *  <li>Each page has a vertical banner with three strings forming
 *  text similar to "<code>Downloaded from physrev.physiology.org on
 *  September 14, 2006 </code>". This banner is implemented in one
 *  of two ways.</li>
 *  <li>Additionally, the first page shows the day the dynamic
 *  document was generated in a variable string like
 *  "<code>This information is current as of September 14,
 *  2006 . </code>".</li>
 * </ul>
 * <h3>Dealing with variable modification dates, instance IDs and
 * metadata</h3>
 * <p>{@link SanitizeMetadata} is used to remove the modification date,
 * erase all metadata, and replace the occurrence of the instance ID in
 * the trailer by something else.</p>
 * <h3>Dealing with the variable vertical banner</h3>
 * <p>{@link EraseVerticalBanner} and {@link ProcessEndTextObject},
 * and alternatively {@link EraseVerticalBanner2} and
 * {@link ProcessEndTextObject2}, are used to remove the vertical
 * banner text.</p>
 * <h3>Dealing with the first page's variable date string</h3>
 * <p>{@link EraseDateString} and {@link ProcessDateString} are used
 * to remove the variable date on the first page.</p>
 * <h3>Simplified version</h3>
 * <p>Because transforms are not robust across PDF format versions,
 * this transform comes with {@link Simplified},
 * a companion transform based on
 * {@link TextScrapingDocumentTransform}.</p>
 * @author Thib Guicherd-Callin
 * @see <a href="http://www.the-aps.org/">American Physiological
 *      Society</a>
 * @see <a href="http://www.physiology.org/">American Physiological
 *      Society Journals Online</a>
 * @see SanitizeMetadata
 * @see EraseVerticalBanner
 * @see EraseVerticalBanner2
 * @see EraseDateString
 * @see Simplified
 */
public class AmericanPhysiologicalSocietyPdfTransform extends SimpleOutputDocumentTransform {

  /**
   * <p>A page stream transform that normalizes the variable date
   * string found on the first page of American Physiological Society
   * PDF files using {@link ProcessDateString}.</p>
   * @author Thib Guicherd-Callin
   * @see AmericanPhysiologicalSocietyPdfTransform
   * @see ProcessDateString
   */
  public static class EraseDateString extends PageStreamTransform {

    /**
     * <p>A string replacement operator for {@link PdfUtil#SHOW_TEXT}
     * which replaces strings starting with
     * "<code>This information is current as of </code>" by
     * "<code>This information is current as of </code>".</p>
     * @author Thib Guicherd-Callin
     * @see EraseDateString
     */
    public static class ProcessDateString extends ReplaceString {

      /* Inherit documentation */
      public String getReplacement(String match) {
        return "This information is current as of ";
      }

      /* Inherit documentation */
      public boolean identify(String candidate) {
        return candidate.startsWith("This information is current as of ");
      }

    }

    /**
     * <p>Builds a new page stream transform.</p>
     * @throws IOException if any processing error occurs.
     */
    public EraseDateString() throws IOException {
      super(// "Tj" operator: replace string conditionally using ProcessDateString
            PdfUtil.SHOW_TEXT, ProcessDateString.class);
    }

  }

  /**
   * <p>A page stream transform that erases text found in the vertical
   * banner of American Physiological Society PDF files.</p>
   * <p>The page stream transform is organized as follows:</p>
   * <ul>
   *  <li>For every {@link PdfUtil#BEGIN_TEXT_OBJECT}, split with
   *  {@link SplitOperatorProcessor}.</li>
   *  <li>For every {@link PdfUtil#END_TEXT_OBJECT}, merge with
   *  {@link ProcessEndTextObject}.</li>
   * </ul>
   * <p>The first page and sometimes the pages other than the first of
   * APS PDF files have been observed to fit the template handled by
   * this transform. The pages other than the first of APS PDF files
   * that do not fit this template can be processed with
   * {@link EraseVerticalBanner2}.</p>
   * @author Thib Guicherd-Callin
   * @see AmericanPhysiologicalSocietyPdfTransform
   * @see ProcessEndTextObject
   */
  public static class EraseVerticalBanner extends PageStreamTransform {

    /**
     * <p>A conditional merge operator processor used by
     * {@link EraseVerticalBanner}, which collapses text objects that
     * contain three strings forming text similar to "<code>Downloaded
     * from physrev.physiology.org on September 14, 2006 </code>".</p>
     * <p>This operator processor functions as follows:</p>
     * <ul>
     *  <li>Check that the output list is 30 tokens long.</li>
     *  <li>Check that the first and last tokens in the output list
     *  are {@link PdfUtil#BEGIN_TEXT_OBJECT} and
     *  {@link PdfUtil#END_TEXT_OBJECT}.</li>
     *  <li>Check that there is a number at index 9. (This value
     *  is a coordinate for centering. The three strings in the
     *  banner are positioned relatively to this variable value.)</li>
     *  <li>Check that a {@link PdfUtil#SHOW_TEXT} operator is present
     *  at index 12 with its string operand. (This is the date
     *  string.)</li>
     *  <li>Check that a {@link PdfUtil#SHOW_TEXT} operator is present
     *  at index 21 with its string operand, and with a color change
     *  ({@link PdfUtil#SET_RGB_COLOR_NONSTROKING} operator) to blue
     *  at index 16. (This is the URL string.)</li>
     *  <li>Check that a {@link PdfUtil#SHOW_TEXT} operator is present
     *  at index 28 and that its string operand is <code>"Downloaded
     *  from "</code>.</li>
     *  <li>If there is a match, merge an empty text object i.e.
     *  just the first and last original tokens. Otherwise, merge all
     *  the original tokens.</li>
     * </ul>
     * <p>Sample page stream dump with a PDF 1.4 file:</p>
<pre>

</pre>
     * <p>Sample page stream dump with a PDF 1.5 file:</p>
<pre>

</pre>
     * <p>Sample page stream dump with a PDF 1.6 file:</p>
<pre>

</pre>
     * <p>In all cases, a match will result in merging this:</p>
<pre>
        PDFOperator{BT}
        PDFOperator{ET}
</pre>
     * @author Thib Guicherd-Callin
     * @see EraseVerticalBanner
     */
    public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {

      /* Inherit documentation */
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(29));
      }

      /* Inherit documentation */
      public boolean identify(List tokens) {
        // The output list is 30 tokens long
        boolean ret = tokens.size() == 30
        // Token [0] is "BT" and token [29] is "ET" (text object for three strings)
        && PdfUtil.matchTextObject(tokens, 0, 29)
        // Token [9] is a number (variable positioning due to centering; three strings positioned relatively to this value)
        && PdfUtil.isPdfNumber(tokens, 9)
        // Token [12] is "Tj" and its operand is a string (date string)
        && PdfUtil.matchShowText(tokens, 12)
        // Token [16] is "rg" and its operands are the RGB triple for blue (color of URL string)
        && PdfUtil.matchSetRgbColorNonStroking(tokens, 16, 0, 0, 1)
        // Token [21] is "Tj" and its operand is a string (URL string)
        && PdfUtil.matchShowText(tokens, 21)
        // Token [28] is "Tj" and its operand is "Downloaded from "
        && PdfUtil.matchShowText(tokens, 28, "Downloaded from ");
        logger.debug3("ProcessEndTextObject candidate match: " + ret);
        return ret;
      }

    }

    /**
     * <p>Builds a new page stream transform.</p>
     * @throws IOException if any processing error occurs.
     */
    public EraseVerticalBanner() throws IOException {
      super(// "BT" operator: split unconditionally
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            // "ET" operator: merge conditionally using ProcessEndTextObject
            PdfUtil.END_TEXT_OBJECT, ProcessEndTextObject.class);
    }

  }

  public static class EraseVerticalBanner2 extends PageStreamTransform {

    public static class ProcessEndTextObject2 extends ConditionalSubsequenceOperatorProcessor {

      /* Inherit documentation */
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(51));
      }

      /* Inherit documentation */
      public int getSubsequenceLength() {
        // Examine the last 52 tokens in the output sequence
        return 52;
      }

      /* Inherit documentation */
      public boolean identify(List tokens) {
        // The output list is 52 tokens long
        boolean ret = tokens.size() == 52
        // Token [0] is "BT" and token [17] is "ET" (date string text object)
        && PdfUtil.matchTextObject(tokens, 0, 17)
        // Token [9] is a number (variable positioning due to centering of date string)
        && PdfUtil.isPdfNumber(tokens, 9)
        // Token [12] is "Tj" and its operand is a string (date string)
        && PdfUtil.matchShowText(tokens, 12)
        // Token [16] is "rg" and its operands are the RGB triple for blue (color of URL string)
        && PdfUtil.matchSetRgbColorNonStroking(tokens, 16, 0, 0, 1)
        // Token [20] is "BT" and token [35] is "ET" (URL string text object)
        && PdfUtil.matchTextObject(tokens, 20, 35)
        // Token [29] is a number (variable positioning due to centering of URL string)
        && PdfUtil.isPdfNumber(tokens, 29)
        // Token [32] is "Tj" and its operand is a string (URL string)
        && PdfUtil.matchShowText(tokens, 32)
        // Token [38] is "BT" and token [51] is "ET" ("Downloaded from " text object)
        && PdfUtil.matchTextObject(tokens, 38, 51)
        // Token [47] is a number (variable positioning due to centering of "Downloaded from ")
        && PdfUtil.isPdfNumber(tokens, 47)
        // Token [50] is "Tj" and its operand is "Downloaded from "
        && PdfUtil.matchShowText(tokens, 50, "Downloaded from ");
        logger.debug3("ProcessEndTextObject2 candidate match: " + ret);
        return ret;
      }

    }

    public EraseVerticalBanner2() throws IOException {
      super(// "ET" operator: inspect subsequences ending in "ET" using ProcessEndTextObject2
            PdfUtil.END_TEXT_OBJECT, ProcessEndTextObject2.class);
    }

  }

  public static class FixHyperlink implements PageTransform {

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      COSArray array = pdfPage.getAnnotation(pdfPage.getNumberOfAnnotations()-1).getRectangle().getCOSArray();
      array.set(1, new COSFloat(1.0f)); // 1.0f is arbitrary
      array.set(3, new COSFloat(2.0f)); // 2.0f is arbitrary
      return true;
    }

  }

  /**
   * <p>A simplified version of
   * {@link AmericanPhysiologicalSocietyPdfTransform}, which applies
   * minimal transformations on the strings of the document, scrapes
   * all string constants and concatenates them into the result output
   * stream.</p>
   * @author Thib Guicherd-Callin
   * @see AmericanPhysiologicalSocietyPdfTransform
   */
  public static class Simplified extends TextScrapingDocumentTransform {

    public static class SimplifiedEraseVerticalBanner extends PageStreamTransform {

      public static class SimplifiedProcessEndTextObject extends ProcessEndTextObject {

        /*
         * Note that identify(List) is inherited from ProcessEndTextObject
         */

        /* Inherit documentation */
        public List getReplacement(List tokens) {
          // Clone the list of tokens
          List list = new ArrayList(tokens);
          // Erase token [11]
          list.set(11, new COSString(" "));
          return list;
        }

      }

      public SimplifiedEraseVerticalBanner() throws IOException {
        super(// "BT" operator: split unconditionally
              PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
              // "ET" operator: merge conditionally using SimplifiedProcessEndTextObject
              PdfUtil.END_TEXT_OBJECT, SimplifiedProcessEndTextObject.class);
      }


    }

    public static class SimplifiedEraseVerticalBanner2 extends PageStreamTransform {

      public static class SimplifiedProcessEndTextObject2 extends ProcessEndTextObject2 {

        /*
         * Note that identify(List) and getSubsequenceLength() are
         * inherited from ProcessEndTextObject2
         */

        /* Inherit documentation */
        public List getReplacement(List tokens) {
          List list = new ArrayList(tokens);
          list.set(11, new COSString(" "));
          return list;
        }

      }

      public SimplifiedEraseVerticalBanner2() throws IOException {
        super(// "ET" operator: inspect subsequences ending in "ET" using SimplifiedProcessEndTextObject2
              PdfUtil.END_TEXT_OBJECT, SimplifiedProcessEndTextObject2.class);
      }


    }

    public DocumentTransform makePreliminaryTransform() throws IOException {
      return new ConditionalDocumentTransform(// If
                                              new TransformFirstPage(// ...erasing the vertical banner (type 1)
                                                                     new SimplifiedEraseVerticalBanner(),
                                                                     // ...and erasing the variable date
                                                                     new EraseDateString()),
                                              // ...succeeds on the first page,
                                              // Then
                                              new TransformEachPageExceptFirst(new AggregatePageTransform(PdfUtil.OR,
                                                                                                          // ...either erase the vertical banner (type 1)
                                                                                                          new SimplifiedEraseVerticalBanner(),
                                                                                                          // ...or erase the vertical banner (type 2)
                                                                                                          new SimplifiedEraseVerticalBanner2()))
                                              // ...on other pages
                                              );
    }

  }

  public AmericanPhysiologicalSocietyPdfTransform() throws IOException {
    super(new ConditionalDocumentTransform(// If
                                           new TransformFirstPage(// ...erasing the vertical banner (type 1)
                                                                  new EraseVerticalBanner(),
                                                                  // ...and erasing the variable date
                                                                  new EraseDateString(),
                                                                  // ...and normalizing the hyperlink
                                                                  new FixHyperlink()),
                                           // ...succeeds on the first page,
                                           // Then
                                           new TransformEachPageExceptFirst(new AggregatePageTransform(PdfUtil.OR,
                                                                                                       // ...either erase the vertical banner (type 1)
                                                                                                       new EraseVerticalBanner(),
                                                                                                       // ... or erase the vertical banner (type 2)
                                                                                                       new EraseVerticalBanner2()),
                                                                            // ...and normalize the hyperlink
                                                                            new FixHyperlink()),
                                           // ...on other pages,
                                           // ...and sanitize the metadata
                                           new SanitizeMetadata()));
  }

  /**
   * <p>A logger for use by this class and nested classes</p>
   */
  private static Logger logger = Logger.getLogger("AmericanPhysiologicalSocietyPdfTransform");

}
