package org.lockss.plugin.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.util.ByteArray;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class ChecksumVerifier {
  public static final String PARAM_CHECKSUM_ALGORITHM =
      Configuration.PREFIX + "baseuc.checksumAlgorithm";
  public static final String DEFAULT_CHECKSUM_ALGORITHM = null;
  private static final int BUFFER_SIZE = 4096;
  
  private MessageDigest checksumProducer;
  protected static Logger logger = Logger.getLogger("ChecksumVerifier");
  
  public ChecksumVerifier() throws Exception {
    checksumProducer = getChecksumProducer();
    if (checksumProducer == null )
      throw new Exception("Configuration error, could not locate checksum algorithm");
  }
  
  public void verifyAllAUs() throws IOException, IllegalStateException {
    LockssDaemon daemon = LockssDaemon.getLockssDaemon();
    if (daemon == null) {
      throw new IllegalStateException("no LOCKSS daemon");
    }
    
    for (ArchivalUnit au : daemon.getPluginManager().getAllAus()) {
      if (!au.getAuId().equals(RootPageProducer.CATALOG_AU_ID)) {
        verifySingleAU(au);
      }
    }
  }
    
  void verifySingleAU(ArchivalUnit au) throws IOException {
    CachedUrlSet cus = au.getAuCachedUrlSet();
    Iterator iter = cus.contentHashIterator();
    while (iter.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode) iter.next();
      CachedUrl cu = AuUtil.getCu(node);
      if (cu != null ) {
        if (cu.hasContent()) {
          //cu.getVersions() -> array of CachedUrl
          //cu.getVersion() -> int version number
          String url = cu.getUrl();
          CIProperties headers = cu.getProperties();
          String checksum = headers.getProperty(CachedUrl.PROPERTY_CHECKSUM);
          /*checksumMatched may be false in the following two cases:
              a) when the re-calculated hash differs from the stored on
              b) when there is no stored hash in the properties of this URL
          */
          boolean checksumMatched = false;
          // only process entries with checksums
          if (!StringUtil.isNullString(checksum)) {
            InputStream is = cu.getUnfilteredInputStream();
            if (verify(is, checksum)) {
              checksumMatched = true;
            }
            is.close();
          }
          if ( ! checksumMatched ) {
            //* how to mark this URL as damaged?
          }
        }
        break;
      }
    }
  }

  private boolean verify(InputStream is, String stored_hash) throws IOException {
    byte[] buf = new byte[BUFFER_SIZE];
    int nread;
    checksumProducer.reset();
    while (true) {
      nread = is.read(buf, 0, BUFFER_SIZE);
      if (nread <= 0) {
        break;
      }
      checksumProducer.update(buf, 0, nread);
    }
    byte bdigest[] = checksumProducer.digest();
    String sdigest = ByteArray.toHexString(bdigest);
    return sdigest.equalsIgnoreCase(stored_hash);
  }

  MessageDigest getChecksumProducer() {
    MessageDigest checksumProducer = null;
    String checksumAlgorithm =
      CurrentConfig.getParam(PARAM_CHECKSUM_ALGORITHM,
                 DEFAULT_CHECKSUM_ALGORITHM);
    if (!StringUtil.isNullString(checksumAlgorithm)) {
      try {
        checksumProducer = MessageDigest.getInstance(checksumAlgorithm);
        return checksumProducer;
      } catch (NoSuchAlgorithmException ex) {
        logger.warning(String.format("Checksum algorithm %s not found, checksuming disabled", checksumAlgorithm));
      }
    }
    return null;
  }  
}
