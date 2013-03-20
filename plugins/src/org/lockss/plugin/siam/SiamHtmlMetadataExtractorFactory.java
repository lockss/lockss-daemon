/*
 * $Id: SiamHtmlMetadataExtractorFactory.java,v 1.1 2013-03-20 17:56:48 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.siam;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

public class SiamHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("SiamHtmlMetadataExtractorFactory");

  @Override
  public FileMetadataExtractor
    createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new SiamHtmlMetadataExtractor();
  }

  /*
   * <meta name="dc.Title" content="Title of Article"></meta>
   * <meta name="dc.Creator" content="D. Author"></meta>
   * <meta name="dc.Creator" content="S. Author2"></meta>
   * <meta name="dc.Subject" content="weighted regularity; elliptic problem; oscillatory diffusion; $hp$ finite elements; 65N30; 35B65; 35J57"></meta>
   * <meta name="dc.Description" content="Long test summary of article, probably taken directly from the adstract..."></meta
   * ><meta name="dc.Publisher" content="Society for Industrial and Applied Mathematics"></meta>
   * <meta name="dc.Date" scheme="WTN8601" content="2012-07-05"></meta>
   * <meta name="dc.Type" content="research-article"></meta>
   * <meta name="dc.Format" content="text/HTML"></meta>
   * <meta name="dc.Identifier" scheme="publisher" content="81839"></meta>
   * <meta name="dc.Identifier" scheme="doi" content="10.1137/10081839X"></meta>
   * <meta name="dc.Source" content="http://dx.doi.org/10.1137/10081839X"></meta>
   * <meta name="dc.Language" content="en"></meta>
   * <meta name="dc.Coverage" content="world"></meta>
   * <meta name="keywords" content="weighted regularity, elliptic problem, oscillatory diffusion, $hp$ finite elements, 65N30, 35B65, 35J57"></meta>
   */
  public static class SiamHtmlMetadataExtractor
    implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      // This could be more than one type of MetadataField.FIELD_* 

      tagMap.put("dc.Date", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.Date", MetadataField.FIELD_DATE);

      tagMap.put("dc.Creator", MetadataField.DC_FIELD_CREATOR);
      tagMap.put("dc.Creator", MetadataField.FIELD_AUTHOR);

      tagMap.put("dc.Title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.Title", MetadataField.FIELD_ARTICLE_TITLE);
      
      tagMap.put("dc.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      tagMap.put("dc.Publisher",  MetadataField.FIELD_PUBLISHER);
      
      tagMap.put("dc.Type", MetadataField.DC_FIELD_TYPE);
      tagMap.put("dc.Format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("dc.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("dc.Coverage",MetadataField.DC_FIELD_COVERAGE);
      tagMap.put("dc.Rights", MetadataField.DC_FIELD_RIGHTS);
      tagMap.put("dc.Source", MetadataField.DC_FIELD_SOURCE);
      
      tagMap.put("keywords",  new MetadataField(MetadataField.FIELD_KEYWORDS, MetadataField.splitAt(",")));
    }

    private String base_url;

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am =
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);

      am.cook(tagMap);

      // publisher name does not appear anywhere on the page in this form
      if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
        am.put(MetadataField.FIELD_PUBLISHER, "Society for Industrial and Applied Mathematics");
      }

      // if the doi isn't in the metadata, we can still get it from the filename
      if (am.get(MetadataField.FIELD_DOI) == null) {

        /*matches() is anchored so must create complete pattern or else use .finds() */
        /* URL is "<base>/doi/abs/(<doi1st>/<doi2nd>) */
        base_url = cu.getArchivalUnit().getConfiguration().get("base_url");
        String patternString = "^" + base_url + "doi/[^/]+/([^/]+)/([^/]+)$";
        Pattern ABSTRACT_PATTERN = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        String url = cu.getUrl();
        Matcher mat = ABSTRACT_PATTERN.matcher(url);

        if (mat.matches()) {
          log.debug3("Pull DOI from URL " + mat.group(1) + "." + mat.group(2));
          am.put(MetadataField.FIELD_DOI, mat.group(1) + "/" + mat.group(2));
        }
      }
      emitter.emitMetadata(cu, am);
    }
  }
}