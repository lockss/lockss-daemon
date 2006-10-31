/*
 * $Id: PersistentPluginState.java,v 1.6 2006-10-31 07:01:06 thib_gc Exp $
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
    public static final int ALL_DIRTY_BITS_ON  = 10;

    private Properties dirtyBits;
    private Properties configParameters;
    private Properties filters;
    private String[] saveFile;

    public PersistentPluginState(){
	dirtyBits           = new Properties();
	configParameters    = new Properties();
	filters             = new Properties();
	saveFile            = new String[2];
	saveFile[LOCATION]  = new String("");
	saveFile[NAME]      = new String("");
    }

    public String getDirtyBit(String key){
	return dirtyBits.getProperty(key,"on");
    }

    public String getConfigParameterValue(String key){
	return configParameters.getProperty(key,"");
    }

    public String getFilterFieldValue(String key){
       	return filters.getProperty(key,"");
    }

    public String[] getSaveFileName(){
	return saveFile;
    }

    public void setDirtyBit(String key,String value){
	dirtyBits.setProperty(key,value);
    }

    public void setAllDirtyBitsOn(){
	//Clears all of the dirty bit properties so that
	//all requests for dirty bits return the default
	//value of "on"
        dirtyBits.clear();
    }

    public void setConfigParameterValue(String key,String value){
	configParameters.setProperty(key,value);
    }

    public void setFilterFieldValue(String key,String value){
	filters.setProperty(key,value);
    }

    public void setSaveFileName(String location,String filename){
	saveFile[LOCATION] = location;
	saveFile[NAME]     = filename;
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
