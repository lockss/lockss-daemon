#!/usr/bin/env python

# $Id: tdb_test.py,v 1.1 2011-02-17 23:14:25 barry409 Exp $

# Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
# all rights reserved.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
# STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
# IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# Except as contained in this notice, the name of Stanford University shall not
# be used in advertising or otherwise to promote the sale, use or other dealings
# in this Software without prior written authorization from Stanford University.

__version__ = '''0.0.1'''

import unittest
import tdb

class TestMap(unittest.TestCase):
    
    def testSimple(self):
        """Test simple put/get"""
        m = tdb._Map()
        # What goes in is what comes out.
        m.set('key', 'value')
        self.assertEquals(m.get('key'), 'value')
        # Singletons and atoms work as keys.
        self.assertEquals(m.get(('key')), 'value')
        self.assertEquals(m.get('foo'), None)
        # Values are write-once
        self.assertRaises(tdb.MapError, m.set, 'key', 'value')
    
    def testParams(self):
        """Test multiple-level put/get"""
        m = tdb._Map()
        m.set(('param', 'foo'), 'bar')
        self.assertEquals(m.get('param'), {'foo': 'bar'})
        self.assertEquals(m.get(('param', 'foo')), 'bar')
        m.set(('param', 'fee', 'baz'), 'bar')
        m.set(('param', 'yab', 'dab'), 'doo')
        self.assertEquals(m.get('param'), {'fee': {'baz': 'bar'}, 'foo': 'bar', 'yab': {'dab': 'doo'}})
    
    def testArgs(self):
        """Test that correct exceptions happen when top-level args are not valid"""
        m = tdb._Map()
        self.assertRaises(tdb.MapError, m.set, (), 'yab')
        self.assertRaises(tdb.MapError, m.get, ())
        self.assertRaises(tdb.MapError, m.set, 'foo', {'yab': 'dab'})
        self.assertRaises(tdb.MapError, m.set, 'foo', None)
        
    def testMismatch(self):
        '''shouldn't be able to set a value and try to access it as an array, or
        the reverse.'''
        m = tdb._Map()
        m.set('foo', 'bar')
        self.assertRaises(tdb.MapError, m.get, ('foo', 'foo'))
        # having both param['foo'] and param['foo']['bar'] isn't permitted
        m.set(('param', 'foo', 'baz'), 'bar')
        self.assertRaises(tdb.MapError, m.set, ('param', 'foo'), 'yab')
        m.set(('param', 'doo'), 'yab')
        self.assertRaises(tdb.MapError, m.set, ('param', 'doo', 'baz'), 'bar')

