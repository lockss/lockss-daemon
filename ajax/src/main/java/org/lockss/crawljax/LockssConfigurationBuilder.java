/*
 * $Id: LockssConfigurationBuilder.java,v 1.1 2014/04/14 23:08:24 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.crawljax;

import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.core.configuration.InputSpecification;

/**
 * Interface which must be implemented for converting a list of properties into
 * a CrawljaxConfiguration.
 */
public interface LockssConfigurationBuilder {
  /**
   * Configure a CrawljaxConfiguration for a given url from a configuration
   * file with all output stored in a directory.
   *
   * @param urlValue the url we use for the CrawljaxConfiguration
   * @param outDir the output directory to use for content
   * @param configFile the properties file to use for the
   * CrawljaxConfiguration
   *
   * @return the CrawljaxConfiguration
   */
  CrawljaxConfigurationBuilder configure(String urlValue,
                                  String outDir,
                                  String configFile);

  /**
   * Build create an InputSpecification and configure it
   * @return a new created and configured InputSpecification
   */
  InputSpecification getInputSpecification();
}
