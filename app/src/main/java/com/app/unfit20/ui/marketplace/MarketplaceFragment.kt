package com.app.unfit20.ui.marketplace

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.app.unfit20.R
import com.app.unfit20.databinding.FragmentMarketplaceBinding
import com.app.unfit20.model.ProductInfo

class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketplaceViewModel by viewModels()
    private lateinit var productsAdapter: ProductsAdapter // define your adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // We now have a <layout> root in fragment_marketplace.xml,
        // so this will generate FragmentMarketplaceBinding
        _binding = FragmentMarketplaceBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Load products on fragment creation
        viewModel.loadProducts()
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter { product ->
            // Handle product click
            navigateToProductDetail(product)
        }
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productsAdapter
        }
    }

    private fun setupListeners() {
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadProducts()
        }

        // Setup empty state
        // Because layout_empty_state.xml is also a <layout>,
        // we can do binding.layoutEmptyState.tvEmptyTitle, etc.
        binding.layoutEmptyState.tvEmptyTitle.text = getString(R.string.no_products_found)
        binding.layoutEmptyState.tvEmptyDescription.text = getString(R.string.empty_products_description)
        binding.layoutEmptyState.btnCreateFirstPost.text = getString(R.string.retry)
        binding.layoutEmptyState.btnCreateFirstPost.setOnClickListener {
            viewModel.loadProducts()
        }

        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    findNavController().navigate(
                        MarketplaceFragmentDirections.actionMarketplaceFragmentToHomeFragment()
                    )
                    true
                }
                R.id.nav_marketplace -> {
                    // Already here
                    true
                }
                R.id.nav_profile -> {
                    findNavController().navigate(
                        MarketplaceFragmentDirections.actionMarketplaceFragmentToProfileFragment()
                    )
                    true
                }
                else -> false
            }
        }
        // Mark marketplace as selected
        binding.bottomNavigation.selectedItemId = R.id.nav_marketplace
    }

    private fun observeViewModel() {
        // Observe main product list
        viewModel.products.observe(viewLifecycleOwner) { products ->
            updateUI(products)
            binding.swipeRefresh.isRefreshing = false
        }

        // Observe loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility =
                if (isLoading && !binding.swipeRefresh.isRefreshing) View.VISIBLE
                else View.GONE
        }

        // Observe error
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun updateUI(products: List<ProductInfo>) {
        productsAdapter.submitList(products)
        if (products.isEmpty()) {
            binding.layoutEmptyState.root.visibility = View.VISIBLE
            binding.rvProducts.visibility = View.GONE
        } else {
            binding.layoutEmptyState.root.visibility = View.GONE
            binding.rvProducts.visibility = View.VISIBLE
        }
    }

    private fun navigateToProductDetail(product: ProductInfo) {
        // TODO: Implement product detail screen
        Toast.makeText(requireContext(), "Selected: ${product.title}", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_marketplace, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_products)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    viewModel.searchProducts(query)
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                // optional: implement real-time search
                return false
            }
        })

        // Reset products when search is closed
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.loadProducts()
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                Toast.makeText(requireContext(), "Filter not implemented yet", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}