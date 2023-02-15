package org.lockss.plugin.atypon.sage;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.HttpHttpsUrlHelper;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.plugin.atypon.BaseAtyponMetadataUtil;
import org.lockss.repository.LockssRepository;
import org.lockss.util.Constants;
import org.lockss.util.HtmlUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.*;

import org.jsoup.Jsoup;


public class SageAtyponHtmlMetadataExtractorFactory extends BaseAtyponHtmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger(SageAtyponHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new SageAtyponHtmlMetadataExtractor();
  }

  public static class SageAtyponHtmlMetadataExtractor
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

      String volume = getAdditionalMetadata(cu, am).trim();
      if (volume != null && volume != "") {
        log.debug3("Sage Check: Volume--------getAdditionalMetadata: volume-------");
        am.put(MetadataField.FIELD_VOLUME, volume);
      } else {
        log.debug3("Sage Check: Volume--------getAdditionalMetadata: volume Failed-------");
        return;
      }

      // Only emit if this item is likely to be from this AU
      // protect against counting overcrawled articles
      ArchivalUnit au = cu.getArchivalUnit();
      log.debug3("Sage Check: ---------SageAtyponHtmlMetadataExtractor start checking-------");
      if (!BaseAtyponMetadataUtil.metadataMatchesTdb(au, am)) {
        log.debug3("Sage Check: ---------SageAtyponHtmlMetadataExtractor failed-------");
        return;
      } else {
        log.debug3("Sage Check: ---------SageAtyponHtmlMetadataExtractor succeed-------");
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
      log.debug3("Sage Check: ---------SageAtyponHtmlMetadataExtractorFactory emitting url = " + url);
      emitter.emitMetadata(cu, am);

    }

    private String getAdditionalMetadata(CachedUrl cu, ArticleMetadata am)
    {

      log.debug3("Sage Check: Volume--------getAdditionalMetadata-------");
      InputStream in = cu.getUnfilteredInputStream();
      if (in != null) {
        try {
          String volume = null;
          volume = getVolumeNumber(in, cu.getEncoding(), cu.getUrl());
          in.close();
          return volume;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return null;
    }

    /*
   view-source:https://journals.sagepub.com/doi/10.1007/s11832-015-0635-2
   <div class="core-enumeration"><a href="/toc/choa/9/1"><span property="isPartOf" typeof="PublicationVolume">Volume <span property="volumeNumber">9</span></span>, <span property="isPartOf" typeof="PublicationIssue">Issue <span property="issueNumber">1</span></span></a></div>

    */
    protected String getVolumeNumber(InputStream in, String encoding, String url) {

      Elements span_element;

      try {
        Document doc = Jsoup.parse(in, encoding, url);

        span_element = doc.select("span[property=\"volumeNumber\"]"); // <span property="volumeNumber">9</span>
        log.debug3("Sage Check: Volume--------Get volume span-------");
        String volume = null;
        if ( span_element != null){
          volume = span_element.text().trim().toLowerCase();
          log.debug3("Sage Check: raw volume text: = " + span_element + ", url = " + url);
          if (volume != null) {
            log.debug3("Sage Check: Volume cleaned: = " + volume + ", url = " + url);
            return volume.trim();
          } else {
            log.debug3("Sage Check: Volume is null" + ", url = " + url);

          }
          return null;
        }
      } catch (IOException e) {
        log.debug3("Sage Check: Volume Error getVolumeNumber", e);
        return null;
      }
      return null;
    }
  }
}