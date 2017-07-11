/* $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.atypon;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbPublisher;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.TitleConfig;
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
  private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^(e)isbn:(\\s)*", Pattern.CASE_INSENSITIVE);
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
    String AU_ISSN = (tdbau == null) ? null : normalize_isbn(tdbau.getPrintIsbn());
    String AU_EISSN = (tdbau == null) ? null : normalize_isbn(tdbau.getEissn());
    // If the tdb lists both values for issn, then check with our found values
    // If we only list one value then don't use this check - what if the au
    // only had the EISSN and the article only listed the ISSN - false negative
    if ( !(StringUtils.isEmpty(AU_ISSN) || StringUtils.isEmpty(AU_EISSN)) ){
      String foundEISSN = normalize_isbn(am.get(MetadataField.FIELD_EISSN));
      String foundISSN = normalize_isbn(am.get(MetadataField.FIELD_ISSN));
      checkedISSN = true;
      // don't go crazy. If the EISSN is there and matches, just move on
      if (foundEISSN != null) { 
        if (!(foundEISSN.equals(AU_EISSN) || foundEISSN.equals(AU_ISSN)) ) {
          return false;
        }
      } else if (foundISSN != null) {
        // there wasn't an EISSN, so let's check the ISSN
        if (!(foundISSN.equals(AU_ISSN) || foundEISSN.equals(AU_EISSN)) ) {
          return false;
        }
      }
    }

    // Use the journalTitle and volume name from the ArticleMetadata
    String foundJournalTitle = am.get(MetadataField.FIELD_PUBLICATION_TITLE);
    String foundVolume = am.get(MetadataField.FIELD_VOLUME);
    String foundDate = am.get(MetadataField.FIELD_DATE);

    // If we got nothing, just return, we can't validate further
    if (StringUtils.isEmpty(foundVolume) && StringUtils.isEmpty(foundDate) &&
        StringUtils.isEmpty(foundJournalTitle)) {
      return isInAu; //return true, we have no way of knowing
    }
    
    // Check VOLUME/YEAR
    if (!(StringUtils.isEmpty(foundVolume) || StringUtils.isEmpty(foundDate))) {

      // Get the AU's volume name from the AU properties. This must be set
      TypedEntryMap tfProps = au.getProperties();
      String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());

      // Do Volume comparison first, it's simpler
      if (isInAu && !(StringUtils.isEmpty(foundVolume))) {
        isInAu =  ( (AU_volume != null) && (AU_volume.equals(foundVolume)));
        log.debug3("After volume check, isInAu :" + isInAu);
      }

      // Add in doing year comparison if FIELD_DATE is set
      // this is more complicated because date format is variable
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
    // if the tdb publication type is NOT a book or book chapter, then we don't belong
    if ( au_type != null && !( MetadataField.PUBLICATION_TYPE_BOOKSERIES.equals(au_type)
          || MetadataField.PUBLICATION_TYPE_BOOK.equals(au_type)) ) {
        // we probably overcrawled and got a BOOK_CHAP or BOOK while 
        // collecting a journal or proceedings 
        return false;
    }

    // Use the book information from the ArticleMetadata
    // remove leading protocol and "-" and extraneous spaces
    String foundEISBN = normalize_isbn(am.get(MetadataField.FIELD_EISBN));
    String foundISBN = normalize_isbn(am.get(MetadataField.FIELD_ISBN));

    // If we got nothing, just return, we can't validate
    if (StringUtils.isEmpty(foundEISBN) &&
        StringUtils.isEmpty(foundISBN)) {
      //TODO: add in additional title check if ISBN metadata isn't available?
      return true; //return true, we have no way of knowing
    }
    
    // Get the AU's volume name from the AU properties. This must be set
    TypedEntryMap tfProps = au.getProperties();
    String AU_EISBN = tfProps.getString(BOOK_EISBN_PARAM);
    String AU_ISBN = (tdbau == null) ? null : normalize_isbn(tdbau.getPrintIsbn());
    
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
  
  public static String normalize_isbn(String isbn) {
    if (isbn == null) {return null;}
    isbn = isbn.trim().replaceAll("-", "");
    Matcher protocol_match = PROTOCOL_PATTERN.matcher(isbn);
    if (protocol_match.find()) {
      return isbn.substring(0, protocol_match.end());
    }
    return isbn;
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

