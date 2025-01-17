package com.wekanmdb.storeinventory.ui.jobs

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.wekanmdb.storeinventory.app.apprealm
import com.wekanmdb.storeinventory.base.BaseViewModel
import com.wekanmdb.storeinventory.model.job.Jobs
import com.wekanmdb.storeinventory.model.store.Stores
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import org.bson.types.ObjectId
import java.util.*
import javax.inject.Inject

class JobsViewModel @Inject constructor() : BaseViewModel<JobsNavigator>() {
    var storeId: ObservableField<String> = ObservableField("")
    var userId: ObservableField<String> = ObservableField("")
    var storesItemList: RealmResults<Stores>? = null
    val storesresponseBody = MutableLiveData<Stores?>()
    val jobresponseBody = MutableLiveData<RealmResults<Jobs>?>()
    var jobsList: RealmResults<Jobs>? = null
    /**
     * This method will gives the store details based on StoreID
     */
    fun getStore() {
        val storeId = ObjectId(storeId.get().toString())
        storesItemList = apprealm?.where<Stores>()?.equalTo("_id", storeId)?.findAll()
        if (storesItemList?.isNotEmpty()!!) {
            val store: Stores? = storesItemList!![0]
            storesresponseBody.postValue(store!!)
        } else {
            storesresponseBody.value = null
        }
    }

    fun storeListener() {
        storesItemList?.addChangeListener { storedetails ->
            if (storedetails.isNotEmpty()) {
                val store: Stores? = storedetails[0]
                storesresponseBody.postValue(store!!)
            } else {
                storesresponseBody.value = null
            }
        }
    }

    /**
     * This method will gives the jobs list based on some query params
     */
    fun getJobList() {
        val userId = ObjectId(userId.get().toString())
        val storeId = ObjectId(storeId.get().toString())
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        jobsList = apprealm?.where<Jobs>()?.equalTo("createdBy._id", userId)?.equalTo("sourceStore._id", storeId)?.greaterThan( Jobs::datetime.name, calendar.time)?.sort( Jobs::datetime.name, Sort.ASCENDING)?.findAll()
            if (jobsList?.size!!>0) {
                jobresponseBody.postValue(jobsList)
            } else {
                jobresponseBody.value = null
            }
    }

    fun jobListener() {
        jobsList?.addChangeListener { jobList ->
            if (jobList.size > 0) {
                jobresponseBody.postValue(jobList)
            } else {
                jobresponseBody.value = null
            }
        }
    }


    fun addJobClick() {
        navigator.addJobClick()
    }
}