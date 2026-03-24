package com.kevann.africanshipping25.database

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object OfflineDataStore {
    private const val PREFS_NAME = "offline_shipping_db"
    private const val KEY_TRUCK_GOODS = "truck_goods"
    private const val KEY_STORE_GOODS = "store_goods"
    private const val KEY_LOADING_LISTS = "loading_lists"
    private const val KEY_WAREHOUSE_GOODS = "warehouse_goods"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Truck Goods Methods
    fun saveTruckGood(good: TruckGoodsEntity) {
        val goods = getTruckGoods().toMutableList()
        goods.add(good.copy(id = goods.size + 1))
        val json = gson.toJson(goods)
        prefs.edit().putString(KEY_TRUCK_GOODS, json).apply()
    }

    fun getTruckGoods(): List<TruckGoodsEntity> {
        val json = prefs.getString(KEY_TRUCK_GOODS, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<TruckGoodsEntity>>() {}.type) ?: emptyList()
    }

    fun getUnsyncedTruckGoods(): List<TruckGoodsEntity> {
        return getTruckGoods().filter { !it.isSynced }
    }

    fun markTruckGoodAsSynced(id: Int) {
        val goods = getTruckGoods().toMutableList()
        val index = goods.indexOfFirst { it.id == id }
        if (index >= 0) {
            goods[index] = goods[index].copy(isSynced = true)
            val json = gson.toJson(goods)
            prefs.edit().putString(KEY_TRUCK_GOODS, json).apply()
        }
    }

    // Store Goods Methods
    fun saveStoreGood(good: StoreGoodsEntity) {
        val goods = getStoreGoods().toMutableList()
        goods.add(good.copy(id = goods.size + 1))
        val json = gson.toJson(goods)
        prefs.edit().putString(KEY_STORE_GOODS, json).apply()
    }

    fun getStoreGoods(): List<StoreGoodsEntity> {
        val json = prefs.getString(KEY_STORE_GOODS, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<StoreGoodsEntity>>() {}.type) ?: emptyList()
    }

    fun getUnsyncedStoreGoods(): List<StoreGoodsEntity> {
        return getStoreGoods().filter { !it.isSynced }
    }

    fun markStoreGoodAsSynced(id: Int) {
        val goods = getStoreGoods().toMutableList()
        val index = goods.indexOfFirst { it.id == id }
        if (index >= 0) {
            goods[index] = goods[index].copy(isSynced = true)
            val json = gson.toJson(goods)
            prefs.edit().putString(KEY_STORE_GOODS, json).apply()
        }
    }

    // Loading Lists Methods
    fun saveLoadingList(list: LoadingListEntity) {
        val lists = getLoadingLists().toMutableList()
        lists.add(list.copy(id = lists.size + 1))
        val json = gson.toJson(lists)
        prefs.edit().putString(KEY_LOADING_LISTS, json).apply()
    }

    fun getLoadingLists(): List<LoadingListEntity> {
        val json = prefs.getString(KEY_LOADING_LISTS, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<LoadingListEntity>>() {}.type) ?: emptyList()
    }

    fun getUnsyncedLoadingLists(): List<LoadingListEntity> {
        return getLoadingLists().filter { !it.isSynced }
    }

    fun markLoadingListAsSynced(id: Int) {
        val lists = getLoadingLists().toMutableList()
        val index = lists.indexOfFirst { it.id == id }
        if (index >= 0) {
            lists[index] = lists[index].copy(isSynced = true)
            val json = gson.toJson(lists)
            prefs.edit().putString(KEY_LOADING_LISTS, json).apply()
        }
    }

    // Warehouse Goods Methods
    fun saveWarehouseGood(good: WarehouseGoodsEntity) {
        val goods = getWarehouseGoods().toMutableList()
        goods.add(good.copy(id = goods.size + 1))
        val json = gson.toJson(goods)
        prefs.edit().putString(KEY_WAREHOUSE_GOODS, json).apply()
    }

    fun getWarehouseGoods(): List<WarehouseGoodsEntity> {
        val json = prefs.getString(KEY_WAREHOUSE_GOODS, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<WarehouseGoodsEntity>>() {}.type) ?: emptyList()
    }

    fun getUnsyncedWarehouseGoods(): List<WarehouseGoodsEntity> {
        return getWarehouseGoods().filter { !it.isSynced }
    }

    fun markWarehouseGoodAsSynced(id: Int) {
        val goods = getWarehouseGoods().toMutableList()
        val index = goods.indexOfFirst { it.id == id }
        if (index >= 0) {
            goods[index] = goods[index].copy(isSynced = true)
            val json = gson.toJson(goods)
            prefs.edit().putString(KEY_WAREHOUSE_GOODS, json).apply()
        }
    }
}
