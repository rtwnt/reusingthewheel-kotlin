Since my [introductory post on Yule][1], I&#8217;ve been doing some work on the project. I&#8217;ve implemented an automatic article publication feature as a part of service layer and I added some methods used by it to the repository layer of my project.

This post covers these changes and some problems I encountered during testing.

<!--more-->

## Repository layer

Although in the previous post I promised the next one would be about the service layer, and [repositories constitute their own layer][2], I decided to cover it here as I didn&#8217;t do it before. Plus, the repository layer is used by the service layer, and will also be used by the controllers, so it needs to be touched upon.

In my project, this layer consists of only one interface (at least for now): [`ArticleRepository`][3]. It declares methods responsible for database queries and commands. It&#8217;s not accompanied by my own implementation of it because Spring Data is able to [generate beans implementing repository interfaces][4], and because I didn&#8217;t need to implement any custom behaviour that would have to be provided by a custom implementation of the interface.

There are three ways of providing a repository interface with data necessary to generate its implementation:

  * [properly naming its methods][5]
  * [using JPA NamedQueries][6]
  * [using Spring Data `@Query` annotation][7]

For most methods I chose the third option, mainly because the queries are complex enough that if they were to be generated from method names, the names would be very long and not very readable. Another reason for the choice is that I don&#8217;t like adding a method by annotating an interface or providing an XML configuration for it, as required by JPA NamedQueries. I think providing a Java method declaration and then annotating it with the `@Query` annotation looks better.

The only method whose implementation is based on its name is [`findOneBySlug(String)`][8].

## Automatic article publication

This feature is provided by the following classes:

  * [`AutoPublicationTask`][9] &#8211; representing the task responsible for publishing all articles due for auto-publication at its execution time
  * [`AutoPublicationTrigger`][10] &#8211; responsible for providing execution times when re-scheduling the auto-publication task
  * [`AutoPublicationTaskFactory`][11] &#8211; a simple factory object providing new instances of `AutoPublicationTask`
  * [`AutoPublicationScheduler`][12] &#8211; creating new auto-publication tasks, scheduling them using `AutoPublicationTrigger` and cancelling an old task when scheduling a new one, so that at most just one task is active at any given time
  * [`SchedulingConfig`][13] &#8211; not part of the service layer, but necessary to enable scheduling and to add `TaskScheduler` bean to be used by the `AutoPublicationScheduler`

## Problems when testing AutoPublicationTask

The `AutoPublicationTask` class performs a few calls to a logger. Logging a message is the way it handles some occurences worth noting. For this reason, I decided to cover the calls in my unit tests for the class.

I&#8217;ve looked up ways of testing that a message was logged, and although creating one&#8217;s own logging appender seemed to be the most frequent suggestion, I decided to go with using a dedicated library. I found two such libraries: [SLF4J Test][14] and [SLF4JTesting][15], and I initially decided to use the first one.

As mentioned on its website, the library provides an [SLF4J][16] implementation and should be the only such implementation on the classpath. I decided to take care of this by excluding the default logger during testing by including [Maven Surefire Plugin][17] in the pom.xml and configuring it properly.

Unfortunately, it didn&#8217;t work, at least not when I was executing tests with Eclipse by using _Run As > JUnit_ Test run configuration. The following warning appeared:

```text
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in
[jar:file:(&#8230;)/ch/qos/logback/logback-classic/1.1.11/logback-classic-1.1.11.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in
[jar:file:(&#8230;)/uk/org/lidalia/slf4j-test/1.2.0/slf4j-test-1.2.0.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings
for an explanation.
SLF4J: Actual binding is of type
[ch.qos.logback.classic.util.ContextSelectorStaticBinder]
```

Not only more than one logger was found on the classpath, but also Logback (provided by spring-boot-starter dependency) was chosen as the default implementation. As a result, the tests didn&#8217;t pass. I managed to fix that by moving declaration of SLF4J Test dependency above that of the starter providing Logback, so that it would be analyzed as the first SLF4J implementation and this way used as an actual implementation during tests. Now the unit tests for `AutoPublicationTask` passed, but I still had to deal with both logger implementations being detected.

Moreover, when I executed all tests, a more serious problem was revealed. Due to the presence of multiple loggers on the classpath during testing, the application context couldn&#8217;t be loaded. As a result, integration tests couldn&#8217;t be executed. They failed with the following error:

```text
java.lang.IllegalStateException: Failed to load ApplicationContext
(&#8230;)
Caused by: java.lang.IllegalArgumentException:
LoggerFactory is not a Logback LoggerContext but Logback
is on the classpath. Either remove Logback or the competing
implementation (class uk.org.lidalia.slf4jtest.TestLoggerFactory
loaded from file:(&#8230;)/uk/org/lidalia/slf4j-test/1.2.0/slf4j-test-1.2.0.jar).
If you are using WebLogic you will need to add 'org.slf4j' to
prefer-application-packages in WEB-INF/weblogic.xml:
uk.org.lidalia.slf4jtest.TestLoggerFactory
```

