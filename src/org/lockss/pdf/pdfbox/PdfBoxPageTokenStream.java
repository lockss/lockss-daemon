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

package org.lockss.pdf.pdfbox;

import java.util.*;

import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.lockss.pdf.*;

/**
 * <p>
 * A {@link PdfBoxTokenStream} implementation suitable for a page
 * token stream, based on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as a direct adapter for the {@link PDStream} class.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public class PdfBoxPageTokenStream extends PdfBoxTokenStream {

  /**
   * <p>
   * The {@link PDStream} instance underpinning this instance.
   * </p>
   * 
   * @since 1.56
   */
  protected PDStream pdStream;
  
  /**
   * <p>
   * Constructor.
   * </p>
   * 
   * @param pdfBoxPage The parent PDF page.
   * @param pdStream The {@link PDStream} being wrapped.
   * @since 1.56
   */
  public PdfBoxPageTokenStream(PdfBoxPage pdfBoxPage,
                               Object nullObject) {
    super(pdfBoxPage);
    if (nullObject != null) {
      throw new IllegalArgumentException("The second argument must always be null");
    }
//    this.pdStream = pdStream; // FIXME
  }
  
  @Override
  public PdfBoxTokenStreamIterator getOperandsAndOperatorIterator() throws PdfException {
    return new PdfBoxTokenStreamIterator(getPage().getPdPage());
  }
  
  @Override
  public void setTokens(Iterator<? extends PdfToken> tokenIterator) throws PdfException {
    pdfBoxPage.getPdPage().setContents(makePdStreamFromTokens(tokenIterator));
  }

  @Override
  protected PDContentStream getPdContentStream() {
    return getPage().getPdPage(); // FIXME: unused?
  }
  
  @Override
  protected PDResources getStreamResources() {
    return pdfBoxPage.getPdPage().getResources();
  }

}
