package uk.org.lockss.plugin.drogoresearch;

import java.io.*;

import org.lockss.filter.*;
import org.lockss.plugin.FilterRule;

public class DrogoResearchFilterRule implements FilterRule {

  private static final String filterStart = "<div id=\"date\">";
  private static final String filterEnd = "</div>";

  public Reader createFilteredReader(Reader reader) {
    HtmlTagFilter.TagPair pair =
      new HtmlTagFilter.TagPair(filterStart, filterEnd, true);
    Reader tagFilter = new HtmlTagFilter(reader, pair);
    return tagFilter;
  }
}
