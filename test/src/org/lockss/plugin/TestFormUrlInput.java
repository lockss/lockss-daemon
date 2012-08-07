package org.lockss.plugin;

import org.lockss.test.LockssTestCase;


public class TestFormUrlInput extends LockssTestCase {

	private String simple_name = "key";
	private String simple_value = "value";
	private String complex_name = "key!@#$%^&*()-= []{},./`\\|"; //note:\\needed for java escaping
	private String complex_value = "value!@#$%^&*()-= []{},./`\\|";
	private FormUrlInput _simple;
	private FormUrlInput _complex;
	
	public void setUp() throws Exception {
		super.setUp();
		_simple = new FormUrlInput(simple_name, simple_value);
	    _complex = new FormUrlInput(complex_name, complex_value);
	}
	
//create a simple form url input and check the values returned
	public void testFormUrlInput() {
		FormUrlInput a = new FormUrlInput("key","value");

		assertEquals(a.getEncodedName(),"key");
		assertEquals(a.getEncodedValue(),"value");
		assertEquals(a.getRawName(),"key");
		assertEquals(a.getRawValue(),"value");
	}

	//a null value is converted to the empty string
	public void testNullValue() {
		FormUrlInput a = new FormUrlInput("key",null);
		assertEquals(a.getEncodedValue(),"");
	}
	
	public void testGetRawName() {
		assertEquals(_complex.getRawName(),complex_name);
	}

	public void testGetRawValue() {
		assertEquals(_complex.getRawValue(),complex_value);
	}

	public void testGetName() {
//		assertEquals(_complex.getName(), UrlUtil.encodeUrl(complex_name));
	}

	public void testGetValue() {
		assertEquals(_complex.getEncodedValue(), "value%21%40%23%24%25%5E%26*%28%29-%3D+%5B%5D%7B%7D%2C.%2F%60%5C%7C");
	}
 
	public void testIOP() {
		FormUrlInput a = new FormUrlInput("navsubmit","Export Results");
		assertEquals(a.toString(),"navsubmit=Export+Results");
	}
	
	public void testToString() {
		assertEquals(_simple.toString(), simple_name+"="+simple_value);
		assertEquals(_complex.toString(), _complex.getEncodedName()+"="+_complex.getEncodedValue());
	}

	public void testCompareTo() {
		
			FormUrlInput a = new FormUrlInput("a key","value");
		FormUrlInput b = new FormUrlInput("b key","value");
		FormUrlInput c = new FormUrlInput("c key","value");

		assert( a.compareTo(b) < 0 );
		assert( b.compareTo(c) < 0 );
		assert( a.compareTo(a) == 0);
		assert( b.compareTo(a) < 0 );
		assert( c.compareTo(b) < 0 );
				
	}
	public void testCompareToValues() {
		FormUrlInput a = new FormUrlInput("key","d value");
		FormUrlInput b = new FormUrlInput("key","e value");
		FormUrlInput c = new FormUrlInput("key","f value");

		assert( a.compareTo(b) < 0 );
		assert( b.compareTo(c) < 0 );
		assert( a.compareTo(a) == 0);
		assert( b.compareTo(a) < 0 );
		assert( c.compareTo(b) < 0 );

	}	
}
