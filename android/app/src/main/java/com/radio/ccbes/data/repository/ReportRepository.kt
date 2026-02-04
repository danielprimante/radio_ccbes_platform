package com.radio.ccbes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.radio.ccbes.data.model.Report
import kotlinx.coroutines.tasks.await

class ReportRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val reportsCollection = firestore.collection("reports")

    /**
     * Submit a report for a post or comment
     */
    suspend fun submitReport(report: Report): Result<Unit> {
        return try {
            reportsCollection.add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
