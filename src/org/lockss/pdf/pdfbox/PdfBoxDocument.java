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

package org.lockss.pdf.pdfbox;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.xml.transform.TransformerException;

import org.apache.commons.collections4.iterators.*;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.xml.*;
import org.lockss.pdf.*;
import org.lockss.pdf.PdfDocument;
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
 * 
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
  private static final Logger log = Logger.getLogger(PdfBoxDocument.class);

  /**
   * <p>
   * The PDF document factory instance that created this PDf document instance.
   * </p>
   * 
   * @since 1.70
   */
  protected PdfBoxDocumentFactory pdfBoxDocumentFactory;
  
  /**
   * <p>
   * The {@link PDDocument} instance this instance represents.
   * </p>
   * 
   * @since 1.56
   */
  protected final PDDocument pdDocument;
  
  /**
   * <p>
   * Whether this instance has been closed.
   * </p>
   * 
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
   * List of weak references to auto-closeable objects to be cleaned up when the
   * document is closed.
   * </p>
   * 
   * @since 1.74.7
   */
  protected List<WeakReference<AutoCloseable>> autoCloseables;
  
  /**
   * <p>
   * Constructor.
   * </p>
   * 
   * @param pdDocument The {@link PDDocument} instance underpinning
   *          this PDF document
   * @since 1.70
   */
  public PdfBoxDocument(PdfBoxDocumentFactory pdfBoxDocumentFactory,
                        PDDocument pdDocument) {
    this.pdfBoxDocumentFactory = pdfBoxDocumentFactory;
    this.pdDocument = pdDocument;
    this.closed = false;
    this.autoCloseables = new ArrayList<WeakReference<AutoCloseable>>();

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
    if (!closed) {
      closed = true;
      try {
        log.debug2("Closing PDF document explicitly");
        pdDocument.close();
        for (WeakReference<AutoCloseable> wref : autoCloseables) {
          try {
            AutoCloseable ac = wref.get();
            if (ac != null) {
              ac.close();
            }
          }
          catch (Exception exc) {
            // ignore
          }
        }
      }
      catch (IOException ioe) {
        log.debug2("Exception closing PDF document explicitly", ioe);
        throw new PdfException(ioe);
      }
      finally {
        autoCloseables.clear();
      }
    }
  }

  @Override
  public String getAuthor() {
    return pdDocument.getDocumentInformation().getAuthor();
  }

  @Override
  public Calendar getCreationDate() {
    return pdDocument.getDocumentInformation().getCreationDate();
  }

  @Override
  public String getCreator() {
    return pdDocument.getDocumentInformation().getCreator();
  }

  @Override
  public PdfBoxDocumentFactory getDocumentFactory() {
    return pdfBoxDocumentFactory;
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
      InputStreamReader reader = new InputStreamReader(metadata.exportXMPMetadata(), StandardCharsets.UTF_8);
      return StringUtil.fromReader(reader);
    }
    catch (IOException ioe) {
      throw new PdfException("Error converting metadata stream to string", ioe);
    }
  }

  @Override
  public Document getMetadataAsXmp() throws PdfException {
    throw new UnsupportedOperationException("Operation not supported in the PDFBox implementation");
  }

  /**
   * <p>
   * Returns the document metadata as an XMPBox {@link XMPMetadata} instance.
   * </p>
   * <p>
   * Note that in PDF parlance, "metadata" is a field you can get and set, just
   * like "author" is a field you can get and set.
   * </p>
   * 
   * @return The document metadata as an XMPBox {@link XMPMetadata} instance.
   * @throws PdfException
   *           If processing fails.
   * @since 1.76
   * @see #setMetadataFromXMPMetadata(XMPMetadata)
   */
  public XMPMetadata getMetadataAsXMPMetadata() throws PdfException {
    try {
      PDMetadata pdMetadata = pdDocument.getDocumentCatalog().getMetadata();
      if (pdMetadata == null) {
        return null;
      }
      DomXmpParser xmpParser = new DomXmpParser();
      return xmpParser.parse(pdMetadata.exportXMPMetadata());
    }
    catch (XmpParsingException | IOException exc) {
      throw new PdfException("Error parsing XMP data", exc);
    }
  }

  @Override
  public Calendar getModificationDate() {
    return pdDocument.getDocumentInformation().getModificationDate();
  }

  @Override
  public int getNumberOfPages() {
    return pdDocument.getNumberOfPages();
  }

  @Override
  public PdfBoxPage getPage(int index) throws PdfException {
    return getDocumentFactory().makePage(this, pdDocument.getPage(index));
  }

  @Override // Just for the covariant default method return type
  public Iterable<PdfBoxPage> getPageIterable() throws PdfException {
    return (Iterable<PdfBoxPage>)PdfDocument.super.getPageIterable();
  }
  
  @Override
  public Iterator<PdfBoxPage> getPageIterator() throws PdfException {
    return new LazyIteratorChain<PdfBoxPage>() {
      @Override
      protected Iterator<PdfBoxPage> nextIterator(int count) { // caution: count is one-based
        try {
          return (count <= getNumberOfPages()) ? new SingletonIterator<>(getPage(count - 1)) : null;
        }
        catch (PdfException pe) {
          throw new PdfRuntimeException("PdfException in page iterator at index " + (count - 1), pe);
        }
      }
    };
  }
  
  @Override // Just for the covariant default method return type
  public List<PdfBoxPage> getPageList() throws PdfException {
    return (List<PdfBoxPage>)PdfDocument.super.getPageList();
  }
  
  public PDDocument getPdDocument() {
    return pdDocument;
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
  public Map<String, PdfToken> getTrailer() {
    /* IMPLEMENTATION NOTE
     * 
     * Contrary to our initial understanding, the trailer dictionary is not
     * just the dictionary that typically only contains the ID at the very end
     * of the document source code; it's really the real root dictionary, which
     * contains ID and Size but also Info (another dictionary) and Root, the
     * logical root dictionary (catalog). Converting it leads to converting the
     * entire object graph, which fails on COSStream. Special-case this part;
     * but this should probably lead to deprecating #setTrailer(Map) in favor
     * of redefining #getTrailer() as returning a live mapping that need not be
     * stored.
     */
    Map<String, PdfToken> ret = new LinkedHashMap<String, PdfToken>();
    COSDictionary trailer = pdDocument.getDocument().getTrailer();
    if (trailer != null) {
      for (Map.Entry<COSName, COSBase> ent : trailer.entrySet()) {
        COSBase val = ent.getValue();
        if (!(val instanceof COSObject)) {
          ret.put(ent.getKey().getName(), PdfBoxToken.convertOne(val));
        }
      }
    }
    return ret;
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
    catch (IOException ioe) {
      log.debug2("Error saving PDF document", ioe);
      throw ioe;
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
     * PDFBox 1.6.0 (probably 1.x) hard-coded ISO-8859-1, but there is no trace
     * of an encoding anymore (now that it comes out as an InputStream and goes
     * back in as a byte array), and the XML specification (2016 Part 3 Section
     * 2.1.1 and 2.1.2) doesn't really adjudicate except for saying UTF-8 is
     * widespread. Using UTF-8 now.
     */
    try {
      pdDocument.getDocumentCatalog().getMetadata().importXMPMetadata(metadata.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException ioe) {
      throw new PdfException("Error converting metadata string", ioe);
    }
  }

  @Override
  public void setMetadataFromXmp(Document xmpDocument) throws PdfException {
    throw new UnsupportedOperationException("Operation not supported in the PDFBox implementation");
  }

  /**
   * <p>
   * Sets the document metadata from an XMPBox {@link XMPMetadata } instance.
   * </p>
   * <p>
   * Note that in PDF parlance, "metadata" is a field you can get and set, just
   * like "author" is a field you can get and set.
   * </p>
   * 
   * @param xmpDocument
   *          The document metadata from an XMPBox {@link XMPMetadata }
   *          instance.
   * @throws PdfException
   *           If processing fails.
   * @since 1.76
   * @see #getMetadataAsXMPMetadata()
   */
  public void setMetadataFromXMPMetadata(XMPMetadata xmpMetadata) throws PdfException {
    try {
      UnsynchronizedByteArrayOutputStream ubaos = new UnsynchronizedByteArrayOutputStream();
      XmpSerializer xmpSerializer = new XmpSerializer();
      xmpSerializer.serialize(xmpMetadata, ubaos, true);
      pdDocument.getDocumentCatalog().getMetadata().importXMPMetadata(ubaos.toByteArray());
    }
    catch (TransformerException | IOException exc) {
      throw new PdfException("Error converting XMP data", exc);
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
    /* See important IMPLEMENTATION NOTE in #getTrailer() */
    COSDictionary trailer = pdDocument.getDocument().getTrailer();
    for (Map.Entry<String, PdfToken> ent : trailerMapping.entrySet()) {
      trailer.setItem(ent.getKey(), (COSBase)PdfBoxToken.unconvertOne(ent.getValue()));
    }
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
        log.warning("Closing PDF document implicitly in finalizer; creation context:" + openStackTrace);
        close();
      }
    }
    catch (Exception exc) {
      log.debug2("Exception closing PDF document implicitly in finalizer", exc);
      // Don't rethrow
    }
  }

}
