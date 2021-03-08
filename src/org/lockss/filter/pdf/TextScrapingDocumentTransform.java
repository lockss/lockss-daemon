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

package org.lockss.filter.pdf;

import java.io.IOException;

import org.lockss.filter.pdf.PageTransformUtil.ExtractStringsToOutputStream;

/**
 * <p>A version of {@link OutputStreamDocumentTransform} that first
 * applies a document transform to the PDF document being processed,
 * then collects all string constants in the resulting PDF document
 * into the output stream.</p>
 * @author Thib Guicherd-Callin.
 * @see PageTransformUtil.ExtractStringsToOutputStream
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public abstract class TextScrapingDocumentTransform extends OutputStreamDocumentTransform {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public TextScrapingDocumentTransform() {}
  
  /**
   * <p>Makes a new document transform which will be applied before
   * scraping all string constants from the document with
   * {@link PageTransformUtil.ExtractStringsToOutputStream}.</p>
   * @return A preliminary document transform.
   * @throws IOException if any processing error occurs.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public abstract DocumentTransform makePreliminaryTransform() throws IOException;

  /* Inherit documentation */
  @Deprecated
  public DocumentTransform makeTransform() throws IOException {
    return new ConditionalDocumentTransform(makePreliminaryTransform(),
                                            new TransformEachPage(new ExtractStringsToOutputStream(outputStream)));
  }

}
