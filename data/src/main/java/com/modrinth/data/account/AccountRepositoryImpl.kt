package com.modrinth.data.account

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.modrinth.domain.account.AccountRepository
import com.modrinth.domain.account.LocalAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * 纯离线本地账户仓库实现。
 *
 * - 当前登录账户 id → DataStore Preferences（跨进程安全，天然产生 Flow）
 * - 账户列表（含 PIN 摘要与收藏）→ context.filesDir/accounts.json（Gson 序列化）
 * - 所有「读-改-写」都在单线程 IO 协程 + Mutex 下串行执行，
 *   避免并发 toggleFavorite 互相覆盖收藏列表
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : AccountRepository {

    private val currentAccountIdKey = stringPreferencesKey("account_current_id")

    private val gson = Gson()
    private val random = Random(System.nanoTime())

    /** 串行化账户文件的读-改-写。 */
    private val ioMutex = Mutex()

    private val accountsFile: File
        get() = File(context.filesDir, "accounts.json")

    /**
     * 账户变更信号：任何写操作完成后自增（收藏 / 注册 / 登录都会触发）。
     * 与 DataStore 的当前 id 流 combine 后，
     * observeCurrentAccount 在收藏变更、重新登录时都会重新发射最新值。
     */
    private val accountsRevision = MutableStateFlow(0L)

    override fun observeCurrentAccount(): Flow<LocalAccount?> =
        combine(dataStore.data, accountsRevision) { prefs, _ ->
            prefs[currentAccountIdKey]
        }.map { id ->
            if (id == null) null else withContext(Dispatchers.IO) { readAccountByIdBlocking(id) }
        }

    override suspend fun register(username: String, pin: String): LocalAccount? = ioMutex.withLock {
        val accounts = readAccounts()
        if (accounts.any { it.username.equals(username, ignoreCase = true) }) return@withLock null

        val account = AccountRecord(
            id = generateId(),
            username = username,
            displayName = username,
            avatarColor = generateAvatarColor(),
            createdAt = System.currentTimeMillis(),
            pinHash = hashPin(pin),
            favorites = emptyList()
        )
        writeAccounts(accounts + account)
        accountsRevision.value += 1
        setCurrentAccountId(account.id)
        toDomain(account)
    }

    override suspend fun login(username: String, pin: String): LocalAccount? = ioMutex.withLock {
        val account = readAccounts().firstOrNull { it.username.equals(username, ignoreCase = true) }
            ?: return@withLock null
        if (account.pinHash != hashPin(pin)) return@withLock null
        setCurrentAccountId(account.id)
        accountsRevision.value += 1
        toDomain(account)
    }

    override suspend fun logout() {
        setCurrentAccountId(null)
    }

    override suspend fun getCurrentAccount(): LocalAccount? {
        val id = dataStore.data.first()[currentAccountIdKey] ?: return null
        return ioMutex.withLock {
            readAccounts().firstOrNull { it.id == id }?.let { toDomain(it) }
        }
    }

    override suspend fun toggleFavorite(accountId: String, projectId: String): LocalAccount? = ioMutex.withLock {
        val accounts = readAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == accountId }
        if (index < 0) return@withLock null

        val current = accounts[index]
        val favorites = if (current.favorites.contains(projectId)) {
            current.favorites - projectId
        } else {
            current.favorites + projectId
        }
        val updated = current.copy(favorites = favorites)
        accounts[index] = updated
        writeAccounts(accounts)
        accountsRevision.value += 1
        toDomain(updated)
    }

    override suspend fun getFavorites(accountId: String): List<String> = ioMutex.withLock {
        readAccounts().firstOrNull { it.id == accountId }?.favorites.orEmpty()
    }

    // region 文件读写（阻塞 IO，仅在 withContext(Dispatchers.IO) 内调用）

    private suspend fun readAccounts(): List<AccountRecord> = withContext(Dispatchers.IO) {
        val file = accountsFile
        if (!file.exists()) return@withContext emptyList()
        try {
            val text = file.readText()
            if (text.isBlank()) emptyList() else parseAccounts(text)
        } catch (e: Exception) {
            Log.e(TAG, "读取 accounts.json 失败", e)
            emptyList()
        }
    }

    /** 阻塞读文件取指定账户（调用方需在 Dispatchers.IO 上执行）。 */
    private fun readAccountByIdBlocking(id: String): LocalAccount? {
        val file = accountsFile
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            if (text.isBlank()) return null
            parseAccounts(text).firstOrNull { it.id == id }?.let { toDomain(it) }
        } catch (e: Exception) {
            Log.e(TAG, "读取账户 $id 失败", e)
            null
        }
    }

    /** 更新 / 清除当前登录账户 id（DataStore Preferences）。 */
    private suspend fun setCurrentAccountId(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) prefs.remove(currentAccountIdKey) else prefs[currentAccountIdKey] = id
        }
    }

    private suspend fun writeAccounts(accounts: List<AccountRecord>) = withContext(Dispatchers.IO) {
        val file = accountsFile
        // 先写临时文件再原子重命名，避免写一半崩溃损坏账户列表
        val tmp = File(context.filesDir, "accounts.json.tmp")
        tmp.writeText(gson.toJson(accounts))
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun parseAccounts(json: String): List<AccountRecord> {
        val type = object : TypeToken<List<AccountRecord>>() {}.type
        return gson.fromJson<List<AccountRecord>>(json, type).orEmpty()
    }

    // endregion

    // region PIN 与生成

    /**
     * 计算 PIN 的 SHA-256 hex 摘要。
     *
     * 说明：纯离线单机场景，只为了不在磁盘上保存明文 PIN，
     * 这是本地轻量校验，**不是安全边界**（无加盐、无慢哈希，
     * 不能抵御认真的攻击者）。
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun generateId(): String {
        val time = System.currentTimeMillis().toString(36)
        val rand = buildString { repeat(10) { append(random.nextInt(36).toString(36)) } }
        return "acct_${time}$rand"
    }

    /** 生成 160°~320° 色相的头像底色，保证相邻账户颜色可区分。 */
    private fun generateAvatarColor(): Int {
        val h = (160 + random.nextInt(0, 160)) / 360f
        val (r, g, b) = hslToRgb(h, 0.55f, 0.55f)
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Int, Int, Int> {
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s
        val x = c * (1 - kotlin.math.abs((h * 6) % 2 - 1))
        val m = l - c / 2
        val (r1, g1, b1) = when {
            h < 1f / 6f -> Triple(c, x, 0f)
            h < 2f / 6f -> Triple(x, c, 0f)
            h < 3f / 6f -> Triple(0f, c, x)
            h < 4f / 6f -> Triple(0f, x, c)
            h < 5f / 6f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Triple(
            ((r1 + m) * 255).toInt().coerceIn(0, 255),
            ((g1 + m) * 255).toInt().coerceIn(0, 255),
            ((b1 + m) * 255).toInt().coerceIn(0, 255)
        )
    }

    private fun toDomain(record: AccountRecord) = LocalAccount(
        id = record.id,
        username = record.username,
        displayName = record.displayName,
        avatarColor = record.avatarColor,
        createdAt = record.createdAt,
        favorites = record.favorites
    )

    // endregion

    /** 持久化到 accounts.json 的账户记录（含 PIN 摘要，不存明文）。 */
    private data class AccountRecord(
        val id: String,
        val username: String,
        val displayName: String,
        val avatarColor: Int,
        val createdAt: Long,
        val pinHash: String,
        val favorites: List<String>
    )

    companion object {
        private const val TAG = "AccountRepository"
    }
}
