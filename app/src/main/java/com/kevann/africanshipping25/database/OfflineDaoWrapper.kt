package com.kevann.africanshipping25.database

import kotlinx.coroutines.flow.Flow

class OfflineDao(
    private val truckGoodsDao: TruckGoodsDao,
    private val storeGoodsDao: StoreGoodsDao,
    private val loadingListDao: LoadingListDao,
    private val warehouseGoodsDao: WarehouseGoodsDao
) {
    // Truck Goods Methods
    suspend fun insertTruckGoods(truckGood: TruckGoodsEntity) = truckGoodsDao.insert(truckGood)
    suspend fun updateTruckGoods(truckGood: TruckGoodsEntity) = truckGoodsDao.update(truckGood)
    suspend fun deleteTruckGoods(truckGood: TruckGoodsEntity) = truckGoodsDao.delete(truckGood)
    fun getTruckGoodsByShipment(shipmentId: String): Flow<List<TruckGoodsEntity>> = truckGoodsDao.getTruckGoodsByShipment(shipmentId)
    suspend fun getUnsyncedTruckGoods(): List<TruckGoodsEntity> = truckGoodsDao.getUnsyncedTruckGoods()
    suspend fun markTruckGoodsAsSynced(id: Int) = truckGoodsDao.markAsSynced(id)
    suspend fun deleteTruckGoodsById(id: Int) = truckGoodsDao.deleteById(id)

    // Store Goods Methods
    suspend fun insertStoreGoods(storeGood: StoreGoodsEntity) = storeGoodsDao.insert(storeGood)
    suspend fun updateStoreGoods(storeGood: StoreGoodsEntity) = storeGoodsDao.update(storeGood)
    suspend fun deleteStoreGoods(storeGood: StoreGoodsEntity) = storeGoodsDao.delete(storeGood)
    fun getStoreGoodsByShipment(shipmentId: String): Flow<List<StoreGoodsEntity>> = storeGoodsDao.getStoreGoodsByShipment(shipmentId)
    suspend fun getUnsyncedStoreGoods(): List<StoreGoodsEntity> = storeGoodsDao.getUnsyncedStoreGoods()
    suspend fun markStoreGoodsAsSynced(id: Int) = storeGoodsDao.markAsSynced(id)
    suspend fun deleteStoreGoodsById(id: Int) = storeGoodsDao.deleteById(id)

    // Loading List Methods
    suspend fun insertLoadingList(loadingList: LoadingListEntity) = loadingListDao.insert(loadingList)
    suspend fun updateLoadingList(loadingList: LoadingListEntity) = loadingListDao.update(loadingList)
    suspend fun deleteLoadingList(loadingList: LoadingListEntity) = loadingListDao.delete(loadingList)
    fun getAllLoadingLists(): Flow<List<LoadingListEntity>> = loadingListDao.getAllLoadingLists()
    suspend fun getUnsyncedLoadingLists(): List<LoadingListEntity> = loadingListDao.getUnsyncedLoadingLists()
    suspend fun markLoadingListAsSynced(id: Int) = loadingListDao.markAsSynced(id)
    suspend fun deleteLoadingListById(id: Int) = loadingListDao.deleteById(id)
    fun searchLoadingLists(searchTerm: String): Flow<List<LoadingListEntity>> = loadingListDao.searchLoadingLists(searchTerm)

    // Warehouse Goods Methods
    suspend fun insertWarehouseGoods(warehouseGoods: WarehouseGoodsEntity) = warehouseGoodsDao.insert(warehouseGoods)
    suspend fun updateWarehouseGoods(warehouseGoods: WarehouseGoodsEntity) = warehouseGoodsDao.update(warehouseGoods)
    suspend fun deleteWarehouseGoods(warehouseGoods: WarehouseGoodsEntity) = warehouseGoodsDao.delete(warehouseGoods)
    fun getWarehouseGoodsByLoadingList(loadingListId: String): Flow<List<WarehouseGoodsEntity>> = warehouseGoodsDao.getWarehouseGoodsByLoadingList(loadingListId)
    suspend fun getUnsyncedWarehouseGoods(): List<WarehouseGoodsEntity> = warehouseGoodsDao.getUnsyncedWarehouseGoods()
    suspend fun markWarehouseGoodsAsSynced(id: Int) = warehouseGoodsDao.markAsSynced(id)
    suspend fun deleteWarehouseGoodsById(id: Int) = warehouseGoodsDao.deleteById(id)
}
