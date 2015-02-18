/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import java.nio.charset.Charset;
import java.text.Format;
import java.util.*;

import org.lockss.daemon.PluginException;
import org.lockss.pdf.*;
import org.lockss.pdf.PdfDocument;
import org.lockss.pdf.PdfPage;
import org.lockss.pdf.PdfUtil;
import org.lockss.plugin.*;
import org.lockss.util.CloseCallbackInputStream.DeleteFileOnCloseInputStream;
import org.lockss.util.*;

/**
 * <p>
 * A filter factory that interprets its input as a PDF document,
 * applies a transform to it, then outputs a byte stream made of the
 * concatenation of document-level metadata (title, author, creation
 * date, etc.), text strings found in pages, and image data embedded
 * in the pages.
 * </p>
 * <p>
 * The default behavior of this filter is driven by the
 * {@link BaseDocumentExtractingTransform} document transform and
 * {@link BasePageExtractingTransform} page transform, which can be
 * subclassed to alter the flow of output. The hooks for subclassing
 * are {@link #getDocumentTransform(ArchivalUnit, OutputStream)} and
 * {@link BaseDocumentExtractingTransform#getPageTransform()}.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see BaseDocumentExtractingTransform
 * @see BasePageExtractingTransform
 */
public abstract class ExtractingPdfFilterFactory
    implements FilterFactory, PdfTransform<PdfDocument> {

  /**
   * <p>
   * A scraper that extracts various fixed strings and image data from
   * a PDF document and outputs it to an output stream.
   * </p>
   * <p>
   * The flow of output is driven by
   * {@link #transform(ArchivalUnit, PdfDocument)}, which can be
   * overridden by a subclass. Whether or how each piece of data is
   * output can be controlled by subclassing the appropriate methods
   * (e.g. {@link #outputAuthor(ArchivalUnit)} for the
   * author field) or the formatting utility methods (e.g.
   * {@link #outputString(String)}). To customize the
   * per-page output, you can subclass or replace
   * {@link BasePageExtractingTransform} via the
   * {@link #getPageTransform()} hook.
   * </p>
   * <p>
   * This class is not thread-safe (it is intended to process one
   * document at a time).
   * </p>
   * @author Thib Guicherd-Callin
   * @since 1.56
   */
  public static class BaseDocumentExtractingTransform
      implements PdfTransform<PdfDocument> {
    
    protected ArchivalUnit au;
    
    protected OutputStream os;
    
    protected PdfDocument pdfDocument;
    
    public BaseDocumentExtractingTransform(OutputStream os) {
      this.os = os;
    }
    
    public Format getDateFormatter() {
      return DATE_FORMATTER;
    }
    
    /**
     * <p>
     * Makes a page transform for use on every page of the document being processed.
     * </p>
     * @return A PDF page transform.
     */
    public PdfTransform<PdfPage> getPageTransform() {
      return new BasePageExtractingTransform(os);
    }
    
    public Charset getStringCharset() {
      return STRING_CHARSET;
    }
    
    public void outputAuthor() throws PdfException {
      outputString(pdfDocument.getAuthor());
    }
    
    public void outputCreationDate() throws PdfException {
      outputDate(pdfDocument.getCreationDate());
    }
    
    public void outputCreator() throws PdfException {
      outputString(pdfDocument.getCreator());
    }
    
    public void outputDate(Calendar calendar)
        throws PdfException {
      try {
        if (calendar != null) {
          os.write(getDateFormatter().format(calendar).getBytes(getStringCharset()));
        }
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
    }
    
    public void outputKeywords() throws PdfException {
      outputString(pdfDocument.getKeywords());
    }
    
    public void outputLanguage() throws PdfException {
      outputString(pdfDocument.getLanguage());
    }
    
    /**
     * <p>
     * Note: in PDF parlance, "metadata" is a document-level field the
     * same way that "author" and "creation date" are document-level
     * fields.
     * </p>
     * @param au An archival unit.
     * @param pdfDocument A PDF document.
     * @throws PdfException If PDF processing fails.
     */
    public void outputMetadata() throws PdfException {
      outputString(pdfDocument.getMetadata());
    }
    
    public void outputModificationDate() throws PdfException {
      outputDate(pdfDocument.getModificationDate());
    }

    public void outputPage(PdfPage pdfPage) throws PdfException {
      PdfTransform<PdfPage> scraper = getPageTransform();
      scraper.transform(au, pdfPage);
    }
    
    public void outputProducer() throws PdfException {
      outputString(pdfDocument.getProducer());
    }
    
    public void outputString(String string) throws PdfException {
      try {
        if (string != null) {
          os.write(string.getBytes(getStringCharset()));
        }
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
    }
    
    public void outputSubject() throws PdfException {
      outputString(pdfDocument.getSubject());
    }
    
    public void outputTitle() throws PdfException {
      outputString(pdfDocument.getTitle());
    }
    
    /**
     * <p>
     * Calls:
     * </p>
     * <ul>
     * <li>{@link #outputCreationDate()}</li>
     * <li>{@link #outputModificationDate()}</li>
     * <li>{@link #outputAuthor()}</li>
     * <li>{@link #outputCreator()}</li>
     * <li>{@link #outputLanguage()}</li>
     * <li>{@link #outputProducer()}</li>
     * <li>{@link #outputSubject()}</li>
     * <li>{@link #outputTitle()}</li>
     * <li>{@link #outputMetadata()}</li>
     * </ul>
     * 
     * @throws PdfException
     *           if any processing error occurs.
     * @since 1.67
     */
    public void outputDocumentInformation() throws PdfException {
      outputCreationDate();
      outputModificationDate();
      outputAuthor();
      outputCreator();
      outputLanguage();
      outputProducer();
      outputSubject();
      outputTitle();
      outputMetadata();
    }
    
    /**
     * <p>
     * Calls {@link #outputDocumentInformation()}, and then for each page,
     * {@link #outputPage(PdfPage)}.
     * </p>
     * 
     * @param pdfDocument
     *          A PDF document.
     * @throws PdfException
     *           if any processing error occurs.
     * @since 1.67
     */
    public void outputDocument(PdfDocument pdfDocument) throws PdfException {
      outputDocumentInformation();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        outputPage(pdfPage);
      }
    }

    @Override
    public void transform(ArchivalUnit au,
                          PdfDocument pdfDocument)
        throws PdfException {
      this.au = au;
      this.pdfDocument = pdfDocument;
      outputDocument(pdfDocument);
    }
  }
  
  /**
   * <p>
   * A scraper that extracts various strings and image data from
   * a PDF page and outputs it to an output stream.
   * </p>
   * <p>
   * The flow of output is driven by
   * {@link #transform(ArchivalUnit, PdfDocument)}, which can be
   * overridden by a subclass.
   * </p>
   * <p>
   * This class is not thread-safe (it is intended to process one
   * page at a time).
   * </p>
   * @author Thib Guicherd-Callin
   * @since 1.56
   */
  public static class BasePageExtractingTransform
      implements PdfTransform<PdfPage> {
    
    protected ArchivalUnit au;
    
    protected OutputStream os;
    
    protected PdfPage pdfPage;
    
    public BasePageExtractingTransform(OutputStream os) {
      this.os = os;
    }
    
    public Format getDateFormatter() {
      return DATE_FORMATTER;
    }
    
    public Charset getStringCharset() {
      return STRING_CHARSET;
    }
    
    public void outputByteStream(InputStream is) throws PdfException {
      try {
        StreamUtil.copy(is, os);
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
    }
    
    public void outputString(String string) throws PdfException {
      try {
        if (string != null) {
          os.write(string.getBytes(getStringCharset()));
        }
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
    }
    
    @Override
    public void transform(ArchivalUnit au,
                          PdfPage pdfPage)
        throws PdfException {
      this.au = au;
      this.pdfPage = pdfPage;
      
      // Use a worker to extract all PDF strings
      PdfTokenStreamWorker worker = new PdfTokenStreamWorker() {
        @Override public void operatorCallback() throws PdfException {
          // 'Tj', '\'' and '"'
          if (isShowText() || isNextLineShowText() || isSetSpacingNextLineShowText()) {
            outputString(getTokens().get(getIndex() - 1).getString());
          }
          // 'TJ'
          else if (isShowTextGlyphPositioning()) {
            for (PdfToken token : getTokens().get(getIndex() - 1).getArray()) {
              if (token.isString()) {
                outputString(token.getString());
              }
            }
          }
        }
      };
      
      // Apply the worker to every token stream in the page
      for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
        worker.process(pdfTokenStream);
      }
      
      // Now output all byte streams in the page wholesale
      for (InputStream byteStream : pdfPage.getAllByteStreams()) {
        outputByteStream(byteStream);
      }
    }

  }
  
  /**
   * <p>
   * A date formatter for use by this class.
   * </p>
   * @since 1.56
   */
  private static final Format DATE_FORMATTER = DateTimeUtil.GMT_DATE_FORMATTER;
  
  /**
   * <p>
   * The charset used for strings by this class.
   * </p>
   * @since 1.56
   */
  private static final Charset STRING_CHARSET = Charset.forName(Constants.ENCODING_UTF_8);

  /**
   * <p>
   * This instance's PDF document factory.
   * </p>
   * @since 1.56
   */
  protected PdfDocumentFactory pdfDocumentFactory;
  
  /**
   * <p>
   * Makes an instance using {@link DefaultPdfDocumentFactory}.
   * </p>
   * @since 1.56
   * @see DefaultPdfDocumentFactory
   */
  public ExtractingPdfFilterFactory() {
    this(DefaultPdfDocumentFactory.getInstance());
  }
  
  /**
   * <p>
   * Makes an instance using the given PDF document factory.
   * </p>
   * @param pdfDocumentFactory A PDF document factory.
   * @since 1.56
   */
  public ExtractingPdfFilterFactory(PdfDocumentFactory pdfDocumentFactory) {
    this.pdfDocumentFactory = pdfDocumentFactory;
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    PdfDocument pdfDocument = null;
    try {
      pdfDocument = pdfDocumentFactory.parse(in);
      transform(au, pdfDocument);
      
      DeferredTempFileOutputStream os = new DeferredTempFileOutputStream(PdfUtil.getPdfMemoryLimit());
      PdfTransform<PdfDocument> scraper = getDocumentTransform(au, os);
      scraper.transform(au, pdfDocument);
      os.close();
      
      if (os.isInMemory()) {
        return new ByteArrayInputStream(os.getData());
      }
      else {
        return new BufferedInputStream(new DeleteFileOnCloseInputStream(os.getFile()));
      }
    }
    catch (IOException ioe) {
      throw new PluginException(ioe);
    }
    catch (PdfException pdfe) {
      throw new PluginException(pdfe);
    }
    finally {
      PdfUtil.safeClose(pdfDocument);
    }
  }
  
  public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au,
                                                        OutputStream os) {
    return new BaseDocumentExtractingTransform(os);
  }

}
