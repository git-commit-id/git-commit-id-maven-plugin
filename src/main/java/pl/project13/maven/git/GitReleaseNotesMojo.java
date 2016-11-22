package pl.project13.maven.git;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;
import pl.project13.maven.git.log.LoggerBridge;
import pl.project13.maven.git.log.MavenLoggerBridge;
import pl.project13.maven.git.release.ReleaseNotes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * This goal can be used to produce release notes between two tags.
 *
 * The release notes are comprised of all commit descriptions from the commit at which the first
 * tag (release tag, possibily) exists and walks the commit graph,
 * till the second (release) tag is reached.
 *
 * Created by pankaj on 10/21/16.
 */
@Mojo(name = "releaseNotes", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class GitReleaseNotesMojo extends GitMojo {

    /**
     * <p>The location of {@code 'release-notes.json'} file.
     *
     * <p>The path here is relative to your project src directory.</p>
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/release-notes.json")
    protected String releaseNotesFileName;

    @Parameter(defaultValue = ".*")
    protected String tagNameRegex;

    @Parameter(defaultValue = ".*")
    protected String commitMessageRegex;

    @Parameter(required = true)
    protected String startTag;
    @Parameter(required = false)
    protected String endTag;

    @NotNull
    private final LoggerBridge log = new MavenLoggerBridge(this, true);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Writer outputWriter = null;
        File fileToCreate = null;
        log.info("Starting releaseNotes generation...");
        try {
            GitDataProvider jGitProvider = JGitProvider
                    .on(dotGitDirectory, log)
                    .setPrefixDot(prefixDot)
                    .setAbbrevLength(abbrevLength)
                    .setDateFormat(dateFormat)
                    .setDateFormatTimeZone(dateFormatTimeZone)
                    .setGitDescribe(gitDescribe)
                    .setCommitIdGenerationMode(commitIdGenerationModeEnum);
            jGitProvider.init();
            ReleaseNotes notes = jGitProvider.generateReleaseNotesBetweenTags(startTag, endTag, commitMessageRegex, tagNameRegex);
            if (notes == null) {
                log.warn("Skipping release notes generation because start tag: {} was not found!", startTag);
            } else {
                //Create release notes file
                fileToCreate = getReleaseNotesFile(project.getBasedir(), releaseNotesFileName);
                Files.createParentDirs(fileToCreate);
                outputWriter = new OutputStreamWriter(new FileOutputStream(fileToCreate), sourceCharset);
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(outputWriter, notes);
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            try {
                if (outputWriter != null) {
                    outputWriter.close();
                    log.info("Wrote release notes to JSON file: {}", fileToCreate.getAbsolutePath());
                }
            }catch (Exception e) {
                //Ignore
            }
        }
    }

    private File getReleaseNotesFile (File base, String releaseNotesFileName) {
        File returnPath = null;

        File currentReleaseNotesFilePath = new File(releaseNotesFileName);
        if (!currentReleaseNotesFilePath.isAbsolute()) {
            returnPath = new File(base, releaseNotesFileName);
        } else {
            returnPath = currentReleaseNotesFilePath;
        }

        return returnPath;
    }

    public String getReleaseNotesFileName() {
        return releaseNotesFileName;
    }
}
