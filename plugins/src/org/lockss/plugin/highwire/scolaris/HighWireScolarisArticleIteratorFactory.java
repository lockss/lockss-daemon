package org.lockss.plugin.highwire.scolaris;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.highwire.HighWireJCoreArticleIteratorVIPFactory;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class HighWireScolarisArticleIteratorFactory extends HighWireJCoreArticleIteratorVIPFactory {

  private static final Logger log = Logger.getLogger(HighWireScolarisArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%scontent/%s/%s/\", base_url, journal_id, volume_name";

  protected static final String PATTERN_TEMPLATE =
      "\"^%scontent/%s/%s/(?!.*[.]toc$)([^/]+/)([^/]+/)?([^./?&]+([.]\\d{1,4})?)$\", base_url, journal_id, volume_name";

  // the other patterns are inherited from HighWireJCoreArticleIteratorVIPFactory

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au) {

      @Override
      protected BuildableSubTreeArticleIterator instantiateBuildableIterator() {
        return new BuildableSubTreeArticleIterator(au, spec) {

          @Override
          protected ArticleFiles createArticleFiles(CachedUrl cu) {
            ArchivalUnit au = cu.getArchivalUnit();
            CachedUrl toc = au.makeCachedUrl(cu.getUrl() + ".toc");
            if ((toc != null) && toc.hasContent()) {
              return null;
            }
            return super.createArticleFiles(cu);
          }
        };
      }
    };

    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    // set up landing page to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU
    // until this is deprecated
    // Note: Often the landing page is also the fulltext html
    builder.addAspect(
        LANDING_PATTERN, LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);

    builder.addAspect(
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // set up pdf landing page to be an aspect
    builder.addAspect(
        PDF_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);

    // set up abstract/extract to be an aspect
    builder.addAspect(Arrays.asList(
            ABSTRACT_REPLACEMENT, EXTRACT_REPLACEMENT),
        ArticleFiles.ROLE_ABSTRACT);

    // set up figures-only to be an aspect
    builder.addAspect(FIGURES_REPLACEMENT,
        ArticleFiles.ROLE_FIGURES);

    // add metadata role from abstract, html or pdf landing page
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE));

    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ABSTRACT);

    return builder.getSubTreeArticleIterator();
  }
}
