---
title: A PHP/Symfony blog engine
date: 2016-12-06T13:33
categories:
  - Development
projects:
  - Blog Engine

---
A few months ago I started a blog engine project based on [Symfony framework][1]. I&#8217;ve been adding things and making changes to it from time to time. Since August, I took a longer break in its development because I wanted to focus on other things, but now I&#8217;m returning to the project.

<!--more-->

## Articles

The core feature of each blogging software is the ability to manage and publish articles or blog posts. Each of them should obviously have a title and content.

It is also useful for an article to have a slug that represents it in URL addresses. It will generally be based on the title of an article, but it&#8217;s good to be able to replace a generated slug with a custom value so that it would be shorter or work better with search engines.

The following data will also be stored and managed for each article:

  * author
  * comments
  * tags
  * date of publication
  * publication status marking the article as published or not published

## Users

The blog engine will support user registration and the following user roles, each with their own privileges:

  * contributor &#8211; creating and editing their own articles, but not publishing them
  * author &#8211; creating their own articles, editing and publishing them
  * editor &#8211; creating, publishing and editing their own articles, publishing and editing articles authored by other users
  * admin &#8211; access to all administration features for an installation of the engine
  * moderator &#8211; hiding comments made by other users, or publishing them if new comment moderation is turned on

There is also a super admin role provided by an external library, but I probably won&#8217;t use it.

The roles are organized in a [hierarchy][2]. Its design relies on comment moderation and content creation/blog administration privileges being separate so that, for example, a user could be either an editor or a moderator, or both. The admin role is an exception: each admin is also a moderator.

Other than user roles, the application will store and manage the following data for each user:

  * comments written by them
  * articles authored by them
  * username
  * email address
  * encrypted password

## Comments

Most blog engines allow users to publish comments under articles, either by handling all comment-related data storage and management themselves or by using third party commenting services and plugins, like Disqus. For this project, I will implement this feature myself.

The following data will be stored and managed for each comment:

  * publication date
  * publication status &#8211; specifying if the comment is currently published (visible) or not
  * article under which the comment was posted
  * data of the author of the comment

Like WordPress and Blogger, my blog engine will allow commenting for both authenticated and non-authenticated users &#8211; a user who just wants to post a comment on an article will be able to do so without having to register, log in and remember or store somewhere their password. Still, such a user will have to provide a username and email address to be able to comment.

## Storage and representation of entities

For database storage and object mapping I decided to use Doctrine ORM and PostgreSQL database.

Blog articles, tags, users and comments are represented by [entity classes][3] that are mapped to rows of their respective tables. Doctrine provides the following options for specifying mapping information:

  * YAML files
  * XML files
  * DocBlock annotations

I chose to use the annotations because I prefer defining metadata as close to entity classes and their mapped properties as possible.

I decided to use [FOSUSerBundle][4] as a library providing features related to user registration and authentication, instead of implementing them myself. My own user entity class extends one provided by the library and adds project-specific features: `comments` and `articles` properties containing posts authored by a user.

## The future of the project

The next steps include:

  * creating form classes for articles, tags and user roles
  * creating voters for authorization based on task type, user role and other criteria
  * adding controller classes and implementing methods for handling HTTP requests

However, there are some decisions I made soon after starting the project which I&#8217;m now reconsidering &#8211; particularly ones related to the user role hierarchy. I will explain everything in the next article about the project, once I decide on possible changes.

 [1]: https://symfony.com/
 [2]: https://github.com/piotr-rusin/blog-engine/blob/master/app/config/security.yml#L12
 [3]: https://github.com/piotr-rusin/blog-engine/tree/master/src/AppBundle/Entity
 [4]: https://github.com/FriendsOfSymfony/FOSUserBundle
