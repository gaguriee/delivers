package com.kgg.android.delivers.myActivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.MainActivity
import com.kgg.android.delivers.databinding.ActivityMyInfoBinding
import kotlinx.android.synthetic.main.activity_main.*


class MyInfoActivity : AppCompatActivity() {

    val firestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference

    private val fireDatabase = FirebaseDatabase.getInstance().reference

    var uid = ""

    private lateinit var auth: FirebaseAuth
    var nickname:String = ""
    var email:String = ""
    var phoneNum:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMyInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        toolbar.setTitle("")

        auth = FirebaseAuth.getInstance()

        uid = auth.currentUser?.uid.toString()


        nickname = intent.getStringExtra("nickname")?.toString()!!
        email = intent.getStringExtra("email")?.toString()!!
        phoneNum = intent.getStringExtra("phoneNum")?.toString()!!
        binding.emailEdit.setText(email)
        binding.phoneEdit.text = phoneNum
        binding.nameEdit.setText(nickname)


        binding.editBtn.setOnClickListener{
            val intent = Intent(applicationContext, MainActivity::class.java)
            var newName = binding.nameEdit.text.toString()
            var newEmail = binding.emailEdit.text.toString()
            val infodt = InfoData(newName,newEmail)
            var infoMap = HashMap<String,Object>()
            infoMap.put("${uid}",infodt as Object)




            firestore.collection("users").whereEqualTo("uid","${uid}").get().
                    addOnSuccessListener {
                        firestore.collection("users").document("${uid}").update("nickname",newName)
                        firestore.collection("users").document("${uid}").update("email",newEmail)
                        Log.d("Chatting", "성공!! ${newName}, ${newEmail}")

                        }.addOnFailureListener { exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }
            startActivity(intent)

                    }

        binding.textView7.setOnClickListener{
            val intent = Intent(applicationContext, MainActivity::class.java)
            binding.nameEdit.text
            binding.emailEdit.text

            firestore.collection("users").whereEqualTo("uid","${uid}").get().
            addOnSuccessListener {
                firestore.collection("users").document("${uid}").update("nickname",nickname)
                firestore.collection("users").document("${uid}").update("email",email)

            }.addOnFailureListener { exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }
        }



        }
    data class InfoData(val nickname:String,val email:String)




}