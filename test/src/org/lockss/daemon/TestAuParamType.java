/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.daemon;

import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.AuParamType.InvalidFormatException;    


public class TestAuParamType extends LockssTestCase {

  public void testParse() throws Exception {

    // String
    assertEquals("foo", AuParamType.String.parse("foo"));
    try {
      AuParamType.String.parse("");
      fail("String.parse(\"\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.String.parse(null);
      fail("String.parse(null) should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // Int
    assertEquals(123, AuParamType.Int.parse("123"));
    assertEquals(-432, AuParamType.Int.parse("-00432"));
    try {
      AuParamType.Int.parse("");
      fail("Int.parse(\"\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Int.parse("abc");
      fail("Int.parse(\"abc\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Int.parse("123456789000");
      fail("Int.parse(\"123456789000\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // Long
    assertEquals(123L, AuParamType.Long.parse("123"));
    assertEquals(-432L, AuParamType.Long.parse("-00432"));
    assertEquals(123456789000L, AuParamType.Long.parse("123456789000"));
    try {
      AuParamType.Long.parse("");
      fail("Long.parse(\"\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Long.parse("abc");
      fail("Long.parse(\"abc\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // PosInt
    assertEquals(123, AuParamType.PosInt.parse("123"));
    try {
      AuParamType.PosInt.parse("-444");
      fail("PosInt.parse(\"-444\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.PosInt.parse("");
      fail("PosInt.parse(\"\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.PosInt.parse("abc");
      fail("PosInt.parse(\"abc\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // Year
    assertEquals(1234, AuParamType.Year.parse("1234"));
    assertEquals(0, AuParamType.Year.parse("0"));
    try {
      AuParamType.Year.parse("-123");
      fail("Year.parse(\"-123\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Year.parse("");
      fail("Year.parse(\"\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Year.parse("abc");
      fail("Year.parse(\"abc\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // Boolean
    assertEquals(true, AuParamType.Boolean.parse("true"));
    assertEquals(true, AuParamType.Boolean.parse("True"));
    assertEquals(true, AuParamType.Boolean.parse("yes"));
    assertEquals(true, AuParamType.Boolean.parse("ON"));
    assertEquals(true, AuParamType.Boolean.parse("1"));
    assertEquals(false, AuParamType.Boolean.parse("false"));
    assertEquals(false, AuParamType.Boolean.parse("False"));
    assertEquals(false, AuParamType.Boolean.parse("no"));
    assertEquals(false, AuParamType.Boolean.parse("oFF"));
    assertEquals(false, AuParamType.Boolean.parse("0"));
    try {
      AuParamType.Boolean.parse("");
      fail("Boolean.parse(\"\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Boolean.parse("abc");
      fail("Boolean.parse(\"abc\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Boolean.parse("123");
      fail("Boolean.parse(\"123\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // TimeInterval
    assertEquals(123L, AuParamType.TimeInterval.parse("123"));
    assertEquals(5 * Constants.MINUTE, AuParamType.TimeInterval.parse("5m"));
    assertEquals(3 * Constants.HOUR, AuParamType.TimeInterval.parse("3h"));
    // This should probably be illegal
    assertEquals(-444L, AuParamType.TimeInterval.parse("-444"));
    try {
      AuParamType.TimeInterval.parse("");
      fail("TimeInterval.parse(\"\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.TimeInterval.parse("abc");
      fail("TimeInterval.parse(\"abc\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    String url1 = "http://example.com/path";

    // Url
    assertEquals(new URL(url1), AuParamType.Url.parse(url1));
    assertEquals(new URL(url1), AuParamType.Url.parse(" " + url1));
    try {
      AuParamType.Url.parse("not a url");
      fail("Url.parse(\"not a url\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Url.parse("");
      fail("Url.parse(\"\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Url.parse("abc");
      fail("Url.parse(\"abc\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // Range
    assertEquals(ListUtil.list("bar", "foo"),
		 AuParamType.Range.parse("bar-foo"));
    assertEquals(ListUtil.list("foo", "foo"),
		 AuParamType.Range.parse("foo-foo"));

    // Singleton is equivalent to range w/ same min and max
    assertEquals(ListUtil.list("bar", "bar"), AuParamType.Range.parse("bar"));
    try {
      AuParamType.Range.parse("foo-");
      fail("Range.parse(\"foo-bar\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Range.parse("-foo");
      fail("Range.parse(\"foo-bar\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.Range.parse("foo-bar");
      fail("Range.parse(\"foo-bar\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // NumRange
    assertEquals(ListUtil.list(1L, 99L), AuParamType.NumRange.parse("1-99"));
    assertEquals(ListUtil.list(1L, 1L), AuParamType.NumRange.parse("1-1"));
    assertEquals(ListUtil.list(1L, 2L), AuParamType.NumRange.parse("1-02"));
    assertEquals(ListUtil.list(8L, 9L), AuParamType.NumRange.parse("08-09"));
    assertEquals(ListUtil.list(1L, 22L), AuParamType.NumRange.parse("1-022"));
    assertEquals(ListUtil.list(4L, 4L), AuParamType.NumRange.parse("4"));
    assertEquals(ListUtil.list("foo", "foo"),
		 AuParamType.NumRange.parse("foo-foo"));

    try {
      AuParamType.NumRange.parse("a-42");
      fail("NumRange.parse(\"a-42\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.NumRange.parse("12-");
      fail("NumRange.parse(\"foo-bar\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.NumRange.parse("-12");
      fail("NumRange.parse(\"foo-bar\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }

    // Set
    assertEquals(ListUtil.list("foo"),
		 AuParamType.Set.parse("foo"));
    assertEquals(ListUtil.list("foo", "bar"),
		 AuParamType.Set.parse("foo, bar"));
    assertEquals(ListUtil.list("foo", "bar", "1"),
		 AuParamType.Set.parse(" foo , bar   ,1 "));

    assertEquals(ListUtil.list("1", "1b", "2", "3", "4", "5", "6", "6A", "6B",
			       "7000", "7001", "7002"),
		 AuParamType.Set.parse(" 1, 1b, {2-6} , 6A,6B ,{7000-7002}"));
    assertEquals(ListUtil.list("1", "2", "3", "5", "6", "8", "9", "11"),
		 AuParamType.Set.parse(" { 1 - 3 }, {5-6} , { 8-9  } ,{11-11}"));
    assertEquals(ListUtil.list("1", "{-2}"), AuParamType.Set.parse("1,{-2}"));
    assertEquals(ListUtil.list("1"), AuParamType.Set.parse("1,"));
    assertEquals(ListUtil.list("foo"), AuParamType.Set.parse("foo,"));
    Object largeSet = AuParamType.Set.parse("1,{0-100000}");
    assertEquals(10000, ((Collection)largeSet).size());

    // UserPasswd
    assertEquals(ListUtil.list("foo", "bar"), 
		 AuParamType.UserPasswd.parse("foo:bar"));
    assertEquals(ListUtil.list("foo", "bar:colon"),
		 AuParamType.UserPasswd.parse("foo:bar:colon"));
    try {
      AuParamType.UserPasswd.parse("foo");
      fail("UserPasswd.parse(\"foo\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.UserPasswd.parse("foo:");
      fail("UserPasswd.parse(\"foo:\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
    try {
      AuParamType.UserPasswd.parse(":foo");
      fail("UserPasswd.parse(\"foo:\") should throw InvalidFormatException");
    } catch (InvalidFormatException expected) {
    }
  }
}
