/*
 * $Id: WrapperState.java,v 1.3 2005-01-04 03:00:46 tlipkis Exp $
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

/**
 * <p>Title: WrapperState </p>
 * <p>Description: Maintains a mapping of plugin objects to their wrapped
 * counterparts, and vice versa.  Also contains factory and other static methods
 * for dealing with the wrapper layer.
 *
 * This class relies on the <code>IdentityHashMap</code> found in JDK1.4
 * Mappings are based on reference equality (==).  It will still compile and
 * run normally under JDK1.3, but wrapping functionality will not work; attempts
 * to load a wrapped plugin will result in the original, underlying plugin being
 * used.
 *
 * This class has only static methods and cannot be instantiated.
 *
 * @author Tyrone Nicholas
 * @version 1.0
 */

import java.util.*;
import java.lang.reflect.*;
import org.lockss.util.*;
import org.lockss.app.LockssDaemon;

public class WrapperState {

  // Private constructor to prevent instantiation
  private WrapperState() {}

  /** Flag to indicate whether wrapping is turned on.  Set to true if running
   * under JDK 1.4, false otherwise.  */
  private static boolean usingWrapping;

  /** Naming prefix for wrapped classes */
  public static final String PREFIX = "Wrapped";

  /** Package of the wrapped classes */
  private static final String PACKAGE = "org.lockss.plugin.wrapper.";

  public static final String WRAPPED_PLUGIN_NAME =
      PACKAGE + "WrappedPlugin";



  /** Master container; key=Class objects, value=map of instances,
   *  where key=instance, value=wrapped instance */
  private static Map classMap;

  /** Logger  */
  private static Logger log;

  /** key=plugin key, value=wrapped plugin */
  private static Map pluginMap;


  /** Static initializer.  Tests for the presence of JDK1.4 by trying to load
   *  the IdentityHashMap class.  */
  static {
    classMap = new HashMap();
    pluginMap = new HashMap();
    log = Logger.getLogger(WrapperLogger.WRAPPED_LOG_NAME);
    try {
      Class mapClass = Class.forName("java.util.IdentityHashMap");
      usingWrapping = true;
      log.info("The wrapper layer is ACTIVE.");
      register("org.lockss.plugin.Plugin");
    } catch (ClassNotFoundException e) {
      usingWrapping = false;
      log.debug("The wrapper layer is INACTIVE.");
    }
  }

  /** Used by generated classes to register themselves */
  public static void register(String intf) {
    try {
      Class cl = Class.forName(intf);
      classMap.put(cl, createIdentityMapIfAvailable());
    } catch (ClassNotFoundException e) {
      log.error("Wrapped class " + intf + " not found.  Wrapping will be DISABLED.");
      usingWrapping = false;
    }
  }

  /** Returns an IdentityMap if available, else WeakHashMap */
  private static Map createIdentityMapIfAvailable() {
    if (usingWrapping) {
      try {
        Class mapClass = Class.forName("java.util.IdentityHashMap");
        return (Map) mapClass.newInstance();
      } catch (Exception e) {
        usingWrapping = false;
        return new WeakHashMap();
      }
    }
    else {
      return new WeakHashMap();
    }
  }

  /** Returns an instance map of a class, creating if it necessary */
  static Map getInstanceMap(Class cl) {
    if (!classMap.containsKey(cl)) {
        Map instanceMap = createIdentityMapIfAvailable();
        classMap.put(cl,instanceMap);
        return instanceMap;
    } else {
      return (Map)classMap.get(cl);
    }
  }

  /** Returns a WrappedMap if using wrapping, else a regular HashMap.
   * Called by the <code>AuSpecificManagerHandler</code> class.  Use this
   * anywhere where a hash map takes wrapped classes as keys. */
  public static HashMap createWrappedMapIfAvailable() {
    if (usingWrapping) {
      return new WrappedMap();
    } else {
      return new HashMap();
    }
  }

  /** Given a regular object, returns a wrapped version of it.  If not using
   * wrapping, or not a class that can be wrapped, or already wrapped */
  public static Object getWrapper(Object obj) {
    if (!usingWrapping || (obj instanceof Wrapped)) {
      return obj;
    }
    try {
      Class classToBeWrapped = findClassWhichCanBeWrapped(obj.getClass());
      if (classToBeWrapped == null) {
        return obj;
      }
      Map instanceMap = getInstanceMap(classToBeWrapped);
      if (instanceMap.containsKey(obj)) {
        return instanceMap.get(obj);
      }
      else {
        String barename =
            ClassUtil.getClassNameWithoutPackage(classToBeWrapped);
        String wrappedName = PACKAGE + PREFIX + barename;
        Class wrapped = Class.forName(wrappedName);
        Class[] classarray = new Class[1];
        classarray[0] = classToBeWrapped;
        Constructor con = wrapped.getConstructor(classarray);
        Object[] objarray = new Object[1];
        objarray[0] = obj;
        Object wrappedObj = con.newInstance(objarray);
        instanceMap.put(obj, wrappedObj);
        if (obj instanceof Plugin) {
          recordNewPlugin((Plugin)obj,(Plugin)wrappedObj);
        }
        return wrappedObj;
      }
    } catch (Exception e) {
      log.error("Error instantiating wrapped class",e);
      return obj;
    }
  }

  /** Place an entry in the plugin map */
  private static void recordNewPlugin(Plugin plugin, Plugin wrappedPlugin) {
    pluginMap.put(plugin.getPluginId(),wrappedPlugin);
  }

