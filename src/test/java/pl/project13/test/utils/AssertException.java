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

package pl.project13.test.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import static pl.project13.test.utils.AssertException.ExceptionMatch.EXCEPTION_CLASS_MUST_EQUAL;

/**
 * Allows expecting and intercepting exceptions in a nice way.
 * Use it to intercept exceptions in your tests, in a way that allows
 * sticking to the given/when/then flow, and validate exception throws on
 * <p/>
 * SoftwareBirr 02.2012
 *
 * @author Konrad Malawski (konrad.malawski@java.pl)
 * @see <a href="https://github.com/softwaremill/softwaremill-common/blob/master/softwaremill-test/softwaremill-test-util/src/main/java/pl/softwaremill/common/test/util/AssertException.java">AssertException in softwaremill-common/softwaremill-test-util</a>
 */
public class AssertException {

  public static interface CodeBlock {
    void run() throws Exception;
  }

  public static abstract class ExceptionMatch {

    public static final ExceptionMatch.Strategy EXCEPTION_CLASS_MUST_EQUAL = new Strategy() {
      @Override
      public boolean matchesExpected(Class<? extends Throwable> expectedClass, @NotNull Throwable got, String expectedMessage) {
        return got.getClass().equals(expectedClass);
      }

      public void failWithExpectedButGot(@NotNull Class<? extends Throwable> expectedClass, @NotNull Throwable got, String expectedMessage) {
        Assert.fail(String.format("Expected [%s] to be thrown but got [%s]", expectedClass.getSimpleName(), got.getClass().getSimpleName()));
      }
    };

    /**
     * Please use EXCEPTION_CLASS_MUST_EQUAL instead
     */
    @Deprecated
    public static final ExceptionMatch.Strategy EXCEPTION_MUST_EQUAL = EXCEPTION_CLASS_MUST_EQUAL;

    public static final ExceptionMatch.Strategy EXCEPTION_MAY_BE_SUBCLASS_OF = new Strategy() {
      @Override
      public boolean matchesExpected(@NotNull Class<? extends Throwable> expectedClass, @NotNull Throwable got, String expectedMessage) {
        return expectedClass.isAssignableFrom(got.getClass());
      }

      public void failWithExpectedButGot(@NotNull Class<? extends Throwable> expectedClass, @NotNull Throwable got, String expectedMessage) {
        Assert.fail(String.format("Expected subclass of [%s] to be thrown but got [%s]", expectedClass.getSimpleName(), got.getClass().getSimpleName()));
      }
    };

    public static final ExceptionMatch.Strategy EXCEPTION_CLASS_AND_MESSAGE_MUST_EQUAL = new Strategy() {
      @Override
      public boolean matchesExpected(Class<? extends Throwable> expectedClass, @NotNull Throwable got, @NotNull String expectedMessage) {
        return got.getClass().equals(expectedClass) && expectedMessage.equals(got.getMessage());
      }

      public void failWithExpectedButGot(@NotNull Class<? extends Throwable> expectedClass, @NotNull Throwable got, String expectedMessage) {
        Assert.fail(String.format("Expected [%s] to be thrown with message [%s] but got [%s] with message [%s]", expectedClass.getSimpleName(),
                                  expectedMessage, got.getClass().getSimpleName(), got.getMessage()));
      }
    };

    static interface Strategy {
      boolean matchesExpected(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage);

      void failWithExpectedButGot(Class<? extends Throwable> expectedClass, Throwable got, String expectedMessage);
    }
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public static <T extends Throwable> void thrown(@NotNull ExceptionMatch.Strategy matchStrategy,
                                                  @NotNull Class<T> expectedThrowableClass,
                                                  @NotNull CodeBlock block) {
    intercept(matchStrategy, expectedThrowableClass, block);
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public static <T extends Throwable> void thrownWithMessage(@NotNull ExceptionMatch.Strategy matchStrategy,
                                                             @NotNull Class<T> expectedThrowableClass, String expectedMessage,
                                                             @NotNull CodeBlock block) {
    intercept(matchStrategy, expectedThrowableClass, expectedMessage, block);
  }

  public static <T extends Throwable> void thrown(@NotNull Class<T> expectedThrowableClass,
                                                  @NotNull CodeBlock block) {
    thrown(EXCEPTION_CLASS_MUST_EQUAL, expectedThrowableClass, block);
  }

  @Nullable
  public static <T extends Throwable> T intercept(@NotNull Class<T> expectedThrowableClass,
                                                  @NotNull CodeBlock block) {
    return intercept(EXCEPTION_CLASS_MUST_EQUAL, expectedThrowableClass, block);
  }

  @Nullable
  public static <T extends Throwable> T intercept(@NotNull ExceptionMatch.Strategy matchStrategy,
                                                  @NotNull Class<T> expectedThrowableClass,
                                                  @NotNull CodeBlock block) {
    return intercept(matchStrategy, expectedThrowableClass, null, block);
  }

  @Nullable
  public static <T extends Throwable> T intercept(@NotNull ExceptionMatch.Strategy matchStrategy,
                                                  @NotNull Class<T> expectedThrowableClass, @Nullable String expectedMessage,
                                                  @NotNull CodeBlock block) {
    try {
      block.run();

      failWithExpectedButGotNothing(expectedThrowableClass); // will throw
      return null; // make compiler happy

    } catch (Throwable thr) {
      boolean gotExpectedException = matchStrategy.matchesExpected(expectedThrowableClass, thr, expectedMessage);
      if (gotExpectedException) {
        return expectedThrowableClass.cast(thr);
      } else {
        matchStrategy.failWithExpectedButGot(expectedThrowableClass, thr, expectedMessage);
        return null; // make compiler happy
      }
    }
  }

  private static void failWithExpectedButGotNothing(@NotNull Class<?> expected) {
    Assert.fail(String.format("Expected [%s] to be thrown but no exception was thrown.", expected.getSimpleName()));
  }

}