class TestChainedMap(unittest.TestCase):
    
    def testShadowing(self):
        '''Test that shadowing works correctly'''
        outer = tdb._ChainedMap()
        outer.set('foo', 'bar')
        self.assertEquals(outer.get('foo'), 'bar')
        inner = tdb._ChainedMap(outer)
        # Before inner overrides, it inherits
        self.assertEquals(inner.get('foo'), 'bar')
        # Now it's overridden
        inner.set('foo', 'baz')
        self.assertEquals(outer.get('foo'), 'bar')
        self.assertEquals(inner.get('foo'), 'baz')
        # Likewise a tree.
        m3 = tdb._ChainedMap(outer)
        m3.set('foo', 'yab')
        self.assertEquals(outer.get('foo'), 'bar')
        self.assertEquals(inner.get('foo'), 'baz')
        self.assertEquals(m3.get('foo'), 'yab')
    
    def testShadowingArray(self):
        outer = tdb._ChainedMap()
        inner = tdb._ChainedMap(outer)
        outer.set(('params', 'foo'), 'bar')
        inner.set(('params', 'foo'), 'baz')
        self.assertEquals(inner.get(('params', 'foo')), 'baz')
        self.assertEquals(outer.get(('params', 'foo')), 'bar')
        # Setting in outer is visible to inner
        outer.set(('params', 'yab'), 'dab')
        self.assertEquals(inner.get('params'), {'foo': 'baz', 'yab': 'dab'})
        
    def testShadowing3(self):
        outer = tdb._ChainedMap()
        inner = tdb._ChainedMap(outer)
        outer.set(('params', 'doo', 'foo'), 'bar')
        inner.set(('params', 'doo', 'foo'), 'baz')
        self.assertEquals(inner.get(('params', 'doo')), {'foo': 'baz'})
        self.assertEquals(outer.get(('params', 'doo')), {'foo': 'bar'})
        self.assertEquals(inner.get('params'), {'doo': {'foo': 'baz'}})
        # Setting in outer is visible to inner
        outer.set(('params', 'doo', 'yab'), 'dab')
        self.assertEquals(inner.get(('params', 'doo')), {'foo': 'baz', 'yab': 'dab'})
        self.assertEquals(inner.get(('params')), {'doo': {'foo': 'baz', 'yab': 'dab'}})
        
    def testMismatchedChild(self):
        '''Test that any setting of an inner scope's value checks the outer
        scope to make sure it's a leaf.'''
        outer = tdb._ChainedMap()
        inner = tdb._ChainedMap(outer)
        outer.set(('params', 'foo'), 'bar')
        self.assertRaises(tdb.MapError, inner.set, 'params', 'baz')
        outer.set('xparams', 'bar')
        self.assertRaises(tdb.MapError, inner.set, ('xparams', 'foo'), 'baz')
        outer.set(('foo', 'bar', 'baz'), 'foo')
        self.assertRaises(tdb.MapError, inner.set, ('foo', 'bar'), 'baz')
        outer.set(('fee', 'bar'), 'foo')
        self.assertRaises(tdb.MapError, inner.set, ('fee', 'bar', 'baz'), 'baz')

    '''
    There are no backpointers from the outer scope to all the inner scopes it contains.
    When an outer scope's value is set, an inner scope might already have it defined as
    a non-leaf, but we can't check.

    def testMismatchedParent(self):
        # Test that any setting of an outer scope's value checks the inner
        # scopes to make sure it's a leaf.
        outer = tdb._ChainedMap()
        inner = tdb._ChainedMap(outer)
        inner.set(('params', 'foo'), 'bar')
        self.assertRaises(tdb.MapError, outer.set, 'params', 'baz')
        inner.set('xparams', 'bar')
        self.assertRaises(tdb.MapError, outer.set, ('xparams', 'foo'), 'baz')
    '''
    
    def testMismatchedParent2(self):
        outer = tdb._ChainedMap()
        inner = tdb._ChainedMap(outer)
        inner.set(('xparams', 'foo'), 'bar')
        outer.set('xparams', 'baz')
        # see testMismatchedParent: it's a bug that this doesn't
        # raise tdb.MapError. But the 'get' does raise it.
        self.assertRaises(tdb.MapError, inner.get, 'xparams')
        inner.set(('yparams', 'foo', 'bar', 'baz'), 'foo')
        outer.set(('yparams', 'foo', 'bar'), 'foo')
        self.assertRaises(tdb.MapError, inner.get, 'yparams')
        inner.set(('zparams', 'foo', 'bar'), 'foo')
        outer.set(('zparams', 'foo', 'bar', 'baz'), 'foo')
        self.assertRaises(tdb.MapError, inner.get, 'zparams')
   
    def testCombine(self):
        outer = tdb._ChainedMap()
        inner = tdb._ChainedMap(outer)
        inner.set(('params', 'doo', 'foo'), 'baz')
        inner.set(('params', 'doo', 'dum'), 'foo')
        inner.set(('params', 'dee'), 'bax')
        outer.set(('params', 'fip'), 'foo')
        self.assertEquals(inner.get('params'), {'fip': 'foo',
                                                'doo': {'foo': 'baz', 'dum': 'foo'},
                                                'dee': 'bax'})
        outer.set(('params', 'doo', 'foo'), 'bar') # shadowed
        outer.set(('params', 'doo', 'fee'), 'bar') # new
        self.assertEquals(inner.get('params'), {'fip': 'foo',
                                                'doo': {'fee': 'bar', 'foo': 'baz', 'dum': 'foo'},
                                                'dee': 'bax'})

class TestAU(unittest.TestCase):

    def testAuid(self):
        for st, par in [('a~b&c~d', {'a': 'b', 'c': 'd'}),
                       ('a~b&c~d', {'c': 'd', 'a': 'b'}),
                       ('base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~123', {'base_url': 'http://www.example.com/', 'volume_name': '123'})]:
            self.assertEquals('org|lockss|plugin|FooPlugin&' + st, tdb.AU.computeAuid('org.lockss.plugin.FooPlugin', par))

if __name__ == '__main__': unittest.main()