  /** Given a wrapped object, get the original.  If the object is not wrapped,
   * or wrapping is not in use, return the original.  */
  public static Object getOriginal(Object obj) {
    if (!usingWrapping || (!(obj instanceof Wrapped))) {
      return obj;
    } else {
      Class origclass = findWrappedClass(obj);
      if (origclass==null) {
        return obj;
      } else {
        Map instanceMap = (Map) classMap.get(origclass);
        Iterator it = instanceMap.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry entry = (Map.Entry) it.next();
          if (obj == entry.getValue()) {
            return entry.getKey();
          }
        }
        log.error("Wrapped object being referenced after deletion.");
        return null;
      }
    }
  }

  /** Returns the wrapped interface implemented by the object, or null
   * if it doesn't implement any  */
  private static Class findWrappedClass(Object obj) {
    Class cl = obj.getClass();
    Iterator it = classMap.keySet().iterator();
    while (it.hasNext()) {
      Class intf = (Class)it.next();
      if (intf.isInstance(obj)) {
        return intf;
      }
    }
    return null;
  }

  /** Tries to find if there is a wrapped prefix of this class  */
  private static Class findClassWhichCanBeWrapped(Class cl) {
    Class[] interfaces = cl.getInterfaces();
    for (int i=0; i<interfaces.length; i++) {
      String barename = ClassUtil.getClassNameWithoutPackage(interfaces[i]);
      try {
        Class wrapper = Class.forName(PACKAGE + PREFIX + barename);
        return interfaces[i];
      } catch (ClassNotFoundException e) {
        log.debug3("Tried interface " + barename + ": no wrapper found.");
      }
    }
    Class superclass = cl.getSuperclass();
    if (superclass==null) {
      return null;
    } else {
      return findClassWhichCanBeWrapped(superclass);
    }
  }


  /** Points an object of class WrappedPlugin to a Plugin object.  Called from
   * the <code>PluginManager</code> class.  Normally, wrapped objects are mapped
   * in their constructors.  This is not done for WrappedPlugin because it is
   * only creation when an archival unit using it is created.  It must
   * therefore be explicitly pointed at an original plugin. */
  static void pointWrappedPlugin(Plugin wrappedPlugin, String pluginKey) {
    // Use reflection to deal with the wrapped plugin, to avoid
    // a build dependency on the generated files
    if (!usingWrapping) {
      throw new RuntimeException(
          "Attempt was made to use wrapping when wrapping unavailable.");
    }
    try {
      Class wclass = wrappedPlugin.getClass();
      if (!wclass.getName().equals(WRAPPED_PLUGIN_NAME)) {
          throw new RuntimeException(
          "Attempt was made to point a non-wrapped plugin.");
      }
      if (!pluginMap.containsKey(pluginKey)) {
        Class[] params = {Plugin.class};
        Method setOriginal = wclass.getMethod("setOriginal", params);
        Object[] objects = new Object[1];
        objects[0] = Class.forName(pluginKey).newInstance();
        setOriginal.invoke(wrappedPlugin, objects);
        Map instanceMap = getInstanceMap(Plugin.class);
        instanceMap.put((Plugin)objects[0], wrappedPlugin);
      }
    } catch (NoSuchMethodException e) {
      handleBadWrappedPlugin();
    } catch (IllegalAccessException e) {
      handleBadWrappedPlugin();
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getTargetException().getMessage());
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public static Plugin retrieveWrappedPlugin(
      String pluginKey, LockssDaemon theDaemon) {
    try {
      String pname = PluginManager.pluginNameFromKey(pluginKey);
      if (pluginMap.containsKey(pname)) {
	log.debug("Already wrapped: " + pluginKey);
        return (Plugin) pluginMap.get(pname);
      }
      else {
        Plugin plugin = (Plugin) Class.forName(pname).newInstance();
        Plugin wrappedPlugin = (Plugin) getWrapper(plugin);
	log.debug("Wrapping: " + plugin + " in " + wrappedPlugin);
        pluginMap.put(pname, wrappedPlugin);
        wrappedPlugin.initPlugin(theDaemon);
        return wrappedPlugin;
      }
    } catch (Exception e) {
      log.error("Error instantating plugin " + pluginKey + ": " + e.getMessage());
      return null;
    }
  }


  private static void handleBadWrappedPlugin() {
    usingWrapping = false;
    String str = "WrappedPlugin class not present or invalid: disabling wrapping.";
    log.error(str);
    throw new RuntimeException(str);
  }


  /** Called by the finalizers of wrapped objects, to remove themselves from
   * the instance map so the originals can be garbage collected as well. */
  public static void removeWrapping(Object obj) {
    if (usingWrapping) {
      Class classToBeWrapped = findWrappedClass(obj);
      if (classToBeWrapped == null) {
        return;
      }
      Map instanceMap = getInstanceMap(classToBeWrapped);
      if (!instanceMap.containsKey(obj)) {
        return;
      }
      else {
        Object wrappedobj = instanceMap.remove(obj);
        if (obj instanceof Plugin) {
          pluginMap.remove(((Plugin)obj).getPluginId());
        }
      }
    }
  }

  public static boolean isUsingWrapping() {
    return usingWrapping;
  }

  // Shorthand to determine if an object is a WrappedPlugin
  public static boolean isWrappedPlugin(Object obj) {
    return obj.getClass().getName().equals(WRAPPED_PLUGIN_NAME);
  }

}
