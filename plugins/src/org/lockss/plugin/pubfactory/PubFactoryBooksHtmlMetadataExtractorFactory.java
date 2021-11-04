package org.lockss.plugin.pubfactory;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class PubFactoryBooksHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {


  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType)
      throws PluginException {
    return new PubfactoryBooksHtmlMetadataExtractor();
  }

  public static class PubfactoryBooksHtmlMetadataExtractor
      implements FileMetadataExtractor {

    // Map HighWire HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_authors", new MetadataField(
          MetadataField.FIELD_AUTHOR, MetadataField.splitAt(";")));
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_xml_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
      tagMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
      tagMap.put("dc.identifier", MetadataField.DC_FIELD_IDENTIFIER);

    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am =
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      if ((am.get(MetadataField.FIELD_DOI) == null) && (am.get(MetadataField.DC_FIELD_IDENTIFIER)!= null )){
        am.put(MetadataField.FIELD_DOI,am.get(MetadataField.DC_FIELD_IDENTIFIER));
      }
      // leave this method here in case we need to make modifications
      // note that access.url isn't set to allow for default full_text_cu value (pdf)
      emitter.emitMetadata(cu, am);
    }

  }
}