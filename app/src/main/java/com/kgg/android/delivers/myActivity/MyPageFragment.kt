package com.kgg.android.delivers.myActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.databinding.FragmentMyPageBinding
import com.kgg.android.delivers.loginActivity.loginActivity


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

// 보영
// 마이페이지
class MyPageFragment : Fragment() {
    var uid = ""
    val firestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference
    var nickname:String = ""
    var email:String = ""
    var phoneNum:String = ""

    private val fireDatabase = FirebaseDatabase.getInstance().reference

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentMyPageBinding.inflate(inflater,container,false)
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()


        val userInfo = firestore.collection("users") //작업할 컬렉션
        var count = 0
        userInfo
            .whereEqualTo("uid", "$uid") //uid가 destinationUid와 일치하는 문서 가져오기
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if(count==0){
                        nickname = document["nickname"] as String
                        phoneNum = document["phoneNum"] as String
                        email = document["email"] as String
                        binding.nameText.text = nickname.toString()
                        binding.phoneText.text = phoneNum.toString()
                    }
                    count++
                    Log.d("mypage", "nickname: $nickname")
                }
                Log.d("mypage", "nickname: $nickname")

            }
            .addOnFailureListener { exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }

        Log.d("mypage", "nickname 테스트 : $nickname")
        binding.infoView.setOnClickListener{
            val intent = Intent(requireContext(), MyInfoActivity::class.java)
            intent.putExtra("nickname",nickname)
            intent.putExtra("email",email)
            intent.putExtra("phoneNum",phoneNum)
            startActivity(intent)
        }
        binding.infoBtn.setOnClickListener{
            val intent = Intent(requireContext(), MyInfoActivity::class.java)
            intent.putExtra("nickname",nickname)
            intent.putExtra("email",email)
            intent.putExtra("phoneNum",phoneNum)
            startActivity(intent)
        }
        binding.postBtn.setOnClickListener{
            val intent = Intent(requireContext(), MyStoryActivity::class.java)
            intent.putExtra("uid",uid)
            startActivity(intent)
        }

        binding.logOutButton.setOnClickListener {
            val intent = Intent(requireContext(), loginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            auth?.signOut()
        }





        return binding.root
    }


}