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

package org.lockss.plugin.clockss.elifesciences;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.regex.Pattern;

public class ElifeSciencesPreprintXmlSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    private static final Logger log = Logger.getLogger(ElifeSciencesPreprintXmlSourceArticleIteratorFactory2.class);
    protected static final String MANIFEST_FILE = "manifest.xml";

    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s[^/]+/.*\\.zip!/content/((\\d+\\.xml)|(\\d+\\.\\d+\\.xml))$\", base_url";

    // Be sure to exclude all nested archives in case supplemental data is provided this way
    protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
            Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        log.debug3("Initializing ArticleIterator for AU: " + au.getName());

        // Diagnostic: Check if AU actually sees the files you listed
        Iterator<CachedUrl> cuIter = au.getAuCachedUrlSet().getCuIterator();
        if (cuIter.hasNext()) {
            log.debug3("AU has files. First file found: " + cuIter.next().getUrl());
        } else {
            log.warning("AU IS EMPTY! No files found in the cached url set.");
        }

        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        String MANIFEST_PATTERN = "\"%s%d/.*\\.zip!/manifest\\.xml$\", base_url, year";

        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setRootTemplate("\"%s%d/\", base_url, year")
                .setPatternTemplate(MANIFEST_PATTERN, Pattern.CASE_INSENSITIVE)
                .setVisitArchiveMembers(true));

        // 2. THIS IS THE KEY: Manually map the "!" path as a role
        // This often forces the builder to accept the URL into the result set
        builder.addAspect(
                Pattern.compile(".*\\.zip!/manifest\\.xml$", Pattern.CASE_INSENSITIVE),
                "manifest_file", // arbitrary role name
                ArticleFiles.ROLE_ARTICLE_METADATA);

        return new MyManifestIterator(au, builder.getSubTreeArticleIterator());
    }

    protected class MyManifestIterator implements Iterator<ArticleFiles> {
        private final ArchivalUnit au;
        private final Iterator<ArticleFiles> subTreeIter;
        private ArticleFiles nextAf = null;

        public MyManifestIterator(ArchivalUnit au, Iterator<ArticleFiles> subTreeIter) {
            this.au = au;
            this.subTreeIter = subTreeIter;
            log.debug3("MyManifestIterator initialized with subTreeIter.");
        }

        @Override
        public boolean hasNext() {
            while (nextAf == null && subTreeIter.hasNext()) {
                ArticleFiles af = subTreeIter.next();
                CachedUrl cu = af.getFullTextCu();
                if (cu == null) cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);

                if (cu != null && cu.hasContent()) {
                    String url = cu.getUrl();
                    if (url.toLowerCase().endsWith(".zip!/manifest.xml")) {
                        log.debug3("MANUAL MATCH: " + url);
                        nextAf = processManifest(cu);
                    }
                }
            }
            return nextAf != null;
        }

        private ArticleFiles processManifest(CachedUrl manifestCu) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(manifestCu.getUnfilteredInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("<!DOCTYPE")) continue; // Skip the DTD line
                    sb.append(line).append("\n");
                }
            } catch (Exception e) {
                log.error("Failed to read manifest stream: " + manifestCu.getUrl());
                return null;
            }

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setValidating(false);
                factory.setNamespaceAware(false); // Set to false to make XPath easier for MECA files

                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(sb.toString())));

                String xmlHref = null;
                String pdfHref = null;

                NodeList instances = doc.getElementsByTagName("instance");
                for (int i = 0; i < instances.getLength(); i++) {
                    Element el = (Element) instances.item(i);
                    String mediaType = el.getAttribute("media-type");
                    String href = el.getAttribute("href");

                    if ("application/xml".equalsIgnoreCase(mediaType)) {
                        xmlHref = href;
                    } else if ("application/pdf".equalsIgnoreCase(mediaType)) {
                        pdfHref = href;
                    }
                }

                if (xmlHref == null) return null;

                String baseUri = manifestCu.getUrl();
                String xmlUrl = resolveRelative(baseUri, xmlHref);
                String pdfUrl = (pdfHref != null) ? resolveRelative(baseUri, pdfHref) : null;

                ArticleFiles af = new ArticleFiles();
                CachedUrl xmlCu = au.makeCachedUrl(xmlUrl);

                if (xmlCu != null && xmlCu.hasContent()) {
                    af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, xmlCu);
                    af.setFullTextCu(xmlCu);

                    if (pdfUrl != null) {
                        CachedUrl pdfCu = au.makeCachedUrl(pdfUrl);
                        if (pdfCu != null && pdfCu.hasContent()) {
                            af.setFullTextCu(pdfCu);
                        }
                    }
                    log.debug3("SUCCESS: Processed manifest for " + xmlUrl);
                    return af;
                }
            } catch (Exception e) {
                log.error("Parsing failed for " + manifestCu.getUrl(), e);
            }
            return null;
        }

        private class ManifestHandler extends DefaultHandler {
            private String xmlHref = null;
            private String pdfHref = null;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                if ("instance".equalsIgnoreCase(localName) || "instance".equalsIgnoreCase(qName)) {
                    String mediaType = attributes.getValue("media-type");
                    String href = attributes.getValue("href");
                    if ("application/xml".equalsIgnoreCase(mediaType)) {
                        xmlHref = href;
                    } else if ("application/pdf".equalsIgnoreCase(mediaType)) {
                        pdfHref = href;
                    }
                }
            }
            public String getXmlHref() { return xmlHref; }
            public String getPdfHref() { return pdfHref; }
        }


        private String getHrefByMediaType(Document doc, String mediaType) {
            NodeList instances = doc.getElementsByTagName("instance");
            for (int i = 0; i < instances.getLength(); i++) {
                Element el = (Element) instances.item(i);
                if (mediaType.equals(el.getAttribute("media-type"))) {
                    return el.getAttribute("href");
                }
            }
            return null;
        }

        private String resolveRelative(String base, String relative) {
            int lastSlash = base.lastIndexOf('/');
            return base.substring(0, lastSlash + 1) + relative;
        }

        @Override
        public ArticleFiles next() {
            ArticleFiles ret = nextAf;
            nextAf = null;
            return ret;
        }
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
