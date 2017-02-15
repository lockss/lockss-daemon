/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.ws.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.xml.bind.DatatypeConverter;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.hasher.HashResult;
import org.lockss.hasher.HashResult.IllegalByteArray;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.ImportPlugin;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.util.ByteArray;
import org.lockss.util.CIProperties;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.PropUtil;
import org.lockss.util.StreamUtil;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;
import org.lockss.ws.entities.ImportWsParams;
import org.lockss.ws.entities.ImportWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * The Import web service implementation.
 */
public class ImportServiceImpl implements ImportService {
  private static Logger log = Logger.getLogger(ImportServiceImpl.class);

  private static final String BASIC_AUTH_KEY = "BasicAuthorization";
  static final String CHECKSUM_KEY = "Checksum";

  private List<String> supportedMessageDigestAlgorithms = null;

  // The keys of properties to be loaded in the archival unit configuration.
  private List<String> auConfigKeys =
      Arrays.asList(PluginManager.AU_PARAM_DISPLAY_NAME);

  private LockssDaemon daemon = LockssDaemon.getLockssDaemon();

  /**
   * Imports a pulled file into an archival unit.
   * 
   * @param importParams
   *          An ImportWsParams with the parameters of the importing operation.
   * @return an ImportWsResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @Override
  public ImportWsResult importPulledFile(ImportWsParams importParams)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "importPulledFile(): ";
    if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "importParams = " + importParams);

    // Prepare the result to be returned.
    ImportWsResult wsResult = new ImportWsResult();

    // Get the user properties map.
    Map<String, String> properties =
	getAndValidateUserProperties(importParams, wsResult);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "properties = " + properties);

    if (wsResult.getIsSuccess() != null) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
      return wsResult;
    }

    // The temporary file, if needed.
    File tmpFile = null;

    InputStream input = null;

    try {
      // Get the URL of the file to be imported.
      String sourceUrl = importParams.getSourceUrl();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sourceUrl = " + sourceUrl);

      // Open a stream to the file to be imported.
      URL url = null;
      URLConnection urlConnection = null;

      try {
	url = new URL(sourceUrl);
	urlConnection = url.openConnection();
	String uInfo = url.getUserInfo();
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "uInfo = " + uInfo);

	if (uInfo != null) {
	  String basicAuth = "Basic "
	      + DatatypeConverter.printBase64Binary(uInfo.getBytes());
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "basicAuth = " + basicAuth);

	  urlConnection.setRequestProperty("Authorization", basicAuth);
	} else if (properties.containsKey(BASIC_AUTH_KEY)) {
	  String basicAuth = properties.get(BASIC_AUTH_KEY);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "basicAuth = " + basicAuth);

	  urlConnection.setRequestProperty("Authorization", basicAuth);
	}

	input = urlConnection.getInputStream();
      } catch (MalformedURLException mue) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Malformed source URL: " + mue.getMessage());
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	return wsResult;
      } catch (IOException ioe) {
	wsResult.setIsSuccess(Boolean.FALSE);

	if (urlConnection == null) {
	  wsResult.setMessage("Cannot open connection to source URL: "
	      + ioe.getMessage());
	} else {
	  wsResult.setMessage("Cannot open input stream to source URL: "
	      + ioe.getMessage());
	}

	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	return wsResult;
      }

      HashResult hashResult = validateChecksumRequest(properties, wsResult);

      if (wsResult.getIsSuccess() != null) {
        if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
        return wsResult;
      }

      // Check whether the content checksum needs to be checked.
      if (hashResult != null) {
	// Yes: Save the content in a temporary location while checking the
	// checksum.
	tmpFile = checkContent(input, hashResult, wsResult);

	if (wsResult.getIsSuccess() != null) {
	  if (log.isDebug2())
	    log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	  return wsResult;
	} else {
	  if (input != null) {
	    try {
	      input.close();
	    } catch (IOException ioe) {
	      log.warning("Exception caught closing input stream", ioe);
	    }
	  }

	  input = new FileInputStream(tmpFile);
	}
      }

      // Import the file.
      importFile(input, importParams, properties, wsResult);
    } catch (Exception e) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Error: " + e.getMessage());
    } finally {
      if (input != null) {
	try {
	  input.close();
	} catch (IOException ioe) {
	  log.warning("Exception caught closing input stream", ioe);
	}
      }

      if (tmpFile != null) {
	if (!tmpFile.delete()) {
	  tmpFile.deleteOnExit();
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
    return wsResult;
  }

  /**
   * Imports a pushed file into an archival unit.
   * 
   * @param importParams
   *          An ImportWsParams with the parameters of the importing operation.
   * @return an ImportWsResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @Override
  public ImportWsResult importPushedFile(ImportWsParams importParams)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "importPushedFile(): ";
    if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "importParams = " + importParams);

    // Prepare the result to be returned.
    ImportWsResult wsResult = new ImportWsResult();

    // Get the user properties map.
    Map<String, String> properties =
	getAndValidateUserProperties(importParams, wsResult);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "properties = " + properties);

    if (wsResult.getIsSuccess() != null) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
      return wsResult;
    }

    // The temporary file, if needed.
    File tmpFile = null;

    InputStream input = null;

    try {
      // Get the wrapper of the pushed file to be imported.
      DataHandler dataHandler = importParams.getDataHandler();

      // Open a stream to the pushed file to be imported.
      try {
	input = dataHandler.getInputStream();
      } catch (IOException ioe) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Cannot open input stream to pushed content: "
	      + ioe.getMessage());

	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	return wsResult;
      }

      HashResult hashResult = validateChecksumRequest(properties, wsResult);

      if (wsResult.getIsSuccess() != null) {
        if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
        return wsResult;
      }

      // Check whether the content checksum needs to be checked.
      if (hashResult != null) {
	// Yes: Save the content in a temporary location while checking the
	// checksum.
	tmpFile = checkContent(input, hashResult, wsResult);

	if (wsResult.getIsSuccess() != null) {
	  if (log.isDebug2())
	    log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	  return wsResult;
	} else {
	  if (input != null) {
	    try {
	      input.close();
	    } catch (IOException ioe) {
	      log.warning("Exception caught closing input stream", ioe);
	    }
	  }

	  input = new FileInputStream(tmpFile);
	}
      }

      // Import the file.
      importFile(input, importParams, properties, wsResult);
    } catch (Exception e) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Error: " + e.getMessage());
    } finally {
      if (input != null) {
	try {
	  input.close();
	} catch (IOException ioe) {
	  log.warning("Exception caught closing input stream", ioe);
	}
      }

      if (tmpFile != null) {
	if (!tmpFile.delete()) {
	  tmpFile.deleteOnExit();
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
    return wsResult;
  }

  /**
   * Provides the names of the supported checksum algorithms.
   * 
   * @return a String[] with the names of the supported checksum algorithms.
   * @throws LockssWebServicesFault
   */
  @Override
  public String[] getSupportedChecksumAlgorithms() {
    return getSupportedMessageDigestAlgorithms()
	.toArray(new String[getSupportedMessageDigestAlgorithms().size()]);
  }

