package dev.aaa1115910.biliapi.http

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BiliLiveHttpApiTest {
    @Test
    fun `get history live room danmaku`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getLiveDanmuHistory(roomId = 22739471)
                println(response)
            }
        }
    }

    @Test
    fun `get live event websocket connect url and token`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getLiveDanmuInfo(roomId = 22739471)
                println(response)
            }
        }
    }

    @Test
    fun `get live room info`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getLiveRoomPlayInfo(roomId = 22739471)
                println(response)
            }
        }
    }

    @Test
    fun `get live list`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getLiveList()
                println(response)
            }
        }
    }

    @Test
    fun `get live recommend`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getLiveRecommend()
                println(response)
            }
        }
    }

    @Test
    fun `get follow live`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getFollowLive()
                println(response)
            }
        }
    }

    @Test
    fun `get live area list`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getLiveAreaList()
                println(response)
            }
        }
    }

    @Test
    fun `get area live list`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getAreaLiveList(
                    parentAreaId = 2,
                    areaId = 0,
                    page = 1
                )
                println(response)
            }
        }
    }

    @Test
    fun `get room init`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getRoomInit(roomId = 22739471)
                println(response)
            }
        }
    }

    @Test
    fun `get room info`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getRoomInfo(roomId = 22739471)
                println(response)
            }
        }
    }

    @Test
    fun `get room play info v2`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getRoomPlayInfoV2(roomId = 22739471)
                println(response)
            }
        }
    }

    @Test
    fun `get live stream url`() {
        Assertions.assertDoesNotThrow {
            runBlocking {
                val response = BiliLiveHttpApi.getLiveStreamUrl(cid = 22739471)
                println(response)
            }
        }
    }

}
