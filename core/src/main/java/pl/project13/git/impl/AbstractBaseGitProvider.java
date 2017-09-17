/*
 * This file is part of git-commit-id-plugin by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.git.impl;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.jetbrains.annotations.NotNull;

import pl.project13.git.api.GitDescribeConfig;
import pl.project13.git.api.GitException;
import pl.project13.git.api.GitProvider;
import pl.project13.maven.git.CommitIdGenerationMode;
import pl.project13.maven.git.log.LoggerBridge;

public abstract class AbstractBaseGitProvider<T extends AbstractBaseGitProvider<T>>
        implements GitProvider {

    @NotNull
    protected final LoggerBridge log;

    protected int abbrevLength;

    protected String dateFormat;

    protected String dateFormatTimeZone;

    protected GitDescribeConfig gitDescribe = new GitDescribeConfig();

    protected AbstractBaseGitProvider(@NotNull LoggerBridge log) {
        this.log = log;
    }

    @Override
    public void setAbbrevLength(int abbrevLength) {
        this.abbrevLength = abbrevLength;
    }

    public void setGitDescribe(GitDescribeConfig gitDescribe) {
        this.gitDescribe = gitDescribe;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setDateFormatTimeZone(String dateFormatTimeZone) {
        this.dateFormatTimeZone = dateFormatTimeZone;
    }

    @Override
    public void init() throws GitException {
        // noop ...
    }

    @Override
    public void prepareGitToExtractMoreDetailedReproInformation()
            throws GitException {
        // noop ...
    }

    @Override
    public void finalCleanUp() throws GitException {
        // noop ...
    }

    protected SimpleDateFormat getSimpleDateFormatWithTimeZone(){
        final SimpleDateFormat smf = new SimpleDateFormat(dateFormat);
        if (dateFormatTimeZone != null){
            smf.setTimeZone(TimeZone.getTimeZone(dateFormatTimeZone));
        }
        return smf;
    }
}
