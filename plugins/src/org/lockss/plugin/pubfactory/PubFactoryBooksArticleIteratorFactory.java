package org.lockss.plugin.pubfactory;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class PubFactoryBooksArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(PubFactoryBooksArticleIteratorFactory.class);

  // don't set the ROOT_TEMPLATE - it is just base_url

  private static final String PATTERN_TEMPLATE =
      "\"^%s(downloadpdf|view)/%s/%s\", base_url, book_isbn, book_isbn";

  // (?![^/]+issue[^/]+) is a negative lookahead to exclude issue TOC pages but to allow articles through
  // it must come before the bit that picks up the filename when it's not an issue
  // book landing page
  // https://www.manchesterhive.com/view/9781847794390/9781847794390.xml
  // NO PDF
  // book chapter
  // https://www.manchesterhive.com/view/9781847794390/9781847794390.00006.xml
  // https://www.manchesterhive.com/downloadpdf/9781847794390/9781847794390.00001.pdf
  private static final Pattern ART_LANDING_PATTERN = Pattern.compile("/view/(\\d+/\\d+\\.\\d+)\\.xml$", Pattern.CASE_INSENSITIVE);
  private static final Pattern ART_PDF_PATTERN = Pattern.compile("/downloadpdf/(\\d+/\\d+\\.\\d+)\\.pdf$", Pattern.CASE_INSENSITIVE);

  // how to get from one of the above to the other
  private final String LANDING_REPLACEMENT = "/view/$1.xml";
  private final String PDF_REPLACEMENT = "/downloadpdf/$1.pdf";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    builder.addAspect(ART_LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // make this one primary by defining it first
    builder.addAspect(ART_PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    // Leave the CITATION_RIS in because if just doing iterator, it's the only one set
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ABSTRACT);


    return builder.getSubTreeArticleIterator();
  }


  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}

