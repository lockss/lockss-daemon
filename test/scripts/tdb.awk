#!/usr/bin/awk -f

#
# BEGIN
#

BEGIN {
 # Constants
 false = 0;
 true = 1;
 CODE_ATTRIBUTE      = "@"
 CODE_ESTIMATED_SIZE = "e"
 CODE_JOURNAL_TITLE  = "j"
 CODE_PARAMETER      = "%"
 CODE_PLUGIN         = "p"
 CODE_PUBLISHER      = "P"
 CODE_RIGHTS         = "R"
 CODE_SKIP_1         = "x"
 CODE_SKIP_2         = "X"
 CODE_STATUS         = "S"
 CODE_TITLE          = "t"
 STATUS_DOES_NOT_EXIST = "does_not_exist"
 STATUS_DO_NOT_PROCESS = "do_not_process"
 STATUS_DOWN           = "down"
 STATUS_EXISTS         = "exists"
 STATUS_MANIFEST       = "manifest"
 STATUS_PRE_RELEASED   = "pre_released"
 STATUS_READY          = "ready"
 STATUS_RELEASED       = "released"
 STATUS_RETRACTED      = "retracted"
 STATUS_TESTING        = "testing"
 LEVEL_CONSERVATIVE_TESTING = "conservativeTesting"
 LEVEL_CONTENT_TESTING      = "contentTesting"
 LEVEL_DANGEROUS            = "dangerous"
 LEVEL_PRE_RELEASE          = "preRelease"
 LEVEL_PRODUCTION           = "production"
 LEVEL_VERY_DANGEROUS       = "veryDangerous"
 LEVEL_DEFAULT              = LEVEL_PRODUCTION
 EXIT_NO_FORMAT_STRING = 11

 # Initial setup
 FS="\t"
 preamble = true
 numAdditionalAttributes = 0
 numParameters = 0
 parseCommandLine()
}

#
# FUNCTIONS
#

# parseCommandLine
function parseCommandLine() {
 if (formatString != "") { parseFormatString() }
 if (outputLevel == "") { outputLevel = LEVEL_DEFAULT }
 parseOutputLevel()
}

# parseFormatString
function parseFormatString() {
 numFields = split(formatString, _fields, ",")
 for (_field = 1 ; _field <= numFields ; ++_field) {
  _code = _fields[_field]
  if (_code == CODE_SKIP_1 || _code == CODE_SKIP_2) { continue }
  else if (_code == CODE_ESTIMATED_SIZE || _code == CODE_JOURNAL_TITLE || _code == CODE_PLUGIN || _code == CODE_TITLE || _code == CODE_PUBLISHER || _code == CODE_STATUS || _code == CODE_RIGHTS) { indexOf[_code] = _field }
  else if (_code == CODE_ATTRIBUTE) { indexAdditionalAttributes[++numAdditionalAttributes] = _field }
  else if (_code == CODE_PARAMETER) { indexParameters[++numParameters] = _field }
 }
}

# parseOutputLevel
function parseOutputLevel() {
 split(outputLevel, _levels, ",")
 for (_i in _levels) {
  if (_levels[_i] == LEVEL_PRODUCTION) { _status = STATUS_RETRACTED "," STATUS_DOWN "," STATUS_RELEASED }
  else if (_levels[_i] == LEVEL_PRE_RELEASE) { _status = STATUS_PRE_RELEASED }
  else if (_levels[_i] == LEVEL_CONTENT_TESTING) { _status = STATUS_RETRACTED "," STATUS_DOWN "," STATUS_RELEASED "," STATUS_PRE_RELEASED "," STATUS_READY "," STATUS_TESTING "," STATUS_MANIFEST "," STATUS_EXISTS }
  else if (_levels[_i] == LEVEL_CONSERVATIVE_TESTING) { _status = STATUS_RELEASED "," STATUS_PRE_RELEASED "," STATUS_READY "," STATUS_TESTING "," STATUS_MANIFEST }
  else if (_levels[_i] == LEVEL_DANGEROUS) { _status = STATUS_RETRACTED "," STATUS_DOWN "," STATUS_RELEASED "," STATUS_PRE_RELEASED "," STATUS_READY "," STATUS_TESTING "," STATUS_MANIFEST "," STATUS_EXISTS "," STATUS_DO_NOT_PROCESS }
  else if (_levels[_i] == LEVEL_VERY_DANGEROUS) { _status = STATUS_RETRACTED "," STATUS_DOWN "," STATUS_RELEASED "," STATUS_PRE_RELEASED "," STATUS_READY "," STATUS_TESTING "," STATUS_MANIFEST "," STATUS_EXISTS "," STATUS_DO_NOT_PROCESS "," STATUS_DOES_NOT_EXIST }
  else { _status = _levels[_i] }
  split(_status, _statuses, ",")
  for (_j in _statuses) { outputLevels[_statuses[_j]] = true; }
 }
}

