package org.lockss.plugin.silverchair;

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


public class SilverchairCommonThemeBooksArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    /*
    GSW book has no book level PDF, it only has chapter level PDF and landing page to the chapters

    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700022/Front-Matter
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700023/Kaolins-Kaolins-and-Kaolins
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700025/Review-of-the-Structural-Relationships-of-the
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700027/The-Diverse-Industrial-Applications-of-Kaolin
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700029/Theories-of-Origin-for-the-Georgia-KaolinsA-Review
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700031/Recrystallization-of-Kaolinite-in-Gray-Kaolins
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700033/Sem-Investigation-of-a-Lateritic-Weathering
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700035/The-Mineralogical-and-Geochemical-Controls-that
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700039/The-Genesis-of-the-China-Clays-of-South-West
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700041/Mobility-of-U-and-Granite-Kaolinization-in
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700043/First-Occurrence-of-Dickite-in-Varicolored-Clays
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700045/The-Geology-Mineralogy-and-Exploitation-of
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700047/Mineralogical-and-Physical-Properties-of-the
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700049/Genetic-Significance-of-Paramagnetic-Centers-in
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700051/The-Nature-Detection-Occurence-and-Origin-of
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700053/The-Hydrothermal-Synthesis-of-Kaolinite-Up-to-350
    https://pubs.geoscienceworld.org/books/book/1811/chapter/107700055/Plates
    https://pubs.geoscienceworld.org/clays/books/book/1811/Kaolin-Genesis-and-Utilization

     */
    private static String ROOT_TEMPLATE = "\"%s\", base_url";
    private static String PATTERN_TEMPLATE =  "\"%s%s\", base_url, resource_id";


    private static Pattern PDF_LANDING_PAGE_PATTERN = Pattern.compile("/([^/]+)/books/(book|edited-volume|monograph)/([^\\.]+)$", Pattern.CASE_INSENSITIVE);

    private static String PDF_LANDING_PAGE_REPLACEMENT = "/$1/books/$2/$3";

    protected static Logger getLog() {
        return Logger.getLogger(SilverchairCommonThemeBooksArticleIteratorFactory.class);
    }
    
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {

        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                        ROOT_TEMPLATE,
                        PATTERN_TEMPLATE,
                        Pattern.CASE_INSENSITIVE);

        builder.addAspect(  PDF_LANDING_PAGE_PATTERN,
                            PDF_LANDING_PAGE_REPLACEMENT,
                            ArticleFiles.ROLE_FULL_TEXT_HTML,
                            ArticleFiles.ROLE_ARTICLE_METADATA);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
