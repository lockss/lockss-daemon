package org.lockss.plugin.atypon.markallen;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkAllenHtmlMetadataExtractorFactory extends BaseAtyponHtmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(MarkAllenHtmlMetadataExtractorFactory.class);


  public FileMetadataExtractor
  createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new MarkAllenHtmlMetadataExtractor();
  }

  public static class MarkAllenHtmlMetadataExtractor
      extends BaseAtyponHtmlMetadataExtractor {

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am =
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);

      am.cook(getTagMap());
      /*
       * if, due to overcrawl, we got to a page that didn't have anything
       * valid, eg "this page not found" html page
       * don't emit empty metadata (because defaults would get put in
       * Must do this after cooking, because it checks size of cooked info
       *
       * This check will be done again at the super.extract() stage but
       * that is fine.
       */
      if (am.isEmpty()) {
        return;
      }

      ArchivalUnit au = cu.getArchivalUnit();
      TdbAu tdbau = au.getTdbAu();

      String foundDate = am.get(MetadataField.FIELD_DATE);
      String foundDOI = am.get(MetadataField.FIELD_DOI);

      if (!StringUtils.isEmpty(foundDate)) {
        String AU_Year = tdbau.getYear();
        // date can come in many formats, so lets try to deal with them,
        // e.g. 2013/09/28, 2013-09-28, 9/28/2013, "September 28, 2013", "2013, Sep 28"
        // other formats are too tricky, e.g. 20130928 so this format should pass the check as well

        String foundYear = null;

        String[] splitDate = foundDate.split("/|-|, ");
        for (String piece : splitDate) {
          if (piece.length() == 4) {
            foundYear = piece;
          }
        }

        if (!StringUtils.isEmpty(foundYear) && (AU_Year != null) && !AU_Year.equals(foundYear)) {
          log.debug3("AUY Year is present and Does not Equal AU year, passing");
          return;
        }
      }

      if (!StringUtils.isEmpty(foundDOI)) {
        TypedEntryMap tfProps = au.getProperties();
        String JOURNAL_ID = tfProps.getString(ConfigParamDescr.JOURNAL_ID.getKey());
        // laboriously parse this out to the journal id that is embedded in the DOI
        // DOI for MarkAllen stuff always looks like this:
        // 10.12968/coan.2018.23.1.41
        // this was pulled in but doesnt look like markallen.
        // 10.1111/j.2044-3862.2009.tb00374.x
        // the letters between the '/' and the second '.' is the Journal ID
        // we extract it.

        int slashIdx = foundDOI.indexOf("/");
        String shouldBeJID = foundDOI.substring(slashIdx + 1, foundDOI.indexOf(".", slashIdx));

        if  ((shouldBeJID != null) && !shouldBeJID.equals(JOURNAL_ID)) {
          log.debug3("JID ARE NOT EQUAL, passing ");
          return;
        }
      }

      // if the journal_id and publication date match, continue the BaseAtypon extract
      super.extract(target, cu, emitter);
    }

  }

}
