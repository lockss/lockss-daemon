/*
 * $Id: EDPCellData.java,v 1.10 2006-06-26 17:46:56 thib_gc Exp $
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

    if(key.equals(EditableDefinablePlugin.PLUGIN_NAME)) {
      m_data = edp.getPluginName();
    }
    else if(key.equals(EditableDefinablePlugin.PLUGIN_IDENTIFIER)) {
      m_data = edp.getPluginIdentifier();
    }
    else if(key.equals(EditableDefinablePlugin.PLUGIN_VERSION)) {
      m_data = edp.getPluginVersion();
    }
    else if(key.equals(EditableDefinablePlugin.PLUGIN_NOTES)) {
      m_data = edp.getPluginNotes();
    }
    else if(key.equals(EditableDefinablePlugin.PLUGIN_PROPS)) {
      m_data = edp.getConfigParamDescrs();
    }
    else if(key.equals(EditableDefinablePlugin.AU_RULES)) {
      m_data = edp.getAuCrawlRules();
    }
    else if(key.equals(EditableDefinablePlugin.AU_START_URL)) {
      m_data = new PrintfTemplate(edp.getAuStartUrl());
    }
    else if(key.equals(EditableDefinablePlugin.AU_NAME)) {
      m_data = new PrintfTemplate(edp.getAuName());
    }
    else if(key.equals(EditableDefinablePlugin.AU_CRAWL_DEPTH)) {
      m_data = new Integer(edp.getAuCrawlDepth());
    }
    else if(key.equals(EditableDefinablePlugin.AU_CRAWL_WINDOW)) {
      m_data = edp.getAuCrawlWindow();
    }
    else if(key.equals(EditableDefinablePlugin.AU_FILTER_SUFFIX)) {
      m_data = edp.getAuFilters();
    }
    else if(key.equals(EditableDefinablePlugin.AU_PAUSE_TIME)) {
      m_data = new Long(edp.getAuPauseTime());
      m_displayString = TimeEditor.millisToString(edp.getAuPauseTime());
    }
    else if(key.equals(EditableDefinablePlugin.AU_NEWCONTENT_CRAWL)) {
      m_data = new Long(edp.getNewContentCrawlIntv());
      m_displayString = TimeEditor.millisToString(edp.getNewContentCrawlIntv());
    }
    else if(key.equals(EditableDefinablePlugin.PLUGIN_EXCEPTION_HANDLER)) {
      m_data = edp.getPluginExceptionHandler();
    }
    else if(key.equals(EditableDefinablePlugin.CM_EXCEPTION_LIST_KEY)) {
      m_data = edp.getSingleExceptionHandlers();
    }
    else if(key.equals(EditableDefinablePlugin.CM_CRAWL_TYPE)) {
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
	  if(m_key.equals(EditableDefinablePlugin.AU_CRAWL_WINDOW))
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
    if (m_key.equals(EditableDefinablePlugin.AU_NAME)) {
      m_plugin.setAuName(template.getViewableTemplate());
    }
    else if (m_key.equals(EditableDefinablePlugin.AU_START_URL)) {
      m_plugin.setAuStartURL(template.getViewableTemplate());
    }

    notifyListenersOfChange();
  }

  public void updateStringData(String data) {
    if(m_plugin == null || m_key == null || data == null) return;
    if(m_key.equals(EditableDefinablePlugin.PLUGIN_NAME)) {
      m_data = data;
      m_plugin.setPluginName((String)m_data);
    }
    else if(m_key.equals(EditableDefinablePlugin.PLUGIN_IDENTIFIER)) {
      m_data = data;
      m_plugin.setPluginIdentifier((String)m_data);
    }
    else if(m_key.equals(EditableDefinablePlugin.PLUGIN_VERSION)) {
      m_data = data;
      m_plugin.setPluginVersion((String)m_data);
    }
    else if(m_key.equals(EditableDefinablePlugin.PLUGIN_NOTES)) {
      m_data = data;
      m_plugin.setPluginNotes((String)m_data);
      m_displayString = data;
    }
    else if(m_key.equals(EditableDefinablePlugin.AU_CRAWL_DEPTH)) {
      m_data = new Integer(data);
      m_plugin.setAuCrawlDepth(((Integer)m_data).intValue());
    }
    //RI
    /*
    else if(m_key.equals(m_plugin.AU_CRAWL_WINDOW)) {
      m_data = data;
      m_plugin.setAuCrawlWindow((String)m_data);
    }
    */
    else if(m_key.equals(EditableDefinablePlugin.AU_PAUSE_TIME)) {
      m_data = new Long(data);
      m_plugin.setAuPauseTime(((Long)m_data).longValue());
      m_displayString = TimeEditor.millisToString(((Long)m_data).longValue());
    }
    else if(m_key.equals(EditableDefinablePlugin.AU_NEWCONTENT_CRAWL)) {
      m_data = new Long(data);
      m_plugin.setNewContentCrawlIntv(((Long) m_data).longValue());
      m_displayString = TimeEditor.millisToString(((Long)m_data).longValue());
    }
    else if(m_key.equals(EditableDefinablePlugin.PLUGIN_EXCEPTION_HANDLER)) {
      m_data = data;
      m_plugin.setPluginExceptionHandler((String)m_data);
    }
    else if(m_key.equals(EditableDefinablePlugin.CM_CRAWL_TYPE)) {
      m_data = data;
      m_plugin.setCrawlType((String)m_data);
    }

    notifyListenersOfChange();
  }

  public void updateOtherData(String data) {
    notifyListenersOfChange();
  }

}
