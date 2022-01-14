package org.lockss.plugin.interresearch;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

public class InterResearchHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(InterResearchHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new InterResearchHtmlMetadataExtractor();
  }

  public static class InterResearchHtmlMetadataExtractor
      extends SimpleHtmlMetaTagMetadataExtractor {

    // Map BePress-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
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
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = super.extract(target, cu);
      am.cook(tagMap);
      return am;
    }

  }
}

/* <meta name="citation_title" content="REVIEW  Assessing North Atlantic right whale health: threats, and development of tools critical for conservation of the species">
<meta name="citation_publication_date" content="2021/02/25">
<meta name="citation_journal_title" content="Diseases of Aquatic Organisms">
<meta name="citation_issn" content="0177-5103">
<meta name="citation_volume" content="143">
<meta name="citation_doi" content="10.3354/dao03578">
<meta name="citation_firstpage" content="205">
<meta name="citation_lastpage" content="226">
<meta name="citation_author" content="Michael J. Moore">
<meta name="citation_author" content="Teresa K. Rowles">
<meta name="citation_author" content="Deborah A. Fauquier">
<meta name="citation_author" content="Jason D. Baker">
<meta name="citation_author" content="Ingrid Biedron">
<meta name="citation_author" content="John W. Durban">
<meta name="citation_author" content="Philip K. Hamilton">
<meta name="citation_author" content="Allison G. Henry">
<meta name="citation_author" content="Amy R. Knowlton">
<meta name="citation_author" content="William A. McLellan">
<meta name="citation_author" content="Carolyn A. Miller">
<meta name="citation_author" content="Richard M. Pace III">
<meta name="citation_author" content="Heather M. Pettis">
<meta name="citation_author" content="Stephen Raverty">
<meta name="citation_author" content="Rosalind M. Rolland">
<meta name="citation_author" content="Robert S. Schick">
<meta name="citation_author" content="Sarah M. Sharp">
<meta name="citation_author" content="Cynthia R. Smith">
<meta name="citation_author" content="Len Thomas">
<meta name="citation_author" content="Julie M. van der Hoop">
<meta name="citation_author" content="Michael H. Ziccardi">
<meta name="citation_keywords" content="Right whale; Health; Trauma; Reproduction; Stressor; Cumulative effects">
<meta name="citation_journal_publisher" content="Inter-Research">

<meta name="citation_journal_abbreviation" content="Dis Aquat Org">
<meta name="citation_journal_abbreviation" content="DAO">
<meta name="citation_issn" content="1616-1580">
<meta name="citation_abstract_html_url" content="https://www.int-res.com/abstracts/dao/v143/p205-226/">
<meta name="citation_xml_url" content="https://www.int-res.com/articles/xml/dao/143/d143p205.xml">
<meta name="citation_pdf_url" content="https://www.int-res.com/articles/feature/d143p205.pdf">
<meta name="citation_author_email" content="mmoore@whoi.edu">

 */