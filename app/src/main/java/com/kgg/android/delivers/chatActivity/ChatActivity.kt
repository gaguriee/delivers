package com.kgg.android.delivers.chatActivity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.MainActivity
import com.kgg.android.delivers.databinding.ActivityChatBinding
import com.kgg.android.delivers.databinding.ItemMineMessageBinding
import com.kgg.android.delivers.databinding.ItemOtherMessageBinding
import java.text.SimpleDateFormat
import java.util.*



class ChatActivity : AppCompatActivity() {
    val binding by lazy { ActivityChatBinding.inflate(layoutInflater) }
    private val storage: FirebaseStorage =
        FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference
    val firestore = FirebaseFirestore.getInstance()
//
    lateinit var btn_quit: ImageButton
    lateinit var btn_send: Button
    lateinit var chat_title: TextView
    lateinit var edt_message: EditText
    lateinit var recyclerView: RecyclerView
    lateinit var chatRoom: ChatRoom
    lateinit var destinationUid: String
    lateinit var chatRoomUid: String
    lateinit var myUid: String
    private val fireDatabase = FirebaseDatabase.getInstance().reference
    private val fireStore = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initializeProperty()

        btn_quit = binding.imgbtnQuit
        edt_message = binding.edtMessage
        recyclerView = binding.recyclerMessages
        btn_send = binding.btnSubmit
        chat_title = binding.txtTItle



        initializeListener()
        //상대방 닉네임 연결 //이때 어느 디비를 참조해야하는가?
        val docCol = fireStore.collection("users") //작업할 컬렉션
        docCol
            .whereEqualTo("uid","$destinationUid") //uid가 destinationUid와 일치하는 문서 가져오기
            .get()
            .addOnSuccessListener { result ->
                for(document in result){
                    var nickname = document["nickname"] as String
                    chat_title.text = nickname
                    Log.d("chatting","nickname: $nickname" )
                    //채팅방 제목을 상대방 닉네임으로 설정
                }

            }
            .addOnFailureListener{exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }




        if (chatRoomUid.isNullOrBlank()) //채팅방 키가 없으면 생성
            setupChatRoomId()
        else //채팅방 키가 있으면 채팅 메세지 목록 보여주기
            setupRecycler()
//        recyclerView = binding.recyclerMessages

//        btn_send.setOnClickListener {
//            Log.d("dest : ", "$destinationUid")
//            val chatRoom = ChatRoom()
//            chatRoom.users.put(myUid.toString(), true)
//            chatRoom.users.put(destinationUid!!, true)
//
//
//            //메세지 저장
//            val message = Message(myUid, edt_message.text.toString(), currentTime)
//            if (chatRoomUid == null) {
//                btn_send.isEnabled = false
//                fireDatabase.child("chatrooms").push().setValue(chatRoom)
//                    .addOnSuccessListener {
//                        //create chatroom
//                        checkChatRoom()
//                        //메세지 보내기
//                        Handler().postDelayed({
//                            Log.d("chatRoomUid : ", "$chatRoomUid")
//                            fireDatabase.child("chatrooms").child(chatRoomUid.toString())
//                                .child("messages").push().setValue(message)
//                            binding.edtMessage.text = null
//
//                        }, 1000L)
//                        Log.d("chatUidNull dest", "$destinationUid")
//                    }
//            } else {
//                fireDatabase.child("chatrooms").child(chatRoomUid.toString())
//                    .child("messages").push().setValue(message)
//                binding.edtMessage.text = null
//                Log.d("chatUidNotNull dest", "$destinationUid")
//
//            }
//
//        }
//        checkChatRoom()
//
//
    }

    fun initializeProperty(){ //변수 초기화
//        myUid = Firebase.auth.currentUser?.uid.toString()!!
//        fireDatabase = FirebaseDatabase.getInstance().reference!!

//        chatRoom = (intent.getSerializableExtra("ChatRoom")) as ChatRoom //인텐트로부터 chatRoom 정보 넘겨받음
        chatRoomUid = intent.getStringExtra("ChatRoomUid")!! //intent로부터 chatRoomUid넘겨받음
        destinationUid = intent.getStringExtra("destinationUid")!! //intent로부터 destinationUid 넘겨받음

        myUid = "jj"
//        chatRoomUid = "a"
//        destinationUid = "jj"
//        postId = intent.getStringExtra("postId")!! //intent로부터 postId넘겨 받음


    }

    //버튼 누를시 리스너 설정
    fun initializeListener() {
        btn_quit.setOnClickListener() { //나가기 X버튼을 눌렀을때
            startActivity(Intent(this@ChatActivity, MainActivity::class.java))
        }

        btn_send.setOnClickListener { //send버튼을 눌렀을 때
            try {//메세지 전송
                Log.d("dest : ", "$destinationUid")

//                var chatRoom = ChatRoom()
//                chatRoom.users.put(myUid, true)
//                chatRoom.users.put(destinationUid, true)

                var message = Message(edt_message.text.toString(), myUid, getDateTimeString()) //메세지 정보 instance생성
                message.senderUid?.let { it1 -> Log.d("Chatting", it1.toString()) }
                message.time?.let{it -> Log.d("Chatting",it)}
                message.message?.let{it -> Log.d("Chatting",it)}
                Log.d("Chatting", "ChatRoomUid : $chatRoomUid")

                fireDatabase.child("chatrooms") //현재 채팅방에 메세지 추가
                    .child(chatRoomUid).child("messages")
                    .push().setValue(message).addOnSuccessListener {
                        Log.d("chatting", "메세지 전송에 성공하였습니다.")
                        edt_message.text.clear()
                    }.addOnCanceledListener {
                        Log.d("chatting", "메세지 전송에 실패하였습니다.")
                    }
            }catch(e:Exception) {
                e.printStackTrace()
                Log.d("chatting", "메세지 전송 중 오류가 발생했습니다.")
            }
        }
    }




    fun getDateTimeString(): String {
        //메세지를 보낸 시간 정보 반환
        try {
            val time = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("MM월 dd일 hh:mm")
            val currentTime = dateFormat.format(Date(time)).toString()

            return currentTime
        }catch(e: Exception) {
            e.printStackTrace()
            throw Exception("getTimeError")
        }


    }

    //수정 필요요
    fun setupChatRoomId() { //chatRoomUid가 없을 경우 초기화 후 채팅 메세지 목록 초기화
        fireDatabase.child("chatrooms")
            .orderByChild("users/${destinationUid}").equalTo(true) //상대방 uid가 포함된 채팅 목록이 있는지 확인
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                   Log.d("Chatting","Fail to read data")
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
//                        val chatRoom = data.getValue<ChatRoom>()
                        chatRoomUid = data.key!! //chatRoomId 초기화
                        setupRecycler() //채팅 메시지 목록 업데이트
                        break


                    }
                }
            })
    }

    fun setupRecycler() { //채팅 메세지 목록 초기화 및 업데이트
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RecyclerViewAdapter(this, chatRoomUid, destinationUid)
    }

