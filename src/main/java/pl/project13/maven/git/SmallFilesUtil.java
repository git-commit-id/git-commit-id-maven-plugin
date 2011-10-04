/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.project13.maven.git;

import java.io.File;
import java.io.IOException;

/**
 * Just one method from Google Guava, so instead of requiring the full library
 * I've used it like this.
 *
 * If you think pulling the entire Guava-Library is a better idea,
 * tell me in an issue or email! :-)
 */
public class SmallFilesUtil {

   public static void createParentDirs(File file) throws IOException {
     File parent = file.getCanonicalFile().getParentFile();
     if (parent == null) {
       return;
     }
     parent.mkdirs();
     if (!parent.isDirectory()) {
       throw new IOException("Unable to create parent directories of " + file);
     }
   }

}
