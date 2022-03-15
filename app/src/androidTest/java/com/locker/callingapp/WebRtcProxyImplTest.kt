package com.locker.callingapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.locker.callingapp.model.CallRoom
import com.locker.callingapp.model.RoomInvite
import com.locker.callingapp.model.User
import com.locker.callingapp.repository.call.ConnectionInfo
import com.locker.callingapp.repository.call.WebRtcProxyImpl
import com.locker.callingapp.repository.cloud.CallRoomCloudDao
import com.locker.callingapp.repository.cloud.CloudResult
import com.locker.callingapp.repository.cloud.WebRtcCloudDao
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class WebRtcProxyImplTest {

    @MockK
    private lateinit var webRtcProxyImpl: WebRtcProxyImpl

    @MockK
    private lateinit var callRoomCloudDao: CallRoomCloudDao
    private val activeRoomFlow = MutableStateFlow(ACTIVE_ROOM)

    @MockK
    private lateinit var webRtcCloudDao: WebRtcCloudDao

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        every { callRoomCloudDao.activeRoom } returns activeRoomFlow

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        webRtcProxyImpl =
            WebRtcProxyImpl(
                appContext,
                callRoomCloudDao,
                webRtcCloudDao,
                CoroutineScope(Dispatchers.IO)
            )
        webRtcProxyImpl.start()
    }

    @Test
    fun testConstruction() {

    }

    @Test
    fun testStart() {
        webRtcProxyImpl.start()
        Thread.sleep(Long.MAX_VALUE)
    }

    @Test
    fun testCall() {
        runBlocking {
            val result =
                (webRtcProxyImpl.call(ACTIVE_ROOM, User("user3")) as CloudResult.Success).value
            assertEquals(ACTIVE_ROOM.id, result.roomId)
            assertEquals("Unexpected type", "OFFER", result.type)

            println("---------------Start SDP----------------")
            println(result.sdp)
            println("-----------------End SDP-------------")
        }

        Thread.sleep(Long.MAX_VALUE)
    }

    @Test
    fun testAnswer() {
        runBlocking {
            val caller = ACTIVE_ROOM.userInfo[0]
            val calledUser = User("user3")
//            val callResult = webRtcProxyImpl.call(ACTIVE_ROOM, calledUser)
//            val callValue = (callResult as CloudResult.Success).value
//            assertEquals(ACTIVE_ROOM.id, callValue.roomId)
//            assertEquals("Unexpected type", "OFFER", callValue.type)
            val answerResult = webRtcProxyImpl.answer(
                RoomInvite(
                    "inviteId",
                    from = caller,
                    to = calledUser,
                    roomId = ACTIVE_ROOM.id,
                    connectionInfo = ConnectionInfo(
                        ACTIVE_ROOM.id,
                        SDP,
                        "OFFER"
                    )
                )
            )

            //delay(Long.MAX_VALUE)
        }
    }

    companion object {
        val ACTIVE_ROOM = CallRoom.Active(
            "roomId",
            listOf("user1", "user2"),
            mutableListOf(User("user1", "User 1"), User("user2", "User 2"))
        )

        const val SDP = """
v=0
o=- 8668434053056933479 2 IN IP4 127.0.0.1
    s=-
    t=0 0
    a=group:BUNDLE audio
    a=msid-semantic: WMS local_track
    m=audio 9 RTP/AVPF 111 103 104 9 102 0 8 106 105 13 110 112 113 126
    c=IN IP4 0.0.0.0
    a=rtcp:9 IN IP4 0.0.0.0
    a=ice-ufrag:ldvx
    a=ice-pwd:/8G+EIH8dQvmZx9iUyEdHSQE
    a=ice-options:trickle renomination
    a=mid:audio
    a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level
    a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
    a=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
    a=sendrecv
    a=rtcp-mux
    a=rtpmap:111 opus/48000/2
    a=rtcp-fb:111 transport-cc
    a=fmtp:111 minptime=10;useinbandfec=1
    a=rtpmap:103 ISAC/16000
    a=rtpmap:104 ISAC/32000
    a=rtpmap:9 G722/8000
    a=rtpmap:102 ILBC/8000
    a=rtpmap:0 PCMU/8000
    a=rtpmap:8 PCMA/8000
    a=rtpmap:106 CN/32000
    a=rtpmap:105 CN/16000
    a=rtpmap:13 CN/8000
    a=rtpmap:110 telephone-event/48000
    a=rtpmap:112 telephone-event/32000
    a=rtpmap:113 telephone-event/16000
    a=rtpmap:126 telephone-event/8000
    a=ssrc:3355963663 cname:12q62eP3qTcAfZMh
    a=ssrc:3355963663 msid:local_track local_track_audio
    a=ssrc:3355963663 mslabel:local_track
    a=ssrc:3355963663 label:local_track_audio
        """
    }
}