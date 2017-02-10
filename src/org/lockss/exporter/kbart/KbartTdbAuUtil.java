/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.oro.text.regex.*;
import org.lockss.config.TdbAu;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.util.*;

/**
 * Utility methods for extracting KBART data from a <code>Tdb</code> structure.
 * 
 * @author Neil Mayo
 * @deprecated A lot of this class is now redundant, having been superseded by
 * {@link org.lockss.util.NumberUtil} methods, genericised methods in
 * {@link org.lockss.exporter.biblio.BibliographicUtil}, or dedicated methods in
 * {@link org.lockss.config.TdbAu} which leverage the greater availability and
 * completeness of metadata.
 */
public class KbartTdbAuUtil {

  private static final Logger log = Logger.getLogger(KbartTdbAuUtil.class);

  // Default attribute keys
  static final String DEFAULT_YEAR_ATTR = "year";
  static final String DEFAULT_VOLUME_ATTR = "volume";
  static final String DEFAULT_TITLE_URL_ATTR = "base_url";

  // Default parameter keys
  static final String DEFAULT_YEAR_PARAM = "year";
  static final String DEFAULT_VOLUME_PARAM = "volume";
  static final String DEFAULT_VOLUME_PARAM_NAME = "volume_name";

  // Default property keys
  static final String DEFAULT_ISSN_PROP = "issn";
  static final String DEFAULT_EISSN_PROP = "eissn";
  static final String DEFAULT_ISSNL_PROP = "issnl";

  /**
   * Attempts to find a Volume string for an AU. Looks for known parameters and
   * attributes, and if nothing is found returns the empty string. The AU's
   * property maps are searched for the following, in the order given: an
   * attribute called "volume"; a parameter called "volume"; a parameter called
   * "volume_name". Sometimes there is a "volume_str" parameter, but it is too
   * inconsistent to parse at this point.
   * 
   * @param au the TdbAu whose params/attributes to search for a volume string
   * @return the value of an existing key, or an empty string
   */
  static String findVolume(TdbAu au) {
    String s = findAuInfo(au, DEFAULT_VOLUME_ATTR, AuInfoType.ATTR);
    if (StringUtils.isEmpty(s)) s = findAuInfo(au, DEFAULT_VOLUME_PARAM, AuInfoType.PARAM);
    if (StringUtils.isEmpty(s)) s = findAuInfo(au, DEFAULT_VOLUME_PARAM_NAME, AuInfoType.PARAM);
    return s==null ? "" : s;
  }

  /**
   * Attempts to find an appropriate IssueFormat for an AU's issue string. Looks
   * for known issue parameters and attributes, and if nothing is found returns
   * null. If the item is not a TdbAu, null is returned.
   * 
   * @param au the TdbAu whose params/attributes to search for an issue key
   * @return the appropriate IssueFormat, or null if no issue key/value found
   */
  static IssueFormat identifyIssueFormat(BibliographicItem au) {
    if (au instanceof TdbAu) {
      for (IssueFormat format : IssueFormat.values()) {
        if (!StringUtils.isEmpty( format.getIssueString((TdbAu)au) )) return format;
      }
    }
    return null;
  }

  /**
   * Attempts to find the year for an AU, looking first in the AU's attributes,
   * then in the parameters, and if nothing is found returns the empty string.
   * 
   * @param au the TdbAu whose properties to search for an attribute
   * @return the value of an existing key, or an empty string
   * @deprecated
   */
  static String findYear(TdbAu au) {
    String s = findAuInfo(au, DEFAULT_YEAR_ATTR, AuInfoType.ATTR);
    if (StringUtils.isEmpty(s))
      s = findAuInfo(au, DEFAULT_YEAR_PARAM, AuInfoType.PARAM);
    return s==null ? "" : s;
  }

  /**
   * Attempts to find the ISSN for an AU. Looks in the AU's properties, and if
   * nothing is found <del>returns the result of the AU's title's
   * <code>getId()</code> method</del>.
   * <p>
   * Note: This method now only returns values from the AU's properties and does
   * not substitute the result of getId(). It also validates the purported ISSN.
   * 
   * @param au the TdbAu whose properties to search for an attribute
   * @return a valid ISSN from the value of an existing key, or the empty String
   */
  static String findIssn(TdbAu au) {
    String s = au.getPrintIssn();
    if (!StringUtils.isEmpty(s)) {
      if (MetadataUtil.isIssn(s)) {
        return s;
      }
      log.warning(String.format("AU %s yielded an invalid non-empty ISSN: %s", au, s));
    }
    return "";
  }

