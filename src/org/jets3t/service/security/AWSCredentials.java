/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2010 James Murty
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.service.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class to contain the Amazon Web Services (AWS) credentials of a user. This class also includes
 * utility methods to store credentials to password-encrypted files, and retrieve credentials from
 * these files.
 *
 * @author James Murty
 * @author Nikolas Coukouma
 */
public class AWSCredentials extends ProviderCredentials {

    /**
     * Construct credentials.
     *
     * @param awsAccessKey
     * AWS access key for an Amazon S3 account.
     * @param awsSecretAccessKey
     * AWS secret key for an Amazon S3 account.
     */
    public AWSCredentials(String awsAccessKey, String awsSecretAccessKey) {
        super(awsAccessKey, awsSecretAccessKey);
    }

    /**
     * Construct credentials, and associate them with a human-friendly name.
     *
     * @param awsAccessKey
     * AWS access key for an Amazon S3 account.
     * @param awsSecretAccessKey
     * AWS secret key for an Amazon S3 account.
     * @param friendlyName
     * a name identifying the owner of the credentials, such as 'James'.
     */
    public AWSCredentials(String awsAccessKey, String awsSecretAccessKey, String friendlyName) {
        super(awsAccessKey, awsSecretAccessKey, friendlyName);
    }

    @Override
    protected String getTypeName() {
        return "regular";
    }

    @Override
    public String getVersionPrefix() {
        return "jets3t AWS Credentials, version: ";
    }

    /**
     * Console utility to store AWS credentials information in an encrypted file in the toolkit's
     * default preferences directory.
     * <p>
     * This class can be run from the command line as:
     * <pre>
     * java org.jets3t.service.security.AWSCredentials &lt;friendlyName> &lt;credentialsFilename> &lt;algorithm>
     * </pre>
     * When run it will prompt for the user's AWS access key,secret key and encryption password.
     * It will then encode into the specified credentials file.
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            printHelp();
            System.exit(1);
        }
        String userName = args[0];
        File encryptedFile = new File(args[1]);
        String algorithm = EncryptionUtil.DEFAULT_ALGORITHM;
        if (args.length == 3) {
            algorithm = args[2];
        }

        // Check arguments provided.
        try {
            FileOutputStream testFOS = new FileOutputStream(encryptedFile);
            testFOS.close();
        } catch (IOException e) {
            System.err.println("Unable to write to file: " + encryptedFile);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // Obtain credentials and password from user.
        System.out.println("Please enter your AWS Credentials");
        System.out.print("Access Key: ");
        String awsAccessKey = reader.readLine();
        System.out.print("Secret Key: ");
        String awsSecretKey = reader.readLine();
        System.out.println("Please enter a password to protect your credentials file (may be empty)");
        System.out.print("Password: ");
        String password = reader.readLine();

        // Create AWSCredentials object and save the details to an encrypted file.
        AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey, userName);
        awsCredentials.save(password, encryptedFile, algorithm);

        System.out.println("Successfully saved AWS Credentials to " + encryptedFile);
    }

    /**
     * Prints help for the use of this class from the console (via the main method).
     */
    private static void printHelp() {
        System.out.println("AWSCredentials <User Name> <File Path> [algorithm]");
        System.out.println();
        System.out.println("User Name: A human-friendly name for the owner of the credentials, e.g. Horace.");
        System.out.println("File Path: Path and name for the encrypted file. Will be replaced if it already exists.");
        System.out.println("Algorithm: PBE encryption algorithm. Defaults to PBEWithMD5AndDES");
    }

}
