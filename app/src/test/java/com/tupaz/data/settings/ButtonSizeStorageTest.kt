package com.tupaz.data.settings

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.tupaz.ui.theme.ButtonSize
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ButtonSizeStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        context = FakeSettingsContext(filesDir, cacheDir)
    }

    @Test
    fun testDefaultButtonSize_returnsNormal() {
        val storage = ButtonSizeStorage(context)
        assertEquals(ButtonSize.NORMAL, storage.getButtonSize())
        assertEquals(ButtonSize.NORMAL, storage.buttonSizeFlow.value)
    }

    @Test
    fun testSaveButtonSize_small_persistsAndEmitsSmall() {
        val storage = ButtonSizeStorage(context)
        storage.saveButtonSize(ButtonSize.SMALL)

        assertEquals(ButtonSize.SMALL, storage.getButtonSize())
        assertEquals(ButtonSize.SMALL, storage.buttonSizeFlow.value)

        val reloadedStorage = ButtonSizeStorage(context)
        assertEquals(ButtonSize.SMALL, reloadedStorage.getButtonSize())
    }

    @Test
    fun testSaveButtonSize_normal_persistsAndEmitsNormal() {
        val storage = ButtonSizeStorage(context)
        storage.saveButtonSize(ButtonSize.NORMAL)

        assertEquals(ButtonSize.NORMAL, storage.getButtonSize())
        assertEquals(ButtonSize.NORMAL, storage.buttonSizeFlow.value)

        val reloadedStorage = ButtonSizeStorage(context)
        assertEquals(ButtonSize.NORMAL, reloadedStorage.getButtonSize())
    }

    @Test
    fun testSaveButtonSize_large_persistsAndEmitsLarge() {
        val storage = ButtonSizeStorage(context)
        storage.saveButtonSize(ButtonSize.LARGE)

        assertEquals(ButtonSize.LARGE, storage.getButtonSize())
        assertEquals(ButtonSize.LARGE, storage.buttonSizeFlow.value)

        val reloadedStorage = ButtonSizeStorage(context)
        assertEquals(ButtonSize.LARGE, reloadedStorage.getButtonSize())
    }

    @Test
    fun testLegacyMediumPreference_automaticallyMigratesToNormal() {
        val prefs = context.getSharedPreferences("tupaz_settings_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("button_size", "MEDIUM").commit()

        val storage = ButtonSizeStorage(context)

        val resolvedSize = storage.getButtonSize()
        assertEquals(ButtonSize.NORMAL, resolvedSize)
        assertEquals(ButtonSize.NORMAL, storage.buttonSizeFlow.value)

        val rawStoredValue = prefs.getString("button_size", null)
        assertEquals("NORMAL", rawStoredValue)
    }

    @Test
    fun testLegacyMixedCaseMediumPreference_automaticallyMigratesToNormal() {
        val prefs = context.getSharedPreferences("tupaz_settings_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("button_size", "Medium").commit()

        val storage = ButtonSizeStorage(context)

        assertEquals(ButtonSize.NORMAL, storage.getButtonSize())
        assertEquals("NORMAL", prefs.getString("button_size", null))
    }
}

private class FakeSettingsContext(
    private val testFilesDir: File,
    private val testCacheDir: File
) : ContextWrapper(null) {

    private val sharedPrefsMap = mutableMapOf<String, MutableMap<String, Any?>>()

    override fun getFilesDir(): File = testFilesDir
    override fun getCacheDir(): File = testCacheDir

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        val prefsName = name ?: "default_prefs"
        val memoryPrefs = sharedPrefsMap.getOrPut(prefsName) { mutableMapOf() }

        return object : SharedPreferences {
            override fun getAll(): MutableMap<String, *> = memoryPrefs
            override fun getString(key: String?, defValue: String?): String? = memoryPrefs[key] as? String ?: defValue
            override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
            override fun getInt(key: String?, defValue: Int): Int = (memoryPrefs[key] as? Int) ?: defValue
            override fun getLong(key: String?, defValue: Long): Long = (memoryPrefs[key] as? Long) ?: defValue
            override fun getFloat(key: String?, defValue: Float): Float = (memoryPrefs[key] as? Float) ?: defValue
            override fun getBoolean(key: String?, defValue: Boolean): Boolean = (memoryPrefs[key] as? Boolean) ?: defValue
            override fun contains(key: String?): Boolean = memoryPrefs.containsKey(key)
            override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
                override fun putString(key: String?, value: String?): SharedPreferences.Editor { if (key != null) memoryPrefs[key] = value; return this }
                override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { if (key != null) memoryPrefs[key] = values; return this }
                override fun putInt(key: String?, value: Int): SharedPreferences.Editor { if (key != null) memoryPrefs[key] = value; return this }
                override fun putLong(key: String?, value: Long): SharedPreferences.Editor { if (key != null) memoryPrefs[key] = value; return this }
                override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { if (key != null) memoryPrefs[key] = value; return this }
                override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { if (key != null) memoryPrefs[key] = value; return this }
                override fun remove(key: String?): SharedPreferences.Editor { memoryPrefs.remove(key); return this }
                override fun clear(): SharedPreferences.Editor { memoryPrefs.clear(); return this }
                override fun apply() {}
                override fun commit(): Boolean = true
            }
            override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }
    }
}
