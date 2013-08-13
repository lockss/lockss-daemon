/*
 * $Id: TaylorAndFrancisRisMetadataExtractorFactory.java,v 1.1 2013-08-13 21:39:25 alexandraohlson Exp $
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import java.io.IOException;

import org.lockss.config.TdbAu;
import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

/*
 * TY  - JOUR
TY  - JOUR
T1  - Title of Article
AU  - author
AU  - other author
Y1  - may be same as DA, maybe not
PY  - 2011
DA  - publication date
N1  - doi: 10.1080/19419899.2010.534489
DO  - 10.1080/19419899.2010.534489
T2  - journal title (alternate)
JF  - journal title
JO  - may be abbreviated journal title
SP  - 159
EP  - 180
VL  - 2
IS  - 2
PB  - publisher or imprint (Routledge)
SN  - 1941-9899
M3  - doi: 10.1080/19419899.2010.534489
UR  - http://dx.doi.org/10.1080/19419899.2010.534489
Y2  - 2013/07/26
ER  - 
 * 
 */
public class TaylorAndFrancisRisMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("TaylorAndFrancisRisMetadataExtractorFactory");
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    
    log.debug3("Inside TaylorAndFrancis Metadata extractor factory for RIS files");
    
    TaylorAndFrancisRisMetadataExtractor tfris = new TaylorAndFrancisRisMetadataExtractor();
    
    tfris.addRisTag("A1", MetadataField.FIELD_AUTHOR); // in case they use this 
    tfris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
     return tfris;
  }
  
  public static class TaylorAndFrancisRisMetadataExtractor
  extends RisMetadataExtractor {

    // we have to override this to add functionality after basic extraction
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      ArticleMetadata am = extract(target, cu); //extract but do some analysis before emitting

      /* if the cooked data isn't complete, we could try to pick up from secondary tags in raw data */
      if (am.get(MetadataField.FIELD_JOURNAL_TITLE) == null) {
        if (am.getRaw("T2") != null) {
          am.put(MetadataField.FIELD_JOURNAL_TITLE, am.getRaw("T2"));
        } else if (am.getRaw("JO") != null) {
          am.put(MetadataField.FIELD_JOURNAL_TITLE, am.getRaw("JO")); // might be unabbreviated version
        }
      } 
      if (am.get(MetadataField.FIELD_DATE) == null) {
        if (am.getRaw("Y1") != null) { // if DA wasn't there, use Y1
          am.put(MetadataField.FIELD_DATE, am.getRaw("Y1"));
        }
      }

      /* BIG IMPORTANT COMMENT */
      /* Taylor & Francis has opaque URLs and in the event of accidental overcrawling, an article could end
       * up getting collected that isn't actually in this AU. 
       * Verify the metadata against the journal title and volume of the AU to make sure that we don't emit metadata
       * for any articles that shouldn't be in this AU.  It's a final last-ditch protective check.
       * If I can't get any valid metadata then don't emit because we can't verify it is in the AU
       */
      String jTitle = am.get(MetadataField.FIELD_JOURNAL_TITLE);
      String vName = am.get(MetadataField.FIELD_VOLUME);
      Boolean definitelyInAU = true;

      ArchivalUnit TandF_au = cu.getArchivalUnit();
      // Get the AU's volume name from the AU properties. This must be set

      //this extra step to get the AU_volume is temporary for debugging
      TypedEntryMap tfProps = TandF_au.getProperties();
      String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());
      //String AU_volume = TandF_au.getProperties().getString(ConfigParamDescr.VOLUME_NAME.getKey());

      // Get the AU's journal_title from the tdbconfig portion of the AU
      TdbAu tf_tau = TandF_au.getTdbAu();
      String AU_journal_title = (tf_tau == null) ? null : tf_tau.getJournalTitle();

      // If we couldn't extract journal title or volume from metadata, we can't verify this is in the correct AU
      definitelyInAU = !(jTitle.isEmpty() && vName.isEmpty());

      // If we're still good and if we got a journal title, compare it to the AU journal title
      // see if the jTitle is equal to or a substring of the AU title as the AU title may contain the subtitle
      if (definitelyInAU && !(jTitle.isEmpty())) {
        definitelyInAU =  ( (AU_journal_title != null) && (AU_journal_title.toUpperCase().indexOf(jTitle.toUpperCase()) != -1));
      }
      // If we're still good and if we got a volume name, compare it to the AU volume name
      if (definitelyInAU && !(vName.isEmpty())) {
        definitelyInAU =  ( (AU_volume != null) && (AU_volume.equals(vName)));
      }
      if (definitelyInAU) {
        // Well we might as well pick up and fill in this since we're already peeking in the AU
        String AU_issn = (tf_tau == null) ? null : tf_tau.getIssn();
        String AU_eissn = (tf_tau == null) ? null : tf_tau.getEissn();
        if ( (AU_issn != null) && !AU_issn.isEmpty()) am.put(MetadataField.FIELD_ISSN, AU_issn);
        if ( (AU_eissn != null) && !AU_eissn.isEmpty()) am.put(MetadataField.FIELD_EISSN, AU_eissn);
        emitter.emitMetadata(cu, am);
      } 
    }
  }
}
