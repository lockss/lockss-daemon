package org.lockss.test;
import junit.framework.*;

public class TestPrivilegedAccessor extends TestCase {

  public void testParent() throws Exception {
    MockParent parent = new MockParent("Charlie");
    assertEquals(PrivilegedAccessor.getValue(parent, "m_name"), "Charlie");

    PrivilegedAccessor.invokeMethod(parent, "setName", "Timmah!");

    assertEquals(PrivilegedAccessor.getValue(parent,"m_name"), "Timmah!");
  }

  public void testChild() throws Exception {
    MockChild child = new MockChild("Charlie",8);
    assertEquals(PrivilegedAccessor.getValue(child, "m_name"), "Charlie");
    assertEquals(PrivilegedAccessor.getValue(child, "m_number"), new Integer(8));

    PrivilegedAccessor.invokeMethod(child, "setName", "Timmah!");
    PrivilegedAccessor.invokeMethod(child, "setNumber", new Integer(3));

    assertEquals(PrivilegedAccessor.getValue(child,"m_name"), "Timmah!");
    assertEquals(PrivilegedAccessor.getValue(child, "m_number"), new Integer(3));
  }

  public void testChildWithParentReference() throws Exception {
    MockParent parent = new MockChild("Charlie",8);
    assertEquals(PrivilegedAccessor.getValue(parent, "m_name"), "Charlie");
    assertEquals(PrivilegedAccessor.getValue(parent, "m_number"), new Integer(8));

    Object args[] = {"Timmah!", new Integer(3)};
    PrivilegedAccessor.invokeMethod(parent, "setData", args);

    assertEquals(PrivilegedAccessor.getValue(parent,"m_name"), "Timmah!");
    assertEquals(PrivilegedAccessor.getValue(parent, "m_number"), new Integer(3));

    PrivilegedAccessor.invokeMethod(parent, "setName", "prashant");
    assertEquals(PrivilegedAccessor.getValue(parent,"m_name"), "prashant");
  }

  public void testInvalidField() throws Exception {
    MockParent parent = new MockParent("Charlie");
    try {
      Object value = PrivilegedAccessor.getValue(parent, "zzz");
    }
    catch(NoSuchFieldException e) {
      return;
    }
    fail("Should throw NoSuchFieldException");
  }

  public void testInvalidMethodName() throws Exception {
    MockChild child = new MockChild("Charlie",8);
    try {
      PrivilegedAccessor.invokeMethod(child, "zzz", "Timmah!");
    }
    catch(NoSuchMethodException e) {
      return;
    }
    fail("Should throw NoSuchMethodException");
  }

  public void testInvalidArguments() throws Exception {
    MockChild child = new MockChild("Charlie",8);
    try {
      PrivilegedAccessor.invokeMethod(child, "setData", "Timmah!");
    }
    catch(NoSuchMethodException e) {
      return;
    }
    fail("Should throw NoSuchMethodException");
  }

  public void testNullClassParam() throws Exception{
    MockParent parent = new MockParent();
    Object nullString = new PrivilegedAccessor.NullClass("java.lang.String");
    Boolean bool = 
      (Boolean)PrivilegedAccessor.invokeMethod(parent, "isNullString", 
					       nullString);
    assertTrue(bool.booleanValue());

  }

  public void testThrowsExceptionOnRealNullParam() throws Exception{
    MockParent parent = new MockParent();
    Object nullString = new PrivilegedAccessor.NullClass("java.lang.String");
    Object[] args = new Object[1];
    args[0] = null;
    try{
      PrivilegedAccessor.invokeMethod(parent, "isNullString", args);
      fail("invokeMethod Didn't throw an exception for a null parameter");
    }
    catch(NoSuchMethodException nsme){
    }
  }


  public TestPrivilegedAccessor( String name ) {
    super( name );
  }
  
  public static void main(String args[]) {
    junit.textui.TestRunner.run(TestPrivilegedAccessor.suite());
  }

  public static Test suite() {
    return new TestSuite(TestPrivilegedAccessor.class);
  }

  // Test utility classes

  public class MockChild extends MockParent {
    private int m_number;

    public MockChild( String name, int number ) {
      super( name );
      m_number = number;
    }

    public int getNumber() {
      return m_number;
    }

    private void setNumber( Integer number ) {
      m_number = number.intValue();
    }
    private void setData( String name, Integer number ) {
      setName(name);
      m_number = number.intValue();
    }
  }

  public class MockParent {
    private String m_name;

    public MockParent() {
    }
    public MockParent(String name) {
      m_name = name;
    }

    public String getName() {
      return m_name;
    }

    protected void setName( String newName ) {
      m_name = newName;
    }

    public boolean isNullString(String str){
      return (str == null);
    }
  }
}
