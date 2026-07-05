package org.moashraf.sayva.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/**
 * A deliberately minimal back-stack, in place of androidx.navigation - this
 * app's navigation needs are simple (push/pop/replace) and androidx.navigation
 * multiplatform pulls in real androidx.lifecycle/savedstate artifacts that
 * aren't worth the dependency weight here.
 */
class SayvaNavController(start: Screen) {
    private val backStack = mutableStateListOf(start)
    val current get() = backStack.last()
    val canGoBack get() = backStack.size > 1

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun replaceAll(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    fun back() {
        if (canGoBack) backStack.removeAt(backStack.lastIndex)
    }
}

@Composable
fun rememberSayvaNavController(start: Screen = Screen.Welcome): SayvaNavController =
    remember { SayvaNavController(start) }
