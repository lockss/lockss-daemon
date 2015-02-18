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

package org.lockss.plugin.taylorandfrancis;

import java.io.IOException;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.plugin.taylorandfrancis.TaylorAndFrancisHtmlMetadataExtractorFactory;

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
public class TafRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(TafRisMetadataExtractorFactory.class);
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    
    log.debug3("Inside TaylorAndFrancis Metadata extractor factory for RIS files");
    TaylorAndFrancisRisMetadataExtractor tfris = new TaylorAndFrancisRisMetadataExtractor();
    tfris.addRisTag("A1", MetadataField.FIELD_AUTHOR); // in case they use this 
    // Do not add the "UR" tag because it points to dx.doi.org - not an access.url in the AU      
    return tfris;
  }
  
  public static class TaylorAndFrancisRisMetadataExtractor extends RisMetadataExtractor {

    // we have to override this to add functionality after basic extraction
    @Override
    public void extract(MetadataTarget target,
                        CachedUrl cu,
                        FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      ArticleMetadata am = extract(target, cu); //extract but do some analysis before emitting

      /* if the cooked data isn't complete, we could try to pick up from secondary tags in raw data */
      if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
        if (am.getRaw("T2") != null) {
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, am.getRaw("T2"));
        } else if (am.getRaw("JO") != null) {
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, am.getRaw("JO")); // might be unabbreviated version
        }
      } 
      if (am.get(MetadataField.FIELD_DATE) == null) {
        if (am.getRaw("Y1") != null) { // if DA wasn't there, use Y1
          am.put(MetadataField.FIELD_DATE, am.getRaw("Y1"));
        }
      }
      
      /* Before emitting, try to verify the article is part of this AU
       * to avoid emitting for overcrawled articles
       * The method lives in the html version of the extractor factory
       */
      ArchivalUnit TandF_au = cu.getArchivalUnit();
      if (TaylorAndFrancisHtmlMetadataExtractorFactory.checkMetadataAgainstTdb(TandF_au, am)) {
        emitter.emitMetadata(cu, am);
      }
    }
  }
}
