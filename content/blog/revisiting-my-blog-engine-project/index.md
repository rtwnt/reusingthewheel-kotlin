I recently went back to developing [Yule][1] - my blog engine project based, among others, on [the Spring Boot framework][2]. I made some progress in both its front end and back end, and I would like to sum it up in this note

<!--more-->

## Changelog
I felt like front end was more lacking and required more attention than the back end, so I decided to focus on it first, modifying the latter only when the former required it. Nevertheless, I still made some back-end-only improvements.

For now, I made the following changes:

* general improvements in styling and layout, achieved with a combination of custom styles and those provided by [the Bootstrap framework][3]
* generalization of dialog windows and JavaScript/jQuery code responsible for handling interactions with them, so that they can now be reused for tasks other than deleting articles. For now, I reused them for implementing an operation allowing a user to cancel editing an article and return to the previous page of admin article table
* updates in pagination on the index page: now it consists of “Previous” and “Next” buttons and of a field containing current page number and a total number of pages
* updates in pagination of the admin article table, and adding a column sorting feature, both achieved with [Thymeleaf Spring Data Dialect][4].
* adding support for rendering Markdown with [flexmark-java][5]
* introducing new elements of service layer: [ArticleRepositoryUpdater][6] and [ArticleProvider][7] classes, and using them to reduce code complexity and repetition in controllers
* some other changes to controllers, like introducing new model attributes and refactoring article table redirection code into a separate method. These changes were related to the ones visible in the front end: adding validation status related styles to forms and adding previously mentioned cancelling feature.

Here are [some screenshots][8] of the current version.

## What’s next?
The current version is far from being final, so I’m going to improve its front end and back end and to add new features. For now I’d like to add a tag or taxonomy system and a capability to register users.

Before that, though, I’m going to refactor unit tests and improve their code coverage. Before the new services were added and calls to their methods replaced much of the code in controllers, the tests covered 45.5% of the code (according to reports generated with [EclEmma][9]). Now they cover 69.3%, so although it’s better, there is still some room for improvements. For example, there are no tests for controllers and there are some execution paths in tested methods not covered by tests.

[1]: {{< ref "/projects/yule" >}}
[2]: https://projects.spring.io/spring-boot/
[3]: https://getbootstrap.com/
[4]: https://github.com/jpenren/thymeleaf-spring-data-dialect
[5]: https://github.com/vsch/flexmark-java
[6]: https://github.com/piotr-rusin/yule/blob/1b2794d5488173de93ea6bdbd500eeeabff675a3/src/main/java/com/github/piotr_rusin/yule/service/ArticleRepositoryUpdater.java
[7]: https://github.com/piotr-rusin/yule/blob/1b2794d5488173de93ea6bdbd500eeeabff675a3/src/main/java/com/github/piotr_rusin/yule/service/ArticleProvider.java
[8]: https://imgur.com/a/ShDyk
[9]: http://www.eclemma.org/

