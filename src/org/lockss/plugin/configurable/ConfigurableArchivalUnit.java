/*
 * $Id: ConfigurableArchivalUnit.java,v 1.2 2004-01-17 00:55:27 clairegriffin Exp $
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
package org.lockss.plugin.configurable;

import java.net.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import gnu.regexp.*;

/**
 * <p>ConfigurableArchivalUnit: An implementatation of Base Archival Unit used
 * with the ConfigurablePlugin to allow a Map of values to be used to configure
 * and define the behaviour of a plugin.</p>
 * @author claire griffin
 * @version 1.0
 */
public class ConfigurableArchivalUnit
    extends BaseArchivalUnit {
  static final protected String CM_AU_START_URL_KEY = "au_start_url";
  static final protected String CM_AU_NAME_KEY = "au_name";
  static final protected String CM_AU_RULES_KEY = "au_crawlrules";
  static final protected String CM_AU_PARAMS_KEY = "au_params";

  protected ExternalizableMap configurationMap;
  static Logger log = Logger.getLogger("ConfigurableArchivalUnit");

  protected ConfigurableArchivalUnit(Plugin myPlugin) {
    super(myPlugin);
    if (myPlugin != null) {
      configurationMap = ((ConfigurablePlugin)myPlugin).getConfigurationMap();
    }
  }


  protected String makeStartUrl() {
    String startstr = configurationMap.getString(CM_AU_START_URL_KEY, "");
    return convertVariableString(startstr);
  }

  protected void loadDefiningConfig(Configuration config) throws
      ConfigurationException {
    List descrList = plugin.getAuConfigProperties();
    for (Iterator it = descrList.iterator(); it.hasNext(); ) {
      ConfigParamDescr descr = (ConfigParamDescr) it.next();
      String key = descr.getKey();
      switch (descr.getType()) {
        case ConfigParamDescr.TYPE_INT:
          int i_val = loadConfigInt(descr, config);
          configurationMap.putInt(key, i_val);
          break;
        case ConfigParamDescr.TYPE_STRING:
          String s_val = loadConfigString(descr, config);
          configurationMap.putString(key, s_val);
          break;
        case ConfigParamDescr.TYPE_URL:
          URL u_val = loadConfigUrl(descr, config);
          configurationMap.putUrl(key, u_val);
          break;
      }
    }
  }

  protected void setAuParams(Configuration config) throws
      org.lockss.plugin.ArchivalUnit.ConfigurationException {
    List paramsList = (List) configurationMap.getCollection(CM_AU_PARAMS_KEY,
        Collections.EMPTY_LIST);
    for (Iterator it = paramsList.iterator(); it.hasNext(); ) {
      Object next = it.next();
      ConfigParamDescr descr = (ConfigParamDescr) next;
      String key = descr.getKey();
      switch (descr.getType()) {
        case ConfigParamDescr.TYPE_INT:
          int i_val = loadConfigInt(descr, config);
          configurationMap.putInt(key, i_val);
          break;
        case ConfigParamDescr.TYPE_STRING:
          String s_val = loadConfigString(descr, config);
          configurationMap.putString(key, s_val);
          break;
        case ConfigParamDescr.TYPE_URL:
          URL u_val = loadConfigUrl(descr, config);
          configurationMap.putUrl(key, u_val);
          break;
      }
    }
  }

  protected String makeName() {
    String namestr = configurationMap.getString(CM_AU_NAME_KEY, "");
    return convertVariableString(namestr);
  }

  protected CrawlRule makeRules() throws gnu.regexp.REException {
    List rules = new LinkedList();
    List templates = (List) configurationMap.getCollection(CM_AU_RULES_KEY,
        Collections.EMPTY_LIST);
    Iterator it = templates.iterator();
    while (it.hasNext()) {
      String rule_template = (String) it.next();
      rules.add(convertRule(rule_template));
    }
    return new CrawlRules.FirstMatch(rules);
  }

  public FilterRule getFilterRule(String mimeType) {
    String filter = configurationMap.getString(mimeType, null);
    if(filter != null) {
      try {
        return (FilterRule) Class.forName(filter).newInstance();
      }
      catch (ClassNotFoundException ex) {
        return null;
      }
      catch (IllegalAccessException ex) {
        return null;
      }
      catch (InstantiationException ex) {
        return null;
      }
    }

    return super.getFilterRule(mimeType);
  }

// ---------------------------------------------------------------------
//   VARIABLE ARGUMENT REPLACEMENT SUPPORT ROUTINES
// ---------------------------------------------------------------------
  private String[] getStringTokens(String tokenString) {
    StringTokenizer st = new StringTokenizer(tokenString,"\n");
    int num_tokens = st.countTokens();
    String[] strs = new String[num_tokens];
    for(int i=0; i < num_tokens; i++) {
      strs[i] = st.nextToken();
    }
    return strs;
  }

  String convertVariableString(String variableString) {
    if(StringUtil.isNullString(variableString)) {
      return variableString;
    }
    String[] strs = getStringTokens(variableString);
    String cur_str = strs[0];
    ArrayList args = new ArrayList();
    boolean has_all_args = true;
    for (int i = 1; i < strs.length && has_all_args; i++) {
      String key = strs[i];
      Object val = configurationMap.getMapElement(key);
      if(val != null) {
        args.add(val);
      }
      else {
        has_all_args = false;
      }
    }
    if(has_all_args) {
      PrintfFormat pf = new PrintfFormat(cur_str);
      cur_str = pf.sprintf(args.toArray());
    }
    else {
      log.warning("missing variable arguments");
    }
    return cur_str;
  }

  CrawlRule convertRule(String variableString) throws REException {
    String[] strs = getStringTokens(variableString);

    int value = Integer.valueOf(strs[0]).intValue();
    String rule = strs[1];
    ArrayList args = new ArrayList();
    boolean has_all_args = true;
    for (int i = 2; i < strs.length && has_all_args; i++) {
      String key = strs[i];
      Object val = configurationMap.getMapElement(key);
      if(val != null) {
       args.add(val);
     }
     else {
       has_all_args = false;
     }
    }
    if(has_all_args) {
      PrintfFormat pf = new PrintfFormat(rule);
      rule = pf.sprintf(args.toArray());
      return new CrawlRules.RE(rule, value);
    }
    return null;
  }
}
