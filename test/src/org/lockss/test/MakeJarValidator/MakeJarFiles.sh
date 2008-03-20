#!/bin/sh

# This method generates jar files for the tests of JarValidator.
# It only needs to be run once; thereafter, use the generated .jar files.
# (It's in the repository so that you can see how the files were generated.)

# The below tests were written based on "JAR File Tests" by Brent E. Edwards.
  
# *** Single-.java file tests: 

# JAR file of one .java file with a good signature
jar cfm Good1.jar SimpleJava.manifest SimpleJava.class SimpleJava.java
jarsigner -keystore "keystore" -storepass "keyskeys" -keypass "goodgood" Good1.jar good 

# JAR file of one .java file whose signature is expired
jar cfm Expired1.jar SimpleJava.manifest SimpleJava.class SimpleJava.java
jarsigner -keystore "keystore" -storepass "keyskeys" -keypass "expired" Expired1.jar expired

# JAR file of one .java file whose signature is not yet created.
jar cfm Future1.jar SimpleJava.manifest SimpleJava.class SimpleJava.java
jarsigner -keystore "keystore" -storepass "keyskeys" -keypass "future" Future1.jar future

# JAR files of one .java file without a signature
jar cfm Unsigned1.jar SimpleJava.manifest SimpleJava.class SimpleJava.java

# JAR files of one .java file whose signature does not match the file
# Sadly, I couldn't find a way to do this automatically; to reproduce,
# uncomment the next two lines, then use a hex editor to change the 
# META-INF/GOOD.DSA file.
#jar cfm0 Modified1.jar SimpleJava.manifest SimpleJava.class SimpleJava.java
#jarsigner -keystore "keystore" -storepass "keyskeys" -keypass "goodgood" Modified1.jar good 

# JAR files of one .java file with a good signature from a source that it does not recognize
jar cfm Unrecognized1.jar SimpleJava.manifest SimpleJava.class SimpleJava.java
jarsigner -keystore "keystore2" -storepass "keyskeys" -keypass "goodgood" Unrecognized1.jar good2

# *** Multiple-.java file tests.  (In each test, multiple files will be present with one file under the given condition.)

# JAR file with one .java file with a good signature
jar cfm Good2.jar SimpleJava2.manifest SimpleJava.class SimpleJava.java SimpleJava2.class SimpleJava2.java
jarsigner -keystore "keystore" -storepass "keyskeys" -keypass "goodgood" Good2.jar good 

# JAR file with one .java file whose signature is expired
jar cfm Expired2.jar SimpleJava2.manifest SimpleJava.class SimpleJava.java SimpleJava2.class SimpleJava2.java
jarsigner -keystore "keystore" -storepass "keyskeys" -keypass "expired" Expired2.jar expired

# JAR file with one .java file whose signature is not yet created.
jar cfm Future2.jar SimpleJava2.manifest SimpleJava.class SimpleJava.java SimpleJava2.class SimpleJava2.java
jarsigner -keystore "keystore" -storepass "keyskeys" -keypass "future" Future2.jar future

# JAR files with one .java file without a signature
jar cfm Unsigned2.jar SimpleJava2.manifest SimpleJava.class SimpleJava.java SimpleJava2.class SimpleJava2.java

# JAR files with one .java file whose signature does not match the file
# Sadly, I couldn't find a way to do this automatically; to reproduce,
# uncomment the next two lines, then use a hex editor to change the 
# META-INF/GOOD.DSA file.
#jar cfm0 Modified2.jar SimpleJava2.manifest SimpleJava.class SimpleJava.java SimpleJava2.class SimpleJava2.java
#jarsigner -keystore "keystore" -storepass "keyskeys" -keypass "goodgood" Modified2.jar good 

# JAR files with one .java file with a good signature from a source that it does not recognize
jar cfm Unrecognized2.jar SimpleJava2.manifest SimpleJava.class SimpleJava.java SimpleJava2.class SimpleJava2.java
jarsigner -keystore "keystore2" -storepass "keyskeys" -keypass "goodgood" Unrecognized2.jar good2

