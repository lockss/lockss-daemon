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

package org.lockss.util;

import java.io.*;
import java.util.*;

import org.pdfbox.cos.*;
import org.pdfbox.exceptions.*;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.*;
import org.pdfbox.pdmodel.fdf.FDFDocument;

/**
 * <p>Convenience class to provide easy access to the internals of
 * a PDF document.</p>
 * <p>The PDFBox API provides several levels of access to a PDF
 * document via different class hierarchies ({@link COSDocument},
 * {@link PDDocument}, {@link FDFDocument}). The only unified view of
 * a PDF document is through {@link PDFParser}, but unintuitively
 * it is a parser more than a document object. This class exposes an
 * API more related to the PDF document under the parser.</p>
 * @author Thib Guicherd-Callin
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class PdfDocument {

  /**
   * <p>The underlying PDF parser.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private PDFParser pdfParser;

  /**
   * <p>Builds a new PDF document (actually a new PDF parser).</p>
   * <p><em>You must call {@link #close()} to release the expensive
   * resources associated with this object, when it is no longer
   * needed but before it is finalized by the runtime system.</em></p>
   * @param inputStream The input stream that contains the PDF document.
   * @throws IOException if any processing error occurs.
   * @see PDFParser#PDFParser(InputStream)
   * @see PDFParser#parse
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PdfDocument(InputStream inputStream) throws IOException {
    this.pdfParser = new PDFParser(inputStream);
    parse();
  }

  @Deprecated
  protected PdfDocument() { }

  /**
   * <p>Closes the underlying {@link COSDocument} instance
   * and releases expensive memory resources held by this object.</p>
   * <p>This method does not throw {@link IOException} in case of
   * failure; use the return value to determine success. However,
   * calling this method does cause this instance to become
   * unusable; any subsequent method calls will likely yield a
   * {@link NullPointerException}.</p>
   * @return True if closing the PDF document succeeded, false
   *         otherwise.
   * @see PDDocument#close
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public boolean close() {
    try {
      getPdDocument().close();
      return true;
    }
    catch (IOException ioe) {
      logger.error("Error while closing a PDF document", ioe);
      return false;
    }
    finally {
      pdfParser = null;
    }
  }

  /**
   * <p>Gets the author from the document information.</p>
   * @return The author of the document (null if not set).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#getAuthor
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public String getAuthor() throws IOException {
    return getDocumentInformation().getAuthor();
  }

  /**
   * <p>Provides access to the underlying {@link COSDocument}
   * instance; <em>use with care.</em></p>
   * @return The underlying {@link COSDocument} instance, pulled from
   *         the underlying {@link PDFParser} instance.
   * @throws IOException if any processing error occurs.
   * @see PDFParser#getDocument
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public COSDocument getCosDocument() throws IOException {
    return getPdfParser().getDocument();
  }

  /**
   * <p>Gets the creation date from the document information.</p>
   * @return The creation date of the document (null if not set).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#getCreationDate
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public Calendar getCreationDate() throws IOException {
    return getDocumentInformation().getCreationDate();
  }

  /**
   * <p>Gets the creator from the document information.</p>
   * @return The creator of the document (null if not set).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#getCreator
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public String getCreator() throws IOException {
    return getDocumentInformation().getCreator();
  }

  /**
   * <p>Gets the keywords from the document information.</p>
   * @return The keywords of the document (null if not set).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#getKeywords
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public String getKeywords() throws IOException {
    return getDocumentInformation().getKeywords();
  }

  /**
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public String getMetadataAsString() throws IOException {
    PDMetadata metadata = getMetadata();
    return metadata == null ? null : metadata.getInputStreamAsString();
  }

  /**
   * <p>Gets the modification date from the document information.</p>
   * @return The modification date of the document (null if not set).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#getModificationDate
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public Calendar getModificationDate() throws IOException {
    return getDocumentInformation().getModificationDate();
  }

  /**
   * <p>Determines the number of pages of this PDF document.</p>
   * @return The total number of pages.
   * @throws IOException if any processing error occurs.
   * @see PDDocument#getNumberOfPages
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public int getNumberOfPages() throws IOException {
    return getPdDocument().getNumberOfPages();
  }

  /**
   * @param index
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PdfPage getPage(int index) throws IOException {
    return new PdfPage(this, getPdPage(index));
  }

  /**
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ListIterator /* of PdfPage */ getPageIterator() throws IOException {
    List pdfPages = new ArrayList();
    for (Iterator iter = getPdPageIterator() ; iter.hasNext() ; ) {
      pdfPages.add(new PdfPage(this, (PDPage)iter.next()));
    }
    return pdfPages.listIterator();
  }

  /**
   * <p>Provides access to the underlying {@link PDDocument}
   * instance; <em>use with care.</em></p>
   * @return The underlying {@link PDDocument} instance, pulled from
   *         the underlying {@link PDFParser} instance.
   * @throws IOException if any processing error occurs.
   * @see PDFParser#getPDDocument
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDDocument getPdDocument() throws IOException {
    return getPdfParser().getPDDocument();
  }

  /**
   * <p>Provides access to the underlying {@link PDFParser}
   * instance; <em>use with care.</em></p>
   * @return The underlying {@link PDFParser} instance.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDFParser getPdfParser() {
    return pdfParser;
  }

  /**
   * @param index
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDPage getPdPage(int index) throws IOException {
    return (PDPage)getPdPages().get(index);
  }

  /**
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ListIterator /* of PDPage */ getPdPageIterator() throws IOException {
    return getPdPages().listIterator();
  }

  /**
   * <p>Gets the producer from the document information.</p>
   * @return The producer of the document (null if not set).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#getProducer
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public String getProducer() throws IOException {
    return getDocumentInformation().getProducer();
  }

  /**
   * <p>Gets the subject from the document information.</p>
   * @return The subject of the document (null if not set).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#getSubject
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public String getSubject() throws IOException {
    return getDocumentInformation().getSubject();
  }

  /**
   * <p>Gets the title from the document information.</p>
   * @return The title of the document (null if not set).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#getTitle
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public String getTitle() throws IOException {
    return getDocumentInformation().getTitle();
  }

  /**
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public COSDictionary getTrailer() throws IOException {
    return getCosDocument().getTrailer();
  }

  /**
   * <p>Instantiates a new {@link PDStream} instance based on this PDF
   * document.</p>
   * @return A newly instantiated {@link PDStream} instance.
   * @throws IOException if any processing error occurs.
   * @see PDStream#PDStream(PDDocument)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDStream makePdStream() throws IOException {
    return new PDStream(getPdDocument());
  }

  /**
   * <p>Unsets the author from the document information.</p>
   * @throws IOException if any processing error occurs.
   * @see #setAuthor
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removeAuthor() throws IOException {
    setAuthor(null);
  }

  /**
   * <p>Unsets the creation date from the document information.</p>
   * @throws IOException if any processing error occurs.
   * @see #setCreationDate
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removeCreationDate() throws IOException {
    setCreationDate(null);
  }

  /**
   * <p>Unsets the creator from the document information.</p>
   * @throws IOException if any processing error occurs.
   * @see #setCreator
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removeCreator() throws IOException {
    setCreator(null);
  }

  /**
   * <p>Unsets the keywords from the document information.</p>
   * @throws IOException if any processing error occurs.
   * @see #setKeywords
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removeKeywords() throws IOException {
    setKeywords(null);
  }

  /**
   * <p>Unsets the modification date from the document information.</p>
   * @throws IOException if any processing error occurs.
   * @see #setModificationDate
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removeModificationDate() throws IOException {
    setModificationDate(null);
  }

  /**
   * @param index
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removePage(int index) throws IOException {
    getPdDocument().removePage(index);
  }

  /**
   * @param pdfPage
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removePage(PdfPage pdfPage) throws IOException {
    removePage(pdfPage.getPdPage());
  }

  /**
   * <p>Unsets the producer from the document information.</p>
   * @throws IOException if any processing error occurs.
   * @see #setProducer
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removeProducer() throws IOException {
    setProducer(null);
  }

  /**
   * <p>Unsets the subject from the document information.</p>
   * @throws IOException if any processing error occurs.
   * @see #setSubject
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removeSubject() throws IOException {
    setSubject(null);
  }

  /**
   * <p>Unsets the title from the document information.</p>
   * @throws IOException if any processing error occurs.
   * @see #setTitle
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void removeTitle() throws IOException {
    setTitle(null);
  }

  /**
   * <p>This will save the underlying {@link PDDocument} instance to
   * an output stream.</p>
   * @param outputStream An output stream into which this document
   *                     will be saved.
   * @throws IOException if any processing error occurs.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void save(OutputStream outputStream) throws IOException {
    try {
      getPdDocument().save(outputStream);
    }
    catch (COSVisitorException cve) {
      IOException ioe = new IOException();
      ioe.initCause(cve);
      throw ioe;
    }
  }

  /**
   * <p>Sets the author in the document information.</p>
   * @param author The new author of the document (null to unset).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#setAuthor
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setAuthor(String author) throws IOException {
    getDocumentInformation().setAuthor(author);
  }

  /**
   * <p>Sets the creation date in the document information.</p>
   * @param date The new creation date of the document (null to unset).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#setCreationDate
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setCreationDate(Calendar date) throws IOException {
    getDocumentInformation().setCreationDate(date);
  }

  /**
   * <p>Sets the creator in the document information.</p>
   * @param creator The new creator of the document (null to unset).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#setCreator
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setCreator(String creator) throws IOException {
    getDocumentInformation().setCreator(creator);
  }

  /**
   * <p>Sets the keywords in the document information.</p>
   * @param keywords The new keywords of the document (null to unset).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#setKeywords
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setKeywords(String keywords) throws IOException {
    getDocumentInformation().setKeywords(keywords);
  }

  /**
   * @param metadataAsString
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setMetadata(String metadataAsString) throws IOException {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(metadataAsString.getBytes());
    PDMetadata pdMetadata = new PDMetadata(getPdDocument(), inputStream, false);
    setMetadata(pdMetadata);
  }

  /**
   * <p>Sets the modification date in the document information.</p>
   * @param date The new modification date of the document (null to unset).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#setModificationDate
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setModificationDate(Calendar date) throws IOException {
    getDocumentInformation().setModificationDate(date);
  }

  /**
   * <p>Sets the producer in the document information.</p>
   * @param producer The new producer of the document (null to unset).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#setProducer
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setProducer(String producer) throws IOException {
    getDocumentInformation().setProducer(producer);
  }

  /**
   * <p>Sets the subject in the document information.</p>
   * @param subject The new subject of the document (null to unset).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#setSubject
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setSubject(String subject) throws IOException {
    getDocumentInformation().setSubject(subject);
  }

  /**
   * <p>Sets the title in the document information.</p>
   * @param title The new title of the document (null to unset).
   * @throws IOException if any processing error occurs.
   * @see PDDocumentInformation#setTitle
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setTitle(String title) throws IOException {
    getDocumentInformation().setTitle(title);
  }

  /**
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected PDDocumentCatalog getDocumentCatalog() throws IOException {
    return getPdDocument().getDocumentCatalog();
  }

  /**
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected PDDocumentInformation getDocumentInformation() throws IOException {
    return getPdDocument().getDocumentInformation();
  }

  /**
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected PDMetadata getMetadata() throws IOException {
    return getDocumentCatalog().getMetadata();
  }

  /**
   * @return
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected List getPdPages() throws IOException {
    return getDocumentCatalog().getAllPages();
  }

  /**
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected void parse() throws IOException {
    // Parse the document before using it
    getPdfParser().parse();

    // Trivial decryption if encrypted without a password
    if (getPdDocument().isEncrypted()) {
      try {
        getPdDocument().decrypt("");
      }
      catch (CryptographyException ce) {
        IOException ioe = new IOException();
        ioe.initCause(ce);
        throw ioe;
      }
      catch (InvalidPasswordException ipe) {
        IOException ioe = new IOException();
        ioe.initCause(ipe);
        throw ioe;
      }
    }
  }

  /**
   * @param pdPage
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected void removePage(PDPage pdPage) throws IOException {
    getPdDocument().removePage(pdPage);
  }

  /**
   * @param metadata
   * @throws IOException
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected void setMetadata(PDMetadata metadata) throws IOException {
    getDocumentCatalog().setMetadata(metadata);
  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(PdfDocument.class);

  /**
   * <p>Closes the underlying {@link COSDocument} instance
   * and releases expensive memory resources held by the given
   * object (which can be null).</p>
   * <p>This method does not throw {@link IOException} in case of
   * failure; use the return value to determine success. However,
   * calling this method does cause the given instance to become
   * unusable; any subsequent method calls will likely yield a
   * {@link NullPointerException}.</p>
   * @param pdfDocument a PDF document instance; can be null.
   * @return True if the PDF document is null or if closing it
   *         succeeded, false otherwise.
   * @see #close()
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static boolean close(PdfDocument pdfDocument) {
    if (pdfDocument == null) {
      return true;
    }
    else {
      return pdfDocument.close();
    }
  }

}
