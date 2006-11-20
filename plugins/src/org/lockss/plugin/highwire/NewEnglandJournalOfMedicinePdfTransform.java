/*
 * $Id: NewEnglandJournalOfMedicinePdfTransform.java,v 1.8 2006-11-20 22:37:28 thib_gc Exp $
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
import java.util.List;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.NormalizeMetadata;
import org.lockss.plugin.highwire.NewEnglandJournalOfMedicinePdfTransform.EraseVariableFooter.ProcessEndTextObject;
import org.lockss.util.*;

/**
 * <p>A PDF transform for New England Journal of Medicine PDF files.</p>
 * <h3>Overview of dynamic elements in NEJM PDF files</h3>
 * <p>NEJM PDF files have the following dynamic elements:</p>
 * <ul>
 *  <li>The modification date is when the dynamic document was
 *  generated.</li>
 *  <li>The metadata contains the modification date.</li>
 *  <li>The metadata contains an instance ID.</li>
 *  <li>The trailer contains an instance ID.</li>
 *  <li>Each page has a footer with text like "<code>Downloaded from
 *  www.nejm.org at Stanford University on September 16, 2006 . </code>".
 * </ul>
 * <h3>Dealing with variable modification dates, instance IDs and
 * metadata</h3>
 * <p>{@link NormalizeMetadata} is used to remove the modification date,
 * erase all metadata, and replace the occurrence of the instance ID in
 * the trailer by something else.</p>
 * <h3>Dealing with the variable footer</h3>
 * <p>{@link EraseVariableFooter} and {@link ProcessEndTextObject}
 * are used to remove the variable footer text.</p>
 * <h3>Simplified version</h3>
 * <p>Because transforms are not robust across PDF format versions,
 * this transform comes with {@link Simplified},
 * a companion transform based on
 * {@link TextScrapingDocumentTransform}.</p>
 * @author Thib Guicherd-Callin
 * @see <a href="http://content.nejm.org/">New England Journal of
 *      Medicine</a>
 * @see NormalizeMetadata
 * @see EraseVariableFooter
 * @see Simplified
 */
public class NewEnglandJournalOfMedicinePdfTransform extends SimpleOutputDocumentTransform {

  /**
   * <p>A page stream transform that erases the variable footer text
   * on New England Journal of Medicine PDF files.</p>
   * <p>The page stream transform is organized as follows:</p>
   * <ul>
   *  <li>For every {@link PdfUtil#BEGIN_TEXT_OBJECT}, split with
   *  {@link SplitOperatorProcessor}.</li>
   *  <li>For every {@link PdfUtil#END_TEXT_OBJECT}, merge with
   *  {@link ProcessEndTextObject}.</li>
   * </ul>
   * @author Thib Guicherd-Callin
   * @see NewEnglandJournalOfMedicinePdfTransform
   * @see ProcessEndTextObject
   */
  public static class EraseVariableFooter extends PageStreamTransform {

