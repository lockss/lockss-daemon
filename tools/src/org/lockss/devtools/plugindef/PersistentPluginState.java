/*
 * $Id: PersistentPluginState.java,v 1.2 2005-08-31 18:35:38 rebeccai Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**********************************************************************
 *  class PersistentPluginState holds user input that should persist
 *  throughout the session with the Plugin Tool.  Examples include
 *  the dirty bit for saving the plugin, and the user input for testing
 *  the crawl rules or validating the plugin.
 *
 *  PersistentPluginState is currently instantiated once as a member
 *  of an EditableDefinablePlugin object.  
 *
 *  Author:  Rebecca Illowsky
 *  Created: 8/31/05
 *  Version: 0.7
 *  LOCKSS
 **********************************************************************/

public class PersistentPluginState{
    /* pluginState Fields */
    public static final int DIRTY_BIT          = 0;
    public static final int CONFIG_PARAMETERS  = 1;
    public static final int FILTERS            = 2;
    public static final int SAVE_FILE          = 3;
       public static final int LOCATION  = 0;
       public static final int NAME      = 1;

    /* pluginState Keys */
    //total number of fields above
    private static final int NUM_FIELDS        = 4;

    public static final int ALL_DIRTY_BITS_ON  = 10;

    private Object[] pluginState = new Object[NUM_FIELDS];

    public PersistentPluginState(){
	pluginState[DIRTY_BIT]                  = new Properties();
	pluginState[CONFIG_PARAMETERS]          = new Properties();
	pluginState[FILTERS]                    = new Properties();
	pluginState[SAVE_FILE]                  = new String[2];
	((String[]) pluginState[SAVE_FILE])[LOCATION]  = new String("");
	((String[]) pluginState[SAVE_FILE])[NAME]      = new String("");
    }

    public String getDirtyBit(String key){
	return (String) ((Properties) pluginState[DIRTY_BIT]).getProperty(key,"on");
    }

    public String getConfigParameterValue(String key){
	return (String) ((Properties) pluginState[CONFIG_PARAMETERS]).getProperty(key,"");
    }

    public String getFilterFieldValue(String key){
       	return (String) ((Properties) pluginState[FILTERS]).getProperty(key,"");
    }

    public String[] getSaveFileName(){
	return (String[]) pluginState[SAVE_FILE];
    }

    public void setDirtyBit(String key,String value){
	((Properties) pluginState[DIRTY_BIT]).setProperty(key,value);
    }

    public void setAllDirtyBitsOn(){
	//Clears all of the dirty bit properties so that
	//all requests for dirty bits return the default
	//value of "on"
        ((Properties) pluginState[DIRTY_BIT]).clear();
    }

    public void setConfigParameterValue(String key,String value){
	((Properties) pluginState[CONFIG_PARAMETERS]).setProperty(key,value);
    }

    public void setFilterFieldValue(String key,String value){
	((Properties) pluginState[FILTERS]).setProperty(key,value);
    }

    public void setSaveFileName(String location,String filename){
	((String[]) pluginState[SAVE_FILE])[LOCATION] = location;
	((String[]) pluginState[SAVE_FILE])[NAME]     = filename;
    }

    public void setPluginState(int field,String key,String value){
	switch(field){
	case ALL_DIRTY_BITS_ON:
	    setAllDirtyBitsOn();
	    break;
	case DIRTY_BIT:
	    setDirtyBit(key,value);
	    break;
	case CONFIG_PARAMETERS:
	    setConfigParameterValue(key,value);
	    break;
	case FILTERS:
	    setFilterFieldValue(key,value);
	    break;
	case SAVE_FILE:
	    setSaveFileName(key,value);
	    break;
	default:
	    break;
	}
    }

}
