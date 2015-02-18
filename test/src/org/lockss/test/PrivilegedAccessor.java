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

package org.lockss.test;
import java.util.*;
import java.lang.reflect.*;
import org.lockss.util.*;

/**
 * <code>PrivilegedAccessor</code> allows access to private or protected
 * constructors, methods and fields of other classes, for unit testing.  It
 * does this using reflection.  These accessors do not work with primitive
 * types, either as arguments or return values.<br>
 * There are several situations in which the compile-time type of an
 * expression, not the run-time class of an object, determines
 * behavior:<ol>
 * <li>References to static methods and fields are resolved based on the
 * type of the referring expression.</li>
 * <li>Overloaded methods are resolved based on the types of the
 * argument expressions.</li>
 * </ol>
 * In order for this facility to behave correctly in these cases, the type
 * of the expression must be explicitly supplied.  Wherever these methods
 * accept an object, one may instead supply an instance of
 * <code>PrivilegedAccessor.Instance</code> that contains both the
 * expression type and value.  */
public class PrivilegedAccessor {
//   static Logger log =
//     Logger.getLoggerWithInitialLevel("PrivAcc", Logger.LEVEL_DEBUG);

  // no instances
  private PrivilegedAccessor() {
  }

  /**
   * Gets the value of the named field and returns it as an object.
   * @param instance the object instance
   * @param fieldName the name of the field
   * @return an object representing the value of the field
   */
  public static Object getValue(Object instance, String fieldName)
      throws IllegalAccessException, NoSuchFieldException {
    if (instance == null) {
      throw new NullPointerException();
    }
    Field field = getField(classOf(instance), fieldName);
    Object val = null;
    if (field.isAccessible()) {
      val = field.get(instance);
    } else {
      try {
	field.setAccessible(true);
	val = field.get(instance);
      } finally {
	field.setAccessible(false);
      }
    }
    return val;
  }

  /**
   * Calls a method on the given object instance, with no arguments.
   * @param instance the object instance
   * @param methodName the name of the method to invoke
   */
  public static Object invokeMethod(Object instance, String methodName)
      throws NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     AmbiguousMethodException {
    Object[] args = {};
    return invokeMethod(instance, methodName, args);
  }

  /**
   * Calls a method on the given object instance with the given argument.
   * @param instance the object instance
   * @param methodName the name of the method to invoke
   * @param arg the argument to pass to the method
   */
  public static Object invokeMethod(Object instance, String methodName,
				    Object arg)
      throws NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     AmbiguousMethodException {
    Object[] args = new Object[1];
    args[0] = arg;
    return invokeMethod(instance, methodName, args);
  }

  /**
   * Calls a method on the given object instance with the given arguments.
   * @param instance the object instance
   * @param methodName the name of the method to invoke
   * @param args an array of objects to pass as arguments
   */
  public static Object invokeMethod(Object instance, String methodName,
				    Object[] args)
      throws NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     AmbiguousMethodException {
    if (args == null) {
      args = new Object[0];
    }
    Method method = findMethod(instance, methodName, args);
    Object val = null;
    if (method.isAccessible()) {
      val = method.invoke(instance, getRealArgs(args));
    } else {
      try {
	method.setAccessible(true);
	val = method.invoke(instance, getRealArgs(args));
      } finally {
	method.setAccessible(false);
      }
    }
    return val;
  }

  /**
   * Invokes the no-argument constructor for the named class
   * @param className the class name
   */
  public static Object invokeConstructor(String className)
      throws ClassNotFoundException,
	     NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     InstantiationException {
    return invokeConstructor(Class.forName(className));
  }

  /**
   * Invokes a one-argument constructor for the named class
   * @param className the class name
   * @param arg the argument to pass to the constructor
   */
  public static Object invokeConstructor(String className, Object arg)
      throws ClassNotFoundException,
	     NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     InstantiationException {
    return invokeConstructor(Class.forName(className), arg);
  }

  /**
   * Invokes a constructor for the named class
   * @param className the class name
   * @param args an array of objects to pass as arguments to the constructor
   */
  public static Object invokeConstructor(String className, Object[] args)
      throws ClassNotFoundException,
	     NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     InstantiationException {
    return invokeConstructor(Class.forName(className), args);
  }

  /**
   * Invokes the no-arg constructor for the specified class
   * @param cls the class
   */
  public static Object invokeConstructor(Class cls)
      throws NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     InstantiationException {
    Object[] args = {};
    return invokeConstructor(cls, args);
  }

  /**
   * Invokes a one-argument constructor for the specified class
   * @param cls the class
   * @param arg the argument to pass to the constructor
   */
  public static Object invokeConstructor(Class cls, Object arg)
      throws NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     InstantiationException {
    Object[] args = new Object[1];
    args[0] = arg;
    return invokeConstructor(cls, args);
  }

