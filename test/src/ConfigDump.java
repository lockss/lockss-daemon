/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

/**
 * Utility to load and dump config files in a canonical form, for easy
 * diffing.
 */
public class ConfigDump {
  static Logger log = Logger.getLogger("ConfigDump");
  static String PREFIX_TITLE_DB = ConfigManager.PARAM_TITLE_DB + ".";
  static List excludeBelow = ListUtil.list(PREFIX_TITLE_DB);

  public static void main(String argv[]) throws Exception {
    List<String> configUrls = new ArrayList();
    String outputFile = null;
    boolean xmlHack = false;
    boolean quiet = false;
    Configuration platConfig = ConfigManager.newConfiguration();


    if (argv.length == 0) {
      usage();
    }
    try {
      for (int ix = 0; ix < argv.length; ix++) {
	String arg = argv[ix];
	if        (arg.startsWith("-t")) {
	  excludeBelow.remove(PREFIX_TITLE_DB);
	} else if (arg.startsWith("-g")) {
 	  platConfig.put(ConfigManager.PARAM_DAEMON_GROUPS,
			 argv[++ix].toLowerCase());
	} else if (arg.startsWith("-h")) {
	  platConfig.put(ConfigManager.PARAM_PLATFORM_FQDN,
			 argv[++ix]);
	} else if (arg.startsWith("-d")) {
	  platConfig.put(ConfigManager.PARAM_DAEMON_VERSION,
			 argv[++ix]);
	} else if (arg.startsWith("-p")) {
	  platConfig.put(ConfigManager.PARAM_PLATFORM_VERSION,
			 argv[++ix]);
	} else if (arg.startsWith("-k")) {
	  xmlHack = true;
	} else if (arg.startsWith("-x")) {
	  String exclude = argv[++ix];
	  if (StringUtil.isNullString(exclude)) {
	    usage();
	  }
	  if (!exclude.endsWith(".")) {
	    exclude = exclude + ".";
	  }
	  excludeBelow.add(exclude);
	} else if (arg.startsWith("-o")) {
	  outputFile = argv[++ix];
	} else if (arg.equals("-v")) {
	  quiet = true;
	} else if (arg.startsWith("-")) {
	  usage();
	} else {
	  configUrls.add(arg);
	}
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      usage();
    }

    if (!xmlHack) {
      System.setProperty("org.lockss.config.noXmlHack", "true");
    }

    PrintStream pout = System.out;
    if (outputFile != null) {
      FileOutputStream fos = new FileOutputStream(outputFile);
      pout = new PrintStream(fos);
    }

    ConfigManager mgr = ConfigManager.makeConfigManager();
    if (!platConfig.isEmpty()) {
      ConfigManager.setPlatformConfig(platConfig);
    }
    if (quiet) {
      for (String url : configUrls) {
	try {
	  ConfigCache configCache = new ConfigCache(null);
	  ConfigFile cf = configCache.find(url);
	  log.debug(url);
	  cf.getConfiguration();
	} catch (Exception e) {
	  // Assumption is that ConfigFile logs a more specific error
	  System.err.println("Couldn't load config file: " + url);
	  System.exit(1);
	}
      }
    } else {
      MockLockssDaemon daemon = new MyMockLockssDaemon();
      Configuration config = mgr.readConfig(configUrls);
      if (config == null) {
	System.err.println("Couldn't load config");
	System.exit(1);
      }
      SortedSet keys = new TreeSet(config.keySet());
      for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	if (!isExcluded(key)) {
	  String val = config.get(key);
	  if (val == null) {
	    val = "(null)";
	  }
	  pout.println(key + " = " + (String)config.get(key));
	}
      }
    }
  }

  static boolean isExcluded(String key) {
    for (Iterator iter = excludeBelow.iterator(); iter.hasNext(); ) {
      if (key.startsWith((String)iter.next())) {
	return true;
      }
    }
    return false;
  }

  static String createLocalConfig(String hostName) {
    String result = null;
    try {
      File tmpdir = FileUtil.createTempDir("configdump", null);
      tmpdir.deleteOnExit();
      File localTxt = new File(tmpdir, "local.txt");
      localTxt.deleteOnExit();
      Properties localProps = new Properties();
      localProps.setProperty(ConfigManager.PARAM_PLATFORM_FQDN,
			     hostName);
      localProps.store(new FileOutputStream(localTxt), null);
      result = localTxt.getAbsolutePath();
    } catch (IOException ex) {
      System.err.println("Unable to create local.txt temp file: " + ex);
    }
    return result;
  }

  static void usage() {
    System.err.println("Usage: ConfigDump [-t] [-k] [-x excludebelow ] [-o outfile] <urls-or-files ...>");
    System.err.println("     -g <group>   set daemon group name");
    System.err.println("     -p <version> set platform version");
    System.err.println("     -d <version> set daemon version");
    System.err.println("     -h <nost>    set host name");
    System.err.println("     -x prefix    exclude subtree below prefix");
    System.err.println("     -t           include title db entries");
    System.err.println("     -k           enable .txt -> .xml kludge");
    System.err.println("     -o outfile   write to outfile, else stdout");
    System.err.println("     -v           verify mode.  No output, just");
    System.err.println("                    check that files load,");
    System.err.println("                    exit with error if any fail");
    System.exit(1);
  }

  /** Just here so we can create an instance of MockLockssDaemon, whose
   * constructor is (deliberately) protected */
  static class MyMockLockssDaemon extends MockLockssDaemon {
  }
}
