/*
 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pion;

import java.io.IOException;

import org.lockss.config.TdbAu;
import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * Augment the standard RisMetadataExtractor to pull metadata from a .ris file
 * found: <base_url>/<vol#>/<issue#>/<year>/<abstract_base>.ris
 * The Pion RIS file is a little non-standard, so add to the map. It looks like this:
 * 
 * TY  - JOUR
 * A1  - Lowe, N
 * A1  - Hagan, J
 * A1  - Iskander, N
 * Y1  - 2010
 * T1  - Revealing talent: informal skills intermediation as an emergent pathway to immigrant labor market incorporation
 * JO  - Environment and Planning A
 * SP  - 205
 * EP  - 222
 * VL  - 42
 * IS  - 1
 * UR  - http://www.envplan.com/abstract.cgi?id=a4238
 * PB  - Pion Ltd
 * SN  - 
 * N2  - This is a description of the article.
 * ER  -
 */
public class PionRisMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(PionRisMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    
    log.debug3("Inside Pion Metadata extractor factory");
    
    PionRisMetadataExtractor ris = new PionRisMetadataExtractor();

    /* Pion seems to use different tags for these RIS fields */
    ris.addRisTag("JO", MetadataField.FIELD_PUBLICATION_TITLE); // requires
    ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    ris.addRisTag("Y1", MetadataField.FIELD_DATE);
    ris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    ris.addRisTag("N2", MetadataField.FIELD_ABSTRACT);
    /* Our MetadataField doesn't yet support either of these field types */
    /*    ris.addRisTag("J1", MetadataField.FIELD_JOURNAL_ALTERNATE_NAME); */
    /*    ris.addRisTag("L1", MetadataField.FIELD_FILE); */

     return ris;
  }
  public static class PionRisMetadataExtractor
  extends RisMetadataExtractor {

    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      ArticleMetadata md = extract(target, cu);
      
      if (md != null) {
        if (!md.hasValidValue(MetadataField.FIELD_DOI)) {
          // fill in DOI from accessURL
          // http://www.envplan.com/abstract.cgi?id=a42117
          // -> doi=10.1068/a42117
          String accessUrl = md.get(MetadataField.FIELD_ACCESS_URL);
          if (accessUrl != null) {
            int i = accessUrl.indexOf("id=");
            if (i > 0) {
              String doi = "10.1068/" +accessUrl.substring(i+3);
              md.put(MetadataField.FIELD_DOI, doi);
            }
          }
        }
        completeMetadata(cu, md);
        emitter.emitMetadata(cu, md);
      }
    }
  };
  
  public static void completeMetadata(CachedUrl cu, ArticleMetadata am) {
    // Pick up some information from the TDB if not in the cooked data
    TdbAu tdbau = cu.getArchivalUnit().getTdbAu(); // returns null if titleConfig is null 
    if (tdbau != null) {
      if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
        // We can try to get the publishger from the tdb file.  This would be the most accurate
        String publisher = tdbau.getPublisherName();
        if (publisher != null) {
          am.put(MetadataField.FIELD_PUBLISHER, publisher);
        }
      }
      if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
        String journal_title = tdbau.getPublicationTitle();
        if (journal_title != null) {
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, journal_title);
        }
      }
    }
  }
}
