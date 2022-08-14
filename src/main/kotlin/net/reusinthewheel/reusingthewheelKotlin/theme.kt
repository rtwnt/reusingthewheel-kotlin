package net.reusinthewheel.reusingthewheelKotlin

import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.stream.appendHTML
import org.w3c.dom.Document
import java.net.URL
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() {
    val website = Website(
        "a",
        URL("https://reusingthewheel.net"),
        "descriptions",
        "Test Author",
        listOf()
    )
    val config = PageConfig(
        "aaa",
        Path.of("a/b/c"),
        LocalDateTime.now(),
        setOf(TaxonomyTerm("tag1", TaxonomyType.CATEGORY), TaxonomyTerm("tag2", TaxonomyType.CATEGORY)),
        website
    )

    val sb = StringBuilder()
    val tagConsumer = sb.appendHTML()

    val tree = customLayout(config)
    println(tree.serialize(true))
}

fun customLayout(pageConfig: PageConfig): Document {
    return getHtmlDocument(pageConfig.website) { pageArticle(pageConfig, "example content") }
}

fun getHtmlDocument(website: Website, mainContent: MAIN.() -> Unit): Document {
    return createHTMLDocument().html {
        fullPageLayout(website) { mainContent(this) }
    }
}

fun getArticleHtmlDocument(pageConfig: PageConfig, content: String) {
    getHtmlDocument(pageConfig.website) {
        pageArticle(pageConfig, content)
    }
}

fun getArticleListHtmlDocument(pageConfig: PageConfig) {
    getHtmlDocument(pageConfig.website) {
        archive("", pageConfig.website.getPosts())
    }
}

fun getTaxonomyListHtmlDocument(pageConfig: PageConfig, taxonomyLinks: List<Link>) {
    getHtmlDocument(pageConfig.website) {
        taxonomyItemList("", taxonomyLinks)
    }
}

fun BODY.pageHeader(website: Website) = header {
    a(website.baseUrl.toString()) {
        +website.title
    }
    nav {
        ul {
            // website.menu_items
            website.menuItems.forEach {
                li {
                    a(it) {
                        +it
                    }
                }
            }
        }
    }
}

fun BODY.pageFooter(website: Website) = footer {
    p {
        +"&copy 2022 &nbsp;"
        a(website.baseUrl.toString()) {
            +website.title
        }
    }
}

fun HTML.fullPageLayout(website: Website, mainContent: MAIN.() -> Unit) = run {
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

fun MAIN.pageArticle(pageConfig: PageConfig, content: String) = article {
        h1 {
            +pageConfig.title
        }
        if (pageConfig.date != null) {
            time {
                +pageConfig.date.format(DateTimeFormatter.ISO_DATE_TIME)
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
                pageConfig.getTaxonomyLinksByType(TaxonomyType.CATEGORY).forEach {
                    li {
                        a(it.url.toString()) {
                            +it.title
                        }
                    }
                }
            }
        }
    }

fun MAIN.archive(title: String, yearToArticles: Map<Int?, List<PageConfig>>) = run {
    h1 { +title }
    div {
        ul {
            id="articles"
            yearToArticles.entries.forEach {
                if (it.key == null) {
                    return@forEach
                }
                li {
                    h2 { +"${it.key!!}" }
                    ul {
                        it.value.forEach {
                            li {
                                time {
                                    +it.getIsoDate()!!
                                }
                                a(it.geUrl().toString()) {
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

fun MAIN.taxonomyItemList(title: String, taxonomyLinks: List<Link>) = article {
        h1 {
            +title
        }
        div {
            ul {
                id="tags"
                taxonomyLinks.forEach {
                    li {
                        a(it.url.toString()) { +it.title }
                    }
                }
            }
        }
    }