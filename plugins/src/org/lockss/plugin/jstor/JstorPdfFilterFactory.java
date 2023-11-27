/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.jstor;

//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.OutputStream;
import java.util.List;

//import org.apache.commons.io.IOUtils;
import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

/*
 * This pdf filter works for both legacy JSTOR and Jstor Current Scholarship
 */
public class JstorPdfFilterFactory extends ExtractingPdfFilterFactory {
  
  private static final Logger logger = Logger.getLogger(JstorPdfFilterFactory.class);

  public static final String DOWNLOADED_FROM = "This content downloaded from ";
  public static final String ALL_USE = "All use subject to http";

/*
 * On each page there is a footer that says (equiv of):
 * "This content downloaded from 171.66.236.16 on Tue, 3 Jun 2014 14:34:21 PM"
 * 
 * The token stream looks like this:
 *6533    [operator:BT]
 *6534    [float:5.57]
 *6535    [integer:0]
 *6536    [integer:0]
 *6537    [float:5.57]
 *6538    [float:154.64]
 *6539    [float:-11.13]
 *6540    [operator:Tm]
 *6541    [name:TT6.0]
 *6542    [integer:1]
 *6543    [operator:Tf]
 *6544    [string:"This content downloaded from 171.66.236.16 on Tue, 3 Jun 2014 14:34:21 PM"]
 *6545    [operator:Tj]
 *6546    [operator:ET]
 *  may or may not be the final BT-ET chunk, eg may have
 *....two additional similar chunks for "All use subject to " and "JSTOR terms and conditions"
 *...and page ends with...
 *6599    [operator:Q]
 *6600    [operator:Q]
 */
  public static class JstorTokenStreamWorker extends PdfTokenStreamWorker {

    private boolean result;
    
    private int beginIndex;
    
    private int endIndex;
    
    private int state;
    
    private String target;
    
    // The footer is close to the bottom of each page
    public JstorTokenStreamWorker(String target) {
      super(Direction.BACKWARD);
      this.target = target;
    }
    
    @Override
    public void operatorCallback() throws PdfException {
      if (logger.isDebug3()) {
        logger.debug3("Jstor Worker: initial: " + state);
        logger.debug3("Jstor Worker: index: " + getIndex());
        logger.debug3("Jstor Worker: operator: " + getOpcode());
      }

      switch (state) {

        case 0: {
          if (isEndTextObject()) {
            endIndex = getIndex();
            ++state;
          } //If not, just continue looking until you find one.
        } break;
        
        case 1: { // We have found an ET object
          if (isShowTextStartsWith(target)) {
            ++state; // It's the one we want
          }
          else if (isBeginTextObject()) { // not the BT-ET we were looking for
             --state; //reset search; keep looking
          }
        } break;
        
        case 2: {
          if (isBeginTextObject()) {  // we need to remove this BT-ET chunk
            beginIndex = getIndex();
            result = true;
            stop(); // found what we needed, stop processing this page
          }
        } break;
        
        default: {
          throw new PdfException("Invalid state in Jstor Worker: " + state);
        }
        
      }
      
      if (logger.isDebug3()) {
        logger.debug3("Jstor Worker: final: " + state);
        logger.debug3("Jstor Worker: result: " + result);
      }
      
    }
    
    @Override
    public void setUp() throws PdfException {
      super.setUp();
      result = false;
      state = 0;
      beginIndex = endIndex = -1;
    }
    
  }

  @Override
  public void transform(ArchivalUnit au,
                        PdfDocument pdfDocument)
      throws PdfException {
    pdfDocument.unsetModificationDate();
    pdfDocument.unsetMetadata();
    pdfDocument.unsetCreationDate();
    pdfDocument.unsetCreator();
    pdfDocument.unsetLanguage();
    pdfDocument.unsetProducer();
    pdfDocument.unsetKeywords();

    JstorTokenStreamWorker worker1 = new JstorTokenStreamWorker(DOWNLOADED_FROM);
    JstorTokenStreamWorker worker2 = new JstorTokenStreamWorker(ALL_USE);
    boolean firstPage = true;
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      PdfTokenStream pdfTokenStream = pdfPage.getPageTokenStream();
      worker1.process(pdfTokenStream);
      if (firstPage && !worker1.result) {
        return; // This PDF needs no further processing
      }
      if (worker1.result) {
        List<PdfToken> tokens = pdfTokenStream.getTokens();
        logger.debug3("REMOVE FROM: " + worker1.beginIndex + " TO: " + worker1.endIndex);
        tokens.subList(worker1.beginIndex, worker1.endIndex + 1).clear(); // The +1 is normal substring/sublist indexing (upper bound is exclusive)
        pdfTokenStream.setTokens(tokens);
      }
      firstPage = false;
      /*
       * May decide to attempt to remove both strings using a single worker
       * Would be somewhat more complex and content may not always have both strings
       */
      pdfTokenStream = pdfPage.getPageTokenStream();
      worker2.process(pdfTokenStream);
      if (worker2.result) {
        List<PdfToken> tokens = pdfTokenStream.getTokens();
        logger.debug3("REMOVE FROM: " + worker2.beginIndex + " TO: " + worker2.endIndex);
        tokens.subList(worker2.beginIndex, worker2.endIndex + 1).clear(); // The +1 is normal substring/sublist indexing (upper bound is exclusive)
        pdfTokenStream.setTokens(tokens);
      }
    }
  }

  /*@Override
  public PdfTransform<PdfDocument> getDocumentTransform(ArchivalUnit au, OutputStream os) {
    return new BaseDocumentExtractingTransform(os) {
      @Override
      public PdfTransform<PdfPage> getPageTransform() {
        return new BasePageExtractingTransform(os) {
          @Override
          public void transform(ArchivalUnit au, PdfPage pdfPage) throws PdfException {
            this.au = au;
            this.pdfPage = pdfPage;
            
            // Use a worker to extract all PDF strings
            PdfTokenStreamWorker worker = new PdfTokenStreamWorker() {
              @Override public void operatorCallback() throws PdfException {
                // 'Tj', '\'' and '"'
                if (isShowText() || isNextLineShowText() || isSetSpacingNextLineShowText()) {
                  outputString(getTokens().get(getIndex() - 1).getString());
                }
                // 'TJ'
                else if (isShowTextGlyphPositioning()) {
                  for (PdfToken token : getTokens().get(getIndex() - 1).getArray()) {
                    if (token.isString()) {
                      outputString(token.getString());
                    }
                  }
                }
              }
            };
            
            // Apply the worker to every token stream in the page
            for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
              worker.process(pdfTokenStream);
            }
          }
        };
      }
    };
  }*/

  /*public static void main(String[] args) throws Exception {
    String[] files = new String[] {
        "/tmp/data/jstor1.pdf",
        "/tmp/data/jstor2.pdf",
    }; 
    for (String file : files) {
      IOUtils.copy(new JstorPdfFilterFactory().createFilteredInputStream(null, new FileInputStream(file), null),
                   new FileOutputStream(file + ".out"));
    }
  }*/

}
