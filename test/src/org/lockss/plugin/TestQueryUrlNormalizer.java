/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin;

import java.util.Comparator;

import org.apache.commons.collections4.comparators.FixedOrderComparator;
import org.lockss.test.LockssTestCase;

public class TestQueryUrlNormalizer extends LockssTestCase{
    public void testQueryUrlNormalizer() throws Exception{
        UrlNormalizer norm = new QueryUrlNormalizer();
        assertEquals("https://wwww.abc/?a=1&b=2&c=3",norm.normalizeUrl("https://wwww.abc/?a=1&b=2&c=3", null));
        assertEquals("https://wwww.abc/?a=1&b=2&c=3",norm.normalizeUrl("https://wwww.abc/?b=2&c=3&a=1", null));
        assertEquals("https://wwww.abc/?c=2&c=1&c=3",norm.normalizeUrl("https://wwww.abc/?c=2&c=1&c=3", null));
        assertEquals("https://wwww.abc/a=1&b=2&c=3",norm.normalizeUrl("https://wwww.abc/a=1&b=2&c=3", null));
        assertEquals("https://wwww.abc/",norm.normalizeUrl("https://wwww.abc/?", null));
        assertEquals("https://wwww.abc/?a=1&b=&c",norm.normalizeUrl("https://wwww.abc/?a=1&b=&c", null));
    }

    public void testKeyComparator() throws Exception{
        UrlNormalizer norm = new QueryUrlNormalizer(){
            @Override
            public Comparator<String> getKeyComparator() {
                // TODO Auto-generated method stub
                FixedOrderComparator<String> comp = new FixedOrderComparator<String>("volume","issue","page");
                comp.setUnknownObjectBehavior(FixedOrderComparator.UnknownObjectBehavior.AFTER);
                return comp;
            }};
            assertEquals("https://wwww.abc/?a=1&b=2&c=3",norm.normalizeUrl("https://wwww.abc/?a=1&b=2&c=3", null));
            assertEquals("https://wwww.abc/?volume=1&issue=2&page=3",norm.normalizeUrl("https://wwww.abc/?volume=1&issue=2&page=3", null));
            assertEquals("https://wwww.abc/?volume=1&issue=2&page=3",norm.normalizeUrl("https://wwww.abc/?issue=2&volume=1&page=3", null));
            assertEquals("https://wwww.abc/?volume=1&issue=2&page=3&x=y",norm.normalizeUrl("https://wwww.abc/?x=y&volume=1&issue=2&page=3", null));
            assertEquals("https://wwww.abc/?volume=1&issue=2&page=3&a=b&x=y",norm.normalizeUrl("https://wwww.abc/?x=y&volume=1&issue=2&page=3&a=b", null));
    }

    public void testDropKey() throws Exception{
        UrlNormalizer norm = new QueryUrlNormalizer(){
            @Override
            public boolean shouldDropKey(String key){
                return key.equals("b") || key.equals("d");
            }
        };
        assertEquals("https://wwww.abc/?a=1&c=3", norm.normalizeUrl("https://wwww.abc/?a=1&c=3",null));
        assertEquals("https://wwww.abc/?a=1&c=3", norm.normalizeUrl("https://wwww.abc/?a=1&b=2&c=3",null));
        assertEquals("https://wwww.abc/?a=3", norm.normalizeUrl("https://wwww.abc/?d=1&b=2&a=3",null));
        assertEquals("https://wwww.abc/", norm.normalizeUrl("https://wwww.abc/?d=1&b=2&b=3",null));
    }

    public void testDropKeyValue() throws Exception{
        UrlNormalizer norm = new QueryUrlNormalizer(){
            @Override
            public boolean shouldDropKeyValue(String key, String value){
                return key.equals("b") && value.equals("2");
            }
        };
        assertEquals("https://wwww.abc/?a=1&c=3", norm.normalizeUrl("https://wwww.abc/?a=1&c=3",null));
        assertEquals("https://wwww.abc/?a=1&c=3", norm.normalizeUrl("https://wwww.abc/?a=1&b=2&c=3",null));
        assertEquals("https://wwww.abc/?a=1&a=3&b=3", norm.normalizeUrl("https://wwww.abc/?a=1&b=3&a=3",null));
        assertEquals("https://wwww.abc/?b=3&d=1", norm.normalizeUrl("https://wwww.abc/?d=1&b=2&b=3",null));
    }
}
