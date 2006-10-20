/*
 * $Id: ObjectSerializerTester.java,v 1.13 2006-10-20 03:28:36 thib_gc Exp $
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

import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.lockss.config.CurrentConfig;
import org.lockss.test.*;
import org.lockss.util.SerializationException;

/**
 * <p>Tests the {@link org.lockss.util.ObjectSerializer} abstract
 * class.</p>
 * <p>This test case uses the standard pattern for unit-testing
 * interfaces and abstract classes. Subclasses of
 * {@link org.lockss.util.ObjectSerializer} are unit-tested by
 * extending this class and implementing the specified abstract
 * methods, so that unit tests for functionality inherited from
 * the abstract class are automatically inherited by subclasses
 * of this unit test.</p>
 * <p>For the convenience of subclasses, this class extends
 * {@link org.custommonkey.xmlunit.XMLTestCase} rather than
 * {@link junit.framework.TestCase}.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class ObjectSerializerTester extends XMLTestCase {

  /**
   * <p>A utility class to encapsulate an object round trip that can
   * be performed using two variant serializers.</p>
   * @author Thib Guicherd-Callin
   * @see ObjectSerializerTester#getRoundTripActions(Object)
   */
  public abstract class DoRoundTrip {
    protected Object result;
    public abstract void doRoundTrip(ObjectSerializer serializer,
                                     ObjectSerializer deserializer)
        throws Exception;
  }

  /**
   * <p>A utility class to encapsulate a variant action that can be
   * performed on a serializer or deserializer.</p>
   * @author Thib Guicherd-Callin
   */
  protected abstract class DoSomething {
    public abstract void doSomething(ObjectSerializer serializer) throws Exception;
  }

  /**
   * <p>A specialized version of
   * {@link ObjectSerializerTester.DoSomething} that has a reference
   * {@link File}.</p>
   * @author Thib Guicherd-Callin
   */
  protected abstract class RememberFile extends DoSomething {
    protected File file;
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  protected Logger logger = Logger.getLogger(StringUtil.shortName(getClass()));

  /**
   * <p>Tests whether the same input data deserialized into two new
   * Object instances produces a consistent result. </p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testDeserializeTwice() throws Exception {

    /*
     * A DoSomething/DoRoundTrip-like action class that deserializes
     * the same serailized data twice.
     */
    abstract class DeserializeTwice {
      protected Object result1 = null;
      protected Object result2 = null;
      public abstract void doDeserializeTwice(ObjectSerializer serializer,
                                              ObjectSerializer deserializer1,
                                              ObjectSerializer deserializer2)
          throws Exception;
    }

    // Make a sample object
    final ExtMapBean original = makeSample_ExtMapBean();

    // Define variant actions
    DeserializeTwice[] actions = new DeserializeTwice[] {
        // With a Writer
        new DeserializeTwice() {
          public void doDeserializeTwice(ObjectSerializer serializer,
                                         ObjectSerializer deserializer1,
                                         ObjectSerializer deserializer2)
              throws Exception {
            StringWriter writer = new StringWriter();
            serializer.serialize(writer, original);
            result1 = serializer.deserialize(new StringReader(writer.toString()));
            result2 = serializer.deserialize(new StringReader(writer.toString()));
          }
        },
        // With an OutputStream
        new DeserializeTwice() {
          public void doDeserializeTwice(ObjectSerializer serializer,
                                         ObjectSerializer deserializer1,
                                         ObjectSerializer deserializer2)
              throws Exception {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serializer.serialize(outputStream, original);
            result1 = serializer.deserialize(new ByteArrayInputStream(outputStream.toByteArray()));
            result2 = serializer.deserialize(new ByteArrayInputStream(outputStream.toByteArray()));
          }
        },
        // With a File
        new DeserializeTwice() {
          public void doDeserializeTwice(ObjectSerializer serializer,
                                         ObjectSerializer deserializer1,
                                         ObjectSerializer deserializer2)
              throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile, original);
            result1 = serializer.deserialize(tempFile);
            result2 = serializer.deserialize(tempFile);
          }
        },
        // With a String
        new DeserializeTwice() {
          public void doDeserializeTwice(ObjectSerializer serializer,
                                         ObjectSerializer deserializer1,
                                         ObjectSerializer deserializer2)
              throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile.toString(), original);
            result1 = serializer.deserialize(tempFile.toString());
            result2 = serializer.deserialize(tempFile.toString());
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of serializer...
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      ObjectSerializer[] deserializers1 = getObjectSerializers_ExtMapBean();
      ObjectSerializer[] deserializers2 = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with serializer/deserializers " + serializer);

        // Perform variant action
        actions[action].doDeserializeTwice(serializers[serializer],
                                           deserializers1[serializer],
                                           deserializers2[serializer]);

        // Verify results
        assertEquals(original.getMap(),
                     ((ExtMapBean)actions[action].result1).getMap());
        assertEquals(original.getMap(),
                     ((ExtMapBean)actions[action].result2).getMap());
        assertEquals(((ExtMapBean)actions[action].result1).getMap(),
                     ((ExtMapBean)actions[action].result2).getMap());
      }
    }
  }

  /**
   * <p>Tests that the deserializer behaves appropriately for its
   * mode when deserialization fails.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testFailedDeserializationMode() throws Exception {

    // Define variant actions
    RememberFile[] actions = new RememberFile[] {
        // With a File
        new RememberFile() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.deserialize(file);
          }
        },
        // With a String
        new RememberFile() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.deserialize(file.toString());
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of deserializer...
      ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
      for (int deserializer = 0 ; deserializer < deserializers.length ; ++deserializer) {
        logger.debug("Begin with deserializer " + deserializer);

        try {
          // Create a bad input file
          actions[action].file = File.createTempFile("testfile", ".xml");
          actions[action].file.deleteOnExit();
          FileWriter writer = new FileWriter(actions[action].file);
          writer.write("BAD INPUT");
          writer.close();

          // Perform variant action
          actions[action].doSomething(deserializers[deserializer]);
          fail("Should have thrown SerializationException ("
               + action + "," + deserializer + ")");
        }
        catch (SerializationException ignore) {
          // success; keep going
        }

        // Verify results
        switch (deserializers[deserializer].getFailedDeserializationMode()) {
          case ObjectSerializer.FAILED_DESERIALIZATION_COPY:
            File[] files = actions[action].file.getParentFile().listFiles();
            File copy = null;
            for (int file = 0 ; file < files.length ; ++file) {
              String candidate = files[file].toString();
              if (!candidate.startsWith(actions[action].file.toString())) {
                continue; // not the right file
              }
              if (!StringUtils.contains(candidate,
                                        CurrentConfig.getParam(ObjectSerializer.PARAM_FAILED_DESERIALIZATION_EXTENSION,
                                                               ObjectSerializer.DEFAULT_FAILED_DESERIALIZATION_EXTENSION))) {
                continue; // not the right file
              }
              String timestamp = StringUtils.substringAfterLast(candidate, ".");
              if (StringUtil.isNullString(timestamp)) {
                continue; // not the right file
              }
              try {
                long time = Long.parseLong(timestamp);
              }
              catch (NumberFormatException nfe) {
                continue; // not the right file
              }
              copy = files[file]; // right file found
              break;
            }
            assertNotNull("FAILED_DESERIALIZATION_COPY: copy not found ("
                          + action + "," + deserializer + ")",
                          copy);
            copy.deleteOnExit(); // clean up
            assertTrue("FAILED_DESERIALIZATION_COPY: copy does not match ("
                       + action + "," + deserializer + ")",
                       FileUtil.isContentEqual(actions[action].file, copy));
            break;
          case ObjectSerializer.FAILED_DESERIALIZATION_IGNORE:
            // nothing; keep going
            break;
          case ObjectSerializer.FAILED_DESERIALIZATION_RENAME:
            File rename = new File(actions[action].file.toString()
                                   + CurrentConfig.getParam(ObjectSerializer.PARAM_FAILED_DESERIALIZATION_EXTENSION,
                                                            ObjectSerializer.DEFAULT_FAILED_DESERIALIZATION_EXTENSION));
            assertTrue("FAILED_DESERIALIZATION_RENAME: renamed file did not exist ("
                       + action + "," + deserializer + ")",
                       rename.exists());
            rename.deleteOnExit(); // clean up
            break;
          default: // shouldn't happen
            fail("Unexpected failed deserialization mode ("
                 + action + "," + deserializer + ")");
            break;
        }
      }
    }

  }

  /**
   * <p>Tests that the deserializer behaves appropriately for its
   * mode when serialization fails.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testFailedSerializationMode() throws Exception {

    // Define a rogue file factory
    class RenameFails implements ObjectSerializer.TempFileFactory {
      protected RememberFile action;
      public RenameFails(RememberFile action) {
        this.action = action;
      }
      public File createTempFile(String prefix, String suffix, File directory) throws IOException {
        File tmp = File.createTempFile(prefix, suffix, directory);
        tmp.deleteOnExit();
        action.file = new File(tmp.toString()) {
          public boolean renameTo(File dest) {
            return false; // fail
          }
        };
        return action.file;
      }
    }

    // Make a sample object
    final ExtMapBean obj = makeSample_ExtMapBean();

    // Define variant actions
    RememberFile[] actions = new RememberFile[] {
        // With a File
        new RememberFile() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.tempFileFactory = new RenameFails(this);
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile, obj);
          }
        },
        // With a String
        new RememberFile() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.tempFileFactory = new RenameFails(this);
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile.toString(), obj);
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of serializer...
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with serializer " + serializer);

        try {
          // Perform variant action
          actions[action].doSomething(serializers[serializer]);
          fail("Should have thrown SerializationException.RenameFailed ("
               + action + "," + serializer + ")");
        }
        catch (SerializationException.RenameFailed ignore) {
          // success; keep going
        }

        // Verify results
        assertEquals(serializers[serializer].getFailedSerializationMode(),
                     actions[action].file.exists());
      }
    }

  }

  /**
   * <p>Tests the serializer throws a {@link SerializationException}
   * when serializing into a file but creating temporary files
   * fails.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testHandlesCannotCreateTempFile() throws Exception {

    // Define a rogue file factory
    final ObjectSerializer.TempFileFactory createTempFileFails = new ObjectSerializer.TempFileFactory() {
      public File createTempFile(String prefix, String suffix, File directory) throws IOException {
        throw new IOException("This fake IOException can safely be ignored"); // fail
      }
    };

    // Make a sample object
    final ExtMapBean obj = makeSample_ExtMapBean();

    // Define variant actions
    DoSomething[] actions = new DoSomething[] {
        // With a File
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.tempFileFactory = createTempFileFails;
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile, obj);
          }
        },
        // With a String
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.tempFileFactory = createTempFileFails;
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile.toString(), obj);
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of serializer...
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with serializer " + serializer);

        try {
          // Perform variant action
          actions[action].doSomething(serializers[serializer]);
          fail("Should have thrown SerializationException ("
               + action + "," + serializer + ")");
        }
        catch (SerializationException ignore) {
          // success
        }
      }
    }

  }

  /**
   * <p>Tests if a sample object can be serialized
   * and deserialized without changing.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   * @see #getRoundTripActions(Object)
   */
  public void testRoundTrip() throws Exception {

    // Make a sample object
    final ExtMapBean original = makeSample_ExtMapBean();

    // Define variant actions
    DoRoundTrip[] actions = getRoundTripActions(original);

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of serializer...
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with serializer/deserializer " + serializer);

        // Perform variant action
        actions[action].doRoundTrip(serializers[serializer],
                                    deserializers[serializer]);

        // Verify results
        assertEquals(original.getMap(),
                     ((ExtMapBean)actions[action].result).getMap());
      }
    }
  }

  /**
   * <p>Checks that serializing with a non-{@link Writer} argument
   * gives the same result as serializing with a {@link Writer}
   * argument.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testSameAsSerializeWriter() throws Exception {

    /*
     * A DoSomething/DoRoundtrip-like action class that gives access
     * to the serialized data with a Reader.
     */
    abstract class SerializeInspect extends DoSomething {
      protected Reader result;
    }

    // Make a sample object
    final ExtMapBean original = makeSample_ExtMapBean();

    // Define variant actions
    SerializeInspect[] actions = new SerializeInspect[] {
        // With an OutputStream
        new SerializeInspect() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serializer.serialize(outputStream, original);
            result = new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray()));
          }
        },
        // With a File
        new SerializeInspect() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile, original);
            result = new FileReader(tempFile);
          }
        },
        // With a String
        new SerializeInspect() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile.toString(), original);
            result = new FileReader(tempFile.toString());
          }
        },
    };

    // Set up XML comparison
    XMLUnit.setIgnoreWhitespace(true);

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of serializer...
      ObjectSerializer[] references = getObjectSerializers_ExtMapBean();
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < references.length ; ++serializer) {
        logger.debug("Begin with serializer " + serializer);

        // Serialize the data with a Writer (reference)
        StringWriter writer = new StringWriter();
        references[serializer].serialize(writer, original);
        StringReader reader = new StringReader(writer.toString());

        // Serialize the data with a variant
        actions[action].doSomething(serializers[serializer]);

        // Verify results
        assertXMLEqual(reader, actions[action].result);
      }
    }
  }

  /**
   * <p>Tests whether the serializer correctly throws
   * {@link SerializationException.FileNotFound} when attempting to
   * read from a file that does not exist.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testThrowsFileNotFound() throws Exception {

    // Define variant actions
    DoSomething[] actions = new DoSomething[] {
        // With a File
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            Object obj = serializer.deserialize(new File("filethatdoesnotexist.bad"));
          }
        },
        // With a String
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            Object obj = serializer.deserialize("filethatdoesnotexist.bad");
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of deserializer...
      ObjectSerializer[] deserializers = getObjectSerializers_ExtMapBean();
      for (int deserializer = 0 ; deserializer < deserializers.length ; ++deserializer) {
        logger.debug("Begin with deserializer " + deserializer);

        try {
          // Perform variant action
          actions[action].doSomething(deserializers[deserializer]);
          fail("Should have thrown SerializationException.FileNotFound ("
               + action + "," + deserializer + ")");
        }
        catch (SerializationException.FileNotFound ignore) {
          // success
        }
      }
    }
  }

  /**
   * <p>Tests that an {@link InterruptedIOException} is thrown when
   * I/O fails because of an underlying
   * {@link InterruptedIOException}.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testThrowsInterruptedIOException() throws Exception {

    // Make a sample object
    final ExtMapBean obj = makeSample_ExtMapBean();

    // Define variant actions
    DoSomething[] actions = new DoSomething[] {
        // With a Writer
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.serialize(new StringWriter() {
              public void write(char[] cbuf, int off, int len) {
                throw new RuntimeException(new InterruptedIOException());
              }
            }, obj);
          }
        },
        // With an OutputStream
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.serialize(new ByteArrayOutputStream() {
              public synchronized void write(byte[] b, int off, int len) {
                throw new RuntimeException(new InterruptedIOException());
              }
            }, obj);
          }
        },
        // With a Reader
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.deserialize(new StringReader("") {
              public int read(char[] cbuf, int off, int len) throws IOException {
                throw new InterruptedIOException();
              }
            });
          }
        },
        // With an InputStream
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.deserialize(new StringInputStream("") {
              public int read(byte[] b, int off, int len) throws IOException {
                throw new InterruptedIOException();
              }
            });
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of serializer...
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with deserializer " + serializer);

        try {
          // Perform variant action
          actions[action].doSomething(serializers[serializer]);
          fail("Should have thrown InterruptedIOException ("
               + action + "," + serializer + ")");
        }
        catch (InterruptedIOException ignore) {
          // success
        }
        catch (RuntimeException re) {
          if (re.getCause() != null && re.getCause() instanceof InterruptedIOException) {
            fail("Should have thrown InterruptedIOException ("
                 + action + "," + serializer + ")");
          }
          else {
            // success
          }
        }
      }
    }

  }

  /**
   * <p>Tests that the serializer throws a {@link NullPointerException}
   * when asked to serialize null.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testThrowsNullPointerException() throws Exception {

    // Define variant actions
    DoSomething[] actions = new DoSomething[] {
        // With a Writer and a LockssSerializable
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.serialize(new StringWriter(), (LockssSerializable)null);
          }
        },
        // With a Writer and a Serializable
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.serialize(new StringWriter(), (Serializable)null);
          }
        },
        // With an OutputStream and a LockssSerializable
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.serialize(new ByteArrayOutputStream(), (LockssSerializable)null);
          }
        },
        // With an OutputStream and a Serializable
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            serializer.serialize(new ByteArrayOutputStream(), (Serializable)null);
          }
        },
        // With a File and a LockssSerializable
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile, (LockssSerializable)null);
          }
        },
        // With a File and a Serializable
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile, (Serializable)null);
          }
        },
        // With a String and a LockssSerializable
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile.toString(), (LockssSerializable)null);
          }
        },
        // With a String and a Serializable
        new DoSomething() {
          public void doSomething(ObjectSerializer serializer) throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile.toString(), (Serializable)null);
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      logger.debug("Begin with action " + action);

      // For each type of serializer...
      ObjectSerializer[] serializers = getObjectSerializers_ExtMapBean();
      for (int serializer = 0 ; serializer < serializers.length ; ++serializer) {
        logger.debug("Begin with serializer " + serializer);

        try {
          // Perform variant action
          actions[action].doSomething(serializers[serializer]);
          fail("Should have thrown NullPointerException ("
               + action + "," + serializer + ")");
        }
        catch (NullPointerException ignore) {
          // success
        }
      }
    }

  }

  /**
   * <p>Prepares a set of serializers with as much combinatorial
   * diversity as the {@link ObjectSerializer} class can provide.</p>
   * <p>Subclasses that have more combinatorial diversity should
   * override this method and create the cross-product of their
   * specific functionality with the value returned by this method.</p>
   * @return An array fo {@link ObjectSerializer} instances.
   * @see #getObjectSerializers_ExtMapBean
   * @see #makeObjectSerializer_ExtMapBean
   */
  protected ObjectSerializer[] getMinimalObjectSerializers_ExtMapBean() {
    // Failed serialization modes
    boolean[] serModes = new boolean[] {
        false,
        true,
    };

    // Failed deserialization modes
    int[] deserModes = new int[] {
        ObjectSerializer.FAILED_DESERIALIZATION_COPY,
        ObjectSerializer.FAILED_DESERIALIZATION_IGNORE,
        ObjectSerializer.FAILED_DESERIALIZATION_RENAME,
    };

    // Serialization read back modes
    boolean[] readBackModes = new boolean[] {
        false,
        true,
    };

    ObjectSerializer[] serializers = new ObjectSerializer[serModes.length * deserModes.length * readBackModes.length];
    int ix = 0;
    for (int serMode = 0 ; serMode < serModes.length ; ++serMode) {
      for (int deserMode = 0 ; deserMode < deserModes.length ; ++deserMode) {
        for (int readBackMode = 0 ; readBackMode < readBackModes.length ; ++readBackMode) {
          serializers[ix] = makeObjectSerializer_ExtMapBean(serModes[serMode], deserModes[deserMode]);
          serializers[ix].setSerializationReadBackMode(readBackModes[readBackMode]);
          ++ix;
        }
      }
    }

    return serializers;
  }

  /**
   * <p>Prepares a set of serializers configured each differently as
   * appropriate for the class being tested, usually intended to mean
   * a combinatorial arrangement of all possible configurations.</p>
   * @return An array fo {@link ObjectSerializer} instances.
   * @see #getMinimalObjectSerializers_ExtMapBean
   * @see #getObjectSerializers_ExtMapBean
   */
  protected abstract ObjectSerializer[] getObjectSerializers_ExtMapBean();

  /**
   * <p>A re-usable set of {@link ObjectSerializerTester.DoRoundTrip}
   * actions that exercise all four argument types for the serializer
   * and deserializer pair, plus the file-oriented ones with a file
   * name starting with a pound sign.</p>
   * @param original An original object.
   * @return An array of actions.
   */
  protected DoRoundTrip[] getRoundTripActions(final Object original) {
    return new DoRoundTrip[] {
        // With a Writer
        new DoRoundTrip() {
          public void doRoundTrip(ObjectSerializer serializer,
                                  ObjectSerializer deserializer)
              throws Exception {
            StringWriter writer = new StringWriter();
            serializer.serialize(writer, original);
            result = deserializer.deserialize(new StringReader(writer.toString()));
          }
        },
        // With an OutputStream
        new DoRoundTrip() {
          public void doRoundTrip(ObjectSerializer serializer,
                                  ObjectSerializer deserializer)
              throws Exception {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serializer.serialize(outputStream, original);
            result = deserializer.deserialize(new ByteArrayInputStream(outputStream.toByteArray()));
          }
        },
        // With a File
        new DoRoundTrip() {
          public void doRoundTrip(ObjectSerializer serializer,
                                  ObjectSerializer deserializer)
              throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile, original);
            result = deserializer.deserialize(tempFile);
          }
        },
        // With a String
        new DoRoundTrip() {
          public void doRoundTrip(ObjectSerializer serializer,
                                  ObjectSerializer deserializer)
              throws Exception {
            File tempFile = File.createTempFile("testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile.toString(), original);
            result = deserializer.deserialize(tempFile.toString());
          }
        },
        // With a File starting with a pound sign
        new DoRoundTrip() {
          public void doRoundTrip(ObjectSerializer serializer,
                                  ObjectSerializer deserializer)
              throws Exception {
            File tempFile = File.createTempFile("#testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile, original);
            result = deserializer.deserialize(tempFile);
          }
        },
        // With a String starting with a pound sign
        new DoRoundTrip() {
          public void doRoundTrip(ObjectSerializer serializer,
                                  ObjectSerializer deserializer)
              throws Exception {
            File tempFile = File.createTempFile("#testfile", ".xml");
            tempFile.deleteOnExit();
            serializer.serialize(tempFile.toString(), original);
            result = deserializer.deserialize(tempFile.toString());
          }
        },
    };
  }

  /**
   * <p>Produces an ObjectSerializer instance to conduct tests based
   * on objects of type {@link org.lockss.util.ExtMapBean}.</p>
   * @param saveTempFiles             A failed serialization mode.
   * @param failedDeserializationMode A failed deserialization mode.
   * @return A newly instantiated instance of the ObjectSerializer
   *         subclass under test, configured properly to operate on
   *         objects of type {@link org.lockss.util.ExtMapBean}.
   */
  protected abstract ObjectSerializer makeObjectSerializer_ExtMapBean(boolean saveTempFiles,
                                                                      int failedDeserializationMode);

  /**
   * <p>Builds a sample {@link org.lockss.util.ExtMapBean}
   * ready to marshal in tests.</p>
   * <p>If subclasses override this method, they must make sure to
   * create an ExtMapBean full of many data types to maximize the
   * difficulty of the test.</p>
   * @return A newly allocated
   *         {@link org.lockss.util.ExtMapBean} filled with
   *         many interesting keys and values.
   */
  protected ExtMapBean makeSample_ExtMapBean() throws Exception {
    return new ExtMapBean(makeSample_TypedEntryMap().m_map);
  }

  /**
   * <p>Builds a sample {@link org.lockss.util.TypedEntryMap}
   * ready to marshal in tests.</p>
   * <p>If subclasses override this method, they must make sure to
   * create a TypedEntryMap full of many data types to maximize the
   * difficulty of the test.</p>
   * @return A newly allocated
   *         {@link org.lockss.util.TypedEntryMap} filled with
   *         many interesting keys and values.
   */
  protected TypedEntryMap makeSample_TypedEntryMap() throws Exception {
    TypedEntryMap tmap = new TypedEntryMap();

    /* Basic data types */
    tmap.putBoolean("boolean.true", true);
    tmap.putBoolean("boolean.false", false);
    tmap.putDouble("double.Math.PI", Math.PI);
    tmap.putDouble("double.Math.E", Math.E);
    tmap.putFloat("float.Float.MAX_VALUE", Float.MAX_VALUE);
    tmap.putFloat("float.0.12345f", 0.12345f);
    tmap.putInt("int.Integer.MIN_VALUE", Integer.MIN_VALUE);
    tmap.putInt("int.Integer.12345", 12345);
    tmap.putLong("long.Long.MAX_VALUE", Long.MAX_VALUE);
    tmap.putLong("long.6507245723L", 6507245723L);
    tmap.putString("string.\"LOCKSS rocks\"", "LOCKSS rocks");
    tmap.putString("string.\"\"", "");

    tmap.putUrl(
        "url.\"http://www.lockss.org/\"",
        new URL("http://www.lockss.org/"));
    tmap.putUrl(
        "url.\"http://www.stanford.edu/\"",
        new URL("http://www.stanford.edu/"));

    /* Collections */
    tmap.putCollection(
        "collection.list.homogeneous",
        ListUtil.list("E", "F", "G", "H"));
    tmap.putCollection(
        "collection.list.heterogeneous",
        ListUtil.list(new Float(1.0), "two", new Long(3)));
    return tmap;
  }

}
