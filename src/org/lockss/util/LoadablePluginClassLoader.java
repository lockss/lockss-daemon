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

import java.net.*;

/**
 * A very simple classloader that overrides the default delegation
 * behavior of URLClassLoader.  This loader will attempt to load a
 * requested class itself first and, if it is unable to, will delegate
 * the class loading to its parent.
 */
public class LoadablePluginClassLoader extends URLClassLoader {
  static Logger log = Logger.getLogger("LPClassLoader");

  public LoadablePluginClassLoader(URL[] urls) {
    super(urls);
  }

  public LoadablePluginClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  /**
   * Attempt to load the requested class from this classloader first.
   * If it is not found, it will delegate to its parent classloader.
   */
  public synchronized Class loadClass(String className)
      throws ClassNotFoundException {
    // Look in the cache (defined by ClassLoader) to see if the class
    // has already been loaded.
    Class c = findLoadedClass(className);

    if (c != null) {
      return c;
    }

    // The class has not already been loaded, so we'll try to find it.
    // We'll ignore ClassNotFoundException, and delegate to our parent
    // classloader if needed.
    try {
      return findClass(className);
    } catch (ClassNotFoundException ignore) {}

    // If we're here, it means we haven't found the class yet.  Ask
    // our parent.  This may throw ClassNotFoundException, but that's
    // what we want if the parent can't find the class.  If there is
    // no parent, we'll just have to throw a ClassNotFoundException.
    ClassLoader parent = getParent();

    if (parent != null) {
      return parent.loadClass(className);
    }

    throw new ClassNotFoundException(className);
  }

  /**
   * Attempt to load the requested resource from this classloader
   * first.  If it is not found, it will delegate to its parent
   * classloader.
   */
  public URL getResource(String name) {
    URL url = findResource(name);
    ClassLoader parent = getParent();

    if (url == null && parent != null) {
      url = parent.getResource(name);
    }

    return url;
  }

  public void close() {
    try {
      log.debug2("Attempting close");
      Class clazz = java.net.URLClassLoader.class;
      java.lang.reflect.Field ucp = clazz.getDeclaredField("ucp");
      ucp.setAccessible(true);
      Object sun_misc_URLClassPath = ucp.get(this);
      java.lang.reflect.Field loaders = 
	sun_misc_URLClassPath.getClass().getDeclaredField("loaders");
      loaders.setAccessible(true);
      Object java_util_Collection = loaders.get(sun_misc_URLClassPath);
      java.util.Collection coll = (java.util.Collection) java_util_Collection;
      log.debug2("Found " + coll.size() + " loaders");
      for (Object sun_misc_URLClassPath_JarLoader :
	     ((java.util.Collection) java_util_Collection).toArray()) {
	try {
	  java.lang.reflect.Field loader = 
            sun_misc_URLClassPath_JarLoader.getClass().getDeclaredField("jar");
	  loader.setAccessible(true);
	  Object java_util_jar_JarFile = 
            loader.get(sun_misc_URLClassPath_JarLoader);
	  ((java.util.jar.JarFile) java_util_jar_JarFile).close();
	  log.debug2("Closed " + java_util_jar_JarFile);
	} catch (Throwable t) {
	  log.warning("JarFile.close()", t);
	  // if we got this far, this is probably not a JAR loader so skip it
	}
      }
    } catch (Throwable t) {
      // probably not a SUN VM
      log.warning("close()", t);
    }
    return;
  }


}
