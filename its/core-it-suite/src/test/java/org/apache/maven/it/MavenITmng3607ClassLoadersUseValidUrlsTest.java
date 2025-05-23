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
import java.net.URI;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3607">MNG-3607</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3607ClassLoadersUseValidUrlsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3607ClassLoadersUseValidUrlsTest() {
        super("[3.0-alpha-2,)");
    }

    /**
     * Test that class loaders created by Maven employ valid URLs, e.g. properly encode characters like spaces.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-3607");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.getSystemProperties().setProperty("test.home", testDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties("target/pcl.properties");
        String url = props.getProperty("maven-core-it.properties");
        assertNotNull(url);
        assertNotNull(new URI(url));
        assertTrue(url.indexOf(' ') < 0);
    }
}
