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
import org.lockss.util.urlconn.*;
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
   * Return a string that represents the current version of this plugin's
   * implementation of the designated feature.  Determines polling
   * compatibility between plugins, when metadata must be recalculated,
   * etc.
   * @return a String representing the feature version, or null if the
   * plugin doesn't implement the feature or doesn't declare a version.
   */
  public String getFeatureVersion(Feature feat);

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
  public List<String> getSupportedTitles();

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
  public List<ConfigParamDescr> getAuConfigDescrs();

  /**
   * Return the ConfigParamDescr for the named param, or null if none.
   */
  public ConfigParamDescr findAuConfigDescr(String key);

  /**
   * Create an instance of the plugin's AuParamFunctor if any.
   * @return an AuParamFunctor or null if none.
   */
  public AuParamFunctor getAuParamFunctor() throws PluginException.LinkageError;

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
  public Collection<ArchivalUnit> getAllAus();

  /**
   * Return the LockssDaemon instance
   * @return the LockssDaemon instance
   */
  public LockssDaemon getDaemon();

  /** Create and return a new instance of a plugin auxilliary class.
   * @param className the name of the auxilliary class
   * @param expectedType Type (class or interface) of expected rexult
   */
  public <T> T newAuxClass(String className, Class<T> expectedType);

  /** Return the Exception map for fetch results & errors.
   * @param au the AU
   * @return the plugin's CacheResultMap
   */
  public CacheResultMap getCacheResultMap();

  /**
   * Return an {@link ArticleMetadataExtractor} that knows how to interpret
   * the ArcticleFiles generated by this plugin's ArticleIterator
   * @param target the purpose for runnning the iterator
   * @param au the AU in question
   * @return A ArticleMetadataExtractor or null
   */
    public ArticleMetadataExtractor
      getArticleMetadataExtractor(MetadataTarget target,
				  ArchivalUnit au);

  /**
   * Return a {@link FileMetadataExtractor} that knows how to extract
   * metadata from content of the given content type
   * @param target the purpose for which metadata is being extracted
   * @param contentType content type to get a metadata extractor for
   * @param au the AU in question
   * @return A FileMetadataExtractor or null
   */
    public FileMetadataExtractor
      getFileMetadataExtractor(MetadataTarget target,
			       String contentType,
			       ArchivalUnit au);

  /**
   * Returns the article iterator factory for the mime type, if any
   * @param contentType the content type
   * @return the ArticleIteratorFactory
   */
    public ArticleIteratorFactory getArticleIteratorFactory();

  /**
   * Returns the default mime type of articles in this AU
   * @return the default MimeType
   */
  public String getDefaultArticleMimeType();

  /**
   * Provides an indication of whether this is a plugin for bulk content.
   * 
   * @return a boolean with <code>true</code> if this is a plugin for bulk
   *         content, <code>false</code> otherwise.
   */
  public boolean isBulkContent();

  /**
   * Names of daemon features whose operation is influenced by plugins.
   * Used by plugins to associate version strings with their
   * implementation/support for that feature.  (Could be used for other
   * things.)
   */
  // Warning: when adding new Features that are preserved in AuState,
  // HistoryRepositoryImpl.loadAuState must be updated.
  public enum Feature {
    /**
     * Version of Plugin data that affects polling, such as hash filters,
     * crawl rules (usually), start URL, etc.  Should be changed whenver
     * the plugin's polling behavior changes in a way that makes it unable
     * to correctly participate in a poll with a peer running a different
     * version.
     * @since Daemon 1.49
     */
    Poll,
      /**
       * Version of article iterators, metadata extractors and factories,
       * etc.  Should be changed whenever this code changes in a way that
       * requires re-extraction of metadata.
       * @since Daemon 1.49
       */
      Metadata,
      /**
       * Version of substance checker patterns.
       * @since Daemon 1.50
       */
      Substance,
      };

}
