package xyz.klinker.messenger.fragment.message.attach

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.yalantis.ucrop.UCrop
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.MessageListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import java.io.File

class AttachmentManager(private val fragment: MessageListFragment) {

    private val activity
        get() = fragment.activity
    private val argManager
        get() = fragment.argManager

    var attachedUri: Uri? = null
    var attachedMimeType: String? = null
    val selectedImageUris = mutableListOf<String>()

    private val editImage: View? by lazy { fragment.rootView!!.findViewById<View>(R.id.edit_image) }
    private val removeImage: View by lazy { fragment.rootView!!.findViewById<View>(R.id.remove_image) }
    private val selectedImageCount: TextView by lazy { fragment.rootView!!.findViewById<View>(R.id.selected_images) as TextView }
    private val attachLayout: View? by lazy { fragment.rootView!!.findViewById<View>(R.id.attach_layout) }
    private val attachedImage: ImageView by lazy { fragment.rootView!!.findViewById<View>(R.id.attached_image) as ImageView }
    private val attachedImageHolder: View by lazy { fragment.rootView!!.findViewById<View>(R.id.attached_image_holder) }
    private val attach: View by lazy { fragment.rootView!!.findViewById<View>(R.id.attach) }

    fun setupHelperViews() {
        editImage?.setBackgroundColor(argManager.colorAccent)
        editImage?.setOnClickListener {
            try {
                val options = UCrop.Options()
                options.setToolbarColor(argManager.color)
                options.setStatusBarColor(argManager.colorDark)
                options.setActiveWidgetColor(argManager.colorAccent)
                options.setCompressionFormat(Bitmap.CompressFormat.JPEG)
                options.setCompressionQuality(100)

                val destination = File.createTempFile("ucrop", "jpg", activity.cacheDir)
                UCrop.of(attachedUri!!, Uri.fromFile(destination))
                        .withOptions(options)
                        .start(activity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        removeImage.setBackgroundColor(argManager.colorAccent)
        removeImage.setOnClickListener {
            clearAttachedData()
            selectedImageUris.clear()
            selectedImageCount.visibility = View.GONE

            if (attachLayout?.visibility == View.VISIBLE) {
                fragment.onBackPressed()
            }
        }

        selectedImageCount.setBackgroundColor(argManager.colorAccent)
    }

    fun clearAttachedData() {
        if (activity != null) {
            DataSource.deleteDrafts(activity, argManager.conversationId)
        }

        attachedImage.setImageDrawable(null)
        attachedImageHolder.visibility = View.GONE
        selectedImageCount.visibility = View.GONE

        attachedUri = null
        attachedMimeType = null
        selectedImageUris.clear()

        fragment.counterCalculator.updateCounterText()
    }

    fun writeDraftOfAttachment() {
        if (attachedUri != null && attachedMimeType != null) {
            DataSource.insertDraft(activity, argManager.conversationId, attachedUri.toString(), attachedMimeType!!)
        }
    }

    fun attachImage(uri: Uri?) {
        if (editImage == null) {
            // this happens when opening the full screen capture image intent
            // then rotating from landscape to portrait
            // the view has not been initialized by the time we are trying to deliver the results, so delay it.
            Handler().postDelayed({ attachImage(uri) }, 500)
            return
        }

        editImage?.visibility = View.VISIBLE

        clearAttachedData()
        attachedUri = uri
        attachedMimeType = MimeType.IMAGE_JPG

        attachedImageHolder.visibility = View.VISIBLE

        try {
            if (activity != null) {
                Glide.with(fragment).load(uri)
                        .apply(RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .placeholder(R.drawable.ic_image_sending))
                        .into(attachedImage)
                fragment.counterCalculator.updateCounterText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun attachAudio(uri: Uri) {
        clearAttachedData()
        attachedUri = uri
        attachedMimeType = MimeType.AUDIO_MP4
        editImage?.visibility = View.GONE

        attachedImageHolder.visibility = View.VISIBLE
        attachedImage.setImageResource(R.drawable.ic_audio_sent)
        fragment.counterCalculator.updateCounterText()
    }

    fun attachContact(uri: Uri) {
        clearAttachedData()
        attachedUri = uri
        attachedMimeType = MimeType.TEXT_VCARD
        editImage?.visibility = View.GONE

        attachedImageHolder.visibility = View.VISIBLE
        attachedImage.setImageResource(R.drawable.ic_contacts)
        attachedImage.imageTintList = ColorStateList.valueOf(Color.BLACK)
        fragment.counterCalculator.updateCounterText()
    }

    fun backPressed() = if (attachLayout?.visibility == View.VISIBLE) {
        attach.isSoundEffectsEnabled = false
        attach.performClick()
        attach.isSoundEffectsEnabled = true
        true
    } else {
        false
    }
}