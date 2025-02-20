/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.it;

import java.io.File;

import org.junit.jupiter.api.Test;

public class MavenITmng8133RootDirectoryInParentTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng8133RootDirectoryInParentTest() {
        super("[4.0.0-beta-5,)");
    }

    @Test
    public void testRootDirectoryInParent() throws Exception {
        File testDir = extractResources("/mng-8133-root-directory-in-parent");

        Verifier verifier = newVerifier(new File(testDir, "parent").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "child").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
