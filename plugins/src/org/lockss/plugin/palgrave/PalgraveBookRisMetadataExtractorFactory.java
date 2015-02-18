/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.palgrave;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.util.Logger;

  // Palgrave Book Citation Export:
  // TY  - BOOK
  // AU  - Larsson, Mats
  // DA  - 2014/08/01
  // PY  - 2012
  // TI  - The Business of Global Energy Transformation: Saving Billions through Sustainable Models
  // PB  - Palgrave Macmillan
  // CY  - Basingstoke
  // SN  - 9781137024497
  // DO  - 10.1057/9781137024497
  // UR  - http://dx.doi.org/10.1057/9781137024497
  // L1  - http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf
  // ER  -
  public class PalgraveBookRisMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
    
    static Logger log = Logger.getLogger(PalgraveBookRisMetadataExtractorFactory.class);
    
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
          String contentType) 
              throws PluginException {
          
      RisMetadataExtractor ris = new RisMetadataExtractor();
      // removing use of DA - PalgraveBook uses this for citation access date
      // and uses PY for publication year
      if (ris.containsRisTag("DA")) {
        ris.removeRisTag("DA");
      }
      log.debug3("ris: " + ris.toString());
      ris.addRisTag("PY", MetadataField.FIELD_DATE);
      ris.addRisTag("TI", MetadataField.FIELD_PUBLICATION_TITLE);
      ris.addRisTag("SN", MetadataField.FIELD_EISBN);
      ris.addRisTag("DO", MetadataField.FIELD_DOI);
      ris.addRisTag("L1", MetadataField.FIELD_ACCESS_URL);
    
    return ris;
  }

}
