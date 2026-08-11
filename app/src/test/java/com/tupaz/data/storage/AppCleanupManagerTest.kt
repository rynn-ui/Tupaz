package com.tupaz.data.storage

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import com.tupaz.data.processing.ProcessingJobConfig
import com.tupaz.data.processing.ProcessingManager
import com.tupaz.data.processing.ProcessingStatus
import com.tupaz.domain.pipeline.AiQuality
import com.tupaz.ui.main.ProjectItem
import com.tupaz.ui.main.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppCleanupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var context: Context

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        cacheDir = tempFolder.newFolder("cache")
        context = FakeTestContext(filesDir, cacheDir)
    }

    @Test
    fun testPerformFullCleanup_clearsProjectsAndCache_preservesModelFiles() {
        val modelStorage = ModelStorage(context)
        val lowParam = modelStorage.getParamFile(AiQuality.LOW.modelId)
        val lowBin = modelStorage.getBinFile(AiQuality.LOW.modelId)
        lowParam.writeText("param dummy")
        lowBin.writeText("bin dummy")

        val medParam = modelStorage.getParamFile(AiQuality.MEDIUM.modelId)
        val medBin = modelStorage.getBinFile(AiQuality.MEDIUM.modelId)
        medParam.writeText("param dummy")
        medBin.writeText("bin dummy")

        val highParam = modelStorage.getParamFile(AiQuality.HIGH.modelId)
        val highBin = modelStorage.getBinFile(AiQuality.HIGH.modelId)
        highParam.writeText("param dummy")
        highBin.writeText("bin dummy")

        val projectStorage = ProjectStorage(context)
        val project = ProjectItem(
            id = "proj_test_cleanup",
            projectName = "Test Cleanup Project",
            status = ProjectStatus.COMPLETED
        )
        projectStorage.saveProject(project)
        assertEquals(1, projectStorage.loadProjects().size)

        val dummyCacheFile = File(cacheDir, "temp_cache_frame.tmp")
        dummyCacheFile.writeText("cache content")
        assertTrue(dummyCacheFile.exists())

        val success = AppCleanupManager.performFullCleanup(context)
        assertTrue(success)

        assertEquals(0, projectStorage.loadProjects().size)
        assertEquals(ProcessingStatus.IDLE, ProcessingManager.state.value.status)

        assertTrue(lowParam.exists())
        assertTrue(lowBin.exists())
        assertTrue(medParam.exists())
        assertTrue(medBin.exists())
        assertTrue(highParam.exists())
        assertTrue(highBin.exists())
    }

    @Test
    fun testPerformFullCleanup_isIdempotent() {
        val firstRun = AppCleanupManager.performFullCleanup(context)
        val secondRun = AppCleanupManager.performFullCleanup(context)

        assertTrue(firstRun)
        assertTrue(secondRun)
        assertEquals(ProcessingStatus.IDLE, ProcessingManager.state.value.status)
        assertEquals(null, ProcessingManager.state.value.errorMessage)
    }

    @Test
    fun testPerformFullCleanup_preservesModels_whenModelsInExternalStorage() {
        val extDir = tempFolder.newFolder("external_models")
        val extContext = FakeTestContext(filesDir, cacheDir, externalModelsDir = extDir)

        val extModelStorage = ModelStorage(extContext)
        val lowParam = extModelStorage.getParamFile(AiQuality.LOW.modelId)
        val lowBin = extModelStorage.getBinFile(AiQuality.LOW.modelId)
        lowParam.writeText("param ext dummy")
        lowBin.writeText("bin ext dummy")

        val medParam = extModelStorage.getParamFile(AiQuality.MEDIUM.modelId)
        val medBin = extModelStorage.getBinFile(AiQuality.MEDIUM.modelId)
        medParam.writeText("param ext dummy")
        medBin.writeText("bin ext dummy")

        val highParam = extModelStorage.getParamFile(AiQuality.HIGH.modelId)
        val highBin = extModelStorage.getBinFile(AiQuality.HIGH.modelId)
        highParam.writeText("param ext dummy")
        highBin.writeText("bin ext dummy")

        val success = AppCleanupManager.performFullCleanup(extContext)
        assertTrue(success)

        assertTrue(lowParam.exists())
        assertTrue(lowBin.exists())
        assertTrue(medParam.exists())
        assertTrue(medBin.exists())
        assertTrue(highParam.exists())
        assertTrue(highBin.exists())
        assertEquals(ProcessingStatus.IDLE, ProcessingManager.state.value.status)
    }

    @Test
    fun testCancelProcessing_whenIdle_isIgnored() {
        ProcessingManager.reset(context)
        assertEquals(ProcessingStatus.IDLE, ProcessingManager.state.value.status)

        ProcessingManager.cancelProcessing(context)
        assertEquals(ProcessingStatus.IDLE, ProcessingManager.state.value.status)
        assertEquals(null, ProcessingManager.state.value.errorMessage)
    }

    @Test
    fun testCancelProcessing_whenProcessing_setsCancelledStateWithNullErrorMessage() {
        ProcessingManager.reset(context)
        val config = ProcessingJobConfig(projectId = "test_proj_cancel", fileName = "test_video.mp4")

        val initialProject = com.tupaz.ui.main.ProjectItem(
            id = "test_proj_cancel",
            projectName = "Cancel Test",
            status = com.tupaz.ui.main.ProjectStatus.PROCESSING
        )
        ProjectStorage(context).saveProject(initialProject)

        ProcessingManager.startProcessing(context, config)
        assertEquals(ProcessingStatus.PROCESSING, ProcessingManager.state.value.status)

        ProcessingManager.cancelProcessing(context)

        val state = ProcessingManager.state.value
        assertEquals(ProcessingStatus.CANCELLED, state.status)
        assertNull(state.errorMessage)
        assertNull(state.outputUriString)
        assertNull(state.outputUri)

        val project = ProjectStorage(context).getProject("test_proj_cancel")
        assertNotNull(project)
        assertEquals(com.tupaz.ui.main.ProjectStatus.CANCELLED, project?.status)
        assertNull(project?.outputUriString)
    }
}

class FakeTestContext(
    private val testFilesDir: File,
    private val testCacheDir: File,
    private val externalModelsDir: File? = null
) : ContextWrapper(null) {
    private val memoryPrefs = mutableMapOf<String, Any?>()

    override fun getFilesDir(): File = testFilesDir
    override fun getCacheDir(): File = testCacheDir
    override fun getExternalFilesDir(type: String?): File? {
        return if (type == "models") externalModelsDir else null
    }

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
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

    override fun getSystemService(name: String): Any? = null
    override fun startService(service: Intent?): ComponentName? = null
    override fun stopService(name: Intent?): Boolean = true
}
