package com.example.searchinsightsdemo.service;


public class StorageFileNotFoundException extends StorageException {

	private static final long serialVersionUID = -4582267306504202071L;

	public StorageFileNotFoundException(String message) {
		super(message);
	}

	public StorageFileNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
