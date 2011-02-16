/*
 * $Id: KbartConverter.java,v 1.3 2011-02-16 23:41:48 easyonthemayo Exp $
 */

/*

Copyright (c) 2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.kbart;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;

import org.lockss.config.Tdb;
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTitle;
import org.lockss.config.TdbAu;
import org.lockss.util.Logger;

import static org.lockss.exporter.kbart.KbartTdbAuUtil.*;
import static org.lockss.exporter.kbart.KbartTitle.Field.*;

/**
 * This class takes metadata in the internal TDB format, and converts it into
 * a format adhering to the KBART recommendations. It extracts the relevant 
 * metadata from internal structures to provide tuples of data suitable for 
 * KBART records. The KBART format focuses at the level of the <emph>title</emph>, 
 * and so the output is made available as <code>KbartTitle</code> objects via an 
 * ordered <code>List</code>. 
 * <p>
 * Note that it is possible to have several <code>KbartTitle</code>
 * objects in a single Publisher which have the same publication title;
 * for example if there are coverage gaps in the license to a particular publication, 
 * there will be a title representing each distinct licensed period (see 5.3.1.9).
 * <p>
 * KBART suggests (5.3.1.11) that the metadata file should be supplied in alphabetical order by 
 * <emph>title</emph>. This requires an overview of all titles before the order can be established,
 * whereas the TDB structure is naturally set up to allow iteration by publisher, it being  
 * a higher element in the hierarchy.
 * <p>
 * Despite this, the class is currently implemented to convert TDB data only when requested.
 * This means that if the class or one of its methods is used multiple times, there will be a 
 * duplication of effort. To overcome this, if a class instance is likely to be reused, a
 * thread-safe lazy loading approach could be implemented. On the other hand, if instantiation seems 
 * like too much work, the whole class could be made static and each method could take a TDB argument. 
 * <p>
 * There is some justification for recording summary statistics about the KbartTitles as they are 
 * created, for example, what the longest field value is, or which fields are universally empty.
 * This can be used to inform layout in outputs that wish to customise the KBART representation.
 * To do this we could pass all field-setting calls through an exporter-specific summariser. 
 * <p>
 * Note that if the underlying <code>Tdb</code> objects are changed during iteration the resulting 
 * output is undefined.
 * <p>
 * <emph>Note that the <code>title_id</code> and <code>title_url</code> fields are currently left 
 * empty as the data we have for these is incomplete or inappropriate.</emph>
 * 
 * 
 * @author Neil Mayo
 */
public class KbartConverter {

  private static Logger log = Logger.getLogger("KbartConverter");

  /** The Tdb data structure from which KBART information will be selectively extracted. */
  private final Tdb tdb;

  /** An instance variable used to record the current year. */
  private final int thisYear; 

  /** The minimum number that will be considered a date of publication. */
  public static final int MIN_PUB_DATE = 1600;

  
  /**
   * Creates a KbartConverter which will extract information from the given TDB 
   * records and provide access to them as KBART-compatible objects.
   * 
   * @param tdb the Tdb structure from which the metadata will be extracted 
   */
  public KbartConverter(Tdb tdb) {
    this.tdb = tdb;
    this.thisYear = Calendar.getInstance().get(Calendar.YEAR);
  }
 
  /**
   * Extract all the TdbTitles from the Tdb object, convert them into 
   * KbartTitle objects and add them to a list. 
   * 
   * @return a List of KbartTitle objects representing all the TDB titles
   */
  public List<KbartTitle> extractAllTitles() {
    List<KbartTitle> list = new Vector<KbartTitle>();
    for (TdbPublisher p : tdb.getAllTdbPublishers().values()) {
      for (TdbTitle t : p.getTdbTitles()) {
	list.addAll( createKbartTitles(t) );
      }
    }
    return list;
  }

