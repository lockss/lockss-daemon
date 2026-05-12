package org.lockss.plugin.iumj;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;

public class HtmlBibtexMetadataExtractor implements FileMetadataExtractor {

    private static final Logger log =
            Logger.getLogger(HtmlBibtexMetadataExtractor.class);

    private static MultiMap tagMap = new MultiValueMap();
    static {
        tagMap.put("author", MetadataField.FIELD_AUTHOR);
        tagMap.put("date", MetadataField.FIELD_DATE);
        tagMap.put("year", MetadataField.FIELD_DATE);
        tagMap.put("title", MetadataField.FIELD_ARTICLE_TITLE);
        tagMap.put("journal", MetadataField.FIELD_PUBLICATION_TITLE);
        tagMap.put("fjournal", MetadataField.FIELD_PUBLICATION_TITLE);
        tagMap.put("volume", MetadataField.FIELD_VOLUME);
        tagMap.put("issue", MetadataField.FIELD_ISSUE);
        tagMap.put("pages", MetadataField.FIELD_START_PAGE);
        tagMap.put("issn", MetadataField.FIELD_ISSN);
        tagMap.put("url", MetadataField.FIELD_ACCESS_URL);
    }

    private ArticleMetadata am;

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException, PluginException {
        ArticleMetadata md = extract(target, cu);
        if (md != null) {
            emitter.emitMetadata(cu, md);
        }
    }

    public final ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
            throws IOException, PluginException {
        if (cu == null) {
            throw new IllegalArgumentException();
        }

        log.debug3("HtmlBibtexMetadataExtractor Parsing: " + cu.getUrl());

        am = new ArticleMetadata();
        Reader reader = null;

        try {
            reader = cu.openForReading();

            String html = readAll(reader);

            log.debug3("HtmlBibtex URL: " + cu.getUrl());
            log.debug3("HtmlBibtex HTML length: " + html.length());

            Document doc = Jsoup.parse(html, cu.getUrl());

            Element pre = doc.selectFirst("body pre");
            if (pre == null) {
                log.debug3("No <pre> block found");
                return am;
            }

            // Parse span-based BibTeX structure directly from HTML elements
            for (Element ent : pre.select("span.ent")) {
                String fieldName = ent.text().trim().toUpperCase();
                // The value is in the next span.brace after the = sign
                // Structure: <span class="ent">FIELD</span> = <span class="brace">{</span>VALUE<span class="brace">}</span>
                Element nextBrace = ent.nextElementSibling();
                // skip the opening brace span, get the text node between braces
                if (nextBrace != null && nextBrace.hasClass("brace")) {
                    // get text between opening and closing brace spans
                    StringBuilder value = new StringBuilder();
                    org.jsoup.nodes.Node node = nextBrace.nextSibling();
                    while (node != null) {
                        if (node instanceof org.jsoup.nodes.TextNode) {
                            value.append(((org.jsoup.nodes.TextNode) node).text());
                        } else if (node instanceof Element) {
                            Element el = (Element) node;
                            if (el.hasClass("brace")) break; // closing brace
                            value.append(el.text());
                        }
                        node = node.nextSibling();
                    }
                    String val = value.toString().trim();
                    log.debug3("Field: " + fieldName + " = " + val);
                    putFieldByName(fieldName, val);
                }
            }

            if (pre == null) {
                pre = doc.selectFirst("div#content");
            }
            if (pre == null) {
                pre = doc.selectFirst("body");
            }
            log.debug3("HtmlBibtex pre found: " + (pre != null));
            if (pre != null) {
                log.debug3("HtmlBibtex pre text: " + pre.text().substring(0, Math.min(200, pre.text().length())));
            }

            if (pre == null) {
                log.debug3("No <pre> block found");
                return am;
            }

            String bibText = pre.text();
            log.debug3("Extracted pre text: " + bibText);

            extractField(bibText, "AUTHOR", MetadataField.FIELD_AUTHOR, "Author");
            extractField(bibText, "TITLE", MetadataField.FIELD_ARTICLE_TITLE, "Title");

            String journal = extractRawField(bibText, "JOURNAL");
            if (journal != null) {
                log.debug3("Publication Title (journal): " + journal);
                am.put(MetadataField.FIELD_PUBLICATION_TITLE, journal);
            } else {
                String fjournal = extractRawField(bibText, "FJOURNAL");
                if (fjournal != null) {
                    log.debug3("Publication Title (fjournal): " + fjournal);
                    am.put(MetadataField.FIELD_PUBLICATION_TITLE, fjournal);
                }
            }

            extractField(bibText, "VOLUME", MetadataField.FIELD_VOLUME, "Volume");
            extractField(bibText, "ISSUE", MetadataField.FIELD_ISSUE, "Issue");
            extractField(bibText, "ISSN", MetadataField.FIELD_ISSN, "ISSN");
            extractField(bibText, "DOI", MetadataField.FIELD_DOI, "DOI");
            extractField(bibText, "ISBN", MetadataField.FIELD_ISBN, "ISBN");

            String pages = extractRawField(bibText, "PAGES");
            if (pages != null) {
                pages = pages.replace("&ndash;", "–").trim();
                log.debug3("Pages: " + pages);

                String[] pageParts = pages.split("--|–|-");
                if (pageParts.length > 0) {
                    String sstart = pageParts[0].trim();
                    log.debug3("Start Page: " + sstart);
                    am.put(MetadataField.FIELD_START_PAGE, sstart);
                }
                if (pageParts.length > 1) {
                    String send = pageParts[1].trim();
                    log.debug3("End Page: " + send);
                    am.put(MetadataField.FIELD_END_PAGE, send);
                }
            }

            String year = extractRawField(bibText, "YEAR");
            if (year != null) {
                year = year.trim();
                log.debug3("Year: " + year);
                am.put(MetadataField.FIELD_DATE, year);
            }

            if (cu.getUrl() != null) {
                am.put(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
            }

            putRawIfPresent(bibText, "AUTHOR");
            putRawIfPresent(bibText, "TITLE");
            putRawIfPresent(bibText, "JOURNAL");
            putRawIfPresent(bibText, "FJOURNAL");
            putRawIfPresent(bibText, "VOLUME");
            putRawIfPresent(bibText, "YEAR");
            putRawIfPresent(bibText, "ISSUE");
            putRawIfPresent(bibText, "PAGES");
            putRawIfPresent(bibText, "ISSN");
            putRawIfPresent(bibText, "DOI");
            putRawIfPresent(bibText, "ISBN");

        } catch (Exception e) {
            log.debug3("Unexpected error: " + e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.debug3("Failed to close reader: " + e.getMessage());
                }
            }
        }

        am.cook(tagMap);
        return am;
    }

