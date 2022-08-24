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

package org.lockss.plugin.springer;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class SpringerJatsSourceZipXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

    protected static Logger log = Logger.getLogger(SpringerJatsSourceZipXmlArticleIteratorFactory.class);
    private static String ARTICLE_METADATA_JATS_META_ROLE = "ArticleMetadataJatsMeta";
    private static String ARTICLE_METADATA_JATS_XML_ROLE = "ArticleMetadataJatsXml";

    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s[^/]+/.*\\.zip!/(.*)_(Article|OnlinePDF|Book)(_\\d+)?\\.pdf$\", base_url";

    // Be sure to exclude all nested archives in case supplemental data is provided this way
    protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
            Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                    Pattern.CASE_INSENSITIVE);

    protected Pattern getExcludeSubTreePattern() {
        return SUB_NESTED_ARCHIVE_PATTERN;
    }

    protected String getIncludePatternTemplate() {
        return ALL_ZIP_XML_PATTERN_TEMPLATE;
    }

    protected static final Pattern PDF_PATTERN = Pattern.compile("/BodyRef/PDF/(.*)_(Article|OnlinePDF|Book)(_\\d+)?\\.pdf$");
    protected static final String PDF_REPLACEMENT = "/BodyRef/PDF/$1_$2$3.pdf";

    /*
    Article:
    ftp_PUB_20-10-20_01-03-55.zip!/JOU=12289/VOL=2020.13/ISU=6/ART=1514/12289_2019_Article_1514_nlm.xml.Meta
    ftp_PUB_20-10-20_01-03-55.zip!/JOU=12289/VOL=2020.13/ISU=6/ART=1514/BodyRef/PDF/12289_2019_Article_1514.pdf

    OnlinePDF:
    ftp_PUB_20-01-22_05-42-08.zip!/JOU=10050/VOL=2018.54/ISU=11/ART=12611/10050_2018_Article_12611_nlm.xml.Meta
    ftp_PUB_20-01-22_05-42-08.zip!/JOU=10050/VOL=2018.54/ISU=11/ART=12611/BodyRef/PDF/10050_2018_12611_OnlinePDF.pdf

    Book:
    ftp_PUB_20-10-02_01-04-17.zip!/BOK=978-1-4842-6103-3/978-1-4842-6103-3_Book_nlm.xml
    ftp_PUB_20-10-02_01-04-17.zip!/BOK=978-1-4842-6103-3/BodyRef/PDF/978-1-4842-6103-3_Book.pdf

     */
    protected static final String XML_REPLACEMENT = "/$1_$2$3_nlm.xml";

    protected static final String XML_META_REPLACEMENT = "/$1_$2$3_nlm.xml.Meta";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
                .setExcludeSubTreePattern(getExcludeSubTreePattern())
                .setVisitArchiveMembers(getIsArchive()));

        //The order of how Aspect defined is important here.
        
        builder.addAspect(PDF_PATTERN,
                          PDF_REPLACEMENT,
                          ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_REPLACEMENT,
                          ARTICLE_METADATA_JATS_XML_ROLE);

        builder.addAspect(XML_META_REPLACEMENT,
                          ARTICLE_METADATA_JATS_META_ROLE);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);
        
        //ArticleMetadata may be provided by both .xml and .xml.Meta file in case of Journals
        //For book/book series, ArticleMetadata is provided by .xml
        builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                      ARTICLE_METADATA_JATS_META_ROLE,
                                      ARTICLE_METADATA_JATS_XML_ROLE);

        return builder.getSubTreeArticleIterator();
    }

    // NOTE - for a child to create their own version of this
    // indicates if the iterator should descend in to archives (for tar/zip deliveries)
    protected boolean getIsArchive() {
        return true;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
