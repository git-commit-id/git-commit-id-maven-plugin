Git describe - short intro to an awesome command
==================================================
Git's [describe command](http://www.kernel.org/pub/software/scm/git/docs/git-describe.html) is the best way to really see "where" a commit is in the repositories "timeline". 

In svn you could easily determine by looking at two revisions (their numbers) which one is "newer" (they look like that `r239`, `r240` ...). Since git is using SHA-1 checksums to identify commits, it's hard to tell which one is "newer" (or can you tell me? `b6a73ed` or `9597545`?). Using describe you can get a part of this back, and even more - you can know the "nearest" tag for a commit. And as tags are used for versioning most of the time that's super useful to track development progress.

Let's get an example to explain git-describe on it:

```
* 2414721 - (HEAD, master) third addition (8 hours ago) <Konrad Malawski>
| d37a598 - second line (8 hours ago) <Konrad Malawski>
| 9597545 - (v1.0) initial commit (8 hours ago) <Konrad Malawski>
```

Running git-describe when you're on the HEAD here, will yield:

```
> git describe
  v.1.0-2-b2414721
```

The format of a describe result is defined as:

```
v1.0-2-g2414721-DEV
 ^   ^  ^       ^
 |   |  |       \-- if a dirtyMarker was given, it will appear here if the repository is in "dirty" state
 |   |  \---------- the "g" prefixed commit id. The prefix is compatible with what git-describe would return - weird, but true.
 |   \------------- the number of commits away from the found tag. So "2414721" is 2 commits ahead of "v1.0", in this example.
 \----------------- the "nearest" tag, to the mentioned commit.
```

Other outputs may look like:   

* **v1.0** - if the repository is "on a tag" (though describe can be forced to print **v1.0.4-0-g2414721** instead if you want -- use the `full` config option),
* **v1.0-DEV** - if the repository is "on a tag", but in "dirty" state. This dirty marker can, and will be included wherever possible,
* **2414721** - a plain commit id hash if not tags were defined (of determined "near" this commit). 
                *It does NOT include the "g" prefix, that is used in the "full" describe output format!*

For more details (on when what output will be returned etc), see <code>man git-describe</code> (or here: [git-describe](http://www.kernel.org/pub/software/scm/git/docs/git-describe.html)). In general, you can assume it's a "best effort" approach, to give you as much info about the repo state as possible.

**describe-short** is also provided, in case you want to display this property to non-techy users, which would panic on the sight of a hash (last part of the describe string) - this property is simply
*the describe output, with the hash part stripped out*.

git-describe and a small "gotcha" with tags
-------------------------------------------
You probably know that git has two kinds of tags:

* **lightweight tags** - which are only a pointer to some object,
* **annotated tags** - which are the same as a lightweight tag and contain additional information, such as a message linked with the tag.

Knowing this, I now can tell you that when you run git-describe, it (by default) looks only for **annotated** tags.
What this means in a real life scenario can be explained on such repository:

```
b6a73ed - (HEAD, master)
d37a598 - (v1.0-fixed-stuff) - a lightweight tag (with no message)
9597545 - (v1.0) - an annotated tag
```

When you run git describe without any options (note that git-commit-id is "acting like" plain git, so all behaviour is as described here, unless you configure it to act otherwise (using the `<tags>true</tags>` option)):

```
> git describe
  annotated-tag-2-gb6a73ed     # the nearest "annotated" tag is found
```

So it did not find the lightweight tag! Do not panic, there's a flag to help with that:

```
> git describe --tags
  lightweight-tag-1-gb6a73ed   # the nearest tag (including lightweights) is found
```

Using only annotated tags to mark builds may be useful if you're using tags to help yourself with annotating
things like "i'll get back to that" etc - you don't need such tags to be exposed. But if you want lightweight
tags to be included in the search, enable this option.

<blockquote>
TIP: If you're using maven's `release:prepare` and `release:perform` it's using <em>annotated</em> tags.
</blockquote>

