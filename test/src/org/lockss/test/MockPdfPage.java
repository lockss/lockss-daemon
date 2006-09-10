/*
 * $Id: MockPdfPage.java,v 1.2 2006-09-10 07:50:51 thib_gc Exp $
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

import java.io.IOException;
import java.util.*;

import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.*;

public class MockPdfPage extends PdfPage {

  public PDRectangle findCropBox() {
    throw new UnsupportedOperationException();
  }

  public PDRectangle findMediaBox() {
    throw new UnsupportedOperationException();
  }

  public PDResources findResources() {
    throw new UnsupportedOperationException();
  }

  public PDRectangle getArtBox() {
    throw new UnsupportedOperationException();
  }

  public PDRectangle getBleedBox() {
    throw new UnsupportedOperationException();
  }

  public COSStream getContentStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  public PDRectangle getCropBox() {
    throw new UnsupportedOperationException();
  }

  public COSDictionary getDictionary() {
    throw new UnsupportedOperationException();
  }

  public PDRectangle getMediaBox() {
    throw new UnsupportedOperationException();
  }

  public PdfDocument getPdfDocument() {
    throw new UnsupportedOperationException();
  }

  public PDPage getPdPage() {
    throw new UnsupportedOperationException();
  }

  public ListIterator getStreamTokenIterator() throws IOException {
    throw new UnsupportedOperationException();
  }

  public List getStreamTokens() throws IOException {
    throw new UnsupportedOperationException();
  }

  public PDRectangle getTrimBox() {
    throw new UnsupportedOperationException();
  }

  public void setArtBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  public void setBleedBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  public void setContents(PDStream rectangle) {
    throw new UnsupportedOperationException();
  }

  public void setCropBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  public void setMediaBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  public void setTrimBox(PDRectangle rectangle) {
    throw new UnsupportedOperationException();
  }

  protected PDStream getContents() throws IOException {
    throw new UnsupportedOperationException();
  }

}
