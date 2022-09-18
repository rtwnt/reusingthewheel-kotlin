package net.reusinthewheel.reusingthewheelKotlin.theme

import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import net.reusinthewheel.reusingthewheelKotlin.data.Page
import net.reusinthewheel.reusingthewheelKotlin.data.PageImpl
import net.reusinthewheel.reusingthewheelKotlin.data.TaxonomyTermProvider
import org.w3c.dom.Document

fun customLayout(page: PageImpl): Document {
    return getHtmlDocument(page) { pageArticle(page, "example content") }
}

fun getHtmlDocument(website: Page, mainContent: MAIN.() -> Unit): Document {
    return createHTMLDocument().html {
        fullPageLayout(website) { mainContent(this) }
    }
}

fun getStartPageHtmlDocument(website: Page, content: String): Document {
    return getHtmlDocument(website) {
        startPageContent(website, content)
    }
}

fun getArticleHtmlDocument(page: Page, content: String): Document {
    return getHtmlDocument(page) {
        pageArticle(page, content)
    }
}

fun getArticleListHtmlDocument(page: Page): Document {
    return getHtmlDocument(page) {
        archive("", page.childPagesByYear)
    }
}

fun getTaxonomyListHtmlDocument(website: PageImpl): Document {
    return getHtmlDocument(website) {
        TaxonomyTermProvider.getAllTaxonomyTerms().entries.forEach {
            taxonomyTermList(it.key, website, it.value)
        }
    }
}

fun BODY.pageHeader(website: Page) = header {
    a(website.url) {
        +website.title
    }
    nav {
        ul {
            // website.menu_items
            website.menuItemPages.forEach {
                li {
                    a(it.url) {
                        +it.title
                    }
                }
            }
        }
    }
}

fun BODY.pageFooter(website: Page) = footer {
    p {
        +"&copy 2022 &nbsp;"
        a(website.url) {
            +website.title
        }
    }
}

fun HTML.fullPageLayout(website: Page, mainContent: MAIN.() -> Unit) = run {
    head {
        title { +website.title }

        meta(charset = "utf-8")
        meta("viewport", "width=device-width, initial-scale=1")
        meta("description", website.description)
        meta("author", website.author)
        meta("keywords", "k1, k2, k3")
        link(rel="stylesheet", href="css/style.css")
    }
    body {
        pageHeader(website)
        main {
            mainContent(this)
        }
        pageFooter(website)
    }
}

fun MAIN.startPageContent(website: Page, content: String) = article {
    h1 {
        +website.title
    }

    div {
        website.content
    }

    h2 {
        +"Most recent posts"
    }
    ul {
        website.childPages
            .values
            .filter { it.url.contains("blog") }
            .sortedBy { it.date }
            .take(5)
            .forEach { page ->
                li {
                    a(page.url) {
                        span { +page.title }
                        time { +"${page.date!!}" }
                    }
                }
            }
    }

    h2 {
        +"Most frequent topics"
    }
    ul {
        TaxonomyTermProvider.getAllTaxonomyTerms()
            .flatMap { it.value }
            .sortedBy { it.childPages.size }
            .take(5)
            .forEach {
                li {
                    a("${it.url}"){
                        span { +it.title }
                        span { +"(${it.childPages.size})" }
                    }
                }
            }
    }
}

fun MAIN.pageArticle(page: Page, content: String) = article {
        h1 {
            +page.title
        }
        if (page.date != null) {
            time {
                +"${page.date}"
            }
        }
        div {
            unsafe {
                +content
            }
        }
        div {
            ul {
                id = "tags"
                page.taxonomyTermPages.values.forEach {
                    li {
                        a(it.url) {
                            +it.title
                        }
                    }
                }
            }
        }
    }

fun MAIN.archive(title: String, yearToArticles: Map<Int, List<Page>>) = run {
    h1 { +title }
    div {
        ul {
            id="articles"
            yearToArticles.entries.forEach {
                li {
                    h2 { +"${it.key}" }
                    ul {
                        it.value.forEach {
                            li {
                                time {
                                    +"${it.date!!}"
                                }
                                a(it.url) {
                                    +it.title
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun MAIN.taxonomyTermList(title: String, website: Page, terms: List<Page>) = article {
        h1 {
            +title
        }
        div {
            ul {
                id="taxonomy-terms"
                terms.forEach {
                    li {
                        a(it.url) { +"${it.title} (${it.childPages.size})" }
                    }
                }
            }
        }
    }