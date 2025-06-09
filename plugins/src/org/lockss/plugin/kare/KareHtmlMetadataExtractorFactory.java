package org.lockss.plugin.kare;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class KareHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(KareHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new KareHtmlMetadataExtractor();
  }

  public static class KareHtmlMetadataExtractor
          implements FileMetadataExtractor {

    private static MultiMap tagMap = new MultiValueMap();

    /*
      <meta name="citation_title" content="A new treatment modality in piriformis syndrome: Ultrasound guided dry needling treatment">
      <meta name="citation_author" content="Bağcıer, Fatih">
      <meta name="citation_author" content="Tufanoğlu, Fatih Hakan">
      <meta name="citation_journal_title" content="Agri">
      <meta name="citation_volume" content="32">
      <meta name="citation_issue" content="3">
      <meta name="citation_firstpage" content="175">
      <meta name="citation_lastpage" content="176">
      <meta name="citation_language" content="en">
      <meta name="keywords" content="Dry needling, Piriformis sendromu, Ultrasound">
      <title>A new treatment modality in piriformis syndrome: Ultrasound guided dry needling treatment [Ağrı]</title>
     */
    static {
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("citation_keywords", MetadataField.FIELD_KEYWORDS);
      tagMap.put("citation_journal_publisher", MetadataField.FIELD_PUBLISHER);
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {
      ArticleMetadata am =
              new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      ArchivalUnit au = cu.getArchivalUnit();


      String tdbVolume = null;
      String citationVolume = null;

      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();

      if (tdbau != null) {
        tdbVolume = tdbau.getVolume();
      }

      citationVolume = am.get(MetadataField.FIELD_VOLUME);

      log.debug3("Check metadata citationVolume = " + citationVolume + ", tdbVolume = " + tdbVolume);

      if (tdbVolume == null || citationVolume == null || !tdbVolume.equals(citationVolume)) {
        log.debug3("Emit metadata falied maching volume, citationVolume = " + citationVolume + ", tdbVolume = " + tdbVolume);
        return;
      }
      emitter.emitMetadata(cu, am);
    }
  }
}