  /**
   * Invokes a constructor for the specified class
   * @param cls the class
   * @param args an array of objects to pass as arguments to the constructor
   */
  public static Object invokeConstructor(Class cls, Object[] args)
      throws NoSuchMethodException,
	     IllegalAccessException,
	     InvocationTargetException,
	     InstantiationException {
    if (args == null) {
      args = new Object[0];
    }
    Constructor constructor = findConstructor(cls, args);
    Object val = null;
    if (constructor.isAccessible()) {
      val = constructor.newInstance(getRealArgs(args));
    } else {
      try {
	constructor.setAccessible(true);
	val = constructor.newInstance(getRealArgs(args));
      } finally {
	constructor.setAccessible(false);
      }
    }
    return val;
  }

  // Return the named field from the given class.
  private static Field getField(Class thisClass, String fieldName)
      throws NoSuchFieldException {
    if (thisClass == null) {
      throw new NoSuchFieldException("Invalid field: " + fieldName);
    }
    try {
      return thisClass.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      return getField(thisClass.getSuperclass(), fieldName);
    }
  }

  // Find the most specific named method applicable to the object
  // and args.  Throw NoSuchMethodException if none applicable, throw
  // AmbiguousMethodException if more than one applicable and none is
  // most specific.  See 15.11.2.2 of Java Language Spec.
  private static Method findMethod(Object o, String methodName,
				   Object args[])
      throws NoSuchMethodException, AmbiguousMethodException {
//      log.debug("findMethod(" + StringUtil.shortName(o) + ", " + methodName +
//  	      ", [" + StringUtil.separatedString(args, ",") + "]");
    List l = new LinkedList();
    for (Class cls = classOf(o); cls != null; cls = cls.getSuperclass()) {
      // For each (super)class, find all methods with given name and
      // parameter types matching args
      Method clsMeths[] = cls.getDeclaredMethods();
      for (int ix = 0; ix < clsMeths.length; ix++) {
	Method m = clsMeths[ix];
	if (m.getName().equals(methodName) &&
	    isMethodApplicable(clsMeths[ix], args)) {
	  l.add(m);
	}
      }
    }
    if (l.isEmpty()) {
      throw new NoSuchMethodException(o.toString() + "." + methodName);
    }
    if (l.size() == 1) {
      return (Method)l.get(0);
    }
    // More than one.  Find the maximally specific methods.
    // There's probably a more efficient (better than O(n**2)) way to do this.
    List maximally = new LinkedList();
    for (Iterator iter1 = l.iterator(); iter1.hasNext(); ) {
      Method m1 = (Method)iter1.next();
    notmax:
      {
	for (Iterator iter2 = l.iterator(); iter2.hasNext(); ) {
	  Method m2 = (Method)iter2.next();
	  if (! isSubsumedBy(m1, m2)) {
	    break notmax;
	  }
	}
	maximally.add(m1);
      }
    }
    if (maximally.size() != 1) {
      throw new AmbiguousMethodException(methodName);
    }
    return (Method)maximally.get(0);
  }

  private static boolean isSubsumedBy(Method ee, Method er) {
    if (! (er.getDeclaringClass().
	   isAssignableFrom(ee.getDeclaringClass()))) {
      return false;
    }
    Class per[] = er.getParameterTypes();
    Class pee[] = ee.getParameterTypes();
    if (per.length != pee.length) {
      return false;
    }
    for (int ix = 0; ix < per.length; ix++) {
      if (!per[ix].isAssignableFrom(pee[ix]))
	return false;
    }
    return true;
  }

  private static boolean isMethodApplicable(Method method, Object args[]) {
    Class params[] = method.getParameterTypes();
    if (params.length != args.length) {
      return false;
    }
    for (int ix = 0; ix < params.length; ix++) {
      if (params[ix].isPrimitive()) {
	// can't handle params with primitive type, so always exclude them
	return false;
      }
      Class argType;
      if (args[ix] != null) {		// null args satisfy any param type
	argType = classOf(args[ix]);
	if (!params[ix].isAssignableFrom(argType))
	  return false;
      }
    }
    return true;
  }

