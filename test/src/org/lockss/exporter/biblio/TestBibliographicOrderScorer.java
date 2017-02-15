package org.lockss.exporter.biblio;

import junit.framework.TestCase;

import static org.lockss.exporter.biblio.BibliographicUtil.*;
import static org.lockss.exporter.biblio.BibliographicOrderScorer.ConsistencyScore;
import static org.lockss.exporter.biblio.BibliographicOrderScorer.SORT_FIELD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test the behaviour of the BibliographicOrderScorer. Note that the tests here are
 * primarily based on the real-world examples which we are trying to accommodate
 * with the class's fuzzy tests for consecutivity and coverage gaps.
 *
 * @author Neil Mayo
 */
public class TestBibliographicOrderScorer extends TestCase {

  /*
    ---------------------------------------------------------------------------
    Real-world example issues which the OrderScorer is intended to solve
    ---------------------------------------------------------------------------
    
    ---------------------------------------------------------------------------
    (1) Mixed volume identifier formats

    au < manifest ; 2005-2006 ; T'ang Studies Volume 2005 ; 2005 >
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
    au < HighWirePressH20Plugin ; manifest ; 1876 ; Mind Volume os-1 ; os-1 >
    au < HighWirePressH20Plugin ; manifest ; 1877 ; Mind Volume os-2 ; os-2 >
    au < HighWirePressH20Plugin ; manifest ; 1878 ; Mind Volume os-3 ; os-3 >
    au < HighWirePressH20Plugin ; manifest ; 1879 ; Mind Volume os-4 ; os-4 >
    au < HighWirePressH20Plugin ; manifest ; 1880 ; Mind Volume os-V ; os-V >
    au < HighWirePressH20Plugin ; manifest ; 1881 ; Mind Volume os-VI ; os-VI >
    au < HighWirePressH20Plugin ; manifest ; 1882 ; Mind Volume 1 ; 1 >
    au < HighWirePressH20Plugin ; manifest ; 1883 ; Mind Volume II ; II >
    au < HighWirePressH20Plugin ; manifest ; 1884 ; Mind Volume III ; III >

    Ordering by year would provide consistent ordering but we need to recognise
    the change in volume identifier schemas while ignoring the changes in number
    formats. There should be 2 ranges os-1 to os-VI, and 1 to III.
    (This is a reduced instance of real examples in OUP. The years have been
    adjusted to make a consistent run, to test the expected impact of the
    volume ordering on the ranges.)

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

    au < released ; Laser Chemistry Volume 1 (1982) ; 1982 ; 1 ; 1982 >
    au < released ; Laser Chemistry Volume 1-4 (1983) ; 1983 ; 1-4 ; 1983 >
    au < released ; Laser Chemistry Volume 4-5 (1984) ; 1984 ; 4-5 ; 1984 >
    au < released ; Laser Chemistry Volume 5 (1985) ; 1985 ; 5 ; 1985 >
    au < released ; Laser Chemistry Volume 5-6 (1986) ; 1986 ; 5-6 ; 1986 >
    au < released ; Laser Chemistry Volume 7 (1987) ; 1987 ; 7 ; 1987 >
    au < released ; Laser Chemistry Volume 8-9 (1988) ; 1988 ; 8-9 ; 1988 >
    au < released ; Laser Chemistry Volume 10 (1989) ; 1989 ; 10 ; 1989 >
    au < released ; Laser Chemistry Volume 10 (1990) ; 1990 ; 10 ; 1990 >
    au < released ; Laser Chemistry Volume 11 (1991) ; 1991 ; 11 ; 1991 >

    ---------------------------------------------------------------------------
    (7) Volume scheme changes; years remain consistent.

    au < released ; Abstract and Applied Analysis Volume 5 (2000) ; 2000 ; 5 ; 2000 >
    au < released ; Abstract and Applied Analysis Volume 6 (2001) ; 2001 ; 6 ; 2001 >
    au < released ; Abstract and Applied Analysis Volume 7 (2002) ; 2002 ; 7 ; 2002 >
    au < released ; Abstract and Applied Analysis Volume 2003 ; 2003 ; 2003 ; 2003 >
    au < released ; Abstract and Applied Analysis Volume 2004 ; 2004 ; 2004 ; 2004 >
    au < released ; Abstract and Applied Analysis Volume 2005 ; 2005 ; 2005 ; 2005 >

    ---------------------------------------------------------------------------
    (8) Publication year gaps with no volume field (primarily Libertas Academica)

    au < released ; 2009 ; Advances in Tumor Virology Volume 1 ; 2009 >
    au < released ; 2011 ; Advances in Tumor Virology Volume 2 ; 2011 >

    au < released ; 2008 ; Clinical Medicine Insights: Psychiatry Volume 1 ; 2008 >
    au < released ; 2009 ; Clinical Medicine Insights: Psychiatry Volume 2 ; 2009 >
    au < released ; 2011 ; Clinical Medicine Insights: Psychiatry Volume 3 ; 2011 >
    au < released ; 2012 ; Clinical Medicine Insights: Psychiatry Volume 4 ; 2012 >

    ---------------------------------------------------------------------------
   */

  // A list of AUs whose years and volumes are very well behaved. The volume 
  // ordering should equal the year ordering, and both should be equal to 
  // the original ordering. There should be no coverage gaps, resulting in a 
  // single range which is also equal to the original list.
  List<BibliographicItem> fullyConsistentAus;

  // A sequence of interleaved duplicate AUs (identical in terms of main fields).
  // This occurs for example when there are 2 sets of AUs, one down, one released 
  // and current.
  List<BibliographicItem> afrTod;

  // A sequence of AUs whose volumes stretch over several years. The list is
  // organised in pairs that should be considered to be of the same volume.
  List<BibliographicItem> textStressMicroVolPairs;

  // Example titles
  List<BibliographicItem> tang;
  List<BibliographicItem> oxEcPap;            // Could be ordered by volume
  List<BibliographicItem> mind;               // Should be ordered by volume
  List<BibliographicItem> euroBusRev;
  List<BibliographicItem> nutFoodSci;
  List<BibliographicItem> intlJournHumArtsComp;
  List<BibliographicItem> expAstr;            // Should be ordered by volume
  List<BibliographicItem> analChem;           // Should be ordered by volume
  List<BibliographicItem> journEndoc;         // Should be ordered by volume
  List<BibliographicItem> yorkGeoSoc;         // Should be ordered by volume
  List<BibliographicItem> commDis;            // Should be ordered by volume
  List<BibliographicItem> geoSocLonMem;       // Should be ordered by volume
  List<BibliographicItem> geoSocLonSP;        // Should be ordered by volume
  List<BibliographicItem> laserChem;
  List<BibliographicItem> aaa;                 // Should be ordered/range-split by year
  List<BibliographicItem> tumVir;             // Should be ordered/range-split by year

  // Record all the problematic test AUs in a list
  List<List<BibliographicItem>> problemTitles;

  // Record all the test AU sequences which are consistent
  List<List<BibliographicItem>> consistentSequences;

  // Make immutable copies of the canonical orderings and store in a list
  List<List<BibliographicItem>> allCanonicalLists;


  //---------------------------------------------------------------------------
  // Manually-specified title ranges for testing get[Volume|Year]RangeConsistency
  //---------------------------------------------------------------------------

  // Expected result of ordering/splitting by volume
  List<TitleRange> tangVolRanges;
  List<TitleRange> oxEcPapVolRanges;
  List<TitleRange> mindVolRanges;
  List<TitleRange> euroBusRevVolRanges;
  List<TitleRange> nutFoodSciVolRanges;
  List<TitleRange> intlJournHumArtsCompVolRanges;
  List<TitleRange> expAstrVolRanges;
  List<TitleRange> analChemVolRanges;
  List<TitleRange> journEndocVolRanges;
  List<TitleRange> yorkGeoSocVolRanges;
  List<TitleRange> commDisVolRanges;
  List<TitleRange> geoSocLonMemVolRanges;
  List<TitleRange> geoSocLonSPVolRanges;

  // Record all the vol ranges in a list
  List<List<TitleRange>> allVolRanges;

  // Expected result of ordering/splitting by year
  List<TitleRange> tangYearRanges;
  List<TitleRange> oxEcPapYearRanges;
  List<TitleRange> mindYearRanges;
  List<TitleRange> euroBusRevYearRanges;
  List<TitleRange> nutFoodSciYearRanges;
  List<TitleRange> intlJournHumArtsCompYearRanges;
  List<TitleRange> expAstrYearRanges;
  List<TitleRange> analChemYearRanges;
  List<TitleRange> journEndocYearRanges;
  List<TitleRange> yorkGeoSocYearRanges;
  List<TitleRange> commDisYearRanges;
  List<TitleRange> geoSocLonMemYearRanges;
  List<TitleRange> geoSocLonSPYearRanges;

  // Record all the year ranges in a list
  List<List<TitleRange>> allYearRanges;

  /**
   * Maintain a list of the indices of title lists that should be ordered by
   * volume based on the analysis performed herein.
   */
  List<Integer> titlesToOrderByVolume;
  /**
   * Maintain a list of the indices of title lists that should be ordered by
   * year based on the analysis performed herein.
   */
  List<Integer> titlesToOrderByYear;
  // Indices that do not appear in either of these lists can be ordered either way

  // Shorten references to sort fields
  private static final BibliographicOrderScorer.SORT_FIELD YR  =
      BibliographicOrderScorer.SORT_FIELD.YEAR;
  private static final BibliographicOrderScorer.SORT_FIELD VOL =
      BibliographicOrderScorer.SORT_FIELD.VOLUME;

