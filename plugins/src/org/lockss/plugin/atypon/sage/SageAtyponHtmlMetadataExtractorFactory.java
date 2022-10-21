package org.lockss.plugin.atypon.sage;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
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

    /**
     * Use parent to extract raw metadata, map
     * to cooked fields, then do specific extract for extra tags by reading the file.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException {

      // extract but do some more processing before emitting
      ArticleMetadata am =
              new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(getTagMap()); //parent set the tagMap

      String volume = getAdditionalMetadata(cu, am);
      if (volume != null) {
        log.debug3("Sage Check: Volume--------getAdditionalMetadata: volume-------");
        am.put(MetadataField.FIELD_VOLUME, volume);
      } else {
        log.debug3("Sage Check: Volume--------getAdditionalMetadata: volume Failed-------");
      }

      emitter.emitMetadata(cu, am);
    }

    /*
    private void getAdditionalMetadata(CachedUrl cu, ArticleMetadata am)
    {
      //Extracts doi from url (doi is included in file, but not formatted well)
      //metadata could come from either full text html or abstract - figure out which
      String doi;
      if ( (cu.getUrl()).contains("abs/")) {
        doi = cu.getUrl().substring(cu.getUrl().indexOf("abs/")+4);
      } else
        doi = cu.getUrl().substring(cu.getUrl().indexOf("full/")+5);
      if ( !(doi == null) && !(doi.isEmpty())) {
        am.put(MetadataField.FIELD_DOI,doi);
      }

      //Extracts the volume and issue number from the end of the doi
      String suffix = doi.substring(doi.indexOf("/"));
      am.put(MetadataField.FIELD_ISSUE, suffix.substring(suffix.lastIndexOf(".")+1));
      am.put(MetadataField.FIELD_VOLUME, suffix.substring(suffix.lastIndexOf(".", suffix.lastIndexOf(".")-1)+1, suffix.lastIndexOf(".")));

      // lastly, hardwire the publisher if it hasn't been set
      if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
        am.put(MetadataField.FIELD_PUBLISHER, "Bloomsbury Qatar Foundation Journals");
      }
      */

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

      Pattern VOLUME_PAT = Pattern.compile("volume\\s*(\\d+)\\s*(\\d+)\\s*issue\\s*(\\d+)\\s*\\d+", Pattern.CASE_INSENSITIVE);
      String VOLUME_REPL = "$1";

      try {
        Document doc = Jsoup.parse(in, encoding, url);

        span_element = doc.select("span[property]"); // <span property="volumeNumber">9</span>
        log.debug3("Sage Check: Volume--------Get volume span-------");
        String raw_volume = null;
        String volume = null;
        if ( span_element != null){
          raw_volume = span_element.text().trim().toLowerCase(); // return "volume 9 9 issue 1 1"
          log.debug3("Sage Check: Volume--------Get volume text-------" + raw_volume);
          Matcher plosM = VOLUME_PAT.matcher(raw_volume);
          if (plosM.matches()) {
            volume = plosM.replaceFirst(VOLUME_REPL);
            log.debug3("Sage Check: Volume cleaned: = " + volume);
            return volume;
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