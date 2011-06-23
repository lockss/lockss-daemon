package org.lockss.config;

import java.util.Calendar;
import java.util.Properties;

import org.lockss.config.Tdb.TdbException;

/**
 * A utility class providing static methods for constructing Tdb
 * hierarchies for testing.
 * 
 * @author Neil Mayo
 *
 */
public class TdbTestUtil {

  private static int paramCount = 0;

  public static String DEFAULT_TITLE_ID = "a_non_issn_title_id";
  public static String DEFAULT_TITLE_ID_RANGE = "a_non_issn_title_id_for_range_title";
  public static String DEFAULT_PUBLISHER = "a_publisher";
  public static String DEFAULT_PLUGIN = "a_plugin";
  public static String DEFAULT_VOLUME = "vol1";
  public static String DEFAULT_WRONG_VOLUME = "This is not a real volume value!";
  public static String DEFAULT_YEAR = "2003";
  public static String DEFAULT_ISSUE = "8";
  
  public static String DEFAULT_ISSN_1 = "0001-0006";
  public static String DEFAULT_EISSN_1 = "0002-0001";
  
  public static String DEFAULT_ISSN_2 = "0010-0005";
  public static String DEFAULT_EISSN_2 = "0020-000X";
  
  public static String DEFAULT_ISSN_3 = "0100-0004";
  public static String DEFAULT_EISSN_3 = "0200-0008";
  
  public static String DEFAULT_ISSN_4 = "1000-0003";
  public static String DEFAULT_EISSN_4 = "2000-0006";

  public static String ISSN_INVALID_FORMAT_1 = "001-006";
  public static String ISSN_INVALID_FORMAT_2 = "0001=0006";
  public static String ISSN_INVALID_CHECK_DIGIT = "0001-0005";
  
  public static String DEFAULT_VOLUME_KEY = "volume";
  public static String DEFAULT_VOLUME_NAME_KEY = "volume_name";

  public static String DEFAULT_URL = "http://www.some.url.com/title";
  
  public static String RANGE_1_START = "2000";
  public static String RANGE_1_END = "2000";
  public static String RANGE_1_START_VOL = "1";
  public static String RANGE_1_END_VOL = "1";

  public static String RANGE_2_START = "2005";
  public static String RANGE_2_END = "2006";
  public static String RANGE_2_START_VOL = "2";
  public static String RANGE_2_END_VOL = "3";

  // Parameters for a journal which runs up to now and must therefore produce empty "last*" fields.
  // There is no coverage gap so only a single output title should be produced.
  // Note that the current date is assigned to a static variable; this should be fine unless the tests
  // are somehow run on a system that has been up for a while. Or over new year..
  public static String RANGE_TO_NOW_START = ""+(Calendar.getInstance().get(Calendar.YEAR) - 1);
  public static String RANGE_TO_NOW_END = ""+Calendar.getInstance().get(Calendar.YEAR);
  public static String RANGE_TO_NOW_START_VOL = "1";
  public static String RANGE_TO_NOW_END_VOL = "2";


