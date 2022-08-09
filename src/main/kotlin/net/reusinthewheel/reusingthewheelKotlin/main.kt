/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package net.reusinthewheel.reusingthewheelKotlin

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.File

fun main(args: Array<String>) {
    val options = getMarkdownOptions()
    val parser = Parser.builder(options).build()
    val renderer = HtmlRenderer.builder(options).build()

    val document = parser.parse(File("content/posts/a-new-blog-engine-project.md").readText(Charsets.UTF_8))
    val visitor = AbstractYamlFrontMatterVisitor()
    visitor.visit(document)
    println(visitor.data)
    val html = renderer.render(document)

    println(html)
}

fun getMarkdownOptions(): MutableDataSet {
    val options = MutableDataSet()
    options.set(Parser.EXTENSIONS, listOf(YamlFrontMatterExtension.create()));
    return options
}