  /**
   * Provides the validated user-specified properties map.
   * 
   * @param importParams
   *          An ImportWsParams with the parameters of the importing operation.
   * @param wsResult
   *          An ImportWsResult with any validation errors.
   * @return a Map<String, String> with the validated properties map.
   */
  private Map<String, String> getAndValidateUserProperties(
      ImportWsParams importParams, ImportWsResult wsResult) {
    final String DEBUG_HEADER = "getAndValidateUserProperties(): ";

    String[] properties = importParams.getProperties();
    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "properties = " + Arrays.toString(properties));
    }

    Map<String, String> resultMap = new HashMap<String, String>();

    if (properties != null && properties.length > 0) {
      for (String property : properties) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "property = " + property);

	int keyValueSeparator = property.trim().indexOf("=");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "keyValueSeparator = " + keyValueSeparator);

	if (keyValueSeparator > 0) {
	  String key = property.substring(0, keyValueSeparator).trim();
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "key = " + key);

	  String value = property.substring(keyValueSeparator + 1).trim();
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "value = " + value);

	  resultMap.put(key, value);
	} else {
	  wsResult.setIsSuccess(Boolean.FALSE);
	  wsResult.setMessage("Missing key/value separator in property '"
	      + property + "'");
	  break;
	}
      }
    }

    return resultMap;
  }

  private HashResult validateChecksumRequest(Map<String, String> properties,
      ImportWsResult wsResult) {
    
    // Check whether no checksum request is made.
    if (!properties.containsKey(CHECKSUM_KEY)) {
      return null;
    }

    try {
      HashResult hashResult = HashResult.make(properties.get(CHECKSUM_KEY));
      String algorithm = hashResult.getAlgorithm();

      // Check for an invalid algorithm.
      if (StringUtil.isNullString(algorithm)) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Invalid checksum algorithm '" + algorithm + "'");
	return null;
      }

      // Check for an unsupported algorithm.
      if (!getSupportedMessageDigestAlgorithms().contains(algorithm)) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Unsupported checksum algorithm '" + algorithm
	    + "'");
	return null;
      }

      return hashResult;
    } catch (IllegalByteArray ibae) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage(ibae.getMessage());
      return null;
    }
  }

  private List<String> getSupportedMessageDigestAlgorithms() {
    final String DEBUG_HEADER = "getSupportedMessageDigestAlgorithms(): ";
    if (supportedMessageDigestAlgorithms == null) {
      supportedMessageDigestAlgorithms = new ArrayList<String>();

      for (Provider provider : Security.getProviders()) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "provider = " + provider);

        for (Provider.Service service : provider.getServices()) {
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "service = " + service);

          if ("MessageDigest".equals(service.getType())) {
            supportedMessageDigestAlgorithms.add(service.getAlgorithm());
            if (log.isDebug3()) log.debug3(DEBUG_HEADER + "algorithm = "
        	+ service.getAlgorithm());

            String displayService = service.toString();
            int beginIndex =
        	displayService.indexOf("aliases: [") + "aliases: [".length();

            if (beginIndex >= "aliases: [".length()) {
              int endIndex = displayService.indexOf("]", beginIndex);
              String aliases = displayService.substring(beginIndex, endIndex);

              for (String alias : StringUtil.breakAt(aliases, ",")) {
                supportedMessageDigestAlgorithms.add(alias.trim());
                if (log.isDebug3())
                  log.debug3(DEBUG_HEADER + "alias = " + alias.trim());
              }
            }
          }
        }
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "supportedMessageDigestAlgorithms = "
	  + supportedMessageDigestAlgorithms);
    }

    return supportedMessageDigestAlgorithms;
  }

  private File checkContent(InputStream input, HashResult hashResult,
      ImportWsResult wsResult) {
    File tmpFile = null;

    try {
      tmpFile = File.createTempFile("imported", "", null);
    } catch (IOException ioe) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Cannot create temporary file: " + ioe.getMessage());
      return tmpFile;
    }

    MessageDigest md = null;

    try {
      md = MessageDigest.getInstance(hashResult.getAlgorithm());

      if (md == null) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("No digest could be obtained");
	return tmpFile;
      }
    } catch (NoSuchAlgorithmException nsae) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Cannot create message digest: " + nsae.getMessage());
      return tmpFile;
    }

    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(tmpFile);
    } catch (FileNotFoundException fnfe) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Cannot find temporary file: " + fnfe.getMessage());
      IOUtil.safeClose(fos);
      return tmpFile;
    }

    try {
      StreamUtil.copy(input, fos, -1, null, true, md);
    } catch (IOException ioe) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Error writing temporary file: " + ioe.getMessage());
      return tmpFile;
    } finally {
      IOUtil.safeClose(fos);
    }

    if (!hashResult.equalsBytes(md.digest())) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Checksum error: Expected = '"
	  + ByteArray.toHexString(hashResult.getBytes()) + "', Found = '"
	  + ByteArray.toHexString(md.digest()) + "'");
    }

    return tmpFile;
  }

  /**
   * Imports a file.
   * 
   * @param input
   *          An InputStream pointing to the content to be imported.
   * @param importParams
   *          An ImportWsParams with the parameters of the importing operation.
   * @param properties
   *          A Map<String, String> with the user-specified properties.
   * @param wsResult
   *          An ImportWsResult with any errors.
   */
  private void importFile(InputStream input, ImportWsParams importParams,
      Map<String, String> properties, ImportWsResult wsResult) {
    final String DEBUG_HEADER = "importFile(): ";
    // Get the specified base URL path.
    String baseUrlPath = importParams.getTargetId();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "baseUrlPath = " + baseUrlPath);

    if (baseUrlPath == null) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Null base_url path");
      return;
    } else if (baseUrlPath.trim().length() == 0) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Empty base_url path '" + baseUrlPath + "'");
      return;
    }

    // Get the identifier of the archival unit where to store the imported
    // file.
    String auId = makeAuId(baseUrlPath);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

    // Process the user-specified properties.
    CIProperties headers = new CIProperties();

    // Set the import timestamp.
    headers.put(CachedUrl.PROPERTY_FETCH_TIME, Long.toString(TimeBase.nowMs()));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "headers = " + headers);

    // Get the plugin manager.
    PluginManager pluginMgr = daemon.getPluginManager();

    // Get the archival unit, if it exists.
    ArchivalUnit au = pluginMgr.getAuFromId(auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

    // Check whether the archival unit does not exist.
    if (au == null) {
      //  Yes: Create it.
      au = createAu(auId, pluginMgr, properties, headers, wsResult);

      if (wsResult.getIsSuccess() != null) {
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	return;
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);
    } else {
      // No.
      if (AuUtil.getAuState(au).hasCrawled()) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Target Archival Unit has crawled already");
	return;
      }

      processProperties(properties, null, headers, wsResult);

      if (wsResult.getIsSuccess() != null) {
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	return;
      }
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "headers = " + headers);

    // TODO: Mark the AU as non-crawlable and non-repairable.
    // TL email 11/17/2015 1:24 PM:
    // We need to add a
    // real property to the AU to precent crawling and repair from publisher,
    // and you'll need to set that when you create an AU for import.  I'm still
    // contemplating where to put that.

    // Create the URL cacher.
    UrlCacher uc = null;

    try {
      // TODO: Validate targetUrl?
      // TL email 11/16/2015 1:46 PM:
      // As long as we're using URL and not URI we'll still be stuck accepting
      // only legal URLs that have a URLStreamHandlerFactory.  The WS import()
      // method should probably check legality before it accepts the file, rather
      // than let bogus URLs cause errors later.
      /*String targetUrl = importParams.getTargetUrl();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "targetUrl = " + targetUrl);
      URL url = new URL(targetUrl);
      String protocol = url.getProtocol();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "protocol = " + protocol);

      URLStreamHandler ush = URL.getURLStreamHandler(protocol);*/

      uc = au.makeUrlCacher(new UrlData(input, headers,
	  importParams.getTargetUrl()));
      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "uc = " + uc);
	log.debug3(DEBUG_HEADER + "uc.getClass() = " + uc.getClass());
      }
    } catch (Exception e) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Error making URL cacher: " + e.getMessage());
      return;
    }

    // Store the file.
    try {
      uc.storeContent();
    } catch (Exception e) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Error storing content: " + e.getMessage());
      return;
    }

    // Alert the NodeManager that the content crawl has finished.
    daemon.getNodeManager(au).newContentCrawlFinished();

    AuUtil.getAuContentSize(au, false);
    AuUtil.getAuDiskUsage(au, false);

    // Report success.
    wsResult.setIsSuccess(Boolean.TRUE);
    wsResult.setMessage("Imported");
  }

  static String makeAuId(String baseUrlPath) {
    String baseUrlHost = "import%2Elockss%2Eorg";
    return ImportPlugin.PLUGIN_KEY + "&base_url~http%3A%2F%2F" + baseUrlHost
	+ "%2F" + baseUrlPath;
  }

  private ArchivalUnit createAu(String auId, PluginManager pluginMgr,
      Map<String, String> properties, CIProperties headers,
      ImportWsResult wsResult) {
    final String DEBUG_HEADER = "createAu(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "properties = " + properties);
      log.debug2(DEBUG_HEADER + "headers = " + headers);
    }

    ArchivalUnit au = null;

    // Get the plugin identifier.
    String pluginId = null;

    try {
      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
    } catch (Exception e) {
      wsResult.setIsSuccess(Boolean.FALSE);
      wsResult.setMessage("Error getting the plugin identifier: "
	  + e.getMessage());
      return null;
    }

    Plugin plugin = null;

    if (ImportPlugin.PLUGIN_KEY.equals(pluginId)) {
      // Get the plugin.
      plugin = pluginMgr.getImportPlugin();

      if (plugin == null) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Invalid pluginId '" + pluginId + "'");
	return null;
      }

      // Now that the Import plugin has been loaded, get the archival unit
      // again, if it exists.
      au = pluginMgr.getAuFromId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

      // Check whether the archival unit now exists.
      if (au != null) {
	processProperties(properties, null, headers, wsResult);

	if (wsResult.getIsSuccess() != null) {
	  if (log.isDebug2())
	    log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	  return null;
	}
      }
    } else {
      // Get the plugin.
      plugin = pluginMgr.getPlugin(pluginId);

      if (plugin == null) {
	boolean pluginLoaded = pluginMgr.ensurePluginLoaded(pluginId);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "pluginLoaded = " + pluginLoaded);

	if (pluginLoaded) {
	  plugin = pluginMgr.getPlugin(pluginId);
	}

	if (plugin == null) {
	  wsResult.setIsSuccess(Boolean.FALSE);
	  wsResult.setMessage("Invalid pluginId '" + pluginId + "'");
	  return null;
	}
      }
    }
  
    // Check whether the archival unit still does not exist.
    if (au == null) {
      // Yes: Get the archival unit key.
      String auKey = null;

      try {
	auKey = PluginManager.auKeyFromAuId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);
      } catch (IllegalArgumentException iae) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Error getting AuKey: " + iae.getMessage());
	return null;
      }

      // Get the properties encoded in the archival unit key.
      Properties props = null;

      try {
	props = PropUtil.canonicalEncodedStringToProps(auKey);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "props = " + props);
      } catch (IllegalArgumentException iae) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Invalid AuKey: " + iae.getMessage());
	return null;
      }

      // Initialize the archival unit configuration.
      Configuration auConfig = null;

      try {
	auConfig = ConfigManager.fromPropertiesUnsealed(props);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auConfig = " + auConfig);
      } catch (RuntimeException re) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Invalid AuKey properties: " + re.getMessage());
	return null;
      }

      processProperties(properties, auConfig, headers, wsResult);

      if (wsResult.getIsSuccess() != null) {
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
	return null;
      }

      // Add the archival unit.
      try {
	au = pluginMgr.createAndSaveAuConfiguration(plugin, auConfig);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);
      } catch (Exception e) {
	wsResult.setIsSuccess(Boolean.FALSE);
	wsResult.setMessage("Error creating AU: " + e.getMessage());
	return null;
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "au = " + au);
    return au;
  }

  /**
   * Loads the user-specified properties into the archival unit configuration or
   * the headers.
   * 
   * @param properties
   *          A String[] with the user-specified properties.
   * @param auConfig
   *          A Configuration with the target archival unit configuration.
   * @param headers
   *          A CIProperties with the headers.
   * @param wsResult
   *          An ImportWsResult with any validation errors.
   */
  private void processProperties(Map<String, String> properties,
      Configuration auConfig, CIProperties headers, ImportWsResult wsResult) {
    final String DEBUG_HEADER = "processProperties(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "properties = " + properties);
      log.debug2(DEBUG_HEADER + "auConfig = " + auConfig);
      log.debug2(DEBUG_HEADER + "headers = " + headers);
    }

    if (properties != null && properties.size() > 0) {
      for (String key : properties.keySet()) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "key = " + key);

	String value = properties.get(key);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "value = " + value);

	if (auConfig != null && auConfigKeys.contains(key)) {
	  auConfig.put(key, value);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "property '" + key + "="
	      + value + "' stored in auConfig");
	} else {
	  headers.put(key, value);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "property '" + key + "="
	      + value + "' stored in headers");
	}
      }
    }
  }
}
