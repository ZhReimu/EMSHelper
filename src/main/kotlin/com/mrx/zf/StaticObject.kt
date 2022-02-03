package com.mrx.zf

import com.alibaba.fastjson.JSON
import com.mrx.XLog
import okhttp3.*
import org.apache.commons.codec.binary.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jsoup.Jsoup
import java.math.BigInteger
import java.security.KeyFactory
import java.security.Security
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

object StaticObject {

    const val MAIN_URL = "http://jwgl.hxut.edu.cn/"

    val client = OkHttpClient.Builder()
        .addNetworkInterceptor(NetworkInterceptor())
        .cookieJar(CookieJar())
        .build()

    val request = Request.Builder()
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36"
        )

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private val logger = XLog.getLogger(this::class)

    fun String.sendGetForResponse() = client.newCall(request.get().url(this).build()).execute()

    fun String.sendGetForString() = this.sendGetForResponse().body!!.string()

    fun String.sendGetForJsonObject() = JSON.parseObject(this.sendGetForString())

    fun String.sendGetForSoup() = this.sendGetForString().toSoup()

    fun String.toSoup() = Jsoup.parse(this)

    fun getTimeStamp() = System.currentTimeMillis()

    fun rsaEncode(btMod: ByteArray, btExp: ByteArray, plainText: String): String {
        val modulus = BigInteger(1, btMod)
        val exponent = BigInteger(1, btExp)
        val keyFactory = KeyFactory.getInstance("RSA")
        val pubKeySpec = RSAPublicKeySpec(modulus, exponent)
        val rsaPublicKey = keyFactory.generatePublic(pubKeySpec) as RSAPublicKey
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey)
        return Base64.encodeBase64String(cipher.doFinal(plainText.toByteArray()))
    }

    private class NetworkInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            logger.debug("正在请求 -> ${request.url}")
            logger.debug(request.header("Cookie"))
            val response = chain.proceed(request)
            logger.debug("响应代码 -> ${response.code}")
            return response
        }

    }

    private class CookieJar : okhttp3.CookieJar {
        // cookie 缓存
        private val cookies = ArrayList<Cookie>()

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            //过期的Cookie
            val invalidCookies: MutableList<Cookie> = ArrayList()
            //有效的Cookie
            val validCookies: MutableList<Cookie> = ArrayList()

            for (cookie in this.cookies) {
                if (cookie.expiresAt < System.currentTimeMillis()) {
                    //判断是否过期
                    invalidCookies.add(cookie)
                } else if (cookie.matches(url)) {
                    //匹配Cookie对应url
                    validCookies.add(cookie)
                }
            }

            // 缓存中移除过期的 Cookie
            this.cookies.removeAll(invalidCookies.toSet())

            // 返回 List<Cookie> 让 Request 进行设置
            return validCookies
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies.addAll(cookies)
        }

    }

}