#!/usr/bin/python

class TdbObject(object):

    def __init__(self):
        self._dictionary = {}

    def get(self, key):
        return self._dictionary.get(key)

    def geti(self, indexed_key):
        key, index = self._key(indexed_key)
        if index:
            if key not in self._dictionary: return None
            else: return self._dictionary[key].get(index)
        else:
            if key not in self._dictionary: return {}
            else: return self._dictionary[key].copy()

    def set(self, key, value):
        self._dictionary[key] = value

    def seti(self, indexed_key, value):
        key, index = self._key(indexed_key)
        if index:
            if key not in self._dictionary: self._dictionary[key] = {}
            self._dictionary[key][index] = value
        else:
            self._dictionary[key] = value.copy()

    def _key(self, str):
        import re
        match = re.match(r'([^\[]+)(?:\[(\w+)\])?$', str)
        if match: return (match.group(1), match.group(2))
        else: raise KeyError, 'invalid key: %s' % (str,)

class ChainedTdbObject(TdbObject):

    def __init__(self, next=None):
        TdbObject.__init__(self)
        self._next = next

    def get(self, key):
        elem = super(ChainedTdbObject,self).get(key)
        if elem or self._next is None: return elem
        return self._next.get(key)

    def geti(self, indexed_key):
        key, index = self._key(indexed_key)
        elem = super(ChainedTdbObject,self).geti(key)
        pelem = {}
        if self._next: pelem = self._next.geti(key)
        pelem.update(elem)
        if index: return pelem.get(index)
        else: return pelem

class Publisher(TdbObject):

    NAME = 'name'

    def name(self): return self.get(Title.NAME)
    def set_name(self, name): self.set(Title.NAME, name)

class Title(TdbObject):

    NAME = 'name'
    PUBLISHER = 'publisher'

    def name(self): return self.get(Title.NAME)
    def set_name(self, name): self.set(Title.NAME, name)
    def publisher(self): return self.get(Title.PUBLISHER)
    def set_publisher(self, publisher): self.set(Title.PUBLISHER, publisher)

class AU(ChainedTdbObject):

    NAME = 'name'
    TITLE = 'title'

    def __init__(self, next=None):
        ChainedTdbObject.__init__(self, next)

    def name(self): return self.get(AU.NAME)
    def set_name(self, name): self.set(AU.NAME, name)
    def title(self): return self.get(AU.TITLE)
    def set_title(self, title): self.set(AU.TITLE, title)

class Tdb(object):

    def __init__(self):
        self.__publishers = {}
        self.__titles = {}
        self.__aus = {}

    def add_publisher(self, publisher):
        key = publisher.name()
        if key in self.__publishers:
            raise KeyError, 'publisher already exists: %s' % (key,)
        self.__publishers[key] = publisher

    def add_title(self, title):
        key = (title.publisher().name(), title.name())
        if key in self.__titles:
            raise KeyError, 'title already exists: %s' % (key,)
        self.__titles[key] = title

    def add_au(self, au):
        print au._dictionary
        key = (au.title().publisher().name(), au.title().name(), au.name())
        if key in self.__aus:
            raise KeyError, 'AU already exists: %s' % (key,)
        self.__aus[key] = au

    def internal_print(self):
        print (self.__publishers, self.__titles, self.__aus)

if __name__ == '__main__':
    tdb = Tdb()
    p_foo = Publisher()
    p_foo.set_name('Society of Foo')
    t_foo = Title()
    t_foo.set_publisher(p_foo)
    t_foo.set_name('Journal of Foo')
    a_foo1 = AU()
    a_foo1.set_title(t_foo)
    a_foo1.set_name('Journal of Foo Volume 1')
    a_foo2 = AU()
    a_foo2.set_title(t_foo)
    a_foo2.set_name('Journal of Foo Volume 2')
    print a_foo1.name()
    print a_foo1.title().name()
    print a_foo1.title().publisher().name()
