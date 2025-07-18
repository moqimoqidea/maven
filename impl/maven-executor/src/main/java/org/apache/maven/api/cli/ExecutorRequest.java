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
package org.apache.maven.api.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Represents a request to execute Maven with command-line arguments.
 * This interface encapsulates all the necessary information needed to execute
 * Maven command with arguments. The arguments are not parsed, they are just passed over
 * to executed tool.
 *
 * @since 4.0.0
 */
@Immutable
@Experimental
public interface ExecutorRequest {
    /**
     * The Maven command.
     */
    String MVN = "mvn";

    /**
     * The command to execute, ie "mvn".
     */
    @Nonnull
    String command();

    /**
     * The immutable list of arguments to pass to the command.
     */
    @Nonnull
    List<String> arguments();

    /**
     * Returns the current working directory for the Maven execution.
     * This is typically the directory from which Maven was invoked.
     *
     * @return the current working directory path
     */
    @Nonnull
    Path cwd();

    /**
     * Returns the Maven installation directory.
     * This is usually set by the Maven launcher script using the "maven.home" system property.
     *
     * @return the Maven installation directory path
     */
    @Nonnull
    Path installationDirectory();

    /**
     * Returns the user's home directory.
     * This is typically obtained from the "user.home" system property.
     *
     * @return the user's home directory path
     */
    @Nonnull
    Path userHomeDirectory();

    /**
     * Returns the map of Java System Properties to set before executing process.
     *
     * @return an Optional containing the map of Java System Properties, or empty if not specified
     */
    @Nonnull
    Optional<Map<String, String>> jvmSystemProperties();

    /**
     * Returns the map of environment variables to set before executing process.
     * This property is used ONLY by executors that spawn a new JVM.
     *
     * @return an Optional containing the map of environment variables, or empty if not specified
     */
    @Nonnull
    Optional<Map<String, String>> environmentVariables();

    /**
     * Returns the list of extra JVM arguments to be passed to the forked process.
     * These arguments allow for customization of the JVM environment in which tool will run.
     * This property is used ONLY by executors that spawn a new JVM.
     *
     * @return an Optional containing the list of extra JVM arguments, or empty if not specified
     */
    @Nonnull
    Optional<List<String>> jvmArguments();

    /**
     * Optional provider for STD in of the Maven. If given, this provider will be piped into std input of
     * Maven.
     *
     * @return an Optional containing the stdin provider, or empty if not specified.
     */
    Optional<InputStream> stdIn();

    /**
     * Optional consumer for STD out of the Maven. If given, this consumer will get all output from the std out of
     * Maven. Note: whether consumer gets to consume anything depends on invocation arguments passed in
     * {@link #arguments()}, as if log file is set, not much will go to stdout.
     *
     * @return an Optional containing the stdout consumer, or empty if not specified.
     */
    Optional<OutputStream> stdOut();

    /**
     * Optional consumer for STD err of the Maven. If given, this consumer will get all output from the std err of
     * Maven. Note: whether consumer gets to consume anything depends on invocation arguments passed in
     * {@link #arguments()}, as if log file is set, not much will go to stderr.
     *
     * @return an Optional containing the stderr consumer, or empty if not specified.
     */
    Optional<OutputStream> stdErr();

    /**
     * Indicate if {@code ~/.mavenrc} should be skipped during execution.
     * <p>
     * Affected only for forked executor by adding MAVEN_SKIP_RC environment variable
     */
    boolean skipMavenRc();

    /**
     * Returns {@link Builder} created from this instance.
     */
    @Nonnull
    default Builder toBuilder() {
        return new Builder(
                command(),
                arguments(),
                cwd(),
                installationDirectory(),
                userHomeDirectory(),
                jvmSystemProperties().orElse(null),
                environmentVariables().orElse(null),
                jvmArguments().orElse(null),
                stdIn().orElse(null),
                stdOut().orElse(null),
                stdErr().orElse(null),
                skipMavenRc());
    }

    /**
     * Returns new builder pre-set to run Maven. The discovery of maven home is attempted, user cwd and home are
     * also discovered by standard means.
     */
    @Nonnull
    static Builder mavenBuilder(@Nullable Path installationDirectory) {
        return new Builder(
                MVN,
                null,
                getCanonicalPath(Paths.get(System.getProperty("user.dir"))),
                installationDirectory != null
                        ? getCanonicalPath(installationDirectory)
                        : discoverInstallationDirectory(),
                getCanonicalPath(Paths.get(System.getProperty("user.home"))),
                null,
                null,
                null,
                null,
                null,
                null,
                false);
    }

