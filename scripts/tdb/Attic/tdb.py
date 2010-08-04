#!/usr/bin/python

# $Id: tdb.py,v 1.2 2010-08-04 18:49:00 thib_gc Exp $

# Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

__version__ = '''0.3.1'''

import re

class Map(object):
    
    def __init__(self, dic={}):
        '''Constructor.'''
        object.__init__(self)
        self._dictionary = dic.copy()

    def get(self, key):
        if type(key) is not tuple: key = ( key, )
        return self._get(key, self._dictionary)
    
    def _get(self, key, rec):
        if len(key) == 0: return rec
        newrec = rec.get(key[0])
        if newrec: return self._get(key[1:], newrec)
        return None
    
    def set(self, key, val):
        if type(key) is not tuple: key = ( key, )
        self._set(key, val, self._dictionary)
    
    def _set(self, key, val, rec):
        k0 = key[0]
        if len(key) == 1:
            rec[k0] = val
            return
        if k0 not in rec:
            rec[k0] = dict()
        newrec = rec.get(k0)
        self._set(key[1:], val, newrec)
    
class ChainedMap(Map):
    
    def __init__(self, next=None):
        Map.__init__(self)
        self._next = next

    def get(self, key):
        myval = super(ChainedMap,self).get(key)
        if self._next is None: return myval
        nextval = self._next.get(key)
        if myval is None: return nextval
        if nextval is None or type(myval) is not dict: return myval
        nextval = nextval.copy()
        self._recursive_update(nextval, myval)
        return nextval
        
    def _recursive_update(self, map1, map2):
        for (k2,v2) in map2.items():
            if type(v2) is dict:
                if k2 not in map1: map1[k2] = dict()
                self._recursive_update(map1[k2], v2)
            else: map1[k2] = v2

class Publisher(Map):

    NAME = 'name'

    def __init__(self):
        '''Constructor.'''
        Map.__init__(self)

    def name(self): return self.get(Publisher.NAME)

class Title(Map):

    NAME = 'name'
    EISBN = 'eisbn'
    EISSN = 'eissn'
    ISBN = 'isbn'
    ISSN = 'issn'
    PUBLISHER = 'publisher'
    TYPE = 'type'

    class Type:
        BOOK = 'book'
        JOURNAL = 'journal'
        DEFAULT = JOURNAL

    def __init__(self):
        '''Constructor.'''
        Map.__init__(self)

    def set_publisher(self, publisher): self.set(Title.PUBLISHER, publisher)

    def name(self): return self.get(Title.NAME)
    def eisbn(self): return self.get(Title.EISBN)
    def eissn(self): return self.get(Title.EISSN)
    def isbn(self): return self.get(Title.ISBN)
    def issn(self): return self.get(Title.ISSN)
    def publisher(self): return self.get(Title.PUBLISHER)
    def type(self): return self.get(Title.TYPE) or Title.Type.DEFAULT

class AU(ChainedMap):

    class Status:
        DOES_NOT_EXIST = 'doesNotExist'
        DO_NOT_PROCESS = 'doNotProcess'
        EXISTS = 'exists'
        MANIFEST = 'manifest'
        WANTED = 'wanted'
        TESTING = 'testing'
        NOT_READY = 'notReady'
        TESTED = 'tested'
        RETESTING = 'retesting'
        READY = 'ready'
        PRE_RELEASING = 'preReleasing'
        PRE_RELEASED = 'preReleased'
        RELEASING = 'releasing'
        RELEASED = 'released'
        DOWN = 'down'
        SUPERSEDED = 'superseded'
        RETRACTED = 'retracted'

    ATTR = 'attr'
    NAME = 'name'
    NONDEFPARAM = 'nondefparam'
    PARAM = 'param'
    PLUGIN = 'plugin'
    PLUGIN_PREFIX = 'pluginPrefix'
    PLUGIN_SUFFIX = 'pluginSuffix'
    PROXY = 'proxy'
    RIGHTS = 'rights'
    STATUS = 'status'
    TITLE = 'title'
    YEAR = 'year'

    def __init__(self, next=None):
        '''Constructor.'''
        ChainedMap.__init__(self, next)

    def set_title(self, title): self.set(AU.TITLE, title)

    def attr(self, attr): return self.get( (AU.ATTR, attr) )
    def attrs(self): return self.get(AU.ATTR) or {}
    def auid(self): return AU.computeAuid(self.plugin(), self.params())
    def name(self): return self.get(AU.NAME)
    def nondefparam(self, nondefparam):
        val = self.get( (AU.NONDEFPARAM, nondefparam) )
        if val is None or len(val) == 0: return None
        else: return val
    def nondefparams(self): return self.get(AU.NONDEFPARAM) or {}
    def param(self, param): return self.get( (AU.PARAM, param) )
    def params(self): return self.get(AU.PARAM) or {}
    def plugin(self): return self.get(AU.PLUGIN) or self.get(AU.PLUGIN_PREFIX) + self.get(AU.PLUGIN_SUFFIX)
    def proxy(self):
        val = self.get(AU.PROXY)
        if val is None or len(val) == 0: return None
        else: return val
    def rights(self): return self.get(AU.RIGHTS)
    def status(self): return self.get(AU.STATUS)
    def title(self): return self.get(AU.TITLE)
    def year(self): return self.get(AU.YEAR)

    @staticmethod
    def __auidencode(c):
        if re.match(r'^[-_*A-Za-z0-9]$', c): return c
        if c == ' ': return '+'
        return '%' + ('0' if ord(c) < 16 else '') + hex(ord(c))[2:].upper()

    @staticmethod
    def computeAuid(plugin, params):
        keys = params.keys()
        keys.sort() # in Java: by iterating over a TreeSet
        #a = '&'.join('~'.join([''.join(self.__auidencode()) for s in []]))
        return plugin.replace('.', '|') + '&' + '&'.join(['~'.join([''.join(map(AU.__auidencode, [c for c in s])) for s in [k, params[k]]]) for k in keys])

class Tdb(object):

    def __init__(self):
        '''Constructor.'''
        self.__publishers = []
        self.__titles = []
        self.__aus = []

    def add_publisher(self, publisher): self.__publishers.append(publisher)
    def add_title(self, title): self.__titles.append(title)
    def add_au(self, au): self.__aus.append(au)

    def publishers(self): return self.__publishers[:]
    def titles(self): return self.__titles[:]
    def aus(self): return self.__aus[:]

###
### Test code
###

import unittest

class TestAU(unittest.TestCase):

    def testAuid(self):
        for st, par in [('a~b&c~d', {'a': 'b', 'c': 'd'}),
                       ('a~b&c~d', {'c': 'd', 'a': 'b'}),
                       ('base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~123', {'base_url': 'http://www.example.com/', 'volume_name': '123'})]:
            self.assertEquals('org|lockss|plugin|FooPlugin&' + st, AU.computeAuid('org.lockss.plugin.FooPlugin', par))

if __name__ == '__main__': unittest.main()
