package org.lockss.plugin.respediatrica;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.util.Logger;

public class ResPediatricaUrlNormalizer implements UrlNormalizer {
    private static final Logger log = Logger.getLogger(org.lockss.plugin.maffey.MaffeyUrlNormalizer.class);

    public String normalizeUrl(String url,
                               ArchivalUnit au)
            throws PluginException {

        log.debug3("Normolizing the url from: " + url);

        url = url.replaceFirst("\\.css\\?v=[0-9]+", ".css");
        url = url.replaceFirst("\\.jpg\\?v=[0-9]+", ".jpg");
        url = url.replaceFirst("\\.jpeg\\?v=[0-9]+", ".jpeg");
        url = url.replaceFirst("\\.png\\?v=[0-9]+", ".png");
        url = url.replaceFirst("\\.js\\?v=[0-9]+", ".js");

        log.debug3("Normolizing the url to: " + url);

        return url;
    }

}
