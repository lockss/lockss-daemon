package org.lockss.servlet;

import org.lockss.plugin.CachedUrl;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import javax.servlet.ServletException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * TimeServlet Tester.
 *
 * @author Claire Griffin
 * @since <pre>02/26/2013</pre>
 * @version 1.0
 */
public class TestTimeServlet extends LockssServletTestCase {
  static Logger log = Logger.getLogger("TestServeContent");
  static final SimpleDateFormat dateFormatter =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

  static {
    dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT")); // section 2.1.1.1
  }

  private MyTimeServlet serv;
  String origResource = "http://a.example.org";
  String daemonPrefix = "http://arxiv.example.net/";
  String mementoDates[] = {
      "Fri, 15 Sep 2000 11:28:26 GMT",
      "Tue, 11 Sep 2001 20:30:51 GMT",
      "Tue, 11 Sep 2001 20:36:10 GMT",
      "Tue, 11 Sep 2001 20:47:33 GMT",
      "Tue, 08 Jul 2008 09:34:33 GMT"
  };
  private MockCachedUrl m_cachedUrls[];

  public void setUp() throws Exception {
    super.setUp();
    serv = new MyTimeServlet();
    MockArchivalUnit mau = new MockArchivalUnit("auid");
    int version = 0;
    m_cachedUrls = new MockCachedUrl[mementoDates.length];
    for(String datestr : mementoDates)
    {
      MockCachedUrl cu = new MockCachedUrl(origResource,mau);
      m_cachedUrls[version++] = cu;
      cu.setVersion(version);
      cu.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED, datestr);
      cu.setProperty(CachedUrl.PROPERTY_FETCH_TIME, datestr);
    }
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   *
   * Method: blankIfNull(String string)
   *
   */
  public void testBlankIfNull() throws Exception {
    // null str is returned as empty string
    String teststr = null;
    assertEquals("", TimeServlet.blankIfNull(teststr));

    // valid string is unchanged
    teststr = "foo";
    assertEquals("foo",TimeServlet.blankIfNull(teststr));
  }

  /**
   *
   * Method: timeGateLink(String origResource, String thisDaemonPrefix)
   */
  public void testTimeGateLink() throws Exception {
    String expected= "<http://arxiv.example.net/timegate/" +origResource+
                     ">; rel=\"timegate\"";
    String result = serv.timeGateLink(origResource, daemonPrefix);
    assertEquals(expected, result);
  }

  /**
   *
   * Method: timeMapLink(String origResource, boolean self, String daemonPrefix,
   * Date first, Date last)
   *
   */
  public void testTimeMapLink() throws Exception {
    Date first = dateFormatter.parse(mementoDates[0]);
    Date last = dateFormatter.parse(mementoDates[mementoDates.length-1]);
    String range= "; from=\""+mementoDates[0] +"\"; until=\"" +
                  mementoDates[mementoDates.length-1]+ "\"";
    // no date, time-map is self
    String expected=
        "<http://arxiv.example.net/timemap/" + origResource +">; " +
                     "rel=\"self\"; type=\"application/link-format\"";
    assertEquals(expected,
        serv.timeMapLink(origResource, true,daemonPrefix, null, null));
    // with dates, time map is self
    expected = expected + range;
    assertEquals(expected,
        serv.timeMapLink(origResource, true,daemonPrefix, first, last));

    expected = "<http://arxiv.example.net/timemap/"+ origResource +">; " +
               "rel=\"timemap\"; type=\"application/link-format\"";

    // no date, time-map is not self
    assertEquals(expected,
        serv.timeMapLink(origResource, false,daemonPrefix, null, null));

    // without dates, time map is not self
    expected = expected + range;
    assertEquals(expected,
        serv.timeMapLink(origResource, false,daemonPrefix, first, last));
  }

  /**
   *
   * Method: mementoLink(CuMemento cuMemento, String rel, String prefix)
   *
   */
  public void testMementoLink() throws Exception {
    checkMemLink(0, TimeServlet.FIRST_MEM);
    checkMemLink(1, TimeServlet.PREV_MEM);
    checkMemLink(2, TimeServlet.MEMENTO);
    checkMemLink(3, TimeServlet.NEXT_MEM);
    checkMemLink(4, TimeServlet.LAST_MEM);
  }

  private void checkMemLink(int index, String relation) throws Exception{
    CuTimeMap.CuMemento memento;
    CachedUrl cu;
    String expected;
    String actual;

    // test first
    cu = m_cachedUrls[index];
    String enc_url = UrlUtil.encodeUrl(origResource);
    memento = new CuTimeMap.CuMemento(cu, TimeServlet.cuTime(cu), index);
    expected = "<" + daemonPrefix+"ServeContent?url="+ enc_url+
               "&auid=auid&version="+ (index+1) + ">; rel=\""
               + relation + "\"; datetime=\""+ mementoDates[index]+"\"";
    actual = serv.mementoLink(memento, relation, daemonPrefix);
    assertEquals(expected, actual);
  }

  /**
   *
   * Method: hasOnlyOneUrlParam()
   *
   */
  public void testHasOnlyOneUrlParam() throws Exception {
   //todo: figure out how to test this.
  }

  class MyTimeServlet extends TimeServlet {

    @Override
    protected void lockssHandleRequest() throws ServletException, IOException {

    }

  }

}
