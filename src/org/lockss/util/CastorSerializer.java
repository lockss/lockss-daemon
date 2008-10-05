/*
 * $Id: CastorSerializer.java,v 1.10 2006-09-28 05:12:00 thib_gc Exp $
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
import java.net.URL;
import java.util.*;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.*;
import org.lockss.app.LockssApp;
import org.lockss.app.LockssAppException;

/**
 * <p>An implementation of {@link org.lockss.util.ObjectSerializer}
 * based on
 * {@link <a href="http://www.castor.org/">Castor</a>}.</p>
 * <p>Castor is a very powerful data binding framework, but its focus
 * is relational database persistence rather than lightweight object
 * serialization. A consequence is that Castor requires heavy lifting
 * on the part of client code. Because of the need for mapping files,
 * subtle bugs (as of this writing) in the implementation of Castor,
 * difficulties encountered in serializing collections, and (in
 * summary) the inadequacy of the whole framework for the task of
 * merely marshalling objects to XML and back, Castor is being phased
 * out of the LOCKSS codebase. Newer serialization facilities are
 * based on {@link <a href="http://xstream.codehaus.org/">XStream</a>}
 * (see {@link XStreamSerializer}).</p>
 * <p>This class is provided for backwards compatibility, as part of
 * a refactoring of marshaller code to make room for XStream-based
 * marshalling. It may be deprecated in the future. New client code
 * should not use this class for serialization tasks.</p>
 * <p>It is generally <em>not safe</em> to use an instance of this
 * class for multiple unrelated serialization operations. It may be
 * possible to re-use the same instance for multiple marshalling
 * or for multiple unmarshalling operations based on the same
 * mapping.</p>
 * <p>This class is not able to enforce that an object graph is
 * serializable while carrying out a serialization task.</p>
 * @author Thib Guicherd-Callin
 * @see XStreamSerializer
 */
public class CastorSerializer extends ObjectSerializer {

  /*
   * IMPLEMENTATION NOTES
   *
   * This class was created in the process of refactoring all XML
   * marshalling code previously in org.lockss.util.XmlMarshaller
   * (which may no longer be around when someone next reads this).
   * The goal was to remove Castor-aware code from data structures
   * that had no business knowing about Castor and data binding
   * frameworks, and to enable phasing out Castor and phasing in
   * XStream.
   *
   * Marshalling with Castor requires mapping files, but I do not
   * know how the ones that were already in the codebase were
   * originally produced.
   *
   * If you are trying to figure out from digging back in CVS
   * history how XmlMarshaller worked, the summary is:
   *  * The basic functionality came from storeToWriter and
   *    loadFromReader.
   *  * All store() methods ended up calling
   *    store(File, Object, Mapping) and all load() methods end up
   *    calling load(File, Class, Mapping).
   */

  /**
   * <p>The Class object intended to represent the class of objects
   * being processed by this instance.</p>
   */
  private Class targetClass;

  /**
   * <p>The {@link org.exolab.castor.mapping.Mapping} object
   * governing the processing of objects of the class represented by
   * {@link #targetClass}.</p>
   */
  private Mapping targetMapping;

  /**
   * <p>Builds a new CastorSerializer instance.</p>
   * @param lockssContext A serialization context object.
   * @param targetMapping The
   *                      {@link org.exolab.castor.mapping.Mapping}
   *                      of objects intended for processing by this
   *                      serializer.
   * @param targetClass   The Class of objects intended for processing
   *                      by this serializer.
   * @see ObjectSerializer#ObjectSerializer(LockssApp)
   */
  public CastorSerializer(LockssApp lockssContext,
                          Mapping targetMapping,
                          Class targetClass) {
    super(lockssContext);
    this.targetMapping = targetMapping;
    this.targetClass = targetClass;
  }

  /**
   * <p>Builds a new CastorSerializer instance.</p>
   * @param lockssContext A serialization context object.
   * @param targetMapping             The {@link Mapping} of objects
   *                                  intended for processing by this
   *                                  serializer.
   * @param targetClass               The Class of objects intended
   *                                  for processing by this serializer.
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see ObjectSerializer#ObjectSerializer(LockssApp, boolean, int)
   */
  public CastorSerializer(LockssApp lockssContext,
                          Mapping targetMapping,
                          Class targetClass,
                          boolean saveTempFiles,
                          int failedDeserializationMode) {
    super(lockssContext,
          saveTempFiles,
          failedDeserializationMode);
    this.targetMapping = targetMapping;
    this.targetClass = targetClass;
  }

