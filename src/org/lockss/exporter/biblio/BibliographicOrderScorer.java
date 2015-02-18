/*
 * $Id$
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.biblio;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.lockss.exporter.biblio.BibliographicUtil.TitleRange;
import org.lockss.util.NumberUtil;
import org.lockss.util.RegexpUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * This is a utility class intended to provide some measure of whether a
 * particular ordering of BibliographicItem objects, based on either year or
 * volume fields, provides an appropriately consistent ordering for both fields.
 * The main purpose is to decide which ordering gives the most consistent (and
 * therefore, we assume, more likely to be correct) results. To decide this
 * we need to know which field was used to order the list. There are two ways
 * of assessing the ordering - looking at the consistency of the whole list,
 * and at the consistency of individual ranges that have been created by
 * identifying coverage gaps in that list.
 * <p>
 * When a list of AUs is ordered in a supposedly chronological order, we expect
 * that the years will be monotonically increasing. This is unfortunately not
 * always the case, so we have to provide a more flexible metric which allows
 * them to overlap slightly. We also expect that the volume sequence should only
 * increase, but this assumption is routinely broken because volume identifier
 * formats change or reset, or sometimes are inconsistently mixed.
 * <p>
 * Volume is the preferred field to inform the calculation of coverage gaps,
 * because it tends to display more consistency. There should be minimal
 * duplication and in general the values should be increasing; it should
 * therefore be simpler to identify whether or not a sequence is unbroken.
 * However because of the inconsistencies mentioned above, and missing data, it
 * is sometimes necessary to fall back on the year sequence to validate the
 * coverage gaps suggested by the volume ordering. When the volume ordering
 * looks particularly suspicious or inconsistent, the list of AUs can be
 * reordered by year to see if that gives a more consistent ordering and set
 * of coverage ranges.
 *
 * <h2>Volume/Year Sequence Consistency</h2>
 *
 * <i>
 * Volume strings can be interpreted as numbers (integers or Roman numerals),
 * mixed strings, or a range defined by a pair of either of the preceding types
 * separated by a hyphen.
 * <br/><br/>
 * Year strings can only be interpreted as 4-digit integers or a range defined
 * by a pair of 4-digit integers separated by a hyphen.
 * </i>
 * <p>
 * A volume sequence is consistent if, over each range defined by the
 * ordered sequence, each volume identifier (or the start or end identifier
 * of a range, depending on the context of the AU in the sequence) is equal to,
 * or an increment of, the previous one. This gets harder to assess for
 * string-based volumes, but sequences which are numerical and consecutive will
 * in general get a high score.
 * <p>
 * A year sequence is consistent if, over each range defined by the
 * ordered sequence, each start date is greater than or equal to the
 * previous start date <b><i>or</i></b> the start of the first range is
 * is included in the second range (in other words there is some kind of
 * overlap).
 *
 * <h2>Appropriate Consecutivity</h2>
 *
 * It is acceptable for consecutive years to be the same; they can also exhibit
 * breaks within a coverage period as long as the volumes are appropriately
 * consecutive and the years are increasing. This allows for the fact that there
 * may be legitimate gaps between consecutive volumes of greater than a year,
 * and also that multiple volumes may get published within a single year.
 * Additionally, particular volume identifiers can be repeated within
 * consecutive AUs, because for example the volume was published in chunks
 * across several years.
 * <p>
 * The concept of "appropriate consecutivity" is more flexible for years; if
 * volumes cannot be recognised as appropriately consecutive, it is safer to
 * record a coverage gap.
 *
 * <h2>Summary</h2>
 *
 * There are methods for checking the strict consecutivity of both years and
 * volumes represented as integers or strings; for checking whether values are
 * increasing or decreasing; and whether pairs of years or volumes appear to be
 * <i>appropriately consecutive</i> (are close enough to be considered
 * consecutive if the other field suggests so) or <i>appropriately sequenced</i>
 * (not causing concern that the AU ordering is deficient).
 * <p>
 * Finally there are methods which bring all these measures together in order
 * to produce a measure of consistency for a list of years or volumes which is
 * the outcome of a particular ordering. These are based on the concepts of
 * (a) how many breaks are in a range and whether they are positive or negative
 * breaks; (b) how much redundancy is in a range; (c) the frequency of coverage
 * gaps produced by an ordering. The definitions of these concepts differ for
 * year and volume, and depending on the ordering context.
 *
 * @author Neil Mayo
 */
public final class BibliographicOrderScorer {

  private static Logger log = Logger.getLogger("BibliographicOrderScorer");

  /** Do not allow instantiation outside of package or subclasses. */
  protected BibliographicOrderScorer() { /* No instantiation. */ }

  /**
   * A pattern to find the last numerical token in a volume string. If the
   * string has no digits, the pattern does not match and the string can be
   * processed alphabetically; otherwise three matches are produced - the
   * string before the matched token, the final numerical token, and the
   * string after the token. The two surrounding strings may be empty.
   */
  private static final Pattern finalNumericalTokenPattern =
    RegexpUtil.uncheckedCompile("^(.*?)(\\d+)([^\\d]*)$");

  /**
   * A pattern to find the last Roman numerical token in a volume string.
   * This is defined as a collection of Roman numeral characters preceded by a
   * word boundary and followed by the end of the string. Three matches are
   * produced - the string before the matched token, the Roman numerical
   * token, at the end of the string, and a dummy empty string at the end so
   * that match groups are mutually comparable with the
   * finalNumericalTokenPattern. Note that this is slightly more
   * restrictive than the pattern for matching numerical tokens; additionally
   * we don't know if the string represents a numeral until we try to parse it.
   */
  /*private static final Pattern finalRomanNumericalTokenPattern =
      RegexpUtil.uncheckedCompile("^(.*?)([MmDdCcLlXxVvIi]+)()$");*/

  /**
   * An enum to allow the primary sort field to be specified when passing in a
   * list of ranges. Also simplifies access to the appropriate methods when
   * deciding the relationship between a pair of values in the given field.
   * <p>
   * Note that some of the method implementations can throw
   * NumberFormatException.
   */
  public static enum SORT_FIELD {

    VOLUME {
      public SORT_FIELD other() { return YEAR; };
      public String getValue(BibliographicItem au) { return au.getVolume(); }
      // Volumes are compared end year to start year
      public String getValueForComparisonAsPrevious(BibliographicItem au) {
        return au.getEndVolume();
      }
      public String getValueForComparisonAsCurrent(BibliographicItem au) {
        return au.getStartVolume();
      }
      public boolean areIncreasing(String first, String second) {
        return areVolumesIncreasing(first, second);
      }
      /**
       * We consider a volume to be decreasing if it is not monotonically
       * increasing. If two volumes are equal, increasing, or if the formats
       * change, <code>false</code> is returned.
       * @param first a volume string
       * @param second a second volume string
       * @return <tt>true</tt> if the volume strings are decreasing
       */
      public boolean areDecreasing(String first, String second) {
        return BibliographicUtil.changeOfFormats(first, second) ? false :
            !areVolumesIncreasing(first, second);
      }
      public boolean areConsecutive(String first, String second) {
        return areVolumesConsecutive(first, second);
      }
      /**
       * Consecutive BibliographicItems must usually have valid (non-zero) and
       * strictly consecutive volumes. However, because of occasional
       * interleaving of duplicate records which appear because of genuine
       * duplicates when a publisher re-releases on a new platform, we allow
       * consecutive BibliographicItems to have the same volume string if their
       * other fields are also duplicated.
       * <p>
       * Because of overlapping volume ranges (see for example "Laser Chemistry")
       * we also allow the first BibliographicItem's end volume to be equal to
       * the second's start volume.
       * <p>
       * A third possibility is that consecutive BibliographicItems have the
       * same volume string and consecutive years, suggesting a volume published
       * in several instalments over a period.
       * <p>
       * This method handles volume ranges, splitting them before checking
       * whether the end volume of the first BibliographicItem and the start
       * volume of the second BibliographicItem are consecutive. With volumes
       * there is no room for fuzzy overlap as there is when comparing years.
       *
       * @param first a BibliographicItem
       * @param second another BibliographicItem
       * @return <code>true</code> if the BibliographicItem volume ranges appear to be appropriately consecutive
       */
      public boolean areAppropriatelyConsecutive(BibliographicItem first,
                                                 BibliographicItem second) {
        String e1 = first.getEndVolume();
        String s2 = second.getStartVolume();
        return
            // True if valid and consecutive volumes
            (areVolumeStringsValid(e1, s2) && areVolumesConsecutive(e1, s2)) ||
            // True if valid and equal boundary volumes
            (areVolumeStringsValid(e1, s2) && e1.equals(s2)) ||
            // True if consecutive duplicate BibliographicItems, including volume
            areVolumesConsecutiveDuplicates(first, second) ||
            // True if valid volumes extending across appropriately consecutive years
            (areVolumeStringsValid(e1, s2) && areExtendedVolume(first, second));
      }

      /**
       * BibliographicItem sequences must display monotonically increasing
       * volumes.
       */
      public boolean areAppropriatelySequenced(BibliographicItem first,
                                               BibliographicItem second) {
        return isMonotonicallyIncreasing(Arrays.asList(first, second), this);
      }
    },


