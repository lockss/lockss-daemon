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
 * {@link AmericanMedicalAssociationPdfTransform.Simplified}, a simplified
 * {@link ResilientTextScrapingDocumentTransform}. But no TDB file uses
 * {@link AmericanMedicalAssociationPdfTransform}, only
 * {@link AmericanMedicalAssociationPdfTransform.Simplified}, so the contents of
 * the parent transform have been purged for brevity.
 * </p>
 * <p>
 * This class defines the additional building block
 * {@link AmericanMedicalAssociationPdfTransform.NormalizeCurrentAsOf}, a page
 * stream transform that replaces strings beginning with
 * {@code "current as of "} by simply {@code "current as of "}.
 * </p>
 * <p>
 * {@link AmericanMedicalAssociationPdfTransform.Simplified} is a
 * {@link ResilientTextScrapingDocumentTransform} that does the following. If
 * {@link CollapseDownloadedFrom} succeeds on the first page, then apply
 * {@link CollapseDownloadedFrom} to all the other pages, and also apply
 * {@link AmericanMedicalAssociationPdfTransform.NormalizeCurrentAsOf} to the
 * first page.
 * </p>
 * 
 * @see HighWirePdfFilterFactory
 * @see AmericanMedicalAssociationPdfTransform.Simplified
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class AmericanMedicalAssociationPdfTransform {

  @Deprecated
  public static class NormalizeCurrentAsOf extends PageStreamTransform {
    
    @Deprecated
    public static class ReplaceCurrentAsOf extends ReplaceString {

      @Override
      @Deprecated
      public String getReplacement(String match) {
        return "current as of ";
      }

      @Override
      @Deprecated
      public boolean identify(String candidate) {
        return candidate.startsWith("current as of ");
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
  public static class Simplified
      extends ResilientTextScrapingDocumentTransform
      implements ArchivalUnitDependent {

    @Deprecated
    protected ArchivalUnit au;

    @Deprecated
    public void setArchivalUnit(ArchivalUnit au) {
      this.au = au;
    }

    @Deprecated
    public DocumentTransform makePreliminaryTransform() throws IOException {
      if (au == null) throw new IOException("Uninitialized AU-dependent transform");
      return new ConditionalDocumentTransform(// If on the first page...
                                              new TransformFirstPage(// ...collapsing "Downloaded from" succeeds,
                                                                     new CollapseDownloadedFrom(au)),
                                              // Then on all other pages...
                                              new TransformEachPageExceptFirst(// ...collapse "Downloaded from",
                                                                               new CollapseDownloadedFrom(au)),
                                              // And on the first page...
                                              new TransformFirstPage(// normalize "current as of"
                                                                     new NormalizeCurrentAsOf(au)));
    }

  }

}
