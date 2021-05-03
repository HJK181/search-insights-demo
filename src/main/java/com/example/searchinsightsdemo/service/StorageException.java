package com.example.searchinsightsdemo.service;


public class StorageException extends RuntimeException {

	private static final long serialVersionUID = 1653087901777144461L;

	public StorageException(String message) {
		super(message);
	}

	public StorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