  /**
   * Sort a set of <code>TdbAu</code> objects for a title into ascending alphabetic name order.
   * By convention of TDB AU naming, this should also be chronological order.
   * There is no guaranteed way to order chronologically due to the dearth of date 
   * metadata included in AUs at the time of writing; however the naming convention
   * appears to be universally observed.
   * <p>
   * Note that the AU names ought to be identical to the name plus a volume identifier.
   * <p>
   * Perhaps this comparator should throw an exception if the conventions appear to be 
   * contravened, rather than trying to order with arbitrary names. 
   * 
   * @param aus a list of TdbAu objects
   */
  private static void sortAus(List<TdbAu> aus) {
    Collections.sort(aus, new TdbAuDateFirstAlphanumericComparator());
  }

  /**
   * Check whether a string appears to represent a publication date.
   * This is taken to be a 4-digit number within a specific range,
   * namely <code>MIN_PUB_DATE</code> to the current year.
   * <p>
   * Note this is used for both validation (checking a value does not 
   * contravene the expected format or content for a year), and 
   * recognition (being able to say that a string looks like it might 
   * be a year).
   *  
   * @param s the string to validate
   * @return whether the string appears to represent a 4-digit publication year
   */
  protected static boolean isPublicationDate(String s) {
    int year;
    try {
      year = Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return false;
    }
    return isPublicationDate(year);
  }

