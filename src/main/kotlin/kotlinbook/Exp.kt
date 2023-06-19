package kotlinbook

import kotlin.random.Random

val list = listOf<Int>(1, 2, 3)

val name = "HelloWorld"

val range = Random.nextInt(name.length).let {
    it.rangeTo(it)
}

fun String.transformRandomLetter(body: String.() -> String): String {
    val range = Random.nextInt(this.length).let {
        it.rangeTo(it)
    }
    println(range)
    println("this ${this.substring(range).body()}")
    return this.replaceRange(range, this.substring(range).body())
}

val headers = mapOf<String, List<String>>(
    "foo" to listOf<String>("bar"),
    "Foo" to listOf<String>("baz")
)

fun main() {
    val someStr = "FooBarBaz".transformRandomLetter {
        "****${this.uppercase()}*****"
    }

    println(someStr)
}
