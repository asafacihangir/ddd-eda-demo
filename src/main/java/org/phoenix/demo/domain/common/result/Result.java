package org.phoenix.demo.domain.common.result;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;


public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }


    T getValue();


    E getError();

    <R> Result<R, E> map(Function<? super T, ? extends R> mapper);

    <R> Result<R, E> flatMap(Function<? super T, Result<R, E>> mapper);

    record Success<T, E>(T value) implements Result<T, E> {
        public Success {
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public E getError() {
            throw new NoSuchElementException("Result is a Success — no error present.");
        }

        @Override
        public <R> Result<R, E> map(Function<? super T, ? extends R> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return Result.success(mapper.apply(value));
        }

        @Override
        public <R> Result<R, E> flatMap(Function<? super T, Result<R, E>> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return mapper.apply(value);
        }
    }

    record Failure<T, E>(E error) implements Result<T, E> {
        public Failure {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T getValue() {
            throw new NoSuchElementException("Result is a Failure — no success value present.");
        }

        @Override
        public E getError() {
            return error;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> Result<R, E> map(Function<? super T, ? extends R> mapper) {
            return (Result<R, E>) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> Result<R, E> flatMap(Function<? super T, Result<R, E>> mapper) {
            return (Result<R, E>) this;
        }
    }
}
