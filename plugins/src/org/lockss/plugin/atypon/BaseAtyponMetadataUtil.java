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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    log.debug3("AU_ISSN: " + AU_ISSN + ", AU_EISSN: " + AU_EISSN);

    // If the tdb lists both values for issn, then check with our found values
    // If we only list one value then don't use this check - what if the au
    // only had the EISSN and the article only listed the ISSN - false negative
    if ( !(StringUtils.isEmpty(AU_ISSN) || StringUtils.isEmpty(AU_EISSN)) ){
      String foundEISSN = normalize_id(am.get(MetadataField.FIELD_EISSN));
      String foundISSN = normalize_id(am.get(MetadataField.FIELD_ISSN));

      log.debug3("Found EISSN: " + foundEISSN + ", Found ISSN: " + foundISSN);

      checkedISSN = true;
      // don't go crazy. If the EISSN is there and matches, just move on
      if (foundEISSN != null) {
        if (!(foundEISSN.equals(AU_EISSN) || foundEISSN.equals(AU_ISSN)) ) {
          log.debug3("Exiting early due to failed ISSN check. AU_ISSN: " + AU_ISSN +
                  ", AU_EISSN: " + AU_EISSN + ", foundEISSN: " + foundEISSN +
                  ", foundISSN: " + foundISSN);
          return false;
        }
      } else if (foundISSN != null) {
        // there wasn't an EISSN, so let's check the ISSN
        if (!(foundISSN.equals(AU_ISSN) || foundISSN.equals(AU_EISSN)) ) {
          log.debug3("Exiting early due to failed ISSN check. AU_ISSN: " + AU_ISSN +
                  ", AU_EISSN: " + AU_EISSN + ", foundEISSN: " + foundEISSN +
                  ", foundISSN: " + foundISSN);
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




    // get the AU's publisher and check if it is in our list
    String pubNameSeg = (tdbau == null) ? null : tdbau.getPublisherName();

    //Do SEG specific check to exclude certain overcrawl Aus on certain ingest machines
    if (isInAu && (pubNameSeg != null)) {
      log.debug3("Seg date check: pubname = " + pubNameSeg);
      Boolean isSeg = pubNameSeg.equals("Society of Exploration Geophysicists");
      if (isSeg) {
        isInAu = false;
        log.debug3("Seg date check: start checking: " + pubNameSeg);
        String seg_date = am.get(MetadataField.FIELD_DATE);
        String seg_pubtitle = am.get(MetadataField.FIELD_PUBLICATION_TITLE);
        
        TdbAu seg_tdbau = au.getTdbAu();
        String seg_AU_Year = seg_tdbau.getYear();
        String seg_tdb_title = seg_tdbau.getPublicationTitle();

        log.debug3("Seg date check: seg_date = " + seg_date + ", seg_AU_YEAR = " + seg_AU_Year
        + ", seg_pubtitle = " + seg_pubtitle + ", seg_tdb_title = " + seg_tdb_title);

        if ((!StringUtils.isEmpty(seg_date) && (seg_AU_Year != null))) {
          for (String auYear : seg_AU_Year.split("/|-|,")) {
            if (auYear.length() == 4) {
              if (seg_date.substring(0, 4).equals(auYear)) {
                log.debug3("Seg date check: seg_date = " + seg_date.substring(0, 4) + ", auYear = " + auYear);

                if ((seg_pubtitle != null) && (seg_tdb_title != null) && seg_tdb_title.toLowerCase().contains(seg_pubtitle.toLowerCase())) {
                  log.debug3("Seg date check: seg_date = " + seg_date + ", seg_AU_YEAR = " + seg_AU_Year
                          + ", check title passed, seg_pubtitle = " + seg_pubtitle + ", seg_tdb_title = " + seg_tdb_title);
                  isInAu = true;
                } else {
                  log.debug3("Seg date check: seg_date = " + seg_date + ", seg_AU_YEAR = " + seg_AU_Year
                          + ", check title failed, seg_pubtitle = " + seg_pubtitle + ", seg_tdb_title = " + seg_tdb_title);
                }
              }
            }
          }
        } else if (StringUtils.isEmpty(seg_date)) {
          // If ".ris" file are not available, seg_date might be null, which means the article is not in the Au
          log.debug3("Seg date check: seg_date is not avaialbe = " + seg_date);

        }
      }
    }


    // Add Atypon-Sage check for an over crawlled Au on ingest1 on Oct/2022 plugin version#41
    // INQUIRY: The Journal of Health Care Organization, Provision, and Financing Volume 59
    // This check need to happen before the following code, since it will return true anyway
    /*
    if (StringUtils.isEmpty(foundVolume) && StringUtils.isEmpty(foundJournalTitle)) {
      log.debug3("Vol and Title was empty, returning: " + isInAu);
      return isInAu; //return true, we have no way of knowing
    }
     */


    String pubNameSage = (tdbau == null) ? null : tdbau.getPublisherName();
    log.debug3("Sage Check: Publisher Specific Checks for Sage = " + pubNameSage);

    String AU_journal_titleSage = (tdbau == null) ? null : tdbau.getPublicationTitle();
    String foundJournalTitleSage = am.get(MetadataField.FIELD_PUBLICATION_TITLE);

    log.debug3("Sage Check: Publisher Specific Checks for Sage = " + pubNameSage + ", AU_journal_titleSage = " +
            AU_journal_titleSage + ", foundJournalTitleSage ="  + foundJournalTitleSage + ", isInAu =" + isInAu);

    if (isInAu && (pubNameSage != null) && (foundJournalTitleSage != null)) {
      Boolean isSage = pubNameSage.equalsIgnoreCase("SAGE Publications");
      if (isSage) {
        log.debug3("Sage Check:  Publisher Specific Checks for Sage");

        if (isInAu && !(StringUtils.isEmpty(foundJournalTitleSage) || StringUtils.isEmpty(AU_journal_titleSage)) ) {
          // normalize titles to catch unimportant differences
          log.debug3("Sage Check:  pre-normalized title from Sage AU is : " + AU_journal_titleSage);
          log.debug3("Sage Check:  pre-normalized title from Sage metadata is : " + foundJournalTitleSage);
          String normAuTitleSage = normalizeTitle(AU_journal_titleSage);
          String normFoundTitleSage = normalizeTitle(foundJournalTitleSage);
          log.debug3("Sage Check: normalized title from Sage AU is : " + normAuTitleSage);
          log.debug3("Sage Check: normalized title from Sage metadata is : " + normFoundTitleSage);
          // If the titles are a subset of each other or are equal after normalization
          isInAu = (
                  ((StringUtils.contains(normAuTitleSage, normFoundTitleSage)) ||
                          (StringUtils.contains(normFoundTitleSage, normAuTitleSage))));
          log.debug3("Sage Check: Publisher Specific Checks for Sage journal title condition meet, isInAu :" + isInAu + ", access.url = " + am.get(MetadataField.FIELD_ACCESS_URL)
                  + ", AU_journal_titleSage = " + AU_journal_titleSage + ", foundJournalTitleSage ="  + foundJournalTitleSage
                  + ", normAuTitleSage =  " + normAuTitleSage + ", normFoundTitleSage = " + normFoundTitleSage);

          // Check VOLUME
          String foundVolumeSage = am.get(MetadataField.FIELD_VOLUME);
          if (!StringUtils.isEmpty(foundVolumeSage)) {
            // Get the AU's volume name from the AU properties. This must be set
            TypedEntryMap tfProps = au.getProperties();
            String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());

            if (isInAu && !(StringUtils.isEmpty(foundVolumeSage))) {
              isInAu =  ( (AU_volume != null) && (AU_volume.equals(foundVolumeSage)));
              log.debug3("Sage Check: After Sage volume check, isInAu :" + isInAu + ", foundVolumeSage = " + foundVolumeSage + ", AU_volume =" + AU_volume);
            }
          }
          return isInAu;
        }
      }
    }

    //Do Siam specific check and emit 0 metadata based on publication title
    String pubNameSiam = (tdbau == null) ? null : tdbau.getPublisherName();

    log.debug3("Siam Check: Publisher Specific Checks for Siam = " + pubNameSiam);

    String AU_journal_titleSiam = (tdbau == null) ? null : tdbau.getPublicationTitle();
    String foundJournalTitleSiam = am.get(MetadataField.FIELD_PUBLICATION_TITLE);

    log.debug3("Siam Check: Publisher Specific Checks for Siam = " + pubNameSiam + ", AU_journal_titleSiam = " +
            AU_journal_titleSiam + ", foundJournalTitleSiam ="  + foundJournalTitleSiam + ", isInAu =" + isInAu);

    if (isInAu && (pubNameSiam != null) && (foundJournalTitleSiam != null)) {
      Boolean isSiam = pubNameSeg.equals("Society for Industrial and Applied Mathematics");
      if (isSiam) {
        log.debug3("Siam Check:  Publisher Specific Checks for Siam");

        if (isInAu && !(StringUtils.isEmpty(foundJournalTitleSiam) || StringUtils.isEmpty(AU_journal_titleSiam)) ) {
          // normalize titles to catch unimportant differences
          log.debug3("Siam Check:  pre-normalized title from Siam AU is : " + AU_journal_titleSiam);
          log.debug3("Siam Check:  pre-normalized title from Siam metadata is : " + foundJournalTitleSiam);
          String normAuTitleSiam = normalizeTitle(AU_journal_titleSiam);
          String normFoundTitleSiam = normalizeTitle(foundJournalTitleSiam);
          log.debug3("Siam Check: normalized title from Siam AU is : " + normAuTitleSiam);
          log.debug3("Siam Check: normalized title from Siam metadata is : " + normFoundTitleSiam);
          // If the titles are a subset of each other or are equal after normalization
          isInAu = (
                  ((StringUtils.contains(normAuTitleSiam, normFoundTitleSiam)) ||
                          (StringUtils.contains(normFoundTitleSiam, normAuTitleSiam))));
          log.debug3("Siam Check: Publisher Specific Checks for Siam journal title condition meet, isInAu :" + isInAu + ", access.url = " + am.get(MetadataField.FIELD_ACCESS_URL)
                  + ", AU_journal_titleSiam = " + AU_journal_titleSiam + ", foundJournalTitleSiam ="  + foundJournalTitleSiam
                  + ", normAuTitleSiam =  " + normAuTitleSiam + ", normFoundTitleSiam = " + normFoundTitleSiam);


          // Check VOLUME
          String foundVolumeSiam = am.get(MetadataField.FIELD_VOLUME);
          if (!StringUtils.isEmpty(foundVolumeSiam)) {
            // Get the AU's volume name from the AU properties. This must be set
            TypedEntryMap tfProps = au.getProperties();
            String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());

            if (isInAu && !(StringUtils.isEmpty(foundVolumeSiam))) {
              isInAu =  ( (AU_volume != null) && (AU_volume.equals(foundVolumeSiam)));
              log.debug3("Siam Check: After Siam volume check, isInAu :" + isInAu + ", foundVolumeSiam = " + foundVolumeSiam + ", AU_volume =" + AU_volume);
            }
          }

          return isInAu;
        }
      }
    }

    // Legacy ASLHA check
    String pubNameAslha = (tdbau == null) ? null : tdbau.getPublisherName();
    log.debug3("Aslha Check: Publisher = " + pubNameAslha);

    if (isInAu && pubNameAslha != null) {
      boolean isAslha = pubNameAslha.equalsIgnoreCase(
              "American Speech-Language-Hearing Association");

      log.debug3("Aslha Check matched? " + isAslha);

      if (isAslha) {
        String foundVolumeAslha = am.get(MetadataField.FIELD_VOLUME);
        String auVolumeAslha    = au.getProperties()
                .getString(ConfigParamDescr.VOLUME_NAME.getKey());

        if (StringUtils.isEmpty(foundVolumeAslha)
                || StringUtils.isEmpty(auVolumeAslha)
                || !foundVolumeAslha.equals(auVolumeAslha)) {
          isInAu = false;
          log.debug3("Aslha volume mismatch or missing; setting isInAu=false"
                  + " (found=" + foundVolumeAslha + ", au=" + auVolumeAslha + ")");
        } else {
          log.debug3("Aslha volumes match; isInAu remains true"
                  + " (found=" + foundVolumeAslha + ", au=" + auVolumeAslha + ")");
        }
        // **only** return here if it was truly the ASLHA publisher:
        log.debug3("Aslha final isInAu=" + isInAu);
        return isInAu;
      }
      // if it wasn’t ASLHA, we fall through to the generic checks
    }

    // SPECIAL_PUBLISHERS check
    Map<String,String> SPECIAL_PUBLISHERS;
    {
      Map<String,String> m = new LinkedHashMap<>();
      m.put("ARRS",          "American Roentgen Ray Society");
      m.put("UChicagoPress", "Archaeological Institute of America");
      m.put("GSLondon",      "Geological Society of London");
      m.put("ACP",           "American College of Physicians");
      m.put("NAS",           "National Academy of Sciences");
      SPECIAL_PUBLISHERS = Collections.unmodifiableMap(m);
    }

    // --- generic loop for all the other publishers ---
    String actualPubName = (tdbau == null) ? null : tdbau.getPublisherName();
    if (isInAu && actualPubName != null) {
      for (Map.Entry<String,String> e : SPECIAL_PUBLISHERS.entrySet()) {
        String key        = e.getKey();
        String expectName = e.getValue();

        log.debug3(key + " Check: Publisher = " + actualPubName);

        if (!actualPubName.equalsIgnoreCase(expectName)) {
          // skip non-matching
          continue;
        }

        // matched—run volume logic
        isInAu = checkVolumeMatch(key, am, au, expectName);
        log.debug3(key + " final isInAu=" + isInAu);

        // return immediately now that we've handled a special publisher
        return isInAu;
      }
    }


    //Do Edinburgh University Press specific check and emit 0 metadata based on publication title
    String pubNameEUP = (tdbau == null) ? null : tdbau.getPublisherName();

    log.debug3("EUP Check: Publisher Specific Checks for EUP = " + pubNameEUP);

    String AU_journal_titleEUP = (tdbau == null) ? null : tdbau.getPublicationTitle();
    String foundJournalTitleEUP = am.get(MetadataField.FIELD_PUBLICATION_TITLE);

    log.debug3("EUP Check: Publisher Specific Checks for EUP = " + pubNameEUP + ", AU_journal_titleEUP = " +
            AU_journal_titleEUP + ", foundJournalTitleEUP ="  + foundJournalTitleEUP + ", isInAu =" + isInAu);

    if (isInAu && (pubNameEUP != null) && (foundJournalTitleEUP != null)) {
      Boolean isEUP = pubNameSeg.equals("Edinburgh University Press");
      if (isEUP) {
        log.debug3("EUP Check:  Publisher Specific Checks for EUP");

        // Check doi
        String eup_jid = tdbau.getParam("journal_id");
        String eup_year = tdbau.getAttr("year");
        String eup_doi = am.get(MetadataField.FIELD_DOI);
        int year = Integer.parseInt(eup_year);


        log.debug3(String.format("EUP Check: EUP doi = %s, jid = %s, eup_year = %s ", eup_jid, eup_doi, eup_year));

        if (eup_jid != null && eup_doi != null && year >= 2025) {
          log.debug3(String.format("EUP Check: EUP doi = %s, jid = %s ", eup_jid, eup_doi));
          if (eup_doi.contains(eup_jid)) {
            log.debug3(String.format("EUP Check: EUP doi = %s contains jid = %s", eup_doi, eup_jid));
          } else {
            log.debug3(String.format("EUP Check: EUP  doi = %s does not contains jid = %s", eup_doi, eup_jid));
          }
        }
        isInAu = eup_doi.contains(eup_jid);
        return isInAu;
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
        log.debug3("After volume check, isInAu :" + isInAu + ", foundVolume = " + foundVolume + ", AU_volume" + AU_volume);
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

    // Get the AU's volume name from the AU properties. This must be set
    TypedEntryMap tfProps = au.getProperties();
    String AU_EISBN = tfProps.getString(BOOK_EISBN_PARAM);
    String AU_ISBN = (tdbau == null) ? null : normalize_id(tdbau.getPrintIsbn());

    String pubNamePA = (tdbau == null) ? null : tdbau.getPublisherName();
    log.debug3("Publisher Specific Checks for publisher = " + pubNamePA);

    if (StringUtils.isEmpty(foundEISBN) &&
        StringUtils.isEmpty(foundISBN)) {
        //if publisher is ASCE Books then check if the doi has the correct eisbn from the tdb file embedded in it
        //if not, return false
        if(pubNamePA != null && pubNamePA.equalsIgnoreCase("American Society of Civil Engineers")){
          if(am.get(MetadataField.FIELD_DOI).contains(AU_EISBN)){
            log.debug3("the doi is " + am.get(MetadataField.FIELD_DOI) + " and metadata will be emitted");
            return true;
          }
          else{
            log.debug3("the doi is " + am.get(MetadataField.FIELD_DOI) + " and metadata will not be emitted");
            return false;
          }
        }
      return true; //return true, we have no way of knowing
    }

    // this is a param...it can't be null, but be safe
    if (AU_EISBN == null) {
      return true; //return true, we have no way of knowing
    }

    // There are some books from Practical Action which have wrong isbn inside ".ris" files when they collected
    // Do not do isbn/eisbn comparation so metadata can be extracted
    if (tdbau == null) {
      log.debug3("Publisher Specific Checks for publisher PA tdbau is null");
    }

    if (isInAu && (pubNamePA != null)) {
      Boolean isPABooks = pubNamePA.equalsIgnoreCase("Practical Action Publishing");
      if ((isPABooks) &&  (!(StringUtils.isEmpty(foundEISBN)) || !(StringUtils.isEmpty(foundISBN)))) {
        log.debug3("Practional Action book eisbn/isbn check, foundEISBN :" + foundEISBN + ", foundISBN" + foundISBN);
        isInAu = true;
        return isInAu;
      }
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

  // helper method
  public static boolean checkVolumeMatch(String publisherKey,
                                   ArticleMetadata am,
                                   ArchivalUnit au,
                                   String expectedPublisherName) {
    String foundVolume = am.get(MetadataField.FIELD_VOLUME);
    String auVolume    = au.getProperties()
            .getString(ConfigParamDescr.VOLUME_NAME.getKey());

    // missing or mismatch → drop out
    if (StringUtils.isEmpty(foundVolume)
            || StringUtils.isEmpty(auVolume)
            || !foundVolume.equals(auVolume)) {

      log.debug3(publisherKey
              + " volume mismatch or missing; setting isInAu=false"
              + " (expectedPublisher=" + expectedPublisherName
              + ", foundVolume="     + foundVolume
              + ", auVolume="        + auVolume + ")");
      return false;
    }

    // match → stay true
    log.debug3(publisherKey
            + " volumes match; isInAu remains true"
            + " (expectedPublisher=" + expectedPublisherName
            + ", foundVolume="     + foundVolume
            + ", auVolume="        + auVolume + ")");
    return true;
  }



}

