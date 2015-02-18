/*
 * $Id$
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

package org.lockss.util;
import java.util.*;
import java.io.*;

/**
 * static methods to retrieve various info about the build environment
 */

public class BuildInfo {
  /** Name of the property file into which the build writes build properties.
      This should agree with the value of build.info.file in build.xml */
  public static final String PROPERTY_RESOURCE =
    "org/lockss/htdocs/build.properties";
  /** Name of build timestamp property.  Should agree with the name used in
   * build.xml */
  public static final String BUILD_TIMESTAMP = "build.timestamp";
  /** Name of build time property.  Should agree with the name used in
   * build.xml */
  public static final String BUILD_TIME = "build.time";
  /** Name of build date property.  Should agree with the name used in
   * build.xml */
  public static final String BUILD_DATE = "build.date";
  /** Name of build user name property.  Should agree with the name used in
   * build.xml */
  public static final String BUILD_USER_NAME = "build.user.name";
  /** Name of build host property.  Should agree with the name used in
   * build.xml */
  public static final String BUILD_HOST = "build.host";
  /** Name of release name property.  Should agree with the name used in
   * build.xml */
  public static final String BUILD_RELEASENAME = "build.releasename";

  private static Logger log = Logger.getLogger("BuildInfo");

  private static Properties buildProps = null;

  /**
   * Return the value of a build property
   * @param prop the name of the property
   * @return the value of the property
   */
  public static String getBuildProperty(String prop) {
    String s = findBuildProps().getProperty(prop);
    if (s != null && !s.startsWith("${")) {
      return s;
    }
    return null;
  }

  /** Return a string with all relevant build info */
  public static String getBuildInfoString() {
    String buildTimeStamp = getBuildProperty(BUILD_TIMESTAMP);
    String buildHost = getBuildProperty(BUILD_HOST);
    String releaseName = getBuildProperty(BUILD_RELEASENAME);

    StringBuffer sb = new StringBuffer();
    sb.append("Daemon ");
    if (releaseName != null) {
      sb.append(releaseName);
      sb.append(" ");
    }
    sb.append("built ");
    sb.append(buildTimeStamp);
    if (buildHost != null) {
      sb.append(" on ");
      sb.append(buildHost);
    }
    return sb.toString();
  }

  private static synchronized Properties findBuildProps() {
    if (buildProps == null) {
      Properties props = new Properties();
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	InputStream istr = loader.getResourceAsStream(PROPERTY_RESOURCE);
	props.load(istr);
	log.debug2(props.toString());
	istr.close();
      } catch (Exception e) {
	log.warning("Can't load build info", e);
	props = new Properties();
      }
      buildProps = props;
    }
    return buildProps;
  }
}

