/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.pdf.pdfbox;

import java.io.*;
import java.util.*;

import javax.xml.transform.TransformerException;

import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.lockss.pdf.*;
import org.lockss.pdf.PdfDocument;
import org.lockss.pdf.PdfPage;
import org.lockss.util.*;
import org.w3c.dom.Document;

/**
 * <p>
 * A {@link PdfDocument} implementation based on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as an adapter for the {@link PDDocument} class.
 * </p>
 * <p>
 * The logger in this class is used to record a few messages at
 * {@link Logger#LEVEL_WARNING} level if certain assumptions about the
 * state of this PDF document are violated. Other logging messages are
 * at {@link Logger#LEVEL_DEBUG2} or finer.
 * </p>
 * <ul>
 * <li>The finalizer ({@link Object#finalize()}) records instances
 * that are garbage-collected without having been explicitly closed,
 * and provides a stack trace context of when the document was
 * created.</li>
 * <li>The ISO-8859-1 encoding is guaranteed to exist in Java, but
 * should an {@link UnsupportedEncodingException} arise, a message is
 * recorded at {@link Logger#LEVEL_WARNING} level.</li>
 * </ul>
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public class PdfBoxDocument implements PdfDocument {

  /**
   * <p>
   * Logger for use by this class.
   * </p>
   * @since 1.56
   */
  private static Logger logger = Logger.getLogger(PdfBoxDocument.class);

  /**
   * <p>
   * The {@link PDDocument} instance this instance represents.
   * </p>
   * @since 1.56
   */
  protected final PDDocument pdDocument;
  
  /**
   * <p>
   * Whether this instance has been closed.
   * </p>
   * @since 1.56
   */
  private volatile boolean closed;

  /**
   * <p>
   * String representation of the context when the document was
   * created.
   * </p>
   */
  private String openStackTrace;
  
  /**
   * <p>
   * This constructor is accessible to classes in this package and
   * subclasses.
   * </p>
   * @param pdDocument The {@link PDDocument} instance underpinning
   *          this PDF document.
   * @since 1.56
   */
  protected PdfBoxDocument(PDDocument pdDocument) {
    this.pdDocument = pdDocument;
    this.closed = false;

    StringBuilder sb = new StringBuilder();
    for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
      if (sb.length() == 0) { continue; } // That's getStackTrace()
      sb.append('\n'); // Intentional, see finalize()
      sb.append(e.toString());
    }
    this.openStackTrace = sb.toString();
  }

  @Override
  public void close() throws PdfException {
    try {
      logger.debug2("Closing PDF document explicitly");
      closed = true;
      pdDocument.close();
    }
    catch (IOException ioe) {
      logger.debug2("Exception closing PDF document explicitly", ioe);
      throw new PdfException(ioe);
    }
  }

  @Override
  public String getAuthor() {
    return pdDocument.getDocumentInformation().getAuthor();
  }

  @Override
  public Calendar getCreationDate() throws PdfException {
    try {
      return pdDocument.getDocumentInformation().getCreationDate();
    }
    catch (IOException ioe) {
      throw new PdfException("Error processing the creation date", ioe);
    }
  }

  @Override
  public String getCreator() {
    return pdDocument.getDocumentInformation().getCreator();
  }

  @Override
  public String getKeywords() {
    return pdDocument.getDocumentInformation().getKeywords();
  }

  @Override
  public String getLanguage() {
    return pdDocument.getDocumentCatalog().getLanguage();
  }

  @Override
  public String getMetadata() throws PdfException {
    try {
      PDMetadata metadata = pdDocument.getDocumentCatalog().getMetadata();
      if (metadata == null) {
        return null;
      }
      return metadata.getInputStreamAsString();
    }
    catch (IOException ioe) {
      throw new PdfException("Error converting metadata stream to string", ioe);
    }
  }

  @Override
  public Document getMetadataAsXmp() throws PdfException {
    try {
      PDMetadata metadata = pdDocument.getDocumentCatalog().getMetadata();
      if (metadata == null) {
        return null;
      }
      return metadata.exportXMPMetadata().getXMPDocument();
    }
    catch (IOException ioe) {
      throw new PdfException("Error parsing XMP data", ioe);
    }
  }

  @Override
  public Calendar getModificationDate() throws PdfException {
    try {
      return pdDocument.getDocumentInformation().getModificationDate();
    }
    catch (IOException ioe) {
      throw new PdfException("Error processing the modification date", ioe);
    }
  }

  @Override
  public int getNumberOfPages() {
    return pdDocument.getNumberOfPages();
  }

  @Override
  public PdfPage getPage(int index) {
    /*
     * IMPLEMENTATION NOTE
     * 
     * The documentation of getAllPages() (PDFBox 1.6.0:
     * PDDocumentCatalog line 205) states that all the elements in the
     * returned list are of type PDPage.
     */
    return new PdfBoxPage(this, (PDPage)pdDocument.getDocumentCatalog().getAllPages().get(index));
  }

  @Override
  public List<PdfPage> getPages() {
    /*
     * IMPLEMENTATION NOTE
     * 
     * The documentation of getAllPages() (PDFBox 1.6.0:
     * PDDocumentCatalog line 205) states that all the elements in the
     * returned list are of type PDPage.
     */
    List<PdfPage> ret = new ArrayList<PdfPage>();
    for (Object obj : pdDocument.getDocumentCatalog().getAllPages()) {
      ret.add(new PdfBoxPage(this, (PDPage)obj));
    }
    return ret;
  }

  @Override
  public String getProducer() {
    return pdDocument.getDocumentInformation().getProducer();
  }

  @Override
  public String getSubject() {
    return pdDocument.getDocumentInformation().getSubject();
  }

  @Override
  public String getTitle() {
    return pdDocument.getDocumentInformation().getTitle();
  }

  @Override
  public PdfTokenFactory getTokenFactory() {
    return PdfBoxTokens.getAdapterInstance();
  }
  
  @Override
  public Map<String, PdfToken> getTrailer() {
    COSDictionary trailer = pdDocument.getDocument().getTrailer();
    if (trailer == null) {
      trailer = new COSDictionary();
    }
    return PdfBoxTokens.getDictionary(trailer);
  }

  @Override
  public void removePage(int index) {
    pdDocument.removePage(index);
  }

  @Override
  public void save(OutputStream outputStream) throws IOException, PdfException {
    if (closed) {
      throw new PdfException("PDF document already closed");
    }
    try {
      pdDocument.save(outputStream);
    }
    catch (COSVisitorException cve) {
      logger.debug2("Error saving PDF document", cve);
      throw new PdfException("Error saving PDF document", cve);
    }
  }

  @Override
  public void setAuthor(String author) {
    pdDocument.getDocumentInformation().setAuthor(author);
  }
  
  @Override
  public void setCreationDate(Calendar date) {
    pdDocument.getDocumentInformation().setCreationDate(date);
  }

  @Override
  public void setCreator(String creator) {
    pdDocument.getDocumentInformation().setCreator(creator);
  }
  
  @Override
  public void setKeywords(String keywords) {
    pdDocument.getDocumentInformation().setKeywords(keywords);
  }

  @Override
  public void setLanguage(String language) {
    pdDocument.getDocumentCatalog().setLanguage(language);
  }

  @Override
  public void setMetadata(String metadata) throws PdfException {
    /*
     * IMPLEMENTATION NOTE
     * 
     * getInputStreamAsString() (PDFBox 1.6.0: PDStream line 496) uses
     * the encoding ISO-8859-1, so we need to encode the string
     * accordingly. If it defined a constant, we could use it, but it
     * hard-codes the string "ISO-8859-1".
     */
    try {
      InputStream is = new ByteArrayInputStream(metadata.getBytes(Constants.ENCODING_ISO_8859_1));
      pdDocument.getDocumentCatalog().setMetadata(new PDMetadata(pdDocument, is, false));
    }
    catch (UnsupportedEncodingException uee) {
      // Shouldn't happen, ISO-8859-1 is guaranteed to exist
      logger.warning("Unexpected unsupported encoding exception: " + Constants.ENCODING_ISO_8859_1, uee);
      throw new PdfException("Unexpected error converting metadata string to stream", uee);
    }
    catch (IOException ioe) {
      throw new PdfException("Error converting metadata string to stream", ioe);
    }
  }

  @Override
  public void setMetadataFromXmp(Document xmpDocument) throws PdfException {
    try {
      pdDocument.getDocumentCatalog().getMetadata().importXMPMetadata(new XMPMetadata(xmpDocument));
    }
    catch (IOException ioe) {
      throw new PdfException("Error converting XMP document to metadata", ioe);
    }
    catch (TransformerException te) {
      throw new PdfException("Error converting XMP document to metadata", te);
    }
  }

  @Override
  public void setModificationDate(Calendar date) {
    pdDocument.getDocumentInformation().setModificationDate(date);
  }

  @Override
  public void setProducer(String producer) {
    pdDocument.getDocumentInformation().setProducer(producer);
  }

  @Override
  public void setSubject(String subject) {
    pdDocument.getDocumentInformation().setSubject(subject);
  }

  @Override
  public void setTitle(String title) {
    pdDocument.getDocumentInformation().setTitle(title);
  }

  @Override
  public void setTrailer(Map<String, PdfToken> trailerMapping) {
    pdDocument.getDocument().setTrailer(PdfBoxTokens.asCOSDictionary(trailerMapping));
  }

  @Override
  public void unsetAuthor() {
    pdDocument.getDocumentInformation().setAuthor(null);
  }

  @Override
  public void unsetCreationDate() {
    pdDocument.getDocumentInformation().setCreationDate(null);
  }

  @Override
  public void unsetCreator() {
    pdDocument.getDocumentInformation().setCreator(null);
  }

  @Override
  public void unsetKeywords() {
    pdDocument.getDocumentInformation().setKeywords(null);
  }

  @Override
  public void unsetLanguage() {
    pdDocument.getDocumentCatalog().setLanguage(null);
  }

  @Override
  public void unsetMetadata() {
    pdDocument.getDocumentCatalog().setMetadata(null);
  }

  @Override
  public void unsetModificationDate() {
    pdDocument.getDocumentInformation().setModificationDate(null);
  }
  
  @Override
  public void unsetProducer() {
    pdDocument.getDocumentInformation().setProducer(null);
  }

  @Override
  public void unsetSubject() {
    pdDocument.getDocumentInformation().setSubject(null);
  }
  
  @Override
  public void unsetTitle() {
    pdDocument.getDocumentInformation().setTitle(null);
  }
  
  @Override
  protected void finalize() throws Throwable {
    try {
      if (!closed) {
        // Starts with newline, doesn't end with one, see constructor
        logger.warning("Closing PDF document implicitly in finalizer; creation context:" + openStackTrace);
        pdDocument.close();
      }
    }
    catch (Exception exc) {
      logger.debug2("Exception closing PDF document implicitly in finalizer", exc);
      // Don't rethrow
    }
  }

}