    class Builder {
        private String command;
        private List<String> arguments;
        private Path cwd;
        private Path installationDirectory;
        private Path userHomeDirectory;
        private Map<String, String> jvmSystemProperties;
        private Map<String, String> environmentVariables;
        private List<String> jvmArguments;
        private InputStream stdIn;
        private OutputStream stdOut;
        private OutputStream stdErr;
        private boolean skipMavenRc;

        private Builder() {}

        @SuppressWarnings("ParameterNumber")
        private Builder(
                String command,
                List<String> arguments,
                Path cwd,
                Path installationDirectory,
                Path userHomeDirectory,
                Map<String, String> jvmSystemProperties,
                Map<String, String> environmentVariables,
                List<String> jvmArguments,
                InputStream stdIn,
                OutputStream stdOut,
                OutputStream stdErr,
                boolean skipMavenRc) {
            this.command = command;
            this.arguments = arguments;
            this.cwd = cwd;
            this.installationDirectory = installationDirectory;
            this.userHomeDirectory = userHomeDirectory;
            this.jvmSystemProperties = jvmSystemProperties;
            this.environmentVariables = environmentVariables;
            this.jvmArguments = jvmArguments;
            this.stdIn = stdIn;
            this.stdOut = stdOut;
            this.stdErr = stdErr;
            this.skipMavenRc = skipMavenRc;
        }

        @Nonnull
        public Builder command(String command) {
            this.command = requireNonNull(command, "command");
            return this;
        }

        @Nonnull
        public Builder arguments(List<String> arguments) {
            this.arguments = requireNonNull(arguments, "arguments");
            return this;
        }

        @Nonnull
        public Builder argument(String argument) {
            if (arguments == null) {
                arguments = new ArrayList<>();
            }
            this.arguments.add(requireNonNull(argument, "argument"));
            return this;
        }

        @Nonnull
        public Builder cwd(Path cwd) {
            this.cwd = getCanonicalPath(requireNonNull(cwd, "cwd"));
            return this;
        }

        @Nonnull
        public Builder installationDirectory(Path installationDirectory) {
            this.installationDirectory =
                    getCanonicalPath(requireNonNull(installationDirectory, "installationDirectory"));
            return this;
        }

        @Nonnull
        public Builder userHomeDirectory(Path userHomeDirectory) {
            this.userHomeDirectory = getCanonicalPath(requireNonNull(userHomeDirectory, "userHomeDirectory"));
            return this;
        }

        @Nonnull
        public Builder jvmSystemProperties(Map<String, String> jvmSystemProperties) {
            this.jvmSystemProperties = jvmSystemProperties;
            return this;
        }

        @Nonnull
        public Builder jvmSystemProperty(String key, String value) {
            requireNonNull(key, "env key");
            requireNonNull(value, "env value");
            if (jvmSystemProperties == null) {
                this.jvmSystemProperties = new HashMap<>();
            }
            this.jvmSystemProperties.put(key, value);
            return this;
        }

        @Nonnull
        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        @Nonnull
        public Builder environmentVariable(String key, String value) {
            requireNonNull(key, "env key");
            requireNonNull(value, "env value");
            if (environmentVariables == null) {
                this.environmentVariables = new HashMap<>();
            }
            this.environmentVariables.put(key, value);
            return this;
        }

        @Nonnull
        public Builder jvmArguments(List<String> jvmArguments) {
            this.jvmArguments = jvmArguments;
            return this;
        }

        @Nonnull
        public Builder jvmArgument(String jvmArgument) {
            if (jvmArguments == null) {
                jvmArguments = new ArrayList<>();
            }
            this.jvmArguments.add(requireNonNull(jvmArgument, "jvmArgument"));
            return this;
        }

        @Nonnull
        public Builder stdIn(InputStream stdIn) {
            this.stdIn = stdIn;
            return this;
        }

        @Nonnull
        public Builder stdOut(OutputStream stdOut) {
            this.stdOut = stdOut;
            return this;
        }

        @Nonnull
        public Builder stdErr(OutputStream stdErr) {
            this.stdErr = stdErr;
            return this;
        }

        @Nonnull
        public Builder skipMavenRc(boolean skipMavenRc) {
            this.skipMavenRc = skipMavenRc;
            return this;
        }

