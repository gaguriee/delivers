package com.kgg.android.delivers.chatActivity

import android.app.Dialog
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
import kotlinx.android.synthetic.main.custom_dialog.*
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
    var headcnt = 1


    private val fireDatabase = FirebaseDatabase.getInstance().reference
    private val fireStore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    // for OCR
    var image //???????????? ?????????
            : Bitmap? = null
    private var mTess //Tess API reference
            : TessBaseAPI? = null
    var datapath = ""


    var OCRTextView // OCR ?????????
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
        //????????? ????????? ??????
        val docCol = fireStore.collection("users") //????????? ?????????
        docCol
            .whereEqualTo("uid", "$destinationUid") //uid??? destinationUid??? ???????????? ?????? ????????????
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var nickname = document["nickname"] as String
                    chat_title.text = nickname
                    Log.d("chatting", "nickname: $nickname")
                    //????????? ????????? ????????? ??????????????? ??????
                }

            }
            .addOnFailureListener { exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }




        if (chatRoomId.equals("")) { //????????? ?????? ????????? ??????
            setupChatRoomId()

        } else //????????? ?????? ????????? ?????? ????????? ?????? ????????????
            setupRecycler()

        // ?????? ?????? ????????? ????????? ?????? ?????????
        btn_payment.setOnClickListener() {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 10)

        }
    }

    // ????????? ?????? ?????????
    override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            10 ->
                if (resultCode == RESULT_OK) {
                    datapath = "$filesDir/tesseract/"

                    checkFile(File(datapath + "tessdata/"))
                    //Tesseract API ?????? ??????
                    val lang = "kor"

                    //OCR ??????
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
                        if(i.contains("??????")||i.contains("??????")||i.contains("?????????")||i.contains("????????????")||i.contains("????????????")||i.contains("????????????")||i.contains("????????????")||i.contains("???????????????")){
                            result = i
                        }
                    }
                    if(result!=""){
                        amount = result.replace("[^0-9]".toRegex(), "") // extract number from string

                    }
                    var dia = CustomDialog(this)
                    dia.MyDia()
                    dia.dialog.btnDone.setOnClickListener{
                        if(dia.dialog.dialogEt.text.toString()!=""&&(isNumeric(dia.dialog.dialogEt.text.toString())==true)){
                            headcnt = dia.dialog.dialogEt.text.toString().toInt()
                            edt_message.setText(" Total amount is ???${amount} ! You should pay : ???"+(amount.toDouble()/headcnt.toDouble()).toString())
                            dia.dialog.dismiss()
                        }else{
                            Toast.makeText(this,"Please enter the number of people!",Toast.LENGTH_SHORT).show()
                        }

                    }



                }
                else {
                    finish() // ?????? ????????? ?????? ?????? ?????? ????????? ????????? ?????? ???????????? ??????
                }

        }
    }




    fun initializeProperty(){ //?????? ?????????
        auth = FirebaseAuth.getInstance()
        myUid = auth.currentUser?.uid.toString()!!

        Log.d("Chatting", "this is chatActivity")

       chatRoomId = intent.getStringExtra("ChatRoomId")!! //intent????????? chatRoomId????????????
       destinationUid = intent.getStringExtra("destinationUid")!! //intent????????? destinationUid ????????????
       postId = intent.getStringExtra("postId")!! //intent????????? postId?????? ??????
        Log.d("Chatting","Chatroom ID: $chatRoomId")
        Log.d("Chatting","dest ID: $destinationUid")
        Log.d("Chatting","post ID: $postId")
        Log.d("Chatting","my ID: ${myUid}")


    }

    //?????? ????????? ????????? ??????
    fun initializeListener() {
        btn_quit.setOnClickListener() { //????????? X????????? ????????????
            startActivity(Intent(this@ChatActivity, MainActivity::class.java))
        }

        btn_send.setOnClickListener { //send????????? ????????? ???
            Log.d("Chatting", "sendDest : $destinationUid")

            //???????????? ???????????? ???????????? ??????
            var sendState = true

            fireDatabase.child("chatrooms")
                .orderByChild("postId")
                .equalTo(postId) //?????? ????????? ???????????? ???????????? ????????? ????????? ?????????
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
                                            "???????????? ???????????? ?????? ???????????? ?????? ??? ????????????.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        sendState = false
                                        break
                                    }

                                    if(!chatRoom.users.getValue(myUid)){
                                        Log.d("Chatting", "destinationUid false")
                                        Toast.makeText(
                                            this@ChatActivity,
                                            "?????? ?????? ??????????????????.",
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
                        if (edt_message.text.isNotEmpty()) { //???????????? empty??? ?????? ????????? ?????? ??? ??????

                            var message = Message(
                                edt_message.text.toString(),
                                myUid,
                                getDateTimeString()
                            ) //????????? ?????? instance??????
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
                                fireDatabase.child("chatrooms") //?????? ???????????? ????????? ??????
                                    .child(it1).child("messages")
                                    .push().setValue(message).addOnSuccessListener {
                                        Log.d("chatting", "????????? ????????? ?????????????????????.")
                                        edt_message.text.clear()
                                    }.addOnCanceledListener {
                                        Log.d("chatting", "????????? ????????? ?????????????????????.")
                                    }
                            }
                        }
                    }
                }catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("chatting", "????????? ?????? ??? ????????? ??????????????????.")
                    }
            }, 1000L)





        }

        exit_button.setOnClickListener{

            AlertDialog.Builder(this)
                .setTitle("????????? ?????????")
                .setMessage("???????????? ?????? ??????????????? ?????? ????????????, ?????? ???????????? ?????? ???????????? ????????? ?????? ??? ??? ????????????. ")
                .setPositiveButton("??????", DialogInterface.OnClickListener { dialog, which ->
                })
                .setNegativeButton("?????????", DialogInterface.OnClickListener { dialog, which ->
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
        //???????????? ?????? ?????? ?????? ??????
        try {
            val time = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("MM??? dd??? hh:mm")
            val currentTime = dateFormat.format(Date(time)).toString()

            return currentTime
        }catch(e: Exception) {
            e.printStackTrace()
            throw Exception("getTimeError")
        }


    }


    fun setupChatRoomId() {
       //chatRoomId??? ?????? ?????? ????????? ??? ?????? ????????? ?????? ?????????
        var chatRoom = ChatRoom(postId)
        chatRoom.users.put(myUid,true)
        chatRoom.users.put(destinationUid, true)
        Log.d("chatting", "$chatRoom")

        fireDatabase.child("chatrooms") //????????? ??????
            .push().setValue(chatRoom).addOnSuccessListener {
                Log.d("chatting", "????????? ????????? ?????????????????????.")
                edt_message.text.clear()
            }.addOnCanceledListener {
                Log.d("chatting", "????????? ????????? ?????????????????????.")
            }


        fireDatabase.child("chatrooms")
            .orderByChild("postId")
            .equalTo(postId) //postId??? ?????? ????????? ?????? ?????????
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                   Log.d("Chatting","Fail to read data")
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("?????????","${snapshot.getChildrenCount()}")
                    var chk_state = false
                    var tmp_id = ""
                    for (data in snapshot.children) {
                        val chatRoom = data.getValue<ChatRoom>()
                        if (chatRoom != null) {
                            if(chatRoom.users.getValue(destinationUid)&&(myUid in chatRoom.users.keys)) //????????? id??? ?????? ???????????? ?????? ??? ???????????? chatRoomId??? ??????
                            {
                                if(chatRoom.users.getValue(myUid)){
                                    chatRoomId = data.key!!
                                    setupRecycler()
                                    break
                                }

                            }

                        } //chatRoomId ?????????



                    }

                }
            })
