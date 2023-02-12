package pl.project13.maven.git;

import pl.project13.core.*;
import pl.project13.core.cibuild.BuildServerDataProvider;
import pl.project13.core.git.GitDescribeConfig;
import pl.project13.core.log.LoggerBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public class Externalize {
    protected interface Callback {
        Supplier<String> supplyProjectVersion();

        @Nonnull
        LoggerBridge getLoggerBridge();

        @Nonnull
        String getDateFormat();

        @Nonnull
        String getDateFormatTimeZone();

        @Nonnull
        String getPrefixDot();

        List<String> getExcludeProperties();

        List<String> getIncludeOnlyProperties();

        @Nullable
        Date getReproducibleBuildOutputTimestamp() throws GitCommitIdExecutionException;

        boolean useNativeGit();

        long getNativeGitTimeoutInMs();

        int getAbbrevLength();

        // TODO: this can't be externalized!
        GitDescribeConfig getGitDescribe();

        CommitIdGenerationMode getCommitIdGenerationMode();

        boolean getUseBranchNameFromBuildEnvironment();

        boolean isOffline();

        String getEvaluateOnCommit();

        File getDotGitDirectory();

        File getProjectBaseDir() throws IOException;
    }

    protected static void loadBuildData(@Nonnull Callback cb, @Nonnull Properties properties) throws GitCommitIdExecutionException {
        Map<String, Supplier<String>> additionalProperties = Collections.singletonMap(
                GitCommitPropertyConstant.BUILD_VERSION, cb.supplyProjectVersion());
        BuildServerDataProvider buildServerDataProvider = BuildServerDataProvider.getBuildServerProvider(
                System.getenv(), cb.getLoggerBridge());
        buildServerDataProvider
                .setDateFormat(cb.getDateFormat())
                .setDateFormatTimeZone(cb.getDateFormatTimeZone())
                .setAdditionalProperties(additionalProperties)
                .setPrefixDot(cb.getPrefixDot())
                .setExcludeProperties(cb.getExcludeProperties())
                .setIncludeOnlyProperties(cb.getIncludeOnlyProperties());
        buildServerDataProvider.loadBuildData(properties, cb.getReproducibleBuildOutputTimestamp());
    }

    protected static void loadGitData(@Nonnull Callback cb, @Nonnull Properties properties) throws GitCommitIdExecutionException {
        if (cb.useNativeGit()) {
            loadGitDataWithNativeGit(cb, properties);
        } else {
            loadGitDataWithJGit(cb, properties);
        }
    }

    private static void loadGitDataWithNativeGit(@Nonnull Callback cb, @Nonnull Properties properties) throws GitCommitIdExecutionException {
        try {
            // TODO: Why does this not use dotGitDir.parent or something?
            final File basedir = cb.getProjectBaseDir();

            GitDataProvider nativeGitProvider = NativeGitProvider
                    .on(basedir, cb.getNativeGitTimeoutInMs(), cb.getLoggerBridge())
                    .setPrefixDot(cb.getPrefixDot())
                    .setAbbrevLength(cb.getAbbrevLength())
                    .setDateFormat(cb.getDateFormat())
                    .setDateFormatTimeZone(cb.getDateFormatTimeZone())
                    .setGitDescribe(cb.getGitDescribe())
                    .setCommitIdGenerationMode(cb.getCommitIdGenerationMode())
                    .setUseBranchNameFromBuildEnvironment(cb.getUseBranchNameFromBuildEnvironment())
                    .setExcludeProperties(cb.getExcludeProperties())
                    .setIncludeOnlyProperties(cb.getIncludeOnlyProperties())
                    .setOffline(cb.isOffline());

            nativeGitProvider.loadGitData(cb.getEvaluateOnCommit(), properties);
        } catch (IOException e) {
            throw new GitCommitIdExecutionException(e);
        }
    }

    private static void loadGitDataWithJGit(@Nonnull Callback cb, @Nonnull Properties properties) throws GitCommitIdExecutionException {
        GitDataProvider jGitProvider = JGitProvider
                .on(cb.getDotGitDirectory(), cb.getLoggerBridge())
                .setPrefixDot(cb.getPrefixDot())
                .setAbbrevLength(cb.getAbbrevLength())
                .setDateFormat(cb.getDateFormat())
                .setDateFormatTimeZone(cb.getDateFormatTimeZone())
                .setGitDescribe(cb.getGitDescribe())
                .setCommitIdGenerationMode(cb.getCommitIdGenerationMode())
                .setUseBranchNameFromBuildEnvironment(cb.getUseBranchNameFromBuildEnvironment())
                .setExcludeProperties(cb.getExcludeProperties())
                .setIncludeOnlyProperties(cb.getIncludeOnlyProperties())
                .setOffline(cb.isOffline());

        jGitProvider.loadGitData(cb.getEvaluateOnCommit(), properties);
    }
}
