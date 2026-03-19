package org.lockss.plugin.rhi;

import org.lockss.config.Configuration;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelictHominoidInquiryMetadataExtractorFactory implements ArticleMetadataExtractorFactory {

    private static final Logger log = Logger.getLogger(RelictHominoidMetadataExtractor.class);

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
        return new RelictHominoidMetadataExtractor();
    }

    public static class RelictHominoidMetadataExtractor implements ArticleMetadataExtractor {
        private static final Logger log = Logger.getLogger(RelictHominoidMetadataExtractor.class);

        // Updated Regex: Handles multiple spaces and is non-greedy on the title
        private static final Pattern TITLE_RE = Pattern.compile(
                "^(.*?)\\s+(\\d+):(\\d+)(?:-(\\d+))?\\s+\\((\\d{4})\\)$",
                Pattern.CASE_INSENSITIVE);

        @Override
        public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
                throws IOException, PluginException {

            ArticleMetadata am = new ArticleMetadata();
            CachedUrl fullTextCu = af.getFullTextCu();
            if (fullTextCu == null) return;

            ArchivalUnit au = fullTextCu.getArchivalUnit();
            TdbAu tdbau = au.getTdbAu();

            // Populate standard TDB fields
            am.putIfBetter(MetadataField.FIELD_PUBLICATION_TITLE, au.getName());
            if (tdbau != null) {
                am.putIfBetter(MetadataField.FIELD_ISSN, tdbau.getIssn());
                am.putIfBetter(MetadataField.FIELD_PUBLISHER, tdbau.getPublisherName());
            }

            String pdfUrl = af.getFullTextUrl();
            CachedUrl summaryCu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);

            // Logic for missing summary page (e.g., -amp- vs -- folder names)
            if (summaryCu == null || !summaryCu.hasContent()) {
                AuUtil.safeRelease(summaryCu);
                Configuration config = au.getConfiguration();
                String baseUrl = config.get(ConfigParamDescr.BASE_URL.getKey());
                String journalId = config.get("journal_id");

                Pattern p = Pattern.compile("media/libraries/[^/]+/([^/]+)/", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(pdfUrl);
                if (m.find()) {
                    String folder = m.group(1).replace("-amp-", "--");
                    String manualUrl = baseUrl + journalId + "/" + folder;
                    log.debug3("Manual HTML lookup triggered: " + manualUrl);
                    summaryCu = au.makeCachedUrl(manualUrl);
                }
            }

            String rawFullTitle = null;
            try {
                if (summaryCu != null && summaryCu.hasContent()) {
                    log.debug3("Attempting to parse title from: " + summaryCu.getUrl());
                    rawFullTitle = parseRawTitleFromHtml(summaryCu, pdfUrl);
                } else {
                    log.debug3("Summary CU is null or empty for PDF: " + pdfUrl);
                }
            } finally {
                AuUtil.safeRelease(summaryCu);
            }

            if (rawFullTitle != null) {
                log.debug3("Successfully parsed title: " + rawFullTitle);
                am.putRaw("raw_article_title", rawFullTitle);
                processTitle(am, rawFullTitle);
            } else {
                log.debug3("Parsing failed for: " + pdfUrl);
                am.putIfBetter(MetadataField.FIELD_ARTICLE_TITLE, "Title Unknown: " + pdfUrl);
            }

            emitter.emitMetadata(af, am);
        }

        private String parseRawTitleFromHtml(CachedUrl cu, String pdfUrl) throws IOException {
            int lastSlash = pdfUrl.lastIndexOf('/');
            String pdfFileName = (lastSlash == -1) ? pdfUrl : pdfUrl.substring(lastSlash + 1);
            log.debug3("Searching HTML for PDF filename: " + pdfFileName);

            try (BufferedReader reader = new BufferedReader(cu.openForReading())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(pdfFileName)) {
                        log.debug3("Found line matching filename: " + line.trim());

                        // Regex captures text inside the <a> tag that links to our PDF
                        String regex = "href=\"[^\"]*" + Pattern.quote(pdfFileName) + "[^\"]*\".*?>(.*?)</a>";
                        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                        Matcher m = p.matcher(line);

                        if (m.find()) {
                            String rawTitle = m.group(1).trim();
                            // Strip tags, normalize &nbsp; and whitespace
                            String cleaned = rawTitle.replaceAll("<[^>]*>", " ")
                                    .replace("&nbsp;", " ")
                                    .replaceAll("\\s+", " ")
                                    .trim();
                            log.debug3("Extracted raw title string: " + cleaned);
                            return cleaned;
                        } else {
                            log.debug3("Line contained filename but link pattern did not match.");
                        }
                    }
                }
            }
            return null;
        }

        private void processTitle(ArticleMetadata am, String fullTitle) {
            Matcher m = TITLE_RE.matcher(fullTitle.trim());
            if (m.find()) {
                am.put(MetadataField.FIELD_ARTICLE_TITLE, m.group(1).trim());
                am.put(MetadataField.FIELD_VOLUME, m.group(2));
                am.put(MetadataField.FIELD_START_PAGE, m.group(3));
                if (m.group(4) != null) {
                    am.put(MetadataField.FIELD_END_PAGE, m.group(4));
                }
                am.put(MetadataField.FIELD_DATE, m.group(5));
            } else {
                am.put(MetadataField.FIELD_ARTICLE_TITLE, fullTitle);
            }
        }
    }
}