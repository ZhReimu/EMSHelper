package com.mrx

import com.mrx.zf.ZFManager


object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        val manager = ZFManager("123", "456")
        manager.login()
        manager.getStudentInfo()
        Thread.sleep(2000)
        manager.getScoreInfo()
        Thread.sleep(4000)
        manager.logout()
    }

}