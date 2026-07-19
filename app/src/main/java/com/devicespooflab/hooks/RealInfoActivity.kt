package com.devicespooflab.hooks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.pm.PackageInfoCompat
import com.devicespooflab.hooks.data.AppSettingsStore
import com.devicespooflab.hooks.ui.ConnectedInfoRow
import com.devicespooflab.hooks.ui.ConnectedSettingsRow
import com.devicespooflab.hooks.ui.SectionTitle
import com.devicespooflab.hooks.ui.SpoofMyDeviceTheme

class RealInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettingsStore.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpoofMyDeviceTheme {
                AppInfoScreen(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppInfoScreen(activity: RealInfoActivity) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val packageInfo = remember { activity.packageManager.getPackageInfo(activity.packageName, 0) }
    val appIcon = remember {
        activity.applicationInfo.loadIcon(activity.packageManager).toBitmap(128, 128).asImageBitmap()
    }
    val overviewRows = listOf(
        stringResource(R.string.app_info_description_title) to stringResource(R.string.xposed_description),
        stringResource(R.string.app_info_package_title) to activity.packageName,
        stringResource(
            R.string.app_info_version_format,
            packageInfo.versionName.orEmpty(),
            PackageInfoCompat.getLongVersionCode(packageInfo),
        ) to
            stringResource(R.string.app_name),
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.toolbar_real_info)) },
                navigationIcon = {
                    IconButton(onClick = activity::finish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.safe_mode_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
        ) {
            item {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(76.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(
                                    R.string.app_info_version_format,
                                    packageInfo.versionName.orEmpty(),
                                    PackageInfoCompat.getLongVersionCode(packageInfo),
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.app_info_overview_title), Modifier.padding(top = 22.dp)) }
            overviewRows.forEachIndexed { index, row ->
                item { ConnectedInfoRow(index, overviewRows.size, row.first, row.second) }
            }
            item { SectionTitle(stringResource(R.string.app_info_credits_title), Modifier.padding(top = 22.dp)) }
            item { ConnectedInfoRow(0, 1, stringResource(R.string.app_info_credits_title), stringResource(R.string.app_info_credits_body)) }
            item { SectionTitle(stringResource(R.string.app_info_links_title), Modifier.padding(top = 22.dp)) }
            item {
                ConnectedSettingsRow(
                    index = 0,
                    count = 1,
                    title = stringResource(R.string.app_info_github_title),
                    summary = stringResource(R.string.app_info_github_summary),
                    icon = Icons.Outlined.Code,
                    onClick = {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                    },
                )
            }
            item { Spacer(Modifier.height(36.dp)) }
        }
    }
}

private const val GITHUB_URL = "https://github.com/BuSung-dev/SpoofMyDevice"
