/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.metapress;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.ojs2.OJS2HtmlMetadataExtractorFactory.OJS2HtmlMetadataExtractor;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

/*
 * TY  - JOUR
JF  - Electronic Government, an International Journal 
T1  - Evaluating usability, user satisfaction and intention to revisit for successful e-government websites
VL  - 8
IS  - 1
SP  - 1
EP  - 19
PY  - 2011/01/01/
UR  - http://dx.doi.org/10.1504/EG.2011.037694
DO  - 10.1504/EG.2011.037694
AU  - Byun, Dae-Ho
AU  - Finnie, Gavin
 */
public class MetaPressHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("MetaPressHtmlMetadataExtractorFactory");
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                 String contentType)
      throws PluginException {
    log.debug3("Inside Metadata extractor factory");
    
    RisMetadataExtractor ris = new RisMetadataExtractor();
  
     return ris;
    }
}
