/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.psychiatryonline;

import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * Metadata on books
 */

public class PsychiatryOnlineMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(PsychiatryOnlineMetadataExtractorFactory.class);
  
  private static final String TITLE = "title";
  private static final String AUTHOR = "author";
  private static final String DOI = "doi";
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType)
          throws PluginException {
    return new PsychiatryOnlineMetadataExtractor();
  }
  
  public static class PsychiatryOnlineMetadataExtractor
    extends SimpleFileMetadataExtractor {
    
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put(TITLE, MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put(AUTHOR, MetadataField.FIELD_AUTHOR);
      tagMap.put(DOI, MetadataField.FIELD_DOI);
    }
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      
      ArticleMetadata am = new ArticleMetadata();
      String url = cu.getUrl();
      
      if        (url.contains("resourceID=5")) {
        am.putRaw(TITLE, "The American Psychiatric Publishing Textbook of Clinical Psychiatry (4th Edition)");
        am.putRaw(AUTHOR, "Edited by; Robert E. Hales, M.D.; M.B.A.; Stuart C. Yudofsky, M.D.");
        am.putRaw(DOI, "10.1176/appi.books.9781585622689");
      } else if (url.contains("resourceID=7")) {
        am.putRaw(TITLE, "Essentials of Clinical Psychopharmacology, Second Edition");
        am.putRaw(AUTHOR, "Edited by; Alan F. Schatzberg, M.D.; Charles B. Nemeroff, M.D., Ph.D.");
        am.putRaw(DOI, "10.1176/appi.books.9781585623167");
      } else if (url.contains("resourceID=29")) {
        am.putRaw(TITLE, "Manual of Clinical Psychopharmacology, Sixth Edition");
        am.putRaw(AUTHOR, "Alan F. Schatzberg, M.D.; Jonathan O. Cole, M.D.; Charles DeBattista, D.M.H., M.D.");
        am.putRaw(DOI, "10.1176/appi.books.9781585622825");
      } else {
        am.putRaw(TITLE, "Metadata not available");
        am.putRaw("metadata", "not available: see plugins/src/org/lockss/" +
            "plugin/psychiatryonline/PsychiatryOnlineMetadataExtractorFactory.java");
      }
      am.cook(tagMap);
      
      return am;
    }
  }
}
