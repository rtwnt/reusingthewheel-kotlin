---
title: 'Url-shortener: a demo and more refactoring'
date: 2016-12-31T16:13
categories:
  - Development
projects:
  - url-shortener

---
A demo instance of my url-shortener project is now available here:

[https://url-shortener.reusingthewheel.tk][1]

Usually, URL shorteners are deployed under a short domain name (for example: [goo.gl][2], [bit.ly][3]). It&#8217;s not the case here, but since it&#8217;s just a demo I didn&#8217;t think looking for a short domain for it was necessary, so I chose a longer domain that is also related to my blog.

I have also made some changes to the application. Some of them were minor, like adjusting font sizes in the front end, renaming some local variables in the back end, etc., but there are also some bigger changes.

<!--more-->

## Choice of replacements for homoglyphs

Previously, the application replaced homoglyphs in alias values according to the following rules:

  * for each group of single-character homoglyphs, the one that was alphabetically the smallest was used to replace the rest
  * for each pair of a multi-character and a single-character homoglyphs, the longer one was always replaced by its shorter equivalent.

These rules didn&#8217;t take into account that characters included in the replacement strings could be missing in the alias alphabet used by the application. It didn&#8217;t cause any error at the time because the alphabet was not designed to be configurable, and its hard-coded value used by the application included all the characters that were present in homoglyph replacements. Still, relying on the alphabet having some properly hard-coded characters was a rather poor and error-prone solution, so I fixed it. Members of each group of homoglyphs are now replaced by the shortest and smallest (in terms of alphabetic order) of their equivalents whose all characters are included in the alphabet used by the application.

## Unification in handling of homoglyphs

Previously, the application used different implementations of homoglyph replacement for single- and multi-letter homoglyphs. The relationships between pairs of single-letter homoglyphs were represented by a [translation object][4] created with [str.maketrans][5] method. When [replacing single-letter homoglyphs][6] in an alias value, this translation object was passed to [str.translate][7] method of a string object representing the alias.

Relationships between pairs of multi-character homoglyphs and their equivalents were represented by [a dictionary object][8], with multi-character homoglyphs being keys and their replacements being values. The [homoglyph replacement was performed][9] by simply looping over key-value pairs and replacing occurrences of a key in an alias string with its respective value.

The first approach was used for its simplicity, but it was limited to mappings of a single character to another character, so it was necessary to use a different approach for multi-letter homoglyphs. However, this made the code unnecessarily more complex, so I decided to abandon the approach using translations and to use just the dictionary approach, regardless of length of homoglyphs.

## Refactoring AliasAlphabet class

In [my last post about the application][10], I described [AliasAlphabet][11] &#8211; a class I introduced to the project when I was reorganizing its architecture to make it follow single responsibility principle more closely. At the moment of writing the post, I had doubts about adding it and I saw it as a candidate for further refactoring, but I also wanted to publish the article as soon as possible, so I left it as it was.

The instances of the class represented alias alphabets used for generating alias values, but they also contained methods for creating aliases. They were added here because creating aliases closely depended on an alias alphabet. Still, I thought it wasn&#8217;t the cleanest design and I was considering some ideas for replacing it. I decided to replace the class with [AliasFactory][12] class, a [string object][13] representing alias alphabet and [a function][14] creating a dictionary mapping homoglyphs to strings that should replace them.

 [1]: https://url-shortener.reusingthewheel.tk/
 [2]: https://goo.gl/
 [3]: https://bitly.com/
 [4]: https://github.com/piotr-rusin/url-shortener/blob/ee506ab166d3a170ee8790d33f20cf1ee88205a5/url_shortener/domain_and_persistence.py#L51
 [5]: https://docs.python.org/3/library/stdtypes.html#str.maketrans
 [6]: https://github.com/piotr-rusin/url-shortener/blob/ee506ab166d3a170ee8790d33f20cf1ee88205a5/url_shortener/domain_and_persistence.py#L191
 [7]: https://docs.python.org/3/library/stdtypes.html#str.translate
 [8]: https://github.com/piotr-rusin/url-shortener/blob/ee506ab166d3a170ee8790d33f20cf1ee88205a5/url_shortener/domain_and_persistence.py#L125
 [9]: https://github.com/piotr-rusin/url-shortener/blob/ee506ab166d3a170ee8790d33f20cf1ee88205a5/url_shortener/domain_and_persistence.py#L170
 [10]: {{< relref "changes-in-url-shortener.md" >}}
 [11]: https://github.com/piotr-rusin/url-shortener/blob/ee506ab166d3a170ee8790d33f20cf1ee88205a5/url_shortener/domain_and_persistence.py#L35
 [12]: https://github.com/piotr-rusin/url-shortener/blob/0207eb20d118df9cf4139f55d9f0a9c8c0fa5b76/url_shortener/domain_and_persistence.py#L76
 [13]: https://github.com/piotr-rusin/url-shortener/blob/0207eb20d118df9cf4139f55d9f0a9c8c0fa5b76/url_shortener/domain_and_persistence.py#L118
 [14]: https://github.com/piotr-rusin/url-shortener/blob/0207eb20d118df9cf4139f55d9f0a9c8c0fa5b76/url_shortener/domain_and_persistence.py#L37
