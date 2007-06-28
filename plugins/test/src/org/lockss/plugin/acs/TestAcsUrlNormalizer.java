package org.lockss.plugin.acs;

import org.lockss.test.LockssTestCase;

public class TestAcsUrlNormalizer extends LockssTestCase {

  public void testLeavesNormalUrlsAlone() throws Exception{
    AcsUrlNormalizer norm = new AcsUrlNormalizer();
    String[] urls = {
	"http://pubs.acs.org/archives/images/blank.gif",
	"http://pubs.acs.org/cgi-bin/abstract.cgi/cmatex/1989/1/i01/f-pdf/f_cm00001a001.pdf",
    };
    for (int ix = 0; ix < urls.length; ix++) {
      assertEquals(urls[ix], norm.normalizeUrl(urls[ix], null));
    }
  }

  public void testRemovesSessionId() throws Exception{
    AcsUrlNormalizer norm = new AcsUrlNormalizer();
    String[] urls = {
	"http://pubs.acs.org/cgi-bin/abstract.cgi/cmatex/1989/1/i01/f-pdf/f_cm00001a001.pdf?sessid=6963",
    };
    String[] normUrls = {
	"http://pubs.acs.org/cgi-bin/abstract.cgi/cmatex/1989/1/i01/f-pdf/f_cm00001a001.pdf?sessid=LOCKSS-FAKE-SESSION-ID",
    };
    for (int ix = 0; ix < urls.length; ix++) {
      assertEquals(normUrls[ix], norm.normalizeUrl(urls[ix], null));
    }
  }
}
