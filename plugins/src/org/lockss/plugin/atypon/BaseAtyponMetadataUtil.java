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

package org.lockss.plugin.atypon;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataField;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.UrlUtil;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

/**
 * This class contains useful routines to allow a metadata extractor to verify
 * the contents of an ArticleMetadata against the current AU's TDB information.
 * This might be used to avoid emitting an article that is likely due to an
 * overcrawl - currently used by BaseAtypon
 * @author alexohlson
 *
 */
public class BaseAtyponMetadataUtil {
  private static final Logger log = Logger.getLogger(BaseAtyponMetadataUtil.class);

  private static final String BOOK_EISBN_PARAM = "book_eisbn";
  private static final String AU_TYPE = "type";
  private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^(e)?is(b|s)n:(\\s)*", Pattern.CASE_INSENSITIVE);

  /**
   * html "dc.*" information may not contain publication title and volume
   * ris information usually does.
   * If the publication type doesn't match the article type (book v journal)
   * If the AU lists BOTH issn & eissn and the metadata has one that doesn'tm atch
   * If the volume is available, check against the TDB information
   * If the volume wasn't available but the year was, try that
   * If we're still valid and the publication title is available, check
   *   a normalized version of that against a normalized version of tdb pub title
   * @param au
   * @param am
   * @return true if the metadata matches the TDB information
   */
  public static boolean metadataMatchesTdb(ArchivalUnit au,
                                           ArticleMetadata am) {

    boolean isInAu = true;
    log.debug3("starting check");
    log.debug3(am.toString());

    //Initial check - are we of the expected type
    // We're only in this method if the type was NOT book or book_chapter
    // or if the article didn't indicate its type (html)
    TdbAu tdbau = au.getTdbAu();
    String au_type = (tdbau == null) ? null : tdbau.getPublicationType();
    // if the tdb publication type is a book or book chapter, then we don't belong
    if ( au_type != null && (MetadataField.PUBLICATION_TYPE_BOOKSERIES.equals(au_type)
        || MetadataField.PUBLICATION_TYPE_BOOK.equals(au_type)) ) {
      // we probably overcrawled and got a journal or proceedings article
      // while preserving a book
      return false;
    }

    // if we do an ISSN check, then we can bypass checking the title later, which is trickier
    Boolean checkedISSN = false;
    String AU_ISSN = (tdbau == null) ? null : normalize_id(tdbau.getPrintIssn());
    String AU_EISSN = (tdbau == null) ? null : normalize_id(tdbau.getEissn());
    // If the tdb lists both values for issn, then check with our found values
    // If we only list one value then don't use this check - what if the au
    // only had the EISSN and the article only listed the ISSN - false negative
    if ( !(StringUtils.isEmpty(AU_ISSN) || StringUtils.isEmpty(AU_EISSN)) ){
      String foundEISSN = normalize_id(am.get(MetadataField.FIELD_EISSN));
      String foundISSN = normalize_id(am.get(MetadataField.FIELD_ISSN));
      checkedISSN = true;
      // don't go crazy. If the EISSN is there and matches, just move on
      if (foundEISSN != null) {
        if (!(foundEISSN.equals(AU_EISSN) || foundEISSN.equals(AU_ISSN)) ) {
          return false;
        }
      } else if (foundISSN != null) {
        // there wasn't an EISSN, so let's check the ISSN
        if (!(foundISSN.equals(AU_ISSN) || foundISSN.equals(AU_EISSN)) ) {
          return false;
        }
      }
    }

    // BEGIN PUBLISHER SPECIFIC CHECKS
    // get the AU's publisher and check if it is in our list
    String pubName = (tdbau == null) ? null : tdbau.getPublisherName();

    if (isInAu && (pubName != null)) {
      Boolean isMarkAllen = pubName.equals("Mark Allen Group");
      Boolean isASCO = pubName.equals("American Society of Clinical Oncology");
      if (isASCO || isMarkAllen) {
        log.debug3("Publisher Specific Checks");
        // check the am date against the au
        isInAu = checkMdDate(au, am);
        if (isInAu && isMarkAllen) {
          isInAu = checkMdDoiJournalID(au, am);
        }
        if (isInAu) {
          log.debug3("Publisher Specific Pass");
        }
      }
    }
    // END PUBLISHER SPECIFIC CHECKS //

    // Use the journalTitle and volume name from the ArticleMetadata
    String foundJournalTitle = am.get(MetadataField.FIELD_PUBLICATION_TITLE);
    String foundVolume = am.get(MetadataField.FIELD_VOLUME);

    // If we got nothing, just return, we can't validate further
    if (StringUtils.isEmpty(foundVolume) && StringUtils.isEmpty(foundJournalTitle)) {
      log.debug3("Vol and Title was empty, returning: " + isInAu);
      return isInAu; //return true, we have no way of knowing
    }

    // Check VOLUME
    if (!StringUtils.isEmpty(foundVolume)) {
      // Get the AU's volume name from the AU properties. This must be set
      TypedEntryMap tfProps = au.getProperties();
      String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());

      if (isInAu && !(StringUtils.isEmpty(foundVolume))) {
        isInAu =  ( (AU_volume != null) && (AU_volume.equals(foundVolume)));
        log.debug3("After volume check, isInAu :" + isInAu);
      }
    }

