package com.app.unfit20.ui.profile

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.unfit20.model.Post

/**
 * For ViewPager2: Each page is a ProfileTabFragment (Posts tab or Liked tab).
 */
class ProfilePagerAdapter(
    fragment: Fragment,
    private val onPostClick: (Post) -> Unit,
    private val onUserClick: (String) -> Unit
) : FragmentStateAdapter(fragment) {

    private var userPosts: List<Post> = emptyList()
    private var userLikedPosts: List<Post> = emptyList()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        // Decide which tab we’re making
        val tabType = when (position) {
            0 -> ProfileTabFragment.TAB_POSTS
            1 -> ProfileTabFragment.TAB_LIKED
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
        // Create the fragment, pass data right away
        return ProfileTabFragment.newInstance(tabType).apply {
            setCallbacks(onPostClick, onUserClick)
            val data = if (tabType == ProfileTabFragment.TAB_POSTS) userPosts else userLikedPosts
            setPostsData(data)
        }
    }

    // Called when user’s own posts change
    fun updatePosts(posts: List<Post>) {
        userPosts = posts
        notifyItemChanged(0) // triggers rebind for first tab
    }

    // Called when user’s liked posts change
    fun updateLikedPosts(posts: List<Post>) {
        userLikedPosts = posts
        notifyItemChanged(1) // triggers rebind for second tab
    }
}