package kr.s10th24b.app.hjsns

import android.util.Log
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
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
    @Exclude
    fun printToString(): String{
        val sb = StringBuilder()
        sb.append("name: $name\n")
        sb.append("email: $email\n")
        sb.append("photoUrl $photoUrl\n")
        sb.append("firebaseUidk: $firebaseUid\n")
        sb.append("userId: $userId\n")
        sb.append("provider: $provider\n")
        sb.append("startTime: $startTime\n")
        return sb.toString()
    }
}