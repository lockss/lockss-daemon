/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.euclidprime;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EuclidPrimeJournalsXmlArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    // Article-level content -- one directory per article, always named *_SendAll.zip:
    //sourcefiles/euclidprime-released/2026_01/as_20_2_SendAll.zip!/2025.4319.348_SendAll.zip/2025.4319.348.pdf
    //sourcefiles/euclidprime-released/2026_01/as_20_2_SendAll.zip!/2025.4319.348_SendAll.zip/2025.4319.348.xml
    //
    // Issue-level metadata -- exactly one sibling directory that is NOT *_SendAll.zip. The XML
    // filename inside it follows no usable convention, so we never key on it:
    //sourcefiles/euclidprime-released/2026_01/as_20_2_SendAll.zip!/2.zip/as020-02.xml
    //sourcefiles/euclidprime-released/2026_01/jca_18_2_SendAll.zip!/2.zip/jca-2026-018-002.xml
    //sourcefiles/euclidprime-released/2026_01/mmj_76_4_SendAll.zip!/4.zip/mmj_2026-076-004.xml
    //sourcefiles/euclidprime-released/2026_01/tbilis.1_19_2_SendAll.zip!/2.zip/019_002.xml
    //sourcefiles/euclidprime-released/2026_01/tjm_49_1_SendAll.zip!/1.zip/tjm_49_1.xml
    //sourcefiles/euclidprime-released/2026_01/jgsp_77_none_SendAll.zip!/none.zip/jgsp-77-2026.xml

    protected static Logger log = Logger.getLogger(EuclidPrimeJournalsXmlArticleIteratorFactory.class);

    /** Role carrying the space-separated list of issue-level XML URLs for this article's issue. */
    public static final String ROLE_ISSUE_XML = "IssueXml";

    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s[^/]+/[^/]+_SendAll\\.zip!/[^/]+_SendAll\\.zip/[^/]+\\.(xml|pdf)$\", base_url";

    /**
     * Issue-level XMLs: same outer zip, but the containing directory is anything EXCEPT
     * {@code *_SendAll.zip}. Structural, so the publisher's inconsistent file naming is irrelevant.
     */
    protected static final String ISSUE_XML_PATTERN_TEMPLATE =
            "\"%s[^/]+/[^/]+_SendAll\\.zip!/(?![^/]+_SendAll\\.zip/)[^/]+/[^/]+\\.xml$\", base_url";

    protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
            Pattern.compile(".*\\.zip!/[^/]+\\.zip/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                    Pattern.CASE_INSENSITIVE);

    /** group(1) = everything through "...!/" (the issue container), group(2) = "tjm_49_1". */
    protected static final Pattern CONTAINER_PATTERN =
            Pattern.compile("^(.*/([^/]+)_SendAll\\.zip!/)", Pattern.CASE_INSENSITIVE);

    /** "tjm_49_1" -> journal "tjm", volume "49", issue "1"; "tbilis.1_19_2" -> "tbilis.1"/19/2. */
    protected static final Pattern ISSUE_TOKEN_PATTERN =
            Pattern.compile("^(.+)_([^_]+)_([^_]+)$");

    protected Pattern getExcludeSubTreePattern() {
        return SUB_NESTED_ARCHIVE_PATTERN;
    }

    protected String getIncludePatternTemplate() {
        return ALL_ZIP_XML_PATTERN_TEMPLATE;
    }

    protected String getIssueXmlPatternTemplate() {
        return ISSUE_XML_PATTERN_TEMPLATE;
    }

    public static final Pattern XML_PATTERN = Pattern.compile("/([^/]+)\\.xml$", Pattern.CASE_INSENSITIVE);
    public static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    public static final String XML_REPLACEMENT = "/$1.xml";
    private static final String PDF_REPLACEMENT = "/$1.pdf";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {

        // Pass 1: locate the issue-level XML(s) belonging to each issue container.
        final Map<String, List<String>> issueXmls = collectIssueXmls(au, target);

        // Pass 2: the article iterator proper -- unchanged behaviour. Note that the include
        // pattern requires the inner directory to end in _SendAll.zip, so issue-level XMLs are
        // NOT emitted as articles. Keep it that way, or article counts drift by one per issue.
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        // no need to limit to ROOT_TEMPLATE
        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
                .setExcludeSubTreePattern(getExcludeSubTreePattern())
                .setVisitArchiveMembers(getIsArchive()));

        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        final Iterator<ArticleFiles> base = builder.getSubTreeArticleIterator();

        // Decorate each ArticleFiles with the issue-XML candidates for its container.
        return new Iterator<ArticleFiles>() {

            @Override
            public boolean hasNext() {
                return base.hasNext();
            }

            @Override
            public ArticleFiles next() {
                ArticleFiles af = base.next();
                if (af != null) {
                    String url = af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA);
                    if (url == null) {
                        url = af.getFullTextUrl();
                    }
                    String container = containerPrefix(url);
                    if (container != null) {
                        List<String> cands = issueXmls.get(container);
                        if (cands != null && !cands.isEmpty()) {
                            af.setRoleString(ROLE_ISSUE_XML, join(cands, " "));
                            if (cands.size() > 1) {
                                log.warning("Container " + container + " has " + cands.size()
                                        + " issue-level XMLs: " + cands
                                        + " -- will disambiguate per article via the toc");
                            }
                        }
                    }
                }
                return af;
            }

            @Override
            public void remove() {
                base.remove();
            }
        };
    }

    /**
     * Enumerate issue-level XMLs, grouped by issue container. Uses a SubTreeArticleIterator
     * because archive members are not enumerable through a plain CachedUrlSet walk.
     */
    protected Map<String, List<String>> collectIssueXmls(ArchivalUnit au, MetadataTarget target) {

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        try {
            SubTreeArticleIteratorBuilder b = new SubTreeArticleIteratorBuilder(au);
            b.setSpec(b.newSpec()
                    .setTarget(target)
                    .setPatternTemplate(getIssueXmlPatternTemplate(), Pattern.CASE_INSENSITIVE)
                    .setVisitArchiveMembers(getIsArchive()));
            b.addAspect(XML_PATTERN, XML_REPLACEMENT, ROLE_ISSUE_XML);
            b.setFullTextFromRoles(ROLE_ISSUE_XML);

            for (Iterator<ArticleFiles> it = b.getSubTreeArticleIterator(); it.hasNext(); ) {
                ArticleFiles af = it.next();
                String url = af.getRoleUrl(ROLE_ISSUE_XML);
                if (url == null) {
                    continue;
                }
                String container = containerPrefix(url);
                if (container == null) {
                    continue;
                }
                List<String> list = map.get(container);
                if (list == null) {
                    list = new ArrayList<String>();
                    map.put(container, list);
                }
                list.add(url);
            }
        } catch (Exception ex) {
            log.warning("Failed to enumerate issue-level XMLs for " + au.getName(), ex);
        }
        log.debug2("Found issue-level XMLs for " + map.size() + " container(s)");
        return map;
    }

    // ---------------------------------------------------------------------------------------------
    // URL token helpers -- shared with the metadata extractor
    // ---------------------------------------------------------------------------------------------

    /** ".../tjm_49_1_SendAll.zip!/" for any URL inside that outer zip, or null. */
    public static String containerPrefix(String url) {
        if (url == null) {
            return null;
        }
        Matcher m = CONTAINER_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    /** "tjm_49_1" for any URL inside that outer zip, or null. */
    public static String containerToken(String url) {
        if (url == null) {
            return null;
        }
        Matcher m = CONTAINER_PATTERN.matcher(url);
        return m.find() ? m.group(2) : null;
    }

    private static String tokenPart(String url, int group) {
        String tok = containerToken(url);
        if (tok == null) {
            return null;
        }
        Matcher m = ISSUE_TOKEN_PATTERN.matcher(tok);
        return m.matches() ? m.group(group) : null;
    }

    public static String journalToken(String url) {
        return tokenPart(url, 1);
    }

    public static String volumeToken(String url) {
        return tokenPart(url, 2);
    }

    public static String issueToken(String url) {
        return tokenPart(url, 3);
    }

    /** "1502179441" from ".../1502179441_SendAll.zip/1502179441.xml". */
    public static String articleIdFromUrl(String url) {
        if (url == null) {
            return null;
        }
        int slash = url.lastIndexOf('/');
        if (slash < 0) {
            return null;
        }
        String name = url.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    protected boolean getIsArchive() {
        return true;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new EuclidPrimeJournalsArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
