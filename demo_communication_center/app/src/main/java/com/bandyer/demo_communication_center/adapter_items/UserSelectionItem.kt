/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center.adapter_items

import android.view.View
import com.bandyer.demo_communication_center.R
import com.bandyer.demo_communication_center.databinding.UserSelectionItemBinding
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

/**
 * A simple RecyclerView item used to display the user name with a checkbox as a cell in the list.
 */
class UserSelectionItem(var name: String) : AbstractItem<UserSelectionItem.ViewHolder>() {

    override val layoutRes: Int
        get() = R.layout.user_selection_item

    override val type: Int
        get() = R.id.user_selection_item_id

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<UserSelectionItem>(view) {

        val binding: UserSelectionItemBinding = UserSelectionItemBinding.bind(view)

        override fun bindView(item: UserSelectionItem, payloads: List<Any>) {
            binding.checkbox.isChecked = item.isSelected
            binding.name.text = item.name
        }

        override fun unbindView(item: UserSelectionItem) {
            binding.checkbox.isChecked = false
            binding.name.text = null
        }
    }
}