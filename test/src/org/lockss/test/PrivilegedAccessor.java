package org.lockss.test;
import java.lang.reflect.*;

/**
 * a.k.a. The "ObjectMolester"
 * <p>
 * This class is used to access a method or field of an object no
 * matter what the access modifier of the method or field.  The syntax
 * for accessing fields and methods is out of the ordinary because this
 * class uses reflection to peel away protection.
 * <p>
 * Here is an example of using this to access a private member.
 * <code>resolveName</code> is a private method of <code>Class</code>.
 *
 * <pre>
 * Class c = Class.class;
 * System.out.println(
 *      PrivilegedAccessor.invokeMethod( c,
 *                                       "resolveName",
 *                                       "/net/iss/common/PrivilegeAccessor" ) );
 * </pre>
 *
 * @author Charlie Hubbard (chubbard@iss.net)
 * @author Prashant Dhokte (pdhokte@iss.net)
 */

public class PrivilegedAccessor {
  
  /**
   * Gets the value of the named field and returns it as an object.
   *
   * @param instance the object instance
   * @param fieldName the name of the field
   * @return an object representing the value of the field
   */
  public static Object getValue( Object instance, String fieldName )                
      throws IllegalAccessException, NoSuchFieldException {
    Field field = getField(instance.getClass(), fieldName);
    field.setAccessible(true);
    return field.get(instance);
  }
  
  /**
   * Calls a method on the given object instance with the given argument.
   *
   * @param instance the object instance
   * @param methodName the name of the method to invoke
   * @param arg the argument to pass to the method
   * @see PrivilegedAccessor#invokeMethod(Object,String,Object[])
   */
  public static Object invokeMethod( Object instance, String methodName, Object arg )
      throws NoSuchMethodException,
    IllegalAccessException, InvocationTargetException  {
    Object[] args = new Object[1];
    args[0] = arg;
    return invokeMethod(instance, methodName, args);
  }
  
  /**
   * Calls a method on the given object instance with the given arguments.
   *
   * @param instance the object instance
   * @param methodName the name of the method to invoke
   * @param args an array of objects to pass as arguments
   * @see PrivilegedAccessor#invokeMethod(Object,String,Object)
   */
  public static Object invokeMethod( Object instance, String methodName, Object[] args ) 
      throws NoSuchMethodException,
    IllegalAccessException, InvocationTargetException  {
    Class[] classTypes = null;
    if( args != null) {
      classTypes = new Class[args.length];
      for( int i = 0; i < args.length; i++ ) {
	if( args[i] != null ){
	  Class curClass = args[i].getClass();
	  if(NullClass.isNullClass(curClass)){
	    classTypes[i] = ((NullClass)args[i]).getFakeClass();
	  }
	  else{
	    classTypes[i] = curClass;
	  }
	}
	else{
 	  throw new NoSuchMethodException("Can't match null parameters "+
 					  "to a method.");
	}
      }
    }
    Method method = getMethod(instance,methodName,classTypes);
    //    return method.invoke(instance,args);
    return method.invoke(instance,resolveNulls(args));
  }
  
  private static Object[] resolveNulls(Object[] objs){
    for (int ix=0; ix<objs.length; ix++){
      if (NullClass.isNullClass(objs[ix].getClass())){
	objs[ix] = null;
      }
    }
    return objs;
  }


  /**
   *
   * @param instance the object instance
   * @param methodName the
   */
  public static Method getMethod( Object instance, String methodName, Class[] classTypes ) 
      throws NoSuchMethodException {
    Method accessMethod = getMethod(instance.getClass(), methodName, classTypes);
    accessMethod.setAccessible(true);
    return accessMethod;
  }
  
  /**
   * Return the named field from the given class.
   */
  private static Field getField(Class thisClass, String fieldName) 
      throws NoSuchFieldException {
    if (thisClass == null)
      throw new NoSuchFieldException("Invalid field : " + fieldName);
    try {
      return thisClass.getDeclaredField( fieldName );
    }
    catch(NoSuchFieldException e) {
      return getField(thisClass.getSuperclass(), fieldName);
    }
  }
  
  /**
   * Return the named method with a method signature matching classTypes
   * from the given class.
   */
  private static Method getMethod(Class thisClass, String methodName, Class[] classTypes) throws NoSuchMethodException {
    if (thisClass == null){
      throw new NoSuchMethodException("Invalid method : " + methodName);
    }
    try {
      return thisClass.getDeclaredMethod( methodName, classTypes );
    }
    catch(NoSuchMethodException e) {
      return getMethod(thisClass.getSuperclass(), methodName, classTypes);
    }
  }

  public static class NullClass{
    private Class myClass = null;

    protected NullClass(){
    }
    
    public static boolean isNullClass(Class curClass){
      if (curClass.isInstance(new NullClass())){
	return true;
      }
      return false;
    }
  

    public NullClass(String className) throws ClassNotFoundException{
      myClass = Class.forName(className);
    }
    
    public Class getFakeClass(){
      return myClass;
    }
  }
}
