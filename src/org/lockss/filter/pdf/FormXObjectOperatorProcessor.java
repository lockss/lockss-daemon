/*
 * $Id: FormXObjectOperatorProcessor.java,v 1.2 2007-08-14 09:19:27 thib_gc Exp $
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

package org.lockss.filter.pdf;

import java.io.*;
import java.util.*;

import org.lockss.util.PdfUtil;
import org.pdfbox.cos.*;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.pdmodel.graphics.xobject.*;
import org.pdfbox.util.PDFOperator;

/**
 * <p>A PDF operator processor that recursively processes the token
 * streams of Form XObjects referenced from within other token
 * streams with {@link PdfUtil#INVOKE_NAMED_XOBJECT}.</p>
 * @author Thib Guicherd-Callin
 * @see PdfUtil#INVOKE_NAMED_XOBJECT
 */
public class FormXObjectOperatorProcessor extends SimpleOperatorProcessor {

  @Override
  public void process(PageStreamTransform pageStreamTransform,
                      PDFOperator operator,
                      List operands)
      throws IOException {
    super.process(pageStreamTransform, operator, operands);
    COSName name = (COSName)operands.get(0);
    PDXObject xob = (PDXObject)pageStreamTransform.getXObjects().get(name.getName());
    if (xob instanceof PDXObjectForm) {
      // Gather data
      PDXObjectForm formXob = (PDXObjectForm)xob;
      PDPage currentPage = pageStreamTransform.getCurrentPage();
      COSStream referencedStream = (COSStream)formXob.getCOSObject();

      // Find the resources associated with the stream
      PDResources resources = formXob.getResources();
      if (resources == null) {
          resources = currentPage.findResources();
      }

      // Save the state of the current stream transform
      boolean saveFlag = pageStreamTransform.getChangeFlag();
      pageStreamTransform.setChangeFlag(false);
      pageStreamTransform.splitOutputList();

      // Process the referenced stream
      pageStreamTransform.processSubStream(currentPage,
                                           resources,
                                           referencedStream);

      if (pageStreamTransform.getChangeFlag()) {
        // The referenced stream required changes
        PDStream resultStream = pageStreamTransform.getCurrentPdfPage().getPdfDocument().makePdStream();
        resultStream.getStream().setName("Subtype", PDXObjectForm.SUB_TYPE);
        OutputStream outputStream = resultStream.createOutputStream();
        ContentStreamWriter tokenWriter = new ContentStreamWriter(outputStream);
        tokenWriter.writeTokens(pageStreamTransform.getOutputList());
        xob.getCOSStream().replaceWithStream(resultStream.getStream());
      }

      // Restore the state of the current stream transform
      pageStreamTransform.mergeOutputList(new ArrayList());
      pageStreamTransform.setChangeFlag(saveFlag || pageStreamTransform.getChangeFlag());
    }
  }

}
