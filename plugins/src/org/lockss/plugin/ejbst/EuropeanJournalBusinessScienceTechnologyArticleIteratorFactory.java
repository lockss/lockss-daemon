package org.lockss.plugin.ejbst;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.wroclawmedicaluniversity.WroclawMedicalUniversityArticleIteratorFactory;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class EuropeanJournalBusinessScienceTechnologyArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(WroclawMedicalUniversityArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
    protected static final String PATTERN_TEMPLATE =
            "\"%s(pdfs|artkey)/\", base_url";

    private static final Pattern HTML_PATTERN = Pattern.compile(
            "/(artkey/.*)",
            Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/$1";

    /*
    private static final Pattern PDF_PATTERN = Pattern.compile(
            "/(pdfs/.*\\.pdf)",
            Pattern.CASE_INSENSITIVE);
    private static final String PDF_REPLACEMENT = "/$1";
     */


    //It has only html and PDF, no separate pages other aspects, like  abstract/full_tex
    //https://www.ejobsat.cz/artkey/ejo-201801-0001_intangible-assets-and-the-determinants-of-a-single-bank-relation-of-german-smes.php
    //https://www.ejobsat.cz/pdfs/ejo/2018/01/01.pdf
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(
                HTML_PATTERN,
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_ABSTRACT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        /*
        builder.addAspect(
            PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);
         */

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}


