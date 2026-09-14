/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package net.pixelos.ota

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import net.pixelos.ota.ui.theme.OpendeltaTheme
import net.pixelos.ota.controller.UpdaterController
import net.pixelos.ota.data.Update
import net.pixelos.ota.data.UpdateStatus
import net.pixelos.ota.preferences.PreferencesActivity
import net.pixelos.ota.ui.common.ProgressDialog
import net.pixelos.ota.ui.SystemUpdateScreen
import net.pixelos.ota.updates.action.AlertDialogState
import net.pixelos.ota.updates.action.UpdateActionDialog
import net.pixelos.ota.updates.action.UpdateActionHandler
import net.pixelos.ota.updates.state.UpdateItemStateMapper
import net.pixelos.ota.updatescheck.UpdatesCheckState
import net.pixelos.ota.updatescheck.rememberUpdatesCheckUiState

abstract class UpdatesScaffoldActivity : ComponentActivity() {
    private val viewModel by viewModels<UpdatesViewModel>()
    private var activeUpdaterController: UpdaterController? by mutableStateOf(null)
    private var controllerStateVersion: Int by mutableIntStateOf(0)

    protected var importDialogVisible: Boolean by mutableStateOf(false)

    private val preferencesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == PreferencesActivity.RESULT_LOCAL_UPDATE) {
            onLocalUpdateClick()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setupCompose() {
        setContent {
            val navController = remember {
                object : NavControllerWrapper {
                    override fun navigate(route: String, popUpCurrent: Boolean) {}
                    override fun navigateBack() = finish()
                }
            }

            CompositionLocalProvider(LocalNavController provides navController) {
                OpendeltaTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    UpdatesScaffoldContent(
                        uiState = uiState,
                        updaterController = activeUpdaterController,
                        controllerStateVersion = controllerStateVersion,
                        onBackClick = { finish() },
                        onRefreshClick = { onRefreshClick() },
                        onLocalUpdateClick = { onLocalUpdateClick() },
                        onPreferencesClick = {
                            preferencesLauncher.launch(
                                Intent(
                                    this@UpdatesScaffoldActivity,
                                    PreferencesActivity::class.java,
                                )
                            )
                        },
                        onControllerStateChanged = { notifyControllerStateChanged() },
                    )
                    if (importDialogVisible) {
                        ProgressDialog(
                            title = stringResource(R.string.local_update_import),
                            text = stringResource(R.string.local_update_import_progress),
                        )
                    }
                }
            }
        }
    }

    protected fun setUpdaterController(controller: UpdaterController?) {
        activeUpdaterController = controller
        notifyControllerStateChanged()
    }

    protected fun notifyControllerStateChanged() {
        controllerStateVersion++
    }

    open fun onRefreshClick() {}
    open fun onLocalUpdateClick() {}
    open fun exportUpdate(update: Update) {}
}

@Composable
private fun UpdatesScaffoldContent(
    uiState: UpdatesViewModel.UiState,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onControllerStateChanged: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as UpdatesScaffoldActivity
    val networkMonitor =
        remember { (context.applicationContext as UpdaterApplication).networkMonitor }
    val networkState by networkMonitor.networkState.collectAsState(
        initial = networkMonitor.currentNetworkState,
    )
    val userPreferencesRepository =
        remember { (context.applicationContext as UpdaterApplication).userPreferencesRepository }
    val streamUpdatesEnabled by userPreferencesRepository.streamUpdatesFlow.collectAsState(
        initial = true,
    )

    val updateItems = remember(
        uiState.updates,
        updaterController,
        networkState,
        streamUpdatesEnabled,
        controllerStateVersion,
    ) {
        val controller = updaterController ?: return@remember emptyList()
        val mapper = UpdateItemStateMapper(context, controller, streamUpdatesEnabled)
        uiState.updates.mapNotNull { update ->
            controller.getUpdate(update.downloadId)?.let {
                mapper.map(it, networkState)
            }
        }
    }

    val actionDialogState = remember { mutableStateOf<AlertDialogState?>(null) }
    actionDialogState.value?.let { dialog ->
        UpdateActionDialog(
            dialog = dialog,
            onDismiss = { actionDialogState.value = null },
        )
    }

    val actionHandler = remember(updaterController) {
        updaterController?.let { controller ->
            UpdateActionHandler(
                activity = activity,
                updaterController = controller,
                exportUpdate = { update -> activity.exportUpdate(update) },
                showDialog = { actionDialogState.value = it },
            )
        }
    }

    val model = uiState.updatesCheckModel
    val checkUiState = rememberUpdatesCheckUiState(model.state)
    val isChecking = checkUiState.displayedState is UpdatesCheckState.Checking
    val isPreparing = uiState.updates.any { it.status == UpdateStatus.STARTING }
    val isBusy = isChecking || isPreparing
    val isIdleAndEmpty = updateItems.isEmpty() && !isBusy
    val isCheckError = checkUiState.displayedState is UpdatesCheckState.NoInternet ||
            checkUiState.displayedState is UpdatesCheckState.Error

    var hasCheckedInSession by remember { mutableStateOf(false) }
    LaunchedEffect(checkUiState.displayedState) {
        if (checkUiState.displayedState is UpdatesCheckState.Checking) {
            hasCheckedInSession = true
        }
    }

    val activeItem = updateItems.firstOrNull { it.progress != null }
        ?: updaterController?.getDisplayUpdateId()?.let { id ->
            updateItems.firstOrNull { it.downloadId == id }
        } ?: updateItems.firstOrNull()

    SystemUpdateScreen(
        supportingText = when (checkUiState.displayedState) {
            UpdatesCheckState.NoInternet ->
                stringResource(R.string.check_your_internet_connection)

            UpdatesCheckState.Error -> stringResource(R.string.updates_check_failed)
            else -> null
        },
        supportingTextIsError = isCheckError,
        isBusy = isBusy,
        hasCheckedInSession = hasCheckedInSession && !isCheckError,
        canCheckForUpdates = model.canCheckForUpdates,
        onBackClick = onBackClick,
        onCheckClick = onRefreshClick,
        onPreferencesClick = onPreferencesClick,
        updateItem = activeItem,
        deviceStatus = uiState.deviceStatus,
        maintainerInfo = uiState.maintainerInfo,
        changelogState = uiState.changelogState,
        onUpdateAction = { action ->
            val item = activeItem
            val controller = updaterController
            if (item != null && controller != null) {
                controller.getUpdate(item.downloadId)?.let { update ->
                    actionHandler?.perform(action, update)
                    onControllerStateChanged()
                }
            }
        },
    )
}
