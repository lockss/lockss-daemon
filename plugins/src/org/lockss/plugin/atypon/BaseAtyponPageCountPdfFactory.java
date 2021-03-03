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

package org.lockss.plugin.atypon;

import org.lockss.daemon.PluginException;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;
import java.io.*;

public class BaseAtyponPageCountPdfFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(BaseAtyponPageCountPdfFactory.class);
  private static final String PDF_HASH_STRING_FORMAT = "This PDF file has: %s pages total";

  /**
   * <p>
   * This instance's PDF document factory.
   * </p>
   * @since 1.56
   */
  protected PdfDocumentFactory pdfDocumentFactory;

  /**
   *
   * A filter factory that interprets its input as a PDF document and
   * generate a fixed format string contains its page count
   * @param pdfDocumentFactory
   */
  public BaseAtyponPageCountPdfFactory(PdfDocumentFactory pdfDocumentFactory) {
    this.pdfDocumentFactory = pdfDocumentFactory;
  }

  /**
   * <p>
   * Makes an instance using {@link DefaultPdfDocumentFactory}.
   * </p>
   * @since 1.56
   * @see DefaultPdfDocumentFactory
   */
  public BaseAtyponPageCountPdfFactory() {
    this(DefaultPdfDocumentFactory.getInstance());
  }

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in, String encoding) throws PluginException {
    PdfDocument pdfDocument = null;
    int pageCount = 0;

    try {
      pdfDocument = pdfDocumentFactory.makeDocument(in);
      pageCount = pdfDocument.getNumberOfPages();

      // default to an empty string so it raises a zero hash error
      String pageCountHashString = "";
      if (pageCount > 0) {
        pageCountHashString = String.format(PDF_HASH_STRING_FORMAT, String.valueOf(pageCount));
      }
      InputStream pageCountHashInputStream = new ByteArrayInputStream(pageCountHashString.toString().getBytes());
      return pageCountHashInputStream;

    } catch (IOException ioe) {
      throw new PluginException(ioe);
    } catch (PdfException pdfe) {
      throw new PluginException(pdfe);
    } finally {
      PdfUtil.safeClose(pdfDocument);
    }
  }
}
