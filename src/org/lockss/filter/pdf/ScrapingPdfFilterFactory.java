/*
 * $Id: ScrapingPdfFilterFactory.java,v 1.1 2012-07-12 03:58:46 thib_gc Exp $
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

public abstract class ScrapingPdfFilterFactory
    implements FilterFactory, PdfTransform<PdfDocument> {

  private static final Logger logger = Logger.getLogger(ScrapingPdfFilterFactory.class);
  
  private static final Charset STRING_CHARSET = Charset.forName(Constants.ENCODING_UTF_8);
  
  private static final Format DATE_FORMATTER = DateTimeUtil.GMT_DATE_FORMATTER;
  
  public static class BaseDocumentScrapingTransform extends OutputStreamTransform<PdfDocument> {
    
    public Charset getStringCharset() {
      return STRING_CHARSET;
    }
    
    public Format getDateFormatter() {
      return DATE_FORMATTER;
    }
    
    public BaseDocumentScrapingTransform(OutputStream os) {
      super(os);
    }
    
    public void outputAuthor(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputString(au, pdfDocument.getAuthor());
    }
    
    public void outputCreationDate(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputDate(au, pdfDocument.getCreationDate());
    }
    
    public void outputCreator(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputString(au, pdfDocument.getCreator());
    }
    
    public void outputDate(ArchivalUnit au, Calendar calendar) throws PdfException {
      try {
        if (calendar != null) {
          os.write(getDateFormatter().format(calendar).getBytes(getStringCharset()));
        }
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
    }
    
    public void outputKeywords(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputString(au, pdfDocument.getKeywords());
    }
    
    public void outputLanguage(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputString(au, pdfDocument.getLanguage());
    }
    
    public void outputMetadata(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputString(au, pdfDocument.getMetadata());
    }
    
    public void outputModificationDate(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputDate(au, pdfDocument.getModificationDate());
    }
    
    public void outputProducer(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputString(au, pdfDocument.getProducer());
    }

    public void outputString(ArchivalUnit au, String string) throws PdfException {
      try {
        if (string != null) {
          os.write(string.getBytes(getStringCharset()));
        }
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
    }
    
    public void outputSubject(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputString(au, pdfDocument.getSubject());
    }
    
    public void outputTitle(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputString(au, pdfDocument.getTitle());
    }
    
    @Override
    public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
      outputCreationDate(au, pdfDocument);
      outputModificationDate(au, pdfDocument);
      outputAuthor(au, pdfDocument);
      outputCreator(au, pdfDocument);
      outputLanguage(au, pdfDocument);
      outputProducer(au, pdfDocument);
      outputSubject(au, pdfDocument);
      outputTitle(au, pdfDocument);
      outputMetadata(au, pdfDocument);
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        outputPage(pdfPage);
      }
    }
    
    public void outputPage(PdfPage pdfPage) throws PdfException {
      OutputStreamTransform<PdfPage> scraper = getPageOutputStreamTransform(os);
    }
    
    public OutputStreamTransform<PdfPage> getPageOutputStreamTransform(OutputStream os) {
      return new BasePageScrapingTransform(os);
    }
    
  }
  
  public static class BasePageScrapingTransform extends OutputStreamTransform<PdfPage> {
    
    public BasePageScrapingTransform(OutputStream os) {
      super(os);
    }
    
    @Override
    public void transform(final ArchivalUnit au, PdfPage pdfPage) throws PdfException {
      
      PdfTokenStreamWorker worker = new PdfTokenStreamWorker() {
        @Override public void setUp() throws PdfException {}
        @Override public void operatorCallback() throws PdfException {
          // 'Tj', '\'' and '"'
          if (   PdfOpcodes.SHOW_TEXT.equals(opcode)
              || PdfOpcodes.NEXT_LINE_SHOW_TEXT.equals(opcode)
              || PdfOpcodes.SET_SPACING_NEXT_LINE_SHOW_TEXT.equals(opcode)) {
            PdfToken operand = tokens.get(index - 1);
            if (operand.isString()) {
              outputString(au, operand.getString());
            }
            logger.debug2("BasePageScrapingTransform: XScrapeText: SHOW_TEXT: invalid input");
          }
          // 'TJ'
          else if (PdfOpcodes.SHOW_TEXT_GLYPH_POSITIONING.equals(opcode)) {
            PdfToken operand = tokens.get(index - 1);
            if (operand.isArray()) {
              for (PdfToken token : operand.getArray()) {
                if (token.isString()) {
                  outputString(au, token.getString());
                }
              }
            }
            logger.debug2("BasePageScrapingTransform: XScrapeText: SHOW_TEXT_GLYPH_POSITIONING: invalid input");
          }
        }
        
      };
      
      for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
        worker.process(pdfTokenStream);
      }
      
      for (InputStream byteStream : pdfPage.getAllByteStreams()) {
        try {
          StreamUtil.copy(byteStream, os);
        }
        catch (IOException ioe) {
          throw new PdfException(ioe);
        }
      }
    }
    
    public void outputString(ArchivalUnit au, String string) throws PdfException {
      try {
        if (string != null) {
          os.write(string.getBytes(getStringCharset()));
        }
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
    }
    
    public Charset getStringCharset() {
      return STRING_CHARSET;
    }
    
    public Format getDateFormatter() {
      return DATE_FORMATTER;
    }

  }

  public OutputStreamTransform<PdfDocument> getDocumentOutputStreamTransform(OutputStream os) {
    return new BaseDocumentScrapingTransform(os);
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    PdfDocument pdfDocument = null;
    try {
      pdfDocument = DefaultPdfDocumentFactory.getInstance().parse(in);
      transform(au, pdfDocument);
      
      DeferredTempFileOutputStream os = new DeferredTempFileOutputStream(PdfUtil.getPdfMemoryLimit());
      OutputStreamTransform<PdfDocument> scraper = getDocumentOutputStreamTransform(os);
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

}