  /**
   * Attempts to find the EISSN for an AU. Looks in the AU's properties, and if
   * nothing is found <del>returns the result of the AU's title's
   * <code>getId()</code> method</del>.
   * <p>
   * This method now only returns values from the AU's properties and does not
   * substitute the result of getId(). It also validates the purported ISSN.
   * 
   * @param au the TdbAu whose properties to search for an attribute
   * @return a valid ISSN from the value of an existing key, or the empty String
   */
  static String findEissn(TdbAu au) {
    String s = au.getEissn();
    if (!StringUtils.isEmpty(s)) {
      if (MetadataUtil.isIssn(s)) {
        return s;
      }
      log.warning(String.format("AU %s yielded an invalid non-empty eISSN: %s", au, s));
    }
    return "";
  }
  

  /**
   * Attempts to find the ISSN-L for an AU. Looks in the AU's properties, and if
   * nothing is found <del>returns the result of the AU's title's
   * <code>getId()</code> method</del>.
   * <p>
   * This method now only returns values from the AU's properties and does not
   * substitute the result of getId(). It also validates the purported ISSN.
   * 
   * @param au the TdbAu whose properties to search for an attribute
   * @return a valid ISSN from the value of an existing key, or the empty String
   */
  static String findIssnL(TdbAu au) {
    String s = au.getIssnL();
    if (!StringUtils.isEmpty(s)) {
      if (MetadataUtil.isIssn(s)) {
        return s;
      }
      log.warning(String.format("AU %s yielded an invalid non-empty ISSNL: %s", au, s));
    }
    return "";
  }
  

  /**
   * Attempts to find a property value by searching for the given key in the AU's properties 
   * map indicated by the AuInfoType. 
   * 
   * @param au the TdbAu whose properties to search for an attribute
   * @param key the key name to find
   * @param type the type of property map to search
   * @return the value of an existing key, or an empty string
   */
  static String findAuInfo(TdbAu au, String key, AuInfoType type) {
    if (key==null || type==null) return "";
    return type.findAuInfo(au, key);
  }

  /**
   * Attempts to find a property value by searching for the given key in the supplied map.
   * If the map is null or the key/value does not exist, an empty string is returned. 
   * 
   * @param map the map to search
   * @param key the key name to find
   * @return the value of an existing key, or an empty string
   */
  private static String findMapValue(Map<String,String> map, String key) {
    if (map==null) return "";
    String s = map.get(key);
    return s==null ? "" : s;
  }

  
  // The following two methods are an attempt at flexible search but are not used any more.
  /**
   * Attempts to find an AU attribute or parameter or property value by searching for the given key 
   * in each of the AU's maps in that order. 
   * 
   * @param au the TdbAu whose properties to search for an attribute
   * @param key the key name to find
   * @return the value of an existing key, or an empty string
   */
  private static String findAuAttrParamProp(TdbAu au, String key) {
    String s = findAuInfo(au, key, AuInfoType.ATTR);
    if (StringUtils.isEmpty(s)) s = findAuInfo(au, key, AuInfoType.PARAM);
    if (StringUtils.isEmpty(s)) s = findAuInfo(au, key, AuInfoType.PROP);
    return s==null ? "" : s;
  }

  /**
   * Attempts to find a map key by searching for each of the 
   * supplied keys in order.
   * 
   * @param au the TdbAu to search for an attribute
   * @param defaultKeyList an array of possible key names, in descending order of importance
   * @param type an info type that has been established for the keys 
   * @return an existing key name, or null
   */
  protected static String findMapKey(TdbAu au, String[] defaultKeyList, AuInfoType type) {
    if (type==null || au==null) return null;
    for (String key: defaultKeyList) {
      String s = findAuInfo(au, key, type);
      if (!StringUtils.isEmpty(s)) return key;
    }
    return null;
  }
  
  /**
   * Attempts to find a map key by searching for each of the 
   * supplied keys in order.
   * 
   * @param au the TdbAu to search for an attribute
   * @param defaultKeyList an array of possible key names, in descending order of importance
   * @return an existing key name, or null
   */
  protected static String findMapKey(TdbAu au, String[] defaultKeyList) {
    AuInfoType type = findAuInfoType(au, defaultKeyList);
    return findMapKey(au, defaultKeyList, type);
  }

  /**
   * Attempts to find a map key by searching for the supplied key in 
   * each AuInfoType's map, in the order they are enumerated.
   * 
   * @param au the TdbAu to search for an attribute
   * @param key a key name
   * @return an AuInfoType, or null
   */
  static AuInfoType findAuInfoType(TdbAu au, String key) {
    for (AuInfoType type: AuInfoType.values()) {
      String s = findAuInfo(au, key, type);
      if (!StringUtils.isEmpty(s)) return type;
    }
    return null;
  }
  
