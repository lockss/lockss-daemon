#!/usr/bin/env python
# coding: utf-8

# $Id: tdbxml.py,v 1.29 2012-08-08 07:08:40 thib_gc Exp $

__copyright__ = '''\
Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.
'''

__version__ = '0.4.3'

from optparse import OptionGroup, OptionParser
import re
import sys
from tdb import *
import tdbparse

class TdbxmlConstants:
    
    OPTION_NO_PUB_DOWN = 'no-pub-down'
    OPTION_NO_PUB_DOWN_SHORT = 'd'
    OPTION_NO_PUB_DOWN_HELP = 'do not include pub_down markers'

    OPTION_OUTPUT_FILE = 'output'
    OPTION_OUTPUT_FILE_SHORT = 'o'
    OPTION_OUTPUT_FILE_HELP = 'write output to a file'

    OPTION_INPUT_FILE = 'input'
    OPTION_INPUT_FILE_SHORT = 'i'
    OPTION_INPUT_FILE_HELP = 'read input from a file'

    OPTION_STYLE = 'style'
    OPTION_STYLE_SHORT = 's'
    OPTION_STYLE_ENTRIES = 'entries'
    OPTION_STYLE_LEGACY = 'legacy'
    OPTION_STYLE_PUBLISHER = 'publisher'
    OPTION_STYLE_DEFAULT = OPTION_STYLE_PUBLISHER
    OPTION_STYLES = [OPTION_STYLE_ENTRIES, OPTION_STYLE_LEGACY, OPTION_STYLE_PUBLISHER]
    OPTION_STYLE_HELP = 'output style (%s) (default: %s)' % (', '.join(OPTION_STYLES), '%default')
    
__IMPLICIT_PARAM_ORDER = [
    'base_url', 'base_url2', 'base_url3', 'base_url4', 'base_url5',
    'oai_request_url',
    'publisher_id', 'publisher_code', 'publisher_name',
    'journal_issn', 'journal_id', 'journal_code', 'journal_abbr', 'journal_dir',
    'year',
    'issues', 'issue_set', 'issue_range', 'num_issue_range',
    'volume_name', 'volume_str', 'volume'
]

__IMPLICIT_PARAM_ORDER_SET = set(__IMPLICIT_PARAM_ORDER)

def __escape(str):
    from xml.sax import saxutils
    return saxutils.escape(str).replace('"', '&quot;').decode('utf-8').encode('ascii', 'xmlcharrefreplace')

RE_VOLUME = re.compile(r'Volume\s+(\S+)$')
RE_WHITE = re.compile(r'\s+')
RE_AA = re.compile(r'Á|À|Â|Ä|̣Ā')
RE_A = re.compile(r'á|à|â|ä|ā')
RE_AE = re.compile(r'æ')
RE_EE = re.compile(r'É|È|Ê|Ë|Ē')
RE_E = re.compile(r'é|è|ê|ë|ē')
RE_II = re.compile(r'Í|Ì|Î|Ï|Ī')
RE_I = re.compile(r'í|ì|î|ï|ī')
RE_SS = re.compile(r'Ş')
RE_S = re.compile(r'ş')
RE_ZZ = re.compile(r'Ž')
RE_Z = re.compile(r'ž')
RE_NONWORD = re.compile(r'\W+')

def __short_au_name(au):
    str = au.name()
    str = RE_VOLUME.sub('\1', str)
    str = RE_WHITE.sub('', str)
    str = RE_AA.sub('A', str)
    str = RE_A.sub('a', str)
    str = RE_AE.sub('ae', str)
    str = RE_EE.sub('E', str)
    str = RE_E.sub('e', str)
    str = RE_II.sub('I', str)
    str = RE_I.sub('i', str)
    str = RE_SS.sub('S', str)
    str = RE_S.sub('s', str)
    str = RE_ZZ.sub('Z', str)
    str = RE_Z.sub('z', str)
    str = RE_NONWORD.sub('', str)
    plu = au.plugin()
    return plu[plu.rfind('.') + 1:] + __escape(str)

