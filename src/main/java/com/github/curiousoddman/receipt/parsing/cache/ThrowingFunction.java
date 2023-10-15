package com.github.curiousoddman.receipt.parsing.cache;

public interface ThrowingFunction<IN, OUT, EX extends Exception> {

    OUT apply(IN in) throws EX;
}
