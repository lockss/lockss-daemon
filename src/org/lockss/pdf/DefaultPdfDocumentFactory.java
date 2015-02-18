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

import org.lockss.pdf.pdfbox.PdfBoxDocumentFactory;

/**
 * <p>
 * The LOCKSS system provides at least one PDF implementation, by
 * default accessed through this class. As of this writing, the
 * default implementation is based on PDFBox 1.6.0 and provided by
 * {@link PdfBoxDocumentFactory}.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public class DefaultPdfDocumentFactory {

  /**
   * <p>
   * Our singleton instance.
   * </p>
   * @since 1.56
   */
  private static final PdfDocumentFactory factory = new PdfBoxDocumentFactory();

  /**
   * <p>
   * Obtains a default PDF document factory.
   * </p>
   * @return A {@link PdfDocumentFactory} instance.
   * @since 1.56
   */
  public static PdfDocumentFactory getInstance() {
    return factory;
  }
  
  /**
   * <p>
   * This class cannot be instantiated.
   * </p>
   * @since 1.56
   */
  private DefaultPdfDocumentFactory() {
    // Prevent instantiation
  }
  
}