def __preamble(tdb, options):
    if options.style == TdbxmlConstants.OPTION_STYLE_ENTRIES: return
    print '''\
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE lockss-config [
<!ELEMENT lockss-config (if|property)*>
<!ELEMENT property (property|list|value|if)*>
<!ELEMENT list (value)+>
<!ELEMENT value (#PCDATA)>
<!ELEMENT test EMPTY>
<!ELEMENT and (and|or|not|test)*>
<!ELEMENT or (and|or|not|test)*>
<!ELEMENT not (and|or|not|test)*>
<!ELEMENT if (and|or|not|then|else|test|property)*>
<!ELEMENT then (if|property)*>
<!ELEMENT else (if|property)*>
<!ATTLIST property name CDATA #REQUIRED>
<!ATTLIST property value CDATA #IMPLIED>
<!ATTLIST test hostname CDATA #IMPLIED>
<!ATTLIST test group CDATA #IMPLIED>
<!ATTLIST test daemonVersionMin CDATA #IMPLIED>
<!ATTLIST test daemonVersionMax CDATA #IMPLIED>
<!ATTLIST test daemonVersion CDATA #IMPLIED>
<!ATTLIST test platformVersionMin CDATA #IMPLIED>
<!ATTLIST test platformVersionMax CDATA #IMPLIED>
<!ATTLIST test platformVersion CDATA #IMPLIED>
<!ATTLIST test platformName CDATA #IMPLIED>
<!ATTLIST if hostname CDATA #IMPLIED>
<!ATTLIST if group CDATA #IMPLIED>
<!ATTLIST if daemonVersionMin CDATA #IMPLIED>
<!ATTLIST if daemonVersionMax CDATA #IMPLIED>
<!ATTLIST if daemonVersion CDATA #IMPLIED>
<!ATTLIST if platformVersionMin CDATA #IMPLIED>
<!ATTLIST if platformVersionMax CDATA #IMPLIED>
<!ATTLIST if platformVersion CDATA #IMPLIED>
<!ATTLIST if platformName CDATA #IMPLIED>
<!ATTLIST list append CDATA #IMPLIED>
]>
'''

def __introduction(tdb, options):
    if options.style == TdbxmlConstants.OPTION_STYLE_ENTRIES: return
    print '''\
<lockss-config>
'''

def __do_param(au, i, param, value):
    print '''\
   <property name="param.%d">
    <property name="key" value="%s" />
    <property name="value" value="%s" />
   </property>''' % ( i, param, value )

def __do_attr(au, attr, value):
    print '''\
   <property name="attributes.%s" value="%s" />''' % ( attr, value )

def __process_au(au, options):
    print '''\
  <property name="%s">
   <property name="attributes.publisher" value="%s" />
   <property name="journalTitle" value="%s" />''' % (
        __short_au_name(au),
        __escape(au.title().publisher().name()),
        __escape(au.title().name()) )
    title = au.title()
    for val, st in [(title.issn(), 'issn'),
                    (title.eissn(), 'eissn'),
                    (title.issnl(), 'issnl')]:
      if val is not None: print '''\
   <property name="%s" value="%s" />''' % ( st, val )
    print '''\
   <property name="title" value="%s" />
   <property name="plugin" value="%s" />''' % (
        __escape(au.name()),
        au.plugin() )
    i = 1
    au_params = au.params()
    for param in __IMPLICIT_PARAM_ORDER:
        pval = au_params.get(param)
        if pval is not None:
            __do_param(au, i, param, pval)
            i = i + 1
    for param, pval in au_params.iteritems():
        if param not in __IMPLICIT_PARAM_ORDER_SET:
            __do_param(au, i, param, pval)
            i = i + 1
    for nondefparam, nondefval in au.nondefparams().iteritems():
        if nondefval is not None:
            __do_param(au, i, nondefparam, nondefval)
            i = i + 1
    au_proxy = au.proxy()
    if au_proxy is not None:
        __do_param(au, 98, 'crawl_proxy', au_proxy)
    if not options.no_pub_down and au.status() in [AU.Status.DOWN, AU.Status.SUPERSEDED, AU.Status.ZAPPED]:
        __do_param(au, 99, 'pub_down', 'true')
    for attr, attrval in au.attrs().iteritems():
        __do_attr(au, attr, attrval)
    for val, st in [(au.edition(), 'edition'),
                    (au.eisbn(), 'eisbn'),
                    (au.isbn(), 'isbn'),
                    (au.year(), 'year'),
                    (au.volume(), 'volume')]:
      if val is not None: __do_attr(au, st, val)
    if au.rights() == 'openaccess':
        __do_attr(au, 'rights', 'openaccess')
    print '''\
  </property>
'''

