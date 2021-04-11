package kr.s10th24b.app.hjsns

import java.io.Serializable

class Users : Serializable {
    var name = ""
    var email = ""
    var photoUrl = ""
    var firebaseUid = ""
    var userId = ""
    var provider = ""
    var startTime = Any()
//    var endTime = Any()
}