package org.lockss.plugin.usdocspln.gov.govinfo;

import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Constants;

public class TestGovInfoHtmlCrawlFilterFactory extends LockssTestCase{
    public void testFilter() throws Exception{
        FilterFactory fact = new GovInfoHtmlCrawlFilterFactory();
        String page = "<html>"+
                        "<head>"+
                        "</head>"+
                        "<body>"+
                            "<a id='mr_url_id'>"+
                              "FAIL"+
                            "</a>"+
                        "</body>"+
                      "</html>";
        Map<String, String> values = new HashMap<String, String>();
        StringSubstitutor sub = new StringSubstitutor(values);
        InputStream in = IOUtils.toInputStream(sub.replace(page),Constants.DEFAULT_ENCODING);
        InputStream out = fact.createFilteredInputStream(null, in, Constants.DEFAULT_ENCODING);
        String result = IOUtils.toString(out, Constants.DEFAULT_ENCODING);
        assertFalse("Your test case failed! \n"+result, result.contains("FAIL")); 
    }
}