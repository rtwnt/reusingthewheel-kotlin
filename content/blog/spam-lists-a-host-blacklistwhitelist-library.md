---
title: Spam-lists – a host blacklist/whitelist library
date: 2016-06-16T15:59
categories:
  - Development
projects:
  - spam-lists

---
The first project I would like to present on this blog is spam-lists ([GitHub][1]): a library for querying custom and third party web address blacklists and whitelists.

<!--more-->

The first service supported by the project is [SURBL][2] &#8211; a DNSBL service listing spam hostnames and IP addresses. DNSBLs are queried by performing DNS queries for a domain consisting of the queried value (or a value derived from it, in case of IP addresses) and a proper suffix associated with the service.

An item is resolved as non-spam if the result of query is NXDOMAIN. If the query returns an IP address, the item is listed &#8211; and the IP address can contain additional information, like classification of the item. In case of SURBL, this information is encoded in the last octet of ip address, as a sum of numbers associated with classifications.

Two other DNSBL services supported by my project are [Spamhaus ZEN][3] &#8211; a list combining a few other lists from Spamhaus, listing different types of malicious IP addresses, and [Spamhaus DBL][4] &#8211; a blacklist containing hostnames. Queries to these lists also return classification information, but not in the same way as SURBL &#8211; here, each possible final octet of returned IP address is associated with a classification value, and always represents membership in a single sublist.

[Google Safe Browsing Lookup API][5] is another service supported by the project. The service lists malicious URL addresses and supports querying for multiple values using a POST request.

The last remote service I decided to support in my project is [HpHosts][6]. This service lists both hostnames and IPv4 addresses, and has a HTTP query API: values can be queried by sending a GET request with proper URL.

Additionally, the library supports using and managing custom host lists, both ordered (using binary search algorithm when searching for existing items or for insertion points for new ones) and unordered.

The project supports querying for non-whitelisted values (for example: a custom host list could be used as a whitelist) and combining multiple clients in one composite URL tester. Redirect resolution is also supported &#8211; the resolved addresses can be included in testing.

I decided to code the project in Python, because I have not used this language for a while and I wanted to refresh my knowledge of it.

Writing the library took much more time and effort than I thought it would, partially because I was making frequent changes to its design, although at first I was convinced my intial project was good enough. For example, the classes responsible for querying host blacklists were not going to contain methods for testing URLs, but instead their instances would be used as dependencies of an instance of a class implementing these methods. However, I decided eventually that it would be better to provide each class of a host-list client with URL-testing methods, and create separate classes (`spam_lists.utils.UrlTesterChain`, and also `spam_lists.utils.GeneralizedUrlTester`) with a responsibility of combining multiple clients.

The library also contains a package with unit and integration tests. Its inclusion was one of the reasons for the development taking more time and effort, because it was the first time I added automated tests to a project. Before that, the closest I got to unit testing was just reading about the topic and writing some unit tests using PHPUnit, just to learn something about it and see how it works. It probably should not be surprising I could not write my tests right at first, even if they passed. I put a lot of effort into refactoring them, for example: by making them parameterized with the help of [nose-parameterized library][7]. I am sure I could still improve the quality of code and organization of `spam_lists.test` package, but I also know I learned a lot. Automatic testing proved extremely helpful during development of the library, and I am already using it for my other new projects.

I am going to continue developing this project by improving quality of its code, adding and improving features and fixing errors. Eventually, I would like to publish it on [PyPI][8].

 [1]: https://github.com/piotr-rusin/spam-lists
 [2]: http://www.surbl.org/
 [3]: https://www.spamhaus.org/zen/
 [4]: https://www.spamhaus.org/dbl/
 [5]: https://developers.google.com/safe-browsing/v3/lookup-guide
 [6]: http://www.hosts-file.net/
 [7]: https://pypi.python.org/pypi/nose-parameterized/
 [8]: https://pypi.python.org/pypi
