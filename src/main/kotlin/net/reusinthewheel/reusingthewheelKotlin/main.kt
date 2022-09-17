/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package net.reusinthewheel.reusingthewheelKotlin

import java.net.URL
import java.nio.file.Path

fun URL.extendWithPath(path: Path): URL {
    return URL(this.protocol, this.host, this.port, Path.of("/", this.path, path.toString()).toString())
}
