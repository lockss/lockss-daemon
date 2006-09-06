/*
 * $Id: PdfPage.java,v 1.1 2006-09-01 06:47:00 thib_gc Exp $
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

package org.lockss.util;

import java.io.IOException;
import java.util.*;

import org.pdfbox.cos.*;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.*;

/**
 * <p>Convenience class to provide easy access to the internals of a
 * PDF page ({@link PDPage}).</p>
 * @author Thib Guicherd-Callin
 * @see PDPage
 */
public class PdfPage {

  /**
   * <p>The underlying {@link PDPage} instance.</p>
   */
  private PDPage pdPage;

  /**
   * <p>Builds a new PDF page.</p>
   * @param pdPage An underlying {@link PDPage} instance.
   */
  public PdfPage(PDPage pdPage) {
    this.pdPage = pdPage;
  }

  protected PdfPage() { }

  /**
   * <p>Finds this page's crop box, looking up hierarchically.</p>
   * @return The page's crop box.
   * @see PDPage#findCropBox
   */
  public PDRectangle findCropBox() {
    return getPdPage().findCropBox();
  }

  /**
   * <p>Finds this page's media box, looking up hierarchically.</p>
   * @return The page's media box.
   * @see PDPage#findMediaBox
   */
  public PDRectangle findMediaBox() {
    return getPdPage().findMediaBox();
  }

  /**
   * <p>Finds this page's resources, looking up hierarchically.</p>
   * @return The page's resources.
   * @see PDPage#findResources
   */
  public PDResources findResources() {
    return getPdPage().findResources();
  }

  /**
   * <p>Gets this page's art box (by default the crop box).</p>
   * @return The page's art box.
   * @see PDPage#getArtBox
   */
  public PDRectangle getArtBox() {
    return getPdPage().getArtBox();
  }

  /**
   * <p>Gets this page's bleed box (by default the crop box).</p>
   * @return The page's bleed box.
   * @see PDPage#getBleedBox
   */
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
   */
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
   */
  public PDRectangle getCropBox() {
    return getPdPage().getCropBox();
  }

  /**
   * <p>Gets this page's underlying dictionary.</p>
   * @return This page's underlying dictionary ({@link COSDictionary}).
   * @see PDPage#getCOSDictionary
   */
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
   */
  public PDRectangle getMediaBox() {
    return getPdPage().getMediaBox();
  }

  /**
   * <p>Convenience method to obtain a list iterator over the
   * tokens of this page's content stream.</p>
   * @return A list iterator of the tokens in this page's content
   *         stream.
   * @throws IOException if any processing error occurs.
   * @see #getStreamTokens
   */
  public ListIterator getStreamTokenIterator() throws IOException {
    return getStreamTokens().listIterator();
  }

  /**
   * <p>Gets the list of tokens in this page's content stream.</p>
   * @return A list of tokens in this page's content stream.
   * @throws IOException if any processing error occurs.
   * @see COSStream#getStreamTokens
   */
  public List getStreamTokens() throws IOException {
    return getContentStream().getStreamTokens();
  }

  /**
   * <p>Gets this page's trim box (by default the crop box).</p>
   * @return The page's trim box.
   * @see PDPage#getTrimBox
   */
  public PDRectangle getTrimBox() {
    return getPdPage().getTrimBox();
  }

  /**
   * <p>Sets the art box for this page.</p>
   * @param rectangle The new art box.
   * @see PDPage#setArtBox
   */
  public void setArtBox(PDRectangle rectangle) {
    getPdPage().setArtBox(rectangle);
  }

  /**
   * <p>Sets the bleed box for this page.</p>
   * @param rectangle The new bleed box.
   * @see PDPage#setBleedBox
   */
  public void setBleedBox(PDRectangle rectangle) {
    getPdPage().setBleedBox(rectangle);
  }

  public void setContents(PDStream rectangle) {
    getPdPage().setContents(rectangle);
  }

  /**
   * <p>Sets the crop box for this page.</p>
   * @param rectangle The new crop box.
   * @see PDPage#setCropBox
   */
  public void setCropBox(PDRectangle rectangle) {
    getPdPage().setCropBox(rectangle);
  }

  /**
   * <p>Sets the media box for this page.</p>
   * @param rectangle The new media box.
   * @see PDPage#setMediaBox
   */
  public void setMediaBox(PDRectangle rectangle) {
    getPdPage().setMediaBox(rectangle);
  }

  /**
   * <p>Sets the trim box for this page.</p>
   * @param rectangle The new trim box.
   * @see PDPage#setTrimBox
   */
  public void setTrimBox(PDRectangle rectangle) {
    getPdPage().setTrimBox(rectangle);
  }

  /**
   * <p>Gets this page's content stream ({@link PDStream}).</p>
   * @return This page's content stream.
   * @throws IOException if any processing error occurs.
   * @see PDPage#getContents
   */
  protected PDStream getContents() throws IOException {
    return getPdPage().getContents();
  }

  /**
   * <p>Provides access to the underlying {@link PDPage} instance;
   * <em>use with care.</em></p>
   * @return This page's underlying {@link PDPage} instance.
   * @see PDPage
   */
  public PDPage getPdPage() {
    return pdPage;
  }

}
