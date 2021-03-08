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

import java.io.IOException;
import java.util.*;

import org.pdfbox.cos.*;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.*;
import org.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

/**
 * <p>Convenience class to provide easy access to the internals of a
 * PDF page ({@link PDPage}).</p>
 * @author Thib Guicherd-Callin
 * @see PDPage
 */
@Deprecated
public class PdfPage {

  /**
   * <p>The associated {@link PdfDocument} instance.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private PdfDocument pdfDocument;

  /**
   * <p>The underlying {@link PDPage} instance.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private PDPage pdPage;

  /**
   * <p>Builds a new PDF page.</p>
   * @param pdPage An underlying {@link PDPage} instance.
   */
  @Deprecated
  public PdfPage(PdfDocument pdfDocument, PDPage pdPage) {
    this.pdfDocument = pdfDocument;
    this.pdPage = pdPage;
  }

  @Deprecated
  protected PdfPage() { }

  /**
   * <p>Finds this page's crop box, looking up hierarchically.</p>
   * @return The page's crop box.
   * @see PDPage#findCropBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDRectangle findCropBox() {
    return getPdPage().findCropBox();
  }

  /**
   * <p>Finds this page's media box, looking up hierarchically.</p>
   * @return The page's media box.
   * @see PDPage#findMediaBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDRectangle findMediaBox() {
    return getPdPage().findMediaBox();
  }

  /**
   * <p>Finds this page's resources, looking up hierarchically.</p>
   * @return The page's resources.
   * @see PDPage#findResources
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDResources findResources() {
    return getPdPage().findResources();
  }

  /**
   * @see #getAnnotations
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDAnnotation getAnnotation(int index) throws IOException {
    return (PDAnnotation)getAnnotations().get(index);
  }

  /**
   * @see #getAnnotations
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ListIterator /* of PDAnnotation */ getAnnotationIterator() throws IOException {
    return getAnnotations().listIterator();
  }

  /**
   * <p>Gets this page's art box (by default the crop box).</p>
   * @return The page's art box.
   * @see PDPage#getArtBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDRectangle getArtBox() {
    return getPdPage().getArtBox();
  }

  /**
   * <p>Gets this page's bleed box (by default the crop box).</p>
   * @return The page's bleed box.
   * @see PDPage#getBleedBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDRectangle getBleedBox() {
    return getPdPage().getBleedBox();
  }

  /**
   * <p>Gets the {@link COSStream} instance underlying to this page's
   * content stream.</p>
   * @return This page's content stream as a {@link COSStream}
   *         instance.
   * @throws IOException if any processing error occurs.
   * @see PDStream#getStream
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public COSStream getContentStream() throws IOException {
    return getContents().getStream();
  }

  /**
   * <p>Gets this page's crop box, <em>without</em> looking up
   * hierarchically.</p>
   * @return The page's crop box, or null if none is defined at this
   *         level.
   * @see #findCropBox
   * @see PDPage#getArtBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDRectangle getCropBox() {
    return getPdPage().getCropBox();
  }

  /**
   * <p>Gets this page's underlying dictionary.</p>
   * @return This page's underlying dictionary ({@link COSDictionary}).
   * @see PDPage#getCOSDictionary
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public COSDictionary getDictionary() {
    return getPdPage().getCOSDictionary();
  }

  /**
   * <p>Gets this page's media box, <em>without</em> looking up
   * hierarchically.</p>
   * @return The page's media box, or null if none is defined at this
   *         level.
   * @see #findMediaBox
   * @see PDPage#getMediaBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDRectangle getMediaBox() {
    return getPdPage().getMediaBox();
  }

  /**
   * @see #getAnnotations
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public int getNumberOfAnnotations() throws IOException {
    return getAnnotations().size();
  }

  /**
   * <p>Provides access to the {@link PdfDocument} instance associated
   * with this PDF page.\</p>
   * @return This page's associated {@link PdfDocument} instance.
   * @see PdfDocument
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PdfDocument getPdfDocument() {
    return pdfDocument;
  }

  /**
   * <p>Provides access to the underlying {@link PDPage} instance;
   * <em>use with care.</em></p>
   * @return This page's underlying {@link PDPage} instance.
   * @see PDPage
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDPage getPdPage() {
    return pdPage;
  }

  /**
   * <p>Convenience method to obtain a list iterator over the
   * tokens of this page's content stream.</p>
   * @return A list iterator of the tokens in this page's content
   *         stream.
   * @throws IOException if any processing error occurs.
   * @see #getStreamTokens
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ListIterator getStreamTokenIterator() throws IOException {
    return getStreamTokens().listIterator();
  }

  /**
   * <p>Gets the list of tokens in this page's content stream.</p>
   * @return A list of tokens in this page's content stream.
   * @throws IOException if any processing error occurs.
   * @see COSStream#getStreamTokens
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public List getStreamTokens() throws IOException {
    return getContentStream().getStreamTokens();
  }

  /**
   * <p>Gets this page's trim box (by default the crop box).</p>
   * @return The page's trim box.
   * @see PDPage#getTrimBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public PDRectangle getTrimBox() {
    return getPdPage().getTrimBox();
  }

  /**
   * <p>Sets the art box for this page.</p>
   * @param rectangle The new art box.
   * @see PDPage#setArtBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setArtBox(PDRectangle rectangle) {
    getPdPage().setArtBox(rectangle);
  }

  /**
   * <p>Sets the bleed box for this page.</p>
   * @param rectangle The new bleed box.
   * @see PDPage#setBleedBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setBleedBox(PDRectangle rectangle) {
    getPdPage().setBleedBox(rectangle);
  }

  /**
   * @param rectangle
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setContents(PDStream rectangle) {
    getPdPage().setContents(rectangle);
  }

  /**
   * <p>Sets the crop box for this page.</p>
   * @param rectangle The new crop box.
   * @see PDPage#setCropBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setCropBox(PDRectangle rectangle) {
    getPdPage().setCropBox(rectangle);
  }

  /**
   * <p>Sets the media box for this page.</p>
   * @param rectangle The new media box.
   * @see PDPage#setMediaBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setMediaBox(PDRectangle rectangle) {
    getPdPage().setMediaBox(rectangle);
  }

  /**
   * <p>Sets the trim box for this page.</p>
   * @param rectangle The new trim box.
   * @see PDPage#setTrimBox
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void setTrimBox(PDRectangle rectangle) {
    getPdPage().setTrimBox(rectangle);
  }

  /**
   * @see PDPage#getAnnotations
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected List /* of PDAnnotation */ getAnnotations() throws IOException {
    return getPdPage().getAnnotations();
  }

  /**
   * <p>Gets this page's content stream ({@link PDStream}).</p>
   * @return This page's content stream.
   * @throws IOException if any processing error occurs.
   * @see PDPage#getContents
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected PDStream getContents() throws IOException {
    return getPdPage().getContents();
  }

}