  /**
   * Make a mock Tdb with some publisher/title/au structure, for testing
   * by other packages. The Tdb constructors are protected so cannot be 
   * used outside the package.
   *  
   * @throws TdbException
   */
  public static Tdb makeTestTdb() throws TdbException {
    Tdb tdb = new Tdb();

    // Create publisher
    TdbPublisher p1 = new TdbPublisher(DEFAULT_PUBLISHER);

    // --------------------------------------------
    // Create title with basic AU examples. The title has a non-ISSN id
    TdbTitle basicTitle = new TdbTitle("basicTitle", DEFAULT_TITLE_ID);
    p1.addTdbTitle(basicTitle);

    // AU with no properties 
    TdbAu emptyAu = new TdbAu("basicTitleEmptyAu", DEFAULT_PLUGIN+"1");
    basicTitle.addTdbAu(emptyAu);
    tdb.addTdbAu(emptyAu);
    
    // AU with basic properties 
    TdbAu au1p1 = createBasicAu("basicTitleAu", DEFAULT_PLUGIN+"2", DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    au1p1.setAttr("year", DEFAULT_YEAR);
    au1p1.setAttr(DEFAULT_VOLUME_KEY, DEFAULT_VOLUME);
    basicTitle.addTdbAu(au1p1);
    tdb.addTdbAu(au1p1);
    
    return tdb;
  }
  

  /**
   * Create and fill a title with ranged AUs, and add it to the supplied publisher.
   * Contains 1 title with 3 AUs, which should split into 2 title ranges for KBART.
   * 
   * @param withVols whether to include volume ranges or just years
   * @return a TdbTitle
   * @throws TdbException
   */
  public static TdbTitle makeRangeTestTitle(boolean withVols) throws TdbException {
    // --------------------------------------------
    // Create another title for testing ranges
    TdbTitle rangeTitle = new TdbTitle("rangeTitle", DEFAULT_TITLE_ID_RANGE);
    rangeTitle.setTdbPublisher(new TdbPublisher(DEFAULT_PUBLISHER));
    
    // AU range 1 (2000-2000, vol 1-1) 
    TdbAu rangeTitleAu1a = createBasicAu("rangeTitleAu1a", DEFAULT_PLUGIN+"1a", DEFAULT_ISSN_2, DEFAULT_EISSN_2);
    rangeTitleAu1a.setAttr("year", RANGE_1_START);
    if (withVols) rangeTitleAu1a.setAttr(DEFAULT_VOLUME_KEY, RANGE_1_START_VOL);
    rangeTitle.addTdbAu(rangeTitleAu1a);

    // AU range 2 (2005-2006, vol 2-3) - coverage gap for years, but not volumes 
    TdbAu rangeTitleAu2a = createBasicAu("rangeTitleAu2a", DEFAULT_PLUGIN+"2a", DEFAULT_ISSN_2, DEFAULT_EISSN_2);
    rangeTitleAu2a.setAttr("year", RANGE_2_START);
    if (withVols) rangeTitleAu2a.setAttr(DEFAULT_VOLUME_KEY, RANGE_2_START_VOL);
    rangeTitle.addTdbAu(rangeTitleAu2a);

    TdbAu rangeTitleAu2b = createBasicAu("rangeTitleAu2b", DEFAULT_PLUGIN+"2b", DEFAULT_ISSN_2, DEFAULT_EISSN_2);
    rangeTitleAu2b.setAttr("year", RANGE_2_END);
    if (withVols) rangeTitleAu2b.setAttr(DEFAULT_VOLUME_KEY, RANGE_2_END_VOL);
    rangeTitle.addTdbAu(rangeTitleAu2b); 

    return rangeTitle; 
  }
  
  
  public static TdbTitle makeRangeToNowTestTitle() throws TdbException {
    TdbTitle rangeTitle = new TdbTitle("rangeToNowTitle", DEFAULT_TITLE_ID_RANGE);
    rangeTitle.setTdbPublisher(new TdbPublisher(DEFAULT_PUBLISHER));

    // AU range (last year - this year, consecutive vols) 
    TdbAu rangeTitleAu3a = createBasicAu("rangeTitleAu3a", DEFAULT_PLUGIN+"3a", DEFAULT_ISSN_3, DEFAULT_EISSN_3);
    rangeTitleAu3a.setAttr("year", RANGE_TO_NOW_START);
    rangeTitleAu3a.setAttr(DEFAULT_VOLUME_KEY, RANGE_TO_NOW_START_VOL);
    rangeTitle.addTdbAu(rangeTitleAu3a);

    TdbAu rangeTitleAu3b = createBasicAu("rangeTitleAu3b", DEFAULT_PLUGIN+"3b", DEFAULT_ISSN_3, DEFAULT_EISSN_3);
    rangeTitleAu3b.setAttr("year", RANGE_TO_NOW_END);
    rangeTitleAu3b.setAttr(DEFAULT_VOLUME_KEY, RANGE_TO_NOW_END_VOL);
    rangeTitle.addTdbAu(rangeTitleAu3b); 
    
    return rangeTitle;
  }
  

  /**
   * Create a TdbTitle with a variety of volume parameters. This is only for volume-related testing; in full
   * use, if AUs identified by the same ISSN and title have different parameter keys, the export will fail.
   * Additionally, provides AUs with an incorrect volume value set against other keys, which should be ignored.  
   * 
   * @param vol the volume string to use in each AU
   * @return a new TdbTitle
   * @throws TdbException
   */
  public static TdbTitle makeVolumeTestTitle(String vol) throws TdbException {

    TdbTitle t1p1 = new TdbTitle("t1p1", DEFAULT_TITLE_ID);
    
    // Create AUs with basic properties
    TdbAu v1au1p1 = createBasicAu("v1au1p1", DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    TdbAu v2au1p1 = createBasicAu("v2au1p1", DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    TdbAu v3au1p1 = createBasicAu("v3au1p1", DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    TdbAu v4au1p1 = createBasicAu("v4au1p1", DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    TdbAu v5au1p1 = createBasicAu("v5au1p1", DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    
    // Set a different type of volume param in each
    v1au1p1.setAttr(DEFAULT_VOLUME_KEY, vol);
    v2au1p1.setParam(DEFAULT_VOLUME_KEY, vol);
    v3au1p1.setParam(DEFAULT_VOLUME_NAME_KEY, vol);
    // Set a combination of fields - first should be found
    v4au1p1.setAttr(DEFAULT_VOLUME_KEY, vol);
    v4au1p1.setParam(DEFAULT_VOLUME_KEY, DEFAULT_WRONG_VOLUME);
    v4au1p1.setParam(DEFAULT_VOLUME_NAME_KEY, DEFAULT_WRONG_VOLUME);
    // Set a combination of fields - first should be ignored
    v5au1p1.setAttr(DEFAULT_VOLUME_KEY, "");
    v5au1p1.setParam(DEFAULT_VOLUME_KEY, vol);
    v5au1p1.setParam(DEFAULT_VOLUME_NAME_KEY, DEFAULT_WRONG_VOLUME);
    
    // Add to title
    t1p1.addTdbAu(v1au1p1);
    t1p1.addTdbAu(v2au1p1);
    t1p1.addTdbAu(v3au1p1);
    t1p1.addTdbAu(v4au1p1);
    t1p1.addTdbAu(v5au1p1);
   
    return t1p1;
  }

  
  /**
   * Create a TdbTitle with default settings plus an issue mapped to by a particular key. This is only 
   * for volume-related testing; in full use, if AUs identified by the same ISSN and title have different 
   * parameter keys, the export will fail. Additionally, provides AUs with an incorrect volume value set 
   * against other keys, which should be ignored.  
   * 
   * @param key the name of the issue key
   * @param issue the value of the issue
   * @throws TdbException
   */
  public static TdbAu makeIssueTestAu(String key, String issue) throws TdbException {
    // Create AUs with basic properties
    TdbAu au = createBasicAu("au1", DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    au.setParam(key, issue);
    return au;
  }
  
  
  /**
   * Create a TdbAu with the given fundamental parameters; name, plugin, issn, eissn. 
   *   
   * @param name
   * @param plugin
   * @param issn
   * @param eissn
   * @return a newly-constructed TdbAu
   * @throws TdbException
   */
  public static TdbAu createBasicAu(String name, String plugin, String issn, String eissn) throws TdbException {
    TdbAu au = new TdbAu(name, plugin);
    au.setPropertyByName("issn", issn);
    //au.setAttr("eissn", eissn);
    au.setPropertyByName("eissn", eissn);
    return au;   
  }
  
  /**
   * Create a simple AU with default values for plugin, issn and eissn, plus the 
   * given values for name and year. If year is null it is not added.
   * 
   * @param name a name for the AU
   * @param year a year for the AU, or null
   * @return an AU
   * @throws TdbException 
   */
  public static TdbAu createBasicAu(String name, String year) throws TdbException {
    TdbAu au = createBasicAu(name, DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    if (year != null) au.setAttr("year", year);
    return au;
  }

  /**
   * Create a simple AU with default values for plugin, issn and eissn, plus the 
   * given values for name, year and volume. If year or volume is null it is not added.
   * 
   * @param name a name for the AU
   * @param year a year for the AU, or null
   * @param volume a volume for the AU, or null
   * @return an AU
   * @throws TdbException 
   */
  public static TdbAu createBasicAu(String name, String year, String volume) throws TdbException {
    TdbAu au = createBasicAu(name, DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    if (year != null) au.setAttr("year", year);
    if (volume != null) au.setAttr("volume", volume);
    return au;
  }

  /**
   * Set a parameter on an AU; needs to be done within the package.
   * 
   * @param au the TdbAu to set a parameter on
   * @param key the parameter key
   * @param val the parameter value
   * @throws TdbException
   */
  public static void setParam(TdbAu au, String key, String val) throws TdbException {
    au.setParam(key, val);
  }
  
  /**
   * Set parameter in a properties hash which may be used with <code>tdb.addTdbAuFromProperties</code>.
   * Parameters are added as param.n.key and param.n.value. This method
   * adds a given parameter key/value pair and returns the number used
   * for the parameter. 
   * 
   * @param p the Properties object to set a parameter on
   * @param key the key of the parameter
   * @param value the value of the parameter
   * @return the index of the parameter
   * @throws TdbException
   */
  public static int setParam(Properties p, String key, String value) throws TdbException {
    int n = ++paramCount;
    p.setProperty("param."+n+".key", key);
    p.setProperty("param."+n+".value", value);
    return n;
  }
 
  
}
