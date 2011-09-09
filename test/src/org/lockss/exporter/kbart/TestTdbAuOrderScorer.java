package org.lockss.exporter.kbart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lockss.config.TdbAu;
import org.lockss.config.TdbTestUtil;
import org.lockss.config.Tdb.TdbException;
import org.lockss.exporter.kbart.KbartConverter.TitleRange;
import org.lockss.exporter.kbart.TdbAuOrderScorer.ConsistencyScore;
import org.lockss.exporter.kbart.TdbAuOrderScorer.SORT_FIELD;

import junit.framework.TestCase;

/**
 * Test the behaviour of the TdbAuOrderScorer. Note that the tests here are
 * primarily based on the real-world examples which we are trying to accommodate
 * with the class's fuzzy tests for consecutivity and coverage gaps.
 * 
 * @author Neil Mayo
 */
public class TestTdbAuOrderScorer extends TestCase {

  /*
    ---------------------------------------------------------------------------
    Real-world example issues which the OrderScorer is intended to solve
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
    (4) Fully consistent sequence with mutuallly-consistent ordering and no 
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
   */
  
  // A list of AUs whose years and volumes are very well behaved. The volume 
  // ordering should equal the year ordering, and both should be equal to 
  // the original ordering. There should be no coverage gaps, resulting in a 
  // single range which is also equal to the original list.
  List<TdbAu> fullyConsistentAus;

  // Example titles
  List<TdbAu> tang;
  List<TdbAu> oxEcPap;            // Could be ordered by volume
  List<TdbAu> euroBusRev;
  List<TdbAu> nutFoodSci;
  List<TdbAu> intlJournHumArtsComp;
  List<TdbAu> expAstr;            // Should be ordered by volume
  List<TdbAu> analChem;           // Should be ordered by volume
  List<TdbAu> journEndoc;         // Should be ordered by volume
  List<TdbAu> yorkGeoSoc;         // Should be ordered by volume
  List<TdbAu> commDis;            // Should be ordered by volume
  List<TdbAu> geoSocLonMem;       // Should be ordered by volume
  List<TdbAu> geoSocLonSP;        // Should be ordered by volume

  // Record all the test AUs in a list
  List<List<TdbAu>> allLists;
  
  // Make immutable copies of the canonical orderings and store in a list
  List<List<TdbAu>> allCanonicalLists;

  
  //---------------------------------------------------------------------------
  // Manually-specified title ranges for testing get[Volume|Year]RangeConsistency
  //---------------------------------------------------------------------------
  
  // Expected result of ordering/splitting by volume
  List<TitleRange> tangVolRanges;
  List<TitleRange> oxEcPapVolRanges;
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
  private static final TdbAuOrderScorer.SORT_FIELD YR  = TdbAuOrderScorer.SORT_FIELD.YEAR;
  private static final TdbAuOrderScorer.SORT_FIELD VOL =  TdbAuOrderScorer.SORT_FIELD.VOLUME;
  
