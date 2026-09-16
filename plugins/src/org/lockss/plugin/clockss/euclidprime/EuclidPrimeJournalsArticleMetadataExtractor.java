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
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Article metadata extractor for the Euclid Prime source plugin.
 *
 * <p>Euclid Prime article-level JATS files carry only
 * {@code <journal-meta><journal-id journal-id-type="euclid">tjm</journal-id></journal-meta>} --
 * there is no {@code <journal-title>} and no {@code <issn>}. Those fields live only in the
 * issue-level XML that the publisher ships alongside the article directories, e.g.
 *
 * <pre>
 *   .../tjm_49_1_SendAll.zip!/1.zip/tjm_49_1.xml          &lt;-- issue-level
 *   .../tjm_49_1_SendAll.zip!/1502179441_SendAll.zip/1502179441.xml   &lt;-- article-level
 * </pre>
 *
 * <p>The issue-level XML's <em>filename</em> follows no usable convention
 * ({@code as020-02.xml}, {@code jca-2026-018-002.xml}, {@code 019_002.xml}, ...), so this class
 * never keys on the filename. The iterator factory identifies issue XMLs structurally -- they are
 * the ones whose containing directory is NOT {@code *_SendAll.zip} -- and hands the candidate URLs
 * over on {@link EuclidPrimeJournalsXmlArticleIteratorFactory#ROLE_ISSUE_XML}.
 *
 * <p><b>Safety properties.</b> This backfill is deliberately constrained so that a wrong or
 * ambiguous issue XML cannot silently corrupt an article record:
 * <ol>
 *   <li>Only fields that are <em>empty</em> on the article record are ever written. The article
 *       XML always wins.</li>
 *   <li>Only <em>issue-level</em> fields are backfilled (publication title, ISSN, eISSN,
 *       publisher, and volume/issue/date as a last resort). Article-level fields -- title,
 *       authors, DOI, pages -- are never taken from the issue XML.</li>
 *   <li>When more than one issue XML is present in the container, candidates are narrowed by
 *       journal id, then volume/issue, then by whether the issue XML's {@code <toc>} actually
 *       lists this article's DOI. The toc is an exact membership list, so this is a real join,
 *       not a guess.</li>
 *   <li>If narrowing still leaves several candidates, a field is written only when every surviving
 *       candidate agrees on its value. Disagreement is logged at ERROR and the field is left
 *       empty. A duplicate delivery is therefore either a no-op or a loud failure -- never a
 *       silent wrong answer.</li>
 * </ol>
 */
public class EuclidPrimeJournalsArticleMetadataExtractor extends BaseArticleMetadataExtractor {

  private static final Logger log =
      Logger.getLogger(EuclidPrimeJournalsArticleMetadataExtractor.class);

  /** Parsed issue XMLs, keyed by URL. Scoped to one indexing run. */
  private final Map<String, IssueMeta> issueCache = new HashMap<String, IssueMeta>();

  public EuclidPrimeJournalsArticleMetadataExtractor(String role) {
    super(role);
  }

  @Override
  public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
      throws IOException, PluginException {
    super.extract(target, af, new BackfillEmitter(emitter));
  }

  /**
   * Wraps the downstream emitter so we can top up the cooked record after the JATS helper has
   * run on the article XML but before it is handed to the metadata database.
   */
  private class BackfillEmitter implements Emitter {

    private final Emitter delegate;

    BackfillEmitter(Emitter delegate) {
      this.delegate = delegate;
    }

    @Override
    public void emitMetadata(ArticleFiles af, ArticleMetadata am) {
      try {
        backfillFromIssueXml(af, am);
      } catch (Exception ex) {
        // Backfill is best-effort. Never let it prevent the article from being emitted.
        log.warning("Issue-XML backfill failed for "
            + (af == null ? "(null af)" : af.getFullTextUrl()), ex);
      }
      delegate.emitMetadata(af, am);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Backfill
  // ---------------------------------------------------------------------------------------------

  protected void backfillFromIssueXml(ArticleFiles af, ArticleMetadata am) {

    if (af == null || am == null) {
      return;
    }

    // Nothing to do if the article XML already supplied everything we would fill.
    if (!isEmpty(am, MetadataField.FIELD_PUBLICATION_TITLE)
        && !isEmpty(am, MetadataField.FIELD_ISSN)
        && !isEmpty(am, MetadataField.FIELD_EISSN)) {
      return;
    }

    String candidateBlob =
        af.getRoleAsString(EuclidPrimeJournalsXmlArticleIteratorFactory.ROLE_ISSUE_XML);
    if (candidateBlob == null || candidateBlob.trim().isEmpty()) {
      log.warning("No issue-level XML found for article " + af.getFullTextUrl()
          + "; publication title and ISSN will be empty");
      return;
    }

    CachedUrl articleCu =
        af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
    if (articleCu == null) {
      articleCu = af.getFullTextCu();
    }
    if (articleCu == null) {
      return;
    }
    ArchivalUnit au = articleCu.getArchivalUnit();
    String articleUrl = articleCu.getUrl();

    List<IssueMeta> candidates = new ArrayList<IssueMeta>();
    for (String url : candidateBlob.trim().split("\\s+")) {
      IssueMeta meta = getIssueMeta(au, url);
      if (meta != null) {
        candidates.add(meta);
      }
    }
    if (candidates.isEmpty()) {
      log.warning("Issue-level XML(s) listed for " + articleUrl + " could not be parsed: "
          + candidateBlob);
      return;
    }

    List<IssueMeta> chosen = select(candidates, articleUrl, am);
    if (chosen.isEmpty()) {
      log.error("No issue-level XML could be matched to " + articleUrl
          + " among candidates [" + candidateBlob + "]; leaving fields empty");
      return;
    }

    fill(am, MetadataField.FIELD_PUBLICATION_TITLE, chosen, "journalTitle", articleUrl);
    fill(am, MetadataField.FIELD_ISSN, chosen, "issn", articleUrl);
    fill(am, MetadataField.FIELD_EISSN, chosen, "eissn", articleUrl);
    fill(am, MetadataField.FIELD_PUBLISHER, chosen, "publisher", articleUrl);
    // These are normally present on the article record already; the isEmpty guard inside fill()
    // means they are only touched when the article XML omitted them.
    fill(am, MetadataField.FIELD_VOLUME, chosen, "volume", articleUrl);
    fill(am, MetadataField.FIELD_ISSUE, chosen, "issue", articleUrl);
    fill(am, MetadataField.FIELD_DATE, chosen, "date", articleUrl);
  }

  /**
   * Narrow the candidate list. Each stage is skipped rather than applied if it would eliminate
   * everything, so a partially-populated issue XML degrades instead of failing.
   */
  protected List<IssueMeta> select(List<IssueMeta> candidates, String articleUrl,
                                   ArticleMetadata am) {

    List<IssueMeta> cur = candidates;
    if (cur.size() == 1) {
      return cur;
    }

    final String journalTok = EuclidPrimeJournalsXmlArticleIteratorFactory.journalToken(articleUrl);
    final String issueTok = EuclidPrimeJournalsXmlArticleIteratorFactory.issueToken(articleUrl);
    final String volTok = EuclidPrimeJournalsXmlArticleIteratorFactory.volumeToken(articleUrl);

    // 1. journal id from the issue XML must agree with the journal token in the zip name.
    cur = keepIfAny(cur, new Filter() {
      public boolean keep(IssueMeta m) {
        return m.journalId == null || journalTokenMatches(m.journalId, journalTok);
      }
    });

    // 2. volume / issue must agree when both sides state them.
    cur = keepIfAny(cur, new Filter() {
      public boolean keep(IssueMeta m) {
        return agrees(m.volume, volTok) && agrees(m.issue, issueTok);
      }
    });

    if (cur.size() == 1) {
      return cur;
    }

    // 3. Decisive test: does the issue XML's toc actually list this article's DOI?
    String doi = normalizeDoi(am.get(MetadataField.FIELD_DOI));
    if (doi == null) {
      doi = normalizeDoi(EuclidPrimeJournalsXmlArticleIteratorFactory.articleIdFromUrl(articleUrl));
    }
    if (doi != null) {
      List<IssueMeta> withDoi = new ArrayList<IssueMeta>();
      List<IssueMeta> withoutToc = new ArrayList<IssueMeta>();
      for (IssueMeta m : cur) {
        if (m.tocIds.isEmpty()) {
          withoutToc.add(m);
        } else if (m.matchesTocId(doi)) {
          withDoi.add(m);
        }
      }
      if (!withDoi.isEmpty()) {
        return withDoi;
      }
      if (!withoutToc.isEmpty()) {
        return withoutToc;
      }
      // Every candidate has a toc and none of them lists this article.
      log.error("Article " + articleUrl + " (id " + doi
          + ") appears in no issue-level toc; refusing to backfill");
      return new ArrayList<IssueMeta>();
    }

    return cur;
  }

  /**
   * Write {@code field} only if the article record left it empty AND every surviving candidate
   * agrees on the value.
   */
  protected void fill(ArticleMetadata am, MetadataField field, List<IssueMeta> chosen,
                      String key, String articleUrl) {

    if (!isEmpty(am, field)) {
      return;
    }
    Set<String> values = new LinkedHashSet<String>();
    for (IssueMeta m : chosen) {
      String v = m.get(key);
      if (v != null && !v.isEmpty()) {
        values.add(v);
      }
    }
    if (values.isEmpty()) {
      return;
    }
    if (values.size() > 1) {
      log.error("Conflicting " + field.getKey() + " across " + chosen.size()
          + " issue-level XMLs for " + articleUrl + ": " + values
          + " -- leaving the field empty rather than guessing");
      return;
    }
    String value = values.iterator().next();
    am.put(field, value);
    log.debug2("Backfilled " + field.getKey() + "=" + value + " for " + articleUrl);
  }

  // ---------------------------------------------------------------------------------------------
  // Issue XML parsing
  // ---------------------------------------------------------------------------------------------

  protected IssueMeta getIssueMeta(ArchivalUnit au, String url) {
    if (issueCache.containsKey(url)) {
      return issueCache.get(url);
    }
    IssueMeta meta = null;
    CachedUrl cu = null;
    try {
      cu = au.makeCachedUrl(url);
      if (cu != null && cu.hasContent()) {
        meta = parseIssueXml(cu, url);
      } else {
        log.warning("Issue-level XML has no content: " + url);
      }
    } catch (Exception ex) {
      log.warning("Could not parse issue-level XML " + url, ex);
    } finally {
      AuUtilSafeRelease(cu);
    }
    issueCache.put(url, meta);
    return meta;
  }

  private static void AuUtilSafeRelease(CachedUrl cu) {
    if (cu != null) {
      try {
        cu.release();
      } catch (Exception ignore) {
        // nothing useful to do
      }
    }
  }

  protected IssueMeta parseIssueXml(CachedUrl cu, String url) throws Exception {
    InputStream in = null;
    try {
      in = cu.getUnfilteredInputStream();

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(false);
      dbf.setValidating(false);
      // Do not go to the network for the JATS DTD.
      trySetFeature(dbf, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      trySetFeature(dbf, "http://xml.org/sax/features/external-general-entities", false);
      trySetFeature(dbf, "http://xml.org/sax/features/external-parameter-entities", false);

      DocumentBuilder db = dbf.newDocumentBuilder();
      db.setEntityResolver(new org.xml.sax.EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId) {
          return new InputSource(new java.io.StringReader(""));
        }
      });
      Document doc = db.parse(new InputSource(in));

      XPath xp = XPathFactory.newInstance().newXPath();

      IssueMeta meta = new IssueMeta(url);
      // local-name() throughout so this also works if a journal ships a namespaced JATS variant.
      meta.journalId = text(xp, doc, "(//*[local-name()='journal-id'])[1]");
      meta.journalTitle = firstNonEmpty(
          text(xp, doc, "(//*[local-name()='journal-title'])[1]"),
          text(xp, doc, "(//*[local-name()='abbrev-journal-title'])[1]"));
      meta.issn = firstNonEmpty(
          text(xp, doc, "(//*[local-name()='issn'][@pub-type='ppub'])[1]"),
          text(xp, doc, "(//*[local-name()='issn'][@publication-format='print'])[1]"));
      meta.eissn = firstNonEmpty(
          text(xp, doc, "(//*[local-name()='issn'][@pub-type='epub'])[1]"),
          text(xp, doc, "(//*[local-name()='issn'][@publication-format='electronic'])[1]"),
          text(xp, doc, "(//*[local-name()='eissn'])[1]"));
      // An unqualified single <issn> is a print ISSN by JATS convention.
      if (meta.issn == null && meta.eissn == null) {
        meta.issn = text(xp, doc, "(//*[local-name()='issn'])[1]");
      }
      meta.publisher = text(xp, doc, "(//*[local-name()='publisher-name'])[1]");
      meta.volume = text(xp, doc, "(//*[local-name()='issue-meta']/*[local-name()='volume'])[1]");
      meta.issue = text(xp, doc, "(//*[local-name()='issue-meta']/*[local-name()='issue'])[1]");
      meta.date = buildDate(
          text(xp, doc, "(//*[local-name()='issue-meta']//*[local-name()='year'])[1]"),
          text(xp, doc, "(//*[local-name()='issue-meta']//*[local-name()='month'])[1]"),
          text(xp, doc, "(//*[local-name()='issue-meta']//*[local-name()='day'])[1]"));

      NodeList toc = (NodeList) xp.evaluate(
          "//*[local-name()='toc']//*[local-name()='article-id']", doc, XPathConstants.NODESET);
      for (int i = 0; i < toc.getLength(); i++) {
        String id = normalizeDoi(nodeText(toc.item(i)));
        if (id != null) {
          meta.tocIds.add(id);
        }
      }

      log.debug2("Parsed issue XML " + url + " -> " + meta);
      return meta;

    } finally {
      org.lockss.util.IOUtil.safeClose(in);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  private interface Filter {
    boolean keep(IssueMeta m);
  }

  /** Apply a filter, but only if it leaves at least one candidate standing. */
  private static List<IssueMeta> keepIfAny(List<IssueMeta> in, Filter f) {
    List<IssueMeta> out = new ArrayList<IssueMeta>();
    for (IssueMeta m : in) {
      if (f.keep(m)) {
        out.add(m);
      }
    }
    return out.isEmpty() ? in : out;
  }

  private static boolean isEmpty(ArticleMetadata am, MetadataField field) {
    String v = am.get(field);
    return v == null || v.trim().isEmpty();
  }

  /** Two values agree if either is absent, or they are equal ignoring case and leading zeros. */
  protected static boolean agrees(String a, String b) {
    if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
      return true;
    }
    return stripLeadingZeros(a).equalsIgnoreCase(stripLeadingZeros(b));
  }

  private static String stripLeadingZeros(String s) {
    String t = s.trim();
    int i = 0;
    while (i < t.length() - 1 && t.charAt(i) == '0') {
      i++;
    }
    return t.substring(i);
  }

  /**
   * The issue XML states a short journal id ("tjm"); the zip name carries the same token, though
   * occasionally with a suffix ("tbilis.1"). Match on the leading component.
   */
  protected static boolean journalTokenMatches(String journalId, String zipToken) {
    if (journalId == null || zipToken == null) {
      return true;
    }
    String a = journalId.trim().toLowerCase();
    String b = zipToken.trim().toLowerCase();
    return a.equals(b) || b.startsWith(a) || a.startsWith(b);
  }

  /** Reduce a DOI or publisher id to its final path segment for comparison. */
  protected static String normalizeDoi(String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.trim().toLowerCase();
    if (s.isEmpty()) {
      return null;
    }
    return s;
  }

  private static String buildDate(String year, String month, String day) {
    if (year == null || year.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder(year);
    if (month != null && !month.isEmpty()) {
      sb.append('-').append(month);
      if (day != null && !day.isEmpty()) {
        sb.append('-').append(day);
      }
    }
    return sb.toString();
  }

  private static String firstNonEmpty(String... vals) {
    for (String v : vals) {
      if (v != null && !v.isEmpty()) {
        return v;
      }
    }
    return null;
  }

  private static String text(XPath xp, Document doc, String expr) throws Exception {
    Node n = (Node) xp.evaluate(expr, doc, XPathConstants.NODE);
    return nodeText(n);
  }

  private static String nodeText(Node n) {
    if (n == null) {
      return null;
    }
    String s = n.getTextContent();
    if (s == null) {
      return null;
    }
    s = s.replaceAll("\\s+", " ").trim();
    return s.isEmpty() ? null : s;
  }

  private static void trySetFeature(DocumentBuilderFactory dbf, String feature, boolean value) {
    try {
      dbf.setFeature(feature, value);
    } catch (Exception ignore) {
      // parser does not know this feature; harmless
    }
  }

  // ---------------------------------------------------------------------------------------------

  /** Immutable-ish view of one issue-level XML. */
  public static class IssueMeta {

    public final String url;
    public String journalId;
    public String journalTitle;
    public String issn;
    public String eissn;
    public String publisher;
    public String volume;
    public String issue;
    public String date;
    public final Set<String> tocIds = new HashSet<String>();

    public IssueMeta(String url) {
      this.url = url;
    }

    public String get(String key) {
      if ("journalTitle".equals(key)) return journalTitle;
      if ("issn".equals(key)) return issn;
      if ("eissn".equals(key)) return eissn;
      if ("publisher".equals(key)) return publisher;
      if ("volume".equals(key)) return volume;
      if ("issue".equals(key)) return issue;
      if ("date".equals(key)) return date;
      return null;
    }

    /**
     * The toc lists full DOIs ("10.3836/tjm/1502179441"); the article may present the same value,
     * or the article directory may only give the trailing id ("1502179441"). Accept either.
     */
    public boolean matchesTocId(String candidate) {
      if (candidate == null) {
        return false;
      }
      if (tocIds.contains(candidate)) {
        return true;
      }
      for (String id : tocIds) {
        if (id.equals(candidate)
            || id.endsWith("/" + candidate)
            || candidate.endsWith("/" + id)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return "IssueMeta[" + url + " journalId=" + journalId + " title=" + journalTitle
          + " issn=" + issn + " eissn=" + eissn + " vol=" + volume + " iss=" + issue
          + " tocIds=" + tocIds.size() + "]";
    }
  }
}
