/*
 * $Id: XStreamSerializer.java,v 1.21 2006-08-29 17:55:29 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.*;

import org.apache.commons.lang.ObjectUtils;
import org.lockss.app.LockssApp;

import com.thoughtworks.xstream.*;
import com.thoughtworks.xstream.alias.*;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.converters.basic.ThreadSafeSimpleDateFormat;
import com.thoughtworks.xstream.core.*;
import com.thoughtworks.xstream.io.*;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.*;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

/**
 * <p>An implementation of {@link ObjectSerializer} based on
 * {@link <a href="http://xstream.codehaus.org/">XStream</a>}.</p>
 * <p>This implementation of {@link ObjectSerializer} is intended to
 * become the preferred way of serializing objects to XML in the
 * LOCKSS codebase because it is much simpler and much lighter than
 * its legacy counterpart, which was based on Castor.</p>
 * <p>It is generally <em>safe</em> to use the same
 * {@link XStreamSerializer} instance for multiple unrelated
 * marshalling and unmarshalling operations.</p>
 * <p>This class supports post-deserialization processing. To take
 * advantage of it, you can either use the traditional Java post-
 * deserialization convention with {@link Serializable}, or you can
 * implement {@link LockssSerializable}. In that case, your class has
 * to define a method named <code>postUnmarshal</code> that accepts
 * one parameter of type {@link LockssApp} and returns
 * <code>void</code>. Although the underlying implementation does not
 * enforce it, it is <em>strongly recommended</em> that the post-
 * deserialization method be <code>protected</code> so that it can
 * call on post-deserialization methods in a superclass.</p>
 * <p>This class supports post-deserialization object resolution. To
 * take advantage of it, you can either use the traditional Java
 * post-deserialization resolution convention with
 * {@link Serializable}, or you can implement
 * {@link LockssSerializable}. In that case, your class has to define
 * a method named <code>postUnmarshalResolve</code> that accepts one
 * parameter of type {@link LockssApp} and returns {@link Object}.
 * Although the underlying implementation does not enforce it, it is
 * <em>strongly recommended</em> that the post-deserialization
 * resolution method be <code>protected</code> so that it can call
 * on post-deserialization resolution methods in a superclass.</p>
 * <p>The {@link #setReferenceMode} method enables clients to specify
 * how instances of this class represent back-references to already-
 * dumped objects in serialized state. If the mode is
 * {@link XStream#ID_REFERENCES}, XML IDs will be used. If the mode
 * is {@link XStream#XPATH_ABSOLUTE_REFERENCES}, absolute XPath paths
 * will be used. If {@link XStream#XPATH_RELATIVE_REFERENCES},
 * relative XPath paths will be used. The default for this class used
 * to be {@link XStream#XPATH_REFERENCES}, which is now deprecated
 * (its meaning is that of {@link XStream#XPATH_RELATIVE_REFERENCES}).
 * The current default for this class is
 * {@link XStream#ID_REFERENCES}.</p>
 * @author Thib Guicherd-Callin
 */
public class XStreamSerializer extends ObjectSerializer {

  /*
   * IMPLEMENTATION NOTES
   *
   * This class is essentially just a wrapper around the XStream
   * class. Because XStream can marshal/unmarshal nearly arbitrary
   * objects (at least as far as the complexity of the LOCKSS codebase
   * goes), its interface was used as a model for ObjectSerializer,
   * hence the vague uselessness of this class. (ObjectSerializer is
   * necessary to refactor Castor-specific code into Castor-specific
   * implementations of serialization. Before ObjectSerializer existed
   * there was Castor-aware code in several data structures that just
   * had no business knowing about XML mappings.)
   */

  /**
   * <p>A replacement for XStream's
   * {@link com.thoughtworks.xstream.converters.basic.DateConverter}
   * which uses 'z' for the time zone instead of 'Z'.</p>
   * <p>The problematic discrepancies between 'z' and 'Z' were the
   * subject of Java Bug Report #709654.</p>
   * @author Thib Guicherd-Callin
   * @see com.thoughtworks.xstream.converters.basic.DateConverter
   * @see com.thoughtworks.xstream.converters.basic.ThreadSafeSimpleDateFormat
   */
  protected static class LockssDateConverter implements Converter {

