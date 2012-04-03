#!/usr/bin/env python
# coding: utf-8

# $Id: tdbxml.py,v 1.24 2012-04-03 01:37:45 thib_gc Exp $
#
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

__version__ = '0.3.7'

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

def __escape(str):
    from xml.sax import saxutils
    return saxutils.escape(str).replace('"', '&quot;').decode('utf-8').encode('ascii', 'xmlcharrefreplace')

def __short_au_name(au):
    str = au.name()
    str = re.sub(r'Volume\s+(\S+)$', r'\1', str)
    str = re.sub(r'\s+', '', str)
    str = re.sub(r'Á|À|Â|Ä|̣Ā', 'A', str)
    str = re.sub(r'á|à|â|ä|ā', 'a', str)
    str = re.sub(r'æ', 'ae', str)
    str = re.sub(r'É|È|Ê|Ë|Ē', 'E', str)
    str = re.sub(r'é|è|ê|ë|ē', 'e', str)
    str = re.sub(r'Í|Ì|Î|Ï|Ī', 'I', str)
    str = re.sub(r'í|ì|î|ï|ī', 'i', str)
    str = re.sub(r'Ž', 'Z', str)
    str = re.sub(r'ž', 'z', str)
    str = re.sub(r'\W+', '', str)
    return au.plugin().split('.')[-1] + __escape(str)

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

def __do_param(au, i, param, value=None):
    if value is None:
        value = au.param(param)
    print '''\
   <property name="param.%d">
    <property name="key" value="%s" />
    <property name="value" value="%s" />
   </property>''' % ( i, param, value )

def __do_attr(au, attr, value=None):
    if value is None:
        value = au.attr(attr)
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
    if au.title().issn() is not None:
        print '''\
   <property name="issn" value="%s" />''' % ( au.title().issn(), )
    if au.title().eissn() is not None:
        print '''\
   <property name="eissn" value="%s" />''' % ( au.title().eissn(), )
    if au.title().issnl() is not None:
        print '''\
   <property name="issnl" value="%s" />''' % ( au.title().issnl(), )
    print '''\
   <property name="title" value="%s" />
   <property name="plugin" value="%s" />''' % (
        __escape(au.name()),
        au.plugin() )
    i = 1
    au_params = au.params()
    for param in __IMPLICIT_PARAM_ORDER:
        if param in au_params:
            __do_param(au, i, param)
            i = i + 1
    for param in au_params:
        if param not in __IMPLICIT_PARAM_ORDER:
            __do_param(au, i, param)
            i = i + 1
    for nondefparam in au.nondefparams():
        nondefval = au.nondefparam(nondefparam)
        if nondefval is not None:
            __do_param(au, i, nondefparam, value=nondefval)
            i = i + 1
    au_proxy = au.proxy()
    if au_proxy is not None:
        __do_param(au, 98, 'crawl_proxy', value=au_proxy)
    if not options.no_pub_down and au.status() in [AU.Status.DOWN, AU.Status.SUPERSEDED, AU.Status.ZAPPED]:
        __do_param(au, 99, 'pub_down', value='true')
    au_attrs = au.attrs()
    for attr in au_attrs:
        __do_attr(au, attr, au_attrs[attr])
    if au.edition() is not None:
        __do_attr(au, 'edition', au.edition());
    if au.eisbn() is not None:
        __do_attr(au, 'eisbn', au.eisbn());
    if au.isbn() is not None:
        __do_attr(au, 'isbn', au.isbn());
    if au.year() is not None:
        __do_attr(au, 'year', au.year())
    if au.volumeName() is not None:
        __do_attr(au, 'volume', au.volumeName())
    if au.status() == AU.Status.PRE_RELEASED:
        __do_attr(au, 'releaseStatus', 'pre-release')
    if au.rights() == 'openaccess':
        __do_attr(au, 'rights', 'openaccess')
    print '''\
  </property>
'''

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
        'publisher2': re.sub(r'\'', '&apos;', __escape(current_pub.name())),
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
