/*

Copyright (c) 2000-2018, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.pdf.pdfbox;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.util.*;
import org.lockss.pdf.*;

/**
 * <p>
 * A new non-wrapping, {@link Serializable} implementation of the
 * {@link PdfToken} family, based on PDFBox 1.8.16.
 * </p>
 * 
 * @since 1.74.4
 * @see PdfToken
 * @see PdfTokenFactory
 */
public class PdfBoxTokens {

  /**
   * <p>
   * A functional interface for a conversion (which in Java 8 should simply be a
   * lambda).
   * </p>
   *
   * @param <T>
   *          The type into which this converter converts.
   * @since 1.74.4
   */
  public interface Converter<T> {
    
    T convert(Object obj);
    
  }
  
  /**
   * <p>
   * A parent class for PDF tokens, that returns {@code false} on every query
   * and throws on every getter {@link UnsupportedOperationException}, and also
   * defines a conversion back to PDFBox objects.
   * </p>
   *
   * @since 1.74.4
   * @see #toPdfBoxObject()
   */
  public static abstract class Tok implements PdfToken, Serializable {

    @Override
    public List<PdfToken> getArray() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean getBoolean() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, PdfToken> getDictionary() {
      throw new UnsupportedOperationException();
    }

    @Override
    public float getFloat() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getInteger() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PdfToken getObject() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getOperator() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getString() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isArray() {
      return false;
    }

    @Override
    public boolean isBoolean() {
      return false;
    }

    @Override
    public boolean isDictionary() {
      return false;
    }

    @Override
    public boolean isFloat() {
      return false;
    }

    @Override
    public boolean isInteger() {
      return false;
    }

    @Override
    public boolean isName() {
      return false;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public boolean isObject() {
      return false;
    }

    @Override
    public boolean isOperator() {
      return false;
    }

    @Override
    public boolean isString() {
      return false;
    }

    /**
     * <p>
     * Turn this object back into a PDFBox object ({@link COSBase} or
     * {@link PDFOperator}).
     * </p>
     * 
     * @return A PDFBox object.
     * @since 1.74.4
     */
    public abstract Object toPdfBoxObject();
    
  }
  
  /**
   * <p>
   * A PDF array.
   * </p>
   * 
   * @since 1.74.4
   * @see COSArray
   */
  public static class Arr extends Tok {
    
    private List<PdfToken> value;
    
    private Arr(List<PdfToken> value) {
      if (value == null) {
        value = Collections.<PdfToken>emptyList();
      }
      ArrayList<PdfToken> val = new ArrayList<PdfToken>(value);
      val.trimToSize();
      this.value = val;
    }
    
    @Override
    public List<PdfToken> getArray() {
      return value;
    }
    
    @Override
    public boolean isArray() {
      return true;
    }

    @Override
    public COSArray toPdfBoxObject() {
      COSArray cosArray = new COSArray();
      for (PdfToken element : value) {
        cosArray.add((COSBase)((Tok)element).toPdfBoxObject());
      }
      return cosArray;
    }
    
    public static Arr of(List<PdfToken> value) {
      return new Arr(value);
    }
    
    public static Arr of(COSArray cosArray) {
      List<PdfToken> elements = new ArrayList<PdfToken>(cosArray.size());
      for (COSBase cosBase : cosArray) {
        elements.add(convertOne(cosBase));
      }
      return of(elements);
    }
    
  }
  
  /**
   * <p>
   * A PDF boolean.
   * </p>
   * 
   * @since 1.74.4
   * @see COSBoolean
   */
  public static class Boo extends Tok {
    
    private boolean value;
    
    private Boo(boolean value) {
      this.value = value;
    }
    
    @Override
    public boolean getBoolean() {
      return value;
    }
    
    @Override
    public boolean isBoolean() {
      return true;
    }

    @Override
    public COSBoolean toPdfBoxObject() {
      return COSBoolean.getBoolean(value);
    }
    
    private Object readResolve() throws ObjectStreamException {
      return of(value);
    }
    
    public static final Boo TRUE = new Boo(true);
    
