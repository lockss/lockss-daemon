package uk.org.lockss.plugin.drogoresearch;

import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class DrogoResearchHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
                                               String encoding)
      throws PluginException {
    HtmlTransform[] transforms = new HtmlTransform[] {

        /*
         * Contains the current date.
         * 
         * Exclude <div id="date">
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("div", "id", "date")),
        
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));
  }

}
