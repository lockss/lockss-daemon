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

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.jar.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;

public class AuIdFromProps {//extends BaseArchivalUnit {
  private Class[] plugins = {
    org.lockss.plugin.simulated.SimulatedPlugin.class,
  };

  public static void main(String argv[]) {
    AuIdFromProps it = new AuIdFromProps();
    it.doIt();
  }

  public void doIt() {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    Plugin plugin = null;

    while (plugin == null) {
      System.out.println("Choose a plugin: ");
      for (int ix=0; ix<plugins.length; ix++) {
	System.out.println(ix+": "+plugins[ix]);
      }
      System.out.print("Choice: ");
      try {
	int choice = Integer.parseInt(in.readLine());
	plugin = (Plugin)plugins[choice].newInstance();
      } catch (Exception e) {
	System.err.println(e.toString());
      }
    }


    Properties props = new Properties();
    try {
      System.err.println("Enter values for the following properties");
      for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	   iter.hasNext();) {
	ConfigParamDescr descr = (ConfigParamDescr)iter.next();
	if (descr.isDefinitional()) {
	  String key = descr.getKey();
	  System.out.print(key + ": ");
	  String line = in.readLine();
	  props.setProperty(key, line);
	}
      }
	//       PropUtil.printPropsTo(props, System.out);
	//       System.out.println();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }


    String pluginId = plugin.getPluginId();
    String auId = PluginManager.generateAuId(pluginId, props);
    StringBuffer propBase = new StringBuffer(auId);
    propBase.setCharAt(auId.indexOf("&"), '.');

//     Vector v = StringUtil.breakAt(auId, '&', 2);
//     String propBase = StringUtil.separatedString(v, ".");
//     String propBase = pluginId+"."+PluginManager.getAUKeyFromAUId(auId);

// 	System.out.println("AUId: "
// 			 +);
    for (Iterator it = props.keySet().iterator(); it.hasNext();) {
      String key = (String)it.next();
      String val = props.getProperty(key);
      System.out.println(PluginManager.PARAM_AU_TREE
			 +"."+propBase+"."+key+"="+val);
    }

  }

}
