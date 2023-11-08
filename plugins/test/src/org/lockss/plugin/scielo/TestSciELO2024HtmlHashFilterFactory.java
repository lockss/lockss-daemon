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
                            "<a class='${floatingBtnError}'>"+
                                "FAIL"+
                            "</a>"+
                            "<header>"+
                                "FAIL"+
                            "</header>"+
                            "<div class='alternativeHeader'>"+
                                "FAIL"+
                            "</div>"+
                            "<section class='journalContent'>"+
                                "<div class='issueIndex'>"+
                                    "<div class='issueIndex'>"+
                                        "FAIL"+
                                    "</div>"+
                                "</div>"+
                            "</section>"+
                            "<section class='${levelMenu}'>"+
                                "FAIL"+
                            "</section>"+
                            "<div class='floatingMenuCtt'>"+
                                "FAIL"+
                            "</div>"+
                            "<ul class='${floatingMenuMobile}'>"+
                                "FAIL"+
                            "</ul>"+
                            "<section class='journalContacts'>"+
                                "FAIL"+
                            "</section>"+
                            "<div class='${share}'>"+
                                "FAIL"+
                            "</div>"+
                            "<div id='error_modal_id'>"+
                                "FAIL"+
                            "</div>"+
                            "<div id='ModalRelatedArticles'>"+
                                "FAIL"+
                            "</div>"+
                            "<div id='metric_modal_id'>"+
                                "FAIL"+
                            "</div>"+
                            "<div id='ModalVersionsTranslations'>"+
                                "FAIL"+
                            "</div>"+
                            "<div id='ModalScimago'>"+
                                "FAIL"+
                            "</div>"+
                            "<style>"+
                                "FAIL"+
                            "</style>"+
                            "<script>"+
                                "FAIL"+
                            "</script>"+
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
                for(String floatingBtnError:Arrays.asList("floatingBtnError FAIL FAIL FAIL", "FAIL FAIL floatingBtnError FAIL FAIL", "FAIL FAIL FAIL FAIL floatingBtnError"))
                    values.put("floatingBtnError", floatingBtnError);
                    for(String floatingMenuMobile:Arrays.asList("floatingMenuMobile FAIL FAIL FAIL", "FAIL FAIL floatingMenuMobile FAIL FAIL", "FAIL FAIL FAIL FAIL floatingMenuMobile"))
                        values.put("floatingMenuMobile",floatingMenuMobile);
                        StringSubstitutor sub = new StringSubstitutor(values);
                        InputStream in = IOUtils.toInputStream(sub.replace(page),Constants.DEFAULT_ENCODING);
                        InputStream out = fact.createFilteredInputStream(null, in, Constants.DEFAULT_ENCODING);
                        String result = IOUtils.toString(out, Constants.DEFAULT_ENCODING);
                        assertFalse(result.contains("FAIL")); //TODO: add fail message
            }
        }
    }
}
