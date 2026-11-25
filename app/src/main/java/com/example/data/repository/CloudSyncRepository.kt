package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.domain.model.AppSettings
import com.example.domain.model.GalleryMedia
import com.example.domain.model.SyncedMediaItem
import com.example.domain.model.UserCloudProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun isValidDocId(id: String): Boolean {
        return id.isNotEmpty() && id.length <= 128 && DOC_ID_REGEX.matches(id)
    }

    fun observeProfile(userId: String): Flow<UserCloudProfile?> {
        require(userId.isNotBlank() && isValidDocId(userId)) { "Invalid userId for profile stream" }
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserCloudProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    fun observeSyncedItems(userId: String): Flow<List<SyncedMediaItem>> {
        require(userId.isNotBlank() && isValidDocId(userId)) { "Invalid userId for synced items stream" }
        val colRef = db.collection("users").document(userId).collection("synced_items")
        return colRef
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SyncedMediaItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun upsertProfile(
        userId: String,
        displayName: String?,
        email: String?,
        settings: AppSettings
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(isValidDocId(userId)) { "Invalid userId format" }

            val safeColumns = settings.gridColumns.coerceIn(2, 5).toLong()
            val safeDisplayName = displayName?.trim()?.take(100)?.takeIf { it.isNotEmpty() }
            val safeEmail = email?.trim()?.take(254)?.takeIf { it.isNotEmpty() }

            val docRef = db.collection("users").document(userId)
            val exists = docRef.get().await().exists()

            if (!exists) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "themeMode" to settings.themeMode.name,
                    "gridColumns" to safeColumns,
                    "relockTimeout" to settings.relockTimeout.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).apply {
                    safeDisplayName?.let { put("displayName", it) }
                    safeEmail?.let { put("email", it) }
                }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "themeMode" to settings.themeMode.name,
                    "gridColumns" to safeColumns,
                    "relockTimeout" to settings.relockTimeout.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).apply {
                    safeDisplayName?.let { put("displayName", it) }
                    safeEmail?.let { put("email", it) }
                }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncFavoritesBatch(
        userId: String,
        favorites: List<GalleryMedia>
    ): Result<Int> {
        val colPath = "users/$userId/synced_items"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(isValidDocId(userId)) { "Invalid userId format" }

            val colRef = db.collection("users").document(userId).collection("synced_items")
            var syncedCount = 0
            for (media in favorites.take(100)) {
                val docId = "media_${media.id}"
                if (!isValidDocId(docId)) continue
                val docRef = colRef.document(docId)
                val safeName = media.displayName.trim().ifBlank { "Untitled" }.take(200)
                val safeBucket = media.bucketName.trim().take(120)
                val safeSize = media.sizeBytes.coerceAtLeast(0L)
                val safeDate = media.dateTakenMs.coerceAtLeast(0L)

                val existingSnap = docRef.get().await()
                if (!existingSnap.exists()) {
                    val payload = mapOf(
                        "userId" to uid,
                        "mediaKey" to media.id.toString().take(128),
                        "displayName" to safeName,
                        "bucketName" to safeBucket,
                        "mediaType" to media.mediaType.name,
                        "favorite" to true,
                        "sizeBytes" to safeSize,
                        "dateTakenMs" to safeDate,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                } else {
                    val updatePayload = mapOf(
                        "favorite" to true,
                        "displayName" to safeName,
                        "bucketName" to safeBucket,
                        "sizeBytes" to safeSize,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updatePayload).await()
                }
                syncedCount++
            }
            Result.success(syncedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    suspend fun updateItemNote(
        userId: String,
        itemId: String,
        note: String
    ): Result<Unit> {
        val docPath = "users/$userId/synced_items/$itemId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(isValidDocId(userId) && isValidDocId(itemId)) { "Invalid document ID" }
            val trimmed = note.trim()
            require(trimmed.length <= 500) { "Note cannot exceed 500 characters" }

            val docRef = db.collection("users").document(userId)
                .collection("synced_items").document(itemId)
            docRef.update(
                mapOf(
                    "note" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSyncedItem(
        userId: String,
        itemId: String
    ): Result<Unit> {
        val docPath = "users/$userId/synced_items/$itemId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(isValidDocId(userId) && isValidDocId(itemId)) { "Invalid document ID" }

            db.collection("users").document(userId)
                .collection("synced_items").document(itemId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private companion object {
        val DOC_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
