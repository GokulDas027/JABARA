package com.lloc.sceneformcodelab

import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference


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
        andy.setContentDescription("andy")
        andy.setOnClickListener { view: View? ->
            addObject(
                Uri.parse("andy.sfb")
            )
        }
        gallery.addView(andy)

        // add cabin to the view on tap
        val cabin = ImageView(this)
        cabin.setImageResource(R.drawable.cabin_thumb)
        cabin.setContentDescription("cabin")
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
    }

    fun onException(throwable: Throwable) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(throwable.message)
            .setTitle("Codelab error!")
        val dialog: AlertDialog = builder.create()
        dialog.show()
        return
    }
}
