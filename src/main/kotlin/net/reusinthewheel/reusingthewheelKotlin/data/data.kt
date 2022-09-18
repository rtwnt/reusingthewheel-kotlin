package net.reusinthewheel.reusingthewheelKotlin.data

import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun String.slugify(whitespaceReplacement: String = "-"): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("[^\\p{ASCII}]".toRegex(), "")
        .replace("[^a-zA-Z\\d\\s]+".toRegex(), "").trim()
        .replace("\\s+".toRegex(), whitespaceReplacement)
        .lowercase()
}

interface Page {
    val title: String
    val slug: String
    val url: String
    val date: LocalDateTime?
    val content: String
    val description: String
    val author: String
    val parent: Page?
    val taxonomyTermPages: Map<String, Page>
    val childPages: Map<String, Page>
    val childPagesByYear: Map<Int, List<Page>>
    val menuItemPages: List<Page>
}

class PageImpl: Page{
    override var title: String = ""
    override var slug: String = ""
        private set
    override var url: String = ""
        private set
    override var date: LocalDateTime? = null
        private set
    override var content: String = ""
    override var description: String = ""
        private set
    override var author: String = ""
        private set

    override var parent: PageImpl? = null
        private set(value) {
            field = value
            if (value == null) {
                return
            }
            url = value.url + "/" + slug
        }

    private val _taxonomyTermPages: MutableMap<String, PageImpl> = mutableMapOf()
    override val taxonomyTermPages: Map<String, PageImpl> get() = _taxonomyTermPages.toMap()
    private fun addTaxonomyTermPage(taxonomyTermPage: PageImpl) {
        _taxonomyTermPages[url] = taxonomyTermPage
        // child page of taxonomy term page is one of the pages classified by the taxonomy term
        taxonomyTermPage._childPages[url] = this
    }

    private val _childPages: MutableMap<String, PageImpl> = mutableMapOf()
    override val childPages: Map<String, PageImpl> get() = _childPages.toMap()
    fun addChildPage(childPage: PageImpl) {
        _childPages[childPage.url] = childPage
        childPage.parent = this
    }

    override val childPagesByYear: Map<Int, List<Page>>
        get() = childPages.values.filter { it.date != null }.groupBy { it.date!!.year }
    private fun getChildPagesRecursive(): List<PageImpl> {
        val result = childPages.values.toMutableList()
        childPages.values.forEach { result.addAll(it.getChildPagesRecursive()) }
        return result
    }

    private val _menuItems = mutableListOf<String>()
    override val menuItemPages: List<Page>
    get() {
        val allChildPages = getChildPagesRecursive().associateBy { it.url }
        val missingUrls = _menuItems.filter { allChildPages[it] == null }
        if (missingUrls.isNotEmpty()) {
            error("Missing URLs in menu for $url: ${missingUrls.joinToString(", ")}")
        }
        return _menuItems.map { allChildPages[it]!! }
    }

    fun merge(other: PageImpl) {
        if (other.title.isNotEmpty()) {
            title = other.title
        }
        if (other.date != null) {
            date = other.date
        }
        if (other.url.isNotEmpty()) {
            url = other.url
        }
        if (other.slug.isNotEmpty()) {
            slug = other.slug
        }
        if (other.content.isNotEmpty()) {
            content = other.content
        }
        if (other.description.isNotEmpty()) {
            description = other.description
        }
        if (other.author.isNotEmpty()) {
            author = other.author
        }
        if (other.parent != null) {
            parent?.merge(other.parent!!)
        }
        other.childPages.forEach { (id, otherChildPage) ->
            val childPage = _childPages[id]
            if (childPage == null) {
                addChildPage(otherChildPage)
            } else {
                _childPages[id]!!.merge(otherChildPage)
            }
        }

        other.taxonomyTermPages.forEach { (id, otherChildPage) ->
            val page = _taxonomyTermPages[id]
            if (page == null) {
                addTaxonomyTermPage(otherChildPage)
            } else {
                _taxonomyTermPages[id]!!.merge(otherChildPage)
            }
        }

        other._menuItems.forEach {
            if (_menuItems.contains(it)) {
                return
            }
            _menuItems.add(it)
        }

    }

    fun validate() {
        val urlsSeen = mutableSetOf<String>()
        isValidNode(urlsSeen)
        getChildPagesRecursive().forEach { it.isValidNode(urlsSeen) }
    }

