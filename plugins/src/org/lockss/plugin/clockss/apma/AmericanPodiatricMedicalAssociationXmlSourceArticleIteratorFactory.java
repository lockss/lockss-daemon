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

package org.lockss.plugin.clockss.apma;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class AmericanPodiatricMedicalAssociationXmlSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

    // Their folder structure is not consistent, it appears to be in these two formats for now:
    //https://clockss-test.lockss.org/sourcefiles/apma-released/2020/apms-110-2.zip!/Assets/i8750-7315-110-2-Article_1.pdf
    //https://clockss-test.lockss.org/sourcefiles/apma-released/2020/apms-110-2.zip!/XML/i8750-7315-110-2-Article_1.xml
    
    //https://clockss-test.lockss.org/sourcefiles/apma-released/2020/APMS_109_1_D!/XML/15-168.xml
    //https://clockss-test.lockss.org/sourcefiles/apma-released/2020/APMS_109_1_D!/XML/i8750-7315-110-2-Article_1.xml

    //https://clockss-test.lockss.org/sourcefiles/apma-released/2020/APMS_109_1_D!/Assets/15-168.pdf
    //https://clockss-test.lockss.org/sourcefiles/apma-released/2020/APMS_109_1_D!/Assets/8750-7315-109_1_a1.pdf.pdf

    //They also have issue.xml, and meta-data.xml which need to be excluded
    protected static Logger log = Logger.getLogger(AmericanPodiatricMedicalAssociationXmlSourceArticleIteratorFactory.class);

    /*
    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s%s/.*\\.zip!/(.*)\\.(xml|pdf)$\", base_url, directory";
    */

    // Exclude cov.pdf & toc.pdf since it has no matching xml file
    // http://clockss-ingest.lockss.org/sourcefiles/apma-released/2020/apms-110-2.zip!/Assets/i8750-7315-110-2-cov.pdf
    //http://clockss-ingest.lockss.org/sourcefiles/apma-released/2020/apms-110-3.zip!/Assets/i8750-7315-110-3-toc.pdf
    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s%s/.*\\.zip!/[^/]+/(.*(?<!cov)(?<!toc))\\.(xml|pdf)$\", base_url, directory";


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

    public static final Pattern XML_PATTERN = Pattern.compile("/XML/((?!.*issue).*)\\.xml$", Pattern.CASE_INSENSITIVE);
    public static final Pattern PDF_PATTERN = Pattern.compile("/Assets/(.*)\\.pdf$", Pattern.CASE_INSENSITIVE);
    public static final String XML_REPLACEMENT = "/XML/$1.xml";
    private static final String PDF_REPLACEMENT = "/Assets/$1.pdf";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        // no need to limit to ROOT_TEMPLATE
        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
                .setExcludeSubTreePattern(getExcludeSubTreePattern())
                .setVisitArchiveMembers(true)
                .setVisitArchiveMembers(getIsArchive()));

        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_XML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        return builder.getSubTreeArticleIterator();
    }

    protected boolean getIsArchive() {
        return true;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
