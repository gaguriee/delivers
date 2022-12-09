package com.kgg.android.delivers.chatActivity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.R
import com.kgg.android.delivers.data.ChatRoom
import com.kgg.android.delivers.data.Message
import com.kgg.android.delivers.databinding.FragmentChatBinding
import com.kgg.android.delivers.databinding.ItemChatBinding
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.ArrayList

//채팅방 목록
class ChatFragment : Fragment() {
    companion object {
        fun newInstance(): ChatFragment {
            return ChatFragment()
        }

    }
    private lateinit var auth: FirebaseAuth
    private val fireDatabase = FirebaseDatabase.getInstance().reference
    private val fireStore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference

    //메모리에 올라갔을 때
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    //프래그먼트를 포함하고 있는 액티비티에 붙었을때
    override fun onAttach(context: Context){
        super.onAttach(context)
    }
    //뷰가 생성되었을때 프래그먼트와 레이아웃 연결시켜주는 부분
    override fun onCreateView(
          inflater: LayoutInflater, container: ViewGroup?,
          savedInstanceState: Bundle?
    ): View? {


        // Inflate the layout for this fragment
        val binding = FragmentChatBinding.inflate(inflater, container, false)
        val cAdapter = binding.chatRecycler
        cAdapter.layoutManager = LinearLayoutManager(requireContext())
        cAdapter.adapter = RecyclerViewAdapter()




        return binding.root
    }

    inner class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.CustomViewHolder>() {
        private val chatRooms = arrayListOf<ChatRoom>() //해당 유저 채팅방 목록
        private val chatRoomKeys: ArrayList<String> = arrayListOf() //채팅방 키 목록
        private var myUid : String? = null //접속한 유저id
        private val destinationUsers: ArrayList<String> = arrayListOf() //상대방 uid를 담기 위한 목록


        init {
            //접속한 userid
            auth = FirebaseAuth.getInstance()
            myUid = auth.currentUser?.uid.toString()!!
            Log.d("Chatting", myUid!!.toString())

            try {
                //자신이 포함된 채팅방의 uid를 모두 가져옴.
                fireDatabase.child("chatrooms")
                    .orderByChild("users/${myUid}")
                    .equalTo(true) //realtime db에서 myUid의 값이 true인걸 가져옴
                    .addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(error: DatabaseError) {
                            Log.d("Chatting", "Fail to read data.")
                        }

                        override fun onDataChange(snapshot: DataSnapshot) {
//                    chatRooms.clear()
                            var count = 0
                            for (data in snapshot.children) {
                                val item = data.getValue<ChatRoom>()
                                chatRooms.add(item!!)
                                chatRoomKeys.add(data.key!!) //해당 채팅방의 키를 담음
                                count += 1
                                Log.d("chatting", "item : $item")
                                Log.d("chatting", "chatRoomKey : ${data.key}")
                            }
                            Log.d("chatting", "item 개수 : $count")
                            notifyDataSetChanged() //데이터가 변경되었음을 알림
                        }
                    })
            }catch(e:Exception){
                e.printStackTrace()
            }

        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }

        inner class CustomViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
                val imageView: ImageView = binding.foodIcon //해당 게시물 사진
                val textView: TextView = binding.userNickName //유저 닉네임
                val textViewLastMessage: TextView = binding.LastMessage //마지막 메시지
        }

        //채팅 리스트에서 선택 시 채팅창으로 이동하기 위해 상대방의 uid를 destinationUsers에 저장해줌.
        override fun onBindViewHolder(holder: CustomViewHolder, @SuppressLint("RecyclerView") position: Int) {
            var destinationUid: String? = null

            //채팅방에 있는 유저 모두 체크
            for (user in chatRooms[chatRooms.size - position -1].users?.keys!!) {
                if (user != myUid) {//본인 제외
                    destinationUid = user
                    destinationUsers.add(destinationUid) //상대방의 uid를 destinationUsers에 저장
                    Log.d("chatting","destinationUid : $destinationUid")
                }
            }

            //채팅방 목록에 띄울 상대방의 닉네임을 위해 정보를 받아와 담아주면서 연결
            val docCol = fireStore.collection("users") //작업할 컬렉션
            docCol
                .whereEqualTo("uid","$destinationUid") //uid가 destinationUid와 일치하는 문서 가져오기
                .get()
                .addOnSuccessListener { result ->
                    for(document in result){
                        var nickname = document["nickname"] as String
                        holder.textView.text = nickname
                        Log.d("chatting","nickname: $nickname" )
                    }

                }
                .addOnFailureListener{exception ->
                    Log.d("ChatFragment", "Error getting documents: $exception")
                }


            //스토리 사진 프로필로 띄우기
            val storyDocCol = fireStore.collection("story")
            storyDocCol
                .whereEqualTo("postId", chatRooms[chatRooms.size - position -1].postId)
                .get()
                .addOnSuccessListener { result ->
                    for(document in result){
                        var resourceId = document["photo"] as String
                        if (resourceId != null) {
                            storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Glide.with(this@ChatFragment)
                                        .load(task.result)
                                        .into(holder.imageView!!)

                                } else {
                                    var category = document["category"] as String
                                    var id = 0
                                    when(category)
                                    {
                                        "chicken" -> id =R.drawable.chicken //치킨
                                        "hamburger"-> id =R.drawable.hamburger //버거
                                        "pizza" -> id =R.drawable.pizza //피자
                                        "coffee"->id =R.drawable.coffee //카페디저트
                                        "bread"-> id =R.drawable.bread //샌드위치
                                        "meat"-> id =R.drawable.meat //고기
                                        "salad"-> id =R.drawable.salad //샐러드
                                        "sushi"-> id =R.drawable.sushi //회초밥
                                        "guitar"-> id =R.drawable.guitar //기타
                                    }
                                    holder.imageView!!.setImageResource(id)
                                }
                            }
                        }
                    }
                }


            //메세지 내림차순 정렬 후 마지막 메시지의 키 값을 가짐
            try {
                val messageMap = TreeMap<String, Message>(reverseOrder()) //TreeMap을 역순으로 선언
                messageMap.putAll(chatRooms[chatRooms.size - position -1].messages) //chatRoom의 messages를 모두 넣어줌
                val lastMessageKey =
                    messageMap.keys.toTypedArray()[0] //toTypedArray()배열로 변환한 뒤 첫번째 값을 lastMessageKey에 넣어줌
                holder.textViewLastMessage.text = chatRooms[chatRooms.size - position -1].messages[lastMessageKey]?.message
            }catch(e: IndexOutOfBoundsException){
                Log.d("Chatting","${e.printStackTrace()}")
            }



            //채팅 리스트에서 선택 시 이동
            holder.itemView.setOnClickListener {
                try {
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("destinationUid", destinationUsers[chatRooms.size - position -1]) //상대방의 id를 넘겨줌
                    Log.d("Chatting","destinationUid: ${destinationUsers[chatRooms.size - position -1]}")
                    intent.putExtra("ChatRoomId", chatRoomKeys[chatRooms.size - position -1]) //채팅방 키 정보 넘겨줌
                    Log.d("Chatting","ChatRoomId: ${chatRoomKeys[chatRooms.size - position -1]}")
                    intent.putExtra("postId", chatRooms[chatRooms.size - position -1].postId) //채팅방 포스트 id넘겨줌

                    context?.startActivity(intent)
                    (context as AppCompatActivity).finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "채팅방 이동 중 문제가 발생하였습니다.", Toast.LENGTH_SHORT).show()
                } //에러 처리
            }
        }
        override fun getItemCount(): Int {
            return chatRooms.size
        }
    }
}


