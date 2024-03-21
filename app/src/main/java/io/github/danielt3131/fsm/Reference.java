/*
 * Copyright (c) 2024 Daniel J. Thompson.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 or later.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.danielt3131.fsm;

public class Reference {
    public static final int SEGMENT_SIZE_EMAIL_PRESET = 20000000; // 20MB
    public static final int SEGMENT_SIZE_MMS_PRESET = 1000000;
    public static final int INPUT_FILE = 10;
    public static final String SAVE_LOCATION = "/storage/emulated/0/Documents/FSM/";
    public static int segmentSize = 0;
    public static int MAX_BUFFFERSIZE = 1024 * 1024 * 250;

}
