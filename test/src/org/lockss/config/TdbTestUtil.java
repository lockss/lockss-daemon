package org.lockss.config;

import java.util.List;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.Logger;
import org.lockss.config.Tdb.TdbException;
import org.lockss.test.ConfigurationUtil;

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

  public static String DEFAULT_ISBN_1 = "978-1-58562-257-3";

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
  public static String RANGE_2_END = "MMIIIIII"; // "2006" -- non-normalized
  public static String RANGE_2_START_VOL = "2";
  public static String RANGE_2_END_VOL = "III"; // 3

  // Parameters for a journal which runs up to now and must therefore produce
  // empty "last*" fields. There is no coverage gap so only a single output
  // title should be produced.
  // Note that the current date is assigned to a static variable; this should be
  // fine unless the tests are somehow run on a system that has been up for a
  // while. Or over new year..
  public static String RANGE_TO_NOW_START = ""+(Calendar.getInstance().get(Calendar.YEAR) - 1);
  public static String RANGE_TO_NOW_END = ""+Calendar.getInstance().get(Calendar.YEAR);
  public static String RANGE_TO_NOW_START_VOL = "1";
  public static String RANGE_TO_NOW_END_VOL = "2";


  /**
   * Set up the configuration to have some TDB structure. This is useful to
   * other tests so is made public. It uses makeTestTdb() to construct the test
   * TDB structure.
   * @throws Exception
   */
  public static final void setUpConfig() throws Exception {
    // Create a test tdb and set it in the current config
    ConfigurationUtil.setTdb(TdbTestUtil.makeTestTdb());
  }

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
    TdbAu au1p1 = createBasicAu("basicTitleAu",
        DEFAULT_PLUGIN+"2", DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    au1p1.setAttr("year", DEFAULT_YEAR);
    au1p1.setAttr(DEFAULT_VOLUME_KEY, DEFAULT_VOLUME);
    basicTitle.addTdbAu(au1p1);
    tdb.addTdbAu(au1p1);

    return tdb;
  }

  /**
   * Create a simple title with a publisher but no TdbAus, using the given id.
   * @param id
   * @return
   * @throws TdbException
   */
  public static TdbTitle makeTitleWithNoAus(String id) throws TdbException {
    TdbTitle title = new TdbTitle("TdbTitle", id);
    TdbPublisher p1 = new TdbPublisher(DEFAULT_PUBLISHER);
    p1.addTdbTitle(title);
    return title;
  }

  /**
   * Create and fill a title with ranged AUs, and add it to the supplied publisher.
   * Contains 1 title with 3 AUs, which should remain as a single range due to a
   * consistent volume ordering.
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
    TdbAu rangeTitleAu1a = createBasicAu("rangeTitleAu1a",
        DEFAULT_PLUGIN+"1a", DEFAULT_ISSN_2, DEFAULT_EISSN_2);
    rangeTitleAu1a.setAttr("year", RANGE_1_START);
    if (withVols) rangeTitleAu1a.setAttr(DEFAULT_VOLUME_KEY, RANGE_1_START_VOL);
    rangeTitle.addTdbAu(rangeTitleAu1a);

    // AU range 2 (2005-2006, vol 2-3) - coverage gap for years, but not volumes
    TdbAu rangeTitleAu2a = createBasicAu("rangeTitleAu2a",
        DEFAULT_PLUGIN+"2a", DEFAULT_ISSN_2, DEFAULT_EISSN_2);
    rangeTitleAu2a.setAttr("year", RANGE_2_START);
    if (withVols) rangeTitleAu2a.setAttr(DEFAULT_VOLUME_KEY, RANGE_2_START_VOL);
    rangeTitle.addTdbAu(rangeTitleAu2a);

    TdbAu rangeTitleAu2b = createBasicAu("rangeTitleAu2b",
        DEFAULT_PLUGIN+"2b", DEFAULT_ISSN_2, DEFAULT_EISSN_2);
    rangeTitleAu2b.setAttr("year", RANGE_2_END);
    if (withVols) rangeTitleAu2b.setAttr(DEFAULT_VOLUME_KEY, RANGE_2_END_VOL);
    rangeTitle.addTdbAu(rangeTitleAu2b);
    return rangeTitle;
  }


  public static TdbTitle makeRangeToNowTestTitle() throws TdbException {
    TdbTitle rangeTitle = new TdbTitle("rangeToNowTitle", DEFAULT_TITLE_ID_RANGE);
    rangeTitle.setTdbPublisher(new TdbPublisher(DEFAULT_PUBLISHER));

    // AU range (last year - this year, consecutive vols)
    TdbAu rangeTitleAu3a = createBasicAu("rangeTitleAu3a", DEFAULT_PLUGIN+"3a",
        DEFAULT_ISSN_3, DEFAULT_EISSN_3);
    rangeTitleAu3a.setAttr("year", RANGE_TO_NOW_START);
    rangeTitleAu3a.setAttr(DEFAULT_VOLUME_KEY, RANGE_TO_NOW_START_VOL);
    rangeTitle.addTdbAu(rangeTitleAu3a);

    TdbAu rangeTitleAu3b = createBasicAu("rangeTitleAu3b", DEFAULT_PLUGIN+"3b",
        DEFAULT_ISSN_3, DEFAULT_EISSN_3);
    rangeTitleAu3b.setAttr("year", RANGE_TO_NOW_END);
    rangeTitleAu3b.setAttr(DEFAULT_VOLUME_KEY, RANGE_TO_NOW_END_VOL);
    rangeTitle.addTdbAu(rangeTitleAu3b);
    return rangeTitle;
  }


  /**
   * Create a TdbTitle with a variety of volume parameters. This is only for
   * volume-related testing; in full use, if AUs identified by the same ISSN and
   * title have different parameter keys, the export will fail. Additionally,
   * provides AUs with an incorrect volume value set against other keys, which
   * should be ignored.
   *
   * @param vol the volume string to use in each AU
   * @return a new TdbTitle
   * @throws TdbException
   */
  public static TdbTitle makeVolumeTestTitle(String vol) throws TdbException {

    TdbTitle t1p1 = new TdbTitle("t1p1", DEFAULT_TITLE_ID);
    t1p1.setTdbPublisher(new TdbPublisher(DEFAULT_PUBLISHER));

    // Create AUs with basic properties
    TdbAu v1au1p1 = createBasicAu("v1au1p1",
        DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    TdbAu v2au1p1 = createBasicAu("v2au1p1",
        DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    TdbAu v3au1p1 = createBasicAu("v3au1p1",
        DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    TdbAu v4au1p1 = createBasicAu("v4au1p1",
        DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
    TdbAu v5au1p1 = createBasicAu("v5au1p1",
        DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);

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
   * Create a TdbTitle with a variety of year attributes. This is only for
   * volume-related testing; in full use, if AUs identified by the same ISSN and
   * title have different parameter keys, the export will fail. Additionally,
   * provides AUs with an incorrect volume value set against other keys, which
   * should be ignored.
   *
   * @param years a list of year strings
   * @return a new TdbTitle
   * @throws TdbException
   */
  public static TdbTitle makeYearTestTitle(String ... years)
      throws TdbException {

    TdbTitle t1p1 = new TdbTitle("t1p1", DEFAULT_TITLE_ID);
    t1p1.setTdbPublisher(new TdbPublisher(DEFAULT_PUBLISHER));

    for (int i = 0; i < years.length; i++) {
      // Create AUs with basic properties and different year
      TdbAu au = createBasicAu("v"+i+"au1p1", DEFAULT_PLUGIN, DEFAULT_ISSN_1, DEFAULT_EISSN_1);
      au.setParam("year", years[i]);
      t1p1.addTdbAu(au);
    }
    return t1p1;
  }


  /**
   * Create a TdbTitle with a variety of year attributes, holding book TdbAus,
   * that is with an ISBN and a type of "book". This is only for volume-related
   * testing; in full use, if AUs identified by the same ISSN and
   * title have different parameter keys, the export will fail. Additionally,
   * provides AUs with an incorrect volume value set against other keys, which
   * should be ignored.
   *
   * @param years a list of year strings
   * @return a new TdbTitle
   * @throws TdbException
   */
  public static TdbTitle makeBookTestTitle(String id, String ... years) throws TdbException {
    TdbTitle bk1 = new TdbTitle("book "+id, DEFAULT_TITLE_ID);
    bk1.setTdbPublisher(new TdbPublisher(DEFAULT_PUBLISHER));
    for (int i = 0; i < years.length; i++) {
      // Create AUs with basic properties and different year
      TdbAu au = createBasicAu("book "+id+":"+i, DEFAULT_PLUGIN);
      au.setAttr("isbn", DEFAULT_ISBN_1);
      au.setParam("year", years[i]);
      au.setPropertyByName("type", "book");
      bk1.addTdbAu(au);
    }
    return bk1;
  }

  /**
   * Create a book series TdbTitle with a variety of year attributes, holding
   * book TdbAus,that is with an ISBN and a type of "book", and also an ISSN
   * reflecting the series. This is only for volume-related
   * testing; in full use, if AUs identified by the same ISSN and
   * title have different parameter keys, the export will fail. Additionally,
   * provides AUs with an incorrect volume value set against other keys, which
   * should be ignored.
   *
   * @param years a list of year strings
   * @return a new TdbTitle
   * @throws TdbException
   */
  public static TdbTitle makeBookSeriesTestTitle(String id, String ... years) throws TdbException {
    TdbTitle bk1 = new TdbTitle("bookSeries "+id, DEFAULT_TITLE_ID);
    bk1.setTdbPublisher(new TdbPublisher(DEFAULT_PUBLISHER));
    for (int i = 0; i < years.length; i++) {
      // Create AUs with basic properties and different year
      TdbAu au = createBasicAu("bookSeries "+id+":"+i, DEFAULT_PLUGIN);
      au.setAttr("isbn", DEFAULT_ISBN_1);
      au.setAttr("issn", DEFAULT_ISSN_1);
      au.setParam("year", years[i]);
      au.setPropertyByName("type", "bookSeries");
      bk1.addTdbAu(au);
    }
    return bk1;
  }


  /**
   * Create a TdbAu with default settings plus an issue mapped to by a
   * particular key. This is only for volume-related testing; in full use,
   * if AUs identified by the same ISSN and title have different parameter keys,
   * the export will fail. Additionally, provides AUs with an incorrect volume
   * value set against other keys, which should be ignored.
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
   * Create a TdbAu with the given fundamental parameters; name, plugin, issn,
   * eissn.
   *
   * @param name
   * @param plugin
   * @param issn
   * @param eissn
   * @return a newly-constructed TdbAu
   * @throws TdbException
   */
  public static TdbAu createBasicAu(String name, String plugin,
                                    String issn, String eissn)
      throws TdbException {
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
   * Create a simple AU with default values for plugin, issn and
   * eissn, plus the given values for name, year and volume. If year or volume
   * is null it is not added.
   *
   * @param name a name for the AU
   * @param year a year for the AU, or null
   * @param volume a volume for the AU, or null
   * @return a TdbAu
   * @throws TdbException
   */
  public static TdbAu createBasicAu(String name, String year, String volume)
      throws TdbException {
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
  public static void setParam(TdbAu au, String key, String val)
      throws TdbException {
    au.setParam(key, val);
  }

  /**
   * Set parameter in a properties hash which may be used with
   * <code>tdb.addTdbAuFromProperties</code>.
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
  public static int setParam(Properties p, String key, String value)
      throws TdbException {
    int n = ++paramCount;
    p.setProperty("param."+n+".key", key);
    p.setProperty("param."+n+".value", value);
    return n;
  }

  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------

  /*
   ---------------------------------------------------------------------------
   Real-world example issues
   ---------------------------------------------------------------------------

   ---------------------------------------------------------------------------
   (1) Mixed volume identifier formats

   au < manifest ; 2007 ; T'ang Studies Volume 2007 ; 2007 >
   au < manifest ; 2008 ; T'ang Studies Volume 2008 ; 2008 >
   au < manifest ; 2009 ; T'ang Studies Volume 27 ; 27 >
   au < manifest ; 2010 ; T'ang Studies Volume 2010 ; 2010 >

   Order by year.

   ---------------------------------------------------------------------------
   au < HighWirePressH20Plugin ; exists ; 1942 ; Oxford Economic Papers Volume os-6 ; os-6 >
   au < HighWirePressH20Plugin ; exists ; 1945 ; Oxford Economic Papers Volume os-7 ; os-7 >
   au < HighWirePressH20Plugin ; exists ; 1948 ; Oxford Economic Papers Volume os-8 ; os-8 >
   au < HighWirePressH20Plugin ; exists ; 1949 ; Oxford Economic Papers Volume 1 ; 1 >
   au < HighWirePressH20Plugin ; exists ; 1950 ; Oxford Economic Papers Volume 2 ; 2 >
   au < HighWirePressH20Plugin ; exists ; 1951 ; Oxford Economic Papers Volume 3 ; 3 >

   Order by volume and produce 2 separate runs. Note that ordering by year would produce
   several runs for the string-based volume, and a single run from 1948 with mixed volume
   identifier formats.

   ---------------------------------------------------------------------------
   (2) Volume numbers that reset or change (e.g. btw year and sequence number)
       - must consider the year to be more authoritative.

   au < released ; 1997 ; European Business Review Volume 97 ; 97 >
   au < released ; 1998 ; European Business Review Volume 98 ; 98 >
   au < released ; 1999 ; European Business Review Volume 99 ; 99 >
   au < released ; 2000 ; European Business Review Volume 12 ; 12 >
   au < released ; 2001 ; European Business Review Volume 13 ; 13 >
   au < released ; 2002 ; European Business Review Volume 14 ; 14 >

   au < released ; 1997 ; Nutrition & Food Science Volume 97 ; 97 >
   au < released ; 1998 ; Nutrition & Food Science Volume 98 ; 98 >
   au < released ; 1999 ; Nutrition & Food Science Volume 99 ; 99 >
   au < released ; 2000 ; Nutrition & Food Science Volume 30 ; 30 >
   au < released ; 2001 ; Nutrition & Food Science Volume 31 ; 31 >
   au < released ; 2002 ; Nutrition & Food Science Volume 32 ; 32 >

   Order by year in both cases.

   ---------------------------------------------------------------------------
   au < ready ; 1994 ; International Journal of Humanities and Arts Computing Volume 6 (1994) ; 6 >
   au < ready ; 1995 ; International Journal of Humanities and Arts Computing Volume 7 (1995) ; 7 >
   au < ready ; 1996 ; International Journal of Humanities and Arts Computing Volume 8 (1996) ; 8 >
   au < ready ; 1997 ; International Journal of Humanities and Arts Computing Volume 9 (1997) ; 9 >
   au < ready ; 1998 ; International Journal of Humanities and Arts Computing Volume 10 (1998) ; 10 >
   au < ready ; 1999 ; International Journal of Humanities and Arts Computing Volume 11 (1999) ; 11 >
   au < ready ; 2000 ; International Journal of Humanities and Arts Computing Volume 12 (2000) ; 12 >
   au < ready ; 2001 ; International Journal of Humanities and Arts Computing Volume 13 (2001) ; 13 >
   au < ready ; 2002 ; International Journal of Humanities and Arts Computing Volume 14 (2002) ; 14 >
   au < ready ; 2007 ; International Journal of Humanities and Arts Computing Volume 1 (2007) ; 1 >
   au < ready ; 2008 ; International Journal of Humanities and Arts Computing Volume 2 (2008) ; 2 >
   au < ready ; 2009 ; International Journal of Humanities and Arts Computing Volume 3 (2009) ; 3 >
   au < ready ; 2010 ; International Journal of Humanities and Arts Computing Volume 4 (2010) ; 4 >
   au < manifest ; 2011 ; International Journal of Humanities and Arts Computing Volume 5 (2011) ; 5 >

   Produce 2 separate runs.
   Looks like full run vols 1-14 when ordered, but years are clearly wrong.
   The years should be authoritative.

   ---------------------------------------------------------------------------
   (3) Inconsistent years - should order by vol in each case

   au < released ; 1994 ; Experimental Astronomy Volume 3 ; 3 >
   au < released ; 1993-1994 ; Experimental Astronomy Volume 4 ; 4 >
   au < released ; 1994 ; Experimental Astronomy Volume 5 ; 5 >

   au < ready ; 1975 ; Fresenius Zeitschrift für Analytische Chemie Volume 275 ; 275 >
   au < ready ; 1972-1975 ; Fresenius Zeitschrift für Analytische Chemie Volume 276 ; 276 >
   au < ready ; 1975 ; Fresenius Zeitschrift für Analytische Chemie Volume 277 ; 277 >

   au < HighWirePressH20Plugin ; released ; 1960 ; Journal of Endocrinology Volume 20 ; 20 >
   au < HighWirePressH20Plugin ; released ; 1960-1961 ; Journal of Endocrinology Volume 21 ; 21 >
   au < HighWirePressH20Plugin ; released ; 1962 ; Journal of Endocrinology Volume 22 ; 22 >
   au < HighWirePressH20Plugin ; released ; 1961-1962 ; Journal of Endocrinology Volume 23 ; 23 >
   au < HighWirePressH20Plugin ; released ; 1962 ; Journal of Endocrinology Volume 24 ; 24 >

   au < released ; 1882-1884 ; Proceedings of the Yorkshire Geological Society Volume 8 ; 8 >
   au < released ; 1885-1887 ; Proceedings of the Yorkshire Geological Society Volume 9 ; 9 >
   au < released ; 1889 ; Proceedings of the Yorkshire Geological Society Volume 10 ; 10 >
   au < released ; 1888-1890 ; Proceedings of the Yorkshire Geological Society Volume 11 ; 11 >
   au < released ; 1891-1894 ; Proceedings of the Yorkshire Geological Society Volume 12 ; 12 >

   au < manifest ; 1988-1989 ; Communication Disorders Quarterly Volume 12 ; 12 >
   au < manifest ; 1990 ; Communication Disorders Quarterly Volume 13 ; 13 >
   au < manifest ; 1988-1992 ; Communication Disorders Quarterly Volume 14 ; 14 >

   For sorting and deciding coverage gaps, the volume is more authoritative in these cases.
   To see this we need to be able to acknowledge that the year sequence which results from
   sorting by volume is appropriately consistent - that is, it does not necessarily indicate
   that the volume ordering is wrong.

   ---------------------------------------------------------------------------
   au < down ; 1992 ; Geological Society of London Memoirs Volume 13 ; 13 >
   au < down ; 1991 ; Geological Society of London Memoirs Volume 14 ; 14 >
   au < down ; 1994 ; Geological Society of London Memoirs Volume 15 ; 15 >

   au < down ; 2007 ; Geological Society of London Special Publications Volume 287 ; 287 >
   au < down ; 2008 ; Geological Society of London Special Publications Volume 288 ; 288 >
   au < down ; 2007 ; Geological Society of London Special Publications Volume 289 ; 289 >

   The start of A is after the entire range of B.
   The year ordering should get a lower score because it will exhibit redundancy and gaps,
   whereas the volume ordering is perfect.

   ---------------------------------------------------------------------------
   (4) Fully consistent sequence with mutually-consistent ordering and no
       redundancy or breaks in either year or volume

   au < HighWirePressH20Plugin ; exists ; 1986 ; Literary and Linguistic Computing Volume 1 ; 1 >
   au < HighWirePressH20Plugin ; exists ; 1987 ; Literary and Linguistic Computing Volume 2 ; 2 >
   au < HighWirePressH20Plugin ; exists ; 1988 ; Literary and Linguistic Computing Volume 3 ; 3 >
   au < HighWirePressH20Plugin ; exists ; 1989 ; Literary and Linguistic Computing Volume 4 ; 4 >
   au < HighWirePressH20Plugin ; exists ; 1990 ; Literary and Linguistic Computing Volume 5 ; 5 >
   au < HighWirePressH20Plugin ; exists ; 1991 ; Literary and Linguistic Computing Volume 6 ; 6 >
   au < HighWirePressH20Plugin ; exists ; 1992 ; Literary and Linguistic Computing Volume 7 ; 7 >
   au < HighWirePressH20Plugin ; exists ; 1993 ; Literary and Linguistic Computing Volume 8 ; 8 >
   au < HighWirePressH20Plugin ; exists ; 1994 ; Literary and Linguistic Computing Volume 9 ; 9 >
   au < HighWirePressH20Plugin ; exists ; 1995 ; Literary and Linguistic Computing Volume 10 ; 10 >

   ---------------------------------------------------------------------------
   (5) Sequence with interleaved duplicate AUs (identical in terms of main fields).
       This occurs for example when there are 2 sets of AUs, one down, one released
       and current. These AUs otherwise display a full and consistent ordering.

   au < down ; 1999 ; Africa Today Volume 46 ; 46 >
   au < released ; 1999 ; Africa Today Volume 46 ; 46 >
   au < down ; 2000 ; Africa Today Volume 47 ; 47 >
   au < released ; 2000 ; Africa Today Volume 47 ; 47 >
   au < down ; 2001 ; Africa Today Volume 48 ; 48 >
   au < released ; 2001 ; Africa Today Volume 48 ; 48 >
   au < down ; 2002-2003 ; Africa Today Volume 49 ; 49 >
   au < released ; 2002-2003 ; Africa Today Volume 49 ; 49 >
   au < down ; 2003-2004 ; Africa Today Volume 50 ; 50 >
   au < released ; 2003-2004 ; Africa Today Volume 50 ; 50 >

   ---------------------------------------------------------------------------
   (6) Sequence with duplicate volumes, presumably representing a single volume
       published across several years.

   au < released ; Texture, Stress, and Microstructure Volume 1 (1972) ; 1972 ; 1 ; 1972 >
   au < released ; Texture, Stress, and Microstructure Volume 1 (1974) ; 1974 ; 1 ; 1974 >
   au < released ; Texture, Stress, and Microstructure Volume 2 (1975) ; 1975 ; 2 ; 1975 >
   au < released ; Texture, Stress, and Microstructure Volume 2 (1976) ; 1976 ; 2 ; 1976 >
   au < released ; Texture, Stress, and Microstructure Volume 2 (1977) ; 1977 ; 2 ; 1977 >
   au < released ; Texture, Stress, and Microstructure Volume 3 (1978) ; 1978 ; 3 ; 1978 >
   au < released ; Texture, Stress, and Microstructure Volume 3 (1979) ; 1979 ; 3 ; 1979 >
   au < released ; Texture, Stress, and Microstructure Volume 4 (1980) ; 1980 ; 4 ; 1980 >
   au < released ; Texture, Stress, and Microstructure Volume 4 (1981) ; 1981 ; 4 ; 1981 >

   ---------------------------------------------------------------------------
  */

  // A list of AUs whose years and volumes are very well behaved. The volume
  // ordering should equal the year ordering, and both should be equal to
  // the original ordering. There should be no coverage gaps, resulting in a
  // single range which is also equal to the original list.
  //public static List<TdbAu> getFullyConsistentAus() { }

  // Shorten references to sort fields
  //private static final TdbAuOrderScorer.SORT_FIELD YR  = TdbAuOrderScorer.SORT_FIELD.YEAR;
  //private static final TdbAuOrderScorer.SORT_FIELD VOL =  TdbAuOrderScorer.SORT_FIELD.VOLUME;

/*
    // Init all the lists
    problemTitles = new ArrayList<List<TdbAu>>();
    consistentSequences = new ArrayList<List<TdbAu>>();
    allCanonicalLists = new ArrayList<List<TdbAu>>();
    allVolRanges = new ArrayList<List<TitleRange>>();
    allYearRanges = new ArrayList<List<TitleRange>>();
    titlesToOrderByVolume = new ArrayList<Integer>();
    titlesToOrderByYear = new ArrayList<Integer>();

    try {
      // ----------------------------------------------------------------------
      TdbAu tang1 = TdbTestUtil.createBasicAu("T'ang Studies Volume 2007", "2007", "2007");
      TdbAu tang2 = TdbTestUtil.createBasicAu("T'ang Studies Volume 2008", "2008", "2008");
      TdbAu tang3 = TdbTestUtil.createBasicAu("T'ang Studies Volume 27",   "2009", "27");
      TdbAu tang4 = TdbTestUtil.createBasicAu("T'ang Studies Volume 2010", "2010", "2010");
      tang = Arrays.asList(tang1, tang2, tang3, tang4);
      tangYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(tang1, tang2, tang3, tang4))
      );
      tangVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(tang3)),
          new TitleRange(Arrays.asList(tang1, tang2)),
          new TitleRange(Arrays.asList(tang4))
      );

      // ----------------------------------------------------------------------
      TdbAu oxEcPap1 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume os-6", "1942", "os-6");
      TdbAu oxEcPap2 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume os-7", "1945", "os-7");
      TdbAu oxEcPap3 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume os-8", "1948", "os-8");
      TdbAu oxEcPap4 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume 1",    "1949", "1");
      TdbAu oxEcPap5 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume 2",    "1950", "2");
      TdbAu oxEcPap6 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume 3",    "1951", "3");
      oxEcPap = Arrays.asList(oxEcPap1, oxEcPap2, oxEcPap3, oxEcPap4, oxEcPap5, oxEcPap6);
      oxEcPapYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(oxEcPap1)),
          new TitleRange(Arrays.asList(oxEcPap2)),
          new TitleRange(Arrays.asList(oxEcPap3, oxEcPap4, oxEcPap5))
      );
      oxEcPapVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(oxEcPap1, oxEcPap2, oxEcPap3)),
          new TitleRange(Arrays.asList(oxEcPap4, oxEcPap5, oxEcPap6))
      );

      // ----------------------------------------------------------------------
      TdbAu euroBusRev1 = TdbTestUtil.createBasicAu("European Business Review Volume 97", "1997", "97");
      TdbAu euroBusRev2 = TdbTestUtil.createBasicAu("European Business Review Volume 98", "1998", "98");
      TdbAu euroBusRev3 = TdbTestUtil.createBasicAu("European Business Review Volume 99", "1999", "99");
      TdbAu euroBusRev4 = TdbTestUtil.createBasicAu("European Business Review Volume 12", "2000", "12");
      TdbAu euroBusRev5 = TdbTestUtil.createBasicAu("European Business Review Volume 13", "2001", "13");
      TdbAu euroBusRev6 = TdbTestUtil.createBasicAu("European Business Review Volume 14", "2002", "14");
      euroBusRev = Arrays.asList(euroBusRev1, euroBusRev2, euroBusRev3, euroBusRev4, euroBusRev5, euroBusRev6);
      euroBusRevYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(euroBusRev1, euroBusRev2, euroBusRev3, euroBusRev4, euroBusRev5, euroBusRev6))
      );
      euroBusRevVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(euroBusRev1, euroBusRev2, euroBusRev3)),
          new TitleRange(Arrays.asList(euroBusRev4, euroBusRev5, euroBusRev6))
      );

      // ----------------------------------------------------------------------
      TdbAu nutFoodSci1 = TdbTestUtil.createBasicAu("Nutrition & Food Science 97", "1997", "97");
      TdbAu nutFoodSci2 = TdbTestUtil.createBasicAu("Nutrition & Food Science 98", "1998", "98");
      TdbAu nutFoodSci3 = TdbTestUtil.createBasicAu("Nutrition & Food Science 99", "1999", "99");
      TdbAu nutFoodSci4 = TdbTestUtil.createBasicAu("Nutrition & Food Science 30", "2000", "30");
      TdbAu nutFoodSci5 = TdbTestUtil.createBasicAu("Nutrition & Food Science 31", "2001", "31");
      TdbAu nutFoodSci6 = TdbTestUtil.createBasicAu("Nutrition & Food Science 32", "2002", "32");
      nutFoodSci = Arrays.asList(nutFoodSci1, nutFoodSci2, nutFoodSci3, nutFoodSci4, nutFoodSci5, nutFoodSci6);
      nutFoodSciYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(nutFoodSci1, nutFoodSci2, nutFoodSci3, nutFoodSci4, nutFoodSci5, nutFoodSci6))
      );
      nutFoodSciVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(nutFoodSci1, nutFoodSci2, nutFoodSci3)),
          new TitleRange(Arrays.asList(nutFoodSci4, nutFoodSci5, nutFoodSci6))
      );

      // ----------------------------------------------------------------------
      TdbAu intlJournHumArtsComp1  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 6 (1994)",  "1994", "6");
      TdbAu intlJournHumArtsComp2  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 7 (1995)",  "1995", "7");
      TdbAu intlJournHumArtsComp3  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 8 (1996)",  "1996", "8");
      TdbAu intlJournHumArtsComp4  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 9 (1997)",  "1997", "9");
      TdbAu intlJournHumArtsComp5  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 10 (1998)", "1998", "10");
      TdbAu intlJournHumArtsComp6  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 11 (1999)", "1999", "11");
      TdbAu intlJournHumArtsComp7  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 12 (2000)", "2000", "12");
      TdbAu intlJournHumArtsComp8  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 13 (2001)", "2001", "13");
      TdbAu intlJournHumArtsComp9  = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 14 (2002)", "2002", "14");
      TdbAu intlJournHumArtsComp10 = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 1 (2007)",  "2007", "1");
      TdbAu intlJournHumArtsComp11 = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 2 (2008)",  "2008", "2");
      TdbAu intlJournHumArtsComp12 = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 3 (2009)",  "2009", "3");
      TdbAu intlJournHumArtsComp13 = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 4 (2010)",  "2010", "4");
      TdbAu intlJournHumArtsComp14 = TdbTestUtil.createBasicAu("International Journal of Humanities and Arts Computing Volume 5 (2011)",  "2011", "5");
      intlJournHumArtsComp = Arrays.asList(intlJournHumArtsComp1, intlJournHumArtsComp2, intlJournHumArtsComp3, intlJournHumArtsComp4,
          intlJournHumArtsComp5, intlJournHumArtsComp6, intlJournHumArtsComp7, intlJournHumArtsComp8, intlJournHumArtsComp9,
          intlJournHumArtsComp10, intlJournHumArtsComp11, intlJournHumArtsComp12, intlJournHumArtsComp13, intlJournHumArtsComp14
      );
      intlJournHumArtsCompYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(intlJournHumArtsComp1, intlJournHumArtsComp2,
              intlJournHumArtsComp3, intlJournHumArtsComp4, intlJournHumArtsComp5,
              intlJournHumArtsComp6, intlJournHumArtsComp7, intlJournHumArtsComp8,
              intlJournHumArtsComp9)
          ),
          new TitleRange(Arrays.asList(intlJournHumArtsComp10, intlJournHumArtsComp11,
              intlJournHumArtsComp12, intlJournHumArtsComp13, intlJournHumArtsComp14)
          )
      );
      intlJournHumArtsCompVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(intlJournHumArtsComp10, intlJournHumArtsComp11,
              intlJournHumArtsComp12, intlJournHumArtsComp13, intlJournHumArtsComp14,
              intlJournHumArtsComp1, intlJournHumArtsComp2, intlJournHumArtsComp3,
              intlJournHumArtsComp4, intlJournHumArtsComp5, intlJournHumArtsComp6,
              intlJournHumArtsComp7, intlJournHumArtsComp8, intlJournHumArtsComp9)
          )
      );

      // ----------------------------------------------------------------------
      TdbAu expAstr1 = TdbTestUtil.createBasicAu("Experimental Astronomy Volume 3", "1994",      "3");
      TdbAu expAstr2 = TdbTestUtil.createBasicAu("Experimental Astronomy Volume 4", "1993-1994", "4");
      TdbAu expAstr3 = TdbTestUtil.createBasicAu("Experimental Astronomy Volume 5", "1994",      "5");
      expAstr = Arrays.asList(expAstr1, expAstr2, expAstr3);
      expAstrYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(expAstr2, expAstr1, expAstr3))
      );
      expAstrVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(expAstr1, expAstr2, expAstr3))
      );

      // ----------------------------------------------------------------------
      TdbAu analChem1 = TdbTestUtil.createBasicAu("Fresenius Zeitschrift für Analytische Chemie Volume 275", "1975",      "275");
      TdbAu analChem2 = TdbTestUtil.createBasicAu("Fresenius Zeitschrift für Analytische Chemie Volume 276", "1972-1975", "276");
      TdbAu analChem3 = TdbTestUtil.createBasicAu("Fresenius Zeitschrift für Analytische Chemie Volume 277", "1975",      "277");
      analChem = Arrays.asList(analChem1, analChem2, analChem3);
      analChemYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(analChem2, analChem1, analChem3))
      );
      analChemVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(analChem1, analChem2, analChem3))
      );

      // ----------------------------------------------------------------------
      TdbAu journEndoc1 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 20", "1960",      "20");
      TdbAu journEndoc2 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 21", "1960-1961", "21");
      TdbAu journEndoc3 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 22", "1962",      "22");
      TdbAu journEndoc4 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 23", "1961-1962", "23");
      TdbAu journEndoc5 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 24", "1962",      "24");
      journEndoc = Arrays.asList(journEndoc1, journEndoc2, journEndoc3, journEndoc4, journEndoc5);
      journEndocYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(journEndoc1, journEndoc2, journEndoc4, journEndoc3, journEndoc5))
      );
      journEndocVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(journEndoc1, journEndoc2, journEndoc3, journEndoc4, journEndoc5))
      );

      // ----------------------------------------------------------------------
      TdbAu yorkGeoSoc1 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 8",  "1882-1884", "8");
      TdbAu yorkGeoSoc2 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 9",  "1885-1887", "9");
      TdbAu yorkGeoSoc3 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 10", "1889",      "10");
      TdbAu yorkGeoSoc4 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 11", "1888-1890", "11");
      TdbAu yorkGeoSoc5 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 12", "1891-1894", "12");
      yorkGeoSoc = Arrays.asList(yorkGeoSoc1, yorkGeoSoc2, yorkGeoSoc3, yorkGeoSoc4, yorkGeoSoc5);
      yorkGeoSocYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(yorkGeoSoc1, yorkGeoSoc2)),
          new TitleRange(Arrays.asList(yorkGeoSoc4, yorkGeoSoc3)),
          new TitleRange(Arrays.asList(yorkGeoSoc5))
      );
      yorkGeoSocVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(yorkGeoSoc1, yorkGeoSoc2, yorkGeoSoc3, yorkGeoSoc4, yorkGeoSoc5))
      );

      // ----------------------------------------------------------------------
      TdbAu commDis1 = TdbTestUtil.createBasicAu("Communication Disorders Quarterly Volume 12", "1988-1989", "12");
      TdbAu commDis2 = TdbTestUtil.createBasicAu("Communication Disorders Quarterly Volume 13", "1990",      "13");
      TdbAu commDis3 = TdbTestUtil.createBasicAu("Communication Disorders Quarterly Volume 14", "1988-1992", "14");
      commDis = Arrays.asList(commDis1, commDis2, commDis3);
      commDisYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(commDis1, commDis3, commDis2))
      );
      commDisVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(commDis1, commDis2, commDis3))
      );

      // ----------------------------------------------------------------------
      TdbAu geoSocLonMem1 = TdbTestUtil.createBasicAu("Geological Society of London Memoirs Volume 13", "1992", "13");
      TdbAu geoSocLonMem2 = TdbTestUtil.createBasicAu("Geological Society of London Memoirs Volume 14", "1991", "14");
      TdbAu geoSocLonMem3 = TdbTestUtil.createBasicAu("Geological Society of London Memoirs Volume 15", "1994", "15");
      geoSocLonMem = Arrays.asList(geoSocLonMem1, geoSocLonMem2, geoSocLonMem3);
      geoSocLonMemYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(geoSocLonMem2, geoSocLonMem1)),
          new TitleRange(Arrays.asList(geoSocLonMem3))
      );
      geoSocLonMemVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(geoSocLonMem1, geoSocLonMem2, geoSocLonMem3))
      );

      // ----------------------------------------------------------------------
      TdbAu geoSocLonSP1 = TdbTestUtil.createBasicAu("Geological Society of London Special Publications Volume 287", "2007", "287");
      TdbAu geoSocLonSP2 = TdbTestUtil.createBasicAu("Geological Society of London Special Publications Volume 288", "2008", "288");
      TdbAu geoSocLonSP3 = TdbTestUtil.createBasicAu("Geological Society of London Special Publications Volume 289", "2007", "289");
      geoSocLonSP = Arrays.asList(geoSocLonSP1, geoSocLonSP2, geoSocLonSP3);
      geoSocLonSPYearRanges = Arrays.asList(
          new TitleRange(Arrays.asList(geoSocLonSP1, geoSocLonSP3, geoSocLonSP2))
      );
      geoSocLonSPVolRanges = Arrays.asList(
          new TitleRange(Arrays.asList(geoSocLonSP1, geoSocLonSP2, geoSocLonSP3))
      );

      // ----------------------------------------------------------------------
      TdbAu llc1  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 1",  "1986", "1");
      TdbAu llc2  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 2",  "1987", "2");
      TdbAu llc3  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 3",  "1988", "3");
      TdbAu llc4  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 4",  "1989", "4");
      TdbAu llc5  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 5",  "1990", "5");
      TdbAu llc6  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 6",  "1991", "6");
      TdbAu llc7  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 7",  "1992", "7");
      TdbAu llc8  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 8",  "1993", "8");
      TdbAu llc9  = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 9",  "1994", "9");
      TdbAu llc10 = TdbTestUtil.createBasicAu("Literary and Linguistic Computing Volume 10", "1995", "10");
      fullyConsistentAus = Arrays.asList(llc1, llc2, llc3, llc4, llc5, llc6, llc7, llc8, llc9, llc10);

      // ----------------------------------------------------------------------
      TdbAu afrTod1a  = TdbTestUtil.createBasicAu("Africa Today Volume 46",  "1999", "46");
      TdbAu afrTod1b  = TdbTestUtil.createBasicAu("Africa Today Volume 46",  "1999", "46");
      TdbAu afrTod2a  = TdbTestUtil.createBasicAu("Africa Today Volume 47",  "2000", "47");
      TdbAu afrTod2b  = TdbTestUtil.createBasicAu("Africa Today Volume 47",  "2000", "47");
      TdbAu afrTod3a  = TdbTestUtil.createBasicAu("Africa Today Volume 48",  "2001", "48");
      TdbAu afrTod3b  = TdbTestUtil.createBasicAu("Africa Today Volume 48",  "2001", "48");
      TdbAu afrTod4a  = TdbTestUtil.createBasicAu("Africa Today Volume 49",  "2002-2003", "49");
      TdbAu afrTod4b  = TdbTestUtil.createBasicAu("Africa Today Volume 49",  "2002-2003", "49");
      TdbAu afrTod5a  = TdbTestUtil.createBasicAu("Africa Today Volume 50",  "2003-2004", "50");
      TdbAu afrTod5b  = TdbTestUtil.createBasicAu("Africa Today Volume 50",  "2003-2004", "50");
      // Note these are purposely ordered in pairs of consecutive duplicates
      afrTod = Arrays.asList(afrTod1a, afrTod1b, afrTod2a, afrTod2b, afrTod3a, afrTod3b,
          afrTod4a, afrTod4b, afrTod5a, afrTod5b);

      // ----------------------------------------------------------------------
      TdbAu textStressMicro1a  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 1",  "1972", "1");
      TdbAu textStressMicro1b  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 1",  "1974", "1");
      TdbAu textStressMicro2a  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 2",  "1975", "2");
      TdbAu textStressMicro2b  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 2",  "1976", "2");
      TdbAu textStressMicro2c  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 2",  "1977", "2");
      TdbAu textStressMicro3a  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 3",  "1978", "3");
      TdbAu textStressMicro3b  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 3",  "1979", "3");
      TdbAu textStressMicro4a  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 4",  "1980", "4");
      TdbAu textStressMicro4b  = TdbTestUtil.createBasicAu("Texture, Stress, and Microstructure Volume 4",  "1981", "4");
      // Order in comparison pairs
      textStressMicroVolPairs = Arrays.asList(
          textStressMicro1a, textStressMicro1b,
          textStressMicro2a, textStressMicro2b,
          textStressMicro2b, textStressMicro2c,
          textStressMicro3a, textStressMicro3b,
          textStressMicro4a, textStressMicro4b
      );
*/



}
