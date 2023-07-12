package org.lockss.plugin.silverchair;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.BaseUrlHttpHttpsUrlNormalizer;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SilverchairScholarlyPublishingCollectiveUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {

    private static final Logger log = Logger.getLogger(SilverchairScholarlyPublishingCollectiveUrlNormalizer .class);

    /*
    Silverchair make version-based-directory for css/javascript/image files,
    Here is a list of examples:
    https://scholarlypublishingcollective.org/psup/victorians-institute/article-abstract/doi/10.5325/victinstj.49.2022.0003/319237/Literacy-and-Preservation-in-The-Death-and-Burial?redirectedFrom=fulltext
    <script src="//dupspc.silverchair-cdn.com/Themes/Silver/app/vendor/v-638221317988379400/jquery-migrate-3.1.0.min.js" type="text/javascript"></script>
      <link rel="apple-touch-icon" sizes="180x180" href="//dupspc.silverchair-cdn.com/Themes/Client/app/img/favicons/v-638221317496185446/apple-touch-icon.png">
      <link rel="icon" type="image/png" href="//dupspc.silverchair-cdn.com/Themes/Client/app/img/favicons/v-638221317496185446/favicon-32x32.png" sizes="32x32">
      <link rel="icon" type="image/png" href="//dupspc.silverchair-cdn.com/Themes/Client/app/img/favicons/v-638221317496185446/favicon-16x16.png" sizes="16x16">
      <link rel="icon" href="//dupspc.silverchair-cdn.com/Themes/Client/app/img/favicons/v-638221317496436198/favicon.ico">
      <link rel="manifest" href="//dupspc.silverchair-cdn.com/Themes/Client/app/img/favicons/v-638221317496436198/manifest.json">
      <link rel="stylesheet" type="text/css" href="//dupspc.silverchair-cdn.com/Themes/Client/app/css/v-638234166373754454/site.min.css" />
      <link rel="stylesheet" type="text/css" href="//dupspc.silverchair-cdn.com/Themes/Silver/app/icons/v-638221317960966804/style.css" />
      <link rel="stylesheet" type="text/css" href="//dupspc.silverchair-cdn.com/Themes/Client/app/css/v-638221317496135940/bg_img.css" />
      <link href="//dupspc.silverchair-cdn.com/data/SiteBuilderAssetsOriginals/Live/CSS/umbrella/v-637931482159725819/spc_Site.css" rel="stylesheet" type="text/css" />
      <link href="//dupspc.silverchair-cdn.com/data/SiteBuilderAssetsOriginals/Live/CSS/psup/v-637740498783989584/Site.css" rel="stylesheet" type="text/css" />

        https://oup.silverchair-cdn.com/UI/app/fonts/v-638221317627781912/icomoon.eot?l8ds64
        https://oup.silverchair-cdn.com/UI/app/fonts/v-638221317627781912/icomoon.svg?l8ds64
        https://oup.silverchair-cdn.com/UI/app/fonts/v-638221317627781912/icomoon.ttf?l8ds64
        https://oup.silverchair-cdn.com/UI/app/fonts/v-638221317627781912/icomoon.woff?l8ds64
        https://oup.silverchair-cdn.com/UI/app/fonts/v-638221317627781912/icons.css
     */

    String[] excludedFileTypes = new String[] {"css", "js", "ico", "png", "json", "bmp", "eot", "gif", "ico", "jpeg", "jpg", "otf", "png", "svg", "tiff", "tif","ttf", "woff", "wof"};

    protected static Pattern VERSIONED_DIRECTORY_NAME =
            Pattern.compile("(.*)\\/(v-\\d+)/(.*)\\.(css|js|ico|png|json|bmp|css|eot|gif|ico|jpe\\?g|js|otf|png|svg|tif\\?f|ttf|woff\\?)",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public String additionalNormalization(String url, ArchivalUnit au)
            throws PluginException {

        String newUrl = url;

        Matcher m1 = VERSIONED_DIRECTORY_NAME.matcher(url);

        if (StringUtils.indexOfAny(url, excludedFileTypes) > -1) {

            log.debug3("Version based diretory check: UrlNormalizer url = " + url );
            if (m1.find()) {
                newUrl = m1.group(1) + "/" + m1.group(3) + "." + m1.group(4);

                log.debug3("Version based diretory check: UrlNormalizer newUrl = " + newUrl );
            }

            return newUrl;
        }

        return url;
    }
}

