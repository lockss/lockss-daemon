/*
 * $Id: NatureHtmlMetadataExtractorFactory.java 40402 2015-03-10 22:37:41Z alexandraohlson $
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

package org.lockss.plugin.spandidos;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

import java.io.IOException;

public class Spandidos2020HtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(Spandidos2020HtmlMetadataExtractorFactory.class);


  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                               String contentType)
      throws PluginException {
    return new SpandidosHtmlMetadataExtractor();
  }

  public static class SpandidosHtmlMetadataExtractor implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();

    static {
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_abstract_html_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("keywords", MetadataField.FIELD_KEYWORDS);

      tagMap.put(MetadataField.ARTICLE_TYPE_JOURNALARTICLE, MetadataField.FIELD_ARTICLE_TYPE);
      tagMap.put(MetadataField.PUBLICATION_TYPE_JOURNAL, MetadataField.FIELD_PUBLICATION_TYPE);
    }


    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {

      ArticleMetadata am =
              new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);

      am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
      am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);

      am.cook(tagMap);

      if (am.isEmpty()) {
        return;
      }

      ArchivalUnit au = cu.getArchivalUnit();
      String metadataVolume = am.get(MetadataField.FIELD_VOLUME);
      log.debug3("Spandidos metadataVolume = " + metadataVolume);
      if (!StringUtils.isEmpty(metadataVolume)) {
        // Get the AU's volume name from the AU properties. This must be set
        TypedEntryMap tfProps = au.getProperties();
        String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());

        log.debug3("Spandidos AU_volume = " + AU_volume);
        Boolean isInAu =  ( (AU_volume != null) && (AU_volume.equals(metadataVolume)));

        if (isInAu) {
          log.debug3("Spandidos AU_volume = " + AU_volume + ", metadataVolume = " + metadataVolume);
          emitter.emitMetadata(cu, am);
        } else {
          log.debug3("Spandidos AU_volume = " + AU_volume + ", metadataVolume = " + metadataVolume);
        }
      }
    }
  }
}
