/*
 * $Id: ConfigDump.java,v 1.13 2007-08-01 04:50:03 tlipkis Exp $
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
  static String PREFIX_TITLE_DB = ConfigManager.PARAM_TITLE_DB + ".";
  static List excludeBelow = ListUtil.list(PREFIX_TITLE_DB);

  public static void main(String argv[]) throws Exception {
    List configUrls = new ArrayList();
    String groupName = null;
    String outputFile = null;
    String hostName = null;
    boolean xmlHack = false;

    if (argv.length == 0) {
      usage();
    }
    try {
      for (int ix = 0; ix < argv.length; ix++) {
	String arg = argv[ix];
	if        (arg.startsWith("-t")) {
	  excludeBelow.remove(PREFIX_TITLE_DB);
	} else if (arg.startsWith("-g")) {
	  groupName = argv[++ix];
	} else if (arg.startsWith("-h")) {
	  hostName = argv[++ix];
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

    if (hostName != null) {
      String localConfig = createLocalConfig(hostName);
      configUrls.add(localConfig);
    }

    ConfigManager mgr = ConfigManager.makeConfigManager();
    MockLockssDaemon daemon = new MyMockLockssDaemon();
    Configuration config = mgr.readConfig(configUrls, groupName);
    if (config == null) {
      System.err.println("Couldn't load config");
      System.exit(1);
    }
    SortedSet keys = new TreeSet(config.keySet());
    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (!isExcluded(key)) {
	pout.println(key + " = " + (String)config.get(key));
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
    System.err.println("         -g <group>   set daemon group name");
    System.err.println("         -h <nost>    set host name");
    System.err.println("         -x prefix    exclude subtree below prefix");
    System.err.println("         -t           include title db entries");
    System.err.println("         -k           enable .txt -> .xml kludge");
    System.err.println("         -o outfile   write to outfile, else stdout");
    System.exit(1);
  }

  /** Just here so we can create an instance of MockLockssDaemon, whose
   * constructor is (deliberately) protected */
  static class MyMockLockssDaemon extends MockLockssDaemon {
  }
}
