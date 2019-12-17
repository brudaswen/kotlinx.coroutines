/*
 * Copyright 2016-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.channels

import kotlinx.coroutines.*
import kotlin.test.*

class BroadcastTest : TestBase() {
    @Test
    fun testBroadcastBasic() = runTest {
        expect(1)
        val b = broadcast {
            expect(4)
            send(1) // goes to receiver
            expect(5)
            send(2) // goes to buffer
            expect(6)
            send(3) // suspends, will not be consumes, but will not be cancelled either
            expect(10)
        }
        yield() // has no effect, because default is lazy
        expect(2)
        b.consume {
            expect(3)
            assertEquals(1, receive()) // suspends
            expect(7)
            assertEquals(2, receive()) // suspends
            expect(8)
        }
        expect(9)
        yield() // to broadcast
        finish(11)
    }

    /**
     * See https://github.com/Kotlin/kotlinx.coroutines/issues/1713
     */
    @Test
    fun testChannelBroadcastLazyClose() = runTest {
        expect(1)
        val a = produce {
            expect(3)
            try {
                send("MSG")
            } finally {
                expect(5)
            }
            expectUnreached()
        }
        expect(2)
        yield() // to produce
        val b = a.broadcast()
        b.close()
        expect(4)
        yield() // to abort produce
        assertTrue(a.isClosedForReceive) // the source channel was consumed
        finish(6)
    }

    @Test
    fun testChannelBroadcastEagerClose() = runTest {
        expect(1)
        val a = produce<Unit> {
            expect(3)
            yield() // back to main
            expectUnreached() // will be cancelled
        }
        expect(2)
        val b = a.broadcast(start = CoroutineStart.DEFAULT)
        yield() // to produce
        expect(4)
        b.close()
        yield() // to produce (cancelled)
        assertTrue(a.isClosedForReceive) // the source channel was consumed
        finish(5)
    }
}