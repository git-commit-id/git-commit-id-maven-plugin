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

package pl.project13.maven.git;

/**
 * Exception used by plugin. Plugin code should operate using this exception, which can then be wrapped into
 * build-tool specific exception at the top level.
 */
public class GitCommitIdExecutionException extends Exception {
    private static final long serialVersionUID = 4608506012492555968L;

    public GitCommitIdExecutionException() {
        super();
    }

    public GitCommitIdExecutionException(String message) {
        super(message);
    }

    public GitCommitIdExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitCommitIdExecutionException(Throwable cause) {
        super(cause);
    }

    public GitCommitIdExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}