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
import org.lockss.config.ConfigManager;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

/**
 * Utility to create title db property files.  Queries user for plugin and
 * parameter values, or dumps all entries known by known plugins.
 */
public class TitleParams {

  public static void main(String argv[]) throws Exception {
    List configUrls = new ArrayList();
    String outputFile = null;
    try {
      for (int ix = 0; ix < argv.length; ix++) {
	String arg = argv[ix];
	if (arg.startsWith("-c")) {
	  configUrls.add(FileTestUtil.urlOfFile(argv[++ix]));
	} else if (arg.startsWith("-o")) {
	  outputFile = argv[++ix];
	} else {
	  usage();
	}
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      usage();
    }

    PrintStream cout = System.err;
    Set uids = new HashSet();
    MockLockssDaemon daemon = new MyMockLockssDaemon();
    ConfigManager.makeConfigManager();
    PluginManager pluginMgr = daemon.getPluginManager();
    daemon.setDaemonInited(true);
    daemon.setDaemonRunning(true);
    pluginMgr.startService();
    if (configUrls.size() > 0) {
      ConfigurationUtil.setCurrentConfigFromUrlList(configUrls);
    }
    List plugins = new ArrayList(pluginMgr.getRegisteredPlugins());
//     Collections.sort(plugins);

    PrintStream pout = System.out;
    if (outputFile != null) {
      FileOutputStream fos = new FileOutputStream(outputFile);
      pout = new PrintStream(fos);
    }
    BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
    Plugin prevPlugin = null;
    while (true) {
      Plugin plugin = null;
      while (plugin == null) {
	StringBuffer sb =
	  new StringBuffer("Enter a plugin number or name, or L,D,X,?");
	if (prevPlugin != null) {
	  sb.append(" [" + prevPlugin.getPluginName() + "]");
	}
	sb.append(": ");
	cout.print(sb.toString());
	try {
	  String line = cin.readLine();
	  if (!StringUtil.isNullString(line)) {
	    if (line.equals("?")) {
	      cout.println("L lists plugins, D dumps current titles, X exits.");
	      continue;
	    }
	    if (line.equalsIgnoreCase("x")) {
	      if (pout != System.out) {
		pout.close();
	      }
	      System.exit(0);
	    } else if (line.equalsIgnoreCase("d")) {
	      doall(pout, cout, cin, plugins);
	    } else if (line.equalsIgnoreCase("l")) {
	      for (int ix=0; ix<plugins.size(); ix++) {
		Plugin p = (Plugin)plugins.get(ix);
		cout.println((ix+1) + ": " + p.getPluginName());
	      }
	    } else {
	      String key = PluginManager.pluginKeyFromName(line);
	      pluginMgr.ensurePluginLoaded(key);
	      plugin = pluginMgr.getPlugin(key);
	      if (plugin == null) {
		int choice = Integer.parseInt(line);
		plugin = (Plugin)plugins.get(choice-1);
	      }
	    }
	  } else if (prevPlugin != null) {
	    plugin = prevPlugin;
	  }
	} catch (Exception e) {
	  cout.println(e.toString());
	}
      }
      prevPlugin = plugin;

      List params = plugin.getAuConfigDescrs();
      cout.print("Title: ");
      String title = cin.readLine();
      cout.print("Journal Title: ");
      String journalTitle = cin.readLine();
      String propId;
      do {
	cout.print("Unique prop abbrev: ");
	propId = cin.readLine();
      } while (!isLegalPropId(propId) || !uids.add(propId));
      TitleConfig tc = new TitleConfig(title, plugin);
      if (!StringUtil.isNullString(journalTitle)) {
	tc.setJournalTitle(journalTitle);
      }
      tc.setParams(new ArrayList());
      for (Iterator iter = params.iterator(); iter.hasNext(); ) {
	ConfigParamDescr descr = (ConfigParamDescr)iter.next();
	cout.print(descr.getDisplayName());
	if (!descr.getKey().equals(descr.getDisplayName())) {
	  cout.print("(");
	  cout.print(descr.getKey());
	  cout.print(")");
	}
	cout.print(": ");
	String line = cin.readLine();
	if (!StringUtil.isNullString(line)) {
	  ConfigParamAssignment cpa = new ConfigParamAssignment(descr, line);
	  tc.getParams().add(cpa);
	}
      }
      Properties p = tc.toProperties(propId);
      p.store(pout, "Title: " + title + " (" + plugin.getPluginName() + ")");

      cout.println();
    }
  }

  private static void doall(PrintStream pout, PrintStream cout,
			    BufferedReader cin, List plugins)
      throws IOException {
    cout.print("Prop abbrev prefix: ");
    String prefix = cin.readLine();
    int cnt = 0;
    for (Iterator piter = plugins.iterator(); piter.hasNext(); ) {
      Plugin plugin = (Plugin)piter.next();
      List titles = plugin.getSupportedTitles();
      for (Iterator titer = titles.iterator(); titer.hasNext(); ) {
	String title = (String)titer.next();
	TitleConfig tc = plugin.getTitleConfig(title);
	Properties p = tc.toProperties(prefix + (++cnt));
	p.store(pout, "Title: " + title + " (" + plugin.getPluginName() + ")");
      }
    }
  }

  static boolean isLegalPropId(String id) {
    return (!StringUtil.isNullString(id)) &&
      id.equals(StringUtil.escapeNonAlphaNum(id));
  }

  static void usage() {
    System.err.println("Usage: TitleParams [-c config-file] [-o output-file]");
    System.exit(0);
  }

  /** Just here so we can create an instance of MockLockssDaemon, whose
   * constructor is (deliberately) protected */
  static class MyMockLockssDaemon extends MockLockssDaemon {
  }
}
