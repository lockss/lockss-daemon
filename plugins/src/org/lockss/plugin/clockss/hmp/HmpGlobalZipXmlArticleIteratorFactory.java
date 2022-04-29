package org.lockss.plugin.clockss.hmp;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;

public class HmpGlobalZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  protected static final String ALL_BUT_MACOS_ZIP_XML_PATTERN_TEMPLATE =
      "\"%s[^/]+/.*\\.zip!/(?!__MACOSX).*\\.xml$\", base_url";

  protected String getIncludePatternTemplate() {
    return ALL_BUT_MACOS_ZIP_XML_PATTERN_TEMPLATE;
  }

}