Since it seemed Eclipse was simply ignoring the exclusion rule specified for Surefire plugin, I started googling for others having this problem and for a possible solution. It didn&#8217;t take long until I found [a post][18] in which someone reported having a similar problem, but with IntelliJ IDEA. The poster mentioned dependency exclusions being ignored by his IDE, but working properly when they ran tests by executing `mvn test` command.

I have previously installed Maven globally, but I never used it until now, simply because running tests and my application through Eclipse was more convenient for me. When I executed it now in my project directory, the exclusion wasn&#8217;t ignored anymore and the warning message didn&#8217;t appear.

I also tried another run configuration provided by Eclipse: _Run As > Maven test_. When using it, the plugin configuration was recognized, too, but the test results weren&#8217;t displayed on JUnit view.

Although using alternative run configuration or directly calling `mvn test` fixed the dependency exclusion being ignored, the integration tests were still failing, now with the following error:

```text
java.lang.NoClassDefFoundError:
ch/qos/logback/classic/joran/JoranConfigurator
(&#8230;)
Caused by: java.lang.ClassNotFoundException:
ch.qos.logback.classic.joran.JoranConfigurator
```

Instead of trying to make SLF4J Test work for me, I decided to drop it and try the SLF4JTesting library instead. It doesn&#8217;t require any special configuration and doesn&#8217;t interfere with a production logger on the classpath. I chose to use it in combination with [Mockito][19], which I was already using for some unit tests for `AutoPublicationTask`.

The documentation of the library [has pointed my attention][20] to another, more fundamental problem with my implementation of the class under test: the logger was assigned to a static private field during its declaration, instead of being an injected dependency or being created by one. This is the pattern of initialization of loggers I&#8217;ve seen frequently in projects and examples of code I&#8217;ve seen on the internet, so I thought it was a standard way of doing this and I simply copied it before I even thought I would end up testing logged messages. However, now I improved my code by:

  * storing the logger per instance of `AutoPublicationTask`
  * creating the logger with a factory passed as a parameter to the constructor of the class

After reimplementing the unit tests with SLF4JTesting, everything worked as it should.

## Other components

For a time I&#8217;ve been considering adding an &#8220;article manager&#8221; service class that would integrate the article repository object and the AutoPublicationScheduler. It would perform CRUD operations and re-schedule the auto-publication task when necessary, namely when saving an article that just switched to or from [`ArticleStatus.SCHEDULED_FOR_PUBLICATION`][21] status or when deleting a scheduled article.

I decided to abandon this idea and implement the operations in a controller, for the following reasons:

  * I realised it&#8217;s better to use [`@SessionAttributes`][22] in a controller class to store an intial state of an article, to test if and how it changed its status during a CRUD operation, instead of implementing my own solution in the [`Article`][23] class and using this solution in the article manager
  * I decided the article manager class did too little to warrant its existence

As for other operations that will be parts of the business logic of my application: I also decided to implement them first in controllers. This way, I will separate two tasks: implementing features and refactoring their code into separate service layer components, and I will be able to complete them in separate steps.

 [1]: {{< relref "a-new-blog-engine-project.md" >}}
 [2]: https://stackoverflow.com/questions/22963352/difference-between-repository-and-service-layer
 [3]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/repository/ArticleRepository.java
 [4]: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods
 [5]: http://docs.spring.io/spring-data/jpa/docs/1.3.0.RELEASE/reference/html/jpa.repositories.html#d0e1045
 [6]: http://docs.spring.io/spring-data/jpa/docs/1.3.0.RELEASE/reference/html/jpa.repositories.html#jpa.query-methods.named-queries
 [7]: http://docs.spring.io/spring-data/jpa/docs/1.3.0.RELEASE/reference/html/jpa.repositories.html#jpa.query-methods.at-query
 [8]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/repository/ArticleRepository.java#L57
 [9]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/service/AutoPublicationTask.java
 [10]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/service/AutoPublicationTrigger.java
 [11]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/service/AutoPublicationTaskFactory.java
 [12]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/service/AutoPublicationScheduler.java
 [13]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/config/SchedulingConfig.java
 [14]: http://projects.lidalia.org.uk/slf4j-test/
 [15]: https://github.com/portingle/slf4jtesting
 [16]: https://www.slf4j.org/
 [17]: http://maven.apache.org/surefire/maven-surefire-plugin/
 [18]: https://intellij-support.jetbrains.com/hc/en-us/community/posts/206253879-IDEA-not-honoring-maven-surefire-properties
 [19]: http://site.mockito.org/
 [20]: https://github.com/portingle/slf4jtesting#basic-example
 [21]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/domain/ArticleStatus.java#L41
 [22]: http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/SessionAttributes.html
 [23]: https://github.com/piotr-rusin/yule/blob/1284de5285a8fb6d30531d5fc26d20231a851679/src/main/java/com/github/piotr_rusin/yule/domain/Article.java