    // If we've come this far and have passed an ISSN check, we're done
    if (checkedISSN) {
      return isInAu;
    }

    String AU_journal_title = (tdbau == null) ? null : tdbau.getPublicationTitle();

    // Now check journalTitle with some flexibility for string differences
    if (isInAu && !(StringUtils.isEmpty(foundJournalTitle) || StringUtils.isEmpty(AU_journal_title)) ) {
      // normalize titles to catch unimportant differences
      log.debug3("pre-normalized title from AU is : " + AU_journal_title);
      log.debug3("pre-normalized title from metadata is : " + foundJournalTitle);
      String normAuTitle = normalizeTitle(AU_journal_title);
      String normFoundTitle = normalizeTitle(foundJournalTitle);
      log.debug3("normalized title from AU is : " + normAuTitle);
      log.debug3("normalized title from metadata is : " + normFoundTitle);
      // If the titles are a subset of each other or are equal after normalization
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
      }
    }

    log.debug3("After metadata check, isInAu is " + isInAu);
    return isInAu;
  }

  /**
   * First check - 
   *   If the publication type doesn't match the article type (book v journal)
   * Then  
   * Books have some trickier issues - chapter vs. whole book vs book in series
   * If the year is available check against the TDB information - quick fail
   * If the eisbn is availalble, check against the TDB information 
   * If the isbn is availalble, check against the TDB information 
   * It's not clear if we should also check title info. I think not. ISBN should rule
   *
   * @param au
   * @param am
   * @return true if the metadata matches the TDB information
   */
  public static boolean metadataMatchesBookTdb(ArchivalUnit au,
                                               ArticleMetadata am) {

    boolean isInAu = false;

    // We're only in this method if the type was BOOK or BOOK_CHAPTER
    TdbAu tdbau = au.getTdbAu();
    String au_type = (tdbau == null) ? null : tdbau.getPublicationType();
    // proceedings, books and book_series could all represent as books/chapters
    // so only discard if the TDB thinks this should actually be a journal - possibly overcrawled?
    if (au_type != null && (MetadataField.PUBLICATION_TYPE_JOURNAL.equals(au_type))) {
      // we probably overcrawled and got a BOOK_CHAP or BOOK while
      // collecting a journal or proceedings
      return false;
    }

    // Use the book information from the ArticleMetadata
    // remove leading protocol and "-" and extraneous spaces
    String foundEISBN = normalize_id(am.get(MetadataField.FIELD_EISBN));
    String foundISBN = normalize_id(am.get(MetadataField.FIELD_ISBN));

    // If we got nothing, just return, we can't validate
    if (StringUtils.isEmpty(foundEISBN) &&
        StringUtils.isEmpty(foundISBN)) {
      //TODO: add in additional title check if ISBN metadata isn't available?
      return true; //return true, we have no way of knowing
    }

    // Get the AU's volume name from the AU properties. This must be set
    TypedEntryMap tfProps = au.getProperties();
    String AU_EISBN = tfProps.getString(BOOK_EISBN_PARAM);
    String AU_ISBN = (tdbau == null) ? null : normalize_id(tdbau.getPrintIsbn());

    // this is a param...it can't be null, but be safe
    if (AU_EISBN == null) {
      return true; //return true, we have no way of knowing
    }

    // if either of the isbn variants matches, we're good
    if ( ((!(StringUtils.isEmpty(foundEISBN))) && AU_EISBN.equals(foundEISBN)) ||
        ((!(StringUtils.isEmpty(foundISBN))) && AU_EISBN.equals(foundISBN)) ) {
      isInAu = true;
    } else if (!StringUtils.isEmpty(AU_ISBN)) {
      // they might have only listed a print isbn, not an eisbn so above would fail...
      isInAu =( ((!(StringUtils.isEmpty(foundISBN))) && AU_ISBN.equals(foundISBN)) ||
          ((!(StringUtils.isEmpty(foundEISBN))) && AU_ISBN.equals(foundEISBN)));
    }
    log.debug3("After eisbn/isbn check, isInAu :" + isInAu);
    return isInAu;
  }

  /* Because we compare a title from metadata with the title in the AU
   * (as set in the tdb file) to make sure the item belongs in this AU,
   * we need to minimize potential for mismatch by normalizing the titles
   * Remove any apostrophes - eg "T'ang" becomes "Tang"; "Freddy's" ==> "Freddys"
   */
  static private final Map<String, String> AtyponNormalizeMap =
      new HashMap<String,String>();
  static {
    AtyponNormalizeMap.put("&", "and");
    AtyponNormalizeMap.put("\u2013", "-"); //en-dash to hyphen
    AtyponNormalizeMap.put("\u2014", "-"); //em-dash to hyphen
    AtyponNormalizeMap.put("\u201c", "\""); //ldquo to basic quote
    AtyponNormalizeMap.put("\u201d", "\""); //rdquo to basic quote
    AtyponNormalizeMap.put("'", ""); //apostrophe to nothing - remove
    AtyponNormalizeMap.put("\u2018", ""); //single left quote to nothing - remove
    AtyponNormalizeMap.put("\u2019", ""); //single right quote (apostrophe alternative) to nothing - remove

  }

  /*
   * Checks the Year found in the Metadata Date field against the Year in the AU.
   * This should work for Journals/publishers, it is only invoked for Mark Allen Group as of 03.12.2021
   * @author markom
   */
  public static boolean checkMdDate(ArchivalUnit au,
                                    ArticleMetadata am) {

    boolean isInAu = true;
    String foundDate = am.get(MetadataField.FIELD_DATE);

    if (!StringUtils.isEmpty(foundDate)) {
      TdbAu tdbau = au.getTdbAu();
      String AU_Year = tdbau.getYear();

      // date can come in many formats, so lets try to deal with them,
      // e.g. 2013/09/28, 2013-09-28, 9/28/2013, "September 28, 2013", "2013, Sep 28"
      // other formats are too tricky, e.g. 20130928 so this format should pass the check as well
      // This date check can likely be instantly used for all Atypon,

      String foundYear = null;
      String[] splitDate = foundDate.split("/|-|, ");

      for (String piece : splitDate) {
        if (piece.length() == 4) {
          foundYear = piece;
        }
      }

      if ((!StringUtils.isEmpty(foundYear) && (AU_Year != null))) {
        isInAu = false;
        for (String auYear : AU_Year.split("/|-|,")) {
          if (auYear.length() == 4) {
            if (foundYear.equals(auYear)) {
              isInAu = true;
              break;
            }
          }
        }
      }

      log.debug3("foundDate: " + foundDate);
    }
    return isInAu;
  }

  /*
  * Checks the Journal ID found in the DOI Metadata field against the Journal ID in the AU.
  * This will not work for all Journals/publishers, it is only invoked for Mark Allen Group as of 03.12.2021
  * @author markom
  */
  public static boolean checkMdDoiJournalID(ArchivalUnit au,
                                   ArticleMetadata am) {

    boolean isInAu = true;
    String foundDOI = am.get(MetadataField.FIELD_DOI);

    if (!StringUtils.isEmpty(foundDOI)) {

      String JOURNAL_ID = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());

      // laboriously parse this out to the journal id that is embedded in the DOI
      // DOI for MarkAllen stuff always looks like this:
      // 10.12968/coan.2018.23.1.41
      // this was pulled in but doesnt look like markallen.
      // 10.1111/j.2044-3862.2009.tb00374.x
      // the letters between the '/' and the second '.' is the Journal ID
      // we extract it.
      // To generalize to all of Atypon more analysis should be done for the DOI entries of other publishers
      // as this is prime for false positives.

      int slashIdx = foundDOI.indexOf("/");
      String shouldBeJID = foundDOI.substring(slashIdx + 1, foundDOI.indexOf(".", slashIdx));
      isInAu = ((shouldBeJID != null) && shouldBeJID.equals(JOURNAL_ID));
      log.debug3("foundDOI: " + foundDOI);
    }
    return isInAu;
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

    //reduce interior multiple space characters to only one
    outTitle = outTitle.replaceAll("\\s+", " ");

    // Using the normalization map, substitute characters
    for (Map.Entry<String, String> norm_entry : AtyponNormalizeMap.entrySet())
    {
      log.debug3("normalizing title by replacing " + norm_entry.getKey() + " with " + norm_entry.getValue());
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


  /*
   * This could work for both ISBN and ISSN type ids
   */
  public static String normalize_id(String id) {
    if (id == null) {return null;}
    id = id.trim().replaceAll("-", "");
    Matcher protocol_match = PROTOCOL_PATTERN.matcher(id);
    if (protocol_match.find()) {
      return id.substring(protocol_match.end());
    }
    return id;
  }

  /*
   *  We can do a little additional cleanup
   *  1. if the DOI wasn't there, get it from the URL
   *  2. if the Publisher wasn't set, get it from the TDB
   *  3. If the Publication Title wasn't set, get it from the TDB
   *  4. if the access.url is set, make sure it's in the AU
   */
  public static void completeMetadata(CachedUrl cu, ArticleMetadata am) {

    // if the doi isn't in the metadata, we can still get it from the filename
    if (am.get(MetadataField.FIELD_DOI) == null) {

      /*matches() is anchored so must create complete pattern or else use .finds() */
      /* URL is "<base>/doi/(abs|full)/<doi1st>/<doi2nd> */
      String base_url = cu.getArchivalUnit().getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      String base_host = UrlUtil.stripProtocol(base_url);
      String patternString = "^https?://" + base_host + "doi/[^/]+/([^/]+)/([^?&]+)$";
      Pattern METADATA_PATTERN = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
      String url = cu.getUrl();
      Matcher mat = METADATA_PATTERN.matcher(url);
      if (mat.matches()) {
        log.debug3("Pull DOI from URL " + mat.group(1) + "." + mat.group(2));
        am.put(MetadataField.FIELD_DOI, mat.group(1) + "/" + mat.group(2));
      }
    }

    // Pick up some information from the TDB if not in the cooked data
    TdbAu tdbau = cu.getArchivalUnit().getTdbAu(); // returns null if titleConfig is null 
    if (tdbau != null) {
      if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
        // We can try to get the publishger from the tdb file.  This would be the most accurate
        String publisher = tdbau.getPublisherName();
        if (publisher != null) {
          am.put(MetadataField.FIELD_PUBLISHER, publisher);
        }
      }
      if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
        String journal_title = tdbau.getPublicationTitle();
        if (journal_title != null) {
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, journal_title);
        }
      }
    }

    // Finally, check the access.url and MAKE SURE that it is in the AU
    // or put it to a value that is
    String potential_access_url;
    if ((potential_access_url = am.get(MetadataField.FIELD_ACCESS_URL)) != null) {
      CachedUrl potential_cu = cu.getArchivalUnit().makeCachedUrl(potential_access_url);
      if ( (potential_cu == null) || (!potential_cu.hasContent()) ){
        //Not in this AU; remove this value; allow for fullTextCu to get set later
        am.replace(MetadataField.FIELD_ACCESS_URL, null);
      }

    }
  }


}

