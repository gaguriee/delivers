package com.kgg.android.delivers.chatActivity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.os.HandlerCompat.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.googlecode.tesseract.android.TessBaseAPI
import com.kgg.android.delivers.MainActivity
import com.kgg.android.delivers.R
import com.kgg.android.delivers.data.ChatRoom
import com.kgg.android.delivers.data.Message
import com.kgg.android.delivers.databinding.ActivityChatBinding
import com.kgg.android.delivers.databinding.ItemMineMessageBinding
import com.kgg.android.delivers.databinding.ItemOtherMessageBinding
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_fast_create.*
import kotlinx.android.synthetic.main.certification.*
import java.io.*
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
    lateinit var exit_button: ImageView
    lateinit var chat_title: TextView
    lateinit var edt_message: EditText
    lateinit var recyclerView: RecyclerView
    lateinit var chatRoom: ChatRoom
    lateinit var btn_payment: ImageButton

    lateinit var myUid: String
    lateinit var chatRoomId: String
    lateinit var destinationUid: String
    lateinit var postId: String


    private val fireDatabase = FirebaseDatabase.getInstance().reference
    private val fireStore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    // for OCR
    var image //사용되는 이미지
            : Bitmap? = null
    private var mTess //Tess API reference
            : TessBaseAPI? = null
    var datapath = ""


    var OCRTextView // OCR 결과뷰
            : TextView? = null

    private var imgUri : Uri? = null



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
        btn_payment = binding.cal




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

        // 정산 버튼 누르면 영수증 사진 올리기
        btn_payment.setOnClickListener() {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 10)

        }
    }

    // 영수증 사진 업로드
    override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            10 ->
                if (resultCode == RESULT_OK) {
                    datapath = "$filesDir/tesseract/"

                    checkFile(File(datapath + "tessdata/"))
                    //Tesseract API 언어 세팅
                    val lang = "kor"

                    //OCR 세팅
                    mTess = TessBaseAPI()
                    mTess!!.init(datapath, lang)
                    imgUri = data?.data!!
                    image = MediaStore.Images.Media.getBitmap(this.contentResolver, imgUri) as Bitmap
                    mTess!!.setImage(image)
                    var OCRresult = mTess!!.utF8Text
                    Log.d("ocrtest","hi!! ${OCRresult}")
                    OCRresult = OCRresult.replace(" ", "") // remove blank
                    val lines = OCRresult.split("\r?\n|\r".toRegex()).toTypedArray() // split by linebreak
                    var result = ""
                    var amount = ""

                    for(i in lines){
                        if(i.contains("합계")||i.contains("함계")||i.contains("총금액")||i.contains("받은금액")||i.contains("걸제굼액")||i.contains("결제굼액")||i.contains("결제금액")||i.contains("총결제금액")){
                            result = i
                        }
                    }
                    if(result!=""){
                        amount = result.replace("[^0-9]".toRegex(), "") // extract number from string
                        edt_message.setText("Total amount is.. : "+amount.toString())
                    }


                }
                else {
                    finish() // 사진 선택이 안된 채로 뒤로 가기가 눌렸을 경우 액티비티 종t
                }

        }
    }




    fun initializeProperty(){ //변수 초기화
        auth = FirebaseAuth.getInstance()
        myUid = auth.currentUser?.uid.toString()!!

        Log.d("Chatting", "this is chatActivity")

       chatRoomId = intent.getStringExtra("ChatRoomId")!! //intent로부터 chatRoomId넘겨받음
       destinationUid = intent.getStringExtra("destinationUid")!! //intent로부터 destinationUid 넘겨받음
       postId = intent.getStringExtra("postId")!! //intent로부터 postId넘겨 받음
        Log.d("Chatting","Chatroom ID: $chatRoomId")
        Log.d("Chatting","dest ID: $destinationUid")
        Log.d("Chatting","post ID: $postId")
        Log.d("Chatting","my ID: ${myUid}")


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
                                if(myUid in chatRoom.users.keys){
                                    if (!chatRoom.users.getValue(destinationUid)) {
                                        Log.d("Chatting", "destinationUid false")
                                        Toast.makeText(
                                            this@ChatActivity,
                                            "상대방이 채팅방을 나가 메세지를 보낼 수 없습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        sendState = false
                                        break
                                    }

                                    if(!chatRoom.users.getValue(myUid)){
                                        Log.d("Chatting", "destinationUid false")
                                        Toast.makeText(
                                            this@ChatActivity,
                                            "이미 나간 채팅방입니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        sendState = false
                                        break
                                    }
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
                    Log.d("스냅샷","${snapshot.getChildrenCount()}")
                    var chk_state = false
                    var tmp_id = ""
                    for (data in snapshot.children) {
                        val chatRoom = data.getValue<ChatRoom>()
                        if (chatRoom != null) {
                            if(chatRoom.users.getValue(destinationUid)&&(myUid in chatRoom.users.keys)) //상대방 id와 같은 채팅방을 찾아 그 데이터의 chatRoomId로 저장
                            {
                                if(chatRoom.users.getValue(myUid)){
                                    chatRoomId = data.key!!
                                    setupRecycler()
                                    break
                                }

                            }

                        } //chatRoomId 초기화



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


            }



        }

        inner class OtherMessageViewHolder(val binding: ItemOtherMessageBinding) : RecyclerView.ViewHolder(binding.root){
            var txtMessage: TextView = binding.txtMessage
            var background = binding.background
            val txtDatetime: TextView = binding.txtDate

            fun bind(position: Int){
                var message = messages[position]
                var sendDate = message.time
                txtMessage.text = message.message

                txtDatetime.text = sendDate


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


    private val langFileName = "kor.traineddata"
    private fun copyFiles(dir:String) {
        try {
            //val filepath = datapath + "tessdata/" + langFileName
            val filepath = dir
            Log.d("path","${filepath}")
            val assetManager = assets
            val instream: InputStream = assetManager.open(langFileName)
            val outstream: OutputStream = FileOutputStream(filepath)
            val buffer = ByteArray(1024)
            var read: Int
            while (instream.read(buffer).also { read = it } != -1) {
                outstream.write(buffer, 0, read)
            }
            outstream.flush()
            outstream.close()
            instream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun checkFile(dir: File) {
        //디렉토리가 없으면 디렉토리를 만들고 그후에 파일을 카피
        val datafilepath = datapath + "tessdata/" + langFileName
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles(datafilepath)
        }
        //디렉토리가 있지만 파일이 없으면 파일카피 진행
        if (dir.exists()) {

            val datafile = File(datafilepath)
            if (!datafile.exists()) {
                copyFiles(datafilepath)
            }
        }
    }


}
