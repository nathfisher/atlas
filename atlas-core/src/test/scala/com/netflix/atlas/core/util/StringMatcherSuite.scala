/*
 * Copyright 2014-2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.atlas.core.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.regex.Pattern

import org.scalatest.FunSuite

class StringMatcherSuite extends FunSuite {

  import com.netflix.atlas.core.util.StringMatcher._

  private def re(s: String): Pattern = Pattern.compile(s)

  private def reic(s: String): Pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE)

  test("matches All") {
    assert(All.matches("foo"))
  }

  test("matches StartsWith") {
    assert(StartsWith("f").matches("foo"))
    assert(StartsWith("foo").matches("foo"))
    assert(!StartsWith("f").matches("bar"))
    assert(!StartsWith("bar").matches("foobar"))
  }

  test("matches IndexOf") {
    assert(IndexOf("f").matches("foo"))
    assert(IndexOf("foo").matches("foo"))
    assert(IndexOf("oo").matches("foo"))
    assert(!IndexOf("f").matches("bar"))
    assert(!IndexOf("bar").matches("fooBar"))
  }

  test("matches IndexOfIgnoreCase") {
    assert(IndexOfIgnoreCase("f").matches("foo"))
    assert(IndexOfIgnoreCase("foo").matches("foo"))
    assert(IndexOfIgnoreCase("oO").matches("foo"))
    assert(!IndexOfIgnoreCase("F").matches("bar"))
    assert(IndexOfIgnoreCase("bar").matches("fooBar"))
  }

  test("matches Regex") {
    assert(Regex(re("^f")).matches("foo"))
    assert(Regex(re("f")).matches("foo"))
    assert(Regex(re("foo")).matches("foo"))
    assert(Regex(re("oo")).matches("foo"))
    assert(!Regex(re("Foo")).matches("foo"))
  }

  test("matches RegexIgnoreCase") {
    assert(Regex(reic("^f")).matches("foo"))
    assert(Regex(reic("f")).matches("foo"))
    assert(Regex(reic("foo")).matches("foo"))
    assert(Regex(reic("oo")).matches("foO"))
    assert(Regex(reic("Foo")).matches("foo"))
  }

  test("matches Or") {
    val matcher = StringMatcher.compile("^f|foo$|bar")
    assert(matcher.matches("foo"))
    assert(matcher.matches("fabc"))
    assert(!matcher.matches("def"))
    assert(matcher.matches("def foo"))
    assert(!matcher.matches("def foo ghi"))
    assert(matcher.matches("def bar"))
    assert(matcher.matches("def bar ghi"))
  }

  test("compile All") {
    assert(compile(".*") === All)
    assert(compile("^.*$") === All)
    assert(compile("^^^.*$$$") === All)
  }

  test("compile StartsWith") {
    assert(compile("^foo.*") === StartsWith("foo"))
  }

  test("compile StartsWith and dot") {
    assert(compile("^foo.bar.*") === PrefixedRegex("foo", re("^foo.bar.*")))
  }

  test("compile StartsWithIgnoreCase") {
    assert(compile("^foo.*", false) === Regex(reic("^foo.*")))
  }

  test("compile Equals") {
    assert(compile("^foo$") === Equals("foo"))
  }

  test("compile EqualsIgnoreCase") {
    assert(compile("^foo$", false) === EqualsIgnoreCase("foo"))
  }

  test("compile starting glob") {
    // Make sure this doesn't get mapped to an index of query
    // https://github.com/Netflix/atlas/issues/841
    assert(compile("^*foo*") === Regex(re("^*foo*")))
  }

  test("compile IndexOf") {
    assert(compile("foo") === IndexOf("foo"))
    assert(compile(".*foo.*") === IndexOf("foo"))
    assert(compile("^.*foo.*$") === IndexOf("foo"))
  }

  test("compile IndexOfIgnoreCase") {
    assert(compile("foo", false) === IndexOfIgnoreCase("foo"))
    assert(compile(".*foo.*", false) === IndexOfIgnoreCase("foo"))
    assert(compile("^.*foo.*$", false) === IndexOfIgnoreCase("foo"))
  }

  test("compile Prefix") {
    val prefix = "foo"
    assert(compile("^foo[bar]") === PrefixedRegex(prefix, re("^foo[bar]")))
    assert(compile("^foo[bar].*") === PrefixedRegex(prefix, re("^foo[bar].*")))
    assert(compile("^foo[bar].*$") === PrefixedRegex(prefix, re("^foo[bar].*$")))
  }

  test("compile PrefixIgnoreCase") {
    assert(compile("^foo[bar]", false) === Regex(reic("^foo[bar]")))
    assert(compile("^foo[bar].*", false) === Regex(reic("^foo[bar].*")))
    assert(compile("^foo[bar].*$", false) === Regex(reic("^foo[bar].*$")))
  }

  test("compile Regex") {
    assert(compile("^.*foo[bar]") === Regex(re("^.*foo[bar]")))
    assert(compile("^.*foo[bar].*") === Regex(re("^.*foo[bar].*")))
    assert(compile("^.*foo[bar].*$") === Regex(re("^.*foo[bar].*$")))
  }

  test("compile RegexIgnoreCase") {
    assert(compile("^.*foo[bar]", false) === Regex(reic("^.*foo[bar]")))
    assert(compile("^.*foo[bar].*", false) === Regex(reic("^.*foo[bar].*")))
    assert(compile("^.*foo[bar].*$", false) === Regex(reic("^.*foo[bar].*$")))
  }

  test("compile Regex end anchor") {
    assert(compile("^foo[1-3]$") === PrefixedRegex("foo", re("^foo[1-3]$")))
  }

  test("compile RegexIgnoreCase end anchor") {
    assert(compile("^foo[1-3]$", false) === Regex(reic("^foo[1-3]$")))
  }

  test("compile Or anchored at start and end") {
    assert(compile("^(a|b|c)$", true) === Or(List(Equals("a"), Equals("b"), Equals("c"))))
  }

  test("compile Or anchored at start only") {
    assert(
      compile("^(a|b|c)", true) === Or(List(StartsWith("a"), StartsWith("b"), StartsWith("c")))
    )
    assert(compile("^a|b|c", true) === Or(List(StartsWith("a"), IndexOf("b"), IndexOf("c"))))
    assert(
      compile("^.*a.*|.*b.*|c*)", true) === Or(
        List(IndexOf("a"), IndexOf("b"), Regex(re("c*")))
      )
    )
  }

  test("compile Or not anchored") {
    assert(compile("(a|b|c)", true) === Or(List(IndexOf("a"), IndexOf("b"), IndexOf("c"))))
    assert(
      compile(".*a.*|.*b.*|c*)", true) === Or(
        List(IndexOf("a"), IndexOf("b"), Regex(re("c*")))
      )
    )
  }

  test("compile Or, too complex") {
    assert(compile("(a(d|e)|b|c)", true) === Regex(re("(a(d|e)|b|c)")))
  }

  private def serde(pattern: String): StringMatcher = {
    val baos = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(baos)
    out.writeObject(compile(pattern))
    out.close()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in = new ObjectInputStream(bais)
    in.readObject().asInstanceOf[StringMatcher]
  }

  test("Regex serializability") {
    val matcher = serde(".*(a|b).*")
    assert(matcher.matches("foobar"))
  }

  test("PrefixedRegex serializability") {
    val matcher = serde("^foo(a|b).*")
    assert(matcher.matches("foobar"))
  }
}
