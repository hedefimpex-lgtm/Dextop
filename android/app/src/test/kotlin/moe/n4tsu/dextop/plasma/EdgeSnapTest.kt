package moe.n4tsu.dextop.plasma

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EdgeSnapTest {
    // Work area 0..1920 wide, starting at the top of the display.
    private fun zone(left: Int, top: Int, right: Int) = EdgeSnap.zone(left, top, right, 0, 0, 1920, 8)

    @Test fun floatingWindowAwayFromEdgesStays() {
        assertNull(zone(400, 200, 1200))
    }

    @Test fun leftEdgeSnapsToLeftHalf() {
        assertEquals(TileZone.LEFT, zone(0, 200, 800))
        assertEquals(TileZone.LEFT, zone(6, 200, 806))
        assertEquals(TileZone.LEFT, zone(-120, 200, 680))
    }

    @Test fun rightEdgeSnapsToRightHalf() {
        assertEquals(TileZone.RIGHT, zone(1120, 200, 1920))
        assertEquals(TileZone.RIGHT, zone(1300, 200, 2100))
    }

    @Test fun sideEdgesWinOverTheTopEdge() {
        assertEquals(TileZone.LEFT, zone(0, 0, 800))
        assertEquals(TileZone.RIGHT, zone(1120, 0, 1920))
    }

    @Test fun topEdgeMaximizes() {
        assertEquals(TileZone.MAXIMIZE, zone(400, 0, 1200))
    }

    @Test fun justOutsideTheThresholdStays() {
        assertNull(zone(9, 200, 809))
        assertNull(zone(400, 1, 1200))
    }

    @Test fun fullWidthWindowNeverSnaps() {
        assertNull(zone(0, 0, 1920))
        assertNull(zone(-10, 300, 1930))
    }
}
