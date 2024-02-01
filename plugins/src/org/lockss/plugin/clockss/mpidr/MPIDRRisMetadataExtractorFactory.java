/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.mpidr;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Here is the sample data from .ris file inside "volume" folder
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
Here is the sample data from .ris file inside "special" folder
TY  - JOUR
A1  - Watkins, Susan
A1  - Kohler, Hans-Peter
A1  - Behrman, Jere
A1  - Zulu, Eliya Msiyaphazi
T1  - Introduction to "Research on Demographic Aspects of HIV/AIDS in Rural Africa"
Y1  - 2003.09.19
JF  - Demographic Research
JO  - Demographic Research
SN  - 1435-9871
SP  - 1
EP  - 30
N1  - 10.4054/DemRes.2003.S1.1
VL  - Special 1
IS  - 1
UR  - https://www.demographic-research.org/special/1/1/default.htm
L1  - https://www.demographic-research.org/special/1/1/s1-1.pdf
L2  - https://www.demographic-research.org/special/1/1/s1-1.pdf
N2  - This paper introduces a set of papers presented at the conference "Research on Demographic Aspects of HIV/AIDS in Rural Africa", held at the Population Studies Center, University of Pennsylvania, October 28-29, 2002.    The aim of the conference was to provide a forum for the presentation of results, to an audience of experts, on a variety of demographic aspects relevant for the study of HIV/AIDS in rural Africa. The aim of this volume is to provide these results to a wider audience.  Although the topics covered are diverse, ranging from methodological issues in the study of HIV/AIDS such as sample attrition to substantive issues such as fertility, divorce, and womenâ€™s autonomy, the papers are united by their use of two similar data sets collected in rural Malawi and Kenya. This introduction thus begins by briefly describing the contents of the volume and the collaborators, and then focuses on a detailed description of the data used by all authors and on the threats to data quality in these contexts. We conclude that demographic studies of HIV/AIDS in rural Africa are likely to face similar threats, and that these should be routinely recognized and acknowledged.
ER  -

 */
public class MPIDRRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(MPIDRRisMetadataExtractorFactory.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {

        MPIDRRisMetadataExtractor ris = new MPIDRRisMetadataExtractor();

        ris.addRisTag("Y1", MetadataField.FIELD_DATE);
        ris.addRisTag("JO", MetadataField.FIELD_PUBLICATION_TITLE);
        ris.addRisTag("T1", MetadataField.FIELD_ARTICLE_TITLE);
        ris.addRisTag("VL", MetadataField.FIELD_VOLUME);
        ris.addRisTag("IS", MetadataField.FIELD_ISSUE);
        ris.addRisTag("SN", MetadataField.FIELD_ISSN);
        ris.addRisTag("N1", MetadataField.FIELD_DOI);
        ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
        ris.addRisTag("SP", MetadataField.FIELD_START_PAGE);
        ris.addRisTag("EP", MetadataField.FIELD_END_PAGE);
        // Do not use UR listed in the ris file! It will get set to full text CU by daemon
        return ris;
    }

    public static class MPIDRRisMetadataExtractor
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
            ArchivalUnit au = cu.getArchivalUnit();

            // match "2021_01", "2021_02", etc folder....
            String pattern = "(.*)([\\d_]{7,})(.*)";

            // Create a Pattern object
            Pattern r =
                    Pattern.compile(pattern);

            // Now create matcher object.
            Matcher m = r.matcher(url_string);
            String localUrl = "";
            if (m.find()) {
                log.debug3("Found value: " + m.group(0));
                log.debug3("Found value: " + m.group(1));
                log.debug3("Found value: " + m.group(2));
                log.debug3("Found value: " + m.group(3));
                localUrl = m.group(1) + m.group(2) + "/";
            } else {
                log.debug3("NO MATCH");
            }

            String pdfurl = am.getRaw("L1");

            String realUrl = pdfurl.substring(0, pdfurl.indexOf("org/")) + "org/";
            String pdfName = pdfurl.replace(realUrl, localUrl).replace(".ris", ".pdf");

            // Their special collection use Uppser case 'S' in PDF file, and lowercase 's' in .ris file
            // when references to the PDF file
            if (pdfurl.contains("special")) {
                 pdfName = pdfName.replace("s1", "S1")
                         .replace("s2", "S2")
                         .replace("s3", "S3");
            }

            log.debug3(String.format("pdfurl: %s, realUrl %s, pdfName %s", pdfurl, realUrl, pdfName));

            CachedUrl fileCu = au.makeCachedUrl(pdfName);
            
            log.debug3("Check for existence of " + pdfName);
            if (fileCu == null || !(fileCu.hasContent())) {
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
            am.put(MetadataField.FIELD_PUBLICATION_TYPE,MetadataField.PUBLICATION_TYPE_JOURNAL);
            am.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_JOURNALARTICLE);

            emitter.emitMetadata(cu, am);
        }
    }
}
