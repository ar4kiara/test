package com.arakiara.remindervoice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arakiara.remindervoice.model.ReminderDays
import com.arakiara.remindervoice.model.TtsStyle

@Database(
    entities = [ReminderEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var instance: ReminderDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE reminders ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT '${ReminderDays.encode(ReminderDays.everyDay)}'",
                )
                database.execSQL(
                    "ALTER TABLE reminders ADD COLUMN ttsStyle TEXT NOT NULL DEFAULT '${TtsStyle.TEGAS.name}'",
                )
                database.execSQL(
                    "ALTER TABLE reminders ADD COLUMN notificationSoundUri TEXT",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE reminders ADD COLUMN ttsVoiceName TEXT",
                )
            }
        }

        fun get(context: Context): ReminderDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_voice.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
