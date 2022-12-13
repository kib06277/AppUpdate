package com.example.appupdate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val mContext: Context) {

    // 是否是最新的应用,默认为false
    private val isNew = false
    private var intercept = false

    // 下载安装包的网络路径
    private val apkUrl = ("http://122.146.250.130/TPMC_BID/Scripts/app-debugtoKotlin.apk")

    // 保存APK的文件夹
    private val savePath = mContext.externalCacheDir?.path
    private val saveFileName = (savePath + "UpdateDemoRelease.apk")

    // 下载线程
    private var downLoadThread: Thread? = null
    private var progress = 0 //當前進度
    var text: TextView? = null

    // 进度条与通知UI刷新的handler和msg常量
    private var mProgress: ProgressBar? = null
    private val DOWN_UPDATE = 1
    private val DOWN_OVER = 2
    /**
     * 检查是否更新的内容
     */
    fun checkUpdateInfo() {
        //这里的isNew本来是要从服务器获取的，我在这里先假设他需要更新
        if (isNew) {
            return
        } else {
            showUpdateDialog()
        }
    }

    /**
     * 显示更新程序对话框，供主程序调用
     */
    private fun showUpdateDialog() {
        val builder = mContext?.let { AlertDialog.Builder(it) }
        builder?.setTitle("软件版本更新")
        builder?.setMessage("有最新的软件包，请下载!")
        builder?.setPositiveButton(
            "下载"
        ) { dialog, which -> showDownloadDialog() }
        builder?.setNegativeButton(
            "以后再说"
        ) { dialog, which -> dialog.dismiss() }
        builder?.create()?.show()
    }

    /**
     * 显示下载进度的对话框
     */
    private fun showDownloadDialog() {
        val builder = mContext?.let { AlertDialog.Builder(it) }
        builder?.setTitle("软件版本更新")
        val inflater = LayoutInflater.from(mContext)
        val v: View = inflater.inflate(R.layout.progressbar, null)
        mProgress = v.findViewById<View>(R.id.progress) as ProgressBar
        builder?.setView(v)
        builder?.setNegativeButton(
            "取消"
        ) { dialog, which -> intercept = true }
        builder?.show()
        downloadApk()
    }

    /**
     * 从服务器下载APK安装包
     */
    private fun downloadApk() {
        downLoadThread = Thread(mdownApkRunnable)
        downLoadThread!!.start()
    }

    private val mdownApkRunnable = Runnable {
        val url: URL
        try {
            url = URL(apkUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connect()
            val length = conn.contentLength
            val ins = conn.inputStream
            val file = File(savePath)
            if (!file.exists()) {
                file.mkdir()
            }
            val apkFile = File(saveFileName)
            val fos = FileOutputStream(apkFile)
            var count = 0
            val buf = ByteArray(1024)
            while (!intercept) {
                val numread = ins.read(buf)
                count += numread
                progress = (count.toFloat() / length * 100).toInt()
                // 下载进度
                mHandler.sendEmptyMessage(DOWN_UPDATE)
                if (numread <= 0) {
                // 下载完成通知安装
                    mHandler.sendEmptyMessage(DOWN_OVER)
                    break
                }
                fos.write(buf, 0, numread)
            }
            fos.close()
            ins.close()
        } catch (e: Exception) {
            Log.i("Error" , "mdownApkRunnable e = $e")
        }
    }

    /**
     * 安装APK内容
     */
    private fun installAPK() {
        try {
            val file = File(saveFileName)
            //判断是否是AndroidN以及更高的版本
            val intent = Intent(Intent.ACTION_VIEW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val contentUri = FileProvider.getUriForFile(mContext, "com.example.appupdate.MainActivity", file)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
            } else {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }
            mContext.startActivity(intent)
        } catch ( e : Exception) {
            Log.i("Error" , "installAPK e = $e")
        }
    }

    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                DOWN_UPDATE -> mProgress!!.progress = progress
                DOWN_OVER -> installAPK()
                else -> {}
            }
        }
    }
}