  /**
   * <p>Builds a new CastorSerializer instance.</p>
   * @param lockssContext   A serialization context object.
   * @param mappingFilename A filename where the mapping file for
   *                        objects intended for processing by this
   *                        serializer can be located.
   * @param targetClass     The Class of objects intended for
   *                        processing by this serializer.
   * @see #CastorSerializer(LockssApp, Mapping, Class)
   */
  public CastorSerializer(LockssApp lockssContext,
                          String mappingFilename,
                          Class targetClass) {
    this(lockssContext,
         getMapping(mappingFilename),
         targetClass);
  }

  /**
   * <p>Builds a new CastorSerializer instance.</p>
   * @param lockssContext             A serialization context object.
   * @param mappingFilename           A filename where the mapping
   *                                  file for objects intended for
   *                                  processing by this serializer
   *                                  can be located.
   * @param targetClass               The Class of objects intended for
   *                                  processing by this serializer.
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see #CastorSerializer(LockssApp, Mapping, Class, boolean, int)
   */
  public CastorSerializer(LockssApp lockssContext,
                          String mappingFilename,
                          Class targetClass,
                          boolean saveTempFiles,
                          int failedDeserializationMode) {
    this(lockssContext,
         getMapping(mappingFilename),
         targetClass,
         saveTempFiles,
         failedDeserializationMode);
  }

  /**
   * <p>Builds a new CastorSerializer instance.</p>
   * <p>Uses a null context.</p>
   * @param mappingFilename A filename where the mapping file for
   *                        objects intended for processing by this
   *                        serializer can be located.
   * @param targetClass     The Class of objects intended for
   *                        processing by this serializer.
   * @see #CastorSerializer(LockssApp, String, Class)
   */
  public CastorSerializer(String mappingFilename,
                          Class targetClass) {
    this(null,
         mappingFilename,
         targetClass);
  }

  /**
   * <p>Builds a new CastorSerializer instance.</p>
   * <p>Uses a null context.</p>
   * @param mappingFilename           A filename where the mapping
   *                                  file for objects intended for
   *                                  processing by this serializer
   *                                  can be located.
   * @param targetClass               The Class of objects intended for
   *                                  processing by this serializer.
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @see #CastorSerializer(LockssApp, String, Class, boolean, int)
   */
  public CastorSerializer(String mappingFilename,
                          Class targetClass,
                          boolean saveTempFiles,
                          int failedDeserializationMode) {
    this(null,
         mappingFilename,
         targetClass,
         saveTempFiles,
         failedDeserializationMode);
  }

  /* Inherit documentation */
  public Object deserialize(Reader reader)
      throws SerializationException, InterruptedIOException {
    try {
      Unmarshaller unmarshaller = new Unmarshaller(targetClass);
      unmarshaller.setMapping(targetMapping);
      return unmarshaller.unmarshal(reader);
    }
    catch (MappingException mappingEx) {
      throw failDeserialize(mappingEx);
    }
    catch (MarshalException marshalEx) {
      Exception inner = marshalEx.getException();
      if (inner != null && inner instanceof InterruptedIOException) {
        throw failDeserialize(inner);
      }
      else {
        throw failDeserialize(marshalEx);
      }
    }
    catch (ValidationException ve) {
      throw failDeserialize(ve);
    }
    catch (RuntimeException re) {
      /*
       * This is a testing artifact. Castor does not throw a runtime
       * exception whose cause is InterruptedIOException, but testing
       * code does. Always rethrow; turn into an InterruptedIOException
       * to satisfy testing code that the serializer is paying
       * attention.
       */
      throwIfInterrupted(re);
      throw re;
    }
  }

  /**
   * <p>Retrieves this instance's target class.</p>
   * @return The underlying target class object.
   */
  public Class getTargetClass() {
    return targetClass;
  }

  /**
   * <p>Retrieves this instance's target mapping.</p>
   * @return The underlying target mapping object.
   */
  public Mapping getTargetMapping() {
    return targetMapping;
  }

