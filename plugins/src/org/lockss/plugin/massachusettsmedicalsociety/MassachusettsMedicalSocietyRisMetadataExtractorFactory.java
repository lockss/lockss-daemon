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

package org.lockss.plugin.massachusettsmedicalsociety;

import java.io.IOException;

import org.lockss.config.TdbAu;
import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.extractor.FileMetadataExtractor.Emitter;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 * Uses the standard RisMetadataExtractor to pull metadata from a .ris file
 * found: www.nejm.org/action/downloadCitation?format=ris&doi=10.1056%2FNEJM197901183000301&include=cit&direct=checked
 * The RIS file should look something like this:
 * 
 * TY  - JOUR
 * T1  - Viral Hepatitis, Type B
 * AU  - Krugman, Saul
 * AU  - Overby, Lacy R.
 * AU  - Mushahwar, Isa K.
 * AU  - Ling, Chung-Mei
 * AU  - Frösner, Gert G.
 * AU  - Deinhardt, Friedrich
 * Y1  - 1979/01/18
 * PY  - 1979
 * DA  - 1979/01/18
 * N1  - doi: 10.1056/NEJM197901183000301
 * DO  - 10.1056/NEJM197901183000301
 * T2  - New England Journal of Medicine
 * JF  - New England Journal of Medicine
 * JO  - N Engl J Med
 * SP  - 101
 * EP  - 106
 * VL  - 300
 * IS  - 3
 * PB  - Massachusetts Medical Society
 * SN  - 0028-4793
 * M3  - doi: 10.1056/NEJM197901183000301
 * UR  - http://dx.doi.org/10.1056/NEJM197901183000301
 * Y2  - 2012/02/29
 * ER  - 
 */
public class MassachusettsMedicalSocietyRisMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(MassachusettsMedicalSocietyRisMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    log.debug3("Inside MassMedical Metadata extractor factory");
    MassachusettsMedicalSocietyRisMetadataExtractor ris = new MassachusettsMedicalSocietyRisMetadataExtractor();
    
    ris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    ris.addRisTag("JF", MetadataField.FIELD_PUBLICATION_TITLE);
    
    return ris;
  }
  public static class MassachusettsMedicalSocietyRisMetadataExtractor 
    extends RisMetadataExtractor {
    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      ArticleMetadata md = extract(target, cu);
      final String urlPrefix = "http://dx.doi.org/";
      final String doiPrefix = "doi: ";
      final String nejmPrefix = "http://www.nejm.org/doi/full/";
      log.debug3("MassMedicalRisMetadataExtractor:extract");
      if (md != null) {
        String accessUrl = md.get(MetadataField.FIELD_ACCESS_URL);
        log.debug3("  access.url=" + accessUrl);

        if (!md.hasValidValue(MetadataField.FIELD_DOI)) {
          // fill in DOI from accessURL
          // http://dx.doi.org/10.1056/NEJMe1211736
          // -> doi: 10.1056/NEJMe1211736
          if (accessUrl != null) {
            if (accessUrl.startsWith(urlPrefix)) {
              String doi = doiPrefix + accessUrl.substring(urlPrefix.length());
              md.put(MetadataField.FIELD_DOI, doi);
            }
          }
        } else {        // doi is valid, no access url
          if (accessUrl == null) {
            String doi = md.get(MetadataField.FIELD_DOI);
            // doi: 10.1056/NEJMe1211736 
            // -> http://www.nejm.org/doi/pdf/10.1056/NEJMe1211736
            StringBuilder newUrl = new StringBuilder(nejmPrefix);
            if (doi != null) {
              newUrl.append(doi);
              log.debug3(" newUrl = " +newUrl);
              md.put(MetadataField.FIELD_ACCESS_URL, newUrl.toString());
            }
          } 
        }
        log.debug3("  access.url=" + md.get(MetadataField.FIELD_ACCESS_URL));
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
        // Get the publishger from the tdb file.  This would be the most accurate
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
    log.debug3(am.toString());
  }
}