RE_APOS = re.compile(r'\'')

def __process(tdb, options):
    current_pub = None
    if options.style == TdbxmlConstants.OPTION_STYLE_LEGACY:
        print '''\
 <property name="org.lockss.title">
'''
    for au in tdb.aus():
        if options.style == TdbxmlConstants.OPTION_STYLE_PUBLISHER and current_pub is not au.title().publisher():
            if current_pub is not None:
                print '''\
 </property>
'''
            current_pub = au.title().publisher()
            print '''\
 <property name="org.lockss.titleSet">

  <property name="%(publisher1)s">
   <property name="name" value="All %(publisher)s AUs" />
   <property name="class" value="xpath" />
   <property name="xpath" value=%(outer)s[attributes/publisher=%(inner)s%(publisher2)s%(inner)s]%(outer)s />
  </property>
  
 </property>
 
 <property name="org.lockss.title">
''' % { 'publisher': __escape(current_pub.name()),
        'publisher1': __escape(current_pub.name().replace('.', '')),
        'publisher2': RE_APOS.sub('&apos;', __escape(current_pub.name())),
        'outer': '"' if "'" not in current_pub.name() else "'",
        'inner': "'" if "'" not in current_pub.name() else '"' }
        __process_au(au, options)
    else:
        if options.style == TdbxmlConstants.OPTION_STYLE_PUBLISHER and current_pub is not None: print '''\
 </property>
'''             
    if options.style == TdbxmlConstants.OPTION_STYLE_LEGACY:
        print '''\
 </property>
'''

def __conclusion(tdb, options):
    if options.style == TdbxmlConstants.OPTION_STYLE_ENTRIES: return
    print '</lockss-config>'

def __postamble(tdb, options):
    pass

def __option_parser__(parser=None):
    if parser is None: parser = OptionParser(version=__version__)
    parser = tdbparse.__option_parser__(parser)
    tdbxml_group = OptionGroup(parser, 'tdbxml module (%s)' % (__version__,))
    tdbxml_group.add_option('-' + TdbxmlConstants.OPTION_STYLE_SHORT,
                            '--' + TdbxmlConstants.OPTION_STYLE,
                            choices=TdbxmlConstants.OPTION_STYLES,
                            default=TdbxmlConstants.OPTION_STYLE_DEFAULT,
                            help=TdbxmlConstants.OPTION_STYLE_HELP)
    tdbxml_group.add_option('-' + TdbxmlConstants.OPTION_NO_PUB_DOWN_SHORT,
                            '--' + TdbxmlConstants.OPTION_NO_PUB_DOWN,
                            action='store_true',
                            help=TdbxmlConstants.OPTION_NO_PUB_DOWN_HELP)
    tdbxml_group.add_option('-' + TdbxmlConstants.OPTION_INPUT_FILE_SHORT,
                            '--' + TdbxmlConstants.OPTION_INPUT_FILE,
                            action='store',
                            dest='input_file',
                            help=TdbxmlConstants.OPTION_INPUT_FILE_HELP
                            )
    tdbxml_group.add_option('-' + TdbxmlConstants.OPTION_OUTPUT_FILE_SHORT,
                            '--' + TdbxmlConstants.OPTION_OUTPUT_FILE,
                            action='store',
                            dest='output_file',
                            help=TdbxmlConstants.OPTION_OUTPUT_FILE_HELP
                            )
    parser.add_option_group(tdbxml_group)
    return parser

def tdb_to_xml(tdb, options):
    __preamble(tdb, options)
    __introduction(tdb, options)
    __process(tdb, options)
    __conclusion(tdb, options)
    __postamble(tdb, options)

def __reprocess_options__(parser, options):
    tdbparse.__reprocess_options__(parser, options)

if __name__ == '__main__':
    parser = __option_parser__()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    __reprocess_options__(parser, options)
    if options.input_file:
        infile = open(options.input_file, 'r')
    else:
        infile = sys.stdin
    try:
        tdb = tdbparse.tdbparse(infile, options)
    except tdbparse.TdbparseSyntaxError, e:
        print >>sys.stderr, e
        exit(1)
    else:
        saveout = sys.stdout
        try:
            if options.output_file:
                sys.stdout = open(options.output_file, 'w')
            tdb_to_xml(tdb, options)
        finally:
            sys.stdout = saveout
