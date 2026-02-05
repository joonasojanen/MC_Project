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

// Developers web page was used as a base and help to create this file
// Also used different forums for debugging but nothing has been directly copied

//---ENTITY---
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: Int = 0,
    @ColumnInfo(name = "username") val username: String = "",
    @ColumnInfo(name = "image_path") val imagePath: String? = null
)

//---DAO--
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE uid = 0 LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

//---DATABASE---
@Database(entities = [UserProfileEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mc_project.db"
                )
                    .build()
                    .also { INSTANCE = it }
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
    private val dao = AppDatabase.getInstance(app).userProfileDao()

    private val profile: StateFlow<UserProfileEntity> =
        dao.observeProfile()
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
            dao.upsert(profile.value.copy(username = newName))
        }
    }

    fun saveImagePath(path: String) {
        viewModelScope.launch {
            dao.upsert(profile.value.copy(imagePath = path))
        }
    }
}