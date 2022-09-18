In [one of my previous posts][1] I described a blog engine I was developing. At the time, I had returned to it after a hiatus and I was convinced I would continue working on it. However, at the same time I was still more interested in developing my python-based [url-shortener project][2] and I also already planned moving on to other projects and technologies. As a result, I got bored of blog-engine again.

Since then, I&#8217;ve been focusing on Java and [Spring Framework][3]. This resulted in a new project: another blog engine, but this time based on Spring and designed with different features.

<!--more-->

## Ideas and features

I decided this blog engine will be simpler than the previous one (at least initially) and will have the following features:

  * a support for a single administrator and blog author
  * configuration based on editing files rather than using a graphical user interface
  * dynamically generated menus used for main navigation element, blogroll widget, etc.
  * autopublication of articles on selected dates

I chose [Spring Boot][4] for the base of my project because I wanted to rely on its starters and simplified configuration and also because that was the advice I&#8217;ve read in a number of posts (for example, [here][5]).

I named my previous blog engine project simply &#8220;blog-engine&#8221;. Not very imaginative, but still unique when compounded with my GitHub username. Choosing the same name for another project is not very practical (not to mention: impossible on GitHub), so I had to pick something else. Since I started the project shortly before Christmas, I decided to name it Yule, after [a historical pagan germanic festival][6] observed near the same time of the year.

## Article entity class

For storing and transfering article data, I initally wanted to implement two classes:

  * a database entity class. Since [Hibernate ORM][7], a JPA implementation I&#8217;m using, can use reflection to access fields of entity objects and only requires a no-argument constructor of any visibility, I could write this class so that it would be properly encapsulated, that is: in a way ensuring all constructor and method calls would result in a valid article object, or in an exception being thrown.
  * a form-backing class. [Thymeleaf][8], a template engine I decided to use, [requires such classes to have getter and setter method][9] for each field included in a form. This class would be allowed to contain data that would be invalid for an article.

However, after some time, I decided to combine the two roles in [the database entity class][10]. After all, this is a simpler solution and instances of such a class may still be validated, no matter the source of their potentially invalid state.

## Standard and custom validation constraints

For validation, I&#8217;m using [Java Bean Validation][11], with [Hibernate Validator][12] as its implementation.

Some validation constraints are applied to specific fields of each article object, regardless of its current state. For example, [each article requires a non-empty title][13]. For such constraints, annotations and validators built into JBV (like [`NotNull`][14]) or Hibernate Validator (like [`NotBlank`][15]) are enough.

Other constraints are applied to articles only with specific statuses: objects representing life cycle stages of individual articles, implemented as instances of [`ArticleStatus` enum][16]. For example, published articles accept only a non-future publication date, the ones scheduled for auto-publication require a future one, while drafts accept any publication date, including none at all. Both published and scheduled articles require a content that is not blank, while drafts have no such requirement.

Because of this dependance of constraints of some fields on value of another field, I had to define a custom, cross-field, class-level constraint and implement a validator for it.

Since these constraints are dependent on a particular value of `ArticleStatus` and occur in different combinations, I decided to implement them as optional dependencies of `ArticleStatus` objects. They are instances of classes extending a common abstract superclass: [`ExistingArticleConstraint`][17], and providing a [constraint-testing method][18], an [error message template][19] and a [name of a field][20] to which the message applies.

The `Article` class has a [`getViolatedStatusConstraints()`][21] method validating constraints provided by the current status of an article and returning those constraints that aren&#8217;t fulfilled by it. This method [is called][22] by my custom constraint validator class. Objects representing violated status constraints are returned by the method and then are used by the validator to [build constraint violations][23], assigning proper error messages to fields for which the constraints were violated.

The validator class is used by my custom constraint annotation: [`StatusConstraintsFulfilled`][24], which is [applied][25] to the `Article` class.

I think that&#8217;s enough for this post. In the next one, I will describe the service layer of the project.

[//]: # ([1]: {{< relref "a-php-symfony-blog-engine.md" >}})

[//]: # ([2]: {{< ref "/projects/url-shortener" >}})

[3]: https://projects.spring.io/spring-framework/
[4]: http://projects.spring.io/spring-boot/
[5]: http://stackoverflow.com/a/35996408
[6]: https://en.wikipedia.org/wiki/Yule
[7]: http://hibernate.org/orm/
[8]: http://www.thymeleaf.org/
[9]: http://www.thymeleaf.org/doc/tutorials/2.1/thymeleafspring.html#creating-a-form
[10]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/domain/Article.java
[11]: http://beanvalidation.org/
[12]: http://hibernate.org/validator/
[13]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/domain/Article.java#L66
[14]: http://docs.oracle.com/javaee/7/api/javax/validation/constraints/NotNull.html
[15]: https://docs.jboss.org/hibernate/validator/5.4/api/org/hibernate/validator/constraints/NotBlank.html
[16]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/domain/ArticleStatus.java
[17]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/validation/ExistingArticleConstraint.java
[18]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/validation/ExistingArticleConstraint.java#L80
[19]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/validation/ExistingArticleConstraint.java#L68
[20]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/validation/ExistingArticleConstraint.java#L58
[21]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/domain/Article.java#L278
[22]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/validation/StatusConstraintValidator.java#L52
[23]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/validation/StatusConstraintValidator.java#L56
[24]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/validation/StatusConstraintsFulfilled.java#L48
[25]: https://github.com/piotr-rusin/yule/blob/8482b99fedf89311bb9a59f4503be260d67eeac7/src/main/java/com/github/piotr_rusin/yule/domain/Article.java#L59
