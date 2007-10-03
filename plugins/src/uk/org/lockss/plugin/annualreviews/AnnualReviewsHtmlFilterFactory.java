package uk.org.lockss.plugin.annualreviews;

import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class AnnualReviewsHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // Filter out <select name="url">...</select>
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("tr",
                                                                                                      "class",
                                                                                                      "identitiesBar")));
  }

}
