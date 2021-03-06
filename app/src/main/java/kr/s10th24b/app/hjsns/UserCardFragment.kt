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
import kr.s10th24b.app.hjsns.databinding.FragmentUserCardBinding
import splitties.toast.toast
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class UserCardFragment(val showType: String) : RxFragment(),
    MyAlertDialogFragment.MyAlertDialogListener {
    lateinit var binding: FragmentUserCardBinding
    lateinit var recyclerViewAdapter: UserCardRecyclerViewAdapter
    lateinit var layoutManager: LinearLayoutManager
    var clickCardSubject = PublishSubject.create<Post>()
    lateinit var menuCard: Post

    lateinit var myCommentCardRef: DatabaseReference
    lateinit var myLikeCardRef: DatabaseReference
    lateinit var childListener: ChildEventListener
    lateinit var childListenerPairsList: MutableList<Pair<Query, ChildEventListener>>

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

        setFirebaseDatabasePostListener(showType)
        setClickCardSubject()
        childListenerPairsList = mutableListOf()
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
                val removeAlertDialog = MyAlertDialogFragment("Fragment", "?????? ????????? ?????????????????????????")
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
            .addOnSuccessListener(requireActivity()) { toast("?????? ?????? ??????") }
            .addOnCanceledListener(requireActivity()) { toast("?????? ?????? ??????") }
            .addOnFailureListener(requireActivity()) { toast("?????? ?????? ??????") }
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

    private fun setFirebaseDatabasePostListener(type: String) {
        recyclerViewAdapter = UserCardRecyclerViewAdapter()
        // FireBase Data pulling and save it to posts variable
        val dbRef = FirebaseDatabase.getInstance()
        myCommentCardRef = dbRef.getReference("/Comments")
        myLikeCardRef = dbRef.getReference("/Likes")
        val ref = when (type) {
            "myCommentCard" -> myCommentCardRef
            "myLikeCard" -> myLikeCardRef
            else -> myCommentCardRef
        }

        val allRef = FirebaseDatabase.getInstance().getReference("/Posts")
//        valueListener = ref.addValueEventListener(object : ValueEventListener {
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.let { ss ->
                        Log.d("KHJ", "childrenCount: ${ss.childrenCount}")
                        Log.d("KHJ", "onDataChanged in valueListener!")
                        for (c in ss.children) {
                            c?.let { tc ->
                                Log.d("KHJ", "key: ${tc.key}")
                                val existenceQuery = when (type) {
                                    "myCommentCard" -> ref.child(tc.key.toString())
                                        .orderByChild("writerId")
                                        .equalTo(CurrentUser.getInstance().userId)
                                    "myLikeCard" -> ref.child(tc.key.toString())
                                        .orderByChild("likerId")
                                        .equalTo(CurrentUser.getInstance().userId)
                                    else -> ref.child(tc.key.toString())
                                        .orderByChild("writerId")
                                        .equalTo(CurrentUser.getInstance().userId)
                                }
                                existenceQuery.addListenerForSingleValueEvent(object :
                                    ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val query =
                                                allRef.orderByChild("postId").equalTo(tc.key)
                                            // ????????? ?????? key ????????? ??????????????? ?????? ???????????? ??? ?????????...
                                            // ??????????????? ???????????? ????????? ?????? ??? ??????????????? ???????????? ?????????.
                                            childListener =
                                                query.addChildEventListener(object :
                                                    ChildEventListener {
                                                    override fun onChildAdded(
                                                        snapshot: DataSnapshot,
                                                        previousChildName: String?
                                                    ) {
//                    toast("onChildAdded()")
                                                        Log.d("KHJ", "onChildAdded!")
                                                        if (snapshot.exists()) {
                                                            snapshot.let { ss ->
                                                                val post =
                                                                    ss.getValue(Post::class.java)
                                                                post?.let { newPost ->
                                                                    // ??? ?????? ????????? ????????? ????????? ??????
                                                                    if (previousChildName == null) {
//                                toast("??? ?????? ?????????????????? ??????")
                                                                        recyclerViewAdapter.cardList.add(
                                                                            newPost
                                                                        )
                                                                        recyclerViewAdapter.notifyItemInserted(
                                                                            0
                                                                        )
                                                                        recyclerViewAdapter.notifyDataSetChanged()
//                                recyclerViewAdapter.notifyDataSetChanged()
                                                                    } else { // ?????? ????????? ????????? ??????... ?????? ?????? ??? ????????? ????????????. ????????? ???????????? ??????????????? ?????????.
//                                toast("??? ?????? ????????? ??????")
//                                val prevIndex = recyclerViewAdapter.cardList
//                                    .indexOfFirst { it.postId == previousChildName }
//                                    recyclerViewAdapter.cardList.add(prevIndex + 1, newPost)
                                                                        recyclerViewAdapter.cardList.add(
                                                                            newPost
                                                                        )
                                                                        recyclerViewAdapter.notifyItemInserted(
                                                                            recyclerViewAdapter.cardList.lastIndex
                                                                        )
                                                                        recyclerViewAdapter.notifyDataSetChanged()
                                                                        val firstCompVisPos =
                                                                            layoutManager.findFirstCompletelyVisibleItemPosition()
//                                val visibleItemCount = binding.cardsFragmentRecyclerView.childCount
                                                                        val visibleItemCount =
                                                                            layoutManager.childCount
                                                                        val totalItemCount =
                                                                            layoutManager.itemCount
//                                toast("firstCompVis: $firstCompVisPos, totalItemCount: $totalItemCount visibleItemCount: $visibleItemCount")
                                                                        if (firstCompVisPos + visibleItemCount >= totalItemCount) {// If scroll top
                                                                            binding.userCardsFragmentRecyclerView.scrollToPosition(
                                                                                recyclerViewAdapter.cardList.lastIndex
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        for ((idx, card) in recyclerViewAdapter.cardList.withIndex()) {
                                                            Log.d("KHJ", "$idx: ${card.message}")
                                                        }
                                                    }

                                                    override fun onChildChanged(
                                                        snapshot: DataSnapshot,
                                                        previousChildName: String?
                                                    ) {
//                    toast("onChildChanged()")
                                                        Log.d("KHJ", "onChildChanged()")
                                                        snapshot.let { ss ->
                                                            //snapshot??? ???????????? Post??? ?????????
                                                            val post = ss.getValue(Post::class.java)
                                                            // ??? Query??? ????????? postId ?????????, ?????? ????????????.
                                                            // ??????, previousChildName ??? ?????? null ???????????? ??????.
                                                            // ??? Query ?????? ????????? child??? ?????? ????????????. (??????)
                                                            post?.let { newPost ->
                                                                val index =
                                                                    recyclerViewAdapter.cardList
                                                                        .indexOfFirst { it.postId == newPost.postId }
                                                                Log.d(
                                                                    "KHJ",
                                                                    "previousChildName: $previousChildName"
                                                                )
                                                                recyclerViewAdapter.cardList[index] =
                                                                    newPost
                                                                recyclerViewAdapter.notifyItemChanged(
                                                                    index
                                                                )
                                                                recyclerViewAdapter.notifyDataSetChanged()
                                                            }
                                                        }
                                                    }

                                                    override fun onChildRemoved(snapshot: DataSnapshot) {
//                    toast("onChildRemoved()")
                                                        Log.d("KHJ", "onChildRemoved()")
                                                        snapshot.let { ss ->
                                                            //snapshot??? ???????????? Post??? ????????? ?????????
                                                            val post = ss.getValue(Post::class.java)
                                                            post.let { newPost ->
                                                                val existIndex =
                                                                    recyclerViewAdapter.cardList
                                                                        .indexOfFirst { it.postId == newPost?.postId }
                                                                if (existIndex != -1) {
                                                                    recyclerViewAdapter.cardList.removeAt(
                                                                        existIndex
                                                                    )
                                                                    recyclerViewAdapter.notifyItemRemoved(
                                                                        existIndex
                                                                    )
                                                                    recyclerViewAdapter.notifyDataSetChanged()
                                                                } else {
                                                                    recyclerViewAdapter.notifyDataSetChanged()
                                                                }
                                                            }
                                                        }
                                                    }

                                                    override fun onChildMoved(
                                                        snapshot: DataSnapshot,
                                                        previousChildName: String?
                                                    ) {
//                    toast("onChildMoved()")
                                                        Log.d("KHJ", "onChildMoved()")
                                                        snapshot.let { ss ->
                                                            //snapshot??? ???????????? Post??? ?????????
                                                            val post = ss.getValue(Post::class.java)
                                                            post.let { newPost ->
                                                                val existIndex =
                                                                    recyclerViewAdapter.cardList
                                                                        .indexOfFirst { it.postId == newPost?.postId }
                                                                recyclerViewAdapter.cardList.removeAt(
                                                                    existIndex
                                                                )
                                                                recyclerViewAdapter.notifyItemRemoved(
                                                                    existIndex
                                                                )
                                                                recyclerViewAdapter.notifyDataSetChanged()
                                                                // prevChildName ??? ?????? ?????? ??? ??????????????? ????????? ???
                                                                if (previousChildName == null) {
                                                                    recyclerViewAdapter.cardList.add(
                                                                        newPost!!
                                                                    )
                                                                    recyclerViewAdapter.notifyItemInserted(
                                                                        recyclerViewAdapter.cardList.lastIndex
                                                                    )
                                                                    recyclerViewAdapter.notifyDataSetChanged()
                                                                } else {
                                                                    // prevChildName ?????? ?????? ??????
                                                                    val prevIndex =
                                                                        recyclerViewAdapter.cardList.indexOfFirst { it.postId == previousChildName }
                                                                    if (prevIndex != -1) {
                                                                        recyclerViewAdapter.cardList.add(
                                                                            prevIndex + 1,
                                                                            newPost!!
                                                                        )
                                                                        recyclerViewAdapter.notifyItemInserted(
                                                                            prevIndex + 1
                                                                        )
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
                                            childListenerPairsList.add(Pair(query, childListener))
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                    }
                                })
                            }
                        }
                    }
                } else {
                    Log.d("KHJ", "snapshot not exist!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
// ??????, ????????? ??? Comment ???????????? ?????? ??? ????????? ?????????? ?????? ??????.
// Fragment????????? commentCount??? ????????????, ?????? ?????? PostChildEventListener ?????? ??????????????? ???????????? ?????? ??????.
// Write ??? Remove??? ?????????, ??? Op??? ????????? ?????????????????? ??? setValue ????????? ?????? ????????? ?????? ????????????.
// ????????????. FBCrudOp ???????????? ????????? FBCrudOp??? ???????????? ?????????. Multi-user ??? duplicated ??? ?????? ???????????? ??????.
// ????????? Comment onAddChild ????????? ?????????????????? ????????? ??? ????????? duplicated ?????? ?????????.
// ?????? ?????? ?????????, local ??? ????????? ????????? ???.

// -> ????????? ?????????, ?????? ???????????? ???????????? ????????? ????????? ????????? ???????????? ????????? CRUD Op ?????? ????????? ????????? ?????????.
// DB ?????? ?????? Post??? count??? ????????? ?????? ????????????. Post ??? CRUD????????? Post??? ???????????? ???????????? ????????????
// ?????? ???????????? ???????????? ????????? ?????? ????????????. ?????? ??? ???????????? "?????????"????????? ???????????????.
// ?????????, ?????? Post??? ????????? ??? ????????? ???????????? ???????????? ???????????? Post??? CRUD Op??? ?????? ???????????? ???????????? Comment ??? Like
// ??? remove ?????? Op ??? ????????????? ????????? ?????? ?????? ???????????? ???????????? ????????? ????????? "????????????"?????? ??????, ???
// multi-user ??????????????? Comment ??? Like??? ????????? ????????? ????????? N?????? ????????? ????????? N??? ????????? ?????? ?????????.

// ?????????, DB ??? ?????? ???????????? ????????? ????????? ?????????. Comment ??? DB?????? ?????? ???????????????,  setValue ???????????? ????????? ????????? ??????.
// ?????? ????????? Manager Service??? ????????? ??? ??? ????????? ????????????, ???????????? ???????????? ???????????? ??? ????????? ???????????? ?????????????????? ??????????
// ??? ?????? ??? ????????? ???????????? ??????????????? ????????? multi user ??? ???????????? CRUD?????? CRUD ??? ?????? ????????? ??? ?????? ??? ??????.


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        binding = FragmentUserCardBinding.inflate(layoutInflater)
        binding.userCardsFragmentRecyclerView.layoutManager = layoutManager
        binding.userCardsFragmentRecyclerView.adapter = recyclerViewAdapter
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
//        Log.d("KHJ", "onSaveInstanceState()!")
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
//        toast("onDestroyView!")
        super.onDestroyView()
    }

    override fun onDestroy() {
//        toast("onDestroy!")
        super.onDestroy()
//        when (showType) {
//            "myCommentCard" -> myCommentCardRef.removeEventListener(valueListener)
//            "myLikeCard" -> myLikeCardRef.removeEventListener(valueListener)
//        }
        for (pair in childListenerPairsList) {
            pair.first.removeEventListener(pair.second)
        }
    }

    inner class UserCardRecyclerViewAdapter :

        RecyclerView.Adapter<UserCardRecyclerViewHolder>() {
        private val mCompositeDisposable: CompositeDisposable = CompositeDisposable()
        val cardList = mutableListOf<Post>()
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): UserCardRecyclerViewHolder {
            return UserCardRecyclerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_post_recycler, parent, false)
            )
        }

        override fun onBindViewHolder(holder: UserCardRecyclerViewHolder, position: Int) {
//            Log.d("KHJ","onBindViewHolder called!")
            val card = cardList[position]
            holder.bind(card, position)
            CardPostRecyclerBinding.bind(holder.itemView).timeTextView.text =
                formatTimeString(card.writeTime as Long)
            mCompositeDisposable.add(
                Observable.interval(30L, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        CardPostRecyclerBinding.bind(holder.itemView).timeTextView.text =
                            formatTimeString(card.writeTime as Long)
                    })
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)
            mCompositeDisposable.dispose()
        }

        override fun getItemCount() = cardList.size
    }

    inner class UserCardRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding = CardPostRecyclerBinding.bind(itemView)


        fun bind(card: Post, position: Int) {
            Log.d("KHJ", "bind called!")
            Log.d("KHJ", "adapterPosition: ${adapterPosition.toString()}")
            Glide.with(itemView.context)
                .load(Uri.parse(card.bgUri))
                .centerCrop()
                .into(binding.cardImageView)
            // if user already like this post
            FirebaseDatabase.getInstance().getReference("Likes/${card.postId}")
                .orderByChild("likerId").equalTo(CurrentUser.getInstance().userId)
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
            // ??????, ?????? ???????????? ????????????, ?????? ????????? Interval Observable??? dispose ????????? ?????????...
            // ?????? ????????? ????????? ??????? ??? Observable reference ??? ????????? ??????????
            // ????????? ????????? ??? recyclerView?????? holder??? ???????????? Observable??? ??????????????? GC?????? ????????????..
            // # Solved with bindUntilEvent() -> Wow!!! RxLifeCycle is Amazing!!
            binding.cardImageView.setOnClickListener {
                clickCardSubject.onNext(card)
            }
//            registerForContextMenu(binding.cardMoreMenuImageView)
            // 1. ?????? registerForContextMenu ??????????????? ??????????????? ??????????????? ????????? ???????????????,
            // ????????????????????? View.ViewGroup??? ??????????????? ???????????? ?????????.. ????????? ????????? ???????????? ?????? ????????????.
            // 2.
            // ?????? Reference??? ???????????? registerForContextMenu??? ???????????? ????????? onLongClickListener
            // ??? ?????? ????????? ???????????? ?????? ????????? context menu??? ????????? ?????????. ????????? ????????? ?????????
            // same element??? ????????? click Listener??? ???????????? ?????? ???????????? ????????? ?????? ????????? ?????????
            // ??? ????????? ?????? ??????????????? ?????? ?????????????????? ????????? ??????. ?????? ?????? ????????????.
            // ???????????? onCreateContextMenu ??? onContextItemSelected??? Fragment????????? ???????????????,
            // ??? ????????? register ??? ??????????????? ?????? ????????????, ?????? register ????????? ????????????,
            // ?????? ????????? ???????????? View.OnCreateContextMenuListener??? ??????????????? ??? ?????? ????????? ?????????
            // onCreateContext ????????? ??????????????? ?????? ?????????.

            // -> ?????????????????? ??????????????? ????????? ?????? objet :  ??? ???????????? ??? ??? ??????.
            // ????????? ????????? ?????????????????? ????????? ???????????? setOnCreateContextMenuListener??? ???????????? ???.
            //  ????????? ?????? ????????? ??? ?????????. ????????? ?????? ????????? ????????? onCreateContextMenu ??? ????????????
            // ?????? ??? ????????? ??????????????? ???????????? ????????? ??? ????????? ??????????????????.

            // override ?????? ???????????? ??? ?????????, bind ?????? ????????? ?????? ??????????????? position ??? ????????????.

            binding.cardImageView.setOnCreateContextMenuListener { menu, v, menuInfo ->
                val card = recyclerViewAdapter.cardList[position]
                menuCard = card
                val inflater = MenuInflater(activity)
                inflater.inflate(R.menu.card_floating_menu, menu)
                //                    menu.setHeaderTitle("??????")
                if (card.writerId == CurrentUser.getInstance().userId) {
                    menu.removeItem(R.id.menu_item_report)
                } else {
                    menu.removeItem(R.id.menu_item_remove)
                    menu.removeItem(R.id.menu_item_modify)
                }
            }
            binding.likeImageView.clicks()
                .observeOn(AndroidSchedulers.mainThread())
                .bindUntilEvent(this@UserCardFragment, FragmentEvent.DESTROY)
                .debounce(300L, TimeUnit.MILLISECONDS)
                .subscribe {
                    val likeRef =
                        FirebaseDatabase.getInstance().getReference("Likes/${card.postId}")
                    // a ??? Unit ??????... ValueEventListener ??? ???????????? remove????????????...
                    likeRef.orderByChild("likerId").equalTo(CurrentUser.getInstance().userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                Log.d("KHJ", "onDataChange in likeClick")
                                if (snapshot.exists()) {
//                                    toast("newLike false")
                                    Log.d("KHJ", "newLike false")
                                    Glide.with(this@UserCardFragment)
                                        .load(R.drawable.lb_ic_thumb_up_outline)
                                        .into(binding.likeImageView)
                                    for (ch in snapshot.children) {
                                        if (ch.child("likerId").value == CurrentUser.getInstance().userId) {
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
                                    Glide.with(this@UserCardFragment)
                                        .load(R.drawable.lb_ic_thumb_up)
                                        .into(binding.likeImageView)
                                    val newRef = FirebaseDatabase.getInstance()
                                        .getReference("Likes/${card.postId}").push()
                                    val like = Like()
                                    like.likeId = newRef.key.toString()
                                    like.likerId = CurrentUser.getInstance().userId
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
            msg = "?????? ???"
        } else if (SEC.let { diffTime /= it; diffTime } < MIN) {
            msg = diffTime.toString() + "??? ???"
        } else if (MIN.let { diffTime /= it; diffTime } < HOUR) {
            msg = diffTime.toString() + "?????? ???"
        } else if (HOUR.let { diffTime /= it; diffTime } < DAY) {
            msg = diffTime.toString() + "??? ???"
        } else {
            val sdf = SimpleDateFormat("yyyy??? MM??? dd??? HH:mm")
            msg = sdf.format(regTime)
        }
        return msg
    }
}
