/*

Copyright (c) 2024, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.laaws;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.text.*;
import okhttp3.Headers;
import okhttp3.MultipartReader;
import okhttp3.MultipartReader.Part;
import okhttp3.Response;

import org.apache.commons.io.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.laaws.client.ApiException;
import org.lockss.proxy.*;
import org.lockss.servlet.*;
import org.lockss.util.*;
import static org.lockss.config.ConfigManager.*;
import static org.lockss.laaws.MigrationConstants.*;

public class ConfigFileMover extends Worker {

  private static final Logger log = Logger.getLogger(ConfigFileMover.class);

  static final String COPIED_CONTENT_MARKER_TEMPLATE =
    "### Copied from LOCKSS %s to LOCKSS %s by migrator, %s";
  static final Pattern COPIED_CONTENT_MARKER_PATTERN =
    Pattern.compile("### Copied from LOCKSS .* to LOCKSS .* by migrator");
  static final String TRANSFORMED_CONTENT_SERVERS_TEMPLATE =
    "### Adapted from LOCKSS %s to LOCKSS %s by migrator, %s";


  private static final Map<String, String> configSectionMap =
    new LinkedHashMap<String, String>() {{
      put(SECTION_NAME_UI_IP_ACCESS, CONFIG_FILE_UI_IP_ACCESS);
      put(SECTION_NAME_PROXY_IP_ACCESS, CONFIG_FILE_PROXY_IP_ACCESS);
      put(SECTION_NAME_CONTENT_SERVERS, CONFIG_FILE_CONTENT_SERVERS);
      put(SECTION_NAME_CRAWL_PROXY, CONFIG_FILE_CRAWL_PROXY);
      put(SECTION_NAME_EXPERT, CONFIG_FILE_EXPERT);
      put(SECTION_NAME_PLUGIN, CONFIG_FILE_PLUGIN_CONFIG);
      put(SECTION_NAME_TITLE_DB, CONFIG_FILE_BUNDLED_TITLE_DB);
    }};

  static String sections[] = {
//     "audit_proxy",
    "content_servers",
    "crawl_proxy",
    "expert",
    "plugin",
    "proxy_ip_access",
    "titledb",
    "ui_ip_access",
  };

  ConfigManager cfgManager;

  public ConfigFileMover(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    cfgManager = LockssDaemon.getLockssDaemon().getConfigManager();
  }

  public void run() {
    log.debug2("Starting Config file Mover: ");
    for (String section : configSectionMap.keySet()) {
      String file = configSectionMap.get(section);
      String v1Content = null;
      try {
        v1Content = readV1Config(file);
        if (v1Content == null) {
          log.debug("V1 config file not present, skipping: " + file);
          continue;
        }
      } catch (IOException e) {
        String msg = "Couldn't read V1 config file: " + file;
        log.error(msg, e);
        auMover.logReportAndError(msg);
        continue;
      }
      String v2Content = readV2Config(section);
      if (v2Content == null) {
        moveConfigFile(section, v1Content);
      } else {
        mergeConfigFile(section, v1Content, v2Content);
      }
    }
//     writeV2MigrationConfig();
  }

//   private void writeV2MigrationConfig() {
//     try {
//       Configuration v2MigConfig = auMover.buildV2MigrateConfig();
//       writeV2ConfigFile(SECTION_NAME_MIGRATION, v2MigConfig,
//                         "Enable V2 migration behavior during migration from V1");
//     } catch (ApiException | IOException e) {
//       String msg = "Couldn't set migration params in V2";
//       log.error(msg, e);
//       auMover.logReportAndError(msg + ": " + e);
//     }
//   }

  private void moveConfigFile(String section, String v1Content) {
    log.debug3("Copying config file; " + section);
    try {
      switch (section) {
      case SECTION_NAME_EXPERT:
        writeV2ConfigFile(section, StringUtil.commentize(v1Content));
        break;
      case SECTION_NAME_CONTENT_SERVERS:
        Configuration c = cfgManager.readCacheConfigFile(configSectionMap.get(section));
        writeV2ConfigFile(section, transformContentServers(section, c),
                          copiedConfigComment(TRANSFORMED_CONTENT_SERVERS_TEMPLATE));
        break;
      default:
        writeV2ConfigFile(section, v1Content);
        break;
      }
      String msg = "Copied config section: " + section;
      log.debug(msg);
      auMover.logReport(msg);
    } catch (ApiException | IOException e) {
      String msg = "Couldn't copy config section: " + section;
      log.error(msg, e);
      auMover.logReportAndError(msg);
    }
  }

  static Configuration transformContentServers(String section, Configuration c) {
    Configuration newContent = c.copy();
    for (Iterator<String> iter = c.keyIterator(); iter.hasNext();) {
      String key = iter.next();
      String val = c.get(key);
      switch (key) {
      case ContentServletManager.PARAM_PORT:
        newContent.put(key, ""+V2_DEFAULT_CONTENTSERVLET_PORT);
        break;
      case ProxyManager.PARAM_PORT:
        newContent.put(key, ""+V2_DEFAULT_PROXY_PORT);
        break;
      case ProxyManager.PARAM_SSL_PORT:
        {
          int v = c.getInt(key, -1);
          if (v > 0) {
            newContent.put(key, ""+V2_DEFAULT_PROXY_SSL_PORT);
          }
        }
        break;
      case AuditProxyManager.PARAM_PORT:
        newContent.put(key, ""+V2_DEFAULT_AUDIT_PROXY_PORT);
        break;
      case AuditProxyManager.PARAM_SSL_PORT:
        {
          int v = c.getInt(key, -1);
          if (v > 0) {
            newContent.put(key, ""+V2_DEFAULT_AUDIT_PROXY_SSL_PORT);
          }
          break;
        }
      default:
        newContent.put(key, val);
      }
    }
    return newContent;
  }


  private boolean isMigrated(String v2Content) {
    Matcher m = COPIED_CONTENT_MARKER_PATTERN.matcher(v2Content);
    return m.find();
  }

  private void mergeConfigFile(String section, String v1Content,
                               String v2Content) {
    log.debug3("Merging config section; " + section);
    try {
      switch (section) {
      case SECTION_NAME_EXPERT:
        if (!isMigrated(v2Content)) {
          writeV2ConfigFile(section,
                            appendComment(section, v1Content, v2Content));
          String msg = "Merged config section: " + section;
          log.debug(msg);
          auMover.logReport(msg);
        } else {
          String msg = "Skipping already-merged config section; " + section;
          log.debug(msg);
          auMover.logReport(msg);
        }
        break;
      default:
        String msg = "Not merging config section; " + section;
        log.debug(msg);
        auMover.logReport(msg);
        break;
      }
    } catch (ApiException | IOException e) {
      String msg = "Couldn't store LOCKSS 2.0 config section: " + section;
      log.error(msg, e);
      auMover.logReportAndError(msg);
    }
  }

  String appendComment(String section, String v1Content, String v2Content) {
    return String.format("%s\n\n%s\n\n%s",
                         v2Content,
                         copiedConfigComment(COPIED_CONTENT_MARKER_TEMPLATE),
                         StringUtil.commentize(v1Content));

  }

  String copiedConfigComment(String template) {
    return String.format(template,
                         auMover.getLocalVersion(),
                         auMover.getCfgSvcVersion(),
                         auMover.nowTimestamp());
  }

  private void writeV2ConfigFile(String section, String v2Content)
      throws ApiException, IOException {
    File file = null;
    try {
      file = FileUtil.createTempFile("v1configfile", null);
      FileUtils.write(file,v2Content, Charset.forName("UTF-8"));
      cfgConfigApiClient.putConfig(section, file, null, null, null, null);
    } finally {
      FileUtil.safeDeleteFile(file);
    }
  }

  private void writeV2ConfigFile(String section, Configuration c, String header)
      throws ApiException, IOException {
    File file = null;
    try {
      file = FileUtil.createTempFile("v1configfile", null);
      try (OutputStream os = new FileOutputStream(file)) {
        c.store(os, header);
      }
      cfgConfigApiClient.putConfig(section, file, null, null, null, null);
    } finally {
      FileUtil.safeDeleteFile(file);
    }
  }

  private String readV1Config(String name) throws IOException {
    File file = cfgManager.getCacheConfigFile(name);
    if (!file.exists()) {
      log.debug3("V1 config file doesn't exist: " + name);
      return null;
    }
    String cont = StringUtil.fromFile(file);
    cont = StringUtil.normalizeEols(cont);

    if (log.isDebug3()) log.debug3("V1 content " + file + ": " + cont);
    if (StringUtil.isNullString(cont)) {
      return null;
    } else {
      return cont;
    }
  }

  private String readV2Config(String section) {
    try (Response resp =
         cfgConfigApiClient.getSectionConfigResponse(section,
                                                     null, null, null, null);
         MultipartReader mprdr = new MultipartReader(resp.body())) {
      Part part = mprdr.nextPart();
      if (log.isDebug3()) log.debug3("V2 part hdrs: " + section + ": "
                                     + part.headers());
      Reader rdr = new InputStreamReader(part.body().inputStream(),
                                         StandardCharsets.UTF_8);
      String res = StringUtil.normalizeEols(StringUtil.fromReader(rdr));
      if (StringUtil.isNullString(res)) {
        return null;
      }
      if (log.isDebug3()) log.debug3("V2 content: " + section + ": " + res);
      return res;
    } catch (ApiException e) {
      if (e.getCode() == 404) {
        log.debug("LOCKSS 2.0 config file not found: " + section);
        return null;
      }
      return null;
    } catch (IOException e) {
      String msg = "Couldn't read LOCKSS 2.0 config file: " + section;
      log.error(msg, e);
      auMover.logReportAndError(msg);
      return null;
    }
  }

}
