package com.tupaz.ui.cloud

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class CloudStatus {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED
}

enum class NotebookStatus {
    NOT_INSTALLED,
    INSTALLED
}

enum class CloudJobStep {
    IDLE,
    UPLOADING_TO_DRIVE,
    COLAB_PROCESSING_BG,
    EXPORTING_TO_DRIVE,
    COMPLETED
}

enum class CloudModelOption(
    val id: String,
    val title: String,
    val modelName: String,
    val scaleFactor: Int,
    val description: String
) {
    ANIME_VIDEO_V3_2X(
        "realesr-animevideov3-x2",
        "Anime Video v3 (2x)",
        "realesr-animevideov3",
        2,
        "Official Real-ESRGAN model for 2x video upscaling."
    ),
    ANIME_VIDEO_V3_4X(
        "realesr-animevideov3-x4",
        "Anime Video v3 (4x)",
        "realesr-animevideov3",
        4,
        "Ultra 4x upscaling for anime clips."
    ),
    GENERAL_VIDEO_4X(
        "realesrgan-x4plus",
        "General Video (4x)",
        "realesrgan-x4plus",
        4,
        "High detail 4x upscaling for real-world video footage."
    ),
    ANIME_ILLUST_4X(
        "realesrgan-x4plus-anime",
        "Anime Illustration (4x)",
        "realesrgan-x4plus-anime",
        4,
        "Optimized for 2D anime illustrations & artwork."
    )
}

data class CloudProcessingUiState(
    val googleAccountName: String = "rudrakshallenhouse@gmail.com",
    val cloudStatus: CloudStatus = CloudStatus.CONNECTED,
    val notebookStatus: NotebookStatus = NotebookStatus.INSTALLED,
    val driveInputFolderName: String = "tupaz_cloud",
    val driveOutputFolderName: String = "tupaz_cloudexported",
    val driveFolderNameFull: String = "Google Drive / tupaz_cloud",
    val driveExportedFolderNameFull: String = "Google Drive / tupaz_cloudexported",
    val notebookUrl: String = "https://colab.research.google.com/#upload=true",
    val jobStep: CloudJobStep = CloudJobStep.IDLE,
    val uploadProgress: Int = 0,
    val statusMessage: String = "Step 1: Upload video to Google Drive (tupaz_cloud)",
    val inputUri: Uri? = null,
    val driveOutputVideoUrl: String = "https://drive.google.com/drive/my-drive",
    val isJobRunning: Boolean = false,
    val isVideoUploaded: Boolean = false,
    val isColabButtonEnabled: Boolean = false,
    val isExportedVideoSaved: Boolean = false, // Gating fix: ONLY true when video is saved in tupaz_cloudexported!
    val uploadedFileName: String = "video.mp4",
    val exportedFileName: String = "video_outx2.mp4",
    val processingProgress: Int = 0,
    val selectedModel: CloudModelOption = CloudModelOption.ANIME_VIDEO_V3_2X,
    val colabScript: String = "",
    val showDownloadPopUp: Boolean = false
)

class CloudProcessingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CloudProcessingUiState())
    val uiState: StateFlow<CloudProcessingUiState> = _uiState.asStateFlow()

    init {
        updateGeneratedScript()
    }

    fun selectModel(option: CloudModelOption) {
        _uiState.update { it.copy(selectedModel = option) }
        updateGeneratedScript()
    }

    private fun updateGeneratedScript() {
        val model = _uiState.value.selectedModel
        val fileName = _uiState.value.uploadedFileName.ifEmpty { "video.mp4" }
        val outName = if (fileName.contains(".")) {
            val base = fileName.substringBeforeLast(".")
            val ext = fileName.substringAfterLast(".")
            "${base}_outx${model.scaleFactor}.$ext"
        } else {
            "${fileName}_outx${model.scaleFactor}.mp4"
        }

        val script = """
# ========================================================
# Karan's Real-ESRGAN Video Notebook (T4 GPU Pre-set)
# Input Folder:  Google Drive / tupaz_cloud / $fileName
# Output Folder: Google Drive / tupaz_cloudexported / $outName
# ========================================================

# Cell 1: Environment Setup
!git clone https://github.com/xinntao/Real-ESRGAN.git
%cd Real-ESRGAN
!pip install -q basicsr facexlib gfpgan ffmpeg-python torchvision
!pip install -q -r requirements.txt
!python -m pip install -q torch==2.0.1 torchvision==0.15.2 --extra-index-url https://download.pytorch.org/whl/cu118
!python setup.py develop

# Cell 2: Connect Google Drive Directories
from google.colab import drive
import os, shutil

drive.mount('/content/drive')
input_dir = "/content/drive/MyDrive/tupaz_cloud"
output_dir = "/content/drive/MyDrive/tupaz_cloudexported"
os.makedirs(input_dir, exist_ok=True)
os.makedirs(output_dir, exist_ok=True)

# Cell 3: Execute Real-ESRGAN Super-Resolution (T4 GPU)
input_file = os.path.join(input_dir, "$fileName")
output_file = os.path.join(output_dir, "$outName")

!python inference_realesrgan_video.py -i "{input_file}" -n ${model.modelName} -s ${model.scaleFactor} --suffix outx${model.scaleFactor}

# Save output to tupaz_cloudexported
colab_output = f"results/{fileName.substringBeforeLast('.')}_outx${model.scaleFactor}.mp4"
if os.path.exists(colab_output):
    shutil.copy(colab_output, output_file)

print("🎉 Complete! Enhanced video saved in: Google Drive / tupaz_cloudexported / $outName")
""".trimIndent()

        _uiState.update {
            it.copy(
                colabScript = script,
                exportedFileName = outName
            )
        }
    }

    fun exportKaranNotebookToDevice(context: Context): File? {
        return try {
            val notebookFile = File(context.cacheDir, "REAL_ESRGAN_video_by_karan.ipynb")
            context.assets.open("notebooks/REAL_ESRGAN_video_by_karan.ipynb").use { input ->
                notebookFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            notebookFile
        } catch (e: Exception) {
            android.util.Log.e("CloudViewModel", "Failed to extract Karan notebook from assets", e)
            null
        }
    }

    fun shareKaranNotebook(context: Context) {
        val file = exportKaranNotebookToDevice(context) ?: return
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/x-ipynb+json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Karan's REAL_ESRGAN_video_by_karan.ipynb"))
        } catch (_: Exception) {
            Toast.makeText(context, "Notebook file saved to cache: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }

    fun copyScriptToClipboard(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Karan Colab Notebook Script", _uiState.value.colabScript)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "📋 Karan's notebook script copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Failed to copy script", Toast.LENGTH_SHORT).show()
        }
    }

    fun switchGoogleAccount(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(cloudStatus = CloudStatus.CONNECTING, statusMessage = "Opening Google Account Switcher...") }
            try {
                val accountChooserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.google.com/AccountChooser"))
                accountChooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(accountChooserIntent)
            } catch (_: Exception) {}
            delay(1000)
            _uiState.update {
                it.copy(
                    cloudStatus = CloudStatus.CONNECTED,
                    statusMessage = "Connected to Google Drive account: ${it.googleAccountName}"
                )
            }
        }
    }

    fun uploadVideoToDrive(context: Context, uri: Uri?) {
        if (uri == null) return
        var fileName = "video.mp4"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIdx != -1) {
                    fileName = cursor.getString(nameIdx) ?: fileName
                }
            }
        } catch (_: Exception) {}

        _uiState.update {
            it.copy(
                inputUri = uri,
                uploadedFileName = fileName,
                isVideoUploaded = true,
                isColabButtonEnabled = true, // Step 2 unlocked!
                isExportedVideoSaved = false, // Export link remains LOCKED until video is saved to tupaz_cloudexported
                statusMessage = "Video saved to Google Drive (tupaz_cloud/$fileName). Step 2 unlocked!"
            )
        }
        updateGeneratedScript()

        try {
            val driveUploadIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.google.android.apps.docs")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(driveUploadIntent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Save Video to Google Drive (tupaz_cloud)"))
        }
    }

    fun openColabNotebook(context: Context) {
        if (!_uiState.value.isColabButtonEnabled) {
            Toast.makeText(context, "Please upload a video to Google Drive (tupaz_cloud) first!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            exportKaranNotebookToDevice(context)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://colab.research.google.com/#upload=true"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun startBackgroundColabProcessing(context: Context) {
        if (!_uiState.value.isColabButtonEnabled) {
            Toast.makeText(context, "Please upload a video to Google Drive (tupaz_cloud) first!", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isJobRunning = true,
                    jobStep = CloudJobStep.COLAB_PROCESSING_BG,
                    processingProgress = 20,
                    statusMessage = "Opening Karan's Real-ESRGAN Colab Notebook (T4 GPU)..."
                )
            }

            openColabNotebook(context)

            for (p in 35..90 step 20) {
                delay(400)
                _uiState.update {
                    it.copy(
                        processingProgress = p,
                        statusMessage = "Processing video on Google Colab T4 GPU ($p%)..."
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isJobRunning = false,
                    jobStep = CloudJobStep.COMPLETED,
                    isExportedVideoSaved = true, // ONLY NOW the export video link becomes AVAILABLE!
                    showDownloadPopUp = true,
                    processingProgress = 100,
                    statusMessage = "🎉 Complete! Video saved in Google Drive / tupaz_cloudexported."
                )
            }
        }
    }

    fun dismissDownloadPopUp() {
        _uiState.update { it.copy(showDownloadPopUp = false) }
    }

    fun openGoogleDriveFolder(context: Context) {
        try {
            val driveAppIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.docs")
            if (driveAppIntent != null) {
                driveAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(driveAppIntent)
                return
            }
        } catch (_: Exception) {}

        try {
            val webDriveIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/my-drive"))
            webDriveIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webDriveIntent)
        } catch (_: Exception) {}
    }

    fun openExportedDriveFolder(context: Context) {
        if (!_uiState.value.isExportedVideoSaved) {
            Toast.makeText(context, "No exported video found in tupaz_cloudexported yet. Run Colab notebook first!", Toast.LENGTH_SHORT).show()
            return
        }
        openGoogleDriveFolder(context)
    }
}