  /* Inherit documentation */
  protected void serialize(Writer writer,
                           Object obj)
      throws SerializationException,
             InterruptedIOException {
    throwIfNull(obj);
    String errorString = "Failed to serialize an object of type " + obj.getClass().getName();

    try {
      Marshaller marshaller = new Marshaller(writer);
      marshaller.setMapping(targetMapping);
      marshaller.marshal(obj);
    }
    catch (IOException ioe) {
      throw failSerialize(errorString,
                         ioe,
                         new SerializationException(errorString, ioe));
    }
    catch (MappingException mappingEx) {
      throw failSerialize(errorString,
                          mappingEx,
                          new SerializationException(errorString, mappingEx));
    }
    catch (CastorException ce) {
      throw failSerialize(errorString,
                          ce,
                          new SerializationException(errorString, ce));
    }
    catch (RuntimeException re) {
      /*
       * This is a testing artifact. Castor does not throw a runtime
       * exception whose cause is InterruptedIOException, but testing
       * code does. Always rethrow; turn into an InterruptedIOException
       * to satisfy testing code that the serializer is paying
       * attention to InterruptedIOException.
       */
      throwIfInterrupted(re);
      throw re;
    }
  }

  /**
   * <p>A cache for previously requested mappings.</p>
   */
  private static HashMap mappingCache = new HashMap();
  // JAVA5: HashMap<Set<String>,Mapping> or similar

  /**
   * <p>Convenience method to obtain a Mapping instance corresponding
   * to the given file name.</p>
   * @param filename A file name where a mapping is stored.
   * @return A Mapping instance corresponding to the file name.
   */
  public static Mapping getMapping(String filename) {
    // JAVA5: collapse this method and getMapping(String[])
    // into single getMapping(String...)
    Set set = new HashSet(); // JAVA5: Set<String>
    set.add(filename);
    return getMapping(set);
  }

  /**
   * <p>Convenience method to obtain a Mapping instance corresponding
   * to all the given file names.</p>
   * @param filenames An array of file names where mappings are stored.
   * @return A Mapping instance corresponding to all the file names.
   */
  public static Mapping getMapping(String[] filenames) {
    // JAVA5: collapse this method and getMapping(String)
    // into single getMapping(String...)
    Set set = new HashSet(); // JAVA5: Set<String>
    set.addAll(Arrays.asList(filenames));
    return getMapping(set);
  }

  /**
   * <p>An exception message formatter used when loading a mapping
   * file fails.</p>
   * @param filename The filename from which the mapping was being
   *        read.
   * @return A new LockssAppException.
   */
  private static LockssAppException failLoadMapping(String filename) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Could not load the following mapping file: ");
    buffer.append(filename);
    return new LockssAppException(buffer.toString());
  }

  /**
   * <p>Retrieves a Mapping instance that encapsulates all the
   * filenames contained in the argument set.</p>
   * @param filenames A set of filenames where the mappings are
   *                  stored.
   * @return A new Mapping instance.
   */
  private static synchronized Mapping getMapping(Set filenames) {
    // JAVA5: Set<String>

    Mapping mapping = (Mapping)mappingCache.get(filenames);

    if (mapping == null) {
      // This request is not cached yet
      mapping = new Mapping();
      Iterator iterator = filenames.iterator(); // JAVA5: foreach
      while (iterator.hasNext()) {
        loadMapping((String)iterator.next(), mapping);
      }

      // Cache result
      mappingCache.put(filenames, mapping);
    }

    return mapping;
  }

  /**
   * <p>Loads a mapping from the given filename into the argument of
   * type {@link Mapping}.</p>
   * @param fromFilename The source file where the mapping is stored.
   * @param intoMapping  A Mapping instance into which the mapping is
   *                     to be loaded.
   */
  private static void loadMapping(String fromFilename, Mapping intoMapping) {
    URL mappingUrl = CastorSerializer.class.getResource(fromFilename);
    if (mappingUrl == null) {
      throw failLoadMapping(fromFilename);
    }
    try {
      intoMapping.loadMapping(mappingUrl);
    }
    catch (Exception e) {
      throw failLoadMapping(fromFilename);
    }
  }

}
