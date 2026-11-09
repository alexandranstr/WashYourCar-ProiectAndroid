import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.washyourcar.data.Converters
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
        Review::class,
        SelfWash::class,
        SelfService::class,
        SelfWashService::class,
        AutoWash::class,
        AutoService::class,
        AutoWashService::class,
        LoyaltyCard::class
    ],
    version = 7,
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

    abstract fun selfWashDao(): SelfWashDao

    abstract fun selfWashServiceDao(): SelfWashServiceDao

    abstract fun autoWashDao(): AutoWashDao

    abstract fun autoWashServiceDao(): AutoWashServiceDao

    abstract fun loyaltyCardDao(): LoyaltyCardDao

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

                    .fallbackToDestructiveMigration(true)
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
                seedAll(db)
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            CoroutineScope(Dispatchers.IO).launch {
                seedAll(db)
            }
        }

        private fun seedAll(db: SupportSQLiteDatabase) {
            seedClassicServices(db)
            seedSelfServices(db)
            seedAutoServices(db)
        }

        private fun isTableEmpty(db: SupportSQLiteDatabase, table: String): Boolean {
            val cursor = db.query("SELECT COUNT(*) FROM $table")
            val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()
            return count == 0
        }

        private fun seedClassicServices(db: SupportSQLiteDatabase) {
            try {
                if (!isTableEmpty(db, "services")) return

                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (1, 'Exterior Wash', 'Standard exterior foam wash and microfiber drying.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (2, 'Interior Cleaning', 'Deep vacuuming, dashboard dusting, and window cleaning.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (3, 'Detailing', 'Professional stain removal and seat conditioning.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (4, 'Polish', 'Fine scratch removal and paint gloss restoration.')")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun seedSelfServices(db: SupportSQLiteDatabase) {
            try {
                if (!isTableEmpty(db, "self_services")) return

                db.execSQL("INSERT OR REPLACE INTO self_services (selfServiceId, name, description) VALUES (1, 'Warm Water & Shampoo Wash', 'Hot water wash with shampoo for removing dirt.')")
                db.execSQL("INSERT OR REPLACE INTO self_services (selfServiceId, name, description) VALUES (2, 'Active Foam / Rim Cleaner', 'Active foam for the body and a dedicated rim cleaner.')")
                db.execSQL("INSERT OR REPLACE INTO self_services (selfServiceId, name, description) VALUES (3, 'Cold Water Rinse', 'Cold water rinse to remove foam and shampoo.')")
                db.execSQL("INSERT OR REPLACE INTO self_services (selfServiceId, name, description) VALUES (4, 'Liquid Wax / Protection', 'Liquid wax that protects the paint and adds shine.')")
                db.execSQL("INSERT OR REPLACE INTO self_services (selfServiceId, name, description) VALUES (5, 'Spot Free Final Rinse', 'Final rinse with demineralized / reverse osmosis water for a spot-free finish.')")
                db.execSQL("INSERT OR REPLACE INTO self_services (selfServiceId, name, description) VALUES (6, 'Air Blower / Insect Remover', 'Air jet holder and bug remover station.')")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun seedAutoServices(db: SupportSQLiteDatabase) {
            try {
                if (!isTableEmpty(db, "auto_services")) return

                db.execSQL("INSERT OR REPLACE INTO auto_services (autoServiceId, name, description) VALUES (1, 'Express / Basic Wash', 'Fast and economical wash program.')")
                db.execSQL("INSERT OR REPLACE INTO auto_services (autoServiceId, name, description) VALUES (2, 'Classic Wash', 'Standard wash program with shampoo and rinse.')")
                db.execSQL("INSERT OR REPLACE INTO auto_services (autoServiceId, name, description) VALUES (3, 'Wax Wash / Deluxe', 'Complete wash with wax protection and drying.')")
                db.execSQL("INSERT OR REPLACE INTO auto_services (autoServiceId, name, description) VALUES (4, 'Ultimate / Intensive (Premium VIP)', 'Premium program with intensive cleaning, wax and full drying.')")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}