package org.lockss.laaws;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.lockss.proxy.*;
import org.lockss.servlet.ContentServletManager;

public class MigrationConstants {
  public static final String V2_PREFIX = "v2";
  public static final String V2_DOT = V2_PREFIX + ".";

  public static final String V2_PARAM_PROXY_PORT =
      ProxyManager.PARAM_PORT;
  public static final String V2_PARAM_PROXY_SSL_PORT =
      ProxyManager.PARAM_SSL_PORT;
  public static final String V2_PARAM_AUDIT_PROXY_PORT =
      AuditProxyManager.PARAM_PORT;
  public static final String V2_PARAM_AUDIT_PROXY_SSL_PORT =
      AuditProxyManager.PARAM_SSL_PORT;
  public static final String V2_PARAM_CONTENTSERVLET_PORT =
      ContentServletManager.PARAM_PORT;
  public static final String V2_PARAM_METADATADBMANAGER_PREFIX =
      "org.lockss.metadataDbManager.";
  public static final String V2_PARAM_DERBY_DB_DIR = "org.lockss.db.derbyDbDir";
  public static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT =
      V2_PARAM_METADATADBMANAGER_PREFIX + "datasource";
  public static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_CLASSNAME =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".className";
  public static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_SERVERNAME =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".serverName";
  public static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_PORTNUMBER =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".portNumber";
  public static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_DATABASENAME =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".databaseName";
  public static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_USER =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".user";
  public static final String V2_PARAM_METADATADBMANAGER_DATASOURCE_PASSWORD =
      V2_PARAM_METADATADBMANAGER_DATASOURCE_ROOT + ".password";

  public static final boolean V2_DEFAULT_PROXYMANAGER_START = false;
  public static final boolean V2_DEFAULT_CONTENTSERVLETMANAGER_START = false;


  public static final int V2_DEFAULT_CFGSVC_UI_PORT = 24621;
  public static final int V2_DEFAULT_PROXY_PORT = 24670;
  public static final int V2_DEFAULT_PROXY_SSL_PORT = 24671;
  public static final int V2_DEFAULT_AUDIT_PROXY_PORT = 24672;
  public static final int V2_DEFAULT_AUDIT_PROXY_SSL_PORT = 24673;
  public static final int V2_DEFAULT_CONTENTSERVLET_PORT = 24680;
  public static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_CLASSNAME =
      EmbeddedDataSource.class.getCanonicalName();
  public static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_SERVERNAME = "localhost";
  public static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER = "1527";
  public static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER_PG = "5432";
  public static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PORTNUMBER_MYSQL = "3306";
  public static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_DATABASENAME = "LockssMetadataDbManager";
  public static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_USER = "LOCKSS";
  public static final String V2_DEFAULT_METADATADBMANAGER_DATASOURCE_PASSWORD = "insecure";
}
