package org.lockss.plugin.scielo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;

public class TestSciELO2024HtmlHashFilterFactory extends LockssTestCase{
    public void testFilter() throws Exception{
        FilterFactory fact = new SciELO2024HtmlHashFilterFactory();
        String page = "<html>"+
                        "<head>"+
                            "FAIL"+
                        "</head>"+
                        "<body>"+
                            "<header>"+
                                "FAIL"+
                            "</header>"+
                            "<section class='${levelMenu}'>"+
                                "FAIL"+
                            "</section>"+
                            "<div class='${share}'>"+
                                "FAIL"+
                            "</div>"+
                            "<footer>"+
                                "FAIL"+
                            "</footer>"+
                        "</body>"+
                      "</html>";
        Map<String, String> values = new HashMap<String, String>();
        for(String levelMenu:Arrays.asList("levelMenu FAIL FAIL FAIL", "FAIL FAIL levelMenu FAIL FAIL", "FAIL FAIL FAIL FAIL levelMenu")){
            values.put("levelMenu", levelMenu);
            for(String share:Arrays.asList("share FAIL FAIL FAIL", "FAIL FAIL share FAIL FAIL", "FAIL FAIL FAIL FAIL share")){
                values.put("share", share);
                StringSubstitutor sub = new StringSubstitutor(values);
                InputStream in = IOUtils.toInputStream(sub.replace(page),Constants.DEFAULT_ENCODING);
                InputStream out = fact.createFilteredInputStream(null, in, Constants.DEFAULT_ENCODING);
                String result = IOUtils.toString(out, Constants.DEFAULT_ENCODING);
                assertFalse(result.contains("FAIL")); //TODO: add fail message
            }
        }
    }
}