  /**
   * Check whether an integer appears to represent a publication date.
   * This is taken to be a number within a specific range,
   * namely <code>MIN_PUB_DATE</code> to the current year.
   *  
   * @param s the string to validate
   * @return whether the string appears to represent a 4-digit publication year
   */
  private static boolean isPublicationDate(int year) {
    return (year >= MIN_PUB_DATE && year <= Calendar.getInstance().get(Calendar.YEAR));
  }

  
 /**
  * Convert a TdbTitle into one or more KbartTitles using as much information as 
  * possible. A TdbTitle will yield multiple KbartTitles if it has gaps in its 
  * coverage of greater than a year, in accordance with KBART 5.3.1.9.
  * Each KbartTitle is created with a publication title and an identifier which
  * may be the ISSN. An attempt is made to fill the first/last issue/volume fields.
  * There are several other KBART fields for which the information is not available.
  * <p>
  * An attempt is made to fill the following KBART fields:
  * <ul>
  *   <li>publication_title</li>
  *   <li>print_identifier</li>
  *   <li>online_identifier</li>
  *   <li>date_first_issue_online</li>
  *   <li>date_last_issue_online</li>
  *   <li>num_first_issue_online</li>
  *   <li>num_last_issue_online</li>
  *   <li>num_first_vol_online</li>
  *   <li>num_last_vol_online</li>
  *   <li><del>title_url</del> (disabled until we have more than a base URL)</li>
  *   <li><del>title_id</del> (temporarily disabled)</li>
  *   <li>publisher_name</li>
  * </ul>
  * The following fields currently have no analog in the TDB data:
  * <ul>
  *   <li><del>first_author</del> (not relevant to journals)</li>
  *   <li>embargo_info</li>
  *   <li>coverage_depth</li>
  *   <li>coverage_notes</li>
  * </ul>
  * <p>
  * We assume AUs are listed in order from earliest to most recent, when they are
  * listed alphabetically by name. See the Comparator defined in this class.
  * 
  * @param tdbt a TdbTitle from which to create the KbartTitle
  * @return a list of KbartTitle objects
  */
  public List<KbartTitle> createKbartTitles(TdbTitle tdbt) {

    List<KbartTitle> kbtList = new Vector<KbartTitle>();
    // Create a list of AUs from the collection returned by the TdbTitle getter,
    // so we can sort it.
    List<TdbAu> aus = new ArrayList<TdbAu>(tdbt.getTdbAus());
    // Chronologically sort the AUs from the collection
    sortAus(aus);
 
    // ---------------------------------------------------------
    // Title which will have the generic properties set; it can
    // be cloned as a base for KBART titles with different ranges.
    KbartTitle baseKbt = new KbartTitle();

    // Add publisher and title and title identifier
    baseKbt.setField(PUBLISHER_NAME, tdbt.getTdbPublisher().getName());
    baseKbt.setField(PUBLICATION_TITLE, tdbt.getName());
    // XXX Disabled title_id temporarily
    //baseKbt.setField(TITLE_ID, tdbt.getId());
    
    // If there are no aus, we have nothing more to add
    if (aus.size()==0) {
      kbtList.add(baseKbt);
      return kbtList;
    }
 
    // ---------------------------------------------------------
    // Now add information that can be retrieved from the AUs
    
    // First AU in the list   
    TdbAu firstAu = aus.get(0);
    // Identify the issue format
    IssueFormat issueFormat = identifyIssueFormat(firstAu);
    
    // Reporting:
    if (issueFormat!=null) {
      log.debug( String.format("AU %s uses issue format: param[%s] = %s", 
	  firstAu, issueFormat.getKey(), issueFormat.getIssueString(firstAu)) );      
    }
    
    // Add ISSN and EISSN if available in properties. If not, the title's unique id is used. 
    // According to TdbTitle, "The ID is guaranteed to be globally unique".
    baseKbt.setField(PRINT_IDENTIFIER, findIssn(firstAu)); 
    baseKbt.setField(ONLINE_IDENTIFIER, findEissn(firstAu)); 

    // Title URL // XXX Disabled until we have more than a base_url
    // URL is not available directly but param[base_url] is sometimes available
    //baseKbt.setField(TITLE_URL, findAuInfo(firstAu, DEFAULT_TITLE_URL_ATTR, AuInfoType.PARAM));
   
    // ---------------------------------------------------------
    // Attempt to create ranges for titles with a coverage gap.
    // Depends on the availability of years in AUs.
    
    // Get a list of year ranges for the AUs, and figure out where the titles need to be split into ranges    
    List<TitleRange> ranges = getAuYearRanges(aus);

    // Iterate through the year ranges creating a title for each range with a gap 
    // longer than a year (KBART 5.3.1.9)
    // We should also create a new title if the AU name changes somewhere down the list
    for (TitleRange range : ranges) {
      KbartTitle kbt = baseKbt.clone();

      // If there are multiple ranges, make sure the title/issn is right (might change with the range?)
      if (ranges.size()>1) {
	updateTitleProperties(range.first, baseKbt);  
      }
	
      // Volume numbers
      kbt.setField(NUM_FIRST_VOL_ONLINE, findVolume(range.first));
      kbt.setField(NUM_LAST_VOL_ONLINE, findVolume(range.last));
      
      // Issue numbers
      if (issueFormat != null) {
  	kbt.setField(NUM_FIRST_ISSUE_ONLINE, issueFormat.getFirstIssue(range.first));
  	kbt.setField(NUM_LAST_ISSUE_ONLINE, issueFormat.getLastIssue(range.last));
      }

      // Issue years (will be zero if years could not be found)
      if (isPublicationDate(range.firstYear) && isPublicationDate(range.lastYear)) {
	kbt.setField(DATE_FIRST_ISSUE_ONLINE, Integer.toString(range.firstYear));
	kbt.setField(DATE_LAST_ISSUE_ONLINE, Integer.toString(range.lastYear));
	// If the final year in the range is this year or later, leave empty the last issue/volume/date fields 
	if (range.lastYear>=thisYear) {
	  kbt.setField(DATE_LAST_ISSUE_ONLINE, "");
	  kbt.setField(NUM_LAST_ISSUE_ONLINE, "");
	  kbt.setField(NUM_LAST_VOL_ONLINE, "");
	}
      }
      
      // Add the title to the list
      kbtList.add(kbt);
    }
    return kbtList;
  }

  
  /**
   * Update the KbartTitle with new values for the title fields if the TdbAu has
   * different values. Fields checked are title name, issn and eissn.
   *  
   * @param au a TdbAu with potentially new field values 
   * @param kbt a KbartTitle whose properties to update
   */
  private static void updateTitleProperties(TdbAu au, KbartTitle kbt) {
    String issnCheck = findIssn(au);
    String eissnCheck = findEissn(au);
    String titleCheck = au.getTdbTitle().getName();
    if (!titleCheck.equals(kbt.getField(PUBLICATION_TITLE))) {
      log.info("Name change within title "+kbt.getField(PUBLICATION_TITLE)+" => "+titleCheck);
      kbt.setField(PUBLICATION_TITLE, titleCheck);
    }
    if (!issnCheck.equals(kbt.getField(PRINT_IDENTIFIER))) {
      log.info("ISSN change within title "+kbt.getField(PRINT_IDENTIFIER)+" => "+issnCheck);
      kbt.setField(PRINT_IDENTIFIER, issnCheck);
    }
    if (!eissnCheck.equals(kbt.getField(ONLINE_IDENTIFIER))) {
      log.info("EISSN change within title "+kbt.getField(ONLINE_IDENTIFIER)+" => "+eissnCheck);
      kbt.setField(ONLINE_IDENTIFIER, eissnCheck);
    }
  }
  
