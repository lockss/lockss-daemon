package org.lockss.plugin.atypon.aslha;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.HttpHttpsUrlHelper;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.plugin.atypon.BaseAtyponMetadataUtil;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AmericanSpeechLanguageHearingAssocHtmlMetadataExtractorFactory extends BaseAtyponHtmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger(AmericanSpeechLanguageHearingAssocHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new AmericanSpeechLanguageHearingAssocAtyponHtmlMetadataExtractor();
  }

  public static class AmericanSpeechLanguageHearingAssocAtyponHtmlMetadataExtractor
          extends BaseAtyponHtmlMetadataExtractor {

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException {

      // NOTE: MarkAllen plugins Override this extract  method and then calls it via super.extract() after
      //       performing additional checks on Date and Doi.

      ArticleMetadata am =
              new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);

      am.cook(getTagMap());
      /*
       * if, due to overcrawl, we got to a page that didn't have anything
       * valid, eg "this page not found" html page
       * don't emit empty metadata (because defaults would get put in
       * Must do this after cooking, because it checks size of cooked info
       */
      if (am.isEmpty()) {
        return;
      }

      String additionalVolume = null;

      try {
        // Attempt to fetch metadata using the new code
        additionalVolume = getAdditionalMetadata(cu, am);
        if (additionalVolume != null) {
          additionalVolume = additionalVolume.trim();
        }
      } catch (UnsupportedOperationException | NoSuchMethodError e) {
        // Handle cases where the method does not exist or isn't supported in legacy code
        log.debug3("getAdditionalMetadata is not supported in this environment. Falling back to default behavior.");
        additionalVolume = ""; // Or some other default value
      }

      log.debug3("additionalVolume in code=============" + additionalVolume);
      if (additionalVolume == null) {
        log.debug3("additionalVolume in code=============volume is nuuuuuulll");
      } else {
        log.debug3("additionalVolume in code=============volume is NOT nuuuuuulll, additionalVolume = " + additionalVolume + ", volume_len = " + additionalVolume.length());
      }

      //if (volume != null && volume !="") {
      if (additionalVolume != null && !additionalVolume.equals("")) {
        log.debug3("AmericanSpeechLanguageHearingAssoc Check: Volume--------getAdditionalMetadata: volume Successfully-------");
        am.put(MetadataField.FIELD_VOLUME, additionalVolume);
      } else {
        log.debug3("AmericanSpeechLanguageHearingAssoc Check: Volume--------getAdditionalMetadata: volume Failed-------");
      }

      // Only emit if this item is likely to be from this AU
      // protect against counting overcrawled articles
      ArchivalUnit au = cu.getArchivalUnit();
      log.debug3("AmericanSpeechLanguageHearingAssoc Check: ---------AmericanSpeechLanguageHearingAssocAtyponHtmlMetadataExtractor start checking-------");
      if (!BaseAtyponMetadataUtil.metadataMatchesTdb(au, am)) {
        log.debug3("AmericanSpeechLanguageHearingAssoc Check: ---------AmericanSpeechLanguageHearingAssocAtyponHtmlMetadataExtractor failed-------");
        return;
      } else {
        log.debug3("AmericanSpeechLanguageHearingAssoc Check: ---------AmericanSpeechLanguageHearingAssocAtyponHtmlMetadataExtractor succeed-------");
      }

      /*
       * Fill in DOI, publisher, other information available from
       * the URL or TDB
       * CORRECT the access.url if it is not in the AU
       */
      BaseAtyponMetadataUtil.completeMetadata(cu, am);

      HttpHttpsUrlHelper helper = new HttpHttpsUrlHelper(cu.getArchivalUnit(),
              ConfigParamDescr.BASE_URL.getKey(),
              "base_url");
      String url = am.get(MetadataField.FIELD_ACCESS_URL);

      if (url != null) {
        url = helper.normalize(url);
        am.replace(MetadataField.FIELD_ACCESS_URL, url);
      }
      // If we've gotten this far, emit
      log.debug3("AmericanSpeechLanguageHearingAssoc Check: ---------AmericanSpeechLanguageHearingAssocAtyponHtmlMetadataExtractorFactory emitting url = " + url);
      emitter.emitMetadata(cu, am);

    }

    private String getAdditionalMetadata(CachedUrl cu, ArticleMetadata am)
    {

      log.debug3("AmericanSpeechLanguageHearingAssoc Check: Volume--------getAdditionalMetadata-------");
      InputStream in = cu.getUnfilteredInputStream();
      if (in != null) {
        try {
          String volume = null;
          volume = getVolumeNumber(in, cu.getEncoding(), cu.getUrl());
          in.close();

          log.debug3("AmericanSpeechLanguageHearingAssoc Check: Volume--------volume returned------" + volume);

          return volume;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return null;
    }

    /*
   view-source:https://pubs.asha.org/doi/full/10.1044/2023_JSLHR-22-00309
   <span class="citation-line">Volume 67, Issue 1</span>
    */
    protected String getVolumeNumber(InputStream in, String encoding, String url) {
      try {
        Document doc = Jsoup.parse(in, encoding, url);

        // Fix the selector to match your actual HTML example
        Elements spanElements = doc.select("span.citation-line");

        log.debug3("Check: Found " + spanElements.size() + " citation-line span elements for URL: " + url);

        if (!spanElements.isEmpty()) {
          String rawText = spanElements.get(0).text();
          log.debug3("Check: Raw volume text: \"" + rawText + "\" from URL: " + url);

          // Extract volume number using regex
          Pattern pattern = Pattern.compile("volume\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
          Matcher matcher = pattern.matcher(rawText);

          if (matcher.find()) {
            String volumeNumber = matcher.group(1);
            log.debug3("Check: Extracted volume number: \"" + volumeNumber + "\" from URL: " + url);
            return volumeNumber;
          } else {
            log.debug3("Check: No volume number found in: \"" + rawText + "\", URL: " + url);
          }
        } else {
          log.debug3("Check: No matching <span class=\"citation-line\"> element found for URL: " + url);
        }
      } catch (IOException e) {
        log.debug3("Error while parsing volume number for URL: " + url, e);
      }

      return null;
    }
  }
}