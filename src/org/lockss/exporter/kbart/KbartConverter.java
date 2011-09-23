/*
 * $Id: KbartConverter.java,v 1.20.2.3 2011-09-23 13:23:33 easyonthemayo Exp $
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.apache.commons.collections.comparators.ComparatorChain;

import org.lockss.config.TdbTitle;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.daemon.AuHealthMetric;
import org.lockss.daemon.AuHealthMetric.HealthUnavailableException;
import org.lockss.util.Logger;
import org.lockss.util.NumberUtil;
import org.lockss.util.StringUtil;
import static org.lockss.exporter.kbart.TdbAuOrderScorer.SORT_FIELD;
import static org.lockss.exporter.kbart.KbartTdbAuUtil.*;
import static org.lockss.exporter.kbart.KbartTitle.Field.*;

/**
 * This class takes metadata in the internal TDB format, and converts it into
 * a format adhering to the KBART recommendations. It extracts the relevant 
 * metadata from internal structures to provide tuples of data suitable for 
 * KBART records. The KBART format focuses at the level of the 
 * <emph>title</emph>, and so the output is made available as 
 * <code>KbartTitle</code> objects via an ordered <code>List</code>. 
 * <p>
 * Note that it is possible to have several <code>KbartTitle</code>
 * objects in a single Publisher which have the same publication title;
 * for example if there are coverage gaps in the license to a particular 
 * publication, there will be a title representing each distinct licensed 
 * period (see 5.3.1.9).
 * <p>
 * KBART suggests (5.3.1.11) that the metadata file should be supplied in 
 * alphabetical order by <emph>title</emph>. This requires an overview of all 
 * titles before the order can be established, whereas the TDB structure is 
 * naturally set up to allow iteration by publisher, it being a higher element 
 * in the hierarchy.
 * <p>
 * Despite this, the class is currently implemented to convert TDB data only 
 * when requested. This means that if the class or one of its methods is used 
 * multiple times, there will be a duplication of effort. To overcome this, 
 * if a class instance is likely to be reused, a thread-safe lazy loading 
 * approach could be implemented. On the other hand, if instantiation seems 
 * like too much work, the whole class could be made static and each method 
 * could take a TDB argument. 
 * <p>
 * There is some justification for recording summary statistics about the 
 * KbartTitles as they are created, for example, what the longest field value 
 * is, or which fields are universally empty. This can be used to inform layout 
 * in outputs that wish to customise the KBART representation. To do this we 
 * could pass all field-setting calls through an exporter-specific summariser. 
 * <p>
 * Note that if the underlying <code>Tdb</code> objects are changed during 
 * iteration the resulting output is undefined.
 * <p>
 * <emph>Note that the <code>title_id</code> field is now filled with the 
 * ISSN-L code, as these have now been incorporated for all titles, and are 
 * used for linking.</emph>
 * 
 * @author Neil Mayo
 */
public class KbartConverter {

  private static Logger log = Logger.getLogger("KbartConverter");

  /** 
   * The string used as a substitution parameter in the output. Occurrences of 
   * this string will be replaced in local LOCKSS boxes with the protocol, host 
   * and port of ServeContent.
    */
  public static final String LABEL_PARAM_LOCKSS_RESOLVER = "LOCKSS_RESOLVER";
  
  /** The minimum number that will be considered a date of publication. */
  public static final int MIN_PUB_DATE = 1600;

  /** 
   * The minimum consistency score for a volume-first ordering to be used 
   * without first comparing it to a year ordering 
   */
  static final float VOLUME_SCORE_THRESHOLD = 1f;
  
  /**
   * Extract all the TdbTitles from the Tdb object, convert them into 
   * KbartTitle objects and add them to a list. 
   * 
   * @return a List of KbartTitle objects representing all the TDB titles
   */
  public static List<KbartTitle> extractAllTitles() {
    return convertTitles(TdbUtil.getAllTdbTitles());
  }

  /**
   * Extract all the TdbTitles from the Tdb object, convert them into 
   * KbartTitle objects and add them to a list. 
   * 
   * @return a List of KbartTitle objects representing all the TDB titles
   * @deprecated instead use TdbUtil to get the list of TdbTitles, which can be cached, and pass it to convertTitles
   */
  public static List<KbartTitle> extractTitles(TdbUtil.ContentScope scope) {
    return convertTitles(TdbUtil.getTdbTitles(scope));
  }
  
  /**
   * Convert the given collection of TdbTitles into KbartTitles.
   * The number of KbartTitles returned is likely to be different to the number
   * of TdbTitles which was originally supplied, as KbartTitles are grouped 
   * based on their coverage period rather than purely by title. 
   * 
   * @param titles a collection of TdbTitles
   * @return a list of KbartTitles
   */
  public static List<KbartTitle> convertTitles(Collection<TdbTitle> titles) {
    if (titles==null) return Collections.emptyList();
    List<KbartTitle> list = new Vector<KbartTitle>();
    for (TdbTitle t : titles) {
      list.addAll( createKbartTitles(t) );
    }
    return list;
  }
  
  /**
   * Convert the given collection of ArchivalUnits into KbartTitles representing
   * coverage ranges.
   * 
   * @param auMap a map of TdbTitles to lists of ArchivalUnits
   * @param showHealth whether or not to calculate a health rating for each title
   * @param rangeFieldsIncluded whether range fields are included in the output
   * @return a list of KbartTitles
   */
  public static List<KbartTitle> convertAus(Map<TdbTitle, List<ArchivalUnit>> auMap, 
      boolean showHealth, boolean rangeFieldsIncluded) {
    if (auMap==null) return Collections.emptyList();
    List<KbartTitle> list = new Vector<KbartTitle>();
    for (List<ArchivalUnit> titleAus : auMap.values()) {
      list.addAll(createKbartTitles(titleAus, showHealth, rangeFieldsIncluded));
    }
    return list;
  }
  
