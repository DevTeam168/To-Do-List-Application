package com.example.todolist.view.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.todolist.R
import com.example.todolist.data.model.SnackBarData
import com.example.todolist.data.model.TaskModel
import com.example.todolist.databinding.ActivityMainBinding
import com.example.todolist.databinding.CustomSnackbarBinding
import com.example.todolist.util.Constant
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private fun MaterialButton.preventSpamClick(action: (MaterialButton) -> Unit) {
    this.setOnClickListener {
        it.isEnabled = false // Disable the button
        action(this)
        Handler(Looper.getMainLooper()).postDelayed({
            it.isEnabled = true
        }, 1000) // 1000 milliseconds = 1 second
    }
}

private fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mAllTaskItems: ArrayList<TaskModel>
    private lateinit var mMatchTasks: ArrayList<TaskModel>
    private lateinit var mAdapter: AllTaskRecViewAdapter
    private var mSharePrefs: SharedPreferences? = null
    private var mEditor: SharedPreferences.Editor? = null
    private var mState: String = Constant.ADD
    private var mUpdateTask: String = Constant.EMPTY
    private var mPreviousTask: String = Constant.EMPTY
    private var mIsPossible: Boolean = false
    private var mUpdatePosition: Int = -1
    private var matchCount: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        init()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    private fun init() {
        mAllTaskItems = ArrayList()
        mMatchTasks = ArrayList()
        clickListener()
        initializeSharePrefs()
        initializeRecyclerView()
    }

    @SuppressLint("CommitPrefEdits")
    private fun initializeSharePrefs() {
        mSharePrefs = getSharedPreferences(Constant.PREFS_NAME, Context.MODE_PRIVATE)
        mEditor = mSharePrefs?.edit()
        val isHasData = mSharePrefs?.contains(Constant.KEY_ALL_TASKS)

        if (isHasData == true) {
            getDataFromLocal()
        }
    }

    private fun getDataFromLocal() {
        val json = mSharePrefs?.getString(
            Constant.KEY_ALL_TASKS,
            resources.getString(R.string.invalid_data)
        )

        if (json != resources.getString(R.string.invalid_data)) {
            val type = object : TypeToken<ArrayList<TaskModel>>() {}.type
            mAllTaskItems = Gson().fromJson(json, type)

            if (mAllTaskItems.size == 0) {
                mBinding.constraintLayoutNoResult.visibility = View.VISIBLE
                mBinding.textViewEmptyItem.text = getString(R.string.no_item)
            }
        }
    }

    private fun initializeRecyclerView() {
        mAdapter = AllTaskRecViewAdapter()
        mAdapter.setAllTaskItem(mAllTaskItems)
        mAdapter.setListener(object : AllTaskRecViewAdapter.ItemClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onItemClick(
                tag: String,
                position: Int,
                holder: AllTaskRecViewAdapter.ViewHolder
            ) {
                when (tag) {
                    Constant.DELETE -> {
                        if (matchCount == 0) {
                            mAllTaskItems.removeAt(position)
                        } else {
                            mAllTaskItems.remove(mMatchTasks[0])
                            mAdapter.setAllTaskItem(mAllTaskItems)
                            mMatchTasks.clear()
                            resetUi()
                        }
                        mAdapter.notifyDataSetChanged()
                        savedToSharedPrefs()
                    }

                    Constant.EDIT -> {
                        if (mState != Constant.UPDATE) {
                            editItemTask(position)
                        }
                    }

                    Constant.MARK -> {
                        markItemTask(position)
                    }
                }
            }
        })

        mBinding.recyclerViewAllTasks.adapter = mAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun markItemTask(position: Int) {
        if (matchCount == 1) {
            mMatchTasks[0].isComplete = !mMatchTasks[0].isComplete
        } else {
            mAllTaskItems[position].isComplete = !mAllTaskItems[position].isComplete
        }
        mAdapter.notifyDataSetChanged()
        savedToSharedPrefs()
    }

    private fun editItemTask(position: Int) {
        mState = Constant.UPDATE
        mPreviousTask = mAllTaskItems[position].task
        mUpdatePosition = position
        mBinding.inputEdtAddTask.apply {
            setText(mPreviousTask)
            requestFocus()
            showKeyboard()
            this.text?.length?.let { setSelection(it) }
        }
        mBinding.buttonAdd.text = getString(R.string.update)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clickListener() {
        mBinding.constraintLayout.setOnClickListener {
            hideKeyboard(this@MainActivity, mBinding.root)
            mBinding.inputEdtAddTask.clearFocus()

            if (mState == Constant.UPDATE) {
                mBinding.buttonAdd.text = getString(R.string.add)
                mState = Constant.ADD
                mBinding.inputEdtAddTask.setText(Constant.EMPTY)
                mAdapter.setAllTaskItem(mAllTaskItems)
                mAdapter.notifyDataSetChanged()
            }
        }

        mBinding.inputEdtAddTask.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mBinding.inputEdtAddTask.clearFocus()
            }
            false
        }

        mBinding.buttonAdd.preventSpamClick {
            val text = mBinding.inputEdtAddTask.text

            when (mState) {
                Constant.ADD -> {
                    if (mIsPossible) {
                        addItemToList(text)
                        resetUi()
                    } else {
                        if (text.toString().trim().isNotEmpty()) {
                            showSnackBar(
                                mBinding.root,
                                SnackBarData(
                                    R.drawable.ic_info_circle,
                                    R.drawable.gradient_red,
                                    getString(R.string.invalid),
                                    getString(R.string.already_exist)
                                )
                            )
                        }
                    }
                }

                Constant.UPDATE -> {
                    if (mIsPossible) {
                        updateTheTask()
                        savedToSharedPrefs()
                        resetUi()

                    } else {
                        showSnackBar(
                            mBinding.root,
                            SnackBarData(
                                R.drawable.ic_info_circle,
                                R.drawable.gradient_red,
                                getString(R.string.invalid),
                                getString(R.string.nothing_change)
                            )
                        )
                    }
                }
            }
        }

        mBinding.inputEdtAddTask.doAfterTextChanged { text ->
            val newTask = text.toString().trim()
            mMatchTasks = mAllTaskItems.filter { it.task.equals(newTask, ignoreCase = true) }
                .map { it } as ArrayList
            matchCount = mMatchTasks.size

            if (mMatchTasks.size == 0) {
                if (newTask.trim().isNotEmpty()) {
                    mIsPossible = true
                    mBinding.constraintLayoutNoResult.visibility = View.VISIBLE
                    mAdapter.setAllTaskItem(mAllTaskItems)
                    mAdapter.notifyDataSetChanged()

                    //for option 'update' the task
                    if (mState == Constant.UPDATE) {
                        mUpdateTask = newTask.trim()
                        mIsPossible = !mUpdateTask.equals(mPreviousTask, ignoreCase = true)
                    }

                } else {
                    mBinding.constraintLayoutNoResult.visibility = View.GONE
                    if (mState == Constant.UPDATE) {
                        mIsPossible = false
                    }
                }

            } else {
                mIsPossible = false
                mBinding.constraintLayoutNoResult.visibility = View.GONE
                mAdapter.setAllTaskItem(mMatchTasks)
                mAdapter.notifyDataSetChanged()
            }

        }
    }

    private fun resetUi() {
        mBinding.inputEdtAddTask.setText(Constant.EMPTY)
        mBinding.constraintLayoutNoResult.visibility = View.GONE
        mBinding.recyclerViewAllTasks.visibility = View.VISIBLE

        //additional condition when user in UPDATE option
        if (mState == Constant.UPDATE) {
            mState = Constant.ADD
            mBinding.buttonAdd.text = getString(R.string.add)
            mBinding.inputEdtAddTask.clearFocus()
            hideKeyboard(this@MainActivity, mBinding.root)
        }
        mIsPossible = false
    }

    private fun updateTheTask() {
        mAllTaskItems[mUpdatePosition].task = mUpdateTask
        mAllTaskItems[mUpdatePosition].dateTime = getCurrentDateTime()
        mAdapter.notifyItemChanged(mUpdatePosition)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addItemToList(text: Editable?) {
        if (text.toString().trim().isNotEmpty()) {
            val dateTime = getCurrentDateTime()
            mAllTaskItems.add(0, TaskModel(text.toString().trim(), dateTime, false))
            mAdapter.notifyItemInserted(0)
            savedToSharedPrefs()
        }
    }

    private fun savedToSharedPrefs() {
        val gson = Gson()
        val jsonData = gson.toJson(mAllTaskItems)

        mEditor?.putString(Constant.KEY_ALL_TASKS, jsonData)
        mEditor?.apply()
    }

    private fun getCurrentDateTime(): String {
        val dateTimeFormat = SimpleDateFormat(Constant.DATE_TIME_PATTERN, Locale.getDefault())
        return dateTimeFormat.format(Date())
    }

    private fun hideKeyboard(activity: Activity?, view: View) {
        val inputManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showSnackBar(view: View, snackBarData: SnackBarData) {
        val customSnackBar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
        val snackBarLayout = customSnackBar.view as Snackbar.SnackbarLayout
        val customSnackBarBinding = CustomSnackbarBinding.inflate(layoutInflater)
        snackBarLayout.addView(customSnackBarBinding.root, 0)
        snackBarLayout.setBackgroundColor(Color.TRANSPARENT)

        //set content data of snackBar
        customSnackBarBinding.ivSnackBarIcon.setImageResource(snackBarData.icon)
        customSnackBarBinding.tvHeader.text = snackBarData.header
        customSnackBarBinding.tvLower.text = snackBarData.description
        customSnackBarBinding.viewIconBg.setBackgroundResource(snackBarData.iconBg)

        //to set the snackBar pop-up from the top
        val param = customSnackBar.view.layoutParams as FrameLayout.LayoutParams
        param.gravity = Gravity.TOP
        param.topMargin = 100
        customSnackBar.view.layoutParams = param

        //set fade-in transition of the snackBar
        customSnackBar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE

        customSnackBar.dismiss()
        lifecycleScope.launch {
            delay(100)
            customSnackBar.show()
        }
    }
}