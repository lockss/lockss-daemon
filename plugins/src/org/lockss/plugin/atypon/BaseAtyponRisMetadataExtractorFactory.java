/*
 * $Id: BaseAtyponRisMetadataExtractorFactory.java,v 1.1 2013-07-01 22:18:05 alexandraohlson Exp $
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

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.util.Logger;

/*
 * TY  - JOUR
T1  - Estimating the Eigenvalue Error of Markov State Models
AU  - Djurdjevac, N.
AU  - Sarich, M.
AU  - Schütte, C.
Y1  - 2012/01/01
PY  - 2012
DA  - 2012/01/01
N1  - doi: 10.1137/100798910
DO  - 10.1137/100798910
T2  - Multiscale Modeling & Simulation
JF  - Multiscale Modeling & Simulation
JO  - Multiscale Model. Simul.
SP  - 61
EP  - 81
VL  - 10
IS  - 1
PB  - Society for Industrial and Applied Mathematics
SN  - 1540-3459
M3  - doi: 10.1137/100798910
UR  - http://dx.doi.org/10.1137/100798910
Y2  - 2013/06/28
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
    
    RisMetadataExtractor ris = new RisMetadataExtractor();
    
    ris.addRisTag("JO", MetadataField.FIELD_JOURNAL_TITLE);
    ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    ris.addRisTag("Y1", MetadataField.FIELD_DATE);
    ris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
     return ris;
  }

}
