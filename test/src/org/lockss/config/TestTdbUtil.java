package org.lockss.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.lockss.config.TdbUtil.ContentScope;
import org.lockss.config.TdbUtil.ContentType;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

/**
 * Test class for <code>org.lockss.config.TdbUtil</code>.
 * Requires the setup of a collection of ArchivalUnits,
 * and of configured/preserved AUs if possible.
 * <p>
 * Currently methods are just tested for a null return value.
 * Testing requires a deeper and more complex mock Tdb to be created, with
 * ArchivalUnits and TitleConfigs.
 * Use MockArchivalUnit or SimulatedArchivalUnit.
 * Also test for book/journal differences.
 *
 * @author  Neil Mayo
 */
public class TestTdbUtil extends LockssTestCase {

  private static final int NUM_MOCK_AUS = 8;
  
  Collection<ArchivalUnit> configuredAus;
  Collection<ArchivalUnit> preservedAus;
  Collection<TdbAu> configuredTdbAus;
  Collection<TdbAu> preservedTdbAus;
  
  Collection<TdbTitle> defaultTitles;
  Collection<TdbTitle> allTitles;
  Collection<TdbTitle> configuredTitles;
  Collection<TdbTitle> preservedTitles;
  
  int numNull, numDefault, numAll, numConfigured, numPreserved;