  /**
   * Create basic BibliographicItems and populate all the lists. For each title, a set of
   * BibliographicItems is created and added to a list; that list is added to either the 
   * <code>problemSequences</code> list or the <code>consistentSequences</code> 
   * list; then expected year and volume ranges are created and added to the 
   * range lists; finally the title list is registered as one which 
   * should be ordered by year, or by volume, or with no preference. 
   */
  protected void setUp() throws Exception {
    super.setUp();

    // Init all the lists
    problemTitles = new ArrayList<List<BibliographicItem>>();
    consistentSequences = new ArrayList<List<BibliographicItem>>();
    allCanonicalLists = new ArrayList<List<BibliographicItem>>();
    allVolRanges = new ArrayList<List<TitleRange>>();
    allYearRanges = new ArrayList<List<TitleRange>>();
    titlesToOrderByVolume = new ArrayList<Integer>();
    titlesToOrderByYear = new ArrayList<Integer>();

    // ----------------------------------------------------------------------
    BibliographicItem tang1 = new BibliographicItemImpl()
        .setName("T'ang Studies Volume 2005")
        .setYear("2005-2006")
        .setVolume("2005");
    BibliographicItem tang2 = new BibliographicItemImpl()
        .setName("T'ang Studies Volume 2007")
        .setYear("2007")
        .setVolume("2007");
    BibliographicItem tang3 = new BibliographicItemImpl()
        .setName("T'ang Studies Volume 2008")
        .setYear("2008")
        .setVolume("2008");
    BibliographicItem tang4 = new BibliographicItemImpl()
        .setName("T'ang Studies Volume 27")
        .setYear("2009")
        .setVolume("27");
    BibliographicItem tang5 = new BibliographicItemImpl()
        .setName("T'ang Studies Volume 2010")
        .setYear("2010")
        .setVolume("2010");
    tang = Arrays.asList(tang1, tang2, tang3, tang4, tang5);
    problemTitles.add(tang);
    allCanonicalLists.add(Collections.unmodifiableList(tang));
    tangYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(tang1, tang2, tang3, tang4, tang5))
    );
    tangVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(tang3)),
        new TitleRange(Arrays.asList(tang1, tang2)),
        new TitleRange(Arrays.asList(tang4))
    );
    allYearRanges.add(tangYearRanges);
    allVolRanges.add(tangVolRanges);
    registerOrderByYear();

    // ----------------------------------------------------------------------
    BibliographicItem oxEcPap1 = new BibliographicItemImpl()
        .setName("Oxford Economic Papers Volume os-6")
        .setYear("1942")
        .setVolume("os-6");
    BibliographicItem oxEcPap2 = new BibliographicItemImpl()
        .setName("Oxford Economic Papers Volume os-7")
        .setYear("1945")
        .setVolume("os-7");
    BibliographicItem oxEcPap3 = new BibliographicItemImpl()
        .setName("Oxford Economic Papers Volume os-8")
        .setYear("1948")
        .setVolume("os-8");
    BibliographicItem oxEcPap4 = new BibliographicItemImpl()
        .setName("Oxford Economic Papers Volume 1")
        .setYear("1949")
        .setVolume("1");
    BibliographicItem oxEcPap5 = new BibliographicItemImpl()
        .setName("Oxford Economic Papers Volume 2")
        .setYear("1950")
        .setVolume("2");
    BibliographicItem oxEcPap6 = new BibliographicItemImpl()
        .setName("Oxford Economic Papers Volume 3")
        .setYear("1951")
        .setVolume("3");
    oxEcPap = Arrays.asList(oxEcPap1, oxEcPap2, oxEcPap3, oxEcPap4, oxEcPap5, oxEcPap6);
    problemTitles.add(oxEcPap);
    allCanonicalLists.add(Collections.unmodifiableList(oxEcPap));
    oxEcPapYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(oxEcPap1)),
        new TitleRange(Arrays.asList(oxEcPap2)),
        new TitleRange(Arrays.asList(oxEcPap3, oxEcPap4, oxEcPap5))
    );
    oxEcPapVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(oxEcPap1, oxEcPap2, oxEcPap3)),
        new TitleRange(Arrays.asList(oxEcPap4, oxEcPap5, oxEcPap6))
    );
    allYearRanges.add(oxEcPapYearRanges);
    allVolRanges.add(oxEcPapVolRanges);
    // Note that OEP can be ordered on either column, as long as we end up
    // with 2 ranges, but we expect volume to be preferred
    //registerOrderByVolume();

    // ----------------------------------------------------------------------
    BibliographicItem mind1 = new BibliographicItemImpl()
        .setName("Mind Volume os-1")
        .setYear("1876")
        .setVolume("os-1");
    BibliographicItem mind2 = new BibliographicItemImpl()
        .setName("Mind Volume os-2")
        .setYear("1877")
        .setVolume("os-2");
    BibliographicItem mind3 = new BibliographicItemImpl()
        .setName("Mind Volume os-3")
        .setYear("1878")
        .setVolume("os-3");
    BibliographicItem mind4 = new BibliographicItemImpl()
        .setName("Mind Volume os-4")
        .setYear("1879")
        .setVolume("os-4");
    BibliographicItem mind5 = new BibliographicItemImpl()
        .setName("Mind Volume os-V")
        .setYear("1880")
        .setVolume("os-V");
    BibliographicItem mind6 = new BibliographicItemImpl()
        .setName("Mind Volume os-VI")
        .setYear("1881")
        .setVolume("os-VI");
    BibliographicItem mind7 = new BibliographicItemImpl()
        .setName("Mind Volume 1")
        .setYear("1882")
        .setVolume("1");
    BibliographicItem mind8 = new BibliographicItemImpl()
        .setName("Mind Volume II")
        .setYear("1883")
        .setVolume("II");
    BibliographicItem mind9 = new BibliographicItemImpl()
        .setName("Mind Volume III")
        .setYear("1884")
        .setVolume("III");
    mind = Arrays.asList(mind1, mind2, mind3, mind4, mind5, mind6, mind7, mind8,
        mind9);
    problemTitles.add(mind);
    allCanonicalLists.add(Collections.unmodifiableList(mind));
    mindYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(mind1, mind2, mind3, mind4, mind5, mind6, mind7, mind8, mind9))
    );
    mindVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(mind1, mind2, mind3, mind4, mind5, mind6)),
        new TitleRange(Arrays.asList(mind7, mind8, mind9))
    );
    allYearRanges.add(mindYearRanges);
    allVolRanges.add(mindVolRanges);
    // Note that Mind can be ordered on either column, as long as we end up
    // with 2 ranges, but we expect year to be preferred
    //registerOrderByYear();
    // NOTE Volume ordering must account for Roman numbers in any token

    // ----------------------------------------------------------------------
    BibliographicItem euroBusRev1 = new BibliographicItemImpl()
        .setName("European Business Review Volume 97")
        .setYear("1997")
        .setVolume("97");
    BibliographicItem euroBusRev2 = new BibliographicItemImpl()
        .setName("European Business Review Volume 98")
        .setYear("1998")
        .setVolume("98");
    BibliographicItem euroBusRev3 = new BibliographicItemImpl()
        .setName("European Business Review Volume 99")
        .setYear("1999")
        .setVolume("99");
    BibliographicItem euroBusRev4 = new BibliographicItemImpl()
        .setName("European Business Review Volume 12")
        .setYear("2000")
        .setVolume("12");
    BibliographicItem euroBusRev5 = new BibliographicItemImpl()
        .setName("European Business Review Volume 13")
        .setYear("2001")
        .setVolume("13");
    BibliographicItem euroBusRev6 = new BibliographicItemImpl()
        .setName("European Business Review Volume 14")
        .setYear("2002")
        .setVolume("14");
    euroBusRev = Arrays.asList(euroBusRev1, euroBusRev2, euroBusRev3, euroBusRev4, euroBusRev5, euroBusRev6);
    problemTitles.add(euroBusRev);
    allCanonicalLists.add(Collections.unmodifiableList(euroBusRev));
    euroBusRevYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(euroBusRev1, euroBusRev2, euroBusRev3, euroBusRev4, euroBusRev5, euroBusRev6))
    );
    euroBusRevVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(euroBusRev1, euroBusRev2, euroBusRev3)),
        new TitleRange(Arrays.asList(euroBusRev4, euroBusRev5, euroBusRev6))
    );
    allYearRanges.add(euroBusRevYearRanges);
    allVolRanges.add(euroBusRevVolRanges);
    registerOrderByYear();

    // ----------------------------------------------------------------------
    BibliographicItem nutFoodSci1 = new BibliographicItemImpl()
        .setName("Nutrition & Food Science 97")
        .setYear("1997")
        .setVolume("97");
    BibliographicItem nutFoodSci2 = new BibliographicItemImpl()
        .setName("Nutrition & Food Science 98")
        .setYear("1998")
        .setVolume("98");
    BibliographicItem nutFoodSci3 = new BibliographicItemImpl()
        .setName("Nutrition & Food Science 99")
        .setYear("1999")
        .setVolume("99");
    BibliographicItem nutFoodSci4 = new BibliographicItemImpl()
        .setName("Nutrition & Food Science 30")
        .setYear("2000")
        .setVolume("30");
    BibliographicItem nutFoodSci5 = new BibliographicItemImpl()
        .setName("Nutrition & Food Science 31")
        .setYear("2001")
        .setVolume("31");
    BibliographicItem nutFoodSci6 = new BibliographicItemImpl()
        .setName("Nutrition & Food Science 32")
        .setYear("2002")
        .setVolume("32");
    nutFoodSci = Arrays.asList(nutFoodSci1, nutFoodSci2, nutFoodSci3, nutFoodSci4, nutFoodSci5, nutFoodSci6);
    problemTitles.add(nutFoodSci);
    allCanonicalLists.add(Collections.unmodifiableList(nutFoodSci));
    nutFoodSciYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(nutFoodSci1, nutFoodSci2, nutFoodSci3, nutFoodSci4, nutFoodSci5, nutFoodSci6))
    );
    nutFoodSciVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(nutFoodSci1, nutFoodSci2, nutFoodSci3)),
        new TitleRange(Arrays.asList(nutFoodSci4, nutFoodSci5, nutFoodSci6))
    );
    allYearRanges.add(nutFoodSciYearRanges);
    allVolRanges.add(nutFoodSciVolRanges);
    registerOrderByYear();

    // ----------------------------------------------------------------------
    BibliographicItem intlJournHumArtsComp1  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 6 (1994)")
        .setYear("1994")
        .setVolume("6");
    BibliographicItem intlJournHumArtsComp2  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 7 (1995)")
        .setYear("1995")
        .setVolume("7");
    BibliographicItem intlJournHumArtsComp3  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 8 (1996)")
        .setYear("1996")
        .setVolume("8");
    BibliographicItem intlJournHumArtsComp4  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 9 (1997)")
        .setYear("1997")
        .setVolume("9");
    BibliographicItem intlJournHumArtsComp5  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 10 (1998)")
        .setYear("1998")
        .setVolume("10");
    BibliographicItem intlJournHumArtsComp6  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 11 (1999)")
        .setYear("1999")
        .setVolume("11");
    BibliographicItem intlJournHumArtsComp7  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 12 (2000)")
        .setYear("2000")
        .setVolume("12");
    BibliographicItem intlJournHumArtsComp8  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 13 (2001)")
        .setYear("2001")
        .setVolume("13");
    BibliographicItem intlJournHumArtsComp9  = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 14 (2002)")
        .setYear("2002")
        .setVolume("14");
    BibliographicItem intlJournHumArtsComp10 = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 1 (2007)")
        .setYear("2007")
        .setVolume("1");
    BibliographicItem intlJournHumArtsComp11 = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 2 (2008)")
        .setYear("2008")
        .setVolume("2");
    BibliographicItem intlJournHumArtsComp12 = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 3 (2009)")
        .setYear("2009")
        .setVolume("3");
    BibliographicItem intlJournHumArtsComp13 = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 4 (2010)")
        .setYear("2010")
        .setVolume("4");
    BibliographicItem intlJournHumArtsComp14 = new BibliographicItemImpl()
        .setName("International Journal of Humanities and Arts Computing Volume 5 (2011)")
        .setYear("2011")
        .setVolume("5");
    intlJournHumArtsComp = Arrays.asList(
        intlJournHumArtsComp1, intlJournHumArtsComp2, intlJournHumArtsComp3,
        intlJournHumArtsComp4, intlJournHumArtsComp5, intlJournHumArtsComp6,
        intlJournHumArtsComp7, intlJournHumArtsComp8, intlJournHumArtsComp9,
        intlJournHumArtsComp10, intlJournHumArtsComp11, intlJournHumArtsComp12,
        intlJournHumArtsComp13, intlJournHumArtsComp14
    );
    problemTitles.add(intlJournHumArtsComp);
    allCanonicalLists.add(Collections.unmodifiableList(intlJournHumArtsComp));
    intlJournHumArtsCompYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(
            intlJournHumArtsComp1, intlJournHumArtsComp2, intlJournHumArtsComp3,
            intlJournHumArtsComp4, intlJournHumArtsComp5, intlJournHumArtsComp6,
            intlJournHumArtsComp7, intlJournHumArtsComp8, intlJournHumArtsComp9)
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
    allYearRanges.add(intlJournHumArtsCompYearRanges);
    allVolRanges.add(intlJournHumArtsCompVolRanges);
    registerOrderByYear();

    // ----------------------------------------------------------------------
    BibliographicItem expAstr1 = new BibliographicItemImpl()
        .setName("Experimental Astronomy Volume 3")
        .setYear("1994")
        .setVolume("3");
    BibliographicItem expAstr2 = new BibliographicItemImpl()
        .setName("Experimental Astronomy Volume 4")
        .setYear("1993-1994")
        .setVolume("4");
    BibliographicItem expAstr3 = new BibliographicItemImpl()
        .setName("Experimental Astronomy Volume 5")
        .setYear("1994")
        .setVolume("5");
    expAstr = Arrays.asList(expAstr1, expAstr2, expAstr3);
    problemTitles.add(expAstr);
    allCanonicalLists.add(Collections.unmodifiableList(expAstr));
    expAstrYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(expAstr2, expAstr1, expAstr3))
    );
    expAstrVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(expAstr1, expAstr2, expAstr3))
    );
    allYearRanges.add(expAstrYearRanges);
    allVolRanges.add(expAstrVolRanges);
    registerOrderByVolume();

    // ----------------------------------------------------------------------
    BibliographicItem analChem1 = new BibliographicItemImpl()
        .setName("Fresenius Zeitschrift für Analytische Chemie Volume 275")
        .setYear("1975")
        .setVolume("275");
    BibliographicItem analChem2 = new BibliographicItemImpl()
        .setName("Fresenius Zeitschrift für Analytische Chemie Volume 276")
        .setYear("1972-1975")
        .setVolume("276");
    BibliographicItem analChem3 = new BibliographicItemImpl()
        .setName("Fresenius Zeitschrift für Analytische Chemie Volume 277")
        .setYear("1975")
        .setVolume("277");
    analChem = Arrays.asList(analChem1, analChem2, analChem3);
    problemTitles.add(analChem);
    allCanonicalLists.add(Collections.unmodifiableList(analChem));
    analChemYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(analChem2, analChem1, analChem3))
    );
    analChemVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(analChem1, analChem2, analChem3))
    );
    allYearRanges.add(analChemYearRanges);
    allVolRanges.add(analChemVolRanges);
    registerOrderByVolume();

    // ----------------------------------------------------------------------
    BibliographicItem journEndoc1 = new BibliographicItemImpl()
        .setName("Journal of Endocrinology Volume 20")
        .setYear("1960")
        .setVolume("20");
    BibliographicItem journEndoc2 = new BibliographicItemImpl()
        .setName("Journal of Endocrinology Volume 21")
        .setYear("1960-1961")
        .setVolume("21");
    BibliographicItem journEndoc3 = new BibliographicItemImpl()
        .setName("Journal of Endocrinology Volume 22")
        .setYear("1962")
        .setVolume("22");
    BibliographicItem journEndoc4 = new BibliographicItemImpl()
        .setName("Journal of Endocrinology Volume 23")
        .setYear("1961-1962")
        .setVolume("23");
    BibliographicItem journEndoc5 = new BibliographicItemImpl()
        .setName("Journal of Endocrinology Volume 24")
        .setYear("1962")
        .setVolume("24");
    journEndoc = Arrays.asList(journEndoc1, journEndoc2, journEndoc3, journEndoc4, journEndoc5);
    problemTitles.add(journEndoc);
    allCanonicalLists.add(Collections.unmodifiableList(journEndoc));
    journEndocYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(journEndoc1, journEndoc2, journEndoc4, journEndoc3, journEndoc5))
    );
    journEndocVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(journEndoc1, journEndoc2, journEndoc3, journEndoc4, journEndoc5))
    );
    allYearRanges.add(journEndocYearRanges);
    allVolRanges.add(journEndocVolRanges);
    registerOrderByVolume();

    // ----------------------------------------------------------------------
    BibliographicItem yorkGeoSoc1 = new BibliographicItemImpl()
        .setName("Proceedings of the Yorkshire Geological Society Volume 8")
        .setYear("1882-1884")
        .setVolume("8");
    BibliographicItem yorkGeoSoc2 = new BibliographicItemImpl()
        .setName("Proceedings of the Yorkshire Geological Society Volume 9")
        .setYear("1885-1887")
        .setVolume("9");
    BibliographicItem yorkGeoSoc3 = new BibliographicItemImpl()
        .setName("Proceedings of the Yorkshire Geological Society Volume 10")
        .setYear("1889")
        .setVolume("10");
    BibliographicItem yorkGeoSoc4 = new BibliographicItemImpl()
        .setName("Proceedings of the Yorkshire Geological Society Volume 11")
        .setYear("1888-1890")
        .setVolume("11");
    BibliographicItem yorkGeoSoc5 = new BibliographicItemImpl()
        .setName("Proceedings of the Yorkshire Geological Society Volume 12")
        .setYear("1891-1894")
        .setVolume("12");
    yorkGeoSoc = Arrays.asList(yorkGeoSoc1, yorkGeoSoc2, yorkGeoSoc3, yorkGeoSoc4, yorkGeoSoc5);
    problemTitles.add(yorkGeoSoc);
    allCanonicalLists.add(Collections.unmodifiableList(yorkGeoSoc));
    yorkGeoSocYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(yorkGeoSoc1, yorkGeoSoc2)),
        new TitleRange(Arrays.asList(yorkGeoSoc4, yorkGeoSoc3)),
        new TitleRange(Arrays.asList(yorkGeoSoc5))
    );
    yorkGeoSocVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(yorkGeoSoc1, yorkGeoSoc2, yorkGeoSoc3, yorkGeoSoc4, yorkGeoSoc5))
    );
    allYearRanges.add(yorkGeoSocYearRanges);
    allVolRanges.add(yorkGeoSocVolRanges);
    registerOrderByVolume();

    // ----------------------------------------------------------------------
    BibliographicItem commDis1 = new BibliographicItemImpl()
        .setName("Communication Disorders Quarterly Volume 12")
        .setYear("1988-1989")
        .setVolume("12");
    BibliographicItem commDis2 = new BibliographicItemImpl()
        .setName("Communication Disorders Quarterly Volume 13")
        .setYear("1990")
        .setVolume("13");
    BibliographicItem commDis3 = new BibliographicItemImpl()
        .setName("Communication Disorders Quarterly Volume 14")
        .setYear("1988-1992")
        .setVolume("14");
    commDis = Arrays.asList(commDis1, commDis2, commDis3);
    problemTitles.add(commDis);
    allCanonicalLists.add(Collections.unmodifiableList(commDis));
    commDisYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(commDis1, commDis3, commDis2))
    );
    commDisVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(commDis1, commDis2, commDis3))
    );
    allYearRanges.add(commDisYearRanges);
    allVolRanges.add(commDisVolRanges);
    registerOrderByVolume();

    // ----------------------------------------------------------------------
    BibliographicItem geoSocLonMem1 = new BibliographicItemImpl()
        .setName("Geological Society of London Memoirs Volume 13")
        .setYear("1992")
        .setVolume("13");
    BibliographicItem geoSocLonMem2 = new BibliographicItemImpl()
        .setName("Geological Society of London Memoirs Volume 14")
        .setYear("1991")
        .setVolume("14");
    BibliographicItem geoSocLonMem3 = new BibliographicItemImpl()
        .setName("Geological Society of London Memoirs Volume 15")
        .setYear("1994")
        .setVolume("15");
    geoSocLonMem = Arrays.asList(geoSocLonMem1, geoSocLonMem2, geoSocLonMem3);
    problemTitles.add(geoSocLonMem);
    allCanonicalLists.add(Collections.unmodifiableList(geoSocLonMem));
    geoSocLonMemYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(geoSocLonMem2, geoSocLonMem1)),
        new TitleRange(Arrays.asList(geoSocLonMem3))
    );
    geoSocLonMemVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(geoSocLonMem1, geoSocLonMem2, geoSocLonMem3))
    );
    allYearRanges.add(geoSocLonMemYearRanges);
    allVolRanges.add(geoSocLonMemVolRanges);
    registerOrderByVolume();

    // ----------------------------------------------------------------------
    BibliographicItem geoSocLonSP1 = new BibliographicItemImpl()
        .setName("Geological Society of London Special Publications Volume 287")
        .setYear("2007")
        .setVolume("287");
    BibliographicItem geoSocLonSP2 = new BibliographicItemImpl()
        .setName("Geological Society of London Special Publications Volume 288")
        .setYear("2008")
        .setVolume("288");
    BibliographicItem geoSocLonSP3 = new BibliographicItemImpl()
        .setName("Geological Society of London Special Publications Volume 289")
        .setYear("2007")
        .setVolume("289");
    geoSocLonSP = Arrays.asList(geoSocLonSP1, geoSocLonSP2, geoSocLonSP3);
    problemTitles.add(geoSocLonSP);
    allCanonicalLists.add(Collections.unmodifiableList(geoSocLonSP));
    geoSocLonSPYearRanges = Arrays.asList(
        new TitleRange(Arrays.asList(geoSocLonSP1, geoSocLonSP3, geoSocLonSP2))
    );
    geoSocLonSPVolRanges = Arrays.asList(
        new TitleRange(Arrays.asList(geoSocLonSP1, geoSocLonSP2, geoSocLonSP3))
    );
    allYearRanges.add(geoSocLonSPYearRanges);
    allVolRanges.add(geoSocLonSPVolRanges);
    registerOrderByVolume();

    // ----------------------------------------------------------------------
    BibliographicItem llc1  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 1")
        .setYear("1986")
        .setVolume("1");
    BibliographicItem llc2  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 2")
        .setYear("1987")
        .setVolume("2");
    BibliographicItem llc3  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 3")
        .setYear("1988")
        .setVolume("3");
    BibliographicItem llc4  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 4")
        .setYear("1989")
        .setVolume("4");
    BibliographicItem llc5  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 5")
        .setYear("1990")
        .setVolume("5");
    BibliographicItem llc6  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 6")
        .setYear("1991")
        .setVolume("6");
    BibliographicItem llc7  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 7")
        .setYear("1992")
        .setVolume("7");
    BibliographicItem llc8  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 8")
        .setYear("1993")
        .setVolume("8");
    BibliographicItem llc9  = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 9")
        .setYear("1994")
        .setVolume("9");
    BibliographicItem llc10 = new BibliographicItemImpl()
        .setName("Literary and Linguistic Computing Volume 10")
        .setYear("1995")
        .setVolume("10");
    fullyConsistentAus = Arrays.asList(llc1, llc2, llc3, llc4, llc5, llc6, llc7, llc8, llc9, llc10);
    consistentSequences.add(fullyConsistentAus);

    // ----------------------------------------------------------------------
    BibliographicItem afrTod1a  = new BibliographicItemImpl()
        .setName("Africa Today Volume 46")
        .setYear("1999")
        .setVolume("46");
    BibliographicItem afrTod1b  = new BibliographicItemImpl()
        .setName("Africa Today Volume 46")
        .setYear("1999")
        .setVolume("46");
    BibliographicItem afrTod2a  = new BibliographicItemImpl()
        .setName("Africa Today Volume 47")
        .setYear("2000")
        .setVolume("47");
    BibliographicItem afrTod2b  = new BibliographicItemImpl()
        .setName("Africa Today Volume 47")
        .setYear("2000")
        .setVolume("47");
    BibliographicItem afrTod3a  = new BibliographicItemImpl()
        .setName("Africa Today Volume 48")
        .setYear("2001")
        .setVolume("48");
    BibliographicItem afrTod3b  = new BibliographicItemImpl()
        .setName("Africa Today Volume 48")
        .setYear("2001")
        .setVolume("48");
    BibliographicItem afrTod4a  = new BibliographicItemImpl()
        .setName("Africa Today Volume 49")
        .setYear("2002-2003")
        .setVolume("49");
    BibliographicItem afrTod4b  = new BibliographicItemImpl()
        .setName("Africa Today Volume 49")
        .setYear("2002-2003")
        .setVolume("49");
    BibliographicItem afrTod5a  = new BibliographicItemImpl()
        .setName("Africa Today Volume 50")
        .setYear("2003-2004")
        .setVolume("50");
    BibliographicItem afrTod5b  = new BibliographicItemImpl()
        .setName("Africa Today Volume 50")
        .setYear("2003-2004")
        .setVolume("50");
    // Note these are purposely ordered in pairs of consecutive duplicates
    afrTod = Arrays.asList(afrTod1a, afrTod1b, afrTod2a, afrTod2b, afrTod3a, afrTod3b,
        afrTod4a, afrTod4b, afrTod5a, afrTod5b);
    consistentSequences.add(afrTod);

    // ----------------------------------------------------------------------
    BibliographicItem textStressMicro1a  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 1")
        .setYear("1972")
        .setVolume("1");
    BibliographicItem textStressMicro1b  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 1")
        .setYear("1974")
        .setVolume("1");
    BibliographicItem textStressMicro2a  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 2")
        .setYear("1975")
        .setVolume("2");
    BibliographicItem textStressMicro2b  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 2")
        .setYear("1976")
        .setVolume("2");
    BibliographicItem textStressMicro2c  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 2")
        .setYear("1977")
        .setVolume("2");
    BibliographicItem textStressMicro3a  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 3")
        .setYear("1978")
        .setVolume("3");
    BibliographicItem textStressMicro3b  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 3")
        .setYear("1979")
        .setVolume("3");
    BibliographicItem textStressMicro4a  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 4")
        .setYear("1980")
        .setVolume("4");
    BibliographicItem textStressMicro4b  = new BibliographicItemImpl()
        .setName("Texture, Stress, and Microstructure Volume 4")
        .setYear("1981")
        .setVolume("4");
    // Order in comparison pairs
    textStressMicroVolPairs = Arrays.asList(
        textStressMicro1a, textStressMicro1b,
        textStressMicro2a, textStressMicro2b,
        textStressMicro2b, textStressMicro2c,
        textStressMicro3a, textStressMicro3b,
        textStressMicro4a, textStressMicro4b
    );

    // ----------------------------------------------------------------------
    BibliographicItem laserChem1  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 1 (1982)")
        .setYear("1982")
        .setVolume("1");
    BibliographicItem laserChem2  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 1-4 (1983)")
        .setYear("1983")
        .setVolume("1-4");
    BibliographicItem laserChem3  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 4-5 (1984)")
        .setYear("1984")
        .setVolume("4-5");
    BibliographicItem laserChem4  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 5 (1985)")
        .setYear("1985")
        .setVolume("5");
    BibliographicItem laserChem5  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 5-6 (1986)")
        .setYear("1986")
        .setVolume("5-6");
    BibliographicItem laserChem6  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 7 (1987)")
        .setYear("1987")
        .setVolume("7");
    BibliographicItem laserChem7  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 8-9 (1988)")
        .setYear("1988")
        .setVolume("8-9");
    BibliographicItem laserChem8  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 10 (1989)")
        .setYear("1989")
        .setVolume("10");
    BibliographicItem laserChem9  = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 10 (1990)")
        .setYear("1990")
        .setVolume("10");
    BibliographicItem laserChem10 = new BibliographicItemImpl()
        .setName("Laser Chemistry Volume 11 (1991)")
        .setYear("1991")
        .setVolume("11");
    laserChem = Arrays.asList(
        laserChem1, laserChem2, laserChem3, laserChem4, laserChem5,
        laserChem6, laserChem7, laserChem8, laserChem9, laserChem10
    );
    consistentSequences.add(laserChem);

    // ----------------------------------------------------------------------
    BibliographicItem aaa1  = new BibliographicItemImpl()
        .setName("Abstract and Applied Analysis Volume 5 (2000)")
        .setYear("2000")
        .setVolume("5");
    BibliographicItem aaa2  = new BibliographicItemImpl()
        .setName("Abstract and Applied Analysis Volume 6 (2001)")
        .setYear("2001")
        .setVolume("6");
    BibliographicItem aaa3  = new BibliographicItemImpl()
        .setName("Abstract and Applied Analysis Volume 7 (2002)")
        .setYear("2002")
        .setVolume("7");
    BibliographicItem aaa4  = new BibliographicItemImpl()
        .setName("Abstract and Applied Analysis Volume 2003")
        .setYear("2003")
        .setVolume("2003");
    BibliographicItem aaa5  = new BibliographicItemImpl()
        .setName("Abstract and Applied Analysis Volume 2004")
        .setYear("2004")
        .setVolume("2004");
    BibliographicItem aaa6  = new BibliographicItemImpl()
        .setName("Abstract and Applied Analysis Volume 2005")
        .setYear("2005")
        .setVolume("2005");

    aaa = Arrays.asList(aaa1, aaa2, aaa3, aaa4, aaa5, aaa6);

    // ----------------------------------------------------------------------
    // Advances in Tumor Virology has no volume field filled in, so should
    // depend only on year, which in this case includes a gap
    BibliographicItem tumVir1  = new BibliographicItemImpl()
        .setName("Advances in Tumor Virology Volume 1")
        .setYear("2009");
        //.setVolume("");
    BibliographicItem tumVir2  = new BibliographicItemImpl()
        .setName("Advances in Tumor Virology Volume 2")
        .setYear("2011");
        //.setVolume("");

    tumVir = Arrays.asList(tumVir1, tumVir2);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  // This would need to be in a different package!
  /*public final void testInstantiation() {
    try {
      new BibliographicOrderScorer();
      fail("It should not be possible to instantiate BibliographicOrderScorer.");
    } catch (RuntimeException e) {
      // Expected exception
    }
  }*/

  // ----------------------------------------------------------------------
  // TODO test the methods of the SORT_FIELD enums ?
  // These all delegate to methods already under test,
  // but sometimes combine them in interesting ways.
  // ----------------------------------------------------------------------

  /**
   * Determines whether a String appears to represent a range. In general, a
   * range is considered to be <i>either</i> two numeric strings <i>or</i> two
   * non-numeric strings, separated by a hyphen '-' with optional whitespace.
   * The second value is expected to be numerically or lexically greater than
   * the first. For example, "s1-4" would not qualify as either a numeric or a
   * non-numeric range, while "I-4" ("I" being a Roman number) and the string
   * range "a-baa" would.
   * <p>
   * "s1-4" and "1-s4" are invalid ranges
   * Both sides if the hyphen must either be parseable as integers, or both
   * involve non-numerical tokens.
   * <p>
   * To allow for identifers that themselves incorporate a hyphen, the input
   * string is only split around the centremost hyphen. If there is an even
   * number of hyphens, the input string is assumed not to represent a parseable
   * range.
   */
  public final void testIsRange() {
    // Invalid input
    assertFalse(isRange(null));
    assertFalse(isRange(""));

    // Invalid ranges
    assertFalse(isRange("s1-4"));
    assertFalse(isRange("1-s4"));
    assertFalse(isRange("s1-s2-s3"));
    assertFalse(isRange("s1-2-3"));
    assertFalse(isRange("s123"));
    assertFalse(isRange("1-2-3"));
    assertFalse(isRange("123"));
    assertFalse(isRange("a-1"));
    assertFalse(isRange("1-two"));
    // Invalid downward ranges
    assertFalse(isRange("II-I"));
    assertFalse(isRange("2-1"));
    assertFalse(isRange("bb-aa"));

    // Valid ranges
    assertTrue(isRange("I-II"));
    assertTrue(isRange("a-aa"));
    assertTrue(isRange("a-b"));
    assertTrue(isRange("a-baa"));
    assertTrue(isRange("aardvark - bat"));
    assertTrue(isRange("s2 - s10"));
    assertTrue(isRange("a-1 - b-1"));
    assertTrue(isRange("1-2-3 - 2-3-4"));
    // Individual non-numerical tokens are not compared
    assertTrue(isRange("s1-t4"));

    // Valid mixed-format ranges
    assertTrue(isRange("I-4"));
    assertTrue(isRange("1-IV"));

    // Identifiers with same casing and same length tokens either side of hyphen
    // will be considered topic ranges if possible, unless the token can be
    // taken as a Roman number. Note the following could make valid increasing
    // topic ranges (ignoring case).
    assertFalse(isRange("os-X"));
    assertFalse(isRange("OS-x"));
    assertFalse(isRange("os-x"));
    assertFalse(isRange("os-VI"));
    assertFalse(isRange("Os-vI"));
    assertTrue(isRange("os-vi"));

    // Currently we allow non-increasing ranges
    assertTrue(isRange("I-I"));
    assertTrue(isRange("a-a"));
    assertTrue(isRange("hello-hello"));
    assertTrue(isRange("1-1"));
    assertTrue(isRange("1-a -1-a"));
  }


  /**
   * Consecutive integer years must be truly consecutive integers.
   * No check is made for "year format".
   */
  public final void testAreYearsConsecutiveIntInt() {
    assertTrue(  BibliographicOrderScorer.areYearsConsecutive(2000,2001) );
    assertTrue(  BibliographicOrderScorer.areYearsConsecutive(2001,2002) );
    assertTrue(  BibliographicOrderScorer.areYearsConsecutive(-1,0) );

    assertFalse( BibliographicOrderScorer.areYearsConsecutive(2000,1999) );
    assertFalse(BibliographicOrderScorer.areYearsConsecutive(2000, 1990));
    assertFalse( BibliographicOrderScorer.areYearsConsecutive(2000,2003) );
    assertFalse( BibliographicOrderScorer.areYearsConsecutive(2001,1999) );
    assertFalse( BibliographicOrderScorer.areYearsConsecutive(2000,2000) );
    assertFalse( BibliographicOrderScorer.areYearsConsecutive(0,0) );
  }

  /**
   * Years must be parseable as numbers.
   */
  public final void testAreYearsConsecutiveStringString() {
    assertTrue(  BibliographicOrderScorer.areYearsConsecutive("2000", "2001") );
    assertTrue(BibliographicOrderScorer.areYearsConsecutive("2001", "2002"));
    assertTrue(BibliographicOrderScorer.areYearsConsecutive("-1", "0"));
    // Spaces will be trimmed
    assertTrue(  BibliographicOrderScorer.areYearsConsecutive(" 2000 ","     2001 ") );

    assertFalse(BibliographicOrderScorer.areYearsConsecutive("2000", "1999"));
    assertFalse( BibliographicOrderScorer.areYearsConsecutive("2000", "1990") );
    assertFalse(BibliographicOrderScorer.areYearsConsecutive("2000", "2003"));
    assertFalse(BibliographicOrderScorer.areYearsConsecutive("2001", "1999"));
    assertFalse( BibliographicOrderScorer.areYearsConsecutive("2000", "2000") );
    assertFalse( BibliographicOrderScorer.areYearsConsecutive("0","0") );

    // Check exceptions
    String[] unparseable1 = {"You can't parse this.",      "Year 2000",  "2000 year", " 2000 "};
    String[] unparseable2 = {"You can't parse this + 1.",  "Year 2001",  "2001 year", " 2 001 "};
    for (int i=0; i<unparseable1.length; i++) {
      try {
        String s1 = unparseable1[i];
        String s2 = unparseable2[i];
        BibliographicOrderScorer.areYearsConsecutive(s1, s2);
        fail(String.format("Should have thrown NumberFormatException parsing %s and %s.", s1, s2));
      } catch (NumberFormatException e) { /* do nothing */ }
    }
  }

  /**
   * Whether there is a positive gap greater than one between the end of one
   * range and the start of another. 
   */
  public final void testIsGapBetween() {
    assertTrue(  BibliographicOrderScorer.isGapBetween("2000", "2002"));
    assertFalse( BibliographicOrderScorer.isGapBetween("2000", "2001"));
    assertFalse( BibliographicOrderScorer.isGapBetween("2000", "2000"));
    assertFalse( BibliographicOrderScorer.isGapBetween("2000", "1999"));
    // ranges
    assertTrue(  BibliographicOrderScorer.isGapBetween("1999-2000", "2002"));
    assertFalse( BibliographicOrderScorer.isGapBetween("2000-2001", "2002"));
    assertTrue(  BibliographicOrderScorer.isGapBetween("2000", "2002-2003"));
    assertTrue(  BibliographicOrderScorer.isGapBetween("1999-2000", "2002 - 2009"));
    assertFalse( BibliographicOrderScorer.isGapBetween("1990-2000", "1980-1985"));
    // exception
    try {
      BibliographicOrderScorer.isGapBetween("1999 to 2000", "a string");
      fail("Should have produced a NumberFormatException.");
    } catch (NumberFormatException e) { /* ignore */ }
  }

  /**
   * A volume string is considered to be valid if it is not empty, and is equal
   * to neither zero nor a lone hyphen.
   */
  public final void testIsVolumeStringValid() {
    // False if empty, null, zero or hyphen
    assertFalse(BibliographicOrderScorer.isVolumeStringValid(null));
    assertFalse(BibliographicOrderScorer.isVolumeStringValid(""));
    assertFalse(BibliographicOrderScorer.isVolumeStringValid("   "));
    assertFalse(BibliographicOrderScorer.isVolumeStringValid("0"));
    assertFalse(BibliographicOrderScorer.isVolumeStringValid(" 0000  "));
    assertFalse(BibliographicOrderScorer.isVolumeStringValid(" - "));

    assertTrue(BibliographicOrderScorer.isVolumeStringValid("null"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("--"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("v0"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("volume one"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("a1-4"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid(" any-old--string-really 019 "));

    // Ranges are fine (even if invalid)
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("1-2"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("2-1"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("II-1"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("!-£"));
    assertTrue(BibliographicOrderScorer.isVolumeStringValid("bb-aa"));
  }

  /**
   * A pair of volume strings are considered to be valid if either one is valid.
   */
  public final void testAreVolumeStringsValid() {
    // False if both are null, empty, zero or hyphen
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("", ""));
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("0", "0"));
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("-", "-"));
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid(null, null));
    // Or combinations
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("", "0"));
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("", "-"));
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("", null));
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("0", "-"));
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("0", null));
    assertFalse(BibliographicOrderScorer.areVolumeStringsValid("-", null));

    // Any one valid volume is fine
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("", "1"));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("0", "1"));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("-", "1"));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid(null, "1"));

    // 2 valid volumes are fine
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("v0", "v0"));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("volume one", "volume two"));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("1", "2"));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("1", "one"));

    // Ranges are fine (even if invalid ranges)
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("II-1", null));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("II-I", null));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("1-two", null));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("2-1", null));
    assertTrue(BibliographicOrderScorer.areVolumeStringsValid("bb-aa", null));

  }

  /**
   * Consecutive integer volumes must be truly consecutive integers.
   */
  public final void testAreVolumesConsecutiveIntInt() {
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive(0,1)  );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive(1,2)  );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive(-1,0) );

    assertFalse( BibliographicOrderScorer.areVolumesConsecutive(0,-1) );
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive(2,1)  );
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive(1,3)  );
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive(1,-2) );
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive(1,1)  );
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive(0,0)  );
  }


  /**
   * Consecutive string volumes must be equal, except that the final numerical
   * token of each string must represent consecutive integers.
   * The method tests for strict consecutivity; no overlap or gaps.
   */
  public final void testAreVolumesConsecutiveStringString() {
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("Top 100 volumes, no 81!", "Top 100 volumes, no 82!") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("1", "2") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("2001", "2002") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("2 men in a boat", "3 men in a boat") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("The 10 commandments", "The 11 commandments") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("a", "b") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("aa", "ab") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("az", "ba") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("LIV", "LV") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("os-LIV", "os-LV") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("os-liv", "os-lv") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("os-IV", "os-V") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("os-iv", "os-v") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("os-4", "os-V") );

    // OUP's IEICE Transactions series
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("E89-C", "E90-C") );

    // The non-numerical string tokens are different
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive("1st volume", "2nd volume") );
    // The non-numerical string tokens are the same
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("4th volume", "5th volume") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("Das 01 volume", "Das 02 volume") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("Das 02 volume 2000", "Das 02 volume 2001") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("Volume s1-1", "Volume s1-2") );
    assertTrue(  BibliographicOrderScorer.areVolumesConsecutive("Volume s100-9", "Volume s100-10") );

    assertFalse( BibliographicOrderScorer.areVolumesConsecutive("2001", "2001") );
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive("Volume s100-1", "Volume s100-11") );
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive("Das 02 volume 2000", "Das 03 volume 2001") );
    assertFalse( BibliographicOrderScorer.areVolumesConsecutive("one", "two") );
  }

  /**
   * Consecutive duplicates must have equal vol, year, issn, title.
   */
  public final void testAreVolumesConsecutiveDuplicates() {
    // The Africa Today volumes are ordered in pairs of consecutive duplicates
    int numPairs = afrTod.size()/2;
    for (int i=0; i<=numPairs; i+=2) {
      BibliographicItem afrTod1 = afrTod.get(i);
      BibliographicItem afrTod2 = afrTod.get(i+1);
      assertTrue( BibliographicOrderScorer.areVolumesConsecutiveDuplicates(afrTod1, afrTod2) );
    }
    // OUP's "The Library..." has consecutive volumes with different formats
    assertTrue( BibliographicOrderScorer.areVolumesConsecutiveDuplicates(
        new BibliographicItemImpl()
            .setName("The Library")
            .setYear("1981")
            .setVolume("s6-3")
        ,
        new BibliographicItemImpl()
            .setName("The Library")
            .setYear("1981")
            .setVolume("s6-III")
    ));
  }

  /**
   * Values are increasing if they don't go down.
   */
  public final void testAreValuesIncreasingIntInt() {
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("0","1")  );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("1","2")  );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("-1","0") );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("1","3")  );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("1","1")  );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("0","0")  );

    assertFalse( BibliographicOrderScorer.areValuesIncreasing("0","-1") );
    assertFalse( BibliographicOrderScorer.areValuesIncreasing("2","1")  );
    assertFalse( BibliographicOrderScorer.areValuesIncreasing("1","-2") );
  }

  /**
   * Parsable integer values are increasing if they don't go down.
   */
  public final void testAreValuesIncreasingStringString() {
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("0","1")  );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("1","2")  );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("-1","0") );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("1","3")  );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("1","1")  );
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("0","0")  );

    assertFalse( BibliographicOrderScorer.areValuesIncreasing("0","-1") );
    assertFalse( BibliographicOrderScorer.areValuesIncreasing("2","1")  );
    assertFalse( BibliographicOrderScorer.areValuesIncreasing("1","-2") );

    // These strings do not parse as integers
    try {
      BibliographicOrderScorer.areValuesIncreasing("one","two");
      fail("Should have thrown NumberFormatException.");
    } catch (NumberFormatException e) { /* do nothing */ }
    // These do after trimming
    assertTrue(  BibliographicOrderScorer.areValuesIncreasing("  0  ","  1  ")  );
  }

  /**
   * Parsable integer values are decreasing if they do go down.
   */
  public final void testAreValuesDecreasingStringString() {
    assertTrue(  BibliographicOrderScorer.areValuesDecreasing("1","0")  );
    assertTrue(  BibliographicOrderScorer.areValuesDecreasing("2","1")  );
    assertTrue(  BibliographicOrderScorer.areValuesDecreasing("0","-1") );
    assertTrue(  BibliographicOrderScorer.areValuesDecreasing("3","1")  );
    assertTrue(  BibliographicOrderScorer.areValuesDecreasing("1","-2") );

    assertFalse( BibliographicOrderScorer.areValuesDecreasing("1","1")  );
    assertFalse( BibliographicOrderScorer.areValuesDecreasing("0","0")  );

    // These strings do not parse as integers
    try {
      BibliographicOrderScorer.areValuesDecreasing("one","two");
      fail("Should have thrown NumberFormatException.");
    } catch (NumberFormatException e) { /* do nothing */ }
    // These do after trimming
    assertTrue(  BibliographicOrderScorer.areValuesDecreasing("  2000  ","  200  ")  );
  }

  /**
   * Increasing integer volumes - second must be gretaer than or equal to the 
   * first.
   */
  public final void testAreVolumesIncreasingIntInt() {
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing(0,1)  );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing(1,2)  );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing(-1,0) );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing(1,3)  );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing(1,1)  );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing(0,0)  );

    assertFalse( BibliographicOrderScorer.areVolumesIncreasing(0,-1) );
    assertFalse( BibliographicOrderScorer.areVolumesIncreasing(2,1)  );
    assertFalse( BibliographicOrderScorer.areVolumesIncreasing(1,-2) );
  }

  /**
   * Increasing string volumes - second must be gretaer than or equal to the 
   * first.
   */
  public final void testAreVolumesIncreasingStringString() {
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("Top 100 volumes, no 81!", "Top 100 volumes, no 84!") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("1", "2") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("1", "II") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("I", "2") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("2001", "2002") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("2001", "2001") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("3 men in a boat", "8 men in a boat") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("The 10 commandments", "The 20 commandments") );

    // The non-numerical string tokens are different
    assertFalse( BibliographicOrderScorer.areVolumesIncreasing("1st volume", "3rd volume") );
    // The non-numerical string tokens are the same
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("4th volume", "8th volume") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("Das 01 volume", "Das 02 volume") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("Das 02 volume 2000", "Das 02 volume 2001") );

    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("Volume s1-1", "Volume s1-1") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("Volume s1-1", "Volume s1-4") );
    assertTrue(  BibliographicOrderScorer.areVolumesIncreasing("Volume s100-9", "Volume s100-10") );

    assertFalse( BibliographicOrderScorer.areVolumesIncreasing("Das 02 volume 2000", "Das 03 volume 2000") );
    assertFalse( BibliographicOrderScorer.areVolumesIncreasing("one", "two") );

    // This is a corner case; if the strings are identical they are 
    // regarded as generally increasing without needing to parse to integers
    assertTrue( BibliographicOrderScorer.areVolumesIncreasing("one","one") );
  }

  /**
   * A pair of BibliographicItems represent the same (extended) volume if they
   * have the same non-zero volume string along with appropriately sequential
   * year ranges.
   */
  public final void testAreExtendedVolume() {
    for (int i=0; i<textStressMicroVolPairs.size(); i+=2) {
      assertTrue(BibliographicOrderScorer.areExtendedVolume(
          textStressMicroVolPairs.get(i),
          textStressMicroVolPairs.get(i+1)
      ));
    }
  }

  /**
   * Two years are appropriately ordered if the second is not less than the 
   * first.
   */
  public final void testAreYearsIncreasingIntInt() {
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(-32,  1889) );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(0,    2001) );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(2000, 2001) );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(2001, 2002) );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(1999, 2000) );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(2001, 2003) );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(2001, 2001) );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(2000, 2000) );

    assertFalse( BibliographicOrderScorer.areYearsIncreasing(2000, 1999) );
    assertFalse( BibliographicOrderScorer.areYearsIncreasing(2002, 2001) );
    assertFalse( BibliographicOrderScorer.areYearsIncreasing(2001, 1997) );
  }

  /**
   * Years must be parseable as numbers, including Roman.
   */
  public final void testAreYearsIncreasingStringString() {
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing("-32",  "1889") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing("0",    "2001") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing("2000", "2001") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing("2001", "2002") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing("1999", "2000") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing("2001", "2003") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing("2001", "2001") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing("2000", "2000") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(" 999 ", " 3001 ") );
    assertTrue(  BibliographicOrderScorer.areYearsIncreasing(" V ", " IX ") );

    assertFalse( BibliographicOrderScorer.areYearsIncreasing("2000", "1999") );
    assertFalse( BibliographicOrderScorer.areYearsIncreasing("2002", "2001") );
    assertFalse( BibliographicOrderScorer.areYearsIncreasing("2001", "1997") );

    // Check exceptions
    String[] unparseable1 = {"You can't parse this.",     "Year 2000",  "2000 year",  " 999 " };
    String[] unparseable2 = {"You can't parse this + 1.", "Year 2001",  "3001 year",  " 3 001 "};
    for (int i=0; i<unparseable1.length; i++) {
      try {
        String s1 = unparseable1[i];
        String s2 = unparseable2[i];
        BibliographicOrderScorer.areYearsIncreasing(s1, s2);
        fail(String.format("Should have thrown NumberFormatException parsing %s and %s.", s1, s2));
      } catch (NumberFormatException e) { /* do nothing */ }
    }
  }


  /**
   * A pair of year ranges are considered to be <i>appropriately consecutive</i>
   * if the second start year is greater than or equal to the first start year
   * while being at most one greater than the first end year (i.e. the start of 
   * the second range follows or is simultaneous with or is within the first 
   * range) or if the first start year is included in the range of the second 
   * (i.e. start of first is within second). 
   * <br/>
   * <i>Note: it is not necessary that the first range's end year is less than 
   * or equal to the second range's end year. This may be enforced later.</i>
   */
  public final void testAreYearRangesAppropriatelyConsecutive() {
    // second start >= first start
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1976") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1977") );

    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1977") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1981") );

    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976-1977") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976-1981") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1978-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1979", "1978-1980") );

    // first start included in second range
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1962", "1961-1962") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1976-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1977", "1976-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1980", "1976-1980") );

    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1975-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1975-1976") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1981", "1972-1980") );

    // Neither condition
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1970-1975") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1970-1975") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1970") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1970") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1978") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1982") );

    // Check exception
    try {
      BibliographicOrderScorer.areYearRangesAppropriatelyConsecutive("1999 to 2000", "a string");
      fail("Should have thrown NumberFormatException.");
    } catch (NumberFormatException e) { /* do nothing */ }
  }

  /**
   * A pair of year ranges are considered to be <i>appropriately sequenced</i>
   * if the second start year is greater than or equal to the first start year
   * while being at most one greater than the first end year (i.e. the start of 
   * the second range follows or is simultaneous with or is within the first 
   * range) or if the first start year is included in the range of the second 
   * (i.e. start of first is within second). 
   * <br/>
   * <i>Note: it is not necessary that the first range's end year is less than 
   * or equal to the second range's end year. This may be enforced later.</i>
   */
  public final void testAreYearRangesAppropriatelySequenced() {
    // second start >= first start
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976", "1976") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976", "1977") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976", "1978") );

    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1976") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1977") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1983") );

    // Second contained in first with coincident start 
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1976-1977") );
    // Second contained in first with coincident end
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1978-1980") );
    // Same range
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1976-1980") );
    // Overlap with second finishing later; coincident start
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1976-1981") );
    // Overlap with second starting within first and finishing after
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1979", "1978-1980") );
    // Second completely after first
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1979", "1981-1982") );

    // First start included in second range - start, middle, end
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976", "1976-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1977", "1976-1980") );
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1980", "1976-1980") );

    // Overlap, second starts before first, coincident ends
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1975-1980") );
    // Overlap, second ends when first starts
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1975-1976") );
    // Overlap, second starts before first, and ends within first
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1981", "1972-1980") );

    // Containment - second in first
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1981", "1977-1980") );
    // Containment - first in second
    assertTrue( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1977-1980", "1976-1981") );

    // Neither condition
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976", "1970-1975") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1970-1975") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1970") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976", "1970") );
    assertFalse( BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1972") );

    // Check exception
    try {
      BibliographicOrderScorer.areYearRangesAppropriatelySequenced("1999 to 2000", "a string");
      fail("Should have thrown NumberFormatException.");
    } catch (NumberFormatException e) { /* do nothing */ }
  }

  /**
   * This method just aggregates countProportionOfBreaksInRange().
   */
  public final void testCountProportionOfBreaks() {
    //fail("Not yet implemented");
  }

  /**
   * This method just aggregates countProportionOfRedundancyInRange().
   */
  public final void testCountProportionOfRedundancy() {
    //fail("Not yet implemented");
  }

  /**
   * The proportion of value pairs in the range that have a break 
   * between them. Note that the definition of a break in years is
   * informed by areAppropriatelyConsecutive().
   */
  public final void testCountProportionOfBreaksInRange() {

    // tang
    checkCountProportionOfBreaksInRange(VOL, tang, 3, 2);
    checkCountProportionOfBreaksInRange(YR,  tang, 3, 0);

    // oxEcPap
    checkCountProportionOfBreaksInRange(VOL, oxEcPap, 1, 3);
    checkCountProportionOfBreaksInRange(YR,  oxEcPap, 1, 2);

    // mind
    checkCountProportionOfBreaksInRange(VOL, mind, 1 , 1);
    checkCountProportionOfBreaksInRange(YR,  mind, 1, 0);

    // euroBusRev
    checkCountProportionOfBreaksInRange(VOL, euroBusRev, 1, 1);
    checkCountProportionOfBreaksInRange(YR,  euroBusRev, 1, 0);

    // nutFoodSci
    checkCountProportionOfBreaksInRange(VOL, nutFoodSci, 1, 1);
    checkCountProportionOfBreaksInRange(YR,  nutFoodSci, 1, 0);

    // intlJournHumArtsComp
    checkCountProportionOfBreaksInRange(VOL, intlJournHumArtsComp, 0, 1);
    checkCountProportionOfBreaksInRange(YR,  intlJournHumArtsComp, 1, 1);

    // expAstr
    checkCountProportionOfBreaksInRange(VOL, expAstr, 0, 0);
    checkCountProportionOfBreaksInRange(YR,  expAstr, 2, 0);

    // analChem
    checkCountProportionOfBreaksInRange(VOL, analChem, 0, 0);
    checkCountProportionOfBreaksInRange(YR,  analChem, 2, 0);

    // journEndoc
    checkCountProportionOfBreaksInRange(VOL, journEndoc, 0, 0);
    checkCountProportionOfBreaksInRange(YR,  journEndoc, 3, 0);

    // yorkGeoSoc
    checkCountProportionOfBreaksInRange(VOL, yorkGeoSoc, 0, 1);
    checkCountProportionOfBreaksInRange(YR,  yorkGeoSoc, 3, 1);

    // commDis
    checkCountProportionOfBreaksInRange(VOL, commDis, 0, 0);
    checkCountProportionOfBreaksInRange(YR,  commDis, 2, 0);

    // geoSocLonMem
    checkCountProportionOfBreaksInRange(VOL, geoSocLonMem, 0, 2);
    checkCountProportionOfBreaksInRange(YR,  geoSocLonMem, 2, 1);

    // geoSocLonSP
    checkCountProportionOfBreaksInRange(VOL, geoSocLonSP, 0, 1);
    checkCountProportionOfBreaksInRange(YR,  geoSocLonSP, 2, 0);

    // afrTod
    checkCountProportionOfBreaksInRange(VOL, afrTod, 0, 0);
    checkCountProportionOfBreaksInRange(YR,  afrTod, 0, 0);

    // laserChem
    checkCountProportionOfBreaksInRange(VOL, laserChem, 0, 0);
    checkCountProportionOfBreaksInRange(YR,  laserChem, 0, 0);

    // aaa
    checkCountProportionOfBreaksInRange(VOL, aaa, 1, 0);
    checkCountProportionOfBreaksInRange(YR,  aaa, 1, 0);

    // tumVir
    // Due to the lack of volume values, all pairs will be considered to have
    // breaks between them
    checkCountProportionOfBreaksInRange(VOL, tumVir, 1, 1);
    checkCountProportionOfBreaksInRange(YR,  tumVir, 1, 1);

  }

  /**
   * The proportion of year pairs in the range that have a break 
   * between them that occurs uniquely in the year sequence and not the volume 
   * sequence.
   */
  public final void testCountProportionOfUniquelyYearBreaks() {
    checkCountProportionOfUniquelyYearBreaks(tang, 0);
    checkCountProportionOfUniquelyYearBreaks(oxEcPap, 2);
    checkCountProportionOfUniquelyYearBreaks(mind, 0);
    checkCountProportionOfUniquelyYearBreaks(euroBusRev, 0);
    checkCountProportionOfUniquelyYearBreaks(nutFoodSci, 0);
    checkCountProportionOfUniquelyYearBreaks(intlJournHumArtsComp, 0);
    checkCountProportionOfUniquelyYearBreaks(expAstr, 0);
    checkCountProportionOfUniquelyYearBreaks(analChem, 0);
    checkCountProportionOfUniquelyYearBreaks(journEndoc, 0);
    checkCountProportionOfUniquelyYearBreaks(yorkGeoSoc, 0);
    checkCountProportionOfUniquelyYearBreaks(commDis, 0);
    checkCountProportionOfUniquelyYearBreaks(geoSocLonMem, 0);
    checkCountProportionOfUniquelyYearBreaks(geoSocLonSP, 0);
    checkCountProportionOfUniquelyYearBreaks(afrTod, 0);
    checkCountProportionOfUniquelyYearBreaks(laserChem, 0);
    checkCountProportionOfUniquelyYearBreaks(aaa, 0);
    checkCountProportionOfUniquelyYearBreaks(tumVir, 1);
  }

  /**
   * The proportion of value pairs in the range that have a descending break 
   * between them. 
   */
  public final void testCountProportionOfNegativeBreaksInRange() {

    // tang
    checkCountProportionOfNegativeBreaksInRange(VOL, tang, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  tang, 1);

    // oxEcPap
    checkCountProportionOfNegativeBreaksInRange(VOL, oxEcPap, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  oxEcPap, 0);

    // mind
    // 1 negative break due to the vol id changing os-N -> N
    checkCountProportionOfNegativeBreaksInRange(VOL, mind, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  mind, 0);

    // euroBusRev
    checkCountProportionOfNegativeBreaksInRange(VOL, euroBusRev, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  euroBusRev, 1);

    // nutFoodSci
    checkCountProportionOfNegativeBreaksInRange(VOL, nutFoodSci, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  nutFoodSci, 1);

    // intlJournHumArtsComp
    checkCountProportionOfNegativeBreaksInRange(VOL, intlJournHumArtsComp, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  intlJournHumArtsComp, 1);

    // expAstr
    checkCountProportionOfNegativeBreaksInRange(VOL, expAstr, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  expAstr, 1);

    // analChem
    checkCountProportionOfNegativeBreaksInRange(VOL, analChem, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  analChem, 1);

    // journEndoc
    checkCountProportionOfNegativeBreaksInRange(VOL, journEndoc, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  journEndoc, 1);

    // yorkGeoSoc
    checkCountProportionOfNegativeBreaksInRange(VOL, yorkGeoSoc, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  yorkGeoSoc, 1);

    // commDis
    checkCountProportionOfNegativeBreaksInRange(VOL, commDis, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  commDis, 1);

    // geoSocLonMem
    checkCountProportionOfNegativeBreaksInRange(VOL, geoSocLonMem, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  geoSocLonMem, 1);

    // geoSocLonSP
    checkCountProportionOfNegativeBreaksInRange(VOL, geoSocLonSP, 1);
    checkCountProportionOfNegativeBreaksInRange(YR,  geoSocLonSP, 1);

    // afrTod
    checkCountProportionOfNegativeBreaksInRange(VOL, afrTod, 0);
    checkCountProportionOfNegativeBreaksInRange(YR,  afrTod, 0);

    // laserChem
    checkCountProportionOfNegativeBreaksInRange(VOL, laserChem, 0);
    checkCountProportionOfNegativeBreaksInRange(YR,  laserChem, 0);

    // aaa
    checkCountProportionOfNegativeBreaksInRange(VOL, aaa, 0);
    checkCountProportionOfNegativeBreaksInRange(YR,  aaa, 0);

    // tumVir
    // Due to the lack of volume values, all vol pairs will be considered to have
    // breaks between them (method tests the "other" field)
    checkCountProportionOfNegativeBreaksInRange(VOL, tumVir, 0);
    checkCountProportionOfNegativeBreaksInRange(YR,  tumVir, 1);

  }

  /**
   * How much redundancy there is in the values of the range; that is, the 
   * proportion of values which are repeated. 
   */
  public final void testCountProportionOfRedundancyInRange() {

    // tang
    checkCountProportionOfRedundancyInRange(VOL, tang, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  tang, 0, 0);

    // oxEcPap
    checkCountProportionOfRedundancyInRange(VOL, oxEcPap, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  oxEcPap, 0, 0);

    // mind
    checkCountProportionOfRedundancyInRange(VOL, mind, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  mind, 0, 0);

    // euroBusRev
    checkCountProportionOfRedundancyInRange(VOL, euroBusRev, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  euroBusRev, 0, 0);

    // nutFoodSci
    checkCountProportionOfRedundancyInRange(VOL, nutFoodSci, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  nutFoodSci, 0, 0);

    // intlJournHumArtsComp
    checkCountProportionOfRedundancyInRange(VOL, intlJournHumArtsComp, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  intlJournHumArtsComp, 0, 0);

    // expAstr
    checkCountProportionOfRedundancyInRange(VOL, expAstr, 0, 1);
    checkCountProportionOfRedundancyInRange(YR,  expAstr, 0, 0);

    // analChem
    checkCountProportionOfRedundancyInRange(VOL, analChem, 0, 1);
    checkCountProportionOfRedundancyInRange(YR,  analChem, 0, 0);

    // journEndoc
    checkCountProportionOfRedundancyInRange(VOL, journEndoc, 0, 1);
    checkCountProportionOfRedundancyInRange(YR,  journEndoc, 0, 0);

    // yorkGeoSoc
    checkCountProportionOfRedundancyInRange(VOL, yorkGeoSoc, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  yorkGeoSoc, 0, 0);

    // commDis
    checkCountProportionOfRedundancyInRange(VOL, commDis, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  commDis, 0, 0);

    // geoSocLonMem
    checkCountProportionOfRedundancyInRange(VOL, geoSocLonMem, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  geoSocLonMem, 0, 0);

    // geoSocLonSP
    checkCountProportionOfRedundancyInRange(VOL, geoSocLonSP, 0, 1);
    checkCountProportionOfRedundancyInRange(YR,  geoSocLonSP, 0, 0);

    // afrTod
    checkCountProportionOfRedundancyInRange(VOL, afrTod, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  afrTod, 0, 0);
    // (no redundancy expected with new definition - consecutive duplicates allowed)

    // laserChem
    checkCountProportionOfRedundancyInRange(VOL, laserChem, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  laserChem, 0, 0);

    // aaa
    checkCountProportionOfRedundancyInRange(VOL, aaa, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  aaa, 0, 0);

    // tumVir
    // Volumes will get a high score as there no values
    checkCountProportionOfRedundancyInRange(VOL, tumVir, 1, 0);
    checkCountProportionOfRedundancyInRange(YR,  tumVir, 1, 0);

  }

  /**
   * All journals should display monotonically increasing ranges on the 
   * sorted fields. Note that ranges should be produced where the format 
   * of the field changes.
   */
  public final void testIsMonotonicallyIncreasing() {
    for (List<BibliographicItem> aus : problemTitles) {
      assertMonotonicIncreaseOnTheSortedField(aus);
    }
    // Test AAA - monotonically increasing on both fields
    assertTrue(BibliographicOrderScorer.isMonotonicallyIncreasing(aaa, YR));
    assertTrue(BibliographicOrderScorer.isMonotonicallyIncreasing(aaa, VOL));

    // Test TumVir - monotonically increasing on year but not empty volumes
    assertTrue(BibliographicOrderScorer.isMonotonicallyIncreasing(tumVir, YR));
    assertFalse(BibliographicOrderScorer.isMonotonicallyIncreasing(tumVir, VOL));
  }

  /**
   * Returns true if one string contains only digits while the other is not
   * parseable as a number.
   */
  public final void testChangeOfFormats() {
    // The consistent list should not display any format changes
    assertFalse( containsChangeOfFormats(fullyConsistentAus, VOL) );
    assertFalse( containsChangeOfFormats(fullyConsistentAus, YR) );

    // The following are currently false but we might want to do some proper
    // regular expression work to find and rate string commonalities, or
    // use something like Levenstein distance.
    assertFalse( changeOfFormats("s1-1", "volume 8") );
    assertFalse( changeOfFormats("99", "ill") );
    // Example of consecutive volume values taken from AAA
    assertFalse( changeOfFormats("7", "2003") );
    // Test AAA - There is no recognisable change of formats on either field
    assertFalse(containsChangeOfFormats(aaa, YR));
    assertFalse(containsChangeOfFormats(aaa, VOL));
  }

  /**
   * The year ordering should provide a better consistency score for years, or 
   * the same for a fully consistent ordering. 
   */
  public final void testGetYearListConsistency() {
    // The year ordering should provide a better consistency score for years
    for (List<BibliographicItem> aus : problemTitles) {
      //System.out.println("Testing getYearListConsistency() on "+aus.get(0).getName());
      assertYearListConsistencyGreaterWhenOrderingByYearFirst(aus);
    }

    // Except for a fully consistent sequence
    for (List<BibliographicItem> aus : consistentSequences) {
      orderVolYear(aus);
      float vyCon = BibliographicOrderScorer.getYearListConsistency(aus);
      orderYearVol(aus);
      float yvCon = BibliographicOrderScorer.getYearListConsistency(aus);
      assertEquals( vyCon, yvCon );
      assertEquals( 1f, vyCon );
      assertEquals( 1f, yvCon );
    }
  }

  /**
   * The volume ordering should provide a better consistency score for volumes, or 
   * the same for a fully consistent ordering. 
   */
  public final void testGetVolumeListConsistency() {
    // The volume ordering should provide a better consistency score for volumes
    for (List<BibliographicItem> aus : problemTitles) {
      //System.out.println("Testing getVolumeListConsistency() on "+aus.get(0).getName());
      assertVolumeListConsistencyGreaterWhenOrderingByVolumeFirst(aus);
    }

    // Except for a fully consistent sequence
    for (List<BibliographicItem> aus : consistentSequences) {
      orderVolYear(aus);
      float vyCon = BibliographicOrderScorer.getVolumeListConsistency(aus);
      orderYearVol(aus);
      float yvCon = BibliographicOrderScorer.getVolumeListConsistency(aus);
      assertEquals( vyCon, yvCon );
      assertEquals( 1f, vyCon );
      assertEquals( 1f, yvCon );
    }
  }

  /**
   * Get a consistency score for years, based upon a list of ranges which were
   * calculated based on a particular ordering.
   * <p>
   * Each range is scored, but we also look at the size and direction of gaps 
   * between ranges. 
   */
  public final void testGetYearRangeConsistency() {
    // Compare scores from year-ordered and volume-ordered ranges
    for (int i=0; i<allYearRanges.size(); i++) {
      float volScore  = BibliographicOrderScorer.getYearRangeConsistency(allVolRanges.get(i));
      float yearScore = BibliographicOrderScorer.getYearRangeConsistency(allYearRanges.get(i));
      String title = allYearRanges.get(i).get(0).items.get(0).getName();
      //System.out.format("%s yearScore %s volScore %s\n", title, yearScore, volScore);
      assertTrue(yearScore > 0);
      assertTrue(yearScore > volScore);
    }
    // Consistent sequence - single range
    for (List<BibliographicItem> aus : consistentSequences) {
      List<TitleRange> ranges = Arrays.asList(new TitleRange(aus));
      orderVolYear(aus);
      float volScore =  BibliographicOrderScorer.getYearRangeConsistency(ranges);
      orderYearVol(aus);
      float yearScore = BibliographicOrderScorer.getYearRangeConsistency(ranges);
      // Should all be equal to 1
      assertEquals(yearScore, volScore);
      assertEquals(1f, yearScore);
      assertEquals(1f, volScore);
    }
  }

  /**
   * Get a consistency score for volumes, based upon a list of ranges which were
   * calculated based on a particular ordering.
   */
  public final void testGetVolumeRangeConsistency() {
    // Compare scores from year-ordered and volume-ordered ranges
    for (int i=0; i<allYearRanges.size(); i++) {
      float yearScore = BibliographicOrderScorer.getVolumeRangeConsistency(allYearRanges.get(i));
      float volScore  = BibliographicOrderScorer.getVolumeRangeConsistency(allVolRanges.get(i));
      assertTrue(volScore > 0);
      assertTrue(volScore > yearScore);
    }
    // Consistent sequence - single range
    for (List<BibliographicItem> aus : consistentSequences) {
      List<TitleRange> ranges = Arrays.asList(new TitleRange(aus));
      orderVolYear(aus);
      float volScore =  BibliographicOrderScorer.getVolumeRangeConsistency(ranges);
      orderYearVol(aus);
      float yearScore = BibliographicOrderScorer.getVolumeRangeConsistency(ranges);
      // Should all be equal to 1
      assertEquals(yearScore, volScore);
      assertEquals(1f, yearScore);
      assertEquals(1f, volScore);
    }
  }

  /**
   * The more frequently coverage gaps occur, the lower the score. 
   */
  public final void testGetCoverageGapFrequencyDiscount() {
    // If there are as many ranges as AUs (>1), there should be a large discount
    assertEquals(0.8f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(5f, 5f));
    assertEquals(0.5f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(2f, 2f));
    assertEquals(0.98f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(50f, 50f));
    // All such results should be >= 0.5
    for (float i : new float[]{16, 50, 500, 1000000}) {
      assertTrue(BibliographicOrderScorer.getCoverageGapFrequencyDiscount(i, i) >= 0.5);
    }

    // If there are no AUs, there should be no discount
    assertEquals(0f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(0f, 0f));
    assertEquals(0f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(0f, 1f));

    // If there is a single range, there should be no discount
    assertEquals(0f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(5f, 1f));
    assertEquals(0f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(1f, 1f));
    assertEquals(0f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(500f, 1f));

    // If there are half as many ranges as AUs, the discount should be < 0.5  
    assertEquals(0.25f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(4f, 2f));
    assertEquals(0.375f, BibliographicOrderScorer.getCoverageGapFrequencyDiscount(8f, 4f));
    for (int i : new int[]{16, 50, 500, 1000000}) {
      assertTrue(BibliographicOrderScorer.getCoverageGapFrequencyDiscount((float)i,
          (float)(i/2)) < 0.5);
    }
  }

  /**
   * Test that we get better consistency scores on the field that was used for
   * ordering. This is essentially a repeat of the tests of
   * get[Year|Volume]RangeConsistency.
   * Also test that scores are btw 0 and 1.
   */
  public final void testGetConsistencyScore() {
    for (int i=0; i<allYearRanges.size(); i++) {
      List<BibliographicItem> aus = problemTitles.get(i);
      // Consistency score based on volume ordering
      orderVolYear(aus);
      ConsistencyScore csVol = BibliographicOrderScorer.getConsistencyScore(aus, allVolRanges.get(i));
      ConsistencyScore csVolOld  =
          BibliographicOrderScorer.getConsistencyScoreOld(aus, allVolRanges.get(i));
      // Consistency score based on year ordering
      orderYearVol(aus);
      ConsistencyScore csYear = BibliographicOrderScorer.getConsistencyScore(aus, allYearRanges.get(i));
      ConsistencyScore csYearOld  =
          BibliographicOrderScorer.getConsistencyScoreOld(aus, allYearRanges.get(i));
      // Scores should be better for the field which was used in ordering
      assertTrue(csYearOld.yearScore > csVolOld.yearScore);
      assertTrue(csVolOld.volScore > csYearOld.volScore);
      assertEquals(csYearOld.volScore, csYear.volScore);
      assertEquals(csYearOld.yearScore, csYear.yearScore);
      assertEquals(csVolOld.volScore, csVol.volScore);
      assertEquals(csVolOld.yearScore, csVol.yearScore);
      assertTrue(csYear.yearScore > csVol.yearScore);
      assertTrue(csVol.volScore > csYear.volScore);
      //assertTrue(csVol.volListScore > csYear.yearListScore);
      // Consistency scores should fall between 0 and 1 inclusive
      assertValidProportion(csVolOld.score);
      assertValidProportion(csVol.score);
      assertValidProportion(csYearOld.score);
      assertValidProportion(csYear.score);
    }
    // Consistent sequence - single range
    List<TitleRange> ranges = Arrays.asList(new TitleRange(fullyConsistentAus));
    orderVolYear(fullyConsistentAus);
    ConsistencyScore csVol  = BibliographicOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    ConsistencyScore csVolOld  =
        BibliographicOrderScorer.getConsistencyScoreOld(fullyConsistentAus, ranges);
    orderYearVol(fullyConsistentAus);
    ConsistencyScore csYear = BibliographicOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    ConsistencyScore csYearOld =
        BibliographicOrderScorer.getConsistencyScoreOld(fullyConsistentAus, ranges);
    // Should all be equal to 1
    assertEquals(csVol.yearScore, csVol.volScore);
    assertEquals(1f, csVol.yearScore);
    assertEquals(1f, csVol.volScore);
    assertEquals(csYear.yearScore, csYear.volScore);
    assertEquals(1f, csYear.yearScore);
    assertEquals(1f, csYear.volScore);

    // Compare the old and new consistency scores
    assertEquals(csVol.yearScore, csVolOld.yearScore);
    assertEquals(csVol.volScore, csVolOld.volScore);
    assertEquals(csYear.yearScore, csYearOld.yearScore);
    assertEquals(csYear.volScore, csYearOld.volScore);
  }

  /**
   *
   */
  public final void testCalculateRelativeBenefitAndLoss() {
    for (int i=0; i<allYearRanges.size(); i++) {
      List<BibliographicItem> aus = problemTitles.get(i);
      String title = allYearRanges.get(i).get(0).items.get(0).getName();
      // Consistency score based on volume ordering
      orderVolYear(aus);
      ConsistencyScore csVol = BibliographicOrderScorer.getConsistencyScore(aus,
          allVolRanges.get(i));
      // Consistency score based on year ordering
      orderYearVol(aus);
      ConsistencyScore csYear = BibliographicOrderScorer.getConsistencyScore(aus,
          allYearRanges.get(i));

      // Calculate relative volume benefit for each score
      float rvbVolYear =
          BibliographicOrderScorer.calculateRelativeBenefitToVolume(csVol, csYear);
      float rvbYearVol =
          BibliographicOrderScorer.calculateRelativeBenefitToVolume(csYear, csVol);
      // Calculate relative year loss for each score
      float rylVolYear =
          BibliographicOrderScorer.calculateRelativeLossToYear(csVol, csYear);
      float rylYearVol =
          BibliographicOrderScorer.calculateRelativeLossToYear(csYear, csVol);

      // Relative benefits should be negations of one another
      assertEquals(rvbVolYear, rvbYearVol == 0 ? rvbYearVol : -rvbYearVol);
      // Relative losses should be negations of one another
      assertEquals(rylVolYear, rylYearVol==0 ? rylYearVol : -rylYearVol);

      // The relative benefits and losses should not be zero for the example cases
      assertTrue(rvbVolYear != 0f);
      assertTrue(rvbYearVol != 0f);
      assertTrue(rylVolYear != 0f);
      assertTrue(rylYearVol != 0f);
    }

    // Consistent sequence - single range
    List<TitleRange> ranges = Arrays.asList(new TitleRange(fullyConsistentAus));
    orderVolYear(fullyConsistentAus);
    ConsistencyScore csVol  = BibliographicOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    orderYearVol(fullyConsistentAus);
    ConsistencyScore csYear = BibliographicOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    // Calculate relative volume benefit for each score
    float rvbVolYear =
        BibliographicOrderScorer.calculateRelativeBenefitToVolume(csVol, csYear);
    float rvbYearVol =
        BibliographicOrderScorer.calculateRelativeBenefitToVolume(csYear, csVol);
    // Calculate relative year loss for each score
    float rylVolYear =
        BibliographicOrderScorer.calculateRelativeLossToYear(csVol, csYear);
    float rylYearVol =
        BibliographicOrderScorer.calculateRelativeLossToYear(csYear, csVol);

    // Relative benefits should be negations of one another
    assertEquals(rvbVolYear, rvbYearVol == 0 ? rvbYearVol : -rvbYearVol);
    // Relative losses should be negations of one another
    assertEquals(rylVolYear, rylYearVol==0 ? rylYearVol : -rylYearVol);

    // Should all be equal to 0
    assertEquals(0f, rvbVolYear);
    assertEquals(0f, rvbYearVol);
    assertEquals(0f, rylVolYear);
    assertEquals(0f, rylYearVol);
  }

  /**
   * Test simply whether volume or year is preferred in line with expectations.
   */
  public final void testPreferVolume() {
    for (int i=0; i<allYearRanges.size(); i++) {
      List<BibliographicItem> aus = problemTitles.get(i);
      String title = allYearRanges.get(i).get(0).items.get(0).getName();

      // Order by volume/year and get a score
      orderVolYear(aus);
      ConsistencyScore csVol = BibliographicOrderScorer.getConsistencyScore(aus, allVolRanges.get(i));
      ConsistencyScore csVolOld = BibliographicOrderScorer.getConsistencyScoreOld(aus, allVolRanges.get(i));
      // Order by year/volume and get a score
      orderYearVol(aus);
      ConsistencyScore csYear = BibliographicOrderScorer.getConsistencyScore(aus, allYearRanges.get(i));
      ConsistencyScore csYearOld = BibliographicOrderScorer.getConsistencyScoreOld(aus, allYearRanges.get(i));

      assertEquals(csVolOld, csVol);
      assertEquals(csYearOld, csYear);

      // Do these scores lead us to prefer volume?
      boolean preferVolume = BibliographicOrderScorer.preferVolume(csVol, csYear);

      // Ensure the preference is in the right direction
      if (titlesToOrderByVolume.contains(i)) {
        assertTrue(title+" should prefer volume", preferVolume);
      } else if (titlesToOrderByYear.contains(i)) {
        assertTrue(title+" should prefer year", !preferVolume);
      } else {
        // Indices that do not appear in either of these lists can be ordered either way 
      }
    }

    // Consistent sequence - single range
    for (List<BibliographicItem> aus : consistentSequences) {
      List<TitleRange> ranges = Arrays.asList(new TitleRange(aus));
      orderVolYear(aus);
      ConsistencyScore csVol  = BibliographicOrderScorer.getConsistencyScore(aus, ranges);
      orderYearVol(aus);
      ConsistencyScore csYear = BibliographicOrderScorer.getConsistencyScore(aus, ranges);
      boolean preferVolume = BibliographicOrderScorer.preferVolume(csVol, csYear);
      assertTrue(preferVolume);
    }

    // Test AAA - prefer year as it is more consistent
    orderVolYear(aaa);
    ConsistencyScore csVol = BibliographicOrderScorer.getConsistencyScore(aaa, Arrays.asList(new TitleRange(aaa)));
    orderYearVol(aaa);
    ConsistencyScore csYear = BibliographicOrderScorer.getConsistencyScore(aaa, Arrays.asList(new TitleRange(aaa)));
    // Vol scoring should show years to be better
    assertTrue(csVol.yearsAreFullyConsistent());
    assertFalse(csVol.volumesAreFullyConsistent());
    assertTrue(csVol.yearScoresAreBetter());
    // Year scoring should show years to be better
    assertTrue(csYear.yearsAreFullyConsistent());
    assertFalse(csYear.volumesAreFullyConsistent());
    assertTrue(csYear.yearScoresAreBetter());
    // Therefore year should be preferred
    assertFalse(
        String.format("AAA should prefer Year to Vol:\n  %s\n  %s", csVol, csYear),
        BibliographicOrderScorer.preferVolume(csVol, csYear)
    );

    // Test TumVir - prefer year as volumes are empty!
    List<BibliographicItem> myTumVir = new ArrayList<BibliographicItem>();
    myTumVir.addAll(tumVir);
    myTumVir.add(
        new BibliographicItemImpl()
            .setName("Advances in Tumor Virology Volume 3")
            .setYear("2012")
    );
    tumVir = myTumVir;

    orderVolYear(tumVir);
    csVol = BibliographicOrderScorer.getConsistencyScore(tumVir, Arrays.asList(new TitleRange(tumVir)));
    orderYearVol(tumVir);
    csYear = BibliographicOrderScorer.getConsistencyScore(tumVir, Arrays.asList(new TitleRange(tumVir)));
    // Year should be preferred due to missing volume values
    assertFalse(
        String.format("TumVir should prefer Year to Vol:\n  %s\n  %s", csVol, csYear),
        BibliographicOrderScorer.preferVolume(csVol, csYear)
    );
  }

  /**
   * Ensure that the new combined iteration methods yield the same results as
   * the original methods.
   */
  public final void testCombinedMethods() {
    BibliographicOrderScorer.Score score;
    float v, y;
    // For each list of lists of BibItems (problemTitles, consistentSequences)
    for (List<List<BibliographicItem>> list :
        new ArrayList<List<List<BibliographicItem>>>() {{
          add(problemTitles);
          add(consistentSequences);
        }}
        ) {
      // For each list of BibItems
      for (List<BibliographicItem> items : list) {
        // For vol/yr, yr/vol orderings
        boolean vy=false;
        for (int i=0; i<2; i++) {
          vy = !vy;
          if (vy) orderVolYear(items);
          else orderYearVol(items);
          // countProportionOfBreaksInRange
          checkVolYearScores(
              BibliographicOrderScorer.countProportionOfBreaksInRange(items),
              BibliographicOrderScorer.countProportionOfBreaksInRange(items, SORT_FIELD.VOLUME),
              BibliographicOrderScorer.countProportionOfBreaksInRange(items, SORT_FIELD.YEAR)
          );
          // countProportionOfNegativeBreaksInRange
          checkVolYearScores(
              BibliographicOrderScorer.countProportionOfNegativeBreaksInRange(items),
              BibliographicOrderScorer.countProportionOfNegativeBreaksInRange(items, SORT_FIELD.VOLUME),
              BibliographicOrderScorer.countProportionOfNegativeBreaksInRange(items, SORT_FIELD.YEAR)
          );
          // countProportionOfRedundancyInRange
          checkVolYearScores(
              BibliographicOrderScorer.countProportionOfRedundancyInRange(items),
              BibliographicOrderScorer.countProportionOfRedundancyInRange(items, SORT_FIELD.VOLUME),
              BibliographicOrderScorer.countProportionOfRedundancyInRange(items, SORT_FIELD.YEAR)
          );
        }
      }
    }
    // For each list of lists of TitleRanges (allVolRanges, allYearRanges)
    for (List<List<TitleRange>> list : new ArrayList<List<List<TitleRange>>>() {{
      add(allVolRanges);
      add(allYearRanges);
    }}) {
      // For each list of TitleRanges, compare the scores for both shuffled and unshuffled
      for (List<TitleRange> items : list) {
        for (int i=0; i<2; i++) {
          // Shuffle the list secon time through
          if (i==1) Collections.shuffle(list);
          // getRangeConsistency uses the combined methods and combines scores previously calculated separately
          checkVolYearScores(
              BibliographicOrderScorer.getRangeConsistency(items),
              BibliographicOrderScorer.getVolumeRangeConsistency(items),
              BibliographicOrderScorer.getYearRangeConsistency(items)
          );
        }
      }
    }
  }

  private final void checkVolYearScores(BibliographicOrderScorer.Score score, float v, float y) {
    assertEquals(v, score.volScore);
    assertEquals(y, score.yearScore);
  }


  //--------------------------------------------------------------------------
  // Supporting methods
  //--------------------------------------------------------------------------

  /**
   * Shuffle a list of BibliographicItems then order by volume and year.
   * @param aus
   */
  private final void orderVolYear(List<BibliographicItem> aus) {
    Collections.shuffle(aus);
    BibliographicUtil.sortByVolumeYear(aus);
  }

  /**
   * Shuffle a list of BibliographicItems then order by year and volume.
   * @param aus
   */
  private final void orderYearVol(List<BibliographicItem> aus) {
    Collections.shuffle(aus);
    BibliographicUtil.sortByYearVolume(aus);
  }

  /**
   * Run tests on the countProportionOfBreaksInRange method, using the 
   * given list of BibliographicItems and primary sort field. The aus are first shuffled, 
   * and then sorted by volume then year, or year then volume, depending on 
   * the primary sort field. Then we test whether the method gives the expected
   * proportions, which are based on the expected number of breaks for each field.
   *
   * @param sortField the primary field on which to sort the aus
   * @param aus the list of BibliographicItems to test with
   * @param expVolBreaks the expected number of volume breaks given the sort field
   * @param expYrBreaks the expected number of year breaks given the sort field
   */
  private final void checkCountProportionOfBreaksInRange(SORT_FIELD sortField,
                                                         List<BibliographicItem> aus,
                                                         int expVolBreaks,
                                                         int expYrBreaks) {
    // Shuffle and sort
    if (sortField == VOL) orderVolYear(aus); else orderYearVol(aus);
    // Get the denominator for calculating proportions. Equal to 1 less than the
    // number of aus.
    float denom = (float)(aus.size() - 1);
    // Check the results
    float volVal = BibliographicOrderScorer.countProportionOfBreaksInRange(aus, VOL);
    assertValidProportion(volVal);
    assertEquals(((float)expVolBreaks)/denom, volVal);
    float yrVal = BibliographicOrderScorer.countProportionOfBreaksInRange(aus, YR);
    assertValidProportion(yrVal);
    assertEquals(((float)expYrBreaks)/denom, yrVal);
  }

  /**
   * Run tests on the countProportionOfUniquelyYearBreaks method, using the 
   * given list of BibliographicItems. The aus are first shuffled, and then sorted by year 
   * then volume. Then we test whether the method gives the expected
   * proportions, which are based on the expected number of unique breaks for 
   * the year field.
   *
   * @param aus the list of BibliographicItems to test with
   * @param expYrBreaks the expected number of uniquely year breaks
   */
  private final void checkCountProportionOfUniquelyYearBreaks(
      List<BibliographicItem> aus, int expYrBreaks) {
    // Shuffle and sort
    orderYearVol(aus);
    // Get the denominator for calculating proportions. Equal to 1 less than the 
    // number of aus.
    float denom = (float)(aus.size() - 1);
    // Check the results
    float yrVal = BibliographicOrderScorer.countProportionOfUniquelyYearBreaks(aus);
    assertValidProportion(yrVal);
    assertEquals(((float)expYrBreaks)/denom, yrVal);
  }

  /**
   * Run tests on the countProportionOfNegativeBreaksInRange method, using the 
   * given list of BibliographicItems and primary sort field. The aus are first shuffled, 
   * and then sorted by volume then year, or year then volume, depending on 
   * the primary sort field. Then we test whether the method gives the expected
   * proportion on the secondary (other) sort field, based on the expected 
   * number of breaks. The values of the primary sort field, having been 
   * ordered on that field, should all show 0 negative breaks (unless some are empty).
   *
   * @param sortField the primary field on which to sort the aus
   * @param aus the list of BibliographicItems to test with
   * @param expBreaks the expected number of negative breaks on the non-sort field
   */
  private final void checkCountProportionOfNegativeBreaksInRange(SORT_FIELD sortField,
                                                         List<BibliographicItem> aus,
                                                         int expBreaks) {
    // Shuffle and sort
    if (sortField == VOL) orderVolYear(aus); else orderYearVol(aus);
    //printVolsYears(aus);
    // Get the denominator for calculating proportions. Equal to 1 less than the
    // number of aus.
    float denom = (float)(aus.size() - 1);
    // Check the results
    float val = BibliographicOrderScorer.countProportionOfNegativeBreaksInRange(aus,
	sortField.other());
    assertValidProportion(val);
    assertEquals(((float)expBreaks)/denom, val);
    // Sanity check
    if (hasMissingValues(aus, sortField))
      assertTrue(BibliographicOrderScorer.countProportionOfNegativeBreaksInRange(
          aus, sortField) > 0f);
    else
      assertEquals(0f, BibliographicOrderScorer.countProportionOfNegativeBreaksInRange(
          aus, sortField));
  }

    /**
     * Run tests on the countProportionOfRedundancyInRange method, using the
     * given list of BibliographicItems and primary sort field. The aus are first shuffled,
     * and then sorted by volume then year, or year then volume, depending on
     * the primary sort field. Then we test whether the method gives the expected
     * proportions, which are based on the expected number of redundant entries
     * for each field. If the field has empty values, we expect a high
     * redundancy of 1.
     *
     * @param sortField the primary field on which to sort the aus
     * @param aus the list of BibliographicItems to test with
     * @param expVolBreaks the expected number of volume breaks given the sort field
     * @param expYrBreaks the expected number of year breaks given the sort field
     */
  private final void checkCountProportionOfRedundancyInRange(SORT_FIELD sortField,
                                                         List<BibliographicItem> aus,
                                                         int expVolBreaks,
                                                         int expYrBreaks) {
    // Shuffle and sort
    if (sortField == VOL) orderVolYear(aus); else orderYearVol(aus);
    // Get the denominator for calculating proportions. Equal to 1 less than the 
    // number of aus.
    float denom = (float)aus.size();

    // Check the results
    float yrVal = BibliographicOrderScorer.countProportionOfRedundancyInRange(aus, YR);
    assertValidProportion(yrVal);
    if (hasMissingValues(aus, YR))
      assertEquals(1f, yrVal);
    else
      assertEquals(((float)expYrBreaks)/denom, yrVal);

    float volVal = BibliographicOrderScorer.countProportionOfRedundancyInRange(aus, VOL);
    assertValidProportion(volVal);
    if (hasMissingValues(aus, VOL))
      assertEquals(1f, volVal);
    else
      assertEquals(((float)expVolBreaks)/denom, volVal);
  }

  /**
   * Try sorting by year first, and compare year consistency to sorting by 
   * volume first.
   * @param aus
   */
  private final void assertYearListConsistencyGreaterWhenOrderingByYearFirst(List<BibliographicItem> aus) {
    // Shuffle and sort
    orderYearVol(aus);
    float yrCon =  BibliographicOrderScorer.getYearListConsistency(aus);
    assertValidProportion(yrCon);
    orderVolYear(aus);
    float volCon = BibliographicOrderScorer.getYearListConsistency(aus);
    assertValidProportion(yrCon);
    String title = aus.get(0).getName();
    assertTrue(title+" requires "+yrCon+" >= "+volCon, yrCon >= volCon);
  }

  /**
   * Try sorting by volume first, and compare volume consistency to sorting by 
   * year first.
   * @param aus
   */
  private final void assertVolumeListConsistencyGreaterWhenOrderingByVolumeFirst(List<BibliographicItem> aus) {
    // Shuffle and sort
    orderVolYear(aus);
    float volCon =  BibliographicOrderScorer.getVolumeListConsistency(aus);
    assertValidProportion(volCon);
    orderYearVol(aus);
    float yrCon = BibliographicOrderScorer.getVolumeListConsistency(aus);
    assertValidProportion(volCon);
    //System.out.format("Testing vol cons %s > %s\n", volCon, yrCon);
    String title = aus.get(0).getName();
    assertTrue(title+" requires "+volCon+" >= "+yrCon, volCon >= yrCon);
  }

  /**
   * Sort both ways, and ensure that the resultant ordering of the primary 
   * sort field is monotonically increasing. All of the example journals here
   * show inconsistency between the year and volume fields, and so we can also 
   * expect the reverse result on the secondary sort field. 
   * @param aus
   */
  private final void assertMonotonicIncreaseOnTheSortedField(List<BibliographicItem> aus) {
    String title = aus.get(0).getName();
    String err = title+" should be monotonically increasing";
    // Shuffle and sort
    orderVolYear(aus);
    assertTrue(err, BibliographicOrderScorer.isMonotonicallyIncreasing(aus, VOL));
    assertFalse(!containsChangeOfFormats(aus, YR) && BibliographicOrderScorer.isMonotonicallyIncreasing(aus, YR));
    orderYearVol(aus);
    assertTrue(BibliographicOrderScorer.isMonotonicallyIncreasing(aus, YR));
    assertFalse(!containsChangeOfFormats(aus, VOL) && BibliographicOrderScorer.isMonotonicallyIncreasing(aus, VOL));
  }

  /**
   * Check whether the given list of BibliographicItems contains any changes in format
   * on the specified field; if it doesn't, we expect the values not to be 
   * monotonically increasing. This uses BibliographicItem method.
   * @param aus
   */
  private final boolean containsChangeOfFormats(List<BibliographicItem> aus, SORT_FIELD field) {
    if (aus.size()<2) return false;
    for (int i=1; i<=aus.size()-1; i++) {
      if (changeOfFormats(
          field.getValueForComparisonAsPrevious(aus.get(i - 1)),
          field.getValueForComparisonAsCurrent(aus.get(i))
      )) return true;
    }
    return false;
  }

  /**
   * Check that the given value represents a valid proportion, that is,
   * is between 0 and 1 inclusive.
   * @param val a float value to check
   */
  private void assertValidProportion(float val) {
    assertTrue(val >= 0);
    assertTrue(val <= 1);
  }

  /**
   * Register the current last entry in allLists as a title that should be 
   * ordered by volume 
   */
  private final void registerOrderByVolume() {
    titlesToOrderByVolume.add(problemTitles.size()-1);
  }

  /**
   * Register the current last entry in allLists as a title that should be 
   * ordered by volume 
   */
  private final void registerOrderByYear() {
    titlesToOrderByYear.add(problemTitles.size()-1);
  }

  // Debugging method
  private void printVolsYears(List<BibliographicItem> aus) {
    System.out.format("---\n");
    for (BibliographicItem bi : aus)
      System.out.format("%s (%s)\n", bi.getVolume(), bi.getYear());
  }


  private boolean hasMissingValues(List<BibliographicItem> aus, SORT_FIELD fieldToCheck) {
    for (BibliographicItem au : aus) {
      if (!fieldToCheck.hasValue(au)) return true;
    }
    return false;
  }

}
