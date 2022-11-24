package com.kgg.android.delivers.chatActivity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.os.HandlerCompat.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.MainActivity
import com.kgg.android.delivers.R
import com.kgg.android.delivers.data.ChatRoom
import com.kgg.android.delivers.data.Message
import com.kgg.android.delivers.databinding.ActivityChatBinding
import com.kgg.android.delivers.databinding.ItemMineMessageBinding
import com.kgg.android.delivers.databinding.ItemOtherMessageBinding
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.certification.*
import java.text.SimpleDateFormat
import java.util.*


class ChatActivity : AppCompatActivity() {
    val binding by lazy { ActivityChatBinding.inflate(layoutInflater) }
    private val storage: FirebaseStorage =
        FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference
    val firestore = FirebaseFirestore.getInstance()

    lateinit var btn_quit: ImageButton
    lateinit var btn_send: Button
    lateinit var exit_button: Button
    lateinit var chat_title: TextView
    lateinit var edt_message: EditText
    lateinit var recyclerView: RecyclerView
    lateinit var chatRoom: ChatRoom

    lateinit var myUid: String
    lateinit var chatRoomId: String
    lateinit var destinationUid: String
    lateinit var postId: String


    private val fireDatabase = FirebaseDatabase.getInstance().reference
    private val fireStore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initializeProperty()

        btn_quit = binding.imgbtnQuit
        edt_message = binding.edtMessage
        recyclerView = binding.recyclerMessages
        btn_send = binding.btnSubmit
        chat_title = binding.txtTItle
        exit_button = binding.exitButton



