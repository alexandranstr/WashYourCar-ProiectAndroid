package com.example.washyourcar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        Appointment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun carDao(): CarDao
    abstract fun carWashDao(): CarWashDao
    abstract fun carWashServiceDao(): CarWashServiceDao
    abstract fun ownerDao(): OwnerDao
    abstract fun employeeDao(): EmployeeDao

    abstract fun appointmentDao(): AppointmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wash_your_car_database"
                )
                    .fallbackToDestructiveMigration()
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

        private fun populateInitialData(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (1, 'Exterior Wash', 'Standard exterior foam wash and microfiber drying.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (2, 'Interior Cleaning', 'Deep vacuuming, dashboard dusting, and window cleaning.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (3, 'Detailing', 'Professional stain removal and seat conditioning.')")
                db.execSQL("INSERT OR REPLACE INTO services (serviceId, name, description) VALUES (4, 'Polish', 'Fine scratch removal and paint gloss restoration.')")

                db.execSQL("INSERT OR REPLACE INTO owners (firebaseUid, firstName, lastName, email, phoneNumber) VALUES ('owner_uid_1', 'Popescu', 'Ionut', 'owner1@gmail.com', '0722111222')")
                db.execSQL("INSERT OR REPLACE INTO owners (firebaseUid, firstName, lastName, email, phoneNumber) VALUES ('owner_uid_2', 'Andrei', 'Vasile', 'owner2@gmail.com', '0733444555')")

                db.execSQL("""
                    INSERT OR REPLACE INTO car_washes (carWashId, ownerId, name, address, city, phoneNumber, rating, accessCode, openTime, closeTime) 
                    VALUES (101, 'owner_uid_1', 'Eco Clean Auto Brasov', 'Calea Bucuresti Nr. 45', 'Brasov', '0268123456', 4.8, 'ECO101', 800, 2000)
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO car_washes (carWashId, ownerId, name, address, city, phoneNumber, rating, accessCode, openTime, closeTime) 
                    VALUES (102, 'owner_uid_2', 'JetX Express Ghimbav', 'Str. Aeroportului Nr. 2', 'Ghimbav', '0268987654', 4.5, 'JET102', 700, 2200)
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO car_washes (carWashId, ownerId, name, address, city, phoneNumber, rating, accessCode, openTime, closeTime) 
                    VALUES (103, 'owner_uid_1', 'Premium Detailing Coresi', 'Str. Zaharia Stancu Nr. 12', 'Brasov', '0268555666', 5.0, 'PREM103', 900, 2100)
                """.trimIndent())

                db.execSQL("""
                    INSERT OR REPLACE INTO car_washes (carWashId, ownerId, name, address, city, phoneNumber, rating, accessCode, openTime, closeTime) 
                    VALUES (104, 'owner_uid_2', 'Fast Bubbles Tractorul', 'Str. 13 Decembrie Nr. 88', 'Brasov', '0268777888', 4.2, 'FAST104', 800, 2200)
                """.trimIndent())

                db.execSQL("INSERT OR REPLACE INTO employees (firebaseUid, carWashId, firstName, lastName, email, phoneNumber, accessToken) VALUES ('emp_uid_1', 101, 'Mihai', 'George', 'mihai@wash.com', '0745111111', 'TOKEN_1')")
                db.execSQL("INSERT OR REPLACE INTO employees (firebaseUid, carWashId, firstName, lastName, email, phoneNumber, accessToken) VALUES ('emp_uid_2', 102, 'Elena', 'Radu', 'elena@wash.com', '0755222222', 'TOKEN_2')")

                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (101, 1, 35.0, 15)")
                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (101, 2, 30.0, 20)")
                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (101, 3, 150.0, 60)")

                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (102, 1, 40.0, 12)")
                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (102, 2, 35.0, 25)")
                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (102, 4, 200.0, 90)")

                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (103, 3, 180.0, 70)")
                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (103, 4, 250.0, 120)")

                db.execSQL("INSERT OR REPLACE INTO car_wash_services (carWashId, serviceId, price, duration) VALUES (104, 1, 30.0, 10)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}