    /**
     * <p>A conditional merge operator processor used by
     * {@link EraseVariableFooter}, which collapses text objects
     * containing a string that starts with
     * "<code>Downloaded from </code>".</p>
     * <p>This operator processor functions as follows:</p>
     * <ul>
     *  <li>Check that the first and last tokens in the output list
     *  are {@link PdfUtil#BEGIN_TEXT_OBJECT} and
     *  {@link PdfUtil#END_TEXT_OBJECT}.</li>
     *  <li>Check that the last operator (before the closing
     *  {@link PdfUtil#END_TEXT_OBJECT}) is {@link PdfUtil#SHOW_TEXT}
     *  and that its operand starts with
     *  "<code>Downloaded from </code>".</li>
     *  <li>If there is a match, merge an empty text object i.e.
     *  just the first and last original tokens. Otherwise, merge all
     *  the original tokens.</li>
     * </ul>
     * <p>Looking back from the end deals more predictably with
     * changes in PDF format versions, as the copyright line is
     * sometimes in the same text object as the dynamic string,
     * sometimes in the preceding one. (See below for examples.)</p>
     * <p>Sample page stream dump with a PDF 1.4 file:</p>
<pre>
        PDFOperator{BT}
        COSName{T1_8}
        COSInt{1}
        PDFOperator{Tf}
        COSInt{0}
        PDFOperator{Tc}
        COSInt{0}
        PDFOperator{Tw}
        COSInt{8}
        COSInt{0}
        COSInt{0}
        COSInt{8}
        COSFloat{182.56415}
        COSFloat{15.99997}
        PDFOperator{Tm}
        COSString{Copyright M-oM-?M-= 2006 Massachusetts Medical Society. All rights reserved. }
        PDFOperator{Tj}
        COSFloat{-2.07748}
        COSInt{1}
        PDFOperator{Td}
        COSString{Downloaded from www.nejm.org at Stanford University on September 8, 2006 . }
        PDFOperator{Tj}
        PDFOperator{ET}
</pre>
     * <p>Sample page stream dump with a PDF 1.5 file:</p>
<pre>
        PDFOperator{BT}
        COSName{T1_5}
        COSInt{1}
        PDFOperator{Tf}
        COSInt{0}
        PDFOperator{Tc}
        COSInt{0}
        PDFOperator{Tw}
        COSInt{8}
        COSInt{0}
        COSInt{0}
        COSInt{8}
        COSFloat{182.5641}
        COSInt{16}
        PDFOperator{Tm}
        COSString{Copyright M-oM-?M-= 2006 Massachusetts Medical Society. All rights reserved. }
        PDFOperator{Tj}
        PDFOperator{ET}
        COSName{GS1}
        PDFOperator{gs}
        PDFOperator{BT}
        COSName{T1_5}
        COSInt{1}
        PDFOperator{Tf}
        COSInt{8}
        COSInt{0}
        COSInt{0}
        COSInt{8}
        COSFloat{163.7202}
        COSInt{24}
        PDFOperator{Tm}
        COSString{Downloaded from www.nejm.org at Stanford University on September 14, 2006 . }
        PDFOperator{Tj}
        PDFOperator{ET}
</pre>
     * <p>Sample page stream dump with a PDF 1.6 file:</p>
<pre>
        PDFOperator{BT}
        COSName{T1_8}
        COSInt{1}
        PDFOperator{Tf}
        COSInt{0}
        PDFOperator{Tc}
        COSInt{0}
        PDFOperator{Tw}
        COSInt{8}
        COSInt{0}
        COSInt{0}
        COSInt{8}
        COSFloat{182.5641}
        COSInt{16}
        PDFOperator{Tm}
        COSString{Copyright M-oM-?M-= 2006 Massachusetts Medical Society. All rights reserved. }
        PDFOperator{Tj}
        PDFOperator{ET}
        COSName{GS2}
        PDFOperator{gs}
        PDFOperator{BT}
        COSName{T1_8}
        COSInt{1}
        PDFOperator{Tf}
        COSInt{8}
        COSInt{0}
        COSInt{0}
        COSInt{8}
        COSFloat{176.1722}
        COSInt{24}
        PDFOperator{Tm}
        COSString{Downloaded from www.nejm.org at Stanford University on July 25, 2006 . }
        PDFOperator{Tj}
        PDFOperator{ET}
</pre>
     * <p>In all cases, a match will result in merging this:</p>
<pre>
        PDFOperator{BT}
        PDFOperator{ET}
</pre>
     * @author Thib Guicherd-Callin
     * @see EraseVariableFooter
     */
    public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {

      /* Inherit documentation */
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(tokens.size() - 1));
      }

