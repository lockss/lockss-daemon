/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.edinburgh;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.HttpHttpsUrlHelper;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.plugin.atypon.BaseAtyponMetadataUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EdinburghHtmlMetadataExtractorFactory
        extends BaseAtyponHtmlMetadataExtractorFactory {

    private static final Logger log =
            Logger.getLogger(EdinburghHtmlMetadataExtractorFactory.class);

    @Override
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {
        return new EdinburghHtmlMetadataExtractor();
    }

    public static class EdinburghHtmlMetadataExtractor
            extends BaseAtyponHtmlMetadataExtractor {

        /* The publisher name does not appear on the page in this form. */
        private static final String PUBLISHER_NAME = "Edinburgh University Press";

        /*
         * <div class="panel_top">
         *   <h2>Feb 2009</h2>
         *   <p class="volumeInfo">Issue: Volume 79, Number 1</p>
         * </div>
         *
         * Anchored on class="volumeInfo" because "Volume nn" appears in several
         * other places on the page (navigation, "other issues in this volume",
         * etc.) and we must not pick up a neighbouring volume. Matched against the
         * whole document rather than line by line, so that where the crawler's
         * line breaks happen to fall does not matter.
         */
        private static final Pattern VOLUME_ISSUE_PATTERN = Pattern.compile(
                "class=\"volumeInfo\"[^>]*>\\s*Issue:\\s*Volume\\s+([0-9]+)\\s*,"
                        + "\\s*Number\\s+([0-9]+(?:\\s*[-–]\\s*[0-9]+)?)",
                Pattern.CASE_INSENSITIVE);

        /*
         * ">ISSN:<" / ">E-ISSN:<" followed, within a short window, by the value.
         * The bounded lazy run tolerates both "<td>ISSN:</td><td>0036-9241</td>"
         * and "<span>ISSN:</span> 0036-9241". If it does not match, nothing is
         * recorded -- which is the safe outcome, since a garbled ISSN would make
         * metadataMatchesTdb() reject every article.
         */
        private static final Pattern ISSN_PATTERN = Pattern.compile(
                ">\\s*ISSN:\\s*<(?:[^<>]|<[^>]*>){0,60}?([0-9]{4}-[0-9]{3}[0-9Xx])",
                Pattern.CASE_INSENSITIVE);

        private static final Pattern EISSN_PATTERN = Pattern.compile(
                ">\\s*E-ISSN:\\s*<(?:[^<>]|<[^>]*>){0,60}?([0-9]{4}-[0-9]{3}[0-9Xx])",
                Pattern.CASE_INSENSITIVE);

        /* e.g. ">Page 145-167<" -- start page only. */
        private static final Pattern PAGE_PATTERN = Pattern.compile(
                ">\\s*Page\\s+([0-9]+)\\s*[-–]\\s*[0-9]+",
                Pattern.CASE_INSENSITIVE);

        private static final Pattern JOURNAL_TITLE_PATTERN = Pattern.compile(
                "<journal-title[^>]*>\\s*(.+?)\\s*</journal-title>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
                throws IOException {

            ArticleMetadata am =
                    new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
            am.cook(getTagMap());   // tagMap comes from BaseAtypon

            // Must happen before the TDB comparison below -- this is what supplies
            // the volume that the comparison keys on.
            scrapePageContent(cu, am);

            /*
             * If overcrawl landed us on a page with nothing useful on it (a "not
             * found" page, say), do not emit -- otherwise TDB defaults get filled in
             * and it counts as a real article. Checked after cooking, because
             * isEmpty() inspects the cooked values.
             */
            if (am.isEmpty()) {
                log.debug3("no metadata on page, skipping: " + cu.getUrl());
                return;
            }

            /*
             * Only emit if this article really belongs to this AU.
             *
             * We deliberately do NOT use BaseAtyponMetadataUtil.metadataMatchesTdb()
             * here. On these pages it rejects valid articles: the older EUP landing
             * pages carry no citation_journal_title and no ISSN meta tag, so that
             * check has no positive identification to work from even when the volume
             * is correct. Volume is the only signal these pages reliably provide,
             * and it is sufficient to keep out cross-volume overcrawl.
             */
            ArchivalUnit au = cu.getArchivalUnit();
            if (!volumeMatchesAu(au, am)) {
                log.debug3("volume mismatch, skipping: " + cu.getUrl()
                        + " [am volume=" + am.get(MetadataField.FIELD_VOLUME)
                        + ", au volume=" + auVolumeName(au) + "]");
                return;
            }

            /*
             * Fill in DOI, publisher and other values derivable from the URL or the
             * TDB, and correct access.url if it is not in the AU.
             */
            BaseAtyponMetadataUtil.completeMetadata(cu, am);

            // After completeMetadata, so our value wins over any TDB/DC publisher.
            am.replace(MetadataField.FIELD_PUBLISHER, PUBLISHER_NAME);

            // This plugin does http -> https conversion; keep access.url consistent.
            HttpHttpsUrlHelper helper = new HttpHttpsUrlHelper(
                    au, ConfigParamDescr.BASE_URL.getKey(), "base_url");
            String url = am.get(MetadataField.FIELD_ACCESS_URL);
            if (url != null) {
                am.replace(MetadataField.FIELD_ACCESS_URL, helper.normalize(url));
            }

            log.debug3("emitting: " + cu.getUrl()
                    + " [volume=" + am.get(MetadataField.FIELD_VOLUME) + "]");
            emitter.emitMetadata(cu, am);
        }

        /**
         * Volume-only overcrawl check: does the volume scraped off the page match
         * the volume this AU was configured for?
         *
         * Fails open. If we could not scrape a volume, or the AU has no volume
         * param, we have not disproved anything and the article is emitted --
         * losing a real article is worse than admitting an overcrawled one.
         */
        protected boolean volumeMatchesAu(ArchivalUnit au, ArticleMetadata am) {
            String amVolume = normalizeVolume(am.get(MetadataField.FIELD_VOLUME));
            String auVolume = normalizeVolume(auVolumeName(au));
            if (amVolume == null || auVolume == null) {
                return true;
            }
            return auVolume.equalsIgnoreCase(amVolume);
        }

        /** The AU's configured volume, from volume_name or volume. */
        protected static String auVolumeName(ArchivalUnit au) {
            Configuration config = au.getConfiguration();
            if (config == null) {
                return null;
            }
            String volume = config.get(ConfigParamDescr.VOLUME_NAME.getKey());
            if (volume == null) {
                volume = config.get(ConfigParamDescr.VOLUME_NUMBER.getKey());
            }
            return volume;
        }

        /** Strip a "Volume"/"Vol." label and leading zeros so 79, Volume 79 and
         *  079 all compare equal. */
        protected static String normalizeVolume(String volume) {
            if (volume == null) {
                return null;
            }
            volume = volume.trim()
                    .replaceFirst("(?i)^vol(ume|\\.)?\\s*", "")
                    .replaceFirst("^0+(?=[0-9])", "")
                    .trim();
            return volume.isEmpty() ? null : volume;
        }

        /**
         * Pull volume, issue, ISSN, start page and journal title out of the page
         * body. Uses replace() rather than put() throughout: put() appends, so a
         * second match elsewhere on the page would leave two values on the field.
         */
        protected void scrapePageContent(CachedUrl cu, ArticleMetadata am)
                throws IOException {

            String content;
            Reader reader = cu.openForReading();
            try {
                content = StringUtil.fromReader(reader);
            } finally {
                IOUtil.safeClose(reader);
            }

            Matcher m = VOLUME_ISSUE_PATTERN.matcher(content);
            if (m.find()) {
                String volume = m.group(1);
                String issue = m.group(2).replaceAll("\\s+", "");
                am.replace(MetadataField.FIELD_VOLUME, volume);
                am.replace(MetadataField.DC_FIELD_CITATION_VOLUME, volume);
                am.replace(MetadataField.FIELD_ISSUE, issue);
                am.replace(MetadataField.DC_FIELD_CITATION_ISSUE, issue);
            } else {
                // Worth knowing about: without a volume the TDB comparison loses most
                // of its power and overcrawled articles can slip through.
                log.debug3("no volumeInfo found on " + cu.getUrl());
            }

            m = EISSN_PATTERN.matcher(content);
            if (m.find()) {
                am.replace(MetadataField.FIELD_EISSN, m.group(1));
                am.replace(MetadataField.DC_FIELD_IDENTIFIER_EISSN, m.group(1));
            }

            m = ISSN_PATTERN.matcher(content);
            if (m.find()) {
                am.replace(MetadataField.FIELD_ISSN, m.group(1));
                am.replace(MetadataField.DC_FIELD_IDENTIFIER_ISSN, m.group(1));
            }

            m = PAGE_PATTERN.matcher(content);
            if (m.find()) {
                am.replace(MetadataField.FIELD_START_PAGE, m.group(1));
                am.replace(MetadataField.DC_FIELD_CITATION_SPAGE, m.group(1));
            }

            m = JOURNAL_TITLE_PATTERN.matcher(content);
            if (m.find()) {
                am.replace(MetadataField.FIELD_JOURNAL_TITLE, m.group(1));
                am.replace(MetadataField.DC_FIELD_RELATION_ISPARTOF, m.group(1));
            }
        }
    }
}