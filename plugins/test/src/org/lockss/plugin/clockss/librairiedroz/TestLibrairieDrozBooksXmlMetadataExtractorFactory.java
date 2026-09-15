package org.lockss.plugin.clockss.librairiedroz;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Asserts on extracted metadata only. The delivered XML is loose -- records vary in which
 * optional elements they carry -- so nothing here asserts document shape or element presence.
 *
 * Differs structurally from the one-XML-to-one-PDF tests (cf. TestISPRSXmlMetadataExtractorFactory,
 * TestOrganizationForHumanBrainMappingJatsXmlMetadataExtractorFactory) in two ways:
 *
 *  1. mdlist.size() is the number of <mods> records, not 1.
 *  2. LibrairieDrozBooksXmlMetadataExtractorFactory.preEmitCheck() calls
 *     makeCachedUrl(epubUrl).hasContent(). A test that mocks ONLY the XML url takes the
 *     EMIT_WITHOUT_CONTENT_FILE fallback on every record and sets FIELD_ACCESS_URL to the
 *     XML url -- it passes while proving nothing about the ISBN -> filename mapping. So the
 *     EPUBs have to be mocked with content too.
 */
public class TestLibrairieDrozBooksXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorTest {

    private static final Logger log = Logger.getLogger(TestLibrairieDrozBooksXmlMetadataExtractorFactory.class);

    private static String BaseUrl = "http://source.host.org/sourcefiles/librairiedroz/";
    private static String Directory = "2026_01";
    // Delivery filename drives the epubs_<date>/ derivation -- must match mods_(\d+)\.xml
    private static String XmlFilename = "mods_20260907.xml";
    private static String EpubDir = "epubs_20260907/";

    private static final int EXPECTED_RECORD_COUNT = 10;

    /** <identifier type="isbn" displayLabel="epub"> for each record, in document order. */
    private static final String[] EPUB_ISBNS = {
        "9782600366496",  // Introduction a l'humanisme juridique
        "9782600365833",  // La Lyre et le Masque
        "9782600365284",  // L'empreinte des lointains
        "9782600364638",  // La Cite des dames de Christine de Pizan
        "9782600363662",  // Sermons sur Esaie
        "9782600362498",  // Paradoxes du lettre
        "9782600330664",  // Registres de la Compagnie des pasteurs, Tome V
        "9782600325998",  // Histoire universelle, Tome III
        "9782600323604",  // Le Quart Livre
        "9782600316095",  // L'Eventail et le Dandy
    };

    private String xmlUrl() {
        return BaseUrl + Directory + "/" + XmlFilename;
    }

    private String epubUrl(String isbn) {
        return BaseUrl + Directory + "/" + EpubDir + isbn + ".epub";
    }

    /**
     * Read a test resource as UTF-8.
     *
     * NOT getResourceContent(): that decodes with the platform default charset, which turns
     * the French titles into mojibake ("Ã " for "à", "Å" for "œ") before the XML parser ever
     * sees them. The delivery is UTF-8 and says so in its declaration, so decode it as such.
     */
    private String getResourceContentUtf8(String fname) throws Exception {
        InputStream is = getClass().getResourceAsStream(fname);
        assertNotNull("test resource not found on the classpath: " + fname, is);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            is.close();
        }
    }

    /** Mock the MODS xml. If mockEpubs, also give every EPUB url real content. */
    private MockCachedUrl setUpAu(MockArchivalUnit mau, boolean mockEpubs) throws Exception {
        // Resolves against this test's package dir, so the sample sits next to this class.
        String drozXml = getResourceContentUtf8("droz_mods.xml");
        assertNotNull(drozXml);

        CIProperties xmlHeader = new CIProperties();
        // Declare the charset so MockCachedUrl round-trips the String back to bytes as UTF-8.
        xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml; charset=utf-8");

        MockCachedUrl mcu = mau.addUrl(xmlUrl(), true, true, xmlHeader);
        mcu.setContent(drozXml);
        mcu.setContentSize(drozXml.length());
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml; charset=utf-8");

        if (mockEpubs) {
            CIProperties epubHeader = new CIProperties();
            epubHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/epub+zip");
            for (String isbn : EPUB_ISBNS) {
                MockCachedUrl ecu = mau.addUrl(epubUrl(isbn), true, true, epubHeader);
                // Content just has to be non-empty for hasContent() to be true.
                ecu.setContent("epub-placeholder");
                ecu.setContentSize("epub-placeholder".length());
                ecu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/epub+zip");
            }
        }
        return mcu;
    }

    private List<ArticleMetadata> extract(MockCachedUrl mcu) throws Exception {
        FileMetadataExtractor me = new LibrairieDrozBooksXmlMetadataExtractorFactory().
                createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
        FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
        return mle.extract(MetadataTarget.Any(), mcu);
    }

    /**
     * One ArticleMetadata per <mods> record, fields cooked, checked against record 1 of the
     * sample.
     */
    public void testExtractArticleXmlSchema() throws Exception {
        MockArchivalUnit mau = new MockArchivalUnit();
        MockCachedUrl mcu = setUpAu(mau, true);

        List<ArticleMetadata> mdlist = extract(mcu);
        assertNotEmpty(mdlist);
        assertEquals(EXPECTED_RECORD_COUNT, mdlist.size());

        ArticleMetadata md = mdlist.get(0);
        assertNotNull(md);

        // Built from \\u escapes so the expected value cannot itself be mangled by however
        // this source file happens to be compiled:
        //   "Introduction à l'humanisme juridique. Auteurs, œuvres, idées, formes, destinées"
        String title = "Introduction à l'humanisme juridique. "
                + "Auteurs, œuvres, idées, formes, destinées";
        assertEquals(title, md.get(MetadataField.FIELD_ARTICLE_TITLE));
        // titleInfo/title is cooked to BOTH title fields; the series name is not used.
        assertEquals(title, md.get(MetadataField.FIELD_PUBLICATION_TITLE));

        assertEquals("Librairie Droz", md.get(MetadataField.FIELD_PUBLISHER));
        assertEquals("fr", md.get(MetadataField.FIELD_LANGUAGE));
        // originInfo/dateIssued verbatim -- year only in this feed, no normalizing.
        assertEquals("2025", md.get(MetadataField.FIELD_DATE));
        // DOI_VALUE strips the https://doi.org/ prefix the publisher sends.
        assertEquals("10.47421/droz66495", md.get(MetadataField.FIELD_DOI));
        assertEquals("9782600066495", md.get(MetadataField.FIELD_ISBN));
        assertEquals("9782600366496", md.get(MetadataField.FIELD_EISBN));
        // relatedItem[@type='series']/part/detail[@type='volume']/number, verbatim. Roman
        // numerals ("CLIII") and dotted numbers ("5.13") appear in other records.
        assertEquals("212", md.get(MetadataField.FIELD_VOLUME));

        // Record 1 credits two editors (roleTerm "edt") and no author, and
        // MODS_AUTHOR_VALUE deliberately drops non-"aut" roles rather than promoting them.
        assertNull(md.get(MetadataField.FIELD_AUTHOR));

        // Set unconditionally in postCookProcess for every record.
        assertEquals(MetadataField.PUBLICATION_TYPE_BOOK, md.get(MetadataField.FIELD_PUBLICATION_TYPE));
        assertEquals(MetadataField.ARTICLE_TYPE_BOOKVOLUME, md.get(MetadataField.FIELD_ARTICLE_TYPE));
    }
}
