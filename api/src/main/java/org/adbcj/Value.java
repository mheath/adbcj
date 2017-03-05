/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.adbcj;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Holds a field value. The {@code Value} methods attempt to convert the field
 * value to Java types.
 * 
 * @author Mike Heath
 */
public interface Value {

    /**
     * Return the field type for this value.
     * 
     * @return the field type for this value.
     */
    Field getField();

    /**
     * The value as a {@code BigDecimal} with full precision . If the value is
     * {@code null}, returns {@code null}.
     * 
     * @return the value as a {@link BigDecimal} or {@code null} if the value is
     * {@code null}.
     * @throws NumberFormatException if the value is not a valid representation
     * of a {@code BigDecimal}.
     */
    BigDecimal getBigDecimal();

    /**
     * Returns the value as a {@code boolean}. If the value is {@code null},
     * returns false. If the value is a numeric type, return {@code true} if the
     * the value is not 0. Otherwise, returns {@true} if the value as a
     * {@code String} is {@code "true"}.
     * 
     * @return the value as a boolean.
     */
    boolean getBoolean();

    /**
     * Returns the value as a {@link Date}. If the value is {@code null},
     * returns {@code null}.
     * 
     * @return the value as a {@link Date} or {@code null} if the value is
     * {@code null}.
     * @throws DbException if the value is not a date type
     */
    Date getDate();

    /**
     * Returns the value as a {@code double}. If the value is {@code null},
     * returns 0.
     * 
     * @return the value as a {@code double} or 0 if the value is {@code null}.
     * @throws NumberFormatException if the value is not a valid representation
     * of a {@code double}.
     */
    double getDouble();

    /**
     * Returns the value as a {@code float}. If the value is {@code null},
     * returns 0.
     * 
     * @return the value as a {@code float} or 0 if the value is {@code null}.
     * @throws NumberFormatException if the value is not a valid representation
     * of a {@code float}.
     */
    float getFloat();

    /**
     * Returns the value as a {@code int}. If the value is {@code null}, returns
     * 0.
     * 
     * @return the value as a {@code double} or 0 if the value is {@code null}.
     * @throws NumberFormatException if the value is not a valid representation
     * of a {@code double}.
     */
    int getInt();

    /**
     * Returns the value as a {@code long}. If the value is {@code null},
     * returns 0.
     * 
     * @return the value as a {@code long} or 0 if the value is {@code null}.
     * @throws NumberFormatException if the value is not a valid representation
     * of a {@code long}.
     */
    long getLong();

    /**
     * Returns the value as a {@link String}. If the value is {@code null},
     * returns {@code null}.
     * 
     * @return the value as a {@link String} or {@code null} if the value is
     * {@code null}.
     */
    String getString();

    /**
     * Returns the value in its native type. If the value is {@code null},
     * return {@code null}.
     * 
     * @return the avleu in its native or {@code null} if the value is
     * {@code null}.
     */
    Object getValue();

    /**
     * Returns the value in its native type. If the value is {@code null},
     * return {@code null}.
     * 
     * @return the avleu in its native or {@code null} if the value is
     * {@code null}.
     */
    short getShort();

    /**
     * Returns {@code true} if the value is {@code null}, {@code false}
     * otherwise.
     * 
     * @return {@code true} if the value is {@code null}, {@code false}
     * otherwise.
     */
    boolean isNull();

}
