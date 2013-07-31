/*
 * $Id: BaseAtyponRisMetadataExtractorFactory.java,v 1.2 2013-07-31 21:43:58 alexandraohlson Exp $
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

package org.lockss.plugin.atypon;

import java.io.IOException;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

/*
 *  NOTE: I have found the following:
 *  JO is often used (incorrectly) but consistently as the abbreviated form of the journal title, use JF and then T2 in preference
 *  Y1 usually is the same as DA, but not always, use DA if it's there
TY  - JOUR
T1  - <article title>
AU  - <author>
AU  - <other author>
Y1  - <date, often same as DA, often slightly later>
PY  - <year of pub>
DA  - <date of pub>
N1  - doi: 10.1137/100798910
DO  - 10.1137/100798910 
T2  - <journal title>
JF  - <journal title>
JO  - <abbreviated journal title>
SP  - <start page>
EP  - <end page>
VL  - <volume>
IS  - <issue>
PB  - <publisher but possibly imprint>
SN  - <issn>
M3  - doi: 10.1137/100798910
UR  - http://dx.doi.org/10.1137/100798910
Y2  - <later date - meaning?>
ER  -  
 * 
 */
public class BaseAtyponRisMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("BaseAtyponRisMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {

    log.debug3("Inside Base Atypon Metadata extractor factory for RIS files");

    BaseAtyponRisMetadataExtractor ba_ris = new BaseAtyponRisMetadataExtractor();

    ba_ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    ba_ris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    return ba_ris;
  }

  public static class BaseAtyponRisMetadataExtractor
  extends RisMetadataExtractor {

    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      ArticleMetadata am = extract(target, cu); 

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
      emitter.emitMetadata(cu, am);
    }

  }

}
