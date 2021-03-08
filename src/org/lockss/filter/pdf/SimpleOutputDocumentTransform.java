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

import java.io.*;

import org.lockss.filter.pdf.DocumentTransformUtil.DocumentTransformDecorator;
import org.lockss.util.*;

/**
 * <p>An output document transform that processes PDF documents with
 * a given document transform, then saves the resulting PDF document
 * into the output stream.</p>
 * @author Thib Guicherd-Callin
 * @see PdfDocument#save
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class SimpleOutputDocumentTransform
    extends DocumentTransformDecorator
    implements OutputDocumentTransform {

  /**
   * <p>Builds a new simple output document transform based on the
   * given document transform.</p>
   * @param documentTransform A document transform.
   * @see DocumentTransformDecorator#DocumentTransformDecorator(DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public SimpleOutputDocumentTransform(DocumentTransform documentTransform) {
    super(documentTransform);
  }

  /* Inherit documentation */
  @Deprecated
  public boolean transform(PdfDocument pdfDocument) throws IOException {
    return documentTransform.transform(pdfDocument);
  }

  /* Inherit documentation */
  @Deprecated
  public boolean transform(PdfDocument pdfDocument,
                           OutputStream outputStream) {
    return PdfUtil.applyAndSave(this,
                                pdfDocument,
                                outputStream);
  }

}
