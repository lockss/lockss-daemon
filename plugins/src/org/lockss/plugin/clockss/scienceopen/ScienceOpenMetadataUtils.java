package org.lockss.plugin.clockss.scienceopen;

import org.lockss.config.TdbAu;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataField;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScienceOpenMetadataUtils {

  private static final Logger log = Logger.getLogger(ScienceOpenMetadataUtils.class);

  private static String ScienceOpen = "ScienceOpen";
  private static Pattern Science_Open = Pattern.compile("Science\\s*Open");

  public static void fillPublisherAndProviderFromTdb(ArticleMetadata am,
                                          CachedUrl cu) {

    // Fill in Publisher and Provider from TDB
    String publisherName = null;
    String providerName = null;
    TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
    if (tdbau != null) {
      publisherName =  tdbau.getPublisherName();
      providerName = tdbau.getProviderName();
    }
    // supply the publisher from the tdb file if the one in the metadata is null
    am.putIfBetter(MetadataField.FIELD_PUBLISHER, publisherName);
    // as ScienceOpen wants to all material they provide 'counted' as one,
    // setting the Provider field to ScienceOpen makes sense.
    log.info("provider: " + am.get(MetadataField.FIELD_PROVIDER));
    if (providerName != null) {
      am.put(MetadataField.FIELD_PROVIDER, providerName);
    }
    log.info("provider: " + am.get(MetadataField.FIELD_PROVIDER));
  }

  public static void normalizePublisher(ArticleMetadata am) {
    String publisherName = am.get(MetadataField.FIELD_PUBLISHER);
    Matcher m = Science_Open.matcher(publisherName);
    if (m.matches()) {
      log.info("Found an improperly formatted pub name:");

    }

  }
}