//
//

//



    inner class RecyclerViewAdapter(
        private val context: Context,
        private val chatRoomUid: String?,
        private val destinationUid: String?):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var messages = ArrayList<Message>()
        var messageKeys: ArrayList<String> = arrayListOf()
//        private var user: user_data? = null
        val myUid = "jj"
        val recyclerView = (context as ChatActivity).recyclerView


        init {
            getMessageList() //메세지 불러오기
        }

        fun getMessageList() { //메세지 목록 불러오기

            if (chatRoomUid != null) {
                fireDatabase.child("chatrooms").child(chatRoomUid)
                    .child("messages")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(error: DatabaseError) {
                            Log.d("Chatting", "Can't read the data" )

                        }

                        override fun onDataChange(snapshot: DataSnapshot) {
                            messages.clear()
                            for (data in snapshot.children) {
                                val item = data.getValue<Message>()!!
                                messages.add(item!!)
                                messageKeys.add(data.key!!)
                                println(messages)
                            }
                            notifyDataSetChanged() //화면 업데이트

                            //메세지를 보낼 시 스크롤을 최하단으로 내림
                            recyclerView?.scrollToPosition(messages.size - 1)
                        }
                    })
            }

        }

        override fun getItemViewType(position: Int): Int { //메세지의 uid에 따라 my/other 메세지 구분
            if(messages[position].senderUid.equals(myUid)) return 1
            else return 0
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when(viewType) {
                1 -> { //viewType이 1일 경우 내 메세지
                    val binding = ItemMineMessageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )

                    return MyMessageViewHolder(binding)
                }
                else -> {//상대방 메세지
                    val binding = ItemOtherMessageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    return  OtherMessageViewHolder(binding)
                }
            }


        }


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
           if(messages[position].senderUid.equals(myUid)){
               (holder as MyMessageViewHolder).bind(position)
           }else{
               (holder as OtherMessageViewHolder).bind(position)
           }
        }

        inner class MyMessageViewHolder(val binding: ItemMineMessageBinding) : RecyclerView.ViewHolder(binding.root) {
            var txtMessage: TextView = binding.txtMessage
            var background = binding.background
//            val txtIsShown: TextView = binding.txtIsShown
            val txtDatetime: TextView = binding.txtDate

            fun bind(position: Int){
                var message = messages[position]

                txtMessage.text = message.message
                txtDatetime.text = message.time

//                sendMsgToDatabase(message) //메세지를 파이어베이스로 전송

            }



        }
//        fun sendMsgToDatabase(message: Message){
//            if (chatRoomUid != null) {
//                FirebaseDatabase.getInstance().getReference("chatrooms")
//                    .child(chatRoomUid).child("messages")
//                    .setValue(message)
//                    .addOnSuccessListener {
//                        Log.d("chatting", "메세지를 성공적으로 저장했습니다.")
//                    }
//            }
//
//        }

        inner class OtherMessageViewHolder(val binding: ItemOtherMessageBinding) : RecyclerView.ViewHolder(binding.root){
            var txtMessage: TextView = binding.txtMessage
            var background = binding.background
            //            val txtIsShown: TextView = binding.txtIsShown
            val txtDatetime: TextView = binding.txtDate

            fun bind(position: Int){
                var message = messages[position]
                var sendDate = message.time

                txtMessage.text = message.message

                txtDatetime.text = sendDate

//                sendMsgToDatabase(message)

            }
        }


        override fun getItemCount(): Int {
            return messages.size
        }


    }
}