/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.taylorandfrancis;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.MetadataUtil;
import org.lockss.util.TypedEntryMap;


public class TaylorAndFrancisHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(TaylorAndFrancisHtmlMetadataExtractorFactory.class);
  
  /* BIG IMPORTANT COMMENT */
  /* Taylor & Francis has opaque URLs and in the event of accidental overcrawling, an article could end
   * up getting collected that isn't actually in this AU. 
   * Verify the metadata against the known parameters of the AU to make sure that we don't emit metadata
   * for any articles that shouldn't be in this AU.  It's a final last-ditch protective check.
   * If I can't get any valid metadata then don't emit because we can't verify it is in the AU
   */
  public static boolean checkMetadataAgainstTdb(ArchivalUnit TandF_au, 
      ArticleMetadata am) { 

    boolean isInAu = true;

    // Get the AU's volume name from the AU properties. This must be set
    TypedEntryMap tfProps = TandF_au.getProperties();
    String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());
    TdbAu tf_tau = TandF_au.getTdbAu();
    String AU_journal_title = (tf_tau == null) ? null : tf_tau.getJournalTitle();

    // Use the journalTitle and volume name from the ArticleMetadata
    String foundJournalTitle = am.get(MetadataField.FIELD_JOURNAL_TITLE);
    String foundVolume = am.get(MetadataField.FIELD_VOLUME);
    // If we got neither, don't emit
    isInAu = !(StringUtils.isEmpty(foundJournalTitle) && StringUtils.isEmpty(foundVolume));

    // Do Volume comparison first, it's simpler
    if (isInAu && !(StringUtils.isEmpty(foundVolume))) {
      isInAu =  ( (AU_volume != null) && (AU_volume.equals(foundVolume)));
    }

    ////Decided on team aggreed on Oct/2021, no longer check tdb title vs metadata title
    ////Comment out the following section
    /*
    // Now check journalTitle with some flexibility for string differences
    if (isInAu && !(StringUtils.isEmpty(foundJournalTitle) || StringUtils.isEmpty(AU_journal_title)) ) {
      // normalize titles to catch unimportant differences
      String normAuTitle = normalizeTitle(AU_journal_title);
      String normFoundTitle = normalizeTitle(foundJournalTitle);
      log.debug3("TaylorAndFrancis: normalized title from AU is : " + normAuTitle);
      log.debug3("TaylorAndFrancis: normalized title from metadata is : " + normFoundTitle);
      // If the titles are a subset of each other or are equal after normalizatin
      isInAu = ( 
          ( (StringUtils.contains(normAuTitle,normFoundTitle)) || 
              (StringUtils.contains(normFoundTitle,normAuTitle))) );

      // last chance... cover weird cases, such as when the publisher mistakenly
      // converts multi-byte in to ? in their text output
      if (!isInAu) {
        log.debug3("one last attempt to match");
        String rawTextAuTitle = generateRawTitle(normAuTitle);
        String rawTextFoundTitle = generateRawTitle(normFoundTitle);
        log.debug3("raw AuTitle: " + rawTextAuTitle);
        log.debug3("raw foundTitle: " + rawTextFoundTitle);
        isInAu =( ( (StringUtils.contains(rawTextAuTitle,rawTextFoundTitle)) || 
            (StringUtils.contains(rawTextFoundTitle,rawTextAuTitle))) );

        if (isInAu == true) {
          log.debug3("TaylorAndFrancis: isInAu is true");
        } else {
          log.debug3("TaylorAndFrancis: isInAu is false");
        }
      }
    }

     */

    if (isInAu) {
      // Well we might as well pick up and fill in this since we're already peeking in the AU
      String AU_issn = (tf_tau == null) ? null : tf_tau.getIssn();
      String AU_eissn = (tf_tau == null) ? null : tf_tau.getEissn();
      if ( (AU_issn != null) && !AU_issn.isEmpty()) am.put(MetadataField.FIELD_ISSN, AU_issn);
      if ( (AU_eissn != null) && !AU_eissn.isEmpty()) am.put(MetadataField.FIELD_EISSN, AU_eissn);
    }
    return isInAu;
  }

  /* Because we compare a title from metadata with the title in the AU 
  * (as set in the tdb file) to make sure the item belongs in this AU,
  * we need to minimize potential for mismatch by normalizing the titles
  */
  static private final Map<String, String> normalizeMap =
      new HashMap<String,String>();
  static {
    normalizeMap.put("&", "and");
    normalizeMap.put("\u2013", "-"); //en-dash to hyphen
    normalizeMap.put("\u2014", "-"); //em-dash to hyphen
    normalizeMap.put("\u201c", "\""); //ldquo to basic quote
    normalizeMap.put("\u201d", "\""); //rdquo to basic quote
    normalizeMap.put("'", ""); //apostrophe to nothing - remove T'ang --> Tang
    normalizeMap.put("\u2018", ""); //single left quote to nothing - remove
    normalizeMap.put("\u2019", ""); //single right quote (apostrophe alternative) to nothing - remove
  }  
  /* To maximize the chance of finding matching titles normalize out stylistic
   * differences as much as possible, eg
   * make lower case,
   * remove leading and trailing spaces 
   * remove a leading "the " in the title
   * map control variations on standard chars (en-dash --> hyphen)
   * and & to "and" 
   * 
   */
  public static String normalizeTitle(String inTitle) {
    String outTitle;
    if (inTitle == null) return null;

    outTitle = inTitle.toLowerCase();
    outTitle = outTitle.trim();

    //Remove a leading "the " in the title
    if (outTitle.startsWith("the "))  {
      outTitle = outTitle.substring(4);// get over the "the " 
    }
    
    // Using the normalization map, substitute characters
    for (Map.Entry<String, String> norm_entry : normalizeMap.entrySet())
    {
        log.debug3("normalizing title by replacing " + norm_entry.getKey() + " with " + norm_entry.getKey());
        // StringUtils.replace is MUCH faster than String.replace
        outTitle = StringUtils.replace(outTitle, norm_entry.getKey(), norm_entry.getValue());
    }
    return outTitle;
  }
  
  /*
   * A last ditch effort to avoid false negatives due to odd characters
   * It is assumed it will already have gone through normalizeTitle which lets us make
   * assumptions
   * remove spaces
   * remove ?,-,:,"
   * 'this is a text:title-for "questions"?' in to
   * 'thisisatextitleforquestions'
   * \W  == non-word character [^a-zA-z0-9]
   * 
   */
  public static String generateRawTitle(String inTitle) {
    String outTitle;
    if (inTitle == null) return null;

    //reduce interior multiple space characters to only one
    //outTitle = inTitle.replaceAll("(\\s|\"|\\?|-|:)", "");
    outTitle = inTitle.replaceAll("\\W", "");
    return outTitle;
  }
    

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new TaylorAndFrancisHtmlMetadataExtractor();
  }

  public static class TaylorAndFrancisHtmlMetadataExtractor
  implements FileMetadataExtractor {

    // Map Taylor & Francis DublinCore HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("dc.Date", MetadataField.FIELD_DATE);
      tagMap.put("dc.Date", MetadataField.DC_FIELD_DATE);
      tagMap.put("dc.Title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.Title", MetadataField.DC_FIELD_TITLE);
      tagMap.put("dc.Creator", MetadataField.FIELD_AUTHOR);
      tagMap.put("dc.Creator", MetadataField.DC_FIELD_CREATOR);
      tagMap.put("dc.Identifier", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("dc.Subject", MetadataField.DC_FIELD_SUBJECT);
      tagMap.put("dc.Description", MetadataField.DC_FIELD_DESCRIPTION);
      // do not pick these up - we will be overriding them and need the values blank
      // once MetadataField.FIELD_PUBLISHER_IMPRINT is implemented, we'll use that instead for this value
      //tagMap.put("dc.Publisher", MetadataField.DC_FIELD_PUBLISHER);
      //tagMap.put("dc.Publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("dc.Type", MetadataField.DC_FIELD_TYPE);
      tagMap.put("dc.Format", MetadataField.DC_FIELD_FORMAT);
      tagMap.put("dc.Source", MetadataField.DC_FIELD_SOURCE);
      tagMap.put("dc.Language", MetadataField.DC_FIELD_LANGUAGE);
      tagMap.put("dc.Coverage", MetadataField.DC_FIELD_COVERAGE);
      tagMap.put("keywords", new MetadataField(MetadataField.FIELD_KEYWORDS, MetadataField.splitAt(";")));
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am =
          new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      // Strip the extra whitespace found in the HTML around and within the "dc.Creator" and "dc.Publisher" fields
      TrimWhitespace(am, MetadataField.DC_FIELD_CREATOR);
      TrimWhitespace(am, MetadataField.FIELD_AUTHOR);
      TrimWhitespace(am, MetadataField.DC_FIELD_PUBLISHER);
      TrimWhitespace(am, MetadataField.FIELD_PUBLISHER);

      // Parse the dc.Identifier fields with scheme values "publisher-id", "doi", and "coden".
      List<String> cookedIdentifierList = am.getList(MetadataField.DC_FIELD_IDENTIFIER);
      List<String> rawIdentifierList = am.getRawList("dc.Identifier");

      String journalTitle = "";
      String volume = "";
      String issue = "";
      String spage = "";
      String epage = "";
      String doi = "";

      for (int j = 0; j < cookedIdentifierList.size(); j++) {

        // If our dc.Identifier field has a comma in it, its content is a comma-delimited list of
        // the journal title, volume, issue, and page range associated with the article.
        // The journal title itself may contain commas, so the list is parsed backwards and all content
        // before ", Vol." is assumed to be part of the journal title.
        // Cannot assume that the split is ", " (with space), so split on "," only and remove leading spaces later

        // This whole thing isn't ideal.  We're counting on the metadata always having standard formatting
        // not to mention that the doi could have a comma in it...
        // While I'll go with the comma split, I am adding regexp checking to try to 
        // find stuff wherever possible. Ultimately there is only so much we can do - for example when a title is missing entirely
        // Here are some real examples (sanitized to protect the innocent):

        // comma in title
        //<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"One, Two &amp; Three, Vol. 19, No. 6, December 2010, pp. 555-567\"></meta>"                
        // no comma after title
        //<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Title Name Vol. 31, No. 2, June 2012, pp. 175-190\"></meta>
        // no title at all and alternately formatted info
        //<meta name="dc.Identifier" scheme="coden" content="Volume 17, Comment 1 © January 2011"></meta>
        // no Vol., because it was preprint
        //<meta name="dc.Identifier" scheme="coden" content="Language and
        // Education, preprint, 2012, pp. 1-24"></meta>
        if (cookedIdentifierList.get(j).contains(",")) {
          String content = cookedIdentifierList.get(j);
          String[] biblioInfo = content.split(",");

          for (int k = biblioInfo.length-1; k >= 0; k--) {
            //If the data was left with leading spaces after the split, remove 'em
            biblioInfo[k] = biblioInfo[k].trim();
            // get the page range
            if (biblioInfo[k].startsWith("pp. ")) {
              // page range separated by hyphen
              if (biblioInfo[k].contains("-")) {
                spage = biblioInfo[k].substring("pp. ".length(), biblioInfo[k].indexOf("-"));
                epage = biblioInfo[k].substring(biblioInfo[k].indexOf('-')+1, biblioInfo[k].length());
              }
              // page range separated by en-dash - unicode 2013
              else if (biblioInfo[k].contains("\u2013")) {
                spage = biblioInfo[k].substring("pp. ".length(), biblioInfo[k].indexOf("\u2013"));
                epage = biblioInfo[k].substring(biblioInfo[k].indexOf("\u2013")+1, biblioInfo[k].length());
              }
              // page range separated by three characters (decimal 226 218 147)
              // (e.g. "Journal of Pharmacy Teaching" Vol. 1, No. 2 dc.Identifier)
              else if (biblioInfo[k].contains("\u00E2\u0080\u0093")) {
                spage = biblioInfo[k].substring("pp. ".length(), biblioInfo[k].indexOf("\u00E2\u0080\u0093"));
                epage = biblioInfo[k].substring(biblioInfo[k].indexOf("\u00E2\u0080\u0093")+3, biblioInfo[k].length());
              }
              // page range is single page
              else {
                spage = biblioInfo[k].substring("pp. ".length(), biblioInfo[k].length());
              }
            }
            // get the issue number
            else if (biblioInfo[k].startsWith("No. ")) {
              issue = biblioInfo[k].substring("No. ".length(), biblioInfo[k].length());
            }
            // get the volume number
            else if (biblioInfo[k].startsWith("Vol. ")) {
              volume = biblioInfo[k].substring("Vol. ".length(), biblioInfo[k].length());
            }
            // we might be at the title, but let's see if, because of a missing comma, it includes the volume
            else if (biblioInfo[k].contains("Vol. ")) {
              volume = biblioInfo[k].substring(biblioInfo[k].indexOf("Vol. ") + "Vol. ".length(), biblioInfo[k].length());
              // and the rest would be the title...but check to make sure this isn't just part of it
              // If we're not at the beginning of the comma-separated list
              // (i.e. the journal title itself contains commas),
              // reinsert the comma that we lost in content.split(", ").
              String titleBit = biblioInfo[k].substring(0,biblioInfo[k].indexOf("Vol. "));
              journalTitle = titleBit.concat(journalTitle);
              if (k != 0) journalTitle = ", ".concat(journalTitle);
            }
            // by this point, we've come backwards in our comma-delimited list and reached
            // the journal title.
            else if (!volume.isEmpty()) {
              journalTitle = biblioInfo[k].concat(journalTitle);

              // If we're not at the beginning of the comma-separated list
              // (i.e. the journal title itself contains commas),
              // reinsert the comma that we lost in content.split(", ").
              if (k != 0) journalTitle = ", ".concat(journalTitle);
            } 
          }

          // org.apache.commons.lang.StringEscapeUtils contains a method for unescaping HTML codes
          // (like &amp;) that may appear in the journal title
          journalTitle = StringEscapeUtils.unescapeHtml4(journalTitle);
          journalTitle = journalTitle.trim(); // mal-formatted identifier could end up with trailing white space

          // Only put values in to metadata if they have valid content; no value will allow it to look elsewhere
          if ( !journalTitle.isEmpty()) am.put(MetadataField.FIELD_JOURNAL_TITLE, journalTitle);
          if ( !volume.isEmpty()) am.put(MetadataField.FIELD_VOLUME, volume);
          if ( !issue.isEmpty()) am.put(MetadataField.FIELD_ISSUE, issue);
          if ( !spage.isEmpty()) am.put(MetadataField.FIELD_START_PAGE, spage);
          if ( !epage.isEmpty()) am.put(MetadataField.FIELD_END_PAGE, epage);
        }
        else if (MetadataUtil.isDoi(cookedIdentifierList.get(j))) {
          doi = cookedIdentifierList.get(j);
          am.put(MetadataField.FIELD_DOI, doi);
        }

      }

      // for right now, manually set all T&F titles to the Taylor & Francis Group
      // once available, we'll use FIELD_PUBLISHER_IMPRINT for the sub publisher (eg. Routledge, Garland, etc)
      am.put(MetadataField.FIELD_PUBLISHER, "Taylor & Francis Group");
      am.put(MetadataField.DC_FIELD_PUBLISHER, "Taylor & Francis Group");

      ArchivalUnit TandF_au = cu.getArchivalUnit();
      if (checkMetadataAgainstTdb(TandF_au, am)) {
        emitter.emitMetadata(cu, am);
      }
    }

    private void TrimWhitespace(ArticleMetadata am, MetadataField md) {
      List<String> list = am.getList(md);
      for (int i = 0; i < list.size(); i++) {
        String curEntry = list.get(i);
        curEntry = curEntry.trim();
        curEntry = curEntry.replace("   ", " ");
        list.set(i, curEntry);
      }
    }


  }

}
