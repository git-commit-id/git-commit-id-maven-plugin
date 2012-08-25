/*
 * This file is part of git-commit-id-plugin by Konrad Malawski <konrad.malawski@java.pl>
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

package pl.project13.maven.git.util;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Pair<A, B> {

  @NotNull
  public final A first;

  @NotNull
  public final B second;

  @SuppressWarnings("ConstantConditions")
  public Pair(A first, B second) {
    Preconditions.checkArgument(first != null, "The first parameter must not be null.");
    Preconditions.checkArgument(second != null, "The second parameter must not be null.");

    this.first = first;
    this.second = second;
  }

  @NotNull
  public static <A, B> Pair<A, B> of(A first, B second) {
    return new Pair<A, B>(first, second);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Pair pair = (Pair) o;

    if (!first.equals(pair.first)) {
      return false;
    }
    //noinspection RedundantIfStatement
    if (!second.equals(pair.second)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = first.hashCode();
    result = 31 * result + (second.hashCode());
    return result;
  }

  @NotNull
  @Override
  public String toString() {
    return String.format("Pair(%s, %s)", first, second);
  }
}
