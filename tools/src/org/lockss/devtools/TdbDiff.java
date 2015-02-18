/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
n
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
package org.lockss.devtools;

import java.io.*;
import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;


/**
 * This class prints the differences between two Tdbs defined by configuration files.
 *
 * @author  Philip Gust
 */
public class TdbDiff {

  PluginManager pluginMgr;
  Tdb tdb1;
  Tdb tdb2;
  
  boolean showFields = false;		// show the params that are
					// different for the same auid
  boolean showAll = false;		// show even auids that are the
					// same with no leading marker
  Collection<String> excludeFields = null;

  int verbose = 0;

  /**
   * Create an instance for the specified PluginManager and Tdbs.
   * 
   * @param pluginMgr the PluginManager
   * @param tdb1 the first Tdb
   * @param tdb2 the second Tdb
   */
  public TdbDiff(PluginManager pluginMgr, Tdb tdb1, Tdb tdb2) {
    if (pluginMgr == null) {
      throw new IllegalArgumentException("PluginManager not specified");
    }
    this.pluginMgr = pluginMgr;
    
    if (tdb1 == null) {
      throw new IllegalArgumentException("Tdb 1 not specified");
    }
    this.tdb1 = tdb1;

    if (tdb2 == null) {
      throw new IllegalArgumentException("Tdb 2 not specified");
    }
    this.tdb2 = tdb2;
  }
  
  /**
   * Return the Plugin for this instance for the specified PluginManager.
   * 
   * @param pluginId the plugin ID
   * @return the Plugin instance
   */
  public Plugin getPluginFor(String pluginId) {
    Plugin plugin = null; 
    String pluginKey = PluginManager.pluginKeyFromName(pluginId);
    if (pluginMgr.ensurePluginLoaded(pluginKey)) {
      plugin = pluginMgr.getPlugin(pluginKey);
    }
    return plugin;
  }
  
  /*
   * Return the Au ID for the specified TdbAu based on definitional parameters.
   * 
   * @param tdbAu the TdbAu
   */
  public String getAuIdFor(TdbAu tdbAu) {
    String pluginId = tdbAu.getPluginId();
    Plugin plugin = getPluginFor(pluginId);
    if (plugin == null) {
      return tdbAu.getId().toString();
    }
    
    Properties auDefProps = new Properties();
    for (Map.Entry<String,String> param : tdbAu.getParams().entrySet()) {
      String key = param.getKey();
      String val = param.getValue();
      ConfigParamDescr descr = plugin.findAuConfigDescr(key);
      if ((descr != null) && descr.isDefinitional()) {
        auDefProps.put(key,val);
      }
    }
    String auId = PluginManager.generateAuId(pluginId, auDefProps);
    return auId;
  }

  /**
   * Returns an ordered map of auIDs to TdbAus for this Tdb.
   * The auIds are computed using only the definitional parameters
   * as specified by the plugin.
   * 
   * @param tdb the Tdb
   * @return a map of auIDs to TdbAus for this Tdb
   */
  public TreeMap<String, TdbAu> getTdbAusFor(Tdb tdb) {
    TreeMap<String,TdbAu> map = new TreeMap<String,TdbAu>();
    for (TdbPublisher pub : tdb.getAllTdbPublishers().values()) {
      for (TdbTitle tdbTitle : pub.getTdbTitles()) {
        for (TdbAu tdbAu : tdbTitle.getTdbAus()) {
          String auId = getAuIdFor(tdbAu);
          map.put(auId, tdbAu);
        }
      }
    }
    return map;
  }