  /**
   * Attempt to extract a list of years from a list of AUs.
   * This depends on the data being available in the TDB record and in
   * the correct format; additionally, the year attribute, if available, 
   * does not have a standard key name.
   * <p>
   * Each AU in a TdbTitle should contain the same attributes, so we just check 
   * the first AU. If no appropriate key is found in the first AU, we see if the volume
   * looks like a publication year, as several publishers use the year as a 
   * volume id. If so, we parse this field as the year.
   * <p>
   * If neither year nor volume appears to contain a year, null is returned.
   * Otherwise a list is constructed with values for the key in each AU 
   * being parsed into Integers on the assumption that they contain a year 
   * in digit format. If any year cannot be parsed, null is returned.
   * A list is only returned if a year could be parsed for every AU.
   * <p>
   * An assumption made here is that a TDB record will tend to have either
   * a full set of well-formed records, or a full-set of ill-formed records.
   * 
   * @param aus the list of AUs to search for years
   * @return a list of parsed Integers from the AUs, in the same order as the supplied list; or null if the parsing was unsuccessful
   */
  private static List<Integer> getAuYears(List<TdbAu> aus) {
    if (aus.isEmpty()) return null;
    List<Integer> years = new ArrayList<Integer>();
    
    // Find the year key (the 2 possible keys, matching year field in either attr or param, 
    // are the same at the moment, so we just match one directly).
    String yearKey = DEFAULT_YEAR_ATTR;

    // Find the year attribute if it exists
    TdbAu first = aus.get(0);
    AuInfoType type = findAuInfoType(first, yearKey);

    // If no type found for year, see if the volume field appears to contain a year 
    if (type==null) {
      String[] possKeys = new String[] {DEFAULT_VOLUME_ATTR, DEFAULT_VOLUME_PARAM, DEFAULT_VOLUME_PARAM_NAME};
      // Find the type, then the key to that type
      type = findAuInfoType(first, possKeys);
      yearKey = findMapKey(first, possKeys, type);
      // If the value does not appear to be a date, reset the type.
      if (!isPublicationDate( findAuInfo(first, yearKey, type) )) type = null;
    }
    // If neither year nor volume appears to contain a year, return null
    if (type==null) return null;
    
    // Otherwise continue to parse the appointed field:
    
    // Get a year for each AU
    try {
      for (TdbAu au : aus) {
	years.add(new Integer( type.findAuInfo(au, yearKey) ));
      }
    } catch (NumberFormatException e) {
      return null;
    }
    return years;
  }
  
  /**
   * A class for convenience in passing back title year ranges after year processing.
   * 
   */
  private static class TitleRange {
    TdbAu first;
    TdbAu last;
    int firstYear; 
    int lastYear;
    
    TitleRange(TdbAu first, TdbAu last, int firstYear, int lastYear) {
      this.first = first;
      this.last = last;
      this.firstYear = firstYear;
      this.lastYear = lastYear;
    }
  }

