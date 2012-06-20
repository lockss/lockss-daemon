/*
 * $Id: PacificAffairsPdfTransform.java,v 1.1.4.2 2012-06-20 00:02:56 nchondros Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;

import org.apache.oro.text.regex.Pattern;
import org.exolab.castor.types.OperationNotSupportedException;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlUrl;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.filter.pdf.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ingenta.IngentaPdfUtil.NeedsArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.pdfbox.cos.*;
import org.pdfbox.util.operator.OperatorProcessor;

public class PacificAffairsPdfTransform implements OutputDocumentTransform,
                                                   NeedsArchivalUnit {
  
  protected static Logger log = Logger.getLogger("PacificAffairsPdfTransform");

  protected ArchivalUnit au;
  
  public void setArchivalUnit(ArchivalUnit au) {
    this.au = au;
  }
  
  @Override
  public boolean transform(PdfDocument pdfDocument) throws IOException {
    throw new IOException("PacificAffairsPdfTransform is an OutputDocumentTransform");
  }

  @Override
  public boolean transform(PdfDocument pdfDocument, OutputStream outputStream) {
    if (au == null) {
      throw new IllegalStateException("Transform cannot be invoked without setting an archival unit");
    }
    try {
      pdfDocument.removeCreationDate();
      pdfDocument.removeModificationDate();
      COSDictionary trailer = pdfDocument.getTrailer();
      if (trailer != null) {
        // Put bogus ID to prevent variable ID
        COSArray id = new COSArray();
        id.add(new COSString("12345678901234567890123456789012"));
        id.add(id.get(0));
        trailer.setItem(COSName.getPDFName("ID"), id);
      }
      new TransformEachPage(new CollapseVariableString()).transform(pdfDocument);
      return true;
    }
    catch (IOException ioe) {
      log.debug("IOException during transform", ioe);
      return false;
    }
  }
  
  public class CollapseVariableString extends PageStreamTransform {
    
    public CollapseVariableString() throws IOException {
      super(new OperatorProcessorFactory() {
                @Override
                public OperatorProcessor newInstanceForName(String className)
                    throws LinkageError,
                           ExceptionInInitializerError,
                           ClassNotFoundException,
                           IllegalAccessException,
                           InstantiationException,
                           SecurityException {
                  return (OperatorProcessor)((PacificAffairsPdfTransform.this.au).getPlugin().newAuxClass(className, OperatorProcessor.class));
                }
            },
            // Split on BT
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            // Merge conditionally on ET
            PdfUtil.END_TEXT_OBJECT, CollapseVariableStringOperatorProcessor.class);
    }
  }
  
  public static class CollapseVariableStringOperatorProcessor extends ConditionalMergeOperatorProcessor {
    
    @Override
    public boolean identify(List tokens) {
      boolean result = false;
      int state = 0;
      
      // Labeled loop
      loop: for (int token = tokens.size() - 1 ; token >= 0 ; --token) {
        if (log.isDebug3()) {
          log.debug3("State " + state + ", token " + token);
        }
        
        switch (state) {
          
          case 0:
            // End of subsequence
            if (token != tokens.size() - 1) {
              break loop;
            }
            // ET
            if (PdfUtil.isEndTextObject(tokens, token)) {
              state = 1;
            }
            break;
            
          case 1:
            // Not BT
            if (!PdfUtil.isBeginTextObject(tokens, token)) {
              break loop;
            }
            // Tj and an operand matching our search string
            if (PdfUtil.matchShowText(tokens, token, "Delivered by Publishing Technology to: ")) {
              state = 2;
            }
            break;
          
          case 2:
            // BT and beginning of subsequence
            if (PdfUtil.isBeginTextObject(tokens, token) && token == 0) {
              result = true;
              break loop;
            }
            break;
        }
        
      }
      
      log.debug3(result ? "Successful identification" : "Unsuccessful identification");
      return result;
    }
    
    @Override
    public List getReplacement(List tokens) {
      return ListUtil.list(tokens.get(0),tokens.get(tokens.size()-1));
    }
  }

}
