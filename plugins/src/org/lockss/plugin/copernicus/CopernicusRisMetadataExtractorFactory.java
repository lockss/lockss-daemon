/*
 * $Id: CopernicusRisMetadataExtractorFactory.java,v 1.1 2012-11-15 21:36:52 alexandraohlson Exp $
 */

/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.copernicus;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.util.Logger;

/*
 * Augment the standard RisMetadataExtractor to pull metadata from a .ris file
 * found: <base_url>/<vol#>/<issue#>/<year>/<abstract_base>.ris
 * The Copernicus RIS file is a little non-standard, so add to the map. It looks like this:
 * 
 * TY  - JOUR
 * A1  - Winkler, R.
 * A1  - Landais, A.
 * A1  - Sodemann, H.
 * A1  - Damgen, L.
 * A1  - Priac, F.
 * A1  - Masson-Delmotte, V.
 * A1  - Stenni, B.
 * A1  - Jouzel, J.
 * T1  - Deglaciation records of 17O-excess in East Antarctica:  reliable reconstruction of oceanic normalized relative humidity from coastal sites
 * JO  - Clim. Past
 * J1  - CP
 * VL  - 8
 * IS  - 1
 * SP  - 1
 * EP  - 16
 * Y1  - 2012/01/03
 * PB  - Copernicus Publications
 * SN  - 1814-9332
 * UR  - http://www.clim-past.net/8/1/2012/
 * L1  - http://www.clim-past.net/8/1/2012/cp-8-1-2012.pdf
 * DO  - 10.5194/cp-8-1-2012
 * ER  - 
 * 
 */
public class CopernicusRisMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("BaseArchivalUnit");
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    
    log.debug3("Inside Copernicus Metadata extractor factory");
    
    RisMetadataExtractor ris = new RisMetadataExtractor();
    /* Copernicus seems to use different tags for these RIS fields */
    ris.addRisTag("JO", MetadataField.FIELD_JOURNAL_TITLE);
    ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    ris.addRisTag("Y1", MetadataField.FIELD_DATE);
    ris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    /* Our MetadataField doesn't yet support either of these field types */
    /*    ris.addRisTag("J1", MetadataField.FIELD_JOURNAL_ALTERNATE_NAME); */
    /*    ris.addRisTag("L1", MetadataField.FIELD_FILE); */

     return ris;
  }

}
