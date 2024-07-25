package org.lockss.plugin.scielo;

import org.lockss.plugin.QueryUrlNormalizer;
import org.lockss.plugin.TestQueryUrlNormalizer;
import org.lockss.plugin.UrlNormalizer;

public class TestSciELO2024UrlNormalizer extends TestQueryUrlNormalizer{
        public void testDropKeyValue() throws Exception{
        UrlNormalizer norm = new SciELO2024UrlNormalizer();
        assertEquals("https://wwww.scielo.br/?c=3", norm.normalizeUrl("https://wwww.scielo.br/?format=html&c=3",null));
        assertEquals("https://wwww.scielo.br/?c=3&format=pdf", norm.normalizeUrl("https://wwww.scielo.br/?format=pdf&c=3",null));
        assertEquals("https://wwww.scielo.br/?c=3&format=", norm.normalizeUrl("https://wwww.scielo.br/?format=&c=3",null));
    }
}
