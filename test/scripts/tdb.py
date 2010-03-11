#!/usr/bin/python

# $Id: tdb.py,v 1.16 2010-03-11 01:36:39 thib_gc Exp $
#
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
    PUBLISHER = 'publisher'

    def __init__(self):
        '''Constructor.'''
        Map.__init__(self)

    def set_publisher(self, publisher): self.set(Title.PUBLISHER, publisher)

    def name(self): return self.get(Title.NAME)
    def publisher(self): return self.get(Title.PUBLISHER)

class AU(ChainedMap):

    ATTR = 'attr'
    ISSN = 'issn'
    NAME = 'name'
    PARAM = 'param'
    PLUGIN = 'plugin'
    PLUGIN_PREFIX = 'pluginPrefix'
    PLUGIN_SUFFIX = 'pluginSuffix'
    PROXY = 'proxy'
    RIGHTS = 'rights'
    STATUS = 'status'
    STATUS_DOES_NOT_EXIST = 'doesNotExist'
    STATUS_DO_NOT_PROCESS = 'doNotProcess'
    STATUS_EXISTS         = 'exists'
    STATUS_MANIFEST       = 'manifest'
    STATUS_WANTED         = 'wanted'
    STATUS_TESTING        = 'testing'
    STATUS_NOT_READY      = 'notReady'
    STATUS_TESTED         = 'tested'
    STATUS_RETESTING      = 'retesting'
    STATUS_READY          = 'ready'
    STATUS_PRE_RELEASING  = 'preReleasing'
    STATUS_PRE_RELEASED   = 'preReleased'
    STATUS_RELEASING      = 'releasing'
    STATUS_RELEASED       = 'released'
    STATUS_DOWN           = 'down'
    STATUS_SUPERSEDED     = 'superseded'
    STATUS_RETRACTED      = 'retracted'
    STATUSES = [ STATUS_DOES_NOT_EXIST,
                 STATUS_DO_NOT_PROCESS,
                 STATUS_EXISTS,
                 STATUS_MANIFEST,
                 STATUS_WANTED,
                 STATUS_TESTING,
                 STATUS_NOT_READY,
                 STATUS_TESTED,
                 STATUS_RETESTING,
                 STATUS_READY,
                 STATUS_PRE_RELEASING,
                 STATUS_PRE_RELEASED,
                 STATUS_RELEASING,
                 STATUS_RELEASED,
                 STATUS_DOWN,
                 STATUS_SUPERSEDED,
                 STATUS_RETRACTED ]
    TITLE = 'title'

    def __init__(self, next=None):
        '''Constructor.'''
        ChainedMap.__init__(self, next)

    def set_title(self, title): self.set(AU.TITLE, title)

    def attr(self, attr): return self.get( (AU.ATTR, attr) )
    def attrs(self): return self.get(AU.ATTR) or dict()
    def issn(self): return self.get(AU.ISSN)
    def name(self): return self.get(AU.NAME)
    def param(self, param): return self.get( (AU.PARAM, param) )
    def params(self): return self.get(AU.PARAM) or dict()
    def plugin(self): return self.get(AU.PLUGIN) or self.get(AU.PLUGIN_PREFIX) + self.get(AU.PLUGIN_SUFFIX)
    def proxy(self):
        val = self.get(AU.PROXY)
        if val is None or len(val) == 0: return None
        else: return val
    def rights(self): return self.get(AU.RIGHTS)
    def status(self): return self.get(AU.STATUS)
    def title(self): return self.get(AU.TITLE)

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
