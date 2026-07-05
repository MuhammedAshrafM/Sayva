package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.Flow
import org.moashraf.sayva.data.LearnCategory
import org.moashraf.sayva.data.Lesson
import org.moashraf.sayva.data.QuizQuestion

/**
 * Consumer of `LearnCategoriesScreen`, `LessonPlayerScreen`, `PracticeScreen`.
 *
 * Persistence: **not** DB-backed. Learn content is static reference data —
 * categories, lessons, and quiz questions ship with the app binary. If we ever
 * do dynamic lesson delivery (e.g. content updates over the wire) this becomes
 * an in-memory cache in front of a remote source.
 *
 * The learned/total count per category comes from [ProgressRepository] — this
 * repo exposes the static content only.
 */
interface LessonsRepository {

    /** All learning categories, in display order. Static — not a Flow. */
    fun getCategories(): List<LearnCategory>

    /** All lessons belonging to a category. */
    fun getLessonsInCategory(categoryId: String): List<Lesson>

    /** Load a single lesson. */
    fun getLesson(lessonId: String): Lesson?

    /** Quiz questions for a specific lesson's practice mode. */
    fun getQuizForLesson(lessonId: String): List<QuizQuestion>

    /**
     * Observe user's per-category progress (learned / total). Combines the
     * static category data with [ProgressRepository]'s per-lesson completion.
     */
    fun observeCategoryProgress(): Flow<Map<String, CategoryProgress>>
}

/** Per-category learned/total pair, keyed by category id. */
data class CategoryProgress(
    val learned: Int,
    val total: Int,
)
