---
title: Migrating to Hugo and GitHub Pages
date: 2017-11-12T16:43
categories:
  - Blog
---

I wanted to have greater control over my blog, so I've been thinking about moving it to a self-hosted WordPress installation. In the end, I decided to migrate from WordPress to a static blog generated with [Hugo][1] and hosted on [GitHub Pages][2]. This post will be the last one posted on [reusingthewheel.wordpress.com][3] - the future ones will be posted on [piotr-rusin.github.io][4].

[1]: https://gohugo.io/
[2]: https://pages.github.com/
[3]: https://reusingthewheel.wordpress.com/
[4]: https://piotr-rusin.github.io

<!--more-->

## Why a static website generator?

I like the convenience of writing content (including tags, categories and other metadata) in markdown, in my editor of choice, saving it to a file and running a few console commands to rebuild my website and send updates to a server. Static website generators, combined with a couple of Git repositories ([one][5] for templates and content and [the other][6] for generated static website) and a GitHub account allow me to enjoy it.

[5]: https://github.com/piotr-rusin/reusingthewheel
[6]: https://github.com/piotr-rusin/piotr-rusin.github.io

Another reason for my choice is that static HTML files can be hosted anywhere, with minimal to no setup. I can even get a reliable, efficient and ad-free hosting for free.

Although no HTML is generated server-side, static website generators still use web templating systems, with all their advantages, like separating data from presentation, allowing me to easily update or replace the latter without having to deal with too much repetitiveness in template code.

Finally, there are security and performance concerns regarding WordPress and other dynamic website engines and web applications (but especially WordPress, judging from both its reputation and popularity). Their complexity and reliance on storing data in database introduce both a potential for bugs that may allow unauthorized access to sensitive data and an overhead in handling HTTP requests. Using a static website generator allows me to avoid these issues without effort.

## User interactions and contributions

I'm the only author of this blog and I don't plan to change that. However, I want potential readers to be able to post comments below my posts, and it's obviously impossible without a server side application for managing comments and data of their authors. Luckily, there are third party comment systems like [Disqus][7] or [Discourse][8]. One's website needs only to contain a piece of JavaScript code responsible for communicating with a server of such a system and presenting comments, so it can be easily integrated with a static website. When I was still planning to move to a self-hosted WordPress installation, I was going to use such a system anyway, mainly because I didn't want to force my readers to register on yet another website if they wanted to both comment and use a persistent account for that.

[7]: https://disqus.com/
[8]: https://www.discourse.org/

## Choosing the right tool

I must admit I didn't do a lot of research before making the choice. I only considered [Jekyll][9], [Pelican][10] and Hugo, and I chose the latter for:

* its relative popularity - it's second (after Jekyll) according to data presented on [StaticGen][11]
* ease of setup - unlike Jekyll and Pelican, Hugo is distributed as a static binary, ready to be put and executed anywhere
* speed - rebuilding even large websites is said to be much faster than in case of Jekyll

After downloading Hugo, trying it out and using it to do a test migration of my blog, I decided I didn't need to look further.

[9]: https://jekyllrb.com/
[10]: https://github.com/getpelican/pelican
[11]: https://www.staticgen.com/

