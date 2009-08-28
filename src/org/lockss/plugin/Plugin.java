/*
 * $Id: Plugin.java,v 1.29.4.1 2009-08-28 23:03:16 dshr Exp $
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

package org.lockss.plugin;

import java.util.*;

import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

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
   * @param daemon the LockssDaemon instance
   */
  public void initPlugin(LockssDaemon daemon);

  /**
   * Called when the application is stopping to allow the plugin to perform
   * any necessary tasks needed to cleanly halt
   */
  public void stopPlugin();

  /**
   * Stop an AU (because it has been deleted or deactivated)
   */
  public void stopAu(ArchivalUnit au);

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
   * Return the minimum daemon version required by this plugin
   * @return the minimum daemon version
   */
  public String getRequiredDaemonVersion();

  /**
   * Return the human-readable name of the plugin
   * @return the name
   */
  public String getPluginName();

  /**
   * Return the human-readable name of the publishing platform, if any.
   * @return the name
   */
  public String getPublishingPlatform();

  /**
   * Return the list of names of the {@link ArchivalUnit}s and volranges
   * supported by this plugin.
   * @return a List of Strings
   */
  public List getSupportedTitles();

  /**
   * Return the (possibly incomplete) parameter assignments
   * that will configure this plugin for the specified title.
   * @param title journal title, as returned by getSupportedTitles().
   * @return the {@link TitleConfig} for the title
   */
  public TitleConfig getTitleConfig(String title);

  /**
   * Return a list of descriptors for configuration parameters required to
   * configure an archival unit for this plugin.
   * @return a List of {@link ConfigParamDescr}s, in the order the plugin
   * would like them presented to the user.
   */
  public List getAuConfigDescrs();

  /**
   * Return the ConfigParamDescr for the named param, or null if none.
   */
  public ConfigParamDescr findAuConfigDescr(String key);

  /**
   * Create or (re)configure an {@link ArchivalUnit} from the specified
   * configuration.
   * @param config {@link Configuration} object with values for the AU
   * config params
   * @param au {@link ArchivalUnit} if one already exists
   * @return a configured {@link ArchivalUnit}
   * @throws ArchivalUnit.ConfigurationException if unable to configure au
   */
  public ArchivalUnit configureAu(Configuration config, ArchivalUnit au)
      throws ArchivalUnit.ConfigurationException;

  /**
   * Create an ArchivalUnit for the AU specified by the configuration.
   * @param auConfig {@link Configuration} object with values for the AU
   * config params
   * @return an {@link ArchivalUnit}
   * @throws ArchivalUnit.ConfigurationException
   */
  public ArchivalUnit createAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException;

  /**
   * Return a collection of all {@link ArchivalUnit}s that exist within this
   * plugin.
   * @return a {@link Collection} of AUs
   */
  public Collection getAllAus();

  /**
   * Return the LockssDaemon instance
   * @return the LockssDaemon instance
   */
  public LockssDaemon getDaemon();

  /** Create and return a new instance of a plugin auxilliary class.
   * @param className the name of the auxilliary class
   * @param expectedType Type (class or interface) of expected rexult
   */
  public Object newAuxClass(String className, Class expectedType);

  /**
   * Return a {@link MetadataExtractor} that knows how to extract URLs from
   * content of the given MIME type
   * @param contentType content type to get a content parser for
   * @param au the AU in question
   * @return A MetadataExtractor or null
   */
    public MetadataExtractor getMetadataExtractor(String contentType,
						  ArchivalUnit au);

  /**
   * Returns the article iterator factory for the mime type, if any
   * @param contentType the content type
   * @return the ArticleIteratorFactory
   */
    public ArticleIteratorFactory getArticleIteratorFactory(String contentType);

  /**
   * Returns the default mime type of articles in this AU
   * @return the default MimeType
   */
  public String getDefaultArticleMimeType();
}
