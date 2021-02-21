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

package org.lockss.pdf;

import java.io.InputStream;
import java.util.*;

import org.apache.commons.collections4.IteratorUtils;
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
 * <li>{@link #getByteStreamList()}</li>
 * <li>{@link #getAnnotations()} / {@link #setAnnotations(List)}</li>
 * <li>{@link #getPageTokenStream()} / {@link #getTokenStreamList()}</li>
 * </ul>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public interface PdfPage {
  
  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #getByteStreamList()} in 1.76.
   * </p>
   * <p>
   * This method can be memory-intensive; {@link #getByteStreamIterable()} or
   * {@link #getByteStreamIterator()} are recommended instead. In particular,
   * {@code for (InputStream is : mypage.getAllByteStreams())} or
   * {@code for (InputStream is : mypage.getByteStreamList())} should be
   * replaced with
   * {@code for (InputStream is : mypage.getByteStreamIterable())}.
   * </p>
   * 
   * @return A list of input streams for each byte stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @deprecated Renamed to {@link #getByteStreamList()} in 1.76.
   * @see #getByteStreamIterator()
   */
  @Deprecated
  default List<InputStream> getAllByteStreams() throws PdfException {
    return IteratorUtils.toList(getByteStreamIterator());
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #getTokenStreamList()} in 1.76.
   * </p>
   * <p>
   * This method can be memory-intensive; {@link #getTokenStreamIterable()} or
   * {@link #getTokenStreamIterator()} are recommended instead. In particular,
   * {@code for (PdfTokenStream ts : mypage.getAllTokenStreams())} or
   * {@code for (PdfTokenStream ts : mypage.getTokenStreamList())} should be
   * replaced with
   * {@code for (PdfTokenStream ts : mypage.getTokenStreamIterable())}.
   * </p>
   * 
   * @return A list of {@link PdfTokenStream} instances (possibly empty).
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @deprecated Renamed to {@link #getTokenStreamList()} in 1.76.
   * @see #getTokenStreamIterator()
   */
  @Deprecated
  default List<PdfTokenStream> getAllTokenStreams() throws PdfException {
    return IteratorUtils.toList(getTokenStreamIterator());
  }

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
   * 
   * @return A non-<code>null</code>, possibly empty, list of PDF
   *         dictionaries, one for each annotation.
   * @throws PdfException
   * @since 1.56
   * @see PdfToken#isDictionary()
   */
  List<? extends PdfToken> getAnnotations() throws PdfException;
  
  /**
   * <p>
   * Returns an iterable (suitable for a "for each" loop) of all the byte
   * streams associated with this page. See {@link #getByteStreamIterator()}
   * for details.
   * </p>
   * 
   * @return An iterable of input streams for each byte stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #getByteStreamIterator()
   */
  default Iterable<InputStream> getByteStreamIterable() throws PdfException {
    return IteratorUtils.asIterable(getByteStreamIterator());
  }

  /**
   * <p>
   * Returns an iterator of all the byte streams associated with this page,
   * represented as input streams.
   * </p>
   * <p>
   * Currently, this API specifies that the following are byte streams:
   * </p>
   * <ul>
   * <li>Image data streams associated with
   * {@link PdfOpcodes#BEGIN_INLINE_IMAGE_DATA} and
   * {@link PdfOpcodes#BEGIN_INLINE_IMAGE} operators anywhere in the page.</li>
   * <li>Image XObjects referenced anywhere in the page.</li>
   * </ul>
   * <p>
   * The order of the byte streams is undefined.
   * </p>
   * 
   * @return An iterator of input streams for each byte stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   */
  Iterator<InputStream> getByteStreamIterator() throws PdfException;
  
  /**
   * <p>
   * Returns a list of all the byte streams associated with this page,
   * represented as input streams. See {@link #getByteStreamIterator()} for
   * details.
   * </p>
   * <p>
   * This method can be memory-intensive; {@link #getByteStreamIterable()} or
   * {@link #getByteStreamIterator()} are recommended instead. In particular,
   * {@code for (InputStream is : mypage.getByteStreamList())} should be
   * replaced with
   * {@code for (InputStream is : mypage.getByteStreamIterable())}.
   * </p>
   * 
   * @return A list of input streams for each byte stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #getByteStreamIterator()
   */
  default List<InputStream> getByteStreamList() throws PdfException {
    return IteratorUtils.toList(getByteStreamIterator());
  }
  
  /**
   * <p>
   * Returns the PDF document associated with this PDF page.
   * </p>
   * 
   * @return The parent PDF document.
   * @since 1.56
   */
  PdfDocument getDocument();
  
  /**
   * <p>
   * Returns the designated page token stream.
   * </p>
   * 
   * @return The page token stream (possibly empty).
   * @throws PdfException If PDF processing fails.
   * @since 1.56
   */
  PdfTokenStream getPageTokenStream() throws PdfException;
  
  /**
   * <p>
   * Returns an iterable (suitable for a "for each" loop) of all the token
   * streams associated with this page. See {@link #getTokenStreamIterator()}
   * for details.
   * </p>
   * 
   * @return An iterable of {@link PdfTokenStream} instances for each token
   *         stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #getTokenStreamIterator()
   */
  default Iterable<? extends PdfTokenStream> getTokenStreamIterable() throws PdfException {
    return IteratorUtils.asIterable(getTokenStreamIterator());
  }
  
  /**
   * <p>
   * Returns an iterator of all the PDF token streams associated with this
   * page.
   * </p>
   * <p>
   * Currently, this API specifies that the following are token
   * streams:
   * </p>
   * <ul>
   * <li>The page token stream.</li>
   * <li>PDF token streams associated with form XObjects referenced anywhere in
   * the page.</li>
   * </ul>
   * <p>
   * The first element in the iterator, if any, is the page token stream; the
   * order of any other token streams in the iterator is undefined.
   * </p>
   * 
   * @return A list of {@link PdfTokenStream} instances (possibly
   *         empty).
   * @throws PdfException If PDF processing fails.
   * @since 1.76
   */
  Iterator<? extends PdfTokenStream> getTokenStreamIterator() throws PdfException;
  
  /**
   * <p>
   * Returns a list of all the PDF token streams associated with this page. See
   * {@link #getTokenStreamIterator()} for details.
   * </p>
   * <p>
   * This method can be memory-intensive; {@link #getTokenStreamIterable()} or
   * {@link #getTokenStreamIterator()} are recommended instead. In particular,
   * {@code for (PdfTokenStream ts : mypage.getTokenStreamList())} should be
   * replaced with
   * {@code for (PdfTokenStream ts : mypage.getTokenStreamIterable())}.
   * </p>
   * 
   * @return A list of {@link PdfTokenStream} instances (possibly empty).
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #getTokenStreamIterator()
   */
  default List<? extends PdfTokenStream> getTokenStreamList() throws PdfException {
    return IteratorUtils.toList(getTokenStreamIterator());
  }
  
  /**
   * <p>
   * Replaces the annotations array of this page with the one given. In other
   * words, it must be the case that for each element {@code a} in the argument,
   * {@code a.isDictionary()} is {@code true}. These dictionaries must also obey
   * other properties to make them valid per the PDF specification. If invalid
   * or malformed annotations are passed to the page, the behavior is undefined.
   * </p>
   * 
   * @param annotations
   *          A list of PDF annotations (PDF dictionaries).
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setAnnotations(List<PdfToken> annotations) throws PdfException;
  
}