    public static final Boo FALSE = new Boo(false);
    
    public static Boo of(boolean value) {
      return value ? TRUE : FALSE;
    }
    
    public static Boo of(COSBoolean cosBoolean) {
      return of(cosBoolean.getValue());
    }
    
  }
  
  /**
   * <p>
   * A PDF dictionary.
   * </p>
   * 
   * @since 1.74.4
   * @see COSDictionary
   */
  public static class Dic extends Tok {
    
    private Map<String, PdfToken> value;
    
    private Dic(Map<String, PdfToken> value) {
      if (value == null) {
        value = Collections.<String,PdfToken>emptyMap();
      }
      this.value = new LinkedHashMap<String, PdfToken>(value);
    }
    
    @Override
    public Map<String, PdfToken> getDictionary() {
      return value;
    }
    
    @Override
    public boolean isDictionary() {
      return true;
    }
    
    @Override
    public COSDictionary toPdfBoxObject() {
      COSDictionary cosDictionary = new COSDictionary();
      for (Map.Entry<String, PdfToken> entry : value.entrySet()) {
        cosDictionary.setItem(entry.getKey(), (COSBase)((Tok)entry.getValue()).toPdfBoxObject());
      }
      return cosDictionary;
    }
    
    public static Dic of(Map<String, PdfToken> value) {
      return new Dic(value);
    }
    
    public static Dic of(COSDictionary cosDictionary) {
      Map<String, PdfToken> mapping = new LinkedHashMap<String, PdfToken>();
      for (Map.Entry<COSName, COSBase> entry : cosDictionary.entrySet()) {
        mapping.put(Nam.of(entry.getKey().getName()).getName(), convertOne(entry.getValue()));
      }
      return of(mapping);
    }
    
  }
  
  /**
   * <p>
   * A PDF float.
   * </p>
   * 
   * @since 1.74.4
   * @see COSFloat
   */
  public static class Flo extends Tok {
    
    private float value;
    
    private Flo(float value) {
      this.value = value;
    }

    @Override
    public float getFloat() {
      return value;
    }

    @Override
    public boolean isFloat() {
      return true;
    }

    @Override
    public COSFloat toPdfBoxObject() {
      return new COSFloat(value);
    }
    
    public static Flo of(float value) {
      return new Flo(value);
    }
    
    public static Flo of(COSFloat cosFloat) {
      return of(cosFloat.floatValue());
    }
    
  }
  
  /**
   * <p>
   * A PDF integer.
   * </p>
   * 
   * @since 1.74.4
   * @see COSInteger
   */
  public static class Int extends Tok {
    
    private long value;
    
    private Int(long value) {
      this.value = value;
    }

    @Override
    public long getInteger() {
      return value;
    }

    @Override
    public boolean isInteger() {
      return true;
    }

    @Override
    public COSInteger toPdfBoxObject() {
      return COSInteger.get(value);
    }
    
    private Object readResolve() throws ObjectStreamException {
      return of(value);
    }
    
    // -100 through 256 (inclusive)
    private static final Int[] instances;
    
    static {
      instances = new Int[357];
      for (int i = 0 ; i < 357 ; ++i) {
        instances[i] = new Int(i - 100);
      }
      
    }
    
    public static Int of(long value) {
      return (-100L <= value && value <= 256L) ? instances[(int)value + 100] : new Int(value);
    }
    
    public static Int of(COSInteger cosInteger) {
      return of(cosInteger.longValue());
    }
    
  }
  
  /**
   * <p>
   * A PDF name.
   * </p>
   * 
   * @since 1.74.4
   * @see COSName
   */
  public static class Nam extends Tok {
    
    private String value;
    
    private Nam(String value) {
      this.value = value;
    }
    
    @Override
    public String getName() {
      return value;
    }
    
    @Override
    public boolean isName() {
      return true;
    }
    
    @Override
    public COSName toPdfBoxObject() {
      return COSName.getPDFName(value);
    }
    
    private Object readResolve() throws ObjectStreamException {
      return of(value);
    }
    
    private static final Map<String, Nam> predefined;
    
