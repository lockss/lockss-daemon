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
import okhttp3.Headers;
import okhttp3.MultipartReader;
import okhttp3.MultipartReader.Part;
import okhttp3.Response;

import org.apache.commons.io.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.laaws.client.ApiException;
import org.lockss.util.*;
import static org.lockss.config.ConfigManager.*;

public class ConfigFileMover extends Worker {

  private static final Logger log = Logger.getLogger(ConfigFileMover.class);
  static final String COPIED_CONTENT_MARKER = "### Copied from LOCKSS 1.0 by migrator ";

  static final String SECTION_NAME_CONTENT_SERVERS = "content_servers";
  static final String SECTION_NAME_CRAWL_PROXY = "crawl_proxy";
  static final String SECTION_NAME_EXPERT = "expert";
  static final String SECTION_NAME_PLUGIN = "plugin";
  static final String SECTION_NAME_PROXY_IP_ACCESS = "proxy_ip_access";
  static final String SECTION_NAME_TITLE_DB = "titledb";
  static final String SECTION_NAME_UI_IP_ACCESS = "ui_ip_access";

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
          log.debug2("Skipping, not present; " + file);
          continue;
        }
      } catch (IOException e) {
        String msg = "Couldn't read V1 config file: " + file;
        log.error(msg, e);
        auMover.logReportAndError(msg);
      }
      String v2Content = readV2Config(section);
      if (v2Content == null) {
        moveConfigFile(section, v1Content);
      } else {
        mergeConfigFile(section, v1Content, v2Content);
      }
    }
  }

  private void moveConfigFile(String section, String v2Content) {
    log.debug3("Copying config file; " + section);
    try {
      switch (section) {
      case SECTION_NAME_EXPERT:
        writeV2ConfigFile(section, StringUtil.commentize(v2Content));
        break;
      default:
        writeV2ConfigFile(section, v2Content);
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

  private void mergeConfigFile(String section, String v1Content,
                               String v2Content) {
    log.debug3("Merging config section; " + section);
    try {
      switch (section) {
      case SECTION_NAME_EXPERT:
        if (v2Content.indexOf(COPIED_CONTENT_MARKER) < 0) {
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
      String msg = "Couldn't store V2 config section: " + section;
      log.error(msg, e);
      auMover.logReportAndError(msg);
    }
  }

  String appendComment(String section, String v1Content, String v2Content) {
    return v2Content + "\n\n" + COPIED_CONTENT_MARKER + " " +
      auMover.nowTimestamp() + "\n\n" + StringUtil.commentize(v1Content);
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

  private String readV1Config(String name) throws IOException {
    File file = cfgManager.getCacheConfigFile(name);
    if (!file.exists()) {
      log.debug2("doesn't exist");
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
    } catch (ApiException | IOException e) {
      String msg = "Couldn't read V2 config file: " + section;
      log.error(msg, e);
      auMover.logReportAndError(msg);
      return null;
    }
  }

}
