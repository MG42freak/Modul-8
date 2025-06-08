package com.example.modul_8

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StudentViewModel : ViewModel() {
    private val db = Firebase.firestore
    var students by mutableStateOf(listOf<Student>())
        private set

    init {
        fetchStudents()
    }

    fun addStudent(student: Student) {
        val studentMap = hashMapOf(
            "id" to student.id,
            "name" to student.name,
            "program" to student.program
        )

        db.collection("students")
            .add(studentMap)
            .addOnSuccessListener { docRef ->
                Log.d("Firestore", "Student added with ID: ${docRef.id}")

                student.phones.forEach { phone ->
                    val phoneMap = hashMapOf("number" to phone)
                    docRef.collection("phones").add(phoneMap)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Phone number added")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Failed to add phone number", e)
                        }
                }

                fetchStudents()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding student", e)
            }
    }

    private fun fetchStudents() {
        db.collection("students")
            .get()
            .addOnSuccessListener { result ->
                val studentList = mutableListOf<Student>()
                for (document in result) {
                    val id = document.getString("id") ?: ""
                    val name = document.getString("name") ?: ""
                    val program = document.getString("program") ?: ""

                    val student = Student(id, name, program, emptyList())
                    val docId = document.id

                    db.collection("students").document(docId)
                        .collection("phones")
                        .get()
                        .addOnSuccessListener { phonesSnapshot ->
                            val phoneNumbers = phonesSnapshot.mapNotNull { it.getString("number") }
                            val updatedStudent = student.copy(phones = phoneNumbers)
                            studentList.add(updatedStudent)
                            students = studentList.sortedBy { it.id } // update only after all done
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error fetching phones", e)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting students", exception)
            }
    }
}