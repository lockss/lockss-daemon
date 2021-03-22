package org.lockss.plugin.atypon.asco;

import org.lockss.plugin.atypon.BaseAtyponArticleIteratorFactory;
import org.lockss.util.Logger;

public class AscoArticleIteratorFactory extends BaseAtyponArticleIteratorFactory {

  private static final Logger log = Logger.getLogger(AscoArticleIteratorFactory.class);

  // For ASCO overcrawling we need to further exclude urls with '_suppl' in them
  private static final String ASCO_PATTERN_TEMPLATE_WITH_ABSTRACT =
      "\"^%sdoi/((abs|full|e?pdf|e?pdfplus)/)?[.0-9]+/(?!_suppl)\", base_url";
  // If it ever turns out we want to not count Meeting Abstracts as Article files use this pattern which will
  // exclude the urls with '_suppl' in the pattern. Which indicates a meeting abstract
  //"\"^%sdoi/((abs|full|e?pdf|e?pdfplus)/)?[.0-9]+/[A-z]+[.0-9]+(?!_suppl)$\", base_url";
  private static final String ASCO_PATTERN_TEMPLATE =
      "\"^%sdoi/((full|e?pdf|e?pdfplus)/)?[.0-9]+/(?!_suppl)\", base_url";
  // Ibid.
  //"\"^%sdoi/((full|e?pdf|e?pdfplus)/)?[.0-9]+/[A-z]+[.0-9]+(?!_suppl)$\", base_url";

  @Override
  protected String getPatternTemplate() {
    return ASCO_PATTERN_TEMPLATE;
  }
  @Override
  protected String getPatternWithAbstractTemplate() {
    return ASCO_PATTERN_TEMPLATE_WITH_ABSTRACT;
  }


}