  protected void setUp() throws Exception {
    super.setUp();

    // Every operation using the Tdb uses TdbUtil.getTdb() to get the CurrentConfig's Tdb,
    // so we need to set that up.
    TdbTestUtil.setUpConfig();

    // (test tdb has a publisher, a title and 2 TdbAus)
    configuredAus = TdbUtil.getConfiguredAus();
    preservedAus = TdbUtil.getPreservedAus();
    configuredTdbAus = TdbUtil.getConfiguredTdbAus();
    preservedTdbAus = TdbUtil.getPreservedTdbAus();
    
    defaultTitles = TdbUtil.getTdbTitles(null, null);
    allTitles = TdbUtil.getTdbTitles(ContentScope.ALL, ContentType.ALL);
    configuredTitles = TdbUtil.getTdbTitles(ContentScope.CONFIGURED, ContentType.ALL);
    preservedTitles = TdbUtil.getTdbTitles(ContentScope.COLLECTED, ContentType.ALL);
    
    numNull = TdbUtil.getNumberTdbTitles(null, null);
    numDefault = TdbUtil.getNumberTdbTitles(ContentScope.DEFAULT_SCOPE, ContentType.ALL);
    numAll = TdbUtil.getNumberTdbTitles(ContentScope.ALL, ContentType.ALL);
    numConfigured = TdbUtil.getNumberTdbTitles(ContentScope.CONFIGURED, ContentType.ALL);
    numPreserved = TdbUtil.getNumberTdbTitles(ContentScope.COLLECTED, ContentType.ALL);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public final void testGetTdb() {
    assertNotNull("Tdb is null", TdbUtil.getTdb());
  }

  public final void testGetTdbTitle() {
    //MockArchivalUnit au = MockArchivalUnit.newInited();
    // The result will be null as the mock AU will have no TitleConfig
    //assertEquals(null, TdbUtil.getTdbTitle(au));
    
    // We can't get the title as we can't get the TdbAu from a MockArchivalUnit
  }

  public final void testGetTdbAu() {
    MockArchivalUnit au = MockArchivalUnit.newInited();
    // The result will be null as the mock AU will have no TitleConfig
    assertEquals(null, TdbUtil.getTdbAu(au));
  }


  public final void testFilterTitlesByType() {
    try {
      List<TdbTitle> titles = new ArrayList<TdbTitle>() {{
        add(TdbTestUtil.makeBookTestTitle("v1", "1990", "2000"));
        add(TdbTestUtil.makeBookTestTitle("v2", "1995", "1996", "2007"));
      }};
      assertIsomorphic(titles, TdbUtil.filterTitlesByType(titles, ContentType.ALL));
      assertIsomorphic(titles, TdbUtil.filterTitlesByType(titles, ContentType.BOOKS));
      assertEmpty(TdbUtil.filterTitlesByType(titles, ContentType.JOURNALS));
      // Add a non-book title
      titles.add(TdbTestUtil.makeYearTestTitle("2010", "2011"));
      assertIsomorphic(titles, TdbUtil.filterTitlesByType(titles, ContentType.ALL));
      assertEquals(titles.size()-1, TdbUtil.filterTitlesByType(titles, ContentType.BOOKS).size());
      assertEquals(1, TdbUtil.filterTitlesByType(titles, ContentType.JOURNALS).size());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Could not create book test title");
    }
  }

  //public final void testFilterAusByType() {  }

  public final void testGetAllTdbTitles() {
    assertNotNull(TdbUtil.getAllTdbTitles());
  }

  public final void testGetTdbTitles() {
    // Get title lists
    assertNotNull(defaultTitles);
    assertNotNull(allTitles);
    assertNotNull(configuredTitles);
    assertNotNull(preservedTitles);
    
    // Check sizes of returned lists
    assertEquals(allTitles.size(), defaultTitles.size());
    assertEquals(1, allTitles.size());
    
    // The following may not be accurate when running tests on a machine that already has 
    // a configured daemon; depends where the figures come from.
    assertEquals(0, configuredTitles.size());
    assertEquals(0, preservedTitles.size());
  }

  public final void testGetNumberTdbTitles() {
    // Check that the number of title is in all cases >= 0
    assertTrue(numAll>=0);
    assertTrue(numNull>=0);
    assertTrue(numDefault>=0);
    assertTrue(numConfigured>=0);
    assertTrue(numPreserved>=0);
    // With null scope, the default is all
    assertTrue(numNull==numDefault);
    assertTrue(numDefault==numAll);
  }
  
  // This is just a wrapper around the individual get*Aus() methods,
  // with a switch statement on the ContentScope. Unusually,
  // because of when AUs are available, passing either ALL or null
  // to the method should result in an empty list.
  public final void testGetAus() {
    assertEmpty(TdbUtil.getAus(ContentScope.ALL, ContentType.ALL));
    assertEmpty(TdbUtil.getAus(null, null));
  }
  
  public final void testGetConfiguredAus() {
    assertNotNull(configuredAus);
  }

  public final void testGetPreservedAus() {
    assertNotNull(preservedAus);
    assertTrue(preservedAus.size()<=configuredAus.size());
  }

  public final void testGetConfiguredTdbAus() {
    assertNotNull(configuredTdbAus);
  }

  public final void testGetPreservedTdbAus() {
    assertNotNull(preservedTdbAus);
    assertTrue(preservedTdbAus.size()<=configuredTdbAus.size());
  }

  public final void testGetConfiguredTdbTitles() {
    assertNotNull(TdbUtil.getConfiguredTdbTitles());
  }

  public final void testGetPreservedTdbTitles() {
    assertNotNull(TdbUtil.getPreservedTdbTitles());
  }

  public final void testGetTdbTitlesFromAus() {
    Collection<ArchivalUnit> aus = getMockAus();
    Collection<TdbTitle> tits = TdbUtil.getTdbTitlesFromAus(aus);
    assertNotNull(tits);
    assertTrue(tits.size()<=aus.size());
  }

  public final void testMapTitlesToAus() {
    Collection<ArchivalUnit> aus = getMockAus();
    Map<TdbTitle, List<ArchivalUnit>> map = TdbUtil.mapTitlesToAus(aus);
    assertNotNull(map);
    assertEquals(TdbUtil.getTdbTitlesFromAus(aus).size(), map.size());
    assertTrue(map.size()<=aus.size());
  }

  public final void testGetTdbAusFromAus() {
    Collection<ArchivalUnit> aus = getMockAus();
    List<TdbAu> tdbAus = TdbUtil.getTdbAusFromAus(aus);
    assertNotNull(tdbAus);
    assertTrue(tdbAus.size()<=aus.size());
    // Note that the result will in fact be empty using mock AUs which don't 
    // correspond to TDB entries.
  }


  /**
   * For each of the the supplied ArchivalUnits, get the corresponding TdbAu
   * and map it to the AU. Note that if an ArchivalUnit has no TitleConfig,
   * there will be no corresponding TdbAu in the returned map, and so it may
   * differ in size to the argument.
   *
   * @param units a collection of ArchivalUnits
   * @return a map of TdbAus to ArchivalUnits
   */
  public final void testMapTdbAusToAus() {
    Collection<ArchivalUnit> aus = getMockAus();
    Map<TdbAu, ArchivalUnit> tdbAus = TdbUtil.mapTdbAusToAus(aus);
    assertNotNull(tdbAus);
    assertTrue(tdbAus.size()<=aus.size());
  }

  /**
   * Test whether an AU appears to be a book, that is it has some sort of ISBN.
   * @param au
   * @return
   */
  public final void testIsBook() throws Tdb.TdbException {
    TdbTitle bookTitle = TdbTestUtil.makeBookTestTitle("v1", "1990", "2000");
    for (TdbAu book:bookTitle.getTdbAus()) {
      assertTrue(TdbUtil.isBook(book));
    }
  }

  /**
   * Test whether an AU appears to be part of a book series.
   * @param au
   * @return
   */
  public final void testIsBookSeries() throws Tdb.TdbException {
    TdbTitle bookSeriesTitle = TdbTestUtil.makeBookSeriesTestTitle("v1", "1990", "2000");
    for (TdbAu book:bookSeriesTitle.getTdbAus()) {
      assertTrue(TdbUtil.isBookSeries(book));
    }
  }

  /**
   * Get the enumerated type of a BibliographicItem.
   * @param au
   * @return
   */
  public final void testGetBibliographicItemType() throws Tdb.TdbException {
    // Test books
    TdbTitle bookTitle = TdbTestUtil.makeBookTestTitle("v1", "1990", "2000");
    for (TdbAu book:bookTitle.getTdbAus()) {
      assertTrue(TdbUtil.getBibliographicItemType(book)==TdbUtil.BibliographicItemType.BOOK);
    }
    // Test books in a series
    TdbTitle bookSeriesTitle = TdbTestUtil.makeBookSeriesTestTitle("v1", "1990", "2000");
    for (TdbAu book:bookSeriesTitle.getTdbAus()) {
      assertTrue(TdbUtil.getBibliographicItemType(book)==TdbUtil.BibliographicItemType.BOOKSERIES);
    }
    // Test default
    TdbAu au = TdbTestUtil.makeIssueTestAu("v1", "1");
    assertTrue(TdbUtil.getBibliographicItemType(au)==TdbUtil.BibliographicItemType.JOURNAL);
  }


  private final Collection<ArchivalUnit> getMockAus() {
    Collection<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();
    for (int i=0; i<NUM_MOCK_AUS; i++) aus.add(MockArchivalUnit.newInited());
    return aus;
  }

}
