package com.wekanmdb.storeinventory.ui.login

import android.content.Context
import android.content.Intent
import android.util.Log
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.wekanmdb.storeinventory.R
import com.wekanmdb.storeinventory.app.*
import com.wekanmdb.storeinventory.base.BaseActivity
import com.wekanmdb.storeinventory.data.AppConstants
import com.wekanmdb.storeinventory.databinding.ActivityLoginBinding
import com.wekanmdb.storeinventory.ui.deliveryjob.DeliveryJobActivity
import com.wekanmdb.storeinventory.ui.home.HomeActivity
import com.wekanmdb.storeinventory.ui.signup.SignupActivity
import com.wekanmdb.storeinventory.utils.EncrytionUtils.getExistingKey
import com.wekanmdb.storeinventory.utils.EncrytionUtils.getNewKey
import com.wekanmdb.storeinventory.utils.RealmUtils.getRealmconfig
import com.wekanmdb.storeinventory.utils.UiUtils
import com.wekanmdb.storeinventory.utils.UiUtils.afterTextChanged
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_signup.*
import org.bson.Document
import org.bson.types.ObjectId

class LoginActivity : BaseActivity<ActivityLoginBinding>(), LoginNavigator {

    companion object {
        fun getCallingIntent(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }

    private lateinit var activityLoginBinding: ActivityLoginBinding
    private lateinit var loginViewModel: LoginViewModel
    private var storeId: ObjectId? = null

    override fun getLayoutId(): Int = R.layout.activity_login
    var showPwd = true
    override fun initView(mViewDataBinding: ViewDataBinding?) {
        activityLoginBinding = mViewDataBinding as ActivityLoginBinding
        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        activityLoginBinding.loginViewModel = loginViewModel
        loginViewModel.navigator = this
        activityLoginBinding.editTextPassword.afterTextChanged {
            if(it.isNotEmpty()){
                View.VISIBLE
            }else{
                View.INVISIBLE
            }.let {visibility->
                activityLoginBinding.imageView43.visibility=visibility
            }

        }
        /*
        * afterTextChanged listening the Email. Once Email get validated the Edit text BG color has been changed
        * */
        activityLoginBinding.editTextTextPersonName3.afterTextChanged {

            if (UiUtils.isValidEmail(loginViewModel.useremail.get().toString().trim())) {
                ContextCompat.getDrawable(this, R.drawable.edittext_bg_yellow)
            }else{
                ContextCompat.getDrawable(this, R.drawable.edittext_bg_gray)
            }.let {
                activityLoginBinding.editTextTextPersonName3.background=it
            }
        }

    }

    // Userlogin AuthenticateUser verify
    override fun loginClick() {
        if (loginViewModel.useremail.get().isNullOrEmpty()) {
            showToast("please enter the username")
            return
        }

        if (loginViewModel.userpassword.get().isNullOrEmpty()) {
            showToast("please enter the password")
            return
        }

        progressBar.visibility = View.VISIBLE
        loginViewModel.getAuthenticateUser()
            .observe(this, { authenticateUser ->
                // Query results are AuthenticateUser
                if (authenticateUser.isSuccess) {
                    user = taskApp.currentUser()
                    key = getExistingKey(appPreference)
                    if (key == null) {
                        key = getNewKey(appPreference)
                    }
                    asyncConfig()
                } else {
                    progressBar.visibility = View.GONE
                    showToast(""+authenticateUser.error)
                }
            })
    }

    override fun signupClick() {
        startActivity(SignupActivity.getCallingIntent(this@LoginActivity))

    }

    override fun showPasswordClick() {
        activityLoginBinding.editTextPassword.requestFocus()
        if (showPwd) {
            showPwd = false
            showPassword(activityLoginBinding.editTextPassword, activityLoginBinding.imageView43)
        } else {
            showPwd = true
            hidePassword(activityLoginBinding.editTextPassword, activityLoginBinding.imageView43)
        }
    }

    // Use the config
    fun asyncConfig() {
        val stores=user?.customData?.get("stores")
        if(stores != null) {
            storeId = (stores as ArrayList<ObjectId>).first()
        }
        Realm.getInstanceAsync(
            getRealmconfig(AppConstants.PARTITIONVALUE),
            object : Realm.Callback() {
                override fun onSuccess(realm: Realm) {
                    apprealm = realm
                    getSyncData()
                }
            })
        if (storeId != null) {
            Realm.getInstanceAsync(getRealmconfig(AppConstants.INVENTORYPARTITIONVALUE +  storeId),
                object : Realm.Callback() {
                    override fun onSuccess(realm: Realm) {
                        storerealm = realm
                        getSyncData()
                    }
                })
        }

    }

    fun getSyncData() {
        if (storeId != null && apprealm != null && storerealm != null) {
            user?.let { login(it.customData) }
        } else if (storeId == null && apprealm != null) {
            user?.let { login(it.customData) }
        }
    }

    private fun login(customData: Document?) {
        try {
            if(customData != null&&customData.size>0) {
                Log.i("login", customData.toString())
                progressBar.visibility = View.GONE
                val customDataString = customData.getString("userRole")
                if (customDataString.equals("store-user", true)) {
                    startActivity(HomeActivity.getCallingIntent(this))
                } else {
                    startActivity(DeliveryJobActivity.getCallingIntent(this))
                }
            }else{
                showToast("Invalid user credentials")
            }
        } catch (e: Exception) {

        }

    }
    private fun showPassword(password: EditText, imageView: ImageView) {
        password.transformationMethod = SingleLineTransformationMethod()
        imageView.setImageDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                R.mipmap.pwd_show
            )
        )
        password.setSelection(password.text!!.length)
    }

    private fun hidePassword(password: EditText, imageView: ImageView) {
        password.transformationMethod = PasswordTransformationMethod()
        imageView.setImageDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                R.mipmap.pwd_hide
            )
        )
        password.setSelection(password.text!!.length)
    }
}
