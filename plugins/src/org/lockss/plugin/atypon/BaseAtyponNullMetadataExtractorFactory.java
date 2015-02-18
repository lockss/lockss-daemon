/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon;

import java.io.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * A metadata extractor so we can NOT emit when an article is found for which
 * there is no existing metadata extractor. 
 * Currently the daemon just emits the appropriate tdb defaults.
 * For Atypon, we hit this case if we have ONLY a pdf mime-type associated
 * with an articlefiles.  This occurs with a dead-end overcrawl due to an 
 * in-line link to a PDF (usually of an original article from a corrigendum)
 * @author alexohlson
 *
 */
public class BaseAtyponNullMetadataExtractorFactory 
  implements FileMetadataExtractorFactory {
  
  public FileMetadataExtractor 
    createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new BaseAtyponNullMetadataExtractor();
  }

  public static class BaseAtyponNullMetadataExtractor
    implements FileMetadataExtractor {
    static Logger log = Logger.getLogger(BaseAtyponNullMetadataExtractor.class);
 

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      log.debug("Suppressing emit of metadata in Null extractor: " + cu.getUrl());
      // do nothing, do not allow TDB info to get used as default
      // by not emitting
    }
    
  }
}