package org.lockss.plugin.ingenta;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

/*
Provider: Ingenta Connect
Database: Ingenta Connect
Content: application/x-research-info-systems

TY  - ABST
AU  - Bissell, Karen
TI  - Essential medicines for chronic respiratory diseases: can people breathe easily in low-income countries?
JO  - The International Journal of Tuberculosis and Lung Disease
PY  - 2015-01-01T00:00:00///
VL  - 19
IS  - 1
SP  - 1
EP  - 1
UR  - https://www.ingentaconnect.com/content/iuatld/ijtld/2015/00000019/00000001/art00001
M3  - doi:10.5588/ijtld.14.0862
UR  - https://doi.org/10.5588/ijtld.14.0862
ER  - 

 */
public class IngentaRisMetadataExtractorFactory2020 implements FileMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(IngentaRisMetadataExtractorFactory2020.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {

        IngentaRisMetadataExtractor ris = new IngentaRisMetadataExtractor();

        ris.addRisTag("DA", MetadataField.FIELD_DATE);
        ris.addRisTag("JO", MetadataField.FIELD_PUBLICATION_TITLE);
        ris.addRisTag("PB", MetadataField.FIELD_PUBLISHER);
        ris.addRisTag("TI", MetadataField.FIELD_ARTICLE_TITLE);
        ris.addRisTag("SP", MetadataField.FIELD_START_PAGE);
        ris.addRisTag("VL", MetadataField.FIELD_VOLUME);
        ris.addRisTag("IS", MetadataField.FIELD_ISSUE);
        ris.addRisTag("SN", MetadataField.FIELD_ISSN);
        ris.addRisTag("M3", MetadataField.FIELD_DOI);

        return ris;
    }

    public class IngentaRisMetadataExtractor
            extends RisMetadataExtractor {

        // override this to do some additional attempts to get valid data before emitting
        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
                throws IOException, PluginException {

            // this extracts from th file and cooks the data according to the map
            ArticleMetadata am = extract(target, cu);
            String url = cu.getUrl();

            log.debug3("Ingenta url =======" + url + "===========");

            String html_appendix = "?crawler=true&mimetype=text/html";
            String html_appendix2 = "?crawler=true";
            String new_access_url = url;
            ArchivalUnit au = cu.getArchivalUnit();

            CachedUrl potential_cu = null;
            CachedUrl pdf_potential_cu = null;

            /*
            Over the years, Ingenta has been sending inconsistent data across two different domains,
            In order to make sense of the data backwards compitable, need to test multiple cases as beneath
             */

            if (url.contains("?format=ris")) {
                // First step, test if www version PDF file exist: http://www.ingentaconnect.com/content/iuatld/ijtld/2016/00000020/00000011/art00001?crawler=true&mimetype=application/pdf
                new_access_url = url.replace("?format=ris", "") + "?crawler=true&mimetype=application/pdf";
                potential_cu = cu.getArchivalUnit().makeCachedUrl(new_access_url);

                log.debug3("url = " + url + ", Pre Case: new_access_url = " + new_access_url);

                if ( (potential_cu != null) && (potential_cu.hasContent()) ){
                    log.debug3("url = " + url + ", CASE-1: new_access_url = " + new_access_url);
                    if (am.get(MetadataField.FIELD_ACCESS_URL) == null) {
                        am.put(MetadataField.FIELD_ACCESS_URL, new_access_url);
                    } else {
                        am.replace(MetadataField.FIELD_ACCESS_URL, new_access_url);
                    }
                } else {
                    // Otherwise,   // First step, test if api version PDF file exist: http://api.ingentaconnect.com/content/iuatld/ijtld/2016/00000020/00000011/art00001?crawler=true&mimetype=application/pdf
                    String pdf_new_access_url = url.replace("?format=ris", "").replace("www", "api") + "?crawler=true&mimetype=application/pdf";
                    pdf_potential_cu = cu.getArchivalUnit().makeCachedUrl(pdf_new_access_url);

                    log.debug3("url = " + url + ", Case-2 WWW PDF: new_access_url = " + pdf_new_access_url);

                    if ( (pdf_potential_cu != null) && (pdf_potential_cu.hasContent()) ){
                        log.debug3("url = " + url + ", CASE-2: new_access_url = " + pdf_new_access_url);
                        if (am.get(MetadataField.FIELD_ACCESS_URL) == null) {
                            am.put(MetadataField.FIELD_ACCESS_URL, pdf_new_access_url);
                        } else {
                            am.replace(MetadataField.FIELD_ACCESS_URL, pdf_new_access_url);
                        }
                    } else {
                        // If no PDF exist, test text/html: http://www.ingentaconnect.com/content/iuatld/ijtld/2016/00000020/00000011/art00001?crawler=true&mimetype=text/html
                        new_access_url = url.replace("?format=ris", "") + "?crawler=true&mimetype=text/html";
                        potential_cu = cu.getArchivalUnit().makeCachedUrl(new_access_url);

                        if ((potential_cu != null) && (potential_cu.hasContent())) {
                            log.debug3("url = " + url + ", CASE-3: new_access_url = " + new_access_url);
                            if (am.get(MetadataField.FIELD_ACCESS_URL) == null) {
                                am.put(MetadataField.FIELD_ACCESS_URL, new_access_url);
                            } else {
                                am.replace(MetadataField.FIELD_ACCESS_URL, new_access_url);
                            }
                        } else {
                            // If no PDF exist, test crawler=true only: http://www.ingentaconnect.com/content/iuatld/ijtld/2016/00000020/00000011/art00001?crawler=true
                            new_access_url = url.replace("?format=ris", "") + "?crawler=true";
                            potential_cu = cu.getArchivalUnit().makeCachedUrl(new_access_url);

                            if ((potential_cu != null) && (potential_cu.hasContent())) {
                                log.debug3("url = " + url + ", CASE-4: new_access_url = " + new_access_url);
                                if (am.get(MetadataField.FIELD_ACCESS_URL) == null) {
                                    am.put(MetadataField.FIELD_ACCESS_URL, new_access_url);
                                } else {
                                    am.replace(MetadataField.FIELD_ACCESS_URL, new_access_url);
                                }
                            }
                        }
                    }
                }
            }


            String publisherName = "Ingenta Connect";

            TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
            if (tdbau != null) {
                publisherName =  tdbau.getPublisherName();
            }

            am.put(MetadataField.FIELD_PUBLISHER, publisherName);

            emitter.emitMetadata(cu, am);
        }

    }
}