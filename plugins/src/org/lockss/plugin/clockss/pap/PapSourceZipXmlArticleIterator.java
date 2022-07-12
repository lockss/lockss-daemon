package org.lockss.plugin.clockss.pap;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;

public class PapSourceZipXmlArticleIterator extends SourceZipXmlArticleIteratorFactory {

  // same general pattern as the parent, except we exclude the toc file(s)
  protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
      "\"%s%s/.*\\.zip!/(?!.*issue-files.*).*\\.xml$\", base_url, directory";

  @Override
  protected String getIncludePatternTemplate() {
    return ALL_ZIP_XML_PATTERN_TEMPLATE;
  }

}