//        setupRecycler()
    }

    fun setupRecycler() { //?????? ????????? ?????? ????????? ??? ????????????
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
            getMessageList() //????????? ????????????
        }

        fun getMessageList() { //????????? ?????? ????????????

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
                            notifyDataSetChanged() //?????? ????????????

                            //???????????? ?????? ??? ???????????? ??????????????? ??????
                            recyclerView?.scrollToPosition(messages.size - 1)
                        }
                    })
            }

        }
        override fun getItemViewType(position: Int): Int { //???????????? uid??? ?????? my/other ????????? ??????
            if(messages[position].senderUid.equals(myUid)) return 1
            else return 0
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when(viewType) {
                1 -> { //viewType??? 1??? ?????? ??? ?????????
                    val binding = ItemMineMessageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )

                    return MyMessageViewHolder(binding)
                }
                else -> {//????????? ?????????
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

    override fun onBackPressed() { //???????????? ??????
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
        //??????????????? ????????? ??????????????? ????????? ????????? ????????? ??????
        val datafilepath = datapath + "tessdata/" + langFileName
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles(datafilepath)
        }
        //??????????????? ????????? ????????? ????????? ???????????? ??????
        if (dir.exists()) {

            val datafile = File(datafilepath)
            if (!datafile.exists()) {
                copyFiles(datafilepath)
            }
        }
    }

    // custom dialog
    class CustomDialog(context:Context){
        val dialog = Dialog(context)
        fun MyDia(){
            dialog.setContentView(R.layout.custom_dialog)
            dialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT)
            dialog.btnCan.setOnClickListener{
                dialog.dismiss()
            }
            dialog.show()
        }
    }
    fun isNumeric(s: String): Boolean {
        return try {
            s.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }








}
