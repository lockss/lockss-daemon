/*
 * $Id: SEGPdfFilterFactory.java,v 1.5 2015-01-13 00:02:53 alexandraohlson Exp $
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

package org.lockss.plugin.atypon.seg;

import java.util.regex.Pattern;

import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponScrapingPdfFilterFactory;
import org.lockss.util.Logger;

/*
 * Example: http://library.seg.org/doi/pdfplus/10.1190/geo2012-0531.1
 */
public class SEGPdfFilterFactory extends BaseAtyponScrapingPdfFilterFactory {
  private static final Logger logger = Logger.getLogger(SEGPdfFilterFactory.class);

  public static final Pattern SEG_DOWNLOADED_PATTERN =
      Pattern.compile("^Downloaded \\d+/\\d+/\\d+ to \\d+\\.\\d+\\.\\d+\\.\\d+", Pattern.CASE_INSENSITIVE);

  /* 
   * Turn on removal of "This article cited by:" pages - the default string is correct
   */
  @Override
  public boolean doRemoveCitedByPage() {
    return true;    
  }  
  @Override
  public boolean doRemoveDownloadStrip() {
    return true;
  }
  /* and set the correct string to use for this publisher */
  @Override
  public Pattern getDownloadStripPattern() {
    return SEG_DOWNLOADED_PATTERN;
  }  


   /*
    * (non-Javadoc)
    * @see org.lockss.plugin.atypon.BaseAtyponScrapingPdfFilterFactory#transform(org.lockss.plugin.ArchivalUnit, org.lockss.pdf.PdfDocument)
    * 
    * Override transform in order to extend the base transforms done before modifying the
    * token stream.
    */
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) throws PdfException {
    // Metadata completely rewritten when file is watermarked, can't compare to original
    /* FIXME 1.67: use the getDocumentTransform/outputDocumentInformation override instead */
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetCreator();
    pdfDocument.unsetProducer();
    pdfDocument.unsetAuthor();
    pdfDocument.unsetTitle();
    pdfDocument.unsetSubject();
    pdfDocument.unsetKeywords();
    /* end FIXME 1.67 */   
    removeCitedByPage(pdfDocument);
    removeDownloadStrip(pdfDocument);
  }

  /* FIXME 1.67 */
  //  @Override
  //  public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au, OutputStream os) {
  //    return new BaseDocumentExtractingTransform(os) {
  //      @Override
  //      public void outputDocumentInformation() throws PdfException {
  //        // Intentionally left blank
  //      }
  //    };
  //  }
  /* end FIXME 1.67 */

}




