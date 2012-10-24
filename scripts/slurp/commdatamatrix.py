#!/usr/bin/env python

import fileinput
import optparse

ORIGIN = 0
FAIL = 1
ACCEPT = 2
SENT = 3
RECEIVED = 4
CHANNEL = 5
SEND_QUEUE= 6
LAST_ATTEMPT = 7
NEXT_RETRY = 8

def make_option_parser(): 
    parser = optparse.OptionParser(usage='Usage: %prog [options] IP1.tsv IP2.tsv...')
    return parser

def from_ip_to_peerid(ipstr):
    return 'TCP:[%s]:9729' % (ipstr,)

def populate_with_data(data):
    for line in fileinput.input():
        if fileinput.isfirstline():
            ipstr = fileinput.filename()[0:-4]
            peerid = from_ip_to_peerid(ipstr)
            data[peerid] = dict()
        try: ary = line[0:-1].split('\t')
        except: raise RuntimeError, '%s:%d: syntax error: %s' % (fileinput.filename(), fileinput.filelineno(), line)
        d = ary[1:]
        for i in [ORIGIN, FAIL, ACCEPT, SENT, RECEIVED]: d[i] = int(d[i])
        data[peerid][ary[0]] = d

def compute_results(data, options):
    lamb = lambda x,y,yd: foo(x,y,yd)
    zzz = set()
    for p1, v1 in data.iteritems():
        zzz.add(p1)
        for p2 in v1.iterkeys(): zzz.add(p2)
    peerids = [x for x in sorted(zzz)]
    print '\t%s' % ('\t'.join(peerids),)
    for p1 in sorted(data.keys()):
        v1 = data[p1]
        s = '%s' % (p1,)
        for p2 in peerids:
            s = s + '\t'
            v2 = v1.get(p2)
            if v2 is None: continue
            s = s + lamb(p1, p2, v2)
        print s

def foo(p1, p2, d):
    if p1 == p2: return '.'
    if d[ACCEPT] > 0:
        if d[ORIGIN] > 0 and d[ORIGIN] > d[FAIL] + 1: return 'X'
        else: return '<'
    else:
        if d[ORIGIN] > 0 and d[ORIGIN] > d[FAIL] + 1: return '>'
        else: return ' '

if __name__ == '__main__':
    parser = make_option_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    data = dict()
    populate_with_data(data)
    compute_results(data, options)

