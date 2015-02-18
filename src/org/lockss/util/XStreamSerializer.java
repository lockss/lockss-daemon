/*
 * $Id$
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

package org.lockss.util;

import java.io.*;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.*;

import org.lockss.app.LockssApp;
import org.lockss.hasher.HashResult;
import org.lockss.util.SerializationException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.alias.*;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.converters.basic.*;
import com.thoughtworks.xstream.converters.reflection.*;
import com.thoughtworks.xstream.core.*;
import com.thoughtworks.xstream.io.*;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.*;

/**
 * <p>An implementation of {@link ObjectSerializer} based on
 * {@link <a href="http://xstream.codehaus.org/">XStream</a>}.</p>
 * <p>This implementation of {@link ObjectSerializer} is intended to
 * become the preferred way of serializing objects to XML in the
 * LOCKSS codebase because it is much simpler and much lighter than
 * its legacy counterpart, which was based on Castor.</p>
 * <p>It is generally <em>safe</em> to use the same XStreamSerializer
 * instance for multiple unrelated marshalling and unmarshalling
 * operations.</p>
 * <p>This class supports post-deserialization processing. To take
 * advantage of it, you can either use the traditional Java post-
 * deserialization convention with {@link Serializable}, or you can
 * implement {@link org.lockss.util.LockssSerializable}. In that case,
 * your class has to define a method named <code>postUnmarshal</code>
 * that accepts one parameter of type {@link LockssApp} and returns
 * <code>void</code>. Although the underlying implementation does not
 * enforce it, it is <em>strongly recommended</em> that the post-
 * deserialization method be <code>protected</code> so that it can
 * call on post-deserialization methods in a superclass.</p>
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

  /*
   * begin PRIVATE STATIC INNER CLASS
   * ================================
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
  private static class LockssDateConverter implements Converter {

    public boolean canConvert(Class type) {
      return Date.class.equals(type);
    }

    public void marshal(Object obj,
                        HierarchicalStreamWriter writer,
                        MarshallingContext context) {
      writer.setValue(formatter.format((Date)obj));
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
      String value = reader.getValue();
      try {
        return formatter.parse(value);
      }
      catch (ParseException pe) {
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

    protected static final ThreadSafeSimpleDateFormat formatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss.S Z",
                                                                                                 4,
                                                                                                 20);

    protected static final ThreadSafeSimpleDateFormat oldFormatter = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z",
                                                                                                    4,
                                                                                                    20);

  }
  /*
   * end PRIVATE STATIC INNER CLASS
   * ==============================
   */

  /*
   * begin PRIVATE STATIC INNER CLASS
   * ================================
   */
  /** HashResult converter - writes & reads "alg:hash" */
  private static class LockssHashResultConverter implements Converter {

    public boolean canConvert(Class type) {
      return HashResult.class.equals(type);
    }

    public void marshal(Object obj,
                        HierarchicalStreamWriter writer,
                        MarshallingContext context) {
      
      HashResult hr = (HashResult)obj;
      if (hr.getAlgorithm() == null) {
	throw new ConversionException("Can't serialize HashResult with no algorithm");
      }
      writer.setValue(((HashResult)obj).toString());
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
      String value = reader.getValue();
      try {
	return HashResult.make(value);
      } catch (Exception e) {
	throw new ConversionException("Cannot parse HashResult: " + value);
      }
    }
  }
  /*
   * end PRIVATE STATIC INNER CLASS
   * ==============================
   */

  /** These are the classes that should have their no-arg constructor run
   * before unmarshalling */
  private static final Class[] CLASSES_NEEDING_CONSTRUCTOR = new Class[] {
    org.lockss.state.AuState.class,
  };

  /** This converter causes the class's no-arg constructor to be called
   * prior to unmarshalling, so that default field values are stored for
   * fields that don't exist in the serialized file.  This makes it
   * possible to use marker values to signal the need to initialize new
   * fields. */
  private static class LockssConstructingConverter extends ReflectionConverter {
    
    public LockssConstructingConverter(Mapper mapper) {
      super(mapper, new PureJavaReflectionProvider());
    }
    
    @Override
    public boolean canConvert(Class type) {
      for (int ix = 0; ix < CLASSES_NEEDING_CONSTRUCTOR.length; ix++) {
	if (type == CLASSES_NEEDING_CONSTRUCTOR[ix]) {
	  return true;
	}
      }
      return false;
    }
  }

  /*
   * begin PRIVATE STATIC INNER CLASS
   * ================================
   */
  /**
   * <p>A runtime exception used internally to
   * {@link XStreamSerializer} only.</p>
   * @author Thib Guicherd-Callin
   */
  private static class LockssNotSerializableException extends RuntimeException {

    /*
     * IMPLEMENTATION NOTES
     *
     * With this class, it's possible to throw an exception that is
     * specifically recognizable as being internal to the custom
     * marshalling/unmarshalling strategies defined in this file,
     * without changing the signatures of the methods involved in the
     * XStream API.
     */

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
  /*
   * end PRIVATE STATIC INNER CLASS
   * ==============================
   */

  /*
   * begin PRIVATE STATIC INNER CLASS
   * ================================
   */
  /**
   * <p>A custom implementation of {@link ReferenceByXPathMarshaller}
   * that checks that serialized object graphs are
   * {@link Serializable}-or-{@link LockssSerializable}.</p>
   * @author Thib Guicherd-Callin
   */
  private static class LockssReferenceByXPathMarshaller
      extends ReferenceByXPathMarshaller {

    private String rootClassName;

    /**
     * <p>Builds a new marshaller.</p>
     * @param writer
     * @param converterLookup
     * @param classMapper
     */
    public LockssReferenceByXPathMarshaller(HierarchicalStreamWriter writer,
                                            DefaultConverterLookup converterLookup,
                                            ClassMapper classMapper,
                                            String rootClassName) {
      super(writer, converterLookup, classMapper);
      this.rootClassName = rootClassName;
    }

    /**
     * <p>A specialized version of
     * {@link ReferenceByXPathMarshaller#convertAnother} that throws
     * a {@link LockssNotSerializableException} if the argument is
     * not {@link Serializable} or {@link LockssSerializable} (and
     * that just invokes the super-implementation to do its work).</p>
     * @param parent {@inheritDoc}
     * @throws LockssNotSerializableException if obj is not
     *                                        {@link Serializable} or
     *                                        {@link LockssSerializable}.
     *  @see ReferenceByXPathMarshaller#convertAnother
     */
    public void convertAnother(Object parent) {
      if ( !( parent instanceof Serializable ||
              parent instanceof LockssSerializable) ) {
        LockssNotSerializableException exc =
          new LockssNotSerializableException(
              rootClassName,
              parent.getClass().getName());
        logger.debug2(exc.getMessage());
        throw exc;
      }
      super.convertAnother(parent);
    }

  }
  /*
   * end PRIVATE STATIC INNER CLASS
   * ==============================
   */

  /*
   * begin PRIVATE STATIC INNER CLASS
   * ================================
   */
  /**
   * <p>This class is used to customize the way XStream traverses an
   * object graph during serialization and deserialization.</p>
   * <p>At serialization time, it checks that object graphs are
   * {@link Serializable}-or-{@link LockssSerializable}. At
   * deserialization time, it invokes any custom post-deserialization
   * methods in the object graph.</p>
   * @author Thib Guicherd-Callin
   * @see ReferenceByXPathMarshallingStrategy
   * @see LockssReferenceByXPathMarshaller
   * @see LockssReferenceByXPathUnmarshaller
   */
  private static class LockssReferenceByXPathMarshallingStrategy
      extends ReferenceByXPathMarshallingStrategy {

    /**
     * <p>Reference to the context object.</p>
     */
    private LockssApp lockssContext;

    /**
     * <p>Builds a new instance.</p>
     * @param lockssContext The context object (needed by the
     *                      unmarshaller).
     */
    public LockssReferenceByXPathMarshallingStrategy(LockssApp lockssContext) {
      this.lockssContext = lockssContext;
    }

    /**
     * <p>Performs unmarshalling with a
     * {@link LockssReferenceByXPathMarshaller} instance.</p>
     * @see LockssReferenceByXPathMarshaller
     */
    public void marshal(HierarchicalStreamWriter writer,
                        Object root,
                        DefaultConverterLookup converterLookup,
                        ClassMapper classMapper,
                        DataHolder dataHolder) {
      new LockssReferenceByXPathMarshaller(
          writer,
          converterLookup,
          classMapper,
          root.getClass().getName()
      ).start(root, dataHolder);
    }

    /**
     * <p>Performs unmarshalling with a
     * {@link LockssReferenceByXPathUnmarshaller} instance.</p>
     * @see LockssReferenceByXPathUnmarshaller
     */
    public Object unmarshal(Object root,
                            HierarchicalStreamReader reader,
                            DataHolder dataHolder,
                            DefaultConverterLookup converterLookup,
                            ClassMapper classMapper) {
      return new LockssReferenceByXPathUnmarshaller(
          lockssContext, root, reader, converterLookup, classMapper).start(
              dataHolder);
    }

  }
  /*
   * end PRIVATE STATIC INNER CLASS
   * ==============================
   */

  /*
   * begin PRIVATE STATIC INNER CLASS
   * ================================
   */
  /**
   * <p>A custom implementation of {@link ReferenceByXPathUnmarshaller}
   * that performs post-deserialization processing on deserialized
   * object graphs.</p>
   * @author Thib Guicherd-Callin
   */
  private static class LockssReferenceByXPathUnmarshaller
      extends ReferenceByXPathUnmarshaller {

    /**
     * <p>A reference to the context object.</p>
     */
    private LockssApp lockssContext;

    /**
     * <p>Builds a new instance by invoking the superclass constructor
     * with the same argument list except for the first parameter
     * (which is just saved).</p>
     * @param lockssContext   A deserialization context object.
     * @param root
     * @param reader
     * @param converterLookup
     * @param classMapper
     */
    public LockssReferenceByXPathUnmarshaller(LockssApp lockssContext,
                                              Object root,
                                              HierarchicalStreamReader reader,
                                              ConverterLookup converterLookup,
                                              ClassMapper classMapper) {
      super(root, reader, converterLookup, classMapper);
      this.lockssContext = lockssContext;
    }

    /**
     * <p>Converts an object using the superclass mechanism, then
     * invokes the protected post-deserialization method
     * <code>postUnmarshal(org.lockss.app.LockssApp)</code> if it is
     * present.</p>
     * @param parent
     * @param type
     */
    public Object convertAnother(Object parent, Class type) {
      Object ret = super.convertAnother(parent, type);
      if (ret instanceof LockssSerializable) {
        Object[] parameters = new Object[] { lockssContext };
        invokeMethod(
            ret,
            POST_UNMARSHAL_METHOD,
            POST_UNMARSHAL_PARAMETERS,
            postUnmarshalCache,
            parameters
        );
        Object surrogate = invokeMethod(
            ret,
            POST_UNMARSHAL_RESOLVE_METHOD,
            POST_UNMARSHAL_RESOLVE_PARAMETERS,
            postUnmarshalResolveCache,
            parameters
        );
        if (surrogate != null) {
          ret = surrogate;
        }
      }
      return ret;
    }

    private Method cacheMethod(Object obj,
                               String methodName,
                               Class[] methodParameters,
                               HashMap methodCache) {
      Class objClass = obj.getClass();
      Method objMethod = null;

      // Look up inheritance hierarchy
      while (objClass != Object.class) {
        try {
          objMethod =
            objClass.getDeclaredMethod(methodName,
                                       methodParameters);
          objClass = Object.class; // executed only if call succeeds
        }
        catch (NoSuchMethodException nsmE) {
          objClass = objClass.getSuperclass();
        }
      }

      // Cache result
      if (objMethod == null) {
        methodCache.put(obj.getClass(), NONE);
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
    private ConversionException failDeserialize(Exception exc) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("An exception of type ");
      buffer.append(exc.getClass().getName());
      buffer.append(" was thrown by an object while it was being deserialized.");
      return new ConversionException(buffer.toString(), exc);
    }

    private Object invokeMethod(Object obj,
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

    private Method lookupMethod(Object obj,
                                String methodName,
                                Class[] methodParameters,
                                HashMap methodCache) {
      Class objClass = obj.getClass();
      Object objMethod = methodCache.get(objClass);

      if (objMethod == null) {
        return cacheMethod(obj,
                           methodName,
                           methodParameters,
                           methodCache);
      }
      else if (objMethod == NONE) {
        return null;
      }
      else {
        return (Method)objMethod;
      }
    }

    /**
     * <p>A special unique value used in maps to denote that the
     * key exists but that it has no value.</p>
     */
    private static final Object NONE = new Object();

    /**
     * <p>The String name of the method automagically called during
     * post-deserialization of {@link LockssSerializable} objects
     * to post-process deserialized objects.</p>
     * @see #POST_UNMARSHAL_PARAMETERS
     */
    private static final String POST_UNMARSHAL_METHOD =
      "postUnmarshal";

    /**
     * <p>The list of parameter types of the method
     * {@link #POST_UNMARSHAL_METHOD}.</p>
     * @see #POST_UNMARSHAL_METHOD
     */
    private static final Class[] POST_UNMARSHAL_PARAMETERS =
      new Class[] { LockssApp.class };

    /**
     * <p>The String name of the method automagically called during
     * post-deserialization of {@link LockssSerializable} objects
     * to perform object substitution.</p>
     * @see #POST_UNMARSHAL_RESOLVE_PARAMETERS
     */
    private static final String POST_UNMARSHAL_RESOLVE_METHOD =
      "postUnmarshalResolve";

    /**
     * <p>The list of parameter types of the method
     * {@link #POST_UNMARSHAL_RESOLVE_METHOD}.</p>
     * @see #POST_UNMARSHAL_RESOLVE_METHOD
     */
    private static final Class[] POST_UNMARSHAL_RESOLVE_PARAMETERS =
      new Class[] { LockssApp.class };

    /**
     * <p>A map to cache post-deserialization {@link Method}s by
     * class.</p>
     */
    private static final HashMap postUnmarshalCache = new HashMap();

    private static final HashMap postUnmarshalResolveCache = new HashMap();

  }
  /*
   * end PRIVATE STATIC INNER CLASS
   * ==============================
   */

  /**
   * <p>A lazy instantiation flag.</p>
   */
  private boolean initialized;

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
    this.initialized = false; // lazy instantiation, see init()
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
    this.initialized = false; // lazy instantiation, see init()
  }

  /* Inherit documentation */
  public Object deserialize(Reader reader)
      throws SerializationException,
             InterruptedIOException {
    try {
      init(); // lazy instantiation
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
      init(); // lazy instantiation
      xs.toXML(obj, writer);
    }
    catch (LockssNotSerializableException lnse) {
      errorString = "Not Serializable or LockssSerializable";
      throw failSerialize(errorString,
                          lnse,
                          new SerializationException.NotSerializableOrLockssSerializable(errorString, lnse));

    }
    catch (StreamException se) {
      throw failSerialize(errorString,
                          se,
                          new SerializationException(errorString, se));
    }
    catch (CannotResolveClassException crce) {
      throw failSerialize(errorString,
                          crce,
                          new SerializationException(errorString, crce));
    }
    catch (BaseException be) {
      throw failSerialize(errorString,
                          be,
                          new SerializationException(errorString, be));
    }
    catch (RuntimeException re) {
      throwIfInterrupted(re);
      throw re;
    }
  }

  /**
   * <p>Performs tasks to resolve the lazy instantiation.</p>
   */
  private synchronized void init() {
    if (!initialized) {
      // Mimic the behavior of XStream's JVM class
      String vendor = System.getProperty("java.vm.vendor");
      float version = 1.3f;
      try {
        version = Float.parseFloat(System.getProperty("java.version").substring(0, 3));
      }
      catch (NumberFormatException nfe) {
        // Keep the default
      }
      Class unsafe = null;
      try {
        unsafe = Class.forName("sun.misc.Unsafe", false, getClass().getClassLoader());
      }
      catch (ClassNotFoundException cnfe) {
        // Keep the default
      }
      ReflectionProvider reflectionProvider = null;
      if (   (   vendor.contains("Sun")
              || vendor.contains("Oracle")
              || vendor.contains("Apple")
              || vendor.contains("Hewlett-Packard")
              || vendor.contains("IBM")
              || vendor.contains("Blackdown"))
          && version >= 1.4f
          && unsafe != null) {
        try {
          reflectionProvider = (ReflectionProvider)Class.forName("com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider", false, getClass().getClassLoader()).newInstance();
        }
        catch (InstantiationException ie) {
          reflectionProvider = new PureJavaReflectionProvider();
        }
        catch (IllegalAccessException iae) {
          reflectionProvider = new PureJavaReflectionProvider();
        }
        catch (ClassNotFoundException cnfe) {
          reflectionProvider = new PureJavaReflectionProvider();
        }
      }
      else {
        reflectionProvider = new PureJavaReflectionProvider();
      }
      HierarchicalStreamDriver driver = new DomDriver();
      
      xs = new XStream(reflectionProvider, driver);
      xs.setMarshallingStrategy(new LockssReferenceByXPathMarshallingStrategy(lockssContext));
      xs.registerConverter(new LockssDateConverter());
      xs.registerConverter(new LockssHashResultConverter());
      xs.registerConverter(new LockssConstructingConverter(xs.getClassMapper()));
      initialized = true;
    }
  }

  /**
   * <p>A logger for use by this serializer.</p>
   */
  private static Logger logger = Logger.getLogger(XStreamSerializer.class);

}