    private fun isValidNode(urlsSeen: MutableSet<String>) {
        if (url.isBlank()) {
            error("Invalid URL value: $url")
        }
        if (urlsSeen.contains(url)) {
            error("Duplicate url: $url")
        }
        urlsSeen.add(url)

        if (parent == null) {
            if (slug.isNotBlank()) {
                error("Invalid slug value for root $slug")
            }
        } else {
            if (slug.isBlank()) {
                error("Invalid slug value for non-root node $slug")
            }
        }
    }

    fun mapFromFrontMatter(frontMatter: FrontMatter) {
        title = frontMatter.title ?: error("Missing title")
        slug = frontMatter.slug ?: title.slugify()
        url = frontMatter.url ?: ""
        date = getDate(frontMatter.date)
        description = frontMatter.description ?: ""
        author = frontMatter.author ?: ""
        _menuItems.addAll(frontMatter.menuItems)

        TaxonomyTermProvider.getOrCreateForAll(frontMatter).forEach {
            addTaxonomyTermPage(it)
        }
    }

    companion object {
        private fun getDate(value: String?): LocalDateTime? {
            if (value != null) {
                val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                return LocalDateTime.parse(value, pattern)
            }
            return null
        }
    }
}

data class TaxonomyType(val singular: String, val plural: String)

data class TaxonomyTypeAndValue(val type: String, val value: String)

object TaxonomyTermProvider {
    private val taxonomyTermCache = mutableMapOf<TaxonomyTypeAndValue, PageImpl>()

    fun getOrCreate(value: String, type: String): PageImpl {
        return taxonomyTermCache.getOrPut(TaxonomyTypeAndValue(type, value)) {
            val result = PageImpl()
            result.title = value
            result
        }
    }

    fun getOrCreateForAll(frontMatter: FrontMatter): List<PageImpl> {
        return frontMatter.taxonomies.flatMap { entry ->
            entry.value.map { value ->
                getOrCreate(value, entry.key)
            }
        }
    }

    fun getAllTypes(): Set<String> {
        return taxonomyTermCache.keys
            .map { it.type }
            .distinct()
            .toSet()
    }

    fun getAllTaxonomyTerms(): Map<String, List<Page>> {
        return taxonomyTermCache.entries
            .map { it.key.type to it.value as Page}
            .groupBy { it.first }
            .entries
            .associate { entry -> entry.key to entry.value.map { it.second } }
    }
}

class WebsiteContentBuilder(private val taxonomyTypes: Set<TaxonomyType>) {
    private val markdownContentParser = MarkdownContentParser()

    private val pages = mutableListOf<PageImpl>()
    lateinit var rootSection: PageImpl

    private fun startDirectory(file: File): Boolean {
        // executed for directories and files
        val page = PageImpl()
        page.title = file.name
        pages.add(page)
        return true
    }

    private fun addContentOrMetadata(file: File) {
        if (pages.isEmpty()) {
            error("Missing page for ${file.path}")
        }
        if (file.extension == "md") {
            pages.last().content = markdownContentParser.parseContent(file)
        } else if (file.extension == "json") {
            val frontMatter = Json.decodeFromString(FrontMatter.serializer(), file.readText(Charsets.UTF_8))
            pages.last().mapFromFrontMatter(frontMatter)
        }
    }

    private fun finishDirectory(file: File): Boolean {
        if (pages.isEmpty()) {
            error("No section to process")
        }
        val currentPage = pages.removeLast()
        if (pages.isEmpty()) {
            rootSection = currentPage
        } else {
            pages.last().addChildPage(currentPage)
        }
        return true
    }

    fun parseAllContent() {
        File("content").walkTopDown()
            .onEnter { startDirectory(it) }
            .onLeave { finishDirectory(it) }
            .forEach {
                if (!it.isDirectory) {
                    println(it.canonicalPath)
                    try {
                        addContentOrMetadata(it)
                    } catch (e: Exception) {
                        println("Error: ${e.message}. Skipping file ${it.path}")
                    }
                }
            }
    }
}
@Serializable
data class FrontMatter(
    val title: String?,
    val slug: String?,
    val url: String?,
    val date: String?,
    val content: String?,
    val description: String?,
    val author: String?,
    val menuItems: List<String>,
    val taxonomies: Map<String, List<String>>// Dynamic taxonomies - will not require recompiling of the project
)

class MarkdownContentParser {
    private val options = getMarkdownOptions()
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    private fun getMarkdownOptions(): MutableDataSet {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(YamlFrontMatterExtension.create()))
        return options
    }

    fun parseContent(file: File): String {
        val markdownFileContent = file.readText(Charsets.UTF_8)
        val markdownDocument = parser.parse(markdownFileContent)

        return renderer.render(markdownDocument)
    }
}