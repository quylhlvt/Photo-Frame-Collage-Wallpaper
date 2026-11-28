package com.example.depthwallpaper.main.staticphoto.picker

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.setLeft
import com.example.depthwallpaper.core.setRight
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.core.show
import com.example.depthwallpaper.databinding.ActivityPhotoPickerBinding
import com.example.depthwallpaper.main.staticphoto.Collages

/**
 * "Choose photo": the photos the user added (from the system photo picker, so no storage permission is
 * needed) as a grid, plus a tray of the chosen ones with Clear All. "+" adds more photos, the check
 * returns the chosen photos as a ClipData of Uris.
 */
class PhotoPickerActivity : AbsBaseActivity<ActivityPhotoPickerBinding>(ActivityPhotoPickerBinding::inflate) {
    private val vm: PickerViewModel by viewModels()
    private lateinit var grid: PhotoGridAdapter
    private val maxPhotos get() = intent.getIntExtra(EXTRA_MAX, Collages.MAX_PHOTOS)

    override fun initView() {
        grid = PhotoGridAdapter(isSelected = { vm.isSelected(it) }, onAdd = { openSystemPicker() }, onLongPress = { confirmDelete(it) }) { image ->
            if (!vm.toggle(image, maxPhotos)) {
                Toast.makeText(this, getString(R.string.max_photos, maxPhotos), Toast.LENGTH_SHORT).show()
            }
        }
        val selectedAdapter = SelectedAdapter { vm.unselect(it) }
        binding.photos.adapter = grid
        binding.selectedList.adapter = selectedAdapter
        (binding.selectedList.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL

        vm.added.observe(this) { grid.submit(it) }
        vm.selected.observe(this) { list ->
            selectedAdapter.submit(list)
            grid.refreshSelection()
            binding.tvSelected.text = if (list.isEmpty()) getString(R.string.picker_select_at_least)
            else getString(R.string.picker_selected_count, list.size)
            binding.btnClearAll.show(list.isNotEmpty())
            binding.selectedList.show(list.isNotEmpty())
            binding.tvNoneSelected.show(list.isEmpty())
            binding.actionBar.btnActionBarRight.visibility = if (list.isNotEmpty()) View.VISIBLE else View.INVISIBLE
        }
        binding.btnClearAll.setOnClickListener { vm.clear() }
    }

    override fun initAction() {
        binding.actionBar.apply {
            setTitle(getString(R.string.picker_title))
            setLeft { onBackPressedDispatcher.onBackPressed() }
            setRight(R.drawable.ic_check_blue) { finishWithPhotos() }
        }
    }

    private val pickMany = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { vm.add(it) }

    /** The system photo picker (PickVisualMedia): no storage permission needed. */
    private fun openSystemPicker() {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        pickMany.launch(request)
    }

    /** Long press removes a photo from the saved list. */
    private fun confirmDelete(image: com.example.depthwallpaper.core.gallery.GalleryImage) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(R.string.picker_delete_confirm)
            .setPositiveButton(R.string.project_delete) { _, _ -> vm.delete(image) }
            .setNegativeButton(R.string.project_cancel, null)
            .show()
    }

    private fun finishWithPhotos() {
        val chosen = vm.selected.value.orEmpty()
        if (chosen.isEmpty()) return
        // File paths, not Uris: a file:// Uri may not leave the app inside an Intent.
        setResult(RESULT_OK, Intent().putStringArrayListExtra(EXTRA_PATHS, ArrayList(chosen.mapNotNull { it.uri.path })))
        finish()
    }

    companion object {
        /** Result / hand-over extra: the chosen photos as file paths. */
        const val EXTRA_PATHS = "paths"

        /** The photos in [intent] as Uris (empty when there are none). */
        fun urisFrom(intent: Intent?): List<android.net.Uri> =
            intent?.getStringArrayListExtra(EXTRA_PATHS).orEmpty().map { android.net.Uri.fromFile(java.io.File(it)) }

        /** How many photos may be chosen (default: the collage maximum). */
        const val EXTRA_MAX = "max"
        private const val MAX_PICK = 20
    }
}
