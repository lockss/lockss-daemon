package org.lockss.plugin.respediatrica;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.respediatrica.ResPediatricaUrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestResPediatricaUrlNormalizer extends LockssTestCase {
    private static final String urlStr1 = "https://cdn.gn1.link/residenciapediatrica/Content/css/responsive.css?v=1";
    private static final String urlStr2 = "https://cdn.gn1.link/residenciapediatrica/Content/css/style.css?v=1";
    private static final String urlStr3 = "https://cdn.gn1.link/residenciapediatrica/Images/liv-re-home.jpg?v=2";
    private static final String urlStr4 = "https://cdn.gn1.link/residenciapediatrica/Scripts/script.js?v=2";

    private static final String resultStr1 = "https://cdn.gn1.link/residenciapediatrica/Content/css/responsive.css";
    private static final String resultStr2 = "https://cdn.gn1.link/residenciapediatrica/Content/css/style.css";
    private static final String resultStr3 = "https://cdn.gn1.link/residenciapediatrica/Images/liv-re-home.jpg";
    private static final String resultStr4 = "https://cdn.gn1.link/residenciapediatrica/Scripts/script.js";

    public void testUrlNormalizer() throws Exception {
        UrlNormalizer normalizer = new ResPediatricaUrlNormalizer();
        assertEquals(resultStr1, normalizer.normalizeUrl(urlStr1, null));
        assertEquals(resultStr2, normalizer.normalizeUrl(urlStr2, null));
        assertEquals(resultStr3, normalizer.normalizeUrl(urlStr3, null));
        assertEquals(resultStr4, normalizer.normalizeUrl(urlStr4, null));
    }
}

