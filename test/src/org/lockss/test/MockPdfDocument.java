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

package org.lockss.test;

import java.io.*;
import java.util.*;

import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.*;

@Deprecated
public class MockPdfDocument extends PdfDocument {

  @Deprecated
  public MockPdfDocument() {
    super();
  }

  @Deprecated
  public boolean close() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public String getAuthor() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public COSDocument getCosDocument() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public Calendar getCreationDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public String getCreator() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public String getKeywords() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public String getMetadataAsString() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public Calendar getModificationDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PdfPage getPage(int index) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public ListIterator getPageIterator() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDDocument getPdDocument() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDFParser getPdfParser() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public String getProducer() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public String getSubject() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public String getTitle() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public COSDictionary getTrailer() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDStream makePdStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeAuthor() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeCreationDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeCreator() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeKeywords() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeModificationDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeProducer() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeSubject() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void removeTitle() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void save(OutputStream outputStream) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setAuthor(String author) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setCreationDate(Calendar date) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setCreator(String creator) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setKeywords(String keywords) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setMetadata(String metadataAsString) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setModificationDate(Calendar date) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setProducer(String producer) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setSubject(String subject) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setTitle(String title) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  protected PDDocumentCatalog getDocumentCatalog() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  protected PDDocumentInformation getDocumentInformation() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  protected PDMetadata getMetadata() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  protected List getPdPages() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  protected void setMetadata(PDMetadata metadata) throws IOException {
    throw new UnsupportedOperationException();
  }

}
