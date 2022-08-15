/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package net.reusinthewheel.reusingthewheelKotlin

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.Comparator
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

fun main() {
    val parser = ContentParser()

    val website = Website(
        "Reusing the wheel",
        URL("https://reusingthewheel.net"),
        "A blog about programming and my other hobbies",
        "Piotr Rusin",
        listOf()
    )

    val allContent = File("content").walkBottomUp()
        .filter { it.path.endsWith(".md") }
        .map {
            parser.parseContent(it, website)
        }.toList()

    println("DONE!")
}

data class Link(val url: URL, val title: String)

class Website(
    val title: String,
    val baseUrl: URL,
    val description: String,
    val author: String,
    val menuItems: List<String>,
    ) {

    private val pages = mutableMapOf<String, PageConfig>()
    private val taxonomyTerms = mutableMapOf<TaxonomyType, MutableSet<TaxonomyTerm>>()

    fun getPages(): Map<String, PageConfig> {
        return pages.toMap()
    }

    fun getTaxonomyTerms(): Map<TaxonomyType, Set<TaxonomyTerm>> {
        return taxonomyTerms.entries.associate { it.key to it.value.toSet() }
    }

    fun addPage(page: PageConfig) {
        page.taxonomyTerms.forEach { taxonomyTerms.getOrPut(it.type, ::mutableSetOf).add(it) }
        if (pages.containsKey(page.title)) {
            error("Can add page ${page.title} (path: ${page.path}) - " +
                    "page ${page.title} (path: ${pages[page.title]!!.path} already exists")
        }
        pages[page.title] = page
    }

    fun getPagesGroupedByYearAndSortedByDate(filter: (page: PageConfig) -> Boolean): SortedMap<Int?, List<PageConfig>> {
        return pages.values.filter(filter)
            .sortedByDescending { it.date }
            .groupBy { it.date?.year }
            .toSortedMap(Comparator.naturalOrder<Int>().reversed())
    }

    fun getPosts(): SortedMap<Int?, List<PageConfig>> {
        return getPagesGroupedByYearAndSortedByDate { it.path.startsWith("/posts") }
    }

    fun getTaxonomyLink(taxonomyTerm: TaxonomyTerm): Link {
        return Link(
            baseUrl.extendWithPath(taxonomyTerm.getPath()),
            taxonomyTerm.value
        )
    }
}

fun URL.extendWithPath(path: Path): URL {
    return URL(this.protocol, this.host, this.port, Path.of("/", this.path, path.toString()).toString())
}

enum class TaxonomyType(val plural: String) {
    CATEGORY("categories"),
    PROJECT("projects");
}

class TaxonomyTerm(val value: String, val type: TaxonomyType) {
    private val pages = mutableSetOf<PageConfig>()

    fun addPage(pageConfig: PageConfig) {
        pages.add(pageConfig)
    }

    fun getPath(): Path {
        return Path.of("${type.plural}/${type.name.lowercase()}")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaxonomyTerm

        if (value != other.value) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

class PageConfig(
    val title: String,
    val path: Path,
    val date: LocalDateTime?,
    val taxonomyTerms: Set<TaxonomyTerm>,
    val website: Website
) {
    init {
        website.addPage(this)
        taxonomyTerms.forEach { it.addPage(this) }
    }
    fun geUrl(): URL {
        return URL(
            website.baseUrl.protocol,
            website.baseUrl.host,
            website.baseUrl.port,
            Path.of(website.baseUrl.path, path.toString()).toString()
        )
    }

    fun getTaxonomyLinksByType(type: TaxonomyType): List<Link> {
        return taxonomyTerms.filter { it.type == type }.map { website.getTaxonomyLink(it) }
    }

    fun getIsoDate(): String? {
        return date?.format(DateTimeFormatter.ISO_DATE_TIME)
    }
}

class ContentParser() {
    private val options = getMarkdownOptions()
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()
    private val frontMatterVisitor = AbstractYamlFrontMatterVisitor()

    private val taxonomyTermCache = mutableMapOf<Pair<TaxonomyType, String>, TaxonomyTerm>()

    private fun getMarkdownOptions(): MutableDataSet {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(YamlFrontMatterExtension.create()));
        return options
    }

    private fun getOrCreateTaxonomyTerm(value: String, type: TaxonomyType): TaxonomyTerm {
        return taxonomyTermCache.getOrPut(Pair(type, value)) { TaxonomyTerm(value, type) }
    }

    fun parseContent(file: File, website: Website): PageConfig {
        val document = parser.parse(file.readText(Charsets.UTF_8))
        frontMatterVisitor.visit(document)
        val taxonomiesForPage = TaxonomyType.values()
            .flatMap { type ->
                frontMatterVisitor.data[type.plural]
                    ?.map {getOrCreateTaxonomyTerm(it, type)} ?: setOf()
            }.toSet()

        val pageConfig = PageConfig(
            frontMatterVisitor.data["title"]?.get(0) ?: error("Missing title"),
            Path.of(file.path.removePrefix("content").removeSuffix(".md")),
            getDate(frontMatterVisitor.data["date"]?.get(0)),
            taxonomiesForPage,
            website
        )

        saveContentToFile(document, pageConfig.path)

        return pageConfig
    }

    private fun getDate(value: String?): LocalDateTime? {
        if (value != null) {
            val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            return LocalDateTime.parse(value, pattern)
        }
        return null
    }

    private fun saveContentToFile(document: Node, path: Path) {
        val html = renderer.render(document)
        val fullPath = Path("public").resolve("./$path/index.html")
        fullPath.parent.createDirectories()
        File(fullPath.toUri()).writeText(html)
    }
}


// TODO: archive view for reusingthewheel.net/blog
// taxonomy list views reusingthewheel.net/t.plural
// archive for taxonomy item views reusingthewheel.net/t.plural/t.value
// index page for reusingthewheel.net
