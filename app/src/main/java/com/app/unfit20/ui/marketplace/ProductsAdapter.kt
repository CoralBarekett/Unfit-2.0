package com.app.unfit20.ui.marketplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.unfit20.R
import com.app.unfit20.databinding.ItemProductBinding
import com.app.unfit20.model.ProductInfo
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.text.NumberFormat
import java.util.Locale

class ProductsAdapter(
    private val onProductClick: (ProductInfo) -> Unit
) : ListAdapter<ProductInfo, ProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProductClick(getItem(position))
                }
            }
        }

        fun bind(product: ProductInfo) = with(binding) {
            // Product name
            tvProductName.text = product.title

            // Price (formatted or "N/A")
            tvProductPrice.text = formatPrice(product.price)

            // Rating (with explicit locale)
            tvProductRating.text = product.rating?.let {
                String.format(Locale.getDefault(), "%.1f", it)
            } ?: "N/A"

            // Product image
            Glide.with(ivProduct.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivProduct)
        }

        private fun formatPrice(price: Double?): String {
            if (price == null) return "N/A"
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            return formatter.format(price)
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductInfo>() {
        override fun areItemsTheSame(oldItem: ProductInfo, newItem: ProductInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductInfo, newItem: ProductInfo): Boolean {
            return oldItem == newItem
        }
    }
}