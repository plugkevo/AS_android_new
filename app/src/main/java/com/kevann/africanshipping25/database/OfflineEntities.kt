package com.kevann.africanshipping25.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "truck_goods_offline")
data class TruckGoodsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shipmentId: String,
    val name: String,
    val goodsNumber: String,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "store_goods_offline")
data class StoreGoodsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shipmentId: String,
    val name: String,
    val storeLocation: String,
    val goodsNumber: String,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "loading_lists_offline")
data class LoadingListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val origin: String,
    val destination: String,
    val extraDetails: String = "",
    val status: String = "New",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "warehouse_goods_offline")
data class WarehouseGoodsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val loadingListId: String,
    val goodNo: String,
    val senderName: String,
    val phoneNumber: String,
    val date: String,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