  /**
   * Attempts to find a map key by searching for each of a selection of keys in turn,
   * in each AuInfoType's map, in the order the AuInfoTypes are enumerated.
   * 
   * @param au the TdbAu to search for an attribute
   * @param keys an array of key names
   * @return an AuInfoType, or null
   */
  static AuInfoType findAuInfoType(TdbAu au, String[] keys) {
    for (String key: keys) {
      for (AuInfoType type: AuInfoType.values()) {
        String s = findAuInfo(au, key, type);
        if (!StringUtils.isEmpty(s)) return type;
      }
    }
    return null;
  }


  /**
   * Construct a regex which will match a token, or a list of tokens separated by the 
   * separator, and produces match groups for the first and last instances of the token. 
   * Each instance of the token may be preceded by the specified padding, which is not 
   * included in the match results. 
   * <p>
   * The resulting regex matches the entire string, allowing for whitespace at each end.
   * 
   * 
   * @param token the main token to be group matched (usually matching one or more of a particular character set) 
   * @param separator a list separator (it's usually a good idea to add optional whitespace around this)
   * @param padding a prefix to the token which will be ignored (e.g. zero-padding)
   * @return a fully-constructed regex string
   */
  private static String constructRegex(String token, String separator, String padding) {
    return String.format("^\\s*%s(%s)(?:%s%s(%s))*\\s*$", padding, token, separator, padding, token);
  }

  /** Default year pattern matches a year or range of years. */
  private static final Pattern defaultYearPattern = 
    RegexpUtil.uncheckedCompile(constructRegex("\\d{4}", "\\s*-\\s*", "")); 

  /**
   * A pattern to match one or more numbers specified as a hyphenated range.
   * Any whitespace is ignored.
   */
  private static final Pattern numberRangePattern = 
    RegexpUtil.uncheckedCompile(constructRegex("\\d+", "\\s*-\\s*", "")); 
  
  /**
   * A pattern to match one or more (optionally zero-padded) numbers specified as a hyphenated range.
   * Any zero-padding and whitespace is ignored.
   */
  private static final Pattern numberRangeIgnoreZeroPaddingPattern = 
    RegexpUtil.uncheckedCompile(constructRegex("\\d+", "\\s*-\\s*", "0*")); 
  
  /**
   * A pattern to match one or more names specified as a comma-separated list.
   * Any whitespace padding around the names is ignored.
   * A name is a sequence of any combination of characters except for comma.
   */
  private static final Pattern nameListPattern = 
    RegexpUtil.uncheckedCompile(constructRegex("[^,]+", "\\s*,\\s*", "")); 

  /**
   * A pattern to match one or more numbers specified as a comma-separated list.
   * Any whitespace padding around the names is ignored.
   * <p>
   * Note that in most cases it might be preferable to use <code>nameListPattern</code>
   * for more flexible matching; sometimes letters do crop up in supposed numbers.
   * The elements of a number list do not tend to undergo any further processing so 
   * it doesn't really matter whether they are guaranteed to be numbers or are 
   * alphanumeric strings. 
   */
  private static final Pattern numberListPattern = 
    RegexpUtil.uncheckedCompile(constructRegex("\\d+", "\\s*,\\s*", "")); 


  /** 
   * Distinguishes between the three sources of AU information - attributes, parameters and properties.
   * Also provides a method to return a value for the type given an AU and a key.  
   * 
   */
  static enum AuInfoType {
    ATTR, PARAM, PROP;
    
    /**
     * Retrieves the correct map of properties from the AU for this issue format.
     * @param au the TdbAu 
     * @return the appropriate properties map, or null
     */
    Map<String,String> getMap(TdbAu au) {
      switch (this) {
      case ATTR:
	return au.getAttrs();
      case PARAM:
	return au.getParams();
      case PROP:
	return au.getProperties();
      default:
	return null;
      }
    }
    
    /**
     * Attempts to find a property value by searching for the given key in the AU's properties 
     * map indicated by the AuInfoType. 
     * 
     * @param au the TdbAu whose properties to search for an attribute
     * @param key the key name to find
     * @param type the type of property map to search
     * @return the value of an existing key, or an empty string
     */
    String findAuInfo(TdbAu au, String key) {
      return findMapValue(getMap(au), key);
    }

  }

