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

package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.AmericanPhysiologicalSocietyPdfTransform.NormalizeCurrentAsOf.ReplaceCurrentAsOf;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.*;
import org.lockss.util.*;
import org.pdfbox.util.operator.OperatorProcessor;

/**
 * <p>
 * Notes to help the transition out of {@link HighWirePdfFilterFactory} and
 * PDFBox 0.7.3. See {@link HighWirePdfFilterFactory} for details.
 * </p>
 * <p>
 * Originally contained an {@link OutputDocumentTransform} and, within
 * {@link AmericanPhysiologicalSocietyPdfTransform.Simplified}, a simplified
 * {@link ResilientTextScrapingDocumentTransform}. But no TDB file uses
 * {@link AmericanPhysiologicalSocietyPdfTransform.Simplified}, only
 * {@link AmericanPhysiologicalSocietyPdfTransform}, so the contents of
 * the nested transform have been purged for brevity.
 * </p>
 * <p>
 * Turns out, this class already had an old narrative description, found below:
 * </p>
 * <hr />
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
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
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
  @Deprecated
  public static class NormalizeCurrentAsOf extends PageStreamTransform {

    /**
     * <p>A string replacement operator for {@link PdfUtil#SHOW_TEXT}
     * which replaces strings starting with
     * "<code>This information is current as of </code>" by
     * "<code>This information is current as of </code>".</p>
     * @author Thib Guicherd-Callin
     * @see NormalizeCurrentAsOf
     */
    @Deprecated
    public static class ReplaceCurrentAsOf extends ReplaceString {

      /* Inherit documentation */
      @Deprecated
      public String getReplacement(String match) {
        return "This information is current as of ";
      }

      /* Inherit documentation */
      @Deprecated
      public boolean identify(String candidate) {
        return candidate.startsWith("This information is current as of ");
      }

    }

    @Deprecated
    public NormalizeCurrentAsOf(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              @Deprecated public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                     OperatorProcessor.class);
              }
            },
            // "Tj" operator: replace string conditionally using ReplaceCurrentAsOf
            PdfUtil.SHOW_TEXT, ReplaceCurrentAsOf.class);
    }

  }

  @Deprecated
  protected ArchivalUnit au;

  @Deprecated
  public void setArchivalUnit(ArchivalUnit au) {
    this.au = au;
  }

  @Deprecated
  public boolean transform(PdfDocument pdfDocument,
                           OutputStream outputStream) {
    return PdfUtil.applyAndSave(this,
                                pdfDocument,
                                outputStream);
  }

  @Deprecated
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
