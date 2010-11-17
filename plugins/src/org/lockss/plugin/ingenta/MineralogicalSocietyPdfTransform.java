/*
 * $Id: MineralogicalSocietyPdfTransform.java,v 1.1 2010-11-17 19:07:38 pgust Exp $
 */ 

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.*;

import org.lockss.filter.pdf.*;
import org.lockss.filter.pdf.PageTransformUtil.ExtractStringsToOutputStream;
import org.lockss.util.*;

/**
 * This class contains PDF transforms that are specific for the Mineralogical Society.
 * 
 * @author Philip Gust
 *
 */
public class MineralogicalSocietyPdfTransform implements OutputDocumentTransform {

  @Override
  public boolean transform(PdfDocument pdfDocument,
                           OutputStream outputStream) {
    
    try {
      if ("RealPage PDF Generator 2.0".equals(pdfDocument.getCreator())) {
        IngentaPdfFilterFactory.logger.debug2("RealPage");
        
        // Extract all text
        return new TransformEachPage(new ExtractStringsToOutputStream(outputStream)).transform(pdfDocument);
      }
      else if ("iText 2.0.7 (by lowagie.com)".equals(pdfDocument.getProducer())) {
        IngentaPdfFilterFactory.logger.debug2("iText");
        return IngentaPdfUtil.simpleTransform(pdfDocument, outputStream);
      }
      else {
        IngentaPdfFilterFactory.logger.debug2("None");
        pdfDocument.save(outputStream);
        return true;
      }
    }
    catch (IOException ioe) {
      IngentaPdfFilterFactory.logger.debug2("IOException in MineralogicalSocietyPdfTransform", ioe);
      return false;
    }
  }

  @Override
  public boolean transform(PdfDocument pdfDocument) throws IOException {
    throw new IOException("MineralogicalSocietyPdfTransform is an OutputDocumentTransform");
  }

}