    static {
      predefined = new HashMap<String, Nam>();
      for (Field field : FieldUtils.getAllFieldsList(COSName.class)) {
        if (field.getType().equals(COSName.class)) {
          int mod = field.getModifiers();
          if (Modifier.isPublic(mod) && Modifier.isStatic(mod) && Modifier.isFinal(mod)) {
            try {
              String value = ((COSName)FieldUtils.readStaticField(field)).getName();
              predefined.put(value, new Nam(value));
            }
            catch (IllegalAccessException exc) {
              // Shouldn't happen
            }
          }
        }
      }
    }
    
    private static final Map<String, Nam> lru = new LRUMap<String, Nam>(8192);
    
    public static synchronized Nam of(String value) {
      Nam ret = predefined.get(value);
      if (ret == null) {
        ret = lru.get(value);
        if (ret == null) {
          ret = new Nam(value);
          lru.put(value, ret);
        }
      }
      return ret;
    }
    
    public static Nam of(COSName cosName) {
      return of(cosName.getName());
    }
    
  }
  
  /**
   * <p>
   * A PDF null.
   * </p>
   * 
   * @since 1.74.4
   * @see COSNull
   */
  public static class Nul extends Tok {
    
    private Nul() {
      // intentionally left blank
    }

    @Override
    public boolean isNull() {
      return true;
    }

    @Override
    public COSNull toPdfBoxObject() {
      return COSNull.NULL;
    }
    
    private Object readResolve() throws ObjectStreamException {
      return getInstance();
    }
    
    public static final Nul NULL = new Nul();
    
    public static Nul getInstance() {
      return NULL;
    }

  }
  
  /**
   * <p>
   * A PDF object token.
   * </p>
   * 
   * @since 1.74.4
   * @see COSObject
   */
  public static class Obj extends Tok {
    
    private PdfToken value;
    
    private long objectNumber;
    
    private long generationNumber;
    
    private Obj(PdfToken value,
                long objectNumber,
                long generationNumber) {
      this.value = value;
      this.objectNumber = objectNumber;
      this.generationNumber = generationNumber;
    }

    @Override
    public PdfToken getObject() {
      return value;
    }
    
    @Override
    public boolean isObject() {
      return true;
    }

    @Override
    public COSObject toPdfBoxObject() {
      COSObject ret = null;
      try {
        ret = new COSObject((COSBase)((Tok)value).toPdfBoxObject());
      }
      catch (IOException exc) {
        /* IMPLEMENTATION NOTE
         * PDFBox 1.8.x: constructor calls setObject which is declared to throw
         * IOException, but the code is all commented out except for the
         * assignment; this can't happen
         */
        throw new IllegalStateException();
      }
      ret.setObjectNumber(Int.of(objectNumber).toPdfBoxObject());
      ret.setGenerationNumber(Int.of(generationNumber).toPdfBoxObject());
      return ret;
    }
    
    public static Obj of(PdfToken value,
                         long objectNumber,
                         long generationNumber) {
      return new Obj(value, objectNumber, generationNumber);
    }
    
    public static Obj of(COSObject cosObject) {
      return of(convertOne(cosObject.getObject()),
                cosObject.getObjectNumber().longValue(),
                cosObject.getGenerationNumber().longValue());
    }
    
  }
  
  /**
   * <p>
   * A PDF operator.
   * </p>
   * 
   * @since 1.74.4
   * @see PDFOperator
   */
  public static class Op extends Tok {
    
    private String value;
    
    private byte[] imageData;
    
    private Map<String, PdfToken> imageParameters;
    
    private Op(String value) {
      this.value = value;
      this.imageData = null;
      this.imageParameters = null;
    }
    
    public byte[] getImageData() {
      return imageData;
    }
    
    @Override
    public String getOperator() {
      return value;
    }
    
    @Override
    public boolean isOperator() {
      return true;
    }

    @Override
    public PDFOperator toPdfBoxObject() {
      PDFOperator pdfOperator = PDFOperator.getOperator(value);
      if (imageData != null) {
        pdfOperator.setImageData(imageData);
      }
      if (imageParameters != null) {
        pdfOperator.setImageParameters(new ImageParameters((COSDictionary)Dic.of(imageParameters).toPdfBoxObject()));
      }
      return pdfOperator;
    }
    
