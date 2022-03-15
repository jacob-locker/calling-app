package com.locker.callingapp.repository.call

import android.content.Context
import android.util.Log
import com.locker.callingapp.TAG
import com.locker.callingapp.repository.cloud.CloudResult
import com.locker.callingapp.repository.cloud.WebRtcCloudDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.webrtc.*
import org.webrtc.IceCandidate
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebRtcProxyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webRtcCloudDao: WebRtcCloudDao,
    private val coroutineScope: CoroutineScope
) : WebRtcProxy {

    private val iceServer by lazy {
        listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer()
        )
    }

    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private val previousCandidates = CopyOnWriteArrayList<com.locker.callingapp.repository.call.IceCandidate>()

    private var isStarted = false
    private var isJoining = true
    private var roomId: String? = null

    private val offered = mutableSetOf<String>()
    private val answered = mutableSetOf<String>()
    private val allCandidates = mutableSetOf<String>()

    override fun start(roomId: String) {
        if (isStarted) return
        isStarted = true
        this.roomId = roomId

        initPeerConnectionFactory()
        startLocalAudioCapture()

        coroutineScope.launch {
            webRtcCloudDao.connections.collect { connectionInfoList ->
                Log.d(TAG, "Collecting  connections $connectionInfoList")
                connectionInfoList.filter { it.type == SessionDescription.Type.OFFER.name && it.fromUserId != webRtcCloudDao.currentUser?.id && it.fromUserId !in answered }
                    .forEach { offerConnection ->
                        answered.add(offerConnection.fromUserId)
                        setRemoteDescription(offerConnection.sessionDescription)
                        answer(offerConnection.fromUserId)
                    }
                connectionInfoList.filter { it.type == SessionDescription.Type.ANSWER.name && it.fromUserId != webRtcCloudDao.currentUser?.id }
                    .forEach { answerConnection ->
                        offered.add(answerConnection.fromUserId)
                        setRemoteDescription(answerConnection.sessionDescription)
                    }
            }
        }

        coroutineScope.launch {
            webRtcCloudDao.candidates.collect { candidateList ->
                Log.d(TAG, "Collecting candidates: $candidateList")
                val newCandidates = candidateList.map { it.id }.toSet()
                peerConnection?.removeIceCandidates(previousCandidates.filter { it.id !in newCandidates }
                    .map { it.toWebRtcIceCandidate() }.toTypedArray())
                previousCandidates.clear()
                previousCandidates.addAll(candidateList)

                candidateList.filter { it.id !in allCandidates && it.userId != webRtcCloudDao.currentUser?.id }
                    .forEach {
                        addIceCandidate(it.toWebRtcIceCandidate())
                    }
            }
        }
    }

    override fun stop(roomId: String) {
        isStarted = false
        peerConnection?.close()
        previousCandidates.clear()
        answered.clear()
        coroutineScope.launch {
            webRtcCloudDao.removeAllCandidatesForUser(webRtcCloudDao.currentUser?.id!!, roomId)
            webRtcCloudDao.removeAllConnectionsForUser(webRtcCloudDao.currentUser?.id!!, roomId)
        }
    }

    private fun startLocalAudioCapture() {
        with(buildPeerConnectionFactory()) {
            val audioSource = createAudioSource(MediaConstraints())
            localAudioTrack = createAudioTrack(LOCAL_TRACK_ID + "_audio", audioSource)

            val localStream = createLocalMediaStream(LOCAL_STREAM_ID)
            localStream.addTrack(localAudioTrack)
            peerConnection = buildPeerConnection(this)
            peerConnection?.addStream(localStream)
        }
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory
            .builder()
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    private fun buildPeerConnection(factory: PeerConnectionFactory) = factory.createPeerConnection(
        iceServer,
        object : PeerConnection.Observer {
            override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {
                Log.d(TAG, "onSignalingChange: ")
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "onIceConnectionChange: ")
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                Log.d(TAG, "onIceConnectionReceivingChange: ")
            }

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "onIceGatheringChange: ")
            }

            override fun onIceCandidate(candidate: IceCandidate) {
                    // Other parties need to add this candidate to their peer connection
                    coroutineScope.launch {
                        signalIceCandidate(candidate, isJoining)

                        addIceCandidate(candidate)
                    }
            }

            override fun onIceCandidatesRemoved(p0: Array<out org.webrtc.IceCandidate>?) {
                Log.d(TAG, "onIceCandidatesRemoved: ")
            }

            override fun onAddStream(p0: MediaStream?) {
                Log.d(TAG, "onAddStream: ")
            }

            override fun onRemoveStream(p0: MediaStream?) {
                Log.d(TAG, "onRemoveStream: ")
            }

            override fun onDataChannel(p0: DataChannel?) {
                Log.d(TAG, "onDataChannel: ")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ")
            }

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                Log.d(TAG, "onAddTrack: ")
            }

        }
    )

    private suspend fun signalIceCandidate(
        iceCandidate: IceCandidate,
        isJoining: Boolean
    ) {
        Log.d(TAG, "Signalling ice candidate.")
        webRtcCloudDao.addCandidate(
            iceCandidate.toIceCandidate(
                userId = webRtcCloudDao.currentUser?.id!!,
                roomId = roomId!!,
                isJoin = isJoining
            )
        )
    }

    private fun addIceCandidate(iceCandidate: IceCandidate) {
        Log.d(TAG, "Adding ice candidate to peer connection")
        peerConnection?.addIceCandidate(iceCandidate)
    }

    override suspend fun call(
        userId: String
    ): CloudResult<Boolean> =
        suspendCoroutine { cont ->
            isJoining = false
            peerConnection?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetFailure(p0: String?) {
                            Log.e(this@WebRtcProxyImpl.TAG, "call: onSetFailure: $p0")
                            cont.resume(CloudResult.Failure(null))
                        }

                        override fun onSetSuccess() {
                            Log.d(TAG, "call: onSetSuccess: \n${desc.description}")
                            Log.d(TAG, "call: onSetSuccess: ${desc.type.name}")
                            runBlocking {
                                cont.resume(
                                    webRtcCloudDao.setConnection(
                                        ConnectionInfo(
                                            roomId!!,
                                            fromUserId = webRtcCloudDao.currentUser?.id!!,
                                            toUserId = userId,
                                            desc.description,
                                            desc.type.name
                                        )
                                    )
                                )
                            }
                        }

                        override fun onCreateSuccess(p0: SessionDescription?) {
                            Log.e(TAG, "call: onCreateSuccess: Description $p0")
                        }

                        override fun onCreateFailure(p0: String?) {
                            Log.e(TAG, "call: onCreateFailure: $p0")
                            cont.resume(CloudResult.Failure(null))
                        }
                    }, desc)
                }

                override fun onSetSuccess() {
                    Log.i(this@WebRtcProxyImpl.TAG, "call: onSetSuccess")
                }

                override fun onSetFailure(p0: String?) {
                    Log.e(this@WebRtcProxyImpl.TAG, "call: onSetFailure: $p0")
                    cont.resume(CloudResult.Failure(null))
                }

                override fun onCreateFailure(p0: String?) {
                    Log.e(this@WebRtcProxyImpl.TAG, "call: onCreateFailure: $p0")
                    cont.resume(CloudResult.Failure(null))
                }
            }, MediaConstraints())
        }

    private suspend fun answer(toUserId: String): CloudResult<Boolean> =
        suspendCoroutine { cont ->
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetFailure(failureMessage: String) {
                            Log.e(TAG, "answer: onSetFailure: $failureMessage")
                            cont.resume(CloudResult.Failure(message = failureMessage))
                        }

                        override fun onSetSuccess() {
                            Log.e(TAG, "answer: onSetSuccess")
                            runBlocking {
                                cont.resume(
                                    webRtcCloudDao.setConnection(
                                        ConnectionInfo(
                                            roomId = roomId!!,
                                            toUserId = toUserId,
                                            fromUserId = webRtcCloudDao.currentUser?.id!!,
                                            sdp = desc.description,
                                            type = desc.type.name
                                        )
                                    )
                                )
                            }
                        }

                        override fun onCreateSuccess(p0: SessionDescription?) {
                            Log.e(TAG, "answer: onCreateSuccess: Description $p0")
                        }

                        override fun onCreateFailure(p0: String?) {
                            Log.e(TAG, "answer: onCreateFailureLocal: $p0")
                        }
                    }, desc)
                }

                override fun onSetSuccess() {
                    Log.d(TAG, "answer: onSetSuccess: ")
                }

                override fun onCreateFailure(failureMessage: String) {
                    Log.e(
                        TAG,
                        "answer: onCreateFailureRemote: $failureMessage\nIt's current state is ${peerConnection?.connectionState()}"
                    )
                    cont.resume(CloudResult.Failure(message = failureMessage))
                }

                override fun onSetFailure(failureMessage: String) {
                    Log.d(TAG, "answer: onSetFailure: $failureMessage")
                    cont.resume(CloudResult.Failure(message = failureMessage))
                }
            }, MediaConstraints())
        }

    private fun setRemoteDescription(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "setRemoteDesc: onSetFailure: $p0")
            }

            override fun onSetSuccess() {
                Log.e(TAG, "setRemoteDesc: onSetSuccessRemoteSession")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.e(TAG, "setRemoteDesc: onCreateSuccessRemoteSession: Description $p0")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "setRemoteDesc: onCreateFailure")
            }
        }, sessionDescription)
    }

    companion object {
        private const val LOCAL_TRACK_ID = "local_track"
        private const val LOCAL_STREAM_ID = "local_track"
    }
}