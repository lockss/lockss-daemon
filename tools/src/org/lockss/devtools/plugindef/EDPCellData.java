/*
 * $Id: EDPCellData.java,v 1.17.40.1 2009-11-03 23:44:56 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.devtools.plugindef;

import java.util.*;
import javax.swing.event.*;
import javax.swing.*;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;

public class EDPCellData {
  EditableDefinablePlugin m_plugin;
  String m_key;
  Object m_data;
  String m_displayString;
  static final String ELLIPSIS = "...";
  ArrayList m_listeners = new ArrayList();

  EDPCellData(EditableDefinablePlugin edp, String key, Object data,
              String displayString) {
    m_plugin = edp;
    m_key = key;
    m_data = data;
    m_displayString = displayString;
  }


  EDPCellData(EditableDefinablePlugin edp, String key){
    this(edp, key, null, null);

    if(key.equals(DefinablePlugin.KEY_PLUGIN_NAME)) {
      m_data = edp.getPluginName();
    }
    else if(key.equals(EditableDefinablePlugin.KEY_PLUGIN_IDENTIFIER)) {
      m_data = edp.getPluginIdentifier();
    }
    else if(key.equals(DefinablePlugin.KEY_PLUGIN_VERSION)) {
      m_data = edp.getPluginVersion();
    }
    else if(key.equals(DefinablePlugin.KEY_PLUGIN_CONFIG_PROPS)) {
      m_data = edp.getConfigParamDescrs();
    }
    else if(key.equals(DefinablePlugin.KEY_PLUGIN_NOTES)) {
      m_data = edp.getPluginNotes();
    }
    else if(key.equals(DefinableArchivalUnit.KEY_AU_START_URL)) {
      m_data = new PrintfTemplate(edp.getAuStartUrl());
    }
    else if(key.equals(DefinableArchivalUnit.KEY_AU_NAME)) {
      m_data = new PrintfTemplate(edp.getAuName());
    }
    else if(key.equals(DefinableArchivalUnit.KEY_AU_CRAWL_RULES)) {
      m_data = edp.getAuCrawlRules();
    }
    else if(key.equals(DefinableArchivalUnit.KEY_AU_DEFAULT_PAUSE_TIME)) {
      m_data = new Long(edp.getAuPauseTime());
      m_displayString = TimeEditor.millisToString(edp.getAuPauseTime());
    }
    else if(key.equals(DefinableArchivalUnit.KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL)) {
      m_data = new Long(edp.getNewContentCrawlInterval());
      m_displayString = TimeEditor.millisToString(edp.getNewContentCrawlInterval());
    }
    else if(key.equals(DefinableArchivalUnit.SUFFIX_FILTER_RULE)) {
      m_data = edp.getHashFilterRules();
    }
    else if(key.equals(DefinableArchivalUnit.SUFFIX_HASH_FILTER_FACTORY)) {
      m_data = edp.getHashFilterFactories();
    }
    else if(key.equals(DefinableArchivalUnit.SUFFIX_CRAWL_FILTER_FACTORY)) {
      m_data = edp.getCrawlFilterFactories();
    }
    else if(key.equals(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH)) {
      m_data = new Integer(edp.getAuCrawlDepth());
    }
    else if(key.equals(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW)) {
      m_data = edp.getAuCrawlWindow();
    }
    else if(key.equals(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW_SER)) {
      m_data = edp.getAuCrawlWindowSer();
    }
    else if(key.equals(DefinablePlugin.KEY_EXCEPTION_HANDLER)) {
      m_data = edp.getPluginExceptionHandler();
    }
    else if(key.equals(DefinablePlugin.KEY_EXCEPTION_LIST)) {
      m_data = edp.getSingleExceptionHandlers();
    }
    else if (key.equals(DefinablePlugin.KEY_REQUIRED_DAEMON_VERSION)) {
      m_data = edp.getRequiredDaemonVersion();
    }
    else if(key.equals(DefinablePlugin.KEY_CRAWL_TYPE)) {
      m_data = edp.getCrawlType();
    }
  }


  public void addChangeListener(ChangeListener listener) {
    m_listeners.add(listener);
  }

  public void removeChangeListener(ChangeListener listener) {
    m_listeners.remove(listener);
  }

  public void notifyListenersOfChange() {
    ChangeEvent event = new ChangeEvent(this);
    for(Iterator it = m_listeners.iterator(); it.hasNext();) {
      ChangeListener listener = (ChangeListener) it.next();
      listener.stateChanged(event);
    }
  }

  public Object getData() {
    return m_data;
  }

  public void setData(Object data) {
    m_data = data;
  }

  public String getKey() {
    return m_key;
  }

  public String getDisplayString() {
    return m_displayString;
  }

  public void setDisplayString(String displayString) {
    m_displayString = displayString;
  }

  public EditableDefinablePlugin getPlugin() {
    return m_plugin;
  }

  public String toString() {
    if(m_displayString == null) {
      if (m_data instanceof Collection || m_data instanceof Map ||
          m_data instanceof CrawlWindows.Interval ||
          m_data instanceof CrawlWindows.Not) {
        m_displayString = ELLIPSIS;
      }
      else if (m_data == null) {
          if(m_key.equals(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW))
              m_displayString = ELLIPSIS;
          else
              m_displayString = "NONE";
      }
      else {
        m_displayString = m_data.toString();
      }
    }
    return m_displayString;
  }

  public void updateTemplateData(PrintfTemplate template) {
    if (m_plugin == null || m_key == null)return;
    m_data = template;
    m_displayString = template.getViewableTemplate();
    if (m_key.equals(DefinableArchivalUnit.KEY_AU_NAME)) {
      m_plugin.setAuName(template.getViewableTemplate());
    }
    else if (m_key.equals(DefinableArchivalUnit.KEY_AU_START_URL)) {
      m_plugin.setAuStartUrl(template.getViewableTemplate());
    }

    notifyListenersOfChange();
  }

  public void updateStringDataAnyway(String data) {
    if (m_plugin == null || m_key == null || data == null) {
      return;
    }
    else if (m_key.equals(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW)) {
      m_data = data;
      m_plugin.setAuCrawlWindow((String)m_data, false);
    }
    else if (m_key.equals(DefinablePlugin.KEY_EXCEPTION_HANDLER)) {
      m_data = data;
      m_plugin.setPluginExceptionHandler((String)m_data, false);
    }
    
    notifyListenersOfChange();
  }

  public void updateStringData(String data) {
    if (m_plugin == null || m_key == null || data == null) {
      return;
    }
    else if(m_key.equals(DefinablePlugin.KEY_PLUGIN_NAME)) {
      m_data = data;
      m_plugin.setPluginName((String)m_data);
    }
    else if(m_key.equals(EditableDefinablePlugin.KEY_PLUGIN_IDENTIFIER)) {
      m_data = data;
      m_plugin.setPluginIdentifier((String)m_data);
    }
    else if(m_key.equals(DefinablePlugin.KEY_PLUGIN_VERSION)) {
      m_data = data;
      m_plugin.setPluginVersion((String)m_data);
    }
    else if(m_key.equals(DefinablePlugin.KEY_PLUGIN_NOTES)) {
      m_data = data;
      m_plugin.setPluginNotes((String)m_data);
      m_displayString = data;
    }
    else if(m_key.equals(DefinableArchivalUnit.KEY_AU_CRAWL_DEPTH)) {
      m_data = new Integer(data);
      m_plugin.setAuCrawlDepth(((Integer)m_data).intValue());
    }
    else if(m_key.equals(DefinableArchivalUnit.KEY_AU_CRAWL_WINDOW)) {
      m_data = data;
      m_plugin.setAuCrawlWindow((String)m_data, true);
    }
    else if(m_key.equals(DefinableArchivalUnit.KEY_AU_DEFAULT_PAUSE_TIME)) {
      m_data = new Long(data);
      m_plugin.setAuPauseTime(((Long)m_data).longValue());
      m_displayString = TimeEditor.millisToString(((Long)m_data).longValue());
    }
    else if(m_key.equals(DefinableArchivalUnit.KEY_AU_DEFAULT_NEW_CONTENT_CRAWL_INTERVAL)) {
      m_data = new Long(data);
      m_plugin.setNewContentCrawlInterval(((Long) m_data).longValue());
      m_displayString = TimeEditor.millisToString(((Long)m_data).longValue());
    }
    else if(m_key.equals(DefinablePlugin.KEY_EXCEPTION_HANDLER)) {
      m_data = data;
      m_plugin.setPluginExceptionHandler((String)m_data, true);
    }
    else if(m_key.equals(DefinablePlugin.KEY_REQUIRED_DAEMON_VERSION)) {
      m_data = data;
      m_plugin.setRequiredDaemonVersion((String)m_data);
    }
    else if(m_key.equals(DefinablePlugin.KEY_CRAWL_TYPE)) {
      m_data = data;
      m_plugin.setCrawlType((String)m_data);
    }

    notifyListenersOfChange();
  }

  public void updateOtherData(String data) {
    notifyListenersOfChange();
  }

}
