package kr.s10th24b.app.hjsns

class CurrentUser {
    companion object {
        private var INSTANCE: Users? = null
        fun getInstance(): Users {
            if (INSTANCE == null) {
                INSTANCE = Users()
                error("CurrentUser INSTANCE not initialized!")
            }
            return INSTANCE as Users
        }
        fun setInstance(user: Users) {
            INSTANCE = user
        }
    }
}