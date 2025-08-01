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
package org.apache.maven.graph;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.CycleDetectedException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
class DefaultProjectDependencyGraphTest {

    private final MavenProject aProject = createA();

    private final MavenProject bProject = createProject(Arrays.asList(toDependency(aProject)), "bProject");

    private final MavenProject cProject = createProject(Arrays.asList(toDependency(bProject)), "cProject");

    private final MavenProject dProject = createProject(
            Arrays.asList(toDependency(aProject), toDependency(bProject), toDependency(cProject)), "dProject");

    private final MavenProject eProject = createProject(
            Arrays.asList(
                    toDependency(aProject), toDependency(bProject), toDependency(cProject), toDependency(dProject)),
            "eProject");

    private final MavenProject depender1 = createProject(Arrays.asList(toDependency(aProject)), "depender1");

    private final MavenProject depender2 = createProject(Arrays.asList(toDependency(aProject)), "depender2");

    private final MavenProject depender3 = createProject(Arrays.asList(toDependency(aProject)), "depender3");

    private final MavenProject depender4 =
            createProject(Arrays.asList(toDependency(aProject), toDependency(depender3)), "depender4");

    private final MavenProject transitiveOnly = createProject(Arrays.asList(toDependency(depender3)), "depender5");

    @Test
    void testNonTransitiveFiltering() throws DuplicateProjectException, CycleDetectedException {
        ProjectDependencyGraph graph = new FilteredProjectDependencyGraph(
                new DefaultProjectDependencyGraph(Arrays.asList(aProject, bProject, cProject)),
                Arrays.asList(aProject, cProject));
        final List<MavenProject> sortedProjects = graph.getSortedProjects();
        assertEquals(aProject, sortedProjects.get(0));
        assertEquals(cProject, sortedProjects.get(1));

        assertTrue(graph.getDownstreamProjects(aProject, false).contains(cProject));
    }

    // Test verifying that getDownstreamProjects does not contain duplicates.
    // This is a regression test for https://github.com/apache/maven/issues/2487.
    //
    // The graph is:
    // aProject -> bProject
    //        | -> dProject
    //        | -> eProject
    // bProject -> cProject
    //        | -> dProject
    //        | -> eProject
    // cProject -> dProject
    //        | -> eProject
    // dProject -> eProject
    //
    // When getting the non-transitive, downstream projects of aProject with a whitelist of aProject, dProject,
    // and eProject, we expect to get dProject, and eProject with no duplicates.
    // Before the fix, this would return dProject and eProject twice, once from bProject and once from cProject. As
    // aProject is whitelisted, it should not be returned as a downstream project for itself. bProject and cProject
    // are not whitelisted, so they should return their downstream projects, both have dProject and eProject as
    // downstream projects. Which would result in dProject and eProject being returned twice, but now the results are
    // made unique.
    @Test
    public void testGetDownstreamDoesNotDuplicateProjects() throws CycleDetectedException, DuplicateProjectException {
        ProjectDependencyGraph graph =
                new DefaultProjectDependencyGraph(Arrays.asList(aProject, bProject, cProject, dProject, eProject));
        graph = new FilteredProjectDependencyGraph(graph, Arrays.asList(aProject, dProject, eProject));
        final List<MavenProject> downstreamProjects = graph.getDownstreamProjects(aProject, false);
        assertEquals(2, downstreamProjects.size());
        assertTrue(downstreamProjects.contains(dProject));
        assertTrue(downstreamProjects.contains(eProject));
    }

    @Test
    void testGetSortedProjects() throws DuplicateProjectException, CycleDetectedException {
        ProjectDependencyGraph graph = new DefaultProjectDependencyGraph(Arrays.asList(depender1, aProject));
        final List<MavenProject> sortedProjects = graph.getSortedProjects();
        assertEquals(aProject, sortedProjects.get(0));
        assertEquals(depender1, sortedProjects.get(1));
    }

    @Test
    void testVerifyExpectedParentStructure() throws CycleDetectedException, DuplicateProjectException {
        // This test verifies the baseline structure used in subsequent tests. If this fails, the rest will fail.
        ProjectDependencyGraph graph = threeProjectsDependingOnASingle();
        final List<MavenProject> sortedProjects = graph.getSortedProjects();
        assertEquals(aProject, sortedProjects.get(0));
        assertEquals(depender1, sortedProjects.get(1));
        assertEquals(depender2, sortedProjects.get(2));
        assertEquals(depender3, sortedProjects.get(3));
    }

