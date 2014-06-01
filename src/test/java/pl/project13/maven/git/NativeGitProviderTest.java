package pl.project13.maven.git;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class NativeGitProviderTest {

  GitCommitIdMojo mojo;
  NativeGitProvider nativeGitProvider;

  @Before
  public void setUp() throws Exception {
    File dotGitDirectory = new File(new File(".git/").getAbsolutePath());
    GitDescribeConfig gitDescribeConfig = new GitDescribeConfig();
    gitDescribeConfig.setSkip(false);

    String prefix = "git";
    int abbrevLength = 7;
    String dateFormat = "dd.MM.yyyy '@' HH:mm:ss z";
    boolean verbose = true;

    mojo = new GitCommitIdMojo();
    mojo.setDotGitDirectory(dotGitDirectory);
    mojo.setPrefix(prefix);
    mojo.setAbbrevLength(abbrevLength);
    mojo.setDateFormat(dateFormat);
    mojo.setVerbose(verbose);
    mojo.useNativeGit(false);
    mojo.setGitDescribe(gitDescribeConfig);


    mojo.runningTests = true;
    mojo.project = mock(MavenProject.class, RETURNS_MOCKS);
    when(mojo.project.getPackaging()).thenReturn("jar");

    nativeGitProvider = NativeGitProvider.on(mojo.lookupGitDirectory()).withLoggerBridge(mojo.getLoggerBridge());
  }

  @Test
  public void testGetOriginRemoteWithNewLine() throws MojoExecutionException {
    // given
    String expectedRemoteUrl = "git@github.com:JohnDoe/maven-git-commit-id-plugin.git";

    NativeGitProvider nativeGit = mock(NativeGitProvider.class);
    StringBuilder testRemoteURL = new StringBuilder();
    testRemoteURL.append("remote git://github.com/JaneDoe/maven-git-commit-id-plugin.git (fetch)\n");
    testRemoteURL.append("remote git://github.com/JaneDoe/maven-git-commit-id-plugin.git (push)\n");

    testRemoteURL.append("origin "+expectedRemoteUrl+" (fetch)\n");
    testRemoteURL.append("origin "+expectedRemoteUrl+" (push)");

    when(nativeGit.runGitCommand(any(File.class),any(String.class))).thenReturn(testRemoteURL.toString());
    when(nativeGit.getOriginRemote(any(File.class))).thenCallRealMethod();

    // when
    String result = nativeGit.getOriginRemote(new File(".git"));
    
    // then
    assertThat(result).isNotNull();
    assertThat(result.equals(expectedRemoteUrl)).isTrue();
  }

  @Test
  public void testGetOriginRemoteWithNoOriginAvailable() throws MojoExecutionException {
    // given
    NativeGitProvider nativeGit = mock(NativeGitProvider.class);
    StringBuilder testRemoteURL = new StringBuilder();
    testRemoteURL.append("remote git://github.com/JaneDoe/maven-git-commit-id-plugin.git (fetch)\n");
    testRemoteURL.append("remote git://github.com/JaneDoe/maven-git-commit-id-plugin.git (push)");

    when(nativeGit.runGitCommand(any(File.class),any(String.class))).thenReturn(testRemoteURL.toString());
    when(nativeGit.getOriginRemote(any(File.class))).thenCallRealMethod();

    // when
    String result = nativeGit.getOriginRemote(new File(".git"));

    // then
    assertThat(result).isNotNull();
  }

}
