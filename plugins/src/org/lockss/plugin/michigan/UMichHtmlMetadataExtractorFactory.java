/*
 * $Id$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.michigan;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;


//<meta name="citation_title" content="Printing and Prophecy: Prognostication and Media Change 1450-1550">
//<meta name="citation_author" content="Green, Jonathan">
//<meta name="citation_publication_date" content="2011">
//<meta name="citation_online_date" content="2011">
//<meta name="citation_isbn" content="978-0-472-11783-3">
//<meta name="citation_isbn" content="978-0-472-02758-3">
//<meta name="citation_isbn" content="978-0-472-90074-9">
//<meta name="citation_doi" content="doi:10.3998/mpub.3209249">
//<meta name="citation_publisher" content="University of Michigan Press">

public class UMichHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(
		  UMichHtmlMetadataExtractorFactory.class);
  
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new UMichHtmlMetadataExtractor();
  }

  public static class UMichHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER); // XXX
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author",
          new MetadataField(MetadataField.FIELD_AUTHOR,
                            MetadataField.splitAt(",")));
    }
    
    
  }
  
}