  // Find the constructor applicable to the args.
  // Throw NoSuchMethodException if none applicable.
  // Throw AmbiguousMethodException if more than one applicable and none is
  // most specific.  (Doesn't match behavior of Java 1.3, which makes
  // compile-time decision of which constructor to call, and calls it even
  // if target class has been recompiled and now has ambiguous constructors.
  // Nevertheless, this seems the best behavior here.)
  private static Constructor findConstructor(Class cls, Object args[])
      throws NoSuchMethodException, AmbiguousMethodException {
    List l = new LinkedList();
    // find all constructors with parameter types matching args
    Constructor cons[] = cls.getDeclaredConstructors();
    for (int ix = 0; ix < cons.length; ix++) {
      Constructor c = cons[ix];
      if (isConstructorApplicable(cons[ix], args)) {
	l.add(c);
      }
    }
    if (l.isEmpty()) {
      throw new NoSuchMethodException(cls.getName() + " constructor");
    }
    if (l.size() == 1) {
      return (Constructor)l.get(0);
    }
    // More than one.  Find the maximally specific constructors.
    // There's probably a more efficient (better than O(n**2)) way to do this.
    List maximally = new LinkedList();
    for (Iterator iter1 = l.iterator(); iter1.hasNext(); ) {
      Constructor c1 = (Constructor)iter1.next();
    notmax:
      {
	for (Iterator iter2 = l.iterator(); iter2.hasNext(); ) {
	  Constructor c2 = (Constructor)iter2.next();
	  if (! isSubsumedBy(c1, c2)) {
	    break notmax;
	  }
	}
	maximally.add(c1);
      }
    }
    if (maximally.size() != 1) {
      throw new AmbiguousMethodException(cls.getName() + " constructor");
    }
    return (Constructor)maximally.get(0);
  }

  private static boolean isSubsumedBy(Constructor ee, Constructor er) {
    Class per[] = er.getParameterTypes();
    Class pee[] = ee.getParameterTypes();
    if (per.length != pee.length) {
      return false;
    }
    for (int ix = 0; ix < per.length; ix++) {
      if (!per[ix].isAssignableFrom(pee[ix]))
	return false;
    }
    return true;
  }

  private static boolean isConstructorApplicable(Constructor constructor,
						 Object args[]) {
    Class params[] = constructor.getParameterTypes();
    if (params.length != args.length) {
      return false;
    }
    for (int ix = 0; ix < params.length; ix++) {
      if (params[ix].isPrimitive()) {
	// can't handle params with primitive type, so always exclude them
	return false;
      }
      Class argType;
      if (args[ix] != null) {		// null args satisfy any param type
	argType = classOf(args[ix]);
	if (!params[ix].isAssignableFrom(argType))
	  return false;
      }
    }
    return true;
  }

  // Return an array of args, replacing any instances of Instance with
  // the real valus stored in it.
  private static Object[] getRealArgs(Object[] args) {
    Object retArgs[] = new Object[args.length];
    for (int ix = 0; ix < args.length; ix++) {
      if (args[ix] instanceof Instance) {
	retArgs[ix] = ((Instance)args[ix]).getValue();
      } else {
	retArgs[ix] = args[ix];
      }
    }
    return retArgs;
  }

  private static Class classOf(Object obj) {
    if (obj instanceof Instance) {
      return ((Instance)obj).getInstanceClass();
    } else {
      return obj.getClass();
    }
  }

  /** Exception thrown when an attempt is made to invoke an overloaded
   * constructor or method and there is no most specific applicable method.
   * (See section 15.11.2.2 of the Java Language Spec.)  Situations in
   * which this is thrown correspond to code that would cause a comple-time
   * error if attempted other than through reflection.  */
  public static class AmbiguousMethodException extends RuntimeException {
    public AmbiguousMethodException(String msg) {
      super(msg);
    }
  }

  /** <code>PrivilegedAccessor.Instance</code> is used when the difference
   * between an object's class and the type of an expression is important,
   * <i>eg</i>, to simulate compile-type decisions about on what class to
   * reference a static field, or which of several overloaded methods to
   * invoke.  (Null argumants are an instances of the latter.  This wrapper
   * is needed for null values only when the method is overloaded.)
   */
  public static class Instance {
    private Object value;
    private Class cls;

    private Instance() {
    }

    /**
     * Create an object that, when passed to the accessors in
     * <code>PrivilegedAccessor</code>, acts like an expression of type
     * <i>cls</i> for purposes of class and method lookup, but whose value
     * is actually <i>value</i>.  <i>value</i> must be either null or of a
     * type assignable to <i>class</i>.
     */
    public Instance(Class cls, Object value) {
      if (value != null && !cls.isInstance(value)) {
	throw new
	  ClassCastException("Instance value must be assignable to class");
      }
      this.cls = cls;
      this.value = value;
    }

    Class getInstanceClass() {
      return cls;
    }

    Object getValue() {
      return value;
    }

    public String toString() {
      return "[Priv.Inst: " + StringUtil.shortName(cls) + ", " + value + "]";
    }
  }
}
