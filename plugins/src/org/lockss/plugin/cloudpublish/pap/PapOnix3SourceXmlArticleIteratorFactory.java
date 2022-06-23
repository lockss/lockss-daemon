package org.lockss.plugin.cloudpublish.pap;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.onixbooks.Onix3BooksXmlArticleIteratorFactory;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class PapOnix3SourceXmlArticleIteratorFactory extends Onix3BooksXmlArticleIteratorFactory {
  protected static Logger log = Logger.getLogger(PapOnix3SourceXmlArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s\",base_url";
  private static final String PATTERN_TEMPLATE = "\"%s%s/.*\\.xml$\",base_url,directory";

  protected static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);

  protected static final String XML_REPLACEMENT = "/$1.xml";


  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    builder.addAspect(XML_PATTERN,
        XML_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }


  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
