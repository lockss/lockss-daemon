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

package org.lockss.plugin.peerj;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.util.Logger;

/*
 * Extracts metadata from PeerJ RIS file:
 *  TY  - JOUR
 *  UR  - http://dx.doi.org/10.7717/xxxxx.173
 *  DO  - 10.7717/xxxxx.173
 *  TI  - article title
 *  AU  - A., Author1
 *  AU  - B., Author2
 *  A2  - C., Secondary Author
 *  DA  - 2013/09/26
 *  PY  - 2013
 *  KW  - kwd1
 *  KW  - kwd2
 *  KW  - kwd3
 *  KW  - kwd4
 *  KW  - kwd5
 *  KW  - kwd6
 *  AB  - article abstract
 *  VL  - 1
 *  SP  - e173
 *  T2  - xxxxx
 *  JO  - xxxxx
 *  J2  - xxxxx
 *  SN  - 1111-2222
 *  ER  - 
 */

  public class PeerJRisMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
    
    static Logger log = 
        Logger.getLogger(PeerJRisMetadataExtractorFactory.class);
    
    public FileMetadataExtractor createFileMetadataExtractor(
        MetadataTarget target, String contentType) throws PluginException {
    
      log.debug3("Inside PeerJ RIS metadata extractor factory");
      RisMetadataExtractor pRisMe = new RisMetadataExtractor();

      pRisMe.addRisTag("TI", MetadataField.FIELD_ARTICLE_TITLE);
      pRisMe.addRisTag("A2", MetadataField.FIELD_AUTHOR);
      pRisMe.addRisTag("KW", MetadataField.FIELD_KEYWORDS);
      pRisMe.addRisTag("AB", MetadataField.FIELD_ABSTRACT);

    return pRisMe;
  }
    
}
