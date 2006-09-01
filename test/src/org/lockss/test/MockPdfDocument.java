/*
 * $Id: MockPdfDocument.java,v 1.1 2006-09-01 07:32:52 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.*;
import java.util.*;

import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.*;

public class MockPdfDocument extends PdfDocument {

  public MockPdfDocument() {
    super();
  }

  public void close() throws IOException {
    throw new UnsupportedOperationException();
  }

  public String getAuthor() throws IOException {
    throw new UnsupportedOperationException();
  }

  public COSDocument getCosDocument() throws IOException {
    throw new UnsupportedOperationException();
  }

  public Calendar getCreationDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  public String getCreator() throws IOException {
    throw new UnsupportedOperationException();
  }

  public String getKeywords() throws IOException {
    throw new UnsupportedOperationException();
  }

  public String getMetadataAsString() throws IOException {
    throw new UnsupportedOperationException();
  }

  public Calendar getModificationDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  public PdfPage getPage(int index) throws IOException {
    throw new UnsupportedOperationException();
  }

  public ListIterator getPageIterator() throws IOException {
    throw new UnsupportedOperationException();
  }

  public PDDocument getPdDocument() throws IOException {
    throw new UnsupportedOperationException();
  }

  public PDFParser getPdfParser() {
    throw new UnsupportedOperationException();
  }

  public String getProducer() throws IOException {
    throw new UnsupportedOperationException();
  }

  public String getSubject() throws IOException {
    throw new UnsupportedOperationException();
  }

  public String getTitle() throws IOException {
    throw new UnsupportedOperationException();
  }

  public COSDictionary getTrailer() throws IOException {
    throw new UnsupportedOperationException();
  }

  public PDStream makePdStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void removeAuthor() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void removeCreationDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void removeCreator() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void removeKeywords() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void removeModificationDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void removeProducer() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void removeSubject() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void removeTitle() throws IOException {
    throw new UnsupportedOperationException();
  }

  public void save(OutputStream outputStream) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setAuthor(String author) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setCreationDate(Calendar date) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setCreator(String creator) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setKeywords(String keywords) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setMetadata(String metadataAsString) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setModificationDate(Calendar date) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setProducer(String producer) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setSubject(String subject) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void setTitle(String title) throws IOException {
    throw new UnsupportedOperationException();
  }

  protected List getAllPages() throws IOException {
    throw new UnsupportedOperationException();
  }

  protected PDDocumentCatalog getDocumentCatalog() throws IOException {
    throw new UnsupportedOperationException();
  }

  protected PDDocumentInformation getDocumentInformation() throws IOException {
    throw new UnsupportedOperationException();
  }

  protected PDMetadata getMetadata() throws IOException {
    throw new UnsupportedOperationException();
  }

  protected void setMetadata(PDMetadata metadata) throws IOException {
    throw new UnsupportedOperationException();
  }

}
