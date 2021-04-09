package kr.s10th24b.app.hjsns

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.jakewharton.rxbinding4.view.clicks
import com.trello.rxlifecycle4.android.ActivityEvent
import com.trello.rxlifecycle4.android.FragmentEvent
import com.trello.rxlifecycle4.components.support.RxDialogFragment
import com.trello.rxlifecycle4.components.support.RxFragment
import com.trello.rxlifecycle4.kotlin.bindToLifecycle
import com.trello.rxlifecycle4.kotlin.bindUntilEvent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import kr.s10th24b.app.hjsns.databinding.CardPostRecyclerBinding
import kr.s10th24b.app.hjsns.databinding.FragmentCardsBinding
import splitties.toast.toast
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CardsFragment : RxFragment(), MyAlertDialogFragment.MyAlertDialogListener {
    lateinit var binding: FragmentCardsBinding
    lateinit var recyclerViewAdapter: CardRecyclerViewAdapter
    lateinit var layoutManager: LinearLayoutManager
    lateinit var postLikeListener: OnSuccessListener<Activity>
    var clickCardSubject = PublishSubject.create<Post>()
    lateinit var menuCard: Post

    //    lateinit var mState: Bundle
    var alreadyCreated = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        toast("onCreate")
        if (savedInstanceState == null) {
            Log.d("KHJ", "savedInstanceState is null!")
        } else {
            Log.d("KHJ", "savedInstanceState is not null!")
        }
        // this registration is removed in onDestroy()
        arguments?.let {
        }

        if (!alreadyCreated) {
            setFirebaseDatabasePostListener()
        }
        setClickCardSubject()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
//        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        return when (item.itemId) {
            R.id.menu_item_modify -> {
                val intent = Intent(context, WriteActivity::class.java)
                intent.putExtra("mode", "postModify")
                intent.putExtra("post", menuCard)
                startActivity(intent)
                true
            }
            R.id.menu_item_remove -> {
                val removeAlertDialog = MyAlertDialogFragment("Fragment","정말 카드를 삭제하시겠습니까?")
                removeAlertDialog.show(childFragmentManager, "Item Removing")
                true
            }
            R.id.menu_item_report -> {
                true
            }
            else -> {
                super.onContextItemSelected(item)
            }
        }
    }

    override fun onPositiveClick(dialog: RxDialogFragment): Boolean {
        val dbRef = FirebaseDatabase.getInstance().getReference("")
        val postPath = "/Posts/${menuCard.postId}"
        val commentPath = "/Comments/${menuCard.postId}"
        val likePath = "/Likes/${menuCard.postId}"
        val childUpdates = hashMapOf<String, Any?>(
            postPath to null,
            commentPath to null,
            likePath to null
        )
        dbRef.updateChildren(childUpdates)
            .addOnSuccessListener(requireActivity()) { toast("카드 삭제 성공") }
            .addOnCanceledListener(requireActivity()) { toast("카드 삭제 취소") }
            .addOnFailureListener(requireActivity()) { toast("카드 삭제 실패") }
        return true
    }

    override fun onNegativeClick(dialog: RxDialogFragment): Boolean {
        return false
    }

    private fun setClickCardSubject() {
//        toast("setClickCardSubject!")
        clickCardSubject
            .observeOn(AndroidSchedulers.mainThread())
            .bindUntilEvent(this, FragmentEvent.DESTROY)
            .subscribe {
                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra("post", it)
                startActivity(intent)
            }
    }

    private fun setFirebaseDatabasePostListener() {
        recyclerViewAdapter = CardRecyclerViewAdapter()
        // FireBase Data pulling and save it to posts variable
        FirebaseDatabase.getInstance().getReference("/Posts")
            .orderByChild("writeTime").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                    toast("onChildAdded()")
                    snapshot.let { ss ->
                        val post = ss.getValue(Post::class.java)
                        post?.let { newPost ->
                            // 새 글이 마지막 부분에 추가된 경우
                            if (previousChildName == null) {
//                                toast("새 글이 마지막부분에 추가")
                                recyclerViewAdapter.cardList.add(newPost as Post)
                                recyclerViewAdapter.notifyItemInserted(0)
                                recyclerViewAdapter.notifyDataSetChanged()
//                                recyclerViewAdapter.notifyDataSetChanged()
                            } else { // 글이 중간에 삽입된 경우... 근데 이건 이 앱에선 상관없음. 무조건 마지막에 추가되도록 했으니.
//                                toast("새 글이 중간에 추가")
//                                val prevIndex = recyclerViewAdapter.cardList
//                                    .indexOfFirst { it.postId == previousChildName }
//                                    recyclerViewAdapter.cardList.add(prevIndex + 1, newPost)
                                recyclerViewAdapter.cardList.add(newPost)
                                recyclerViewAdapter.notifyItemInserted(recyclerViewAdapter.cardList.lastIndex)
                                recyclerViewAdapter.notifyDataSetChanged()
                                val firstCompVisPos =
                                    layoutManager.findFirstCompletelyVisibleItemPosition()
//                                val visibleItemCount = binding.cardsFragmentRecyclerView.childCount
                                val visibleItemCount = layoutManager.childCount
                                val totalItemCount = layoutManager.itemCount
//                                toast("firstCompVis: $firstCompVisPos, totalItemCount: $totalItemCount visibleItemCount: $visibleItemCount")
                                if (firstCompVisPos + visibleItemCount >= totalItemCount) {// If scroll top
                                    binding.cardsFragmentRecyclerView.scrollToPosition(
                                        recyclerViewAdapter.cardList.lastIndex
                                    )
                                }
                            }
                        }
                    }
                    for ((idx, card) in recyclerViewAdapter.cardList.withIndex()) {
                        Log.d("KHJ", "$idx: ${card.message}")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                    toast("onChildChanged()")
                    snapshot.let { ss ->
                        //snapshot의 데이터를 Post의 객체로
                        val post = ss.getValue(Post::class.java)
                        post.let { newPost ->
                            val prevIndex = recyclerViewAdapter.cardList
                                .indexOfFirst { it.postId == previousChildName }
                            if (newPost != null) {
                                recyclerViewAdapter.cardList[prevIndex + 1] = newPost
                                recyclerViewAdapter.notifyItemChanged(prevIndex + 1)
                                recyclerViewAdapter.notifyDataSetChanged()
                            } else {
                                error("Error in onChildChanged!")
                            }
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
//                    toast("onChildRemoved()")
                    snapshot.let { ss ->
                        //snapshot의 데이터를 Post의 객체로 가져옴
                        val post = ss.getValue(Post::class.java)
                        post.let { newPost ->
                            val existIndex = recyclerViewAdapter.cardList
                                .indexOfFirst { it.postId == newPost?.postId }
                            if (existIndex != -1) {
                                recyclerViewAdapter.cardList.removeAt(existIndex)
                                recyclerViewAdapter.notifyItemRemoved(existIndex)
                                recyclerViewAdapter.notifyDataSetChanged()
                            } else {
                                recyclerViewAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//                    toast("onChildMoved()")
                    snapshot.let { ss ->
                        //snapshot의 데이터를 Post의 객체로
                        val post = ss.getValue(Post::class.java)
                        post.let { newPost ->
                            val existIndex = recyclerViewAdapter.cardList
                                .indexOfFirst { it.postId == newPost?.postId }
                            recyclerViewAdapter.cardList.removeAt(existIndex)
                            recyclerViewAdapter.notifyItemRemoved(existIndex)
                            recyclerViewAdapter.notifyDataSetChanged()
                            // prevChildName 이 없는 경우 맨 마지막으로 이동된 것
                            if (previousChildName == null) {
                                recyclerViewAdapter.cardList.add(newPost!!)
                                recyclerViewAdapter.notifyItemInserted(recyclerViewAdapter.cardList.lastIndex)
                                recyclerViewAdapter.notifyDataSetChanged()
                            } else {
                                // prevChildName 다음 글로 추가
                                val prevIndex =
                                    recyclerViewAdapter.cardList.indexOfFirst { it.postId == previousChildName }
                                if (prevIndex != -1) {
                                    recyclerViewAdapter.cardList.add(prevIndex + 1, newPost!!)
                                    recyclerViewAdapter.notifyItemInserted(prevIndex + 1)
                                    recyclerViewAdapter.notifyDataSetChanged()
                                } else {
                                    error("Error in onChildMoved!")

                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    toast("onCancelled()")
                    error.toException().printStackTrace()
                    error(error.toString())
                }
            })
    }
    // 근데, 여기서 또 Comment 리스너를 따로 달 필요가 있을까? 아니 없다.
    // Fragment에서는 commentCount만 필요한데, 이건 이미 PostChildEventListener 에서 실시간으로 처리되고 있기 때문.
    // Write 나 Remove가 됐다면, 그 Op을 실행한 클라이언트가 그 setValue 신호를 쐈기 때문에 결국 처리된다.
    // 명심하자. FBCrudOp 안에서는 또다른 FBCrudOp이 들어가선 안된다. Multi-user 때 duplicated 된 결과 초래하기 때문.
    // 그래서 Comment onAddChild 안에서 코멘트카운트 늘리는 거 했다가 duplicated 됐던 것이다.
    // 따로 함수 안에서, local 한 곳에서 처리할 것.

    // -> 정확히 말하면, 다른 클라우드 데이터와 관련된 조작을 또다른 클라우드 데이터 CRUD Op 에서 행하면 안되는 것이다.
    // DB 에서 직접 Post의 count를 줄이면 바로 반영된다. Post 의 CRUD에서는 Post의 클라우드 데이터만 처리하고
    // 다른 클라우드 데이터는 다루지 않기 때문이다. 이를 내 방식대로 "독립적"이라고 정의하겠다.
    // 하지만, 만약 Post가 지워질 때 관련된 코멘트와 좋아요를 없애려고 Post의 CRUD Op에 다른 클라우드 데이터인 Comment 와 Like
    // 를 remove 하는 Op 을 넣는다면? 이렇게 되면 다른 클라우드 데이터를 다루게 되므로 "비독립적"이게 되며, 곧
    // multi-user 환경에서는 Comment 와 Like를 한번만 지워야 할거를 N명의 사람이 있으면 N번 지우게 되는 것이다.

    // 하지만, DB 를 직접 지우거나 할때는 적용이 안된다. Comment 를 DB에서 직접 지워버리면,  setValue 이벤트가 작동을 안하기 때문.
    // 만약 여기서 Manager Service가 있어서 딱 단 하나만 존재하는, 서버급의 서비스가 존재해서 이 데이터 조작들을 총괄해준다면 어떨까?
    // 이 때는 단 하나의 리스너만 존재하므로 기존에 multi user 를 고려해서 CRUD안에 CRUD 를 넣지 못했던 걸 넣을 수 있다.


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        binding = FragmentCardsBinding.inflate(layoutInflater)
        binding.cardsFragmentRecyclerView.layoutManager = layoutManager
        binding.cardsFragmentRecyclerView.adapter = recyclerViewAdapter
        return binding.root
    }

    private fun saveInstanceState() {
    }

    override fun onAttach(context: Context) {
//        toast("onAttach()")
        super.onAttach(context)
    }

    override fun onStart() {
//        toast("onStart!")
        super.onStart()
    }

    override fun onResume() {
//        toast("onResume!")
        // Refresh View Data
        super.onResume()
        alreadyCreated = true
    }

    override fun onPause() {
//        toast("onPause()!")
        super.onPause()
    }

    override fun onStop() {
//        toast("onStop()!")
        saveInstanceState()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
//        toast("onSaveInstanceState()")
        Log.d("KHJ", "onSaveInstanceState()!")
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
//        toast("onDestroyView!")
        super.onDestroyView()
    }

    override fun onDestroy() {
//        toast("onDestroy!")
        super.onDestroy()
    }

    inner class CardRecyclerViewAdapter :

        RecyclerView.Adapter<CardRecyclerViewHolder>() {
        val cardList = mutableListOf<Post>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardRecyclerViewHolder {
            return CardRecyclerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_post_recycler, parent, false)
            )
        }

        override fun onBindViewHolder(holder: CardRecyclerViewHolder, position: Int) {
//            Log.d("KHJ","onBindViewHolder called!")
            val card = cardList[position]
            holder.bind(card, position)
        }

        override fun getItemCount() = cardList.size
    }

    inner class CardRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding = CardPostRecyclerBinding.bind(itemView)


        fun bind(card: Post, position: Int) {
            Log.d("KHJ", adapterPosition.toString())
            Glide.with(itemView.context)
                .load(Uri.parse(card.bgUri))
                .centerCrop()
                .into(binding.cardImageView)
            // if user already like this post
            FirebaseDatabase.getInstance().getReference("Likes/${card.postId}")
                .orderByChild("likerId").equalTo(getMyId())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
//                            toast("like already exist")
                            Log.d("KHJ", "like already exist")
                            Glide.with(itemView.context)
                                .load(R.drawable.lb_ic_thumb_up)
                                .into(binding.likeImageView)
                        } else {
//                            toast("like not exist")
                            Log.d("KHJ", "like not exist")
                            Glide.with(itemView.context)
                                .load(R.drawable.lb_ic_thumb_up_outline)
                                .into(binding.likeImageView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d("KHJ", "onCacelled in likerId")
                        error("onCacelled in likerId")
                    }
                })
            binding.contentTextView.text = card.message
            binding.commentCountTextView.text = card.commentCount.toString()
            binding.likeCountTextView.text = card.likeCount.toString()
            // 헌데, 특정 포스트가 삭제되면, 그와 관련된 Interval Observable도 dispose 해줘야 하는데...
            // 그건 구현을 어떻게 하지? 그 Observable reference 를 어떻게 구하지?
            // 이렇게 있으면 또 recyclerView에서 holder가 없어져도 Observable을 갖고있어서 GC되지 않을텐데..
            // # Solved with bindUntilEvent() -> Wow!!! RxLifeCycle is Amazing!!
            binding.timeTextView.text = formatTimeString(card.writeTime as Long)
            Observable.interval(60L, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .bindUntilEvent(this@CardsFragment, FragmentEvent.DESTROY)
                .subscribe {
                    binding.timeTextView.text = formatTimeString(card.writeTime as Long)
                }
            binding.cardImageView.setOnClickListener {
                clickCardSubject.onNext(card)
            }
//            registerForContextMenu(binding.cardMoreMenuImageView)
            // 1. 일단 registerForContextMenu 파라미터로 리스트뷰나 그리드뷰를 넣으면 작동하지만,
            // 리사이클러뷰는 View.ViewGroup을 상속받기에 작동하지 않더라.. 그래서 바인드 뷰홀더에 직접 해야한다.
            // 2.
            // 기존 Reference에 존재하는 registerForContextMenu를 활용하면 일일이 onLongClickListener
            // 로 하지 않아도 자동으로 길게 누르면 context menu가 뜨도록 해준다. 하지만 이렇게 하니까
            // same element에 대해서 click Listener가 존재하면 클릭 리스너만 반응을 하는 문제가 있어서
            // 긴 삽질을 통해 유튜브에서 인도 프로그래머의 영상을 봤다. 이하 그의 영상내용.
            // 기존에는 onCreateContextMenu 와 onContextItemSelected를 Fragment에다가 선언했는데,
            // 이 방식은 register 로 일반적으로 하는 방식이나, 나는 register 함수를 못쓰기에,
            // 직접 뷰홀더 클래스에 View.OnCreateContextMenuListener를 구현해줘서 뷰 홀더 클래스 내에서
            // onCreateContext 함수를 오버라이딩 해서 써줬다.

            // -> 생각해보니까 오버라이딩 안하고 그냥 objet :  로 구현하면 될 것 같다.
            // 그리고 기존에 클릭이벤트와 겹쳤던 거에다가 setOnCreateContextMenuListener를 걸어주면 끝.
            //  메뉴도 직접 지정할 수 있더라. 그래서 이제 카드의 정보를 onCreateContextMenu 로 넘겨주고
            // 내가 이 카드의 작성자인지 아닌지를 판별할 수 있는지 찾아봐야한다.

            // override 에서 처리하는 게 아니라, bind 함수 내에서 바로 구현하면서 position 을 이용했다.

            binding.cardImageView.setOnCreateContextMenuListener { menu, v, menuInfo ->
                val card = recyclerViewAdapter.cardList[position]
                menuCard = card
                val inflater = MenuInflater(activity)
                inflater.inflate(R.menu.card_floating_menu, menu)
                //                    menu.setHeaderTitle("메뉴")
                if (card.writerId == getMyId()) {
                    menu.removeItem(R.id.menu_item_report)
                } else {
                    menu.removeItem(R.id.menu_item_remove)
                    menu.removeItem(R.id.menu_item_modify)
                }
            }
            binding.likeImageView.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .bindUntilEvent(this@CardsFragment, FragmentEvent.DESTROY)
                .debounce(300L, TimeUnit.MILLISECONDS)
                .subscribe {
                    val likeRef =
                        FirebaseDatabase.getInstance().getReference("Likes/${card.postId}")
                    // a 가 Unit 이다... ValueEventListener 를 반환해야 remove가능한데...
                    likeRef.orderByChild("likerId").equalTo(getMyId())
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                Log.d("KHJ", "onDataChange in likeClick")
                                if (snapshot.exists()) {
//                                    toast("newLike false")
                                    Log.d("KHJ", "newLike false")
                                    Glide.with(this@CardsFragment)
                                        .load(R.drawable.lb_ic_thumb_up_outline)
                                        .into(binding.likeImageView)
                                    for (ch in snapshot.children) {
                                        if (ch.child("likerId").value == getMyId()) {
                                            val removeLikeRef =
                                                likeRef.child(ch.getValue(Like::class.java)!!.likeId)
                                            removeLikeRef.removeValue()
                                        }
                                    }
                                    val postRef = FirebaseDatabase.getInstance()
                                        .getReference("Posts/${card.postId}")
                                    postRef.runTransaction(object : Transaction.Handler {
                                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                                            val p = currentData.getValue(Post::class.java)
                                                ?: return Transaction.success(currentData)
                                            p.likeCount--
                                            currentData.value = p
                                            return Transaction.success(currentData)
                                        }

                                        override fun onComplete(
                                            error: DatabaseError?,
                                            committed: Boolean,
                                            currentData: DataSnapshot?
                                        ) {
                                            Log.d(
                                                "KHJ",
                                                "postLikeTransaction:onComplete(), $error"
                                            )
                                        }
                                    })
                                } else {
//                                    toast("newLike true")
                                    Log.d("KHJ", "newLike true")
                                    Glide.with(this@CardsFragment)
                                        .load(R.drawable.lb_ic_thumb_up)
                                        .into(binding.likeImageView)
                                    val newRef = FirebaseDatabase.getInstance()
                                        .getReference("Likes/${card.postId}").push()
                                    val like = Like()
                                    like.likeId = newRef.key.toString()
                                    like.likerId = getMyId()
                                    like.postId = card.postId
                                    like.likeTime = ServerValue.TIMESTAMP
                                    newRef.setValue(like)
                                    val postRef = FirebaseDatabase.getInstance()
                                        .getReference("Posts/${card.postId}")
                                    postRef.runTransaction(object : Transaction.Handler {
                                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                                            val p = currentData.getValue(Post::class.java)
                                                ?: return Transaction.success(currentData)
                                            p.likeCount++
                                            currentData.value = p
                                            return Transaction.success(currentData)
                                        }

                                        override fun onComplete(
                                            error: DatabaseError?,
                                            committed: Boolean,
                                            currentData: DataSnapshot?
                                        ) {
                                            Log.d(
                                                "KHJ",
                                                "postLikeTransaction:onComplete(), $error"
                                            )
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.d("KHJ", "onCacelled in likerId")
                                error("onCacelled in likerId")
                            }
                        })
                }
        }
    }

    fun formatTimeString(regTime: Long): String? {
        val SEC = 60
        val MIN = 60
        val HOUR = 24
        val DAY = 30
        val MONTH = 12
        val curTime = System.currentTimeMillis()
        var diffTime = (curTime - regTime) / 1000
        var msg: String? = null
        if (diffTime < SEC) {
            msg = "방금 전"
        } else if (SEC.let { diffTime /= it; diffTime } < MIN) {
            msg = diffTime.toString() + "분 전"
        } else if (MIN.let { diffTime /= it; diffTime } < HOUR) {
            msg = diffTime.toString() + "시간 전"
        } else if (HOUR.let { diffTime /= it; diffTime } < DAY) {
            msg = diffTime.toString() + "일 전"
        } else {
            val sdf = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm")
            msg = sdf.format(regTime)
        }
        return msg
    }

    @SuppressLint("HardwareIds")
    fun getMyId(): String { // Return Device ID
        return Settings.Secure.getString(activity?.contentResolver, Settings.Secure.ANDROID_ID)
    }
}