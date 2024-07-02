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

package org.lockss.plugin.archivepp;

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
TY  - JOUR
T1  - Constituents of essential oils from the leaf, fruit, and flower of Decaspermum parviflorum (Lam.) J. Scott
A1  - Tran Hau Khanh
A1  - Pham Hong Ban
A1  - Tran Minh Hoi
JF  - Archives of Pharmacy Practice
JO  - Arch Pharm Pract
SN  - 2320-5210
Y1  - 2020
VL  - 11
IS  - 1
SP  - 88
 */
public class ArchivesPharmacyPracticeRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
    private static final Logger log = Logger.getLogger(ArchivesPharmacyPracticeRisMetadataExtractorFactory.class);

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String contentType)
            throws PluginException {

        ArchivesPharmacyPracticeRisMetadataExtractor ris = new ArchivesPharmacyPracticeRisMetadataExtractor();

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
        return ris;
    }

    public static class ArchivesPharmacyPracticeRisMetadataExtractor
            extends RisMetadataExtractor {

        // override this to do some additional attempts to get valid data before emitting
        @Override
        public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
                throws IOException, PluginException {

            ArticleMetadata am = extract(target, cu);


            am.put(MetadataField.FIELD_PUBLICATION_TYPE,MetadataField.PUBLICATION_TYPE_JOURNAL);
            am.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_JOURNALARTICLE);

            emitter.emitMetadata(cu, am);
        }
    }
}
