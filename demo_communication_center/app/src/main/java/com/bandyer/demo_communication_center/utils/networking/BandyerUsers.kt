/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center.utils.networking

import java.util.*

/**
 * WARNING!!!
 * The networking package is used only to fetch the users, to make the demo app run out of the box.
 * With the least efforts.
 * <p>
 * Model used to map the users coming from the mocked network call
 *
 * @author kristiyan
 */
data class BandyerUsers(val user_id_list: ArrayList<String>?)