  /**
   * Print the differences between the TdbAus for the two Tdbs by auId.
   * 
   * @param ps the stream to print to
   */
  public void printTdbDiffsByAu(PrintStream ps) {
    Iterator<Map.Entry<String,TdbAu>> auItr1 = getTdbAusFor(tdb1).entrySet().iterator();
    Iterator<Map.Entry<String,TdbAu>> auItr2 = getTdbAusFor(tdb2).entrySet().iterator();
    Map.Entry<String,TdbAu> auEntry1 = auItr1.hasNext() ? auItr1.next() : null;
    Map.Entry<String,TdbAu> auEntry2 = auItr2.hasNext() ? auItr2.next() : null;
    
    while ((auEntry1 != null) || (auEntry2 != null)) {
      if (auEntry2 == null) {
        // no more aus for tdb2; list au for tdb1
        ps.println("< " + auEntry1.getKey());
        if (showFields && showAll) {
          printTdbAuDiffs(ps, auEntry1.getValue(), auEntry1.getValue());
        }
        auEntry1 = auItr1.hasNext() ? auItr1.next() : null;
      } else if (auEntry1 == null) {
        // no more aus for tdb1; list au for tdb2
        ps.println("> " + auEntry2.getKey());
        if (showFields && showAll) {
          printTdbAuDiffs(ps, auEntry2.getValue(), auEntry2.getValue());
        }
        auEntry2 = auItr2.hasNext() ? auItr2.next() : null;
      } else {
        // compare auids for tdb1 and tdb2
        int auCmpr = auEntry1.getKey().compareTo(auEntry2.getKey());
        if (auCmpr < 0) {
          // list auid1 that does not exist in tdb2
          ps.println("< " + auEntry1.getKey());
          if (showFields && showAll) {
            printTdbAuDiffs(ps, auEntry1.getValue(), auEntry1.getValue());
          }
          auEntry1 = auItr1.hasNext() ? auItr1.next() : null;
        } else if (auCmpr > 0) {
          // list auid2 that does not exist in tdb1
          ps.println("> " + auEntry2.getKey());
          if (showFields && showAll) {
            printTdbAuDiffs(ps, auEntry2.getValue(), auEntry2.getValue());
          }
          auEntry2 = auItr2.hasNext() ? auItr2.next() : null;
        } else {
          // compare same auid in au1 and au2 
          Map<String,String> paramMap1 = auEntry1.getValue().getParams();
          Map<String,String> paramMap2 = auEntry2.getValue().getParams();
	  String diffs = tdbAuDiffs(auEntry1.getValue(), auEntry2.getValue());
          if (diffs.isEmpty()) {
            if (showAll) {
              // all parameters are the same so list as same
              System.out.println("  " + auEntry1.getKey());
              if (showFields && showAll) {
                printTdbAuDiffs(ps, auEntry1.getValue(), auEntry1.getValue());
              }
            }
          } else {
            // some parameters are different so list as different
            System.out.println("! " + auEntry1.getKey());
            if (showFields) {
	      ps.print(diffs);
            }
          }
          
          // advance to next aus for tdb1 and tdb2
          auEntry1 = auItr1.hasNext() ? auItr1.next() : null;
          auEntry2 = auItr2.hasNext() ? auItr2.next() : null;
        }
      }
    }
  }

  /**
   * Print the parameter differences between two aus.
   *  
   * @param tdbAu1 the first au
   * @param tdbAu2 the second au
   */
  public void printTdbAuDiffs(PrintStream ps, TdbAu tdbAu1, TdbAu tdbAu2) {
    ps.print(tdbAuDiffs(tdbAu1, tdbAu2));
  }

  public String tdbAuDiffs(TdbAu tdbAu1, TdbAu tdbAu2) {
    StringBuilder sb = new StringBuilder();
    // determine which params are different
    Map<String,String> props1 = allTopLevelProps(tdbAu1);
    Map<String,String> props2 = allTopLevelProps(tdbAu2);
    appendMapDiffs(sb, props1, props2);
    appendMapDiffs(sb, tdbAu1.getParams(), tdbAu2.getParams());
    appendMapDiffs(sb, tdbAu1.getAttrs(), tdbAu2.getAttrs());
    return sb.toString();
  }

