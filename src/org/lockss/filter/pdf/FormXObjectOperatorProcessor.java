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
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class FormXObjectOperatorProcessor extends SimpleOperatorProcessor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public FormXObjectOperatorProcessor() {}
  
  @Override
  @Deprecated
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
