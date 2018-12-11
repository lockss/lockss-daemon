package org.lockss.pdf.pdfbox;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.util.*;
import org.lockss.pdf.*;
import org.lockss.util.FileBackedList;

public class PdfBoxTokenFactory implements PdfTokenFactory {

  public interface Converter<T> {
    
    T convert(Object obj);
    
  }
  
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
    
    public abstract Object toPdfBoxObject();
    
  }
  
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
    public Object toPdfBoxObject() {
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
    public Object toPdfBoxObject() {
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
    public Object toPdfBoxObject() {
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
        mapping.put(getInstance().makeName(entry.getKey().getName()).getName(), convertOne(entry.getValue()));
      }
      return of(mapping);
    }
    
  }
  
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
    public Object toPdfBoxObject() {
      return new COSFloat(value);
    }
    
    public static Flo of(float value) {
      return new Flo(value);
    }
    
    public static Flo of(COSFloat cosFloat) {
      return of(cosFloat.floatValue());
    }
    
  }
  
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
    public Object toPdfBoxObject() {
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
    public Object toPdfBoxObject() {
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
    
    private static Map<String, Nam> lru = new LRUMap<String, Nam>(8192);
    
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
  
  public static class Nul extends Tok {
    
    private Nul() {
      // intentionally left blank
    }

    @Override
    public boolean isNull() {
      return true;
    }

    @Override
    public Object toPdfBoxObject() {
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
    public Object toPdfBoxObject() {
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
    public Object toPdfBoxObject() {
      return new COSString(value);
    }
    
    public static Str of(String value) {
      return new Str(value);
    }
    
    public static Str of(COSString cosString) {
      return of(cosString.getString());
    }
    
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
  public PdfToken makeObject(PdfToken value) {
    throw new UnsupportedOperationException(); // FIXME
  }

  @Override
  public PdfToken makeOperator(String operator) {
    return Op.of(operator);
  }

  @Override
  public PdfToken makeString(String value) {
    return Str.of(value);
  }
  
  private static final PdfBoxTokenFactory instance = new PdfBoxTokenFactory();

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
    convertToPdfToken.put(COSNull.class, new Converter<PdfToken>() {
      @Override
      public PdfToken convert(Object obj) {
        COSObject cosObject = (COSObject)obj;
        return getInstance().makeObject(convertOne(cosObject.getCOSObject())); // FIXME
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
  
  public static PdfBoxTokenFactory getInstance() {
    return instance;
  }
  
  public static PdfToken convertOne(Object pdfBoxObject) {
    if (pdfBoxObject == null) {
      return getInstance().makeNull();
    }
    Converter<PdfToken> converter = convertToPdfToken.get(pdfBoxObject.getClass());
    if (converter == null) {
      throw new IllegalStateException("Encountered a token of unexpected type: " + pdfBoxObject.getClass().getCanonicalName());
    }
    return converter.convert(pdfBoxObject);
  }
  
  public static List<PdfToken> convertList(List<Object> pdfBoxObjects) {
    List<PdfToken> ret = new ArrayList<PdfToken>(pdfBoxObjects.size());
    for (Object pdfBoxObject : pdfBoxObjects) {
      ret.add(convertOne(pdfBoxObject));
    }
    return ret;
  }
  
  public static Object unconvertOne(PdfToken pdfToken) {
    return ((Tok)pdfToken).toPdfBoxObject();
  }
  
  /* not unlimited! */
  public static List<Object> unconvertList(List<PdfToken> pdfTokens) {
    List<Object> ret = new ArrayList<Object>(pdfTokens.size());
    for (PdfToken pdfToken : pdfTokens) {
      ret.add(unconvertOne(pdfToken));
    }
    return ret;
  }

  public static void main(String[] args) throws Exception {
    class MyClass implements Serializable {
      private double foo;
      private long bar;
      MyClass(double foo, long bar) {
        this.foo = foo;
        this.bar = bar;
      }
//      private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException
//      {      
//        this.foo = s.readDouble();
//        this.bar = s.readLong();
//      }
//   
//      private void writeObject(ObjectOutputStream s) throws IOException
//      {
//        s.writeDouble(this.foo);
//        s.writeLong(this.bar);
//      }
    }
    MyClass x = new MyClass(1.2, 3L);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(x);
    System.out.println(baos.size());
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    MyClass y = (MyClass)ois.readObject();
    System.out.println(y.foo);
    System.out.println(y.bar);
  }
  
}
