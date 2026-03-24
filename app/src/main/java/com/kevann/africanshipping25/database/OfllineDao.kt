package com.kevann.africanshipping25.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TruckGoodsDao {
    @Insert
    suspend fun insert(truckGood: TruckGoodsEntity)

    @Update
    suspend fun update(truckGood: TruckGoodsEntity)

    @Delete
    suspend fun delete(truckGood: TruckGoodsEntity)

    @Query("SELECT * FROM truck_goods_offline WHERE shipmentId = :shipmentId")
    fun getTruckGoodsByShipment(shipmentId: String): Flow<List<TruckGoodsEntity>>

    @Query("SELECT * FROM truck_goods_offline WHERE isSynced = 0")
    suspend fun getUnsyncedTruckGoods(): List<TruckGoodsEntity>

    @Query("UPDATE truck_goods_offline SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("DELETE FROM truck_goods_offline WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface StoreGoodsDao {
    @Insert
    suspend fun insert(storeGood: StoreGoodsEntity)

    @Update
    suspend fun update(storeGood: StoreGoodsEntity)

    @Delete
    suspend fun delete(storeGood: StoreGoodsEntity)

    @Query("SELECT * FROM store_goods_offline WHERE shipmentId = :shipmentId")
    fun getStoreGoodsByShipment(shipmentId: String): Flow<List<StoreGoodsEntity>>

    @Query("SELECT * FROM store_goods_offline WHERE isSynced = 0")
    suspend fun getUnsyncedStoreGoods(): List<StoreGoodsEntity>

    @Query("UPDATE store_goods_offline SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("DELETE FROM store_goods_offline WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface LoadingListDao {
    @Insert
    suspend fun insert(loadingList: LoadingListEntity)

    @Update
    suspend fun update(loadingList: LoadingListEntity)

    @Delete
    suspend fun delete(loadingList: LoadingListEntity)

    @Query("SELECT * FROM loading_lists_offline ORDER BY createdAt DESC")
    fun getAllLoadingLists(): Flow<List<LoadingListEntity>>

    @Query("SELECT * FROM loading_lists_offline WHERE isSynced = 0")
    suspend fun getUnsyncedLoadingLists(): List<LoadingListEntity>

    @Query("UPDATE loading_lists_offline SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("DELETE FROM loading_lists_offline WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM loading_lists_offline WHERE name LIKE '%' || :searchTerm || '%' ORDER BY createdAt DESC")
    fun searchLoadingLists(searchTerm: String): Flow<List<LoadingListEntity>>
}

@Dao
interface WarehouseGoodsDao {
    @Insert
    suspend fun insert(warehouseGoods: WarehouseGoodsEntity)

    @Update
    suspend fun update(warehouseGoods: WarehouseGoodsEntity)

    @Delete
    suspend fun delete(warehouseGoods: WarehouseGoodsEntity)

    @Query("SELECT * FROM warehouse_goods_offline WHERE loadingListId = :loadingListId")
    fun getWarehouseGoodsByLoadingList(loadingListId: String): Flow<List<WarehouseGoodsEntity>>

    @Query("SELECT * FROM warehouse_goods_offline WHERE isSynced = 0")
    suspend fun getUnsyncedWarehouseGoods(): List<WarehouseGoodsEntity>

    @Query("UPDATE warehouse_goods_offline SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("DELETE FROM warehouse_goods_offline WHERE id = :id")
    suspend fun deleteById(id: Int)
}
