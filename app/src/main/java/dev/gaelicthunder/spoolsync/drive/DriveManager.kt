package dev.gaelicthunder.spoolsync.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import com.google.api.client.http.ByteArrayContent

class DriveManager(private val context: Context, account: GoogleSignInAccount) {

    private val credential = GoogleAccountCredential.usingOAuth2(
        context,
        listOf(
            "https://www.googleapis.com/auth/drive.file",
            "https://www.googleapis.com/auth/drive.appdata"
        )
    ).apply {
        selectedAccount = account.account
    }

    private val driveService = Drive.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    )
        .setApplicationName("SpoolSync")
        .build()

    suspend fun uploadBackup(filename: String, content: String): String? = withContext(Dispatchers.IO) {
        try {
            val existing = findFile(filename)
            
            val fileMetadata = File().apply {
                name = filename
                mimeType = "application/json"
                if (existing == null) {
                    parents = listOf("appDataFolder")
                }
            }

            val mediaContent = ByteArrayContent.fromString("application/json", content)

            val file = if (existing != null) {
                driveService.files().update(existing.id, fileMetadata, mediaContent).execute()
            } else {
                driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            }

            file.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadBackup(filename: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = findFile(filename) ?: return@withContext null

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun findFile(filename: String): File? {
        return try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$filename'")
                .setFields("files(id, name)")
                .execute()

            result.files.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun listBackups(): List<String> = withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, modifiedTime)")
                .execute()

            result.files.map { it.name }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
