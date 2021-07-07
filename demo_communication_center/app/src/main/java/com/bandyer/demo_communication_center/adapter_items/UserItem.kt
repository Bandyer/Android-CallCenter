/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center.adapter_items

import android.view.View
import com.bandyer.demo_communication_center.R
import com.bandyer.demo_communication_center.databinding.ItemUserBinding
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

/**
 * A simple RecyclerView item used to display the user alias as a cell in the list.
 */
class UserItem(var userAlias: String) : AbstractItem<UserItem.ViewHolder>() {

    override val layoutRes: Int
        get() = R.layout.item_user

    override val type: Int
        get() = R.id.user_item_id

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<UserItem>(view) {

        private val binding: ItemUserBinding = ItemUserBinding.bind(view)

        override fun bindView(item: UserItem, payloads: List<Any>) {
            binding.userAlias.text = item.userAlias
        }

        override fun unbindView(item: UserItem) {
            binding.userAlias.text = null
        }
    }
}