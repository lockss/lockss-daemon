package org.lockss.plugin.clockss.airiti;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import java.io.IOException;

/*
Here is the sample data from .ris file
TY  - JOUR
T1  - How to Use Pectoral Nerve Blocks Effectively-An Evidence-Based Update
AU  - Hironobu Ueshima
AU  - Hiroshi Otake
AU  - Eiko Hara
AU  - Rafael Blanco
KW  - pectoral nerve (PECS) block
KW  - chest wall block
KW  - breast cancer surgery
KW  - perioperative pain management
JO  - Asian Journal of Anesthesiology
VL  - 57
IS  - 2
PY  - 2019
DA  - 2019/06/01
AB  - Since the original description in 2011, the array of pectoral nerve blocks has evolved. The pectoral nerve (PECS) block in conjunction with general anesthesia can decrease an additional analgesic in perioperative period for breast cancer surgeries. Current literature on the PECS block has reported three several types (PECS I, PECS II, and serratus plane blocks). The PECS I block is the same as to the first injection in the PECS II block. The second injection in the PECS II block and the serratus plane block blocks intercostal nerves (Th2-6) and provides an analgesic for the breast cancer surgery. However, the PECS I block (or first injection in the PECS II block) has no analgesic, because both lateral and medial pectralis nerve blocks are motor nerves. PECS block in previous reports, when added to opioid-based general anesthesia, may improve analgesia and decrease narcotic use for breast cancer surgery. Moreover, PECS block compares favorably with other regional techniques for selected types of surgery. A major limitation of the PECS block is that it cannot block the internal mammary region. Therefore, some studies have reported its ability to block the anterior branches of the intercostal nerve. PECS block is an effective analgesic tool for the anterolateral chest. In particular, the PECS block can provide more effective analgesia for breast cancer surgery.
SP  - 28
EP  - 36
LA  - English
SN  - 2468-824X
UR  - http://dx.doi.org/10.6859/aja.201906_57(2).0002
PB  - Taiwan Society of Anesthesiologists & Ainosco Press
DO  - 10.6859/aja.201906_57(2).0002
ER  - 
 */
public class AiritiMetadataExtractorFactory implements FileMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(AiritiMetadataExtractorFactory.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {

        AiritiMetadataExtractor ris = new AiritiMetadataExtractor();

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

    public static class AiritiMetadataExtractor
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

            emitter.emitMetadata(cu, am);
        }

    }
}
