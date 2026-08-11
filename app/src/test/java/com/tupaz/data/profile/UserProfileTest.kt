package com.tupaz.data.profile

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import com.tupaz.data.firebase.FirebaseSyncManager
import com.tupaz.data.processing.ProcessingManager
import com.tupaz.data.processing.ProcessingStatus
import com.tupaz.ui.onboarding.ProfileSetupUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UserProfileTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var context: Context
    private lateinit var profileStorage: UserProfileStorage

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        cacheDir = tempFolder.newFolder("cache")
        context = ProfileTestContext(filesDir, cacheDir)
        profileStorage = UserProfileStorage(context)
    }

    @Test
    fun testFirstLaunch_detectsOnboardingIncomplete() {
        assertFalse(profileStorage.isOnboardingCompleted())
        assertNull(profileStorage.getUserProfile())
    }

    @Test
    fun testCompletedOnboarding_routesToHome() {
        profileStorage.setOnboardingCompleted(true)
        assertTrue(profileStorage.isOnboardingCompleted())
    }

    @Test
    fun testValidation_validNameAndAgeAccepted() {
        val state = ProfileSetupUiState(name = "Rudraksh", age = "20")
        assertTrue(state.isValid)
    }

    @Test
    fun testValidation_blankNameRejected() {
        val stateBlank = ProfileSetupUiState(name = "   ", age = "20")
        assertFalse(stateBlank.isValid)

        val stateEmpty = ProfileSetupUiState(name = "", age = "20")
        assertFalse(stateEmpty.isValid)
    }

    @Test
    fun testValidation_invalidAgeRejected() {
        val stateZero = ProfileSetupUiState(name = "Rudraksh", age = "0")
        assertFalse(stateZero.isValid)

        val stateNegative = ProfileSetupUiState(name = "Rudraksh", age = "-5")
        assertFalse(stateNegative.isValid)

        val stateTooHigh = ProfileSetupUiState(name = "Rudraksh", age = "999")
        assertFalse(stateTooHigh.isValid)
    }

    @Test
    fun testSaveProfileLocally_storesDataCorrectly() {
        val profile = profileStorage.createDefaultProfile("Rudraksh", 20, "uid_test_123")
        profileStorage.saveUserProfile(profile)
        profileStorage.setOnboardingCompleted(true)

        assertTrue(profileStorage.isOnboardingCompleted())
        val saved = profileStorage.getUserProfile()
        assertNotNull(saved)
        assertEquals("Rudraksh", saved?.name)
        assertEquals(20, saved?.age)
        assertEquals("uid_test_123", saved?.userId)
    }

    @Test
    fun testSaveAndSyncProfile_instantLocalSaveFirst() = runBlocking {
        val syncManager = FirebaseSyncManager(context)
        val resultProfile = syncManager.saveAndSyncProfile("Rudraksh", 20)

        assertTrue(profileStorage.isOnboardingCompleted())
        val saved = profileStorage.getUserProfile()
        assertNotNull(saved)
        assertEquals("Rudraksh", saved?.name)
        assertEquals(20, saved?.age)
        assertEquals(true, resultProfile.pendingSync)
    }

    @Test
    fun testOfflineFailure_preservesLocalProfileAndSetsPendingSync() = runBlocking {
        val syncManager = FirebaseSyncManager(context)
        val profile = syncManager.saveAndSyncProfile("Offline User", 25)

        val saved = profileStorage.getUserProfile()
        assertNotNull(saved)
        assertEquals("Offline User", saved?.name)
        assertEquals(25, saved?.age)
        assertTrue(profileStorage.isPendingSync())
        assertTrue(saved!!.pendingSync)
    }

    @Test
    fun testEditProfile_preservesFirstInstallTimestamp() = runBlocking {
        val syncManager = FirebaseSyncManager(context)
        val initial = syncManager.saveAndSyncProfile("Initial Name", 20)
        val initialFirstInstall = initial.firstInstall
        assertTrue(initialFirstInstall > 0L)

        // Wait brief moment
        kotlinx.coroutines.delay(10)

        val updated = syncManager.saveAndSyncProfile("Updated Name", 21)
        val saved = profileStorage.getUserProfile()

        assertNotNull(saved)
        assertEquals("Updated Name", saved?.name)
        assertEquals(21, saved?.age)
        assertEquals(initialFirstInstall, saved?.firstInstall)
    }

    @Test
    fun testExistingProcessingStartupNavigation_remainsUnchanged() {
        ProcessingManager.reset(context)
        assertEquals(ProcessingStatus.IDLE, ProcessingManager.state.value.status)
        assertNull(ProcessingManager.state.value.errorMessage)
    }

    @Test
    fun firebaseFailure_doesNotPreventHomeNavigation() = runBlocking {
        val syncManager = FirebaseSyncManager(context)

        val localProfile = syncManager.saveProfileLocally("Test User", 22)
        val homeNavigated = profileStorage.isOnboardingCompleted()

        assertTrue("Home navigation must succeed immediately after local save", homeNavigated)
        assertTrue(profileStorage.isOnboardingCompleted())

        val saved = profileStorage.getUserProfile()
        assertNotNull(saved)
        assertEquals("Test User", saved?.name)
        assertEquals(22, saved?.age)

        val synced = syncManager.syncProfileInBackground(localProfile)
        assertTrue("Pending sync must be true when Firebase sync fails or is offline", synced.pendingSync)
        assertTrue("Onboarding must remain completed despite Firebase failure", profileStorage.isOnboardingCompleted())
    }
}

class ProfileTestContext(
    private val testFilesDir: File,
    private val testCacheDir: File
) : ContextWrapper(null) {
    private val sharedPrefsMap = mutableMapOf<String, ProfileTestPrefs>()

    override fun getFilesDir(): File = testFilesDir
    override fun getCacheDir(): File = testCacheDir
    override fun getApplicationContext(): Context = this

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return sharedPrefsMap.getOrPut(name) { ProfileTestPrefs() }
    }

    override fun getSystemService(name: String): Any? = null
    override fun startService(service: Intent?): ComponentName? = null
    override fun stopService(name: Intent?): Boolean = true
}

class ProfileTestPrefs : SharedPreferences {
    private val memory = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = memory
    override fun getString(key: String?, defValue: String?): String? = (memory[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = (memory[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (memory[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (memory[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (memory[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = memory.containsKey(key)

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor { if (key != null) memory[key] = value; return this }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { if (key != null) memory[key] = values; return this }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor { if (key != null) memory[key] = value; return this }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor { if (key != null) memory[key] = value; return this }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { if (key != null) memory[key] = value; return this }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { if (key != null) memory[key] = value; return this }
        override fun remove(key: String?): SharedPreferences.Editor { memory.remove(key); return this }
        override fun clear(): SharedPreferences.Editor { memory.clear(); return this }
        override fun apply() {}
        override fun commit(): Boolean = true
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
}
