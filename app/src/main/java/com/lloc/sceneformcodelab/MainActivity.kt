package com.lloc.sceneformcodelab

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.Menu
import android.view.MenuItem
import android.view.PixelCopy
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.rendering.AnimationData
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
class MainActivity : AppCompatActivity() {
    private var fragment = ArFragment()
    private val pointer = PointerDrawable()
    private var modelLoader: ModelLoader? = null
    private var isTracking = false
    private val isHitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment

        fragment.arSceneView.scene
            .addOnUpdateListener { frameTime: FrameTime? ->
                fragment.onUpdate(frameTime)
                onUpdate()
            }
        modelLoader = ModelLoader(WeakReference(this))
        initializeGallery()
        fab.setOnClickListener { takePhoto()}
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onUpdate() {
        val trackingChanged: Boolean = updateTracking()
        val contentView: View = findViewById(android.R.id.content)
        if (trackingChanged) {
            if (isTracking) {
                contentView.overlay.add(pointer)
            } else {
                contentView.overlay.remove(pointer)
            }
            contentView.invalidate()
        }
        if (isTracking) {
            val hitTestChanged: Boolean = updateHitTest()
            if (hitTestChanged) {
                pointer.setEnabled(isHitting)
                contentView.invalidate()
            }
        }
    }

    private fun updateTracking(): Boolean {
        val frame: Frame? = fragment.arSceneView.arFrame
        val wasTracking = isTracking
        isTracking = frame != null &&
                frame.camera.trackingState === TrackingState.TRACKING
        return isTracking != wasTracking
    }

    private fun updateHitTest(): Boolean {
        val frame: Frame? = fragment.arSceneView.arFrame
        val pt: Point = getScreenCenter()
        val hits: List<HitResult?>
        val wasHitting = isHitting
        var isHitting = false
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane &&
                    (trackable).isPoseInPolygon(hit.hitPose)
                ) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }

    private fun getScreenCenter(): Point {
        val vw: View = findViewById(android.R.id.content)
        return Point(vw.width / 2, vw.height / 2)
    }

    private fun initializeGallery() {
        val gallery: LinearLayout = findViewById(R.id.gallery_layout)

        // add andy to the view on tap
        val andy = ImageView(this)
        andy.setImageResource(R.drawable.droid_thumb)
        andy.contentDescription = "andy"
        andy.setOnClickListener { view: View? ->
            addObject(
                Uri.parse("andy.sfb")
            )
        }
        gallery.addView(andy)

        // add cabin to the view on tap
        val cabin = ImageView(this)
        cabin.setImageResource(R.drawable.cabin_thumb)
        cabin.contentDescription = "cabin"
        cabin.setOnClickListener { view: View? ->
            addObject(
                Uri.parse("Cabin.sfb")
            )
        }
        gallery.addView(cabin)

        // add house to the view
        val house = ImageView(this)
        house.setImageResource(R.drawable.house_thumb)
        house.contentDescription = "house"
        house.setOnClickListener { view: View? ->
            addObject(
                Uri.parse("House.sfb")
            )
        }
        gallery.addView(house)

        // add igloo to the view
        val igloo = ImageView(this)
        igloo.setImageResource(R.drawable.igloo_thumb)
        igloo.contentDescription = "igloo"
        igloo.setOnClickListener { view: View? ->
            addObject(
                Uri.parse("igloo.sfb")
            )
        }
        gallery.addView(igloo)
    }

    private fun addObject(model: Uri) {
        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane &&
                    trackable.isPoseInPolygon(hit.hitPose)
                ) {
                    modelLoader!!.loadModel(hit.createAnchor(), model)
                    break
                }
            }
        }
    }

    fun addNodeToScene(anchor: Anchor?, renderable: ModelRenderable?) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(anchorNode)
        node.select()
        startAnimation(node, renderable)
    }

    // Animation not working, moved on to next, will check later
    private fun startAnimation(node: TransformableNode, renderable: ModelRenderable?) {
        if (renderable == null || renderable.animationDataCount == 0) {
            return
        }
        for (i in 0 until renderable.animationDataCount) {
            val animationData: AnimationData = renderable.getAnimationData(i)
        }
        val animator = ModelAnimator(renderable.getAnimationData(0), renderable)
//        animator.start() //start animation on placing the object itself
        node.setOnTapListener{_, _ ->
            print("onNodeTap recorded")
            togglePauseAndResume(animator)
        }
    }

    private fun togglePauseAndResume(animator: ModelAnimator) {
        when {
            animator.isPaused -> {
                animator.resume()
            }
            animator.isStarted -> {
                animator.pause()
            }
            else -> {
                animator.start()
            }
        }
    }

    private fun generateFilename(): String {
        val date: String =
            SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return if (android.os.Build.VERSION.SDK.toInt() < 29 ) {
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ).toString() + File.separator.toString() + "Sceneform/" + date + "_screenshot.jpg"
        } else {
            getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            ).toString() + File.separator.toString() + "Sceneform/" + date + "_screenshot.jpg"
        }
    }

    @Throws(IOException::class)
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {
        val out = File(filename)
        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            FileOutputStream(filename).use { outputStream ->
                ByteArrayOutputStream().use { outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
            }
        } catch (ex: IOException) {
            throw IOException("Failed to save bitmap to disk", ex)
        }
    }

    private fun takePhoto() {
        val filename: String = generateFilename()
        val view: ArSceneView = fragment.arSceneView

        // Create a bitmap the size of the scene view.
        val bitmap: Bitmap = Bitmap.createBitmap(view.width, view.height,
            Bitmap.Config.ARGB_8888)

        // handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()

        // make the request to copy
        PixelCopy.request(view,bitmap, {copyResult :Int ->
            if(copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename)
                } catch (e: IOException) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
                }
                val snackbar: Snackbar = Snackbar.make(findViewById(android.R.id.content),
                    "Photo saved", Snackbar.LENGTH_LONG)
                snackbar.setAction("Open in Photos") {
                    val photoFile = File(filename)

                    val photoURI: Uri = FileProvider.getUriForFile(this,
                        this.packageName+".ar.codelab.name.provider",
                        photoFile)
                    val intent = Intent(Intent.ACTION_VIEW, photoURI)
                    intent.setDataAndType(photoURI, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                snackbar.show()
            } else {
                Toast.makeText(this, "Failed to Capture", Toast.LENGTH_LONG).show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    fun onException(throwable: Throwable) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(throwable.message)
            .setTitle("App error!")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        return
    }
}

