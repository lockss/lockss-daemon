package org.lockss.plugin.ejobsat;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.wroclawmedicaluniversity.WroclawMedicalUniversityMetadataExtractorFactory;

import java.io.IOException;

public class EuropeanJournalBusinessScienceTechnologyMetadataExtractorFactory implements FileMetadataExtractorFactory {

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new WroclawMedicalUniversityMetadataExtractorFactory.WroclawMedicalUniversityHtmlMetadataExtractor();
    }

    /*
    <meta name="citation_journal_title"	content="European Journal of Business Science and Technology">
	<meta name="citation_issn"			content="23366494">  // this is the print issn
	<meta name="citation_issn"			content="26947161">
	<meta name="citation_authors"			content="Firdmuc, Jarko; Schreiber, Philipp; Siddiqui, Martin">
	<meta name="citation_title"			content="Intangible Assets and the Determinants of a Single Bank Relation of German SMEs">
	<meta name="citation_date"			content="2018/7/31">
	<meta name="citation_volume"			content="4">
	<meta name="citation_issue"			content="1">
	<meta name="citation_firstpage"		content="5">
	<meta name="citation_lastpage"		content="30">
	<meta name="citation_abstract_html_url"	content="http://ejobsat.cz/doi/10.11118/ejobsat.v4i1.130.html">
	<meta name="citation_fulltext_html_url"	content="http://ejobsat.cz/doi/10.11118/ejobsat.v4i1.130.html">
	<meta name="citation_pdf_url"			content="http://ejobsat.cz/doi/10.11118/ejobsat.v4i1.130.pdf">
	<meta name="citation_doi"			content="10.11118/ejobsat.v4i1.130">

	<meta name="dc.Identifier" scheme="URI"	content="doi:10.11118/ejobsat.v4i1.130">
	<meta name="dc.Identifier" scheme="URI"	content="https://doi.org/10.11118/ejobsat.v4i1.130">
	<meta name="dc.Source"				content="http://ejobsat.cz/doi/10.11118/ejobsat.v4i1.130.html">
	<meta name="dc.Title"				content="Intangible Assets and the Determinants of a Single Bank Relation of German SMEs">
	<meta name="dc.Creator" 				content="Firdmuc, Jarko">
	<meta name="dc.Creator" 				content="Schreiber, Philipp">
	<meta name="dc.Creator" 				content="Siddiqui, Martin">
	<meta name="dc.Keywords"				content="relationship banking, SME, bank lending, capital structure, intangible assets">
	<meta name="dc.Date"				content="2018/7/31">
	<meta name="dc.Publisher"			content="European Journal of Business Science and Technology">
	<meta name="dc.Description"			content="We focus on the determinants and potential benefits of relationship banking. Based on the existing literature and the unique role intangible assets play regarding firms' capital structure, we test two hypotheses using rich data on firm-bank relationships in Germany. We show that firstly, a high share of intangible assets does not worsen the access of firms to debt financing. And secondly, firms with a high share of intangible assets are statistically significantly more likely to choose an exclusive and persistent bank relation.">
	<meta name="dc.Rights"				content="&copy; European Journal of Business Science and Technology, 2018">
	<meta name="dc.Type"				content="Text">
	<meta name="dc.Format" scheme="IMT"	content="application/pdf">
	<meta name="dc.Language"				content="en">
     */
    public static class WroclawMedicalUniversityHtmlMetadataExtractor
            extends SimpleHtmlMetaTagMetadataExtractor {
        private static MultiMap tagMap = new MultiValueMap();
        static {
            tagMap.put("citation_authors", MetadataField.FIELD_AUTHOR);
            tagMap.put("citation_date", MetadataField.FIELD_DATE);
            tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
            tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
            tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
            tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
            tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
            tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
            tagMap.put("citation_doi", MetadataField.FIELD_DOI);
            tagMap.put("citation_issn", MetadataField.FIELD_EISSN);   //the second one
            tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
            tagMap.put("dc.Publisher", MetadataField.DC_FIELD_PUBLISHER);
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


