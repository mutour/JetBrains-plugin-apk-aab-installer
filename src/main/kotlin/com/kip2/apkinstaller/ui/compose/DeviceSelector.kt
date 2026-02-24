package com.kip2.apkinstaller.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.model.DeviceState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import com.kip2.apkinstaller.InstallerBundle
@Composable
fun DeviceSelector(
    devices: List<Device>,
    onSelectionChanged: (List<Device>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val allSelected = devices.isNotEmpty() && selectedIds.size == devices.size

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = allSelected,
                onCheckedChange = { checked ->
                    selectedIds = if (checked) devices.map { it.id }.toSet() else emptySet()
                    onSelectionChanged(devices.filter { it.id in selectedIds })
                }
            )
            Text(InstallerBundle.message("device.selector.select.all"), modifier = Modifier.padding(start = 8.dp))
        }

        Divider(Orientation.Horizontal)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (devices.isEmpty()) {
                Text(
                    InstallerBundle.message("device.no.devices"),
                    modifier = Modifier.align(Alignment.Center),
                    style = JewelTheme.defaultTextStyle
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(devices) { device ->
                        DeviceItem(
                            device = device,
                            isSelected = device.id in selectedIds,
                            onSelectedChange = { checked ->
                                selectedIds = if (checked) selectedIds + device.id else selectedIds - device.id
                                onSelectionChanged(devices.filter { it.id in selectedIds })
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: Device,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = onSelectedChange)
        
        Spacer(Modifier.width(12.dp))
        
        Column {
            Text(
                text = "${device.model} [${device.id}]",
                style = JewelTheme.defaultTextStyle
            )
            Text(
                text = "API ${device.apiLevel} • ${device.state.name.lowercase().capitalize()}",
                style = JewelTheme.defaultTextStyle,
                color = if (device.state == DeviceState.ONLINE) 
                    JewelTheme.globalColors.text.normal 
                else 
                    JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
            )
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { it.uppercase() }
