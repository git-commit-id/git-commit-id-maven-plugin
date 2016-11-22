package pl.project13.maven.git;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import junitparams.JUnitParamsRunner;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import pl.project13.maven.git.release.ReleaseNotes;

import java.io.File;
import java.nio.charset.Charset;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by pankaj on 10/21/16.
 */
@RunWith(JUnitParamsRunner.class)
public class GitReleaseNotesMojoIntegrationTests extends GitIntegrationTest {
    @Test
    public void testNotesBetweenTwoConsecutiveTags() throws Exception {
        // given
        mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.RELEASE_NOTES_BASIC)
                .create();

        MavenProject targetProject = mavenSandbox.getChildProject();
        setProjectToExecuteMojoIn(targetProject);

        alterMojoSettings("useNativeGit", false);

        alterMojoSettings("commitMessageRegex", ".REL:*");
        alterMojoSettings("startTag", "R_1.0.2");
        alterMojoSettings("endTag", "R_1.0.1");
        alterMojoSettings("releaseNotesFileName", "release-notes.json");

        //Given
        String targetFilePath = "release-notes.json";
        File expectedFile = new File(targetProject.getBasedir(), targetFilePath);
        try {
            // when
            mojo.execute();

            // then
            assertThat(expectedFile).exists();
            String json = Files.toString(expectedFile, Charset.forName("UTF-8"));
            ObjectMapper om = new ObjectMapper();
            ReleaseNotes notes =  new ReleaseNotes();
            notes = om.readValue(json, notes.getClass());
            assertThat(notes).isNotNull();
            assertThat(notes.getTagList()).isNotNull();
            assertThat(notes.getTagList().size()).isEqualTo(1);
            assertThat(notes.getTagList().get(0).getFeatureList()).isNotNull();
            assertThat(notes.getTagList().get(0).getFeatureList().size()).isEqualTo(5);
        } finally {
            FileUtils.forceDelete(expectedFile);
        }
    }

    @Test
    public void testNotesBetweenTwoTags() throws Exception {
        // given
        mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.RELEASE_NOTES_BASIC)
                .create();

        MavenProject targetProject = mavenSandbox.getChildProject();
        setProjectToExecuteMojoIn(targetProject);

        alterMojoSettings("useNativeGit", false);

        alterMojoSettings("commitMessageRegex", ".REL:*");
        alterMojoSettings("startTag", "R_1.0.2");
        alterMojoSettings("endTag", "R_1.0.0");
        alterMojoSettings("releaseNotesFileName", "release-notes.json");

        //Given
        String targetFilePath = "release-notes.json";
        File expectedFile = new File(targetProject.getBasedir(), targetFilePath);
        try {
            // when
            mojo.execute();

            // then
            assertThat(expectedFile).exists();
            String json = Files.toString(expectedFile, Charset.forName("UTF-8"));
            ObjectMapper om = new ObjectMapper();
            ReleaseNotes notes =  new ReleaseNotes();
            notes = om.readValue(json, notes.getClass());
            assertThat(notes).isNotNull();
            assertThat(notes.getTagList()).isNotNull();
            assertThat(notes.getTagList().size()).isEqualTo(2);
            assertThat(notes.getTagList().get(0).getFeatureList()).isNotNull();
            assertThat(notes.getTagList().get(0).getFeatureList().size()).isEqualTo(5);
            assertThat(notes.getTagList().get(1).getFeatureList()).isNotNull();
            assertThat(notes.getTagList().get(1).getFeatureList().size()).isEqualTo(5);
        } finally {
            FileUtils.forceDelete(expectedFile);
        }
    }

    @Test
    public void testNotesWithOneTagSpecified() throws Exception {
        // given
        mavenSandbox.withParentProject("my-pom-project", "pom")
                .withChildProject("my-jar-module", "jar")
                .withGitRepoInChild(AvailableGitTestRepo.RELEASE_NOTES_BASIC)
                .create();

        MavenProject targetProject = mavenSandbox.getChildProject();
        setProjectToExecuteMojoIn(targetProject);

        alterMojoSettings("useNativeGit", false);

        alterMojoSettings("commitMessageRegex", ".REL:*");
        alterMojoSettings("startTag", "R_1.0.2");
        //alterMojoSettings("endTag", "R_1.0.0");
        alterMojoSettings("releaseNotesFileName", "release-notes.json");

        //Given
        String targetFilePath = "release-notes.json";
        File expectedFile = new File(targetProject.getBasedir(), targetFilePath);
        try {
            // when
            mojo.execute();

            // then
            assertThat(expectedFile).exists();
            String json = Files.toString(expectedFile, Charset.forName("UTF-8"));
            ObjectMapper om = new ObjectMapper();
            ReleaseNotes notes =  new ReleaseNotes();
            notes = om.readValue(json, notes.getClass());
            assertThat(notes).isNotNull();
            assertThat(notes.getTagList()).isNotNull();
            assertThat(notes.getTagList().size()).isEqualTo(2);
            assertThat(notes.getTagList().get(0).getFeatureList()).isNotNull();
            assertThat(notes.getTagList().get(0).getFeatureList().size()).isEqualTo(5);
            assertThat(notes.getTagList().get(1).getFeatureList()).isNotNull();
            assertThat(notes.getTagList().get(1).getFeatureList().size()).isEqualTo(5);
        } finally {
            FileUtils.forceDelete(expectedFile);
        }
    }


    @Override
    public GitMojo getGitMojo() {
        return new GitReleaseNotesMojo();
    }
}
