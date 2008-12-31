#!/usr/bin/python

# $Id: tdb.py,v 1.7 2008-12-31 12:15:02 thib_gc Exp $
#
# Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

#import re

class Map(object):
    
    def __init__(self, dic={}):
        '''Constructor.'''
        object.__init__(self)
        self._dictionary = dic.copy()

    def get(self, key):
        if type(key) is not tuple: key = ( key, )
        return self._recursive_get(self._dictionary, key)
    
    def set(self, key, value):
        if type(key) is not tuple: key = ( key, )
        self._recursive_set(self._dictionary, key, value)
    
    def _recursive_get(self, map, key):
        kfirst, krest = key[0], key[1:]
        mrest = map.get(kfirst)
        if len(krest) == 0 or kfirst not in map: return mrest
        return self._recursive_get(mrest, krest) 

    def _recursive_set(self, map, key, value):
        kfirst, krest = key[0], key[1:]
        if kfirst not in map and len(krest) > 0: map[kfirst] = dict()
        if len(krest) == 0: map[kfirst] = value
        else: self._recursive_set(map[kfirst], krest, value) 

class ChainedMap(Map):
    
    def __init__(self, next=None):
        Map.__init__(self)
        self._next = next

    def get(self, key):
        value = super(ChainedMap,self).get(key)
        if value or self._next is None: return value
        return self._next.get(key)    

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
    RIGHTS='rights'
    STATUS = 'status'
    TITLE = 'title'

    def __init__(self, next=None):
        ChainedMap.__init__(self, next)

    def set_title(self, title): self.set(AU.TITLE, title)

    def attr(self, attr): return self.get(( AU.ATTR, attr ))
    def attrs(self): return self.get(AU.ATTR) or dict()
    def issn(self): return self.get(AU.ISSN)
    def name(self): return self.get(AU.NAME)
    def param(self, param): return self.get(( AU.PARAM, param ))
    def params(self): return self.get(AU.PARAM) or dict()
    def plugin(self): return self.get(AU.PLUGIN)
    def rights(self): return self.get(AU.RIGHTS)
    def status(self): return self.get(AU.STATUS)
    def title(self): return self.get(AU.TITLE)

class Tdb(object):

    def __init__(self):
        self.__publishers = []
        self.__titles = []
        self.__aus = []

    def add_publisher(self, publisher): self.__publishers.append(publisher)
    def add_title(self, title): self.__titles.append(title)
    def add_au(self, au): self.__aus.append(au)

    def publishers(self): return self.__publishers[:]
    def titles(self): return self.__titles[:]
    def aus(self): return self.__aus[:]

    def internal_print(self):
        print self.publishers()
        print self.titles()
        print self.aus()