  /**
   * Sort a set of {@link TdbAu} objects for a title by start date, volume and 
   * finally name. There is no guaranteed way to order chronologically due to 
   * missing or inconsistent metadata. This sorting is provided as a second
   * choice when volume ordering fails to provide a consistent enough sequence 
   * of years or volumes. This often happens if the formats occurring in the 
   * volume field are mixed - see in particular BMJ.
   * 
   * @param aus a list of TdbAu objects
   */
  static void sortTdbAusByYearVolume(List<TdbAu> aus) {
    ComparatorChain cc = new ComparatorChain();
    cc.addComparator(TdbAuAlphanumericComparatorFactory.getFirstDateComparator());
    cc.addComparator(TdbAuAlphanumericComparatorFactory.getVolumeComparator());
    cc.addComparator(TdbAuAlphanumericComparatorFactory.getNameComparator());
    Collections.sort(aus, cc);
  }

  /**
   * Sort a set of {@link TdbAu} objects for a title by volume, start date and 
   * finally name. There is no guaranteed way to order chronologically due to 
   * missing or inconsistent metadata, but in general sorting by volumes tends 
   * to give the most correct ordering.
   * 
   * @param aus a list of TdbAu objects
   */
  static void sortTdbAusByVolumeYear(List<TdbAu> aus) {
    ComparatorChain cc = new ComparatorChain();
    cc.addComparator(TdbAuAlphanumericComparatorFactory.getVolumeComparator());
    cc.addComparator(TdbAuAlphanumericComparatorFactory.getFirstDateComparator());
    cc.addComparator(TdbAuAlphanumericComparatorFactory.getNameComparator());
    Collections.sort(aus, cc);
  }

  /**
   * Analyse the list of TdbAus to see whether it contains a mix of volume 
   * formats. Formats are differentiated based on the
   * {@link TdbAuOrderScorer.changeOfFormats()} method.
   * @param aus a list of TdbAu objects
   * @return <code>true</code> if any pair of consecutive volume fields differ in format
   */
  static boolean containsMixedFormats(List<TdbAu> aus) {
    String lastVol = aus.get(0).getVolume();
    for (TdbAu au: aus) {
      String vol = au.getVolume();
      // The first time there is a difference, return true
      if (TdbAuOrderScorer.changeOfFormats(vol, lastVol)) return true;
      lastVol = vol;
    }
    return false; 
  }

