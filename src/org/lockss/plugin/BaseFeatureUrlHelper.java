/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.TypedEntryMap;

/**
 * Base class for FeatureUrlHelper.  Plugins should extend this rather than
 * implement the {@link FeatureUrlHelper} interface in order to retain
 * binary compatibility in case methods are added to the interface. */
public class BaseFeatureUrlHelper implements FeatureUrlHelper {

  /** {@inheritDoc}
   * Default implementation returns null */
  @Override
  public List<String> getFeatureUrls(ArchivalUnit au,
				     OpenUrlResolver.OpenUrlInfo.ResolvedTo itemType,
				     TypedEntryMap paramMap) 
      throws PluginException, IOException {
    return null;
  }

  /** {@inheritDoc}
   * Default implementation returns null */
  @Override
  public Collection<String> getAccessUrls(ArchivalUnit au) 
      throws PluginException, IOException {
    return null;
  }
}
