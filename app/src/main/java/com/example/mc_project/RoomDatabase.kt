package com.example.mc_project

import android.content.Context
import android.net.Uri
import androidx.room.*
import java.io.File
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Developers web page was used as a base and help to create this file
// Also used different forums for debugging but nothing has been directly copied

//---ENTITIES---
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: Int = 0,
    @ColumnInfo(name = "username") val username: String = "",
    @ColumnInfo(name = "image_path") val imagePath: String? = null
)

@Entity(tableName = "camera_images")
data class CameraImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

//---DAOS--
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE uid = 0 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface CameraImageDao {

    @Query("SELECT * FROM camera_images ORDER BY created_at DESC")
    fun observeImages(): Flow<List<CameraImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: CameraImageEntity)

    @Query("DELETE FROM camera_images")
    suspend fun clearAll()
}

//---DATABASE---
@Database(entities = [UserProfileEntity::class, CameraImageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun cameraImageDao(): CameraImageDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mc_project.db"
                ).build()
            }
    }
}

//---IMAGE STORAGE HELPER---
fun copyPickedImageToAppStorage(context: Context, uri: Uri): String {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: error("Cannot open input stream for: $uri")

    val outFile = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")

    inputStream.use { input -> outFile.outputStream().use { output -> input.copyTo(output) } }

    return outFile.absolutePath
}

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    // --- data saving ---
    private val db = AppDatabase.getInstance(app)
    private val profileDao = db.userProfileDao()
    private val cameraDao = db.cameraImageDao()

    // --- PROFILE ---
    private val profile: StateFlow<UserProfileEntity> =
        profileDao.observeProfile()
            .map { it ?: UserProfileEntity(uid = 0) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                UserProfileEntity(uid = 0)
            )

    val name = profile.map { it.username }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            "")

    val imagePath = profile.map { it.imagePath }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            null)

    fun saveName(newName: String) {
        viewModelScope.launch {
            profileDao.upsert(profile.value.copy(username = newName))
        }
    }

    fun saveImagePath(path: String) {
        viewModelScope.launch {
            profileDao.upsert(profile.value.copy(imagePath = path))
        }
    }

    // --- GALLERY ---
    val cameraImages: StateFlow<List<CameraImageEntity>> =
        cameraDao.observeImages()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun addCameraImage(path: String) {
        viewModelScope.launch {
            cameraDao.insert(CameraImageEntity(imagePath = path))
        }
    }

    fun clearCameraImages() {
        viewModelScope.launch {
            cameraDao.clearAll()
        }
    }

    // --- Sensor/conversation feed messages ---
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun addMessage(imagePath: String) {
        val updated = _messages.value + Message(imagePath)
        _messages.value = if (updated.size > 200) updated.takeLast(200) else updated
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    private val _sensorDark = MutableStateFlow(false)
    val sensorDark: StateFlow<Boolean> = _sensorDark

    fun setSensorDark(value: Boolean) {
        _sensorDark.value = value
    }

}