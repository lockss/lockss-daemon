/*
 * $Id: Plugin.java,v 1.7 2003-02-20 22:27:26 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.*;
import org.lockss.daemon.*;

/**
 * Interface required by a plugin to be used by the lockss daemon.  All
 * Plugin implementations must have a no-arg constructor.
 * @author Claire Griffin
 * @version 1.0
 */
public interface Plugin {

  /**
   * Called after plugin is loaded to give the plugin time to perform any
   * needed initializations
   */
  public void initPlugin();

  /**
   * Called when the application is stopping to allow the plugin to perform
   * any necessary tasks needed to cleanly halt
   */
  public void stopPlugin();

  /**
   * Return a string that uniquely represents the identity of this plugin
   * @return a string that identifies this plugin
   */
  public String getPluginId();

  /**
   * Return a string that represents the current version of this plugin
   * @return a String representing the current version
   */
  public String getVersion();

  /**
   * Return the list of names of the Archival Units and volranges supported by
   * this plugin
   * @return a List of Strings
   */
  public List getSupportedAUNames();

  /**
   * Return the set of configuration properties required to configure
   * an archival unit for this plugin.
   * @return a List of strings which are the names of the properties for
   * which values are needed in order to configure an AU
   */
  public List getAUConfigProperties();

  /**
   * Return the AU Id string for the ArchivalUnit handling the AU specified
   * by the given configuration. This must be completely determined by the
   * subset of the configuration info that's necessary to identify the AU.
   * @return the AUId string
   * @throws ArchivalUnit.ConfigurationException if the configuration is
   * illegal in any way.
   */
  public String getAUIdFromConfig(Configuration configInfo) 
      throws ArchivalUnit.ConfigurationException;

  /**
   * Create or (re)configure an ArchivalUnit from the specified configuration.
   * @param auConfig Configuration object with values for all properties
   * returned by {@link #getAUConfigProperties()}
   */
  public ArchivalUnit configureAU(Configuration config)
      throws ArchivalUnit.ConfigurationException;

  /**
   * Create an ArchivalUnit for the AU specified by the configuration.
   * @param auConfig Configuration object with values for all properties
   * returned by {@link #getAUConfigProperties()}
   */
  public ArchivalUnit createAU(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException;

  /**
   * Lookup the ArchivalUnit corresponding to the AUId.
   * @param auId The AUId for an AU within this plugin.  (Often obtained from
   * {@link AuUrl#getAuId(URL)}.)
   * @return the ArchivalUnit, or null if not found
   */
  public ArchivalUnit getAU(String auId);

  /**
   * Return a collection of all ArchivalUnits that exist within this plugin
   * @return a collection of AUs
   */
  public Collection getAllAUs() ;
}
