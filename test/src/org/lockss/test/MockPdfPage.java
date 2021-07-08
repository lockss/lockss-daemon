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

import java.io.IOException;
import java.util.*;

import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.*;

@Deprecated
public class MockPdfPage extends PdfPage {

  @Deprecated
  public PDRectangle findCropBox() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDRectangle findMediaBox() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDResources findResources() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDRectangle getArtBox() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDRectangle getBleedBox() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public COSStream getContentStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDRectangle getCropBox() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public COSDictionary getDictionary() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDRectangle getMediaBox() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PdfDocument getPdfDocument() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDPage getPdPage() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public ListIterator getStreamTokenIterator() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public List getStreamTokens() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public PDRectangle getTrimBox() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setArtBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setBleedBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setContents(PDStream rectangle) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setCropBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setMediaBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public void setTrimBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  protected PDStream getContents() throws IOException {
    throw new UnsupportedOperationException();
  }

}
