/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.pdf;

import java.io.*;

/**
 * <p>
 * Provides access to a PDF implementation.
 * </p>
 * <p>
 * The PDF API acts as a facade; the result of intermingling objects
 * from multiple PDF implementations is undefined and will likely not
 * work at all.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see DefaultPdfDocumentFactory
 */
public interface PdfDocumentFactory {
  
  /**
   * <p>
   * Interprets the bytes in the given input stream as a PDF document.
   * </p>
   * <p>
   * The input stream may be closed by this call.
   * </p>
   * <p>
   * It is possible that the returned document is not fully parsed due
   * to lazy parsing, caching, and other implementation-dependent
   * details, so full parsing of parts of the document may not be
   * triggered until these parts are accessed.
   * </p>
   * <p>
   * If parsing fails and an exception is thrown, the state of the
   * input stream is undefined.
   * </p>
   * @param pdfInputStream A PDF document as an input stream.
   * @return A parsed PDF document.
   * @throws IOException If parsing fails at the I/O level.
   * @throws PdfCryptographyException If parsing fails at the
   *           cryptography level.
   * @throws PdfException If parsing fails at the PDF level.
   * @since 1.56
   */
  PdfDocument parse(InputStream pdfInputStream)
      throws IOException,
             PdfCryptographyException,
             PdfException;
  
}