    YEAR {
      public SORT_FIELD other() { return VOLUME; };
      public String getValue(BibliographicItem au) { return au.getYear(); }
      // Year are compared start year to start year
      public String getValueForComparisonAsPrevious(BibliographicItem au) {
        return au.getStartYear();
      }
      public String getValueForComparisonAsCurrent(BibliographicItem au) {
        return au.getStartYear();
      }
      public boolean areIncreasing(String first, String second) {
        return areYearsIncreasing(first, second);
      }
      /** Years are decreasing if not strictly consecutive and not increasing. */
      public boolean areDecreasing(String first, String second) {
        return !areConsecutive(first, second) &&
            !areVolumesIncreasing(first, second);
      }
      public boolean areConsecutive(String first, String second) {
        return areYearsConsecutive(first, second);
      }
      /** Consecutive BibliographicItems must have appropriately consecutive
       * year ranges. */
      public boolean areAppropriatelyConsecutive(BibliographicItem first,
                                                 BibliographicItem second) {
        // Appropriate sequencing, plus no significant gap between the ranges
        return areYearRangesAppropriatelyConsecutive(first.getYear(),
            second.getYear());
      }
      /** BibliographicItem sequences must have appropriately sequenced year
       * ranges. */
      public boolean areAppropriatelySequenced(BibliographicItem first,
                                               BibliographicItem second) {
        return areYearRangesAppropriatelySequenced(first.getYear(),
            second.getYear());
      }
    }
    ;

    /** Whether the BibliographicItem has a valid (non-null and non-empty)
     * value on the field. */
    public boolean hasValue(BibliographicItem au) {
      //return getValue(au)!=null;
      return !StringUtil.isNullString(getValue(au));
    }
    // Abstract methods for SORT_FIELD
    /** The 'other' or secondary sort field when this is the primary sort field. */
    public abstract SORT_FIELD other();
    /** Gets the full value of the field. */
    public abstract String getValue(BibliographicItem au);
    /**
     * Gets the value of the field's range to use for comparison when the
     * BibliographicItem is the <i>previous</i> one in a pair comparison. This
     * is not necessarily equivalent to the end of the range, for example years
     * are only compared on start year due to the frequent unreliability of end
     * years.
     */
    public abstract String getValueForComparisonAsPrevious(BibliographicItem au);
    /**
     * Gets the value of the field's range to use for comparison when the
     * BibliographicItem is the <i>current</i> one in a pair comparison. This is
     * not necessarily equivalent to the start of the range.
     */
    public abstract String getValueForComparisonAsCurrent(BibliographicItem au);
    /** Whether the BibliographicItems have increasing values on the field. */
    public abstract boolean areIncreasing(String first, String second);
    /** Whether the BibliographicItems have decreasing values on the field. */
    public abstract boolean areDecreasing(String first, String second);
    /** Whether the BibliographicItems have consecutive values on the field. */
    public abstract boolean areConsecutive(String first, String second);
    /** Whether the BibliographicItems have field values which are appropriately
     * sequenced. */
    public abstract boolean areAppropriatelySequenced(BibliographicItem first,
                                                      BibliographicItem second);
    /** Whether the BibliographicItems have field values which are appropriately
     * consecutive. */
    public abstract boolean areAppropriatelyConsecutive(BibliographicItem first,
                                                        BibliographicItem second);
  };


  /**
   * An object to represent the several values comprising a consistency score.
   * For a given ordering, there is a score for each field based on how
   * consistent a set of ranges it produces, and a score for each field based
   * on how consistent the full list of field values is.
   */
  public static class ConsistencyScore {
    /**
     * The minimum overall consistency score for an ordering to be used
     * without consulting the rival ordering.
     */
    static final float TOTAL_SCORE_THRESHOLD = 1f;
    /**
     * The minimum acceptable consistency score for the volume values in a
     * volume-first ordering, such that the ordering can be used without first
     * comparing it to a year ordering.
     */
    static final float VOLUME_SCORE_THRESHOLD = 0.95f;


    public final float volScore, yearScore, volListScore, yearListScore, score;

    public boolean hasMissingVolValues, hasMissingYearValues;

    /**
     * Create a ConsistencyScore from the values in a Calculator object.
     * @param calc a calculator that has performed the calculations
     */
    public ConsistencyScore(Calculator calc) {
      this(calc.volumeRangeConsistency, calc.yearRangeConsistency, 
          calc.volumeListConsistency, calc.yearListConsistency);
    }

    /**
     * Create a ConsistencyScore from explicitly defined values.
     * @param volScore
     * @param yearScore
     * @param volListScore
     * @param yearListScore
     */
    public ConsistencyScore(float volScore, float yearScore,
                            float volListScore, float yearListScore) {
      this.volScore = volScore;
      this.yearScore = yearScore;
      this.volListScore = volListScore;
      this.yearListScore = yearListScore;
      // Overall score is the sum of the products of the range scores and
      // full sequence scores. This measure might be improved.
      this.score = (yearScore * volScore + yearListScore * volListScore)/2;
    }

    /**
     * Whether the volumes are fully consistent under this ordering. That is,
     * the volume score and volume list score are both 1.
     * @return
     */
    public boolean volumesAreFullyConsistent() {
      return volScore==1 && volListScore==1;
    }
    /**
     * Whether the years are fully consistent under this ordering. That is,
     * the year score and year list score are both 1.
     * @return
     */
    public boolean yearsAreFullyConsistent() {
      return yearScore==1 && yearListScore==1;
    }

    /**
     * Whether all internal scores are zero, in which case we probably need to
     * find another way of deciding between volume and year.
     * @return
     */
    public boolean allScoresAreZero() {
      return yearScore==0 && yearListScore==0
          && volScore==0 && volListScore==0;
    }

    /**
     * Whether the year scores are consistently better than the volume scores
     * under this ordering.
     * @return
     */
    public boolean yearScoresAreBetter() {
      return yearScore > volScore && yearListScore > volListScore;
    }


    /**
       * The volume score is satisfactory if the overall score meets the
       * TOTAL_SCORE_THRESHOLD, or the individual volume scores both meet the
       * VOLUME_SCORE_THRESHOLD. This method should be used to check whether a
       * volume ordering yields a 'good enough' set of scores for the volume field.
       * <p>
       * The point of this is to prevent an algorithm from having to check the
       * year ordering. In some cases the year ordering can yield a slightly
       * better score than volume because the years look more complete when
       * ordered although the volumes become more seriously messed up.
       * See for example Springer titles "Bulletin Géodésique (1946 - 1975)" and
       * "Journal of Economics".
       * Another approach would be to discount the year scores slightly or to
       * take account of the size of breaks in an ordering as well as their
       * presence and direction (see countProportionOf(Negative)Breaks methods).
       * <p>
       * Note that volume is the preferred indicator as years can be far more
       * unreliable and harder to interpret.
       * @return
       */
    public boolean isVolumeScoreSatisfactory() {
      return score >= TOTAL_SCORE_THRESHOLD
          || (volScore >=VOLUME_SCORE_THRESHOLD && volListScore >=VOLUME_SCORE_THRESHOLD)
          ;
    }

    /**
     * Pretty print the contributing scores for this ConsistencyScore.
     * @return
     */
    public String toString() {
      StringBuilder sb = new StringBuilder(
          String.format("ConsistencyScore vol %f \t year %f \t " +
              "volList %f \t yearList %f \t Overall %s",
              volScore, yearScore, volListScore, yearListScore, score
          )
      );
      if (hasMissingVolValues) sb.append(" (missing volume values)");
      if (hasMissingYearValues) sb.append(" (missing year values)");
      return sb.toString();
    }


