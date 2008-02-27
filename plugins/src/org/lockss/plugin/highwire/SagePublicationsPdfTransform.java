/*
 * $Id: SagePublicationsPdfTransform.java,v 1.3 2008-02-27 21:48:25 thib_gc Exp $
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
import java.util.List;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.*;
import org.lockss.util.*;

public class SagePublicationsPdfTransform
    implements OutputDocumentTransform,
               ArchivalUnitDependent {

  public static class RecognizeSyntheticPage implements PageTransform {

    public boolean transform(PdfPage pdfPage) throws IOException {
      // Initially, assume the recognition fails
      boolean ret = false;

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
          case 7:
            // Text: http://<something>.sagepub.com
            if (PdfUtil.matchShowTextMatches(tokens, tok, "http://[-0-9A-Za-z]+\\.sagepub\\.com")) { ++progress; }
            break;

          case 2:
            // Text: "The online version of this article can be found at:"
            if (PdfUtil.matchShowText(tokens, tok, "The online version of this article can be found at:")) { ++progress; }
            break;

          case 3:
            // Text: "Published by:"
            if (PdfUtil.matchShowText(tokens, tok, "Published by:")) { ++progress; }
            break;

          case 4:
            // Text: "http://www.sagepublications.com"
            if (PdfUtil.matchShowText(tokens, tok, "http://www.sagepublications.com")) { ++progress; }
            break;

          case 5:
            // Text: "can be found at:"
            if (PdfUtil.matchShowText(tokens, tok, "can be found at:")) { ++progress; }
            break;

          case 6:
            // Text: "Additional services and information for "
            if (PdfUtil.matchShowText(tokens, tok, "Additional services and information for ")) { ++progress; }
            break;

          // case 7: see case 2

          case 8:
            // Text: "Downloaded from "
            if (PdfUtil.matchShowText(tokens, tok, "Downloaded from ")) { ret = true; break iteration; }
            break;

          default:
            // Should never happen
            break iteration;

        }

      }

      // Return true if only if all the steps were visited successfully
      return ret;
    }

  }

  public static class RemovePage implements PageTransform {

    public boolean transform(PdfPage pdfPage) throws IOException {
      // Remove this page from the document
      pdfPage.getPdfDocument().removePage(pdfPage);
      return true;
    }

  }

  public static class Simplified
      extends ResilientTextScrapingDocumentTransform
      implements ArchivalUnitDependent {

    protected ArchivalUnit au;

    public void setArchivalUnit(ArchivalUnit au) {
      this.au = au;
    }

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
                                                new TransformEachPage(// ...collapse "Downloaded from"
                                                                      new CollapseDownloadedFrom(au))
                                              });
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
    DocumentTransform documentTransform = new ConditionalDocumentTransform(// If the first page...
                                                                           new TransformFirstPage(// ...is recognized as a synthetic page,
                                                                                                  new RecognizeSyntheticPage()),
                                                                           // Then...
                                                                           new DocumentTransform[] {
                                                                             // The first page...
                                                                             new TransformFirstPage(// ...has its hyperlinks normalized,
                                                                                                    new NormalizeHyperlinks(),
                                                                                                    // ...and is removed,
                                                                                                    new RemovePage()),
                                                                             // ...and on all the pages now that the first is gone...
                                                                             new TransformEachPage(// ...collapse "Downloaded from" and normalize the hyperlink,
                                                                                                   new CollapseDownloadedFromAndNormalizeHyperlinks(au)),
                                                                             // ...and normalize the metadata
                                                                             new NormalizeMetadata()
                                                                           });
    return documentTransform.transform(pdfDocument);
  }

}