  /**
   * Create basic TdbAus and populate all the lists.
   */
  protected void setUp() throws Exception {
    super.setUp();

    // Init all the lists
    allLists = new ArrayList<List<TdbAu>>();
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
      allLists.add(tang);
      allCanonicalLists.add(Collections.unmodifiableList(tang));
      tangYearRanges = Arrays.asList(
	  new TitleRange(Arrays.asList(tang1, tang2, tang3, tang4))
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
      TdbAu oxEcPap1 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume os-6", "1942", "os-6");
      TdbAu oxEcPap2 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume os-7", "1945", "os-7");
      TdbAu oxEcPap3 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume os-8", "1948", "os-8");
      TdbAu oxEcPap4 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume 1",    "1949", "1");
      TdbAu oxEcPap5 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume 2",    "1950", "2");
      TdbAu oxEcPap6 = TdbTestUtil.createBasicAu("Oxford Economic Papers Volume 3",    "1951", "3");
      oxEcPap = Arrays.asList(oxEcPap1, oxEcPap2, oxEcPap3, oxEcPap4, oxEcPap5, oxEcPap6);
      allLists.add(oxEcPap);
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
      TdbAu euroBusRev1 = TdbTestUtil.createBasicAu("European Business Review Volume 97", "1997", "97");
      TdbAu euroBusRev2 = TdbTestUtil.createBasicAu("European Business Review Volume 98", "1998", "98");
      TdbAu euroBusRev3 = TdbTestUtil.createBasicAu("European Business Review Volume 99", "1999", "99");
      TdbAu euroBusRev4 = TdbTestUtil.createBasicAu("European Business Review Volume 12", "2000", "12");
      TdbAu euroBusRev5 = TdbTestUtil.createBasicAu("European Business Review Volume 13", "2001", "13");
      TdbAu euroBusRev6 = TdbTestUtil.createBasicAu("European Business Review Volume 14", "2002", "14");
      euroBusRev = Arrays.asList(euroBusRev1, euroBusRev2, euroBusRev3, euroBusRev4, euroBusRev5, euroBusRev6);
      allLists.add(euroBusRev);
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
      TdbAu nutFoodSci1 = TdbTestUtil.createBasicAu("Nutrition & Food Science 97", "1997", "97");
      TdbAu nutFoodSci2 = TdbTestUtil.createBasicAu("Nutrition & Food Science 98", "1998", "98");
      TdbAu nutFoodSci3 = TdbTestUtil.createBasicAu("Nutrition & Food Science 99", "1999", "99");
      TdbAu nutFoodSci4 = TdbTestUtil.createBasicAu("Nutrition & Food Science 30", "2000", "30");
      TdbAu nutFoodSci5 = TdbTestUtil.createBasicAu("Nutrition & Food Science 31", "2001", "31");
      TdbAu nutFoodSci6 = TdbTestUtil.createBasicAu("Nutrition & Food Science 32", "2002", "32");
      nutFoodSci = Arrays.asList(nutFoodSci1, nutFoodSci2, nutFoodSci3, nutFoodSci4, nutFoodSci5, nutFoodSci6);
      allLists.add(nutFoodSci);
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
      allLists.add(intlJournHumArtsComp);
      allCanonicalLists.add(Collections.unmodifiableList(intlJournHumArtsComp));
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
      allYearRanges.add(intlJournHumArtsCompYearRanges);
      allVolRanges.add(intlJournHumArtsCompVolRanges);
      registerOrderByYear();
      
      // ----------------------------------------------------------------------
      TdbAu expAstr1 = TdbTestUtil.createBasicAu("Experimental Astronomy Volume 3", "1994",      "3");
      TdbAu expAstr2 = TdbTestUtil.createBasicAu("Experimental Astronomy Volume 4", "1993-1994", "4");
      TdbAu expAstr3 = TdbTestUtil.createBasicAu("Experimental Astronomy Volume 5", "1994",      "5");
      expAstr = Arrays.asList(expAstr1, expAstr2, expAstr3);
      allLists.add(expAstr);
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
      TdbAu analChem1 = TdbTestUtil.createBasicAu("Fresenius Zeitschrift für Analytische Chemie Volume 275", "1975",      "275");
      TdbAu analChem2 = TdbTestUtil.createBasicAu("Fresenius Zeitschrift für Analytische Chemie Volume 276", "1972-1975", "276");
      TdbAu analChem3 = TdbTestUtil.createBasicAu("Fresenius Zeitschrift für Analytische Chemie Volume 277", "1975",      "277");
      analChem = Arrays.asList(analChem1, analChem2, analChem3);
      allLists.add(analChem);
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
      TdbAu journEndoc1 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 20", "1960",      "20");
      TdbAu journEndoc2 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 21", "1960-1961", "21");
      TdbAu journEndoc3 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 22", "1962",      "22");
      TdbAu journEndoc4 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 23", "1961-1962", "23");
      TdbAu journEndoc5 = TdbTestUtil.createBasicAu("Journal of Endocrinology Volume 24", "1962",      "24");
      journEndoc = Arrays.asList(journEndoc1, journEndoc2, journEndoc3, journEndoc4, journEndoc5);
      allLists.add(journEndoc);
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
      TdbAu yorkGeoSoc1 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 8",  "1882-1884", "8");
      TdbAu yorkGeoSoc2 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 9",  "1885-1887", "9");
      TdbAu yorkGeoSoc3 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 10", "1889",      "10");
      TdbAu yorkGeoSoc4 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 11", "1888-1890", "11");
      TdbAu yorkGeoSoc5 = TdbTestUtil.createBasicAu("Proceedings of the Yorkshire Geological Society Volume 12", "1891-1894", "12");
      yorkGeoSoc = Arrays.asList(yorkGeoSoc1, yorkGeoSoc2, yorkGeoSoc3, yorkGeoSoc4, yorkGeoSoc5);
      allLists.add(yorkGeoSoc);
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
      TdbAu commDis1 = TdbTestUtil.createBasicAu("Communication Disorders Quarterly Volume 12", "1988-1989", "12");
      TdbAu commDis2 = TdbTestUtil.createBasicAu("Communication Disorders Quarterly Volume 13", "1990",      "13");
      TdbAu commDis3 = TdbTestUtil.createBasicAu("Communication Disorders Quarterly Volume 14", "1988-1992", "14");
      commDis = Arrays.asList(commDis1, commDis2, commDis3);
      allLists.add(commDis);
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
      TdbAu geoSocLonMem1 = TdbTestUtil.createBasicAu("Geological Society of London Memoirs Volume 13", "1992", "13");
      TdbAu geoSocLonMem2 = TdbTestUtil.createBasicAu("Geological Society of London Memoirs Volume 14", "1991", "14");
      TdbAu geoSocLonMem3 = TdbTestUtil.createBasicAu("Geological Society of London Memoirs Volume 15", "1994", "15");
      geoSocLonMem = Arrays.asList(geoSocLonMem1, geoSocLonMem2, geoSocLonMem3);
      allLists.add(geoSocLonMem);
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
      TdbAu geoSocLonSP1 = TdbTestUtil.createBasicAu("Geological Society of London Special Publications Volume 287", "2007", "287");
      TdbAu geoSocLonSP2 = TdbTestUtil.createBasicAu("Geological Society of London Special Publications Volume 288", "2008", "288");
      TdbAu geoSocLonSP3 = TdbTestUtil.createBasicAu("Geological Society of London Special Publications Volume 289", "2007", "289");
      geoSocLonSP = Arrays.asList(geoSocLonSP1, geoSocLonSP2, geoSocLonSP3);
      allLists.add(geoSocLonSP);
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

    } catch (TdbException e) {
      fail("Error setting up test TdbAus.");
    }
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  // TODO test the methods of the SORT_FIELD enums ?
  // These all delegate to methods already under test.
 
  /**
   * Consecutive integer years must be truly consecutive integers.
   * No check is made for "year format".
   */
  public final void testAreYearsConsecutiveIntInt() {
    assertTrue(  TdbAuOrderScorer.areYearsConsecutive(2000,2001) );
    assertTrue(  TdbAuOrderScorer.areYearsConsecutive(2001,2002) );
    assertTrue(  TdbAuOrderScorer.areYearsConsecutive(-1,0) );
    
    assertFalse( TdbAuOrderScorer.areYearsConsecutive(2000,1999) );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive(2000,1990) );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive(2000,2003) );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive(2001,1999) );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive(2000,2000) );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive(0,0) );
  }

  /**
   * Years must be parseable as numbers.
   */
  public final void testAreYearsConsecutiveStringString() {
    assertTrue(  TdbAuOrderScorer.areYearsConsecutive("2000","2001") );
    assertTrue(  TdbAuOrderScorer.areYearsConsecutive("2001","2002") );
    assertTrue(  TdbAuOrderScorer.areYearsConsecutive("-1","0") );
    // Spaces will be trimmed
    assertTrue(  TdbAuOrderScorer.areYearsConsecutive(" 2000 ","     2001 ") );
    
    assertFalse( TdbAuOrderScorer.areYearsConsecutive("2000","1999") );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive("2000","1990") );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive("2000","2003") );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive("2001","1999") );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive("2000","2000") );
    assertFalse( TdbAuOrderScorer.areYearsConsecutive("0","0") );

    // Check exceptions
    String[] unparseable1 = {"You can't parse this.",      "Year 2000",  "2000 year", " 2000 "};
    String[] unparseable2 = {"You can't parse this + 1.",  "Year 2001",  "2001 year", " 2 001 "};
    for (int i=0; i<unparseable1.length; i++) {
      try {
	String s1 = unparseable1[i];
	String s2 = unparseable2[i];
	TdbAuOrderScorer.areYearsConsecutive(s1, s2);
	fail(String.format("Should have thrown NumberFormatException parsing %s and %s.", s1, s2));
      } catch (NumberFormatException e) { /* do nothing */ }
    }
  }

  /**
   * Whether there is a positive gap greater than one between the end of one
   * range and the start of another. 
   */
  public final void testIsBetweenRangeGap() {
    assertTrue(  TdbAuOrderScorer.isGapBetween("2000", "2002")); 
    assertFalse( TdbAuOrderScorer.isGapBetween("2000", "2001")); 
    assertFalse( TdbAuOrderScorer.isGapBetween("2000", "2000"));
    assertFalse( TdbAuOrderScorer.isGapBetween("2000", "1999"));
    // ranges
    assertTrue(  TdbAuOrderScorer.isGapBetween("1999-2000", "2002")); 
    assertFalse( TdbAuOrderScorer.isGapBetween("2000-2001", "2002")); 
    assertTrue(  TdbAuOrderScorer.isGapBetween("2000", "2002-2003")); 
    assertTrue(  TdbAuOrderScorer.isGapBetween("1999-2000", "2002 - 2009"));
    assertFalse( TdbAuOrderScorer.isGapBetween("1990-2000", "1980-1985"));
    // exception
    try {
      TdbAuOrderScorer.isGapBetween("1999 to 2000", "a string");
      fail("Should have produced a NumberFormatException.");
    } catch (NumberFormatException e) { /* ignore */ }
  }

  /**
   * Consecutive integer volumes must be truly consecutive integers.
   */
  public final void testAreVolumesConsecutiveIntInt() {
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive(0,1)  );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive(1,2)  );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive(-1,0) );
    
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive(0,-1) );
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive(2,1)  );
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive(1,3)  );
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive(1,-2) );
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive(1,1)  );
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive(0,0)  );
  }


  /**
   * Consecutive string volumes must be equal, except that the final numerical
   * token of each string must represent consecutive integers.
   */
  public final void testAreVolumesConsecutiveStringString() {
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("Top 100 volumes, no 81!", "Top 100 volumes, no 82!") );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("1", "2") );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("2001", "2002") );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("2 men in a boat", "3 men in a boat") );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("The 10 commandments", "The 11 commandments") );
    
    // The non-numerical string tokens are different
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive("1st volume", "2nd volume") );
    // The non-numerical string tokens are the same
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("4th volume", "5th volume") );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("Das 01 volume", "Das 02 volume") );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("Das 02 volume 2000", "Das 02 volume 2001") );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("Volume s1-1", "Volume s1-2") );
    assertTrue(  TdbAuOrderScorer.areVolumesConsecutive("Volume s100-9", "Volume s100-10") );
    
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive("2001", "2001") );
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive("Volume s100-1", "Volume s100-11") );
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive("Das 02 volume 2000", "Das 03 volume 2001") );
    assertFalse( TdbAuOrderScorer.areVolumesConsecutive("one", "two") );
  }

  /**
   * Values are increasing if they don't go down.
   */
  public final void testAreValuesIncreasingIntInt() {
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("0","1")  );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("1","2")  );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("-1","0") );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("1","3")  );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("1","1")  );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("0","0")  );
    
    assertFalse( TdbAuOrderScorer.areValuesIncreasing("0","-1") );
    assertFalse( TdbAuOrderScorer.areValuesIncreasing("2","1")  );
    assertFalse( TdbAuOrderScorer.areValuesIncreasing("1","-2") );
  }

  /**
   * Parsable integer values are increasing if they don't go down.
   */
  public final void testAreValuesIncreasingStringString() {
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("0","1")  );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("1","2")  );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("-1","0") );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("1","3")  );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("1","1")  );
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("0","0")  );
    
    assertFalse( TdbAuOrderScorer.areValuesIncreasing("0","-1") );
    assertFalse( TdbAuOrderScorer.areValuesIncreasing("2","1")  );
    assertFalse( TdbAuOrderScorer.areValuesIncreasing("1","-2") );
    
    // These strings do not parse as integers
    try {
      TdbAuOrderScorer.areValuesIncreasing("one","two");
      fail("Should have thrown NumberFormatException.");
    } catch (NumberFormatException e) { /* do nothing */ }
    // These do after trimming
    assertTrue(  TdbAuOrderScorer.areValuesIncreasing("  0  ","  1  ")  );
  }

  /**
   * Parsable integer values are decreasing if they do go down.
   */
  public final void testAreValuesDecreasingStringString() {
    assertTrue(  TdbAuOrderScorer.areValuesDecreasing("1","0")  );
    assertTrue(  TdbAuOrderScorer.areValuesDecreasing("2","1")  );
    assertTrue(  TdbAuOrderScorer.areValuesDecreasing("0","-1") );
    assertTrue(  TdbAuOrderScorer.areValuesDecreasing("3","1")  );
    assertTrue(  TdbAuOrderScorer.areValuesDecreasing("1","-2") );

    assertFalse( TdbAuOrderScorer.areValuesDecreasing("1","1")  );
    assertFalse( TdbAuOrderScorer.areValuesDecreasing("0","0")  );
        
    // These strings do not parse as integers
    try {
      TdbAuOrderScorer.areValuesDecreasing("one","two");
      fail("Should have thrown NumberFormatException.");
    } catch (NumberFormatException e) { /* do nothing */ }
    // These do after trimming
    assertTrue(  TdbAuOrderScorer.areValuesDecreasing("  2000  ","  200  ")  );
  }

  /**
   * Increasing integer volumes - second must be gretaer than or equal to the 
   * first.
   */
  public final void testAreVolumesIncreasingIntInt() {
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing(0,1)  );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing(1,2)  );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing(-1,0) );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing(1,3)  );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing(1,1)  );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing(0,0)  );
    
    assertFalse( TdbAuOrderScorer.areVolumesIncreasing(0,-1) );
    assertFalse( TdbAuOrderScorer.areVolumesIncreasing(2,1)  );
    assertFalse( TdbAuOrderScorer.areVolumesIncreasing(1,-2) );
  }
  
  /**
   * Increasing string volumes - second must be gretaer than or equal to the 
   * first.
   */
  public final void testAreVolumesIncreasingStringString() {
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("Top 100 volumes, no 81!", "Top 100 volumes, no 84!") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("1", "2") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("2001", "2002") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("2001", "2001") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("3 men in a boat", "8 men in a boat") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("The 10 commandments", "The 20 commandments") );
    
    // The non-numerical string tokens are different
    assertFalse( TdbAuOrderScorer.areVolumesIncreasing("1st volume", "3rd volume") );
    // The non-numerical string tokens are the same
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("4th volume", "8th volume") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("Das 01 volume", "Das 02 volume") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("Das 02 volume 2000", "Das 02 volume 2001") );
    
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("Volume s1-1", "Volume s1-1") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("Volume s1-1", "Volume s1-4") );
    assertTrue(  TdbAuOrderScorer.areVolumesIncreasing("Volume s100-9", "Volume s100-10") );
    
    assertFalse( TdbAuOrderScorer.areVolumesIncreasing("Das 02 volume 2000", "Das 03 volume 2000") );
    assertFalse( TdbAuOrderScorer.areVolumesIncreasing("one", "two") );

    // This is a corner case; if the strings are identical they are 
    // regarded as generally increasing without needing to parse to integers
    assertTrue( TdbAuOrderScorer.areVolumesIncreasing("one","one") );
  }
  
  /**
   * Two years are appropriately ordered if the second is not less than the 
   * first.
   */
  public final void testAreYearsIncreasingIntInt() {
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(-32,  1889) );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(0,    2001) );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(2000, 2001) );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(2001, 2002) );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(1999, 2000) );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(2001, 2003) );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(2001, 2001) );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(2000, 2000) );
    
    assertFalse( TdbAuOrderScorer.areYearsIncreasing(2000, 1999) );
    assertFalse( TdbAuOrderScorer.areYearsIncreasing(2002, 2001) );
    assertFalse( TdbAuOrderScorer.areYearsIncreasing(2001, 1997) );
  }

  /**
   * Years must be parseable as numbers.
   */
  public final void testAreYearsIncreasingStringString() {
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing("-32",  "1889") );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing("0",    "2001") );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing("2000", "2001") );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing("2001", "2002") );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing("1999", "2000") );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing("2001", "2003") );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing("2001", "2001") );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing("2000", "2000") );
    assertTrue(  TdbAuOrderScorer.areYearsIncreasing(" 999 ", " 3001 ") );
    
    assertFalse( TdbAuOrderScorer.areYearsIncreasing("2000", "1999") );
    assertFalse( TdbAuOrderScorer.areYearsIncreasing("2002", "2001") );
    assertFalse( TdbAuOrderScorer.areYearsIncreasing("2001", "1997") );

    // Check exceptions
    String[] unparseable1 = {"You can't parse this.",     "Year 2000",  "2000 year",  " 999 " };
    String[] unparseable2 = {"You can't parse this + 1.", "Year 2001",  "3001 year",  " 3 001 "};
    for (int i=0; i<unparseable1.length; i++) {
      try {
	String s1 = unparseable1[i];
	String s2 = unparseable2[i];
	TdbAuOrderScorer.areYearsIncreasing(s1, s2);
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
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1976") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1977") );
        
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1977") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1981") );
    
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976-1977") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976-1981") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1978-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1979", "1978-1980") );
    
    // first start included in second range
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1962", "1961-1962") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1976-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1977", "1976-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1980", "1976-1980") );
    
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1976-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1975-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1975-1976") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1981", "1972-1980") );
    
    // Neither condition
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1970-1975") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1970-1975") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1970") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1970") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976", "1978") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1976-1980", "1982") );
    
    // Check exception
    try {
      TdbAuOrderScorer.areYearRangesAppropriatelyConsecutive("1999 to 2000", "a string");
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
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976", "1976") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976", "1977") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976", "1978") );
    
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1976") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1977") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1983") );
    
    // Second contained in first with coincident start 
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1976-1977") );
    // Second contained in first with coincident end
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1978-1980") );
    // Same range
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1976-1980") );
    // Overlap with second finishing later; coincident start
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1976-1981") );
    // Overlap with second starting within first and finishing after
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1979", "1978-1980") );
    // Second completely after first
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1979", "1981-1982") );
    
    // First start included in second range - start, middle, end
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976", "1976-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1977", "1976-1980") );
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1980", "1976-1980") );

    // Overlap, second starts before first, coincident ends
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1975-1980") );
    // Overlap, second ends when first starts
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1975-1976") );
    // Overlap, second starts before first, and ends within first
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1981", "1972-1980") );

    // Containment - second in first
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1981", "1977-1980") );
    // Containment - first in second
    assertTrue( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1977-1980", "1976-1981") );

    // Neither condition
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976", "1970-1975") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1970-1975") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1970") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976", "1970") );
    assertFalse( TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1976-1980", "1972") );
    
    // Check exception
    try {
      TdbAuOrderScorer.areYearRangesAppropriatelySequenced("1999 to 2000", "a string");
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
    checkCountProportionOfBreaksInRange(VOL, tang, 2, 2);
    checkCountProportionOfBreaksInRange(YR,  tang, 2, 0);

    // oxEcPap
    checkCountProportionOfBreaksInRange(VOL, oxEcPap, 1, 3);
    checkCountProportionOfBreaksInRange(YR,  oxEcPap, 1, 2);

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
    
  }

  /**
   * The proportion of year pairs in the range that have a break 
   * between them that occurs uniquely in the year sequence and not the volume 
   * sequence.
   */
  public final void testCountProportionOfUniquelyYearBreaks() {
    checkCountProportionOfUniquelyYearBreaks(tang, 0);
    checkCountProportionOfUniquelyYearBreaks(oxEcPap, 2);
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
    checkCountProportionOfRedundancyInRange(VOL, commDis, 0, 1);
    checkCountProportionOfRedundancyInRange(YR,  commDis, 0, 0);

    // geoSocLonMem
    checkCountProportionOfRedundancyInRange(VOL, geoSocLonMem, 0, 0);
    checkCountProportionOfRedundancyInRange(YR,  geoSocLonMem, 0, 0);

    // geoSocLonSP
    checkCountProportionOfRedundancyInRange(VOL, geoSocLonSP, 0, 1);
    checkCountProportionOfRedundancyInRange(YR,  geoSocLonSP, 0, 0);
    
  }
  
  /**
   * All journals should display monotonically increasing ranges on the 
   * sorted fields. Note that ranges should be produced where the format 
   * of the field changes.
   */
  public final void testIsMonotonicallyIncreasing() {
    for (List<TdbAu> aus : allLists) {
      assertMonotonicIncreaseOnTheSortedField(aus);
    }
  }

  /**
   * Returns true if one string is parseable as an integer while the other is not.
   */
  public final void testChangeOfFormats() {
    assertTrue(  TdbAuOrderScorer.changeOfFormats("string", "1") );
    assertTrue(  TdbAuOrderScorer.changeOfFormats("1", "string") );
    assertFalse( TdbAuOrderScorer.changeOfFormats("1", "1") );
    assertFalse( TdbAuOrderScorer.changeOfFormats("string", "string") );
    // The consistent list should not display any format changes
    assertFalse( containsChangeOfFormats(fullyConsistentAus, VOL) );
    assertFalse( containsChangeOfFormats(fullyConsistentAus, YR) );
    // The following is currently false but we might want to do some proper
    // regular expression work to find and rate string commonalities, or
    // use something like Levenstein distance.
    assertFalse( TdbAuOrderScorer.changeOfFormats("s1-1", "volume 8") );
  }
    
  /**
   * The year ordering should provide a better consistency score for years, or 
   * the same for a fully consistent ordering. 
   */
  public final void testGetYearListConsistency() {
    // The year ordering should provide a better consistency score for years
    for (List<TdbAu> aus : allLists) {
      //System.out.println("Testing getYearListConsistency() on "+aus.get(0).getName());
      assertYearListConsistencyGreaterWhenOrderingByYearFirst(aus);
    }
   
    // Except for a fully consistent sequence
    orderVolYear(fullyConsistentAus);
    float vyCon = TdbAuOrderScorer.getYearListConsistency(fullyConsistentAus);
    orderYearVol(fullyConsistentAus);
    float yvCon = TdbAuOrderScorer.getYearListConsistency(fullyConsistentAus);
    assertEquals( vyCon, yvCon );
    assertEquals( 1f, vyCon );
    assertEquals( 1f, yvCon );
  }

  /**
   * The volume ordering should provide a better consistency score for volumes, or 
   * the same for a fully consistent ordering. 
   */
  public final void testGetVolumeListConsistency() {
    // The volume ordering should provide a better consistency score for volumes
    for (List<TdbAu> aus : allLists) {
      //System.out.println("Testing getVolumeListConsistency() on "+aus.get(0).getName());
      assertVolumeListConsistencyGreaterWhenOrderingByVolumeFirst(aus);
    }
    
    // Except for a fully consistent sequence    
    orderVolYear(fullyConsistentAus);
    float vyCon = TdbAuOrderScorer.getVolumeListConsistency(fullyConsistentAus);
    orderYearVol(fullyConsistentAus);
    float yvCon = TdbAuOrderScorer.getVolumeListConsistency(fullyConsistentAus);
    assertEquals( vyCon, yvCon );
    assertEquals( 1f, vyCon );
    assertEquals( 1f, yvCon );
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
      float volScore  = TdbAuOrderScorer.getYearRangeConsistency(allVolRanges.get(i));
      float yearScore = TdbAuOrderScorer.getYearRangeConsistency(allYearRanges.get(i));
      String title = allYearRanges.get(i).get(0).tdbAus.get(0).getName();
      //System.out.format("%s yearScore %s volScore %s\n", title, yearScore, volScore);
      assertTrue(yearScore > 0);
      assertTrue(yearScore > volScore);
    }
    // Consistent sequence - single range
    List<TitleRange> ranges = Arrays.asList(new TitleRange(fullyConsistentAus));
    orderVolYear(fullyConsistentAus);
    float volScore =  TdbAuOrderScorer.getYearRangeConsistency(ranges);
    orderYearVol(fullyConsistentAus);
    float yearScore = TdbAuOrderScorer.getYearRangeConsistency(ranges);
    // Should all be equal to 1
    assertEquals(yearScore, volScore);
    assertEquals(1f, yearScore);
    assertEquals(1f, volScore);
  }

  /**
   * Get a consistency score for volumes, based upon a list of ranges which were
   * calculated based on a particular ordering.
   */
  public final void testGetVolumeRangeConsistency() {
    // Compare scores from year-ordered and volume-ordered ranges
    for (int i=0; i<allYearRanges.size(); i++) {
      float yearScore = TdbAuOrderScorer.getVolumeRangeConsistency(allYearRanges.get(i));
      float volScore  = TdbAuOrderScorer.getVolumeRangeConsistency(allVolRanges.get(i));
      assertTrue(volScore > 0);
      assertTrue(volScore > yearScore);
    }
    // Consistent sequence - single range
    List<TitleRange> ranges = Arrays.asList(new TitleRange(fullyConsistentAus));
    orderVolYear(fullyConsistentAus);
    float volScore =  TdbAuOrderScorer.getVolumeRangeConsistency(ranges);
    orderYearVol(fullyConsistentAus);
    float yearScore = TdbAuOrderScorer.getVolumeRangeConsistency(ranges);
    // Should all be equal to 1
    assertEquals(yearScore, volScore);
    assertEquals(1f, yearScore);
    assertEquals(1f, volScore);
  }

  /**
   * The more frequently coverage gaps occur, the lower the score. 
   */
  public final void testGetCoverageGapFrequencyDiscount() {
    // If there are as many ranges as AUs (>1), there should be a large discount
    assertEquals(0.8f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(5f, 5f));
    assertEquals(0.5f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(2f, 2f));
    assertEquals(0.98f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(50f, 50f));
    // All such results should be >= 0.5
    for (float i : new float[]{16, 50, 500, 1000000}) {
      assertTrue(TdbAuOrderScorer.getCoverageGapFrequencyDiscount(i, i) >= 0.5);
    }
    
    // If there are no AUs, there should be no discount
    assertEquals(0f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(0f, 0f));
    assertEquals(0f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(0f, 1f));
    
    // If there is a single range, there should be no discount
    assertEquals(0f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(5f, 1f));
    assertEquals(0f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(1f, 1f));
    assertEquals(0f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(500f, 1f));
    
    // If there are half as many ranges as AUs, the discount should be < 0.5  
    assertEquals(0.25f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(4f, 2f));
    assertEquals(0.375f, TdbAuOrderScorer.getCoverageGapFrequencyDiscount(8f, 4f));
    for (int i : new int[]{16, 50, 500, 1000000}) {
      assertTrue(TdbAuOrderScorer.getCoverageGapFrequencyDiscount((float)i,
	  (float)(i/2)) < 0.5);
    }
  }

  /**
   * Test that we get better consistency scores on the field that was used for
   * ordering. This is essentially a repeat of the tests of 
   * get[Year|Volume]RangeConsistency.
   */
  public final void testGetConsistencyScore() {
    for (int i=0; i<allYearRanges.size(); i++) {
      List<TdbAu> aus = allLists.get(i);
      // Consistency score based on volume ordering
      orderVolYear(aus);
      ConsistencyScore csVol = TdbAuOrderScorer.getConsistencyScore(aus, allVolRanges.get(i));
      // Consistency score based on year ordering
      orderYearVol(aus);
      ConsistencyScore csYear = TdbAuOrderScorer.getConsistencyScore(aus, allYearRanges.get(i));
      // Scores should be better for the field which was used in ordering
      assertTrue(csYear.yearScore > csVol.yearScore);
      assertTrue(csVol.volScore > csYear.volScore);
      //assertTrue(csVol.volListScore > csYear.yearListScore);
    }
    // Consistent sequence - single range
    List<TitleRange> ranges = Arrays.asList(new TitleRange(fullyConsistentAus));
    orderVolYear(fullyConsistentAus);
    ConsistencyScore csVol  = TdbAuOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    orderYearVol(fullyConsistentAus);
    ConsistencyScore csYear = TdbAuOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    // Should all be equal to 1
    assertEquals(csVol.yearScore, csVol.volScore);
    assertEquals(1f, csVol.yearScore);
    assertEquals(1f, csVol.volScore);
    assertEquals(csYear.yearScore, csYear.volScore);
    assertEquals(1f, csYear.yearScore);
    assertEquals(1f, csYear.volScore);
  }

  /**
   * Calculate a relative score - the sum of the differences of each 
   * individual score. 
   */
  /*
  public final void testCalculateRelativeBenefit() {
    for (int i=0; i<allYearRanges.size(); i++) {
      List<TdbAu> aus = allLists.get(i);
      String title = allYearRanges.get(i).get(0).tdbAus.get(0).getName();
      orderVolYear(aus);
      ConsistencyScore csVol = TdbAuOrderScorer.getConsistencyScore(aus, allVolRanges.get(i), VOL);
      orderYearVol(aus);
      ConsistencyScore csYear = TdbAuOrderScorer.getConsistencyScore(aus, allYearRanges.get(i), YR);
      
      float rbVolYear = TdbAuOrderScorer.calculateRelativeBenefit(csVol, csYear);
      float rbYearVol = TdbAuOrderScorer.calculateRelativeBenefit(csYear, csVol);
      // Relative benefits should be negations of one another
      assertEquals(rbVolYear, rbYearVol==0 ? rbYearVol : -rbYearVol);
      // The relative benefits should not be zero for the problematic cases
      assertTrue(rbVolYear != 0f);
      assertTrue(rbYearVol != 0f);

      // Ensure the relative benefit swings the right way
      if (titlesToOrderByVolume.contains(i)) {
	assertTrue(title+" should prefer volume; rbVolYear="+rbVolYear, rbVolYear > 0);
      } else if (titlesToOrderByYear.contains(i)) {
	assertTrue(title+" should prefer year; rbYearVol="+rbYearVol, rbYearVol > 0);
      } else {
	// Indices that do not appear in either of these lists can be ordered either way 
      }
    }
    // Consistent sequence - single range
    List<TitleRange> ranges = Arrays.asList(new TitleRange(fullyConsistentAus));
    // TODO do the sorting here and elsewhere, and use canonical for title range
    orderVolYear(fullyConsistentAus);
    ConsistencyScore csVol  = TdbAuOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    orderYearVol(fullyConsistentAus);
    ConsistencyScore csYear = TdbAuOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    float rbVolYear = TdbAuOrderScorer.calculateRelativeBenefit(csVol, csYear);
    float rbYearVol = TdbAuOrderScorer.calculateRelativeBenefit(csYear, csVol);
    assertEquals(rbVolYear, rbYearVol);
    // Should all be equal to 0
    assertEquals(0f, rbVolYear);
    assertEquals(0f, rbYearVol);
  }
  */
   
  /**
   * Test simply whether volume or year is preferred in line with expectations.
   */
  public final void testPreferVolume() {
    for (int i=0; i<allYearRanges.size(); i++) {
      List<TdbAu> aus = allLists.get(i);
      String title = allYearRanges.get(i).get(0).tdbAus.get(0).getName();

      // Order by volume/year and get a score
      orderVolYear(aus);
      ConsistencyScore csVol = TdbAuOrderScorer.getConsistencyScore(aus, allVolRanges.get(i));
      // Order by year/volume and get a score
      orderYearVol(aus);
      ConsistencyScore csYear = TdbAuOrderScorer.getConsistencyScore(aus, allYearRanges.get(i));

      // Do these scores lead us to prefer volume?
      boolean preferVolume = TdbAuOrderScorer.preferVolume(csVol, csYear);
      
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
    List<TitleRange> ranges = Arrays.asList(new TitleRange(fullyConsistentAus));
    // TODO do the sorting here and elsewhere, and use canonical for title range
    orderVolYear(fullyConsistentAus);
    ConsistencyScore csVol  = TdbAuOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    orderYearVol(fullyConsistentAus);
    ConsistencyScore csYear = TdbAuOrderScorer.getConsistencyScore(fullyConsistentAus, ranges);
    boolean preferVolume = TdbAuOrderScorer.preferVolume(csVol, csYear);
    assertTrue(preferVolume);
  }
   
  
  
  //--------------------------------------------------------------------------
  // Supporting methods
  //--------------------------------------------------------------------------
  
  /**
   * Shuffle a list of TdbAus then order by volume and year.
   * @param aus
   */
  private final void orderVolYear(List<TdbAu> aus) {
    Collections.shuffle(aus);
    KbartConverter.sortTdbAusByVolumeYear(aus);
  }
  
  /**
   * Shuffle a list of TdbAus then order by year and volume.
   * @param aus
   */
  private final void orderYearVol(List<TdbAu> aus) {
    Collections.shuffle(aus);
    KbartConverter.sortTdbAusByYearVolume(aus);
  }

  /**
   * Run tests on the countProportionOfBreaksInRange method, using the 
   * given list of TdbAus and primary sort field. The aus are first shuffled, 
   * and then sorted by volume then year, or year then volume, depending on 
   * the primary sort field. Then we test whether the method gives the expected
   * proportions, which are based on the expected number of breaks for each field.
   * 
   * @param sortField the primary field on which to sort the aus
   * @param aus the list of TdbAus to test with
   * @param expVolBreaks the expected number of volume breaks given the sort field
   * @param expYrBreaks the expected number of year breaks given the sort field
   */
  private final void checkCountProportionOfBreaksInRange(SORT_FIELD sortField, 
                                                         List<TdbAu> aus, 
                                                         int expVolBreaks, 
                                                         int expYrBreaks) {
    // Shuffle and sort
    if (sortField == VOL) orderVolYear(aus); else orderYearVol(aus);
    // Get the denominator for calculating proportions. Equal to 1 less than the 
    // number of aus.
    float denom = (float)(aus.size() - 1);
    // Check the results
    float volVal = TdbAuOrderScorer.countProportionOfBreaksInRange(aus, VOL);
    assertValidProportion(volVal);
    assertEquals(((float)expVolBreaks)/denom, volVal);
    float yrVal = TdbAuOrderScorer.countProportionOfBreaksInRange(aus, YR);
    assertValidProportion(yrVal);
    assertEquals(((float)expYrBreaks)/denom, yrVal);
  }

  /**
   * Run tests on the countProportionOfUniquelyYearBreaks method, using the 
   * given list of TdbAus. The aus are first shuffled, and then sorted by year 
   * then volume. Then we test whether the method gives the expected
   * proportions, which are based on the expected number of unique breaks for 
   * the year field.
   * 
   * @param aus the list of TdbAus to test with
   * @param expYrBreaks the expected number of uniquely year breaks
   */
  private final void checkCountProportionOfUniquelyYearBreaks(
      List<TdbAu> aus, int expYrBreaks) {
    // Shuffle and sort
    orderYearVol(aus);
    // Get the denominator for calculating proportions. Equal to 1 less than the 
    // number of aus.
    float denom = (float)(aus.size() - 1);
    // Check the results
    float yrVal = TdbAuOrderScorer.countProportionOfUniquelyYearBreaks(aus);
    assertValidProportion(yrVal);
    assertEquals(((float)expYrBreaks)/denom, yrVal);
  }
  
  /**
   * Run tests on the countProportionOfNegativeBreaksInRange method, using the 
   * given list of TdbAus and primary sort field. The aus are first shuffled, 
   * and then sorted by volume then year, or year then volume, depending on 
   * the primary sort field. Then we test whether the method gives the expected
   * proportion on the secondary (other) sort field, based on the expected 
   * number of breaks. The values of the primary sort field, having been 
   * ordered on that field, should all show 0 negative breaks.
   * 
   * @param sortField the primary field on which to sort the aus
   * @param aus the list of TdbAus to test with
   * @param expBreaks the expected number of negative breaks on the non-sort field
   */
  private final void checkCountProportionOfNegativeBreaksInRange(SORT_FIELD sortField, 
                                                         List<TdbAu> aus, 
                                                         int expBreaks) {
    // Shuffle and sort
    if (sortField == VOL) orderVolYear(aus); else orderYearVol(aus);
    // Get the denominator for calculating proportions. Equal to 1 less than the 
    // number of aus.
    float denom = (float)(aus.size() - 1);
    // Check the results
    float val = TdbAuOrderScorer.countProportionOfNegativeBreaksInRange(aus, 
	sortField.other());
    assertValidProportion(val);
    assertEquals(((float)expBreaks)/denom, val);
    // Sanity check
    assertEquals(0f, TdbAuOrderScorer.countProportionOfNegativeBreaksInRange(
	aus, sortField));
  }

  /**
   * Run tests on the countProportionOfRedundancyInRange method, using the 
   * given list of TdbAus and primary sort field. The aus are first shuffled, 
   * and then sorted by volume then year, or year then volume, depending on 
   * the primary sort field. Then we test whether the method gives the expected
   * proportions, which are based on the expected number of redundant entries 
   * for each field.
   * 
   * @param sortField the primary field on which to sort the aus
   * @param aus the list of TdbAus to test with
   * @param expVolBreaks the expected number of volume breaks given the sort field
   * @param expYrBreaks the expected number of year breaks given the sort field
   */
  private final void checkCountProportionOfRedundancyInRange(SORT_FIELD sortField, 
                                                         List<TdbAu> aus, 
                                                         int expVolBreaks, 
                                                         int expYrBreaks) {
    // Shuffle and sort
    if (sortField == VOL) orderVolYear(aus); else orderYearVol(aus);
    // Get the denominator for calculating proportions. Equal to 1 less than the 
    // number of aus.
    float denom = (float)aus.size();
    // Check the results
    float yrVal = TdbAuOrderScorer.countProportionOfRedundancyInRange(aus, YR);
    assertValidProportion(yrVal);
    assertEquals(((float)expYrBreaks)/denom, yrVal);
    float volVal = TdbAuOrderScorer.countProportionOfRedundancyInRange(aus, VOL);
    assertValidProportion(volVal);
    assertEquals(((float)expVolBreaks)/denom, volVal);
  }

  /**
   * Try sorting by year first, and compare year consistency to sorting by 
   * volume first.
   * @param aus
   */
  private final void assertYearListConsistencyGreaterWhenOrderingByYearFirst(List<TdbAu> aus) {
    // Shuffle and sort
    orderYearVol(aus);
    float yrCon =  TdbAuOrderScorer.getYearListConsistency(aus);
    assertValidProportion(yrCon);
    orderVolYear(aus);
    float volCon = TdbAuOrderScorer.getYearListConsistency(aus);
    assertValidProportion(yrCon);
    String title = aus.get(0).getName();
    assertTrue(title+" requires "+yrCon+" >= "+volCon, yrCon >= volCon);
  }
   
  /**
   * Try sorting by volume first, and compare volume consistency to sorting by 
   * year first.
   * @param aus
   */
  private final void assertVolumeListConsistencyGreaterWhenOrderingByVolumeFirst(List<TdbAu> aus) {
    // Shuffle and sort
    orderVolYear(aus);
    float volCon =  TdbAuOrderScorer.getVolumeListConsistency(aus);
    assertValidProportion(volCon);
    orderYearVol(aus);
    float yrCon = TdbAuOrderScorer.getVolumeListConsistency(aus);
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
  private final void assertMonotonicIncreaseOnTheSortedField(List<TdbAu> aus) {
    // Shuffle and sort
    orderVolYear(aus);
    assertTrue(TdbAuOrderScorer.isMonotonicallyIncreasing(aus, VOL));
    assertFalse(!containsChangeOfFormats(aus, YR) && TdbAuOrderScorer.isMonotonicallyIncreasing(aus, YR));
    orderYearVol(aus);
    assertTrue(TdbAuOrderScorer.isMonotonicallyIncreasing(aus, YR));
    assertFalse(!containsChangeOfFormats(aus, VOL) && TdbAuOrderScorer.isMonotonicallyIncreasing(aus, VOL));
  }
  
  /**
   * Check whether the given list of TdbAus contains any changes in format
   * on the specified field; if it doesn't, we expect the values not to be 
   * monotonically increasing. 
   * @param aus
   */
  private final boolean containsChangeOfFormats(List<TdbAu> aus, SORT_FIELD field) {
    if (aus.size()<2) return false;
    for (int i=1; i<=aus.size()-1; i++) {
      if (TdbAuOrderScorer.changeOfFormats(
	  field.getValue(aus.get(i-1)), 
	  field.getValue(aus.get(i))
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
    titlesToOrderByVolume.add(allLists.size()-1);
  }

  /**
   * Register the current last entry in allLists as a title that should be 
   * ordered by volume 
   */
  private final void registerOrderByYear() {
    titlesToOrderByYear.add(allLists.size()-1);
  }

  
}
