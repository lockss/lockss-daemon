package org.lockss.servlet;

import org.lockss.plugin.CachedUrl;
import org.lockss.test.*;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.lockss.servlet.CuTimeMap.formatter;

/**
 * CuTimeMap Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>02/26/2013</pre>
 */
public class TestCuTimeMap extends LockssTestCase {
  String origResource = "http://a.example.org";
  String daemonPrefix = "http://arxiv.example.net/";
  static String mementoDates[] = {
      "Tue, 08 Jul 2008 09:34:33 GMT",
      "Tue, 11 Sep 2001 20:47:33 GMT",
      "Tue, 11 Sep 2001 20:36:10 GMT",
      "Tue, 11 Sep 2001 20:30:51 GMT",
      "Fri, 15 Sep 2000 11:28:26 GMT"
  };
  private MockCachedUrl memCu[];
  private CuTimeMap timeMap;
  private long acceptDateTime;

  public void setUp() throws Exception {
    super.setUp();
    MockArchivalUnit mau = new MockArchivalUnit("auid");
    int version = 0;
    memCu = new MockCachedUrl[mementoDates.length];
    for(String datestr : mementoDates)
    {
      MockCachedUrl cu = new MockCachedUrl(origResource, mau);
      memCu[version++] = cu;
      cu.setVersion(version);
      cu.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED, datestr);
      cu.setProperty(CachedUrl.PROPERTY_FETCH_TIME, datestr);
    }
    acceptDateTime = formatter.parse(mementoDates[2]).getTime();
    List list = Arrays.asList(memCu);
    timeMap = new CuTimeMap(list, acceptDateTime);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /** Method: getVersionArrays(Collection<CachedUrl> cachedUrls) */
  public void testGetVersionArrays() throws Exception {
    List list = Arrays.asList(memCu);
    Collection<CachedUrl[]> versions = CuTimeMap.getVersionArrays(list);
  }

