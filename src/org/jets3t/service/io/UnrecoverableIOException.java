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
package org.jets3t.service.io;

import java.io.IOException;

/**
 * Indicates an IOException that cannot, or should not, be recovered from. For example, if a user
 * deliberately cancels an upload this exception should be thrown to indicate to JetS3t that the
 * error was intentional.
 *
 * @author James Murty
 */
public class UnrecoverableIOException extends IOException {
    private static final long serialVersionUID = 1423979730178522822L;

    public UnrecoverableIOException(String message) {
        super(message);
    }

}
