/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */

package com.dmsl.anyplace.googleapi;

import java.io.Serializable;

import com.dmsl.anyplace.nav.IPoisClass;
import com.google.api.client.util.Key;

public class Place implements Serializable, IPoisClass {

	@Key
	public String id;

	@Key
	public String name;

	@Key
	public String reference;

	@Key
	public String icon;

	@Key
	public String vicinity;

	@Key
	public Geometry geometry;

	@Key
	public String formatted_address = "";

	@Key
	public String formatted_phone_number;

	@Override
	public String toString() {
		return name + " - " + id + " - " + reference;
	}

	public static class Geometry implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6671182645807819149L;
		@Key
		public Location location;
	}

	public static class Location implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 177304297858841489L;

		@Key
		public double lat;

		@Key
		public double lng;
	}

	@Override
	public double lat() {
		return geometry.location.lat;
	}

	@Override
	public double lng() {
		return geometry.location.lng;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String description() {
		return formatted_address;
	}

	@Override
	public Type type() {
		return Type.GooglePlace;
	}

	@Override
	public String id() {
		// maybe we should return reference
		return id;
	}
}