  /** 
   * Enum which records the characteristics of different types of property in an AU which
   * provide information about its issues. That information may be found in different places in the AU 
   * (attributes map, parameters map, properties map), indexed with different key names, and
   * in different formats. This class encapsulates the details of those variations.
   * <p>
   * The formats are created in order from most to least likely.
   * Patterns are immutable and can be used by multiple threads.
   * <p>
   * Note that nondefparam parameter values do not make it into the Tdb records currently,
   * so we will not encounter those.
   */
  static enum IssueFormat {
   
    // nondefparam does not occur
    // [zero-padded]num[,num]*
    //ISSUE_FORMAT_1("nondefparam[issues]", defaultPattern, defaultLastPattern),
    
    // num[-num]
    ISSUE_FORMAT_2(AuInfoType.PARAM, "num_issue_range", numberRangePattern),

    // [comma-sep list of names (not parsable to numbers)]
    ISSUE_FORMAT_3(AuInfoType.PARAM, "issue_set", nameListPattern),
    
    // num (only in tdb/prod/european_organization_for_nuclear_research.tdb)
    ISSUE_FORMAT_4(AuInfoType.PARAM, "issue_no", numberRangePattern),

    // comma-sep strings (only in centro_de_filosofia_da_universidade_de_lisboa.xml)
    // usually zero-padded numbers, occasional letters - treat the tokens as alphabetic strings
    ISSUE_FORMAT_5(AuInfoType.PARAM, "issues", nameListPattern),

    // num[-num] (only in tdb/prod/university_of_toronto_department_of_french.tdb)
    ISSUE_FORMAT_6a(AuInfoType.PARAM, "issue_no.", numberRangePattern),
    
    // string (only in tdb/prod/university_of_toronto_department_of_french.tdb)
    ISSUE_FORMAT_6b(AuInfoType.PARAM, "issue_dir", nameListPattern);

    /** 
     * Each IssueFormat has a key which is the string used as an key in the TdbTitle properties 
     * map indicated by the type parameter, and a Pattern which should match the attribute's 
     * string value to yield first and last issue strings. The pattern <emph>must</emph> 
     * contain exactly two group matches.
     *
     * @param type an AuInfoType indicating in which map the issue is found
     * @param key the key to the issue field in the map
     * @param pattern a pattern with two bracketed groups which will match first and last issues in the property string 
     */
    IssueFormat(AuInfoType type, String key, Pattern pattern) {
      this.type = type;
      this.key = key;
      this.pattern = pattern;
    }

    /** The type of property, that is attribute, parameter or property. */
    private final AuInfoType type;
    /** The key to the property map. */
    private final String key;
    /** A pattern that will match the issue string to yield first and last issues. */
    private final Pattern pattern;

    String getKey() {
     return key; 
    }
    
    /**
     * Retrieve an issue string from the given AU using the rules of this format.
     * 
     * @param au the AU from which to extract an issue string
     * @return the issue string retrieved from the AU according to the rules of this format, or empty string
     */
    String getIssueString(TdbAu au) {
      //return findMapValue(type.getMap(au), this.key);
      return type.findAuInfo(au, this.key);
    }


    /**
     * Extract the issue string from the TdbAu and parse it to find the first issue.
     * 
     * @param au the TdbAu to extract issue from
     * @return the first issue specified in the issue string
     */
    String getFirstIssue(TdbAu au) {
      return extractFirstLastIssues(au)[0];
    }
    
    /**
     * Extract the issue string from the TdbAu and parse it to find the last issue.
     * 
     * @param au the TdbAu to extract issue from
     * @return the last issue specified in the issue string
     */
    String getLastIssue(TdbAu au) {
      return extractFirstLastIssues(au)[1];
    }

    /**
     * Extract the first and last issue parameters from the given AU using the information
     * encapsulated in this enum. Note that the AU supplied should be the same one, or of the
     * same format, as the one used to select this particular IssueFormat. If the pattern fails 
     * to match, the returned strings will be empty.
     * 
     * @param au the AU object from which to extract issue information
     * @return a string array with two entries, first and last issue
     */
    String[] extractFirstLastIssues(TdbAu au) {
      String[] res = new String[] {"", ""};
      Perl5Matcher matcher = RegexpUtil.getMatcher();
      // Match the pattern to the issue string and ensure there is at least one match
      if (matcher.contains(getIssueString(au), pattern) && matcher.getMatch().groups()>1) {
	MatchResult mr = matcher.getMatch();
	res[0] = mr.group(1).trim();
	// There should always be a second match, but check. If it is not null, use it,
	// otherwise the last date is the same as the first date.
	if (mr.groups() > 2) {	
	  String g = mr.group(mr.groups()-1);
	  res[1] = g==null ? res[0] : g.trim();
	}
      }
      return res;
    }
  }
  
}
