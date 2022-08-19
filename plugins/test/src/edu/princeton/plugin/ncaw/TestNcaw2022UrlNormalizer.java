package edu.princeton.plugin.ncaw;

import org.lockss.config.Configuration;
import org.lockss.plugin.UrlNormalizer;
import edu.princeton.plugin.ncaw.Ncaw2022UrlNormalizer;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;

public class TestNcaw2022UrlNormalizer extends LockssTestCase {
    public void testNormalizer ()
    throws Exception {

    Configuration config = ConfigurationUtil.fromArgs("base_url", "https://www.example.com/", "volume_name", "21", "year", "2022");
    MockArchivalUnit m_mau = new MockArchivalUnit();
    m_mau.setConfiguration(config);

    UrlNormalizer n = new Ncaw2022UrlNormalizer();

    assertEquals("https://www.example.com/try.pdf", n.normalizeUrl("http://www.example.com/try.pdf", m_mau));
    assertEquals("https://www.example.com/try.pdf", n.normalizeUrl("http://example.com/try.pdf", m_mau));
    assertEquals("https://www.example.com/try.pdf", n.normalizeUrl("https://www.example.com/try.pdf", m_mau));
    assertEquals("https://www.example.com/try.pdf", n.normalizeUrl("https://example.com/try.pdf", m_mau));
    assertEquals("http://www.notanexample.com/try.pdf", n.normalizeUrl("http://www.notanexample.com/try.pdf", m_mau));
    assertEquals("https://www.notanexample.com/try.pdf", n.normalizeUrl("https://www.notanexample.com/try.pdf", m_mau));
    assertEquals("http://notanexample.com/try.pdf", n.normalizeUrl("http://notanexample.com/try.pdf", m_mau));
    assertEquals("https://notanexample.com/try.pdf", n.normalizeUrl("https://notanexample.com/try.pdf", m_mau));
}
}
