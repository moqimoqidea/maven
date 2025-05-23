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
package org.apache.maven.api.services;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.toolchain.PersistedToolchains;

/**
 *
 * @since 4.0.0
 */
@Experimental
public interface ToolchainsBuilderResult extends Result<ToolchainsBuilderRequest> {
    /**
     * Gets the assembled toolchains.
     *
     * @return the assembled toolchains, never {@code null}
     */
    @Nonnull
    PersistedToolchains getEffectiveToolchains();

    /**
     * Gets the problems that were encountered during the settings building. Note that only problems of severity
     * {@link BuilderProblem.Severity#WARNING} and below are reported here. Problems with a higher severity level cause
     * the settings builder to fail with a {@link ToolchainsBuilderException}.
     *
     * @return the problems that were encountered during the settings building, can be empty but never {@code null}
     */
    @Nonnull
    ProblemCollector<BuilderProblem> getProblems();
}