    private void putFieldByName(String fieldName, String value) {
        switch (fieldName) {
            case "AUTHOR":   am.put(MetadataField.FIELD_AUTHOR, value); break;
            case "TITLE":    am.put(MetadataField.FIELD_ARTICLE_TITLE, value); break;
            case "JOURNAL":  am.put(MetadataField.FIELD_PUBLICATION_TITLE, value); break;
            case "FJOURNAL":
                if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null)
                    am.put(MetadataField.FIELD_PUBLICATION_TITLE, value);
                break;
            case "VOLUME":   am.put(MetadataField.FIELD_VOLUME, value); break;
            case "ISSUE":    am.put(MetadataField.FIELD_ISSUE, value); break;
            case "ISSN":     am.put(MetadataField.FIELD_ISSN, value); break;
            case "DOI":      am.put(MetadataField.FIELD_DOI, value); break;
            case "YEAR":     am.put(MetadataField.FIELD_DATE, value); break;
            case "PAGES":
                value = value.replace("\u2013", "-").replace("&ndash;", "-");
                String[] parts = value.split("-");
                if (parts.length > 0) am.put(MetadataField.FIELD_START_PAGE, parts[0].trim());
                if (parts.length > 1) am.put(MetadataField.FIELD_END_PAGE, parts[1].trim());
                break;
            default: break;
        }
    }

    private void extractField(String text, String fieldName,
                              MetadataField field, String label) {
        String value = extractRawField(text, fieldName);
        if (value != null) {
            log.debug3(label + ": " + value);
            am.put(field, value);
        }
    }

    private void putRawIfPresent(String text, String fieldName) {
        String value = extractRawField(text, fieldName);
        if (value != null) {
            am.putRaw(fieldName.toLowerCase(), value);
        }
    }

    private String extractRawField(String text, String fieldName) {
        Pattern p = Pattern.compile(
                "(?is)\\b" + Pattern.quote(fieldName) + "\\b\\s*=\\s*\\{(.*?)\\}\\s*,?"
        );
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}