/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center.adapter_items

import android.view.View
import com.bandyer.demo_communication_center.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_user.*

/**
 * A simple RecyclerView item used to display the user alias as a cell in the list.
 */
class UserItem(var userAlias: String) : AbstractItem<UserItem, UserItem.ViewHolder>() {

    override fun getType(): Int {
        return R.id.user_item_id
    }

    override fun getLayoutRes(): Int {
        return R.layout.item_user
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(override val containerView: View) : FastAdapter.ViewHolder<UserItem>(containerView), LayoutContainer {

        override fun bindView(item: UserItem, payloads: List<Any>) {
            userAlias.text = item.userAlias
        }

        override fun unbindView(item: UserItem) {
            userAlias.text = null
        }
    }
}