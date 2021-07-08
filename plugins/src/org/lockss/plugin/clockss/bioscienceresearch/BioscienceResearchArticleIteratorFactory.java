package org.lockss.plugin.clockss.bioscienceresearch;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class BioscienceResearchArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(BioscienceResearchArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%s\", base_url, directory";
    private static final String PATTERN_TEMPLATE = "\"%s%s.*/[^/]+\", base_url, directory";

    // https://clockss-test.lockss.org/sourcefiles/bioscienceresearch-released/2021_01/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/Scholar548.pdf
    // https://clockss-test.lockss.org/sourcefiles/bioscienceresearch-released/2021_01/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/548.xml

    // <base_url><directory>/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/548.xml
    // <base_url><directory>/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/Scholar548.pdf
    //                 new content only contains the number.
    // <base_url><directory>/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/548.pdf
    // this is a lot of group capturing, but since we need the bit between the "-" and second "/"
    // this string occurs before the \d+.pdf url
    protected static final Pattern XML_PATTERN = Pattern.compile("/([^-/]*)-([^/]*)/([^/]*/[^/]*)/(\\D*)(\\d+)\\.xml$");
    protected static final String XML_REPLACEMENT = "/$1-$2/$3/$5.xml";
    protected static final Pattern PDF_PATTERN = Pattern.compile("/([^-/]*)-([^/]*)/([^/]*/[^/]*)/(\\D*)(\\d+)\\.pdf$");
    protected static final String PDF_REPLACEMENT_1 = "/$1-$2/$3/$2$5.pdf";
    protected static final String PDF_REPLACEMENT_2 = "/$1-$2/$3/$5.pdf";
    protected static final List<String> PDF_REPLACEMENTS = new ArrayList<>();

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
            ROOT_TEMPLATE,
            PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        PDF_REPLACEMENTS.add(PDF_REPLACEMENT_1);
        PDF_REPLACEMENTS.add(PDF_REPLACEMENT_2);
        builder.addAspect(PDF_PATTERN,
            PDF_REPLACEMENTS,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_PATTERN,
            XML_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA,
            ArticleFiles.ROLE_FULL_TEXT_XML);

        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA) {
            @Override
            public void extract(MetadataTarget target, ArticleFiles af,
                                ArticleMetadataExtractor.Emitter emitter)
                throws IOException, PluginException {
                // if there is no metadata file, then this is the pdf associated with a "randomly" named xml file.
                // do not emit
                if (af.getRole(ArticleFiles.ROLE_ARTICLE_METADATA) == null){
                    log.debug3("found a null metadata url, ignoring");
                    CachedUrl cu = getCuToExtract(af);
                    AuUtil.safeRelease(cu);
                } else {
                    // otherwise we proceed as normal.
                    super.extract(target, af, emitter);
                }
            }

            @Override
            protected void checkAccessUrl(ArticleFiles af,
                                          CachedUrl cu, ArticleMetadata am) {
                log.debug3("CHECKING ACCESS URL: ");
                if (af.getFullTextUrl().contains(".xml") ) {
                    log.debug3("   it is an xml file, lets try to change: " + af.getFullTextUrl());
                    String access_val = am.get(MetadataField.FIELD_ACCESS_URL);
                    CachedUrl testCu = cu.getArchivalUnit().makeCachedUrl(access_val);
                    if ((testCu != null) && testCu.hasContent()) {
                        log.debug3("    ACCESS URL passed test, lets set it: " + access_val);
                        af.setFullTextCu(testCu);
                        af.setRoleString(ArticleFiles.ROLE_FULL_TEXT_PDF, access_val);
                    }
                }
                super.checkAccessUrl(af, cu, am);
            }};
    }

}