package org.lockss.db;

import java.io.File;
import java.util.Properties;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;
import org.lockss.util.PlatformUtil;

public class SchemaDocDbCreator {

  /**
   * Main method.
   * @param args
   */
  public static void main(String[] args) throws Exception {
    String tempDirPath = 
	new File(PlatformUtil.getSystemTempDir(), "schema_doc_db").toString();
    String arg;

    for (int i = 0; i < args.length; i++) {
      arg = args[i];

      if (i < args.length - 1 && "-d".equals(arg)) {
	tempDirPath = args[++i];
      }
    }

    ConfigManager.makeConfigManager();
    Logger.resetLogs();
    LockssDaemon daemon = new MockLockssDaemon() {};

    // Set the database log.
    System.setProperty("derby.stream.error.file",
		       new File(tempDirPath, "derby.log").getAbsolutePath());

    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    DbManager dbManager = daemon.getDbManager();
    dbManager.startService();
    dbManager.stopService();
  }
}
