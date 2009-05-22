#!/bin/sh
for i in 1 2 3 4
do  cd test$i
    rm -rf V1 V3 cache config dpid history iddb jvm_args local.opt localA localB plugins simcontent test.out v3state
    cd ..
done
