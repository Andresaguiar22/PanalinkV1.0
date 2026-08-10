package com.example.creative.post

import com.example.creative.core.CreativeLayer
import org.junit.Assert.*
import org.junit.Test

class PostCarouselTest {

    @Test
    fun testCarouselPageManagement() {
        val manager = PostPageManager(initialPages = listOf(PostPage(id = "p1", pageIndex = 0)))

        assertEquals(1, manager.pages.value.size)

        manager.addPage(PostPage(id = "p2"))
        assertEquals(2, manager.pages.value.size)

        manager.duplicatePage("p1")
        assertEquals(3, manager.pages.value.size)

        manager.removePage("p2")
        assertEquals(2, manager.pages.value.size)
    }

    @Test
    fun testCarouselReorder() {
        val manager = PostPageManager(
            initialPages = listOf(
                PostPage(id = "p1", pageIndex = 0),
                PostPage(id = "p2", pageIndex = 1),
                PostPage(id = "p3", pageIndex = 2)
            )
        )

        manager.movePage(0, 2)

        val pages = manager.pages.value
        assertEquals("p2", pages[0].id)
        assertEquals("p3", pages[1].id)
        assertEquals("p1", pages[2].id)
    }
}
