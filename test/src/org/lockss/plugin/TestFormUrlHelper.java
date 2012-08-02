package org.lockss.plugin;

import org.lockss.test.LockssTestCase;
import org.lockss.util.UrlUtil;

import java.util.Random;

public class TestFormUrlHelper extends LockssTestCase {

	
	public void testNoKeys() {
		FormUrlHelper helper = new FormUrlHelper("action");
		assertEquals(helper.toString(), "action?");
	}
	public void testOneKeys() {
		FormUrlHelper helper = new FormUrlHelper("action");
		helper.add("key","value");
		assertEquals(helper.toString(), "action?key=value");
	}
	public void testTwoKeys() {
		FormUrlHelper helper = new FormUrlHelper("action");
		helper.add("key","value");
		helper.add("key","value");
		assertEquals(helper.toString(), "action?key=value&key=value");
	}
	public void testTwoKeysLimitZero() {
		FormUrlHelper helper = new FormUrlHelper("action");
		helper.add("key","value");
		helper.add("key","value");
		helper.applyLimit("key",0);
		assertEquals(helper.toString(), "action?");
		helper.convertFromEncodedString(helper.toEncodedString());
		assertEquals(helper.toEncodedString(), "action?");		
	}
	public void testTwoKeysLimitOne() {
		FormUrlHelper helper = new FormUrlHelper("action");
		helper.add("key","value1");
		helper.add("key","value2");
		helper.applyLimit("key",1);
		assertEquals(helper.toString(), "action?key=value1");
		helper.convertFromEncodedString(helper.toEncodedString());
		assertEquals(helper.toEncodedString(), "action?key=value1");
	}
	public void testTwoKeysLimitOneSorted() {
		FormUrlHelper helper = new FormUrlHelper("action");
		helper.add("key","value2");
		helper.add("key","value1");
		helper.sortKeyValues();
		helper.applyLimit("key",1);
		assertEquals(helper.toString(), "action?key=value1");
		helper.convertFromEncodedString(helper.toEncodedString());
		assertEquals(helper.toEncodedString(), "action?key=value1");
	}
	public void testTwoKeysComplex() {
		FormUrlHelper helper = new FormUrlHelper("action");
		helper.add("key","val ue");
		helper.add("key","value=");
		
		assertEquals(helper.toEncodedString(), "action?key=val+ue&key=value%3D");
		helper.convertFromEncodedString(helper.toEncodedString());
		assertEquals(helper.toEncodedString(), "action?key=val+ue&key=value%3D");		
	}
	
	public void testRandomStrings() {	
		RandomString a =  new RandomString();
		String action = "http://www.example.com/F2";
		FormUrlHelper helper = new FormUrlHelper(action);
		
		for  (int i=0;i<10;i++) {
			String name = a.nextString(20);
			String value = a.nextString(20);
			helper.add(name, value);	
		}
		assertEquals(helper.toString(),"http://www.example.com/F2?xewCkvklCEj>az=g@0ax=nniAik86AEE@cAr9@Dn?&ozdAf>14A77DrsBslu7h==hECiyi1xmmbjB=rB1i6&vnj2nFrC=5C1xaDoz?Ao==ahpmqqDA4qaqhs4z4=e&>3Fe6iv35B=4A7kru6ni=bg48Ezisomgv6d@9oDyw&?=F97dt?=c=x59m=1pom=tuwl@Argsq0d@yxuookD&uBqe5cb20snlj?7fvFEm=iyxjAumx>k7>n3EC=mez&ggcnvtavipqDtjo0ldiF=DuqhBi6@De3um19mEFEA&2nvA?gnF1>zy3Dpgsv8b=b4v6lgCuB91x1tt72Aw5&a?vwsvwt6m8u4k34B4rr==4ub7dB2E7y@zyCx1ae4&C@Flnpa59luboCwz9>7n=e3qEbC1yE0300ja5Dwpb");
		assertEquals(helper.toEncodedString(),"http://www.example.com/F2?xewCkvklCEj%3Eaz%3Dg%400ax=nniAik86AEE%40cAr9%40Dn%3F&ozdAf%3E14A77DrsBslu7h=%3DhECiyi1xmmbjB%3DrB1i6&vnj2nFrC%3D5C1xaDoz%3FAo=%3DahpmqqDA4qaqhs4z4%3De&%3E3Fe6iv35B%3D4A7kru6ni=bg48Ezisomgv6d%409oDyw&%3F%3DF97dt%3F%3Dc%3Dx59m%3D1pom=tuwl%40Argsq0d%40yxuookD&uBqe5cb20snlj%3F7fvFEm=iyxjAumx%3Ek7%3En3EC%3Dmez&ggcnvtavipqDtjo0ldiF=DuqhBi6%40De3um19mEFEA&2nvA%3FgnF1%3Ezy3Dpgsv8b=b4v6lgCuB91x1tt72Aw5&a%3Fvwsvwt6m8u4k34B4rr=%3D4ub7dB2E7y%40zyCx1ae4&C%40Flnpa59luboCwz9%3E7n=e3qEbC1yE0300ja5Dwpb");
		helper.convertFromEncodedString(helper.toEncodedString());
		assertEquals(helper.toEncodedString(),"http://www.example.com/F2?xewCkvklCEj%3Eaz%3Dg%400ax=nniAik86AEE%40cAr9%40Dn%3F&ozdAf%3E14A77DrsBslu7h=%3DhECiyi1xmmbjB%3DrB1i6&vnj2nFrC%3D5C1xaDoz%3FAo=%3DahpmqqDA4qaqhs4z4%3De&%3E3Fe6iv35B%3D4A7kru6ni=bg48Ezisomgv6d%409oDyw&%3F%3DF97dt%3F%3Dc%3Dx59m%3D1pom=tuwl%40Argsq0d%40yxuookD&uBqe5cb20snlj%3F7fvFEm=iyxjAumx%3Ek7%3En3EC%3Dmez&ggcnvtavipqDtjo0ldiF=DuqhBi6%40De3um19mEFEA&2nvA%3FgnF1%3Ezy3Dpgsv8b=b4v6lgCuB91x1tt72Aw5&a%3Fvwsvwt6m8u4k34B4rr=%3D4ub7dB2E7y%40zyCx1ae4&C%40Flnpa59luboCwz9%3E7n=e3qEbC1yE0300ja5Dwpb");
	}
		
	//helper method to generate random strings
	public class RandomString
	{

	  private char[] symbols = new char[46];

	  private final Random _random = new Random(8081); //use a fixed seed for consistency

	  public RandomString()
	  {
		  int i;
		    for (i = 0; i < 10; ++i)
		      symbols[i] = (char) ('0' + i);
		    for (i = 10; i < 26+10; ++i)
		      symbols[i] = (char) ('a' + i - 10);
		    for (i = 26+10; i < 26+10+10; ++i)
			  symbols[i] = (char) ('=' + i - 26 - 10);
	  }

	  public String nextString(int length)
	  {
		char[] buf = new char[length];
	    for (int i = 0; i < buf.length; i++) 
	      buf[i] = symbols[_random.nextInt(symbols.length)];
	    return new String(buf);
	  }

	}

}
