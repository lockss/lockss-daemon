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
import org.lockss.crawler.CrawlSeed;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;

/**
 * Utility to validate and dump plugin definitions.
 */
public class PluginUtil {
  static Logger log = Logger.getLogger("PluginUtil");

  static PluginManager pmgr;
  static MockLockssDaemon daemon;
  static PrintStream pout;
  static boolean quiet = false;
  static boolean verbose = false;
  static boolean err = false;

  public static void main(String argv[]) throws Exception {
    List<String> names = new ArrayList();
    List<String> attributes = new ArrayList();
    String outputFile = null;
    String configFile = null;
    Configuration auConfig = null;

    if (argv.length == 0) {
      usage();
    }
    try {
      for (int ix = 0; ix < argv.length; ix++) {
	String arg = argv[ix];
	if (arg.startsWith("-o")) {
	  outputFile = argv[++ix];
	} else if (arg.startsWith("-q")) {
	  quiet = true;
	} else if (arg.startsWith("-v")) {
	  verbose = true;
	} else if (arg.startsWith("-c")) {
	  configFile = argv[++ix];
	} else if (arg.startsWith("-a")) {
	  attributes.add(argv[++ix]);
	} else if (arg.startsWith("-")) {
	  usage();
	} else {
	  names.add(arg);
	}
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      usage();
    }

    ConfigManager cmgr = ConfigManager.makeConfigManager();
    daemon = new MyMockLockssDaemon();
    pmgr = daemon.getPluginManager();

    pout = System.out;
    if (outputFile != null) {
      FileOutputStream fos = new FileOutputStream(outputFile);
      pout = new PrintStream(fos);
    }

    if (configFile != null) {
      auConfig = ConfigurationUtil.fromFile(configFile);
    }

    for (String pname : names) {
      Plugin plug;
      File file = new File(pname);
      if (file.exists() && file.isFile()) {
	plug = load(file);
      } else {
	plug = load(pname);
      }
      if (plug != null) {
	if (!quiet) {
	  pout.println(pname + " loaded");
	}
	if (verbose) {
	  dump(plug);
	}
	if (auConfig != null) {
	  ArchivalUnit au = plug.createAu(auConfig);
	  if (!attributes.isEmpty()) {
	    for (String attr : attributes) {
	      if ("start-url".equals(attr)) {
          pout.println("Start urls: " + au.getStartUrls());
          continue;
	      }
  		}
	      }
	    }
	  }
	}
    if (err) {
      System.exit(1);
    }
  }

  static Plugin load(File file) {
    try {
      DefinablePlugin plug = new DefinablePlugin();
      plug.initPlugin(daemon, file);
      return plug;
    } catch (Exception e) {
      System.err.println("Error: can't load plugin: " + file + ": "
			 + e.toString());
    }
    err = true;
    return null;
  }

  static Plugin load(String pname) {
    String key = PluginManager.pluginKeyFromId(pname);
    try {
      PluginManager.PluginInfo pi = pmgr.loadPlugin(key, null);
      return pi.getPlugin();
    } catch (PluginException.PluginNotFound e) {
      System.err.println("Error: plugin not found: " + pname);
    } catch (PluginException.LinkageError e) {
      System.err.println("Error: can't load plugin: " + pname + e.toString());
    } catch (PluginException.IncompatibleDaemonVersion e) {
      System.err.println("Error: incompatible Plugin: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("Error: can't load plugin: " + pname + ": "
			 + e.toString());
    }
    err = true;
    return null;
  }

  static void dump(Plugin plug) {
    if (plug instanceof DefinablePlugin) {
      DefinablePlugin dplug = (DefinablePlugin)plug;
      pout.println(dplug.getDefinitionMap());
    }
  }

  static void usage() {
    PrintStream p = System.err;
    p.println("Usage: PluginUtil [-q] [-o outfile] <plugin-names ...>");
    p.println("     -o outfile   write to outfile, else stdout");
    p.println("     -q           quiet");
    p.println("     -v           verbose (print plugin def)");
    p.println("     -c <file>    load AU config from file");
    p.println("     -a <attr>    print AU attribute");
    p.println("                    start-url  list of start URLs");
    p.println();
    p.println("  Plugin names may be filenames or fully-qualified names to be");
    p.println("  loaded from the classpath");

    System.exit(1);
  }

  /** Just here so we can create an instance of MockLockssDaemon, whose
   * constructor is (deliberately) protected */
  static class MyMockLockssDaemon extends MockLockssDaemon {
  }
}