    // XXX Default implementations of equals and hashcode, to support the test
    // class comparing the results of old algorithms against new parallelised
    // versions.
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ConsistencyScore that = (ConsistencyScore) o;
      if (Float.compare(that.score, score) != 0) return false;
      if (Float.compare(that.volListScore, volListScore) != 0) return false;
      if (Float.compare(that.volScore, volScore) != 0) return false;
      if (Float.compare(that.yearListScore, yearListScore) != 0) return false;
      if (Float.compare(that.yearScore, yearScore) != 0) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result = (volScore != +0.0f ? Float.floatToIntBits(volScore) : 0);
      result = 31 * result + (yearScore != +0.0f ? Float.floatToIntBits(yearScore) : 0);
      result = 31 * result + (volListScore != +0.0f ? Float.floatToIntBits(volListScore) : 0);
      result = 31 * result + (yearListScore != +0.0f ? Float.floatToIntBits(yearListScore) : 0);
      result = 31 * result + (score != +0.0f ? Float.floatToIntBits(score) : 0);
      return result;
    }
  }

  /**
   * A convenience object to hold consistency scores along with the ranges which
   * result from the ordering that gave rise to the scores.
   */
  public static class ConsistencyScoreWithRanges extends ConsistencyScore {
    public final List<TitleRange> ranges;
    public ConsistencyScoreWithRanges(Calculator calc) {
      super(calc);
      this.ranges = calc.ranges;
    }

    /**
     * @deprecated only used by getConsistencyScoreOld
     */
    public ConsistencyScoreWithRanges(float volScore, float yearScore,
                                      float volListScore, float yearListScore,
                                      List<TitleRange> ranges) {
      super(volScore, yearScore, volListScore, yearListScore);
      this.ranges = ranges;
      checkForMissingFieldValues();
    }

    protected void checkForMissingFieldValues() {
      // Check for missing field values
      for (TitleRange range : ranges) {
        for (BibliographicItem au : range.items) {
          if (!SORT_FIELD.VOLUME.hasValue(au)) this.hasMissingVolValues = true;
          if (!SORT_FIELD.YEAR.hasValue(au)) this.hasMissingYearValues = true;
          // Return if we already know the answer for both
          if (hasMissingVolValues && hasMissingYearValues) return;
        }
      }
    }

  }

  /**
   * Checks whether two years are strictly consecutive, that is, the second is
   * one greater than the first.
   *
   * @param first an integer representing the first year
   * @param second an integer representing the second year
   * @return true if the second year is one greater than the first
   */
  static final boolean areYearsConsecutive(int first, int second) {
    return NumberUtil.areConsecutive(first, second);
  }

  /**
   * Checks whether two string years are strictly consecutive, that is, the
   * second is one greater than the first.
   *
   * @param first a String representing the first year
   * @param second a String representing the subsequent year
   * @return true if the years can be parsed and the second year is one greater than the first
   */
  static final boolean areYearsConsecutive(String first, String second) {
    return NumberUtil.areConsecutive(first, second);
  }

  /**
   * Whether there appears to be a coverage gap between year ranges. That is,
   * whether there is a positive gap greater than one between the end of one
   * range and the start of another. If the supplied strings
   * are not parsable as ranges or single years, it will be impossible to
   * establish whether there is a gap and an exception is thrown.
   *
   * @param firstRange the first range or year
   * @param secondRange the second range or year
   * @return true if the strings could be parsed and there is a gap between the ranges
   * @throws NumberFormatException if the Strings do not represent year ranges or integers
   */
  static final boolean isGapBetween(String firstRange, String secondRange)
  throws NumberFormatException {
    int s2 = NumberUtil.parseInt(NumberUtil.getRangeStart(secondRange));
    int e1 = NumberUtil.parseInt(NumberUtil.getRangeEnd(firstRange));
    return s2 - e1 > 1;
  }

  /**
   * A volume string is considered to be valid if (when trimmed of whitespace)
   * it is non-null, not empty, and is equal neither to zero nor a lone hyphen.
   * This applies to a range string too, though it does not indicate whether the
   * range itself is valid.
   * @param vol a volume string
   * @return <code>true</code> if the string is non-empty, not zero and not a single hyphen
   */
  static final boolean isVolumeStringValid(String vol) {
    if (vol==null) return false;
    vol = vol.trim();
    // False if the string is null or empty
    if (StringUtil.isNullString(vol)) return false;
    // False if the trimmed string is a hyphen
    if (StringUtil.equalStringsIgnoreCase(vol, "-")) return false;
    // Finally, check if the string is equivalent to zero
    try {
      return NumberUtil.parseInt(vol) != 0;
    } catch (NumberFormatException e) {
      // String could not be parsed as an integer - consider it valid!
      return true;
    }
  }

  /**
   * A pair of volume strings are considered to be valid if either one is valid.
   * This applies to range strings too, though it does not indicate whether the
   * ranges themselves are valid.
   * @param first a volume string
   * @param second another volume string
   * @return <code>true</code> if either string is valid
   */
  static final boolean areVolumeStringsValid(String first, String second) {
    return isVolumeStringValid(first) || isVolumeStringValid(second);
  }

  /**
   * Checks whether two integer volumes are strictly consecutive, that is, the
   * second is one greater than the first.
   *
   * @param first an integer representing the first volume
   * @param second an integer representing the subsequent volume
   * @return true if the second volume is one greater than the first
   */
  static final boolean areVolumesConsecutive(int first, int second) {
    return NumberUtil.areConsecutive(first, second);
  }

  /**
   * Two string-based volume identifiers are considered consecutive based on a
   * simple test - if the final numerical token of the second string increments
   * in value while the rest of the string remains the same as in the first,
   * we consider them consecutive. For example, "s1-4" and "s1-5" are
   * considered consecutive (examples taken from Journal of Orthopaedic Surgery).
   * Before comparison, the identifiers are normalised using BibliographicUtil,
   * so that Roman numbers are converted to zero-padded Arabic equivalents.
   * <p>
   * This is a simplistic test but anything more complicated would likely
   * result in false positives. It also is not clever enough to recognise that,
   * for example, "s1-12" and "s2-1" might reasonably be construed as
   * consecutive, especially if they occur in the same or adjacent years.
   * <p>
   * First the strings are compared as numbers; if this doesn't work they
   * are compared alphabetically. If this also fails, the strings are analysed
   * to see if they are equal except for their final numerical tokens, which 
   * should be consecutive. If this yields false, the identifiers are normalised
   * (which converts any Roman numbers to Arabic) and compared again.
   * Finally false is returned if none of these tests yields true.
   * <p>
   * Normalising the identifiers allows us to catch identifier sequences like
   * os-4 and os-V, as seen in OUP.
   * <p>
   * Note that checking haveConsecutiveFinalNumericalTokens on <i>unnormalised 
   * strings</i> is only performed in order to catch identifiers which have an
   * incrementing number somewhere in the middle of a larger string, and which 
   * can also contain tokens that look like Roman numerals. Specifically this 
   * solves an issue with OUP's IEICE Transactions series which contains 
   * consecutive volume identifiers like E89-C and E90-C. 
   * However this is currently the only known case and the test 
   * may add significantly to the processing time.
   * <!--
   * It is also possible to encounter a volume string like "s6-IV". It is
   * harder to establish consecutivity here, as it is necessary to recognise a
   * token that looks like a Roman numeral, and then to see if it parses.
   * To avoid having to do this for every possible token embedded in the string,
   * the definition of "final Roman numeric token" is more restrictive than that
   * for "final digital numerical token", in that it must occur after a non-word
   * character, and be at the end of the string. Given this, we first check if
   * there appears to be a Roman token at the end of the string (which would
   * preclude any numerical tokens following), and if there is none we
   * consider the final numerical token of the string. If the string does end
   * with what appears to be a Roman numeral, but that does not change between
   * the two strings, we fall through to numerical token comparison instead.
   * -->
   * <p>
   * Note that {@link NumberUtil.areConsecutive(String, String)} will accept
   * Roman numerals.
   *
   * @param first a String representing the first volume
   * @param second a String representing the subsequent volume
   * @return whether the volumes appear to be consecutive
   */
  public static final boolean areVolumesConsecutive(String first, String second) {
    // First try and compare as numbers (including Roman)
    try {
      return NumberUtil.areConsecutive(first, second);
    } catch (NumberFormatException e) {
      // fall through to the string-based comparison
    }

    // Try and compare alphabetically
    try {
      return NumberUtil.areAlphabeticallyConsecutive(first, second);
    } catch (NumberFormatException e) {
      // fall through to the string-based comparison
    }
    
    // See if the final numerical tokens are consecutive while the outer tokens are equal.
    if (haveConsecutiveFinalNumericalTokens(first, second)) return true;
    
    // Try again with normalised identifiers (Roman to Arabic)
    if (haveConsecutiveFinalNumericalTokens(
          BibliographicUtil.normaliseIdentifier(first), 
          BibliographicUtil.normaliseIdentifier(second)
    )) return true;

    // At least one string doesn't match
    return false;
  }

  /**
   * Tests if each volume identifier is equal except for the final number 
   * token, which should be consecutive. The strings are tokenised and
   * the last numerical token of each is compared for consecutivity, while
   * the outer strings are compared for equality.
   * 
   * @param first a String representing the first volume
   * @param second a String representing the subsequent volume
   * @return whether the volumes appear to have consecutive final numerical tokens
   */  
  protected static boolean haveConsecutiveFinalNumericalTokens(String first, String second) {
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    MatchResult mr1, mr2;
    // If there are numerical tokens at the end of the strings, check if they are consecutive
    if (matcher.contains(first, finalNumericalTokenPattern)) {
      mr1 = matcher.getMatch();
      // Does the second vol string match
      if (matcher.contains(second, finalNumericalTokenPattern)) {
        mr2 = matcher.getMatch();
        // If the outer token matches are equal, test the consecutivity of the number tokens
        return (mr1.group(1).equals(mr2.group(1)) &&
            mr1.group(3).equals(mr2.group(3)) &&
                NumberUtil.areConsecutive(mr1.group(2), mr2.group(2)));
      }
    }
    return false;
  }


  /**
   * Two <code>BibliographicItem</code>s may be considered consecutive even if
   * they share a volume string, as long as the rest of their fields are also
   * equivalent. This is taken to indicate that the volumes are genuine
   * duplicates supplied by the publisher, because for example they have
   * re-released on a new platform, rather than accidental duplicates introduced
   * through errors in either the input records or the publisher records.
   * @param first a BibliographicItem representing the first volume
   * @param second a BibliographicItem representing the subsequent volume
   * @return whether the volumes appear to be consecutive
   */
  static final boolean areVolumesConsecutiveDuplicates(BibliographicItem first,
                                                       BibliographicItem second) {
    return BibliographicUtil.areApparentlyEquivalent(first, second);
  }

  /**
   * A pair of numbers represents an increasing sequence if the second
   * is greater than or equal to the first.
   *
   * @param first the first integer in the pair
   * @param second the second integer in the pair
   * @return true if the difference between the integers is not less than 0
   */
  static final boolean areValuesIncreasing(int first, int second) {
    return second - first >= 0;
  }

  /**
   * A pair of integer values represents an increasing sequence if the second
   * integer is greater than or equal to the first. This version of the method
   * works on integers represented as strings, including Roman numerals.
   *
   * @param first a string representing the first integer
   * @param second a string representing the subsequent integer
   * @return true if the strings could be parsed and the difference between the integers is non-negative
   * @throws NumberFormatException if the Strings do not represent integers
   */
  static final boolean areValuesIncreasing(String first, String second)
      throws NumberFormatException {
    //if (first==null || second==null) return false; //throw new NumberFormatException("Null value");
    return areValuesIncreasing(NumberUtil.parseInt(first),
        NumberUtil.parseInt(second));
  }

  /**
   * A pair of integer values represents a decreasing sequence if the second
   * integer is less than the first. This version of the method
   * works on integers represented as strings, including Roman numerals.
   *
   * @param first a string representing the first integer
   * @param second a string representing the subsequent integer
   * @return true if the strings could be parsed and the second is less than the first
   * @throws NumberFormatException if the Strings do not represent integers
   */
  static final boolean areValuesDecreasing(String first, String second)
      throws NumberFormatException {
    //if (first==null || second==null) return false; //throw new NumberFormatException("Null value");
    return NumberUtil.parseInt(first) > NumberUtil.parseInt(second);
  }

  /**
   * A pair of volume numbers represents an increasing sequence if the second
   * is greater than or equal to the first.
   *
   * @param first an integer representing the first volume
   * @param second an integer representing the subsequent volume
   * @return true if the difference between the volumes is not less than 0
   */
  static final boolean areVolumesIncreasing(int first, int second) {
    return areValuesIncreasing(first, second);
  }

  /**
   * Two string-based volume identifiers are considered to be increasing based
   * on a simple test - if the strings are the same except for the final
   * numerical token, and that token in the second string represents a greater
   * or equal value than that in the first, we consider them to be increasing.
   * For example, "s1-4" and "s1-15" are considered to be generally increasing.
   * The final token can be Roman or Arabic as we use
   * {@link NumberUtil.areIntegersConsecutive}.
   *
   * @param first a String representing the first volume
   * @param second a String representing the subsequent volume
   * @return whether the volumes appear to be monotonically increasing
   * @throws NumberFormatException if the Strings do not represent integers
   */
  static final boolean areVolumesIncreasing(String first, String second)
      throws NumberFormatException {
    if (first==null || second==null) return false; //throw new NumberFormatException("Null value");
    // If the strings are equal, there's no need to tokenise
    if (first.equals(second)) return true;
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    MatchResult mr1, mr2;
    // Does the first vol string match
    if (matcher.contains(BibliographicUtil.normaliseIdentifier(first),
                         finalNumericalTokenPattern)) {
      mr1 = matcher.getMatch();
      // Does the second vol string match
      if (matcher.contains(BibliographicUtil.normaliseIdentifier(second),
                           finalNumericalTokenPattern)) {
        mr2 = matcher.getMatch();

        // If the outer tokens match, test the consecutivity of the number tokens
        return mr1.group(1).equals(mr2.group(1)) &&
            mr1.group(3).equals(mr2.group(3)) &&
            areValuesIncreasing(mr1.group(2), mr2.group(2));
      }
    }
    // At least one string doesn't match
    return false;
  }

  /**
   * A pair of BibliographicItems represent the same (extended) volume if they
   * have the same value non-zero volume string along with appropriately
   * sequential year ranges.
   * @param first a BibliographicItem
   * @param second another BibliographicItem
   * @return whether the BibliographicItems seem to represent a volume extending over consecutive years
   * @throws NumberFormatException
   */
  static final boolean areExtendedVolume(BibliographicItem first,
                                         BibliographicItem second)
      throws NumberFormatException {
    String vol1 = first.getVolume();
    String vol2 = second.getVolume();
    // Same volume range (not 0), appropriately consecutive years
    return
        NumberUtil.areRangesEqual(vol1, vol2) &&
        areVolumeStringsValid(vol1, vol2) &&
        SORT_FIELD.YEAR.areAppropriatelySequenced(first, second);
  }

  /**
   * A pair of years represents an increasing sequence if the second
   * year is greater than or equal to the first year.
   *
   * @param first an integer representing the first date
   * @param second an integer representing the subsequent date
   * @return true if the difference between the years is non-negative
   */
  static final boolean areYearsIncreasing(int first, int second) {
    return areValuesIncreasing(first, second);
  }

  /**
   * A pair of years represents an increasing sequence if the second
   * year is greater than or equal to the first year. This version
   * works on years represented as strings. This method does not accept
   * year range strings, but does accept Roman numerals.
   *
   * @param first a string representing the first date
   * @param second a string representing the subsequent date
   * @return true if the strings could be parsed and the difference between the years is non-negative
   * @throws NumberFormatException if the Strings do not represent integers
   */
  static final boolean areYearsIncreasing(String first, String second)
      throws NumberFormatException {
    if (first==null || second==null) return false; //throw new NumberFormatException("Null value");
    return areYearsIncreasing(NumberUtil.parseInt(first),
        NumberUtil.parseInt(second));
  }

  /**
   * Checks whether a pair of year ranges appears to be in an order consistent
   * with a chronological ordering of volumes with no coverage gap. If a pair
   * of years or year ranges is <i>appropriately consecutive</i>, it suggests
   * that there is no coverage gap between them. If they are not
   * <i>appropriately consecutive</i> but they are <i>appropriately
   * sequential</i>, the volumes may be consulted to indicate whether or not
   * there is a coverage gap.
   * <p>
   * Note that the concept of <i>appropriate consecutivity</i> embodied here
   * is for establishing that BibliographicItems relating to a journal range are
   * reasonably ordered and 'consecutive enough' not to warrant the creation of
   * a coverage gap based on years. However there may be legitimate gaps
   * between consecutive volumes of greater than a year, and also multiple
   * volumes may get published within a single year - to assess this more
   * flexible criterion, the {@link areYearRangesAppropriatelySequenced()}
   * method should be used.
   * <p>
   * This method accounts for string-based years, representing either a single
   * year, or a year range in the form "1989-1991". For the purposes of
   * exposition, a single year A may be considered to be a year range of A to A.
   * In general, year ranges are considered to be appropriately consecutive if
   * there is some kind of overlap between the two ranges or if there is no
   * more than a year between them.
   * <blockquote>
   * <p>
   * A pair of year ranges are considered to be <i>appropriately consecutive</i>
   * if the second start year is greater than or equal to the first start year
   * while being at most one greater than the first end year (i.e. the start of
   * the second range follows immediately upon or is simultaneous with or is
   * within the first range) or if the first start year is included in the
   * range of the second (i.e. the start of the first range is within the
   * second). That is, it is <i>appropriately sequential</i> and there is no gap
   * between the end of the first range and the start of the second.
   * <br/>
   * <i>Note: perhaps we should also ensure that the first range's end year is
   * less than or equal to the second range's end year.</i>
   * </p>
   * </blockquote>
   * <p>
   * This rather flexible definition accounts for some oddities in the years
   * specified for certain journals - see for example:
   * <ul>
   * <li>Experimental Astronomy vols 3-5</li>
   * <li>Fresenius Zeitschrift f&uuml;r Analytische Chemie vols 275-277</li>
   * <li>Journal of Endocrinology vols 22-23</li>
   * <li>Proceedings of the Yorkshire Geological Society vols 10-11</li>
   * <li>Communication Disorders Quarterly vols 12-14</li>
   * </ul>
   * <p>
   * Even worse publication dates sometimes occur and will not be ratified by
   * this method; however luckily the journals involved do have consistent
   * volumes. For example:
   * <ul>
   * <li>Geological Society of London Memoirs vols 13-15</li>
   * <li>Geological Society of London Special Publications 287-289</li>
   * </ul>
   *
   * @param first a string representing a year or year range
   * @param second a string representing the subsequent year or year range
   * @return whether the year ranges may be considered to be appropriately consecutive
   * @throws NumberFormatException if the Strings do not represent year ranges or integers
   */
  static final boolean areYearRangesAppropriatelyConsecutive(String first,
                                                             String second)
      throws NumberFormatException {
    String e1 = NumberUtil.getRangeEnd(first);
    String s2 = NumberUtil.getRangeStart(second);
    // Appropriate sequencing, plus no significant gap between the ranges
    return areYearRangesAppropriatelySequenced(first, second) &&
        !isGapBetween(e1, s2);
  }

  /**
   * Check whether a pair of year ranges appears to be in an order consistent
   * with a chronological ordering of volumes. If each sequential pair of years
   * in a volume-ordered list of BibliographicItems is
   * <i>appropriately sequential</i>, it suggests that the ordering is correct.
   * If they are <i>appropriately sequential</i> but not <i>appropriately
   * consecutive</i>, the volumes may be consulted to indicate whether or not
   * there is a coverage gap.
   * <p>
   * Note that the concept of <i>appropriate sequence</i> embodied here is
   * useful for establishing that BibliographicItems in an ordered list of
   * volumes display a reasonable ordering of years. However <i>it is not an
   * appropriate concept for use in deciding where coverage gaps should occur
   * based on the year field. For this application the more strict
   * {@link #areYearRangesAppropriatelyConsecutive} method should be used
   * to decide where there are temporal breaks.</i>
   * <p>
   * This method accounts for string-based years, representing either a single
   * year or a year range in the form "1989-1991". For the purposes of
   * exposition, a single year A may be considered to be a year range of A to A.
   * In general, year ranges are considered to be appropriately sequenced if
   * there is some kind of overlap between the two ranges or if the start of
   * the second range is later than the start of the first.
   * <blockquote>
   * <p>
   * A pair of year ranges are considered to be <i>appropriately sequential</i>
   * if the second start year is greater than or equal to the first start year
   * (i.e. the start of the second range follows or is simultaneous with or is
   * within the first range) or if the first start year is included in the
   * range of the second (i.e. start of first is within second).
   * <br/>
   * <i>Note: perhaps we should also ensure that the first range's end year is
   * less than or equal to the second range's end year.</i>
   * </p>
   * </blockquote>
   *
   * @param first a string representing a year or year range
   * @param second a string representing the subsequent year or year range
   * @return whether the year ranges may be considered to be appropriately sequenced
   * @throws NumberFormatException if the Strings do not represent year ranges or integers
   */
  static final boolean areYearRangesAppropriatelySequenced(String first,
                                                           String second)
      throws NumberFormatException {
    String s1 = NumberUtil.getRangeStart(first);
    String s2 = NumberUtil.getRangeStart(second);
    return areYearsIncreasing(s1, s2) || NumberUtil.rangeIncludes(second, s1);
    // Alternatively, check also that if the first range starts after the second,
    // it is entirely contained within the second: && NumberUtil.rangeIncludes(second, e1)
  }

  /**
   * Calculate the average proportion of breaks there are in a set of title ranges.
   * @param ranges a list of title ranges
   * @param fieldToCheck the field to analyse for breaks
   * @return a decimal value between 0 and 1
   */
  static final float countProportionOfBreaks(List<TitleRange> ranges,
                                             SORT_FIELD fieldToCheck) {
    float total = 0;
    for (TitleRange tr: ranges) {
      total += countProportionOfBreaksInRange(tr.items, fieldToCheck);
    }
    return total/ranges.size();
  }

  /**
   * Calculate the average proportion of redundancy in a set of title ranges.
   * This is only really useful across coverage ranges, for the whole range of
   * the title.
   * @param ranges a list of title ranges
   * @param fieldToCheck the field to analyse for redundancy
   * @return a decimal value between 0 and 1
   */
  static final float countProportionOfRedundancy(List<TitleRange> ranges,
                                                 SORT_FIELD fieldToCheck) {
    float total = 0;
    for (TitleRange tr: ranges) {
      total += countProportionOfRedundancyInRange(tr.items, fieldToCheck);
    }
    return total/ranges.size();
  }

  /**
   * Calculate the proportion of value pairs in the range that have a break
   * between them. Note that if this method is passed a sequence which
   * represents a gapless range, the result will be zero if
   * <code>fieldToCheck</code> is the primary field informing the ordering.
   *
   * @param aus a list of BibliographicItems
   * @param fieldToCheck the field to analyse for breaks
   * @return a decimal value between 0 and 1
   */
  static final float countProportionOfBreaksInRange(
      List<? extends BibliographicItem> aus, SORT_FIELD fieldToCheck) {
    int numPairs = aus.size() - 1;
    if (numPairs<1) return 0;
    int total = 0;
    for (int i=1; i<=numPairs; i++) {
      BibliographicItem lastAu = aus.get(i-1);
      BibliographicItem thisAu = aus.get(i);
      // If there is not a full set of field values, return a high value
      if (!fieldToCheck.hasValue(lastAu) || !fieldToCheck.hasValue(thisAu))
        return 1f;
      // Compare the values to see if there is a break.
      try {
        if (!fieldToCheck.areAppropriatelyConsecutive(lastAu, thisAu)) {
          total++;
        }
      } catch (NumberFormatException e) {
        // There's something funny about the field values, so issue a warning
        // and carry on without counting a break.
        log.warning("Could not check if "+fieldToCheck+
            " values are appropriately consecutive: "+e.getMessage());
      }
    }
    return (float)total/(float)numPairs;
  }

  /**
   * Calculate the proportion of value pairs in the range that have a break
   * between them, for both volume and year. Note that if this method is
   * passed a sequence which represents a gapless range, the result will be
   * zero on the primary field informing the ordering.
   *
   * @param aus a list of BibliographicItems
   * @return a pair of decimal values between 0 and 1
   */
  static final Score countProportionOfBreaksInRange(
      List<? extends BibliographicItem> aus) {
    int numPairs = aus.size() - 1;
    if (numPairs<1) return new Score(0, 0);
    float volScore = 0, yearScore = 0;
    boolean volDone = false, yearDone = false;
    int totalv = 0, totaly = 0;
    for (int i=1; i<=numPairs; i++) {
      BibliographicItem lastAu = aus.get(i-1);
      BibliographicItem thisAu = aus.get(i);
      // ----------------------------------------------------------------------
      // Calculate for vols
      if (!volDone) {
        // If there is not a full set of field values, return a high value
        if (!SORT_FIELD.VOLUME.hasValue(lastAu) || !SORT_FIELD.VOLUME.hasValue(thisAu)) {
          volScore = 1f;
          volDone = true;
        } else {
          // Compare the values to see if there is a break.
          try {
            if (!SORT_FIELD.VOLUME.areAppropriatelyConsecutive(lastAu, thisAu)) {
              totalv++;
            }
          } catch (NumberFormatException e) {
            // There's something funny about the field values, so issue a warning
            // and carry on without counting a break.
            log.warning("Could not check if "+SORT_FIELD.VOLUME+
                " values are appropriately consecutive: "+e.getMessage());
          }
        }
      }
      // ----------------------------------------------------------------------
      // Calculate for years
      if (!yearDone) {
        // If there is not a full set of field values, return a high value
        if (!SORT_FIELD.YEAR.hasValue(lastAu) || !SORT_FIELD.YEAR.hasValue(thisAu)) {
          yearScore = 1f;
          yearDone = true;
        } else {
          // Compare the values to see if there is a break.
          try {
            if (!SORT_FIELD.YEAR.areAppropriatelyConsecutive(lastAu, thisAu)) {
              totaly++;
            }
          } catch (NumberFormatException e) {
            // There's something funny about the field values, so issue a warning
            // and carry on without counting a break.
            log.warning("Could not check if "+SORT_FIELD.YEAR+
                " values are appropriately consecutive: "+e.getMessage());
          }
        }
      }
      // Break the loop if we have vals for both vol and year because
      // neither has a full set of field values
      if (volDone && yearDone) break;
    }
    return new Score(
        (float)totalv/(float)numPairs,
        (float)totaly/(float)numPairs
    );
  }

  /**
   * Calculate the proportion of year pairs in the range that have a break
   * between them which is not also apparent in the volume field. When
   * there is a parallel break in volumes, the break is less likely to indicate
   * a problem in the years. By contrast, any break in volume is considered a
   * coverage gap.
   *
   * @param aus a list of BibliographicItems
   * @return a decimal value between 0 and 1
   */
  static final float countProportionOfUniquelyYearBreaks(
      List<? extends BibliographicItem> aus) {
    SORT_FIELD yearField = SORT_FIELD.YEAR;
    int numPairs = aus.size() - 1;
    if (numPairs<1) return 0;
    int total = 0;
    for (int i=1; i<=numPairs; i++) {
      BibliographicItem lastAu = aus.get(i-1);
      BibliographicItem thisAu = aus.get(i);
      // If there is not a full set of field values, return a high value
      if (!yearField.hasValue(lastAu) || !yearField.hasValue(thisAu))
        return 1f;
      try {
        // Compare the values to see if there is a break.
        boolean isYearBreak = !yearField.areAppropriatelyConsecutive(lastAu, thisAu);
        // Don't count a year break when there is a parallel break in the volume
        // field. This will only apply when counting a full unsplit sequence, and
        // should not occur when counting breaks in coverage ranges.
        if (isYearBreak) {
          // First check that volume fields are available for reference and not null.
          SORT_FIELD volField = SORT_FIELD.VOLUME;
          boolean isVolAvailable = volField.hasValue(lastAu) && volField.hasValue(thisAu);
          // Count a break if the volume info is not available, or if there is
          // no parallel break in volume
          if (!isVolAvailable || volField.areAppropriatelyConsecutive(lastAu, thisAu))
            total++;
        }
      } catch (NumberFormatException e) {
        // There's something funny about the year field values, so issue a
        // warning and carry on without counting a break.
        log.warning("Could not check if year values constitute a break. " +
            e.getMessage());
      }
    }
    return (float)total/(float)numPairs;
  }

  /**
   * Calculate the proportion of value pairs in the range that have a break
   * between them which represents decreasing values. Note that breaks are
   * identified using the {@link SORT_FIELD.areAppropriatelyConsecutive()}
   * method, and descending year values may actually be appropriately
   * consecutive. To count towards the proportion, years must be both
   * non-consecutive by that measure, and also decreasing.
   *
   * @param aus a list of BibliographicItems
   * @param fieldToCheck the field to analyse for breaks
   * @return a decimal value between 0 and 1
   */
  static final float countProportionOfNegativeBreaksInRange(
      List<? extends BibliographicItem> aus, SORT_FIELD fieldToCheck) {
    int numPairs = aus.size() - 1;
    if (numPairs<1) return 0;
    int total = 0;
    for (int i=1; i<=numPairs; i++) {
      BibliographicItem lastAu = aus.get(i-1);
      BibliographicItem thisAu = aus.get(i);
      String lastVal = fieldToCheck.getValueForComparisonAsPrevious(lastAu);
      String thisVal = fieldToCheck.getValueForComparisonAsCurrent(thisAu);
      // If there is not a full set of field values, return a high value
      if (   StringUtil.isNullString(lastVal) 
          || StringUtil.isNullString(thisVal)) return 1f;
      // Record a negative break
      try {
        if (fieldToCheck.areDecreasing(lastVal, thisVal)) {
          total++;
        }
      } catch (NumberFormatException e) {
        // There's something funny about the field values, so issue a
        // warning and carry on without counting a negative break.
        log.warning("Could not check if " + fieldToCheck 
            + " values constitute a negative break: \"" 
            + lastVal + "\" : \"" + thisVal + "\". " + e.getMessage());
      }
    }
    return (float)total/(float)numPairs;
  }


  /**
   * Calculate the proportion of value pairs in the range that have a break
   * between them which represents decreasing values. We do this for both
   * volume and year. Note that breaks are identified using the
   * {@link SORT_FIELD.areAppropriatelyConsecutive()}
   * method, and descending year values may actually be appropriately
   * consecutive. To count towards the proportion, years must be both
   * non-consecutive by that measure, and also decreasing.
   *
   * @param aus a list of BibliographicItems
   * @return a pair of decimal values between 0 and 1
   */
  static final Score countProportionOfNegativeBreaksInRange(
      List<? extends BibliographicItem> aus) {
    float volScore = 0, yearScore = 0;
    boolean volDone = false, yearDone = false;
    int numPairs = aus.size() - 1;
    if (numPairs<1) return new Score(0, 0);
    int totalv = 0, totaly = 0;
    for (int i=1; i<=numPairs; i++) {
      BibliographicItem lastAu = aus.get(i-1);
      BibliographicItem thisAu = aus.get(i);
      // ----------------------------------------------------------------------
      // Calculate for volume
      if (!volDone) {
        String lastValVol = SORT_FIELD.VOLUME.getValueForComparisonAsPrevious(lastAu);
        String thisValVol = SORT_FIELD.VOLUME.getValueForComparisonAsCurrent(thisAu);
        // If there is not a full set of field values, return a high value
        if (   StringUtil.isNullString(lastValVol) 
            || StringUtil.isNullString(thisValVol)) {
          volScore = 1f;
          volDone = true;
        }
        // Record a negative break
        else try {
          if (SORT_FIELD.VOLUME.areDecreasing(lastValVol, thisValVol)) {
            totalv++;
        }
        } catch (NumberFormatException e) {
          // There's something funny about the field values, so issue a
          // warning and carry on without counting a negative break.
          log.warning("Could not check if " + SORT_FIELD.VOLUME
              + " values constitute a negative break: \"" 
              + lastValVol + "\" : \"" + thisValVol + "\". " + e.getMessage());
        }
      }
      // ----------------------------------------------------------------------
      // Calculate for year
      if (!yearDone) {
        String lastValYear = SORT_FIELD.YEAR.getValueForComparisonAsPrevious(lastAu);
        String thisValYear = SORT_FIELD.YEAR.getValueForComparisonAsCurrent(thisAu);
        // If there is not a full set of field values, return a high value
        if (   StringUtil.isNullString(lastValYear) 
            || StringUtil.isNullString(thisValYear)) {
          yearScore = 1f;
          yearDone = true;
        }
        // Record a negative break
        if (!yearDone) try {
          if (SORT_FIELD.YEAR.areDecreasing(lastValYear, thisValYear)) {
            totaly++;
        }
        } catch (NumberFormatException e) {
          // There's something funny about the field values, so issue a
          // warning and carry on without counting a negative break.
          log.warning("Could not check if " + SORT_FIELD.YEAR
              + " values constitute a negative break: \"" 
              + lastValYear + "\" : \"" + thisValYear + "\". " + e.getMessage());
        }
      }
      // Break the loop if we have vals for both vol and year because
      // neither has a full set of field values
      if (volDone && yearDone) break;
    }
    // ----------------------------------------------------------------------
    // Calculate scores if not already done
    if (!volDone) volScore = (float)totalv/(float)numPairs;
    if (!yearDone) yearScore = (float)totaly/(float)numPairs;
    return new Score(volScore, yearScore);
  }

  /**
   * How much redundancy there is in the values of the range. <s>For volumes,
   * which should not have duplicates (but do), this is the proportion of values
   * which are repeated. For years, which can have duplicates that should be
   * consecutive, this is the proportion of values which duplicate earlier
   * values but not their preceding value.</s>
   * <p>
   * Duplicates can appear in both year and volume fields. For volumes, this is
   * mostly because of duplicate records due to something like a publisher
   * moving to a different platform. It should not (but does) occur within the
   * volume fields of a single journal run. For this reason, a duplicate value
   * is not counted if it duplicates only the preceding value.
   *
   * @param aus a list of BibliographicItems
   * @param fieldToCheck the field to analyse for redundancy
   * @return a decimal value between 0 and 1
   */
  static final float countProportionOfRedundancyInRange(
      List<? extends BibliographicItem> aus, SORT_FIELD fieldToCheck) {
    int numVals = aus.size();
    if (numVals<2) return 0;
    Set<String> uniqueVals = new HashSet<String>();
    String lastValue = null;
    int redundantEntries = 0;
    for (BibliographicItem au : aus) {
      // If there is not a full set of field values, return a high value
      if (!fieldToCheck.hasValue(au)) return 1f;
      String value = fieldToCheck.getValue(au);
      // If the value is a duplicate and different from the last value, record it
      if (!uniqueVals.add(value) && !value.equals(lastValue)) redundantEntries++;
      lastValue = value;
    }
    return (float)redundantEntries / (float)numVals;
  }

  /**
   * How much redundancy there is in the values of the range. <s>For volumes,
   * which should not have duplicates (but do), this is the proportion of values
   * which are repeated. For years, which can have duplicates that should be
   * consecutive, this is the proportion of values which duplicate earlier
   * values but not their preceding value.</s>
   * <p>
   * Duplicates can appear in both year and volume fields. For volumes, this is
   * mostly because of duplicate records due to something like a publisher
   * moving to a different platform. It should not (but does) occur within the
   * volume fields of a single journal run. For this reason, a duplicate value
   * is not counted if it duplicates only the preceding value.
   *
   * @param aus a list of BibliographicItems
   * @return a pair of decimal values between 0 and 1
   */
  static final Score countProportionOfRedundancyInRange(
      List<? extends BibliographicItem> aus) {
    float volScore = 0, yearScore = 0;
    boolean volDone = false, yearDone = false;
    int numVals = aus.size();
    if (numVals<2) return new Score(0,0);
    Set<String> uniqueValsVol = new HashSet<String>();
    Set<String> uniqueValsYear = new HashSet<String>();
    String lastValueVol = null;
    String lastValueYear = null;
    int redundantEntriesVol = 0,redundantEntriesYear = 0;
    for (BibliographicItem au : aus) {
      // If there is not a full set of field values, return a high value
      // If the value is a duplicate and different from the last value, record it
      if (!SORT_FIELD.VOLUME.hasValue(au)) {
        volScore = 1f;
        volDone = true;
      } else {
        String valueVol = SORT_FIELD.VOLUME.getValue(au);
        if (!uniqueValsVol.add(valueVol) && !valueVol.equals(lastValueVol)) redundantEntriesVol++;
        lastValueVol = valueVol;
      }
      // Year calcs
      if (!SORT_FIELD.YEAR.hasValue(au)) {
        yearScore = 1f;
        yearDone = true;
      } else {
        String valueYear = SORT_FIELD.YEAR.getValue(au);
        if (!uniqueValsYear.add(valueYear) && !valueYear.equals(lastValueYear)) redundantEntriesYear++;
        lastValueYear = valueYear;
      }
      // Break the loop if we have vals for both vol and year because 
      // neither has a full set of field values
      if (volDone && yearDone) break;
    }
    // ----------------------------------------------------------------------
    // Calculate scores if not already done
    if (!volDone) volScore = (float)redundantEntriesVol / (float)numVals;
    if (!yearDone) yearScore = (float)redundantEntriesYear / (float)numVals;
    return new Score(volScore, yearScore);
  }

  /**
   * Determine whether a list of BibliographicItems is ordered in such a way that the
   * sequence of values in the specified field is monotonically increasing,
   * that is, the consecutive values do not decrease at any point. Note that
   * the values of some fields change format over the course of a publication;
   * the primary and most obvious example is switching between a purely numeric
   * and a string format. For this reason, a switch of formats between the
   * pairs is allowed.
   *
   * @param aus a list of BibliographicItems
   * @param fieldToCheck the field to check for monotonic increase
   * @return whether the list of BibliographicItems shows monotonic increase in the specified value
   */
  static final boolean isMonotonicallyIncreasing(
      List<? extends BibliographicItem> aus, SORT_FIELD fieldToCheck) {
    int numPairs = aus.size() - 1;
    if (numPairs<1) return true; // Uninterestingly true
    String thisVal, lastVal;
    for (int i=1; i<=numPairs; i++) {
      lastVal = fieldToCheck.getValueForComparisonAsPrevious(aus.get(i-1));
      thisVal = fieldToCheck.getValueForComparisonAsCurrent(aus.get(i));
      if (!fieldToCheck.areIncreasing(lastVal, thisVal)) {
        // Check if there is a change of formats
        if (BibliographicUtil.changeOfFormats(lastVal, thisVal)) {
          log.warning(String.format("Ignoring change of formats in %s: %s %s\n",
              fieldToCheck, lastVal, thisVal));
          continue;
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Uses a rough heuristic to calculate a consistency value for
   * the sequence of start years provided by the given list of AUs.
   * Contributing measures include the proportion of redundancy,
   * the number of breaks which are unique to the year field, and
   * the number of negative breaks.
   *
   * @param aus a list of BibliographicItems describing a full ordered sequence
   * @return a consistency rating for the sequence of values in the year field
   */
  static final float getYearListConsistency(
      List<? extends BibliographicItem> aus) {
    // Combine redundancy, breaks, etc to produce a measure
    float red = countProportionOfRedundancyInRange(aus, SORT_FIELD.YEAR);
    // For years in a sequence, we only count breaks which occur uniquely in the
    // year field; if there is a volume break too it is not considered a problem.
    // Positive year breaks are expected in a list of AUs that has not been split
    // on coverage gaps.
    float brk = countProportionOfUniquelyYearBreaks(aus);
    float negbrk = countProportionOfNegativeBreaksInRange(aus, SORT_FIELD.YEAR);
    return (1-red) * (1-brk) * (1-negbrk);
  }

  /**
   * Uses a rough heuristic to calculate a consistency value for
   * the sequence of volumes provided by the given list of AUs.
   * Contributing measures include the proportion of redundancy,
   * the number of breaks, and the number of negative breaks.
   *
   * @param aus a list of BibliographicItems describing a full ordered sequence
   * @return a consistency rating for the sequence of values in the volume field
   */
  static final float getVolumeListConsistency(
      List<? extends BibliographicItem> aus) {
    // For volumes in a sequence, although there should be no duplication,
    // we do sometimes see it because there are two copies of an AU out there,
    // one 'released' and one 'down'. These will get interleaved in the AU
    // ordering, and so we accept redundancy between consecutive volumes.
    float red = countProportionOfRedundancyInRange(aus, SORT_FIELD.VOLUME);
    float brk = countProportionOfBreaksInRange(aus, SORT_FIELD.VOLUME);
    float negbrk = countProportionOfNegativeBreaksInRange(aus, SORT_FIELD.VOLUME);
    return (1-red) * (1-brk) * (1-negbrk);
  }

  /**
   * Get a consistency score for the years in a set of coverage ranges, which
   * were calculated based on a particular ordering.
   * <p>
   * Contributing measures include the proportion of redundancy, the proportion
   * of breaks, and the proportion of negative breaks. These are calculated for
   * each range and then averaged. Finally a discount is applied based on the
   * frequency of coverage gaps (ranges) in the full sequence. In general,
   * a good ordering should produce less coverage gaps. Note that we do not
   * use {@link countProportionOfUniquelyYearBreaks()} in this measure, so all
   * year breaks in a specific coverage range detract from the value of the
   * year ordering.
   *
   * @param ranges a list of ranges calculated from an ordering
   * @return a consistency score for the calculated ranges
   */
  static final float getYearRangeConsistency(List<TitleRange> ranges) {
    SORT_FIELD sf = SORT_FIELD.YEAR;
    float totalRed = 0, totalBrk = 0, totalAus = 0, totalNegbrk = 0;
    for (TitleRange rng : ranges) {
      float red = countProportionOfRedundancyInRange(rng.items, sf);
      float brk = countProportionOfBreaksInRange(rng.items, sf);
      float negbrk = countProportionOfNegativeBreaksInRange(rng.items, sf);
      totalRed += red;
      totalBrk += brk;
      totalNegbrk += negbrk;
      totalAus += rng.items.size();
    }
    float numRanges = (float)ranges.size();
    // Discount for large number of ranges - prefer less ranges
    float gfs = getCoverageGapFrequencyDiscount((float)totalAus, numRanges);
    // Average the redundancy and break scores
    float red = totalRed/numRanges;
    float brk = totalBrk/numRanges;
    float negbrk = totalNegbrk/numRanges;
    return (1-red) * (1-brk) * (1-gfs) * (1-negbrk);
  }

  /**
   * Get a consistency score for the volumes in a set of coverage ranges, which
   * were calculated based on a particular ordering.
   * <p>
   * Contributing measures include the proportion of redundancy, the proportion
   * of breaks, and the proportion of negative breaks. These are calculated for
   * each range and then averaged. Finally a discount is applied based on the
   * frequency of coverage gaps (ranges) in the full sequence. In general,
   * a good ordering should produce less coverage gaps.
   *
   * @param ranges a list of ranges calculated from an ordering
   * @return a consistency score for the calculated ranges
   */
  static final float getVolumeRangeConsistency(List<TitleRange> ranges) {
    SORT_FIELD sf = SORT_FIELD.VOLUME;
    float totalRed = 0, totalBrk = 0, totalAus = 0, totalNegbrk = 0;
    for (TitleRange rng : ranges) {
      float red = countProportionOfRedundancyInRange(rng.items, sf);
      float brk = countProportionOfBreaksInRange(rng.items, sf);
      float negbrk = countProportionOfNegativeBreaksInRange(rng.items, sf);
      totalRed += red;
      totalBrk += brk;
      totalNegbrk += negbrk;
      totalAus += rng.items.size();
    }
    float numRanges = (float)ranges.size();
    // Discount for large number of ranges - prefer less ranges
    float gfs = getCoverageGapFrequencyDiscount((float)totalAus, numRanges);
    // Average the redundancy and break scores
    float red = totalRed/numRanges;
    float brk = totalBrk/numRanges;
    float negbrk = totalNegbrk/numRanges;
    return (1-red) * (1-brk) * (1-gfs) * (1-negbrk);
  }

  /**
   * Get a consistency score for both volumes and years in a set of coverage
   * ranges, performing both calculations in the same iteration over the ranges.
   * <p>
   * Contributing measures include the proportion of redundancy, the proportion
   * of breaks, and the proportion of negative breaks. These are calculated for
   * each range and then averaged. Finally a discount is applied based on the
   * frequency of coverage gaps (ranges) in the full sequence. In general,
   * a good ordering should produce less coverage gaps.
   *
   * @param ranges a list of ranges calculated from an ordering
   * @return vol/year consistency scores for the calculated ranges
   */
  static final Score getRangeConsistency(List<TitleRange> ranges) {
    float totalAus = 0;
    float totalRedVol = 0, totalBrkVol = 0, totalNegbrkVol = 0;
    float totalRedYear = 0, totalBrkYear = 0, totalNegbrkYear = 0;
    for (TitleRange rng : ranges) {
      totalAus += rng.items.size();
      // Use new methods
      Score redScore = countProportionOfRedundancyInRange(rng.items);
      Score brkScore = countProportionOfBreaksInRange(rng.items);
      Score negbrkScore = countProportionOfNegativeBreaksInRange(rng.items);
      // Vols
      totalRedVol += redScore.volScore;
      totalBrkVol += brkScore.volScore;
      totalNegbrkVol += negbrkScore.volScore;
      // Years
      totalRedYear += redScore.yearScore;
      totalBrkYear += brkScore.yearScore;
      totalNegbrkYear += negbrkScore.yearScore;
    }
    float numRanges = (float)ranges.size();
    // Discount for large number of ranges - prefer less ranges
    float gfs = getCoverageGapFrequencyDiscount((float)totalAus, numRanges);
    // Average the redundancy and break scores for vol and year
    float redv = totalRedVol/numRanges;
    float brkv = totalBrkVol/numRanges;
    float negbrkv = totalNegbrkVol/numRanges;
    float redy = totalRedYear/numRanges;
    float brky = totalBrkYear/numRanges;
    float negbrky = totalNegbrkYear/numRanges;
    return new Score(
        (1-redv) * (1-brkv) * (1-gfs) * (1-negbrkv),
        (1-redy) * (1-brky) * (1-gfs) * (1-negbrky)
    );
  }

  /**
   * The more frequently coverage gaps occur, the lower the score. Sometimes
   * there are genuinely lots of coverage gaps, but in general we prefer an
   * ordering which produces fewer gaps.
   *
   * @param numAus how many BibliographicItems are in the sequence
   * @param numRanges how many ranges are in the sequence; should be less than or equal to <code>numAus</code>
   * @return a floating point multiplier between 0 and 1 - 1/numAus which is greater the less gaps there are
   */
  static final float getCoverageGapFrequencyDiscount(float numAus, float numRanges) {
    if (numAus==0f) return 0f;
    return (numRanges - 1) / numAus;
  }

  /**
   * Calculate a consistency score for the given list of calculated ranges.
   *
   * @param aus the full list of aus ordered by the sortField
   * @param ranges an ordered list of title ranges derived from an ordering
   * @return a score between 0 and 1
   */
  public static final ConsistencyScoreWithRanges getConsistencyScore(
      List<? extends BibliographicItem> aus,
      List<TitleRange> ranges) {
    return new ConsistencyScoreWithRanges(new Calculator(aus, ranges));
  }

  // This is the original getConsistencyScore, which used to call individual
  // methods for volume and year.
  // The newer approach which calls combined methods appears to be no faster,
  // and in fact slightly slower for the very small numbers of AUs in titles

  /**
   * @deprecated
   */
  protected static final ConsistencyScoreWithRanges getConsistencyScoreOld(
      List<? extends BibliographicItem> aus,
      List<TitleRange> ranges) {
    //long s = System.currentTimeMillis();
    float volScore = getVolumeRangeConsistency(ranges);
    float yearScore = getYearRangeConsistency(ranges);
    float volListScore  = BibliographicOrderScorer.getVolumeListConsistency(aus);
    float yearListScore = BibliographicOrderScorer.getYearListConsistency(aus);
    return new ConsistencyScoreWithRanges(volScore, yearScore,
        volListScore, yearListScore, ranges);
  }

  /**
   * Look at the scores and decide whether the volume ordering yields a net
   * benefit over the year scores and should be preferred.
   * If the orderings are isomorphic, the relative benefits will be equal,
   * so we should check first if the year scores are better than vol scores.
   * @param volScore  the ConsistencyScore for volume ordering
   * @param yearScore the ConsistencyScore for year ordering
   * @return whether to prefer the volume ordering
   */
  public static final boolean preferVolume(ConsistencyScore volScore,
                                           ConsistencyScore yearScore) {

    // If both scores contain zero scores, we have to decide another way,
    // so prefer volume if it has a full set of values, otherwise year.
    if (volScore.allScoresAreZero() && yearScore.allScoresAreZero()) {
      return !volScore.hasMissingVolValues;
    }

    // If there are non-zero scores, calculate relative benefit and loss
    float volBenefit = calculateRelativeBenefitToVolume(volScore, yearScore);
    float yrLoss = calculateRelativeLossToYear(volScore, yearScore);

    // If benefit scores are equal, check if year scores are greater; otherwise prefer volume
    if (volBenefit == yrLoss) {
      return !(volScore.yearScoresAreBetter() && yearScore.yearScoresAreBetter());
    }
    // Use the volume ordering if the benefit of using it outweighs the loss to year orderings
    else return volBenefit > yrLoss;
  }

  /**
   * Calculate the relative benefit to volume orderings of the first ordering
   * over the second ordering. This is the increase in the aggregated volume
   * score plus the increase in the volume score for the full sequence.
   * The return value will be negative, zero or positive as the first
   * consistency score represents relative deficit, equality or benefit to
   * the second.
   * @param firstScore  the ConsistencyScore for the first ordering
   * @param secondScore the ConsistencyScore for the second ordering
   * @return the relative benefit to volume sequences of using the first ordering
   */
  static final float calculateRelativeBenefitToVolume(ConsistencyScore firstScore,
                                                      ConsistencyScore secondScore) {
    // Relative benefit of first ordering to volume coverage ranges
    float firstOrdVolBen = firstScore.volScore - secondScore.volScore;
    // Relative benefit of first ordering to full ordered volume sequence
    float firstOrdVolListBen = firstScore.volListScore - secondScore.volListScore;
    // Benefit to volume sequence and ranges of using the first ordering
    return firstOrdVolBen + firstOrdVolListBen;
  }

  /**
   * Calculate the relative loss to year orderings of the first ordering
   * over the second ordering. This is the decrease in the aggregated year
   * score plus the decrease in the year score for the full sequence.
   * The return value will be negative, zero or positive as the first
   * consistency score represents relative benefit, equality or deficit/loss to
   * the second. Note that a positive result indicates loss.
   * @param firstScore  the ConsistencyScore for the first ordering
   * @param secondScore the ConsistencyScore for the second ordering
   * @return the relative loss to year sequences of using the first ordering
   */
  static final float calculateRelativeLossToYear(ConsistencyScore firstScore,
                                                 ConsistencyScore secondScore) {
    // Relative loss of first ordering to year coverage ranges
    float firstOrdYearLoss = secondScore.yearScore - firstScore.yearScore;
    // Relative loss of first ordering to full ordered year sequence
    float firstOrdYearListLoss = secondScore.yearListScore - firstScore.yearListScore;
   // Benefit to year sequence and ranges of using the first ordering
    return firstOrdYearLoss + firstOrdYearListLoss;

    // Note that the loss of using first over second is equal to the benefit
    // of using the second over the first, in case that is more intelligible.
    // Relative benefit of second ordering to year coverage ranges
    //float secondOrdYearBen = firstScore.yearScore - secondScore.yearScore;
    // Relative benefit of second ordering to full ordered year sequence
    //float secondOrdYearListBen = firstScore.yearListScore - secondScore.yearListScore;
    //return secondOrdYearBen + secondOrdYearListBen;
  }

  /**
   * A class for grouping together the calculations associated with consistency
   * scoring; this allows a range of metrics to be calculated on the AUs in a
   * reduced number of iterations over the list. It is supported by a set of new
   * BibliographicOrderScorer methods which provide combined functionality,
   * returning multiple values in a Score object.
   */
  protected static class Calculator {

    final List<? extends BibliographicItem> aus;
    final List<TitleRange> ranges;
    
    // Calculate all the figures
    float volumeRangeConsistency = 0;
    float yearRangeConsistency = 0;
    float volumeListConsistency = 0;
    float yearListConsistency = 0;

    /**
     * Create a score calculator over the given set of AUs and the ordered
     * ranges. The calculations are done immediately.
     * @param aus
     * @param ranges
     */
    protected Calculator(List<? extends BibliographicItem> aus, List<TitleRange> ranges) {
      this.aus = aus;
      this.ranges = ranges;
      calculateAll();
    }

    /**
     * Calculate all the metrics. The calls use new version of the methods which
     * perform calculations simultaneously for both volume and year, thereby
     * reducing the number of iterations over the ranges and lists, and return
     * both scores in a Score object.
     */
    private void calculateAll() {
      // Calculate range consistency scores
      Score rangeConsistency = getRangeConsistency(ranges);
      this.volumeRangeConsistency = rangeConsistency.volScore;
      this.yearRangeConsistency = rangeConsistency.yearScore;
      // Calculate list consistency scores
      Score redScore = countProportionOfRedundancyInRange(aus);
      Score brkScore = countBreaksInRange(aus);
      Score negbrkScore = countProportionOfNegativeBreaksInRange(aus);
      //System.out.format("red %s, brk %s, negbrk %s", redScore, brkScore, negbrkScore);
      this.volumeListConsistency = (1-redScore.volScore) * (1-brkScore.volScore) * (1-negbrkScore.volScore);
      this.yearListConsistency = (1-redScore.yearScore) * (1-brkScore.yearScore) * (1-negbrkScore.yearScore);
    }

    /**
     * Volume and year have different metrics for breaks and so are calculated
     * separately within this method; it would be nice to have a static method
     * in BibliographicOrderScorer which combines the algorithms in one
     * iteration over the list, if possible.
     * @param aus
     * @return
     */
    private Score countBreaksInRange(List<? extends BibliographicItem> aus) {
      return new Score(
          countProportionOfBreaksInRange(aus, SORT_FIELD.VOLUME),
          countProportionOfUniquelyYearBreaks(aus)
      );
    }

  }
  

  /**
   * A simple wrapper for a pair of float-valued scores for volume and year.
   */
  protected static class Score {
    final float volScore;
    final float yearScore;
    
    public Score(float volScore, float yearScore) {
      super();
      this.volScore = volScore;
      this.yearScore = yearScore;
    }

    @Override
    public String toString() {
      return "Score{" +
          "volScore=" + volScore +
          ", yearScore=" + yearScore +
          '}';
    }
  }

}
