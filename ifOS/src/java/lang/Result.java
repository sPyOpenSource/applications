/*
 * Copyright (C) 2026 xuyi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package java.lang;

import java.util.function.Consumer;

/**
 *
 * @author xuyi
 * @param <T>
 * @param <E>
 */
public class Result<T, E> {
    
    private final T value;
    private final E error;
    private final boolean isSuccess;

    private Result(T value, E error, boolean isSuccess) {
        this.value = value;
        this.error = error;
        this.isSuccess = isSuccess;
    }

    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(value, null, true);
    }

    public static <T, E> Result<T, E> failure(E error) {
        return new Result<>(null, error, false);
    }

    public boolean isSuccess() { return isSuccess; }
    public boolean isFailure() { return !isSuccess; }

    // Execute actions based on the outcome (Java 8 style)
    public void handle(Consumer<T> onSuccess, Consumer<E> onFailure) {
        if (isSuccess) {
            onSuccess.accept(value);
        } else {
            onFailure.accept(error);
        }
    }

}
