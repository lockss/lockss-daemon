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

import java.io.*;
import java.util.*;

import org.apache.commons.collections4.IteratorUtils;
import org.w3c.dom.Document;

/**
 * <p>
 * Abstraction for a PDF document. In the PDF hierarchy represented by this
 * package, a PDF document has zero or more PDF pages ( {@link PdfPage}).
 * </p>
 * <p>
 * <b>Document-level metadata</b>
 * </p>
 * <ul>
 * <li>{@link #getAuthor()} / {@link #setAuthor(String)} /
 * {@link #unsetAuthor()}</li>
 * <li>{@link #getCreationDate()} / {@link #setCreationDate(Calendar)} /
 * {@link #unsetCreationDate()}</li>
 * <li>{@link #getCreator()} / {@link #setCreator(String)} /
 * {@link #unsetCreator()}</li>
 * <li>{@link #getKeywords()} / {@link #setKeywards(String)} /
 * {@link #unsetKeywords()}</li>
 * <li>{@link #getLanguage()} / {@link #setLanguage(String)} /
 * {@link #unsetLanguage()}</li>
 * <li>{@link #getModificationDate()} / {@link #setModificationDate(Calendar)} /
 * {@link #unsetModificationDate()}</li>
 * <li>{@link #getProducer()} / {@link #setProducer(String)} /
 * {@link #unsetProducer()}</li>
 * <li>{@link #getSubject()} / {@link #setSubject(String)} /
 * {@link #unsetSubject()}</li>
 * <li>{@link #getTitle()} / {@link #setTitle(String)} /
 * {@link #unsetTitle()}</li>
 * <li>{@link #getMetadataAsXmp()} / {@link #setMetadataFromXmp(Document)}</li>
 * </ul>
 * <p>
 * <b>Hierarchical</b>
 * </p>
 * <ul>
 * <li>{@link #getNumberOfPages()} / {@link #getPage(int)} / {@link #getPages()}
 * / {@link #removePage(int)}</li>
 * </ul>
 * <p>
 * <b>Structural</b>
 * </p>
 * <ul>
 * <li>{@link #close()}</li>
 * <li>{@link #getTokenFactory()}</li>
 * <li>{@link #getTrailer()} / {@link #setTrailer(Map)}</li>
 * <li>{@link #save(OutputStream)}</li>
 * </ul>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public interface PdfDocument {

  /**
   * <p>
   * Releases resources associated with this document when it is no longer in
   * use. The behavior of this object if operations are performed on it after it
   * is closed is undefined.
   * </p>
   * 
   * @throws PdfException
   *           If there is a major error releasing resources associated with
   *           this object.
   * @since 1.56
   */
  void close() throws PdfException;

  /**
   * <p>
   * Retrieves the author field from the given document.
   * </p>
   * 
   * @return The author field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  String getAuthor() throws PdfException;

  /**
   * <p>
   * Retrieves the creation date field from the given document.
   * </p>
   * 
   * @return The creation date field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  Calendar getCreationDate() throws PdfException;

  /**
   * <p>
   * Retrieves the creator field from the given document.
   * </p>
   * 
   * @return The creator field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  String getCreator() throws PdfException;

  /**
   * <p>
   * Returns a PDF document factory instance suitable for this document.
   * </p>
   * 
   * @return A PDF document factory instance.
   * @since 1.70
   */
  PdfDocumentFactory getDocumentFactory();

  /**
   * <p>
   * Retrieves the keywords field from the given document.
   * </p>
   * 
   * @return The keywords field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  String getKeywords() throws PdfException;

  /**
   * <p>
   * Retrieves the language field from the given document.
   * </p>
   * 
   * @return The language field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  String getLanguage() throws PdfException;

  /**
   * <p>
   * Retrieves the document metadata as a standalone string.
   * </p>
   * <p>
   * Note that in PDF parlance, "metadata" is a field you can get and set, just
   * like "author" is a field you can get and set.
   * </p>
   * 
   * @return The metadata as a string, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  String getMetadata() throws PdfException;

  /**
   * <p>
   * Retrieves the document metadata as an XMP document.
   * </p>
   * <p>
   * Note that in PDF parlance, "metadata" is a field you can get and set, just
   * like "author" is a field you can get and set.
   * </p>
   * 
   * @return The metadata as an XMP document, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  Document getMetadataAsXmp() throws PdfException;

  /**
   * <p>
   * Retrieves the modification date field from the given document.
   * </p>
   * 
   * @return The creation date field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  Calendar getModificationDate() throws PdfException;

  /**
   * <p>
   * Returns the number of pages in this document.
   * </p>
   * 
   * @return The document's number of pages.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  int getNumberOfPages() throws PdfException;

  /**
   * <p>
   * Gets the page at the given (zero-based) index.
   * </p>
   * 
   * @param index
   *          The page's index in the range from <code>0</code> inclusive to
   *          {@link #getNumberOfPages()} exclusive.
   * @return A {@link PdfPage} instance corresponding to the requested page.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  PdfPage getPage(int index) throws PdfException;

  /**
   * <p>
   * Returns an iterable (suitable for a "for each" loop) of all the pages in
   * this document. See {@link #getPageIterator()} for details.
   * </p>
   * 
   * @return An iterable of {@link PdfPage} instances.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #getPageIterator()
   */
  default Iterable<? extends PdfPage> getPageIterable() throws PdfException {
    return IteratorUtils.asIterable(getPageIterator());
  }

  /**
   * <p>
   * Returns an iterator of all the pages in this document.
   * </p>
   * 
   * @return An iterator of {@link PdfPage} instances.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   */
  Iterator<? extends PdfPage> getPageIterator() throws PdfException;

  /**
   * <p>
   * Gets a list of the pages in this document. See {@link #getPageIterator()}
   * for details.
   * </p>
   * <p>
   * This method can be memory-intensive; {@link #getPageIterable()} or
   * {@link #getPageIterator()} are recommended instead. In particular,
   * {@code for (PdfPage page : mydoc.getPageList())} should be replaced with
   * {@code for (PdfPage page : mydoc.getPageIterable())}.
   * </p>
   * 
   * @return A list of {@link PdfPage} instances
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #getPageIterator()
   */
  default List<? extends PdfPage> getPageList() throws PdfException {
    return IteratorUtils.toList(getPageIterator());
  }

  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #getPageList()} in 1.76.
   * </p>
   * <p>
   * This method can be memory-intensive; {@link #getPageIterable()} or
   * {@link #getPageIterator()} are recommended instead. In particular,
   * {@code for (PdfPage page : mydoc.getPages())} or
   * {@code for (PdfPage page : mydoc.getPageList())} should be replaced with
   * {@code for (PdfPage page : mydoc.getPageIterable())}.
   * </p>
   * 
   * @return A list of {@link PdfPage} instances
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @deprecated Renamed to {@link #getPageList()} in 1.76.
   * @see #getPageIterator()
   */
  @Deprecated
  default List<PdfPage> getPages() throws PdfException {
    return IteratorUtils.toList(getPageIterator());
  }

  /**
   * <p>
   * Retrieves the producer field from the given document.
   * </p>
   * 
   * @return The producer field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  String getProducer() throws PdfException;

  /**
   * <p>
   * Retrieves the subject field from the given document.
   * </p>
   * 
   * @return The subject field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  String getSubject() throws PdfException;

  /**
   * <p>
   * Retrieves the title field from the given document.
   * </p>
   * 
   * @return The title field, or <code>null</code> if unset.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  String getTitle() throws PdfException;

  /**
   * <p>
   * Retrieves the document's trailer dictionary.
   * </p>
   * 
   * @return The non-<code>null</code> trailer dictionary, possibly empty.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see PdfToken
   */
  Map<String, PdfToken> getTrailer() throws PdfException;

  /**
   * <p>
   * Removes the page at the given zero-based index from this document.
   * </p>
   * <p>
   * This affects the result of {@link #getNumberOfPages()},
   * {@link #getPage(int)} and {@link #getPages()}.
   * </p>
   * 
   * @param index
   *          The page's index in the range from <code>0</code> inclusive to
   *          {@link #getNumberOfPages()} exclusive.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void removePage(int index) throws PdfException;

  /**
   * <p>
   * Externalizes this document into the given output stream. The output stream
   * is not closed at the end of this call; it is the caller's responsibility to
   * close it.
   * </p>
   * 
   * @param outputStream
   *          An output stream.
   * @throws IOException
   *           If the operation fails on the I/O level.
   * @throws PdfException
   *           If the operation fails on the PDF level.
   * @since 1.56
   */
  void save(OutputStream outputStream) throws IOException, PdfException;

  /**
   * <p>
   * Sets the author field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> author string.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setAuthor(String author) throws PdfException;

  /**
   * <p>
   * Sets the creation date field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> creation date.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setCreationDate(Calendar date) throws PdfException;

  /**
   * <p>
   * Sets the creator field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> creator string.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setCreator(String creator) throws PdfException;

  /**
   * <p>
   * Sets the keywords field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> keywords string.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setKeywords(String keywords) throws PdfException;

  /**
   * <p>
   * Sets the language field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> language string.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setLanguage(String language) throws PdfException;

  /**
   * <p>
   * Sets the document metadata as a standalone string.
   * </p>
   * <p>
   * Note that in PDF parlance, "metadata" is a field you can get and set, just
   * like "author" is a field you can get and set.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setMetadata(String metadata) throws PdfException;

  /**
   * <p>
   * Sets the document metadata as an XMP document.
   * </p>
   * <p>
   * Note that in PDF parlance, "metadata" is a field you can get and set, just
   * like "author" is a field you can get and set.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setMetadataFromXmp(Document xmpDocument) throws PdfException;

  /**
   * <p>
   * Sets the modification date field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> modification date.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setModificationDate(Calendar date) throws PdfException;

  /**
   * <p>
   * Sets the producer field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> producer string.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setProducer(String producer) throws PdfException;

  /**
   * <p>
   * Sets the subject field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> subject string.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setSubject(String subject) throws PdfException;

  /**
   * <p>
   * Sets the title field in the given document.
   * </p>
   * 
   * @param author
   *          The non-<code>null</code> title string.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void setTitle(String title) throws PdfException;

  /**
   * <p>
   * Sets the document's trailer dictionary.
   * </p>
   * 
   * @param trailerMapping
   *          The trailer dictionary.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   * @see PdfToken
   */
  void setTrailer(Map<String, PdfToken> trailerMapping) throws PdfException;

  /**
   * <p>
   * Unsets the author field in the given document, such that
   * {@link #getAuthor()} returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetAuthor() throws PdfException;

  /**
   * <p>
   * Unsets the creation date field in the given document, such that
   * {@link #getCreationDate()} returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetCreationDate() throws PdfException;

  /**
   * <p>
   * Unsets the creator field in the given document, such that
   * {@link #getCreator()} returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetCreator() throws PdfException;

  /**
   * <p>
   * Unsets the keywords field in the given document, such that
   * {@link #getKeywords()} returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetKeywords() throws PdfException;

  /**
   * <p>
   * Unsets the language field in the given document, such that
   * {@link #getLanguage()} returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetLanguage() throws PdfException;

  /**
   * <p>
   * Unsets the document metadata, such that {@link #getMetadata()} returns
   * <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetMetadata() throws PdfException;

  /**
   * <p>
   * Unsets the modification date field in the given document, such that
   * {@link #getModificationDate()} returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetModificationDate() throws PdfException;

  /**
   * <p>
   * Unsets the producer field in the given document, such that
   * {@link #getProducer()} returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetProducer() throws PdfException;

  /**
   * <p>
   * Unsets the subject field in the given document, such that
   * {@link #getSubject()} returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetSubject() throws PdfException;

  /**
   * <p>
   * Unsets the title field in the given document, such that {@link #getTitle()}
   * returns <code>null</code>.
   * </p>
   * 
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.56
   */
  void unsetTitle() throws PdfException;

}
