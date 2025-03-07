package org.lockss.plugin.emhswiss;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;

import java.io.IOException;

public class EmhSwissMedicalTriggeredPluginHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new EmhSwissMedicalTriggeredPluginHtmlMetadataExtractor();
    }

    /*
      <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no" />
      <meta name="citation_publisher" content="EMH Swiss Medical Publishers" />
      <meta name="citation_journal_title" content="Cardiovascular Medicine" />
      <meta name="citation_issn" content="1664-204X" />
      <meta name="citation_volume" content="019" />
      <meta name="citation_issue" content="01" />
      <meta name="citation_firstpage" content="28" />
      <meta name="citation_lastpage" content="33" />
      <meta name="citation_title" content="A patient with arrhythmias and infective cardiac disease" />
      <meta name="citation_doi" content="10.4414/cvm.2016.00386" />
      <meta name="citation_year" content="2016" />
      <meta name="citation_online_date" content="2016-01-20" />
      <meta name="citation_public_url" content="https://emhsmp2025.clockss.org/emhsmp/cvm/019/01/00386/index.html" />
      <meta name="citation_pdf_url" content="https://emhsmp2025.clockss.org/emhsmp/cvm/019/01/00386/article_pdf/cvm-2016-00386.pdf" />
      <meta name="citation_author" content="Giuseppe Cocco" />
      <meta name="citation_author_email" content="praxis@cocco.ch" />
      <meta name="citation_author_institution" content="Private Office, Postfach 119, Marktkasse 10A, CH-4310, Rheinfelden (Argau), SWITZERLAND; praxis@cocco.ch" />
      <meta name="citation_author" content="Philipp Amiet" />
      <meta name="citation_author_institution" content="Private medical office" />
     */
    public static class EmhSwissMedicalTriggeredPluginHtmlMetadataExtractor
            extends SimpleHtmlMetaTagMetadataExtractor {
        private static MultiMap tagMap = new MultiValueMap();
        static {
            tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
            tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
            tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
            tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
            tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
            tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
            tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
            tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("citation_doi", MetadataField.FIELD_DOI);
            tagMap.put("citation_year", MetadataField.FIELD_DATE);
            tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
            tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
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



