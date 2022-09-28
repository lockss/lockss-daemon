package org.lockss.plugin.taar;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class TaarHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType)
      throws PluginException {
    return new TaarHtmlMetadataExtractor();
  }

  public static class TaarHtmlMetadataExtractor implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      // YYYY/MM
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_abstract", MetadataField.FIELD_ABSTRACT);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      ArticleMetadata am =
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      emitter.emitMetadata(cu, am);
    }

  }
}