# printOneEntry
function printOneEntry() {
 _status = $indexOf[CODE_STATUS]
 if (!outputLevels[_status]) { return }

 printf  "  <property name=\"%s\">\n", getOpaqueName($indexOf[CODE_TITLE])

 if (indexOf[CODE_PUBLISHER] != "") { printOneAttribute("publisher=" xml($indexOf[CODE_PUBLISHER])) }
 printf  "   <property name=\"journalTitle\" value=\"%s\" />\n", xml($indexOf[CODE_JOURNAL_TITLE])
   printf  "   <property name=\"title\" value=\"%s%s\" />\n", xml($indexOf[CODE_TITLE]), (_status == STATUS_PRE_RELEASED ? " (Pre-release)" : "")
 printf  "   <property name=\"plugin\" value=\"%s\" />\n", $indexOf[CODE_PLUGIN]

 for (_param = 1 ; _param <= numParameters ; ++_param) {
  _pair = $indexParameters[_param]
  if (_pair == "") { continue }
  printOneParameter(_param, _pair)
 }
 if (_status == STATUS_DOWN) { printOneParameter(99, "pub_down=true") }

 for (_attr = 1 ; _attr <= numAdditionalAttributes ; ++_attr) {
  _pair = $indexAdditionalAttributes[_attr]
  if (_pair == "") { continue }
  printOneAttribute(_pair)
 }
 if (indexOf[CODE_RIGHTS] != "" && $indexOf[CODE_RIGHTS] == "openaccess") { printOneAttribute("rights=openaccess") }
 if (indexOf[CODE_ESTIMATED_SIZE] != "" && $indexOf[CODE_ESTIMATED_SIZE] != "") {
  printf "   <property name=\"estSize\" value=\"%s\" />\n", $indexOf[CODE_ESTIMATED_SIZE]
 }
 if (_status == STATUS_PRE_RELEASED) { printOneAttribute("releaseStatus=pre-release") }

 printf  "  </property>\n\n"

}

# printOneParameter
function printOneParameter(_num, _pair) {
 printf  "   <property name=\"param.%d\">\n", _num
 printf  "    <property name=\"key\" value=\"%s\" />\n", getKey(_pair)
 printf  "    <property name=\"value\" value=\"%s\" />\n", getValue(_pair)
 printf  "   </property>\n"
}

# printOneAttribute
function printOneAttribute(_pair) {
 printf  "   <property name=\"attributes.%s\" value=\"%s\" />\n", getKey(_pair), getValue(_pair)
}

# getOpaqueName
function getOpaqueName(_str) {
 gsub(/ Volume /, "", _str); # FIXME
 gsub(/&/, "and", _str);
 gsub(/[^a-zA-Z0-9]/, "", _str);
 return namePrefix _str
}

# getKey
function getKey(_str) {
 return match(_str, /^[^=]+=/) ? substr(_str, 1, RLENGTH - 1) : _str
}

# getValue
function getValue(_str) {
 return match(_str, /^[^=]+=/) ? substr(_str, RLENGTH + 1, length(_str) - RLENGTH) : _str
}

# xml
function xml(_str) {
 gsub(/&/, "\&amp;", _str);
 gsub(/</, "\&lt;", _str);
 gsub(/>/, "\&gt;", _str);
 return _str
}

#
# PATTERNS
#

# Blank lines
/^\t*$/ {
 next
}

# Format line
/^# @formatString=/ {
 if (preamble && formatString == "") {
  formatString = substr($1, 17, length($1) - 16)
  parseFormatString()
 }
 next
}

# Name prefix line
/^# @namePrefix=/ {
 if (preamble && namePrefix == "") { namePrefix = substr($1, 15, length($1) - 14) }
 next
}

# Comment line
/^#/ {
 next
}

# Normal line
# unconditional
{
 preamble = false
 if (formatString == "") { exit EXIT_NO_FORMAT_STRING }
 printOneEntry()
}

