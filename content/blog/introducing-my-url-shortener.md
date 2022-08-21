---
title: Introducing my URL shortener
date: 2016-08-05T13:18
categories:
  - Development
projects:
  - url-shortener
  - spam-lists

---
URL addresses can be quite long, mainly because they often consist of path and query components, both containing a number of characters forming names of path segments and values of parameters (not necessarily meaningful to a human reader), or performing other functions. For that reason, they may be hard to type and memorize, or simply aesthetically unpleasing.

Because of that, URL shorteners were created. There are plenty of them already, so there is probably no reason to create yet another such application, perhaps with one exception: it seems like an interesting idea for a small programming project that could be later extended with other features. This is why I decided to create [my own URL shortener][1].

<!--more-->

## Technologies

I decided to write the project in Python 3.4 and to use the following libraries:

  * [Flask framework][2]
  * [WTForms][3]
  * [Flask-WTF][4]
  * [SQLAlchemy][5]
  * [Flask-SQLAlchemy][6]
  * [spam-lists][7]

## Querying for target URL

The role of a URL shortener is to provide a shorter alias URL for an address requested by a user. The target URL is associated with an alias URL and stored in a database, at least for some time.

In case of my application, the redirection service can be accessed by sending a HTTP request with the address `host.com/<alias>`, where:

  * `host.com` is a hostname pointing to an instance of my URL shortener hosted on a web server
  * `<alias>` is the alias string for a URL registered in my application.

## Previewing alias URL and its target URL

Each registered alias URL can be previewed by sending a HTTP request to `host.com/preview/<alias>` URL. The response is an HTML document displaying alias URL and a target URL to which it redirects.

## Creating and registering a short alias

The alias value is generated randomly and stored in the database together with its target URL. In the database, the alias value is represented by an integer primary key. When used in URL addresses handled by my application, it is displayed as a string. This relies on the string form being treated simply as a numeral expressed in an alternative, non-decimal numeral system, with a set of characters used as its digits. This way, the operation of converting the alias value between its forms is an instance of base conversion.

Representing the alias value is the responsibility of [url_shortener.models.Alias][8] class. It uses an instance of [url_shortener.models.NumeralSystem][9] as a base for converting between the integer and string representations of its instance.

The process of conversion of an Alias value to and from an integer stored in a database is handled by my custom SQLAlchemy column type: [url_shortener.models.IntegerAlias][10].

The Alias class provides a method returning a factory of its instances, with its values generated randomly for given minimum and maximum number of characters in an alias. This factory is instantiated in [url\_shortener.event\_handlers][11] module, where it is used by `assign_alias_before_insert(mapper, connection, target)` &#8211; a handler of a `before_insert` event, responsible for generating a random alias value and assigning it to an object representing a shortened URL about to be inserted into the database.

The process of registering a URL is implemented in [url_shortener.models.register][12] function. In case a randomly generated alias value already exists, an instance of `sqlalchemy.exc.IntegrityError` is raised. It is handled by rolling back all changes, logging its occurence and retrying the operation until it is successful or a pre-configured limit of alias URL registration attempts is exceeded.

## Configuration options

The application provides a set of configuration options that are loaded from [url\_shortener.default\_config][13] module. These options can (and some of them must) be overridden by providing a custom configuration file whose path must be set to `URL_SHORTENER_CONFIGURATION` environment variable.

## Spam protection

URL shorteners can be abused by spammers, so it is necessary to implement countermeasures. My application provides three layers of protection:

  * a reCAPTCHA form field, provided by Flask-WTF
  * validation against a host blacklist, provided by my spam-lists project
  * third party service clients for recognizing spam urls, also provided by my spam-lists project.

The clients and classes provided by spam-lists library are used to implement form validators for URL field. The form class looks [like this][14].

When someone attempts to request an alias for a spam or blacklisted URL, or reCAPTCHA verification fails, the request is handled by displaying proper error information above the form.

In case a URL address was blacklisted locally or by a third party service after it was registered by my application, each request to its alias URL is handled by using the preview feature. In such a case, the preview page displays additional information about the URL being locally blacklisted or otherwise recognized as spam.

## The future of the project

The main features of the project seem to be complete, so I think I will be ready to release its first stable, non-development version after I improve its frontend by making some modifications to its layout and adding CSS styles. However, this does not mean the development of the project will be over. I will revisit it, add other features and release subsequent stable versions once they are complete.

Some of my current ideas for future additions are:

  * administration panel for viewing and manually removing registered URL aliases and managing local blacklist
  * configurable choice of third party service clients provided by spam-lists to be used during validation
  * a possibility for requesting custom alias strings, with their minimum and maximum length restrictions separate from ones set for randomly generated alias strings.

 [1]: https://github.com/piotr-rusin/url-shortener
 [2]: http://flask.pocoo.org/
 [3]: https://wtforms.readthedocs.io/en/latest/
 [4]: https://flask-wtf.readthedocs.io/en/latest/
 [5]: http://www.sqlalchemy.org/
 [6]: http://flask-sqlalchemy.pocoo.org/2.1/
 [7]: https://github.com/piotr-rusin/spam-lists
 [8]: https://github.com/piotr-rusin/url-shortener/blob/2bbcb9bcc97f8226e7d90d201c57933421ee050e/url_shortener/models.py#L97
 [9]: https://github.com/piotr-rusin/url-shortener/blob/2bbcb9bcc97f8226e7d90d201c57933421ee050e/url_shortener/models.py#L31
 [10]: https://github.com/piotr-rusin/url-shortener/blob/2bbcb9bcc97f8226e7d90d201c57933421ee050e/url_shortener/models.py#L210
 [11]: https://github.com/piotr-rusin/url-shortener/blob/2bbcb9bcc97f8226e7d90d201c57933421ee050e/url_shortener/event_handlers.py
 [12]: https://github.com/piotr-rusin/url-shortener/blob/2bbcb9bcc97f8226e7d90d201c57933421ee050e/url_shortener/models.py#L288
 [13]: https://github.com/piotr-rusin/url-shortener/blob/2bbcb9bcc97f8226e7d90d201c57933421ee050e/url_shortener/default_config.py
 [14]: https://github.com/piotr-rusin/url-shortener/blob/2bbcb9bcc97f8226e7d90d201c57933421ee050e/url_shortener/forms.py#L9
