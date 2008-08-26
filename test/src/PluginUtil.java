/*
 * $Id: PluginUtil.java,v 1.1.2.1 2008-08-26 06:11:19 tlipkis Exp $
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
    String outputFile = null;

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

    for (String pname : names) {
      dump(pname);
    }
    if (err) {
      System.exit(1);
    }
  }

  static void dump(String pname) {
    String key = PluginManager.pluginKeyFromId(pname);
    try {
      pmgr.loadPlugin(key, null);
      Plugin plug = pmgr.getPlugin(key);
      if (!quiet) {
	pout.println(pname + " OK");
	if (verbose) {
	  if (plug instanceof DefinablePlugin) {
	    DefinablePlugin dplug = (DefinablePlugin)plug;
	    pout.println(dplug.getDefinitionMap());
	  }
	}
      }
      return;
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
  }

  static void usage() {
    System.err.println("Usage: PluginUtil [-q] [-o outfile] <plugin-names ...>");
    System.err.println("     -o outfile   write to outfile, else stdout");
    System.err.println("     -q           verbose (print plugin def)");
    System.exit(1);
  }

  /** Just here so we can create an instance of MockLockssDaemon, whose
   * constructor is (deliberately) protected */
  static class MyMockLockssDaemon extends MockLockssDaemon {
  }
}