  Map<String,String> allTopLevelProps(TdbAu au) {
    Map<String,String> props = new HashMap<String,String>(au.getProperties());
    props.put("journalTitle", au.getJournalTitle());
    props.put("title", au.getName());
    return props;
  }

  boolean isIncl(String name) {
    return isIncl(name, null);
  }

  boolean isIncl(String name, String context) {
    return excludeFields == null ||
      !(excludeFields.contains(name) ||
	(context != null && excludeFields.contains(context + name)));
  }

  void appendln(StringBuilder sb, String str) {
    sb.append(str);
    sb.append("\n");
  }

  void appendMapDiffs(StringBuilder sb,
		      Map<String,String> map1, Map<String,String> map2) {
    Iterator<Map.Entry<String,String>> iter1 = 
      new TreeMap<String,String>(map1).entrySet().iterator();
    Iterator<Map.Entry<String,String>> iter2 = 
      new TreeMap<String,String>(map2).entrySet().iterator();
    Map.Entry<String,String> paramEntry1 = iter1.hasNext() ? iter1.next() : null;
    Map.Entry<String,String> paramEntry2 = iter2.hasNext() ? iter2.next() : null;
    while ((paramEntry1 != null) || (paramEntry2 != null)) {
      if (paramEntry2 == null) {
	// no more parameters for au2; list parameter for au1 
	if (isIncl(paramEntry1.getKey(), "<")) {
	  appendln(sb, "  < " + paramEntry1.getKey());
	}
	paramEntry1 = iter1.hasNext() ? iter1.next() : null;
      } else if (paramEntry1 == null) {
        // no more parameters for au1; list parameter for au2 
	if (isIncl(paramEntry2.getKey(), ">")) {
	  appendln(sb, "  > " + paramEntry2.getKey());
	}
	paramEntry2 = iter2.hasNext() ? iter2.next() : null;
      } else {
        // compare parameters for au1 and au2
	int paramCmpr = paramEntry1.getKey().compareTo(paramEntry2.getKey());
	if (paramCmpr < 0) {
	  // list parameter1 that does not exist in au2
	  if (isIncl(paramEntry1.getKey(), "<")) {
	    appendln(sb, "  < " + paramEntry1.getKey());
	  }
	  paramEntry1 = iter1.hasNext() ? iter1.next() : null;
	} else if (paramCmpr > 0) {
	  // list parameter2 that does not exist in au1
	  if (isIncl(paramEntry2.getKey(), ">")) {
	    appendln(sb, "  > " + paramEntry2.getKey());
	  }
	  paramEntry2 = iter2.hasNext() ? iter2.next() : null;
	} else {
	  if (paramEntry1.getValue().equals(paramEntry2.getValue())) {
	    if (showAll && isIncl(paramEntry1.getKey())) {
	      // list parameter whose value is the same in au1 and au2
	      appendln(sb, "    " + paramEntry1.getKey());
	    }
	  } else if (isIncl(paramEntry1.getKey(), "!")) {
	      // list parameter whose value is different in au1 and au2
	      appendln(sb, "  ! " + paramEntry1.getKey());
	      if (verbose >= 1) {
		appendln(sb, "   < " + paramEntry1.getValue());
		appendln(sb, "   > " + paramEntry2.getValue());
	      }
	  }
	  // advance to next param for au1 and au2
	  paramEntry1 = iter1.hasNext() ? iter1.next() : null;
	  paramEntry2 = iter2.hasNext() ? iter2.next() : null;
	}
      }
    }
  }
  
  /**
   * Show usage message.
   * 
   * @param message the message to display
   */
  static public void usage(String message) {
    System.err.println(message);
    usage();
  }

  /**
   * Show usage message.
   * 
   * @param message the message to display
   */
  static public void usage() {
    System.err.println("Usage: tdbdiff [-showAll] [-showFields] [-excludeFields <field1> ... <fieldN>] -config <file1> ... <fileN> -config <file1> ... <fileN>");
  }

