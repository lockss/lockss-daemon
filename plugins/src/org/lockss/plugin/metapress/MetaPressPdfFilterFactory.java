/*
 * $Id: MetaPressPdfFilterFactory.java,v 1.3.4.2 2014-07-18 15:54:38 wkwilson Exp $
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

package org.lockss.plugin.metapress;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class MetaPressPdfFilterFactory implements FilterFactory {
  
  /**
   * An enum for publisher IDs used by MetaPress
   */
  protected enum PublisherId {
    /** Unknown */
    UNKNOWN,
    /** Metapress Essential */
    ESSENTIAL,
    /** Inderscience */
    INDERSCIENCE,
    /** Liverpool University Press */
    LIVERPOOL,
    /** Manchester University Press */
    MANCHESTER,
    /** Multi-Science */
    MULTISCIENCE,
    /** Practical Action Publishing */
    PRACTICALACTION,
    /** United Kingdom Serials Group */
    UKSG,
  }
  
  private static final Logger logger = Logger.getLogger(MetaPressPdfFilterFactory.class);
  private static Pattern JOURNAL_ID = Pattern.compile("^https?://([^.]+)[.]metapress[.]com/");
  
  private FilterFactory papFiltFact = new PracticalActionPdfFilterFactory();
  
  /*
   * Filter factory for each different transform because some publisher transforms are
   * simple transforms and some not.
   */
  private class PracticalActionPdfFilterFactory extends SimplePdfFilterFactory {
    
    private Pattern pat1 = Pattern.compile("^Delivered by ");
    private Pattern pat2 = Pattern.compile("^IP Address: ");
    
    protected class MetaPressWorker extends PdfTokenStreamWorker {
      
      protected boolean result;
      protected int beginIndex;
      protected int endIndex;
      protected int state;
      
      public MetaPressWorker() {
      }
      
      @Override
      public void setUp() throws PdfException {
        super.setUp();
        this.state = 0;
        this.result = false;
        this.beginIndex = -1;
        this.endIndex = -1;
      }
      
      @Override
      public void operatorCallback() throws PdfException {
        if (logger.isDebug3()) {
          logger.debug3("MetaPressWorker: initial: " + state);
          logger.debug3("MetaPressWorker: index: " + getIndex());
          logger.debug3("MetaPressWorker: operator: " + getOpcode());
        }
        
        switch (state) {
          case 0: {
            if (isBeginTextObject()) {
              beginIndex = getIndex();
              ++state;
            }
          } break;
          
          case 1: {
            if (isShowTextFind(pat1)) {
              ++state;
            }
          } break;
          
          case 2: {
            if (isShowTextFind(pat2)) {
              ++state;
            }
          } break;
          
          case 3: {
            if (isEndTextObject()) {
              endIndex = getIndex();
              result = true;
              stop(); 
            }
          } break;
          
          default: {
            throw new PdfException("Invalid state in MetaPressWorker: " + state);
          }
        }
        
        if (logger.isDebug3()) {
          logger.debug3("MetaPressWorker: final: " + state);
          logger.debug3("MetaPressWorker: result: " + result);
        }
        
      }
      
    }
    
    @Override
    public void transform(ArchivalUnit au, PdfDocument pdfDocument)
        throws PdfException {
      pdfDocument.unsetCreationDate();
      pdfDocument.unsetModificationDate();
      pdfDocument.unsetMetadata();
      PdfUtil.normalizeTrailerId(pdfDocument);
      
      MetaPressWorker worker = new MetaPressWorker();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        List<PdfTokenStream> pdfTokenStreamList = pdfPage.getAllTokenStreams();
        for (PdfTokenStream pdfTokenStream : pdfTokenStreamList) {
          List<PdfToken> tokens = pdfTokenStream.getTokens();
          worker.process(pdfTokenStream);
          if (worker.result) {
            tokens.subList(worker.beginIndex, worker.endIndex).clear();
          }
          pdfTokenStream.setTokens(tokens);
        }
      }
    }
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding)
          throws PluginException {
    
    PublisherId publisherId = PublisherId.UNKNOWN;
    String publisher_sd = "";
    try {
      Configuration config = au.getConfiguration();
      Matcher mat = JOURNAL_ID.matcher(config.get("base_url"));
      if (mat.matches()) {
        publisher_sd = mat.group(1).toUpperCase();
        publisher_sd.replace("-", "");
        publisherId = PublisherId.valueOf(publisher_sd);
      }
    } catch (Exception e) {
      logger.debug(String.format("Exception: %s : %s", publisher_sd, e.toString()));
    }
    
    switch (publisherId) {
      case PRACTICALACTION:
        return papFiltFact.createFilteredInputStream(au, in, encoding);
        
      case UNKNOWN:
      default:
        return in;
    }
  }
  
}