    private Object readResolve() throws ObjectStreamException {
      return of(value);
    }
    
    private static final HashMap<String, Op> cache = new HashMap<String, Op>();
    
    public static synchronized Op of(String value) {
      Op ret = cache.get(value);
      if (ret == null) {
        ret = new Op(value);
        if (!PdfOpcodes.BEGIN_IMAGE_DATA.equals(value) && !PdfOpcodes.BEGIN_IMAGE_OBJECT.equals(value)) {
          cache.put(value, ret);
        }
      }
      return ret;
    }
    
    public static Op of(PDFOperator pdfOperator) {
      Op ret = of(pdfOperator.getOperation());
      if (pdfOperator.getImageData() != null) {
        ret.imageData = pdfOperator.getImageData();
      }
      if (pdfOperator.getImageParameters() != null && pdfOperator.getImageParameters().getDictionary() != null) {
        ret.imageParameters = Dic.of(pdfOperator.getImageParameters().getDictionary()).getDictionary();
      }
      return ret;
    }
    
  }
  
  /**
   * <p>
   * A PDF string.
   * </p>
   * 
   * @since 1.74.4
   * @see COSString
   */
  public static class Str extends Tok {
    
    private String value;
    
    private Str(String value) {
      this.value = value;
    }
    
    @Override
    public String getString() {
      return value;
    }
    
    @Override
    public boolean isString() {
      return true;
    }

    @Override
    public COSString toPdfBoxObject() {
      return new COSString(value);
    }
    
    public static Str of(String value) {
      return new Str(value);
    }
    
    public static Str of(COSString cosString) {
      return of(cosString.getString());
    }
    
  }
  
  /**
   * <p>
   * A PDF token factory based on {@link Tok}.
   * </p>
   * 
   * @since 1.74.4
   * @see Tok
   */
  public static class Factory implements PdfTokenFactory {

    private Factory() {
      // intentionally left blank
    }
    
    @Override
    public PdfToken makeArray() {
      return Arr.of((List<PdfToken>)null);
    }
  
    @Override
    public PdfToken makeArray(List<PdfToken> arrayElements) {
      return Arr.of(arrayElements);
    }
  
    @Override
    public PdfToken makeBoolean(boolean value) {
      return Boo.of(value);
    }
  
    @Override
    public PdfToken makeDictionary() {
      return Dic.of((Map<String, PdfToken>)null);
    }
  
    @Override
    public PdfToken makeDictionary(Map<String, PdfToken> mapping) {
      return Dic.of(mapping);
    }
  
    @Override
    public PdfToken makeFloat(float value) {
      return Flo.of(value);
    }
  
    @Override
    public PdfToken makeInteger(long value) {
      return Int.of(value);
    }
  
    @Override
    public PdfToken makeName(String value) {
      return Nam.of(value);
    }
  
    @Override
    public PdfToken makeNull() {
      return Nul.getInstance();
    }
  
    @Override
    public PdfToken makeObject(PdfToken value,
                               long objectNumber,
                               long generationNumber) {
      return Obj.of(value, objectNumber, generationNumber);
    }
  
    @Override
    public PdfToken makeOperator(String operator) {
      return Op.of(operator);
    }
  
    @Override
    public PdfToken makeString(String value) {
      return Str.of(value);
    }
    
    private static final Factory instance = new Factory();

    /**
     * <p>
     * Gets an instance of this class.
     * </p>
     * 
     * @return An instance of this class.
     * @since 1.74.4
     * @see #instance
     */
    public static Factory getInstance() {
      return instance;
    }
    
  }

  /**
   * <p>
   * Map of {@link PdfToken} converters.
   * </p>
   * 
   * @since 1.74.4
   */
  private static final Map<Class<?>, Converter<PdfToken>> convertToPdfToken;
  
