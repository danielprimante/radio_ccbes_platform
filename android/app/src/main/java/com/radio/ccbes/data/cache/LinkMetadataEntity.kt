package com.radio.ccbes.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "link_metadata")
data class LinkMetadataEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val domain: String?,
    val youtubeVideoId: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface LinkMetadataDao {
    @Query("SELECT * FROM link_metadata WHERE url = :url LIMIT 1")
    suspend fun getMetadata(url: String): LinkMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: LinkMetadataEntity)

    @Query("DELETE FROM link_metadata WHERE timestamp < :expiryTime")
    suspend fun deleteOldMetadata(expiryTime: Long)
}
