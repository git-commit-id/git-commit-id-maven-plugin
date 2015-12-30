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

package pl.project13.maven.git.log;

import org.apache.maven.plugin.Mojo;

/**
 * Bridges logging to standard Maven log adhering to verbosity level.
 */
public class VerboseLogger implements VerboseLog {

    private boolean verbose;
    private final Mojo mojo;

    public VerboseLogger(Mojo mojo, boolean verbose) {
        this.mojo = mojo;
        this.verbose = verbose;
    }

    @Override
    public void debug(CharSequence content) {
        if (verbose) {
            mojo.getLog().debug(content);
        }
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (verbose) {
            mojo.getLog().debug(content, error);
        }
    }

    @Override
    public void debug(Throwable error) {
        if (verbose) {
            mojo.getLog().debug(error);
        }
    }

    @Override
    public void info(CharSequence content) {
        if (verbose) {
            mojo.getLog().info(content);
        }
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        if (verbose) {
            mojo.getLog().info(content, error);
        }
    }

    @Override
    public void info(Throwable error) {
        if (verbose) {
            mojo.getLog().info(error);
        }
    }

    @Override
    public void warn(CharSequence content) {
        if (verbose) {
            mojo.getLog().warn(content);
        }
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        if (verbose) {
            mojo.getLog().warn(content, error);
        }
    }

    @Override
    public void warn(Throwable error) {
        if (verbose) {
            mojo.getLog().warn(error);
        }
    }

    @Override
    public void error(CharSequence content) {
        if (verbose) {
            mojo.getLog().error(content);
        }
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        if (verbose) {
            mojo.getLog().error(content, error);
        }
    }

    @Override
    public void error(Throwable error) {
        if (verbose) {
            mojo.getLog().error(error);
        }
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public boolean isDebugEnabled() {
        return mojo.getLog().isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return mojo.getLog().isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return mojo.getLog().isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return mojo.getLog().isErrorEnabled();
    }
}