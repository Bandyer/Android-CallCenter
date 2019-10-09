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
import kotlinx.android.synthetic.main.user_selection_item.*

/**
 * A simple RecyclerView item used to display the user name with a checkbox as a cell in the list.
 */
class UserSelectionItem(var name: String) : AbstractItem<UserSelectionItem, UserSelectionItem.ViewHolder>() {

    override fun getType(): Int {
        return R.id.user_selection_item_id
    }

    override fun getLayoutRes(): Int {
        return R.layout.user_selection_item
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(override var containerView: View) : FastAdapter.ViewHolder<UserSelectionItem>(containerView), LayoutContainer {

        override fun bindView(item: UserSelectionItem?, payloads: MutableList<Any>?) {
            checkbox.isChecked = item?.isSelected ?: false
            name.text = item?.name
        }

        override fun unbindView(item: UserSelectionItem?) {
            checkbox.isChecked = false
            name.text = null
        }
    }
}