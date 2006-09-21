/*
 * $Id: HighWirePdfFilterFactory.java,v 1.1 2006-09-21 05:50:52 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.filter.pdf.DocumentTransform;
import org.lockss.plugin.*;
import org.lockss.util.PdfDocument;
import org.pdfbox.cos.*;

public class HighWirePdfFilterFactory implements FilterFactory {

  public static class SanitizeMetadata implements DocumentTransform {
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      // Get rid of the modification date
      pdfDocument.removeModificationDate();
      // Get rid of the metadata
      pdfDocument.setMetadata(" ");
      // Replace instance ID by document ID in trailer
      COSBase idObj = pdfDocument.getTrailer().getItem(COSName.getPDFName("ID"));
      if (idObj != null && idObj instanceof COSArray) {
        COSArray idArray = (COSArray)idObj;
        idArray.set(1, idArray.get(0));
        return true;
      }
      else {
        return false;
      }
    }
  }

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    throw new UnsupportedOperationException("Unimplemented");
  }

}
