package com.example.depthwallpaper.main.staticphoto.picker

import android.content.ClipData
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
        grid = PhotoGridAdapter(isSelected = { vm.isSelected(it) }, onAdd = { openSystemPicker() }) { image ->
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

    private val pickMany = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { vm.add(it, maxPhotos) }
    private val pickOne = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.add(listOf(it), maxPhotos) } }

    /** The system photo picker (PickVisualMedia): no storage permission needed. */
    private fun openSystemPicker() {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (maxPhotos > 1) pickMany.launch(request) else pickOne.launch(request)
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

    companion object {
        /** How many photos may be chosen (default: the collage maximum). */
        const val EXTRA_MAX = "max"
        private const val MAX_PICK = 20
    }
}
