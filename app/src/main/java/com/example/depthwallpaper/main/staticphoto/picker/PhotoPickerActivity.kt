package com.example.depthwallpaper.main.staticphoto.picker

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.setLeft
import com.example.depthwallpaper.core.setRight
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.core.show
import com.example.depthwallpaper.databinding.ActivityPhotoPickerBinding
import com.example.depthwallpaper.main.staticphoto.Collages

/**
 * "Choose photo": the device gallery as a 3-column grid with a folder switcher ("Recents" / "Choose
 * folder") and a tray of the chosen photos. Returns the chosen photos as a ClipData of content Uris.
 */
class PhotoPickerActivity : AbsBaseActivity<ActivityPhotoPickerBinding>(ActivityPhotoPickerBinding::inflate) {
    private val vm: PickerViewModel by viewModels()
    private lateinit var grid: PhotoGridAdapter
    private val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun initView() {
        grid = PhotoGridAdapter(isSelected = { vm.isSelected(it) }) { image ->
            if (!vm.toggle(image, Collages.MAX_PHOTOS)) {
                Toast.makeText(this, getString(R.string.max_photos, Collages.MAX_PHOTOS), Toast.LENGTH_SHORT).show()
            }
        }
        val folderAdapter = FolderAdapter { vm.openFolder(it) }
        val selectedAdapter = SelectedAdapter { vm.remove(it) }
        binding.photos.adapter = grid
        binding.folders.adapter = folderAdapter
        binding.selectedList.adapter = selectedAdapter
        (binding.selectedList.layoutManager as androidx.recyclerview.widget.LinearLayoutManager).orientation =
            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL

        vm.visible.observe(this) {
            grid.submit(it)
            binding.emptyText.show(it.isEmpty() && vm.folders.value.orEmpty().isNotEmpty())
        }
        vm.folders.observe(this) { folderAdapter.submit(it) }
        vm.selected.observe(this) { list ->
            selectedAdapter.submit(list)
            grid.refreshSelection()
            binding.tvSelected.text = if (list.isEmpty()) getString(R.string.picker_select_at_least)
            else getString(R.string.picker_selected_count, list.size)
            binding.btnClearAll.show(list.isNotEmpty())
            binding.selectedList.show(list.isNotEmpty())
            binding.tvNoneSelected.show(list.isEmpty())
            updateRight()
        }
        vm.currentFolderName.observe(this) { updateTitle() }
        vm.folderMode.observe(this) { folderMode ->
            binding.folders.show(folderMode)
            binding.photos.show(!folderMode)
            binding.sheet.show(true)
            updateTitle()
            updateRight()
        }
        binding.btnClearAll.setOnClickListener { vm.clear() }
    }

    override fun initAction() {
        binding.actionBar.setLeft { onBackPressedDispatcher.onBackPressed() }
        binding.actionBar.tvCenter.setOnClickListener { vm.toggleFolderMode() }
        binding.actionBar.setTitle(getString(R.string.picker_recents))
        updateRight()
        ensurePermission()
    }

    /** The title is the open folder with an arrow; the arrow points up while the folder list is open. */
    private fun updateTitle() {
        val name = vm.currentFolderName.value.orEmpty().ifEmpty { getString(R.string.picker_recents) }
        val arrow = if (vm.folderMode.value == true) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
        binding.actionBar.setTitle(name)
        binding.actionBar.tvCenter.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, arrow, 0)
        binding.actionBar.tvCenter.compoundDrawablePadding = dp(6)
    }

    /** The check confirms: it closes the folder list, or returns the photos when the photo grid is open. */
    private fun updateRight() {
        val inFolders = vm.folderMode.value == true
        val canConfirm = inFolders || vm.selected.value.orEmpty().isNotEmpty()
        binding.actionBar.setRight(R.drawable.ic_check_blue) {
            if (vm.folderMode.value == true) vm.toggleFolderMode() else finishWithPhotos()
        }
        binding.actionBar.btnActionBarRight.visibility = if (canConfirm) android.view.View.VISIBLE else android.view.View.INVISIBLE
    }

    private fun finishWithPhotos() {
        val chosen = vm.selected.value.orEmpty()
        if (chosen.isEmpty()) return
        val clip = ClipData.newUri(contentResolver, "photos", chosen.first().uri)
        chosen.drop(1).forEach { clip.addItem(ClipData.Item(it.uri)) }
        setResult(RESULT_OK, Intent().apply {
            clipData = clip
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
        finish()
    }

    private fun ensurePermission() {
        if (ContextCompat.checkSelfPermission(this, readPermission) == PackageManager.PERMISSION_GRANTED) {
            vm.load(this, getString(R.string.picker_recents))
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(readPermission), REQ)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            vm.load(this, getString(R.string.picker_recents))
        } else {
            Toast.makeText(this, R.string.picker_permission_needed, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /** Back closes the folder list first. */
    override fun handleBackPressed(): Boolean {
        if (vm.folderMode.value == true) { vm.toggleFolderMode(); return true }
        return false
    }

    private companion object { const val REQ = 2001 }
}
