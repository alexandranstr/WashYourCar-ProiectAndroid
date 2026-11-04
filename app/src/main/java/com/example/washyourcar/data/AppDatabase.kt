package com.example.washyourcar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.washyourcar.data.dao.*
import com.example.washyourcar.data.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Customer::class,
        Car::class,
        CarWash::class,
        Service::class,
        CarWashService::class,
        Owner::class,
        Employee::class,
        Appointment::class,
        Review::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun carDao(): CarDao
    abstract fun carWashDao(): CarWashDao
    abstract fun carWashServiceDao(): CarWashServiceDao
    abstract fun ownerDao(): OwnerDao
    abstract fun employeeDao(): EmployeeDao

    abstract fun appointmentDao(): AppointmentDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reviews` (
                        `reviewId` TEXT NOT NULL,
                        `carWashId` INTEGER NOT NULL,
                        `customerId` TEXT NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `rating` INTEGER NOT NULL,
                        `comment` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`reviewId`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wash_your_car_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(AppDatabaseCallback())
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                populateInitialData(db)
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            CoroutineScope(Dispatchers.IO).launch {
                seedInitialDataIfEmpty(db)
            }
        }

        private fun populateInitialData(db: SupportSQLiteDatabase) {
            seedInitialDataIfEmpty(db)
        }

        private fun seedInitialDataIfEmpty(db: SupportSQLiteDatabase) {
            try {
                val cursor = db.query("SELECT COUNT(*) as count FROM services")
                val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                cursor.close()

                if (count > 0) return

                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (1, 'Exterior Wash', 'Standard exterior foam wash and microfiber drying.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (2, 'Interior Cleaning', 'Deep vacuuming, dashboard dusting, and window cleaning.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (3, 'Detailing', 'Professional stain removal and seat conditioning.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (4, 'Polish', 'Fine scratch removal and paint gloss restoration.')")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}