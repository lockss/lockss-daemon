package org.lockss.plugin.informaworld;

import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class InformaworldHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
                                               String encoding)
      throws PluginException {
    HtmlTransform[] transforms = new HtmlTransform[] {

        /*
         * Exclude Institution Name
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("td", "id", "instlistbar")),

    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));
  }

}
