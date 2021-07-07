/*
 * Copyright (C) 2020 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center.model

import kotlinx.serialization.Serializable

/**
 * FileUploaded
 * @suppress
 * @author kristiyan
 **/
@Serializable
class FileUploaded(val url: String, val senderName: String, val userAlias: String?)