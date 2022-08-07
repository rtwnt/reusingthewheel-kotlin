---
title: Progress in development of spam-lists
date: 2016-07-09T19:19
categories:
  - Development
projects:
  - spam-lists

---
Since the introduction of my spam-lists library on this blog, I made some progress in its development.

I reorganized the library a bit, changed the inheritance hierarchy of exception classes, fixed some errors and published the package on PyPI. In this post, I&#8217;m going to describe the changes and explain my reasoning behind them.

<!--more-->

## Splitting spam\_lists.service\_models module

The `spam_lists.service_models` module contained two groups of classes:

  * client classes for various services
  * classes representing custom host lists

Originally, the whole project consisted of only one module. Later I decided to split its code into separate modules, including `service_models`, defined as a module containing &#8220;classes of objects serving as clients for remote and local spam listing services&#8221;, that is: classes representing clients for various third party services and those representing user-made host lists.

Adding more custom host list classes made me think of splitting the module, but I think it would probably have been better to split it even earlier. Although I did not have any preconfigured instances of clients at the time of introducing the `service_models` module, I think it would have been better to have a `clients` module already. It would have contained definitions of client classes, and later I would have added instances of some DNSBL service clients to it. At the time I had only one class representing a custom host list, but it would have been put in some different module.

Taking this into account, I decided to:

  * introduce new `host_collections` module and move the host collection classes there
  * move the client classes to `clients` module

After these changes, the only class left in `service_models` was `HostList`: a base class for both the client and the host collection classes. I changed the name of the module to `host_list` as I thought it was better for its current purpose as a container for this class.

## A better name for spam_lists.utils

The `spam_lists.utils` module was intended to contain various utilities that could be used by a user of my library to:

  * create composite URL testers
  * create custom implementations of functions and methods responsible for testing URLs or registered hostnames
  * etc.

The usage of a very general term &#8220;util&#8221; as the name of the module reflected that it probably was not well defined. It ended up containing two classes representing composite URL testers and a class of a dependency of one of them: `RedirectUrlResolver`. Therefore, I decided to rename the module to `composites`.

## Inheritance hierarchy of exception classes

My project contains a few exception classes, some of them being `UnathorizedAPIKeyError`, `InvalidHostError` and `InvalidURLError`. All three directly extended two classes: `SpamListsError` and `ValueError`, which was a bit repetitive. I introduced `SpamListsValueError` class as a subclass of both `SpamListsError` and `ValueError` classes, and I made it a base class for all the classes that previously extended `SpamListsError` and `ValueError`.

## Fixing errors in a rich comparison method

The host classes of my project depend on the following classes from other modules:

  * `dns.name.Name` of the dnspython and dnspython3 projects
  * `IPv4Address` and `IPv6Address` from the `ipaddress` module of Python 3, or from ipaddress library for Python 2

Originally, the relationship was that of inheritance: `Hostname` class extended the `Name` class, and the ip address classes extended their counterparts from `ipaddress` module. All the third party classes provide their own implementations of rich comparison methods, so my classes naturally inherited them, although they didn&#8217;t need them at the time.

