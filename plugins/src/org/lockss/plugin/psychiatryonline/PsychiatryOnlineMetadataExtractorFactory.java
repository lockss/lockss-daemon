/*
 * $Id$
 */

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
