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

package org.lockss.plugin.highwire;

import org.lockss.filter.pdf.ExtractingPdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;


/**
 * A pdf filter that handles the more challenging PDFs of child plugins where pdfplus
 * files have non-substantive changes (annotation uris, ordering, etc). In this
 * case scrape out the content text for comparison.
 * This code modified from BaseAtyponScrapingPdfFilterFactory  @author alexohlson
 *
 */
public class HighWireJCoreScrapingPdfFilterFactory extends ExtractingPdfFilterFactory {
  
  private static final Logger log = Logger.getLogger(HighWireJCoreScrapingPdfFilterFactory.class);
  
  /*
   * Many pdf files have the CreationDate and ModDate and the two ID numbers in the trailer
   * vary from collection to collection. Filter them out to avoid incorrect hash failures.
   * This is a scraping filter to it also returns just the actual pdf content and not the layout.
   */
  @Override
  public void transform(ArchivalUnit au, PdfDocument pdfDocument) 
      throws PdfException {
    
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetCreator();
    pdfDocument.unsetProducer();
    pdfDocument.unsetAuthor();
    pdfDocument.unsetTitle();
    pdfDocument.unsetSubject();
    pdfDocument.unsetKeywords();
    PdfUtil.normalizeTrailerId(pdfDocument);
    PdfUtil.normalizeAllTokenStreams(pdfDocument);
  }
}

