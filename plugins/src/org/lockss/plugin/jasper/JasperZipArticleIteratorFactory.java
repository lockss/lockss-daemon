package org.lockss.plugin.jasper;

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

public class JasperZipArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(JasperZipArticleIteratorFactory.class);

  protected static final String ALL_ZIP_JSON_PATTERN_TEMPLATE =
      "\"[^/]+/.*\\.tar.gz!/.*\\.json$\"";

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
      Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
          Pattern.CASE_INSENSITIVE);

  // ... 2051-5960/00003741594643f4996e2555a01e03c7/data/s40478-018-0619-9.pdf
  // ... 2051-5960/00003741594643f4996e2555a01e03c7/data/metadata/metadata.json
  public static final Pattern PDF_PATTERN = Pattern.compile("/(.*)/data/(.*)\\.pdf$", Pattern.CASE_INSENSITIVE);
  private static final String PDF_REPLACEMENT = "/$1/data/$2.pdf";
  // metadata
  public static final String JSON_REPLACEMENT = "/$1/data/metadata/metadata.json";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
        .setTarget(target)
        .setPatternTemplate(ALL_ZIP_JSON_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
        .setExcludeSubTreePattern(SUB_NESTED_ARCHIVE_PATTERN));

    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // While we can't identify articles that are *just* PDF which is why they
    // can't trigger an articlefiles by themselves, we can identify them
    // by replacement and they should be the full text CU.
    builder.addAspect(JSON_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    //Now set the order for the full text cu
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ARTICLE_METADATA); // though if it comes to this it won't emit

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
