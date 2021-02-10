package org.lockss.plugin.clockss.mpidr;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;

/*
Here is the sample data from .ris file inside volume folder
TY  - JOUR
A1  - Kannisto, VÃ¤inÃ¶
A1  - Nieminen, Mauri
A1  - Turpeinen, Oiva
T1  - Finnish Life Tables since 1751
Y1  - 1999.07.01
JF  - Demographic Research
JO  - Demographic Research
SN  - 1435-9871
N1  - 10.4054/DemRes.1999.1.1
VL  - 1
IS  - 1
UR  - https://www.demographic-research.org/volumes/vol1/1/default.htm
L1  - https://www.demographic-research.org/volumes/vol1/1/1-1.pdf
L2  - https://www.demographic-research.org/volumes/vol1/1/1-1.pdf
N2  - A recently completed series of life tables from 1751 to 1995 is used for identifying four stages of mortality transition in Finland, separated by the years 1880, 1945 and 1970.  The cyclical fluctuation of the death rate in the eighteenth and nineteenth centuries is measured and examined in relation to epidemics, famines and wars.  Important permanent changes in mortality also took place in this early period.  Each of the successive stages of transition produced its own characteristic pattern of mortality change which contrasted with those of the other stages.  Finally, the age profile of the years added to life is drawn to illustrate the end result of each stage of mortality transition. (All figures follow at the end of the document.)
ER  -
-
 */
public class MPIDRRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(MPIDRRisMetadataExtractorFactory.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {

        ScientificVideoProtocolsRisMetadataExtractor ris = new ScientificVideoProtocolsRisMetadataExtractor();

        ris.addRisTag("DA", MetadataField.FIELD_DATE);
        ris.addRisTag("JO", MetadataField.FIELD_PUBLICATION_TITLE);
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
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
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

            String publisherName = "Max Planck Insitute for Demographic Research";

            TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
            if (tdbau != null) {
                publisherName =  tdbau.getPublisherName();
            }

            am.put(MetadataField.FIELD_PUBLISHER, publisherName);

            emitter.emitMetadata(cu, am);
        }

    }
}
