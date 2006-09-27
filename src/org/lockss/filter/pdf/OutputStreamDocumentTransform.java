/*
 * $Id: OutputStreamDocumentTransform.java,v 1.2 2006-09-27 08:00:32 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
 */
public abstract class OutputStreamDocumentTransform implements OutputDocumentTransform {

  /**
   * <p>The output stream for the current call to
   * {@link #transform(PdfDocument, OutputStream)}.</p>
   */
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
   */
  public abstract DocumentTransform makeTransform() throws IOException;

  /* Inherit documentation */
  public synchronized boolean transform(PdfDocument pdfDocument) throws IOException {
    logger.debug2("Begin output stream document transform");
    if (outputStream == null) {
      throw new NullPointerException("Output stream uninitialized");
    }
    DocumentTransform documentTransform = makeTransform();
    boolean ret = documentTransform.transform(pdfDocument);
    logger.debug2("Output stream document transform result: " + ret);
    return ret;
  }

  /* Inherit documentation */
  public synchronized boolean transform(PdfDocument pdfDocument,
                                        OutputStream outputStream) {
    try {
      logger.debug2("Begin output stream document transform");
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
   */
  private static Logger logger = Logger.getLogger("OutputStreamDocumentTransform");

}
