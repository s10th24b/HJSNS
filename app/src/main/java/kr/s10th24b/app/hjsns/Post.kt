package kr.s10th24b.app.hjsns

import java.io.Serializable

class Post: Serializable {
    var postId = ""
    var writerId = ""
    var message = ""
    var writeTime: Any = Any()
    var bgUri = ""
    var commentCount = 0L
    var likeCount = 0L
}