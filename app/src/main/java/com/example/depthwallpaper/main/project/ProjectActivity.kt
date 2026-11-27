package com.example.depthwallpaper.main.project

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.WallpaperUi
import com.example.depthwallpaper.core.show
import com.example.depthwallpaper.core.setLeft
import com.example.depthwallpaper.core.setRight
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.core.showRight
import com.example.depthwallpaper.databinding.ActivityProjectBinding
import com.example.depthwallpaper.databinding.PopupProjectMenuBinding
import com.example.depthwallpaper.main.service.DepthWallpaperService
import com.example.depthwallpaper.main.service.VideoWallpaperService

/**
 * Saved wallpapers. Tap one (or its "..." button) for Download / Set / Delete; long press to pick
 * several and Share, Download or Delete them together.
 */
class ProjectActivity : AbsBaseActivity<ActivityProjectBinding>(ActivityProjectBinding::inflate) {
    private val vm: ProjectViewModel by viewModels()
    private lateinit var store: ProjectStore
    private lateinit var adapter: ProjectAdapter
    private var menu: PopupWindow? = null
    private var menuProject: Project? = null

    override fun initView() {
        store = ProjectStore(this)
        adapter = ProjectAdapter(
            onMenu = { p, anchor -> showMenu(p, anchor) },
            onToggle = { vm.toggle(it.id) },
            onLongPress = { vm.startSelecting(it.id) },
        )
        binding.list.adapter = adapter
        val refresh = { adapter.submit(vm.projects.value.orEmpty(), vm.selecting.value == true, vm.selectedIds.value.orEmpty()) }
        vm.projects.observe(this) { refresh() }
        vm.selecting.observe(this) { refresh() }
        vm.selectedIds.observe(this) { refresh() }
        // The trash, the bottom bar and the empty text follow the ViewModel.
        vm.selecting.observe(this) {
            binding.actionBar.showRight(it)
            binding.bottomBar.show(it)
        }
        vm.isEmpty.observe(this) { binding.emptyText.show(it) }
    }

    override fun initAction() {
        binding.actionBar.apply {
            setTitle(getString(R.string.project_title))
            setLeft { onBackPressedDispatcher.onBackPressed() }
            setRight(R.drawable.ic_trash_red) { confirmDelete(vm.selectedProjects()) }
            showRight(false)
        }
        binding.btnShare.setOnClickListener { share(vm.selectedProjects()) }
        binding.btnDownload.setOnClickListener { download(vm.selectedProjects()) }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        Thread {
            val list = store.list()
            runOnUiThread { if (!isFinishing) vm.projects.value = list }
        }.start()
    }

    // ---- pop-up menu on one project ----
    private fun showMenu(p: Project, anchor: View) {
        menu?.dismiss()
        menuProject = p
        val pb = PopupProjectMenuBinding.inflate(LayoutInflater.from(this))
        pb.menuDownload.setOnClickListener { menu?.dismiss(); menuProject?.let { download(listOf(it)) } }
        pb.menuSet.setOnClickListener { menu?.dismiss(); menuProject?.let { setWallpaper(it) } }
        pb.menuDelete.setOnClickListener { menu?.dismiss(); menuProject?.let { confirmDelete(listOf(it)) } }
        val popup = PopupWindow(pb.root, dp(190), android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.elevation = dp(8).toFloat()
        popup.showAsDropDown(anchor, -dp(150), -dp(8))
        menu = popup
    }

    // ---- actions ----
    private fun setWallpaper(p: Project) {
        Thread {
            val ok = store.apply(p)
            runOnUiThread {
                if (!ok) {
                    Toast.makeText(this, R.string.project_set_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val service = if (p.type == ProjectType.VIDEO) VideoWallpaperService::class.java else DepthWallpaperService::class.java
                WallpaperUi.openWallpaperPicker(this, service)
            }
        }.start()
    }

    private fun download(list: List<Project>) {
        // Before Android 10, writing to the gallery needs the storage permission.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 7)
            Toast.makeText(this, R.string.project_storage_permission, Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val ok = list.all { store.saveToGallery(it) }
            runOnUiThread {
                Toast.makeText(this, if (ok) R.string.project_downloaded else R.string.project_download_failed, Toast.LENGTH_SHORT).show()
                vm.stopSelecting()
            }
        }.start()
    }

    private fun share(list: List<Project>) {
        Thread {
            val uris = list.mapNotNull { store.shareUri(it) }
            runOnUiThread {
                if (uris.isEmpty()) return@runOnUiThread
                val send = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
                else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                send.type = if (list.all { it.type == ProjectType.VIDEO }) "video/*" else if (list.none { it.type == ProjectType.VIDEO }) "image/*" else "*/*"
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(Intent.createChooser(send, null))
            }
        }.start()
    }

    private fun confirmDelete(list: List<Project>) {
        AlertDialog.Builder(this)
            .setMessage(R.string.project_delete_confirm)
            .setPositiveButton(R.string.project_delete) { _, _ ->
                list.forEach { store.delete(it) }
                vm.stopSelecting()
                reload()
            }
            .setNegativeButton(R.string.project_cancel, null)
            .show()
    }

    override fun handleBackPressed(): Boolean {
        if (vm.selecting.value == true) {
            vm.stopSelecting()
            return true
        }
        return false
    }
}
