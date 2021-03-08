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
import java.util.List;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.*;
import org.lockss.util.*;

/**
 * <p>
 * Notes to help the transition out of {@link HighWirePdfFilterFactory} and
 * PDFBox 0.7.3. See {@link HighWirePdfFilterFactory} for details.
 * </p>
 * <p>
 * Originally contained an {@link OutputDocumentTransform} and, within
 * {@link SagePublicationsPdfTransform.Simplified}, a simplified
 * {@link ResilientTextScrapingDocumentTransform}. But no TDB file uses
 * {@link SagePublicationsPdfTransform}, only
 * {@link SagePublicationsPdfTransform.Simplified}, so the contents of the
 * parent transform have been purged for brevity.
 * </p>
 * <p>
 * This class defines two additional building blocks.
 * </p>
 * <p>
 * {@link SagePublicationsPdfTransform.RecognizeSyntheticPage}, is a page
 * transform that recognizes the following pattern by enumerating the page
 * stream tokens (forward). Right at the beginning Tj with a URL string, then Tj
 * with the string
 * {@code "The online version of this article can be found at:"}, then Tj with a
 * string matching such patterns as {@code "Published by"} or
 * {@code "...on behalf of"}, then Tj with the URL string
 * {@code "http://www.sagepublications.com"}, then Tj with the string
 * {@code "Additional services and information for "}, then Tj with a URL
 * string, then Tj with the string {@code "Downloaded from "}.
 * </p>
 * <p>
 * {@link RemovePage} is a page transform that simply removes its page from the
 * enclosing document.
 * </p>
 * <p>
 * {@link SagePublicationsPdfTransform.Simplified} is a
 * {@link ResilientTextScrapingDocumentTransform} that does the following. If
 * {@link RecognizeSyntheticPage} succeeds on the first page, then the first
 * page is removed, and also {@link CollapseDownloadedFrom} is applied to all
 * remaining pages, and also {@link NormalizeMetadata} is applied.
 * </p>
 * 
 * @see HighWirePdfFilterFactory
 * @see SagePublicationsPdfTransform.Simplified
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class SagePublicationsPdfTransform {
  
  @Deprecated
  protected static Logger logger = Logger.getLogger(SagePublicationsPdfTransform.class);
  
  @Deprecated
  public static class RecognizeSyntheticPage implements PageTransform {

    @Deprecated
    public boolean transform(PdfPage pdfPage) throws IOException {
      // Initially, assume the recognition fails
      boolean ret = false;
      logger.debug3("RecognizeSyntheticPage invoked");

      // Get the tokens for the entire page
      List tokens = pdfPage.getStreamTokens();

      // Iterate through the tokens (forward)
      int progress = 0;
      iteration: for (int tok = 0 ; tok < tokens.size() ; ++tok) {
        // Select the current step in the recognition
        switch (progress) {

          case 0:
            // Beginning of sequence
            if (tok != 0) { break iteration; } else { ++progress; }
            break;

          case 1:
          case 6:
            // Text: a URL (no longer necessarily under sagepub.com)
            if (PdfUtil.matchShowTextMatches(tokens, tok, "http://[-0-9A-Za-z]+(?:\\.[-0-9A-Za-z]+)+")) { ++progress; }
            break;

          case 2:
            // Text: "The online version of this article can be found at:"
            if (PdfUtil.matchShowText(tokens, tok, "The online version of this article can be found at:")) { ++progress; }
            break;

          case 3:
            // Text: "Published by:" or "Published on behalf of"
            if (PdfUtil.matchShowTextMatches(tokens, tok, "Published (?:by:|on behalf of)")) { ++progress; }
            break;

          case 4:
            // Text: "http://www.sagepublications.com"
            if (PdfUtil.matchShowText(tokens, tok, "http://www.sagepublications.com")) { ++progress; }
            break;

          case 5:
            // Text: "Additional services and information for "
            if (PdfUtil.matchShowText(tokens, tok, "Additional services and information for ")) { ++progress; }
            break;

          // case 6: see case 1

          case 7:
            // Text: "Downloaded from "
            if (PdfUtil.matchShowText(tokens, tok, "Downloaded from ")) { ret = true; break iteration; }
            break;

          default:
            // Should never happen
            break iteration;

        }

      }

      // Return true if only if all the steps were visited successfully
      if (logger.isDebug3()) { logger.debug3("RecognizeSyntheticPage returning " + ret); }
      return ret;
    }

  }

  @Deprecated
  public static class RemovePage implements PageTransform {

    @Deprecated
    public boolean transform(PdfPage pdfPage) throws IOException {
      // Remove this page from the document
      pdfPage.getPdfDocument().removePage(pdfPage);
      return true;
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
      return new ConditionalDocumentTransform(// If the first page...
                                              new TransformFirstPage(// ...is recognized as a synthetic page,
                                                                     new RecognizeSyntheticPage()),
                                              // Then...
                                              new DocumentTransform[] {
                                                // The first page...
                                                new TransformFirstPage(// ...is removed,
                                                                       new RemovePage()),
                                                // ...and on all the pages now that the first is gone...
                                                new TransformEachPage(// ...collapse "Downloaded from",
                                                                      new CollapseDownloadedFrom(au)),
//                                              // ...and normalize the metadata
                                                new HighWirePdfFilterFactory.NormalizeMetadata(),
                                              });
    }

  }

}
