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

import java.io.InputStream;
import java.util.List;

import org.lockss.pdf.pdfbox.PdfBoxDocumentFactory;

/**
 * <p>
 * Abstraction for a PDF page. In the PDF hierarchy represented by
 * this package, a PDF page is in a PDF document and has zero or more
 * PDF token streams ({@link PdfTokenStream}), including a special one
 * designated as the page token stream (see
 * {@link #getPageTokenStream()}); zero or more byte streams
 * (represented by {@link InputStream} instances); and zero or more
 * annotations (represented by PDF dictionaries).
 * </p>
 * <p>
 * <b>Hierarchical</b>
 * </p>
 * <ul>
 * <li>{@link #getDocument()}</li>
 * <li>{@link #getAllByteStreams()}</li>
 * <li>{@link #getAnnotations()} / {@link #setAnnotations(List)}</li>
 * <li>{@link #getPageTokenStream()} / {@link #getAllTokenStreams()}</li>
 * </ul>
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public interface PdfPage {
  
  /**
   * <p>
   * Returns a PDF token factory associated with this PDF page.
   * </p>
   * @return A PDF token factory.
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  PdfTokenFactory getTokenFactory() throws PdfException;
  
  /**
   * <p>
   * Returns a list of all the byte streams associated with this page,
   * represented as input streams.
   * </p>
   * <p>
   * Currently, this API specifies that the following are byte
   * streams:
   * </p>
   * <ul>
   * <li>Image data streams associated with
   * {@link PdfOpcodes#BEGIN_IMAGE_DATA} and
   * {@link PdfOpcodes#BEGIN_IMAGE_OBJECT} operators.</li>
   * <li>Image data streams associated with image XObjects referenced
   * by the page token stream.</li>
   * </ul>
   * <p>
   * This definition may change in a future version. (For example, the
   * latter may be changed such that a reference in the page token
   * stream is no longer required.) The order of the byte streams is
   * undefined.
   * </p>
   * @return A list of input streams for each byte stream.
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  List<InputStream> getAllByteStreams() throws PdfException;
  
  /**
   * <p>
   * Returns a list of all the PDF token streams associated with this
   * page.
   * </p>
   * <p>
   * Currently, this API specifies that the following are byte
   * streams:
   * </p>
   * <ul>
   * <li>The page token stream.</li>
   * <li>PDF token streams associated with form XObjects referenced by
   * the page token stream.</li>
   * </ul>
   * <p>
   * This definition may change in a future version. (For example, the
   * latter may be changed to include PostScript XObjects.) The order
   * of the PDF token streams is undefined. In particular, the
   * position of the page token stream within the returned list is
   * undefined.
   * </p>
   * @return A list of {@link PdfTokenStream} instances (possibly
   *         empty).
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  List<PdfTokenStream> getAllTokenStreams() throws PdfException;
  
  /**
   * <p>
   * Returns the list of annotations associated with this page, as PDF
   * dictionaries. In other words, for every element <code>a</code> in
   * the returned list, <code>a.isDictionary()</code> is <b>true</b>.
   * The order of the returned list is undefined.
   * </p>
   * <p>
   * Note that changing the resulting list does not change the
   * annotations of the page; only a call to
   * {@link #setAnnotations(List)} does.
   * </p>
   * @return A non-<code>null</code>, possibly empty, list of PDF
   *         dictionaries, one for each annotation.
   * @throws PdfException
   * @since 1.56
   * @see PdfToken#isDictionary()
   */
  List<PdfToken> getAnnotations() throws PdfException;
  
  /**
   * <p>
   * Returns the PDF document associated with this PDF page.
   * </p>
   * @return The parent PDF document.
   * @since 1.56
   */
  PdfDocument getDocument();
  
  /**
   * <p>
   * Returns the designated page token stream.
   * </p>
   * @return The page token stream (possibly empty).
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  PdfTokenStream getPageTokenStream() throws PdfException;
  
  /**
   * <p>
   * Replaces the annotations array of this page with the one given.
   * In other words, it must be the case that for each element
   * <code>a</code> in the argument, <code>a.isDictionary()</code> is
   * <b>true</b>. These dictionaries must also obey other properties
   * to make them valid per the PDF specification. If invalid or
   * malformed annotations are passed to the page, the behavior is
   * undefined.
   * </p>
   * @param annotations A list of PDF annotations (PDF dictionaries).
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  void setAnnotations(List<PdfToken> annotations) throws PdfException;
  
}
