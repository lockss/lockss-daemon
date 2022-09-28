package org.lockss.plugin.taar;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class TaarArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(TaarArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE =
      "\"^%sarticles/%s-\", base_url, journal_id"; // can't use volume to limit articles. as url numbers do not necessarily match metadata

  private static final Pattern ART_FULLTEXT_PATTERN = Pattern.compile("/articles/([^.?]+)$", Pattern.CASE_INSENSITIVE);
  // http://dev-liverpoolup.cloudpublish.co.uk/read/?item_type=journal_article&item_id=20098&mode=download
  private static final Pattern ART_PDF_PATTERN = Pattern.compile("/articles/(.+)[.]pdf$", Pattern.CASE_INSENSITIVE);

  private final String ART_REPLACEMENT = "/articles/$1";
  private final String PDF_REPLACEMENT = "/articles/$1.pdf";

  // RIS and Bibtex are actually coming from crossref, so ignore them.

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setRootTemplate(ROOT_TEMPLATE)
        .setPatternTemplate(PATTERN_TEMPLATE));

    builder.addAspect(ART_FULLTEXT_PATTERN,
        ART_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    return builder.getSubTreeArticleIterator();
  }


  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}