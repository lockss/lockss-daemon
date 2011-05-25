package org.lockss.config;

import java.util.ArrayList;
import java.util.Collection;

import org.lockss.config.TdbUtil.ContentScope;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.test.LockssTestCase;

/**
 * Test class for <code>org.lockss.config.TdbUtil</code>.
 * Requires the setup of a collection of ArchivalUnits,
 * and of configured/preserved AUs if possible.
 * <p>
 * Currently methods are just tested for a null return value.
 * Testing requires a deeper and more complex mock Tdb to be created, with
 * ArchivalUnits and TitleConfigs.
 *
 * @author  Neil Mayo
 */
public class TestTdbUtil extends LockssTestCase {

  protected void setUp() throws Exception {
    super.setUp();

    // Every operation using the Tdb uses TdbUtil.getTdb() to get the CurrentConfig's Tdb,
    // so we need to set that up.
    
    // Create a new ConfigManager and set the curent configuration
    ConfigManager mgr = ConfigManager.makeConfigManager();
    mgr.setCurrentConfig(ConfigManager.newConfiguration());
    // Create a test tdb and set it in the current config
    ConfigManager.getCurrentConfig().setTdb(TdbTestUtil.makeTestTdb());
    // (test tdb has a publisher, a title and 2 TdbAus)
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public final void testGetTdb() {
    assertNotNull("Tdb is null", TdbUtil.getTdb());
  }

  public final void testGetTdbTitle() {
    // TODO requires an ArchivalUnit
    //fail("Not yet implemented");
  }

  public final void testGetTdbAu() {
    // TODO requires an ArchivalUnit
    //fail("Not yet implemented");
  }

  public final void testGetAllTdbAus() {
    assertNotNull(TdbUtil.getAllTdbAus());
  }

  public final void testGetAllTdbTitles() {
    assertNotNull(TdbUtil.getAllTdbTitles());
  }

  public final void testGetTdbTitles() {
    // Get title lists
    Collection<TdbTitle> defaultTitles = TdbUtil.getTdbTitles(null);
    assertNotNull(defaultTitles);
    
    Collection<TdbTitle> allTitles = TdbUtil.getTdbTitles(ContentScope.ALL);
    assertNotNull(allTitles);

    Collection<TdbTitle> configuredTitles = TdbUtil.getTdbTitles(ContentScope.CONFIGURED);
    assertNotNull(configuredTitles);
    
    Collection<TdbTitle> preservedTitles = TdbUtil.getTdbTitles(ContentScope.PRESERVED);
    assertNotNull(preservedTitles);
    
    // Check sizes of returned lists
    assertEquals(allTitles.size(), defaultTitles.size());
    assertEquals(1, allTitles.size());
    
    // The following may not be accurate when running tests on a machine that already has 
    // a configured daemon; depends where the figures come from.
    assertEquals(0, configuredTitles.size());
    assertEquals(0, preservedTitles.size());
  }

  public final void testGetPreservedAus() {
    assertNotNull(TdbUtil.getPreservedAus());
  }

  public final void testGetConfiguredAus() {
    assertNotNull(TdbUtil.getConfiguredAus());
  }

  public final void testGetConfiguredTdbAus() {
    assertNotNull(TdbUtil.getConfiguredTdbAus());
  }

  public final void testGetPreservedTdbAus() {
    assertNotNull(TdbUtil.getPreservedTdbAus());
  }

  public final void testGetConfiguredTdbTitles() {
    assertNotNull(TdbUtil.getConfiguredTdbTitles());
  }

  public final void testGetPreservedTdbTitles() {
    assertNotNull(TdbUtil.getPreservedTdbTitles());
  }

  public final void testGetTdbTitlesFromAus() {
    Collection<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();
    assertNotNull(TdbUtil.getTdbTitlesFromAus(aus));
  }

}