        @Nonnull
        public ExecutorRequest build() {
            return new Impl(
                    command,
                    arguments,
                    cwd,
                    installationDirectory,
                    userHomeDirectory,
                    jvmSystemProperties,
                    environmentVariables,
                    jvmArguments,
                    stdIn,
                    stdOut,
                    stdErr,
                    skipMavenRc);
        }

        private static class Impl implements ExecutorRequest {
            private final String command;
            private final List<String> arguments;
            private final Path cwd;
            private final Path installationDirectory;
            private final Path userHomeDirectory;
            private final Map<String, String> jvmSystemProperties;
            private final Map<String, String> environmentVariables;
            private final List<String> jvmArguments;
            private final InputStream stdIn;
            private final OutputStream stdOut;
            private final OutputStream stdErr;
            private final boolean skipMavenRc;

            @SuppressWarnings("ParameterNumber")
            private Impl(
                    String command,
                    List<String> arguments,
                    Path cwd,
                    Path installationDirectory,
                    Path userHomeDirectory,
                    Map<String, String> jvmSystemProperties,
                    Map<String, String> environmentVariables,
                    List<String> jvmArguments,
                    InputStream stdIn,
                    OutputStream stdOut,
                    OutputStream stdErr,
                    boolean skipMavenRc) {
                this.command = requireNonNull(command);
                this.arguments = arguments == null ? List.of() : List.copyOf(arguments);
                this.cwd = getCanonicalPath(requireNonNull(cwd));
                this.installationDirectory = getCanonicalPath(requireNonNull(installationDirectory));
                this.userHomeDirectory = getCanonicalPath(requireNonNull(userHomeDirectory));
                this.jvmSystemProperties = jvmSystemProperties != null ? Map.copyOf(jvmSystemProperties) : null;
                this.environmentVariables = environmentVariables != null ? Map.copyOf(environmentVariables) : null;
                this.jvmArguments = jvmArguments != null ? List.copyOf(jvmArguments) : null;
                this.stdIn = stdIn;
                this.stdOut = stdOut;
                this.stdErr = stdErr;
                this.skipMavenRc = skipMavenRc;
            }

            @Override
            public String command() {
                return command;
            }

            @Override
            public List<String> arguments() {
                return arguments;
            }

            @Override
            public Path cwd() {
                return cwd;
            }

            @Override
            public Path installationDirectory() {
                return installationDirectory;
            }

            @Override
            public Path userHomeDirectory() {
                return userHomeDirectory;
            }

            @Override
            public Optional<Map<String, String>> jvmSystemProperties() {
                return Optional.ofNullable(jvmSystemProperties);
            }

            @Override
            public Optional<Map<String, String>> environmentVariables() {
                return Optional.ofNullable(environmentVariables);
            }

            @Override
            public Optional<List<String>> jvmArguments() {
                return Optional.ofNullable(jvmArguments);
            }

            @Override
            public Optional<InputStream> stdIn() {
                return Optional.ofNullable(stdIn);
            }

            @Override
            public Optional<OutputStream> stdOut() {
                return Optional.ofNullable(stdOut);
            }

            @Override
            public Optional<OutputStream> stdErr() {
                return Optional.ofNullable(stdErr);
            }

            @Override
            public boolean skipMavenRc() {
                return skipMavenRc;
            }

            @Override
            public String toString() {
                return getClass().getSimpleName() + "{" + "command='"
                        + command + '\'' + ", arguments="
                        + arguments + ", cwd="
                        + cwd + ", installationDirectory="
                        + installationDirectory + ", userHomeDirectory="
                        + userHomeDirectory + ", jvmSystemProperties="
                        + jvmSystemProperties + ", environmentVariables="
                        + environmentVariables + ", jvmArguments="
                        + jvmArguments + ", stdinProvider="
                        + stdIn + ", stdoutConsumer="
                        + stdOut + ", stderrConsumer="
                        + stdErr + ", skipMavenRc="
                        + skipMavenRc + "}";
            }
        }
    }

    @Nonnull
    static Path discoverInstallationDirectory() {
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome == null) {
            throw new ExecutorException("requires maven.home Java System Property set");
        }
        return getCanonicalPath(Paths.get(mavenHome));
    }

    @Nonnull
    static Path discoverUserHomeDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new ExecutorException("requires user.home Java System Property set");
        }
        return getCanonicalPath(Paths.get(userHome));
    }

    @Nonnull
    static Path getCanonicalPath(Path path) {
        requireNonNull(path, "path");
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return getCanonicalPath(path.getParent()).resolve(path.getFileName());
        }
    }
}
