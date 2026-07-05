package org.moashraf.sayva

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform