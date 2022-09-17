package net.reusinthewheel.reusingthewheelKotlin.data

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
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
        internal set
    override var slug: String = ""
        private set
    override var url: String = ""
        private set
    override var date: LocalDateTime? = null
        private set
    override var content: String = ""
        private set
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
        val result = childPages.values.toMutableList();
        childPages.values.forEach { result.addAll(it.getChildPagesRecursive()) }
        return result;
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
                return;
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

    companion object {
        fun from(markdownContent: MarkdownContent, taxonomyTermTypes: Set<TaxonomyType>): PageImpl {
            val result = PageImpl()
            result.title = markdownContent.frontMatter["TITLE"]?.lastOrNull() ?: error("Missing title")
            result.slug = markdownContent.frontMatter["SLUG"]?.lastOrNull() ?: result.title.slugify()
            result.url = markdownContent.frontMatter["URL"]?.lastOrNull() ?: ""
            result.date = getDate(markdownContent.frontMatter["DATE"]?.lastOrNull())
            result.content = markdownContent.renderedHtml
            result.description = markdownContent.frontMatter["DESCRIPTION"]?.lastOrNull() ?: ""
            result.author = markdownContent.frontMatter["AUTHOR"]?.lastOrNull() ?: ""

            TaxonomyTermProvider.getOrCreateForAll(markdownContent, taxonomyTermTypes).forEach {
                result.addTaxonomyTermPage(it)
            }

            // ignoring child pages - they will be added during public directory walk

            return result
        }

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

object TaxonomyTermProvider {
    private val taxonomyTermCache = mutableMapOf<Pair<TaxonomyType, String>, PageImpl>()

    fun getOrCreate(value: String, type: TaxonomyType): PageImpl {
        return taxonomyTermCache.getOrPut(Pair(type, value)) {
            val result = PageImpl()
            result.title = value
            result
        }
    }

    fun getOrCreateForAll(markdownContent: MarkdownContent, taxonomyTermTypes: Set<TaxonomyType>): List<PageImpl> {
        return taxonomyTermTypes.flatMap { type ->
            val values = markdownContent.frontMatter[type.plural] ?: return listOf()
            values.map { value ->
                getOrCreate(value, type)
            }
        }
    }

    fun getAllTypes(): Set<TaxonomyType> {
        return taxonomyTermCache.keys
            .map { it.first }
            .distinct()
            .toSet()
    }

    fun getAllTaxonomyTerms(): Map<TaxonomyType, List<Page>> {
        return taxonomyTermCache.entries
            .map { it.key.first to it.value as Page}
            .groupBy { it.first }
            .entries
            .associate { it.key to it.value.map { it.second } }
    }
}

class WebsiteContentBuilder(private val taxonomyTypes: Set<TaxonomyType>) {
    private val markdownContentParser = MarkdownContentParser()

    private val pages = mutableListOf<PageImpl>()
    lateinit var rootSection: PageImpl

    fun startSection(file: File): Boolean {
        // executed for directories and files
        val page = PageImpl()
        page.title = file.name
        pages.add(page)
        return true
    }

    fun addContent(file: File) {
        // executed for directories and files
        if (pages.isEmpty()) {
            error("Missing page for ${file.path}")
        }
        val markdownContent = markdownContentParser.parseContent(file)
        val page = PageImpl.from(markdownContent, taxonomyTypes)
        // at this point, last() is always representing an index page of a section, a list page
        if (file.name == "_index.md") {
            // merge with .last() - which will always represent directory
            pages.last().merge(page)
        } else {
            // add as a child of last
            pages.last().addChildPage(page)
        }
    }

    fun finishSection(file: File): Boolean {
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
            .onEnter { startSection(it) }
            .onLeave { finishSection(it) }
            .forEach {
                if (!it.isDirectory) {
                    println(it.canonicalPath)
                    try {
                        addContent(it)
                    } catch (e: Exception) {
                        println("Error: ${e.message}. Skipping file ${it.path}")
                    }
                }
            }
    }
}

data class MarkdownContent(val frontMatter: Map<String, List<String>>, val renderedHtml: String)

class MarkdownContentParser() {
    private val options = getMarkdownOptions()
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    private fun getMarkdownOptions(): MutableDataSet {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(YamlFrontMatterExtension.create()))
        return options
    }

    fun parseContent(file: File): MarkdownContent {
        val markdownDocument = parser.parse(file.readText(Charsets.UTF_8))
        val frontMatterVisitor = AbstractYamlFrontMatterVisitor()
        frontMatterVisitor.visit(markdownDocument)

        return MarkdownContent(
            frontMatterVisitor.data,
            renderer.render(markdownDocument)
        )
    }
}