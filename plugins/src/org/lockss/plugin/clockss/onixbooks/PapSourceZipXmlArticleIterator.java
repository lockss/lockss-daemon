package org.lockss.plugin.clockss.onixbooks;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;

public class PapSourceZipXmlArticleIterator extends SourceZipXmlArticleIteratorFactory {

  // same general pattern as the parent, except we exclude the toc file(s)
  protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
      "\"%s[^/]+/.*\\.zip!/(?!.*issue-files.*).*\\.xml$\", base_url";

  protected String getIncludePatternTemplate() {
    return ALL_ZIP_XML_PATTERN_TEMPLATE;
  }

}