  static {
    convertToPdfToken = new HashMap<Class<?>, Converter<PdfToken>>();
    convertToPdfToken.put(COSArray.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Arr.of((COSArray)obj);
      }
    });
    convertToPdfToken.put(COSBoolean.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Boo.of((COSBoolean)obj);
      }
    });
    convertToPdfToken.put(COSDictionary.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Dic.of((COSDictionary)obj);
      }
    });
    convertToPdfToken.put(COSFloat.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Flo.of((COSFloat)obj);
      }
    });
    convertToPdfToken.put(COSInteger.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Int.of((COSInteger)obj);
      }
    });
    convertToPdfToken.put(COSName.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Nam.of((COSName)obj);
      }
    });
    convertToPdfToken.put(COSNull.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Nul.getInstance();
      }
    });
    convertToPdfToken.put(COSObject.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Obj.of((COSObject)obj);
      }
    });
    convertToPdfToken.put(PDFOperator.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Op.of((PDFOperator)obj);
      }
    });
    convertToPdfToken.put(COSString.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        return Str.of((COSString)obj);
      }
    });
  }
  
  /**
   * <p>
   * Convenience method to convert a list with {@link #convertOne(Object)}.
   * </p>
   * 
   * @param pdfBoxObjects
   *          A list of PDFBox objects ({@link CODBase} or {@link PDFOperator}).
   * @return A list of {@link Tok} instances.
   * @since 1.74.4
   */
  public static List<PdfToken> convertList(List<Object> pdfBoxObjects) {
    List<PdfToken> ret = new ArrayList<PdfToken>(pdfBoxObjects.size());
    for (Object pdfBoxObject : pdfBoxObjects) {
      ret.add(convertOne(pdfBoxObject));
    }
    return ret;
  }
  
  /**
   * <p>
   * Converts one PDFBox object ({@link CODBase} or {@link PDFOperator}) to a
   * {@link Tok} instance.
   * </p>
   * 
   * @param pdfBoxObject
   *          A PDFBox instance.
   * @return A {@link Tok} instance.
   * @throws IllegalStateException
   *           If the given object is not of a recognized type. In particular,
   *           {@link COSStream} is not supported.
   * @since 1.74.4
   * @see #convertToPdfToken
   */
  public static PdfToken convertOne(Object pdfBoxObject) {
    if (pdfBoxObject == null) {
      return Nul.getInstance();
    }
    Converter<PdfToken> converter = convertToPdfToken.get(pdfBoxObject.getClass());
    if (converter == null) {
      throw new IllegalStateException("Encountered a token of unexpected type: " + pdfBoxObject.getClass().getCanonicalName());
    }
    return converter.convert(pdfBoxObject);
  }

  /**
   * <p>
   * Convenience call to {@link Factory#getInstance()}.
   * </p>
   * 
   * @return A {@link Factory} instance.
   * @since 1.74.4
   */
  public static Factory getFactory() {
    return Factory.getInstance();
  }
  
  /**
   * <p>
   * Convenience method to unconvert a list with
   * {@link #unconvertOne(PdfToken)}.
   * </p>
   * 
   * @param pdfTokens
   *          A list of {@link PdfToken} instances that are really {@link Tok}
   *          instances.
   * @return A list of PDFBox objects ({@link CODBase} or {@link PDFOperator}).
   * @since 1.74.4
   */
  public static List<Object> unconvertList(List<PdfToken> pdfTokens) {
    List<Object> ret = new ArrayList<Object>(pdfTokens.size());
    for (PdfToken pdfToken : pdfTokens) {
      ret.add(unconvertOne(pdfToken));
    }
    return ret;
  }

  /**
   * <p>
   * Converts a {@link Tok} instance back to a PDFBox object ({@link CODBase} or
   * {@link PDFOperator}).
   * </p>
   * 
   * @param pdfToken
   *          A {@link PdfToken} instance that is really a {@link Tok} instance.
   * @return A PDFBox object ({@link CODBase} or {@link PDFOperator}).
   * @since 1.74.4
   */
  public static Object unconvertOne(PdfToken pdfToken) {
    return ((Tok)pdfToken).toPdfBoxObject();
  }

}
