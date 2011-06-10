/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
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

import java.io.File;

/**
 * A wrapper class for a File, that makes it possible to recognize a temporary
 * file.
 *
 * @author James Murty
 */
public class TempFile extends File {
    private static final long serialVersionUID = 1971413408549070295L;

    public TempFile(File file) {
        super(file.getAbsolutePath());
    }
}
