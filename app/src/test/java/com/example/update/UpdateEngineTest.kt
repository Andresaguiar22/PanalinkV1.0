package com.example.update

import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.content.pm.Signature
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.io.IOException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UpdateEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testFile: File
    private val expectedContent = "PanaLinkTestAPKContent"
    // SHA-256 of "PanaLinkTestAPKContent"
    private val correctSha256 = "c321453d588fd6d6fc35105330239263afdc845e44bd9cc26986becebb37e598"
    private val incorrectSha256 = "0000000000000000000000000000000000000000000000000000000000000000"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }
        testFile = File(updatesDir, "test_panalink_update.apk")
        testFile.writeText(expectedContent)
    }

    @After
    fun tearDown() {
        if (::testFile.isInitialized && testFile.exists()) {
            testFile.delete()
        }
    }

    // ==========================================
    // 1. VERSION COMPARISON TESTS
    // ==========================================
    @Test
    fun testVersionComparison_Equal() {
        val currentVersionCode = 100L
        val remoteVersionInfo = AppVersionInfo(
            versionCode = 100L,
            versionName = "1.0.0",
            downloadUrl = "https://example.com/download",
            changelog = emptyList(),
            mandatory = false,
            sha256 = "hash",
            minimumSupportedVersionCode = 90L
        )

        val versionManager = object : AppVersionManager(ApplicationProvider.getApplicationContext()) {
            override fun getCurrentVersionCode(): Long = currentVersionCode
            override fun getCurrentVersionName(): String = "1.0.0"
        }

        val status = versionManager.checkUpdateStatus(remoteVersionInfo)
        assertEquals(UpdateStatus.UP_TO_DATE, status)
    }

    @Test
    fun testVersionComparison_Superior() {
        val currentVersionCode = 100L
        val remoteVersionInfo = AppVersionInfo(
            versionCode = 150L,
            versionName = "1.5.0",
            downloadUrl = "https://example.com/download",
            changelog = emptyList(),
            mandatory = false,
            sha256 = "hash",
            minimumSupportedVersionCode = 90L
        )

        val versionManager = object : AppVersionManager(ApplicationProvider.getApplicationContext()) {
            override fun getCurrentVersionCode(): Long = currentVersionCode
            override fun getCurrentVersionName(): String = "1.0.0"
        }

        val status = versionManager.checkUpdateStatus(remoteVersionInfo)
        assertEquals(UpdateStatus.UPDATE_AVAILABLE, status)
    }

    @Test
    fun testVersionComparison_Inferior_DowngradePrevention() {
        val currentVersionCode = 200L
        val remoteVersionInfo = AppVersionInfo(
            versionCode = 100L,
            versionName = "1.0.0",
            downloadUrl = "https://example.com/download",
            changelog = emptyList(),
            mandatory = true,
            sha256 = "hash",
            minimumSupportedVersionCode = 90L
        )

        val versionManager = object : AppVersionManager(ApplicationProvider.getApplicationContext()) {
            override fun getCurrentVersionCode(): Long = currentVersionCode
            override fun getCurrentVersionName(): String = "2.0.0"
        }

        // Downgrade should be marked as UP_TO_DATE to prevent downgrade prompt
        val status = versionManager.checkUpdateStatus(remoteVersionInfo)
        assertEquals(UpdateStatus.UP_TO_DATE, status)
    }

    @Test
    fun testVersionComparison_MandatoryUpdateRequired() {
        val currentVersionCode = 100L
        val remoteVersionInfo = AppVersionInfo(
            versionCode = 150L,
            versionName = "1.5.0",
            downloadUrl = "https://example.com/download",
            changelog = emptyList(),
            mandatory = false,
            sha256 = "hash",
            // Current version code (100) is lower than minimum supported (110)
            minimumSupportedVersionCode = 110L
        )

        val versionManager = object : AppVersionManager(ApplicationProvider.getApplicationContext()) {
            override fun getCurrentVersionCode(): Long = currentVersionCode
            override fun getCurrentVersionName(): String = "1.0.0"
        }

        val status = versionManager.checkUpdateStatus(remoteVersionInfo)
        assertEquals(UpdateStatus.MANDATORY_UPDATE, status)
    }

    // ==========================================
    // 2. MANIFEST PARSING TESTS
    // ==========================================
    @Test
    fun testManifestParsing_Valid() = kotlinx.coroutines.runBlocking {
        val mockClient = createMockHttpClient { request ->
            val json = """
                {
                  "versionCode": 200,
                  "versionName": "2.0.0",
                  "downloadUrl": "https://example.com/panalink.apk",
                  "sha256": "66f776269bdfa8a49ba5e1cb2a488c03260beeb03083dbca80dfdb17631da7e3",
                  "mandatory": true,
                  "minimumSupportedVersionCode": 150,
                  "changelog": ["Nuevas llamadas", "Mejoras de UI"]
                }
            """.trimIndent()
            createMockResponse(request, json)
        }

        val config = UpdateConfig("https://example.com/manifest.json")
        val repo = HttpUpdateManifestRepository(config, mockClient)

        val result = repo.fetchUpdateManifest()
        assertTrue(result.isSuccess)

        val info = result.getOrNull()!!
        assertEquals(200L, info.versionCode)
        assertEquals("2.0.0", info.versionName)
        assertEquals("https://example.com/panalink.apk", info.downloadUrl)
        assertEquals("66f776269bdfa8a49ba5e1cb2a488c03260beeb03083dbca80dfdb17631da7e3", info.sha256)
        assertTrue(info.mandatory)
        assertEquals(150L, info.minimumSupportedVersionCode)
        assertEquals(2, info.changelog.size)
        assertEquals("Nuevas llamadas", info.changelog[0])
    }

    @Test
    fun testManifestParsing_Incomplete() = kotlinx.coroutines.runBlocking {
        val mockClient = createMockHttpClient { request ->
            // Missing downloadUrl and sha256
            val json = """
                {
                  "versionCode": 200,
                  "versionName": "2.0.0"
                }
            """.trimIndent()
            createMockResponse(request, json)
        }

        val config = UpdateConfig("https://example.com/manifest.json")
        val repo = HttpUpdateManifestRepository(config, mockClient)

        val result = repo.fetchUpdateManifest()
        assertTrue(result.isFailure)
    }

    @Test
    fun testManifestParsing_InvalidUrl() = kotlinx.coroutines.runBlocking {
        val config = UpdateConfig("") // Empty URL
        val repo = HttpUpdateManifestRepository(config)

        val result = repo.fetchUpdateManifest()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    // ==========================================
    // 3. INTEGRITY TESTS
    // ==========================================
    private fun calculateSha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testIntegrity_Correct() {
        val dynamicCorrectSha = calculateSha256(testFile)
        val isVerified = ApkIntegrityVerifier.verifySha256(testFile, dynamicCorrectSha)
        assertTrue(isVerified)
    }

    @Test
    fun testIntegrity_Incorrect() {
        val isVerified = ApkIntegrityVerifier.verifySha256(testFile, incorrectSha256)
        assertFalse(isVerified)
    }

    @Test
    fun testIntegrity_FileNotExist() {
        val missingFile = File(tempFolder.root, "non_existent.apk")
        val isVerified = ApkIntegrityVerifier.verifySha256(missingFile, correctSha256)
        assertFalse(isVerified)
    }

    // ==========================================
    // 4. UPDATE STATUS AND CHECKS
    // ==========================================
    @Test
    fun testUpdateChecker_StateTransitions() = kotlinx.coroutines.runBlocking {
        val remoteVersionInfo = AppVersionInfo(
            versionCode = 150L,
            versionName = "1.5.0",
            downloadUrl = "https://example.com/download",
            changelog = emptyList(),
            mandatory = false,
            sha256 = "hash",
            minimumSupportedVersionCode = 90L
        )

        val mockRepo = object : UpdateManifestRepository {
            override suspend fun fetchUpdateManifest(): Result<AppVersionInfo> {
                return Result.success(remoteVersionInfo)
            }
        }

        val versionManager = object : AppVersionManager(ApplicationProvider.getApplicationContext()) {
            override fun getCurrentVersionCode(): Long = 100L
            override fun getCurrentVersionName(): String = "1.0.0"
        }

        val checker = UpdateChecker(mockRepo, versionManager)
        assertEquals(UpdateStatus.IDLE, checker.state.value)

        val finalStatus = checker.checkForUpdates(force = true)
        assertEquals(UpdateStatus.UPDATE_AVAILABLE, finalStatus)
        assertEquals(UpdateStatus.UPDATE_AVAILABLE, checker.state.value)
        assertEquals(remoteVersionInfo, checker.latestVersionInfo.value)
    }

    // ==========================================
    // 5. OFFLINE BEHAVIOR TESTS
    // ==========================================
    @Test
    fun testOfflineBehavior_NoCache_GracefulFallback() = kotlinx.coroutines.runBlocking {
        val mockRepo = object : UpdateManifestRepository {
            override suspend fun fetchUpdateManifest(): Result<AppVersionInfo> {
                return Result.failure(UnknownHostException("No internet connection"))
            }
        }

        val versionManager = object : AppVersionManager(ApplicationProvider.getApplicationContext()) {
            override fun getCurrentVersionCode(): Long = 100L
            override fun getCurrentVersionName(): String = "1.0.0"
        }

        val checker = UpdateChecker(mockRepo, versionManager)
        val status = checker.checkForUpdates(force = true)
        
        // Offline should fallback to UP_TO_DATE gracefully to avoid blocking user offline
        assertEquals(UpdateStatus.UP_TO_DATE, status)
    }

    // ==========================================
    // 6. HARDENING & SECURITY TESTS (P6.8.1)
    // ==========================================

    @Test
    fun testRejectWrongPackageName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = org.robolectric.Shadows.shadowOf(context.packageManager)

        // Setup installed package signatures
        val pm = context.packageManager
        val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        @Suppress("DEPRECATION")
        installedInfo.signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
        shadowPackageManager.addPackage(installedInfo)

        // Setup archive with mismatched package name
        val archiveInfo = PackageInfo().apply {
            packageName = "com.wrong.package"
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
            versionCode = 150
        }
        shadowPackageManager.setPackageArchiveInfo(testFile.absolutePath, archiveInfo)

        val versionManager = object : AppVersionManager(context) {
            override fun getCurrentVersionCode(): Long = 100L
        }
        val installer = AndroidPackageInstaller(context, versionManager)

        val result = installer.installApk(testFile, correctSha256)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testRejectDowngrade() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = org.robolectric.Shadows.shadowOf(context.packageManager)

        // Setup installed package
        val pm = context.packageManager
        val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        @Suppress("DEPRECATION")
        installedInfo.signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
        shadowPackageManager.addPackage(installedInfo)

        // Setup archive with lower version code
        val archiveInfo = PackageInfo().apply {
            packageName = context.packageName
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
            versionCode = 50
        }
        shadowPackageManager.setPackageArchiveInfo(testFile.absolutePath, archiveInfo)

        val versionManager = object : AppVersionManager(context) {
            override fun getCurrentVersionCode(): Long = 100L
        }
        val installer = AndroidPackageInstaller(context, versionManager)

        val result = installer.installApk(testFile, correctSha256)
        assertTrue("Expected failure but was success", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Expected IllegalArgumentException but was ${exception?.javaClass?.name}: ${exception?.message}", exception is IllegalArgumentException)
    }

    @Test
    fun testRejectSameVersion() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = org.robolectric.Shadows.shadowOf(context.packageManager)

        // Setup installed package
        val pm = context.packageManager
        val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        @Suppress("DEPRECATION")
        installedInfo.signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
        shadowPackageManager.addPackage(installedInfo)

        // Setup archive with identical version code
        val archiveInfo = PackageInfo().apply {
            packageName = context.packageName
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
            versionCode = 100
        }
        shadowPackageManager.setPackageArchiveInfo(testFile.absolutePath, archiveInfo)

        val versionManager = object : AppVersionManager(context) {
            override fun getCurrentVersionCode(): Long = 100L
        }
        val installer = AndroidPackageInstaller(context, versionManager)

        val result = installer.installApk(testFile, correctSha256)
        assertTrue("Expected failure but was success", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Expected IllegalArgumentException but was ${exception?.javaClass?.name}: ${exception?.message}", exception is IllegalArgumentException)
    }

    @Test
    fun testAcceptHigherVersion() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = org.robolectric.Shadows.shadowOf(context.packageManager)

        // Setup installed package
        val pm = context.packageManager
        val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        @Suppress("DEPRECATION")
        installedInfo.signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
        shadowPackageManager.addPackage(installedInfo)

        // Setup archive with higher version code
        val archiveInfo = PackageInfo().apply {
            packageName = context.packageName
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
            versionCode = 150
        }
        shadowPackageManager.setPackageArchiveInfo(testFile.absolutePath, archiveInfo)

        val versionManager = object : AppVersionManager(context) {
            override fun getCurrentVersionCode(): Long = 100L
        }
        val installer = AndroidPackageInstaller(context, versionManager)

        val result = installer.installApk(testFile, correctSha256)
        val exception = result.exceptionOrNull()
        assertTrue("Expected success but failed with: ${exception?.message}", result.isSuccess)
    }

    @Test
    fun testRejectInvalidSigningCertificate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = org.robolectric.Shadows.shadowOf(context.packageManager)

        // Setup installed package with one signature
        val pm = context.packageManager
        val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        @Suppress("DEPRECATION")
        installedInfo.signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
        shadowPackageManager.addPackage(installedInfo)

        // Setup archive with different signature
        val archiveInfo = PackageInfo().apply {
            packageName = context.packageName
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature(byteArrayOf(9, 9, 9, 9)))
            versionCode = 150
        }
        shadowPackageManager.setPackageArchiveInfo(testFile.absolutePath, archiveInfo)

        val versionManager = object : AppVersionManager(context) {
            override fun getCurrentVersionCode(): Long = 100L
        }
        val installer = AndroidPackageInstaller(context, versionManager)

        val result = installer.installApk(testFile, correctSha256)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testRejectMissingCertificate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPackageManager = org.robolectric.Shadows.shadowOf(context.packageManager)

        // Setup installed package
        val pm = context.packageManager
        val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        @Suppress("DEPRECATION")
        installedInfo.signatures = arrayOf(Signature(byteArrayOf(1, 2, 3, 4)))
        shadowPackageManager.addPackage(installedInfo)

        // Setup archive with no signature
        val archiveInfo = PackageInfo().apply {
            packageName = context.packageName
            @Suppress("DEPRECATION")
            signatures = null
            versionCode = 150
        }
        shadowPackageManager.setPackageArchiveInfo(testFile.absolutePath, archiveInfo)

        val versionManager = object : AppVersionManager(context) {
            override fun getCurrentVersionCode(): Long = 100L
        }
        val installer = AndroidPackageInstaller(context, versionManager)

        val result = installer.installApk(testFile, correctSha256)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testUrlSecurity_HttpsProduction() = kotlinx.coroutines.runBlocking {
        val config = UpdateConfig(manifestUrl = "https://example.com/manifest.json", isProduction = true)
        val mockClient = createMockHttpClient { request ->
            val json = """
                {
                  "versionCode": 200,
                  "versionName": "2.0.0",
                  "downloadUrl": "https://example.com/panalink.apk",
                  "sha256": "66f776269bdfa8a49ba5e1cb2a488c03260beeb03083dbca80dfdb17631da7e3"
                }
            """.trimIndent()
            createMockResponse(request, json)
        }
        val repo = HttpUpdateManifestRepository(config, mockClient)
        val result = repo.fetchUpdateManifest()
        assertTrue(result.isSuccess)
    }

    @Test
    fun testUrlSecurity_HttpProduction() = kotlinx.coroutines.runBlocking {
        val config = UpdateConfig(manifestUrl = "https://example.com/manifest.json", isProduction = true)
        val mockClient = createMockHttpClient { request ->
            val json = """
                {
                  "versionCode": 200,
                  "versionName": "2.0.0",
                  "downloadUrl": "http://example.com/panalink.apk",
                  "sha256": "66f776269bdfa8a49ba5e1cb2a488c03260beeb03083dbca80dfdb17631da7e3"
                }
            """.trimIndent()
            createMockResponse(request, json)
        }
        val repo = HttpUpdateManifestRepository(config, mockClient)
        val result = repo.fetchUpdateManifest()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testManifestPermissionsAndProvider() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        assertTrue(
            "REQUEST_INSTALL_PACKAGES permission must be declared in AndroidManifest.xml",
            requestedPermissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")
        )

        val providerInfo = pm.getProviderInfo(
            android.content.ComponentName(context.packageName, "androidx.core.content.FileProvider"),
            0
        )
        assertNotNull("FileProvider must be declared in AndroidManifest.xml", providerInfo)
        assertEquals("FileProvider authority is incorrect", "${context.packageName}.provider", providerInfo.authority)
    }

    @Test
    fun testDownloadApk_InsufficientSpace() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockClient = createMockHttpClient { request ->
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Length", "10000000000000000") // Very large size to trigger space check failure
                .body("Dummy data".toByteArray().toResponseBody("application/vnd.android.package-archive".toMediaTypeOrNull()))
                .build()
        }

        val downloadManager = ApkDownloadManager(context, mockClient)
        val file = downloadManager.downloadApk(
            downloadUrl = "https://example.com/update.apk",
            expectedSha256 = "66f776269bdfa8a49ba5e1cb2a488c03260beeb03083dbca80dfdb17631da7e3",
            versionCode = 2L
        )

        assertNull(file)
        val state = downloadManager.downloadState.value
        assertTrue(state is DownloadState.Error)
        val errorMsg = (state as DownloadState.Error).message
        assertTrue(errorMsg.contains("Espacio en disco insuficiente") || errorMsg.contains("storage space") || errorMsg.contains("insuficiente"))
    }

    @Test
    fun testDownloadApk_Success_ReuseCache() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val updatesDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
        
        // Compute actual SHA of "Matching content" first
        val dummyFile = File(updatesDir, "dummy_temp.txt").apply { writeText("Matching content") }
        val computedSha = calculateSha256(dummyFile)
        dummyFile.delete()

        val shaPrefix = computedSha.take(8)
        val destinationFile = File(updatesDir, "panalink_update_2_$shaPrefix.apk")
        destinationFile.writeText("Matching content")

        val downloadManager = ApkDownloadManager(context, OkHttpClient())
        val file = downloadManager.downloadApk(
            downloadUrl = "https://example.com/update.apk",
            expectedSha256 = computedSha,
            versionCode = 2L
        )

        assertNotNull("File should not be null, it should be retrieved from cache", file)
        assertEquals(destinationFile.absolutePath, file?.absolutePath)
        assertTrue(downloadManager.downloadState.value is DownloadState.Success)
    }

    @Test
    fun testUrlSecurity_ManifestHttpProduction_Blocked() = kotlinx.coroutines.runBlocking {
        val config = UpdateConfig(manifestUrl = "http://example.com/manifest.json", isProduction = true)
        val repo = HttpUpdateManifestRepository(config, OkHttpClient())
        val result = repo.fetchUpdateManifest()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testManifestParsing_InvalidVersionFields() = kotlinx.coroutines.runBlocking {
        val mockClient = createMockHttpClient { request ->
            val json = """
                {
                  "versionCode": 0,
                  "versionName": "",
                  "downloadUrl": "https://example.com/panalink.apk",
                  "sha256": "66f776269bdfa8a49ba5e1cb2a488c03260beeb03083dbca80dfdb17631da7e3"
                }
            """.trimIndent()
            createMockResponse(request, json)
        }

        val config = UpdateConfig("https://example.com/manifest.json")
        val repo = HttpUpdateManifestRepository(config, mockClient)

        val result = repo.fetchUpdateManifest()
        assertTrue(result.isFailure)
    }

    @Test
    fun testGitHubProvider_Success() = kotlinx.coroutines.runBlocking {
        val config = UpdateConfig(
            manifestUrl = "https://example.com/manifest.json",
            providerType = UpdateConfig.ProviderType.GITHUB_RELEASES,
            isProduction = true,
            githubOwner = "my-owner",
            githubRepo = "my-repo",
        )
        
        val mockClient = createMockHttpClient { request ->
            val json = """
                {
                  "versionCode": 2,
                  "versionName": "2.0.0",
                  "downloadUrl": "https://github.com/my-owner/my-repo/releases/download/v2.0.0/panalink-release-2.0.0.apk",
                  "sha256": "66f776269bdfa8a49ba5e1cb2a488c03260beeb03083dbca80dfdb17631da7e3"
                }
            """.trimIndent()
            createMockResponse(request, json)
        }

        val repo = GitHubUpdateManifestRepository(config, mockClient)
        val result = repo.fetchUpdateManifest()
        assertTrue(result.isSuccess)
        val info = result.getOrNull()
        assertNotNull(info)
        assertEquals(2L, info?.versionCode)
        assertEquals("2.0.0", info?.versionName)
        assertEquals("https://github.com/my-owner/my-repo/releases/download/v2.0.0/panalink-release-2.0.0.apk", info?.downloadUrl)
    }

    @Test
    fun testGitHubProvider_MismatchedRepositoryInProduction() = kotlinx.coroutines.runBlocking {
        val config = UpdateConfig(
            manifestUrl = "https://example.com/manifest.json",
            providerType = UpdateConfig.ProviderType.GITHUB_RELEASES,
            isProduction = true,
            githubOwner = "my-owner",
            githubRepo = "my-repo",
        )
        
        val mockClient = createMockHttpClient { request ->
            val json = """
                {
                  "versionCode": 2,
                  "versionName": "2.0.0",
                  "downloadUrl": "https://github.com/unauthorized-owner/unauthorized-repo/releases/download/v2.0.0/attack.apk",
                  "sha256": "66f776269bdfa8a49ba5e1cb2a488c03260beeb03083dbca80dfdb17631da7e3"
                }
            """.trimIndent()
            createMockResponse(request, json)
        }

        val repo = GitHubUpdateManifestRepository(config, mockClient)
        val result = repo.fetchUpdateManifest()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        val errorMsg = result.exceptionOrNull()?.message
        assertTrue(errorMsg?.contains("Production GITHUB_RELEASES download URL must point to the configured repository") == true)
    }

    @Test
    fun testGitHubConfig_EmptyConfig() = kotlinx.coroutines.runBlocking {
        val config = UpdateConfig(
            manifestUrl = "https://example.com/manifest.json",
            providerType = UpdateConfig.ProviderType.GITHUB_RELEASES,
            isProduction = true,
            githubOwner = "",
            githubRepo = "",
        )
        val repo = GitHubUpdateManifestRepository(config, OkHttpClient())
        val result = repo.fetchUpdateManifest()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    // ==========================================
    // HELPERS FOR MOCK HTTP
    // ==========================================
    private fun createMockHttpClient(
        interceptor: (okhttp3.Request) -> Response
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                interceptor(chain.request())
            }
            .build()
    }

    private fun createMockResponse(request: okhttp3.Request, json: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(json.toResponseBody("application/json".toMediaTypeOrNull()))
            .build()
    }
}
