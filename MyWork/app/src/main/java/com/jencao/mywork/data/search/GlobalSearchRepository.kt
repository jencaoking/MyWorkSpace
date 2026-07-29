package com.jencao.mywork.data.search

import com.jencao.mywork.data.local.AppDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 跨模块全局搜索仓储：并行检索所有功能板块，聚合为统一的 [GlobalSearchResult] 列表。
 *
 * 采用 LIKE 匹配（每个板块在 DAO 层各自提供 search 查询），避免为全局搜索新增 FTS 表与
 * 数据库迁移；单个板块最多返回 50 条，命中数量足够覆盖移动端检索场景。
 */
@Singleton
class GlobalSearchRepository @Inject constructor(
    private val db: AppDatabase
) {
    suspend fun search(keyword: String): List<GlobalSearchResult> {
        val kw = keyword.trim()
        if (kw.isEmpty()) return emptyList()
        return coroutineScope {
            val tasks = async {
                db.taskDao().search(kw).map { e ->
                    GlobalSearchResult(
                        id = e.id,
                        module = SearchModule.TASK,
                        title = (e.title ?: "").ifBlank { "未命名任务" },
                        snippet = (e.content ?: "").ifBlank { e.repeatRule ?: "" }.take(120)
                    )
                }
            }
            val notes = async {
                db.noteDao().searchLike(kw).map { e ->
                    GlobalSearchResult(
                        id = e.id,
                        module = SearchModule.NOTE,
                        title = (e.title ?: "").ifBlank { "未命名笔记" },
                        snippet = (e.content ?: "").take(120)
                    )
                }
            }
            val movies = async {
                db.movieBookDao().search(kw).map { e ->
                    val snip = listOf(e.originalTitle, e.overview, e.note)
                        .firstNotNullOfOrNull { it?.takeIf { s -> s.isNotBlank() } } ?: ""
                    GlobalSearchResult(
                        id = e.id,
                        module = SearchModule.MOVIE_BOOK,
                        title = (e.title ?: "").ifBlank { "未命名条目" },
                        snippet = snip.take(120)
                    )
                }
            }
            val sports = async {
                db.sportRecordDao().search(kw).map { e ->
                    GlobalSearchResult(
                        id = e.id,
                        module = SearchModule.SPORT,
                        title = (e.type ?: "").ifBlank { "运动记录" },
                        snippet = (e.note ?: "").take(120)
                    )
                }
            }
            val english = async {
                db.englishWordDao().search(kw).map { e ->
                    GlobalSearchResult(
                        id = e.id,
                        module = SearchModule.ENGLISH,
                        title = e.word,
                        snippet = (e.meaning ?: "").ifBlank { e.example ?: "" }.take(120)
                    )
                }
            }
            val health = async {
                db.healthRecordDao().search(kw).map { e ->
                    GlobalSearchResult(
                        id = e.id,
                        module = SearchModule.HEALTH,
                        title = (e.type ?: "").ifBlank { "健康记录" },
                        snippet = (e.note ?: "").take(120)
                    )
                }
            }
            val account = async {
                db.accountRecordDao().search(kw).map { e ->
                    GlobalSearchResult(
                        id = e.id,
                        module = SearchModule.ACCOUNT,
                        title = "${e.category ?: ""} ${e.amount}",
                        snippet = (e.note ?: "").take(120)
                    )
                }
            }
            val pomodoro = async {
                db.pomodoroSessionDao().search(kw).map { e ->
                    GlobalSearchResult(
                        id = e.id,
                        module = SearchModule.POMODORO,
                        title = "${(e.mode ?: "").ifBlank { "专注" }} ${e.durationMin}分钟",
                        snippet = (e.note ?: "").take(120)
                    )
                }
            }
            listOf(tasks, notes, movies, sports, english, health, account, pomodoro)
                .awaitAll()
                .flatten()
        }
    }
}
