package org.lockss.plugin.silverchair;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;

public class ScBooksArticleIteratorFactory implements ArticleIteratorFactory,
  ArticleMetadataExtractorFactory {

  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = "\"^book\\.aspx\\?bookid=\\d+$\", base_url";

  private static final java.util.regex.Pattern
    HTML_PATTERN = Pattern.compile("/book\\.aspx\\?bookid=(\\d+)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/book.aspx?bookid=$1";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(
    ArchivalUnit au, MetadataTarget target)
    throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    builder.setSpec(target,
                    ROOT_TEMPLATE,
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    builder.addAspect(HTML_PATTERN,
                      HTML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                  ArticleFiles.ROLE_FULL_TEXT_HTML);
    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(
    MetadataTarget target) throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
