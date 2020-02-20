package org.lockss.plugin.clockss.scientificvideoprotocols;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

/*
Here is the sample data from .ris file
TY  - JOUR
AU  - Ramakrishnan, Venki
AU  - Alsari, Mejd
PY  - 2019
DA  - 2019/6/11
TI  - The Ribosome Under Synchrotron Light
JO  - Scientific Video Protocols
SP  - 1
VL  - 1
IS  - 1
AB  -
SN  - 2632-4350
UR  - https://doi.org/10.32386/scivpro.000014
DO  - 10.32386/scivpro.000014
ER  -
 */
public class ScientificVideoProtocolsRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(ScientificVideoProtocolsRisMetadataExtractorFactory.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {

        ScientificVideoProtocolsRisMetadataExtractor ris = new ScientificVideoProtocolsRisMetadataExtractor();

        ris.addRisTag("DA", MetadataField.FIELD_DATE);
        ris.addRisTag("JO", MetadataField.FIELD_PUBLISHER);
        ris.addRisTag("TI", MetadataField.FIELD_ARTICLE_TITLE);
        ris.addRisTag("SP", MetadataField.FIELD_START_PAGE);
        ris.addRisTag("VL", MetadataField.FIELD_VOLUME);
        ris.addRisTag("IS", MetadataField.FIELD_ISSUE);
        ris.addRisTag("SN", MetadataField.FIELD_ISSN);
        ris.addRisTag("DO", MetadataField.FIELD_DOI);
        // Do not use UR listed in the ris file! It will get set to full text CU by daemon
        return ris;
    }

    public static class ScientificVideoProtocolsRisMetadataExtractor
            extends RisMetadataExtractor {

        // override this to do some additional attempts to get valid data before emitting
        @Override
        public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter)
                throws IOException, PluginException {

            // this extracts from th file and cooks the data according to the map
            ArticleMetadata am = extract(target, cu);

            // post-cook processing...
            // check for existence of content file - return without emitting if not there
            String url_string = cu.getUrl();
            String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
            ArchivalUnit au = cu.getArchivalUnit();
            CachedUrl fileCu = au.makeCachedUrl(pdfName);
            log.debug3("Check for existence of " + pdfName);
            if(fileCu == null || !(fileCu.hasContent())) {
                log.debug3(pdfName + " was not in cu");
                return; // do not emit, just return - no content
            }


            am.put(MetadataField.FIELD_ACCESS_URL, pdfName);

            emitter.emitMetadata(cu, am);
        }

    }
}
