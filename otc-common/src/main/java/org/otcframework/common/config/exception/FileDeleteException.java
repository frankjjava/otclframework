/**
* Copyright (c) otcframework.org
*
* @author  Franklin J Abel
* @version 1.0
* @since   2020-06-08 
*
* This file is part of the OTC framework.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/
package org.otcframework.common.config.exception;

import org.otcframework.common.exception.OtcException;

/**
 * The Class OtcConfigException.
 */
public class FileDeleteException extends OtcException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -3222880717814289359L;

	/**
	 * Instantiates a new otc config exception.
	 *
	 * @param errorCode the error code
	 */
	public FileDeleteException(String errorCode) {
		super(errorCode);
	}

	/**
	 * Instantiates a new otc config exception.
	 *
	 * @param errorCode the error code
	 * @param msg       the msg
	 */
	public FileDeleteException(String errorCode, String msg) {
		super(errorCode, msg);
	}

	/**
	 * Instantiates a new otc config exception.
	 *
	 * @param cause the cause
	 */
	public FileDeleteException(Throwable cause) {
		super(cause);
	}

	/**
	 * Instantiates a new otc config exception.
	 *
	 * @param errorCode the error code
	 * @param cause     the cause
	 */
	public FileDeleteException(String errorCode, Throwable cause) {
		super(errorCode, cause);
	}

	/**
	 * Instantiates a new otc config exception.
	 *
	 * @param errorCode the error code
	 * @param msg       the msg
	 * @param cause     the cause
	 */
	public FileDeleteException(String errorCode, String msg, Throwable cause) {
		super(errorCode, msg, cause);
	}
}
