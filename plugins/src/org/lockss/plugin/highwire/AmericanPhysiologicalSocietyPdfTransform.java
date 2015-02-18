/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.filter.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.AmericanPhysiologicalSocietyPdfTransform.NormalizeCurrentAsOf.ReplaceCurrentAsOf;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.*;
import org.lockss.util.*;
import org.pdfbox.util.operator.OperatorProcessor;

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
 * <p>{@link NormalizeMetadata} is used to remove the modification date,
 * erase all metadata, and replace the occurrence of the instance ID in
 * the trailer by something else.</p>
 * <h3>Dealing with the variable vertical banner</h3>
 * <p>{@link EraseVerticalBanner} and {@link ProcessEndTextObject},
 * and alternatively {@link EraseVerticalBanner2} and
 * {@link ProcessEndTextObject2}, are used to remove the vertical
 * banner text.</p>
 * <h3>Dealing with the first page's variable date string</h3>
 * <p>{@link NormalizeCurrentAsOf} and {@link ReplaceCurrentAsOf} are used
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
 * @see NormalizeMetadata
 * @see EraseVerticalBanner
 * @see EraseVerticalBanner2
 * @see NormalizeCurrentAsOf
 * @see Simplified
 */
@Deprecated
public class AmericanPhysiologicalSocietyPdfTransform
    implements OutputDocumentTransform,
               ArchivalUnitDependent {

  /**
   * <p>A page stream transform that normalizes the variable date
   * string found on the first page of American Physiological Society
   * PDF files using {@link ReplaceCurrentAsOf}.</p>
   * @author Thib Guicherd-Callin
   * @see AmericanPhysiologicalSocietyPdfTransform
   * @see ReplaceCurrentAsOf
   */
  public static class NormalizeCurrentAsOf extends PageStreamTransform {

    /**
     * <p>A string replacement operator for {@link PdfUtil#SHOW_TEXT}
     * which replaces strings starting with
     * "<code>This information is current as of </code>" by
     * "<code>This information is current as of </code>".</p>
     * @author Thib Guicherd-Callin
     * @see NormalizeCurrentAsOf
     */
    public static class ReplaceCurrentAsOf extends ReplaceString {

      /* Inherit documentation */
      public String getReplacement(String match) {
        return "This information is current as of ";
      }

      /* Inherit documentation */
      public boolean identify(String candidate) {
        return candidate.startsWith("This information is current as of ");
      }

    }

    public NormalizeCurrentAsOf(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                     OperatorProcessor.class);
              }
            },
            // "Tj" operator: replace string conditionally using ReplaceCurrentAsOf
            PdfUtil.SHOW_TEXT, ReplaceCurrentAsOf.class);
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
  @Deprecated
  public static class Simplified
      extends ResilientTextScrapingDocumentTransform
      implements ArchivalUnitDependent {

    protected ArchivalUnit au;

    public void setArchivalUnit(ArchivalUnit au) {
      this.au = au;
    }

    public DocumentTransform makePreliminaryTransform() throws IOException {
      if (au == null) throw new IOException("Uninitialized AU-dependent transform");
      return new ConditionalDocumentTransform(// If...
                                              new AggregateDocumentTransform(// ...on the first page...
                                                                             new TransformFirstPage(// ...collapsing "Donwloaded from" succeeds...
                                                                                                    new CollapseDownloadedFrom(au)),
                                                                             // ...and on at least one page...
                                                                             new TransformEachPage(PdfUtil.OR,
                                                                                                   // ...normalizing "This information is current as of" succeeds,
                                                                                                   new NormalizeCurrentAsOf(au))),
                                              // ...then on every page except the first...
                                              new TransformEachPageExceptFirst(// ...collapse "Downloaded from"
                                                                               new CollapseDownloadedFrom(au)));
    }

  }

  protected ArchivalUnit au;

  public void setArchivalUnit(ArchivalUnit au) {
    this.au = au;
  }

  public boolean transform(PdfDocument pdfDocument,
                           OutputStream outputStream) {
    return PdfUtil.applyAndSave(this,
                                pdfDocument,
                                outputStream);
  }

  public boolean transform(PdfDocument pdfDocument) throws IOException {
    if (au == null) throw new IOException("Uninitialized AU-dependent transform");
    DocumentTransform documentTransform = new ConditionalDocumentTransform(// If...
                                                                           new AggregateDocumentTransform(// ...on the first page...
                                                                                                          new TransformFirstPage(// ...collapsing "Downloaded from" and normalizing its hyperlink succeeds...
                                                                                                                                 new CollapseDownloadedFromAndNormalizeHyperlinks(au)),
                                                                                                          // ...and on at least one page...
                                                                                                          new TransformEachPage(PdfUtil.OR,
                                                                                                                                // ...normalizing "This information is current as of" succeeds,
                                                                                                                                new NormalizeCurrentAsOf(au))),
                                                                           // ...then on every page except the first...
                                                                           new TransformEachPageExceptFirst(// ...collapse "Downloaded from" and normalize its hyperlink,
                                                                                                            new CollapseDownloadedFromAndNormalizeHyperlinks(au)),
                                                                           // ...and normalize the metadata
                                                                           new NormalizeMetadata());
    return documentTransform.transform(pdfDocument);
  }
}
