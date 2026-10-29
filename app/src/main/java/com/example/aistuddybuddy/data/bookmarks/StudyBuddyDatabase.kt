package com.example.aistuddybuddy.data.bookmarks

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(entities = [BookmarkEntity::class], version = 1, exportSchema = false)
@TypeConverters(BookmarkConverters::class)
abstract class StudyBuddyDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}

class BookmarkConverters {
    @TypeConverter
    fun fromOptions(options: List<String>): String = Json.encodeToString(options)

    @TypeConverter
    fun toOptions(value: String): List<String> = runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}
