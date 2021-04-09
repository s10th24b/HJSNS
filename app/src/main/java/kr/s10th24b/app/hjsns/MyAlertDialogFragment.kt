package kr.s10th24b.app.hjsns

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.trello.rxlifecycle4.components.support.RxDialogFragment
import java.lang.ClassCastException

class MyAlertDialogFragment(
    val type: String,
    val message: String,
    val positive: String = "네",
    val negative: String = "아니오"
) : RxDialogFragment() {
    lateinit var listener: MyAlertDialogListener

    interface MyAlertDialogListener {
        fun onPositiveClick(dialog: RxDialogFragment): Boolean
        fun onNegativeClick(dialog: RxDialogFragment): Boolean
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(message)
                .setPositiveButton(positive) { dialog, which ->
                    listener.onPositiveClick(this)
                }
                .setNegativeButton(negative) { dialog, which ->
                    listener.onNegativeClick(this)
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    // Verify the Fragment .onAttach() method to instantiate the AlertDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the AlertDialogListener so we can send events to host activity
//            listener = context as MyAlertDialogListener // 여기서의 context 는, MainActivity -> ProfileFragment 를 거쳐서 온 것
            // 위처럼 하면, MainActivity에 Listener를 구현하고 거기서 써야한다. 하지만 나는 Fragment 안에서 쓰고싶기 때문에.
            // ProfileFragment에서 show 를 할 때 FragmentManager 문제인줄 알고, parentFragmentManager 와 childFragmentManager를 검색해보고
            // 지식을 쌓고 있었는데.. 우연히 한 티스토리에서 알게 되었다. show 가 문제가 아니라, 리스너 등록에서
            // 넘어온 context, 즉, 근본이 MainActivity 인 컨텍스트가 아니라 이 Dialog의 부모인 ProfileFragment를 가리키는
            // parentFragment를 캐스팅해야하는 것이었다....
            //https://jijs.tistory.com/entry/interface%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%98%EC%97%AC-DialogFragment%EC%9D%B4%EB%B2%A4%ED%8A%B8%EC%9D%98-%EA%B5%AC%ED%98%84%EB%B6%80%EB%A5%BC-%EB%8B%A4%EB%A5%B8-%EC%9E%A5%EC%86%8C%EC%97%90-%EA%B5%AC%ED%98%84%ED%95%9C%EB%8B%A4
                when(type) {
                    "Activity" -> listener = context as MyAlertDialogListener
                    "Fragment" -> listener = parentFragment as MyAlertDialogListener
                    else -> {}
                }
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(("$context must implement AlertDialogListener"))
        }
    }
}