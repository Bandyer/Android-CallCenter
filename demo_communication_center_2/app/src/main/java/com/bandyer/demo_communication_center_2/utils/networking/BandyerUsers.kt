package com.bandyer.demo_communication_center_2.utils.networking

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
