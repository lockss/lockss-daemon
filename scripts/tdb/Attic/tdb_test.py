#!/usr/bin/env python

# $Id: tdb_test.py,v 1.3 2012-08-07 22:55:37 aishizaki Exp $

# Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

__version__ = '''0.4.0'''

import unittest
from tdb import AU, Map, MapError

class TestMap(unittest.TestCase):
    
    def testGet(self):
        '''Tests simple set/get.'''
        m = Map()
        # What goes in is what comes out.
        m.set('key1', 'value1')
        self.assertEquals(m.get(('key1',)), 'value1')
        # Atoms work as keys.
        self.assertEquals(m.get('key2'), None)
        # Values are write-once
        self.assertRaises(MapError, m.set, 'key1', 'value1b')
    
    def testGetPrefix(self):
        '''Tests set/get_prefix.'''
        m = Map()
        # Tuple key
        m.set(('key1a', 'key1b'), 'value1ab')
        self.assertEquals(m.get(('key1a', 'key1b')), 'value1ab')
        # Retrieve by prefix
        self.assertEquals(m.get_prefix('key1a'), {'key1b': 'value1ab'})
        # get_prefix only retrieves at the given level
        m.set(('key1a', 'key1b', 'key1c'), 'value1abc')
        self.assertEquals(m.get_prefix('key1a'), {'key1b': 'value1ab'})
    
    def testArgumentChecking(self):
        '''Tests argument checking.'''
        # Keys cannot be None, the empty tuple, or something that isn't a tuple or a string
        m = Map()
        self.assertRaises(MapError, m.set, None, 'value1')
        self.assertRaises(MapError, m.set, (), 'value1')
        self.assertRaises(MapError, m.set, [1,2,3], 'value1')
        self.assertRaises(MapError, m.get, None)
        self.assertRaises(MapError, m.get, ())
        self.assertRaises(MapError, m.get, [1,2,3])
        
    def testInheritance(self):
        '''Tests that inheritance from a parent map works'''
        parent_map = Map()
        parent_map.set('key1', 'value1')
        self.assertEquals(parent_map.get('key1'), 'value1')
        child_map = Map(parent_map)
        # Check that the child map inherits values from the parent map
        self.assertEquals(child_map.get('key1'), 'value1')
        # Now override the value
        child_map.set('key1', 'value1child')
        self.assertEquals(parent_map.get('key1'), 'value1')
        self.assertEquals(child_map.get('key1'), 'value1child')
        # Likewise with a shared parent
        other_child = Map(parent_map)
        other_child.set('key1', 'value1other')
        self.assertEquals(parent_map.get('key1'), 'value1')
        self.assertEquals(child_map.get('key1'), 'value1child')
        self.assertEquals(other_child.get('key1'), 'value1other')
    
    def testInheritancePrefix(self):
        '''Tests that inheritance works with prefixes'''
        parent_map = Map()
        parent_map.set(('key1a', 'key1b'), 'value1ab')
        child_map = Map(parent_map)
        self.assertEquals(parent_map.get(('key1a', 'key1b')), 'value1ab')
        self.assertEquals(child_map.get(('key1a', 'key1b')), 'value1ab')
        self.assertEquals(parent_map.get_prefix(('key1a',)), {'key1b': 'value1ab'})
        self.assertEquals(child_map.get_prefix(('key1a',)), {'key1b': 'value1ab'})
        # Override in the child
        child_map.set(('key1a', 'key1b'), 'value1abchild')
        self.assertEquals(parent_map.get(('key1a', 'key1b')), 'value1ab')
        self.assertEquals(child_map.get(('key1a', 'key1b')), 'value1abchild')
        self.assertEquals(parent_map.get_prefix(('key1a',)), {'key1b': 'value1ab'})
        self.assertEquals(child_map.get_prefix(('key1a',)), {'key1b': 'value1abchild'})
        
class TestAU(unittest.TestCase):

    def testAuid(self):
        for st, par in [('a~b&c~d', {'a': 'b', 'c': 'd'}),
                       ('a~b&c~d', {'c': 'd', 'a': 'b'}),
                       ('base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~123', {'base_url': 'http://www.example.com/', 'volume_name': '123'}),
                       ('base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~123', {'volume_name': '123', 'base_url': 'http://www.example.com/'})]:
            self.assertEquals('org|lockss|plugin|FooPlugin&' + st, AU.compute_auid('org.lockss.plugin.FooPlugin', par))
    
    def testAuidPlus(self):
        # test an auid with nondef params 1) typical, 2) with nothing on the right side of the equal sign, 3) empty ndp argument
        for st, par, ndp in [('base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~123', 
                              {'base_url': 'http://www.example.com/', 'volume_name': '123'},
                              {'journal_code': 'Delawho'}),
                             ('base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~123', 
                              {'volume_name': '123', 'base_url': 'http://www.example.com/'},
                              {'issues': ''})]:
            self.assertEquals('org|lockss|plugin|FooPlugin&' + st +'&&&NondefParamsFollow&&&' + ndp.keys()[0] + '~'+ ndp[ndp.keys()[0]],
                              AU.compute_auidplus('org.lockss.plugin.FooPlugin', par, ndp))
        # test an auid with with two nondef params
        st = 'base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~123' 
        par = {'volume_name': '123','base_url': 'http://www.example.com/'}
        ndp = {'journal_code': 'Delawho','issues': '13'}
        self.assertEquals('org|lockss|plugin|FooPlugin&' + st +'&&&NondefParamsFollow&&&' + 'issues' + '~'+ ndp['issues']
                              +'&&&NondefParamsFollow&&&' + 'journal_code' + '~'+ ndp['journal_code'],
                            AU.compute_auidplus('org.lockss.plugin.FooPlugin', par, ndp))
                    
if __name__ == '__main__': unittest.main()
