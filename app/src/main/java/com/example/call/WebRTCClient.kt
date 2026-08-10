package com.example.call

import android.content.Context
import android.util.Log
import org.webrtc.*
import java.util.*

/**
 * WebRTCClient manages PeerConnectionFactory, PeerConnection, local media track acquisition,
 * SDP negotiations, ICE candidates, and camera switching.
 */
class WebRTCClient(
    private val context: Context,
    private val eglBaseContext: EglBase.Context,
    private val listener: WebRTCListener
) {
    companion object {
        private const val TAG = "WebRTCClient"
        private const val AUDIO_TRACK_ID = "ARDAMSa0"
        private const val VIDEO_TRACK_ID = "ARDAMSv0"
        private const val STREAM_ID = "ARDAMS"
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    interface WebRTCListener {
        fun onIceCandidateCreated(candidate: IceCandidate)
        fun onRemoteTrackAdded(transceiver: RtpTransceiver)
        fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState)
    }

    init {
        initializePeerConnectionFactory()
        peerConnectionFactory = createPeerConnectionFactory()
        peerConnection = createPeerConnection()
    }

    private fun initializePeerConnectionFactory() {
        Log.d(TAG, "Initializing PeerConnectionFactory")
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        val encoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    private fun createPeerConnection(): PeerConnection? {
        val iceServers = IceServerProvider.getIceServers()
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val pcObserver = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "onSignalingChange: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "onIceConnectionChange: $state")
                state?.let { listener.onIceConnectionStateChanged(it) }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "onIceConnectionReceivingChange: $receiving")
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "onIceGatheringChange: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                Log.d(TAG, "onIceCandidate: $candidate")
                candidate?.let { listener.onIceCandidateCreated(it) }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "onIceCandidatesRemoved")
            }

            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "onAddStream: ${stream?.id}")
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "onRemoveStream")
            }

            override fun onDataChannel(channel: DataChannel?) {
                Log.d(TAG, "onDataChannel")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded")
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                Log.d(TAG, "onAddTrack")
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                Log.d(TAG, "onTrack (Unified Plan)")
                transceiver?.let { listener.onRemoteTrackAdded(it) }
            }
        }

        return peerConnectionFactory?.createPeerConnection(rtcConfig, pcObserver)
    }

    /**
     * Set up local audio capturing.
     */
    fun startLocalAudio() {
        Log.d(TAG, "Starting local audio track")
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
        
        localAudioTrack?.let {
            peerConnection?.addTrack(it, listOf(STREAM_ID))
        }
    }

    /**
     * Set up local video capturing and optionally pipe to local view.
     */
    fun startLocalVideo(localView: SurfaceViewRenderer) {
        Log.d(TAG, "Starting local video track")
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }

        videoCapturer = createVideoCapturer(enumerator)
        if (videoCapturer == null) {
            Log.e(TAG, "Failed to create video capturer")
            return
        }

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
        localVideoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
        
        videoCapturer!!.initialize(surfaceTextureHelper, context, localVideoSource!!.capturerObserver)
        // Request standard 640x480 at 30fps
        videoCapturer!!.startCapture(640, 480, 30)

        localVideoTrack = peerConnectionFactory?.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
        
        // Add renderer to view local screen
        localVideoTrack?.addSink(localView)

        localVideoTrack?.let {
            peerConnection?.addTrack(it, listOf(STREAM_ID))
        }
    }

    private fun createVideoCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        // Try front facing first
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        // Try any back camera next
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        return null
    }

    /**
     * Toggle microphone on/off.
     */
    fun toggleMic(enabled: Boolean) {
        Log.d(TAG, "Toggling mic: $enabled")
        localAudioTrack?.setEnabled(enabled)
    }

    /**
     * Toggle local camera stream on/off.
     */
    fun toggleVideo(enabled: Boolean) {
        Log.d(TAG, "Toggling local video: $enabled")
        localVideoTrack?.setEnabled(enabled)
    }

    /**
     * Switch between front and back camera.
     */
    fun switchCamera() {
        Log.d(TAG, "Switching camera facing direction")
        val cameraCapturer = videoCapturer as? CameraVideoCapturer ?: return
        cameraCapturer.switchCamera(null)
    }

    /**
     * Initiates standard SDP offer creation.
     */
    fun createOffer(observer: SdpObserver) {
        Log.d(TAG, "Creating SDP Offer")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(object : SdpObserver by observer {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d(TAG, "SDP Offer created successfully, setting local description")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set successfully for Offer")
                        observer.onCreateSuccess(sdp)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(err: String?) {
                        Log.e(TAG, "Failed to set local description: $err")
                        observer.onSetFailure(err)
                    }
                }, sdp)
            }
        }, constraints)
    }

    /**
     * Initiates standard SDP answer creation.
     */
    fun createAnswer(observer: SdpObserver) {
        Log.d(TAG, "Creating SDP Answer")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createAnswer(object : SdpObserver by observer {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d(TAG, "SDP Answer created successfully, setting local description")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set successfully for Answer")
                        observer.onCreateSuccess(sdp)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(err: String?) {
                        Log.e(TAG, "Failed to set local description: $err")
                        observer.onSetFailure(err)
                    }
                }, sdp)
            }
        }, constraints)
    }

    /**
     * Set remote description SDP from opponent.
     */
    fun setRemoteDescription(sdp: SessionDescription, observer: SdpObserver) {
        Log.d(TAG, "Setting Remote Description")
        peerConnection?.setRemoteDescription(observer, sdp)
    }

    /**
     * Insert received remote ICE Candidate.
     */
    fun addIceCandidate(candidate: IceCandidate) {
        Log.d(TAG, "Adding remote ICE candidate: ${candidate.sdp}")
        peerConnection?.addIceCandidate(candidate)
    }

    /**
     * Request ICE Restart on peerConnection.
     */
    fun restartIce() {
        Log.d(TAG, "Requesting ICE Restart on peerConnection")
        peerConnection?.restartIce()
    }

    /**
     * Clear up resources properly to prevent memory leaks.
     */
    fun close() {
        Log.d(TAG, "Closing WebRTC Client resources")
        try {
            videoCapturer?.stopCapture()
        } catch (ignored: Exception) {}
        
        try {
            videoCapturer?.dispose()
        } catch (ignored: Exception) {}
        
        surfaceTextureHelper?.dispose()
        
        try {
            peerConnection?.dispose()
        } catch (ignored: Exception) {}
        
        try {
            localAudioSource?.dispose()
            localVideoSource?.dispose()
            peerConnectionFactory?.dispose()
        } catch (ignored: Exception) {}
    }
}
