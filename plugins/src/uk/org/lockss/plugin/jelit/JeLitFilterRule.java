package uk.org.lockss.plugin.jelit;

import java.io.*;

import org.lockss.filter.*;
import org.lockss.plugin.FilterRule;

public class JeLitFilterRule implements FilterRule {

  private static final String filterStart = "<p>This list was generated on <strong>";
  private static final String filterEnd = "</strong>.</p>";

  public Reader createFilteredReader(Reader reader) {
    HtmlTagFilter.TagPair pair =
      new HtmlTagFilter.TagPair(filterStart, filterEnd, true);
    Reader tagFilter = new HtmlTagFilter(reader, pair);
    return tagFilter;
  }
}
