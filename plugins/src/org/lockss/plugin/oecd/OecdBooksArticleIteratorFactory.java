package org.lockss.plugin.oecd;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class OecdBooksArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(OecdBooksArticleIteratorFactory.class);

  private static final String PATTERN_TEMPLATE =
      "\"%s(%s/%s_%s|sites/|deliver/)\", base_url, topic, book_title, book_id";

  // landing page
  // https://www.oecd-ilibrary.org/social-issues-migration-health/housing-and-inclusive-growth_6ef36f4b-en
  private static Pattern LANDING_PATTERN;

  // full text html
  // https://www.oecd-ilibrary.org/sites/6ef36f4b-en/index.html?itemId=/content/publication/6ef36f4b-en
  // full text pdf & epub
  // https://www.oecd-ilibrary.org/deliver/6ef36f4b-en.pdf?itemId=%2Fcontent%2Fpublication%2F6ef36f4b-en&mimeType=pdf
  // https://www.oecd-ilibrary.org/deliver/6ef36f4b-en.epub?itemId=%2Fcontent%2Fpublication%2F6ef36f4b-en&mimeType=epub
  // citation files
  // https://www.oecd-ilibrary.org/social-issues-migration-health/covid-19-and-well-being_6ef36f4b-en/cite/endnote

  // more complicated
  // https://www.oecd-ilibrary.org/nuclear-energy/the-economics-of-long-term-operation-of-nuclear-power-plants_9789264992054-en
  // https://www.oecd-ilibrary.org/the-economics-of-long-term-operation-of-nuclear-power-plants_5k409f793gls.pdf?itemId=%2Fcontent%2Fpublication%2F9789264992054-en&mimeType=pdf

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    String topic = au.getConfiguration().get("topic");
    String book_title = au.getConfiguration().get("book_title");
    String book_id= au.getConfiguration().get("book_id");

    LANDING_PATTERN = Pattern.compile(
        String.format("/(%s/%s_(%s))$", topic, book_title, book_id));
    String LANDING_REPLACEMENT = String.format("/%s/%s_$2", topic, book_title);

    String PDF_REPLACEMENT = "/deliver/$2.pdf?itemId=%2Fcontent%2Fpublication%2F$2&mimeType=pdf";
    String EPUB_REPLACEMENT = "/deliver/$2.epub?itemId=%2Fcontent%2Fpublication%2F$2&mimeType=epub";
    String HTML_REPLACEMENT = "/sites/$2/index.html?itemId=/content/publication/$2";

    // for LANDING replacement
    String RIS_REPLACEMENT = "/$1/cite/ris";
    String BIB_REPLACEMENT = "/$1/cite/bib";
    String ENDNOTE_REPLACEMENT = "/$1/cite/endnote";

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    builder.addAspect(
        LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);

    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(
        EPUB_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_EPUB);

    builder.addAspect(
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    // citation files
    builder.addAspect(
        RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);
    builder.addAspect(
        BIB_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_BIBTEX);
    builder.addAspect(
        ENDNOTE_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_ENDNOTE);

    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_EPUB
    );

    builder.setRoleFromOtherRoles(
        ArticleFiles.ROLE_CITATION,
        Arrays.asList(
            ArticleFiles.ROLE_CITATION_RIS,
            ArticleFiles.ROLE_CITATION_BIBTEX,
            ArticleFiles.ROLE_CITATION_ENDNOTE
        )
    );

    return builder.getSubTreeArticleIterator();
  }


  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    // use same metadataextractor as journals for finding pdf on page
    return new OecdJournalsArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA, true);
  }
}