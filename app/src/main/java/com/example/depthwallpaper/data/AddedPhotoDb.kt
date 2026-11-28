package com.example.depthwallpaper.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/** A photo the user added on the Choose photo screen. The image itself is a copy kept in the app's own storage. */
@Entity(tableName = "added_photo")
data class AddedPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val addedAt: Long = System.currentTimeMillis(),
)

@Dao
interface AddedPhotoDao {
    @Query("SELECT * FROM added_photo ORDER BY addedAt DESC, id DESC")
    fun observeAll(): LiveData<List<AddedPhoto>>

    @Insert
    fun insert(photo: AddedPhoto): Long

    @Query("SELECT * FROM added_photo WHERE id = :id")
    fun find(id: Long): AddedPhoto?

    @Query("DELETE FROM added_photo WHERE id = :id")
    fun delete(id: Long)
}

@Database(entities = [AddedPhoto::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun addedPhotos(): AddedPhotoDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app.db")
                .build().also { instance = it }
        }
    }
}