  /**
   * Get a list of years for the AUs, and figure out where the titles need to be split into ranges.
   * If no years could be established, the single full range of the list is returned. 
   * <p>
   * If the first and last years are not available, we look at each volume field to see if its
   * value looks like a publication year, as several publishers use the year as a volume id.
   * If it meets the criteria we use it in the date field.
   * <p>
   * Finally, if the last range in a title spans to the current year, we leave the last year, volume 
   * and issue fields empty as suggested by KBART 5.3.2.8 - 5.3.2.10.
   * 
   * 
   * @param aus ordered list of AUs
   * @return a similarly-ordered list of TitleRange objects 
   */
  private static List<TitleRange> getAuYearRanges(List<TdbAu> aus) {
    List<Integer> years = getAuYears(aus);
    List<TitleRange> ranges = new ArrayList<TitleRange>();
    // Return immediately if there are no AUs
    int n = aus.size();
    if (n == 0) return ranges;
    
    int firstIssueYear;           // The first year in the current range
    int currentIssueYear;         // The current year (last year in the current range)
    int prevIssueYear;            // The year from the previous loop
    TdbAu firstAu;                // The first AU in the current range
    TdbAu currentAu;              // The current AU (last AU in the current range)
    TdbAu prevAu;                 // The AU from the previous loop

    // Just add the full range if no years are available
    if (years==null) {
      ranges.add(new TitleRange(aus.get(0), aus.get(n-1), 0, 0));
      return ranges;
    }
      
    // Iterate through the years, starting a new title record if there is a gap 
    // longer than a year (KBART 5.3.1.9). The years are in the same order as the aus list.
    // Set first au and year
    firstIssueYear = years.get(0);
    firstAu = aus.get(0);
    currentIssueYear = firstIssueYear;
    currentAu = firstAu;
    
    // Iterate through the years, starting a new title record if there is a gap 
    // longer than a year (KBART 5.3.1.9)
    for (int i=0; i<n; i++) {
      // Reset year vars
      prevAu = currentAu;
      prevIssueYear = currentIssueYear;
      currentIssueYear = years.get(i);
      currentAu = aus.get(i);
      
      // There is a gap
      if (i>0 && currentIssueYear - prevIssueYear > 1) {
	// Finish the old title and start a new title 
	ranges.add(new TitleRange(firstAu, prevAu, firstIssueYear, prevIssueYear));
	// Set new title properties
	firstAu = currentAu;
	  firstIssueYear = currentIssueYear;
      }
      // On the last title
      if (i==n-1) {
	// finish current title
	ranges.add(new TitleRange(firstAu, currentAu, firstIssueYear, currentIssueYear));
      }
    }
    return ranges;
  }


  
  
  /**
   * Sort a set of <code>TdbAu</code> objects into ascending alphabetic name order.
   * By convention of TDB AU naming, this should also be chronological order.
   * There is no guaranteed way to order chronologically due to the incompleteness of date 
   * metadata included in AUs at the time of writing; however the naming convention
   * appears to be universally observed.
   * <p>
   * Note that the AU names ought to be identical to the name plus a volume or year identifier.
   * Takes account of numerical volume identifiers, which should get ordered 
   * numerically by magnitude. Mixed identifiers are treated as text. Proper pairwise 
   * comparison of text and number components of a pair of strings can be achieved
   * through {@link org.lockss.exporter.kbart.TdbAuAlphanumericComparator}. 
   * <p>
   * Perhaps this comparator should throw an exception if the conventions appear to be 
   * contravened, rather than trying to order with arbitrary names. 
   *  	
   * @author neil
   * @deprecated
   */
  public static class TdbAuComparator implements Comparator<TdbAu> {
    public int compare(TdbAu au1, TdbAu au2) {
      String au1name = au1.getName();
      String au2name = au2.getName();
      String title = au1.getTdbTitle().getName();
      if (au1name.startsWith(title) && au2name.startsWith(title)) {
	String a1 = au1name.substring(title.length());
	String a2 = au2name.substring(title.length());
	// compare suffices after the title name
	// try and cast to ints first
	try { 
	  Integer y1 = new Integer(a1);	    
	  Integer y2 = new Integer(a2);
	  return y1.compareTo(y2);
	} catch (NumberFormatException e) {
	  return a1.compareTo(a2); 
	}
      } else {
	// do straight alpha comparison
	return au1name.compareTo(au2name);
      }
    }
  }

  
}
