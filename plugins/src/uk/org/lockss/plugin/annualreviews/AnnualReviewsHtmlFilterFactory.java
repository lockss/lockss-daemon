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
    HtmlTransform[] transforms = new HtmlTransform[] {
        // Filter out <select name="url">...</select>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("tr",
                                                                         "class",
                                                                         "identitiesBar")),
        // Filter out <div class="CitedBySectionContent">...</div>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "CitedBySectionContent")),
        // Filter out <table class="articleEntry">...</table>
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("table",
                                                                         "class",
                                                                         "articleEntry")),
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));
  }

}
