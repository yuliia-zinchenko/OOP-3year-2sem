package com.library.command;

/**
 * GoF: Command. Encapsulates an operation as an object.
 */
public interface Command<R> {
    R execute();
}
