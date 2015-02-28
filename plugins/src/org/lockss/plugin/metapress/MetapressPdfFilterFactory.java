/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.filter.pdf.*;
import org.lockss.pdf.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class MetapressPdfFilterFactory implements FilterFactory {
  
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
  
  private static final Logger log = Logger.getLogger(MetapressPdfFilterFactory.class);
  
  private static Pattern PUBLISHER_ID = Pattern.compile("^https?://([^.]+)[.]metapress[.]com/");
  
  private FilterFactory papFiltFact = new PracticalActionPdfFilterFactory();
  
  /*
   * Filter factory for each different transform because some publisher transforms are
   * simple transforms and some not.
   */
  private class PracticalActionPdfFilterFactory extends SimplePdfFilterFactory {
    
    protected class MetaPressStateMachine extends PdfTokenStreamStateMachine {
      
      protected static final String DELIVERED_BY = "Delivered by ";
      protected static final String IP_ADDRESS = "IP Address: ";

      public MetaPressStateMachine() {
        super(log);
      }
      
      @Override
      public void state0() throws PdfException {
        if (isBeginTextObject()) {
          setBegin(getIndex());
          setState(1);
        }
      }

      @Override
      public void state1() throws PdfException {
        if (isShowTextStartsWith(DELIVERED_BY)) {
          setState(2);
        }
        else if (isEndTextObject()) {
          setState(0);
        }
      }

      @Override
      public void state2() throws PdfException {
        if (isShowTextStartsWith(IP_ADDRESS)) {
          setState(3);
        }
        else if (isEndTextObject()) {
          setState(0);
        }
      }

      @Override
      public void state3() throws PdfException {
        if (isEndTextObject()) {
          setEnd(getIndex());
          setResult(true);
          stop(); 
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
      
      PdfTokenStreamStateMachine worker = new MetaPressStateMachine();
      for (PdfPage pdfPage : pdfDocument.getPages()) {
        for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
          worker.process(pdfTokenStream);
          if (worker.getResult()) {
            List<PdfToken> tokens = pdfTokenStream.getTokens();
            tokens.subList(worker.getBegin(), worker.getEnd() + 1).clear();
            pdfTokenStream.setTokens(tokens);
            break; // go to the next page
          }
        }
      }
    }
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding)
          throws PluginException {
    
    PublisherId publisherId = null;
    String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    Matcher mat = PUBLISHER_ID.matcher(base_url);
    if (mat.matches()) {
      try {
        publisherId = PublisherId.valueOf(mat.group(1).toUpperCase().replace("-", ""));
      }
      catch (IllegalArgumentException iae) {
        log.debug(String.format("Illegal publisher ID: %s", mat.group(1)), iae);
        publisherId = PublisherId.UNKNOWN;
      }
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