    @Test
    void testVerifyThatDownstreamProjectsComeInSortedOrder() throws CycleDetectedException, DuplicateProjectException {
        final List<MavenProject> downstreamProjects =
                threeProjectsDependingOnASingle().getDownstreamProjects(aProject, true);
        assertEquals(depender1, downstreamProjects.get(0));
        assertEquals(depender2, downstreamProjects.get(1));
        assertEquals(depender3, downstreamProjects.get(2));
    }

    @Test
    void testTransitivesInOrder() throws CycleDetectedException, DuplicateProjectException {
        final ProjectDependencyGraph graph =
                new DefaultProjectDependencyGraph(Arrays.asList(depender1, depender4, depender2, depender3, aProject));

        final List<MavenProject> downstreamProjects = graph.getDownstreamProjects(aProject, true);
        assertEquals(depender1, downstreamProjects.get(0));
        assertEquals(depender3, downstreamProjects.get(1));
        assertEquals(depender4, downstreamProjects.get(2));
        assertEquals(depender2, downstreamProjects.get(3));
    }

    @Test
    void testNonTransitivesInOrder() throws CycleDetectedException, DuplicateProjectException {
        final ProjectDependencyGraph graph =
                new DefaultProjectDependencyGraph(Arrays.asList(depender1, depender4, depender2, depender3, aProject));

        final List<MavenProject> downstreamProjects = graph.getDownstreamProjects(aProject, false);
        assertEquals(depender1, downstreamProjects.get(0));
        assertEquals(depender3, downstreamProjects.get(1));
        assertEquals(depender4, downstreamProjects.get(2));
        assertEquals(depender2, downstreamProjects.get(3));
    }

    @Test
    void testWithTransitiveOnly() throws CycleDetectedException, DuplicateProjectException {
        final ProjectDependencyGraph graph = new DefaultProjectDependencyGraph(
                Arrays.asList(depender1, transitiveOnly, depender2, depender3, aProject));

        final List<MavenProject> downstreamProjects = graph.getDownstreamProjects(aProject, true);
        assertEquals(depender1, downstreamProjects.get(0));
        assertEquals(depender3, downstreamProjects.get(1));
        assertEquals(transitiveOnly, downstreamProjects.get(2));
        assertEquals(depender2, downstreamProjects.get(3));
    }

    @Test
    void testWithMissingTransitiveOnly() throws CycleDetectedException, DuplicateProjectException {
        final ProjectDependencyGraph graph = new DefaultProjectDependencyGraph(
                Arrays.asList(depender1, transitiveOnly, depender2, depender3, aProject));

        final List<MavenProject> downstreamProjects = graph.getDownstreamProjects(aProject, false);
        assertEquals(depender1, downstreamProjects.get(0));
        assertEquals(depender3, downstreamProjects.get(1));
        assertEquals(depender2, downstreamProjects.get(2));
    }

    @Test
    void testGetUpstreamProjects() throws CycleDetectedException, DuplicateProjectException {
        ProjectDependencyGraph graph = threeProjectsDependingOnASingle();
        final List<MavenProject> downstreamProjects = graph.getUpstreamProjects(depender1, true);
        assertEquals(aProject, downstreamProjects.get(0));
    }

    private ProjectDependencyGraph threeProjectsDependingOnASingle()
            throws CycleDetectedException, DuplicateProjectException {
        return new DefaultProjectDependencyGraph(Arrays.asList(depender1, depender2, depender3, aProject));
    }

    private static MavenProject createA() {
        MavenProject result = new MavenProject();
        result.setGroupId("org.apache");
        result.setArtifactId("A");
        result.setVersion("1.2");
        return result;
    }

    static Dependency toDependency(MavenProject mavenProject) {
        final Dependency dependency = new Dependency();
        dependency.setArtifactId(mavenProject.getArtifactId());
        dependency.setGroupId(mavenProject.getGroupId());
        dependency.setVersion(mavenProject.getVersion());
        return dependency;
    }

    private static MavenProject createProject(List<Dependency> dependencies, String artifactId) {
        MavenProject result = new MavenProject();
        result.setGroupId("org.apache");
        result.setArtifactId(artifactId);
        result.setVersion("1.2");
        result.setDependencies(dependencies);
        return result;
    }
}