  /** Method: newerStampedCu(CachedUrl[] cuArray, int cursor) */
  public void testNewerStampedCu() throws Exception {
    CuTimeMap.CuMemento mem;
    // check oldest
    mem = timeMap.newerStampedCu(memCu,0);
    assertEquals(0, mem.index);
    // check cu with older and newer
    mem =timeMap.newerStampedCu(memCu, 2);
    assertEquals(2, mem.index);
    // check a missing date-range
    MockCachedUrl mcu = memCu[3];
    mcu.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED, "");
    mcu.setProperty(CachedUrl.PROPERTY_FETCH_TIME, "");
    mem = timeMap.newerStampedCu(memCu, 3);
    assertEquals(2, mem.index);
    // check out of range
    try {
      mem = timeMap.newerStampedCu(memCu, memCu.length);
      fail("should throw on out of range");
    } catch (IllegalArgumentException ex) {
    }
  }

  /** Method: olderStampedCu(CachedUrl[] cuArray, int cursor) */
  public void testOlderStampedCu() throws Exception {
    CuTimeMap.CuMemento mem;
    // check oldest
    mem = timeMap.olderStampedCu(memCu, 0);
    assertEquals(0, mem.index);
    // check cu with older and newer
    mem =timeMap.olderStampedCu(memCu, 2);
    assertEquals(2, mem.index);
    // check a missing date-range
    MockCachedUrl mcu = memCu[3];
    mcu.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED, "");
    mcu.setProperty(CachedUrl.PROPERTY_FETCH_TIME, "");
    mem = timeMap.olderStampedCu(memCu, 3);
    assertEquals(4, mem.index);
    // check out of range
    try {
      mem = timeMap.newerStampedCu(memCu, -1);
      fail("should throw on out of range");
    } catch (IllegalArgumentException ex) {
    }
  }
  private CuTimeMap.CuMemento makeMemento(int index) throws ParseException {
    CachedUrl cu = memCu[index];
    Date time = formatter.parse(mementoDates[index]);
    return new CuTimeMap.CuMemento(cu, time, index);
  }
  /** Method: CuMemento.before(final CuMemento memento) */
  public void testBefore() throws Exception {
    CuTimeMap.CuMemento mem1 = makeMemento(0);
    CuTimeMap.CuMemento mem2 = makeMemento(1);
    assertTrue(mem2.before(mem1));
    assertFalse(mem1.before(mem2));
    //check null
    assertFalse(mem1.before(null));
  }

  /** Method: CuMemento.after(final CuMemento memento) */
  public void testAfter() throws Exception {
    CuTimeMap.CuMemento mem1 = makeMemento(0);
    CuTimeMap.CuMemento mem2 = makeMemento(1);
    assertTrue(mem1.after(mem2));
    assertFalse(mem2.after(mem1));
    //check null
    assertFalse(mem2.after(null));
  }

  /** Method: CuMemento.url() */
  public void testUrl() throws Exception {
    CuTimeMap.CuMemento mem2 = makeMemento(1);
    assertEquals(origResource, mem2.url());
  }

  /** Method: CuMemento.sameAuAndVersion(CuMemento mem) */
  public void testSameAuAndVersion() throws Exception {
  }

  public void testBookendMapConstruction() throws Exception
  {
    List list = Arrays.asList(memCu);
    CuTimeMap bookendMap = new CuTimeMap(list);
    CuTimeMap.CuMemento newest = makeMemento(0);
    CuTimeMap.CuMemento oldest = makeMemento(list.size()-1);
    assertTrue(oldest.sameAuAndVersion(bookendMap.first()));
    assertTrue(newest.sameAuAndVersion(bookendMap.last()));
    assertNull(bookendMap.selected());
    assertNull(bookendMap.next());
    assertNull(bookendMap.prev());
  }

  public void testBalancedMapContstruction() throws Exception
  {
    List list = Arrays.asList(memCu);
    CuTimeMap.CuMemento newest = makeMemento(0);
    CuTimeMap.CuMemento next = makeMemento(1);
    CuTimeMap.CuMemento selected = makeMemento(2);
    CuTimeMap.CuMemento prev = makeMemento(3);
    CuTimeMap.CuMemento oldest = makeMemento(list.size()-1);
    assertTrue(oldest.sameAuAndVersion(timeMap.first()));
    assertTrue(newest.sameAuAndVersion(timeMap.last()));
    assertTrue(selected.sameAuAndVersion(timeMap.selected()));
    assertTrue(next.sameAuAndVersion(timeMap.next()));
    assertTrue(prev.sameAuAndVersion(timeMap.prev()));

  }

  public void testNewerMementoMapConstruction() throws Exception {
    List list = Arrays.asList(memCu);
    long targetTime;
    targetTime=formatter.parse(mementoDates[mementoDates.length-1]).getTime();
    targetTime -= 360000;
    CuTimeMap test_map = new CuTimeMap(list,targetTime);
    CuTimeMap.CuMemento newest = makeMemento(0);
    CuTimeMap.CuMemento oldest = makeMemento(list.size()-1);
    CuTimeMap.CuMemento selected = oldest;
    CuTimeMap.CuMemento prev = null;
    CuTimeMap.CuMemento next = makeMemento(list.size()-2);
    assertTrue(oldest.sameAuAndVersion(test_map.first()));
    assertTrue(newest.sameAuAndVersion(test_map.last()));
    assertTrue(selected.sameAuAndVersion(test_map.selected()));
    assertTrue(next.sameAuAndVersion(test_map.next()));
    assertNull(prev);

  }
  public void testOlderMementoMapConstruction() throws Exception {
    List list = Arrays.asList(memCu);
    long targetTime;
    targetTime=formatter.parse(mementoDates[0]).getTime();
    targetTime += 360000;
    CuTimeMap test_map = new CuTimeMap(list,targetTime);
    CuTimeMap.CuMemento newest = makeMemento(0);
    CuTimeMap.CuMemento oldest = makeMemento(list.size()-1);
    CuTimeMap.CuMemento selected = newest;
    CuTimeMap.CuMemento prev = makeMemento(1);;
    CuTimeMap.CuMemento next = null;
    assertTrue(oldest.sameAuAndVersion(test_map.first()));
    assertTrue(newest.sameAuAndVersion(test_map.last()));
    assertTrue(selected.sameAuAndVersion(test_map.selected()));
    assertNull(next);
    assertTrue(prev.sameAuAndVersion(test_map.prev()));

  }

}
