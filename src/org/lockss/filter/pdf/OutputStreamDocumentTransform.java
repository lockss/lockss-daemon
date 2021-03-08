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

import org.lockss.util.*;

/**
 * <p>A base version of {@link OutputDocumentTransform} that keeps the
 * output stream being processed by the current call to
 * {@link #transform(PdfDocument, OutputStream)} as an instance
 * variable.</p>
 * <p>This class implements synchronization at the level of
 * {@link #transform(PdfDocument, OutputStream)} so that under normal
 * circumstances it is safe for subclasses to assume a correlation
 * between the {@link #outputStream} field at the time
 * {@link #makeTransform} is called and the subsequent call to
 * {@link DocumentTransform#transform(PdfDocument)} being called on
 * that method's return value.</p>
 * @author Thib Guicherd-Callin
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public abstract class OutputStreamDocumentTransform implements OutputDocumentTransform {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public OutputStreamDocumentTransform() {}
  
  /**
   * <p>The output stream for the current call to
   * {@link #transform(PdfDocument, OutputStream)}.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected OutputStream outputStream;

  /**
   * <p>Makes a new document transform, which can (under normal
   * operating conditions) take advantage of the output stream
   * associated with the current call to
   * {@link #transform(PdfDocument, OutputStream)}.</p>
   * <p>Preconditions</p>
   * <ul>
   *  <li>outputStream != null</li>
   * </ul>
   * @see #outputStream
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public abstract DocumentTransform makeTransform() throws IOException;

  /* Inherit documentation */
  @Deprecated
  public synchronized boolean transform(PdfDocument pdfDocument) throws IOException {
    logger.debug3("Begin output stream document transform");
    if (outputStream == null) {
      throw new NullPointerException("Output stream uninitialized");
    }
    DocumentTransform documentTransform = makeTransform();
    boolean ret = documentTransform.transform(pdfDocument);
    logger.debug2("Output stream document transform result: " + ret);
    return ret;
  }

  /* Inherit documentation */
  @Deprecated
  public synchronized boolean transform(PdfDocument pdfDocument,
                                        OutputStream outputStream) {
    try {
      logger.debug3("Begin output stream document transform");
      this.outputStream = outputStream;
      return transform(pdfDocument);
    }
    catch (IOException ioe) {
      logger.error("Output stream document transform failed", ioe);
      return false;
    }
    finally {
      this.outputStream = null;
    }
  }

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(OutputStreamDocumentTransform.class);

}
