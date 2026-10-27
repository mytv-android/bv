package com.github.mytv.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.reply.Comment
import dev.aaa1115910.biliapi.entity.reply.CommentPage
import dev.aaa1115910.biliapi.entity.reply.CommentReplyPage
import dev.aaa1115910.biliapi.entity.reply.CommentSort
import dev.aaa1115910.biliapi.repositories.CommentRepository
import com.github.mytv.bv.BVApp
import com.github.mytv.bv.util.Prefs
import com.github.mytv.bv.util.fException
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class CommentViewModel(
    private val commentRepository: CommentRepository
) : ViewModel() {
    companion object {
        val logger = KotlinLogging.logger {}
    }

    var commentId = 0L
    var commentType = 0L

    val comments = mutableStateListOf<Comment>()
    val replies = mutableStateListOf<Comment>()
    var replyRootComment by mutableStateOf<Comment?>(null)

    var rpid by mutableLongStateOf(0L)
    var rpCount by mutableIntStateOf(0)

    var nextCommentPage = CommentPage()
    var nextCommentReplyPage = CommentReplyPage()

    var commentSort by mutableStateOf(CommentSort.Hot)
    var replySort by mutableStateOf(CommentSort.Time)

    var hasMoreComments by mutableStateOf(true)
    var hasMoreReplies by mutableStateOf(true)
    var refreshingComments by mutableStateOf(false)
    var refreshingReplies by mutableStateOf(false)
    var updatingComments by mutableStateOf(false)
    var updatingReplies by mutableStateOf(false)

    suspend fun loadMoreComment() {
        if (updatingComments) return
        updatingComments = true
        if (!hasMoreComments) {
            updatingComments = false
            delay(300)
            refreshingComments = false
            return
        }
        logger.fInfo { "Load more comments: [commentId=$commentId, commentType=$commentType, page=$nextCommentPage]" }
        runCatching {
            val commentsData = commentRepository.getComments(
                id = commentId,
                type = commentType,
                page = nextCommentPage,
                sort = commentSort,
                preferApiType = Prefs.apiType
            )
            nextCommentPage = commentsData.nextPage
            hasMoreComments = commentsData.hasNext
            val firstPage = comments.isEmpty()
            // 接口把置顶评论单独返回：只在第一页插到列表最前
            if (firstPage) {
                comments.addAll(commentsData.topReplies)
            }
            // 按 rpid 去重，避免置顶评论同时出现在普通列表 / 分页重复
            val knownIds = comments.mapTo(HashSet()) { it.rpid }
            commentsData.comments.forEach { comment ->
                if (knownIds.add(comment.rpid)) comments.add(comment)
            }
            // 若置顶评论混在普通列表中（接口字段标记），稳定排序把置顶挪到最前，其余顺序不变
            if (firstPage) {
                val ordered = comments.sortedByDescending { it.isPinned }
                comments.clear()
                comments.addAll(ordered)
            }
        }.onFailure {
            logger.fException(it) { "Load more comments failed" }
            withContext(Dispatchers.Main) {
                "加载评论失败：${it.localizedMessage}".toast(BVApp.context)
            }
        }
        updatingComments = false
        delay(300)
        refreshingComments = false
    }

    suspend fun switchCommentSort(newSort: CommentSort) {
        logger.fInfo { "Switch comment sort to ${newSort.name}" }
        commentSort = newSort
        refreshComments()
    }

    suspend fun refreshComments() {
        refreshingComments = true
        logger.fInfo { "refresh comments" }
        nextCommentPage = CommentPage()
        hasMoreComments = true
        comments.clear()
        loadMoreComment()
    }

    suspend fun loadMoreReplies() {
        if (updatingReplies) return
        updatingReplies = true
        if (!hasMoreReplies) {
            updatingReplies = false
            delay(300)
            refreshingReplies = false
            return
        }
        logger.fInfo { "Load more replies, commentId=$commentId, commentType=$commentType, page=$nextCommentReplyPage" }
        runCatching {
            val commentRepliesData = commentRepository.getCommentReplies(
                rpid = rpid,
                type = commentType,
                commentId = commentId,
                page = nextCommentReplyPage,
                sort = replySort,
                preferApiType = Prefs.apiType
            )
            nextCommentReplyPage = commentRepliesData.nextPage
            hasMoreReplies = commentRepliesData.hasNext
            if (replyRootComment == null) replyRootComment = commentRepliesData.rootComment
            replies.addAll(commentRepliesData.replies)
        }.onFailure {
            logger.fException(it) { "Load more replies failed" }
        }
        updatingReplies = false
        delay(300)
        refreshingReplies = false
    }

    suspend fun switchReplySort(newSort: CommentSort) {
        logger.fInfo { "Switch reply sort to ${newSort.name}" }
        replySort = newSort
        refreshReplies()
    }

    suspend fun refreshReplies() {
        refreshingReplies = true
        logger.fInfo { "refresh replies" }
        nextCommentReplyPage = CommentReplyPage()
        hasMoreReplies = true
        replies.clear()
        loadMoreReplies()
    }
}