At some moment, I decided to replace inheritance with composition for these classes. Instances of my own host object classes were going to store and use instances of the dependency classes in their value attribute. I was already planning to introduce a custom host list class that would utilize sorting, and for that, my host classes needed to implement `__lt__(other)` method (responsible for overloading &#8220;<&#8221; comparison operator), and with the change of their relationship to the third-party classes, they could no longer use their implementations of the method directly. I had to implement it myself, and I did it for the `IPAddress` class (a base class for both of my ip address classes), but I forgot about it when modifying the `Hostname` class. I realized it only after writing my first blog post, and I fixed the omission by introducing `Host`: a common base class for `Hostname` and `IPAddress`, and moving `__lt__(other)` method from `IPAddress` class to it. I also added unit tests for this method for all of my host object classes, not only for those extending `IPAddress` class.

At this moment, the code of the method looked like this:

```python
def __lt__(self, other):
    ''' Check if the other is smaller
    This method is necessary for sorting and search
    algorithms using bisect_right. It handles TypeError
    by returning NotImplemented

    :param other: a value to be compared
    :returns: result of comparison as implemented in base
        classes, or NotImplemented
    '''
    try:
        return self.value < other
    except TypeError:
        return NotImplemented
```

I also had to fix an error in its original implementation: it compared `self.value` to the `other` object, not to `other.value`.

Another problem arised from the ways the `__lt__(other)` method has been implemented by the possible classes of value attribute.

The IP address classes support only comparison with instances of the same class, and when attempting to compare an instance of one of them to an object of a different base type, the method [returns NotImplemented constant][1]. When this object is another type of IP address, [a TypeError is raised][2].

In the port of the module for Python 2, [this looks a bit different][3], but in practice works the same.

The `Name` class also supports comparison with an object of the same type, but in case of the other object being of a different type, it never raises `TypeError`, but instead [returns the NotImplemented constant][4].

The return value `NotImplemented` is handled differently by Python 2 and 3 interpreters: in Python 2, it causes the comparison operation to be handled elsewhere, while in Python 3, it results in a `TypeError` being raised, with its message stating the types are not orderable.

As you can see in the code, my method handled `TypeError` raised by the comparison operation by returning `NotImplemented` constant. It means that my rich comparsion method could not handle comparison for some combinations of versions of the Python interpreter and types of the objects being compared.

I fixed this compatibility error by modifying the code like this:

```python
def __lt__(self, other):
    ''' Check if the other is smaller.

    This method is necessary for sorting and search
    algorithms using bisect_right.

    :param other: a value to be compared
    :returns: result of comparison between value attributes of
        both this object and the other, or of comparison between
        their unicode string representations.
        In case of the other not having necessary attributes,
        NotImplemented constant is returned.
    '''
    try:
        try:
            return self.value < other.value
        except TypeError:
            return self.to_unicode() < other.to_unicode()
    except AttributeError:
        return NotImplemented
```


Still, the behaviour of the method was inconsistent between the versions: when using Python 2 to execute the comparison of an object of a `Hostname` type to an object of `IPv4Address` type, the `Name.__lt__(other)` method of `Hostname.value` returned `NotImplemented` value, and Python interpreter handled the comparison. However, when using Python 3, the comparison operation raised `TypeError` to be handled by my method.

Returning `NotImplemented` constant in response to an `AttributeError` being raised also resulted in inconsistent behaviour of my method in different Python versions. Because of this, I had to modify the code again to fix this inconsistency.

I also noticed the docstring of my method was misleading, because its first line sounded like the method was responsible for &#8220;other < self&#8221; comparison, not &#8220;self < other&#8221;. I fixed it, too, and the current code of the method looks like this:

```python
def __lt__(self, other):
    ''' Check if self is less than the other

    This method is necessary for sorting and search
    algorithms using bisect_right.

    :param other: a value to be compared
    :returns: result of comparison between value
        attributes of both this object and the other,
        or of comparison between their unicode string
        representations.
    :raises TypeError: in case of the other not having
        either value or to_unicode attributes.
    '''
    try:
        try:
            result = self.value.__lt__(other.value)
        except TypeError:
            return self._compare_strings(other)
    else:
        if result == NotImplemented:
            result = self._compare_strings(other)
        return result
    except AttributeError:
        msg = 'Unorderable types: {}() < {}()'.format(
            self.__class__.__name__,
            other.__class__.__name__
        )
        raise TypeError(msg)

def _compare_strings(self, other):
    return self.to_unicode() < other.to_unicode()
```

## Releasing the project

I decided to publish my project on PyPI, partially because I thought it was already usable, and partially because I already used it in another project, so installing it from PyPI using pip would be easier than using it as a git submodule.

I added a setup module, updated the project&#8217;s README, registered and published it in the repository. Although the library seemed to work properly, I knew there might still be some bugs I should fix and improvements I should make before declaring it stable, so I published it as a beta version. I was right: the initial package turned out not to contain LICENSE and requirements.txt files. I added MANIFEST.in file to fix it, and I converted the README from markdown to reStructuredText, so its rendering could be handled by PyPI website. I made a few other improvements and published the second beta of the package: since then, I further improved the README, fixed some errors and published a few more beta versions.

I think version numbers of the project need an explanation: the current version is 1.0.0b7, the first beta version was 1.0.0b1, and the version before that was 0.9. The previous number was result of me realising I should apply a version number to the library. At the moment, I thought I was close to publishing its first stable, &#8220;finished&#8221; release, with all functionality that was planned for it already implemented and all errors either fixed or still undetected. I wanted to mark such a version as 1.0, so I thought it was reasonable for the last versions before it to be given the number 0.9. After that, I read more about versioning systems, and although I decided to still follow the 0.9 version with a 1.0 one, I also decided to apply [the pattern recommended by PEP 440][5] and the suggestions provided by [this answer on stackoverflow][6]. Hence the version 1.0.0b1, with the initial section marking the release of the stable version of the library and the change in the versioning pattern. The middle number is going to change for subsequent feature updates, and the third number is going to signify stable bugfix-only releases. The final &#8220;b1&#8221; segment signifies the first beta version, and it follows the pattern from the PEP.

I think I will spend a little more time reading the code, looking for errors to fix and improvements to documentation I could make. I do not plan to make any major changes to it until after I release the library as a stable version 1.0.0.

 [1]: https://hg.python.org/cpython/file/3.5/Lib/ipaddress.py#l562
 [2]: https://hg.python.org/cpython/file/3.5/Lib/ipaddress.py#l564
 [3]: https://github.com/phihag/ipaddress/blob/master/ipaddress.py#L681
 [4]: https://github.com/rthalley/dnspython/blob/master/dns/name.py#L330
 [5]: https://www.python.org/dev/peps/pep-0440/#public-version-identifiers
 [6]: http://stackoverflow.com/a/8867153/5069081