  enum Mode {Diff, PPrint};

  /**
   * Main method takes two groups of configuration files preceded by -config switch
   * and compares the Tdbs that they specify.  The output is a list of auIDs that are
   * different.  Aus that appear in the first group but not the second are preceded
   * by a "&lt;"; ones that are in the second group but not the first are preceded by
   * a "&gt;"; ones that are in both but different are preceded by a "!".
   * <p>
   * The -showFields switch expands the the same au whose parameters are different 
   * between the two configurations.
   * <p>
   * The -showAll switch also shows aus that are the same between two configurations,
   * with no leading marker, and parameters that are the same
   * @param args
   */
  static public void main(String[] args) {
    ConfigManager.makeConfigManager();
    Logger.resetLogs();
    LockssDaemon daemon = new MockLockssDaemon() {};
    PluginManager pluginMgr = daemon.getPluginManager();
    ConfigManager configMgr = daemon.getConfigManager();

    boolean showAll = false;
    boolean showFields = false;
    int verbose = 0;
    Collection<String> excludeFields = null;
    boolean inExclude = false;
    Mode mode = Mode.Diff;
    
    Configuration config1 = null;
    Configuration config2 = null;
    Configuration curConfig = null;

    for (String arg : args) {
      if (arg.equalsIgnoreCase("-showAll")) {
        showAll = true;
      } else if (arg.equalsIgnoreCase("-showFields")) {
        showFields = true;
      } else if (arg.equalsIgnoreCase("-v")) {
        verbose++;
      } else if (arg.equalsIgnoreCase("-pp")) {
	mode = Mode.PPrint;
      } else if (arg.equalsIgnoreCase("-config")) {
	inExclude = false;
        if (config1 == null) {
          curConfig = config1 = ConfigManager.newConfiguration();
        } else if (config2 == null) {
          curConfig = config2 = ConfigManager.newConfiguration();
        } else {
          usage("Too many -config switches specified");
          System.exit(1);
        }
      } else if (arg.equalsIgnoreCase("-excludeFields")) {
	if (excludeFields == null) {
	  excludeFields = new HashSet<String>();
	}
	inExclude = true;
      } else if (arg.equalsIgnoreCase("-help")) {
        usage("List the differences between two TDBs specified by two sets of configurations");
        System.exit(0);
      } else if (arg.startsWith("-")) {
        usage("Unknown flag " + arg);
        System.exit(1);
      } else if (curConfig == null) {
        usage("Configuration file specified before -config1 or -config2 flag");
        System.exit(1);
      } else {
	if (inExclude) {
	  excludeFields.add(arg);
	} else {
	  try {
	    Configuration config = configMgr.loadConfigFromFile(new File(arg).toURI().toURL().toExternalForm());
	    curConfig.copyFrom(config);
	  } catch (IOException ex) {
	    System.err.println("Error loading configuration file \""
			       + arg + "\": " + ex.getMessage());
	    System.exit(1);
	  }
	}
      }
    }

    if (config1 == null) {
      usage("No -config switches specified.");
      System.exit(1);
    }

    if (mode == Mode.Diff && config2 == null) {
      usage("Too few -config switches specified.");
      System.exit(1);
    }

    Tdb tdb1 = config1.getTdb();
    if (tdb1 == null) {
      tdb1 = new Tdb();
    }
    
    switch (mode) {
    case Diff:
      Tdb tdb2 = config2.getTdb();
      if (tdb2 == null) {
	tdb2 = new Tdb();
      }
      TdbDiff tdbDiff = new TdbDiff(pluginMgr, tdb1, tdb2);
      tdbDiff.showFields = showFields;
      tdbDiff.showAll = showAll;
      tdbDiff.excludeFields = excludeFields;
      tdbDiff.verbose = verbose;
      tdbDiff.printTdbDiffsByAu(System.out);
      break;
    case PPrint:
      tdb1.prettyPrint(System.out);
      break;
    }      
  }
}
