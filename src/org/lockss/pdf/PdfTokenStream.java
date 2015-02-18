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

import java.util.List;

/**
 * <p>
 * Abstraction for a PDF token stream. In the PDF hierarchy
 * represented by this package, a PDF token stream is in a PDF page
 * and is a sequence of PDF tokens ({@link PdfToken}).
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public interface PdfTokenStream {

  /**
   * <p>
   * Returns the PDF page associated with this PDF token stream.
   * </p>
   * @return The parent PDF page.
   * @since 1.56
   */
  PdfPage getPage();

  /**
   * <p>
   * Returns a PDF token factory associated with this PDF token stream.
   * </p>
   * @return A PDF token factory.
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  PdfTokenFactory getTokenFactory() throws PdfException;
  
  /**
   * <p>
   * Returns the sequence of PDF tokens contained in this PDF token
   * stream.
   * </p>
   * <p>
   * Note that changing the resulting list does not change the tokens
   * of the stream; only a call to {@link #setTokens(List)} does.
   * </p>
   * @return A list of PDF tokens (possibly empty).
   * @throws PdfException If PDF processing fails.
   */
  List<PdfToken> getTokens() throws PdfException;
  
  /**
   * <p>
   * Replaces the tokens in the token stream with the given ones.
   * </p>
   * @param newTokens A list of PDF tokens for this stream.
   * @throws PdfException If PDF processing fails.
   */
  void setTokens(List<PdfToken> newTokens) throws PdfException;
  
}
