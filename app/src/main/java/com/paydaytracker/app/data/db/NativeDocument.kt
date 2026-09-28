package com.paydaytracker.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Keeps the non-tabular v2.4.10 state, including future backup fields, losslessly. */
@Entity(tableName = "native_document")
data class NativeDocument(@PrimaryKey val id: Int = 1, val json: String = "{}")
@Dao
interface NativeDocumentDao {
    @Query("SELECT * FROM native_document WHERE id = 1") fun observe(): Flow<NativeDocument?>
    @Query("SELECT * FROM native_document WHERE id = 1") suspend fun get(): NativeDocument?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(document: NativeDocument)
}
