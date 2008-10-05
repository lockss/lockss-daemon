/*
 * $Id: NormalizeTrailerId.java,v 1.2 2008-01-16 00:41:00 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.anthrosource;

import java.io.IOException;

import org.lockss.filter.pdf.DocumentTransform;
import org.lockss.util.PdfDocument;
import org.pdfbox.cos.*;

public class NormalizeTrailerId implements DocumentTransform {

  public boolean transform(PdfDocument pdfDocument) throws IOException {
    // The PDF name "ID"
    COSName idName = COSName.getPDFName("ID");

    // Create the trailer if it does not exist
    COSDictionary trailer = pdfDocument.getTrailer();
    if (trailer == null) {
      trailer = new COSDictionary();
      pdfDocument.getCosDocument().setTrailer(trailer);
    }

    // The ID is noisily positioned within the trailer
    trailer.removeItem(idName);

    // The ID itself is noisy
    COSArray id = new COSArray();
    id.add(new COSString("12345678901234567890123456789012"));
    id.add(id.get(0));
    trailer.setItem(idName, id);

    return true; // success
  }

}