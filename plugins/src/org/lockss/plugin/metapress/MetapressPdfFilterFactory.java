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
  protected static class PracticalActionPdfFilterFactory extends SimplePdfFilterFactory {
    
    protected static class MetaPressStateMachine extends PdfTokenStreamStateMachine {
      
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
      }

      @Override
      public void state2() throws PdfException {
        if (isShowTextStartsWith(IP_ADDRESS)) {
          setState(3);
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
