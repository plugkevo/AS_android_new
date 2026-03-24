package com.kevann.africanshipping25.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TruckGoodsEntity::class, StoreGoodsEntity::class, LoadingListEntity::class, WarehouseGoodsEntity::class],
    version = 1
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun truckGoodsDao(): TruckGoodsDao
    abstract fun storeGoodsDao(): StoreGoodsDao
    abstract fun loadingListDao(): LoadingListDao
    abstract fun warehouseGoodsDao(): WarehouseGoodsDao

    companion object {
        @Volatile
        private var instance: OfflineDatabase? = null

        fun getInstance(context: Context): OfflineDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "offline_shipping_db"
                ).build().also { instance = it }
            }
        }

        fun getDatabase(context: Context): OfflineDatabase {
            return getInstance(context)
        }
    }
}
