package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.moashraf.sayva.data.LearnCategory
import org.moashraf.sayva.data.Lesson
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.data.QuizQuestion

/**
 * Static-content [LessonsRepository] — categories, lessons, and quiz questions
 * ship with the app binary via [MockSayvaData]. When we introduce a remote
 * content source (CDN + local cache), this becomes the sync boundary.
 *
 * The mock data has `learned`/`total` fields baked into [LearnCategory]. We
 * ignore those and derive category progress from [ProgressRepository] instead —
 * that's the correct source of truth (per-user, mutable). The fields on
 * LearnCategory itself get treated as defaults for a fresh install.
 */
class LessonsRepositoryImpl(
    private val progressRepository: ProgressRepository,
) : LessonsRepository {

    override fun getCategories(): List<LearnCategory> = MockSayvaData.learnCategories

    override fun getLessonsInCategory(categoryId: String): List<Lesson> {
        // MockSayvaData.lessons doesn't carry category association today — the
        // one seeded lesson ("Hello") belongs conceptually to Greetings. When
        // real content lands, add a categoryId field to Lesson and filter here.
        return if (categoryId == "greetings") MockSayvaData.lessons else emptyList()
    }

    override fun getLesson(lessonId: String): Lesson? =
        MockSayvaData.lessons.firstOrNull { it.id == lessonId }

    override fun getQuizForLesson(lessonId: String): List<QuizQuestion> {
        // Mock has a single quiz set that we reuse regardless of lesson.
        return MockSayvaData.quizQuestions
    }

    override fun observeCategoryProgress(): Flow<Map<String, CategoryProgress>> =
        // Real implementation would combine ProgressRepository with per-category
        // completion counts from a `lesson_completions` table. Until that table
        // exists, use each category's static "learned/total" from MockSayvaData
        // as the initial state and never emit updates. The Flow contract still
        // holds — collectors see something on subscribe.
        flowOf(
            MockSayvaData.learnCategories.associate { category ->
                category.id to CategoryProgress(
                    learned = category.learned,
                    total = category.total,
                )
            },
        )
}