      /* Inherit documentation */
      public boolean identify(List tokens) {
        // Look back from the end
        int last = tokens.size() - 1;
        // Token [0] is "BT" and token [last] is "ET"
        boolean ret = PdfUtil.matchTextObject(tokens, 0, last)
        // Token [last-1] is "Tj" and its operand starts with "Downloaded from "
        && PdfUtil.matchShowTextStartsWith(tokens, last - 1, "Downloaded from ");
        logger.debug3("ProcessEndTextObject candidate match: " + ret);
        return ret;
      }

    }

    /**
     * <p>Builds a new page stream transform.</p>
     * @throws IOException if any processing error occurs.
     */
    public EraseVariableFooter() throws IOException {
      super(// "BT" operator: split unconditionally
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            // "ET" operator: merge conditionally using ProcessEndTextObject
            PdfUtil.END_TEXT_OBJECT, ProcessEndTextObject.class);
    }

  }

  /**
   * <p>A simplified version of
   * {@link NewEnglandJournalOfMedicinePdfTransform} that only
   * normalizes variable strings and concatenates all string constants
   * into the result output stream (ignoring all formatting, numbers
   * and other data).</p>
   * <p>This transform has low coverage but is more resilient to
   * changes in PDF format versions than its counterpart
   * {@link NewEnglandJournalOfMedicinePdfTransform}.</p>
   * @author Thib Guicherd-Callin
   * @see NewEnglandJournalOfMedicinePdfTransform
   * @see SimplifiedEraseVariableFooter
   */
  public static class Simplified extends TextScrapingDocumentTransform {

    /**
     * <p>A simplified version of {@link EraseVariableFooter}
     * that uses {@link SimplifiedProcessShowText} to normalize the
     * variable footer string.</p>
     * @author Thib Guicherd-Callin
     * @see Simplified
     * @see SimplifiedProcessShowText
     * @see EraseVariableFooter
     */
    public static class SimplifiedEraseVariableFooter extends PageStreamTransform {

      /**
       * <p>A string replacement operator for {@link PdfUtil#SHOW_TEXT}
       * which replaces strings starting with
       * "<code>Downloaded from </code>" by
       * "<code>Downloaded from </code>".</p>
       * @author Thib Guicherd-Callin
       * @see SimplifiedEraseVariableFooter
       */
      public static class SimplifiedProcessShowText extends ReplaceString {

        /* Inherit documentation */
        public String getReplacement(String match) {
          // Replace by "Downloaded from "
          return "Downloaded from ";
        }

        /* Inherit documentation */
        public boolean identify(String candidate) {
          // The string starts with "Donwloaded from "
          return candidate.startsWith("Downloaded from ");
        }

      }

      /**
       * <p>Builds a new page stream transform.</p>
       * @throws IOException if any processing error occurs.
       */
      public SimplifiedEraseVariableFooter() throws IOException {
        super(// "Tj" operator: replace string conditionally using SimplifiedProcessShowText
              PdfUtil.SHOW_TEXT, SimplifiedProcessShowText.class);
      }

    }

    /* Inherit documentation */
    public DocumentTransform makePreliminaryTransform() throws IOException {
      return new ConditionalDocumentTransform(// If erasing the variable string of the first page succeeds
                                              new TransformFirstPage(new SimplifiedEraseVariableFooter()),
                                              // Then erase the variable string on other pages
                                              new TransformEachPageExceptFirst(new SimplifiedEraseVariableFooter()));
    }

  }

  /**
   * <p>Builds a a new transform for New England Journal of Medicine
   * PDF files.</p>
   * <p>The overall transform is expressed as a strict conditional.
   * The "if" transform is removing the variable text on the first
   * page with {@link TransformFirstPage} and
   * {@link EraseVariableFooter}. The "then" transform is the
   * (implicit) aggregation of removing the variable text on every
   * page except the first with {@link TransformEachPageExceptFirst}
   * and {@link EraseVariableFooter}, and of sanitizing the metadata
   * with {@link NormalizeMetadata}.</p>
   * <p>Because the conditional is strict (default), if the "if" part
   * succeeds but the "else" part does not, a
   * {@link DocumentTransformException} is thrown.</p>
   * @throws IOException if any processing error occurs.
   */
  public NewEnglandJournalOfMedicinePdfTransform() throws IOException {
    super(new ConditionalDocumentTransform(// If erasing the variable footer of the first page succeeds
                                           new TransformFirstPage(new EraseVariableFooter()),
                                           // Then erase the variable footer on other pages
                                           new TransformEachPageExceptFirst(new EraseVariableFooter()),
                                           // ...and sanitize the metadata
                                           new NormalizeMetadata()));
  }

  /**
   * <p>A logger for use by this class and nested classes.</p>
   */
  private static Logger logger = Logger.getLogger("NewEnglandJournalOfMedicinePdfTransform");

}