  /**
   * Sort a set of {@link KbartTitle}s (representing a single TdbTitle) by 
   * title, first date and then last date. The comparator chain should also 
   * work on a list of titles representing more than one TdbTitle.
   * @param titles a list of KbartTitles
   */
  static void sortKbartTitles(List<KbartTitle> titles) {
    ComparatorChain cc = new ComparatorChain();
    cc.addComparator(
        KbartTitleComparatorFactory.getComparator(PUBLICATION_TITLE)
    );
    cc.addComparator(
        KbartTitleComparatorFactory.getComparator(DATE_FIRST_ISSUE_ONLINE)
    );
    cc.addComparator(
        KbartTitleComparatorFactory.getComparator(DATE_LAST_ISSUE_ONLINE)
    );
    Collections.sort(titles, cc);
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
   * @param year the string to validate
   * @return whether the string appears to represent a 4-digit publication year
   */
  private static boolean isPublicationDate(int year) {
    return (year >= MIN_PUB_DATE && year <= Calendar.getInstance().get(Calendar.YEAR));
  }
  
  /**
   * Convert a collection of ArchivalUnits into one or more KbartTitles using 
   * as much information as possible. This version of the method acts upon a 
   * collection of ArchivalUnits rather than a TdbTitle. This is necessary for 
   * calculations with preserved or configured AUs, which may represent a 
   * subset of the items available in a TdbTitle.
   * <p>
   * If requested, this method also calculates health values for each title and
   * creates {@link KbartTitleHealthWrapper}s to decorate the real KbartTitles.
   * If range fields are not included in the output, the healths of the title
   * ranges are aggregated into a single health value for the whole range of 
   * the given title.
   * 
   * @param titleAus a list of ArchivalUnits relating to a single title
   * @param showHealth whether to show health values in the output 
   * @param rangeFieldsIncluded whether the export fields include range fields
   * @return a list of KbartTitle objects in no particular order
   */
  public static List<KbartTitle> createKbartTitles(Collection<ArchivalUnit> titleAus, 
      boolean showHealth, boolean rangeFieldsIncluded) {
    if (titleAus==null) return Collections.emptyList();
    // Create a list of TdbAus from the ArchivalUnits
    final List<TdbAu> tdbAus = TdbUtil.getTdbAusFromAus(titleAus);
    // Map the AUs from TdbAus for later reference
    final Map<TdbAu, ArchivalUnit> ausMap = TdbUtil.mapTdbAusToAus(titleAus);
    // Calculate the KbartTitles, each mapped to the TdbAu range that informed it
    final Map<KbartTitle, TitleRange> map = createKbartTitlesWithRanges(tdbAus);
    // Start constructing a result list
    List<KbartTitle> res = new ArrayList<KbartTitle>();

    // A tally of health values for the current title, in case they need to 
    // be aggregated due to a lack of range fields in the output 
    double totalHealth = 0;
    int numTdbAus = 0;
    // For each (ordered) KbartTitle, use its TdbAu range, and the mapped AUs, 
    // to calculate the aggregate health of its underlying AUs.
    List<KbartTitle> keyList = new ArrayList<KbartTitle>(map.keySet());
    sortKbartTitles(keyList);
    Iterator<KbartTitle> it = keyList.iterator();
    
    while (it.hasNext()) {
      final KbartTitle kbt = it.next();
      if (showHealth) {
        try {
          // Add a KbartTitle wrapper that decorates it with a health value
          double health = AuHealthMetric.getAggregateHealth(
              new ArrayList<ArchivalUnit>() {{
                for (TdbAu tdbAu : map.get(kbt).tdbAus) add(ausMap.get(tdbAu));
              }}
          );
          // If there are no range fields in the output, aggregate all the
          // individual KbartTitle healths.
          if (!rangeFieldsIncluded) {
            int numContributingAus = map.get(kbt).tdbAus.size();
            // Health is an average over the AUs
            totalHealth += health * numContributingAus;
            numTdbAus += numContributingAus;
            // On the final KbartTitle, calculate the average health
            if (!it.hasNext()) {
              res.add(new KbartTitleHealthWrapper(kbt, totalHealth/numTdbAus));
            }
          } else
            res.add(new KbartTitleHealthWrapper(kbt, health));
        } catch (HealthUnavailableException e) {
          // Total health is unknown if any individual health is unknown
          log.warning("KbartTitle has unknown health due to AU.", e);
          //res.add(new KbartTitleHealthWrapper(kbt, AuHealthMetric.UNKNOWN_HEALTH));
          res.add(kbt);
        }
      } else 
        res.add(kbt);
    }
    return res;
  }
  
  // Given a range of AUs representing a title, map TdbAus to Aus
  // and then create KbartTitles form the TdbAus, wrapping with a health from the AUs
  /*public static List<KbartTitle> createKbartTitlesWithAuRanges(Collection<ArchivalUnit> titleAus) {
    if (titleAus==null) return Collections.emptyList();
    // Create a list of TdbAus from the ArchivalUnits so we can sort it.
    List<TdbAu> aus = TdbUtil.getTdbAusFromAus(titleAus);
    return createKbartTitles(aus);
  }*/

  
  /**
   * Convert a TdbTitle into one or more KbartTitles using as much information as 
   * possible.
   * 
   * @param tdbt a TdbTitle from which to create the KbartTitle
   * @return a list of KbartTitle objects which may be unordered
   */
  public static List<KbartTitle> createKbartTitles(TdbTitle tdbt) {
    if (tdbt==null) return Collections.emptyList();
    // Create a list of AUs from the collection returned by the TdbTitle getter,
    // so we can sort it.
    List<TdbAu> aus = new ArrayList<TdbAu>(tdbt.getTdbAus());
    return createKbartTitles(aus);
  }
  
  /**
   * Create a list of KbartTitles from the supplied range of TdbAus.
   * The list is sorted.
   * 
   * @param aus a list of TdbAus which represent a single title
   * @return a list of KbartTitles
   */
  public static List<KbartTitle> createKbartTitles(List<TdbAu> aus) {
    List<KbartTitle> res = new ArrayList<KbartTitle>(createKbartTitlesWithRanges(aus).keySet());
    sortKbartTitles(res);
    return res;
  }
  
  /**
   * Convert a list of TdbAus into one or more KbartTitles using as much
   * information as possible, but maintain a record of the TdbAu range for each 
   * KbartTitle, and return as a map of the former to the latter. Note that 
   * this should only be called with a list of TdbAus which represent a single 
   * title, otherwise the Map will be large and the assumptions about which 
   * metadata is shared by the titles will not hold.
   * <p> 
   * A list of TdbAus relating to a single title will yield multiple 
   * KbartTitles if it has gaps in its coverage of greater than a year, in 
   * accordance with KBART 5.3.1.9. Each KbartTitle is created with a 
   * publication title and an identifier which is a valid ISSN. An attempt is 
   * made to fill the first/last issue/volume fields. The title URL is set to a
   * substitution parameter and issn argument. The parameter can be substituted 
   * during URL resolution to the local URL to ServeContent. There are several 
   * other KBART fields for which the information is not available.
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
   *   <li>title_url</li>
   *   <li>title_id</li>
   *   <li>publisher_name</li>
   * </ul>
   * The following fields currently have no analog in the TDB data:
   * <ul>
   *   <li><del>first_author</del> (not relevant to journals)</li>
   *   <li>embargo_info</li>
   *   <li>coverage_depth</li>
   *   <li>coverage_notes (free text field, may be used for PEPRS data)</li>
   * </ul>
   * <p>
   * We assume AUs are listed in order from earliest to most recent, when they 
   * are listed alphabetically by name.
   * 
   * @param aus a list of TdbAus relating to a single title
   * @return a map of KbartTitle to the TitleRange underlying it
   */
  public static Map<KbartTitle, TitleRange> createKbartTitlesWithRanges(List<TdbAu> aus) {
    
    // If there are no aus, we have no title records to add
    if (aus==null || aus.size()==0) return Collections.emptyMap();

    // Get a list of year ranges for the AUs, and figure out where the titles 
    // need to be split into ranges    
    TitleRangeInfo tri = getAuCoverageRanges(aus);
    List<TitleRange> ranges = tri.ranges;
       
    // If there is at least one AU, get the first for reference
    TdbAu firstAu = aus.get(0);
    // Identify the issue format
    IssueFormat issueFormat = identifyIssueFormat(firstAu);
    // Reporting:
    if (issueFormat!=null) {
      log.debug( String.format("AU %s uses issue format: param[%s] = %s", 
          firstAu, issueFormat.getKey(), issueFormat.getIssueString(firstAu)) );
    }

    // -----------------------------------------------------------------------
    // Create a title which will have the generic properties set; it can
    // be cloned as a base for KBART titles with different ranges.
    KbartTitle baseKbt = createBaseKbartTitle(firstAu);

    // ---------------------------------------------------------
    // Attempt to create ranges for titles with a coverage gap.
    // Depends on the availability of years in AUs.
    Map<KbartTitle, TitleRange> kbtList = new HashMap<KbartTitle, TitleRange>();

    // Iterate through the year ranges creating a title for each range with a 
    // gap longer than a year (KBART 5.3.1.9). We should also create a new 
    // title if the AU name changes somewhere down the list
    for (TitleRange range : ranges) {
      // If there are multiple ranges, make sure the title/issn is right 
      // (might change with the range?)
      if (ranges.size()>1) {
        updateTitleProperties(range.first, baseKbt);
      }

      // Reporting
      verifyRange(range);
      
      KbartTitle kbt = baseKbt.clone();
      fillKbartTitle(kbt, range, issueFormat, tri.hasVols);
      
      // Add the title to the map
      kbtList.put(kbt, range);
    }
    return kbtList;
  }


  /**
   * Use the supplied TdbAu to create a KbartTitle with the basic common 
   * fields set, which can then be cloned to create KbartTitles on which the 
   * remaining range-specific fields can be set. The fields which are set on 
   * the base title are the generic title fields, that is publisher name, 
   * publication title, ISSN identifiers and URL.
   * 
   * @param au a sample AU from which to take general field values
   * @return a KbartTitle with only general (title-level) properties set
   */
  static KbartTitle createBaseKbartTitle(TdbAu au) {
    // Get a TdbTitle for reference, and construct a base KbartTitle which can be cloned
    TdbTitle tdbt = au.getTdbTitle();

    KbartTitle baseKbt = new KbartTitle();

    // Add publisher and title and title identifier
    baseKbt.setField(PUBLISHER_NAME, tdbt.getTdbPublisher().getName());
    baseKbt.setField(PUBLICATION_TITLE, tdbt.getName());

    // Now add information that can be retrieved from the AUs
    // Add ISSN and EISSN if available in properties. If not, the title's unique id is used. 
    // According to TdbTitle, "The ID is guaranteed to be globally unique".
    baseKbt.setField(PRINT_IDENTIFIER, findIssn(au));
    baseKbt.setField(ONLINE_IDENTIFIER, findEissn(au));
    baseKbt.setField(TITLE_ID, findIssnL(au));

    // Title URL
    // Set using a substitution parameter 
    // e.g. LOCKSS_RESOLVER?issn=1234-5678 (issn or eissn or issn-l)
    baseKbt.setField(TITLE_URL, 
        LABEL_PARAM_LOCKSS_RESOLVER + baseKbt.getResolverUrlParams()
    ); 
    return baseKbt;
  }
  
  /**
   * Fill a KbartTitle object with data based on the supplied objects.
   * @param kbt a KbartTitle
   * @param range a TitleRange for the title
   * @param issueFormat an IssueFormat for the title
   * @param hasVols whether the title has volumes
   */
  private static void fillKbartTitle(KbartTitle kbt, 
      				     TitleRange range, 
      				     IssueFormat issueFormat, 
      				     boolean hasVols) {
    // Volume numbers (we omit volumes if the title did not yield a full and 
    // consistent set)
    // Note that this omits uncertain volume data on close to 400 titles 
    if (hasVols) {
      kbt.setField(NUM_FIRST_VOL_ONLINE, range.first.getStartVolume());
      kbt.setField(NUM_LAST_VOL_ONLINE, range.last.getEndVolume());
    }
    
    // Issue numbers
    if (issueFormat != null) {
      kbt.setField(NUM_FIRST_ISSUE_ONLINE,
          issueFormat.getFirstIssue(range.first));
      kbt.setField(NUM_LAST_ISSUE_ONLINE,
          issueFormat.getLastIssue(range.last));
    }
    
    // Issue years (will be zero if years could not be found)
    if (isPublicationDate(range.getFirstYear()) && 
        isPublicationDate(range.getLastYear())) {
      //if (tri.hasYears) {
      kbt.setField(DATE_FIRST_ISSUE_ONLINE,
          Integer.toString(range.getFirstYear()));
      kbt.setField(DATE_LAST_ISSUE_ONLINE,
          Integer.toString(range.getLastYear()));
      // If the final year in the range is this year or later,
      // leave empty the last issue/volume/date fields
      if (range.getLastYear() >= getThisYear()) {
        kbt.setField(DATE_LAST_ISSUE_ONLINE, "");
        kbt.setField(NUM_LAST_ISSUE_ONLINE, "");
        kbt.setField(NUM_LAST_VOL_ONLINE, "");
      }
    }

  }
  
  
  /**
   * Get the current year from a calendar.
   * @return an integer representing the current year
   */
  private static int getThisYear() { 
    return Calendar.getInstance().get(Calendar.YEAR);
  }
  
  /**
   * Update the KbartTitle with new values for the title fields if the TdbAu has
   * different values. Fields checked are title name, issn, eissn, and issnl.
   *  
   * @param au a TdbAu with potentially new field values 
   * @param kbt a KbartTitle whose properties to update
   */
  private static void updateTitleProperties(TdbAu au, KbartTitle kbt) {
    String issnCheck = findIssn(au);
    String eissnCheck = findEissn(au);
    String issnlCheck = findIssnL(au);
    String titleCheck = au.getTdbTitle().getName();
    if (!titleCheck.equals(kbt.getField(PUBLICATION_TITLE))) {
      log.info(String.format("Name change within title %s => %s", 
          kbt.getField(PUBLICATION_TITLE), titleCheck));
      kbt.setField(PUBLICATION_TITLE, titleCheck);
    }
    if (!issnCheck.equals(kbt.getField(PRINT_IDENTIFIER))) {
      log.info(String.format("ISSN change within title %s => %s", 
          kbt.getField(PRINT_IDENTIFIER), issnCheck));
      kbt.setField(PRINT_IDENTIFIER, issnCheck);
    }
    if (!eissnCheck.equals(kbt.getField(ONLINE_IDENTIFIER))) {
      log.info(String.format("EISSN change within title %s => %s", 
          kbt.getField(ONLINE_IDENTIFIER), eissnCheck));
      kbt.setField(ONLINE_IDENTIFIER, eissnCheck);
    }
    if (!issnlCheck.equals(kbt.getField(TITLE_ID))) {
      log.info(String.format("ISSN-L change within title %s => %s",
          kbt.getField(TITLE_ID), issnlCheck));
      kbt.setField(TITLE_ID, issnlCheck);
    }
  }
  
  /**
   * Attempt to extract a list of years from a list of AUs.
   * A list is constructed from the year fields on the assumption 
   * that they contain either (a) a year in digit format; or (b) a year range 
   * in the format "1900-1990". From these data each AU is given a year range, 
   * with a start and end year represented by integers. If any year cannot be 
   * parsed, null is returned. A list is only returned if a year could be 
   * parsed for every AU.
   * <p>
   * An assumption made here is that a TDB record will tend to have either
   * a full set of well-formed records, or a full-set of ill-formed records.
   * 
   * @param aus the list of AUs to search for years
   * @return a list of year ranges from the AUs, parsed into Integers, in the same order as the supplied list; or null if the parsing was unsuccessful
   */
  static List<YearRange> getAuYears(List<TdbAu> aus) {
    if (aus==null || aus.isEmpty()) return null;
    List<YearRange> years = new ArrayList<YearRange>();
    // Get a year for each AU
    try {
      for (TdbAu au : aus) {
        YearRange yrRng = new YearRange( au );
        // If the range is not valid, give up producing years
        if (!yrRng.isValid()) return null;
        years.add(yrRng);
      }
    } catch (NumberFormatException e) {
      return null;
    }
    return years;
  }
  
   
  /**
   * Extract volume fields from a list of AUs for a single title.
   * <p>
   * If the given AUs <b>all</b> have values for volume, and those 
   * values are not all the same (e.g. a list of placeholders like zero), a 
   * list of strings is returned of the same size and order of the AU list. 
   * Otherwise (the values are empty, all the same, or are not 
   * available for all the AUs) a null is returned.
   * <p>
   * If a TDB record does not have a full set of well-formed volume fields, we 
   * give up trying to establish volumes. If a volume cannot be parsed for one 
   * of the AUs, none are returned. This means that we get one range; the 
   * alternative is to produce a number of ranges which might include false 
   * positives. That is, a gap would be produced either side of an AU with no 
   * volume.
   * 
   * @param aus a list of AUs
   * @return a list of volumes from the AUs, in the same order as the supplied list; or null if the parsing was unsuccessful
   */
  static List<VolumeRange> getAuVols(List<TdbAu> aus) {
    if (aus==null || aus.isEmpty()) return null;
    // Create a list of the appropriate size
    int n = aus.size();
    List<VolumeRange> vols = new ArrayList<VolumeRange>(n);
 
    // Keep track of whether volumes differ or appear to be all the same (e.g. 0)
    boolean volsDiffer = false;
    // Get a volume for each AU
    String lastVol = null;
    for (TdbAu au : aus) {
      try {
        VolumeRange volRng = new VolumeRange(au);
        if (!volRng.isValid()) return null;
        vols.add(new VolumeRange(au));
      } catch (NumberFormatException e) {
        return null;
      }
      String vol = au.getVolume();
      // If there are multiple AUs, the last vol is non-null, and it and the
      // current vol differ, consider that we've got a variety of values
      if (!volsDiffer && lastVol!=null && !lastVol.equals(vol)) {
        volsDiffer = true;
      }
      lastVol = vol;
    }

    // Only return the vols if they differ across the list, or if there is a 
    // single AU 
    return (volsDiffer || aus.size() == 1) ? vols : null;
  }
    
  /**
   * Establish coverage gaps for the list of AUs, and return a list of title 
   * ranges, representing coverage periods which can be turned directly into 
   * title records. The method firsts gets a list of either volumes or years 
   * for the AUs, and uses them to figure out where the titles need to be 
   * split into ranges. If consecutive AUs have a difference between their
   * coverage values as defined by the field type, it is considered a coverage
   * gap. If no volumes or years could be established, the single full range of
   * the list is returned.
   * <p>
   * The volume information is considered preferentially, as it is more likely 
   * to give an indication of a true coverage gap; in particular there are 
   * cases where a journal is released less frequently than annually, so 
   * there is no actual coverage gap but the criteria recommended by KBART in 
   * 5.3.1.9 will result in one. This point of the recommendations is 
   * particularly ambiguous and will be referred to the KBART organisation. 
   * In the meantime this algorithm represents our own interpretation of 
   * "coverage gap".   
   * <p>
   * Finally, if the last range in a title spans to the current year, we leave 
   * the last year, volume and issue fields empty as suggested by 
   * KBART 5.3.2.8 - 5.3.2.10.
   * 
   * @param aus ordered list of AUs
   * @return a TitleRangeInfo containing a similarly-ordered list of TitleRange objects 
   */
  static TitleRangeInfo getAuCoverageRanges(final List<TdbAu> aus) {
    // Return immediately if there are no AUs
    final int n = aus.size();
    if (n == 0) return new TitleRangeInfo();

    // Sort the AUs by volume first
    sortTdbAusByVolumeYear(aus);
    boolean preferVolume = true;
    SORT_FIELD sortField = SORT_FIELD.VOLUME;

    // Get the resultant ordered lists of volumes and years
    List<VolumeRange> vols = getAuVols(aus);
    List<YearRange> years = getAuYears(aus);
    // Check if there are full sets of vols and years 
    // (these results will inhere for any ordering)
    boolean hasFullVols = vols!=null && vols.size()==n;
    boolean hasFullYears = years!=null && years.size()==n;

    List<TitleRange> rangesByVol = null, rangesByYear = null;
    // Calculate ranges based on the vol ordering
    rangesByVol = getAuCoverageRangesImpl(aus, vols, sortField, hasFullVols, hasFullYears);
    
    // Analyse the consistency of the resultant year ordering and ranges split
    TdbAuOrderScorer.ConsistencyScore csVol = 
        TdbAuOrderScorer.getConsistencyScore(aus, rangesByVol);
    
    // If the volumes are incomplete or score is unsatisfactory, and the years
    // are complete, try a year-first ordering
    if ((!hasFullVols || csVol.score < VOLUME_SCORE_THRESHOLD) && hasFullYears) {
       // Order by year first
      sortTdbAusByYearVolume(aus);
      sortField = SORT_FIELD.YEAR;
      // Recalculate the vols and years given the new ordering
      years = getAuYears(aus);

      // Calculate ranges based on the year ordering
      rangesByYear = getAuCoverageRangesImpl(aus, years, sortField, hasFullVols, hasFullYears);
      
      // Analyse the consistency of the resultant volume ordering and ranges split
      TdbAuOrderScorer.ConsistencyScore csYear = 
          TdbAuOrderScorer.getConsistencyScore(aus, rangesByYear);

      // Calculate the relative benefit of volume ordering over year ordering
      preferVolume = TdbAuOrderScorer.preferVolume(csVol, csYear);
      if (preferVolume) {
        // TODO This is the third time the sorting calcs are done - vols, years and ranges are cached
        sortTdbAusByVolumeYear(aus);
      }

    }

    // Log which ordering is used by each title after the analysis
    log.debug(String.format("%s will use %s ordering\n",
        aus.get(0).getJournalTitle(), preferVolume ? "VOLUME" : "YEAR"));

    return new TitleRangeInfo(preferVolume ? rangesByVol : rangesByYear, hasFullVols, hasFullYears);
  }

  // TODO simplify and use TdbAu instead of String for volume calcs; remove parameterisation?
  /**
   * A parameterised version of the <code>getAuCoverageRanges()</code> method 
   * to simplify the logic.
   * @param <T>
   * @param aus an ordered list of TdbAus for which to create coverage ranges
   * @param coverageValues a list of year or volume range values ordered like the TdbAus
   * @param sortField the field on which the TdbAus are ordered
   * @param hasFullVols <tt>true</tt> if there is a full set of volume values
   * @param hasFullYears <tt>true</tt> if there is a full set of year values
   * @return a list of TitleRange objects
   */
  private static <T> List<TitleRange> getAuCoverageRangesImpl(List<TdbAu> aus,
      List<T> coverageValues, SORT_FIELD sortField,
      boolean hasFullVols, boolean hasFullYears) {

    // The first coverage value in the current range
    T firstCoverageVal;
    // The current coverage value (last value in the current range)
    T currentCoverageVal;
    // The first AU in the current range
    TdbAu firstAu;
    // The current AU (last AU in the current range)
    TdbAu currentAu;
    // The previous AU
    TdbAu previousAu;
    // The index of the first AU of the current range
    int firstAuIndex;
    // A list of ranges to return
    List<TitleRange> ranges = new ArrayList<TitleRange>();
      
    // If there are no coverage values or the wrong number, return single 
    // full range
    if (coverageValues==null || coverageValues.size()!=aus.size()) {
      if (sortField==SORT_FIELD.VOLUME)
        ranges.add(new TitleRange(aus.subList(0, aus.size())));
      log.warning(String.format(
          "%s lacks a complete and consistent set of %ss; no coverage gap " +
              "calculations possible with this field.",
          aus.get(0).getJournalTitle(), sortField
      ));
      return ranges;
    }

    // Set first au and coverage value
    firstAuIndex = 0;
    firstCoverageVal = coverageValues.get(firstAuIndex);
    firstAu = aus.get(firstAuIndex);
    // Set current au and coverage value
    currentCoverageVal = firstCoverageVal;
    currentAu = firstAu;
    previousAu = null;

    int numPairs = aus.size() - 1;
    // If there is only one AU (no pairs) add a single range
    if (numPairs==0) ranges.add(new TitleRange(aus));
    // Iterate through the values, starting a new title record if there
    // is a coverage gap, defined as non-consecutive numerical volumes,
    // or a gap between years which is greater than 1
    // (Interpretation of KBART 5.3.1.9)
    for (int i=1; i<=numPairs; i++) {
      // Reset au and coverage vars
      currentCoverageVal = coverageValues.get(i);
      previousAu = aus.get(i-1);
      currentAu = aus.get(i);

      boolean isCoverageGap = false;
      // Whether there is a gap on the chosen sort field
      boolean gapOnField = isCoverageGap(previousAu, currentAu, sortField);
      // Whether there is a gap on volume field
      boolean gapOnVolume = sortField==SORT_FIELD.VOLUME ? gapOnField :
          hasFullVols && isCoverageGap(previousAu, currentAu, SORT_FIELD.VOLUME);

      // If using the volume field, we just need to check if volumes dictate a
      // coverage gap.
      if (sortField==SORT_FIELD.VOLUME) isCoverageGap = gapOnVolume;
      // If using the year field, we check it for a coverage gap, but prefer
      // the volume field if values are available.
      if (sortField==SORT_FIELD.YEAR)
        isCoverageGap = hasFullVols ? gapOnVolume : gapOnField;
        //isCoverageGap = hasFullVols ? gapOnField && gapOnVolume : gapOnField;

      /*log.debug(String.format(
          "%s isCoverageGap %b hasFullVols %b gapOnVolume %b gapOnField %s %b\n",
          aus.get(0).getJournalTitle(), isCoverageGap, hasFullVols,
          gapOnVolume, sortField, gapOnField
      ));*/

      if (isCoverageGap) {
        // Finish the old title and start a new title
        ranges.add(new TitleRange(aus.subList(firstAuIndex, i)));
        // Set new title properties
        firstAuIndex = i;
        firstAu = currentAu;
        firstCoverageVal = currentCoverageVal;
      }
      // On the last title; finish current title
      if (i==numPairs) ranges.add(new TitleRange(aus.subList(firstAuIndex, i+1)));
    }
    return ranges;
  }

  /**
   * A generic method for establishing whether there is a coverage gap between
   * the given TdbAus. The field argument indicates the primary field which will
   * be analysed for a coverage gap. If the values are not appropriately
   * consecutive on that field, there is a coverage gap.
   * @param au1 the first TdbAu
   * @param au2 the second TdbAu
   * @param field the primary field used in sorting the TdbAus
   * @return <code>true</code> if the TdbAus appear to have a coverage gap between
   */
  private static boolean isCoverageGap(TdbAu au1, TdbAu au2, SORT_FIELD field) {
    return !field.areAppropriatelyConsecutive(au1, au2);
  }


  /////////////////////////////////////////////////////////////////////////////
  // REPORTING
  /////////////////////////////////////////////////////////////////////////////
  /**
   * Reporting only - do some sanity checks on the range to identify possible TDB 
   * errors. Any problems will be logged as warnings.
   * @param range a TitleRange to check
   */
  private static final void verifyRange(TitleRange range) {
    // Compare the *first* year in the AU at each end of the range
    if (!verifyYearRangeConsistency(range)) {
      log.warning(String.format("TitleRange problem for %s. Years: %s - %s", 
          range.first.getTdbTitle(),
          range.first.getStartYear(),
          range.last.getStartYear()
      ));
    }
    // Compare the volumes
    if (!verifyVolumeRangeConsistency(range)) {
      log.warning(String.format("TitleRange problem for %s. Volumes: %s - %s",
          range.first.getTdbTitle(),
          range.first.getStartVolume(),
          range.last.getEndVolume()
      ));
    }
  }
  
  // Compare the first and last years of a range to see if it looks odd
  private static boolean verifyYearRangeConsistency(TitleRange range) {
    // Compare the *first* year in the AU at each end of the range
    int firstAuYear = KbartTdbAuUtil.getFirstYearAsInt(range.first);
    int lastAuYear = KbartTdbAuUtil.getFirstYearAsInt(range.last);
    return firstAuYear <= lastAuYear;
  }
  
  // Compare the first and last volumes of a range to see if it looks odd
  private static boolean verifyVolumeRangeConsistency(TitleRange range) {
    String firstVol = range.first.getStartVolume();
    String lastVol = range.last.getEndVolume();
    // If either of the volumes is null, we can't verify; return true
    if (firstVol==null || lastVol==null) return true;
    // Check if the first vol is greater than last
    if (NumberUtil.isInteger(firstVol) && NumberUtil.isInteger(lastVol)) {
      return Integer.parseInt(firstVol) <= Integer.parseInt(lastVol);
    } else {
      return firstVol.compareTo(lastVol) <= 0;
    }
  }
  /////////////////////////////////////////////////////////////////////////////

  
  /**
   * A class for convenience in passing back title year ranges after year 
   * processing. A year can be set to zero if it is not available. The onus 
   * is on the client to check whether the year is valid.
   * <p>
   * Note that this class now holds a list of TdbAu references covering the 
   * whole range, whereas it used to just hold references to the first and 
   * last TdbAus. This is because we now need to keep track of the AUs which
   * have informed the creation of the KbartTitle, in order to calculate 
   * health. 
   */
  static class TitleRange {
    /** The first TdbAu in the range. */
    TdbAu first;
    /** The last TdbAu in the range. */
    TdbAu last;
    /** The year range of the title. */
    YearRange yearRange;
    /** List of the <code>TdbAu</code>s making up the range. */
    List<TdbAu> tdbAus;

    /** Gets the first year in the range. */
    int getFirstYear() { return yearRange.first; }
    /** Gets the last year in the range. */
    int getLastYear() { return yearRange.last; }
    
    /**
     * Constructor extracts the years from the end points of the given AUs. 
     * 
     * @param tdbAus the list of TdbAus which underlie the title range 
     */
    TitleRange(List<TdbAu> tdbAus) {
      this.tdbAus = tdbAus;
      this.first = tdbAus.get(0);
      this.last = tdbAus.get(tdbAus.size()-1);
      this.yearRange = getYearRange(tdbAus);
    }
    
    /**
     * Produces a YearRange from the values available in the list of TdbAus.
     * The year range of a list of TdbAus is considered to be from the start 
     * date of the first AU, to the end date of the last AU. Some journals show
     * odd years for the end of the run - such as a year which is later than 
     * the end of the subsequent issues - but in most cases these appear to be 
     * mistakes, so we do not currently search for the latest end year in the 
     * list. 
     * <p>
     * <s>The year range might not be from first AU to last AU; some AUs inbetween 
     * have longer ranges due to late publication of issues. This method 
     * constructs a year range from the first year of the first AU to the latest
     * end year of any of the constituent AUs.</s>
     * <p>
     * If years are not available/parseable they are set to zero.
     *   
     * @param aus the ordered range of TdbAus which inform the year range 
     * @return a YearRange covering the whole range of the list of TdbAus
     */
    private static YearRange getYearRange(List<TdbAu> aus) {
      return new YearRange(aus.get(0), aus.get(aus.size()-1));
      // Alternative last year: int l = KbartTdbAuUtil.getLatestYear(aus);
    }

    /** Produces a friendly String representation of the range. */
    public String toString() {
      return String.format("TitleRange %s %s", first.getJournalTitle(), yearRange);
    }
  }

  /**
   * Represents a volume range with two strings.
   *
   */
  static class VolumeRange {
    /** The first volume string in the range. */
    String first;
    /** The last volume string in the range. */
    String last;

    /**
     * Create a volume range from a single TdbAu. If its volume string cannot be
     * parsed, a <code>NumberFormatException</code> is thrown.
     * @param au a single TdbAu representing a whole range
     * @throws NumberFormatException
     */
    VolumeRange(TdbAu au) throws NumberFormatException {
      this.first = au.getStartVolume();
      this.last = au.getEndVolume();
    }

    /**
     * Create a volume range from AUs. If there is a volume range in the first AU,
     * the first volume of the range is taken; if the last AU has a range, the
     * last volume is taken. If parsing fails, a
     * <code>NumberFormatException</code> is thrown.
     * @param first first TdbAu of the range
     * @param last last TdbAu of the range
     * @throws NumberFormatException
     */
    VolumeRange(TdbAu first, TdbAu last) throws NumberFormatException {
      this.first = first.getStartVolume();
      this.last = last.getEndVolume();
    }

    /**
     * Whether the range contains two valid volume strings, that is non-null
     * and not empty.
     * @return <code>true</code> if both values are available
     */
    public boolean isValid() {
      return !StringUtil.isNullString(first) &&
          !StringUtil.isNullString(last);
    }

    /** Produces a friendly String representation of the range. */
    public String toString() {
      return String.format("(Volumes %s-%s)", first, last);
    }
  }

  /**
   * Represents a year range without any contextual assumptions - a first year 
   * and a last year. Can be used to represent the range of a particular AU, of 
   * a range of AUs, or anything else that has a year range. Invalid years will 
   * usually be set to 0.
   *
   */
  static class YearRange {
    /** The first year in the range. */
    int first;
    /** The last year in the range. */
    int last;
    
    /**
     * Create a year range from a single TdbAu which contains either a single year
     * or a hyphenated year range. If the string cannot be parsed according 
     * to these criteria, the years are set to zero.
     * @param au a single TdbAu representing a whole range
     */
    YearRange(TdbAu au) { this(au, au);  }

    /**
     * Create a year range with pre-parsed values.
     * @param first first year of the range
     * @param lastYear last year of the range
     */
    YearRange(int first, int lastYear) {
      this.first = first;
      this.last = lastYear;
    }

    /**
     * Create a year range from AUs. If there is a year range in the first AU,
     * the first year of the range is taken; if the last AU has a range, the 
     * last year is taken. If parsing fails, the year is set to 0. 
     * @param first first AU of the range
     * @param last last AU of the range
     */
    YearRange(TdbAu first, TdbAu last) {
      this.first = KbartTdbAuUtil.getFirstYearAsInt(first);
      this.last = KbartTdbAuUtil.getLastYearAsInt(last);
    }
    
    /**
     * Whether the range contains two valid year values, that is not zero.
     * @return <code>true</code> if both values are available
     */
    public boolean isValid() {
      return first !=0 && last != 0;
    }
    
    /** Produces a friendly String representation of the range. */
    public String toString() {
      return String.format("(Years %d-%d)", first, last);
    }
  }

  /**
   * An object to encapsulate the results of coverage gap processing, that is the 
   * TitleRange objects produced for a TdbTitle, and flags indicating whether 
   * the volume and year metadata is considered complete enough to make use of
   * in calculating ranges and correct enough to show in the output.
   */
  static class TitleRangeInfo { 
    /** The full list of TitleRanges calculated for the title. */
    public final List<TitleRange> ranges;
    /** Whether the title is considered to have valid volumes. */
    public final boolean hasVols;
    /** Whether the title is considered to have valid years. */
    public final boolean hasYears;
    /** Create a TitleRangeInfo with final fields. */
    TitleRangeInfo(final List<TitleRange> ranges, final boolean hasVols, final boolean hasYears) {
      this.ranges = ranges; 
      this.hasVols = hasVols; 
      this.hasYears = hasYears; 
    }
    /** Create a TitleRangeInfo with flags set to false. */
    TitleRangeInfo(final List<TitleRange> ranges) {
      this(ranges, false, false); 
    }
    /** Create a TitleRangeInfo with an empty list of ranges. */
    TitleRangeInfo() {
      this.ranges = Collections.emptyList(); 
      this.hasVols = false; 
      this.hasYears = false; 
    }
  }

}
