package org.lockss.plugin.scalar;

import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.LineRewritingReader;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class ScalarJavaScriptLinkRewriterFactory implements LinkRewriterFactory {

    static final Logger logger =
            Logger.getLogger(ScalarJavaScriptLinkRewriterFactory.class);
    
    public InputStream createLinkRewriter(
            String mimeType, ArchivalUnit au, InputStream in,
            String encoding, final String srcUrl,
            final ServletUtil.LinkTransform srvLinkXform)
            throws PluginException, IOException {

        logger.debug3("Fei - JavaScriptLinkRewriterFactory src = " + srcUrl + ", mimeType = " + mimeType);

        if (in == null) {
            throw new IllegalArgumentException("Called with null InputStream");
        }

        //http://blackquotidian.supdigital.org/system/application/views/melons/cantaloupe/js/main.js
        final String localReadJSFile = "/melons/cantaloupe/js/main.js";
        
        /*
        <script type="text/javascript" src="http://blackquotidian.supdigital.org/system/application/views/arbors/html5_RDFa/js/jquery-3.4.1.min.js"></script>
        <script type="text/javascript" src="http://localhost:8081/ServeContent?url=http%3A%2F%2Fblackquotidian.supdigital.org%2Fsystem%2Fapplication%2Fviews%2Farbors%2Fhtml5_RDFa%2Fjs%2Fjquery-3.4.1.min.js"></script>

        var script_uri = '';
        $('script[src]').each(function() {  // Certain hotel wifi are injecting spam <script> tags into the page
            var $this = $(this);
            if ($this.attr('src').indexOf('jquery-3.4.1.min.js') != -1) {
                script_uri = $this.attr('src');
                return false;
            }
        });
        var scheme = (script_uri.indexOf('https://') != -1) ? 'https://' : 'http://';
        var base_uri = scheme+script_uri.replace(scheme,'').split('/').slice(0,-2).join('/');
        var system_uri = scheme+script_uri.replace(scheme,'').split('/').slice(0,-6).join('/');
        var index_uri = scheme+script_uri.replace(scheme,'').split('/').slice(0,-7).join('/');
        var arbors_uri = base_uri.substr(0, base_uri.lastIndexOf('/'));
        var views_uri = arbors_uri.substr(0, arbors_uri.lastIndexOf('/'));
        var modules_uri = views_uri+'/melons';
        var widgets_uri = views_uri+'/widgets';

        base_uri+'/js/jquery.rdfquery.rules-1.0.js',
        base_uri+'/js/jquery.RDFa.js',
        base_uri+'/js/form-validation.js?v=2',
        widgets_uri+'/nav/jquery.scalarrecent.js',
        widgets_uri+'/cookie/jquery.cookie.js',
        widgets_uri+'/api/scalarapi.js'
        widgets_uri+'/cookie/jquery.cookie.js'
        widgets_uri+'/notice/jquery.scalarnotice.js'
        widgets_uri+'/spinner/spin.min.js'
        widgets_uri+'/d3/d3.min.js'
        widgets_uri+'/mediaelement/annotorious.debug.js',
        widgets_uri+'/mediaelement/css/annotorious.css',
        widgets_uri+'/mediaelement/mediaelement.css',
        widgets_uri+'/mediaelement/jquery.mediaelement.js'
        widgets_uri+'/replies/replies.js'


        base_uri = "http://blackquotidian.supdigital.org/system/application/views/arbors/html5_RDFa"
        system_uri = "http://blackquotidian.supdigital.org/system"
        widgets_uri = "http://blackquotidian.supdigital.org/system/application/views/widgets"

         */

        final String baseUrlPath = "base_uri+";

        final String widgetUrlPath = "widgets_uri+";

        Reader filteredReader = FilterUtil.getReader(in, encoding);
        LineRewritingReader rewritingReader = new LineRewritingReader(filteredReader) {
            @Override
            public String rewriteLine(String line) {
                // handle tutorial-image part
                if (srcUrl.contains(localReadJSFile)) {
                    logger.debug3("Fei - rewriteLine" + line);

                    String serveContentUrl =  "/ServeContent?url=" + srcUrl.replace(localReadJSFile, "");
                    String serverBaseUrlUrl = "'" + serveContentUrl + "/arbors/html5_RDFa'";
                    String widgetUrlPathUrl = "'" + serveContentUrl + "/widgets'";

                    int baseUrlFound = line.indexOf(baseUrlPath);
                    int widgetUrlPathFound = line.indexOf(widgetUrlPath);

                    String replacedLine = line;
                    
                    // handle tutorial-image part
                    if (baseUrlFound > -1) {
                        logger.debug3("baseUrlFound = " + baseUrlFound);
                        logger.debug3("line = " + line);
                        logger.debug3("serveContentUrl = " + serveContentUrl + ", serverBaseUrlUrl = " + serverBaseUrlUrl);
                        replacedLine = replacedLine.replaceAll(baseUrlPath, serverBaseUrlUrl);
                        logger.debug3("baseUrlFound = " + replacedLine);

                    }

                    // handle tutorial-image part
                    if (widgetUrlPathFound  > -1) {
                        logger.debug3("baseUrlFound = " + baseUrlFound);
                        logger.debug3("line = " + line);
                        logger.debug3("(widgetUrlPathUrl = " + widgetUrlPathUrl + ", widgetUrlPath = " + widgetUrlPath);
                        replacedLine = replacedLine.replaceAll(widgetUrlPath, widgetUrlPathUrl);
                        logger.debug3("widgetUrlPath = " + replacedLine);
                    }

                    return replacedLine;

                }
                return line;
            }
        };
        return new ReaderInputStream(rewritingReader);
    }
}


