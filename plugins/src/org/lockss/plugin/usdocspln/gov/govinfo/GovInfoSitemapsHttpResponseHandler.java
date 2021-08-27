package org.lockss.plugin.usdocspln.gov.govinfo;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GovInfoSitemapsHttpResponseHandler implements CacheResultHandler {

  private static final Logger logger = Logger.getLogger(GovInfoSitemapsHttpResponseHandler.class);

  /*examples:
    app/details/lib/bootstrap/ico/apple-touch-icon-167.png
    app/details/lib/bootstrap/ico/apple-touch-icon-76.png
    apple-touch-icon-72.png
    sites/all/apple-touch-icon-152.png
    sites/all/apple-touch-icon-72.png
    sites/all/themes/custom/misc/menu-collapsed.png
    sites/all/themes/custom/misc/menu-expanded.png
    sites/all/themes/custom/misc/menu-leaf.png
    app/dynamic/stylesheets/fonts/glyphicons-halflings-regular.svg
    sites/all/themes/custom/bootstrap-fdsys/bootstrap/fonts/glyphicons-halflings-regular.eot
    sites/all/themes/custom/bootstrap-fdsys/bootstrap/fonts/glyphicons-halflings-regular.woff2
    sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.eot
    sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.eot%3Fv=4.3.0
    sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.svg%3Fv=4.3.0
   */
  protected static final Pattern NON_FATAL_GRAPHICS_PATTERN =
      Pattern.compile("\\.(bmp|css|eot|gif|ico|jpe?g|js|png|svg|tif?f|ttc|ttf|woff.?|dfont|otf)");

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to GovInfoSitemapsHttpResponseHandler.init()");
  }

  public static final class NoFailRetryableNetworkException_2_10S
      extends CacheException.RetryableNetworkException_2_10S {

    private static final long serialVersionUID = 1L;

    public NoFailRetryableNetworkException_2_10S(String message) {
      super(message);
    }

    @Override
    protected void setAttributes() {
      super.setAttributes();
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
      logger.debug2(responseCode + " " + url);
      switch (responseCode) {
        case 404:
        case 504:
          Matcher fmat = NON_FATAL_GRAPHICS_PATTERN.matcher(url);
          if (fmat.find()) {
            return new NoFailRetryableNetworkException_2_10S(responseCode + " Forbidden (non-fatal)");
          }
          return new CacheException.RetrySameUrlException(responseCode + " Not Found");

        default:
          logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
          throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
      }
    }

  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, final Exception ex)
      throws PluginException {
    logger.warning("Unexpected exception (" + ex + ") in handleResult(): AU " + au.getName() + "; URL " + url);
    throw new UnsupportedOperationException("Unexpected exception (" + ex + ")");
  }
}