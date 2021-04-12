package kr.s10th24b.app.hjsns

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
class Post: Serializable {
    var postId = ""
    var writerId = ""
    var message = ""
    var writeTime: Any = Any()
    var bgUri = ""
    var commentCount = 0L
    var likeCount = 0L


    fun toMap(): Map<String,Any?> {
        return mapOf(
            "postId" to postId,
            "writerId" to writerId,
            "message" to message,
            "writeTime" to writeTime,
            "bgUri" to bgUri,
            "commentCount" to commentCount,
            "likeCount" to likeCount
        )
    }
}