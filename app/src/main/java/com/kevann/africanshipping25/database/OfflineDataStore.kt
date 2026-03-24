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

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getPrefs(context: Context? = null): SharedPreferences {
        return prefs ?: run {
            if (context == null) {
                throw IllegalStateException("OfflineDataStore not initialized. Call init(context) first.")
            }
            init(context)
            prefs!!
        }
    }

    // Truck Goods Methods
    fun saveTruckGood(good: TruckGoodsEntity, context: Context? = null) {
        val goods = getTruckGoods(context).toMutableList()
        goods.add(good.copy(id = goods.size + 1))
        val json = gson.toJson(goods)
        getPrefs(context).edit().putString(KEY_TRUCK_GOODS, json).apply()
    }

    fun getTruckGoods(context: Context? = null): List<TruckGoodsEntity> {
        val json = getPrefs(context).getString(KEY_TRUCK_GOODS, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<TruckGoodsEntity>>() {}.type) ?: emptyList()
    }

    fun getUnsyncedTruckGoods(context: Context? = null): List<TruckGoodsEntity> {
        return getTruckGoods(context).filter { !it.isSynced }
    }

    fun markTruckGoodAsSynced(id: Int, context: Context? = null) {
        val goods = getTruckGoods(context).toMutableList()
        val index = goods.indexOfFirst { it.id == id }
        if (index >= 0) {
            goods[index] = goods[index].copy(isSynced = true)
            val json = gson.toJson(goods)
            getPrefs(context).edit().putString(KEY_TRUCK_GOODS, json).apply()
        }
    }

    // Store Goods Methods
    fun saveStoreGood(good: StoreGoodsEntity, context: Context? = null) {
        val goods = getStoreGoods(context).toMutableList()
        goods.add(good.copy(id = goods.size + 1))
        val json = gson.toJson(goods)
        getPrefs(context).edit().putString(KEY_STORE_GOODS, json).apply()
    }

    fun getStoreGoods(context: Context? = null): List<StoreGoodsEntity> {
        val json = getPrefs(context).getString(KEY_STORE_GOODS, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<StoreGoodsEntity>>() {}.type) ?: emptyList()
    }

    fun getUnsyncedStoreGoods(context: Context? = null): List<StoreGoodsEntity> {
        return getStoreGoods(context).filter { !it.isSynced }
    }

    fun markStoreGoodAsSynced(id: Int, context: Context? = null) {
        val goods = getStoreGoods(context).toMutableList()
        val index = goods.indexOfFirst { it.id == id }
        if (index >= 0) {
            goods[index] = goods[index].copy(isSynced = true)
            val json = gson.toJson(goods)
            getPrefs(context).edit().putString(KEY_STORE_GOODS, json).apply()
        }
    }

    // Loading Lists Methods
    fun saveLoadingList(list: LoadingListEntity, context: Context? = null) {
        val lists = getLoadingLists(context).toMutableList()
        lists.add(list.copy(id = lists.size + 1))
        val json = gson.toJson(lists)
        getPrefs(context).edit().putString(KEY_LOADING_LISTS, json).apply()
    }

    fun getLoadingLists(context: Context? = null): List<LoadingListEntity> {
        val json = getPrefs(context).getString(KEY_LOADING_LISTS, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<LoadingListEntity>>() {}.type) ?: emptyList()
    }

    fun getUnsyncedLoadingLists(context: Context? = null): List<LoadingListEntity> {
        return getLoadingLists(context).filter { !it.isSynced }
    }

    fun markLoadingListAsSynced(id: Int, context: Context? = null) {
        val lists = getLoadingLists(context).toMutableList()
        val index = lists.indexOfFirst { it.id == id }
        if (index >= 0) {
            lists[index] = lists[index].copy(isSynced = true)
            val json = gson.toJson(lists)
            getPrefs(context).edit().putString(KEY_LOADING_LISTS, json).apply()
        }
    }

    // Warehouse Goods Methods
    fun saveWarehouseGood(good: WarehouseGoodsEntity, context: Context? = null) {
        val goods = getWarehouseGoods(context).toMutableList()
        goods.add(good.copy(id = goods.size + 1))
        val json = gson.toJson(goods)
        getPrefs(context).edit().putString(KEY_WAREHOUSE_GOODS, json).apply()
    }

    fun getWarehouseGoods(context: Context? = null): List<WarehouseGoodsEntity> {
        val json = getPrefs(context).getString(KEY_WAREHOUSE_GOODS, "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<WarehouseGoodsEntity>>() {}.type) ?: emptyList()
    }

    fun getUnsyncedWarehouseGoods(context: Context? = null): List<WarehouseGoodsEntity> {
        return getWarehouseGoods(context).filter { !it.isSynced }
    }

    fun markWarehouseGoodAsSynced(id: Int, context: Context? = null) {
        val goods = getWarehouseGoods(context).toMutableList()
        val index = goods.indexOfFirst { it.id == id }
        if (index >= 0) {
            goods[index] = goods[index].copy(isSynced = true)
            val json = gson.toJson(goods)
            getPrefs(context).edit().putString(KEY_WAREHOUSE_GOODS, json).apply()
        }
    }
}
