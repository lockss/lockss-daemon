/*
 * $Id$
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

package org.lockss.ant;

import java.io.File;
import java.lang.reflect.*;

import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.selectors.*;

/**
 * Selector that filters classes that have a
 * <pre>public static Class testedClasses[]</pre>
 * array that includes the named class (passed in as 'class' property)
 *
 * @author tal
 */
public class ClassTestSelector extends BaseExtendSelector {

  private String className = null;
  private Class classToTest = null;

  public final static String CLASS_KEY = "class";

  public ClassTestSelector() {
  }

  public String toString() {
    return "{ClassTestSelector class: " + className + "}";
  }

  /**
   * Set the name of the class to test.
   *
   * @param className
   */
  public void setClass(String className) {
    this.className = className;
  }

  /**
   * When using this as a custom selector, this method will be called.
   * It translates each parameter into the appropriate setXXX() call.
   *
   * @param parameters the complete set of parameters for this selector
   */
  public void setParameters(Parameter[] parameters) {
    super.setParameters(parameters);
    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
	String paramname = parameters[i].getName();
	if (CLASS_KEY.equalsIgnoreCase(paramname)) {
	  setClass(parameters[i].getValue());
	}
	else {
	  setError("Invalid parameter " + paramname);
	}
      }
    }
  }

  /**
   * Checks to make sure all settings are kosher. In this case, it
   * means that the className has been set, and corresponds to a
   * loadable class
   */
  public void verifySettings() {
    if (className == null) {
      setError("The class attribute is required");
    }
    if (classToTest == null) {
      try {
	classToTest = Class.forName(className);
      } catch (ClassNotFoundException e) {
	setError("class not found: " + className);
      }
    }
  }

  /**
   * Transoform a filename into a class name and attempt to load the class
   */
  private Class classFromFilename(String filename) {
    int dot = filename.lastIndexOf('.');
    String noext = (dot == 0
		    ? filename
		    : filename.substring(0, dot));
    String name = noext.replace(File.separatorChar, '.');
    Class c = null;
    try {
      c = Class.forName(name);
    } catch (ClassNotFoundException e) {
      setError("class not found for file: " + filename);
    }
    return c;
  }

  /**
   * The heart of the matter. This is where the selector gets to decide
   * on the inclusion of a file in a particular fileset.
   *
   * @param basedir the base directory the scan is being done from
   * @param filename is the name of the file to check
   * @param file is a java.io.File object the selector can use
   * @return true if the named file corresponds to a class whose
   * testedClasses array includes the class named by the 'class' parameter
   */
  public boolean isSelected(File basedir, String filename, File file) {
    // throw BuildException on error
    validate();

    if (file.isDirectory()) {
      return false;
    }

    Class candidateClass = classFromFilename(filename);
    if (candidateClass == null) {
      return false;
    }
    try {
      Field fld = candidateClass.getDeclaredField("testedClasses");
      Class testedClasses[] = (Class [])(fld.get(null));
      for (int ix = 0; ix < testedClasses.length; ix++) {
	if (classToTest == testedClasses[ix]) {
	  return true;
	}
      }
    } catch (NoSuchFieldException e) {
      // Test class doesn't have testedClasses field, no error
      return false;
    } catch (IllegalAccessException e) {
      // If it has the field, complain if it isn't accessible
      setError(candidateClass.getName() + ".testedClasses is inaccessible");
    } catch (ClassCastException e) {
      // or is of the wrong type
      setError(candidateClass + ".testedClasses is not an array of classes");
    }
    return false;
  }
}

