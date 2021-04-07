package kr.s10th24b.app.hjsns

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
class Comment : Serializable {
    var commentId = ""
    var postId = ""
    var writerId = ""
    var message = ""
    var writeTime: Any = Any()
    var bgUri = ""

    @Exclude
    fun toMap(): Map<String,Any?> {
        return mapOf(
            "commentId" to commentId,
            "postId" to postId,
            "writerId" to writerId,
            "message" to message,
            "writeTime" to writeTime,
            "bgUri" to bgUri
        )
    }
}