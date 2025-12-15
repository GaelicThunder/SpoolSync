package dev.gaelicthunder.spoolsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.gaelicthunder.spoolsync.auth.GoogleAuthManager
import dev.gaelicthunder.spoolsync.ui.SpoolSyncNavigation
import dev.gaelicthunder.spoolsync.ui.SpoolSyncViewModel
import dev.gaelicthunder.spoolsync.ui.theme.SpoolSyncTheme

class MainActivity : ComponentActivity() {

    private lateinit var authManager: GoogleAuthManager
    private lateinit var viewModel: SpoolSyncViewModel

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = authManager.handleSignInResult(result.data)
        if (account != null) {
            viewModel.onGoogleSignInSuccess(this, account)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authManager = GoogleAuthManager(this)

        setContent {
            SpoolSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    viewModel = viewModel()
                    
                    LaunchedEffect(Unit) {
                        viewModel.setAuthManager(authManager, signInLauncher)
                        authManager.getLastSignedInAccount()?.let { account ->
                            viewModel.onGoogleSignInSuccess(this@MainActivity, account)
                        }
                    }
                    
                    SpoolSyncNavigation(viewModel)
                }
            }
        }
    }
}
