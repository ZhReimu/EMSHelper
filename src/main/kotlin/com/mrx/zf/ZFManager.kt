package com.mrx.zf

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.mrx.XLog
import com.mrx.zf.StaticObject.MAIN_URL
import com.mrx.zf.StaticObject.client
import com.mrx.zf.StaticObject.getTimeStamp
import com.mrx.zf.StaticObject.request
import com.mrx.zf.StaticObject.rsaEncode
import com.mrx.zf.StaticObject.sendGetForJsonObject
import com.mrx.zf.StaticObject.sendGetForSoup
import com.mrx.zf.StaticObject.sendGetForString
import com.mrx.zf.StaticObject.toSoup
import okhttp3.FormBody
import org.apache.commons.codec.binary.Base64
import org.jsoup.Jsoup

/**
 * 正方教务系统
 * @property ZH String 账号
 * @property MM String 密码
 * @property logger Logger 日志对象
 * @property logged Boolean 是否已登录, 已登录为 true, 未登录为 false
 * @property csrfToken (String..String?) 登录表单中的参数之一
 * @property encodedMM String ras 加密后的 密码
 * @constructor
 */
class ZFManager(private val ZH: String, private val MM: String) {

    private val logger = XLog.getLogger(this::class)

    private var logged = false

    private val csrfToken by lazy {
        println("正在获取 csrfToken")
        val token = Jsoup.parse(MAIN_URL.sendGetForString())
            .select("#csrftoken").attr("value")
        logger.debug("token -> $token")
        println("csrfToken 获取成功")
        return@lazy token
    }

    private val encodedMM by lazy {
        println("正在获取公钥")
        val url = "http://jwgl.hxut.edu.cn/xtgl/login_getPublicKey.html" +
                "?time=${getTimeStamp()}" +
                "&_=${getTimeStamp() + 2000}"
        val json = url.sendGetForJsonObject()
        logger.debug("PublicKey -> $json")
        println("公钥获取成功")
        val btMod = Base64.decodeBase64(json["modulus"] as String)
        val btExp = Base64.decodeBase64(json["exponent"] as String)
        val encodedKey = rsaEncode(btMod, btExp, MM)
        logger.debug("encodedKey -> $encodedKey")
        println("密码加密完成")
        return@lazy encodedKey
    }

    /**
     * 查询学生姓名与专业
     */
    fun getStudentInfo() {
        if (!logged) {
            throw IllegalStateException("请先登录！")
        }
        println("正在查询学生信息")
        val url = "http://jwgl.hxut.edu.cn/xtgl/index_cxYhxxIndex.html?xt=jw&localeKey=zh_CN" +
                "&_=${getTimeStamp()}&gnmkdm=index&su=$ZH"
        val soup = url.sendGetForSoup()
        val studentName = soup.select("body > div > div > h4").text()
        val studentMajor = soup.select("body > div > div > p").text()
        logger.info("学生姓名 -> $studentName, 学生专业 -> $studentMajor")
        println("学生姓名 -> $studentName, 学生专业 -> $studentMajor")
    }

    /**
     * 登录账号
     */
    fun login() {
        if (logged) {
            throw IllegalStateException("该账号已经登录过了！")
        }
        println("正在尝试登录账号")
        val body = FormBody.Builder()
            .add("csrfToken", csrfToken)
            .add("language", "zh_CN")
            .add("yhm", ZH)
            .add("mm", encodedMM)
            .add("mm", encodedMM)
            .build()
        val res = client.newCall(
            request
                .url("http://jwgl.hxut.edu.cn/xtgl/login_slogin.html?time=${getTimeStamp()}")
                .post(body)
                .addHeader("Referer", "http://jwgl.hxut.edu.cn/xtgl/login_slogin.html")
                .build()
        ).execute()
        val script = res.body!!.string().toSoup().select("head > script").first()!!.data()
        if ("dl_loginForward" !in script) {
            logger.error("登录失败, 用户名或密码不正确，请重新输入！")
            throw IllegalStateException("登录失败, 用户名或密码不正确，请重新输入！")
        }
        val redirect = "http://jwgl.hxut.edu.cn/xtgl/dl_loginForward.html?language=&_t=${getTimeStamp()}"
            .sendGetForString()
        if ("学生课表查询" in redirect) {
            logged = true
            logger.info("登录成功")
            println("登录成功")
        }
    }

    /**
     * 登出账号
     */
    fun logout() {
        if (!logged) {
            throw IllegalStateException("请先登录！")
        }
        println("正在登出账号")
        val url = "http://jwgl.hxut.edu.cn/logout?t=${getTimeStamp()}&login_type="
        val res = url.sendGetForString()
        if ("学生账号为学号" in res) {
            logger.info("退出登录成功")
            logged = false
            println("退出登录成功")
        } else {
            logger.error("退出登录失败!")
            println("退出登录失败!")
            logger.error(res)
        }
    }

    /**
     * 查询当前用户的成绩
     */
    fun getScoreInfo() {
        if (!logged) {
            throw IllegalStateException("请先登录！")
        }
        println("正在查询成绩")
        val url = "http://jwgl.hxut.edu.cn/cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005&su=$ZH"
        val json = url.sendGetForJsonObject()
        logger.debug("成绩查询结果 -> $json")
        println("成绩查询结果:")
        (json["items"]!! as JSONArray).forEach {
            if (it is JSONObject) {
                val score = Score(it["bfzcj"], it["jd"], it["jsxm"], it["kcxzmc"], it["kcmc"])
                println(score)
            }
        }
    }

    private data class Score(val score: Any?, val jd: Any?, val tn: Any?, val ct: Any?, val cn: Any?) {
        override fun toString(): String {
            return "课程名: $cn\n分数 $score, 绩点 $jd, 老师名字 $tn, 课程类型 $ct"
        }
    }
}