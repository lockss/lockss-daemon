#!/bin/sh

KEY_DIR=${1}
FILE=${2}

gpg --detach-sign --armor --homedir $KEY_DIR --no-random-seed-file $FILE
