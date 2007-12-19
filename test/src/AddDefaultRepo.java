/*
 * $Id: AddDefaultRepo.java,v 1.1 2007-12-19 05:16:06 tlipkis Exp $
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
import static org.lockss.plugin.PluginManager.*;
import org.lockss.test.*;

/**
 * Utility to add an explicit reserved.repository prop to each entry in an
 * au.txt file that doesn't already have one.
 */
public class AddDefaultRepo {

  public static void main(String argv[]) throws Exception {
    List configUrls = new ArrayList();
    String outputFile = null;
    String defRepo = null;

    if (argv.length == 0) {
      usage();
    }
    try {
      for (int ix = 0; ix < argv.length; ix++) {
	String arg = argv[ix];
	if        (arg.startsWith("-o")) {
	  outputFile = argv[++ix];
	} else if (arg.startsWith("-r")) {
	  defRepo = argv[++ix];
	} else if (arg.startsWith("-")) {
	  usage();
	} else {
	  configUrls.add(arg);
	}
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      usage();
    }

    if (configUrls.isEmpty() || defRepo == null) {
      usage();
    }

    PrintStream pout = System.out;
    if (outputFile != null) {
      FileOutputStream fos = new FileOutputStream(outputFile);
      pout = new PrintStream(fos);
    }

    ConfigManager mgr = ConfigManager.makeConfigManager();
    MockLockssDaemon daemon = new MyMockLockssDaemon();
    Configuration config = mgr.readConfig(configUrls);
    if (config == null) {
      System.err.println("Couldn't load config");
      System.exit(1);
    }
    Configuration allPlugs = config.getConfigTree(PARAM_AU_TREE);
    for (Iterator plugIter = allPlugs.nodeIterator(); plugIter.hasNext(); ) {
      String pluginKey = (String)plugIter.next();
      Configuration pluginConf = allPlugs.getConfigTree(pluginKey);
      for (Iterator auIter = pluginConf.nodeIterator(); auIter.hasNext(); ) {
	String auid = (String)auIter.next();
	Configuration oneAu = pluginConf.getConfigTree(auid);
	String name = oneAu.get(AU_PARAM_DISPLAY_NAME);
	String repo = oneAu.get(AU_PARAM_REPOSITORY);
	if (repo == null) {
	  String parm = PARAM_AU_TREE + "." + pluginKey + "." + auid + "." +
	    AU_PARAM_REPOSITORY;
	  config.put(parm, defRepo);
	}
	// 	  System.exit(0);
      }
    }
    config.store(pout, "AU Configuration");
    pout.close();
  }

  static void usage() {
    System.err.println("Usage: AddDefaultRepo -r repo [-o outfile] <url-or-file>");
    System.err.println("         -r <repo>    default repository");
    System.err.println("         -o outfile   write to outfile, else stdout");
    System.exit(1);
  }

  /** Just here so we can create an instance of MockLockssDaemon, whose
   * constructor is (deliberately) protected */
  static class MyMockLockssDaemon extends MockLockssDaemon {
  }
}