        initializeListener()
        //상대방 닉네임 연결
        val docCol = fireStore.collection("users") //작업할 컬렉션
        docCol
            .whereEqualTo("uid", "$destinationUid") //uid가 destinationUid와 일치하는 문서 가져오기
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var nickname = document["nickname"] as String
                    chat_title.text = nickname
                    Log.d("chatting", "nickname: $nickname")
                    //채팅방 제목을 상대방 닉네임으로 설정
                }

            }
            .addOnFailureListener { exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }




        if (chatRoomId.equals("")) { //채팅방 키가 없으면 생성
            setupChatRoomId()

        } else //채팅방 키가 있으면 채팅 메세지 목록 보여주기
            setupRecycler()
    }


    fun initializeProperty(){ //변수 초기화
        auth = FirebaseAuth.getInstance()
        myUid = auth.currentUser?.uid.toString()!!

        Log.d("Chatting", "this is chatActivity")

       chatRoomId = intent.getStringExtra("ChatRoomId")!! //intent로부터 chatRoomId넘겨받음
       destinationUid = intent.getStringExtra("destinationUid")!! //intent로부터 destinationUid 넘겨받음
       postId = intent.getStringExtra("postId")!! //intent로부터 postId넘겨 받음


    }

    //버튼 누를시 리스너 설정
    fun initializeListener() {
        btn_quit.setOnClickListener() { //나가기 X버튼을 눌렀을때
            startActivity(Intent(this@ChatActivity, MainActivity::class.java))
        }

        btn_send.setOnClickListener { //send버튼을 눌렀을 때
            Log.d("Chatting", "sendDest : $destinationUid")

            //상대방이 채팅방을 나갔는지 확인
            var sendState = true

            fireDatabase.child("chatrooms")
                .orderByChild("postId")
                .equalTo(postId) //해당 포스트 아이디에 해당되는 채팅방 데이터 가져옴
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (data in snapshot.children) {
                            val chatRoom = data.getValue<ChatRoom>()
                            if (chatRoom != null) {
                                if (!chatRoom.users.getValue(destinationUid)) {
                                    Log.d("Chatting", "destinationUid false")
                                    Toast.makeText(
                                        this@ChatActivity,
                                        "상대방이 채팅방을 나가 메세지를 보낼 수 없습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    sendState = false
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.d("Chatting","Fail to read data")
                    }
                })


            Handler().postDelayed({
                try {
                    if(sendState) {
                        if (edt_message.text.isNotEmpty()) { //메세지가 empty가 아닐 경우만 보낼 수 있음

                            var message = Message(
                                edt_message.text.toString(),
                                myUid,
                                getDateTimeString()
                            ) //메세지 정보 instance생성
                            message.senderUid?.let { it1 ->
                                Log.d(
                                    "Chatting",
                                    it1.toString()
                                )
                            }
                            message.time?.let { it -> Log.d("Chatting", it) }
                            message.message?.let { it -> Log.d("Chatting", it) }
                            Log.d("Chatting", "ChatRoomId : $chatRoomId")

                            chatRoomId?.let { it1 ->
                                fireDatabase.child("chatrooms") //현재 채팅방에 메세지 추가
                                    .child(it1).child("messages")
                                    .push().setValue(message).addOnSuccessListener {
                                        Log.d("chatting", "메세지 전송에 성공하였습니다.")
                                        edt_message.text.clear()
                                    }.addOnCanceledListener {
                                        Log.d("chatting", "메세지 전송에 실패하였습니다.")
                                    }
                            }
                        }
                    }
                }catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("chatting", "메세지 전송 중 오류가 발생했습니다.")
                    }
            }, 1000L)





        }

        exit_button.setOnClickListener{

            AlertDialog.Builder(this)
                .setTitle("채팅방 나가기")
                .setMessage("나가기를 하면 대화내용이 모두 삭제되고, 해당 포스트에 대해 상대방과 채팅을 다시 할 수 없습니다. ")
                .setPositiveButton("취소", DialogInterface.OnClickListener { dialog, which ->
                })
                .setNegativeButton("나가기", DialogInterface.OnClickListener { dialog, which ->
                    var childUpdates: HashMap<String, Boolean> =  HashMap()
                    childUpdates.put("chatrooms/$chatRoomId/users/$myUid", false)
                    fireDatabase.updateChildren(childUpdates as Map<String, Any>)
                    Log.d("Chatting","Chatroom $chatRoomId destroyed.")

                    var intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                })
                .show()



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
    fun setupChatRoomId() {
       //chatRoomId가 없을 경우 초기화 후 채팅 메세지 목록 초기화
        var chatRoom = ChatRoom(postId)
        chatRoom.users.put(myUid,true)
        chatRoom.users.put(destinationUid, true)
        Log.d("chatting", "$chatRoom")

        fireDatabase.child("chatrooms") //채팅방 생성
            .push().setValue(chatRoom).addOnSuccessListener {
                Log.d("chatting", "채팅방 생성에 성공하였습니다.")
                edt_message.text.clear()
            }.addOnCanceledListener {
                Log.d("chatting", "채팅방 생성에 실패하였습니다.")
            }


        fireDatabase.child("chatrooms")
            .orderByChild("postId")
            .equalTo(postId) //postId가 같은 채팅방 목록 가져옴
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                   Log.d("Chatting","Fail to read data")
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val chatRoom = data.getValue<ChatRoom>()
                        if (chatRoom != null) {
                            if(chatRoom.users.getValue(destinationUid)) //상대방 id와 같은 채팅방을 찾아 그 데이터의 chatRoomId로 저장
                                chatRoomId = data.key!!

                        } //chatRoomId 초기화
                        setupRecycler() //채팅 메시지 목록 업데이트
                        break


                    }
                }
            })
//        setupRecycler()
    }

    fun setupRecycler() { //채팅 메세지 목록 초기화 및 업데이트
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RecyclerViewAdapter(this, chatRoomId, destinationUid)
    }

//
//

//

    inner class RecyclerViewAdapter(
        private val context: Context,
        private val chatRoomId: String?,
        private val destinationUid: String?):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var messages = ArrayList<Message>()
        var messageKeys: ArrayList<String> = arrayListOf()

        val myUid = auth.currentUser?.uid.toString()!!
//        val myUid = "WoKw1NJYG8TB9Z4GDWh4H5e9ieh1"
        val recyclerView = (context as ChatActivity).recyclerView


        init {
            getMessageList() //메세지 불러오기
        }

        fun getMessageList() { //메세지 목록 불러오기

            if (chatRoomId != null) {
                fireDatabase.child("chatrooms").child(chatRoomId)
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
            val txtDatetime: TextView = binding.txtDate

            fun bind(position: Int){
                var message = messages[position]
                txtMessage.text = message.message
                txtDatetime.text = message.time

//                sendMsgToDatabase(message) //메세지를 파이어베이스로 전송

            }



        }
//        fun sendMsgToDatabase(message: Message){
//            if (chatRoomId != null) {
//                FirebaseDatabase.getInstance().getReference("chatrooms")
//                    .child(chatRoomId).child("messages")
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

    override fun onBackPressed() { //뒤로가기 처리
        startActivity(Intent(this, MainActivity::class.java))
        finish()

    }


}
