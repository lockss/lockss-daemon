/*
 * $Id: ConfigDump.java,v 1.1 2004-07-22 19:59:13 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

/**
 * Utility to create title db property files.  Queries user for plugin and
 * parameter values, or dumps all entries known by known plugins.
 */
public class ConfigDump {

  public static void main(String argv[]) throws Exception {
    List configUrls = new ArrayList();
    String outputFile = null;
    boolean includeTitles = false;

    if (argv.length == 0) {
      usage();
    }
    try {
      for (int ix = 0; ix < argv.length; ix++) {
	String arg = argv[ix];
	if (arg.startsWith("-t")) {
	  includeTitles = true;
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

    PrintStream pout = System.out;
    if (outputFile != null) {
      FileOutputStream fos = new FileOutputStream(outputFile);
      pout = new PrintStream(fos);
    }
    MockLockssDaemon daemon = new MockLockssDaemon();

    ConfigManager mgr = ConfigManager.makeConfigManager();
//     ConfigManager mgr = daemon.getConfigManager();
    Configuration config = mgr.readConfig(configUrls);
    SortedSet keys = new TreeSet(config.keySet());
    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (includeTitles || !key.startsWith(ConfigManager.PARAM_TITLE_DB)) {
	pout.println(key + " = " + (String)config.get(key));
      }
    }
  }

  static void usage() {
    System.err.println("Usage: ConfigDump [-t] [-o outfile] <urls-or-files ...>");
    System.err.println("         -t   include title db entries");
    System.err.println("         -o outfile   write to outfile, else stdout");
    System.exit(1);
  }
}