    /* Inherit documentation */
    public boolean canConvert(Class type) {
      return type.equals(Date.class);
    }

    /* Inherit documentation */
    public void marshal(Object obj,
                        HierarchicalStreamWriter writer,
                        MarshallingContext context) {
      writer.setValue(formatter.format((Date)obj));
    }

    /* Inherit documentation */
    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
      String value = reader.getValue();
      try {
        return formatter.parse(value);
      }
      catch (ParseException pe1) {
        try {
          // Maybe an old (daemon 1.16 and earlier) serialized form
          return oldFormatter.parse(value);
        }
        catch (ParseException pe2) {
          // Cannot parse date
          throw new ConversionException("Cannot parse date: " + value);
        }
      }
    }

    /**
     * <p>A thread-safe date format.</p>
     */
    protected static final ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss.S Z",
                                                                                                 4,
                                                                                                 20);

    /**
     * <p>A thread-safe date format for the old default once accepted
     * by this class.</p>
     */
    protected static final ThreadSafeSimpleDateFormat oldFormatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z",
                                                                                                    4,
                                                                                                    20);

  }

  /**
   * <p>A marshalling strategy for XStream instances, that provides
   * facilities for implementing post-deserialization processing and
   * post-deserialization object resolution via the marker interface
   * {@link LockssSerializable}.</p>
   * <p>For this to happen, <em>the
   * {@link TreeMarshaller#convertAnother(Object)} method of
   * {@link AbstractReferenceMarshaller} instances returned by
   * {@link #getMarshaller} should call
   * {@link #throwUnlessSerializable}, and the
   * {@link TreeUnmarshaller#convertAnother(Object, Class)} method of
   * {@link AbstractReferenceUnmarshaller} instances returned by
   * {@link #getUnmarshaller} should call
   * {@link #doUnmarshal(Object)}.</em></p>
   * @author Thib Guicherd-Callin
   * @see #getMarshaller
   * @see #getUnmarshaller
   */
  protected static abstract class LockssMarshallingStrategy implements MarshallingStrategy {

    /**
     * <p>A context object for deserialization.</p>
     */
    protected LockssApp lockssContext;

    /**
     * <p>Builds a new marshalling strategy with the given context
     * object.</p>
     * @param lockssContext A context object for deserialization.
     */
    protected LockssMarshallingStrategy(LockssApp lockssContext) {
      this.lockssContext = lockssContext;
    }

    /* Inherit documentation */
    public void marshal(HierarchicalStreamWriter writer,
                        Object obj,
                        ConverterLookup converterLookup,
                        Mapper mapper,
                        DataHolder dataHolder) {
      getMarshaller(writer,
                    converterLookup,
                    mapper,
                    obj.getClass().getName()).start(obj,
                                                    dataHolder);
    }

    /**
     * @deprecated Use {@link #marshal(HierarchicalStreamWriter, Object, ConverterLookup, Mapper, DataHolder)}
     */
    public void marshal(HierarchicalStreamWriter writer,
                        Object obj,
                        DefaultConverterLookup converterLookup,
                        ClassMapper classMapper,
                        DataHolder dataHolder) {
      throw new UnsupportedOperationException("Deprecated; use marshal(HierarchicalStreamWriter, Object, ConverterLookup, Mapper, DataHolder)");
    }

    /* Inherit documentation */
    public Object unmarshal(Object root,
                            HierarchicalStreamReader reader,
                            DataHolder dataHolder,
                            ConverterLookup converterLookup,
                            Mapper mapper) {
      return getUnmarshaller(root,
                             reader,
                             converterLookup,
                             mapper).start(dataHolder);
    }

    /**
     * @deprecated Use {@link #unmarshal(Object, HierarchicalStreamReader, DataHolder, ConverterLookup, Mapper)}
     */
    public Object unmarshal(Object arg0, HierarchicalStreamReader arg1, DataHolder arg2, DefaultConverterLookup arg3, ClassMapper arg4) {
      throw new UnsupportedOperationException("Deprecated; use unmarshal(Object, HierarchicalStreamReader, DataHolder, ConverterLookup, Mapper)");
    }

    /**
     * <p>Accepts a raw deserialized object; performs
     * post-deserialization on it if needed; performs
     * post-deserialization object resolution if needed;
     * and returns a result for the deserialization.</p>
     * <p>This method should be called by the
     * {@link TreeUnmarshaller#convertAnother(Object, Class)} method of
     * {@link AbstractReferenceUnmarshaller} instances returned by
     * {@link #getUnmarshaller}.</p>
     * @param rawDeserializedObject A freshly deserialized object.
     * @return A fully deserialized object, possibly post-processed
     *         and possibly a surrogate for the original.
     * @see #getUnmarshaller
     */
    protected Object doUnmarshal(Object rawDeserializedObject) {
      Object ret = rawDeserializedObject;
      if (ret instanceof LockssSerializable) {
        Object[] parameters = new Object[] { lockssContext };
        invokeMethod(ret,
                     POST_UNMARSHAL_METHOD,
                     POST_UNMARSHAL_PARAMETERS,
                     postUnmarshalCache,
                     parameters);
        Object surrogate = invokeMethod(ret,
                                        POST_UNMARSHAL_RESOLVE_METHOD,
                                        POST_UNMARSHAL_RESOLVE_PARAMETERS,
                                        postUnmarshalResolveCache,
                                        parameters);
        if (surrogate != null) {
          ret = surrogate;
        }
      }
      return ret;
    }

    /**
     * <p>Gets a reference marshaller for the given root class name
     * using the given serialization environment.</p>
     * <p>The {@link TreeMarshaller#convertAnother(Object)} method of
     * {@link AbstractReferenceMarshaller} instances returned by
     * this method should call {@link #throwUnlessSerializable}.</p>
     * @param writer          See {@link AbstractReferenceMarshaller#AbstractReferenceMarshaller(HierarchicalStreamWriter, ConverterLookup, Mapper)}.
     * @param converterLookup See {@link AbstractReferenceMarshaller#AbstractReferenceMarshaller(HierarchicalStreamWriter, ConverterLookup, Mapper)}.
     * @param mapper          See {@link AbstractReferenceMarshaller#AbstractReferenceMarshaller(HierarchicalStreamWriter, ConverterLookup, Mapper)}.
     * @param rootClassName   The name of the class of the root of the
     *                        object graph being serialized, as
     *                        returned by {@link Class#getName()}.
     * @return An instance of {@link AbstractReferenceMarshaller}.
     * @see #throwUnlessSerializable
     */
    protected abstract AbstractReferenceMarshaller getMarshaller(HierarchicalStreamWriter writer,
                                                                 ConverterLookup converterLookup,
                                                                 Mapper mapper,
                                                                 String rootClassName);

    /**
     * <p>Gets a reference unmarshaller for the given root object
     * using the given deserialization environment.</p>
     * <p>The {@link TreeUnmarshaller#convertAnother(Object, Class)} method of
     * {@link AbstractReferenceUnmarshaller} instances returned by
     * this method should call {@link #doUnmarshal(Object)}.</p>
     * @param root            See {@link AbstractReferenceUnmarshaller#AbstractReferenceUnmarshaller(Object, HierarchicalStreamReader, ConverterLookup, Mapper)}.
     * @param reader          See {@link AbstractReferenceUnmarshaller#AbstractReferenceUnmarshaller(Object, HierarchicalStreamReader, ConverterLookup, Mapper)}.
     * @param converterLookup See {@link AbstractReferenceUnmarshaller#AbstractReferenceUnmarshaller(Object, HierarchicalStreamReader, ConverterLookup, Mapper)}.
     * @param mapper          See {@link AbstractReferenceUnmarshaller#AbstractReferenceUnmarshaller(Object, HierarchicalStreamReader, ConverterLookup, Mapper)}.
     * @return An instance of {@link AbstractReferenceUnmarshaller}.
     * @see #doUnmarshal
     */
    protected abstract AbstractReferenceUnmarshaller getUnmarshaller(Object root,
                                                                     HierarchicalStreamReader reader,
                                                                     ConverterLookup converterLookup,
                                                                     Mapper mapper);

    /**
     * <p>The String name of the method automagically called during
     * post-deserialization of {@link LockssSerializable} objects
     * to post-process deserialized objects.</p>
     * @see #POST_UNMARSHAL_PARAMETERS
     */
    protected static final String POST_UNMARSHAL_METHOD = "postUnmarshal";

    /**
     * <p>The list of parameter types of the method
     * {@link #POST_UNMARSHAL_METHOD}.</p>
     * @see #POST_UNMARSHAL_METHOD
     */
    protected static final Class[] POST_UNMARSHAL_PARAMETERS = new Class[] { LockssApp.class };


    /**
     * <p>The String name of the method automagically called during
     * post-deserialization of {@link LockssSerializable} objects
     * to perform object resolution.</p>
     * @see #POST_UNMARSHAL_RESOLVE_PARAMETERS
     */
    protected static final String POST_UNMARSHAL_RESOLVE_METHOD = "postUnmarshalResolve";

    /**
     * <p>The list of parameter types of the method
     * {@link #POST_UNMARSHAL_RESOLVE_METHOD}.</p>
     * @see #POST_UNMARSHAL_RESOLVE_METHOD
     */
    protected static final Class[] POST_UNMARSHAL_RESOLVE_PARAMETERS = new Class[] { LockssApp.class };

    /**
     * <p>A map to cache post-deserialization {@link Method}s by
     * class.</p>
     */
    protected static final HashMap postUnmarshalCache = new HashMap();

    /**
     * <p>A map to cache post-deserialization resolution
     * {@link Method}s by class.</p>
     */
    protected static final HashMap postUnmarshalResolveCache = new HashMap();

    /**
     * <p>Retrieves a method up an object's class hierarchy and caches
     * the result for later retrieval.</p>
     * <p>If the object does not have a matching method in its
     * class hierarchy, the method cache will map the object's
     * {@link Class} (as determined by {@link Object#getClass()})
     * to {@link ObjectUtils#NULL} (and this method will return null).
     * Otherwise, it will map it to the corresponding {@link Method}
     * instance (and this method will return it).</p>
     * @param obj              An object.
     * @param methodName       The name of the method being sought.
     * @param methodParameters The parameter types for the method.
     * @param methodCache      A map where the method will be cached.
     * @return A {@link Method} instance for the method being sought
     *         if found, null otherwise.
     */
    protected static Method cacheMethod(Object obj,
                                        String methodName,
                                        Class[] methodParameters,
                                        HashMap methodCache) {
      Class objClass = obj.getClass();
      Method objMethod = null;

      // Look up inheritance hierarchy
      while (objClass != Object.class) {
        try {
          objMethod = objClass.getDeclaredMethod(methodName, methodParameters);
          objClass = Object.class; // executed only if call succeeds
        }
        catch (NoSuchMethodException nsmE) {
          objClass = objClass.getSuperclass();
        }
      }

      // Cache result
      if (objMethod == null) {
        methodCache.put(obj.getClass(), ObjectUtils.NULL);
      }
      else {
        objMethod.setAccessible(true); // monstrous, monstrous
        methodCache.put(obj.getClass(), objMethod);
      }
      return objMethod;
    }

    /**
     * <p>An exception message formatter used when an exception is
     * thrown by the post-deserialization mechanism.</p>
     * @param exc The exception thrown by the underlying code.
     * @return A new ConversionException with <code>e</code> nested.
     */
    protected static ConversionException failDeserialize(Exception exc) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("An exception of type ");
      buffer.append(exc.getClass().getName());
      buffer.append(" was thrown by an object while it was being deserialized.");
      return new ConversionException(buffer.toString(), exc);
    }

    /**
     * <p>Invokes a method on an object with the given arguments, if
     * the object has such a method in its class hierarchy (caching
     * the lookup in the provided method cache), and returns the
     * result of invoking the method.</p>
     * <p>If the object does not have a matching method in its
     * class hierarchy, the method cache will map the object's
     * {@link Class} (as determined by {@link Object#getClass()})
     * to {@link ObjectUtils#NULL} (and this method will return null).
     * Otherwise, it will map it to the corresponding {@link Method}
     * instance (and this method will return the result of invoking it
     * with the given parameters, which <em>may</em> also return
     * null).</p>
     * @param obj              An object.
     * @param methodName       The name of the method to be invoked.
     * @param methodParameters The parameters for the method sought.
     * @param methodCache      A map where the lookup is to be cached.
     * @param methodArguments  The actual arguments to the method
     *                         being invoked.
     * @return The result of invoking the method with the given
     *         arguments if it exists (which <em>may</em> be null);
     *         null otherwise.
     */
    protected static Object invokeMethod(Object obj,
                                         String methodName,
                                         Class[] methodParameters,
                                         HashMap methodCache,
                                         Object[] methodArguments) {
      Method met = lookupMethod(obj,
                                methodName,
                                methodParameters,
                                methodCache);
      Object ret = null;
      if (met != null) {
        try {
          ret = met.invoke(obj, methodArguments);
        }
        catch (Exception exc) {
          throw failDeserialize(exc);
        }
      }
      return ret;
    }

    /**
     * <p>Retrieves a {@link Method} associated with an object's
     * class from the given method cache, or searches for that method
     * in the object's class hierarchy and caches the lookup if there
     * is no entry in the method cache.</p>
     * <p>If the object does not have a matching method in its
     * class hierarchy, the method cache will map the object's
     * {@link Class} (as determined by {@link Object#getClass()})
     * to {@link ObjectUtils#NULL} (and this method will return null).
     * Otherwise, it will map it to the corresponding {@link Method}
     * instance (and this method will return it).</p>
     * @param obj              An object.
     * @param methodName       The name of the method being sought.
     * @param methodParameters The parameters for the method.
     * @param methodCache      A method cache to use.
     * @return A {@link Method} instance for the corresponding method,
     *         or null if there is no such method.
     */
    protected static Method lookupMethod(Object obj,
                                         String methodName,
                                         Class[] methodParameters,
                                         HashMap methodCache) {
      Object objMethod = methodCache.get(obj.getClass());
      if (objMethod == null) {
        return cacheMethod(obj,
                           methodName,
                           methodParameters,
                           methodCache);
      }
      else if (objMethod == ObjectUtils.NULL) {
        return null;
      }
      else {
        return (Method)objMethod;
      }
    }

    /**
     * <p>Throws a {@link LockssNotSerializableException} if the given
     * object is not either {@link Serializable} or
     * {@link LockssSerializable}.</p>
     * <p>This method should be called by the
     * {@link TreeMarshaller#convertAnother(Object)} method of
     * {@link AbstractReferenceMarshaller} instances returned by
     * {@link #getMarshaller}.</p>
     * @param obj           An object.
     * @param rootClassName The class name of the root of the object
     *                      graph <code>obj</code> comes from.
     * @see #getMarshaller
     */
    protected static void throwUnlessSerializable(Object obj,
                                                  String rootClassName) {
      if ( !( obj instanceof Serializable ||
              obj instanceof LockssSerializable) ) {
        LockssNotSerializableException exc = new LockssNotSerializableException(rootClassName,
                                                                                obj.getClass().getName());
        logger.debug2("Not Serializable or LockssSerializable", exc);
        throw exc;
      }
    }

  }

  /**
   * <p>A runtime exception used internally to
   * {@link XStreamSerializer} only.</p>
   * @author Thib Guicherd-Callin
   */
  protected static class LockssNotSerializableException extends RuntimeException {

    /**
     * <p>Builds a new exception.</p>
     * @param rootClassName    Name of the class of the root of the
     *                         object graph that caused the exception.
     * @param currentClassName Name of the class that actually caused
     *                         the exception.
     */
    public LockssNotSerializableException(String rootClassName,
                                          String currentClassName) {
      super(makeMessage(rootClassName, currentClassName));
    }

    /**
     * <p>Formats the error message.</p>
     * @param rootClassName    Name of the class of the root of the
     *                         object graph that caused the exception.
     * @param currentClassName Name of the class that actually caused
     *                         the exception.
     * @return A formatted error string.
     */
    private static String makeMessage(String rootClassName,
                                      String currentClassName) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Could not serialize an object of type ");
      buffer.append(currentClassName);
      buffer.append(" while serializing a root object of type ");
      buffer.append(rootClassName);
      return buffer.toString();
    }

  }

  /**
   * <p>A version of {@link LockssMarshallingStrategy} that uses XML
   * IDs for references.</p>
   * @author Thib Guicherd-Callin
   * @see XStream#ID_REFERENCES
   * @see XStreamSerializer#setReferenceMode
   */
  protected static class LockssReferenceByIdMarshallingStrategy extends LockssMarshallingStrategy {

    protected class LockssReferenceByIdMarshaller extends ReferenceByIdMarshaller {

      protected String rootClassName;

      public LockssReferenceByIdMarshaller(HierarchicalStreamWriter writer,
                                           ConverterLookup converterLookup,
                                           Mapper mapper,
                                           String rootClassName) {
        super(writer, converterLookup, mapper);
        this.rootClassName = rootClassName;
      }

      /* Inherit documentation */
      public void convertAnother(Object parent) {
        throwUnlessSerializable(parent, rootClassName);
        super.convertAnother(parent);
      }

    }

    protected class LockssReferenceByIdUnmarshaller extends ReferenceByIdUnmarshaller {

      public LockssReferenceByIdUnmarshaller(Object root,
                                             HierarchicalStreamReader reader,
                                             ConverterLookup converterLookup,
                                             Mapper mapper) {
        super(root, reader, converterLookup, mapper);
      }

      /* Inherit documentation */
      public Object convertAnother(Object parent, Class type) {
        return doUnmarshal(super.convertAnother(parent, type));
      }

    }

    public LockssReferenceByIdMarshallingStrategy(LockssApp lockssContext) {
      super(lockssContext);
    }

    /* Inherit documentation */
    protected AbstractReferenceMarshaller getMarshaller(HierarchicalStreamWriter writer,
                                                        ConverterLookup converterLookup,
                                                        Mapper mapper,
                                                        String rootClassName) {
      return new LockssReferenceByIdMarshaller(writer,
                                               converterLookup,
                                               mapper,
                                               rootClassName);
    }

    protected AbstractReferenceUnmarshaller getUnmarshaller(Object root,
                                                            HierarchicalStreamReader reader,
                                                            ConverterLookup converterLookup,
                                                            Mapper mapper) {
      return new LockssReferenceByIdUnmarshaller(root,
                                                 reader,
                                                 converterLookup,
                                                 mapper);
    }

  }

  /**
   * <p>A version of {@link LockssMarshallingStrategy} that uses XPath
   * paths for references.</p>
   * @author Thib Guicherd-Callin
   * @see XStream#XPATH_ABSOLUTE_REFERENCES
   * @see XStream#XPATH_RELATIVE_REFERENCES
   * @see XStreamSerializer#setReferenceMode
   */
  protected static class LockssReferenceByXPathMarshallingStrategy extends LockssMarshallingStrategy {

    protected class LockssReferenceByXPathMarshaller extends ReferenceByXPathMarshaller {

      protected String rootClassName;

      public LockssReferenceByXPathMarshaller(HierarchicalStreamWriter writer,
                                              ConverterLookup converterLookup,
                                              Mapper mapper,
                                              String rootClassName) {
        super(writer, converterLookup, mapper, mode);
        this.rootClassName = rootClassName;
      }

      /* Inherit documentation */
      public void convertAnother(Object parent) {
        throwUnlessSerializable(parent, rootClassName);
        super.convertAnother(parent);
      }

    }

    protected class LockssReferenceByXPathUnmarshaller extends ReferenceByXPathUnmarshaller {

      public LockssReferenceByXPathUnmarshaller(Object root,
                                                HierarchicalStreamReader reader,
                                                ConverterLookup converterLookup,
                                                Mapper mapper) {
        super(root, reader, converterLookup, mapper);
      }

      /* Inherit documentation */
      public Object convertAnother(Object parent, Class type) {
        return doUnmarshal(super.convertAnother(parent, type));
      }

    }

    protected int mode;

    public LockssReferenceByXPathMarshallingStrategy(LockssApp lockssContext) {
      this(lockssContext, ReferenceByXPathMarshallingStrategy.RELATIVE);
    }

    public LockssReferenceByXPathMarshallingStrategy(LockssApp lockssContext,
                                                     int mode) {
      super(lockssContext);
      this.mode = mode;
    }

    /* Inherit documentation */
    protected AbstractReferenceMarshaller getMarshaller(HierarchicalStreamWriter writer,
                                                        ConverterLookup converterLookup,
                                                        Mapper mapper,
                                                        String rootClassName) {
      return new LockssReferenceByXPathMarshaller(writer,
                                                  converterLookup,
                                                  mapper,
                                                  rootClassName);
    }

    /* Inherit documentation */
    protected AbstractReferenceUnmarshaller getUnmarshaller(Object root,
                                                            HierarchicalStreamReader reader,
                                                            ConverterLookup converterLookup,
                                                            Mapper mapper) {
      return new LockssReferenceByXPathUnmarshaller(root,
                                                    reader,
                                                    converterLookup,
                                                    mapper);
    }

  }

  /**
   * <p>A deserialization context object.</p>
   */
  protected LockssApp lockssContext;

  /**
   * <p>An instance of the {@link com.thoughtworks.xstream.XStream}
   * facade class.</p>
   */
  private XStream xs;

  /**
   * <p>Builds a new XStreamSerializer instance with a null context.</p>
   * <p>It is safe to use the same XStreamSerializer instance for
   * multiple unrelated marshalling and unmarshalling operations.</p>
   * @see #XStreamSerializer(LockssApp)
   */
  public XStreamSerializer() {
    this(null);
  }

  /**
   * <p>Builds a new XStreamSerializer instance with a null
   * context, and with the given failed serialization and
   * deserialization modes.</p>
   * <p>It is safe to use the same XStreamSerializer instance for
   * multiple unrelated marshalling and unmarshalling operations.</p>
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see ObjectSerializer#ObjectSerializer(LockssApp, boolean, int)
   */
  public XStreamSerializer(boolean saveTempFiles,
                           int failedDeserializationMode) {
    this(null,
         saveTempFiles,
         failedDeserializationMode);
  }

  /**
   * <p>Builds a new XStreamSerializer instance with the given
   * context, with default failed serialization and deserialization
   * modes.</p>
   * <p>It is safe to use the same XStreamSerializer instance for
   * multiple unrelated marshalling and unmarshalling operations.</p>
   * @param lockssContext A serialization context object.
   * @see ObjectSerializer#ObjectSerializer(LockssApp)
   */
  public XStreamSerializer(LockssApp lockssContext) {
    super(lockssContext);
    this.lockssContext = lockssContext;
    init();
  }

  /**
   * <p>Builds a new XStreamSerializer instance with the given
   * context, failed serialization mode and failed deserialization
   * mode.</p>
   * <p>It is safe to use the same XStreamSerializer instance for
   * multiple unrelated marshalling and unmarshalling operations.</p>
   * @param lockssContext             A serialization context object.
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see ObjectSerializer#ObjectSerializer(LockssApp, boolean, int)
   */
  public XStreamSerializer(LockssApp lockssContext,
                           boolean saveTempFiles,
                           int failedDeserializationMode) {
    super(lockssContext,
          saveTempFiles,
          failedDeserializationMode);
    this.lockssContext = lockssContext;
    init();
  }

  /* Inherit documentation */
  public Object deserialize(Reader reader)
      throws SerializationException,
             InterruptedIOException {
    try {
      return xs.fromXML(reader);
    }
    catch (CannotResolveClassException crce) {
      throw failDeserialize(crce);
    }
    catch (BaseException be) {
      throw failDeserialize(be);
    }
    catch (RuntimeException re) {
      throwIfInterrupted(re);
      throw re;
    }
    catch (InstantiationError ie) {
      throw failDeserialize(new Exception(ie));
    }
  }

  /* Inherit documentation */
  protected void serialize(Writer writer,
                           Object obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    String errorString = "Failed to serialize an object of type " + obj.getClass().getName();

    try {
      xs.toXML(obj, writer);
    }
    catch (XStreamSerializer.LockssNotSerializableException lnse) {
      throw failSerialize("Not Serializable or LockssSerializable",
                          lnse,
                          new SerializationException.NotSerializableOrLockssSerializable(lnse));

    }
    catch (StreamException se) {
      throw failSerialize(errorString,
                          se,
                          new SerializationException(se));
    }
    catch (CannotResolveClassException crce) {
      throw failSerialize(errorString,
                          crce,
                          new SerializationException(crce));
    }
    catch (BaseException be) {
      throw failSerialize(errorString,
                          be,
                          new SerializationException(be));
    }
    catch (RuntimeException re) {
      throwIfInterrupted(re);
      throw re;
    }
  }

  /**
   * <p>Sets the reference mode of this serializer, that is, the
   * mode the serializer will use to represent back references to
   * already-dumped objects in serialized state.</p>
   * <p>Note that {@link XStream#XPATH_REFERENCES} (previously the
   * default for this class) is deprecated; use
   * {@link XStream#XPATH_RELATIVE_REFERENCES} instead.</p>
   * @param referenceMode One of {@link XStream#ID_REFERENCES},
   *                      {@link XStream#XPATH_ABSOLUTE_REFERENCES} or
   *                      {@link XStream#XPATH_RELATIVE_REFERENCES}.
   * @throws IllegalArgumentException if the given reference mode is
   *                                  not one of the allowed modes.
   * @see XStream#ID_REFERENCES
   * @see XStream#XPATH_ABSOLUTE_REFERENCES
   * @see XStream#XPATH_RELATIVE_REFERENCES
   */
  protected void setReferenceMode(int referenceMode) {
    switch (referenceMode) {
      case XStream.ID_REFERENCES:
        xs.setMode(XStream.ID_REFERENCES);
        xs.setMarshallingStrategy(new LockssReferenceByIdMarshallingStrategy(lockssContext));
        break;
      case XStream.XPATH_ABSOLUTE_REFERENCES:
        xs.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        xs.setMarshallingStrategy(new LockssReferenceByXPathMarshallingStrategy(lockssContext,
                                                                                ReferenceByXPathMarshallingStrategy.ABSOLUTE));
        break;
      case XStream.XPATH_RELATIVE_REFERENCES:
        xs.setMode(XStream.XPATH_RELATIVE_REFERENCES);
        xs.setMarshallingStrategy(new LockssReferenceByXPathMarshallingStrategy(lockssContext,
                                                                                ReferenceByXPathMarshallingStrategy.RELATIVE));
        break;
      default:
        IllegalArgumentException iae = new IllegalArgumentException("Illegal reference mode: " + referenceMode);
        logger.warning("Attempt to set reference mode to " + referenceMode, iae);
        throw iae;
    }
  }

  /**
   * <p>Performs initialization tasks for the constructors.</p>
   * @see #XStreamSerializer(LockssApp)
   * @see #XStreamSerializer(LockssApp, boolean, int)
   */
  private void init() {
    xs = new XStream(new DomDriver());
    xs.registerConverter(new LockssDateConverter());
    setReferenceMode(XStream.ID_REFERENCES);
  }

}
